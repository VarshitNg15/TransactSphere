import React, { useState, useEffect } from 'react';
import api from '../api/axiosConfig';
import { Users, MessageSquare, Shield, Lock, Eye, CheckCircle, XCircle, Activity, AlertTriangle, FileText } from 'lucide-react';
import './Dashboard.css';

const AdminDashboard = () => {
  const [activeTab, setActiveTab] = useState('customers');
  const [users, setUsers] = useState([]);
  const [feedbacks, setFeedbacks] = useState([]);
  const [analytics, setAnalytics] = useState(null);
  const [fraudLogs, setFraudLogs] = useState([]);
  const [auditLogs, setAuditLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  
  // KYC Modal State
  const [kycModalOpen, setKycModalOpen] = useState(false);
  const [kycImage, setKycImage] = useState(null);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [usersRes, feedbacksRes, statsRes, fraudRes, auditRes] = await Promise.allSettled([
        api.get('/users'),
        api.get('/users/feedback'),
        api.get('/admin/stats').catch(() => api.get('/analytics/dashboard')),
        api.get('/admin/fraud/logs').catch(() => api.get('/fraud/logs')),
        api.get('/audit/logs')
      ]);

      if (usersRes.status === 'fulfilled') {
        const sortedUsers = usersRes.value.data.sort((a, b) => {
          if (!a.uniqueId || !b.uniqueId) return 0;
          return a.uniqueId.localeCompare(b.uniqueId);
        });
        setUsers(sortedUsers);
      } else {
        setError('Failed to fetch some admin data. Ensure you have Admin privileges.');
      }

      if (feedbacksRes.status === 'fulfilled') setFeedbacks(feedbacksRes.value.data);
      if (statsRes.status === 'fulfilled') setAnalytics(statsRes.value.data);
      if (fraudRes.status === 'fulfilled') setFraudLogs(fraudRes.value.data);
      if (auditRes.status === 'fulfilled') setAuditLogs(auditRes.value.data);
      
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleKycStatus = async (userId, status) => {
    try {
      await api.put(`/users/${userId}/kyc?status=${status}`);
      const res = await api.get('/users');
      setUsers(res.data);
    } catch (err) {
      console.error(err);
      alert('Failed to update KYC status');
    }
  };

  const handleBlockUser = async (userId, isBlocked) => {
    try {
      await api.put(`/auth/users/${userId}/block?block=${!isBlocked}`);
      alert(`User ${!isBlocked ? 'blocked' : 'unblocked'} successfully.`);
      const res = await api.get('/users');
      setUsers(res.data);
    } catch (err) {
      console.error(err);
      alert('Failed to update user block status');
    }
  };

  const handleFreezeAccounts = async (userId, freezeStatus) => {
    try {
      await api.put(`/accounts/user/${userId}/freeze?freeze=${!freezeStatus}`);
      alert(`User's accounts ${!freezeStatus ? 'frozen' : 'unfrozen'} successfully.`);
    } catch (err) {
      console.error(err);
      alert('Failed to update account freeze status');
    }
  };

  const handleResolveFraud = async (logId) => {
    try {
      await api.put(`/admin/fraud/resolve/${logId}`).catch(() => api.put(`/fraud/resolve/${logId}`));
      alert('Fraud alert resolved successfully.');
      const res = await api.get('/admin/fraud/logs').catch(() => api.get('/fraud/logs'));
      setFraudLogs(res.data);
    } catch (err) {
      console.error(err);
      alert('Failed to resolve fraud alert');
    }
  };

  const openKycModal = (imgData) => {
    setKycImage(imgData);
    setKycModalOpen(true);
  };

  const closeKycModal = () => {
    setKycModalOpen(false);
    setKycImage(null);
  };

  if (loading) return <div className="dashboard-container"><div className="loader"></div></div>;

  return (
    <div className="dashboard-container" style={{ display: 'flex', gap: '2rem', alignItems: 'flex-start' }}>
      
      {/* Sidebar */}
      <div className="glass-panel" style={{ width: '280px', padding: '2rem 1.5rem', position: 'sticky', top: '2rem', display: 'flex', flexDirection: 'column', gap: '2rem' }}>
        <div>
          <h3 style={{ fontSize: '1.2rem', fontFamily: 'Outfit', color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '1px', marginBottom: '1rem' }}>Admin Panel</h3>
          <ul style={{ listStyle: 'none', padding: 0, display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {[
              { id: 'analytics', icon: Activity, label: 'System Analytics' },
              { id: 'customers', icon: Users, label: 'Customers & KYC' },
              { id: 'fraud', icon: AlertTriangle, label: 'Fraud Alerts' },
              { id: 'audit', icon: FileText, label: 'Audit Logs' },
              { id: 'feedbacks', icon: MessageSquare, label: 'User Feedbacks' },
            ].map(tab => (
              <li key={tab.id}>
                <button 
                  onClick={() => setActiveTab(tab.id)}
                  style={{ 
                    width: '100%', 
                    background: activeTab === tab.id ? 'var(--accent)' : 'transparent', 
                    color: activeTab === tab.id ? '#fff' : 'var(--text-primary)',
                    border: activeTab === tab.id ? 'none' : '1px solid rgba(255,255,255,0.1)',
                    boxShadow: activeTab === tab.id ? '0 4px 12px var(--accent-glow)' : 'none',
                    display: 'flex', alignItems: 'center', gap: '10px', justifyContent: 'flex-start'
                  }}
                >
                  <tab.icon size={18} /> {tab.label}
                </button>
              </li>
            ))}
          </ul>
        </div>
        
        <div style={{ marginTop: 'auto', padding: '1.5rem', background: 'rgba(99, 102, 241, 0.1)', borderRadius: '16px', border: '1px solid rgba(99, 102, 241, 0.2)' }}>
          <h4 style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '10px', color: 'var(--accent)' }}><Shield size={16} /> System Status</h4>
          <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>All 10 microservices are operating normally. Kafka streams active.</p>
        </div>
      </div>

      {/* Main Content Area */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '2rem' }}>
        <div className="dashboard-header glass-panel">
          <div>
            <h1>Dashboard Overview</h1>
            <p>Total Customers: <strong style={{ color: 'var(--accent)' }}>{users.length}</strong> &nbsp;|&nbsp; Unresolved Alerts: <strong style={{ color: 'var(--danger)' }}>{fraudLogs?.filter(f => !f.resolved).length || 0}</strong></p>
          </div>
        </div>

        {error && <div className="error-message glass-panel" style={{ background: 'rgba(239, 68, 68, 0.1)', border: '1px solid rgba(239, 68, 68, 0.3)' }}>{error}</div>}

        {activeTab === 'analytics' && (
          <div className="glass-panel" style={{ padding: '2rem' }}>
            <h2 style={{ fontSize: '1.5rem', fontFamily: 'Outfit', marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '10px' }}>
              <Activity size={24} color="var(--accent)" /> System Analytics
            </h2>
            {analytics ? (
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '1.5rem' }}>
                <div style={{ background: 'rgba(255,255,255,0.02)', padding: '1.5rem', borderRadius: '12px', border: '1px solid rgba(255,255,255,0.05)' }}>
                  <p style={{ fontSize: '12px', color: 'var(--text-secondary)', textTransform: 'uppercase', marginBottom: '5px' }}>Total Transaction Volume</p>
                  <p style={{ fontSize: '2rem', fontWeight: 800, color: 'var(--accent)' }}>₹{parseFloat(analytics.totalVolume || 0).toLocaleString('en-IN', { maximumFractionDigits: 0 })}</p>
                </div>
                <div style={{ background: 'rgba(255,255,255,0.02)', padding: '1.5rem', borderRadius: '12px', border: '1px solid rgba(255,255,255,0.05)' }}>
                  <p style={{ fontSize: '12px', color: 'var(--text-secondary)', textTransform: 'uppercase', marginBottom: '5px' }}>Total Transactions Processed</p>
                  <p style={{ fontSize: '2rem', fontWeight: 800 }}>{analytics.totalCount || 0}</p>
                </div>
                <div style={{ background: 'rgba(255,255,255,0.02)', padding: '1.5rem', borderRadius: '12px', border: '1px solid rgba(255,255,255,0.05)' }}>
                  <p style={{ fontSize: '12px', color: 'var(--text-secondary)', textTransform: 'uppercase', marginBottom: '5px' }}>Total Active Users</p>
                  <p style={{ fontSize: '2rem', fontWeight: 800 }}>{users.length}</p>
                </div>
                <div style={{ background: 'rgba(255,255,255,0.02)', padding: '1.5rem', borderRadius: '12px', border: '1px solid rgba(255,255,255,0.05)' }}>
                  <p style={{ fontSize: '12px', color: 'var(--text-secondary)', textTransform: 'uppercase', marginBottom: '5px' }}>Fraudulent Attempts Blocked</p>
                  <p style={{ fontSize: '2rem', fontWeight: 800, color: 'var(--danger)' }}>{fraudLogs.length}</p>
                </div>
              </div>
            ) : (
              <p style={{ color: 'var(--text-secondary)', fontStyle: 'italic' }}>Analytics data is currently unavailable.</p>
            )}
          </div>
        )}

        {activeTab === 'fraud' && (
          <div className="glass-panel" style={{ padding: '2rem' }}>
            <h2 style={{ fontSize: '1.5rem', fontFamily: 'Outfit', marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '10px' }}>
              <AlertTriangle size={24} color="var(--danger)" /> Fraud Alerts
            </h2>
            <div className="table-responsive">
              <table>
                <thead>
                  <tr>
                    <th>Timestamp</th>
                    <th>Account</th>
                    <th>Reason</th>
                    <th>Risk Score</th>
                    <th>Status</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {fraudLogs.length > 0 ? fraudLogs.map(log => (
                    <tr key={log.id}>
                      <td style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>{new Date(log.timestamp).toLocaleString()}</td>
                      <td style={{ fontFamily: 'monospace' }}>{log.accountNumber}</td>
                      <td>{log.reason}</td>
                      <td>
                        <span style={{ color: log.riskScore > 80 ? 'var(--danger)' : 'var(--warning)', fontWeight: 700 }}>
                          {log.riskScore}
                        </span>
                      </td>
                      <td>
                        <span className={`status-badge ${log.resolved ? 'completed' : 'failed'}`}>
                          {log.resolved ? 'Resolved' : 'Active'}
                        </span>
                      </td>
                      <td>
                        {!log.resolved && (
                          <button onClick={() => handleResolveFraud(log.id)} style={{ padding: '6px 12px', fontSize: '11px', background: 'var(--success)', display: 'flex', alignItems: 'center', gap: '4px' }}>
                            <CheckCircle size={12} /> Resolve
                          </button>
                        )}
                      </td>
                    </tr>
                  )) : (
                    <tr><td colSpan="6" style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-secondary)' }}>No fraud alerts found.</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {activeTab === 'audit' && (
          <div className="glass-panel" style={{ padding: '2rem' }}>
            <h2 style={{ fontSize: '1.5rem', fontFamily: 'Outfit', marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '10px' }}>
              <FileText size={24} color="var(--accent)" /> System Audit Logs
            </h2>
            <div className="table-responsive">
              <table>
                <thead>
                  <tr>
                    <th>Timestamp</th>
                    <th>Service</th>
                    <th>Action</th>
                    <th>Target User/Resource</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {auditLogs.length > 0 ? auditLogs.map(log => (
                    <tr key={log.id}>
                      <td style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>{new Date(log.timestamp).toLocaleString()}</td>
                      <td><span style={{ background: 'rgba(255,255,255,0.05)', padding: '2px 8px', borderRadius: '4px', fontSize: '0.8rem' }}>{log.serviceName}</span></td>
                      <td style={{ fontWeight: 500 }}>{log.action}</td>
                      <td style={{ fontFamily: 'monospace', color: 'var(--text-secondary)' }}>{log.targetIdentifier}</td>
                      <td>
                        <span className={`status-badge ${log.status === 'SUCCESS' ? 'completed' : 'failed'}`}>
                          {log.status}
                        </span>
                      </td>
                    </tr>
                  )) : (
                    <tr><td colSpan="5" style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-secondary)' }}>No audit logs available.</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {activeTab === 'customers' && (
          <div className="glass-panel" style={{ padding: '2rem' }}>
            <h2 style={{ fontSize: '1.5rem', fontFamily: 'Outfit', marginBottom: '1.5rem' }}>Customer Management</h2>
            <div className="table-responsive">
              <table>
                <thead>
                  <tr>
                    <th>Unique ID</th>
                    <th>Username</th>
                    <th>Email</th>
                    <th>KYC Status</th>
                    <th>KYC Doc</th>
                    <th>KYC Actions</th>
                    <th>Security</th>
                  </tr>
                </thead>
                <tbody>
                  {users.map(user => (
                    <tr key={user.id}>
                      <td style={{ fontFamily: 'monospace', fontWeight: 600 }}>{user.uniqueId || `USR-${user.id}`}</td>
                      <td>{user.username}</td>
                      <td style={{ color: 'var(--text-secondary)' }}>{user.email}</td>
                      <td>
                        <span className={`status-badge ${user.kycStatus?.toLowerCase()}`}>{user.kycStatus}</span>
                      </td>
                      <td>
                        {user.kycDocument ? (
                          <button onClick={() => openKycModal(user.kycDocument)} style={{ padding: '6px 12px', fontSize: '11px', background: 'rgba(255,255,255,0.05)', border: '1px solid var(--accent)', color: 'var(--text-primary)', boxShadow: 'none', display: 'flex', alignItems: 'center', gap: '4px' }}>
                            <Eye size={12} /> View
                          </button>
                        ) : (
                          <span style={{ color: '#64748b', fontSize: '12px', fontStyle: 'italic' }}>Not Uploaded</span>
                        )}
                      </td>
                      <td>
                        {user.kycStatus === 'PENDING' ? (
                          <div style={{ display: 'flex', gap: '8px' }}>
                            <button onClick={() => handleKycStatus(user.id, 'APPROVED')} style={{ padding: '6px 10px', fontSize: '11px', background: 'var(--success)', boxShadow: 'none' }} title="Approve">
                              <CheckCircle size={14} />
                            </button>
                            <button onClick={() => handleKycStatus(user.id, 'REJECTED')} style={{ padding: '6px 10px', fontSize: '11px', background: 'var(--danger)', boxShadow: 'none' }} title="Reject">
                              <XCircle size={14} />
                            </button>
                          </div>
                        ) : (
                          <span style={{ color: '#64748b', fontSize: '12px' }}>-</span>
                        )}
                      </td>
                      <td>
                        <div style={{ display: 'flex', gap: '8px' }}>
                          <button 
                            onClick={() => handleBlockUser(user.id, false)} 
                            style={{ padding: '6px 10px', fontSize: '11px', background: 'rgba(245, 158, 11, 0.2)', color: 'var(--warning)', border: '1px solid var(--warning)', boxShadow: 'none', display: 'flex', alignItems: 'center', gap: '4px' }}
                            title="Toggle Block"
                          >
                            <Shield size={12} />
                          </button>
                          <button 
                            onClick={() => handleFreezeAccounts(user.id, false)} 
                            style={{ padding: '6px 10px', fontSize: '11px', background: 'rgba(59, 130, 246, 0.2)', color: '#3b82f6', border: '1px solid #3b82f6', boxShadow: 'none', display: 'flex', alignItems: 'center', gap: '4px' }}
                            title="Toggle Freeze"
                          >
                            <Lock size={12} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {activeTab === 'feedbacks' && (
          <div className="glass-panel" style={{ padding: '2rem' }}>
            <h2 style={{ fontSize: '1.5rem', fontFamily: 'Outfit', marginBottom: '1.5rem' }}>User Feedbacks & Complaints</h2>
            {feedbacks.length > 0 ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
                {feedbacks.map(fb => (
                  <div key={fb.id} style={{ background: 'rgba(255,255,255,0.02)', padding: '1.5rem', borderRadius: '16px', border: '1px solid rgba(255,255,255,0.05)', position: 'relative' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '12px' }}>
                      <h4 style={{ color: 'var(--accent)', fontSize: '1.1rem' }}>{fb.subject}</h4>
                      <span style={{ fontSize: '12px', color: 'var(--text-secondary)', background: 'rgba(255,255,255,0.05)', padding: '4px 10px', borderRadius: '10px' }}>
                        {new Date(fb.createdAt).toLocaleDateString()}
                      </span>
                    </div>
                    <p style={{ fontSize: '1rem', color: 'var(--text-primary)', lineHeight: '1.5' }}>{fb.message}</p>
                    <div style={{ marginTop: '1rem', paddingTop: '1rem', borderTop: '1px solid rgba(255,255,255,0.05)', display: 'flex', alignItems: 'center', gap: '10px' }}>
                      <div style={{ width: '24px', height: '24px', borderRadius: '50%', background: 'var(--accent)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '10px', fontWeight: 'bold' }}>
                        {fb.username?.charAt(0).toUpperCase()}
                      </div>
                      <span style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>From: <strong style={{ color: 'var(--text-primary)' }}>{fb.username}</strong> (ID: {fb.userId})</span>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div style={{ textAlign: 'center', padding: '3rem', background: 'rgba(255,255,255,0.02)', borderRadius: '16px', border: '1px dashed rgba(255,255,255,0.1)' }}>
                <MessageSquare size={48} color="rgba(255,255,255,0.1)" style={{ marginBottom: '1rem' }} />
                <p style={{ color: 'var(--text-secondary)' }}>No feedbacks available.</p>
              </div>
            )}
          </div>
        )}
      </div>

      {/* KYC Modal */}
      {kycModalOpen && (
        <div className="modal-overlay" onClick={closeKycModal}>
          <div className="modal-content glass-panel" onClick={e => e.stopPropagation()} style={{ maxWidth: '80%', maxHeight: '90vh', overflow: 'auto', padding: '2rem' }}>
            <button onClick={closeKycModal} style={{ position: 'absolute', right: '15px', top: '15px', background: 'rgba(239, 68, 68, 0.2)', color: 'var(--danger)', border: '1px solid var(--danger)', padding: '6px 12px', fontSize: '12px', boxShadow: 'none' }}>Close</button>
            <h3 style={{ marginBottom: '1.5rem', fontFamily: 'Outfit', fontSize: '1.5rem' }}>KYC Document Preview</h3>
            <div style={{ background: '#000', borderRadius: '12px', padding: '10px', display: 'flex', justifyContent: 'center' }}>
              {kycImage && (kycImage.startsWith('data:image') ? (
                <img src={kycImage} alt="KYC Document" style={{ maxWidth: '100%', maxHeight: '60vh', objectFit: 'contain', borderRadius: '8px' }} />
              ) : (
                <iframe src={kycImage} style={{ width: '100%', height: '60vh', border: 'none', background: '#fff', borderRadius: '8px' }} title="KYC Doc"></iframe>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminDashboard;
