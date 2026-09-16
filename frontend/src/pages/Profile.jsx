import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import QRCode from 'react-qr-code';
import api from '../api';

const Profile = () => {
  const { username } = useParams();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  
  const [messageName, setMessageName] = useState('');
  const [messageContent, setMessageContent] = useState('');
  const [messageSuccess, setMessageSuccess] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        const { data } = await api.get(`/public/u/${username}`);
        setProfile(data);
      } catch (err) {
        setError(true);
      } finally {
        setLoading(false);
      }
    };
    fetchProfile();
  }, [username]);

  const showError = (msg) => {
    setErrorMsg(msg);
    setTimeout(() => setErrorMsg(''), 4000);
  };

  const handleMessageSubmit = async (e) => {
    e.preventDefault();
    try {
      await api.post(`/public/u/${username}/messages`, {
        senderName: messageName || 'Anonymous',
        content: messageContent
      });
      setMessageSuccess(true);
      setMessageName('');
      setMessageContent('');
      setTimeout(() => setMessageSuccess(false), 3000);
    } catch (err) {
      showError('Failed to send message.');
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center" style={{ minHeight: '60vh' }}>
        <div style={{
          width: '50px', height: '50px', border: '4px solid rgba(0,113,227,0.2)', 
          borderTopColor: 'var(--accent-color)', borderRadius: '50%',
          animation: 'spin 1s linear infinite'
        }}>
          <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
        </div>
      </div>
    );
  }

  if (error || !profile) {
    return (
      <div className="card glass text-center" style={{ maxWidth: '400px', margin: '4rem auto' }}>
        <h2 className="mb-4">User Not Found</h2>
        <p className="text-secondary mb-6">The profile you are looking for does not exist or has been suspended.</p>
        <Link to="/" className="btn btn-primary">Go Home</Link>
      </div>
    );
  }

  return (
    <div style={{ minHeight: '100vh', width: '100%', padding: '2rem 1rem', background: 'var(--bg-color)', color: 'var(--text-primary)', transition: 'background-color 0.3s ease' }}>
      {errorMsg && (
        <div className="toast error mb-4 text-center" style={{ position: 'fixed', top: '20px', left: '50%', transform: 'translateX(-50%)', zIndex: 9999 }}>
          {errorMsg}
        </div>
      )}
      <motion.div 
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="card glass" 
        style={{ maxWidth: '600px', margin: '0 auto', textAlign: 'center', background: 'var(--surface-color)', borderColor: 'var(--border-color)' }}
      >
        <h1 className="mb-2" style={{ color: 'var(--text-primary)' }}>{profile.name}</h1>
        <div className="text-secondary mb-6" style={{ fontSize: '1.2rem' }}>@{profile.username}</div>
        {profile.bio && <p className="mb-8" style={{ color: 'var(--text-primary)' }}>{profile.bio}</p>}

        <div className="flex-col gap-4">
          {profile.links && profile.links.length > 0 ? (
            profile.links.map(link => (
              <motion.a
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                key={link.id}
                href={`${import.meta.env.VITE_BACKEND_URL}/r/${link.shortUrl}`}
                target="_blank"
                rel="noopener noreferrer"
                className="btn"
                style={{ 
                  padding: '16px 24px', 
                  fontSize: '1.1rem', 
                  borderRadius: 'var(--radius-lg)', 
                  background: 'var(--surface-solid)',
                  color: 'var(--text-primary)',
                  border: '1px solid var(--border-color)',
                  boxShadow: 'var(--shadow-sm)',
                  display: 'flex',
                  justifyContent: 'center',
                  width: '100%'
                }}
              >
                {link.title}
              </motion.a>
            ))
          ) : (
            <p className="text-secondary">No active links found.</p>
          )}
        </div>

        {profile.enableUpiPayment && profile.upiId && (
          <div className="mt-8 flex-col items-center" style={{ borderTop: '1px solid var(--border-color)', paddingTop: '2rem' }}>
            <h3 className="mb-6" style={{ fontSize: '1.1rem', marginTop: 0, color: 'var(--text-secondary)' }}>☕ Buy Me a Coffee</h3>
            <div style={{ background: 'white', padding: '16px', borderRadius: 'var(--radius-lg)', display: 'inline-block', marginBottom: '16px' }}>
              <QRCode value={`upi://pay?pa=${profile.upiId}&pn=${encodeURIComponent(profile.name)}`} size={150} />
            </div>
            <div>
              <a 
                href={`upi://pay?pa=${profile.upiId}&pn=${encodeURIComponent(profile.name)}`}
                className="btn"
                style={{ 
                  padding: '10px 20px', 
                  fontSize: '0.95rem',
                  background: 'var(--surface-solid)',
                  color: 'var(--text-primary)',
                  border: '1px solid var(--border-color)',
                  borderRadius: 'var(--radius-full)'
                }}
              >
                Pay via UPI ({profile.upiId})
              </a>
            </div>
          </div>
        )}

        {profile.enablePublicMessaging && (
          <div className="mt-8 flex-col items-center" style={{ borderTop: '1px solid var(--border-color)', paddingTop: '2rem' }}>
            <h3 className="mb-4" style={{ fontSize: '1.1rem', marginTop: 0, color: 'var(--text-secondary)' }}>Public Messages</h3>
            {messageSuccess ? (
              <div className="toast success mb-4" style={{ position: 'relative', bottom: 0, transform: 'none', display: 'inline-block' }}>
                Message sent successfully!
              </div>
            ) : (
              <form onSubmit={handleMessageSubmit} style={{ width: '100%', maxWidth: '400px', margin: '0 auto', textAlign: 'left' }}>
                <div className="form-group mb-4">
                  <input 
                    type="text" 
                    className="form-control" 
                    placeholder="Your Name (Optional)" 
                    value={messageName}
                    onChange={(e) => setMessageName(e.target.value)}
                    style={{ background: 'var(--surface-solid)', color: 'var(--text-primary)', borderColor: 'var(--border-color)' }}
                  />
                </div>
                <div className="form-group mb-4">
                  <textarea 
                    className="form-control" 
                    placeholder="Leave a message..." 
                    rows="3" 
                    value={messageContent}
                    onChange={(e) => setMessageContent(e.target.value)}
                    required
                    style={{ background: 'var(--surface-solid)', color: 'var(--text-primary)', borderColor: 'var(--border-color)' }}
                  />
                </div>
                <button type="submit" className="btn btn-primary" style={{ width: '100%', padding: '12px' }}>Send Message</button>
              </form>
            )}
          </div>
        )}

        <div className="mt-8" style={{ borderTop: '1px solid var(--border-color)', opacity: 0.7, paddingTop: '2rem' }}>
          <a href="/" style={{ color: 'var(--text-secondary)', textDecoration: 'none', fontSize: '0.9rem' }}>
            Powered by <strong style={{ color: 'var(--accent-color)' }}>Linkly</strong>
          </a>
        </div>
      </motion.div>
    </div>
  );
};

export default Profile;
