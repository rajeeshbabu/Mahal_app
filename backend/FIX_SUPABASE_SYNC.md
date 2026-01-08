# Fix: Subscriptions Not Syncing to Supabase

## Problem
Subscriptions are being saved to SQLite but NOT syncing to Supabase. The Supabase table is empty.

## Root Cause
Supabase credentials are **not configured** in `application.properties`. The sync code checks if Supabase is configured and skips sync if it's not.

## Solution

### Step 1: Get Your Supabase Credentials

1. Go to your Supabase project: https://app.supabase.com
2. Select your project
3. Go to **Settings** → **API**
4. Copy:
   - **Project URL** (e.g., `https://xxxxx.supabase.co`)
   - **anon/public key** (the `anon` key, not the `service_role` key)

### Step 2: Configure Supabase in Backend

Edit `backend/src/main/resources/application.properties`:

```properties
# Supabase Configuration
supabase.url=https://YOUR-PROJECT-ID.supabase.co
supabase.key=YOUR-ANON-KEY-HERE
```

**Replace:**
- `YOUR-PROJECT-ID` with your actual Supabase project ID
- `YOUR-ANON-KEY-HERE` with your actual anon/public key

**Example:**
```properties
supabase.url=https://abcdefghijklmnop.supabase.co
supabase.key=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFiY2RlZmdoaWprbG1ub3AiLCJyb2xlIjoiYW5vbiIsImlhdCI6MTY0NTIzNDU2MywiZXhwIjoxOTYwODEwNTYzfQ.xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

### Step 3: Restart Backend

After configuring, **restart your Spring Boot backend** for changes to take effect.

### Step 4: Sync Existing Subscriptions

Your existing subscriptions in SQLite need to be synced to Supabase. You have two options:

#### Option A: Manual Sync via API (Recommended)

```bash
# Sync all existing subscriptions to Supabase
curl http://localhost:8080/api/subscriptions/sync/all
```

Or use a browser/Postman:
```
GET http://localhost:8080/api/subscriptions/sync/all
```

#### Option B: Check Backend Logs

After restarting, check backend logs. You should see:
- ✅ `✓ Successfully synced subscription to Supabase` (if configured correctly)
- ⚠️ `⚠️ Supabase not configured` (if still not configured)

### Step 5: Verify Sync

1. **Check Supabase Dashboard:**
   - Go to your Supabase project
   - Navigate to **Table Editor** → `subscriptions`
   - You should see your subscriptions

2. **Test New Subscription:**
   - Create a new subscription through the frontend
   - It should automatically sync to Supabase
   - Check logs for: `✓ Successfully synced subscription to Supabase`

## Troubleshooting

### Issue: Still seeing "Supabase not configured" after configuring

**Check:**
1. ✅ Did you uncomment the lines? (Remove the `#` at the start)
2. ✅ Did you replace the placeholder values?
3. ✅ Did you restart the backend?
4. ✅ Check for typos in the URL/key

**Verify configuration:**
```bash
# Check if backend can read the config (look for errors in startup logs)
# The backend should start without errors
```

### Issue: Getting HTTP 401/403 errors

**Possible causes:**
1. Wrong API key (using `service_role` instead of `anon` key)
2. Row Level Security (RLS) blocking inserts
3. Missing RLS policies

**Fix RLS:**
```sql
-- In Supabase SQL Editor, run:
ALTER TABLE subscriptions ENABLE ROW LEVEL SECURITY;

-- Create a policy that allows inserts (adjust as needed):
CREATE POLICY "Allow inserts for authenticated users"
ON subscriptions FOR INSERT
WITH CHECK (true);

-- Or disable RLS temporarily for testing:
ALTER TABLE subscriptions DISABLE ROW LEVEL SECURITY;
```

### Issue: Getting HTTP 400/422 errors

**Possible causes:**
1. Missing required columns in Supabase table
2. Data type mismatches
3. Missing `user_id` field

**Fix:**
Ensure your Supabase `subscriptions` table has all required columns:
```sql
CREATE TABLE subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id TEXT NOT NULL,
    user_email TEXT,
    plan_duration TEXT NOT NULL,
    status TEXT NOT NULL,
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    razorpay_subscription_id TEXT UNIQUE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
```

## Quick Test

After configuring, test with:

```bash
# 1. Sync existing subscriptions
curl http://localhost:8080/api/subscriptions/sync/all

# 2. Check response - should show:
# {"success":true,"message":"Synced 2 subscriptions to Supabase","syncedCount":2}

# 3. Verify in Supabase dashboard
```

## Next Steps

Once sync is working:
1. ✅ New subscriptions will automatically sync to Supabase
2. ✅ Updates will automatically sync to Supabase
3. ✅ Backend will pull from Supabase every 5 minutes (bidirectional sync)

