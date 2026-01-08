-- Create subscriptions table in Supabase for syncing subscription data
-- Run this in Supabase SQL Editor

CREATE TABLE IF NOT EXISTS subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id TEXT NOT NULL,
    user_email TEXT,
    plan_duration TEXT NOT NULL,  -- 'monthly' or 'yearly'
    status TEXT NOT NULL,  -- 'pending', 'active', 'cancelled', 'expired'
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    razorpay_subscription_id TEXT UNIQUE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_subscriptions_user_id ON subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_user_email ON subscriptions(user_email);
CREATE INDEX IF NOT EXISTS idx_subscriptions_razorpay_id ON subscriptions(razorpay_subscription_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_status ON subscriptions(status);

-- Enable Row Level Security (RLS)
ALTER TABLE subscriptions ENABLE ROW LEVEL SECURITY;

-- RLS Policy: Users can only see their own subscriptions
CREATE POLICY "Users can view own subscriptions"
    ON subscriptions FOR SELECT
    USING (auth.uid()::text = user_id);

-- RLS Policy: Allow inserts from backend (using anon key)
-- Note: For production, consider using service_role key which bypasses RLS
CREATE POLICY "Allow subscription inserts"
    ON subscriptions FOR INSERT
    WITH CHECK (true);

-- RLS Policy: Allow updates from backend
CREATE POLICY "Allow subscription updates"
    ON subscriptions FOR UPDATE
    USING (true);

-- Add comment
COMMENT ON TABLE subscriptions IS 'Subscription data synced from backend Spring Boot application';

