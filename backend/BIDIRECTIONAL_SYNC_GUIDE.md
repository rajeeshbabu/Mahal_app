# Bidirectional Subscription Sync Guide

## Overview

The application now supports **bidirectional sync** for subscriptions between:
- **Backend SQLite** (`backend/data/mahal_db.db`)
- **Supabase** (cloud database)
- **Frontend SQLite** (via backend API)

## How It Works

### 1. Backend → Supabase (Push Sync)
When a subscription is created or updated in the backend SQLite database, it automatically syncs to Supabase:

- ✅ **On Create**: New subscriptions are pushed to Supabase immediately
- ✅ **On Update**: Subscription status changes are pushed to Supabase
- ✅ **On Delete**: Subscription deletions are synced to Supabase

**Location**: `SubscriptionService.java`, `RazorpaySubscriptionService.java`

### 2. Supabase → Backend (Pull Sync)
The backend automatically pulls changes from Supabase every 5 minutes:

- ✅ **Scheduled Sync**: Runs every 5 minutes automatically
- ✅ **Manual Sync**: Can be triggered via REST API
- ✅ **Conflict Resolution**: Supabase version wins if it's newer (based on `updated_at`)

**Location**: `BidirectionalSyncService.java`

### 3. Frontend → Backend
The frontend calls the backend API to create/check subscriptions. The backend then:
- Saves to backend SQLite
- Syncs to Supabase

## Configuration

### Step 1: Configure Supabase

Edit `backend/src/main/resources/application.properties`:

```properties
# Supabase Configuration
supabase.url=https://your-project-id.supabase.co
supabase.key=your-supabase-anon-key
```

**Important**: Uncomment and fill in your actual Supabase credentials!

### Step 2: Verify Supabase Table

Ensure your Supabase `subscriptions` table has the correct schema:

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

-- Enable Row Level Security
ALTER TABLE subscriptions ENABLE ROW LEVEL SECURITY;

-- Create policy (adjust as needed for your security requirements)
CREATE POLICY "Users can manage their own subscriptions"
ON subscriptions FOR ALL
USING (auth.uid()::text = user_id OR user_id = current_setting('request.jwt.claims', true)::json->>'sub');
```

## API Endpoints

### Push to Supabase (Manual Sync)

**Sync all subscriptions to Supabase:**
```bash
GET http://localhost:8080/api/subscriptions/sync/all
```

**Sync specific subscription:**
```bash
GET http://localhost:8080/api/subscriptions/sync/{id}
```

### Pull from Supabase (Manual Sync)

**Pull all subscriptions from Supabase:**
```bash
POST http://localhost:8080/api/subscriptions/sync/pull-from-supabase
```

## Automatic Sync Schedule

- **Pull from Supabase**: Every 5 minutes (300,000 ms)
- **Push to Supabase**: Immediately on create/update/delete

## Testing the Sync

### Test 1: Create Subscription and Verify Supabase Sync

1. Create a subscription through the frontend
2. Check backend logs for: `✓ Successfully synced subscription to Supabase`
3. Verify in Supabase dashboard that the subscription appears

### Test 2: Pull from Supabase

1. Manually add/update a subscription in Supabase
2. Wait 5 minutes OR call: `POST /api/subscriptions/sync/pull-from-supabase`
3. Check backend SQLite database - subscription should be synced

### Test 3: Conflict Resolution

1. Update a subscription in Supabase (change status, end_date, etc.)
2. Wait for automatic pull (5 minutes) or trigger manually
3. Verify backend SQLite has the updated values from Supabase

## Troubleshooting

### Issue: Subscriptions not syncing to Supabase

**Check:**
1. ✅ Supabase URL and key are configured in `application.properties`
2. ✅ Backend logs show: `Supabase not configured` warning?
3. ✅ Check Supabase table exists and has correct schema
4. ✅ Verify network connectivity to Supabase

**Solution:**
```bash
# Check if Supabase is configured
curl http://localhost:8080/api/subscriptions/sync/all

# Check backend logs for errors
```

### Issue: Pull from Supabase not working

**Check:**
1. ✅ Supabase is configured
2. ✅ Subscriptions exist in Supabase
3. ✅ Row Level Security (RLS) policies allow read access
4. ✅ Check backend logs for errors

**Solution:**
```bash
# Manually trigger pull
curl -X POST http://localhost:8080/api/subscriptions/sync/pull-from-supabase

# Check backend logs
```

### Issue: Sync conflicts

**Behavior:**
- If Supabase version has newer `updated_at` timestamp → Supabase wins
- If local version is newer → Local wins (and pushes to Supabase)

**Resolution:**
- The system automatically resolves conflicts based on `updated_at` timestamp
- Most recent update wins

## Database Locations

- **Backend SQLite**: `backend/data/mahal_db.db`
- **Frontend SQLite**: `frontend/mahal.db`
- **Supabase**: Cloud database (configured via `supabase.url`)

## Logs

Watch for these log messages:

**Successful sync to Supabase:**
```
✓ Successfully synced subscription to Supabase (user_id: user@example.com)
```

**Successful pull from Supabase:**
```
✓ Synced 3 subscription(s) from Supabase to local database.
✓ Pulled new subscription from Supabase: sub_xxxxx
✓ Updated local subscription from Supabase: sub_xxxxx
```

**Supabase not configured:**
```
⚠️  Supabase not configured. Subscription saved to backend SQLite database only.
```

## Next Steps

1. ✅ Configure Supabase credentials in `application.properties`
2. ✅ Restart backend to enable sync
3. ✅ Create a test subscription and verify it appears in Supabase
4. ✅ Test pull sync by updating a subscription in Supabase
5. ✅ Monitor logs to ensure sync is working

## Notes

- **Scheduled sync** runs in the background automatically
- **Manual sync** can be triggered via REST API endpoints
- **Conflict resolution** is automatic based on timestamps
- **Frontend** doesn't directly sync to SQLite - it uses backend API which handles all sync

