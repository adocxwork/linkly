import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Trash2, Shield, UserX, UserCheck } from 'lucide-react';
import api from '../api';

const AdminDashboard = () => {
  const [users, setUsers] = useState([]);
  const [keepAlive, setKeepAlive] = useState(false);
  const [deleteModal, setDeleteModal] = useState({ show: false, username: null });
  const [errorMsg, setErrorMsg] = useState('');
  const admin = JSON.parse(localStorage.getItem('user'));

  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    try {
      const res = await api.get('/admin/users');
      setUsers(res.data);
      const pingRes = await api.get('/system/keep-alive');
      setKeepAlive(pingRes.data.enabled);
    } catch (err) {
      showError('Error fetching admin data');
    }
  };

  const toggleKeepAlive = async () => {
    try {
      const { data } = await api.post('/system/keep-alive', { enabled: !keepAlive });
      setKeepAlive(data.enabled);
    } catch (err) {
      showError('Failed to toggle keep-alive');
    }
  };

  const showError = (msg) => {
    setErrorMsg(msg);
    setTimeout(() => setErrorMsg(''), 4000);
  };

  const toggleSuspend = async (username) => {
    try {
      await api.put(`/admin/users/${username}/suspend`);
      fetchUsers();
    } catch (err) {
      showError(err.response?.data?.message || 'Error suspending user');
    }
  };

  const confirmDelete = async () => {
    try {
      await api.delete(`/admin/users/${deleteModal.username}`);
      setDeleteModal({ show: false, username: null });
      fetchUsers();
    } catch (err) {
      showError(err.response?.data?.message || 'Error deleting user');
    }
  };

  return (
    <div style={{ paddingBottom: '4rem' }}>
      {errorMsg && (
        <div className="toast error mb-4 text-center" style={{ position: 'fixed', top: '20px', left: '50%', transform: 'translateX(-50%)', zIndex: 9999 }}>
          {errorMsg}
        </div>
      )}
      <div className="flex items-center gap-2 mb-6">
        <Shield size={32} color="var(--accent-color)" />
        <h1>Admin Control Panel</h1>
      </div>

      {/* KEEP ALIVE CARD */}
      <div className="card glass mb-8 flex justify-between items-center" style={{ padding: '1.5rem', marginBottom: '2rem' }}>
        <div>
          <h3 className="mb-1">Keep Server Awake</h3>
          <p className="text-secondary text-sm">Prevents Render from sleeping</p>
        </div>
        <button 
          onClick={toggleKeepAlive}
          style={{
            width: '44px', height: '24px', borderRadius: '12px', border: 'none', cursor: 'pointer',
            background: keepAlive ? '#10b981' : '#e5e7eb',
            position: 'relative', transition: 'background 0.3s'
          }}
        >
          <div style={{
            width: '20px', height: '20px', borderRadius: '50%', background: 'white',
            position: 'absolute', top: '2px', left: keepAlive ? '22px' : '2px',
            transition: 'left 0.3s', boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
          }} />
        </button>
      </div>

      <div className="card glass">
        <h3 className="mb-6">Registered Users ({users.length})</h3>
        
        <div className="flex-col gap-4">
          {users.map(u => (
            <div key={u.id} className="flex items-center justify-between" style={{ padding: '15px 0', borderBottom: '1px solid var(--border-color)' }}>
              <div>
                <div className="flex items-center gap-2">
                  <h4 style={{ margin: 0 }}>{u.name}</h4>
                  {u.role === 'ROLE_ADMIN' && <span style={{ fontSize: '0.7rem', background: 'var(--accent-color)', color: 'white', padding: '2px 6px', borderRadius: '4px' }}>ADMIN</span>}
                  {u.isSuspended && <span style={{ fontSize: '0.7rem', background: 'var(--danger-color)', color: 'white', padding: '2px 6px', borderRadius: '4px' }}>SUSPENDED</span>}
                </div>
                <div className="text-secondary" style={{ fontSize: '0.9rem' }}>@{u.username} • {u.email}</div>
              </div>
              
              {u.role !== 'ROLE_ADMIN' && (
                <div className="flex gap-2">
                  <button 
                    onClick={() => toggleSuspend(u.username)}
                    className={`btn btn-icon ${u.isSuspended ? 'btn-primary' : 'btn-secondary'}`}
                    title={u.isSuspended ? 'Unsuspend User' : 'Suspend User'}
                  >
                    {u.isSuspended ? <UserCheck size={18} /> : <UserX size={18} />}
                  </button>
                  <button 
                    onClick={() => setDeleteModal({ show: true, username: u.username })}
                    className="btn btn-icon text-danger"
                    style={{ backgroundColor: 'rgba(255,59,48,0.1)' }}
                    title="Delete User"
                  >
                    <Trash2 size={18} />
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      </div>

      <AnimatePresence>
        {deleteModal.show && (
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
              <h3 className="mb-4 text-danger">Delete User?</h3>
              <p className="mb-6 text-secondary">Are you sure you want to completely delete user @{deleteModal.username}? All their links and data will be permanently erased.</p>
              <div className="flex justify-between gap-4">
                <button onClick={() => setDeleteModal({ show: false, username: null })} className="btn btn-secondary" style={{ flex: 1 }}>
                  Cancel
                </button>
                <button onClick={confirmDelete} className="btn btn-danger" style={{ flex: 1 }}>
                  Delete User
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

export default AdminDashboard;
