# Subscription Supabase Sync Setup

## Overview

Subscriptions are now automatically synced to Supabase when created or updated in the backend. This ensures subscription data is available across all instances and can be accessed from web applications or mobile apps.

## Configuration

### 1. Add Supabase Configuration

Add the following to `backend/src/main/resources/application.properties`:

```properties
# Supabase Configuration (for syncing subscriptions)
supabase.url=https://your-project-id.supabase.co
supabase.key=your-supabase-anon-key
```

Replace:
- `your-project-id` with your actual Supabase project ID
- `your-supabase-anon-key` with your Supabase anon/public API key

You can find these in your Supabase Dashboard → Settings → API.

### 2. Create Subscriptions Table in Supabase

Run this SQL in your Supabase SQL Editor:

```sql
-- Create subscriptions table
CREATE TABLE IF NOT EXISTS subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id TEXT NOT NULL,
    plan_duration TEXT NOT NULL,  -- 'monthly' or 'yearly'
    status TEXT NOT NULL,  -- 'pending', 'active', 'cancelled', 'expired'
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    razorpay_subscription_id TEXT UNIQUE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Create index for user_id (important for queries)
CREATE INDEX IF NOT EXISTS idx_subscriptions_user_id ON subscriptions(user_id);

-- Create index for razorpay_subscription_id (for webhook lookups)
CREATE INDEX IF NOT EXISTS idx_subscriptions_razorpay_id ON subscriptions(razorpay_subscription_id);

-- Enable Row Level Security (RLS)
ALTER TABLE subscriptions ENABLE ROW LEVEL SECURITY;

-- RLS Policy: Users can only see their own subscriptions
CREATE POLICY "Users can view own subscriptions"
    ON subscriptions FOR SELECT
    USING (auth.uid()::text = user_id);

-- RLS Policy: Service role can insert/update (for backend sync)
-- Note: If using anon key, you may need to adjust this policy
-- or use service_role key for backend operations
CREATE POLICY "Service can insert subscriptions"
    ON subscriptions FOR INSERT
    WITH CHECK (true);

CREATE POLICY "Service can update subscriptions"
    ON subscriptions FOR UPDATE
    USING (true);
```

**Important Notes:**
- If you're using the `anon` key from the backend, you may need to disable RLS for inserts/updates from the backend, or use a service role key
- For production, consider using the `service_role` key for backend operations (it bypasses RLS)
- The `user_id` field stores the email or identifier of the user who owns the subscription

## How It Works

### Automatic Sync

When subscriptions are created or updated, they are automatically synced to Supabase:

1. **Subscription Created** (via `RazorpaySubscriptionService.createSubscription()`)
   - Subscription saved to backend database
   - Automatically synced to Supabase as INSERT

2. **Subscription Updated** (via `SubscriptionService.updateSubscriptionStatus()`, `activateSubscription()`, `cancelSubscription()`, etc.)
   - Subscription updated in backend database
   - Automatically synced to Supabase as UPDATE

### Sync Service

The `SupabaseSyncService` handles all communication with Supabase:
- Uses REST API (`/rest/v1/subscriptions`)
- Adds `user_id` to all operations for user isolation
- Handles errors gracefully (logs errors but doesn't fail the main operation)
- Skips sync silently if Supabase is not configured

### Data Format

The subscription data is converted to JSON with these fields:
- `id` - Subscription ID (backend database ID)
- `user_id` - User identifier (email or username)
- `user_email` - User email address
- `plan_duration` - "monthly" or "yearly"
- `status` - "pending", "active", "cancelled", "expired"
- `start_date` - Subscription start timestamp (ISO format)
- `end_date` - Subscription end timestamp (ISO format)
- `razorpay_subscription_id` - Razorpay subscription ID
- `created_at` - Creation timestamp
- `updated_at` - Last update timestamp

## Testing

### 1. Test Subscription Creation

1. Create a subscription via the frontend or API
2. Check backend logs for: `✓ Successfully synced subscription to Supabase`
3. Verify in Supabase: Go to Table Editor → `subscriptions` table

### 2. Test Subscription Update

1. Activate a subscription (via webhook or mock payment)
2. Check backend logs for: `✓ Successfully updated subscription in Supabase`
3. Verify in Supabase that the status changed to "active"

### 3. Verify Without Configuration

If Supabase is not configured:
- Operations will complete successfully in the backend
- No sync will occur (silently skipped)
- No errors will be thrown

## Troubleshooting

### Sync Not Working

1. **Check Configuration**
   - Verify `supabase.url` and `supabase.key` are set in `application.properties`
   - Restart the backend after adding configuration

2. **Check Logs**
   - Look for error messages like: `✗ Supabase insert failed`
   - Check for: `Supabase not configured` (means properties are missing)

3. **Check Supabase**
   - Verify table exists: `subscriptions`
   - Check RLS policies (may need to adjust for anon key)
   - Verify API key has correct permissions

4. **HTTP Errors**
   - `401/403`: Invalid API key or RLS blocking
   - `404`: Table doesn't exist or wrong URL
   - `409`: Record already exists (treated as success)

### RLS Policy Issues

If you get `403 Forbidden` errors:
- Option 1: Use `service_role` key instead of `anon` key (bypasses RLS)
- Option 2: Adjust RLS policies to allow inserts/updates from anon key
- Option 3: Temporarily disable RLS for testing (NOT recommended for production)

## Integration with Frontend

The frontend can now query subscription status from Supabase:
- Direct queries to Supabase `subscriptions` table
- Real-time updates via Supabase Realtime (if enabled)
- Access from web/mobile apps without hitting the backend

## Notes

- Sync is **asynchronous** and **non-blocking** - subscription operations complete even if sync fails
- Sync failures are logged but don't affect the main operation
- The backend database remains the source of truth
- Supabase is used as a synchronized copy for multi-instance access

