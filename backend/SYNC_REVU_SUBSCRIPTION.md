# Sync revu@gmail.com Subscription to Supabase

## Problem
The subscription for revu@gmail.com exists in backend SQLite (ID=3) but is not in Supabase.

## Solution: Sync via API

### Option 1: Sync by User Email (Easiest)

Since the subscription ID is 3, you can sync it directly:

**Using Browser:**
```
GET http://localhost:8080/api/subscriptions/sync/3
```

**Using curl:**
```powershell
curl http://localhost:8080/api/subscriptions/sync/3
```

### Option 2: Sync All Subscriptions

This will sync ALL subscriptions including revu@gmail.com:

```
GET http://localhost:8080/api/subscriptions/sync/all
```

## Verify Sync

After syncing, check:

1. **Backend Logs** - You should see:
   ```
   ✓ Synced subscription ID: 3
   ✅ Successfully synced subscription to Supabase
   ```

2. **Supabase Dashboard**:
   - Go to https://app.supabase.com
   - Navigate to **Table Editor** → `subscriptions`
   - Look for revu@gmail.com - it should now be there!

## If Sync Still Fails

Check for these errors in backend logs:

- **HTTP 401** → RLS policy issue (you may need to run the RLS fix SQL again)
- **HTTP 400/422** → Schema mismatch (check table structure)
- **Connection error** → Network/URL issue

