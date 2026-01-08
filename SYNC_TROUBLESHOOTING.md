# Supabase Sync Troubleshooting Guide

## If Supabase Tables Are Empty

### Step 1: Verify Tables Exist in Supabase
**CRITICAL**: Before sync can work, you must create the tables in Supabase!

1. Open your Supabase dashboard: https://supabase.com/dashboard/project/hkckhwxpxfylaeqnlrrv
2. Go to SQL Editor
3. Copy the entire contents of `supabase_schema.sql`
4. Paste and run it in the SQL Editor
5. Verify tables were created by checking the Table Editor

### Step 2: Verify Configuration
Check that `supabase.properties` has the correct values:
- `supabase.url=https://hkckhwxpxfylaeqnlrrv.supabase.co` (NO escaped colons!)
- `supabase.key=<your-api-key>`

### Step 3: Check Console Output
When the application runs, look for these log messages:
- `"Starting initial sync of existing data..."`
- `"Queued X records for sync"` for each table
- `"Syncing X pending operations..."`
- `"→ Inserting into Supabase table: <table_name>"`
- `"✓ Successfully inserted into <table_name>"` (success)
- `"✗ Supabase insert failed..."` (error - check the error message)

### Step 4: Trigger Sync Manually
You can trigger sync manually using:
```bash
java -cp "bin;lib/sqlite-jdbc.jar" SyncInitialData
```

### Step 5: Common Issues

#### Issue: "Table does not exist" error
**Solution**: Run the SQL schema in Supabase SQL Editor (Step 1)

#### Issue: "HTTP 401" or "HTTP 403" error
**Solution**: Check that your API key is correct and has proper permissions

#### Issue: "HTTP 404" error
**Solution**: Verify the Supabase URL is correct (should end with `.supabase.co`)

#### Issue: No sync happening at all
**Solution**: 
- Check internet connection
- Verify Supabase is configured (check console for "Supabase not configured" messages)
- Check if sync queue has pending operations

### Step 6: Verify Data is Queued
The initial sync should queue all existing data. Check console output for messages like:
- "Queued 3 income records for sync"
- "Queued 4 member records for sync"
- etc.

If you see "Queued 0 records", your local database might be empty, or the DAOs might not be finding the data.

### Step 7: Check Sync Queue Status
The sync system will automatically:
- Queue operations when you create/update/delete records
- Sync queued operations every 30 seconds when online
- Retry failed operations

Check console for sync progress messages.

## Still Having Issues?

1. **Check the console output** - Look for error messages
2. **Verify Supabase is accessible** - Try accessing your Supabase dashboard
3. **Check table names match** - Table names in code must exactly match Supabase table names
4. **Verify RLS policies** - Make sure Row Level Security policies allow INSERT/UPDATE/DELETE (the schema includes `CREATE POLICY ... FOR ALL USING (true) WITH CHECK (true);`)

