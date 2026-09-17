# Linkly: Complete Project Documentation

## 1. Project Overview
**Linkly** is an enterprise-grade Creator Identity Platform designed to handle high-throughput traffic scenarios. It is engineered not just as a functional product, but as a demonstration of advanced system architecture, robust security, and event-driven data streaming.

## 2. System Architecture

The application is deployed across a distributed micro-architecture:
- **Frontend Edge Network (Vercel):** Hosts the compiled React SPA. It acts as a Reverse Proxy, rewriting all `/api/*` and `/r/*` requests directly to the backend cluster. This circumvents modern browser restrictions on third-party cookies (Intelligent Tracking Prevention) since all traffic appears as first-party to the browser.
- **Backend Application Cluster (Render):** A Spring Boot Java application handling business logic, authentication, and database orchestration.
- **Primary Data Store (Supabase PostgreSQL):** Handles ACID-compliant, persistent storage of Users, Links, and Analytics data.
- **In-Memory Cache & Message Broker (Upstash Redis):** Serves a dual purpose. It acts as a lightning-fast read cache for public profiles, and as a distributed message queue (Redis Streams) for processing high-volume analytics events asynchronously.

## 3. Key Engineering Decisions & Phases

### Phase 1 & 2: Core Foundation & Security
- Implemented **JWT (JSON Web Tokens)** for stateless, scalable authentication.
- To prevent XSS, tokens are stored strictly in `HttpOnly` cookies rather than `localStorage`.
- Built a custom Drag-and-Drop link sorting algorithm utilizing `@hello-pangea/dnd` and optimistic UI updates.

### Phase 3: High-Performance Caching
- Public Creator profiles (`/u/{username}`) often face massive viral traffic spikes. Hitting the database for every page load would cause connection exhaustion.
- Implemented **Spring Data Redis**. Profile GET requests are aggressively cached (`@Cacheable`).
- Cache invalidation (`@CacheEvict`) is strictly tied to state-mutating actions (adding a link, updating a profile), ensuring users always see the latest data with sub-millisecond read times.

### Phase 4: Advanced Geographic & Device Analytics
- Ingests raw `X-Forwarded-For` and `User-Agent` headers.
- Pings a lightning-fast IP-Geolocation API to convert raw IPs into actionable Country/City metrics.
- Parses User-Agents to categorize clicks by Browser (Chrome, Safari, etc.) and Device Type (Mobile, Desktop).
- Data is visualized using responsive **Recharts** SVGs on the creator dashboard.

### Phase 5: Event-Driven Analytics (Redis Streams)
- **The Problem:** Writing to PostgreSQL is slow. If a link goes viral and receives 10,000 clicks per second, synchronously updating the `click_analytics` table would crash the database thread pool.
- **The Solution:** Implemented an Event-Driven Architecture using **Redis Streams**. 
- When a user clicks a link, the redirect controller instantly publishes a `ClickEvent` to a Redis Stream and redirects the user (taking < 1ms).
- A background `AnalyticsStreamConsumer` securely consumes the queue at a safe, controlled speed, processes the geolocation data, and performs the database writes asynchronously, effectively shielding the database from load spikes.

### Phase 6: Keep-Alive Infrastructure
- Render spins down free-tier servers after 15 minutes of inactivity, causing a 50-second "cold start" for the next visitor.
- Built a `KeepAliveService` with Spring's `@Scheduled` annotation to ping the server's own health endpoint, overriding the idle-timeout mechanism.
- Governed by a `SystemSettings` table flag, allowing the administrator to toggle the Keep-Alive engine dynamically without redeploying the application.

## 4. UI/UX Philosophy
The frontend utilizes a strict, "Apple-inspired" design language:
- **Typography:** Bold, tightly-kerned Sans-Serif headers (`font-weight: 700`, `letter-spacing: -0.04em`).
- **Glassmorphism:** Widespread use of `backdrop-filter: blur(20px)` over translucent backgrounds to create a deep, layered application feel.
- **Responsiveness:** CSS Grid and Flexbox with mobile-first media queries to ensure 100% feature parity across desktop and mobile devices.

## 5. Security Summary
- **No Information Disclosure:** Global Exception Handlers catch all unhandled errors and return generic 500 status messages to the client to prevent stack-trace leaking.
- **CORS Hardening:** Specifically configured to only accept credentials from trusted origins (the Vercel edge network and localhost).
- **Password Hashing:** Passwords are cryptographically hashed using `BCrypt` before ever touching the database.
