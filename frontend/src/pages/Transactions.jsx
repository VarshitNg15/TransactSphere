import React, { useState, useEffect } from 'react';
import api from '../api/axiosConfig';
import './Transactions.css';

const Transactions = () => {
  const [accounts, setAccounts] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  
  const [form, setForm] = useState({
    type: 'TRANSFER', // TRANSFER, DEPOSIT, WITHDRAWAL
    sourceAccount: '',
    targetAccount: '',
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
      }
      if (Array.isArray(txRes.data)) {
        setTransactions(txRes.data);
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

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMessage({ text: 'Processing...', type: 'info' });
    
    try {
      let endpoint = '';
      const payload = {
        amount: parseFloat(form.amount),
        description: form.description
      };

      if (form.type === 'TRANSFER') {
        endpoint = '/transactions/transfer';
        payload.sourceAccountNumber = form.sourceAccount;
        payload.targetAccountNumber = form.targetAccount;
      } else if (form.type === 'DEPOSIT') {
        endpoint = '/transactions/deposit';
        payload.targetAccountNumber = form.sourceAccount;
      } else if (form.type === 'WITHDRAWAL') {
        endpoint = '/transactions/withdraw';
        payload.sourceAccountNumber = form.sourceAccount;
      }

      await api.post(endpoint, payload);
      setMessage({ text: 'Transaction successful!', type: 'success' });
      setForm({ ...form, amount: '', description: '', targetAccount: '' });
      fetchData(); // refresh data
    } catch (err) {
      setMessage({ text: err.response?.data?.message || 'Transaction failed.', type: 'error' });
    }
  };

  if (loading) return <div className="loader">Loading Transactions...</div>;

  return (
    <div className="transactions-container">
      <div className="glass-panel tx-form-panel">
        <h2>New Transaction</h2>
        
        {message.text && (
          <div className={`message-box ${message.type}`}>
            {message.text}
          </div>
        )}

        <form onSubmit={handleSubmit} className="tx-form">
          <div className="form-group">
            <label>Transaction Type</label>
            <select name="type" value={form.type} onChange={handleInputChange} className="input-field">
              <option value="TRANSFER">Transfer</option>
              <option value="DEPOSIT">Deposit</option>
              <option value="WITHDRAWAL">Withdrawal</option>
            </select>
          </div>

          <div className="form-group">
            <label>{form.type === 'DEPOSIT' ? 'Destination Account' : 'Source Account'}</label>
            <select name="sourceAccount" value={form.sourceAccount} onChange={handleInputChange} className="input-field" required>
              {accounts.map(acc => (
                <option key={acc.accountNumber} value={acc.accountNumber}>
                  {acc.accountNumber} - ₹{acc.balance} ({acc.accountType})
                </option>
              ))}
            </select>
          </div>

          {form.type === 'TRANSFER' && (
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
          )}

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

          <button type="submit" className="tx-button">Confirm Transaction</button>
        </form>
      </div>

      <div className="glass-panel tx-history-panel">
        <h2>Transaction History</h2>
        {transactions.length === 0 ? (
          <p className="empty-state">No transactions found.</p>
        ) : (
          <div className="table-responsive">
            <table className="tx-table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>ID</th>
                  <th>Type</th>
                  <th>Amount</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {transactions.map(tx => (
                  <tr key={tx.transactionId}>
                    <td>{new Date(tx.timestamp).toLocaleString()}</td>
                    <td><small>{tx.transactionId.substring(0, 8)}...</small></td>
                    <td>{tx.transactionType}</td>
                    <td className={tx.transactionType === 'DEPOSIT' || (tx.transactionType === 'TRANSFER' && accounts.some(a => a.accountNumber === tx.targetAccountNumber)) ? 'text-success' : 'text-danger'}>
                      ₹{tx.amount}
                    </td>
                    <td>
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
  );
};

export default Transactions;
