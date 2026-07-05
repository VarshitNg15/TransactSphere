import React, { useEffect, useState } from 'react';
import api from '../api/axiosConfig';
import { CreditCard, Bell, MessageSquare } from 'lucide-react';
import './Dashboard.css';

const Dashboard = () => {
  const [profile, setProfile] = useState(null);
  const [accounts, setAccounts] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  
  const [feedbackOpen, setFeedbackOpen] = useState(false);
  const [feedbackForm, setFeedbackForm] = useState({ subject: '', message: '' });
  const [feedbackSubmitting, setFeedbackSubmitting] = useState(false);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [profileRes, accountsRes, notificationsRes] = await Promise.all([
          api.get('/users/profile'),
          api.get('/accounts'),
          api.get('/notifications').catch(() => ({ data: [] }))
        ]);
        setProfile(profileRes.data);
        setAccounts(accountsRes.data);
        if (Array.isArray(notificationsRes.data)) {
          setNotifications(notificationsRes.data.slice(0, 5));
        }
      } catch (err) {
        console.error('Failed to load dashboard data', err);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  const createAccount = async (type) => {
    try {
      await api.post('/accounts', { accountType: type });
      const res = await api.get('/accounts');
      setAccounts(res.data);
      alert(`${type} account created successfully!`);
    } catch (err) {
      console.error('Failed to create account', err);
      alert(err.response?.data?.message || 'Failed to create account');
    }
  };

  const handleFeedbackSubmit = async (e) => {
    e.preventDefault();
    setFeedbackSubmitting(true);
    try {
      await api.post('/users/feedback', feedbackForm);
      alert('Feedback submitted successfully. Thank you!');
      setFeedbackForm({ subject: '', message: '' });
      setFeedbackOpen(false);
    } catch (err) {
      console.error(err);
      alert('Failed to submit feedback.');
    } finally {
      setFeedbackSubmitting(false);
    }
  };

  if (loading) return <div className="loader"></div>;

  return (
    <div className="dashboard-container" style={{ position: 'relative' }}>
      <header className="dashboard-header glass-panel" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h1 style={{ fontSize: '2rem', marginBottom: '5px' }}>Welcome back, <span style={{ color: 'var(--accent)' }}>{profile?.firstName || profile?.username || 'User'}</span>!</h1>
          <p style={{ color: '#9ca3af' }}>Here is your financial summary.</p>
        </div>
        <button className="auth-button" onClick={() => setFeedbackOpen(true)} style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <MessageSquare size={18} /> Submit Feedback
        </button>
      </header>

      <div className="grid-container" style={{ marginTop: '10px' }}>
        <div className="glass-panel accounts-panel">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
            <h2 style={{ display: 'flex', alignItems: 'center', gap: '10px' }}><CreditCard size={24} color="var(--accent)" /> My Accounts</h2>
            <div style={{ display: 'flex', gap: '10px' }}>
              <button className="auth-button" style={{ padding: '8px 12px', fontSize: '12px' }} onClick={() => createAccount('SAVINGS')}>+ Savings</button>
              <button className="auth-button" style={{ padding: '8px 12px', fontSize: '12px', background: 'transparent', border: '1px solid var(--accent)' }} onClick={() => createAccount('CURRENT')}>+ Current</button>
            </div>
          </div>
          
          {accounts.length === 0 ? (
            <p style={{ color: '#9ca3af', fontStyle: 'italic' }}>You don't have any accounts yet. Create one to get started!</p>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
              {accounts.map(acc => (
                <div key={acc.accountNumber} style={{ background: 'rgba(255,255,255,0.05)', padding: '20px', borderRadius: '15px', border: '1px solid rgba(255,255,255,0.1)', position: 'relative' }}>
                  <div style={{ fontSize: '12px', textTransform: 'uppercase', color: '#9ca3af', letterSpacing: '1px' }}>{acc.accountType}</div>
                  <div style={{ fontSize: '1.2rem', fontFamily: 'monospace', margin: '10px 0', letterSpacing: '2px' }}>{acc.accountNumber}</div>
                  <div style={{ fontSize: '2rem', fontWeight: 'bold', color: 'var(--accent)' }}>
                    ₹{parseFloat(acc.balance).toFixed(2)}
                  </div>
                  {acc.frozen && <span className="status-badge fraudulent" style={{ position: 'absolute', top: '20px', right: '20px' }}>Frozen</span>}
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="glass-panel notifications-panel">
          <h2 style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '20px' }}><Bell size={24} color="var(--warning)" /> Recent Notifications</h2>
          {notifications.length === 0 ? (
            <p style={{ color: '#9ca3af', fontStyle: 'italic' }}>No new notifications.</p>
          ) : (
            <ul style={{ listStyle: 'none', display: 'flex', flexDirection: 'column', gap: '15px' }}>
              {notifications.map((notif, idx) => (
                <li key={idx} style={{ display: 'flex', gap: '15px', padding: '15px', background: 'rgba(255,255,255,0.02)', borderRadius: '10px', borderLeft: '4px solid var(--accent)' }}>
                  <Bell size={20} color="var(--accent)" style={{ marginTop: '3px' }} />
                  <div>
                    <p style={{ marginBottom: '5px', fontSize: '0.95rem' }}>{notif.message}</p>
                    <small style={{ color: '#64748b' }}>{new Date(notif.timestamp).toLocaleString()}</small>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>

      {feedbackOpen && (
        <div className="modal-overlay" onClick={() => setFeedbackOpen(false)} style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, 
          background: 'rgba(0,0,0,0.8)', zIndex: 1000, display: 'flex', justifyContent: 'center', alignItems: 'center'
        }}>
          <div className="modal-content glass-panel" onClick={e => e.stopPropagation()} style={{ width: '400px', position: 'relative' }}>
            <button onClick={() => setFeedbackOpen(false)} style={{ position: 'absolute', right: '15px', top: '15px', background: 'var(--danger)', padding: '5px 10px', fontSize: '12px' }}>Close</button>
            <h3 style={{ marginBottom: '20px' }}>Submit Feedback</h3>
            <form onSubmit={handleFeedbackSubmit}>
              <div className="form-group">
                <label>Subject</label>
                <input 
                  type="text" 
                  className="input-field" 
                  value={feedbackForm.subject}
                  onChange={e => setFeedbackForm({...feedbackForm, subject: e.target.value})}
                  required 
                  maxLength={50}
                />
              </div>
              <div className="form-group">
                <label>Message</label>
                <textarea 
                  className="input-field" 
                  value={feedbackForm.message}
                  onChange={e => setFeedbackForm({...feedbackForm, message: e.target.value})}
                  required 
                  rows={5}
                />
              </div>
              <button type="submit" className="auth-button" disabled={feedbackSubmitting}>
                {feedbackSubmitting ? 'Submitting...' : 'Submit'}
              </button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Dashboard;
