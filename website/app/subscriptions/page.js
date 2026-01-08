"use client";

export const dynamic = 'force-dynamic';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { supabase } from '@/lib/supabase';

export default function SubscriptionsPage() {
  const [subscriptions, setSubscriptions] = useState([]);
  const [pricing, setPricing] = useState([]);
  const [loading, setLoading] = useState(true);
  const [savingPrices, setSavingPrices] = useState(false);
  const [user, setUser] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
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

      // Extract parts to build: YYYY-MM-DD HH:mm:ss.SSS
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
    fetchData();
  }, []);

  const fetchData = async () => {
    setLoading(true);
    await Promise.all([fetchSubscriptions(), fetchPricing()]);
    setLoading(false);
  };

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

  const formatLocalDate = (dateStr) => {
    return formatDate(dateStr);
  };

  const fetchSubscriptions = async () => {
    try {
      const { data, error } = await supabase
        .from('subscriptions')
        .select('*')
        .order('created_at', { ascending: false });

      if (error) throw error;
      setSubscriptions(data || []);
    } catch (err) {
      console.error('Error fetching subscriptions:', err.message);
    }
  };

  const fetchPricing = async () => {
    try {
      const { data, error } = await supabase
        .from('subscription_pricing')
        .select('*')
        .order('plan_duration', { ascending: true });

      if (error) throw error;
      setPricing(data || []);
    } catch (err) {
      console.error('Error fetching pricing:', err.message);
    }
  };

  const handleUpdatePrice = async (id, newAmountRupees) => {
    if (!id) {
      alert("Error: Missing record ID");
      return;
    }

    const amountRupees = parseFloat(newAmountRupees);
    if (isNaN(amountRupees)) {
      alert("Please enter a valid number");
      return;
    }

    // Store exactly what is entered in the dashboard
    const amount = amountRupees;

    setSavingPrices(true);
    console.log(`🔄 Updating price for ${id} to ${amount}`);

    try {
      const { data, error } = await supabase
        .from('subscription_pricing')
        .update({
          amount_paise: amount, // Keeping the column name but storing Rupees as requested
          updated_at: getFormattedDateTime()
        })
        .eq('id', id)
        .select();

      if (error) {
        console.error('Supabase Error:', error);
        throw error;
      }

      console.log('✅ Update successful:', data);
      alert('Price updated successfully!');
      fetchPricing();
    } catch (err) {
      console.error('Catch Error updating pricing:', err);
      alert('Failed to update price: ' + (err.message || 'Network error (Failed to fetch)'));
    } finally {
      setSavingPrices(false);
    }
  };

  const handleInitializePricing = async () => {
    setSavingPrices(true);
    try {
      const defaultPlans = [
        { plan_duration: 'monthly', amount_paise: 100 },
        { plan_duration: 'yearly', amount_paise: 1000 }
      ];

      const { error } = await supabase
        .from('subscription_pricing')
        .insert(defaultPlans);

      if (error) throw error;

      alert('Pricing initialized successfully!');
      fetchPricing();
    } catch (err) {
      console.error('Error initializing pricing:', err.message);
      alert('Failed to initialize pricing: ' + (err.message || 'Error occurred'));
    } finally {
      setSavingPrices(false);
    }
  };


  const handlePlanChange = async (sub, newPlan) => {
    // Force lowercase for internal consistency
    const plan = newPlan.toLowerCase();
    console.log(`🔄 Changing plan for user ${sub.user_id} to ${plan}`);

    const updatePayload = {
      plan_duration: plan,
      updated_at: getFormattedDateTime()
    };

    // If subscription is active, we should also update the end_date based on the new plan
    if (sub.status === 'active' && sub.start_date) {
      let startDateObj = new Date(sub.start_date);
      let endDateObj = new Date(startDateObj);

      if (newPlan.toLowerCase() === 'yearly') {
        endDateObj.setFullYear(endDateObj.getFullYear() + 1);
      } else {
        endDateObj.setMonth(endDateObj.getMonth() + 1);
      }

      updatePayload.end_date = getFormattedDateTime(endDateObj);
    }

    try {
      const { data, error } = await supabase
        .from('subscriptions')
        .update(updatePayload)
        .eq('user_id', sub.user_id)
        .select();

      if (error) throw error;

      console.log('✅ Plan updated successfully:', data);

      // Update local state
      const updatedSubscriptions = subscriptions.map(s =>
        s.user_id === sub.user_id ? { ...s, ...updatePayload } : s
      );
      setSubscriptions(updatedSubscriptions);
      alert(`Plan updated to ${newPlan} successfully!`);

    } catch (err) {
      console.error('Error updating plan:', err.message);
      alert('Failed to update plan: ' + err.message);
      fetchSubscriptions();
    }
  };

  const handleActivate = async (sub) => {
    console.log('🔄 Activating subscription:', sub);
    console.log('🔑 Using user_id for update:', sub.user_id); // Debug log

    try {
      const { data, error } = await supabase
        .from('subscriptions')
        .update({
          superadmin_status: 'activated',
          status: 'active', // Explicitly set status to active
          updated_at: getFormattedDateTime()
        })
        .eq('user_id', sub.user_id) // Reverted to user_id as requested
        .select();

      if (error) throw error;

      if (!data || data.length === 0) {
        console.warn('⚠️ No rows were updated. Check if user_id match.');
        alert(`Warning: No rows updated. user_id used: ${sub.user_id}`);
      } else {
        console.log('✅ Subscription activated successfully:', data);
        alert('Subscription activated successfully! (Unlocked)');
      }

      fetchSubscriptions();
    } catch (err) {
      console.error('Error activating subscription:', err.message);
      alert('Failed to activate: ' + err.message);
    }
  };

  const handleDeactivate = async (sub) => {
    console.log('🔄 Deactivating subscription:', sub);
    console.log('🔑 Using user_id for update:', sub.user_id); // Debug log
    if (!confirm(`Are you sure you want to deactivate (Lock) this subscription?`)) return;

    try {
      const { data, error } = await supabase
        .from('subscriptions')
        .update({
          superadmin_status: 'deactivated',
          status: 'cancelled', // Explicitly set status to cancelled
          updated_at: getFormattedDateTime()
        })
        .eq('user_id', sub.user_id) // Reverted to user_id as requested
        .select();

      if (error) throw error;

      if (!data || data.length === 0) {
        console.warn('⚠️ No rows were updated. Check if user_id match.');
        alert(`Warning: No rows updated. user_id used: ${sub.user_id}`);
      } else {
        console.log('✅ Subscription deactivated successfully:', data);
        alert('Subscription deactivated successfully! (Locked)');
      }

      fetchSubscriptions();
    } catch (err) {
      console.error('Error deactivating subscription:', err.message);
      alert('Failed to deactivate: ' + err.message);
    }
  };

  const handleStatusChange = async (sub, newStatus) => {
    console.log(`🔄 Updating status for user ${sub.user_id} to ${newStatus}`);

    // Optimistic update moved to successful response block to ensure date accuracy

    const updatePayload = {
      status: newStatus,
      updated_at: getFormattedDateTime()
    };

    if (newStatus === 'active') {
      const now = new Date();

      // Calculate End Date based on duration
      let endDateObj = new Date(now);
      if (sub.plan_duration?.toLowerCase() === 'yearly') {
        endDateObj.setFullYear(endDateObj.getFullYear() + 1);
      } else {
        // Default to monthly
        endDateObj.setMonth(endDateObj.getMonth() + 1);
      }

      updatePayload.start_date = getFormattedDateTime(now);
      updatePayload.end_date = getFormattedDateTime(endDateObj);
      updatePayload.superadmin_status = 'activated'; // Ensure unlocked
    }

    try {
      const { data, error } = await supabase
        .from('subscriptions')
        .update(updatePayload)
        .eq('user_id', sub.user_id)
        .select();

      if (error) throw error;

      console.log('✅ Status updated successfully:', data);

      // Optimistic update for UI responsiveness (with new dates if active)
      const updatedItem = { ...sub, status: newStatus };
      if (newStatus === 'active') {
        updatedItem.start_date = updatePayload.start_date;
        updatedItem.end_date = updatePayload.end_date;
        updatedItem.superadmin_status = 'activated';
      }

      const updatedSubscriptions = subscriptions.map(s =>
        s.user_id === sub.user_id ? updatedItem : s
      );
      setSubscriptions(updatedSubscriptions);

    } catch (err) {
      console.error('Error updating status:', err.message);
      alert('Failed to update status: ' + err.message);
      fetchSubscriptions(); // Revert on error
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('admin_role');
    localStorage.removeItem('admin_email');
    router.push('/');
  };

  const getStatusColor = (status) => {
    switch (status?.toLowerCase()) {
      case 'active': return '#dcfce7';
      case 'pending': return '#fef9c3';
      case 'cancelled': return '#fee2e2';
      default: return '#f1f5f9';
    }
  };

  const getStatusTextColor = (status) => {
    switch (status?.toLowerCase()) {
      case 'active': return '#166534';
      case 'pending': return '#854d0e';
      case 'cancelled': return '#991b1b';
      default: return '#64748b';
    }
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
        <div className="pricing-section">
          <div className="section-header">
            <h2>Subscription Pricing</h2>
            <p>Update fees for all communities (amounts in Rupees)</p>
          </div>
          <div className="pricing-cards">
            {pricing.map((item) => (
              <div key={item.id} className="pricing-mini-card">
                <div className="price-input-info">
                  <span className="plan-label">{item.plan_duration}</span>
                  <div className="price-input-wrapper">
                    <span>₹</span>
                    <input
                      type="number"
                      defaultValue={item.amount_paise}
                      id={`price-${item.id}`}
                      className="inline-price-input"
                      step="0.01"
                    />
                  </div>
                </div>
                <button
                  className="btn-update-price"
                  disabled={savingPrices}
                  onClick={() => {
                    const priceInRupees = document.getElementById(`price-${item.id}`).value;
                    handleUpdatePrice(item.id, priceInRupees);
                  }}
                >
                  {savingPrices ? '...' : 'Update'}
                </button>
              </div>
            ))}
            {pricing.length === 0 && (
              <div className="empty-pricing-state">
                <p>No pricing plans found. Initializing will create default Monthly/Yearly plans.</p>
                <button
                  className="btn-update-price"
                  onClick={handleInitializePricing}
                  disabled={savingPrices}
                >
                  {savingPrices ? 'Initializing...' : 'Initialize Default Pricing'}
                </button>
              </div>
            )}

          </div>
        </div>

        <div className="page-header" style={{ marginTop: '40px' }}>
          <div>
            <h1>All Subscriptions</h1>
            <p>Manage and view all Mahal community subscriptions</p>
          </div>
          <div className="header-actions">
            <div className="search-box">
              <iconify-icon icon="lucide:search"></iconify-icon>
              <input
                type="text"
                placeholder="Search by email..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>
            <button onClick={fetchSubscriptions} className="btn-refresh">
              <iconify-icon icon="lucide:refresh-cw"></iconify-icon>
              Refresh
            </button>
          </div>
        </div>

        {loading ? (
          <div className="loading-state">Loading subscriptions...</div>
        ) : (
          <div className="table-wrapper">
            <table className="sub-table">
              <thead>
                <tr>
                  <th>Mahal / User</th>
                  <th>Plan</th>
                  <th>Status</th>
                  <th>Admin Lock</th>
                  <th>Start Date</th>
                  <th>End Date</th>
                  <th>Razorpay ID</th>
                  <th>Updated At</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {subscriptions
                  .filter(sub =>
                    sub.user_email?.toLowerCase().includes(searchTerm.toLowerCase())
                  )
                  .map((sub) => (
                    <tr key={sub.id}>
                      <td>
                        <div className="user-cell">
                          <strong>{sub.user_email || 'Unknown'}</strong>
                          <span>ID: {sub.user_id?.substring(0, 8)}...</span>
                        </div>
                      </td>
                      <td>
                        <select
                          className="plan-select"
                          value={sub.plan_duration?.toLowerCase() || 'monthly'}
                          onChange={(e) => handlePlanChange(sub, e.target.value)}
                        >
                          <option value="monthly">Monthly</option>
                          <option value="yearly">Yearly</option>
                        </select>
                      </td>
                      <td>
                        <select
                          className="status-select"
                          value={sub.status?.toLowerCase() || 'pending'}
                          onChange={(e) => handleStatusChange(sub, e.target.value)}
                          style={{
                            backgroundColor: getStatusColor(sub.status),
                            color: getStatusTextColor(sub.status),
                            borderColor: getStatusColor(sub.status)
                          }}
                        >
                          <option value="active">Active</option>
                          <option value="pending">Pending</option>
                          <option value="cancelled">Cancelled</option>
                        </select>
                      </td>
                      <td>
                        <span className="status-pill" style={{
                          backgroundColor: sub.superadmin_status === 'deactivated' ? '#fee2e2' : '#dcfce7',
                          color: sub.superadmin_status === 'deactivated' ? '#991b1b' : '#166534'
                        }}>
                          {sub.superadmin_status || 'Active'}
                        </span>
                      </td>
                      <td>{formatLocalDate(sub.start_date)}</td>
                      <td>{formatLocalDate(sub.end_date)}</td>
                      <td className="mono">{sub.razorpay_subscription_id || '-'}</td>
                      <td>{formatDate(sub.updated_at)}</td>
                      <td>
                        {/* Logic: 
                            - If Deactivated by Admin -> Show Activate (Unlock)
                            - Else If Pending/Cancelled -> Show Activate (Grant)
                            - Else If Active -> Show Deactivate (Lock)
                        */}

                        {(sub.superadmin_status === 'deactivated' || sub.status?.toLowerCase() === 'pending' || sub.status?.toLowerCase() === 'cancelled') && (
                          <button
                            className="btn-activate"
                            onClick={() => handleActivate(sub)}
                          >
                            {sub.superadmin_status === 'deactivated' ? 'Reactivate' : 'Activate'}
                          </button>
                        )}

                        {sub.superadmin_status !== 'deactivated' && sub.status?.toLowerCase() === 'active' && (
                          <button
                            className="btn-deactivate"
                            onClick={() => handleDeactivate(sub)}
                          >
                            Deactivate
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
              </tbody>
            </table>
            {subscriptions.length === 0 && (
              <div className="empty-state">No subscriptions found.</div>
            )}
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
        .btn-logout:hover {
          background: #fff5f5;
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
        .page-header p {
          color: var(--muted-foreground);
        }
        .btn-refresh {
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
        .header-actions {
          display: flex;
          gap: 12px;
          align-items: center;
        }
        .search-box {
          display: flex;
          align-items: center;
          gap: 8px;
          background: white;
          border: 1px solid var(--border);
          padding: 8px 16px;
          border-radius: 8px;
          min-width: 300px;
        }
        .search-box input {
          border: none;
          outline: none;
          font-size: 14px;
          width: 100%;
        }
        .search-box iconify-icon {
          color: #64748b;
          font-size: 18px;
        }
        .table-wrapper {
          background: white;
          border-radius: 12px;
          border: 1px solid var(--border);
          overflow: hidden;
          box-shadow: 0 4px 15px rgba(0,0,0,0.02);
        }
        .sub-table {
          width: 100%;
          border-collapse: collapse;
          text-align: left;
        }
        .sub-table th {
          background: #f1f5f9;
          padding: 16px;
          font-size: 13px;
          font-weight: 600;
          color: #64748b;
          text-transform: uppercase;
          letter-spacing: 0.05em;
        }
        .sub-table td {
          padding: 20px 16px;
          border-bottom: 1px solid #f1f5f9;
          font-size: 14px;
        }
        .user-cell {
          display: flex;
          flex-direction: column;
        }
        .user-cell span {
          font-size: 12px;
          color: var(--muted-foreground);
        }
        .plan-badge {
          background: #e0f2fe;
          color: #0369a1;
          padding: 4px 8px;
          border-radius: 4px;
          font-size: 12px;
          font-weight: 600;
          text-transform: capitalize;
        }
        .status-pill {
          padding: 4px 10px;
          border-radius: 9999px;
          font-size: 12px;
          font-weight: 600;
          text-transform: capitalize;
        }
        .status-select {
          padding: 4px 24px 4px 10px;
          border-radius: 9999px;
          font-size: 12px;
          font-weight: 600;
          text-transform: capitalize;
          border: 1px solid transparent;
          outline: none;
          cursor: pointer;
          appearance: none;
          background-image: url("data:image/svg+xml;charset=US-ASCII,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%22292.4%22%20height%3D%22292.4%22%3E%3Cpath%20fill%3D%22%23007CB2%22%20d%3D%22M287%2069.4a17.6%2017.6%200%200%200-13-5.4H18.4c-5%200-9.3%201.8-12.9%205.4A17.6%2017.6%200%200%200%200%2082.2c0%205%201.8%209.3%205.4%2012.9l128%20127.9c3.6%203.6%207.8%205.4%2012.8%205.4s9.2-1.8%2012.8-5.4L287%2095c3.5-3.5%205.4-7.8%205.4-12.8%200-5-1.9-9.2-5.5-12.8z%22%2F%3E%3C%2Fsvg%3E");
          background-repeat: no-repeat;
          background-position: right 8px center;
          background-size: 8px auto;
        }
        .mono {
          font-family: monospace;
          color: #64748b;
        }
        .plan-select {
          padding: 4px 8px;
          border-radius: 4px;
          font-size: 13px;
          font-weight: 600;
          color: #0369a1;
          background-color: #e0f2fe;
          border: 1px solid #bae6fd;
          cursor: pointer;
          outline: none;
        }
        .plan-select:hover {
          background-color: #bae6fd;
        }
        .btn-activate {
          background: var(--primary);
          color: white;
          border: none;
          padding: 6px 12px;
          border-radius: 6px;
          font-size: 13px;
          font-weight: 600;
          cursor: pointer;
          transition: filter 0.2s;
        }
        .btn-activate:hover {
          filter: brightness(1.1);
        }
        .btn-deactivate {
          background: #fee2e2;
          color: #dc2626;
          border: 1px solid #fecaca;
          padding: 6px 12px;
          border-radius: 6px;
          font-size: 13px;
          font-weight: 600;
          cursor: pointer;
          transition: all 0.2s;
        }
        .btn-deactivate:hover {
          background: #fecaca;
        }
        .pricing-section {
          background: white;
          border-radius: 12px;
          border: 1px solid var(--border);
          padding: 24px;
          margin-bottom: 24px;
          box-shadow: 0 4px 15px rgba(0,0,0,0.02);
        }
        .section-header h2 {
          font-size: 18px;
          font-weight: 700;
          margin-bottom: 4px;
        }
        .section-header p {
          font-size: 13px;
          color: var(--muted-foreground);
          margin-bottom: 20px;
        }
        .pricing-cards {
          display: flex;
          gap: 20px;
          flex-wrap: wrap;
        }
        .pricing-mini-card {
          flex: 1;
          min-width: 250px;
          background: #f8fafc;
          border: 1px solid #e2e8f0;
          border-radius: 8px;
          padding: 16px;
          display: flex;
          align-items: center;
          justify-content: space-between;
          gap: 12px;
        }
        .plan-info {
          display: flex;
          flex-direction: column;
          gap: 4px;
        }
        .plan-label {
          font-size: 12px;
          font-weight: 600;
          text-transform: uppercase;
          color: #64748b;
          letter-spacing: 0.05em;
        }
        .price-input-wrapper {
          display: flex;
          align-items: center;
          gap: 4px;
          font-weight: 700;
          font-size: 18px;
        }
        .inline-price-input {
          width: 80px;
          border: 1px solid #cbd5e1;
          border-radius: 4px;
          padding: 4px 8px;
          font-size: 16px;
          font-weight: 700;
          background: white;
        }
        .btn-update-price {
          background: var(--primary);
          color: white;
          border: none;
          padding: 8px 16px;
          border-radius: 6px;
          font-size: 13px;
          font-weight: 600;
          cursor: pointer;
        }
        .btn-update-price:disabled {
          opacity: 0.6;
        }
        .loading-state, .empty-state, .empty-pricing-state {
          text-align: center;
          padding: 80px;
          background: white;
          border-radius: 12px;
          border: 1px solid var(--border);
          color: var(--muted-foreground);
        }
        .empty-pricing-state {
          padding: 40px;
          flex: 1;
        }
        .empty-pricing-state p {
          margin-bottom: 16px;
        }
      `}</style>
    </div>
  );
}
