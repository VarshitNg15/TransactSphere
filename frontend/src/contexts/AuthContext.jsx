import React, { createContext, useState, useEffect, useContext } from 'react';
import api from '../api/axiosConfig';

const AuthContext = createContext();

export const useAuth = () => useContext(AuthContext);

export const AuthProvider = ({ children }) => {
  const [token, setToken] = useState(() => localStorage.getItem('token') || null);
  const [isAuthenticated, setIsAuthenticated] = useState(!!token);
  
  useEffect(() => {
    if (token) {
      localStorage.setItem('token', token);
      setIsAuthenticated(true);
    } else {
      localStorage.removeItem('token');
      setIsAuthenticated(false);
    }
  }, [token]);

  const login = (newToken, role = null) => {
    localStorage.setItem('token', newToken);
    if (role) localStorage.setItem('role', role);
    setToken(newToken);
    setIsAuthenticated(true);
  };

  const logout = async () => {
    try {
      if (token) {
        await api.post('/auth/logout');
      }
    } catch (e) {
      console.error("Logout API call failed", e);
    } finally {
      localStorage.removeItem('token');
      localStorage.removeItem('role');
      setToken(null);
      setIsAuthenticated(false);
    }
  };

  const isAdmin = localStorage.getItem('role') === 'ADMIN';

  return (
    <AuthContext.Provider value={{ token, isAuthenticated, isAdmin, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};
