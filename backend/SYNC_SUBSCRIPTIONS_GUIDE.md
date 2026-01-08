# Complete Guide: Syncing Subscriptions to Backend H2 and Supabase

## How Subscriptions Are Stored

### New Subscriptions (Automatic)

When a user subscribes through the frontend:

1. **Frontend** → Calls backend API: `POST /api/subscriptions/create`
2. **Backend** → `RazorpaySubscriptionService.createSubscription()`:
   - Creates Subscription entity
   - Saves to **Backend H2 database** via `subscriptionRepository.save()`
   - Attempts to sync to **Supabase** (if configured)
3. **Result:**
   - ✅ Saved in Backend H2 database (`backend/data/mahal_db`)
   - ✅ Synced to Supabase (if configured)

**This happens automatically - no manual steps needed!**

### Existing Subscriptions (Manual Sync Needed)

If you have existing subscriptions that were created before Supabase sync was set up, you need to manually sync them.

## Step 1: Verify Subscriptions in Backend H2

1. **Open H2 Console:** `http://localhost:8080/h2-console`
2. **Connect:**
   - JDBC URL: `jdbc:h2:file:./data/mahal_db`
   - Username: `sa`
   - Password: (empty)
3. **Check existing subscriptions:**
```sql
SELECT id, user_id, user_email, plan_duration, status, created_at 
FROM subscriptions 
ORDER BY created_at DESC;
```

## Step 2: Configure Supabase Sync

Edit `backend/src/main/resources/application.properties`:

```properties
# Uncomment and fill in your Supabase credentials:
supabase.url=https://your-project-id.supabase.co
supabase.key=your-supabase-anon-key
```

**Get your credentials:**
- Supabase Dashboard → Settings → API
- Copy Project URL and anon/public key

## Step 3: Create Subscriptions Table in Supabase

Run this SQL in Supabase SQL Editor:
- `backend/sql/CREATE_SUBSCRIPTIONS_TABLE_SUPABASE.sql`

Or if table exists but missing `user_email` column:
- `backend/sql/ADD_USER_EMAIL_SUPABASE.sql`

## Step 4: Sync Existing Subscriptions to Supabase

You have two options:

### Option A: Use REST API (Recommended)

After configuring Supabase and restarting backend:

1. **Sync all subscriptions:**
   ```
   GET http://localhost:8080/api/subscriptions/sync/all
   ```
   
   Or open in browser: `http://localhost:8080/api/subscriptions/sync/all`

2. **Check backend console** - you'll see:
   ```
   ========================================
   Syncing X subscriptions to Supabase...
   ========================================
   ✓ Synced subscription ID: 1 (user: user@example.com)
   ✓ Synced subscription ID: 2 (user: user2@example.com)
   ...
   ========================================
   Sync Complete!
     ✓ Successfully synced: X
   ========================================
   ```

### Option B: Manual SQL Sync

If you prefer, you can manually insert subscriptions into Supabase using SQL (see below).

## Step 5: Restart Backend

Restart the backend so it loads the Supabase configuration.

**After this, new subscriptions will automatically sync to Supabase!**

## Verification

### Check Backend H2:
```sql
SELECT COUNT(*) FROM subscriptions;
SELECT * FROM subscriptions ORDER BY created_at DESC;
```

### Check Supabase:
```sql
SELECT COUNT(*) FROM subscriptions;
SELECT * FROM subscriptions ORDER BY created_at DESC;
```

Both should show the same number of subscriptions (after sync).

