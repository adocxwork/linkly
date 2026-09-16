# 🔗 Linkly

![Linkly Banner](https://img.shields.io/badge/Linkly-High%20Performance%20URL%20Shortener-0071e3?style=for-the-badge)

Linkly is a production-grade URL shortener and "Link-in-Bio" platform engineered for high performance, security, and scalability. Designed to handle thousands of requests seamlessly, it provides users with a centralized dashboard to manage their digital presence, track link clicks, and share their curated profiles.

## ✨ Features

- **Link-in-Bio Profiles (`/u/{username}`)**: Clean, minimalist public profiles to showcase active links.
- **Custom Aliases**: Users can define custom short-link aliases (e.g., `/r/my-portfolio`).
- **Drag & Drop Reordering**: Intuitive frontend interface to reorder links in real-time.
- **Analytics & Tracking**: Built-in click tracking and dashboard statistics.
- **Role-Based Access Control (RBAC)**: Secure Admin control panel to suspend or delete abusive users.
- **Public Messaging & UPI**: Optional modules for profile visitors to leave messages or support the creator.

## 🛠️ Tech Stack

**Backend (The Core)**
* **Java 21 & Spring Boot 3.x**: Robust, enterprise-level backend architecture.
* **PostgreSQL**: Relational database for persistent, ACID-compliant data storage.
* **Upstash Redis**: Serverless, in-memory data store for sub-millisecond profile caching.
* **Spring Security & JWT**: Stateless authentication utilizing `HttpOnly` cookies to mitigate XSS and CSRF attacks.

**Frontend (The Interface)**
* **React 18 & Vite**: Lightning-fast modern frontend tooling.
* **Framer Motion**: Fluid, native-feeling page transitions and micro-interactions.
* **Zod & React-Hook-Form**: Type-safe, rigorous client-side form validation.

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

3. **Run the Application:**
   Linkly includes a custom bash script to concurrently boot both the Spring Boot server and the Vite development server.
   ```bash
   chmod +x run.sh
   ./run.sh
   ```

4. **Access the Application:**
   Open `http://localhost:5173` in your browser.

## 🛡️ Security Highlights

Linkly was built with a "security-first" mindset:
* **Cookie-Based JWTs:** Tokens are never exposed to `localStorage`. They are transported via secure `HttpOnly` cookies.
* **CORS & Proxy Architecture:** In production, Vercel edge-routes `/api` traffic directly to the Render backend, completely bypassing strict Safari/Brave third-party cookie blocking.
* **Database Constraints:** Complete JPA entity validation (`@Column(unique=true)`) prevents race conditions during user registration or alias creation.

---
*Developed as a showcase of modern full-stack engineering, emphasizing robust backend architecture and seamless user experiences.*
