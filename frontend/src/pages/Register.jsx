import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import api from '../api';

const Register = () => {
  const [formData, setFormData] = useState({ name: '', username: '', email: '', password: '' });
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleRegister = async (e) => {
    e.preventDefault();
    try {
      const { data } = await api.post('/auth/register', formData);
      localStorage.setItem('token', data.token);
      localStorage.setItem('user', JSON.stringify(data.user));
      navigate('/dashboard');
    } catch (err) {
      setError(err.response?.data?.message || 'Registration failed');
    }
  };

  return (
    <motion.div 
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="card glass" 
      style={{ maxWidth: '400px', margin: '4rem auto' }}
    >
      <h2 className="text-center mb-6">Create Account</h2>
      {error && <div className="toast error mb-4 text-center">{error}</div>}
      <form onSubmit={handleRegister}>
        <div className="form-group">
          <label className="form-label">Name</label>
          <input 
            type="text" 
            className="form-control"
            value={formData.name}
            onChange={(e) => setFormData({...formData, name: e.target.value})}
            required 
          />
        </div>
        <div className="form-group">
          <label className="form-label">Username</label>
          <input 
            type="text" 
            className="form-control"
            value={formData.username}
            onChange={(e) => setFormData({...formData, username: e.target.value})}
            required 
          />
        </div>
        <div className="form-group">
          <label className="form-label">Email</label>
          <input 
            type="email" 
            className="form-control"
            value={formData.email}
            onChange={(e) => setFormData({...formData, email: e.target.value})}
            required 
          />
        </div>
        <div className="form-group">
          <label className="form-label">Password</label>
          <input 
            type="password" 
            className="form-control"
            value={formData.password}
            onChange={(e) => setFormData({...formData, password: e.target.value})}
            required 
          />
        </div>
        <button type="submit" className="btn btn-primary" style={{ width: '100%' }}>
          Sign Up
        </button>
      </form>
      <p className="text-center mt-6 text-secondary">
        Already have an account? <Link to="/login" style={{ color: 'var(--accent-color)', textDecoration: 'none' }}>Log in</Link>
      </p>
    </motion.div>
  );
};

export default Register;
