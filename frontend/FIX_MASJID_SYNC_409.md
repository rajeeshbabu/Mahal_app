# Fix: Masjid HTTP 409 Error - Not Syncing to Supabase

## Problem

When adding a new masjid:
- ✅ Saved to SQLite
- ❌ HTTP 409 error when syncing to Supabase
- ❌ Masjid not appearing in Supabase

## Root Cause

HTTP 409 means a record with the same primary key (ID) already exists in Supabase. The old code treated this as "success" but didn't actually update the record.

## Solution Applied

I've added **UPSERT support** using Supabase's `resolution=merge-duplicates` header:

```java
conn.setRequestProperty("Prefer", "resolution=merge-duplicates,return=representation");
```

This means:
- **If record doesn't exist** → INSERT (create new)
- **If record exists** → UPDATE (merge with existing data)

## How to Test

1. **Recompile frontend** (if needed):
   ```bash
   cd frontend
   .\run.bat
   ```

2. **Add a new masjid** through the UI

3. **Check logs** - You should see:
   ```
   ✓ Successfully inserted into masjids (user_id: X)
   ```
   (No more HTTP 409 errors!)

4. **Check Supabase** - Masjid should appear in `masjids` table

## If You Still See 409

If you still get HTTP 409 after this fix:

1. **Check Supabase** - Look for a masjid with ID 13 (or whatever ID the error shows)
2. **Delete the duplicate** in Supabase if it's stale data
3. **Try adding masjid again**

The UPSERT header should prevent 409 errors going forward.

## What Changed

- ✅ Added `resolution=merge-duplicates` header to INSERT requests
- ✅ Improved 409 error handling (tries UPDATE if INSERT fails)
- ✅ Better logging to show what's happening

Now masjids will sync properly to Supabase! 🎉

