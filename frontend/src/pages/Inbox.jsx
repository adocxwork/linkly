import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { MessageSquare, Trash2 } from 'lucide-react';
import api, { errorMessage } from '../api';

const Inbox = () => {
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [errorMsg, setErrorMsg] = useState('');
  const [cursor, setCursor] = useState(null);
  const [loadingMore, setLoadingMore] = useState(false);

  const pageCursor = (page) => {
    if (page.length < 200) return null;
    const last = page.at(-1);
    return { before: last.createdAt, beforeId: last.id };
  };

  useEffect(() => {
    const controller = new AbortController();
    api.get('/messages', { signal: controller.signal })
      .then(({ data }) => {
        setMessages(data);
        setCursor(pageCursor(data));
      })
      .catch((error) => {
        if (!controller.signal.aborted) setErrorMsg(errorMessage(error, 'Failed to load messages'));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, []);

  const loadOlder = async () => {
    setLoadingMore(true);
    setErrorMsg('');
    try {
      const { data } = await api.get('/messages', { params: cursor });
      setMessages((current) => [...current, ...data]);
      setCursor(pageCursor(data));
    } catch (error) {
      setErrorMsg(errorMessage(error, 'Failed to load older messages'));
    } finally {
      setLoadingMore(false);
    }
  };

  const handleDelete = async (id) => {
    try {
      await api.delete(`/messages/${id}`);
      setMessages((current) => current.filter((message) => message.id !== id));
    } catch {
      setErrorMsg('Failed to delete message');
    }
  };

  if (loading) return <div className="text-center mt-8 text-secondary">Loading messages...</div>;

  return (
    <div style={{ paddingBottom: '4rem', maxWidth: '800px', margin: '0 auto' }}>
      <h1 className="mb-6 flex items-center gap-4">
        <MessageSquare size={32} color="var(--accent-color)" /> Inbox
      </h1>
      
      {errorMsg && (
        <div className="toast error mb-4 text-center" style={{ position: 'fixed', top: '20px', left: '50%', transform: 'translateX(-50%)', zIndex: 9999 }}>
          {errorMsg}
        </div>
      )}

      <div className="flex-col gap-4">
        {messages.map((msg, idx) => (
          <motion.div 
            key={msg.id}
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: Math.min(idx * 0.015, 0.2) }}
            className="card glass"
            style={{ padding: '20px' }}
          >
            <div className="flex justify-between items-center mb-2">
              <h4 style={{ margin: 0, fontWeight: 600 }}>{msg.senderName}</h4>
              <div className="flex items-center gap-3">
                <span className="text-secondary" style={{ fontSize: '0.85rem' }}>
                  {new Date(msg.createdAt).toLocaleString()}
                </span>
                <button 
                  onClick={() => handleDelete(msg.id)}
                  className="btn btn-icon text-danger"
                  style={{ background: 'transparent', border: 'none', padding: '4px' }}
                  title="Delete message"
                >
                  <Trash2 size={16} />
                </button>
              </div>
            </div>
            <p style={{ margin: 0, whiteSpace: 'pre-wrap', color: 'var(--text-primary)' }}>{msg.content}</p>
          </motion.div>
        ))}
        {messages.length === 0 && !errorMsg && (
          <div className="text-center text-secondary mt-8 card glass">
            No messages yet. When someone leaves a public message on your profile, it will appear here.
          </div>
        )}
      </div>
      {cursor && (
        <button type="button" className="btn btn-secondary mt-6" disabled={loadingMore} onClick={loadOlder}>
          {loadingMore ? 'Loading…' : 'Load older messages'}
        </button>
      )}
    </div>
  );
};

export default Inbox;
