import React, { useEffect, useState } from 'react';
import api from '../api/axiosConfig';
import { CreditCard, Bell, ArrowUpRight, ArrowDownRight, RefreshCw } from 'lucide-react';
import './Dashboard.css';

const Dashboard = () => {
  const [profile, setProfile] = useState(null);
  const [accounts, setAccounts] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);

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
          setNotifications(notificationsRes.data.slice(0, 5)); // Last 5
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

  if (loading) return <div className="loader">Loading Dashboard...</div>;

  return (
    <div className="dashboard-container">
      <header className="dashboard-header">
        <h1>Welcome back, {profile?.firstName || profile?.username || 'User'}!</h1>
        <p>Here is your financial summary.</p>
      </header>

      <div className="dashboard-grid">
        <div className="glass-panel accounts-panel">
          <div className="panel-header">
            <h2><CreditCard size={24} /> My Accounts</h2>
            <div className="account-actions">
              <button className="sm-button" onClick={() => createAccount('SAVINGS')}>+ Savings</button>
              <button className="sm-button" onClick={() => createAccount('CURRENT')}>+ Current</button>
            </div>
          </div>
          {accounts.length === 0 ? (
            <p className="empty-state">You don't have any accounts yet. Create one to get started!</p>
          ) : (
            <div className="accounts-list">
              {accounts.map(acc => (
                <div key={acc.accountNumber} className="account-card glass-panel">
                  <div className="account-type">{acc.accountType}</div>
                  <div className="account-number">{acc.accountNumber}</div>
                  <div className="account-balance">
                    <span className="currency">₹</span>
                    {parseFloat(acc.balance).toFixed(2)}
                  </div>
                  {acc.frozen && <span className="frozen-badge">Frozen</span>}
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="glass-panel notifications-panel">
          <div className="panel-header">
            <h2><Bell size={24} /> Recent Notifications</h2>
          </div>
          {notifications.length === 0 ? (
            <p className="empty-state">No new notifications.</p>
          ) : (
            <ul className="notifications-list">
              {notifications.map((notif, idx) => (
                <li key={idx} className="notification-item">
                  <div className="notif-icon">
                    <Bell size={16} />
                  </div>
                  <div className="notif-content">
                    <p>{notif.message}</p>
                    <small>{new Date(notif.timestamp).toLocaleString()}</small>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
