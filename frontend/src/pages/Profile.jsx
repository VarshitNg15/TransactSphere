import React, { useState, useEffect } from 'react';
import api from '../api/axiosConfig';
import './Dashboard.css';

const Profile = () => {
  const [profile, setProfile] = useState(null);
  const [accounts, setAccounts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [kycFileBase64, setKycFileBase64] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [editMode, setEditMode] = useState(false);
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    phoneNumber: '',
    email: '',
    address: ''
  });

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      const profileRes = await api.get('/users/profile');
      setProfile(profileRes.data);
      setFormData({
        firstName: profileRes.data.firstName || '',
        lastName: profileRes.data.lastName || '',
        phoneNumber: profileRes.data.phoneNumber || '',
        email: profileRes.data.email || '',
        address: profileRes.data.address || ''
      });
      const accountsRes = await api.get('/accounts');
      setAccounts(accountsRes.data);
    } catch (err) {
      console.error(err);
      setError('Failed to fetch profile data');
    } finally {
      setLoading(false);
    }
  };

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      const reader = new FileReader();
      reader.onloadend = () => {
        setKycFileBase64(reader.result);
      };
      reader.readAsDataURL(file);
    }
  };

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleUpdateProfile = async (e) => {
    e.preventDefault();
    setUploading(true);
    try {
      await api.put('/users/profile', {
        ...formData,
        kycDocument: profile.kycDocument
      });
      await fetchData();
      setEditMode(false);
      alert('Profile updated successfully!');
    } catch (err) {
      console.error(err);
      alert(err.response?.data?.message || 'Failed to update profile');
    } finally {
      setUploading(false);
    }
  };

  const uploadKyc = async () => {
    if (!kycFileBase64) return;
    setUploading(true);
    try {
      await api.put('/users/profile', {
        firstName: profile.firstName || formData.firstName || '',
        lastName: profile.lastName || formData.lastName || '',
        phoneNumber: profile.phoneNumber || formData.phoneNumber || '',
        email: profile.email || formData.email || '',
        address: profile.address || formData.address || '',
        kycDocument: kycFileBase64
      });
      await fetchData();
      setKycFileBase64(null);
      alert('KYC Document uploaded successfully!');
    } catch (err) {
      console.error(err);
      alert('Failed to upload KYC');
    } finally {
      setUploading(false);
    }
  };

  if (loading) return <div className="dashboard-container">Loading...</div>;

  return (
    <div className="dashboard-container">
      <div className="dashboard-header glass-panel">
        <h2 style={{ fontFamily: 'Outfit', color: 'var(--accent)' }}>My Profile</h2>
      </div>
      
      {error && <div className="error-message">{error}</div>}

      <div className="grid-container">
        <div className="glass-panel profile-details">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h3 style={{ fontFamily: 'Outfit' }}>User Details</h3>
            <button className="auth-button" style={{ width: 'auto', padding: '5px 15px', fontFamily: 'Inter' }} onClick={() => setEditMode(!editMode)}>
              {editMode ? 'Cancel' : 'Edit Profile'}
            </button>
          </div>
          
          {editMode ? (
            <form onSubmit={handleUpdateProfile} style={{ marginTop: '15px' }}>
              <div className="form-group">
                <label>First Name</label>
                <input type="text" name="firstName" className="input-field" value={formData.firstName} onChange={handleChange} required />
              </div>
              <div className="form-group">
                <label>Last Name</label>
                <input type="text" name="lastName" className="input-field" value={formData.lastName} onChange={handleChange} required />
              </div>
              <div className="form-group">
                <label style={{ fontFamily: 'Inter' }}>Phone Number</label>
                <input type="text" name="phoneNumber" className="input-field" value={formData.phoneNumber} onChange={handleChange} pattern="\d{10}" title="Phone number must be exactly 10 digits" required />
              </div>
              <div className="form-group">
                <label>Email</label>
                <input type="email" name="email" className="input-field" value={formData.email} onChange={handleChange} required />
              </div>
              <div className="form-group">
                <label>Address</label>
                <textarea name="address" className="input-field" value={formData.address} onChange={handleChange} required />
              </div>
              <button type="submit" className="auth-button" disabled={uploading}>
                {uploading ? 'Saving...' : 'Save Changes'}
              </button>
            </form>
          ) : (
            <div style={{ marginTop: '15px', fontFamily: 'Inter', display: 'flex', flexDirection: 'column', gap: '8px' }}>
              <p><strong>Username:</strong> {profile?.username}</p>
              <p><strong>First Name:</strong> {profile?.firstName || 'N/A'}</p>
              <p><strong>Last Name:</strong> {profile?.lastName || 'N/A'}</p>
              <p><strong>Phone:</strong> {profile?.phoneNumber || 'N/A'}</p>
              <p><strong>Email:</strong> {profile?.email}</p>
              <p><strong>Address:</strong> {profile?.address || 'N/A'}</p>
              <p><strong>KYC Status:</strong> <span className={`status-badge ${profile?.kycStatus?.toLowerCase()}`}>{profile?.kycStatus}</span></p>
            </div>
          )}
          
          <div className="kyc-upload-section" style={{ marginTop: '30px', borderTop: '1px solid rgba(255,255,255,0.1)', paddingTop: '15px' }}>
            <h4 style={{ fontFamily: 'Outfit', marginBottom: '10px' }}>Upload KYC Document</h4>
            <input type="file" onChange={handleFileChange} style={{ fontFamily: 'Inter' }} />
            <button onClick={uploadKyc} disabled={!kycFileBase64 || uploading} style={{ marginTop: '10px' }} className="auth-button">
              {uploading ? 'Uploading...' : 'Submit KYC'}
            </button>
          </div>
        </div>

        <div className="glass-panel accounts-list">
          <h3 style={{ fontFamily: 'Outfit', marginBottom: '15px' }}>My Accounts</h3>
          {accounts.length > 0 ? (
            <ul style={{ fontFamily: 'Inter' }}>
              {accounts.map(acc => (
                <li key={acc.id} style={{ marginBottom: '10px', borderBottom: '1px solid rgba(255,255,255,0.1)', paddingBottom: '10px' }}>
                  <p><strong>Type:</strong> {acc.accountType}</p>
                  <p><strong>Number:</strong> {acc.accountNumber}</p>
                  <p><strong>Balance:</strong> ${acc.balance}</p>
                </li>
              ))}
            </ul>
          ) : (
            <p>No accounts found.</p>
          )}
        </div>
      </div>
    </div>
  );
};

export default Profile;
