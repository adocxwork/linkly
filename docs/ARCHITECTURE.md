# Linkly backend interview guide

Linkly is one Spring Boot application with a React frontend. PostgreSQL stores accounts, links, inbox messages and analytics. Redis Streams carry click events, and a scheduled worker in the same application processes them. Flyway versions the schema.

## Follow the code

| Flow | Read in this order |
|---|---|
| Sign in | `AuthController` → `AuthService` → Spring Security’s authentication manager → `CustomUserDetailsService` → `JwtUtil` |
| Access a private link | `SecurityConfig` → `JwtAuthenticationFilter` → `LinkController` → `LinkService` → `LinkRepository` |
| Open a short link | `RateLimitingFilter` → `RedirectController` → `LinkService.resolveRedirect` → `ClickEventPublisher` |
| Record a click | `ClickStreamWorker` → `ClickProcessor` → optional `GeoIpService` → `ClickBatchWriter` |
| Leave a message | `PublicController` → `MessageService` → `MessageRepository` |

## The redirect and analytics path

1. Bucket4j applies a per-IP limit with Caffeine holding bounded, expiring buckets. On Render, `ClientIpResolver` uses the configured number of trusted hops counted from the right of `X-Forwarded-For`. Local runs default to the socket address.
2. An indexed Postgres query joins the link and its owner. The link must be active and its owner must be unsuspended. Public profile reads also use PostgreSQL. The prior Redis cache could preserve suspended profiles or links after invalidation failed. The trade-off is a database round trip on every redirect.
3. The application ignores recognizable crawlers and browser previews. A valid GET hands off an event with a UUID to a bounded executor and returns `302` with `Cache-Control: no-store`. HEAD resolves the URL without recording a click.
4. The executor sends `XADD` to Redis. If publishing fails or the executor is saturated, it places the same event in the bounded local buffer. When both paths are full, counters expose dropped events. Both in-memory queues lose pending work on a process restart.
5. Every two seconds the worker checks local work. It reads Redis after a publish signal and on a five-minute sweep. The fixed consumer name lets a replacement process on the same single-instance deployment reread pending entries. The consumer checks its pending list before fresh records.
6. PostgreSQL inserts by event UUID with `ON CONFLICT DO NOTHING`. The same statement adds only newly inserted rows to each link’s counter. After commit the worker acknowledges Redis records. A timeout after commit can produce a replay without increasing the counter again. The Redis stream is length-limited, so a long outage can still result in trimmed events.

Valid events remain pending on database failures and retry with a delay. Malformed entries are acknowledged and counted, with no separate dead-letter stream. A persistent processing error can stall the consumer and requires operator investigation. The fallback queue also has limited capacity.

## Authentication and data ownership

Login accepts an email or username case-insensitively. Passwords are BCrypt hashes, with a 72-byte UTF-8 limit. The JWT identifies an immutable user UUID and carries the account’s `token_version`. Spring checks the current account on protected requests. Password changes, suspension and global logout advance the version. UUIDs keep a token for a deleted account from identifying a later account that takes its username.

The database has case-insensitive unique indexes, which protect concurrent registration even when both requests pass the application’s availability checks. Account and link ownership checks use service/repository queries, with administrative routes requiring `ROLE_ADMIN`. Update transactions lock the user row for account state changes and concurrent link creation/reordering. Link editing leaves the analytics counter to the writer.

The browser stores bearer tokens in local storage. Script execution on the frontend origin could access them. The current design uses database-backed revocation for the demo. A production session design would need its own threat model, cookie/CSRF or token-refresh decisions, and stronger recovery and abuse handling.

## Database and privacy

- `users` owns `links` and `messages`, and `links` owns `click_analytics`. PostgreSQL foreign keys cascade deletions. Public profiles return only active public link fields.
- The inbox sorts by `(created_at, id)` using a composite index, and older pages use that pair as a cursor. Analytics aggregates run in SQL and work grows as historical events accumulate.
- A keyed IP hash supports approximate unique counts. Shared networks can collapse several people into one hash and dynamic IP addresses can split a visitor across hashes. Upstash can hold raw IP data in stream entries. Optional GeoJS enrichment receives raw IP addresses and keeps a local lookup cache. Treat the stream, logs, hashes and provider access as part of the privacy design.
- Flyway V1–V4 establish the baseline, migrate analytics and enable RLS. V5 adds case-insensitive uniqueness, V6 fixes pre-Flyway nullable flags and V7 indexes inbox pagination. PostgreSQL session advisory locking lets V4 update Flyway’s history table while preserving the existing migration checksum.

## Explain these trade-offs in an interview

| Question | What to explain |
|---|---|
| Why an event UUID? | Redis delivery and acknowledgements can be retried. A primary key plus a transactional counter update makes the write safe to repeat. |
| What happens if Redis is down? | Redirects still resolve from PostgreSQL. Analytics can use memory briefly and is eventually visible. A restart or full queue can lose events. |
| Why keep a counter on `links`? | The dashboard can read counts without scanning all click rows. The event insert and increment must commit together. |
| Why skip the redirect cache? | Disabling a link and suspending an account affect what can be served. Direct indexed reads honor committed changes without requiring invalidation to succeed. Measure latency before revisiting the design. |
| Why use both readiness and liveness? | Liveness represents process health. Login checks readiness including PostgreSQL, so visitors can see when the app can accept credentials. |
| What changes beyond a single instance? | Use unique consumer identities with pending-entry recovery, decide how to enforce distributed limits and measure throughput before introducing more components. |

For practice, trace one click from the browser to the counter in a debugger. Then run the replay integration test, explain why the second write returns zero and describe when the system can still lose an event.
