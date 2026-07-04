import React, { useState, useEffect } from 'react';
import api from '../api/axiosConfig';
import './Dashboard.css';

const AdminDashboard = () => {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    try {
      const response = await api.get('/users');
      setUsers(response.data);
    } catch (err) {
      console.error(err);
      setError('Failed to fetch users. Ensure you have Admin privileges.');
    } finally {
      setLoading(false);
    }
  };

  const handleKycStatus = async (userId, status) => {
    try {
      await api.put(`/users/${userId}/kyc?status=${status}`);
      fetchUsers(); // Refresh the list
    } catch (err) {
      console.error(err);
      alert('Failed to update KYC status');
    }
  };

  if (loading) return <div className="dashboard-container">Loading...</div>;

  return (
    <div className="dashboard-container">
      <div className="dashboard-header glass-panel">
        <h2>Admin Dashboard</h2>
        <p>Total Customers: {users.length}</p>
      </div>

      {error && <div className="error-message">{error}</div>}

      <div className="glass-panel" style={{ marginTop: '20px' }}>
        <h3>Customer KYC Approvals</h3>
        <table style={{ width: '100%', textAlign: 'left', marginTop: '10px' }}>
          <thead>
            <tr>
              <th>ID</th>
              <th>Username</th>
              <th>Email</th>
              <th>Status</th>
              <th>KYC Document</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {users.map(user => (
              <tr key={user.id} style={{ borderBottom: '1px solid rgba(255,255,255,0.1)' }}>
                <td>{user.id}</td>
                <td>{user.username}</td>
                <td>{user.email}</td>
                <td><span className={`status-badge ${user.kycStatus?.toLowerCase()}`}>{user.kycStatus}</span></td>
                <td>
                  {user.kycDocument ? (
                    <a href={user.kycDocument} target="_blank" rel="noreferrer" style={{ color: '#6366f1' }}>View Document</a>
                  ) : (
                    'Not Uploaded'
                  )}
                </td>
                <td>
                  {user.kycStatus === 'PENDING' && (
                    <>
                      <button onClick={() => handleKycStatus(user.id, 'APPROVED')} className="auth-button" style={{ padding: '5px 10px', fontSize: '12px', marginRight: '5px' }}>Approve</button>
                      <button onClick={() => handleKycStatus(user.id, 'REJECTED')} className="auth-button" style={{ padding: '5px 10px', fontSize: '12px', backgroundColor: '#ef4444' }}>Reject</button>
                    </>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default AdminDashboard;
