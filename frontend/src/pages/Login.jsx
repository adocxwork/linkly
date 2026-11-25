import React, { useEffect, useRef, useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import api, { errorMessage, readinessUrl, saveSession } from '../api';

const STARTUP_WINDOW_MS = 120_000;
const PROBE_TIMEOUT_MS = 15_000;
const RETRY_DELAY_MS = 3_000;

function useBackendReadiness(attempt) {
  const [status, setStatus] = useState('checking');

  useEffect(() => {
    let active = true;
    const deadline = Date.now() + STARTUP_WINDOW_MS;
    let request;
    let timeout;
    let retry;

    async function probe() {
      const timeLeft = deadline - Date.now();
      if (timeLeft <= 0) {
        setStatus('unavailable');
        return;
      }
      request = new AbortController();
      timeout = window.setTimeout(() => request.abort(), Math.min(PROBE_TIMEOUT_MS, timeLeft));
      try {
        const response = await fetch(readinessUrl, {
          cache: 'no-store',
          credentials: 'omit',
          headers: { Accept: 'application/json' },
          signal: request.signal,
        });
        if (response.ok && (await response.json()).status === 'UP' && active) {
          setStatus('ready');
          return;
        }
      } catch {
        // The host can time out or return a startup page before Spring is ready.
      } finally {
        window.clearTimeout(timeout);
      }
      if (!active) return;
      const remaining = deadline - Date.now();
      if (remaining <= 0) {
        setStatus('unavailable');
      } else {
        retry = window.setTimeout(probe, Math.min(RETRY_DELAY_MS, remaining));
      }
    }

    probe();
    return () => {
      active = false;
      request?.abort();
      window.clearTimeout(timeout);
      window.clearTimeout(retry);
    };
  }, [attempt]);

  return [status, setStatus];
}

const Login = () => {
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [attempt, setAttempt] = useState(0);
  const [backendStatus, setBackendStatus] = useBackendReadiness(attempt);
  const [submitting, setSubmitting] = useState(false);
  const inFlight = useRef(false);
  const navigate = useNavigate();

  const handleLogin = async (e) => {
    e.preventDefault();
    if (backendStatus !== 'ready' || inFlight.current) return;
    inFlight.current = true;
    setSubmitting(true);
    setError('');
    try {
      const { data } = await api.post('/auth/login', { identifier, password });
      saveSession(data);
      if (data.user.role === 'ROLE_ADMIN') {
        navigate('/admin');
      } else {
        navigate('/dashboard');
      }
    } catch (err) {
      setError(errorMessage(err, 'Login failed'));
    } finally {
      inFlight.current = false;
      setSubmitting(false);
    }
  };

  return (
    <motion.div 
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="card glass" 
      style={{ maxWidth: '400px', margin: '4rem auto', position: 'relative' }}
    >
      <h2 className="text-center mb-6">Welcome Back</h2>
      <div role="status" className="mb-4 text-center text-secondary">
        {backendStatus === 'checking' && 'Checking the server. If it was idle, it may take about a minute to wake up. You can fill in your details while you wait.'}
        {backendStatus === 'unavailable' && 'The server is taking longer than expected. Check again to see if it is ready.'}
      </div>
      {backendStatus === 'unavailable' && (
        <button type="button" className="btn btn-secondary mb-4" onClick={() => {
          setBackendStatus('checking');
          setAttempt((value) => value + 1);
        }}>Check again</button>
      )}
      {error && <div role="alert" className="toast error mb-4 text-center">{error}</div>}
      <form onSubmit={handleLogin}>
        <div className="form-group">
          <label className="form-label" htmlFor="login-identifier">Username or Email</label>
          <input 
            id="login-identifier"
            autoComplete="username"
            type="text" 
            className="form-control"
            value={identifier}
            onChange={(e) => setIdentifier(e.target.value)}
            required 
          />
        </div>
        <div className="form-group">
          <label className="form-label" htmlFor="login-password">Password</label>
          <input 
            id="login-password"
            autoComplete="current-password"
            type="password" 
            className="form-control"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required 
          />
        </div>
        <button type="submit" className="btn btn-primary" style={{ width: '100%' }}
          disabled={backendStatus !== 'ready' || submitting}>
          {submitting ? 'Signing in…' : 'Log In'}
        </button>
      </form>
      <p className="text-center mt-6 text-secondary">
        Don't have an account? <Link to="/register" style={{ color: 'var(--accent-color)', textDecoration: 'none' }}>Sign up</Link>
      </p>
    </motion.div>
  );
};

export default Login;
