# Linkly 🔗

Linkly is an elegant, high-performance link-in-bio application that lets you curate and showcase your digital presence in a single, beautiful place. Customize your public profile, monitor your link engagement with robust analytics, and receive direct payments and public messages straight from your audience.

---

## 🌟 Key Features

- **Personalized Profile (`/p/{username}`)**: Share one link that directs visitors to all your important content.
- **Custom Link Reordering**: Seamless drag-and-drop / manual reordering of your links to prioritize the most important ones.
- **Link Analytics**: Monitor exactly how many times each of your links has been clicked.
- **Integrated Payments**: Turn on your UPI QR code and let visitors "Buy you a coffee" seamlessly.
- **Public Messaging**: Allow your audience to leave public messages directly on your profile, manageable via a dedicated Inbox.
- **Admin Dashboard**: A secure portal for administrators to manage users (suspend/delete).
- **Responsive & Glassmorphic UI**: Beautiful Apple-inspired user interface built using Vite + React.

---

## 🛠 Tech Stack

**Frontend**:
- React 18
- Vite
- Framer Motion (for fluid animations)
- Lucide React (for icons)

**Backend**:
- Java 21
- Spring Boot 3
- Spring Security (JWT Authentication)
- Spring Data JPA

**Database**:
- PostgreSQL 16

---

## 🚀 Getting Started

### Prerequisites
1. **Java 21+** installed.
2. **Node.js** (v18+) installed.
3. **PostgreSQL** installed and running locally.

### 1. Database Setup
Create a PostgreSQL database named `linkly`:
```sql
CREATE DATABASE linkly;
```
*(Linkly uses Hibernate, so tables will be automatically generated upon the first backend start.)*

### 2. Backend Setup
Navigate to the root directory and update your application properties if your Postgres credentials differ from the default (`username: postgres`, `password: postgres`):
`src/main/resources/application.properties`

Start the Spring Boot backend:
```bash
./mvnw spring-boot:run
```
*(The backend will start on `http://localhost:8080`)*

### 3. Frontend Setup
Open a new terminal and navigate to the frontend directory:
```bash
cd frontend
npm install
npm run dev
```
*(The frontend will start on `http://localhost:5173`)*

---

## 🛡️ Default Admin Account
Upon the first boot, an admin account is automatically seeded:
- **Username**: `admin`
- **Password**: `admin123`

You can use this account to access the `/admin` dashboard and manage user accounts.

---

## 📝 Usage Guide
1. **Sign Up**: Create an account and choose your `@username`. You can optionally provide your UPI ID and enable public messaging.
2. **Dashboard**: Add your URLs. Each URL added gets a shortened `/r/...` link and begins tracking clicks.
3. **Reordering**: Use the `^` and `v` buttons on your links to change the display order.
4. **Settings**: Customize your Bio, toggle your UPI Payment QR, or change your messaging preferences.
5. **Share**: Share your `http://localhost:5173/p/{username}` link with the world!
