-- Supabase Database Schema for Mahal Management System
-- Run this SQL in your Supabase SQL Editor to create all required tables

-- Table: admins
CREATE TABLE IF NOT EXISTS admins (
    id BIGSERIAL PRIMARY KEY,
    user_id TEXT NOT NULL,
    name TEXT NOT NULL,
    password TEXT NOT NULL,
    full_name TEXT,
    active INTEGER DEFAULT 1,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_admins_user_id ON admins(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_admins_name_unique ON admins(user_id, name);
CREATE INDEX IF NOT EXISTS idx_admins_active ON admins(active);

-- Table: incomes
CREATE TABLE IF NOT EXISTS incomes (
    id BIGSERIAL PRIMARY KEY,
    masjid_id BIGINT,
    member_id BIGINT,
    income_type_id BIGINT,
    amount NUMERIC(15, 2),
    date DATE,
    payment_mode TEXT,
    receipt_no TEXT,
    remarks TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Table: expenses
CREATE TABLE IF NOT EXISTS expenses (
    id BIGSERIAL PRIMARY KEY,
    expense_type TEXT,
    amount NUMERIC(15, 2),
    date DATE,
    masjid_id BIGINT,
    notes TEXT,
    receipt_path TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Table: members
CREATE TABLE IF NOT EXISTS members (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    qualification TEXT,
    father_name TEXT,
    mother_name TEXT,
    district TEXT,
    panchayat TEXT,
    mahal TEXT,
    date_of_birth DATE,
    address TEXT,
    mobile TEXT,
    gender TEXT,
    id_proof_type TEXT,
    id_proof_no TEXT,
    photo_path TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Table: due_collections
CREATE TABLE IF NOT EXISTS due_collections (
    id BIGSERIAL PRIMARY KEY,
    masjid_id BIGINT,
    member_id BIGINT,
    due_type_id BIGINT,
    amount NUMERIC(15, 2),
    date DATE,
    payment_mode TEXT,
    receipt_no TEXT,
    remarks TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Table: inventory_items
CREATE TABLE IF NOT EXISTS inventory_items (
    id BIGSERIAL PRIMARY KEY,
    item_name TEXT NOT NULL,
    sku_code TEXT,
    quantity INTEGER,
    location TEXT,
    purchase_date DATE,
    supplier TEXT,
    value NUMERIC(15, 2),
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Table: damaged_items
CREATE TABLE IF NOT EXISTS damaged_items (
    id BIGSERIAL PRIMARY KEY,
    inventory_item_id BIGINT,
    quantity INTEGER,
    damage_date DATE,
    reason TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Table: rent_items
CREATE TABLE IF NOT EXISTS rent_items (
    id BIGSERIAL PRIMARY KEY,
    inventory_item_id BIGINT,
    rate_per_day NUMERIC(15, 2),
    deposit NUMERIC(15, 2),
    available BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Table: rents
CREATE TABLE IF NOT EXISTS rents (
    id BIGSERIAL PRIMARY KEY,
    rent_item_id BIGINT,
    renter_name TEXT,
    renter_mobile TEXT,
    rent_start_date DATE,
    rent_end_date DATE,
    amount NUMERIC(15, 2),
    deposit NUMERIC(15, 2),
    status TEXT DEFAULT 'BOOKED',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Table: events
CREATE TABLE IF NOT EXISTS events (
    id BIGSERIAL PRIMARY KEY,
    event_name TEXT NOT NULL,
    start_date_time TIMESTAMPTZ,
    end_date_time TIMESTAMPTZ,
    event_place TEXT,
    masjid_id BIGINT,
    event_details TEXT,
    organizer TEXT,
    contact TEXT,
    attachments_path TEXT,
    is_public BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Table: masjids
CREATE TABLE IF NOT EXISTS masjids (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    abbreviation TEXT,
    address TEXT,
    waqf_board_no TEXT,
    state TEXT,
    email TEXT,
    mobile TEXT,
    registration_no TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Table: staff
CREATE TABLE IF NOT EXISTS staff (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    designation TEXT,
    salary NUMERIC(15, 2),
    address TEXT,
    mobile TEXT,
    email TEXT,
    joining_date DATE,
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Table: staff_salaries
CREATE TABLE IF NOT EXISTS staff_salaries (
    id BIGSERIAL PRIMARY KEY,
    staff_id BIGINT NOT NULL,
    salary NUMERIC(15, 2),
    paid_date DATE,
    paid_amount NUMERIC(15, 2),
    payment_mode TEXT,
    remarks TEXT,
    balance NUMERIC(15, 2),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Table: committees
CREATE TABLE IF NOT EXISTS committees (
    id BIGSERIAL PRIMARY KEY,
    member_name TEXT NOT NULL,
    mobile TEXT,
    designation TEXT,
    other_details TEXT,
    masjid_id BIGINT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Table: houses
CREATE TABLE IF NOT EXISTS houses (
    id BIGSERIAL PRIMARY KEY,
    address TEXT NOT NULL,
    house_number TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Table: students
CREATE TABLE IF NOT EXISTS students (
    id BIGSERIAL PRIMARY KEY,
    user_id TEXT NOT NULL,
    name TEXT NOT NULL,
    course TEXT,
    admission_number TEXT,
    admission_date DATE,
    mobile TEXT,
    email TEXT,
    address TEXT,
    father_name TEXT,
    mother_name TEXT,
    guardian_mobile TEXT,
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Table: income_types
CREATE TABLE IF NOT EXISTS income_types (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    type TEXT,
    default_amount NUMERIC(15, 2),
    description TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Table: due_types
CREATE TABLE IF NOT EXISTS due_types (
    id BIGSERIAL PRIMARY KEY,
    due_name TEXT NOT NULL,
    frequency TEXT,
    amount NUMERIC(15, 2),
    description TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Table: prayer_times
CREATE TABLE IF NOT EXISTS prayer_times (
    id BIGSERIAL PRIMARY KEY,
    prayer_date DATE NOT NULL UNIQUE,
    fajr TEXT,
    sunrise TEXT,
    dhuhr TEXT,
    asr TEXT,
    maghrib TEXT,
    isha TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Table: marriage_certificates
CREATE TABLE IF NOT EXISTS marriage_certificates (
    id BIGSERIAL PRIMARY KEY,
    certificate_no TEXT,
    groom_name TEXT,
    bride_name TEXT,
    parent_name_of_groom TEXT,
    parent_name_of_bride TEXT,
    address_of_groom TEXT,
    address_of_bride TEXT,
    place_of_marriage TEXT,
    marriage_status TEXT,
    marriage_date DATE,
    additional_notes TEXT,
    supporting_docs_path TEXT,
    pdf_path TEXT,
    qr_code TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Table: death_certificates
CREATE TABLE IF NOT EXISTS death_certificates (
    id BIGSERIAL PRIMARY KEY,
    certificate_no TEXT,
    name TEXT,
    parent_name TEXT,
    address TEXT,
    thalook TEXT,
    date_of_death DATE,
    cause TEXT,
    place_of_death TEXT,
    issued_date DATE,
    pdf_path TEXT,
    qr_code TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Table: jamath_certificates
CREATE TABLE IF NOT EXISTS jamath_certificates (
    id BIGSERIAL PRIMARY KEY,
    certificate_no TEXT,
    name TEXT,
    parent_name TEXT,
    address TEXT,
    thalook TEXT,
    date DATE,
    remarks TEXT,
    pdf_path TEXT,
    qr_code TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Table: custom_certificates
CREATE TABLE IF NOT EXISTS custom_certificates (
    id BIGSERIAL PRIMARY KEY,
    certificate_no TEXT,
    template_name TEXT,
    template_content TEXT,
    field_data TEXT,
    issued_date DATE,
    pdf_path TEXT,
    qr_code TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Enable Row Level Security (RLS) policies for public access
-- This allows the anon role to INSERT, UPDATE, DELETE, and SELECT

ALTER TABLE admins ENABLE ROW LEVEL SECURITY;
ALTER TABLE incomes ENABLE ROW LEVEL SECURITY;
ALTER TABLE expenses ENABLE ROW LEVEL SECURITY;
ALTER TABLE members ENABLE ROW LEVEL SECURITY;
ALTER TABLE due_collections ENABLE ROW LEVEL SECURITY;
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
ALTER TABLE students ENABLE ROW LEVEL SECURITY;
ALTER TABLE income_types ENABLE ROW LEVEL SECURITY;
ALTER TABLE due_types ENABLE ROW LEVEL SECURITY;
ALTER TABLE prayer_times ENABLE ROW LEVEL SECURITY;
ALTER TABLE marriage_certificates ENABLE ROW LEVEL SECURITY;
ALTER TABLE death_certificates ENABLE ROW LEVEL SECURITY;
ALTER TABLE jamath_certificates ENABLE ROW LEVEL SECURITY;
ALTER TABLE custom_certificates ENABLE ROW LEVEL SECURITY;

-- Create policies to allow all operations for anon role
-- You may want to restrict these policies based on your security requirements

CREATE POLICY "Allow all operations for anon role on admins" ON admins
    FOR ALL USING (true) WITH CHECK (true);

CREATE POLICY "Allow all operations for anon role on incomes" ON incomes
    FOR ALL USING (true) WITH CHECK (true);

CREATE POLICY "Allow all operations for anon role on expenses" ON expenses
    FOR ALL USING (true) WITH CHECK (true);

CREATE POLICY "Allow all operations for anon role on members" ON members
    FOR ALL USING (true) WITH CHECK (true);

CREATE POLICY "Allow all operations for anon role on due_collections" ON due_collections
    FOR ALL USING (true) WITH CHECK (true);

CREATE POLICY "Allow all operations for anon role on inventory_items" ON inventory_items
    FOR ALL USING (true) WITH CHECK (true);

CREATE POLICY "Allow all operations for anon role on damaged_items" ON damaged_items
    FOR ALL USING (true) WITH CHECK (true);

CREATE POLICY "Allow all operations for anon role on rent_items" ON rent_items
    FOR ALL USING (true) WITH CHECK (true);

CREATE POLICY "Allow all operations for anon role on rents" ON rents
    FOR ALL USING (true) WITH CHECK (true);

CREATE POLICY "Allow all operations for anon role on events" ON events
    FOR ALL USING (true) WITH CHECK (true);

CREATE POLICY "Allow all operations for anon role on masjids" ON masjids
    FOR ALL USING (true) WITH CHECK (true);

CREATE POLICY "Allow all operations for anon role on staff" ON staff
    FOR ALL USING (true) WITH CHECK (true);

CREATE POLICY "Allow all operations for anon role on staff_salaries" ON staff_salaries
    FOR ALL USING (true) WITH CHECK (true);

CREATE POLICY "Allow all operations for anon role on committees" ON committees
    FOR ALL USING (true) WITH CHECK (true);

CREATE POLICY "Allow all operations for anon role on houses" ON houses
    FOR ALL USING (true) WITH CHECK (true);

CREATE POLICY "Allow all operations for anon role on students" ON students
    FOR ALL USING (true) WITH CHECK (true);

CREATE POLICY "Allow all operations for anon role on income_types" ON income_types
    FOR ALL USING (true) WITH CHECK (true);

CREATE POLICY "Allow all operations for anon role on due_types" ON due_types
    FOR ALL USING (true) WITH CHECK (true);

CREATE POLICY "Allow all operations for anon role on prayer_times" ON prayer_times
    FOR ALL USING (true) WITH CHECK (true);

CREATE POLICY "Allow all operations for anon role on marriage_certificates" ON marriage_certificates
    FOR ALL USING (true) WITH CHECK (true);

CREATE POLICY "Allow all operations for anon role on death_certificates" ON death_certificates
    FOR ALL USING (true) WITH CHECK (true);

CREATE POLICY "Allow all operations for anon role on jamath_certificates" ON jamath_certificates
    FOR ALL USING (true) WITH CHECK (true);

CREATE POLICY "Allow all operations for anon role on custom_certificates" ON custom_certificates
    FOR ALL USING (true) WITH CHECK (true);

