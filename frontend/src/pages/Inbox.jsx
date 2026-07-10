import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { MessageSquare, Trash2 } from 'lucide-react';
import api from '../api';

const Inbox = () => {
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchMessages = async () => {
      try {
        const { data } = await api.get('/messages');
        setMessages(data);
      } catch (err) {
        console.error(err);
      } finally {
        setLoading(false);
      }
    };
    fetchMessages();
  }, []);

  const handleDelete = async (id) => {
    try {
      await api.delete(`/messages/${id}`);
      setMessages(messages.filter(msg => msg.id !== id));
    } catch (err) {
      alert('Failed to delete message');
    }
  };

  if (loading) return <div className="text-center mt-8 text-secondary">Loading messages...</div>;

  return (
    <div style={{ paddingBottom: '4rem', maxWidth: '800px', margin: '0 auto' }}>
      <h1 className="mb-6 flex items-center gap-4">
        <MessageSquare size={32} color="var(--accent-color)" /> Inbox
      </h1>
      
      <div className="flex-col gap-4">
        {messages.map((msg, idx) => (
          <motion.div 
            key={msg.id}
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: idx * 0.05 }}
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
        {messages.length === 0 && (
          <div className="text-center text-secondary mt-8 card glass">
            No messages yet. When someone leaves a public message on your profile, it will appear here.
          </div>
        )}
      </div>
    </div>
  );
};

export default Inbox;
