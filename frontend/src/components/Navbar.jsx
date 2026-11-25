import { useRef, useState } from 'react';
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom';
import { ArrowUpRight, LayoutDashboard, Link2, LogOut, Mail, Menu, Settings, Shield, X } from 'lucide-react';
import { readStoredUser } from '../session';

const workspaceRoutes = [
  { to: '/dashboard', label: 'Dashboard', Icon: LayoutDashboard },
  { to: '/inbox', label: 'Inbox', Icon: Mail },
  { to: '/settings', label: 'Settings', Icon: Settings },
];
const adminRoute = { to: '/admin', label: 'Admin', Icon: Shield };

export default function Navbar() {
  const navigate = useNavigate();
  const location = useLocation();
  const [openLocation, setOpenLocation] = useState(null);
  const menuButton = useRef(null);
  const user = localStorage.getItem('token') ? readStoredUser() : null;
  const routes = user?.role === 'ROLE_ADMIN' ? [...workspaceRoutes, adminRoute] : workspaceRoutes;
  const current = user && routes.find(({ to }) => to === location.pathname);
  const menuOpen = openLocation === location.key;
  const closeMenu = () => setOpenLocation(null);

  function handleLogout() {
    localStorage.removeItem('user');
    localStorage.removeItem('token');
    closeMenu();
    navigate('/login', { replace: true });
  }

  return (
    <header className="site-header">
      <a href="#main-content" className="skip-link">Skip to content</a>
      <nav className="site-nav" aria-label="Main navigation" onKeyDown={(event) => {
        if (event.key === 'Escape' && menuOpen) {
          closeMenu();
          menuButton.current?.focus();
        }
      }}>
        <Link to="/" className="site-brand" onClick={closeMenu}>
          <span className="brand-mark"><Link2 size={21} aria-hidden="true" /></span>
          Linkly
        </Link>

        {current && <span className="mobile-current">{current.label}</span>}
        <button type="button" className="nav-menu-button" ref={menuButton}
          aria-label={menuOpen ? 'Close navigation' : 'Open navigation'}
          aria-expanded={menuOpen} aria-controls="site-navigation"
          onClick={() => setOpenLocation(menuOpen ? null : location.key)}>
          {menuOpen ? <X size={22} aria-hidden="true" /> : <Menu size={22} aria-hidden="true" />}
        </button>

        <div id="site-navigation" className={`site-navigation${menuOpen ? ' is-open' : ''}${!user ? ' is-public' : ''}`}>
          {user ? (
            <>
              <div className="workspace-nav">
                {routes.map(({ to, Icon, label }) => (
                  <NavLink key={to} to={to} end className="nav-item" onClick={closeMenu}>
                    <Icon size={17} aria-hidden="true" /> {label}
                  </NavLink>
                ))}
              </div>
              <div className="nav-actions">
                <a href={`/p/${encodeURIComponent(user.username)}`} target="_blank" rel="noopener noreferrer"
                  className="nav-profile" aria-label="View public profile (opens in a new tab)" onClick={closeMenu}>
                  View public profile <ArrowUpRight size={17} aria-hidden="true" />
                </a>
                <button type="button" className="nav-item nav-signout" onClick={handleLogout}>
                  <LogOut size={16} aria-hidden="true" /> Sign out
                </button>
              </div>
            </>
          ) : (
            <div className="nav-actions">
              <NavLink to="/login" end className="nav-item" onClick={closeMenu}>Sign in</NavLink>
              <NavLink to="/register" end className="btn btn-primary nav-register" onClick={closeMenu}>Create account</NavLink>
            </div>
          )}
        </div>
      </nav>
    </header>
  );
}
