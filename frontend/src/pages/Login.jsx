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
      localStorage.setItem('token', data.token);
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

  return (
    <motion.div 
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="card glass" 
      style={{ maxWidth: '400px', margin: '4rem auto' }}
    >
      <h2 className="text-center mb-6">Welcome Back</h2>
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
