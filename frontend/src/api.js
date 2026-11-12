import axios from 'axios';

const BACKEND_URL = import.meta.env.VITE_BACKEND_URL || '';

const api = axios.create({
  baseURL: `${BACKEND_URL}/api`,
  timeout: 60000, // Render free tier can take ~50s to wake up
});

// Attach the JWT (stored after login/register) to every request.
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 401 = token missing, expired or revoked: log out locally and go to the login page.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const path = window.location.pathname;
    const onPublicPage = path === '/login' || path === '/register' || path.startsWith('/p/');
    if (error.response?.status === 401 && !onPublicPage) {
      localStorage.removeItem('user');
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

/** Human-readable message for any API error. */
export const errorMessage = (err, fallback = 'Something went wrong') => {
  if (err.response?.status === 429) {
    return err.response.data?.message || 'Too many requests. Please wait a moment.';
  }
  if (err.response?.data?.message) {
    return err.response.data.message;
  }
  if (err.code === 'ECONNABORTED' || !err.response) {
    return 'Server is not responding (it may be waking up). Please try again in a few seconds.';
  }
  return fallback;
};

/** Full public URL of a short link. */
export const shortLinkUrl = (code) => {
  const base = import.meta.env.VITE_SHORT_LINK_BASE_URL || BACKEND_URL || window.location.origin;
  return `${base.replace(/\/+$/, '')}/r/${code}`;
};

export const saveSession = ({ token, user }) => {
  if (token) localStorage.setItem('token', token);
  if (user) localStorage.setItem('user', JSON.stringify(user));
};

export default api;
