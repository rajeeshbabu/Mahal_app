# Sync Existing Subscriptions to Supabase

## ✅ Supabase is Now Configured!

The backend is now configured with the same Supabase credentials as the frontend:
- **URL**: `https://hkckhwxpxfylaeqnlrrv.supabase.co`
- **Project**: Same as frontend

## Next Steps

### Step 1: Restart Backend

**Important**: You must restart your Spring Boot backend for the Supabase configuration to take effect.

```bash
# Stop the current backend (Ctrl+C)
# Then restart it:
cd backend
mvn spring-boot:run
```

### Step 2: Sync Your 2 Existing Subscriptions

After restarting, sync your existing subscriptions from SQLite to Supabase:

#### Option A: Using Browser
Open in your browser:
```
http://localhost:8080/api/subscriptions/sync/all
```

#### Option B: Using curl (PowerShell)
```powershell
curl http://localhost:8080/api/subscriptions/sync/all
```

#### Option C: Using Postman/API Client
```
GET http://localhost:8080/api/subscriptions/sync/all
```

### Step 3: Verify Sync

1. **Check Backend Logs:**
   You should see:
   ```
   ========================================
   Syncing 2 subscriptions to Supabase...
   ========================================
   🔄 Syncing subscription to Supabase: sub_xxxxx (operation: INSERT, user: user@example.com)
   ✅ Successfully synced subscription to Supabase: sub_xxxxx
   ✓ Synced subscription ID: 1 (user: user@example.com)
   ...
   ========================================
   Sync Complete!
     ✓ Successfully synced: 2
   ========================================
   ```

2. **Check Supabase Dashboard:**
   - Go to: https://app.supabase.com
   - Select your project
   - Navigate to **Table Editor** → `subscriptions`
   - You should see your 2 subscriptions!

### Step 4: Test New Subscriptions

Create a new subscription through the frontend. It should automatically sync to Supabase now.

## Troubleshooting

### If you see "Supabase not configured" after restarting:

1. ✅ Check that you restarted the backend
2. ✅ Verify `application.properties` has the correct values (no typos)
3. ✅ Check backend startup logs for any errors

### If sync fails with HTTP errors:

1. **HTTP 401/403**: Check that the API key is correct
2. **HTTP 400/422**: Verify the `subscriptions` table schema in Supabase matches expected format
3. **HTTP 409**: Record already exists (this is OK, it means it's already synced)

### Check Supabase Table Schema

Ensure your Supabase `subscriptions` table has:
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

## What Happens Next

✅ **New subscriptions** → Automatically sync to Supabase  
✅ **Subscription updates** → Automatically sync to Supabase  
✅ **Every 5 minutes** → Backend pulls from Supabase (bidirectional sync)  
✅ **Manual sync** → Available via API endpoints

Your subscriptions are now fully synced between:
- Backend SQLite (`backend/data/mahal_db.db`)
- Supabase (cloud database)
- Frontend SQLite (via backend API)

