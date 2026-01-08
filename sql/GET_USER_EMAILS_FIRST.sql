-- STEP 1: Get user emails from admins table
-- Run this first in DB Browser for SQLite or SQLite command line
-- This will show you the emails you need to use in the subscription INSERT queries

SELECT id, name AS email, full_name, active 
FROM admins 
ORDER BY id;

-- Copy the email values (from the 'email' column) and use them in:
-- - sql/ADD_SUBSCRIPTIONS_FOR_USERS_SQLITE.sql
-- - backend/sql/ADD_SUBSCRIPTIONS_FOR_USERS_SUPABASE.sql

