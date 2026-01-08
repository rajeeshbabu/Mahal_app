# Fix: Masjid Not Getting Added to Supabase

## Problem

When you add a new masjid:
- ✅ Masjid is saved to SQLite successfully
- ❌ Masjid is NOT added to Supabase
- ⚠️  You see: "Record already exists in masjids (HTTP 409 - duplicate key)"

## Root Cause

HTTP 409 (duplicate key) means a record with the same ID already exists in Supabase. The code was treating this as "success" but not actually updating the record.

## What I've Fixed

1. ✅ **Added UPSERT support** - Uses `resolution=merge-duplicates` header to automatically update if record exists
2. ✅ **Improved 409 handling** - When 409 occurs, tries to UPDATE the existing record
3. ✅ **Better error handling** - Returns false on failure so sync can retry

## How It Works Now

### Before (Broken):
1. Try INSERT → Get HTTP 409
2. Treat as success → Do nothing
3. Result: Masjid not in Supabase ❌

### After (Fixed):
1. Try INSERT with UPSERT header → If exists, auto-updates
2. If still 409 → Extract ID and try UPDATE
3. Result: Masjid synced to Supabase ✅

## Test It

1. **Add a new masjid** through the frontend
2. **Check backend logs** - You should see:
   ```
   ✓ Successfully inserted into masjids (user_id: X)
   ```
   OR
   ```
   ✓ Successfully updated existing record in masjids (ID: X)
   ```

3. **Check Supabase** - The masjid should appear in the `masjids` table

## If Still Not Working

If you still see HTTP 409 errors:

1. **Check if masjid already exists in Supabase** with that ID
2. **Delete the duplicate** in Supabase (if it's old/stale data)
3. **Try adding masjid again**

Or manually sync:
- The sync manager will retry failed operations
- Wait for automatic sync (runs every 60 seconds)
- Or trigger manual sync from Settings

## Technical Details

The fix uses Supabase's `resolution=merge-duplicates` header which:
- If record doesn't exist → INSERT
- If record exists (by primary key) → UPDATE with new data

This is equivalent to SQL's `INSERT ... ON CONFLICT UPDATE`.

