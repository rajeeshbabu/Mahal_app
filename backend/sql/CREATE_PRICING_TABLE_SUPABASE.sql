-- Create subscription_pricing table in Supabase
-- Run this in Supabase SQL Editor

CREATE TABLE IF NOT EXISTS subscription_pricing (
    id BIGSERIAL PRIMARY KEY,
    plan_duration TEXT NOT NULL UNIQUE, -- 'monthly' or 'yearly'
    amount_paise BIGINT NOT NULL,       -- Amount in paise (e.g. 10000 for ₹100.00)
    currency TEXT DEFAULT 'INR',
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Insert default prices if they don't exist
INSERT INTO subscription_pricing (plan_duration, amount_paise)
VALUES ('monthly', 100), ('yearly', 100)
ON CONFLICT (plan_duration) DO NOTHING;

-- Enable Row Level Security (RLS)
ALTER TABLE subscription_pricing ENABLE ROW LEVEL SECURITY;

-- RLS Policy: Everyone can view prices (public read)
CREATE POLICY "Allow public read access for pricing"
    ON subscription_pricing FOR SELECT
    USING (true);

-- RLS Policy: Only superadmin (or service role) can update prices
-- For simplicity in this demo, we allow all updates, but in production this should be restricted
CREATE POLICY "Allow price updates"
    ON subscription_pricing FOR UPDATE
    USING (true)
    WITH CHECK (true);

CREATE POLICY "Allow price inserts"
    ON subscription_pricing FOR INSERT
    WITH CHECK (true);

-- Add comment
COMMENT ON TABLE subscription_pricing IS 'Stores subscription plan pricing details';
