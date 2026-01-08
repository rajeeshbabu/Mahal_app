"use client";

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { supabase } from '@/lib/supabase';

export default function PricingManagementPage() {
  const [pricing, setPricing] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [user, setUser] = useState(null);
  const router = useRouter();

  // Helper to get formatted Indian Standard Time (IST) string: YYYY-MM-DD HH:mm:ss.SSS
  const getFormattedDateTime = (date = new Date()) => {
    try {
      const formatter = new Intl.DateTimeFormat('en-GB', {
        timeZone: 'Asia/Kolkata',
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false
      });

      const parts = formatter.formatToParts(date);
      const getPart = (type) => parts.find(p => p.type === type).value;

      const year = getPart('year');
      const month = getPart('month');
      const day = getPart('day');
      const hour = getPart('hour');
      const minute = getPart('minute');
      const second = getPart('second');
      const ms = String(date.getMilliseconds()).padStart(3, '0');

      return `${year}-${month}-${day} ${hour}:${minute}:${second}.${ms}`;
    } catch (e) {
      console.error('IST Formatting error:', e);
      return date.toISOString().replace('T', ' ').replace('Z', '');
    }
  };

  useEffect(() => {
    const adminEmail = localStorage.getItem('admin_email');
    if (!adminEmail) {
      router.push('/login');
      return;
    }
    setUser(adminEmail);
    fetchPricing();
  }, []);

  const formatDate = (dateStr) => {
    if (!dateStr) return 'N/A';
    try {
      const date = new Date(dateStr);
      if (isNaN(date.getTime())) return dateStr;
      return getFormattedDateTime(date);
    } catch (e) {
      return dateStr;
    }
  };

  const fetchPricing = async () => {
    try {
      const { data, error } = await supabase
        .from('subscription_pricing')
        .select('*')
        .order('plan_duration', { ascending: true });

      if (error) throw error;

      // Map to simpler format if needed or just use data
      setPricing(data || []);
    } catch (err) {
      console.error('Error fetching pricing:', err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleUpdatePrice = async (id, newAmountRupees) => {
    setSaving(true);
    try {
      const { error } = await supabase
        .from('subscription_pricing')
        .update({
          amount_paise: parseFloat(newAmountRupees),
          updated_at: getFormattedDateTime()
        })
        .eq('id', id);

      if (error) throw error;

      alert('Price updated successfully!');
      fetchPricing();
    } catch (err) {
      console.error('Error updating pricing:', err.message);
      alert('Failed to update: ' + err.message);
    } finally {
      setSaving(false);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('admin_role');
    localStorage.removeItem('admin_email');
    router.push('/');
  };

  return (
    <div className="dashboard-container">
      <nav className="dashboard-nav">
        <div className="container nav-content">
          <div className="logo" onClick={() => router.push('/')} style={{ cursor: 'pointer' }}>
            <div className="logo-icon">
              <iconify-icon icon="lucide:building-2" style={{ fontSize: '18px' }}></iconify-icon>
            </div>
            MahalSoft Dashboard
          </div>
          <div className="user-info">
            <span>{user}</span>
            <button onClick={handleLogout} className="btn-logout">Logout</button>
          </div>
        </div>
      </nav>

      <main className="container dashboard-main">
        <div className="page-header">
          <div>
            <h1>Manage Subscription Pricing</h1>
            <p>Update monthly and yearly amounts for all communities</p>
          </div>
          <div className="header-actions">
            <button onClick={() => router.push('/subscriptions')} className="btn-secondary">
              Back to Subscriptions
            </button>
            <button onClick={fetchPricing} className="btn-refresh">
              <iconify-icon icon="lucide:refresh-cw"></iconify-icon>
              Refresh
            </button>
          </div>
        </div>

        {loading ? (
          <div className="loading-state">Loading pricing data...</div>
        ) : (
          <div className="pricing-grid">
            {pricing.map((item) => (
              <div key={item.id} className="pricing-card">
                <div className="card-header">
                  <h2 className="plan-name">{item.plan_duration.toUpperCase()} PLAN</h2>
                  <span className="currency-badge">{item.currency}</span>
                </div>

                <div className="price-input-group">
                  <label>Amount (in Rupees)</label>
                  <input
                    type="number"
                    defaultValue={item.amount_paise}
                    id={`amount-${item.id}`}
                    className="price-input"
                    step="0.01"
                  />
                  <p className="helper-text">Example: 100 = ₹100.00</p>
                  <p className="current-price-preview">
                    Current: <strong>₹{(item.amount_paise).toFixed(2)}</strong>
                  </p>
                </div>

                <button
                  className="btn-save"
                  disabled={saving}
                  onClick={() => {
                    const val = document.getElementById(`amount-${item.id}`).value;
                    handleUpdatePrice(item.id, val);
                  }}
                >
                  {saving ? 'Saving...' : 'Update Price'}
                </button>

                <div className="card-footer">
                  <span>Last updated: {formatDate(item.updated_at)}</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </main>

      <style jsx>{`
        .dashboard-container {
          min-height: 100vh;
          background: #f8fafc;
        }
        .dashboard-nav {
          background: white;
          border-bottom: 1px solid var(--border);
          padding: 16px 0;
        }
        .nav-content {
          display: flex;
          align-items: center;
          justify-content: space-between;
        }
        .logo {
          display: flex;
          align-items: center;
          gap: 10px;
          font-weight: 700;
          font-size: 18px;
          color: var(--primary);
        }
        .logo-icon {
          width: 32px;
          height: 32px;
          background: var(--primary);
          border-radius: 6px;
          display: flex;
          align-items: center;
          justify-content: center;
          color: white;
        }
        .user-info {
          display: flex;
          align-items: center;
          gap: 16px;
          font-size: 14px;
        }
        .btn-logout {
          background: transparent;
          color: #dc2626;
          border: 1px solid #fee2e2;
          padding: 6px 12px;
          border-radius: 6px;
          font-size: 13px;
          font-weight: 500;
          cursor: pointer;
        }
        .dashboard-main {
          padding-top: 40px;
          padding-bottom: 80px;
        }
        .page-header {
          display: flex;
          align-items: center;
          justify-content: space-between;
          margin-bottom: 32px;
        }
        .page-header h1 {
          font-size: 28px;
          font-weight: 700;
          margin-bottom: 4px;
        }
        .header-actions {
          display: flex;
          gap: 12px;
        }
        .btn-refresh, .btn-secondary {
          display: flex;
          align-items: center;
          gap: 8px;
          background: white;
          border: 1px solid var(--border);
          padding: 8px 16px;
          border-radius: 8px;
          font-size: 14px;
          font-weight: 500;
          cursor: pointer;
        }
        .btn-secondary:hover {
          background: #f1f5f9;
        }
        .pricing-grid {
          display: grid;
          grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
          gap: 24px;
          max-width: 800px;
        }
        .pricing-card {
          background: white;
          border-radius: 12px;
          border: 1px solid var(--border);
          padding: 24px;
          box-shadow: 0 4px 15px rgba(0,0,0,0.02);
        }
        .card-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 24px;
        }
        .plan-name {
          font-size: 18px;
          font-weight: 700;
          color: #1e293b;
        }
        .currency-badge {
          background: #e0f2fe;
          color: #0369a1;
          padding: 4px 8px;
          border-radius: 4px;
          font-size: 12px;
          font-weight: 600;
        }
        .price-input-group {
          margin-bottom: 24px;
        }
        .price-input-group label {
          display: block;
          font-size: 14px;
          font-weight: 500;
          margin-bottom: 8px;
          color: #64748b;
        }
        .price-input {
          width: 100%;
          padding: 12px;
          border: 1px solid var(--border);
          border-radius: 8px;
          font-size: 24px;
          font-weight: 700;
          color: #1e293b;
          margin-bottom: 8px;
        }
        .helper-text {
          font-size: 12px;
          color: #94a3b8;
          margin-bottom: 8px;
        }
        .current-price-preview {
          font-size: 14px;
          color: #1e293b;
        }
        .btn-save {
          width: 100%;
          background: var(--primary);
          color: white;
          border: none;
          padding: 12px;
          border-radius: 8px;
          font-weight: 600;
          cursor: pointer;
          transition: filter 0.2s;
        }
        .btn-save:hover:not(:disabled) {
          filter: brightness(1.1);
        }
        .btn-save:disabled {
          opacity: 0.7;
          cursor: not-allowed;
        }
        .card-footer {
          margin-top: 16px;
          padding-top: 16px;
          border-top: 1px solid #f1f5f9;
          font-size: 11px;
          color: #94a3b8;
        }
        .loading-state {
          text-align: center;
          padding: 80px;
          background: white;
          border-radius: 12px;
          border: 1px solid var(--border);
          color: var(--muted-foreground);
        }
      `}</style>
    </div>
  );
}
