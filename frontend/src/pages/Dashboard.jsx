import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Copy, Check, Trash2, Power, Plus } from 'lucide-react';
import api from '../api';

const Dashboard = () => {
  const [links, setLinks] = useState([]);
  const [stats, setStats] = useState({ totalLinks: 0, totalClicks: 0 });
  const [newTitle, setNewTitle] = useState('');
  const [newUrl, setNewUrl] = useState('');
  const [copiedId, setCopiedId] = useState(null);
  const [deleteModal, setDeleteModal] = useState({ show: false, linkId: null });
  const user = JSON.parse(localStorage.getItem('user'));

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      const [linksRes, statsRes] = await Promise.all([
        api.get('/links'),
        api.get(`/links/dashboard/${user.username}`)
      ]);
      setLinks(linksRes.data);
      setStats(statsRes.data);
    } catch (err) {
      console.error(err);
    }
  };

  const handleAddLink = async (e) => {
    e.preventDefault();
    try {
      await api.post('/links', { title: newTitle, originalUrl: newUrl, active: true });
      setNewTitle('');
      setNewUrl('');
      fetchData();
    } catch (err) {
      alert(err.response?.data?.message || 'Error adding link');
    }
  };

  const toggleLink = async (id, currentStatus) => {
    try {
      await api.put(`/links/${id}`, { active: !currentStatus });
      fetchData();
    } catch (err) {
      console.error(err);
    }
  };

  const confirmDelete = async () => {
    try {
      await api.delete(`/links/${deleteModal.linkId}`);
      setDeleteModal({ show: false, linkId: null });
      fetchData();
    } catch (err) {
      console.error(err);
    }
  };

  const copyToClipboard = (shortUrl, id) => {
    const fullUrl = `http://localhost:8080/r/${shortUrl}`;
    navigator.clipboard.writeText(fullUrl);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 2000);
  };

  return (
    <div style={{ paddingBottom: '4rem' }}>
      <h1 className="mb-6">Hello, {user.name}</h1>
      
      <div className="flex gap-4 mb-8">
        <div className="card glass" style={{ flex: 1 }}>
          <div className="text-secondary mb-2">Total Links</div>
          <h2>{stats.totalLinks}</h2>
        </div>
        <div className="card glass" style={{ flex: 1 }}>
          <div className="text-secondary mb-2">Total Clicks</div>
          <h2>{stats.totalClicks}</h2>
        </div>
      </div>

      <div className="card glass mb-8">
        <h3 className="mb-4">Create New Link</h3>
        <form onSubmit={handleAddLink} className="flex gap-4 items-center">
          <input 
            type="text" 
            placeholder="Title (e.g. My Twitter)" 
            className="form-control" 
            value={newTitle}
            onChange={(e) => setNewTitle(e.target.value)}
            required 
            style={{ flex: 1 }}
          />
          <input 
            type="url" 
            placeholder="https://example.com" 
            className="form-control" 
            value={newUrl}
            onChange={(e) => setNewUrl(e.target.value)}
            required 
            style={{ flex: 2 }}
          />
          <button type="submit" className="btn btn-primary" style={{ height: '48px' }}>
            <Plus size={18} /> Add
          </button>
        </form>
      </div>

      <div className="flex-col gap-4">
        {links.map(link => (
          <motion.div 
            key={link.id}
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            className="card glass flex items-center justify-between"
            style={{ padding: '20px', opacity: link.active ? 1 : 0.6 }}
          >
            <div>
              <h4 style={{ marginBottom: '4px' }}>{link.title}</h4>
              <div className="text-secondary" style={{ fontSize: '0.9rem', marginBottom: '8px' }}>{link.originalUrl}</div>
              <div className="flex items-center gap-4">
                <span style={{ color: 'var(--accent-color)', fontWeight: 500 }}>/r/{link.shortUrl}</span>
                <span className="text-secondary" style={{ fontSize: '0.85rem' }}>{link.clickCount} clicks</span>
              </div>
            </div>
            
            <div className="flex items-center gap-2">
              <button 
                onClick={() => copyToClipboard(link.shortUrl, link.id)} 
                className="btn btn-secondary btn-icon"
                title="Copy Link"
              >
                {copiedId === link.id ? <Check size={18} color="#34c759" /> : <Copy size={18} />}
              </button>
              
              <button 
                onClick={() => toggleLink(link.id, link.active)}
                className={`btn btn-icon ${link.active ? 'btn-secondary' : 'btn-primary'}`}
                title={link.active ? 'Disable Link' : 'Enable Link'}
              >
                <Power size={18} />
              </button>

              <button 
                onClick={() => setDeleteModal({ show: true, linkId: link.id })}
                className="btn btn-icon text-danger"
                style={{ backgroundColor: 'rgba(255,59,48,0.1)' }}
                title="Delete Link"
              >
                <Trash2 size={18} />
              </button>
            </div>
          </motion.div>
        ))}
        {links.length === 0 && (
          <div className="text-center text-secondary mt-8">No links found. Create one above!</div>
        )}
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
              <h3 className="mb-4">Delete Link?</h3>
              <p className="mb-6 text-secondary">Are you sure you want to delete this link? This action cannot be undone.</p>
              <div className="flex justify-between gap-4">
                <button onClick={() => setDeleteModal({ show: false, linkId: null })} className="btn btn-secondary" style={{ flex: 1 }}>
                  Cancel
                </button>
                <button onClick={confirmDelete} className="btn btn-danger" style={{ flex: 1 }}>
                  Delete
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

export default Dashboard;
