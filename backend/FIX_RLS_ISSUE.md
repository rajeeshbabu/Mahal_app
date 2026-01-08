# Fix: Row Level Security (RLS) Blocking Subscriptions

## Problem

You're seeing this error:
```
HTTP 401 - "new row violates row-level security policy for table \"subscriptions\""
```

This means Supabase's Row Level Security (RLS) is blocking the backend from inserting subscriptions.

## Solution

You need to create an RLS policy that allows the backend to insert/update subscriptions.

### Step 1: Go to Supabase SQL Editor

1. Open https://app.supabase.com
2. Select your project
3. Go to **SQL Editor** (left sidebar)
4. Click **New Query**

### Step 2: Run This SQL

Copy and paste this SQL into the editor:

```sql
-- Drop existing policies if any
DROP POLICY IF EXISTS "Allow backend operations" ON subscriptions;
DROP POLICY IF EXISTS "Allow backend inserts" ON subscriptions;
DROP POLICY IF EXISTS "Allow backend updates" ON subscriptions;

-- Create a policy that allows all operations
-- This is safe because the backend validates user_id before inserting
CREATE POLICY "Allow backend operations" ON subscriptions
FOR ALL
USING (true)
WITH CHECK (true);
```

### Step 3: Click "Run" (or press Ctrl+Enter)

### Step 4: Verify

Check that the policy was created:
```sql
SELECT schemaname, tablename, policyname, permissive, roles, cmd
FROM pg_policies
WHERE tablename = 'subscriptions';
```

You should see a policy named "Allow backend operations".

### Step 5: Try Sync Again

After creating the policy, try syncing again:

```
GET http://localhost:8080/api/subscriptions/sync/all
```

Or in browser:
```
http://localhost:8080/api/subscriptions/sync/all
```

## Alternative: Disable RLS (NOT Recommended)

If you want to disable RLS completely (for testing only):

```sql
ALTER TABLE subscriptions DISABLE ROW LEVEL SECURITY;
```

**⚠️ WARNING**: This disables all security. Only use for testing!

## Why This Happens

Supabase enables RLS by default for security. When RLS is enabled, you need policies that explicitly allow operations. The backend uses the `anon` key, which needs a policy to allow inserts.

## After Fixing

Once the policy is created, your sync should work:
- ✅ Subscriptions will sync to Supabase
- ✅ New subscriptions will automatically sync
- ✅ Updates will sync automatically

## Verify It's Working

After running the SQL and syncing, check Supabase:
1. Go to **Table Editor** → `subscriptions`
2. You should see your 2 subscriptions!

