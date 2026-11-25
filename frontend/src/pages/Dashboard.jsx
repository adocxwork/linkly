import React, { useState, useEffect, Suspense, lazy } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Copy, Check, Trash2, Power, Plus, GripVertical, BarChart2, Link2, MousePointer2, ArrowUpRight } from 'lucide-react';
import { DragDropContext, Droppable, Draggable } from '@hello-pangea/dnd';
import { z } from 'zod';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import api, { errorMessage, shortLinkUrl } from '../api';
// Charts library is only downloaded when someone opens analytics.
const AnalyticsModal = lazy(() => import('../components/AnalyticsModal'));

const linkSchema = z.object({
  title: z.string().min(1, 'Title is required').max(100),
  originalUrl: z.string().url('Must be a valid URL').max(2048).regex(/^https?:\/\//i, 'Must start with http:// or https://'),
  customAlias: z.string().regex(/^([a-zA-Z0-9_-]{3,30})?$/, '3-30 letters, numbers, dashes or underscores').optional().or(z.literal('')),
});
const Dashboard = () => {
  const [links, setLinks] = useState([]);
  const [stats, setStats] = useState({ totalLinks: 0, totalClicks: 0 });
  const [copiedId, setCopiedId] = useState(null);
  const [deleteModal, setDeleteModal] = useState({ show: false, linkId: null });
  const [analyticsModal, setAnalyticsModal] = useState({ show: false, link: null });
  const [errorMsg, setErrorMsg] = useState('');
  const [loading, setLoading] = useState(true);
  const [loadFailed, setLoadFailed] = useState(false);

  const { register, handleSubmit, formState: { errors, isSubmitting }, reset } = useForm({
    resolver: zodResolver(linkSchema),
    defaultValues: { title: '', originalUrl: '', customAlias: '' }
  });

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    setLoadFailed(false);
    try {
      const [linksRes, statsRes] = await Promise.all([
        api.get('/links'),
        api.get('/links/dashboard')
      ]);
      setLinks(linksRes.data);
      setStats(statsRes.data);
    } catch (err) {
      setLoadFailed(true);
      showError(errorMessage(err, 'Could not load your links'));
    } finally {
      setLoading(false);
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
      showError(errorMessage(err, 'Error adding link'));
    }
  };

  const toggleLink = async (id, currentStatus, title, originalUrl) => {
    try {
      await api.put(`/links/${id}`, { title, originalUrl, active: !currentStatus });
      fetchData();
    } catch (err) {
      showError(errorMessage(err, 'Error updating link'));
    }
  };

  const confirmDelete = async () => {
    try {
      await api.delete(`/links/${deleteModal.linkId}`);
      setDeleteModal({ show: false, linkId: null });
      fetchData();
    } catch (err) {
      showError(errorMessage(err, 'Error deleting link'));
    }
  };

  const copyToClipboard = async (shortUrl, id) => {
    try {
      await navigator.clipboard.writeText(shortLinkUrl(shortUrl));
      setCopiedId(id);
      setTimeout(() => setCopiedId((current) => current === id ? null : current), 2000);
    } catch {
      showError('Copy failed. You can select and copy the short URL below.');
    }
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
    } catch {
      showError('Failed to reorder');
      fetchData(); // revert on failure
    }
  };

  return (
    <div style={{ paddingBottom: '4rem' }}>
      <header className="page-header">
        <h1>Dashboard</h1>
        <p>Manage your links and track how people use them.</p>
      </header>
      
      {errorMsg && (
        <div className="toast error mb-4 text-center" style={{ position: 'fixed', top: '20px', left: '50%', transform: 'translateX(-50%)', zIndex: 9999 }}>
          {errorMsg}
        </div>
      )}

      <div className="dashboard-stats">
        <div className="card stat-card">
          <span className="feature-icon"><Link2 size={20} aria-hidden="true" /></span>
          <div><p>Total links</p><strong>{loading || loadFailed ? '—' : stats.totalLinks}</strong></div>
        </div>
        <div className="card stat-card">
          <span className="feature-icon"><MousePointer2 size={20} aria-hidden="true" /></span>
          <div><p>Total clicks</p><strong>{loading || loadFailed ? '—' : stats.totalClicks}</strong></div>
        </div>
      </div>

      <div className="card glass mb-8">
        <h2 className="section-title mb-4">Create a short link</h2>
        <form onSubmit={handleSubmit(onSubmit)} className="flex-col gap-4">
          <div className="create-link-fields">
            <div>
              <label className="form-label" htmlFor="link-title">Link name</label>
              <input id="link-title" type="text" placeholder="Engineering notes" className="form-control"
                {...register('title')}
              />
              {errors.title && <p className="text-danger" style={{ fontSize: '0.8rem', marginTop: '4px' }}>{errors.title.message}</p>}
            </div>
            <div>
              <label className="form-label" htmlFor="link-originalUrl">Destination URL</label>
              <input id="link-originalUrl" type="url" placeholder="https://example.com" className="form-control"
                {...register('originalUrl')}
              />
              {errors.originalUrl && <p className="text-danger" style={{ fontSize: '0.8rem', marginTop: '4px' }}>{errors.originalUrl.message}</p>}
            </div>
            <div>
              <label className="form-label" htmlFor="link-customAlias">Custom alias (optional)</label>
              <input id="link-customAlias" type="text" placeholder="engineering-notes" className="form-control"
                {...register('customAlias')}
              />
              {errors.customAlias && <p className="text-danger" style={{ fontSize: '0.8rem', marginTop: '4px' }}>{errors.customAlias.message}</p>}
            </div>
            <button type="submit" className="btn btn-primary create-link-submit" disabled={isSubmitting}>
              <Plus size={18} aria-hidden="true" /> {isSubmitting ? 'Creating…' : 'Create link'}
            </button>
          </div>
        </form>
      </div>

      <h2 className="section-title mb-4">Your links</h2>
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
                      className="card link-row"
                      style={{ 
                        padding: '20px', 
                        opacity: link.active ? 1 : 0.6,
                        marginBottom: '1rem',
                        ...provided.draggableProps.style,
                        boxShadow: snapshot.isDragging ? '0 10px 20px rgba(0,0,0,0.1)' : undefined
                      }}
                    >
                      <div className="flex items-center gap-4 link-description">
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
                          <div className="link-meta">
                            {link.active ? (
                              <a className="short-link" href={shortLinkUrl(link.shortUrl)} target="_blank" rel="noopener noreferrer">
                                {shortLinkUrl(link.shortUrl).replace(/^https?:\/\//, '')} <ArrowUpRight size={15} aria-hidden="true" />
                              </a>
                            ) : <span className="text-secondary">{link.shortUrl}</span>}
                            <span className="text-secondary" style={{ fontSize: '0.85rem' }}>{link.clickCount} clicks</span>
                          </div>
                        </div>
                      </div>
                      
                      <div className="flex items-center gap-2">
                        <button 
                          onClick={() => setAnalyticsModal({ show: true, link })} 
                          className="btn btn-secondary link-action"
                          title="View Analytics"
                        >
                          <BarChart2 size={17} aria-hidden="true" /> Analytics
                        </button>

                        <button 
                          onClick={() => copyToClipboard(link.shortUrl, link.id)} 
                          className="btn btn-secondary link-action"
                          title="Copy Link"
                        >
                          {copiedId === link.id ? <><Check size={17} aria-hidden="true" /> Copied</> : <><Copy size={17} aria-hidden="true" /> Copy</>}
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
              {loading && <p role="status" className="card empty-state">Loading your links…</p>}
              {!loading && loadFailed && (
                <div className="card empty-state">
                  <p>Couldn’t load your links.</p>
                  <button type="button" className="btn btn-secondary" onClick={fetchData}>Try again</button>
                </div>
              )}
              {!loading && !loadFailed && links.length === 0 && (
                <p className="card empty-state">Your short links will appear here. Create one above to get started.</p>
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

      {analyticsModal.show && (
        <Suspense fallback={null}>
          <AnalyticsModal
            link={analyticsModal.link}
            onClose={() => setAnalyticsModal({ show: false, link: null })}
          />
        </Suspense>
      )}
    </div>
  );
};

export default Dashboard;
