-- ==========================================
-- INSTRUCTIONS FOR DATABASE MIGRATION
-- ==========================================

-- 1. SUPABASE (Run in Supabase SQL Editor website)
-- This adds the column to the cloud database.
-- Option A: If column DOES NOT exist yet:
ALTER TABLE subscriptions ADD COLUMN superadmin_status text DEFAULT 'activated';

-- Option B: If column ALREADY EXISTS (Fix for Error 42701):
ALTER TABLE subscriptions ALTER COLUMN superadmin_status SET DEFAULT 'activated';


-- 2. SQLITE (Run locally if you have an SQLite tool, OR delete the .db file)
-- If you strictly want to keep existing local data, use an SQLite browser (like DB Browser for SQLite) 
-- to open 'mahal_db.db' and run this query:
ALTER TABLE subscriptions ADD COLUMN superadmin_status TEXT DEFAULT 'activated';

-- NOTE: If you don't have important local data, the EASIEST way to fix the local SQLite
-- is to simply DELETE the 'mahal_db.db' file. The application will recreate it 
-- automatically with the new column when you restart the Backend.
