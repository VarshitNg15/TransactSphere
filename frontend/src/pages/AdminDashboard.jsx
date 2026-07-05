import React, { useState, useEffect } from 'react';
import api from '../api/axiosConfig';
import './Dashboard.css';

const AdminDashboard = () => {
  const [activeTab, setActiveTab] = useState('customers');
  const [users, setUsers] = useState([]);
  const [feedbacks, setFeedbacks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  
  // KYC Modal State
  const [kycModalOpen, setKycModalOpen] = useState(false);
  const [kycImage, setKycImage] = useState(null);

  useEffect(() => {
    fetchUsers();
    fetchFeedbacks();
  }, []);

  const fetchUsers = async () => {
    try {
      const response = await api.get('/users');
      // Sort users by uniqueId ascending
      const sortedUsers = response.data.sort((a, b) => {
        if (!a.uniqueId || !b.uniqueId) return 0;
        return a.uniqueId.localeCompare(b.uniqueId);
      });
      setUsers(sortedUsers);
    } catch (err) {
      console.error(err);
      setError('Failed to fetch users. Ensure you have Admin privileges.');
    } finally {
      setLoading(false);
    }
  };

  const fetchFeedbacks = async () => {
    try {
      const response = await api.get('/users/feedback');
      setFeedbacks(response.data);
    } catch (err) {
      console.error(err);
    }
  };

  const handleKycStatus = async (userId, status) => {
    try {
      await api.put(`/users/${userId}/kyc?status=${status}`);
      fetchUsers();
    } catch (err) {
      console.error(err);
      alert('Failed to update KYC status');
    }
  };

  const handleBlockUser = async (userId, isBlocked) => {
    try {
      await api.put(`/auth/users/${userId}/block?block=${!isBlocked}`);
      alert(`User ${!isBlocked ? 'blocked' : 'unblocked'} successfully.`);
      fetchUsers();
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
    <div className="dashboard-container" style={{ display: 'flex', gap: '20px', alignItems: 'flex-start' }}>
      
      {/* Sidebar */}
      <div className="glass-panel" style={{ width: '250px', padding: '1.5rem', position: 'sticky', top: '20px' }}>
        <h3 style={{ marginBottom: '20px', borderBottom: '1px solid rgba(255,255,255,0.1)', paddingBottom: '10px' }}>Admin Panel</h3>
        <ul style={{ listStyle: 'none', padding: 0, display: 'flex', flexDirection: 'column', gap: '10px' }}>
          <li>
            <button 
              onClick={() => setActiveTab('customers')}
              className="auth-button"
              style={{ width: '100%', background: activeTab === 'customers' ? 'var(--accent)' : 'transparent', border: '1px solid var(--accent)' }}
            >
              Customers & KYC
            </button>
          </li>
          <li>
            <button 
              onClick={() => setActiveTab('feedbacks')}
              className="auth-button"
              style={{ width: '100%', background: activeTab === 'feedbacks' ? 'var(--accent)' : 'transparent', border: '1px solid var(--accent)' }}
            >
              User Feedbacks
            </button>
          </li>
        </ul>
      </div>

      {/* Main Content Area */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '20px' }}>
        <div className="dashboard-header glass-panel">
          <h2>Dashboard Overview</h2>
          <p>Total Customers: {users.length} | Total Feedbacks: {feedbacks.length}</p>
        </div>

        {error && <div className="error-message glass-panel" style={{ background: 'rgba(239, 68, 68, 0.2)', border: '1px solid #ef4444' }}>{error}</div>}

        {activeTab === 'customers' && (
          <div className="glass-panel">
            <h3>Customer Management</h3>
            <div style={{ overflowX: 'auto', marginTop: '15px' }}>
              <table style={{ width: '100%', textAlign: 'left', borderCollapse: 'collapse' }}>
                <thead>
                  <tr style={{ borderBottom: '2px solid rgba(255,255,255,0.2)' }}>
                    <th style={{ padding: '12px' }}>Unique ID</th>
                    <th style={{ padding: '12px' }}>Username</th>
                    <th style={{ padding: '12px' }}>Email</th>
                    <th style={{ padding: '12px' }}>KYC Status</th>
                    <th style={{ padding: '12px' }}>KYC Doc</th>
                    <th style={{ padding: '12px' }}>KYC Actions</th>
                    <th style={{ padding: '12px' }}>Security Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {users.map(user => (
                    <tr key={user.id} style={{ borderBottom: '1px solid rgba(255,255,255,0.1)', transition: 'background 0.2s' }}>
                      <td style={{ padding: '12px' }}>{user.uniqueId || `USR-${user.id}`}</td>
                      <td style={{ padding: '12px' }}>{user.username}</td>
                      <td style={{ padding: '12px' }}>{user.email}</td>
                      <td style={{ padding: '12px' }}>
                        <span className={`status-badge ${user.kycStatus?.toLowerCase()}`}>{user.kycStatus}</span>
                      </td>
                      <td style={{ padding: '12px' }}>
                        {user.kycDocument ? (
                          <button onClick={() => openKycModal(user.kycDocument)} className="auth-button" style={{ padding: '4px 8px', fontSize: '11px', background: 'transparent', border: '1px solid var(--accent)' }}>View</button>
                        ) : (
                          <span style={{ color: '#9ca3af', fontSize: '12px' }}>Not Uploaded</span>
                        )}
                      </td>
                      <td style={{ padding: '12px' }}>
                        {user.kycStatus === 'PENDING' && (
                          <div style={{ display: 'flex', gap: '5px' }}>
                            <button onClick={() => handleKycStatus(user.id, 'APPROVED')} className="auth-button" style={{ padding: '4px 8px', fontSize: '11px', background: 'var(--success)' }}>Approve</button>
                            <button onClick={() => handleKycStatus(user.id, 'REJECTED')} className="auth-button" style={{ padding: '4px 8px', fontSize: '11px', background: 'var(--danger)' }}>Reject</button>
                          </div>
                        )}
                      </td>
                      <td style={{ padding: '12px' }}>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '5px' }}>
                          <button 
                            onClick={() => handleBlockUser(user.id, false)} 
                            className="auth-button" 
                            style={{ padding: '4px 8px', fontSize: '11px', background: 'var(--warning)' }}
                          >
                            Toggle Block
                          </button>
                          <button 
                            onClick={() => handleFreezeAccounts(user.id, false)} 
                            className="auth-button" 
                            style={{ padding: '4px 8px', fontSize: '11px', background: '#3b82f6' }}
                          >
                            Toggle Freeze
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
          <div className="glass-panel">
            <h3>User Feedbacks & Complaints</h3>
            {feedbacks.length > 0 ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '15px', marginTop: '15px' }}>
                {feedbacks.map(fb => (
                  <div key={fb.id} style={{ background: 'rgba(255,255,255,0.05)', padding: '15px', borderRadius: '10px', border: '1px solid rgba(255,255,255,0.1)' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '10px' }}>
                      <h4 style={{ color: 'var(--accent)' }}>{fb.subject}</h4>
                      <span style={{ fontSize: '12px', color: '#9ca3af' }}>{new Date(fb.createdAt).toLocaleDateString()}</span>
                    </div>
                    <p style={{ fontSize: '14px', marginBottom: '10px' }}>{fb.message}</p>
                    <div style={{ fontSize: '12px', color: '#9ca3af', borderTop: '1px solid rgba(255,255,255,0.1)', paddingTop: '10px' }}>
                      From User ID: {fb.userId} | Username: {fb.username}
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <p style={{ marginTop: '15px', color: '#9ca3af' }}>No feedbacks available.</p>
            )}
          </div>
        )}
      </div>

      {/* KYC Modal */}
      {kycModalOpen && (
        <div className="modal-overlay" onClick={closeKycModal} style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, 
          background: 'rgba(0,0,0,0.8)', zIndex: 1000, display: 'flex', justifyContent: 'center', alignItems: 'center'
        }}>
          <div className="modal-content glass-panel" onClick={e => e.stopPropagation()} style={{ maxWidth: '80%', maxHeight: '90vh', overflow: 'auto', position: 'relative' }}>
            <button onClick={closeKycModal} style={{ position: 'absolute', right: '10px', top: '10px', background: 'var(--danger)', padding: '5px 10px' }}>Close</button>
            <h3 style={{ marginBottom: '15px' }}>KYC Document Preview</h3>
            {kycImage && (kycImage.startsWith('data:image') ? (
              <img src={kycImage} alt="KYC Document" style={{ maxWidth: '100%', height: 'auto', borderRadius: '8px' }} />
            ) : (
              <iframe src={kycImage} style={{ width: '100%', height: '500px', border: 'none', background: '#fff' }} title="KYC Doc"></iframe>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminDashboard;
