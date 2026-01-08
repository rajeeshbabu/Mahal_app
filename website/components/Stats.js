"use client";

export default function Stats() {
  const stats = [
    { value: '500+', label: 'Mahals Registered' },
    { value: '2M+', label: 'Members Managed' },
    { value: '100%', label: 'Secure Data' },
    { value: '24/7', label: 'Premium Support' },
  ];

  return (
    <section className="stats-container">
      <div className="container">
        <div className="stats-grid">
          {stats.map((stat, index) => (
            <div key={index} className="stat-item">
              <div className="stat-value">{stat.value}</div>
              <div className="stat-label">{stat.label}</div>
            </div>
          ))}
        </div>
      </div>
      <style jsx>{`
        .stats-container {
          border-top: 1px solid var(--border);
          border-bottom: 1px solid var(--border);
          background: var(--card);
        }
        .stats-grid {
          display: grid;
          grid-template-columns: repeat(4, 1fr);
        }
        .stat-item {
          padding: 40px;
          text-align: center;
          border-right: 1px solid var(--border);
        }
        .stat-item:last-child { border-right: none; }
        .stat-value {
          font-size: 36px;
          font-weight: 700;
          color: var(--primary);
          margin-bottom: 8px;
        }
        .stat-label {
          font-size: 14px;
          color: var(--muted-foreground);
          font-weight: 500;
        }
        @media (max-width: 768px) {
          .stats-grid { grid-template-columns: repeat(2, 1fr); }
          .stat-item:nth-child(2) { border-right: none; }
          .stat-item { border-bottom: 1px solid var(--border); }
          .stat-item:nth-last-child(-n+2) { border-bottom: none; }
        }
      `}</style>
    </section>
  );
}
