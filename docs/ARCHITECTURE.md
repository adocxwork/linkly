# Linkly: how the backend works

A study guide to the code, organised the way an interviewer would ask about it.

## 1. Package map

| Package | Responsibility |
|---|---|
| `controller` | HTTP endpoints only: validate input, call a service, return DTOs |
| `service` | Business logic: auth, links, users, messages, short-code generation |
| `analytics` | The click pipeline: publisher, stream worker, batch writer, Geo-IP, UA parsing, IP hashing |
| `redis` | `RedisGateway` (the only class touching Redis, with a circuit breaker), JSON cache, key names |
| `security` | JWT creation and validation, auth filter, rate-limit filter, Spring Security config |
| `system` | Admin switches (`SystemSettingsService`), health report, self-ping, admin seeding |
| `web` | `ClientIpResolver` (real client IP behind proxies) |
| `entity` / `repository` / `dto` | JPA entities, Spring Data repositories, request and response objects |
| `config` | Redis connection factory, OpenAPI |
| `resources/db/migration` | Flyway SQL migrations: the single source of truth for the schema |

## 2. Request flows

### Redirect: `GET /r/{code}` (the hot path)
1. `RateLimitingFilter` checks the client IP's token bucket (120/min). Over the limit gives `429`.
2. `RedirectController` resolves the client IP (`ClientIpResolver`) and calls `LinkService.resolveRedirect`.
3. Cache-aside lookup: `redirect:v2:{code}` in Redis.
   - Hit: no database access at all.
   - Miss: one query (`findRedirectTarget`) that joins the owner to check "active" and "not suspended".
     The result is cached for 1 hour.
   - Redis down or disabled: the gateway returns "miss" immediately, so we go to the DB.
4. `ClickEventPublisher.publish`: bots and link-preview fetchers are ignored. The event is added to
   the Redis Stream with `XADD … MAXLEN ~ 10000`. If Redis is unavailable, it goes to the bounded
   in-memory `LocalClickBuffer` instead.
5. The response is `302 Found` with `Cache-Control: no-store`. It is not a 301, because browsers
   cache 301s and would skip us, losing analytics and ignoring later edits to the link.
6. Unknown, inactive or suspended links redirect to the frontend's `/link-error` page.

### Click processing (background)
`ClickStreamWorker.tick()` runs every 2 s on a scheduler thread:
1. It drains up to 100 events from the in-memory buffer. If storing fails, they are put back.
2. It reads the Redis Stream only if there is a reason to: an in-memory flag says "this instance
   published", or the 5-minute sweep is due. That keeps idle Redis usage near zero, which matters on
   Upstash's free quota.
3. It reads through a consumer group: first its own **pending** records (`ID 0`, delivered earlier
   but never acknowledged), then **new** ones (`ID >`).
4. `ClickProcessor`: one batched Geo-IP HTTP call for all unique IPs (cached with Caffeine, paused
   for 5 minutes after a failure). Then `ClickBatchWriter` stores everything in one transaction.
5. `XACK` runs only after a successful commit (at-least-once delivery). On failure the records stay
   pending and are retried on the next sweep. After 5 failures they are acknowledged and counted as
   "dead-lettered".

### Authenticated API request
1. `JwtAuthenticationFilter` reads `Authorization: Bearer …`, verifies the HMAC signature and
   expiry, loads the user, and compares the token's `ver` claim with `users.token_version`.
   - Mismatch (password changed, logged out everywhere, suspended): the request is treated as anonymous, giving `401`.
   - Suspended user: `401` "Your account has been suspended".
2. Spring Security's URL rules decide access: `/api/admin/**` needs `ROLE_ADMIN`, everything not
   public needs authentication.
3. Services look up resources *together with the owner* (`findByIdAndUserUsername`), so "not yours"
   and "doesn't exist" both return `404`. Nobody can probe other users' IDs.

## 3. Failure handling

| Failure | Effect | Why |
|---|---|---|
| Redis unreachable | Caches skipped, clicks go to the in-memory queue, `/admin` shows Redis DOWN | `RedisGateway` circuit breaker; Lettuce `REJECT_COMMANDS` fails fast instead of queueing |
| `REDIS_URL` not set | App starts normally, runs without Redis | We build `LettuceConnectionFactory` ourselves; Spring Boot refuses an empty `spring.data.redis.url` |
| Admin turns Redis off | Zero Redis calls from then on; persisted across restarts | `system_settings.redis_enabled`, cached in memory |
| Upstash quota exhausted | Same as unreachable: breaker opens and fallbacks take over | All Redis errors trip the breaker |
| Geo-IP API down | Clicks stored with country "Unknown"; lookups paused for 5 minutes | Third-party failures must not lose clicks |
| DB down while processing clicks | Batch not acknowledged or re-queued; retried later | At-least-once |
| DB down entirely | API returns 500; `/actuator/health` = DOWN; liveness stays UP (no restart loop) | PostgreSQL is the system of record and cannot be bypassed |
| In-memory queue full | New clicks dropped and counted (`droppedBufferFull`) | Bounded memory beats crashing with an out-of-memory error |
| Render instance asleep | First request takes about 50 s (cold start) | Free tier. Mitigated by the self-ping or an external pinger |

## 4. Data model

```
users ──< links ──< click_analytics
  └────< messages
system_settings (single row "DEFAULT")
```
- All foreign keys use `ON DELETE CASCADE`. Deleting a user is one SQL statement, and the database
  removes their links, clicks and messages. The JPA entities deliberately do **not** map these
  collections: a JPA cascade would load every child row into memory first.
- `links.click_count` is a denormalised counter (fast dashboards). `click_analytics` holds the detail.
- `click_analytics.ip_hash` is `HMAC-SHA256(secret, ip)`. That is enough to count unique visitors
  without storing personal data.
- Indexes: `links(user_id)`, `click_analytics(link_id, clicked_at)` for the per-link time series,
  `messages(user_id, created_at)`.

### Migrations
- `V1` builds the schema on a fresh database.
- `V2` widens `click_count` to `bigint`.
- `V3` is the reliability and security release: token versions, the Redis switch column, IP hashing,
  cascading FKs and indexes. It is written defensively (`IF EXISTS`, `DO` blocks) because production
  was originally built by Hibernate. Flyway **baselines** an existing database at V1 and then applies V2 and V3.

## 5. Trade-offs (likely interview questions)

- **Why Redis Streams rather than Kafka or RabbitMQ?** Redis was already there for caching; Streams
  provide consumer groups, acknowledgements and pending lists, which is enough for one consumer at
  this scale. Kafka would add cost and operational weight for no benefit here.
- **Why not write the click synchronously?** The redirect would then pay for a DB write plus a
  third-party Geo-IP call. Queuing keeps redirects in single-digit milliseconds. The price is
  eventual consistency: dashboards lag by a couple of seconds.
- **Why cache-aside with short TTLs?** On writes we evict keys *after the transaction commits*. If
  Redis was unreachable during an eviction, the TTL (10 min for profiles, 1 h for redirects) limits
  how long stale data can survive.
- **Why JWT version numbers?** Pure stateless JWTs cannot be revoked. One integer per user, checked
  against the user row we load anyway, makes revocation possible at no extra cost.
- **Why in-memory rate limiting?** There is one instance on Render, and Redis-backed buckets would
  spend Upstash quota on every request. With several instances, buckets would move to Redis.
- **Why the right-most `X-Forwarded-For` entry?** Each proxy appends what it saw. Entries to the left
  of our own proxy are whatever the client sent, so they are forgeable.
