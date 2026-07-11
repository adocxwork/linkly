# Linkly 🔗

![Linkly Banner](https://img.shields.io/badge/Linkly-Live-success?style=for-the-badge&logo=vercel)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3-6DB33F?style=for-the-badge&logo=spring)
![React](https://img.shields.io/badge/React-18-61DAFB?style=for-the-badge&logo=react)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791?style=for-the-badge&logo=postgresql)

**Linkly** is an elegant, high-performance link-in-bio application that lets you curate and showcase your digital presence in a single, beautiful place. Customize your public profile, monitor your link engagement with robust analytics, and receive direct payments and public messages straight from your audience.

### 🌍 Live Application
- **Frontend (Live Website):** [https://linkly-plum.vercel.app/](https://linkly-plum.vercel.app/)
- **Backend API:** [https://linkly-amwf.onrender.com/](https://linkly-amwf.onrender.com/)

---

## 🌟 Key Features

- **Personalized Profile (`/p/{username}`)**: Share one link that directs visitors to all your important content.
- **Link Analytics**: Monitor exactly how many times each of your links has been clicked in real-time.
- **Custom Link Reordering**: Seamless drag-and-drop capability to prioritize your most important links.
- **Integrated Payments**: Turn on your UPI QR code and let visitors "Buy you a coffee" seamlessly.
- **Public Messaging**: Allow your audience to leave public messages directly on your profile, manageable via a dedicated Inbox.
- **Admin Dashboard**: A secure portal for administrators to manage users (suspend/delete) globally.
- **Responsive & Glassmorphic UI**: Beautiful Apple-inspired user interface built using Vite, React, and Framer Motion.

---

## 🛠 Tech Stack

**Frontend**:
- React 18 & Vite
- Framer Motion (for fluid animations)
- Lucide React (for iconography)
- Vanilla CSS (Glassmorphic Design System)

**Backend**:
- Java 21
- Spring Boot 3
- Spring Security (Stateless JWT Authentication)
- Spring Data JPA (Hibernate)

**Database**:
- PostgreSQL 16 (Hosted on Supabase)

---

## 🚀 Local Setup & Installation

If you would like to run Linkly on your local machine for development, follow these steps:

### Prerequisites
1. **Java 21+** installed.
2. **Node.js** (v18+) installed.
3. **PostgreSQL** installed and running locally.

### 1. Database Setup
Create a PostgreSQL database named `linkly` using `psql` or pgAdmin:
```sql
CREATE DATABASE linkly;
```
*(Linkly uses Hibernate, so all tables and relations will be automatically generated upon the first backend start.)*

### 2. Backend Setup
1. Open the project in your favorite IDE (IntelliJ IDEA, VSCode, etc).
2. Navigate to `src/main/resources/application.properties`.
3. Update the `spring.datasource.username` and `spring.datasource.password` if your local Postgres credentials differ from the default.
4. Open a terminal in the root directory and start the Spring Boot server:
```bash
./mvnw spring-boot:run
```
*(The backend will start running on `http://localhost:8080`)*

### 3. Frontend Setup
1. Open a new terminal and navigate to the frontend directory:
```bash
cd frontend
```
2. Install the required Node dependencies:
```bash
npm install
```
3. Start the Vite development server:
```bash
npm run dev
```
*(The frontend will start running on `http://localhost:5173`)*

---

## 🛡️ Default Admin Account

Upon the first boot of the application, an admin account is automatically seeded into the database to give you immediate control over the platform.

- **Username**: `admin`
- **Password**: `admin`

You can log in using these credentials to access the `/admin` dashboard and manage user accounts.

---

## 📝 Usage Guide

1. **Sign Up**: Create an account and choose your `@username`. You can optionally provide your UPI ID and enable public messaging during onboarding.
2. **Dashboard**: Add your URLs. Each URL added gets a shortened `/r/...` link and begins tracking clicks instantly.
3. **Reordering**: Drag and drop your links using the grip icon on the left side of each link card to change their display order.
4. **Settings**: Customize your Bio, toggle your UPI Payment QR, or change your messaging preferences dynamically.
5. **Share**: Share your unique `https://linkly-plum.vercel.app/p/{username}` link with the world!
