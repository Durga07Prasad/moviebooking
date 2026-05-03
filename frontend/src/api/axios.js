import axios from 'axios';

/**
 * Axios instance — all API calls go through this
 * ================================================
 * With Vite proxy configured (/api → http://localhost:8080),
 * we use a RELATIVE baseURL. This means:
 * - Browser sends request to: http://localhost:3000/api/auth/login
 * - Vite proxies it to:       http://localhost:8080/api/auth/login
 * - No CORS errors possible
 *
 * OOAD: Adapter Pattern (frontend side) — this instance adapts
 * the raw API to a consistent interface for all pages.
 * ================================================
 */
const api = axios.create({
  baseURL: '',   // Empty = use Vite proxy (relative URLs)
  headers: { 'Content-Type': 'application/json' },
});

// ── Request interceptor: attach JWT token to every request ──
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// ── Response interceptor: auto-logout on 401 Unauthorized ──
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.clear();
      window.location.href = '/';
    }
    return Promise.reject(error);
  }
);

export default api;
