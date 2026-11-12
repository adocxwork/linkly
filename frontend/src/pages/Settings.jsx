import React, { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import api, { errorMessage, saveSession } from '../api';

const Settings = () => {
  const cachedUser = JSON.parse(localStorage.getItem('user') || '{}');
  const [user, setUser] = useState(cachedUser);
  const [form, setForm] = useState({
    name: cachedUser.name || '',
    bio: cachedUser.bio || '',
    email: cachedUser.email || '',
    upiId: cachedUser.upiId || '',
    enableUpiPayment: !!cachedUser.enableUpiPayment,
    enablePublicMessaging: !!cachedUser.enablePublicMessaging,
    currentPassword: '',
    newPassword: '',
  });
  const [message, setMessage] = useState('');
  const [errorMsg, setErrorMsg] = useState('');
  const [deleteModal, setDeleteModal] = useState(false);
  const navigate = useNavigate();

  // Load fresh data from the server (localStorage may be stale, e.g. edited on another device).
  useEffect(() => {
    api.get('/users/profile').then(({ data }) => {
      setUser(data);
      saveSession({ user: data });
      setForm((f) => ({
        ...f,
        name: data.name || '',
        bio: data.bio || '',
        email: data.email || '',
        upiId: data.upiId || '',
        enableUpiPayment: !!data.enableUpiPayment,
        enablePublicMessaging: !!data.enablePublicMessaging,
      }));
    }).catch(() => {});
  }, []);

  const set = (field) => (e) =>
    setForm({ ...form, [field]: e.target.type === 'checkbox' ? e.target.checked : e.target.value });

  const showError = (msg) => {
    setErrorMsg(msg);
    setTimeout(() => setErrorMsg(''), 5000);
  };

  const sensitiveChange = form.newPassword !== '' || (form.email && form.email.toLowerCase() !== (user.email || '').toLowerCase());

  const handleUpdate = async (e) => {
    e.preventDefault();
    try {
      const { data } = await api.put('/users/settings', form);
      saveSession(data); // a password change revokes old tokens, so store the new one
      setUser(data.user);
      setForm((f) => ({ ...f, currentPassword: '', newPassword: '' }));
      setMessage('Settings updated successfully!');
      setTimeout(() => setMessage(''), 3000);
    } catch (err) {
      showError(errorMessage(err, 'Error updating settings'));
    }
  };

  const logoutEverywhere = async () => {
    try {
      await api.post('/users/me/revoke-sessions');
    } catch {
      // ignore: we log out locally either way
    }
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    navigate('/login');
  };

  const confirmDeleteAccount = async () => {
    try {
      await api.delete('/users/me');
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      navigate('/login');
    } catch (err) {
      showError(errorMessage(err, 'Error deleting account'));
    }
  };

  return (
    <div style={{ maxWidth: '600px', margin: '0 auto', paddingBottom: '4rem' }}>
      <h1 className="mb-6">Account Settings</h1>

      {message && <div className="toast success mb-4 text-center">{message}</div>}
      {errorMsg && (
        <div className="toast error mb-4 text-center" style={{ position: 'fixed', top: '20px', left: '50%', transform: 'translateX(-50%)', zIndex: 9999 }}>
          {errorMsg}
        </div>
      )}

      <div className="card glass mb-8">
        <h3 className="mb-4">Profile</h3>
        <form onSubmit={handleUpdate}>
          <div className="form-group">
            <label className="form-label">Name</label>
            <input type="text" className="form-control" value={form.name} onChange={set('name')} maxLength={50} required />
          </div>
          <div className="form-group">
            <label className="form-label">Bio</label>
            <textarea className="form-control" value={form.bio} onChange={set('bio')} maxLength={255} rows="3"
              placeholder="Tell visitors about yourself" />
          </div>
          <div className="form-group">
            <label className="form-label">UPI ID</label>
            <input type="text" className="form-control" value={form.upiId} onChange={set('upiId')} placeholder="e.g. yourname@okbank" />
          </div>
          <div className="form-group flex items-center gap-2 mb-4" style={{ marginTop: '1rem' }}>
            <input type="checkbox" id="enableUpi" checked={form.enableUpiPayment} onChange={set('enableUpiPayment')}
              style={{ width: '18px', height: '18px', accentColor: 'var(--accent-color)' }} />
            <label htmlFor="enableUpi" style={{ margin: 0, fontWeight: 500, cursor: 'pointer' }}>Show UPI payment QR on my profile</label>
          </div>
          <div className="form-group flex items-center gap-2 mb-6">
            <input type="checkbox" id="enableMessaging" checked={form.enablePublicMessaging} onChange={set('enablePublicMessaging')}
              style={{ width: '18px', height: '18px', accentColor: 'var(--accent-color)' }} />
            <label htmlFor="enableMessaging" style={{ margin: 0, fontWeight: 500, cursor: 'pointer' }}>Accept anonymous messages</label>
          </div>

          <h3 className="mb-4 mt-8">Login details</h3>
          <div className="form-group">
            <label className="form-label">Email address</label>
            <input type="email" className="form-control" value={form.email} onChange={set('email')} required />
          </div>
          <div className="form-group">
            <label className="form-label">New password</label>
            <input type="password" className="form-control" value={form.newPassword} onChange={set('newPassword')}
              placeholder="Leave blank to keep current password" minLength={8} maxLength={72} autoComplete="new-password" />
          </div>
          {sensitiveChange && (
            <div className="form-group">
              <label className="form-label">Current password (required to change email or password)</label>
              <input type="password" className="form-control" value={form.currentPassword} onChange={set('currentPassword')}
                required autoComplete="current-password" />
            </div>
          )}
          <button type="submit" className="btn btn-primary">Save Changes</button>
        </form>
      </div>

      <div className="card glass mb-8">
        <h3 className="mb-2">Sessions</h3>
        <p className="text-secondary mb-4">Signed in on a device you no longer use? This logs you out everywhere, including here.</p>
        <button onClick={logoutEverywhere} className="btn btn-secondary">Log out of all devices</button>
      </div>

      {user.role !== 'ROLE_ADMIN' && (
        <div className="card glass" style={{ borderColor: 'rgba(255, 59, 48, 0.3)' }}>
          <h3 className="mb-2 text-danger">Danger Zone</h3>
          <p className="text-secondary mb-4">Once you delete your account, there is no going back. Please be certain.</p>
          <button onClick={() => setDeleteModal(true)} className="btn btn-danger">Delete My Account</button>
        </div>
      )}

      <AnimatePresence>
        {deleteModal && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="modal-overlay">
            <motion.div initial={{ scale: 0.95, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} exit={{ scale: 0.95, opacity: 0 }} className="modal-content">
              <h3 className="mb-4 text-danger">Delete Account?</h3>
              <p className="mb-6 text-secondary">All your links, messages and analytics will be permanently deleted.</p>
              <div className="flex justify-between gap-4">
                <button onClick={() => setDeleteModal(false)} className="btn btn-secondary" style={{ flex: 1 }}>Cancel</button>
                <button onClick={confirmDeleteAccount} className="btn btn-danger" style={{ flex: 1 }}>Yes, Delete My Account</button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

export default Settings;
