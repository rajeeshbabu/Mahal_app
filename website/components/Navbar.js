"use client";

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';

export default function Navbar() {
  const [isAdmin, setIsAdmin] = useState(false);
  const router = useRouter();

  useEffect(() => {
    setIsAdmin(!!localStorage.getItem('admin_email'));
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('admin_role');
    localStorage.removeItem('admin_email');
    setIsAdmin(false);
    router.push('/');
  };

  return (
    <nav className="navbar">
      <div className="container nav-content">
        <div className="logo" onClick={() => router.push('/')} style={{ cursor: 'pointer' }}>
          <div className="logo-icon">
            <iconify-icon icon="lucide:building-2" style={{ fontSize: '18px' }}></iconify-icon>
          </div>
          Mahal Management System
        </div>
        <div className="nav-links">
          <a href="#features" className="nav-link">Features</a>
          <a href="#solutions" className="nav-link">Solutions</a>
          <a href="#pricing" className="nav-link">Pricing</a>
          <a href="#contact" className="nav-link">Contact</a>
        </div>
        <div className="nav-actions">
          {isAdmin ? (
            <>
              <button
                className="btn btn-outline"
                onClick={() => router.push('/subscriptions')}
              >
                Dashboard
              </button>
              <button className="btn btn-ghost" onClick={handleLogout}>Logout</button>
            </>
          ) : (
            <>
              <button className="btn btn-ghost" onClick={() => router.push('/login')}>Sign in</button>
              <button className="btn btn-primary">Get Started</button>
            </>
          )}
        </div>
      </div>
      <style jsx>{`
        .navbar {
          height: 72px;
          border-bottom: 1px solid var(--border);
          display: flex;
          align-items: center;
          background: rgba(255, 255, 255, 0.9);
          backdrop-filter: blur(8px);
          position: sticky;
          top: 0;
          z-index: 50;
        }
        .nav-content {
          display: flex;
          align-items: center;
          justify-content: space-between;
          width: 100%;
        }
        .logo {
          display: flex;
          align-items: center;
          gap: 10px;
          font-weight: 700;
          font-size: 20px;
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
        .nav-links {
          display: flex;
          gap: 32px;
        }
        .nav-link {
          color: var(--muted-foreground);
          font-size: 14px;
          font-weight: 500;
          transition: color 0.2s;
        }
        .nav-link:hover {
          color: var(--primary);
        }
        .nav-actions {
          display: flex;
          gap: 12px;
        }
        @media (max-width: 768px) {
          .nav-links { display: none; }
        }
      `}</style>
    </nav>
  );
}
