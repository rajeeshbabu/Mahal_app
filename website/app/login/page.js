"use client";

export const dynamic = 'force-dynamic';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { supabase } from '@/lib/supabase';
import bcrypt from 'bcryptjs';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const router = useRouter();

  const handleLogin = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      console.log('Attempting login for:', email);
      console.log('Fetching from table: superadmin');

      // 1. Fetch admin record from Supabase superadmin table
      const { data: admin, error: fetchError } = await supabase
        .from('superadmin')
        .select('*')
        .eq('name', email)
        .eq('active', 1)
        .single();

      if (fetchError) {
        console.error('Supabase fetch error:', fetchError);
        throw new Error(`Database error: ${fetchError.message}`);
      }

      console.log('Admin data received:', admin ? 'Yes' : 'No');
      if (!admin) {
        throw new Error('User not found in superadmin table');
      }

      // 2. Compare password with hashed password using bcrypt
      console.log('Validating password with BCrypt...');
      const isPasswordValid = await bcrypt.compare(password, admin.password);
      console.log('Password comparison result:', isPasswordValid);

      if (!isPasswordValid) {
        throw new Error('Invalid credentials');
      }

      // 3. Set a local session (for simplicity in this demo)
      localStorage.setItem('admin_role', 'super_admin');
      localStorage.setItem('admin_email', admin.name);
      console.log('Login successful, redirecting to /subscriptions');

      router.push('/subscriptions');
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <div className="login-header">
          <div className="logo-icon">
            <iconify-icon icon="lucide:building-2" style={{ fontSize: '24px' }}></iconify-icon>
          </div>
          <h1>Super Admin Login</h1>
          <p>Login to manage your Mahal subscriptions</p>
        </div>

        {error && <div className="error-badge">{error}</div>}

        <form onSubmit={handleLogin} className="login-form">
          <div className="form-group">
            <label htmlFor="email">Email Address</label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="admin@example.com"
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              required
            />
          </div>

          <button type="submit" className="login-btn" disabled={loading}>
            {loading ? 'Authenticating...' : 'Sign In'}
          </button>
        </form>

        <div className="login-footer">
          <a href="/">← Back to Website</a>
        </div>
      </div>

      <style jsx>{`
        .login-container {
          min-height: 100vh;
          display: flex;
          align-items: center;
          justify-content: center;
          background: var(--muted);
          padding: 24px;
        }
        .login-card {
          background: white;
          padding: 40px;
          border-radius: 16px;
          box-shadow: 0 10px 40px rgba(0, 0, 0, 0.05);
          width: 100%;
          max-width: 440px;
          border: 1px solid var(--border);
        }
        .login-header {
          text-align: center;
          margin-bottom: 32px;
        }
        .logo-icon {
          width: 48px;
          height: 48px;
          background: var(--primary);
          border-radius: 12px;
          display: flex;
          align-items: center;
          justify-content: center;
          color: white;
          margin: 0 auto 16px;
        }
        .login-header h1 {
          font-size: 24px;
          font-weight: 700;
          margin-bottom: 8px;
        }
        .login-header p {
          color: var(--muted-foreground);
          font-size: 14px;
        }
        .error-badge {
          background: #fee2e2;
          color: #dc2626;
          padding: 12px;
          border-radius: 8px;
          font-size: 14px;
          margin-bottom: 24px;
          text-align: center;
          border: 1px solid #fecaca;
        }
        .login-form {
          display: flex;
          flex-direction: column;
          gap: 20px;
        }
        .form-group {
          display: flex;
          flex-direction: column;
          gap: 8px;
        }
        .form-group label {
          font-size: 14px;
          font-weight: 500;
        }
        .form-group input {
          width: 100%;
          padding: 12px;
          border-radius: 8px;
          border: 1px solid var(--border);
          background: var(--background);
          font-size: 14px;
          transition: border-color 0.2s;
        }
        .form-group input:focus {
          outline: none;
          border-color: var(--primary);
        }
        .login-btn {
          background: var(--primary);
          color: white;
          padding: 12px;
          border-radius: 8px;
          font-weight: 600;
          border: none;
          cursor: pointer;
          transition: filter 0.2s;
          margin-top: 12px;
        }
        .login-btn:hover {
          filter: brightness(1.1);
        }
        .login-btn:disabled {
          opacity: 0.7;
          cursor: not-allowed;
        }
        .login-footer {
          margin-top: 32px;
          text-align: center;
        }
        .login-footer a {
          font-size: 14px;
          color: var(--muted-foreground);
          text-decoration: none;
        }
        .login-footer a:hover {
          color: var(--primary);
        }
      `}</style>
    </div>
  );
}
