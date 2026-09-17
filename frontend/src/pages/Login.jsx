import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import api from '../api';

const Login = () => {
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleLogin = async (e) => {
    e.preventDefault();
    try {
      const { data } = await api.post('/auth/login', { identifier, password });
      localStorage.setItem('user', JSON.stringify(data.user));
      if (data.user.role === 'ROLE_ADMIN') {
        navigate('/admin');
      } else {
        navigate('/dashboard');
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Login failed');
    }
  };

  const [keepAlive, setKeepAlive] = React.useState(false);
  const [loadingStatus, setLoadingStatus] = React.useState(true);
  const [toggleLoading, setToggleLoading] = React.useState(false);

  React.useEffect(() => {
    const fetchKeepAlive = async () => {
      try {
        const { data } = await api.get('/system/keep-alive');
        setKeepAlive(data.enabled);
      } catch (err) {
        console.error("Failed to fetch keep-alive status", err);
      } finally {
        setLoadingStatus(false);
      }
    };
    fetchKeepAlive();
  }, []);

  const handleToggleKeepAlive = async () => {
    setToggleLoading(true);
    try {
      const { data } = await api.post('/system/keep-alive', { enabled: !keepAlive });
      setKeepAlive(data.enabled);
    } catch (err) {
      console.error("Failed to toggle keep-alive", err);
    } finally {
      setToggleLoading(false);
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
      
      {/* Keep Alive Status Bar */}
      <div style={{
        background: 'var(--surface-color)', padding: '1rem', borderRadius: '12px', 
        marginBottom: '1.5rem', display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        border: '1px solid var(--border-color)'
      }}>
        <div style={{ display: 'flex', flexDirection: 'column' }}>
          <span style={{ fontSize: '0.85rem', fontWeight: '600', color: 'var(--text-color)' }}>Keep Server Awake</span>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
            {loadingStatus ? 'Checking server status (might take 30s)...' : 'Prevents Render from sleeping'}
          </span>
        </div>
        <button 
          onClick={handleToggleKeepAlive}
          disabled={loadingStatus || toggleLoading}
          style={{
            background: keepAlive ? '#10b981' : '#e5e7eb',
            border: 'none', borderRadius: '20px', width: '44px', height: '24px',
            position: 'relative', cursor: (loadingStatus || toggleLoading) ? 'wait' : 'pointer',
            transition: 'background 0.3s ease', padding: 0
          }}
        >
          <div style={{
            position: 'absolute', top: '2px', left: keepAlive ? '22px' : '2px',
            width: '20px', height: '20px', background: 'white', borderRadius: '50%',
            transition: 'left 0.3s cubic-bezier(0.4, 0.0, 0.2, 1)', 
            boxShadow: '0 2px 4px rgba(0,0,0,0.2)'
          }} />
        </button>
      </div>

      {error && <div className="toast error mb-4 text-center">{error}</div>}
      <form onSubmit={handleLogin}>
        <div className="form-group">
          <label className="form-label">Username or Email</label>
          <input 
            type="text" 
            className="form-control"
            value={identifier}
            onChange={(e) => setIdentifier(e.target.value)}
            required 
          />
        </div>
        <div className="form-group">
          <label className="form-label">Password</label>
          <input 
            type="password" 
            className="form-control"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required 
          />
        </div>
        <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '1.5rem' }}>
          <Link to="/forgot-password" style={{ color: 'var(--accent-color)', fontSize: '0.85rem', textDecoration: 'none' }}>
            Forgot password?
          </Link>
        </div>
        <button type="submit" className="btn btn-primary" style={{ width: '100%' }}>
          Log In
        </button>
      </form>
      <p className="text-center mt-6 text-secondary">
        Don't have an account? <Link to="/register" style={{ color: 'var(--accent-color)', textDecoration: 'none' }}>Sign up</Link>
      </p>
    </motion.div>
  );
};

export default Login;
