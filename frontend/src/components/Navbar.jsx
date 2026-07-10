import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Link2, LogOut, Settings, LayoutDashboard, Shield } from 'lucide-react';

const Navbar = () => {
  const navigate = useNavigate();
  const token = localStorage.getItem('token');
  const user = JSON.parse(localStorage.getItem('user') || 'null');

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    navigate('/login');
  };

  return (
    <nav className="navbar glass" style={{ padding: '0 20px', height: '64px' }}>
      <Link to="/" className="nav-brand">
        <Link2 size={24} color="var(--accent-color)" />
        Linkly
      </Link>
      <div className="nav-links">
        {token && user ? (
          <>
            {user.role === 'ROLE_ADMIN' && (
              <Link to="/admin" className="btn btn-secondary">
                <Shield size={18} /> Admin
              </Link>
            )}
            <Link to="/dashboard" className="btn btn-secondary">
              <LayoutDashboard size={18} /> Dashboard
            </Link>
            <Link to={`/p/${user.username}`} target="_blank" className="btn btn-secondary">
              Profile
            </Link>
            <Link to="/settings" className="btn btn-icon">
              <Settings size={20} />
            </Link>
            <button onClick={handleLogout} className="btn btn-icon text-danger">
              <LogOut size={20} />
            </button>
          </>
        ) : (
          <>
            <Link to="/login" className="btn btn-secondary">Login</Link>
            <Link to="/register" className="btn btn-primary">Sign Up</Link>
          </>
        )}
      </div>
    </nav>
  );
};

export default Navbar;
