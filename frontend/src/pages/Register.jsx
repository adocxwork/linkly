import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import api from '../api';

const Register = () => {
  const [name, setName] = useState('');
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [upiId, setUpiId] = useState('');
  const [enableUpiPayment, setEnableUpiPayment] = useState(false);
  const [enablePublicMessaging, setEnablePublicMessaging] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleRegister = async (e) => {
    e.preventDefault();
    try {
      const { data } = await api.post('/auth/register', { 
        name, 
        username, 
        email, 
        password, 
        upiId: enableUpiPayment ? upiId : null,
        enableUpiPayment,
        enablePublicMessaging
      });
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
            value={name}
            onChange={(e) => setName(e.target.value)}
            required 
          />
        </div>
        <div className="form-group">
          <label className="form-label">Username</label>
          <input 
            type="text" 
            className="form-control"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required 
          />
        </div>
        <div className="form-group">
          <label className="form-label">Email</label>
          <input 
            type="email" 
            className="form-control"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
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
              placeholder="Create a password"
              required 
            />
          </div>
          
          <div className="form-group flex items-center gap-2 mb-4">
            <input 
              type="checkbox" 
              id="enablePublicMessaging"
              checked={enablePublicMessaging}
              onChange={(e) => setEnablePublicMessaging(e.target.checked)}
              style={{ width: '16px', height: '16px' }}
            />
            <label htmlFor="enablePublicMessaging" className="text-secondary" style={{ fontSize: '0.9rem', cursor: 'pointer', margin: 0 }}>
              Receive public messages on my profile
            </label>
          </div>

          <div className="form-group flex items-center gap-2 mb-4">
            <input 
              type="checkbox" 
              id="enableUpiPayment"
              checked={enableUpiPayment}
              onChange={(e) => setEnableUpiPayment(e.target.checked)}
              style={{ width: '16px', height: '16px' }}
            />
            <label htmlFor="enableUpiPayment" className="text-secondary" style={{ fontSize: '0.9rem', cursor: 'pointer', margin: 0 }}>
              Display a UPI Payment QR code on my profile
            </label>
          </div>

          {enableUpiPayment && (
            <div className="form-group mb-6">
              <label className="form-label">UPI ID</label>
              <input 
                type="text" 
                className="form-control"
                value={upiId}
                onChange={(e) => setUpiId(e.target.value)}
                placeholder="e.g. john@upi"
                required
              />
            </div>
          )}
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
