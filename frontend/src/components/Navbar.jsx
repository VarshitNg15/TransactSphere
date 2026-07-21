import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { useTheme } from '../contexts/ThemeContext';
import { Sun, Moon, LogOut, Wallet } from 'lucide-react';
import './Navbar.css';

const Navbar = () => {
  const { isAuthenticated, isAdmin, logout } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <nav className="navbar glass-panel">
      <div className="nav-brand">
        <Link to="/" className="brand-link">
          <Wallet className="brand-icon" />
          <span>TransactSphere</span>
        </Link>
      </div>
      <div className="nav-links">
        <button className="icon-button" onClick={toggleTheme} aria-label="Toggle Theme">
          {theme === 'light' ? <Moon size={20} /> : <Sun size={20} />}
        </button>
        {isAuthenticated ? (
          <>
            {isAdmin ? (
              <Link to="/admin" className="nav-item">Admin Dashboard</Link>
            ) : (
              <>
                <Link to="/dashboard" className="nav-item">Dashboard</Link>
                <Link to="/profile" className="nav-item">Profile</Link>
                <Link to="/transactions" className="nav-item">Transactions</Link>
                <Link to="/statements" className="nav-item">Statements</Link>
              </>
            )}
            <button className="icon-button" onClick={handleLogout} aria-label="Logout">
              <LogOut size={20} />
            </button>
          </>
        ) : (
          <>
            <Link to="/login" className="nav-item">Login</Link>
            <Link to="/register" className="nav-item">Register</Link>
          </>
        )}
      </div>
    </nav>
  );
};

export default Navbar;
