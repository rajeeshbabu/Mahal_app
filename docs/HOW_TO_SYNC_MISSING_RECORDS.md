# How to Sync Missing Records to Supabase

If you notice that some records from your local database are not appearing in Supabase, follow these steps:

## Quick Fix: Run the Sync Utility

1. **Make sure you're logged in** as the user whose records you want to sync

2. **Run the FindAndSyncMissingRecords utility:**
   ```bash
   java -cp "lib/sqlite-jdbc.jar;bin" com.mahal.util.FindAndSyncMissingRecords
   ```

   This utility will:
   - Check for failed sync operations
   - Count your local records
   - Clear old sync queue entries
   - Re-queue all your records for sync
   - Attempt to sync them to Supabase

3. **Check the console output** for any errors

4. **Verify in Supabase** that records have been synced

## Manual Steps (If utility doesn't work)

### Step 1: Check for Failed Operations

Run this SQL query in your local database (`mahal.db`):
```sql
SELECT table_name, operation, COUNT(*) as count 
FROM sync_queue 
WHERE sync_status = 'FAILED' 
GROUP BY table_name, operation;
```

This shows which operations failed.

### Step 2: Reset Failed Operations

To retry failed operations:
```sql
UPDATE sync_queue SET sync_status = 'PENDING', retry_count = 0 WHERE sync_status = 'FAILED';
```

### Step 3: Clear and Re-queue All Records

In your Java application:
1. Log in as the user whose records you want to sync
2. Call `SyncHelper.performInitialSync()` - this will re-queue all records

### Step 4: Trigger Sync

The sync should happen automatically, but you can also manually trigger it:
- Call `SyncHelper.triggerSync()` in code
- Or restart the application (it will auto-sync on startup)

## Common Issues and Fixes

### Issue: Records not syncing for user_id 2

**Solution:**
1. Log in as user_id 2 (e.g., "rajeesh")
2. Run `FindAndSyncMissingRecords` utility
3. Or restart the application while logged in as user_id 2

### Issue: Some tables not syncing

**Check:**
- Ensure the table has `user_id` column in Supabase
- Check console logs for specific error messages
- Verify the table name matches exactly (case-sensitive)

### Issue: HTTP 409 errors (Duplicate Key)

This is usually OK - it means the record already exists in Supabase. The sync treats this as success.

### Issue: HTTP 400/422 errors (Bad Request)

This usually means:
- A required field is missing
- Data type mismatch
- Column doesn't exist in Supabase

**Fix:** Check the error message in console logs - it will tell you which field is the problem.

## Verify Sync Success

After syncing, check in Supabase:
1. Go to your table
2. Filter by `user_id = '2'` (or whatever user_id you synced)
3. Verify records are present

## For All Users

To sync records for ALL users:
1. Log in as user 1 → Run sync utility or restart app
2. Log out
3. Log in as user 2 → Run sync utility or restart app
4. Repeat for each user

Each user's records will only sync when that user is logged in (for security/isolation).
