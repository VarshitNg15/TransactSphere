import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../api/axiosConfig';
import { Eye, EyeOff } from 'lucide-react';
import './Auth.css';

const ForgotPassword = () => {
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    newPassword: ''
  });
  const [showPassword, setShowPassword] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  
  const navigate = useNavigate();

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');
    setLoading(true);
    try {
      await api.post('/auth/forgot-password', formData);
      setMessage('Password reset successfully. You can now login.');
      setTimeout(() => navigate('/login'), 2000);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to reset password. Check credentials.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="glass-panel auth-card">
        <h2 className="auth-title">Reset Password</h2>
        <p className="auth-subtitle">Enter details to reset your password</p>
        
        {error && <div className="auth-error">{error}</div>}
        {message && <div className="auth-success" style={{color: '#10b981', textAlign: 'center', marginBottom: '15px'}}>{message}</div>}
        
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Username</label>
            <input type="text" name="username" className="input-field" onChange={handleChange} required />
          </div>
          <div className="form-group">
            <label>Email</label>
            <input type="email" name="email" className="input-field" onChange={handleChange} required />
          </div>
          <div className="form-group" style={{ position: 'relative' }}>
            <label>New Password</label>
            <input 
              type={showPassword ? "text" : "password"} 
              name="newPassword" 
              className="input-field" 
              onChange={handleChange} 
              required 
            />
            <button 
              type="button"
              className="password-toggle"
              onClick={() => setShowPassword(!showPassword)}
              style={{ position: 'absolute', right: '10px', top: '35px', background: 'none', border: 'none', color: '#9ca3af', cursor: 'pointer' }}
            >
              {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
            </button>
          </div>
          <button type="submit" className="auth-button" disabled={loading}>
            {loading ? 'Resetting...' : 'Reset Password'}
          </button>
        </form>
        <div className="auth-footer">
          Remember your password? <Link to="/login">Login</Link>
        </div>
      </div>
    </div>
  );
};

export default ForgotPassword;
