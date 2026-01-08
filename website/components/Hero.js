"use client";

export default function Hero() {
  return (
    <section className="hero">
      <div className="container">
        <div className="hero-grid">
          <div>
            <div className="hero-badge">
              <iconify-icon icon="lucide:sparkles" style={{ fontSize: '14px', marginRight: '6px' }}></iconify-icon>
              New 2.0 Version Released
            </div>
            <h1 className="hero-title">
              Smart Management for Your Mahal Community
            </h1>
            <p className="hero-desc">
              Streamline administrative tasks, manage memberships, track donations, and issue certificates effortlessly. The complete digital solution for modern Mahal committees.
            </p>
            <div className="hero-actions">
              <button className="btn btn-primary" style={{ height: '48px', padding: '0 32px', fontSize: '16px' }}>
                Start Free Trial
                <iconify-icon icon="lucide:arrow-right" style={{ fontSize: '18px' }}></iconify-icon>
              </button>
              <button className="btn btn-outline" style={{ height: '48px', padding: '0 24px', fontSize: '16px' }}>
                <iconify-icon icon="lucide:play-circle" style={{ fontSize: '18px' }}></iconify-icon>
                Watch Demo
              </button>
            </div>
            <div className="trusted-by">
              <div className="avatar-group">
                <img src="https://app.banani.co/avatar1.jpeg" alt="User" />
                <img src="https://app.banani.co/avatar2.jpg" alt="User" />
                <img src="https://app.banani.co/avatar4.jpg" alt="User" />
              </div>
              <div className="trusted-text">
                Trusted by <strong>500+</strong> communities
              </div>
            </div>
          </div>
          <div className="hero-image-wrapper">
            <div className="hero-image">
              <img
                src="https://images.unsplash.com/photo-1623278132336-bd316c0f9c78?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w2MjYyMDB8MHwxfHNlYXJjaHwxfHxkYXNoYm9hcmQlMjBzb2Z0d2FyZSUyMGludGVyZmFjZSUyMGNsZWFuJTIwd2hpdGV8ZW58MHx8fHwxNzY3MTU1ODYyfDA&ixlib=rb-4.1.0&q=80&w=1080"
                alt="Dashboard Preview"
              />
            </div>
            <div className="floating-card">
              <div className="card-icon">
                <iconify-icon icon="lucide:check-circle-2" style={{ fontSize: '20px' }}></iconify-icon>
              </div>
              <div>
                <div className="card-title">Monthly Report</div>
                <div className="card-subtitle">Generated Successfully</div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <style jsx>{`
        .hero {
          padding: 100px 0;
          background: linear-gradient(to bottom, var(--secondary) 0%, var(--background) 100%);
        }
        .hero-grid {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 64px;
          align-items: center;
        }
        .hero-badge {
          display: inline-flex;
          align-items: center;
          padding: 6px 12px;
          background-color: var(--secondary);
          color: var(--primary);
          border-radius: 9999px;
          font-size: 12px;
          font-weight: 600;
          margin-bottom: 24px;
          border: 1px solid rgba(15, 118, 110, 0.1);
        }
        .hero-title {
          font-size: 48px;
          line-height: 1.1;
          font-weight: 700;
          letter-spacing: -0.02em;
          margin-bottom: 24px;
          color: var(--foreground);
        }
        .hero-desc {
          font-size: 18px;
          line-height: 1.6;
          color: var(--muted-foreground);
          margin-bottom: 32px;
          max-width: 540px;
        }
        .hero-actions {
          display: flex;
          gap: 16px;
        }
        .trusted-by {
          margin-top: 40px;
          display: flex;
          align-items: center;
          gap: 16px;
        }
        .avatar-group {
          display: flex;
        }
        .avatar-group img {
          width: 36px;
          height: 36px;
          border-radius: 50%;
          border: 2px solid white;
          margin-left: -10px;
        }
        .avatar-group img:first-child { margin-left: 0; }
        .trusted-text {
          font-size: 14px;
          color: var(--muted-foreground);
        }
        .trusted-text strong { color: var(--foreground); }
        .hero-image-wrapper { position: relative; }
        .hero-image {
          width: 100%;
          border-radius: var(--radius-lg);
          box-shadow: 0 20px 40px -10px rgba(0,0,0,0.1);
          border: 1px solid var(--border);
          overflow: hidden;
        }
        .hero-image img { width: 100%; height: auto; display: block; }
        .floating-card {
          position: absolute;
          bottom: -20px;
          left: -20px;
          background: white;
          padding: 16px;
          border-radius: 12px;
          box-shadow: 0 10px 30px rgba(0,0,0,0.1);
          border: 1px solid var(--border);
          display: flex;
          align-items: center;
          gap: 12px;
        }
        .card-icon {
          width: 40px;
          height: 40px;
          background: #dcfce7;
          border-radius: 8px;
          display: flex;
          align-items: center;
          justify-content: center;
          color: #166534;
        }
        .card-title { font-weight: 600; font-size: 14px; }
        .card-subtitle { font-size: 12px; color: var(--muted-foreground); }
        @media (max-width: 968px) {
          .hero-grid { grid-template-columns: 1fr; gap: 48px; }
          .hero-title { font-size: 36px; }
        }
      `}</style>
    </section>
  );
}
