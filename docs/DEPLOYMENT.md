# Deployment guide (Render + Supabase + Upstash + Vercel)

Follow the steps in order. The backend changes include a **database migration (V3)**, so step 0 matters.

---

## Step 0: Back up the production database (5 minutes)

V3 rewrites foreign keys and **clears the raw visitor IP addresses** already stored in
`click_analytics` (new clicks store a salted hash instead). Take a backup first.

1. Supabase dashboard → your project → **Connect** (top bar) → **Session pooler**. Copy the URI.
   It looks like `postgresql://postgres.<project-ref>:[YOUR-PASSWORD]@aws-0-<region>.pooler.supabase.com:5432/postgres`.
2. On your Mac (you have PostgreSQL 18 installed, and its `pg_dump` can dump older servers):
   ```bash
   pg_dump "postgresql://postgres.<project-ref>:<password>@aws-0-<region>.pooler.supabase.com:5432/postgres" -Fc -f linkly-backup.dump
   ```
3. Keep `linkly-backup.dump` somewhere safe and **not** in the repo. To restore later:
   `pg_restore --clean --if-exists -d "<same URI>" linkly-backup.dump`.

---

## Step 1: Supabase (PostgreSQL)

### 1a. Use the Session pooler connection
Render cannot reach Supabase's direct connection (IPv6 only). Use the **Session pooler** (port **5432**).
Do **not** use the Transaction pooler (port 6543): it breaks Flyway's migration lock and prepared statements.

From the Session pooler URI, build the values for Render:

| Render variable | Value |
|---|---|
| `DB_URL` | `jdbc:postgresql://aws-0-<region>.pooler.supabase.com:5432/postgres?sslmode=require` |
| `DB_USERNAME` | `postgres.<project-ref>` (the part before `:` in the URI) |
| `DB_PASSWORD` | your database password (Project Settings → Database → reset it if you don't know it) |

### 1b. Turn on Row Level Security (closes Supabase's auto-generated public API)
Supabase exposes every table in the `public` schema through its REST API. Nothing in Linkly uses that
API: the backend connects directly as the table owner, and owners bypass RLS. So enabling RLS with no
policies blocks that side door without affecting the app.

**After** the first successful deploy (so V3 has run), open the SQL Editor and run:
```sql
alter table public.users            enable row level security;
alter table public.links            enable row level security;
alter table public.messages         enable row level security;
alter table public.click_analytics  enable row level security;
alter table public.system_settings  enable row level security;
alter table public.flyway_schema_history enable row level security;
```
Then open the app and check that login and the dashboard still work. Supabase's **Security Advisor**
should stop warning about RLS.

### 1c. Free-tier pausing
Free Supabase projects pause after about a week without activity. The backend's self-ping (or an
external pinger, see step 7) touches the database every 10 minutes and prevents this.

---

## Step 2: Upstash (Redis)

1. Upstash console → your database → **Usage**. Compare monthly commands with your plan's limit.
   The old code polled Redis every second (about 86k commands a day even with zero traffic), which
   likely caused the "limits" errors. The new code uses about 600 a day when idle.
