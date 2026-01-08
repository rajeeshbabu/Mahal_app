# Adding Subscriptions for Existing Users

## Overview

This guide explains how to add subscription records for your 3 registered users to both SQLite and Supabase.

## Step 1: Get User Emails

First, get the email addresses of your 3 registered users:

### Using DB Browser for SQLite:

1. Open `frontend/mahal.db` in DB Browser
2. Go to "Execute SQL" tab
3. Run this query:

```sql
SELECT id, name AS email, full_name, active FROM admins ORDER BY id;
```

4. Copy the email addresses (from the `email` column)

### Or using SQLite command line:

```bash
sqlite3 frontend/mahal.db "SELECT id, name AS email, full_name FROM admins ORDER BY id;"
```

You'll get output like:
```
1|user1@example.com|User One|1
2|user2@example.com|User Two|1
3|user3@example.com|User Three|1
```

## Step 2: Add Subscriptions to SQLite

1. Open `frontend/mahal.db` in DB Browser
2. Go to "Execute SQL" tab
3. Open `sql/ADD_SUBSCRIPTIONS_FOR_USERS_SQLITE.sql`
4. **Replace** the placeholder emails (`user1@example.com`, `user2@example.com`, `user3@example.com`) with your actual user emails
5. Copy and paste the SQL into DB Browser
6. Click "Execute SQL" (F5)

**Example** (if your emails are sam@example.com, rajeesh@example.com, revu@example.com):

```sql
INSERT INTO subscriptions (user_id, user_email, plan_duration, status, start_date, end_date, razorpay_subscription_id, created_at, updated_at)
VALUES 
    ('sam@example.com', 'sam@example.com', 'monthly', 'active', 
     datetime('now'), datetime('now', '+1 month'), 'sub_dummy_1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('rajeesh@example.com', 'rajeesh@example.com', 'monthly', 'active', 
     datetime('now'), datetime('now', '+1 month'), 'sub_dummy_2', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('revu@example.com', 'revu@example.com', 'yearly', 'active', 
     datetime('now'), datetime('now', '+1 year'), 'sub_dummy_3', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
```

## Step 3: Add Subscriptions to Supabase

1. Open Supabase Dashboard → SQL Editor
2. Open `backend/sql/ADD_SUBSCRIPTIONS_FOR_USERS_SUPABASE.sql`
3. **Replace** the placeholder emails with your actual user emails (same ones from Step 1)
4. Copy and paste the SQL into Supabase SQL Editor
5. Click "Run"

**Example** (same emails):

```sql
INSERT INTO subscriptions (user_id, user_email, plan_duration, status, start_date, end_date, razorpay_subscription_id, created_at, updated_at)
VALUES 
    ('sam@example.com', 'sam@example.com', 'monthly', 'active', 
     NOW(), NOW() + INTERVAL '1 month', 'sub_dummy_1', NOW(), NOW()),
    ('rajeesh@example.com', 'rajeesh@example.com', 'monthly', 'active', 
     NOW(), NOW() + INTERVAL '1 month', 'sub_dummy_2', NOW(), NOW()),
    ('revu@example.com', 'revu@example.com', 'yearly', 'active', 
     NOW(), NOW() + INTERVAL '1 year', 'sub_dummy_3', NOW(), NOW())
ON CONFLICT (razorpay_subscription_id) DO NOTHING;
```

## Step 4: Verify

### SQLite:
```sql
SELECT id, user_email, plan_duration, status, start_date, end_date FROM subscriptions;
```

### Supabase:
```sql
SELECT id, user_email, plan_duration, status, start_date, end_date FROM subscriptions ORDER BY created_at DESC;
```

## What These Subscriptions Include

- **User 1 & 2:** Monthly subscriptions (active, expires in 1 month)
- **User 3:** Yearly subscription (active, expires in 1 year)
- All have status: `'active'`
- All have dummy Razorpay subscription IDs (for testing)

You can modify:
- `plan_duration`: 'monthly' or 'yearly'
- `status`: 'active', 'pending', 'cancelled', 'expired'
- `start_date` and `end_date`: adjust as needed

## Files Created

1. `sql/GET_USER_EMAILS_FIRST.sql` - Query to get user emails
2. `sql/ADD_SUBSCRIPTIONS_FOR_USERS_SQLITE.sql` - SQLite INSERT script
3. `backend/sql/ADD_SUBSCRIPTIONS_FOR_USERS_SUPABASE.sql` - Supabase INSERT script

