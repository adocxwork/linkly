import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Trash2, Shield, UserX, UserCheck } from 'lucide-react';
import api from '../api';

const AdminDashboard = () => {
  const [users, setUsers] = useState([]);
  const [deleteModal, setDeleteModal] = useState({ show: false, username: null });
  const admin = JSON.parse(localStorage.getItem('user'));

  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    try {
      const res = await api.get('/admin/users');
      setUsers(res.data);
    } catch (err) {
      console.error(err);
    }
  };

  const toggleSuspend = async (username) => {
    try {
      await api.put(`/admin/users/${username}/suspend`);
      fetchUsers();
    } catch (err) {
      alert(err.response?.data?.message || 'Error suspending user');
    }
  };

  const confirmDelete = async () => {
    try {
      await api.delete(`/admin/users/${deleteModal.username}`);
      setDeleteModal({ show: false, username: null });
      fetchUsers();
    } catch (err) {
      alert(err.response?.data?.message || 'Error deleting user');
    }
  };

  return (
    <div style={{ paddingBottom: '4rem' }}>
      <div className="flex items-center gap-2 mb-6">
        <Shield size={32} color="var(--accent-color)" />
        <h1>Admin Control Panel</h1>
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
