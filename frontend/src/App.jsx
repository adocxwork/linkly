import { readStoredUser } from './session';
import React, { Suspense, lazy } from 'react';
import { BrowserRouter, Routes, Route, Navigate, useParams } from 'react-router-dom';
import Navbar from './components/Navbar';

// Each page is loaded on demand, so visitors of a public profile don't download the
// dashboard, charts or admin code.
const Home = lazy(() => import('./pages/Home'));
const Login = lazy(() => import('./pages/Login'));
const Register = lazy(() => import('./pages/Register'));
const Dashboard = lazy(() => import('./pages/Dashboard'));
const AdminDashboard = lazy(() => import('./pages/AdminDashboard'));
const Settings = lazy(() => import('./pages/Settings'));
const Profile = lazy(() => import('./pages/Profile'));
const Inbox = lazy(() => import('./pages/Inbox'));
const ErrorPage = lazy(() => import('./pages/ErrorPage'));

const ProtectedRoute = ({ children, adminOnly = false }) => {
  const user = readStoredUser();
  const token = localStorage.getItem('token');

  if (!user || !token) {
    return <Navigate to="/login" />;
  }
  // UI-only check for convenience; the backend enforces admin access on every request.
  if (adminOnly && user.role !== 'ROLE_ADMIN') {
    return <Navigate to="/dashboard" />;
  }
  return children;
};

function PublicProfileRoute() {
  const { username } = useParams();
  return <Profile key={username} username={username} />;
}

function App() {
  return (
    <BrowserRouter>
      <Navbar />
      <main id="main-content" className="container app-content" tabIndex={-1}>
        <Suspense fallback={<div className="text-center text-secondary mt-8">Loading…</div>}>
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/dashboard" element={<ProtectedRoute><Dashboard /></ProtectedRoute>} />
            <Route path="/inbox" element={<ProtectedRoute><Inbox /></ProtectedRoute>} />
            <Route path="/settings" element={<ProtectedRoute><Settings /></ProtectedRoute>} />
            <Route path="/admin" element={<ProtectedRoute adminOnly><AdminDashboard /></ProtectedRoute>} />
            <Route path="/p/:username" element={<PublicProfileRoute />} />
            <Route path="/link-error" element={<ErrorPage />} />
            <Route path="*" element={<ErrorPage variant="notFound" />} />
          </Routes>
        </Suspense>
      </main>
    </BrowserRouter>
  );
}

export default App;
