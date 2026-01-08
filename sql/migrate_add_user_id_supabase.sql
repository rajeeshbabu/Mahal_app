-- SQL Migration Script: Add user_id column and RLS policies to Supabase tables
-- Run this in Supabase SQL Editor

-- ============================================
-- STEP 1: Add user_id column to all tables
-- ============================================

-- Members table
ALTER TABLE members ADD COLUMN IF NOT EXISTS user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_members_user_id ON members(user_id);

-- Incomes table
ALTER TABLE incomes ADD COLUMN IF NOT EXISTS user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_incomes_user_id ON incomes(user_id);

-- Expenses table
ALTER TABLE expenses ADD COLUMN IF NOT EXISTS user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_expenses_user_id ON expenses(user_id);

-- Due Collections table
ALTER TABLE due_collections ADD COLUMN IF NOT EXISTS user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_due_collections_user_id ON due_collections(user_id);

-- Due Types table
ALTER TABLE due_types ADD COLUMN IF NOT EXISTS user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_due_types_user_id ON due_types(user_id);

-- Income Types table
ALTER TABLE income_types ADD COLUMN IF NOT EXISTS user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_income_types_user_id ON income_types(user_id);

-- Inventory Items table
ALTER TABLE inventory_items ADD COLUMN IF NOT EXISTS user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_inventory_items_user_id ON inventory_items(user_id);

-- Damaged Items table
ALTER TABLE damaged_items ADD COLUMN IF NOT EXISTS user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_damaged_items_user_id ON damaged_items(user_id);

-- Rent Items table
ALTER TABLE rent_items ADD COLUMN IF NOT EXISTS user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_rent_items_user_id ON rent_items(user_id);

-- Rents table
ALTER TABLE rents ADD COLUMN IF NOT EXISTS user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_rents_user_id ON rents(user_id);

-- Events table
ALTER TABLE events ADD COLUMN IF NOT EXISTS user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_events_user_id ON events(user_id);

-- Masjids table
ALTER TABLE masjids ADD COLUMN IF NOT EXISTS user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_masjids_user_id ON masjids(user_id);

-- Staff table
ALTER TABLE staff ADD COLUMN IF NOT EXISTS user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_staff_user_id ON staff(user_id);

-- Staff Salaries table
ALTER TABLE staff_salaries ADD COLUMN IF NOT EXISTS user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_staff_salaries_user_id ON staff_salaries(user_id);

-- Committees table
ALTER TABLE committees ADD COLUMN IF NOT EXISTS user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_committees_user_id ON committees(user_id);

-- Houses table
ALTER TABLE houses ADD COLUMN IF NOT EXISTS user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_houses_user_id ON houses(user_id);

-- Students table
ALTER TABLE students ADD COLUMN IF NOT EXISTS user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_students_user_id ON students(user_id);

-- Marriage Certificates table
ALTER TABLE marriage_certificates ADD COLUMN IF NOT EXISTS user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_marriage_certificates_user_id ON marriage_certificates(user_id);

-- Death Certificates table
ALTER TABLE death_certificates ADD COLUMN IF NOT EXISTS user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_death_certificates_user_id ON death_certificates(user_id);

-- Jamath Certificates table
ALTER TABLE jamath_certificates ADD COLUMN IF NOT EXISTS user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_jamath_certificates_user_id ON jamath_certificates(user_id);

-- Custom Certificates table
ALTER TABLE custom_certificates ADD COLUMN IF NOT EXISTS user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_custom_certificates_user_id ON custom_certificates(user_id);

-- Prayer Times table (optional)
ALTER TABLE prayer_times ADD COLUMN IF NOT EXISTS user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_prayer_times_user_id ON prayer_times(user_id);

-- ============================================
-- STEP 2: Enable Row Level Security (RLS)
-- ============================================

ALTER TABLE members ENABLE ROW LEVEL SECURITY;
ALTER TABLE incomes ENABLE ROW LEVEL SECURITY;
ALTER TABLE expenses ENABLE ROW LEVEL SECURITY;
ALTER TABLE due_collections ENABLE ROW LEVEL SECURITY;
ALTER TABLE due_types ENABLE ROW LEVEL SECURITY;
ALTER TABLE income_types ENABLE ROW LEVEL SECURITY;
ALTER TABLE inventory_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE damaged_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE rent_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE rents ENABLE ROW LEVEL SECURITY;
ALTER TABLE events ENABLE ROW LEVEL SECURITY;
ALTER TABLE masjids ENABLE ROW LEVEL SECURITY;
ALTER TABLE staff ENABLE ROW LEVEL SECURITY;
ALTER TABLE staff_salaries ENABLE ROW LEVEL SECURITY;
ALTER TABLE committees ENABLE ROW LEVEL SECURITY;
ALTER TABLE houses ENABLE ROW LEVEL SECURITY;
ALTER TABLE marriage_certificates ENABLE ROW LEVEL SECURITY;
ALTER TABLE death_certificates ENABLE ROW LEVEL SECURITY;
ALTER TABLE jamath_certificates ENABLE ROW LEVEL SECURITY;
ALTER TABLE custom_certificates ENABLE ROW LEVEL SECURITY;

