import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Link2, LogOut, Settings, LayoutDashboard, Shield, Menu, X, User, Mail } from 'lucide-react';
import api from '../api';

const Navbar = () => {
  const navigate = useNavigate();
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const user = JSON.parse(localStorage.getItem('user') || 'null');

  const handleLogout = async () => {
    try {
      await api.post('/auth/logout');
    } catch (e) {}
    localStorage.removeItem('user');
    navigate('/login');
  };

  const toggleMenu = () => setIsMobileMenuOpen(!isMobileMenuOpen);
  const closeMenu = () => setIsMobileMenuOpen(false);

  return (
    <nav className="navbar glass" style={{ padding: '0 20px', height: '64px' }}>
      <Link to="/" className="nav-brand" onClick={closeMenu}>
        <Link2 size={24} color="var(--accent-color)" />
        Linkly
      </Link>
      
      <button className="mobile-menu-btn" onClick={toggleMenu}>
        {isMobileMenuOpen ? <X size={24} /> : <Menu size={24} />}
      </button>

      <div className={`nav-links ${isMobileMenuOpen ? 'open' : ''}`}>
        {user ? (
          <>
            {user.role === 'ROLE_ADMIN' && (
              <Link to="/admin" className="btn btn-secondary" onClick={closeMenu}>
                <Shield size={18} /> Admin
              </Link>
            )}
            <Link to="/dashboard" className="btn btn-secondary" onClick={closeMenu}>
              <LayoutDashboard size={18} /> Dashboard
            </Link>
            <Link to={`/p/${user.username}`} target="_blank" className="btn btn-secondary" onClick={closeMenu}>
              <User size={18} /> Profile
            </Link>
            <Link to="/inbox" className="btn btn-secondary" onClick={closeMenu}>
              <Mail size={18} /> Inbox
            </Link>
            <Link to="/settings" className="btn btn-secondary" onClick={closeMenu}>
              <Settings size={18} /> Settings
            </Link>
            <button onClick={() => { handleLogout(); closeMenu(); }} className="btn btn-secondary" style={{ color: 'var(--danger-color)' }}>
              <LogOut size={18} /> Logout
            </button>
          </>
        ) : (
          <>
            <Link to="/login" className="btn btn-secondary" onClick={closeMenu}>Login</Link>
            <Link to="/register" className="btn btn-primary" onClick={closeMenu}>Sign Up</Link>
          </>
        )}
      </div>
    </nav>
  );
};

export default Navbar;
