# Linkly - Comprehensive Technical & Architecture Documentation

This document serves as the absolute source of truth for **Linkly**, detailing the complete architecture, technological choices, database schema, deployment strategy, and step-by-step logic explaining exactly how this full-stack application operates from end to end.

---

## 1. Project Overview & Objective

**Linkly** is a robust, full-stack web application designed for users to centralize their online identity into a single link-in-bio page. Unlike basic static link aggregators, Linkly is a highly dynamic platform that integrates internal link shortening, detailed click analytics, drag-and-drop link sorting, seamless UPI-based payments, and a bespoke public messaging system, wrapped in an elegant Apple-inspired UI.

---

## 2. Complete Technology Stack & Deployment

### 2.1 Backend Architecture (Render)
The backend serves as a highly scalable RESTful API built on the Java ecosystem and deployed on Render as a Dockerized web service.
*   **Language**: Java 21 LTS
*   **Framework**: Spring Boot 3.2.4
*   **Data Access**: Spring Data JPA / Hibernate
*   **Security Framework**: Spring Security 6
*   **Authentication**: Stateless JWT (JSON Web Tokens)
*   **Build Tool**: Maven (via Dockerfile multi-stage build)
*   **Hosting Provider**: Render.com (Deployed via Custom `Dockerfile` for Java 21 support)

### 2.2 Frontend Architecture (Vercel)
The frontend is a declarative, reactive Single Page Application (SPA) prioritizing fluid animations and visual excellence.
*   **Framework**: React 18
*   **Build Tool**: Vite (Lightning fast HMR and compilation)
*   **Styling**: Pure Vanilla CSS with CSS Variables. (Tailwind was deliberately excluded to ensure bespoke Apple-esque glassmorphic aesthetic fidelity).
*   **Routing**: React Router DOM (v6). Handled via `vercel.json` SPA rewrite configurations.
*   **Animation**: Framer Motion (Orchestrates layout transitions, component entry animations, and hover micro-interactions)
*   **Drag & Drop**: `@hello-pangea/dnd` (For seamless link reordering)
*   **Icons**: Lucide React
*   **HTTP Client**: Axios (configured with interceptors for JWT injection)
*   **Hosting Provider**: Vercel

### 2.3 Database Infrastructure (Supabase)
*   **Database**: PostgreSQL 16
*   **Hosting Provider**: Supabase
*   **Connection Strategy**: IPv4 Session Pooling (`aws-0-region.pooler.supabase.com:5432`) to ensure compatibility with Render's IPv4-only network constraints.

---

## 3. Database Schema (Entities)

The database consists of three primary tables generated via Hibernate DDL.

### 3.1 Users Table (`users`)
- `id` (UUID, Primary Key)
- `name` (String)
- `username` (String, Unique, Indexed)
- `email` (String, Unique)
- `password` (String, BCrypt Hashed)
- `bio` (String, Optional)
- `role` (Enum: `ROLE_USER`, `ROLE_ADMIN`)
- `is_suspended` (Boolean) - Hard disables access and profile visibility if true.
- `upi_id` (String, Optional)
- `enable_upi_payment` (Boolean)
- `enable_public_messaging` (Boolean)
- `created_at` / `updated_at` (Timestamps)

### 3.2 Links Table (`links`)
- `id` (UUID, Primary Key)
- `title` (String)
- `original_url` (String)
- `short_url` (String, Unique, Base62 Hash)
- `click_count` (Integer, default 0)
- `active` (Boolean) - Toggles if the link is visible and redirectable.
- `sort_order` (Integer) - Stores the exact index for drag-and-drop custom positioning.
- `user_id` (Foreign Key -> `users.id` with ON DELETE CASCADE)

### 3.3 Messages Table (`messages`)
- `id` (UUID, Primary Key)
- `sender_name` (String, Default: "Anonymous")
- `content` (Text)
- `user_id` (Foreign Key -> `users.id` with ON DELETE CASCADE)
- `created_at` (Timestamp)

---

## 4. Detailed Architecture & Design Patterns

The backend strictly follows an **N-Tier Architecture**:

### 4.1 Presentation Layer (Controllers)
Located in `com.gupta.linkly.controller`, this layer strictly manages HTTP routing, input validation (via `@Valid`), and maps JSON payloads to internal DTOs.
-   `AuthController`: Manages `/register` and `/login`.
-   `LinkController`: Manages CRUD, custom drag-and-drop reordering (`/reorder`), and analytics fetching for the dashboard.
-   `PublicController`: Unauthenticated endpoints to retrieve `PublicProfileResponse` (by `username`) and post public messages.
-   `MessageController`: Serves the user's secure Inbox endpoints (fetching and deleting messages).
-   `AdminController`: Restricted to `ROLE_ADMIN`. Manages user suspension and deletion.
-   `RedirectController`: Captures short URL hashes (e.g., `/r/xyz123`), validates `isActive`, increments analytics, and triggers an HTTP 302 redirect.

