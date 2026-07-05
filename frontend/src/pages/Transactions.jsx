import React, { useState, useEffect } from 'react';
import api from '../api/axiosConfig';
import './Transactions.css';

const Transactions = () => {
  const [activeTab, setActiveTab] = useState('transfer'); // transfer, requests
  const [accounts, setAccounts] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [incomingReqs, setIncomingReqs] = useState([]);
  const [outgoingReqs, setOutgoingReqs] = useState([]);
  const [loading, setLoading] = useState(true);
  
  const [form, setForm] = useState({
    type: 'TRANSFER', // Only TRANSFER now
    sourceAccount: '',
    targetAccount: '',
    amount: '',
    description: ''
  });

  const [reqForm, setReqForm] = useState({
    requesterAccountNumber: '',
    targetUsername: '',
    amount: '',
    description: ''
  });

  const [message, setMessage] = useState({ text: '', type: '' });

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      const [accRes, txRes] = await Promise.all([
        api.get('/accounts'),
        api.get('/transactions/my').catch(() => ({ data: [] }))
      ]);
      setAccounts(accRes.data);
      if (accRes.data.length > 0) {
        setForm(prev => ({ ...prev, sourceAccount: accRes.data[0].accountNumber }));
        setReqForm(prev => ({ ...prev, requesterAccountNumber: accRes.data[0].accountNumber }));
      }
      if (Array.isArray(txRes.data)) {
        setTransactions(txRes.data);
      }
      
      api.get('/requests/incoming').then(res => setIncomingReqs(res.data)).catch(console.error);
      if (accRes.data.length > 0) {
         api.get(`/requests/outgoing?accountNumber=${accRes.data[0].accountNumber}`).then(res => setOutgoingReqs(res.data)).catch(console.error);
      }

    } catch (err) {
      console.error('Failed to fetch data', err);
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };
  
  const handleReqInputChange = (e) => {
    setReqForm({ ...reqForm, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMessage({ text: 'Processing...', type: 'info' });
    
    try {
      const payload = {
        amount: parseFloat(form.amount),
        description: form.description,
        sourceAccountNumber: form.sourceAccount,
        targetAccountNumber: form.targetAccount
      };

      await api.post('/transactions/transfer', payload);
      setMessage({ text: 'Transfer successful!', type: 'success' });
      setForm({ ...form, amount: '', description: '', targetAccount: '' });
      fetchData(); 
    } catch (err) {
      setMessage({ text: err.response?.data?.message || 'Transfer failed.', type: 'error' });
    }
  };

  const handleRequestSubmit = async (e) => {
    e.preventDefault();
    setMessage({ text: 'Sending request...', type: 'info' });
    try {
      await api.post('/requests', {
        ...reqForm,
        amount: parseFloat(reqForm.amount)
      });
      setMessage({ text: 'Money request sent successfully!', type: 'success' });
      setReqForm({ ...reqForm, amount: '', description: '', targetUsername: '' });
      fetchData();
    } catch (err) {
      setMessage({ text: err.response?.data?.message || 'Failed to send request.', type: 'error' });
    }
  };

  const handleAcceptRequest = async (id) => {
    if (accounts.length === 0) return alert('No accounts to transfer from');
    try {
      await api.put(`/requests/${id}/accept?sourceAccountNumber=${accounts[0].accountNumber}`);
      setMessage({ text: 'Request accepted and money transferred!', type: 'success' });
      fetchData();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to accept request');
    }
  };

  const handleRejectRequest = async (id) => {
    try {
      await api.put(`/requests/${id}/reject`);
      setMessage({ text: 'Request rejected.', type: 'info' });
      fetchData();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to reject request');
    }
  };

  if (loading) return <div className="dashboard-container"><div className="loader"></div></div>;

  return (
    <div className="dashboard-container">
      <div className="dashboard-header glass-panel" style={{ display: 'flex', gap: '15px' }}>
        <button 
          className="auth-button" 
          onClick={() => { setActiveTab('transfer'); setMessage({text:'', type:''}); }}
          style={{ background: activeTab === 'transfer' ? 'var(--accent)' : 'transparent', border: '1px solid var(--accent)' }}
        >
          Transfer Money
        </button>
        <button 
          className="auth-button" 
          onClick={() => { setActiveTab('requests'); setMessage({text:'', type:''}); }}
          style={{ background: activeTab === 'requests' ? 'var(--accent)' : 'transparent', border: '1px solid var(--accent)' }}
        >
          Money Requests
        </button>
      </div>

      {message.text && (
        <div className={`message-box ${message.type}`} style={{ margin: '20px 0' }}>
          {message.text}
        </div>
      )}

      {activeTab === 'transfer' && (
        <div className="transactions-container" style={{ padding: 0, marginTop: '20px' }}>
          <div className="glass-panel tx-form-panel">
            <h2>Send Money</h2>
            <form onSubmit={handleSubmit} className="tx-form">
              <div className="form-group">
                <label>Source Account</label>
                <select name="sourceAccount" value={form.sourceAccount} onChange={handleInputChange} className="input-field" required>
                  {accounts.map(acc => (
                    <option key={acc.accountNumber} value={acc.accountNumber}>
                      {acc.accountNumber} - ₹{acc.balance}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label>Target Account Number</label>
                <input 
                  type="text" 
                  name="targetAccount" 
                  value={form.targetAccount} 
                  onChange={handleInputChange} 
                  className="input-field" 
                  required 
                />
              </div>

              <div className="form-group">
                <label>Amount (₹)</label>
                <input 
                  type="number" 
                  name="amount" 
                  value={form.amount} 
                  onChange={handleInputChange} 
                  className="input-field" 
                  min="1"
                  step="0.01"
                  required 
                />
              </div>

              <div className="form-group">
                <label>Description</label>
                <input 
                  type="text" 
                  name="description" 
                  value={form.description} 
                  onChange={handleInputChange} 
                  className="input-field" 
                />
              </div>

              <button type="submit" className="tx-button auth-button">Transfer Now</button>
            </form>
          </div>

          <div className="glass-panel tx-history-panel">
            <h2>Transaction History</h2>
            {transactions.length === 0 ? (
              <p className="empty-state">No transactions found.</p>
            ) : (
              <div className="table-responsive">
                <table className="tx-table" style={{ width: '100%', textAlign: 'left' }}>
                  <thead>
                    <tr style={{ borderBottom: '1px solid rgba(255,255,255,0.2)' }}>
                      <th style={{ padding: '10px' }}>Date</th>
                      <th style={{ padding: '10px' }}>ID</th>
                      <th style={{ padding: '10px' }}>Amount</th>
                      <th style={{ padding: '10px' }}>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {transactions.map(tx => (
                      <tr key={tx.transactionId} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                        <td style={{ padding: '10px' }}>{new Date(tx.timestamp).toLocaleString()}</td>
                        <td style={{ padding: '10px' }}><small>{tx.transactionId}</small></td>
                        <td style={{ padding: '10px' }} className={accounts.some(a => a.accountNumber === tx.targetAccountNumber) ? 'text-success' : 'text-danger'}>
                          ₹{tx.amount}
                        </td>
                        <td style={{ padding: '10px' }}>
                          <span className={`status-badge ${tx.status.toLowerCase()}`}>
                            {tx.status}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}

      {activeTab === 'requests' && (
        <div className="transactions-container" style={{ padding: 0, marginTop: '20px' }}>
          <div className="glass-panel tx-form-panel">
            <h2>Request Money</h2>
            <form onSubmit={handleRequestSubmit} className="tx-form">
              <div className="form-group">
                <label>Receive To Account</label>
                <select name="requesterAccountNumber" value={reqForm.requesterAccountNumber} onChange={handleReqInputChange} className="input-field" required>
                  {accounts.map(acc => (
                    <option key={acc.accountNumber} value={acc.accountNumber}>
                      {acc.accountNumber} - ₹{acc.balance}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label>Target Username</label>
                <input 
                  type="text" 
                  name="targetUsername" 
                  value={reqForm.targetUsername} 
                  onChange={handleReqInputChange} 
                  className="input-field" 
                  required 
                />
              </div>

              <div className="form-group">
                <label>Amount (₹)</label>
                <input 
                  type="number" 
                  name="amount" 
                  value={reqForm.amount} 
                  onChange={handleReqInputChange} 
                  className="input-field" 
                  min="1"
                  step="0.01"
                  required 
                />
              </div>

              <div className="form-group">
                <label>Description</label>
                <input 
                  type="text" 
                  name="description" 
                  value={reqForm.description} 
                  onChange={handleReqInputChange} 
                  className="input-field" 
                />
              </div>

              <button type="submit" className="tx-button auth-button">Send Request</button>
            </form>
          </div>

          <div className="glass-panel tx-history-panel" style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
            <div>
              <h3 style={{ color: 'var(--warning)', marginBottom: '10px' }}>Incoming Requests</h3>
              {incomingReqs.length === 0 ? <p style={{ color: '#9ca3af' }}>No incoming requests.</p> : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                  {incomingReqs.map(req => (
                    <div key={req.id} style={{ background: 'rgba(255,255,255,0.05)', padding: '15px', borderRadius: '10px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <div>
                        <p><strong>₹{req.amount}</strong> requested for {req.description}</p>
                        <small style={{ color: '#9ca3af' }}>To: {req.requesterAccountNumber}</small>
                      </div>
                      {req.status === 'PENDING' ? (
                        <div style={{ display: 'flex', gap: '10px' }}>
                          <button onClick={() => handleAcceptRequest(req.id)} className="auth-button" style={{ background: 'var(--success)', padding: '5px 15px' }}>Accept</button>
                          <button onClick={() => handleRejectRequest(req.id)} className="auth-button" style={{ background: 'var(--danger)', padding: '5px 15px' }}>Reject</button>
                        </div>
                      ) : (
                        <span className={`status-badge ${req.status.toLowerCase()}`}>{req.status}</span>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div style={{ borderTop: '1px solid rgba(255,255,255,0.1)', paddingTop: '20px' }}>
              <h3 style={{ color: '#60a5fa', marginBottom: '10px' }}>Sent Requests</h3>
              {outgoingReqs.length === 0 ? <p style={{ color: '#9ca3af' }}>No sent requests.</p> : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                  {outgoingReqs.map(req => (
                    <div key={req.id} style={{ background: 'rgba(255,255,255,0.05)', padding: '15px', borderRadius: '10px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <div>
                        <p>Requested <strong>₹{req.amount}</strong> from @{req.targetUsername}</p>
                        <small style={{ color: '#9ca3af' }}>{req.description}</small>
                      </div>
                      <span className={`status-badge ${req.status.toLowerCase()}`}>{req.status}</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Transactions;
