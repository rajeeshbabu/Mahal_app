"use client";

export default function Features() {
  const features = [
    {
      icon: 'lucide:users',
      title: 'Family Registry',
      desc: 'Maintain a comprehensive digital register of all families and members with detailed profiles, history, and status tracking.'
    },
    {
      icon: 'lucide:wallet',
      title: 'Accounts & Finance',
      desc: 'Track income, expenses, and donations. Generate monthly financial reports and balance sheets automatically.'
    },
    {
      icon: 'lucide:file-check',
      title: 'Certificates',
      desc: 'Issue Marriage, Birth, Death, and Membership certificates instantly with pre-formatted printable templates.'
    },
    {
      icon: 'lucide:hand-coins',
      title: 'Subscription Tracking',
      desc: 'Manage monthly subscriptions (Varavu) and special collections. Send automated reminders for dues.'
    },
    {
      icon: 'lucide:bell-ring',
      title: 'Communication',
      desc: 'Send SMS and WhatsApp notifications to members regarding meetings, events, or important announcements.'
    },
    {
      icon: 'lucide:bar-chart-3',
      title: 'Analytics & Reports',
      desc: 'Gain insights into demographic data, collection trends, and expense analysis with visual dashboards.'
    }
  ];

  return (
    <section id="features" className="section-spacing">
      <div className="container">
        <div className="section-header">
          <span className="section-tag">KEY FEATURES</span>
          <h2 className="section-title">
            Everything you need to manage your community effectively
          </h2>
          <p className="section-subtitle">
            Designed specifically for Mahal committees to handle daily operations, financial tracking, and member engagement in one place.
          </p>
        </div>

        <div className="features-grid">
          {features.map((feature, index) => (
            <div key={index} className="feature-card">
              <div className="feature-icon">
                <iconify-icon icon={feature.icon} style={{ fontSize: '24px' }}></iconify-icon>
              </div>
              <h3 className="feature-title">{feature.title}</h3>
              <p className="feature-desc">{feature.desc}</p>
            </div>
          ))}
        </div>
      </div>
      <style jsx>{`
        .section-header {
          text-align: center;
          max-width: 700px;
          margin: 0 auto;
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
          letter-spacing: -0.01em;
        }
        .section-subtitle {
          font-size: 16px;
          color: var(--muted-foreground);
          line-height: 1.6;
        }
        .features-grid {
          display: grid;
          grid-template-columns: repeat(3, 1fr);
          gap: 32px;
          margin-top: 64px;
        }
        .feature-card {
          background: var(--card);
          border: 1px solid var(--border);
          border-radius: var(--radius-lg);
          padding: 32px;
          display: flex;
          flex-direction: column;
          gap: 16px;
          transition: all 0.3s ease;
        }
        .feature-card:hover {
          transform: translateY(-5px);
          box-shadow: 0 10px 30px rgba(0,0,0,0.05);
          border-color: var(--primary);
        }
        .feature-icon {
          width: 48px;
          height: 48px;
          border-radius: var(--radius-md);
          background-color: var(--secondary);
          color: var(--primary);
          display: flex;
          align-items: center;
          justify-content: center;
          margin-bottom: 8px;
        }
        .feature-title { font-size: 18px; font-weight: 600; }
        .feature-desc { font-size: 14px; line-height: 1.6; color: var(--muted-foreground); }
        @media (max-width: 968px) {
          .features-grid { grid-template-columns: repeat(2, 1fr); }
        }
        @media (max-width: 640px) {
          .features-grid { grid-template-columns: 1fr; }
        }
      `}</style>
    </section>
  );
}
