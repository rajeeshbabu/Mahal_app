# Fix: Subscriptions Not Syncing to Supabase

## Problem

When a user subscribes:
- ✅ Subscription IS saved to **backend H2 database** (correct)
- ❌ Subscription is NOT syncing to **Supabase** (Supabase not configured)
- ℹ️  Subscriptions are NOT stored in SQLite - they're in backend H2 (this is correct by design)

## Root Cause

**Supabase is not configured** in `backend/src/main/resources/application.properties`. The sync code checks if Supabase is configured, and if not, it silently skips the sync.

## Solution: Configure Supabase

### Step 1: Get Your Supabase Credentials

1. Go to your Supabase Dashboard: https://supabase.com/dashboard
2. Select your project
3. Go to **Settings** → **API**
4. Copy:
   - **Project URL** (e.g., `https://xxxxx.supabase.co`)
   - **anon/public key** (under "Project API keys")

### Step 2: Update application.properties

Edit `backend/src/main/resources/application.properties`:

**Find these lines (currently commented):**
```properties
# supabase.url=https://your-project-id.supabase.co
# supabase.key=your-supabase-anon-key
```

**Uncomment and add your values:**
```properties
supabase.url=https://your-actual-project-id.supabase.co
supabase.key=your-actual-anon-key-here
```

### Step 3: Create Subscriptions Table in Supabase

Make sure the subscriptions table exists in Supabase:

1. Open Supabase Dashboard → **SQL Editor**
2. Run the SQL script: `backend/sql/CREATE_SUBSCRIPTIONS_TABLE_SUPABASE.sql`
3. Or if table exists but missing `user_email` column, run: `backend/sql/ADD_USER_EMAIL_SUPABASE.sql`

### Step 4: Restart Backend

1. **Stop the backend application**
2. **Start it again** (to load the new configuration)
3. The backend will now sync subscriptions to Supabase

### Step 5: Test

1. Create a new subscription via the frontend
2. Check backend console logs - you should see:
   ```
   ✓ Successfully synced subscription to Supabase (user_id: user@example.com)
   ```
3. Check Supabase - Go to **Table Editor** → `subscriptions` table - you should see the new subscription

## Verify Subscription is in Backend H2

Even if Supabase isn't configured, subscriptions ARE being saved. To verify:

1. Open H2 Console: `http://localhost:8080/h2-console`
2. Connect: `jdbc:h2:file:./data/mahal_db`, user: `sa`, password: (empty)
3. Run:
```sql
SELECT id, user_id, user_email, plan_duration, status, created_at 
FROM subscriptions 
ORDER BY created_at DESC;
```

You should see all subscriptions there.

## About SQLite

**Important:** Subscriptions are NOT stored in the frontend SQLite database (`frontend/mahal.db`). They're stored in:

- **Backend H2 Database** (`backend/data/mahal_db`) - PRIMARY storage ✅
- **Supabase** - Cloud sync (after configuration) ✅
- **Frontend SQLite** - NOT used for subscriptions (by design)

This is correct architecture:
- Frontend SQLite = Local app data (members, masjids, etc.)
- Backend H2 = Subscription data (managed by backend API)
- Supabase = Cloud backup/sync

If you see subscriptions in H2 but not in Supabase, the issue is just Supabase configuration - subscriptions are working correctly!

## Summary

1. ✅ Subscriptions ARE being saved (in backend H2)
2. ❌ Supabase sync not working (because Supabase not configured)
3. 🔧 Fix: Configure Supabase URL and key in `application.properties`
4. 🔄 Restart backend to apply changes