2. **Details** → copy the **TLS** endpoint as a URL:
   `rediss://default:<password>@<name>.upstash.io:6379` (note `rediss` with two s's = TLS).
   This is `REDIS_URL` on Render.
3. **Eviction**: keep it **disabled** (the default). With eviction on, Upstash may silently delete
   unprocessed click events when memory is full.
4. Optional clean-up in **Data Browser**: delete the old keys `publicProfiles::*` and `redirect:*`
   (they would expire on their own within 24 h anyway). Leave `link-clicks-stream`: any
   unprocessed events in it will still be picked up.

> If Upstash ever goes down or runs out of quota, the app keeps working: open **Admin → Use Redis**
> and turn it off, or just let the circuit breaker handle it.

---

## Step 3: Render (backend)

Dashboard → your web service (`linkly-amwf`) →

### 3a. Environment variables (Environment tab)
Set or update:

| Key | Value | Notes |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | from step 1a | |
| `REDIS_URL` | from step 2 | leave empty to run without Redis |
| `JWT_SECRET` | keep the existing value if it is 32+ characters | changing it logs everyone out; generate a new one with `openssl rand -hex 32` |
| `FRONTEND_URL` | `https://linkly-plum.vercel.app` | no trailing slash; used for CORS |
| `ADMIN_PASSWORD` | a strong password, 8+ characters | only used if no admin exists yet |
| `TRUSTED_PROXY_COUNT` | `1` | verify in step 6 |
| `IP_HASH_SALT` | optional: `openssl rand -hex 32` | defaults to `JWT_SECRET` |

**Delete** if present (no longer used): `VITE_BACKEND_URL` and any `SPRING_DATA_REDIS_*` or
`SPRING_JPA_HIBERNATE_DDL_AUTO` overrides. You do **not** need to set `PORT` or `RENDER_EXTERNAL_URL`:
Render provides both, and the self-ping uses `RENDER_EXTERNAL_URL` automatically.

### 3b. Health check (Settings tab → Health Checks)
Set **Health Check Path** to:
```
/actuator/health/liveness
```
Liveness only says "the JVM is running", so a short Supabase or Upstash blip will not make Render
restart the service in a loop.

### 3c. Deploy
Push to GitHub (step 5). Render rebuilds from the `Dockerfile`. In **Logs**, a healthy first boot shows:
```
Successfully baselined schema with version: 1
Migrating schema "public" to version "2 - alter click count to bigint"
Migrating schema "public" to version "3 - reliability and security"
Successfully applied 2 migrations ...
Started LinklyApplication in ... seconds
```
(On later deploys Flyway only says the schema is up to date.)

If the deploy fails, the log shows why. The most common causes are a wrong `DB_URL` format (it must
start with `jdbc:postgresql://`) or a `JWT_SECRET` shorter than 32 characters (the app says so explicitly).

---

## Step 4: Vercel (frontend)

1. Project → Settings → **Environment Variables**: `VITE_BACKEND_URL` = `https://linkly-amwf.onrender.com`
   (also set in `frontend/.env.production`; the dashboard value wins if both exist).
2. Leave `VITE_SHORT_LINK_BASE_URL` **unset**. Short links then point straight at Render, so the
   backend sees the visitor's real IP. If you route them through Vercel, every click passes through an
   extra proxy and you would need to re-calibrate `TRUSTED_PROXY_COUNT` for `/r/` traffic.
3. Redeploy (pushing to GitHub triggers it automatically if the project is connected).

---

## Step 5: GitHub

```bash
git push origin main
```
Open the repo's **Actions** tab. The **CI** workflow runs backend tests (including the PostgreSQL
integration test via Testcontainers) and the frontend lint and build. Add the badge to the README if you like:
`![CI](https://github.com/adocxwork/linkly/actions/workflows/ci.yml/badge.svg)`

---

## Step 6: Verify production (10 minutes)

1. Log in as `admin` → **Admin Control Panel**.
2. **System health** should show PostgreSQL UP and Redis UP (with latency). The click pipeline
   should be in "Redis Stream" mode.
3. Open **Request IP debug**. `resolvedClientIp` must equal your public IP (search "what is my ip").
   - Shows your IP: `TRUSTED_PROXY_COUNT=1` is correct.
   - Shows a Render or cloud address: try `2`, redeploy and check again.
   - `xForwardedFor` empty: set `TRUSTED_PROXY_COUNT=0`.
4. Create a link, open its short URL in a private window, wait about 5 s and open **Analytics**. The
   click should appear with your country.
5. Toggle **Use Redis** off and click the short link again. It still redirects, and the pipeline
   shows "In-memory fallback queue". Toggle it back on.
6. Turn **Backend self-ping** on if you want the service to stay awake (see step 7).

---

## Step 7 (optional): A more robust keep-awake

The self-ping runs inside the app, so it cannot wake a service that is already asleep (for example
after a redeploy with no traffic). An external pinger is more reliable:

1. Create a free account at cron-job.org (or use UptimeRobot).
2. Add a job: URL `https://linkly-amwf.onrender.com/actuator/health`, every 10 minutes.

`/actuator/health` (not `/liveness`) also runs a database check, which keeps Supabase from pausing.
Render's free tier includes 750 instance-hours a month, enough for one always-on service.

## Step 8 (optional): Custom domain for short links

Render supports custom domains on the free tier (Settings → Custom Domains). For example, point
`go.yourname.dev` at the service and set `VITE_SHORT_LINK_BASE_URL=https://go.yourname.dev` on
Vercel. Short links become `go.yourname.dev/r/abc1234`.
