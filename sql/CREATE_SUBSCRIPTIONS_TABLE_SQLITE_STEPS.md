# Create Subscriptions Table in SQLite Database (mahal.db)

## Overview

This guide explains how to create the subscriptions table in the frontend SQLite database (`frontend/mahal.db`).

**Note:** Subscriptions are typically managed by the backend H2 database. However, if you need the table in SQLite as well, follow these steps.

## Method 1: Using DB Browser for SQLite (Recommended)

1. **Download DB Browser for SQLite:** https://sqlitebrowser.org/
2. **Open the database:**
   - File → Open Database
   - Navigate to: `frontend/mahal.db`
3. **Execute SQL:**
   - Go to "Execute SQL" tab
   - Copy and paste the SQL from `sql/CREATE_SUBSCRIPTIONS_TABLE_SQLITE.sql`
   - Click "Execute SQL" or press F5
4. **Verify:**
   - Go to "Browse Data" tab
   - Select "subscriptions" table from dropdown
   - You should see an empty table with the correct columns

## Method 2: Using SQLite Command Line

If you have SQLite installed:

```bash
sqlite3 frontend/mahal.db < sql/CREATE_SUBSCRIPTIONS_TABLE_SQLITE.sql
```

Or interactively:

```bash
sqlite3 frontend/mahal.db
```

Then paste the SQL:

```sql
CREATE TABLE IF NOT EXISTS subscriptions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id TEXT NOT NULL,
    user_email TEXT,
    plan_duration TEXT NOT NULL,
    status TEXT NOT NULL,
    start_date TEXT,
    end_date TEXT,
    razorpay_subscription_id TEXT UNIQUE,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_subscriptions_user_id ON subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_user_email ON subscriptions(user_email);
CREATE INDEX IF NOT EXISTS idx_subscriptions_status ON subscriptions(status);
CREATE INDEX IF NOT EXISTS idx_subscriptions_razorpay_id ON subscriptions(razorpay_subscription_id);
```

## SQL Script

The complete SQL script is in: `sql/CREATE_SUBSCRIPTIONS_TABLE_SQLITE.sql`

## Verification

After creating the table, verify it exists:

```sql
-- Check if table exists
SELECT name FROM sqlite_master WHERE type='table' AND name='subscriptions';

-- View table structure
PRAGMA table_info(subscriptions);

-- View all columns
SELECT * FROM subscriptions LIMIT 1;
```

## Important Notes

1. **SQLite vs H2 Differences:**
   - SQLite uses `INTEGER PRIMARY KEY AUTOINCREMENT`
   - SQLite uses `TEXT` for strings and timestamps (not VARCHAR or TIMESTAMP)
   - SQLite uses `CURRENT_TIMESTAMP` for default timestamps

2. **Subscriptions Management:**
   - Subscriptions are primarily stored in the **backend H2 database**
   - The frontend SQLite database (`mahal.db`) is for local data (members, masjids, etc.)
   - If you create this table in SQLite, it's mainly for reference or if you plan to sync subscriptions locally

3. **Data Consistency:**
   - If you maintain subscriptions in both databases, you'll need to keep them in sync
   - The backend is the primary source of truth for subscriptions

