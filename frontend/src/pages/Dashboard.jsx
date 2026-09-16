import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Copy, Check, Trash2, Power, Plus, GripVertical } from 'lucide-react';
import { DragDropContext, Droppable, Draggable } from '@hello-pangea/dnd';
import { z } from 'zod';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import api from '../api';

const linkSchema = z.object({
  title: z.string().min(1, 'Title is required').max(100),
  originalUrl: z.string().url('Must be a valid URL'),
  customAlias: z.string().regex(/^[a-zA-Z0-9-_]*$/, 'Letters, numbers, dashes, underscores only').optional().or(z.literal('')),
});
const Dashboard = () => {
  const [links, setLinks] = useState([]);
  const [stats, setStats] = useState({ totalLinks: 0, totalClicks: 0 });
  const [copiedId, setCopiedId] = useState(null);
  const [deleteModal, setDeleteModal] = useState({ show: false, linkId: null });
  const [errorMsg, setErrorMsg] = useState('');
  const user = JSON.parse(localStorage.getItem('user') || '{}');

  const { register, handleSubmit, formState: { errors }, reset } = useForm({
    resolver: zodResolver(linkSchema),
    defaultValues: { title: '', originalUrl: '', customAlias: '' }
  });

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      const [linksRes, statsRes] = await Promise.all([
        api.get('/links'),
        api.get('/links/dashboard')
      ]);
      setLinks(linksRes.data);
      setStats(statsRes.data);
    } catch (err) {
      console.error(err);
    }
  };

  const showError = (msg) => {
    setErrorMsg(msg);
    setTimeout(() => setErrorMsg(''), 4000);
  };

  const onSubmit = async (data) => {
    try {
      await api.post('/links', { title: data.title, originalUrl: data.originalUrl, customAlias: data.customAlias, active: true });
      reset();
      fetchData();
    } catch (err) {
      showError(err.response?.data?.message || 'Error adding link');
    }
  };

  const toggleLink = async (id, currentStatus, title, originalUrl) => {
    try {
      await api.put(`/links/${id}`, { title, originalUrl, active: !currentStatus });
      fetchData();
    } catch (err) {
      showError(err.response?.data?.message || 'Error updating link');
    }
  };

  const confirmDelete = async () => {
    try {
      await api.delete(`/links/${deleteModal.linkId}`);
      setDeleteModal({ show: false, linkId: null });
      fetchData();
    } catch (err) {
      showError(err.response?.data?.message || 'Error deleting link');
    }
  };

  const copyToClipboard = (shortUrl, id) => {
    const baseUrl = import.meta.env.VITE_BACKEND_URL || window.location.origin;
    const fullUrl = `${baseUrl}/r/${shortUrl}`;
    navigator.clipboard.writeText(fullUrl);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 2000);
  };

  const handleDragEnd = async (result) => {
    if (!result.destination) return;
    
    const sourceIndex = result.source.index;
    const destinationIndex = result.destination.index;
    
    if (sourceIndex === destinationIndex) return;

    const newLinks = Array.from(links);
    const [movedLink] = newLinks.splice(sourceIndex, 1);
    newLinks.splice(destinationIndex, 0, movedLink);
    
    setLinks(newLinks);
    
    try {
      await api.put('/links/reorder', {
        linkIds: newLinks.map(l => l.id)
      });
    } catch (err) {
      showError('Failed to reorder');
      fetchData(); // revert on failure
    }
  };

  return (
    <div style={{ paddingBottom: '4rem' }}>
      <h1 className="mb-6">Hello, {user.name}</h1>
      
      {errorMsg && (
        <div className="toast error mb-4 text-center" style={{ position: 'fixed', top: '20px', left: '50%', transform: 'translateX(-50%)', zIndex: 9999 }}>
          {errorMsg}
        </div>
      )}

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
        <form onSubmit={handleSubmit(onSubmit)} className="flex-col gap-4">
          <div className="flex gap-4 items-start flex-wrap">
            <div style={{ flex: '1 1 200px' }}>
              <input 
                type="text" 
                placeholder="Title (e.g. My Twitter)" 
                className="form-control" 
                {...register('title')}
              />
              {errors.title && <p className="text-danger" style={{ fontSize: '0.8rem', marginTop: '4px' }}>{errors.title.message}</p>}
            </div>
            <div style={{ flex: '2 1 300px' }}>
              <input 
                type="url" 
                placeholder="https://example.com" 
                className="form-control" 
                {...register('originalUrl')}
              />
              {errors.originalUrl && <p className="text-danger" style={{ fontSize: '0.8rem', marginTop: '4px' }}>{errors.originalUrl.message}</p>}
            </div>
            <div style={{ flex: '1 1 200px' }}>
              <input 
                type="text" 
                placeholder="Custom Alias (optional)" 
                className="form-control" 
                {...register('customAlias')}
              />
              {errors.customAlias && <p className="text-danger" style={{ fontSize: '0.8rem', marginTop: '4px' }}>{errors.customAlias.message}</p>}
            </div>
            <button type="submit" className="btn btn-primary" style={{ height: '48px', flexShrink: 0 }}>
              <Plus size={18} /> Add
            </button>
          </div>
        </form>
      </div>

      <DragDropContext onDragEnd={handleDragEnd}>
        <Droppable droppableId="links">
          {(provided) => (
            <div 
              className="flex-col gap-4"
              {...provided.droppableProps}
              ref={provided.innerRef}
            >
              {links.map((link, index) => (
                <Draggable key={link.id} draggableId={link.id} index={index}>
                  {(provided, snapshot) => (
                    <div
                      ref={provided.innerRef}
                      {...provided.draggableProps}
                      className="card glass flex items-center justify-between"
                      style={{ 
                        padding: '20px', 
                        opacity: link.active ? 1 : 0.6,
                        marginBottom: '1rem',
                        ...provided.draggableProps.style,
                        boxShadow: snapshot.isDragging ? '0 10px 20px rgba(0,0,0,0.1)' : undefined
                      }}
                    >
                      <div className="flex items-center gap-4">
                        <div 
                          {...provided.dragHandleProps} 
                          className="text-secondary" 
                          style={{ cursor: 'grab', padding: '8px', display: 'flex', alignItems: 'center' }}
                        >
                          <GripVertical size={20} />
                        </div>
                        <div>
                          <h4 style={{ marginBottom: '4px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                            {link.title}
                            {!link.active && (
                              <span style={{ fontSize: '0.7rem', background: 'var(--danger-color)', color: 'white', padding: '2px 6px', borderRadius: '4px', fontWeight: 'normal' }}>
                                DISABLED
                              </span>
                            )}
                          </h4>
                          <div className="text-secondary" style={{ fontSize: '0.9rem', marginBottom: '8px', textDecoration: link.active ? 'none' : 'line-through' }}>
                            {link.originalUrl}
                          </div>
                          <div className="flex items-center gap-4">
                            <span style={{ color: 'var(--accent-color)', fontWeight: 500, textDecoration: link.active ? 'none' : 'line-through' }}>
                              /r/{link.shortUrl}
                            </span>
                            <span className="text-secondary" style={{ fontSize: '0.85rem' }}>{link.clickCount} clicks</span>
                          </div>
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
                          onClick={() => toggleLink(link.id, link.active, link.title, link.originalUrl)}
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
                    </div>
                  )}
                </Draggable>
              ))}
              {provided.placeholder}
              {links.length === 0 && (
                <div className="text-center text-secondary mt-8">No links found. Create one above!</div>
              )}
            </div>
          )}
        </Droppable>
      </DragDropContext>

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
