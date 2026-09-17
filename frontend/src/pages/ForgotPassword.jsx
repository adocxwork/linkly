import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';
import api from '../api';

const ForgotPassword = () => {
  const [identifier, setIdentifier] = useState('');
  const [status, setStatus] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setStatus('');
    try {
      const { data } = await api.post('/auth/forgot-password', { identifier });
      setStatus({ type: 'success', message: data.message });
    } catch (err) {
      setStatus({ type: 'error', message: err.response?.data?.message || 'Failed to request reset' });
    } finally {
      setLoading(false);
    }
  };

  return (
    <motion.div 
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="card glass" 
      style={{ maxWidth: '400px', margin: '4rem auto' }}
    >
      <h2 className="text-center mb-6">Reset Password</h2>
      
      {status && (
        <div className={`toast ${status.type} mb-4 text-center`}>
          {status.message}
        </div>
      )}

      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label className="form-label">Email or Username</label>
          <input 
            type="text" 
            className="form-control"
            value={identifier}
            onChange={(e) => setIdentifier(e.target.value)}
            required 
            placeholder="Enter your email or username"
          />
        </div>
        
        <button type="submit" className="btn btn-primary" style={{ width: '100%' }} disabled={loading}>
          {loading ? 'Sending...' : 'Send Reset Link'}
        </button>
      </form>

      <p className="text-center mt-6 text-secondary">
        Remembered your password? <Link to="/login" style={{ color: 'var(--accent-color)', textDecoration: 'none' }}>Log in</Link>
      </p>
    </motion.div>
  );
};

export default ForgotPassword;
