import React from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { AlertCircle } from 'lucide-react';

const ErrorPage = () => {
  return (
    <motion.div 
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      className="card glass text-center" 
      style={{ maxWidth: '450px', margin: '4rem auto', padding: '40px' }}
    >
      <div className="flex justify-center mb-6">
        <AlertCircle size={48} color="var(--danger-color)" />
      </div>
      <h2 className="mb-4">Link Unavailable</h2>
      <p className="text-secondary mb-8">
        The link you clicked has been disabled by the user, or the account is currently suspended.
      </p>
      <Link to="/" className="btn btn-primary" style={{ display: 'inline-flex', padding: '12px 24px' }}>
        Return to Linkly
      </Link>
    </motion.div>
  );
};

export default ErrorPage;
