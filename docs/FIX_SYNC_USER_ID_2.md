# Fix: Sync User ID 2's Records (Committees & Masjids) to Supabase

## Problem
You added committee and masjid records for user_id 2 in the local database, but they're not showing in Supabase.

## Solution Steps

### Step 1: Log in as User ID 2
1. Log in to your Java application as the user with user_id 2 (e.g., "rajeesh")
2. Make sure you're logged in before proceeding

### Step 2: Run the Sync Utility

Run this utility to force sync all records for user_id 2:

**Option A: From Code (in your application)**
```java
com.mahal.util.ForceSyncUserRecords.main(new String[]{});
```

**Option B: Check Sync Status First**
```java
com.mahal.util.CheckSyncStatus.main(new String[]{});
```

This will show:
- How many records are pending sync
- If there are any failed operations
- Local record counts

### Step 3: Verify Records are Queued

The utility will:
1. Queue all existing records for user_id 2
2. Attempt to sync them to Supabase
3. Show detailed logs of what's happening

### Step 4: Check Console Logs

Look for messages like:
- `"Queued sync operation for table: committees, operation: INSERT, user_id: 2"`
- `"Queued sync operation for table: masjids, operation: INSERT, user_id: 2"`
- `"✓ Successfully inserted into committees (user_id: 2)"`
- `"✓ Successfully inserted into masjids (user_id: 2)"`

### Step 5: Verify in Supabase

After sync completes, check Supabase:
1. Go to Supabase Dashboard > Table Editor
2. Select `committees` table
3. Filter by `user_id = '2'` (if filter available, or use SQL query)
4. Select `masjids` table
5. Filter by `user_id = '2'`

You should see your records!

## Quick SQL Query to Check in Supabase

Run this in Supabase SQL Editor to see if records are there:

```sql
-- Check committees for user_id 2
SELECT id, user_id, member_name, mobile, designation 
FROM committees 
WHERE user_id = '2';

-- Check masjids for user_id 2
SELECT id, user_id, name, abbreviation, address 
FROM masjids 
WHERE user_id = '2';
```

## Common Issues

### Issue: No records in Supabase
**Possible causes:**
1. Records weren't queued when created
2. Sync failed with errors
3. user_id wasn't set correctly

**Solution:**
- Run `ForceSyncUserRecords` utility (it re-queues everything)
- Check console logs for error messages
- Make sure you're logged in as user_id 2

### Issue: Sync shows errors
**Check console logs for:**
- HTTP error codes (400, 404, 500, etc.)
- Error messages from Supabase
- Network connectivity issues

**Common errors:**
- **HTTP 400/422**: Column mismatch or missing required field
- **HTTP 404**: Table doesn't exist in Supabase
- **HTTP 500**: Server error on Supabase side

### Issue: Records queued but not syncing
**Check:**
1. Is Supabase configured? (`supabase.properties` file)
2. Is internet connection working?
3. Check `syncPendingOperations()` is being called

**Solution:**
- Manually trigger sync: `SyncManager.getInstance().syncPendingOperations()`
- Or restart the application (auto-sync runs on startup)

## Alternative: Manual Sync via Initial Sync

If the utility doesn't work, you can manually trigger initial sync:

1. Log in as user_id 2
2. In code, call: `SyncHelper.performInitialSync()`
3. This will queue all existing records and attempt to sync them

## Verification Checklist

After running sync, verify:

- [ ] Console shows "Queued sync operation" messages for committees and masjids
- [ ] Console shows "✓ Successfully inserted" messages (or HTTP 409 for duplicates)
- [ ] No error messages in console
- [ ] Records appear in Supabase when filtering by `user_id = '2'`

If all checkboxes are marked but records still don't appear, check:
- Supabase table structure matches local DB
- user_id column exists in Supabase tables
- Network/firewall isn't blocking requests
