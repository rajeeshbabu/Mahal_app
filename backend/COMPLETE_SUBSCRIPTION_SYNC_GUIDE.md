# Complete Guide: Subscriptions Storage and Sync

## How Subscriptions Work

### Architecture

1. **Backend H2 Database** (`backend/data/mahal_db`)
   - Primary storage for all subscriptions
   - Created automatically when subscription is created
   - Managed by Spring Boot/JPA

2. **Supabase**
   - Cloud backup/sync
   - Synced from backend H2
   - Requires configuration to enable

3. **Frontend SQLite** (`frontend/mahal.db`)
   - NOT used for subscriptions (by design)
   - Used for local app data (members, masjids, etc.)

## How New Subscriptions Are Added (Automatic)

When a user subscribes:

1. **Frontend** → User clicks "Subscribe" → Calls `POST /api/subscriptions/create`
2. **Backend `RazorpaySubscriptionService`**:
   ```java
   Subscription subscription = new Subscription();
   subscription.setUserId(userEmail);  // User's email
   subscription.setUserEmail(userEmail); // Same email
   subscription.setPlanDuration(planDuration); // "monthly" or "yearly"
   subscription.setStatus("pending");
   subscription = subscriptionRepository.save(subscription); // ✅ Saved to H2
   
   // Auto-sync to Supabase (if configured)
   if (supabaseSyncService != null) {
       SubscriptionSyncHelper.syncSubscription(supabaseSyncService, subscription, "INSERT");
   }
   ```
3. **Result:**
   - ✅ Saved to Backend H2 database
   - ✅ Synced to Supabase (if configured)

**This is 100% automatic - no manual steps needed!**

## How to Enable Supabase Sync

### Step 1: Configure Supabase

Edit `backend/src/main/resources/application.properties`:

```properties
# Uncomment and add your Supabase credentials:
supabase.url=https://your-project-id.supabase.co
supabase.key=your-supabase-anon-key
```

**Get credentials from:** Supabase Dashboard → Settings → API

### Step 2: Create Table in Supabase

Run in Supabase SQL Editor:
- `backend/sql/CREATE_SUBSCRIPTIONS_TABLE_SUPABASE.sql`

### Step 3: Restart Backend

Restart the backend to load Supabase configuration.

### Step 4: Test

Create a new subscription - it will automatically sync to Supabase!

**Backend logs will show:**
```
✓ Successfully synced subscription to Supabase (user_id: user@example.com)
```

## How to Sync Existing Subscriptions to Supabase

If you have subscriptions in H2 that were created before Supabase was configured:

### Option 1: Use REST API (Easiest)

After configuring Supabase:

1. **Start backend**
2. **Call sync endpoint:**
   ```
   GET http://localhost:8080/api/subscriptions/sync/all
   ```
   
   Or open in browser: `http://localhost:8080/api/subscriptions/sync/all`

3. **Check backend console** for sync progress

### Option 2: Manual SQL Sync

1. **Get subscriptions from H2:**
   - Open H2 Console: `http://localhost:8080/h2-console`
   - Run: `SELECT * FROM subscriptions;`
   - Copy the data

2. **Insert into Supabase:**
   - Open Supabase SQL Editor
   - Use template from `backend/sql/SYNC_EXISTING_SUBSCRIPTIONS_TO_SUPABASE.sql`
   - Replace placeholder values with actual data
   - Run the INSERT statements

## Verification Queries

### Check Backend H2:

```sql
-- H2 Console: http://localhost:8080/h2-console
SELECT COUNT(*) AS total FROM subscriptions;
SELECT id, user_email, plan_duration, status, created_at 
FROM subscriptions 
ORDER BY created_at DESC;
```

### Check Supabase:

```sql
-- Supabase SQL Editor
SELECT COUNT(*) AS total FROM subscriptions;
SELECT id, user_email, plan_duration, status, created_at 
FROM subscriptions 
ORDER BY created_at DESC;
```

Both should show the same count after sync.

## Summary

- ✅ **New subscriptions:** Automatically saved to H2 and synced to Supabase (if configured)
- ✅ **Existing subscriptions:** Use REST API endpoint `/api/subscriptions/sync/all` to sync
- ✅ **Backend H2:** Primary storage (always works)
- ✅ **Supabase:** Cloud sync (requires configuration)
- ✅ **Frontend SQLite:** Not used for subscriptions (by design)

## Troubleshooting

### Subscriptions not in Supabase

1. **Check Supabase configuration:**
   - Verify `supabase.url` and `supabase.key` are set in `application.properties`
   - Restart backend after configuration

2. **Check backend logs:**
   - Look for: `✓ Successfully synced subscription to Supabase`
   - Or: `⚠️  Supabase not configured`

3. **Check Supabase table exists:**
   - Run: `backend/sql/CREATE_SUBSCRIPTIONS_TABLE_SUPABASE.sql`

4. **Sync existing subscriptions:**
   - Use: `GET http://localhost:8080/api/subscriptions/sync/all`


