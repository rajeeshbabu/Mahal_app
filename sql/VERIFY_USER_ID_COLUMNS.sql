-- ============================================
-- Verify user_id columns were added
-- Run this AFTER running ADD_USER_ID_COLUMNS_SUPABASE.sql
-- ============================================

SELECT table_name, column_name 
FROM information_schema.columns 
WHERE column_name = 'user_id' 
AND table_schema = 'public'
ORDER BY table_name;



