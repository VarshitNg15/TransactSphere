import React, { useState, useEffect } from 'react';
import api from '../api/axiosConfig';
import { FileText, Download, CreditCard, ChevronRight } from 'lucide-react';
import './Dashboard.css';

const Statements = () => {
  const [accounts, setAccounts] = useState([]);
  const [selectedAccount, setSelectedAccount] = useState(null);
  const [statementData, setStatementData] = useState(null);
  const [loadingAccounts, setLoadingAccounts] = useState(true);
  const [loadingStatement, setLoadingStatement] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchAccounts();
  }, []);

  const fetchAccounts = async () => {
    try {
      const response = await api.get('/accounts');
      setAccounts(response.data);
    } catch (err) {
      console.error(err);
      setError('Failed to load accounts.');
    } finally {
      setLoadingAccounts(false);
    }
  };

  const fetchStatement = async (accountNumber) => {
    setLoadingStatement(true);
    setStatementData(null);
    setError('');
    try {
      const response = await api.get(`/statements/account/${accountNumber}`);
      setStatementData(response.data);
      setSelectedAccount(accountNumber);
    } catch (err) {
      console.error(err);
      setError('Failed to fetch statement for this account.');
    } finally {
      setLoadingStatement(false);
    }
  };

  const handleDownload = () => {
    if (!selectedAccount) return;
    
    // Create a temporary anchor element to trigger the file download from the backend
    const token = localStorage.getItem('token');
    if (!token) return;

    const baseURL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';
    fetch(`${baseURL}/statements/account/${selectedAccount}/download`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
    .then(response => response.blob())
    .then(blob => {
      const url = window.URL.createObjectURL(new Blob([blob]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `Statement_${selectedAccount}.csv`);
      document.body.appendChild(link);
      link.click();
      link.parentNode.removeChild(link);
    })
    .catch(err => {
      console.error('Download failed', err);
      alert('Failed to download statement.');
    });
  };

  if (loadingAccounts) return <div className="dashboard-container"><div className="loader"></div></div>;

  return (
    <div className="dashboard-container" style={{ display: 'flex', flexDirection: 'row', gap: '2rem', alignItems: 'flex-start' }}>
      {/* Sidebar for Accounts */}
      <div className="glass-panel" style={{ width: '320px', padding: '2rem 1.5rem', position: 'sticky', top: '2rem' }}>
        <h3 style={{ fontSize: '1.2rem', fontFamily: 'Outfit', color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '1px', marginBottom: '1.5rem' }}>Select Account</h3>
        {error && !selectedAccount && <div className="error-message" style={{ padding: '0.5rem', marginBottom: '1rem', fontSize: '0.9rem' }}>{error}</div>}
        
        {accounts.length === 0 ? (
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', fontStyle: 'italic' }}>No accounts found.</p>
        ) : (
          <ul style={{ listStyle: 'none', padding: 0, display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {accounts.map(acc => (
              <li key={acc.accountNumber}>
                <button 
                  onClick={() => fetchStatement(acc.accountNumber)}
                  style={{ 
                    width: '100%', 
                    background: selectedAccount === acc.accountNumber ? 'var(--accent)' : 'rgba(255,255,255,0.03)', 
                    color: selectedAccount === acc.accountNumber ? '#fff' : 'var(--text-primary)',
                    border: '1px solid rgba(255,255,255,0.05)',
                    padding: '1rem',
                    borderRadius: '12px',
                    display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                    boxShadow: selectedAccount === acc.accountNumber ? '0 4px 12px var(--accent-glow)' : 'none'
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px', textAlign: 'left' }}>
                    <CreditCard size={18} />
                    <div>
                      <div style={{ fontSize: '12px', opacity: 0.8, textTransform: 'uppercase' }}>{acc.accountType}</div>
                      <div style={{ fontFamily: 'monospace', fontSize: '14px', marginTop: '2px' }}>{acc.accountNumber}</div>
                    </div>
                  </div>
                  <ChevronRight size={18} opacity={selectedAccount === acc.accountNumber ? 1 : 0.5} />
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>

      {/* Main Statement Content */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '2rem' }}>
        <div className="dashboard-header glass-panel">
          <div>
            <h1>Account Statements</h1>
            <p>View and download your official bank statements.</p>
          </div>
          {statementData && (
            <button className="auth-button" onClick={handleDownload} style={{ width: 'auto', display: 'flex', alignItems: 'center', gap: '8px', margin: 0, background: 'var(--success)' }}>
              <Download size={18} /> Download CSV
            </button>
          )}
        </div>

        {error && selectedAccount && <div className="error-message glass-panel" style={{ background: 'rgba(239, 68, 68, 0.1)', border: '1px solid rgba(239, 68, 68, 0.3)' }}>{error}</div>}

        {loadingStatement ? (
          <div className="glass-panel" style={{ padding: '4rem', display: 'flex', justifyContent: 'center' }}>
            <div className="loader"></div>
          </div>
        ) : statementData ? (
          <div className="glass-panel" style={{ padding: '2rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', borderBottom: '1px solid rgba(255,255,255,0.1)', paddingBottom: '1.5rem', marginBottom: '1.5rem' }}>
              <div>
                <h2 style={{ fontSize: '1.5rem', fontFamily: 'Outfit', display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <FileText size={24} color="var(--accent)" />
                  Statement Summary
                </h2>
                <p style={{ color: 'var(--text-secondary)', marginTop: '5px' }}>Account: <strong style={{ color: 'var(--text-primary)', fontFamily: 'monospace' }}>{statementData.accountNumber}</strong></p>
              </div>
              <div style={{ textAlign: 'right' }}>
                <p style={{ fontSize: '12px', color: 'var(--text-secondary)', textTransform: 'uppercase' }}>Generated At</p>
                <p style={{ fontWeight: 600 }}>{new Date(statementData.generatedAt).toLocaleString()}</p>
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1.5rem', marginBottom: '2rem' }}>
              <div style={{ background: 'rgba(255,255,255,0.02)', padding: '1.5rem', borderRadius: '12px', border: '1px solid rgba(255,255,255,0.05)' }}>
                <p style={{ fontSize: '12px', color: 'var(--text-secondary)', textTransform: 'uppercase', marginBottom: '5px' }}>Period Start</p>
                <p style={{ fontSize: '1.1rem', fontWeight: 600 }}>{statementData.startDate}</p>
              </div>
              <div style={{ background: 'rgba(255,255,255,0.02)', padding: '1.5rem', borderRadius: '12px', border: '1px solid rgba(255,255,255,0.05)' }}>
                <p style={{ fontSize: '12px', color: 'var(--text-secondary)', textTransform: 'uppercase', marginBottom: '5px' }}>Period End</p>
                <p style={{ fontSize: '1.1rem', fontWeight: 600 }}>{statementData.endDate}</p>
              </div>
              <div style={{ background: 'rgba(255,255,255,0.02)', padding: '1.5rem', borderRadius: '12px', border: '1px solid rgba(255,255,255,0.05)' }}>
                <p style={{ fontSize: '12px', color: 'var(--text-secondary)', textTransform: 'uppercase', marginBottom: '5px' }}>Total Transactions</p>
                <p style={{ fontSize: '1.8rem', fontWeight: 800, color: 'var(--accent)' }}>{statementData.transactionCount}</p>
              </div>
            </div>

            {statementData.transactions && statementData.transactions.length > 0 ? (
              <div className="table-responsive">
                <table>
                  <thead>
                    <tr>
                      <th>Date</th>
                      <th>Type</th>
                      <th>Reference</th>
                      <th style={{ textAlign: 'right' }}>Amount</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {statementData.transactions.map((txn, idx) => {
                      const isCredit = (txn.transactionType === 'DEPOSIT' || (txn.transactionType === 'TRANSFER' && txn.targetAccountNumber === selectedAccount));
                      return (
                        <tr key={idx}>
                          <td style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>{new Date(txn.timestamp).toLocaleString()}</td>
                          <td><span className={`status-badge ${txn.transactionType.toLowerCase()}`} style={{ background: 'rgba(255,255,255,0.05)', color: 'var(--text-primary)', border: 'none' }}>{txn.transactionType}</span></td>
                          <td style={{ fontFamily: 'monospace', fontSize: '0.9rem' }}>{txn.id}</td>
                          <td style={{ textAlign: 'right', fontWeight: 600, color: isCredit ? 'var(--success)' : 'var(--text-primary)' }}>
                            {isCredit ? '+' : '-'}₹{parseFloat(txn.amount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                          </td>
                          <td><span className={`status-badge ${txn.status.toLowerCase()}`}>{txn.status}</span></td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            ) : (
              <p style={{ textAlign: 'center', color: 'var(--text-secondary)', padding: '2rem 0', fontStyle: 'italic' }}>No transactions found for this period.</p>
            )}
          </div>
        ) : (
          <div className="glass-panel" style={{ padding: '4rem', textAlign: 'center', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
            <FileText size={64} color="rgba(255,255,255,0.1)" style={{ marginBottom: '1rem' }} />
            <h3 style={{ fontSize: '1.5rem', color: 'var(--text-primary)', marginBottom: '0.5rem' }}>No Account Selected</h3>
            <p style={{ color: 'var(--text-secondary)' }}>Please select an account from the sidebar to view its statement.</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default Statements;
