-- ============================================
-- Fix missing index for masjids table
-- Run this if masjids table is missing its index
-- ============================================

CREATE INDEX IF NOT EXISTS idx_masjids_user_id ON masjids(user_id);



