# 🔗 Linkly: Enterprise-Grade Creator Platform

![Linkly Banner](https://img.shields.io/badge/Linkly-High%20Throughput%20Creator%20Platform-0071e3?style=for-the-badge)

Linkly is a highly scalable, production-ready **Creator Identity Platform** designed to aggregate digital presences into a single, lightning-fast micro-landing page. Engineered specifically to handle unpredictable traffic surges from viral social media campaigns, Linkly combines robust link aggregation (a la Linktree) with powerful alias management and high-throughput analytics (a la Bitly).

Developed from the ground up as a **distributed, backend-intensive application**, this project serves as a showcase of modern, enterprise-level system design, rigorous security standards, and high-performance caching strategies.

## 🔥 Engineering Highlights (Why Linkly Stands Out)

- **Sub-Millisecond Read Latency:** Leverages a serverless **Upstash Redis** caching layer. Creator profile endpoints heavily utilize `@Cacheable` and event-driven `@CacheEvict` invalidation, ensuring that viral traffic spikes hit RAM instead of bottlenecking the PostgreSQL database.
- **Enterprise Security Architecture:** Completely stateless authentication powered by JSON Web Tokens (JWT). Tokens are strictly transported via `HttpOnly`, `SameSite=Lax` cookies, rendering the application virtually immune to Cross-Site Scripting (XSS) and Cross-Site Request Forgery (CSRF).
- **Reverse Proxy Network Routing:** The architecture bridges a Vercel-hosted React edge network with a Render-hosted Spring Boot cluster. Custom Vercel Rewrite rules act as a reverse proxy, bypassing draconian browser third-party cookie restrictions without compromising CORS integrity.
- **Optimistic UI & Fluid Interactions:** Features a custom drag-and-drop link reordering engine (`@hello-pangea/dnd`). State is mutated optimistically on the client to provide instant feedback, while silently synchronizing array sort orders with the backend asynchronously.

## ✨ Core Features

- **Centralized Creator Hub (`/u/{username}`)**: Clean, glassmorphism-inspired public profiles designed for maximum conversion.
- **Granular Traffic Analytics**: Built-in click tracking algorithms to monitor audience engagement in real-time.
- **Custom Vanity Aliases**: Advanced routing allows creators to claim hyper-specific alias endpoints.
- **Direct Support & Messaging**: Integrated modules for public messaging and seamless peer-to-peer monetization (UPI integrations).
- **Role-Based Access Control (RBAC)**: Dedicated administrative command center for active platform moderation and user suspension.

## 🛠️ The Tech Stack

* **Backend:** Java 21, Spring Boot 3.x, Spring Security, Spring Data JPA
* **Database:** PostgreSQL (Primary ACID Storage)
* **Caching:** Upstash Redis (High-Throughput In-Memory Data Store)
* **Frontend:** React 18, Vite, Framer Motion, Zod, React-Hook-Form
* **Infrastructure:** Vercel (Edge Routing), Render (App Cluster)

## 🚀 Quick Start (Local Development)

### Prerequisites
- Java 21+
- Node.js 18+
- PostgreSQL (running locally on port 5432)
- Redis (optional for local, required for prod)

### Setup
1. **Clone the repository:**
   ```bash
   git clone https://github.com/yourusername/linkly.git
   cd linkly
   ```

2. **Configure the Environment:**
   Update `src/main/resources/application.properties` with your PostgreSQL credentials.

3. **Boot the Cluster:**
   Linkly includes a custom orchestrator script to concurrently boot both the Spring Boot server and the Vite edge server.
   ```bash
   chmod +x run.sh
   ./run.sh
   ```

4. **Access the Application:**
   Open `http://localhost:5173` in your browser.

---
*Architected and engineered as a comprehensive demonstration of modern, production-grade backend scaling, security, and full-stack integration.*