### 4.2 Service Layer (Business Logic)
Located in `com.gupta.linkly.service`, this is where the core logic lives. 
-   **Security**: `AuthService` handles BCrypt hashing and orchestrates JWT creation.
-   **Link Shortening Strategy**: `UrlShortenerService` generates unique Base62 hashes using the standard string `abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789`. Random 6-character strings are generated and checked against the database until a globally unique hash is found.
-   **Analytics Strategy**: When a redirect happens, the system increments `clickCount` safely.
-   **Admin Control**: `AdminService` handles toggling `isSuspended`, instantly locking users out.

### 4.3 Data Access Layer (Repositories)
Located in `com.gupta.linkly.repository`, utilizing Spring Data JPA. Interfaces extend `JpaRepository`. Methods like `findByUserOrderBySortOrderAscCreatedAtDesc` allow for dynamic sorting natively translating into optimized SQL queries.

### 4.4 Exception Handling Strategy
A robust, centralized error handler lives in `GlobalExceptionHandler.java` (using `@ControllerAdvice`). It catches specific errors like `ResourceNotFoundException`, `DuplicateResourceException`, and Spring's `MethodArgumentNotValidException`, consistently mapping them to a standardized JSON `ErrorResponse` structure.

---

## 5. How Critical Features Work

### 5.1 Link Shortening & Redirection Lifecycle
1.  **Creation**: A user submits a long URL (e.g., `https://github.com/adityagupta`).
2.  **Hashing**: The `UrlShortenerService` creates a 6-character random alphanumeric string (e.g., `aB3x9Q`).
3.  **Persistence**: The Link is saved in Postgres.
4.  **Redirection**: A visitor clicks `https://linkly-amwf.onrender.com/r/aB3x9Q`.
5.  **Analytics**: The `RedirectController` intercepts this GET request, ensures the link and user account are active, increments the `click_count` by 1, saves it, and responds with an HTTP 302 (Found) redirecting the browser to the original URL.

### 5.2 Custom Link Reordering (Drag and Drop)
To give users granular control over their link display order:
1.  **Frontend State**: `Dashboard.jsx` utilizes `@hello-pangea/dnd` to allow users to drag link cards. Upon drop, the array indices are swapped locally for immediate visual feedback.
2.  **Backend Sync**: On drop completion, the frontend fires a `PUT /api/links/reorder` payload detailing the exact sequential ordering of UUIDs. The backend iteratively updates the `sortOrder` integer for each UUID and flushes the bulk transaction to PostgreSQL.

### 5.3 The "Buy Me a Coffee" (UPI) Integration
Instead of building a complex payment gateway integration (Stripe/Razorpay), Linkly leverages India's robust UPI framework natively:
1.  User enters their `UPI ID` during registration or via settings.
2.  The React app leverages the `react-qr-code` package.
3.  The QR code natively encodes the standard URI scheme: `upi://pay?pa={UPI_ID}&pn={USER_NAME}`.
4.  When scanned via Google Pay, PhonePe, or Paytm on a mobile device, it opens the payment app with pre-filled, secure payment details directed exclusively to the user.

### 5.4 Public Messaging System & Inbox
1.  **Opt-In**: Messaging is disabled by default. A user must set `enablePublicMessaging` to true in Settings.
2.  **Submission**: Visitors invoke an unauthenticated `POST /api/public/u/{username}/messages` with `senderName` and `content`.
3.  **Inbox Management**: The user visits `/inbox`, triggering a secure request to fetch their messages. They can also permanently delete unwanted messages via the `MessageService` which maps to a Trash icon in the UI.

### 5.5 Admin Control & User Suspension
1.  **Seeding**: On backend startup (`LinklyApplication.java`), an admin account (`admin` / `admin`) is seeded automatically if it doesn't exist.
2.  **Dashboard**: The admin logs in and accesses `/admin`. They can view all users, total links, and total platform clicks.
3.  **Suspension**: If an admin suspends a user, the `isSuspended` flag is set to `true`. 
    - The user is instantly logged out (handled via JWT validation logic).
    - Their public profile (`/p/{username}`) immediately returns a 404.
    - All of their shortened `/r/` links immediately disable and return a 404 to protect end-users.

---

## 6. Security & Infrastructure Protocols
- **Stateless JWT**: Authentication relies entirely on Bearer tokens. No session state is held on the server, allowing Render to scale the backend horizontally if needed.
- **BCrypt Hashing**: Passwords are never stored or transmitted in plaintext.
- **CORS Configuration**: The backend explicitly allows Cross-Origin Resource Sharing from the Vercel production domain, rejecting unauthorized API attempts.
- **Admin Isolation**: Accounts flagged with `ROLE_ADMIN` cannot be deleted or suspended. Only standard `ROLE_USER` accounts are subjected to Admin actions.
- **Data Isolation (IDOR Protection)**: Every `GET`, `PUT`, `DELETE` operation strictly verifies the JWT subject against the requested resource's owner, preventing Insecure Direct Object References.
- **Cascade Deletion**: When an admin deletes an account (or if a user deletes their own), Postgres `ON DELETE CASCADE` constraints ensure all linked messages and URLs are instantly scrubbed from the database, ensuring zero orphaned rows and strict data privacy compliance.
- **SPA Routing**: The Vercel deployment relies on a `vercel.json` rewrite configuration to intercept client-side routes (like `/login` or `/inbox`) and serve `index.html`, preventing native 404s on page reloads.
