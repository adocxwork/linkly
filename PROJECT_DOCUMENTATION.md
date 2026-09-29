# 📚 Linkly Technical Documentation

## 1. System Architecture

Linkly is a decoupled Full-Stack web application featuring a React (Vite) frontend and a Spring Boot backend. 

### Core Components
*   **Frontend (React/Vite):** Hosted on Vercel. Communicates with the backend REST API via Axios. Implements a global request interceptor to attach JWT Bearer tokens for authenticated requests.
*   **Backend (Spring Boot 3):** Hosted on Render. Serves as the core API, handling authentication, business logic, analytics processing, and database interactions.
*   **Database (PostgreSQL):** Relational storage for Users, Links, Analytics, and System Settings. Managed using Flyway migrations (`V1__init_schema.sql`).
*   **Cache & Message Broker (Redis):** Provided by Upstash. Used for `@Cacheable` method caching (e.g., user profiles), manual key-value caching (link redirect resolution), and Redis Streams (asynchronous analytics processing).

---

## 2. Request Data Flow & Integrations

### The Redirection Flow (`GET /r/{shortUrl}`)
This is the most critical path in the system, designed to handle high concurrency.
1.  **Request Arrival:** A user clicks a short link (`/r/abc`). The request hits the `RedirectController`.
2.  **IP & User-Agent Parsing:** The backend extracts the client's actual IP address and User-Agent.
3.  **Cache Lookup (`LinkService`):** The system queries Redis for the original URL using the key `redirect:{shortUrl}`.
4.  **Database Fallback:** If the cache is missed (or Redis is unavailable due to connection failure), the system queries PostgreSQL, then updates the Redis cache.
5.  **Analytics Dispatch:** A `ClickEvent` payload is published to the `link-clicks-stream` Redis Stream. If Redis is down, it synchronously falls back to the `AnalyticsService`.
6.  **Redirection:** The server returns an HTTP 302 redirect to the original URL.

### Background Analytics Consumer (`AnalyticsStreamConsumer`)
1.  **Polling:** A `StreamListener` continuously polls the `link-clicks-stream` consumer group.
2.  **Geolocation:** For each event, `AnalyticsService` queries `get.geojs.io` to resolve the IP address into a Country and City. A `ConcurrentHashMap` acts as a local LRU-style cache to prevent rate-limiting from the Geo API.
3.  **Persistence:** The rich analytics event is persisted to PostgreSQL, and the Stream message is manually acknowledged (`ACK`).
4.  **Error Handling:** If Redis throws a "max requests limit exceeded" error, an error handler pauses polling for 5 seconds to prevent CPU exhaustion.

---

## 3. Security Implementation

### Stateless Authentication (JWT)
*   **Token Generation:** `AuthService` generates a JSON Web Token signed with an HMAC-SHA256 secret (`JWT_SECRET`).
*   **Transport:** The token is returned in the JSON payload on login/register and stored in browser `localStorage`.
*   **Validation:** `JwtAuthenticationFilter` intercepts incoming requests, reads the `Authorization: Bearer <token>` header, validates the signature/expiration, and populates the Spring `SecurityContextHolder`.

### Rate Limiting
Implemented using **Bucket4j** in `RateLimitingFilter`. It utilizes `ConcurrentHashMap` for fast, in-memory rate limiting based on the client IP address.
*   `/api/auth/**`: 10 requests per minute.
*   `/api/public/**`: 30 requests per minute.
*   `/r/**`: 300 requests per minute.

---

## 4. Codebase Structure

### Backend (`src/main/java/com/gupta/linkly/`)
*   `config/`: Infrastructure beans (Redis, WebMvc, Swagger).
*   `controller/`: REST API endpoints.
*   `dto/`: Data Transfer Objects for strictly typed request/response bodies.
*   `entity/`: JPA Entities mapping to PostgreSQL tables.
*   `exception/`: Global `@ControllerAdvice` exception handlers.
*   `repository/`: Spring Data JPA interfaces.
*   `security/`: JWT filters, Custom UserDetails, Bucket4j rate limiting.
*   `service/`: Core business logic and background tasks.

### Frontend (`frontend/src/`)
*   `api.js`: Axios instance with JWT interceptors and 401/403 logout logic.
*   `components/`: Reusable UI components (Navbar, AnalyticsModal).
*   `pages/`: View components mapped to React Router routes (Dashboard, Login, Admin).

---

## 5. Deployment & Configuration

### Environment Variables
| Variable | Description | Default / Example |
| :--- | :--- | :--- |
| `DB_URL` | JDBC Postgres connection string | `jdbc:postgresql://localhost:5432/linkly` |
| `DB_USERNAME` | Database user | `adityagupta` |
| `DB_PASSWORD` | Database password | *(Empty)* |
| `REDIS_URL` | Redis connection URI | *(Empty)* |
| `JWT_SECRET` | 256-bit Hex String for JWT signing | *(Required)* |
| `FRONTEND_URL` | Used for CORS configuration | `https://linkly-plum.vercel.app` |
| `ADMIN_PASSWORD` | Default password for seeded admin | Auto-generated UUID |

### Infrastructure
*   **Flyway Migrations:** Enabled via `spring.flyway.enabled=true`. The schema is controlled by `V1__init_schema.sql`.
*   **Keep-Alive Daemon:** `KeepAliveService` runs every 14 minutes. It queries the `SystemSettings` table. If enabled (controllable via the Admin UI), it pings the `/api/system/health` endpoint to prevent the Render container from spinning down.
