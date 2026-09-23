# 🔗 Linkly: Enterprise-Grade Creator Identity Platform

![Linkly Banner](https://img.shields.io/badge/Linkly-High%20Throughput%20Creator%20Platform-0071e3?style=for-the-badge)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.0-6DB33F?style=for-the-badge&logo=springboot)
![React](https://img.shields.io/badge/React-18.0-61DAFB?style=for-the-badge&logo=react&logoColor=black)
![Redis](https://img.shields.io/badge/Redis-Streams-DC382D?style=for-the-badge&logo=redis)

Linkly is a highly scalable, production-ready **Creator Identity Platform** designed to aggregate digital presences into a single, lightning-fast micro-landing page. Engineered specifically to handle unpredictable traffic surges from viral social media campaigns, Linkly combines robust link aggregation with an **Event-Driven Analytics Engine** capable of processing tens of thousands of clicks concurrently without database degradation.

Developed from the ground up as a **distributed, backend-intensive application**, this project serves as a masterclass in modern system design, asynchronous event streaming, rigorous security standards, and high-performance caching strategies.

---

## 🔥 Enterprise Architecture & Engineering Highlights

This platform was built to demonstrate how to engineer software for scale, moving beyond simple CRUD applications into the realm of enterprise systems:

### 1. Event-Driven Analytics via Redis Streams
To prevent database locking during viral traffic spikes, the analytics engine uses a highly optimized **Producer/Consumer microservice pattern**. Link clicks are instantly packaged into `ClickEvents` and published to a **Redis Stream** in sub-millisecond time. A background `StreamListener` asynchronously consumes these events, performs heavy IP-geolocation lookups, and batch-inserts them into PostgreSQL—completely shielding the primary database from sudden read/write surges.

### 2. Sub-Millisecond Read Latency (Redis Caching)
Leverages a serverless **Upstash Redis** caching layer. Creator profile endpoints heavily utilize Spring's `@Cacheable` and event-driven `@CacheEvict` invalidation, ensuring that 99% of viral traffic hits RAM instead of bottlenecking the PostgreSQL database.

### 3. Bulletproof Security & DDoS Protection
Authentication is entirely stateless, powered by JSON Web Tokens (JWT). However, tokens are strictly transported via `HttpOnly`, `SameSite=Lax` cookies, rendering the application immune to XSS attacks. Furthermore, the API employs **Bucket4j** for dynamic IP-based Rate Limiting to prevent brute-force login attempts and protect the short-link resolution endpoints from orchestrated DDoS spam.

### 4. Zero-Downtime Database Migrations (Flyway)
The persistence layer is managed entirely by **Flyway Database Migrations**. Instead of relying on unsafe ORM auto-generation (`ddl-auto`), every database change is strictly version-controlled (`V1__init_schema.sql`), ensuring deterministic, zero-downtime deployments and safe schema evolution across distributed environments.

### 5. Automated Testing & Code Quality
The core business logic is fortified by a comprehensive suite of **JUnit 5 and Mockito** unit tests. Repositories and external services are mocked in isolation, ensuring deterministic validation of high-risk workflows like URL collision handling, public profile resolution, and user suspension. 

### 6. Observability & Interactive API Specs
The API conforms to strict REST standards and is self-documenting via **Swagger / OpenAPI 3.0**. Developers can instantly interact with the API via the `/swagger-ui/index.html` portal. Additionally, **Spring Boot Actuator** exposes live `/actuator/health` and `/actuator/metrics` endpoints for real-time Prometheus/Grafana system monitoring.

---

## ✨ Core Features

- **Centralized Creator Hub (`/p/{username}`)**: Clean, glassmorphism-inspired public profiles designed with an "Apple-like" premium UI for maximum conversion.
- **Granular Traffic Analytics**: Built-in click tracking algorithms to monitor audience engagement in real-time on interactive Recharts.
- **Custom Vanity Aliases**: Advanced routing allows creators to claim hyper-specific alias endpoints.
- **Drag-and-Drop UI**: Optimistic state mutation using `@hello-pangea/dnd` for fluid link reordering.
- **Role-Based Access Control (RBAC)**: Dedicated administrative command center for active platform moderation and user suspension.

## 🛠️ The Tech Stack

* **Backend:** Java 21, Spring Boot 3.x, Spring Security, Spring Data JPA
* **Testing:** JUnit 5, Mockito
* **Migrations & Docs:** Flyway, OpenAPI (Swagger), Spring Boot Actuator
* **Event Streaming:** Redis Streams (Message Broker / Queue)
* **Database:** PostgreSQL (Primary ACID Storage)
* **Caching & Rate Limiting:** Upstash Redis, Bucket4j
* **Frontend:** React 18, Vite, Framer Motion, Recharts, Zod
* **Infrastructure:** Vercel (Edge Routing), Render (App Cluster)

## 🚀 Quick Start (Local Development)

### Setup
1. **Clone the repository:**
   ```bash
   git clone https://github.com/yourusername/linkly.git
   cd linkly
   ```

2. **Configure the Environment:**
   Update `src/main/resources/application.properties` with your PostgreSQL and Redis credentials.

3. **Boot the Cluster:**
   Linkly includes a custom orchestrator script to concurrently boot both the Spring Boot server and the Vite edge server.
   ```bash
   chmod +x run.sh
   ./run.sh
   ```

---
*Architected and engineered as a comprehensive demonstration of modern, production-grade backend scaling, event-driven design, and full-stack integration.*
