"use client";

export default function Footer() {
  return (
    <footer id="contact" className="footer">
      <div className="container">
        <div className="footer-grid">
          <div className="footer-brand">
            <div className="logo">
              <div className="logo-icon">
                <iconify-icon icon="lucide:building-2" style={{ fontSize: '16px' }}></iconify-icon>
              </div>
              Mahal Management System
            </div>
            <p>
              The leading management software solution for community organizations, mosques, and parish administrations.
            </p>
          </div>
          <div className="footer-col">
            <h4>Product</h4>
            <div className="footer-links">
              <a href="#" className="footer-link">Features</a>
              <a href="#" className="footer-link">Pricing</a>
              <a href="#" className="footer-link">Updates</a>
              <a href="#" className="footer-link">Security</a>
            </div>
          </div>
          <div className="footer-col">
            <h4>Company</h4>
            <div className="footer-links">
              <a href="#" className="footer-link">About Us</a>
              <a href="#" className="footer-link">Careers</a>
              <a href="#" className="footer-link">Contact</a>
              <a href="#" className="footer-link">Partners</a>
            </div>
          </div>
          <div className="footer-col">
            <h4>Resources</h4>
            <div className="footer-links">
              <a href="#" className="footer-link">Help Center</a>
              <a href="#" className="footer-link">Guides</a>
              <a href="#" className="footer-link">API Status</a>
              <a href="#" className="footer-link">Privacy Policy</a>
            </div>
          </div>
        </div>
        <div className="footer-bottom">
          <div>© 2026 Mahal Managment System Inc. All rights reserved.</div>
          <div className="social-links">
            <a href="#"><iconify-icon icon="lucide:twitter"></iconify-icon></a>
            <a href="#"><iconify-icon icon="lucide:facebook"></iconify-icon></a>
            <a href="#"><iconify-icon icon="lucide:linkedin"></iconify-icon></a>
            <a href="#"><iconify-icon icon="lucide:instagram"></iconify-icon></a>
          </div>
        </div>
      </div>
      <style jsx>{`
        .footer {
          padding: 64px 0 32px;
          background-color: var(--card);
          border-top: 1px solid var(--border);
        }
        .footer-grid {
          display: grid;
          grid-template-columns: 2fr 1fr 1fr 1fr;
          gap: 40px;
          margin-bottom: 64px;
        }
        .footer-brand p {
          margin-top: 16px;
          color: var(--muted-foreground);
          font-size: 14px;
          line-height: 1.6;
          max-width: 300px;
        }
        .footer-col h4 {
          font-size: 14px;
          font-weight: 600;
          margin-bottom: 24px;
          color: var(--foreground);
        }
        .footer-links {
          display: flex;
          flex-direction: column;
          gap: 12px;
        }
        .footer-link {
          color: var(--muted-foreground);
          font-size: 14px;
          transition: color 0.2s;
        }
        .footer-link:hover { color: var(--primary); }
        .logo { display: flex; align-items: center; gap: 10px; font-weight: 700; font-size: 20px; color: var(--primary); }
        .logo-icon { width: 28px; height: 28px; background: var(--primary); border-radius: 6px; display: flex; align-items: center; justify-content: center; color: white; }
        .footer-bottom {
          padding-top: 32px;
          border-top: 1px solid var(--border);
          display: flex;
          justify-content: space-between;
          align-items: center;
          color: var(--muted-foreground);
          font-size: 13px;
        }
        .social-links { display: flex; gap: 24px; }
        .social-links a { color: inherit; font-size: 20px; transition: color 0.2s; }
        .social-links a:hover { color: var(--primary); }
        @media (max-width: 768px) {
          .footer-grid { grid-template-columns: 1fr 1fr; }
          .footer-brand { grid-column: span 2; }
          .footer-bottom { flex-direction: column; gap: 20px; text-align: center; }
        }
      `}</style>
    </footer>
  );
}
