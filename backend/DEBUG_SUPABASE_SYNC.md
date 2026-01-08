# Debug: Supabase Sync Not Working

## Quick Debugging Steps

### Step 1: Check if Backend is Running with New Config

**IMPORTANT**: You must restart the backend after changing `application.properties`!

1. Stop the backend (Ctrl+C)
2. Start it again:
   ```bash
   cd backend
   mvn spring-boot:run
   ```

### Step 2: Check if Supabase is Configured

Open in browser:
```
http://localhost:8080/api/test/subscriptions/list
```

This will show:
- How many subscriptions are in SQLite
- Whether Supabase is configured
- Supabase URL

### Step 3: Try Manual Sync with Detailed Logging

**Option A: Sync all subscriptions**
```
GET http://localhost:8080/api/subscriptions/sync/all
```

**Option B: Test sync a single subscription (shows detailed logs)**
```
POST http://localhost:8080/api/test/subscriptions/sync/{id}
```
Replace `{id}` with the actual subscription ID (usually 1 or 2)

### Step 4: Check Backend Logs

Look for these messages:

**✅ Success:**
```
✓ Successfully inserted subscription to Supabase
✅ Successfully synced subscription to Supabase
```

**❌ Error - Supabase not configured:**
```
⚠️  ⚠️  ⚠️  Supabase not configured! ⚠️  ⚠️  ⚠️
```

**❌ Error - HTTP errors:**
```
✗ Supabase insert failed: HTTP 401
✗ Supabase insert failed: HTTP 403
✗ Supabase insert failed: HTTP 400
```

## Common Issues

### Issue 1: Backend Not Restarted

**Symptom**: Still seeing "Supabase not configured" after updating config

**Fix**: 
1. Stop backend (Ctrl+C)
2. Restart: `mvn spring-boot:run`
3. Try sync again

### Issue 2: HTTP 401/403 (Unauthorized)

**Symptom**: `✗ Supabase insert failed: HTTP 401` or `HTTP 403`

**Causes**:
- Wrong API key
- Using `service_role` key instead of `anon` key
- Key has expired

**Fix**: 
1. Go to Supabase → Settings → API
2. Copy the `anon` key (not `service_role`)
3. Update `application.properties`
4. Restart backend

### Issue 3: HTTP 400/422 (Bad Request)

**Symptom**: `✗ Supabase insert failed: HTTP 400`

**Causes**:
- Missing required columns in Supabase table
- Wrong data types
- Row Level Security (RLS) blocking insert

**Fix**:
1. Check Supabase table schema matches expected format
2. Check RLS policies allow inserts
3. Temporarily disable RLS for testing:
   ```sql
   ALTER TABLE subscriptions DISABLE ROW LEVEL SECURITY;
   ```

### Issue 4: Table Doesn't Exist

**Symptom**: `HTTP 404` or connection errors

**Fix**:
1. Go to Supabase dashboard
2. Check if `subscriptions` table exists
3. If not, create it using SQL script

## Verify Supabase Table Schema

Run this in Supabase SQL Editor:

```sql
-- Check if table exists and see its structure
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'subscriptions'
ORDER BY ordinal_position;

-- Check RLS status
SELECT tablename, rowsecurity 
FROM pg_tables 
WHERE tablename = 'subscriptions';
```

## Expected Table Schema

Your `subscriptions` table should have:

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

## Test Endpoints Created

I've added a test controller with these endpoints:

1. **List all subscriptions** (check what's in SQLite):
   ```
   GET http://localhost:8080/api/test/subscriptions/list
   ```

2. **Test sync single subscription** (with detailed logging):
   ```
   POST http://localhost:8080/api/test/subscriptions/sync/{id}
   ```

## Next Steps

1. ✅ **Restart backend** (if you haven't already)
2. ✅ **Test configuration**: `GET /api/test/subscriptions/list`
3. ✅ **Try sync**: `GET /api/subscriptions/sync/all`
4. ✅ **Check logs** for error messages
5. ✅ **Verify Supabase table** exists and has correct schema
6. ✅ **Check RLS policies** if getting 401/403 errors

## Still Not Working?

Share these details:
1. What HTTP error code you're seeing (if any)
2. Backend log messages
3. Response from `/api/test/subscriptions/list`
4. Whether Supabase table exists and has correct schema

