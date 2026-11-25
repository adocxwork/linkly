# Deploy and verify Linkly

The current patch should go to Render before Vercel. It includes database migrations and a login readiness check. Existing sessions will ask users to sign in again after switching JWTs to account UUIDs.

## Supabase and Flyway

- Back up production before applying migrations. Use the **Session pooler**, port **5432**, from the Supabase Connect dialog. It provides an IPv4 endpoint for long-lived JDBC sessions and supports Flyway’s session advisory locks. [Supabase connection options](https://supabase.com/docs/guides/database/connecting-to-postgres)
- Use the exact pooler host from the dashboard in `DB_URL`, for example `jdbc:postgresql://<pooler-host>:5432/postgres?sslmode=require`. Use the pooler username, typically `postgres.<project-ref>`, and store its password only in Render’s secret settings. `sslmode=require` encrypts the connection. Use `verify-full` after configuring and testing the appropriate trust chain.
- Before V5, check for preexisting case-only account duplicates. Both queries should return zero rows. Resolve any conflicts with the affected owners before deploying.

```sql
SELECT lower(username) AS normalized, count(*)
FROM users GROUP BY lower(username) HAVING count(*) > 1;

SELECT lower(email) AS normalized, count(*)
FROM users GROUP BY lower(email) HAVING count(*) > 1;
```

- V4 enables row-level security on the app tables and Flyway history. Linkly uses JDBC with the database owner, so this blocks Supabase Data API roles without policies while retaining backend access. A separate migration role and least-privilege runtime role are work for a production deployment.
- V4 needs a session-level advisory lock because it alters Flyway's history table. The app sets this in `application.properties`. Confirm the Render log shows the remaining migrations applied before Spring starts. Leave Hibernate schema changes disabled.

## Render and Upstash

| Render setting | Value |
|---|---|
| Runtime | Docker, repository root |
| Health check | `/actuator/health/liveness` |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Supabase Session pooler credentials |
| `JWT_SECRET` | A strong secret of at least 32 UTF-8 bytes |
| `FRONTEND_URL` | `https://linkly-plum.vercel.app` |
| `TRUSTED_PROXY_COUNT` | `1` for the direct Render route, subject to verification in the admin IP panel |
| `REDIS_URL` | Upstash TLS URL (`rediss://`), or empty for local fallback |
| `IP_HASH_SALT` | Keep the effective value stable to preserve unique counts across deploys |
| `ADMIN_PASSWORD` | Strong, at most 72 UTF-8 bytes. Only seeds an admin when none exists |
| `GEOIP_ENABLED` | Defaults to `false`. Set `true` if you want country enrichment and account for GeoJS receiving raw client IPs |

Keep Upstash eviction disabled and check command quotas in its dashboard. The stream cap can trim old entries under backlog. Legacy Redis redirect and profile caches expire on their own. Preserve the click stream when deploying.

Render’s liveness path avoids database-driven restart loops. Login uses `/actuator/health/readiness`, which includes PostgreSQL. Remove any Render override for `SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT` and use `DB_CONNECTION_TIMEOUT_MS` if needed. The app defaults to 30 seconds. [Render health checks](https://render.com/docs/health-checks)

## Vercel

Set the project root to `frontend` and `VITE_BACKEND_URL` to `https://linkly-amwf.onrender.com`. The browser will call Render directly for API requests and readiness, using the allowed Vercel origin on the backend. If using another frontend domain, update `FRONTEND_URL` or `CORS_ALLOWED_ORIGINS`. Leaving `VITE_SHORT_LINK_BASE_URL` unset makes generated links point straight to Render and keeps the expected proxy topology.

## Smoke test after deploying

1. Open the liveness and readiness URLs. Expect `200` and JSON status `UP` from each with PostgreSQL available. Check the browser-facing readiness request from the Vercel login origin.
2. Leave the free service idle, then open the login page. Confirm the notice appears promptly and Log In enables once readiness returns `UP`. Cold starts take roughly a minute on Render. Retry is available if the readiness window expires. [Render free services](https://render.com/docs/free)
3. Sign in with a test account. Create a link, open it with a browser, verify the redirect and check the click count. Disable the link and verify the next visit shows the link error page.
4. Verify the admin dashboard reports Redis and PostgreSQL correctly, the resolved IP matches the client and the pending count drains. Toggle Redis for a test of the fallback, then restore the desired setting.
5. Check the Supabase dashboard before sharing the demo after a long idle period. Free projects can pause independently of Render. [Supabase project pausing](https://supabase.com/docs/guides/platform/free-project-pausing)
