-- Delete all records from subscriptions table
DELETE FROM subscriptions;

-- Verify deletion (should return 0 rows)
SELECT COUNT(*) FROM subscriptions;

