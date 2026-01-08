-- SQL Migration Script: Add user_id column to all local SQLite tables
-- Run this script to add user_id column to existing tables

-- Members table
ALTER TABLE members ADD COLUMN user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_members_user_id ON members(user_id);

-- Incomes table
ALTER TABLE incomes ADD COLUMN user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_incomes_user_id ON incomes(user_id);

-- Expenses table
ALTER TABLE expenses ADD COLUMN user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_expenses_user_id ON expenses(user_id);

-- Due Collections table
ALTER TABLE due_collections ADD COLUMN user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_due_collections_user_id ON due_collections(user_id);

-- Due Types table
ALTER TABLE due_types ADD COLUMN user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_due_types_user_id ON due_types(user_id);

-- Income Types table
ALTER TABLE income_types ADD COLUMN user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_income_types_user_id ON income_types(user_id);

-- Inventory Items table
ALTER TABLE inventory_items ADD COLUMN user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_inventory_items_user_id ON inventory_items(user_id);

-- Damaged Items table
ALTER TABLE damaged_items ADD COLUMN user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_damaged_items_user_id ON damaged_items(user_id);

-- Rent Items table
ALTER TABLE rent_items ADD COLUMN user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_rent_items_user_id ON rent_items(user_id);

-- Rents table
ALTER TABLE rents ADD COLUMN user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_rents_user_id ON rents(user_id);

-- Events table
ALTER TABLE events ADD COLUMN user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_events_user_id ON events(user_id);

-- Masjids table
ALTER TABLE masjids ADD COLUMN user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_masjids_user_id ON masjids(user_id);

-- Admins table
ALTER TABLE admins ADD COLUMN user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_admins_user_id ON admins(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_admins_user_name_unique ON admins(user_id, name);
-- Set user_id = id for existing admins
UPDATE admins SET user_id = CAST(id AS TEXT) WHERE user_id IS NULL OR user_id = '';

-- Staff table
ALTER TABLE staff ADD COLUMN user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_staff_user_id ON staff(user_id);

-- Staff Salaries table
ALTER TABLE staff_salaries ADD COLUMN user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_staff_salaries_user_id ON staff_salaries(user_id);

-- Committees table
ALTER TABLE committees ADD COLUMN user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_committees_user_id ON committees(user_id);

-- Houses table
ALTER TABLE houses ADD COLUMN user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_houses_user_id ON houses(user_id);

-- Marriage Certificates table
ALTER TABLE marriage_certificates ADD COLUMN user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_marriage_certificates_user_id ON marriage_certificates(user_id);

-- Death Certificates table
ALTER TABLE death_certificates ADD COLUMN user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_death_certificates_user_id ON death_certificates(user_id);

-- Jamath Certificates table
ALTER TABLE jamath_certificates ADD COLUMN user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_jamath_certificates_user_id ON jamath_certificates(user_id);

-- Custom Certificates table
ALTER TABLE custom_certificates ADD COLUMN user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_custom_certificates_user_id ON custom_certificates(user_id);

-- Prayer Times table (optional - may be shared, but adding for consistency)
ALTER TABLE prayer_times ADD COLUMN user_id TEXT;
CREATE INDEX IF NOT EXISTS idx_prayer_times_user_id ON prayer_times(user_id);

-- Note: After running this migration, you'll need to update existing records
-- to assign them to users. You may want to assign all existing records to the first admin user.
-- Example:
-- UPDATE members SET user_id = '1' WHERE user_id IS NULL;
-- (Replace '1' with the actual user_id of the admin user)
