"use client";

export default function CTA() {
  return (
    <section className="section-spacing">
      <div className="container">
        <div className="cta-box">
          <h2 className="cta-title">Ready to modernize your Mahal?</h2>
          <p className="cta-desc">
            Join hundreds of forward-thinking committees that have switched to digital management. Start your 14-day free trial today.
          </p>
          <button className="btn btn-primary cta-btn">
            Get Started Now
          </button>
        </div>
      </div>
      <style jsx>{`
        .cta-box {
          background-color: var(--primary);
          border-radius: var(--radius-lg);
          padding: 80px 40px;
          text-align: center;
          color: white;
        }
        .cta-title {
          font-size: 36px;
          font-weight: 700;
          margin-bottom: 24px;
          color: white;
        }
        .cta-desc {
          font-size: 18px;
          color: rgba(255, 255, 255, 0.9);
          max-width: 600px;
          margin: 0 auto 40px;
          line-height: 1.6;
        }
        .cta-btn {
          background: white;
          color: var(--primary);
          padding: 14px 32px;
          font-size: 16px;
          font-weight: 600;
        }
        .cta-btn:hover {
          background: var(--secondary);
          color: var(--primary);
        }
      `}</style>
    </section>
  );
}
