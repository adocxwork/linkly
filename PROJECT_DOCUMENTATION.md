# Linkly - Comprehensive Project Documentation

This document serves as the detailed technical documentation for Linkly, outlining the complete architecture, technological choices, and step-by-step logic detailing how this full-stack application operates.

## 1. Project Overview & Objective
Linkly is a robust, full-stack web application designed for users to centralize their online identity into a single link-in-bio page. Unlike basic static link aggregators, Linkly is a dynamic platform that integrates internal link shortening, detailed click analytics, seamless UPI-based payments, and a bespoke public messaging system, wrapped in an elegant Apple-inspired UI.

## 2. Complete Technology Stack

### 2.1 Backend Architecture
The backend serves as a highly scalable RESTful API built on the Java ecosystem.
*   **Language**: Java 21 LTS
*   **Framework**: Spring Boot 3
*   **Data Access**: Spring Data JPA / Hibernate
*   **Database**: PostgreSQL 16
*   **Security Framework**: Spring Security 6
*   **Authentication**: JWT (JSON Web Tokens)
*   **Build Tool**: Maven

### 2.2 Frontend Architecture
The frontend is a declarative, reactive Single Page Application (SPA) prioritizing fluid animations and visual excellence.
*   **Framework**: React 18
*   **Build Tool**: Vite (Lightning fast HMR and compilation)
*   **Styling**: Pure CSS with CSS Variables to maintain complete control over Glassmorphism and responsive design. (Tailwind was deliberately excluded to ensure bespoke Apple-esque aesthetic fidelity).
*   **Routing**: React Router DOM (v6)
*   **Animation**: Framer Motion (Orchestrates layout transitions, component entry animations, and hover micro-interactions)
*   **Icons**: Lucide React
*   **HTTP Client**: Axios (configured with interceptors for JWT injection)

---

## 3. Detailed Architecture & Design Patterns

The application strictly follows an **N-Tier Architecture**:

### 3.1 Presentation Layer (Controllers)
Located in `com.gupta.linkly.controller`, this layer strictly manages HTTP routing, input validation (via `@Valid`), and maps JSON to internal DTOs (Data Transfer Objects).
-   `AuthController`: Manages `/register` and `/login`.
-   `LinkController`: Manages CRUD and custom reordering for links.
-   `PublicController`: Unauthenticated endpoints to retrieve `PublicProfileResponse` and post public messages.
-   `MessageController`: Serves the user's secure Inbox endpoints.
-   `RedirectController`: Captures short URL hashes (e.g., `/r/xyz123`) and processes 302 redirects while asynchronously recording analytics.

### 3.2 Service Layer (Business Logic)
Located in `com.gupta.linkly.service`, this is where the core logic lives. 
-   **Security**: `AuthService` handles BCrypt hashing and orchestrates JWT creation.
-   **Link Shortening Strategy**: `UrlShortenerService` generates unique Base62 hashes using the standard string `abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789`. Random 6-character strings are generated and checked against the database until a unique one is found.
-   **Analytics Strategy**: When a redirect happens, the system increments a `click_count` on the link entity.

### 3.3 Data Access Layer (Repositories)
Located in `com.gupta.linkly.repository`, utilizing Spring Data JPA. Interfaces extend `JpaRepository` providing out-of-the-box pagination and boilerplate CRUD operations. Methods like `findByUserOrderBySortOrderAscCreatedAtDesc` allow for dynamic sorting natively translating into SQL queries.

### 3.4 Exception Handling Strategy
A robust, centralized error handler lives in `GlobalExceptionHandler.java` (using `@ControllerAdvice`). It catches specific errors like `ResourceNotFoundException`, `DuplicateResourceException`, and Spring's `MethodArgumentNotValidException`, consistently mapping them to a standardized `ErrorResponse` structure.

---

## 4. How Critical Features Work

### 4.1 Link Shortening & Redirection Lifecycle
1.  **Creation**: A user submits a long URL (e.g., `https://github.com/adityagupta`).
2.  **Hashing**: The `UrlShortenerService` creates a 6-character random alphanumeric string (e.g., `aB3x9Q`).
3.  **Persistence**: The Link is saved in Postgres.
4.  **Redirection**: A user visits `http://localhost:8080/r/aB3x9Q`.
5.  **Analytics**: The `RedirectController` intercepts this GET request, performs a DB lookup, increments the `clickCount` by 1, saves it, and responds with an HTTP 302 (Found) pointing to the original URL.

### 4.2 Security & Authentication Flow
1.  **Login**: User submits credentials. `AuthenticationManager` verifies the BCrypt hashed password in PostgreSQL.
2.  **JWT Issue**: If valid, `JwtUtil` issues a token signed with an HMAC-SHA256 secret.
3.  **Client Storage**: React stores this token in `localStorage`.
4.  **Interceptor**: Axios intercepts all subsequent outbound requests, appending `Authorization: Bearer <token>` to headers.
5.  **Validation**: `JwtAuthenticationFilter` intercepts incoming requests, verifies the signature, and injects the `CustomUserDetails` into the Spring Security Context.

### 4.3 Custom Link Reordering
To give users granular control over their link display:
1.  **Database Addition**: A `sortOrder` integer column was added to the `links` table.
2.  **Frontend State**: In `Dashboard.jsx`, arrows allow users to swap array indices locally for immediate visual feedback.
3.  **Backend Sync**: On clicking an arrow, the frontend fires a `PUT /api/links/reorder` payload detailing the exact sequential ordering IDs. The backend iteratively updates the `sortOrder` for each ID and flushes it to the database.

### 4.4 The "Buy Me a Coffee" (UPI) Integration
Instead of building a complex payment gateway integration (Stripe/Razorpay), Linkly leverages India's robust UPI framework natively:
1.  User enters `UPI ID` during registration or via settings.
2.  The React app leverages the `react-qr-code` package.
3.  The QR code natively encodes the URI scheme: `upi://pay?pa={UPI_ID}&pn={USER_NAME}`.
4.  When scanned via Google Pay or PhonePe on a mobile device, it opens the app with pre-filled, secure payment details directed exclusively to the user.

### 4.5 Public Messaging System
1.  **Opt-In**: Messaging is disabled by default. A user must set `enablePublicMessaging` to true.
2.  **Submission**: Visitors invoke an unauthenticated `POST /api/public/u/{username}/messages` with `senderName` and `content`.
3.  **Persistence**: Saved to the `messages` table with a foreign key to the `User`.
4.  **Inbox**: The user visits `/inbox`, triggering a secure, authenticated request to fetch all messages linked to their ID.

---

## 5. Security Protocols Implemented
-   **BCrypt Hashing**: Passwords are never stored in plaintext.
-   **Admin Restrictions**: Accounts flagged with `ROLE_ADMIN` cannot be deleted or suspended. Only standard `ROLE_USER` accounts are subjected to Admin actions.
-   **Data Isolation**: Every `GET`, `PUT`, `DELETE` operation strictly verifies the JWT subject against the requested resource's owner, preventing Insecure Direct Object References (IDOR).
-   **Cascade Deletion**: When a user decides to delete their account, Postgres `ON DELETE CASCADE` rules ensure all linked messages and URLs are instantly scrubbed, ensuring data privacy compliance.
