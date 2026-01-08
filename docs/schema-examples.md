# Database Schema Examples

## SQLite Schema (Local Database)

```sql
-- Members table with sync fields
CREATE TABLE IF NOT EXISTS members (
    id TEXT PRIMARY KEY,                    -- UUID (36 chars)
    user_id TEXT NOT NULL,                  -- JWT user_id - CRITICAL for isolation
    name TEXT NOT NULL,
    qualification TEXT,
    father_name TEXT,
    mother_name TEXT,
    district TEXT,
    panchayat TEXT,
    mahal TEXT,
    date_of_birth TEXT,                     -- ISO 8601 date (YYYY-MM-DD)
    address TEXT,
    mobile TEXT,
    gender TEXT,
    id_proof_type TEXT,
    id_proof_no TEXT,
    photo_path TEXT,
    updated_at TEXT NOT NULL,               -- ISO 8601 UTC timestamp - CRITICAL for conflict resolution
    created_at TEXT NOT NULL,               -- ISO 8601 UTC timestamp
    is_synced INTEGER DEFAULT 0,            -- 0 = false, 1 = true
    sync_version INTEGER DEFAULT 0,         -- Optimistic locking
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_members_user_id ON members(user_id);
CREATE INDEX idx_members_updated_at ON members(updated_at);
CREATE INDEX idx_members_is_synced ON members(is_synced);

-- Sync metadata table
CREATE TABLE IF NOT EXISTS sync_metadata (
    user_id TEXT PRIMARY KEY,
    last_sync_time TEXT,                    -- ISO 8601 UTC timestamp
    updated_at TEXT DEFAULT CURRENT_TIMESTAMP
);

-- Sync queue (for offline operations)
CREATE TABLE IF NOT EXISTS sync_queue (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    table_name TEXT NOT NULL,
    operation TEXT NOT NULL,                -- INSERT, UPDATE, DELETE
    record_id TEXT NOT NULL,                -- UUID
    user_id TEXT NOT NULL,
    data TEXT,                              -- JSON string
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    synced_at TEXT,
    sync_status TEXT DEFAULT 'PENDING',     -- PENDING, SYNCING, SYNCED, FAILED
    retry_count INTEGER DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_sync_queue_user_status ON sync_queue(user_id, sync_status);
CREATE INDEX idx_sync_queue_created_at ON sync_queue(created_at);
```

## Supabase/PostgreSQL Schema

```sql
-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Members table
CREATE TABLE members (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,                  -- JWT user_id - CRITICAL for isolation
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
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),  -- UTC timestamp - CRITICAL for conflict resolution
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),  -- UTC timestamp
    sync_version BIGINT NOT NULL DEFAULT 0,         -- Optimistic locking
    
    CONSTRAINT fk_members_user_id FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE
);

CREATE INDEX idx_members_user_id ON members(user_id);
CREATE INDEX idx_members_updated_at ON members(updated_at);

-- Row Level Security (RLS) policies for user isolation
ALTER TABLE members ENABLE ROW LEVEL SECURITY;

-- Policy: Users can only see their own records
CREATE POLICY "Users can view own members"
    ON members FOR SELECT
    USING (auth.uid() = user_id);

-- Policy: Users can only insert their own records
CREATE POLICY "Users can insert own members"
    ON members FOR INSERT
    WITH CHECK (auth.uid() = user_id);

-- Policy: Users can only update their own records
CREATE POLICY "Users can update own members"
    ON members FOR UPDATE
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- Policy: Users can only delete their own records
CREATE POLICY "Users can delete own members"
    ON members FOR DELETE
    USING (auth.uid() = user_id);

-- Function to auto-update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    NEW.sync_version = OLD.sync_version + 1;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger to auto-update updated_at on UPDATE
CREATE TRIGGER update_members_updated_at
    BEFORE UPDATE ON members
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Similar schema for other tables (incomes, expenses, etc.)
CREATE TABLE incomes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    income_type_id UUID,
    amount DECIMAL(10, 2) NOT NULL,
    date DATE NOT NULL,
    member_id UUID,
    masjid_id UUID,
    notes TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    sync_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_incomes_user_id FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE
);

-- Apply RLS policies to incomes table
ALTER TABLE incomes ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own incomes" ON incomes FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Users can insert own incomes" ON incomes FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users can update own incomes" ON incomes FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users can delete own incomes" ON incomes FOR DELETE USING (auth.uid() = user_id);

CREATE INDEX idx_incomes_user_id ON incomes(user_id);
CREATE INDEX idx_incomes_updated_at ON incomes(updated_at);

-- Sync metadata table (optional, for server-side tracking)
CREATE TABLE sync_metadata (
    user_id UUID PRIMARY KEY,
    last_sync_time TIMESTAMPTZ,
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT fk_sync_metadata_user_id FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE
);
```

## Key Schema Requirements

### 1. ID Strategy (UUID)
- **SQLite**: `TEXT PRIMARY KEY` (36 characters for UUID)
- **PostgreSQL**: `UUID PRIMARY KEY DEFAULT uuid_generate_v4()`
- **Why**: Prevents ID conflicts across distributed clients

### 2. User Isolation (user_id)
- **Every table must have `user_id` column**
- **Foreign key** to users table
- **RLS policies** in Supabase (users can only access their own records)
- **Why**: Multi-user security - prevents data leakage

### 3. Timestamps (updated_at)
- **SQLite**: `TEXT` storing ISO 8601 UTC timestamps
- **PostgreSQL**: `TIMESTAMPTZ` (timezone-aware, UTC)
- **Auto-update on UPDATE** via triggers/functions
- **Index on updated_at** for incremental sync queries
- **Why**: Conflict resolution (last-write-wins)

### 4. Sync Fields
- **is_synced**: Boolean flag (local tracking)
- **sync_version**: Long integer (optimistic locking)
- **Why**: Track sync status and handle conflicts

### 5. Indexes
- **user_id**: For filtering by user
- **updated_at**: For incremental sync queries
- **is_synced**: For finding unsynced records
- **Why**: Performance for sync queries