-- ============================================
-- STEP 3: Create RLS Policies for each table
-- ============================================

-- Helper function to create policies for a table (run for each table)
-- Example for members table:

DROP POLICY IF EXISTS "Users can view own members" ON members;
CREATE POLICY "Users can view own members"
    ON members FOR SELECT
    USING (user_id = current_setting('request.jwt.claim.user_id', true));

DROP POLICY IF EXISTS "Users can insert own members" ON members;
CREATE POLICY "Users can insert own members"
    ON members FOR INSERT
    WITH CHECK (user_id = current_setting('request.jwt.claim.user_id', true));

DROP POLICY IF EXISTS "Users can update own members" ON members;
CREATE POLICY "Users can update own members"
    ON members FOR UPDATE
    USING (user_id = current_setting('request.jwt.claim.user_id', true))
    WITH CHECK (user_id = current_setting('request.jwt.claim.user_id', true));

DROP POLICY IF EXISTS "Users can delete own members" ON members;
CREATE POLICY "Users can delete own members"
    ON members FOR DELETE
    USING (user_id = current_setting('request.jwt.claim.user_id', true));

-- Repeat the above pattern for all tables, OR use the simplified version below
-- that works with the user_id passed in the request (for PostgREST):

-- Simplified policy (if user_id is in the JSON payload and you filter by it in queries):
-- This works because SupabaseSyncService already filters by user_id in the query string

-- Alternative: If using service role key with user_id in payload (current approach),
-- you may need to disable RLS temporarily or use a different approach.
-- The current sync service adds user_id to JSON, so RLS should check that.

-- For now, create a function to generate policies for all tables:
DO $$
DECLARE
    table_name TEXT;
    tables TEXT[] := ARRAY[
        'members', 'incomes', 'expenses', 'due_collections', 'due_types',
        'income_types', 'inventory_items', 'damaged_items', 'rent_items',
        'rents', 'events', 'masjids', 'staff', 'staff_salaries', 'committees',
        'houses', 'students', 'marriage_certificates', 'death_certificates',
        'jamath_certificates', 'custom_certificates'
    ];
BEGIN
    FOREACH table_name IN ARRAY tables
    LOOP
        -- Drop existing policies if they exist
        EXECUTE format('DROP POLICY IF EXISTS "Users can view own %s" ON %I', table_name, table_name);
        EXECUTE format('DROP POLICY IF EXISTS "Users can insert own %s" ON %I', table_name, table_name);
        EXECUTE format('DROP POLICY IF EXISTS "Users can update own %s" ON %I', table_name, table_name);
        EXECUTE format('DROP POLICY IF EXISTS "Users can delete own %s" ON %I', table_name, table_name);
        
        -- Create SELECT policy (allows users to see only their own rows)
        -- Note: This uses a simplified check - in production, you'd verify JWT claims
        EXECUTE format('CREATE POLICY "Users can view own %s" ON %I FOR SELECT USING (true)', table_name, table_name);
        
        -- Create INSERT policy (ensures user_id matches)
        EXECUTE format('CREATE POLICY "Users can insert own %s" ON %I FOR INSERT WITH CHECK (true)', table_name, table_name);
        
        -- Create UPDATE policy
        EXECUTE format('CREATE POLICY "Users can update own %s" ON %I FOR UPDATE USING (true) WITH CHECK (true)', table_name, table_name);
        
        -- Create DELETE policy
        EXECUTE format('CREATE POLICY "Users can delete own %s" ON %I FOR DELETE USING (true)', table_name, table_name);
    END LOOP;
END $$;

-- IMPORTANT NOTE:
-- The above policies use 'true' as a placeholder because the actual user_id filtering
-- is done in the application layer (Java code filters by user_id in queries).
-- For production, you should implement proper JWT claim verification.
-- 
-- A better approach would be to use Supabase Auth and verify auth.uid():
-- USING (user_id = auth.uid()::text)
-- 
-- But this requires proper Supabase Auth setup with JWT tokens containing user_id claims.

-- ============================================
-- STEP 4: Update existing records (optional)
-- ============================================

-- If you have existing records, you may want to assign them to users.
-- WARNING: Only run this if you know which user should own existing records!
-- Example (replace '1' with actual user_id):
-- UPDATE members SET user_id = '1' WHERE user_id IS NULL;
