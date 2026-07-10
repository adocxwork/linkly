import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import api from '../api';

const Settings = () => {
  const user = JSON.parse(localStorage.getItem('user'));
  const [name, setName] = useState(user.name || '');
  const [bio, setBio] = useState(user.bio || '');
  const [email, setEmail] = useState(user.email || '');
  const [upiId, setUpiId] = useState(user.upiId || '');
  const [enableUpiPayment, setEnableUpiPayment] = useState(user.enableUpiPayment || false);
  const [password, setPassword] = useState('');
  const [message, setMessage] = useState('');
  const [deleteModal, setDeleteModal] = useState(false);
  const navigate = useNavigate();

  const handleUpdate = async (e) => {
    e.preventDefault();
    try {
      await api.put('/users/settings', { name, bio, email, password, upiId, enableUpiPayment });
      
      const updatedUser = { ...user, name, bio, upiId, enableUpiPayment, email: email || user.email };
      localStorage.setItem('user', JSON.stringify(updatedUser));
      
      setMessage('Settings updated successfully!');
      setPassword('');
      setTimeout(() => setMessage(''), 3000);
    } catch (err) {
      alert(err.response?.data?.message || 'Error updating settings');
    }
  };

  const confirmDeleteAccount = async () => {
    try {
      await api.delete('/users/me');
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      navigate('/login');
    } catch (err) {
      alert(err.response?.data?.message || 'Error deleting account');
    }
  };

  return (
    <div style={{ maxWidth: '600px', margin: '0 auto', paddingBottom: '4rem' }}>
      <h1 className="mb-6">Account Settings</h1>
      
      {message && <div className="toast success mb-4 text-center">{message}</div>}

      <div className="card glass mb-8">
        <h3 className="mb-4">Update Details</h3>
        <form onSubmit={handleUpdate}>
          <div className="form-group">
            <label className="form-label">Name</label>
            <input 
              type="text" 
              className="form-control"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Your full name"
            />
          </div>
          <div className="form-group">
            <label className="form-label">Bio</label>
            <textarea 
              className="form-control"
              value={bio}
              onChange={(e) => setBio(e.target.value)}
              placeholder="Tell us about yourself"
              rows="3"
            />
          </div>
          <div className="form-group">
            <label className="form-label">Email Address</label>
            <input 
              type="email" 
              className="form-control"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="Leave blank to keep unchanged"
            />
          </div>

          <div className="form-group">
            <label className="form-label">UPI ID</label>
            <input 
              type="text" 
              className="form-control"
              value={upiId}
              onChange={(e) => setUpiId(e.target.value)}
              placeholder="e.g. yourname@upi"
            />
          </div>

          <div className="form-group flex items-center gap-2 mb-4" style={{marginTop: '1rem'}}>
            <input 
              type="checkbox" 
              id="enableUpi"
              checked={enableUpiPayment}
              onChange={(e) => setEnableUpiPayment(e.target.checked)}
              style={{ width: '18px', height: '18px', accentColor: 'var(--accent-color)' }}
            />
            <label htmlFor="enableUpi" style={{ margin: 0, fontWeight: 500, cursor: 'pointer' }}>
              Enable QR for UPI Payment on Profile
            </label>
          </div>

          <div className="form-group">
            <label className="form-label">New Password</label>
            <input 
              type="password" 
              className="form-control"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Leave blank to keep unchanged"
            />
          </div>
          <button type="submit" className="btn btn-primary">Save Changes</button>
        </form>
      </div>

      {user.role !== 'ROLE_ADMIN' && (
        <div className="card glass" style={{ borderColor: 'rgba(255, 59, 48, 0.3)' }}>
          <h3 className="mb-2 text-danger">Danger Zone</h3>
          <p className="text-secondary mb-4">Once you delete your account, there is no going back. Please be certain.</p>
          <button onClick={() => setDeleteModal(true)} className="btn btn-danger">
            Delete My Account
          </button>
        </div>
      )}

      <AnimatePresence>
        {deleteModal && (
          <motion.div 
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="modal-overlay"
          >
            <motion.div 
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="modal-content"
            >
              <h3 className="mb-4 text-danger">Delete Account?</h3>
              <p className="mb-6 text-secondary">Are you entirely sure you want to delete your account? All your links will be permanently deleted.</p>
              <div className="flex justify-between gap-4">
                <button onClick={() => setDeleteModal(false)} className="btn btn-secondary" style={{ flex: 1 }}>
                  Cancel
                </button>
                <button onClick={confirmDeleteAccount} className="btn btn-danger" style={{ flex: 1 }}>
                  Yes, Delete My Account
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

export default Settings;
