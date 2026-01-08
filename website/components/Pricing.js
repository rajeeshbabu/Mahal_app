"use client";

import { useEffect, useState } from 'react';
import { supabase } from '@/lib/supabase';

export default function Pricing() {
  const [pricing, setPricing] = useState([]);
  const [isYearly, setIsYearly] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchPricing();
  }, []);

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
    } finally {
      setLoading(false);
    }
  };

  const currentPlan = pricing.find(p =>
    isYearly ? p.plan_duration?.toLowerCase() === 'yearly' : p.plan_duration?.toLowerCase() === 'monthly'
  );

  const amountRupees = currentPlan ? currentPlan.amount_paise : (isYearly ? 1000 : 100);

  return (
    <section id="pricing" className="section-spacing pricing-section">
      <div className="container">
        <div className="section-header">
          <span className="section-tag">PRICING PLANS</span>
          <h2 className="section-title">Simple, transparent pricing</h2>
          <p className="section-subtitle">
            Choose the plan that best fits your community's needs. All plans include full access to every feature.
          </p>
        </div>

        <div className="pricing-toggle-wrapper">
          <span className={!isYearly ? 'active' : ''}>Monthly</span>
          <button
            className={`pricing-toggle-btn ${isYearly ? 'yearly' : 'monthly'}`}
            onClick={() => setIsYearly(!isYearly)}
          >
            <div className="toggle-thumb"></div>
          </button>
          <span className={isYearly ? 'active' : ''}>Yearly <span className="save-badge">Save 20%</span></span>
        </div>

        <div className="pricing-grid">
          <div className="pricing-card featured">
            <div className="card-header">
              <h3 className="card-title">Professional</h3>
              <p className="card-desc">Perfect for growing and established communities.</p>
            </div>
            <div className="card-price">
              <span className="currency">₹</span>
              <span className="amount">{amountRupees}</span>
              <span className="duration">/{isYearly ? 'year' : 'month'}</span>
            </div>
            <ul className="card-features">
              <li>
                <iconify-icon icon="lucide:check" style={{ color: 'var(--primary)' }}></iconify-icon>
                Unlimited Members & Families
              </li>
              <li>
                <iconify-icon icon="lucide:check" style={{ color: 'var(--primary)' }}></iconify-icon>
                Financial Accounting & Reports
              </li>
              <li>
                <iconify-icon icon="lucide:check" style={{ color: 'var(--primary)' }}></iconify-icon>
                Certificate Generation
              </li>
              <li>
                <iconify-icon icon="lucide:check" style={{ color: 'var(--primary)' }}></iconify-icon>
                SMS & WhatsApp Integration
              </li>
              <li>
                <iconify-icon icon="lucide:check" style={{ color: 'var(--primary)' }}></iconify-icon>
                Cloud Backup & Security
              </li>
              <li>
                <iconify-icon icon="lucide:check" style={{ color: 'var(--primary)' }}></iconify-icon>
                Priority Support
              </li>
            </ul>
            <button className="btn btn-primary card-btn">Get Started Now</button>
          </div>
        </div>
      </div>

      <style jsx>{`
        .pricing-section {
          background-color: white;
        }
        .section-header {
          text-align: center;
          max-width: 600px;
          margin: 0 auto 48px;
        }
        .section-tag {
          color: var(--primary);
          font-weight: 600;
          font-size: 14px;
          margin-bottom: 12px;
          display: block;
        }
        .section-title {
          font-size: 32px;
          font-weight: 700;
          margin-bottom: 16px;
        }
        .section-subtitle {
          color: var(--muted-foreground);
          font-size: 16px;
          line-height: 1.6;
        }
        .pricing-toggle-wrapper {
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 16px;
          margin-bottom: 48px;
          font-weight: 600;
          font-size: 15px;
        }
        .pricing-toggle-wrapper span {
          color: var(--muted-foreground);
        }
        .pricing-toggle-wrapper span.active {
          color: var(--foreground);
        }
        .save-badge {
          background-color: #dcfce7;
          color: #166534;
          padding: 2px 8px;
          border-radius: 99px;
          font-size: 11px;
          margin-left: 4px;
        }
        .pricing-toggle-btn {
          width: 52px;
          height: 28px;
          background-color: #e2e8f0;
          border-radius: 99px;
          border: none;
          position: relative;
          cursor: pointer;
          transition: background-color 0.3s;
        }
        .pricing-toggle-btn.yearly {
          background-color: var(--primary);
        }
        .toggle-thumb {
          width: 20px;
          height: 20px;
          background-color: white;
          border-radius: 50%;
          position: absolute;
          top: 4px;
          left: 4px;
          transition: transform 0.3s;
          box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        .pricing-toggle-btn.yearly .toggle-thumb {
          transform: translateX(24px);
        }
        .pricing-grid {
          display: flex;
          justify-content: center;
          max-width: 480px;
          margin: 0 auto;
        }
        .pricing-card {
           background: white;
           border: 1px solid var(--border);
           border-radius: var(--radius-lg);
           padding: 48px 40px;
           width: 100%;
           display: flex;
           flex-direction: column;
           gap: 24px;
           transition: all 0.3s ease;
           position: relative;
        }
        .pricing-card.featured {
          box-shadow: 0 20px 40px rgba(15, 118, 110, 0.08);
          border-color: var(--primary);
        }
        .card-header {
          text-align: center;
        }
        .card-title {
          font-size: 20px;
          font-weight: 700;
          margin-bottom: 8px;
        }
        .card-desc {
          color: var(--muted-foreground);
          font-size: 14px;
        }
        .card-price {
          text-align: center;
          margin: 16px 0;
        }
        .currency {
          font-size: 24px;
          font-weight: 700;
          vertical-align: top;
          margin-right: 4px;
        }
        .amount {
          font-size: 48px;
          font-weight: 800;
          letter-spacing: -0.02em;
        }
        .duration {
          color: var(--muted-foreground);
          font-size: 16px;
        }
        .card-features {
          list-style: none;
          padding: 0;
          margin: 0;
          display: flex;
          flex-direction: column;
          gap: 16px;
        }
        .card-features li {
          display: flex;
          align-items: center;
          gap: 12px;
          font-size: 15px;
          color: #4b5563;
        }
        .card-btn {
          margin-top: 16px;
          height: 48px;
          font-weight: 600;
        }
      `}</style>
    </section>
  );
}
