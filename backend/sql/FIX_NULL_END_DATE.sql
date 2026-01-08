-- Fix null end_date for subscriptions in Supabase
-- This script updates subscriptions with null end_date based on their plan_duration

-- For monthly subscriptions: add 1 month to start_date
UPDATE subscriptions
SET end_date = start_date + INTERVAL '1 month'
WHERE end_date IS NULL 
  AND plan_duration = 'monthly'
  AND start_date IS NOT NULL;

-- For yearly subscriptions: add 1 year to start_date
UPDATE subscriptions
SET end_date = start_date + INTERVAL '1 year'
WHERE end_date IS NULL 
  AND plan_duration = 'yearly'
  AND start_date IS NOT NULL;

-- If start_date is also null, set both start_date and end_date based on created_at
UPDATE subscriptions
SET 
  start_date = created_at,
  end_date = CASE 
    WHEN plan_duration = 'monthly' THEN created_at + INTERVAL '1 month'
    WHEN plan_duration = 'yearly' THEN created_at + INTERVAL '1 year'
    ELSE created_at + INTERVAL '1 month'
  END
WHERE end_date IS NULL
  AND start_date IS NULL
  AND created_at IS NOT NULL;

-- Verify the fix
SELECT 
    id,
    user_id,
    user_email,
    plan_duration,
    status,
    start_date,
    end_date,
    created_at
FROM subscriptions
WHERE user_email = 'revu@gmail.com' OR user_id = 'revu@gmail.com'
ORDER BY created_at DESC;

