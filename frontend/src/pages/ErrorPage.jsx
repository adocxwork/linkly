import React from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { AlertCircle } from 'lucide-react';

const MESSAGES = {
  link: {
    title: 'Link Unavailable',
    text: 'This link does not exist, was disabled by its owner, or the account is suspended.',
  },
  notFound: {
    title: 'Page Not Found',
    text: "The page you're looking for doesn't exist.",
  },
};

const ErrorPage = ({ variant = 'link' }) => {
  const { title, text } = MESSAGES[variant] || MESSAGES.link;
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
      <h2 className="mb-4">{title}</h2>
      <p className="text-secondary mb-8">
        {text}
      </p>
      <Link to="/" className="btn btn-primary" style={{ display: 'inline-flex', padding: '12px 24px' }}>
        Return to Linkly
      </Link>
    </motion.div>
  );
};

export default ErrorPage;
