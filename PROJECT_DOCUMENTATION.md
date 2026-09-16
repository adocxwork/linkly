# 📘 Linkly: Technical Project Documentation

This document provides a deep dive into the architectural decisions, data flow, and inner workings of the Linkly application. It is intended for developers, engineers, and technical recruiters who wish to understand the "how" and "why" behind the codebase.

---

## 1. System Architecture

Linkly follows a standard monolithic client-server architecture, cleanly decoupled via a RESTful JSON API.

*   **Client Layer:** React SPAs built with Vite.
*   **API Gateway / Proxy:** Vercel Rewrites (`vercel.json`) acting as a reverse proxy.
*   **Application Layer:** Spring Boot 3 handling business logic, caching, and security.
*   **Persistence Layer:** PostgreSQL (Primary Data) + Upstash Redis (Cache).

### The Reverse Proxy Solution (Vercel -> Render)
Modern browsers (like Safari and Chrome Incognito) aggressively block third-party cookies. Because the frontend (Vercel) and backend (Render) operate on different domains, the `HttpOnly` JWT cookie would normally be rejected.
**Solution:** The frontend uses `vercel.json` to proxy all `/api/*` and `/r/*` traffic to the backend. To the browser, the frontend and backend appear to exist on the exact same domain, ensuring seamless cookie transmission without complex CORS preflight issues.

---

## 2. Security & Authentication Flow

Authentication is strictly stateless, utilizing JSON Web Tokens (JWT).

### Token Lifecycle
1.  **Login:** The user submits credentials. The `AuthService` hashes the password utilizing `BCrypt` and validates it against the DB.
2.  **Generation:** `JwtUtil` generates a cryptographically signed token.
3.  **Delivery:** The token is not returned in the JSON body. Instead, it is attached to an `HttpOnly`, `SameSite=Lax` cookie named `linkly_token`. This makes the token completely invisible to client-side JavaScript (thwarting Cross-Site Scripting - XSS).
4.  **Verification:** Every incoming request passes through the `JwtAuthenticationFilter`. The filter extracts the cookie, validates the signature, and ensures the user account `isSuspended == false`.

---

## 3. The Data Layer (PostgreSQL & Spring Data JPA)

The database schema is heavily relational and normalized.

### Core Entities
*   **User:** Contains standard identity fields, along with platform settings (`enableUpiPayment`, `enablePublicMessaging`).
*   **Link:** Tied to a User via a `@ManyToOne` relationship. Tracks the `originalUrl`, `shortUrl`, `active` status, and `sortOrder`.
*   **Message:** Tied to a User. Represents public messages left on their profile.

### Short URL Generation Algorithm
If a user does not provide a custom alias, the `UrlShortenerService` generates a random 6-character alphanumeric string (`SecureRandom`). It recursively checks the database to ensure absolute uniqueness before saving, preventing collision edge cases.

---

## 4. High-Performance Caching (Redis)

To handle massive spikes in traffic (e.g., when a user links their profile on a viral social media post), the public profile endpoint (`/public/u/{username}`) is aggressively cached.

### Implementation Details
*   **Data Serialization:** Java Objects are serialized into JSON using `GenericJackson2JsonRedisSerializer` in the `RedisConfig`. This prevents class-cast exceptions and makes the Redis store readable.
*   **`@Cacheable`:** Read operations intercept the DB call and fetch directly from RAM.
*   **Event-Driven Eviction (`@CacheEvict`):** Cache invalidation is the hardest problem in computer science, handled elegantly here via AOP (Aspect-Oriented Programming). Whenever a user updates their settings, reorders their links, or adds/deletes a link, the `UserService` and `LinkService` trigger a targeted cache eviction for that specific `#username`. The next visitor will trigger a fresh DB query, rehydrating the cache.

---

## 5. Frontend Engineering

The frontend avoids heavy component frameworks (like Material UI) in favor of lightweight, custom CSS variables to maintain an Apple-inspired "Glassmorphism" aesthetic.

### Key Libraries
*   **Zod & React-Hook-Form:** Forms do not rely on standard React `useState` (which causes excessive re-renders). Inputs are registered to the hook, and Zod validates schemas before the payload ever hits the network.
*   **@hello-pangea/dnd:** Used for the drag-and-drop link reordering in the dashboard. When a user drags a link, the frontend optimistically updates the UI array, and asynchronously fires an `api.put('/links/reorder')` payload containing the newly sorted IDs.
*   **Axios Interceptors:** A global Axios interceptor catches any `401 Unauthorized` or `403 Forbidden` responses. If a user's session expires or they are suspended, the interceptor automatically purges local state and forcibly redirects them to the login screen.

---

## 6. Scalability & Future Roadmap

The current architecture is highly horizontally scalable.
*   The Spring Boot application is entirely stateless.
*   Multiple instances of the backend can be spun up behind a Load Balancer, all relying on the centralized Redis and Postgres clusters.

**Planned Features (Phase 4 & Beyond):**
1.  **IP Geolocation Analytics:** Utilizing `ipapi` to track geographic click heatmaps.
2.  **Kafka Event Streaming:** Offloading click-tracking (`clickCount++`) to an asynchronous Kafka topic to prevent write-locks on the primary database during viral traffic spikes.
