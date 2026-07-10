import React, { useEffect, useState } from 'react';
import api from '../api/axiosConfig';
import { CreditCard, Bell, MessageSquare, Plus, PlusCircle, Activity, Eye, EyeOff } from 'lucide-react';
import './Dashboard.css';

const Dashboard = () => {
  const [profile, setProfile] = useState(null);
  const [accounts, setAccounts] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [analytics, setAnalytics] = useState(null);
  const [loading, setLoading] = useState(true);
  const [showAmount, setShowAmount] = useState(false);
  
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
        
        const userProfile = profileRes.data;
        setProfile(userProfile);
        setAccounts(accountsRes.data);
        if (Array.isArray(notificationsRes.data)) {
          setNotifications(notificationsRes.data.slice(0, 5));
        }

        // Fetch user analytics from analytics-service
        if (userProfile && userProfile.id) {
          try {
            const analyticsRes = await api.get(`/analytics/user/${userProfile.id}`);
            setAnalytics(analyticsRes.data);
          } catch (analyticsErr) {
            console.error('Failed to load user analytics', analyticsErr);
          }
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

  if (loading) return <div className="dashboard-container"><div className="loader"></div></div>;

  return (
    <div className="dashboard-container">
      <header className="dashboard-header glass-panel">
        <div>
          <h1>Welcome back, <span style={{ color: 'var(--accent)' }}>{profile?.firstName || profile?.username || 'User'}</span>!</h1>
          <p>Here is your financial summary for today.</p>
        </div>
        <button className="auth-button" onClick={() => setFeedbackOpen(true)} style={{ width: 'auto', display: 'flex', alignItems: 'center', gap: '8px', margin: 0 }}>
          <MessageSquare size={18} /> Feedback
        </button>
      </header>

      {/* Analytics Overview Panel */}
      {analytics && (
        <div className="glass-panel" style={{ display: 'flex', gap: '2rem', padding: '1.5rem 2.5rem' }}>
          <div style={{ flex: 1, borderRight: '1px solid rgba(255,255,255,0.1)' }}>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', textTransform: 'uppercase', display: 'flex', alignItems: 'center', gap: '6px' }}>
              <Activity size={16} color="var(--accent)" /> Total Transaction Volume
            </p>
            <h2 style={{ fontSize: '2rem', fontWeight: 800, marginTop: '0.5rem', display: 'flex', alignItems: 'center', gap: '8px' }}>
              {showAmount ? `₹${parseFloat(analytics.totalVolume || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}` : '₹****'}
              <button onClick={() => setShowAmount(!showAmount)} style={{ background: 'transparent', border: 'none', padding: 0, color: 'var(--text-secondary)', cursor: 'pointer', boxShadow: 'none' }}>
                {showAmount ? <EyeOff size={20} /> : <Eye size={20} />}
              </button>
            </h2>
          </div>
          <div style={{ flex: 1, borderRight: '1px solid rgba(255,255,255,0.1)' }}>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', textTransform: 'uppercase' }}>Deposits</p>
            <h2 style={{ fontSize: '1.8rem', fontWeight: 700, color: 'var(--success)', marginTop: '0.5rem', display: 'flex', alignItems: 'center', gap: '8px' }}>
              {showAmount ? `₹${parseFloat(analytics.depositVolume || 0).toLocaleString('en-IN')}` : '₹****'}
            </h2>
          </div>
          <div style={{ flex: 1, borderRight: '1px solid rgba(255,255,255,0.1)' }}>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', textTransform: 'uppercase' }}>Withdrawals</p>
            <h2 style={{ fontSize: '1.8rem', fontWeight: 700, color: 'var(--text-primary)', marginTop: '0.5rem', display: 'flex', alignItems: 'center', gap: '8px' }}>
              {showAmount ? `₹${parseFloat(analytics.withdrawalVolume || 0).toLocaleString('en-IN')}` : '₹****'}
            </h2>
          </div>
          <div style={{ flex: 1 }}>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', textTransform: 'uppercase' }}>Total Transactions</p>
            <h2 style={{ fontSize: '1.8rem', fontWeight: 700, marginTop: '0.5rem' }}>
              {analytics.totalCount || 0}
            </h2>
          </div>
        </div>
      )}

      <div className="grid-container">
        <div className="glass-panel accounts-panel">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
            <h2><CreditCard size={28} color="var(--accent)" /> My Accounts</h2>
            <div style={{ display: 'flex', gap: '10px' }}>
              <button onClick={() => createAccount('SAVINGS')} style={{ padding: '8px 16px', fontSize: '13px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                <Plus size={16} /> Savings
              </button>
              <button onClick={() => createAccount('CURRENT')} style={{ padding: '8px 16px', fontSize: '13px', background: 'transparent', border: '1px solid var(--accent)', display: 'flex', alignItems: 'center', gap: '6px' }}>
                <PlusCircle size={16} /> Current
              </button>
            </div>
          </div>
          
          {accounts.length === 0 ? (
            <p style={{ color: 'var(--text-secondary)', fontStyle: 'italic', textAlign: 'center', padding: '2rem 0' }}>You don't have any accounts yet. Create one to get started!</p>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
              {accounts.map(acc => (
                <div key={acc.accountNumber} className="account-card">
                  <div className="account-type">{acc.accountType} Account</div>
                  <div className="account-number">{acc.accountNumber}</div>
                  <div className="account-balance" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    {showAmount ? `₹${parseFloat(acc.balance).toLocaleString('en-IN', { minimumFractionDigits: 2 })}` : '₹****'}
                    <button onClick={() => setShowAmount(!showAmount)} style={{ background: 'transparent', border: 'none', padding: 0, color: 'var(--text-secondary)', cursor: 'pointer', boxShadow: 'none' }}>
                      {showAmount ? <EyeOff size={16} /> : <Eye size={16} />}
                    </button>
                  </div>
                  {acc.frozen && <span className="status-badge fraudulent" style={{ position: 'absolute', top: '20px', right: '20px' }}>Frozen</span>}
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="glass-panel notifications-panel">
          <h2><Bell size={28} color="var(--warning)" /> Notifications</h2>
          {notifications.length === 0 ? (
            <p style={{ color: 'var(--text-secondary)', fontStyle: 'italic', textAlign: 'center', padding: '2rem 0' }}>No new notifications right now.</p>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              {notifications.map((notif, idx) => (
                <div key={idx} className="notification-item">
                  <Bell size={24} color="var(--accent)" style={{ flexShrink: 0, marginTop: '2px' }} />
                  <div>
                    <p>{notif.message}</p>
                    <small>{new Date(notif.timestamp).toLocaleString()}</small>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {feedbackOpen && (
        <div className="modal-overlay" onClick={() => setFeedbackOpen(false)}>
          <div className="modal-content glass-panel" onClick={e => e.stopPropagation()}>
            <button onClick={() => setFeedbackOpen(false)} style={{ position: 'absolute', right: '20px', top: '20px', background: 'rgba(255,255,255,0.1)', padding: '6px 12px', fontSize: '13px' }}>Close</button>
            <h3 style={{ marginBottom: '1.5rem', fontSize: '1.5rem', fontFamily: 'Outfit' }}>Submit Feedback</h3>
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
                  placeholder="What is this regarding?"
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
                  placeholder="Tell us your thoughts..."
                  style={{ resize: 'vertical' }}
                />
              </div>
              <button type="submit" style={{ width: '100%', marginTop: '1rem' }} disabled={feedbackSubmitting}>
                {feedbackSubmitting ? 'Sending...' : 'Send Feedback'}
              </button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Dashboard;
