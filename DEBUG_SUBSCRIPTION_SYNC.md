# Debug: Subscriptions Not Appearing in Supabase

## Current Architecture

Subscriptions are stored in:
- ✅ **Backend H2 Database** (`backend/data/mahal_db`) - This is the PRIMARY storage
- ❌ **NOT in SQLite** (`frontend/mahal.db`) - Subscriptions are backend-only
- ❓ **Supabase** - Should sync from backend, but might not be configured

## Issue: Subscriptions Not in Supabase

### Check 1: Is Supabase Configured?

Open `backend/src/main/resources/application.properties` and check:

```properties
# These should NOT be commented out:
supabase.url=https://your-project-id.supabase.co
supabase.key=your-supabase-anon-key
```

If they're commented (starting with `#`), Supabase sync won't work.

### Check 2: Check Backend Logs

When a subscription is created, you should see one of these in the backend console:

**If Supabase is configured:**
```
✓ Successfully synced subscription to Supabase (user_id: user@example.com)
```

**If Supabase is NOT configured:**
- No error message (silently skipped)
- But subscriptions ARE saved to H2 database

### Check 3: Verify Subscription is in H2 Database

1. Open H2 Console: `http://localhost:8080/h2-console`
2. Connect: `jdbc:h2:file:./data/mahal_db`, user: `sa`, password: (empty)
3. Run:
```sql
SELECT * FROM subscriptions ORDER BY created_at DESC;
```

If you see subscriptions here, they ARE being saved - just not syncing to Supabase.

## Fix: Configure Supabase

1. **Get your Supabase credentials:**
   - Go to Supabase Dashboard → Settings → API
   - Copy:
     - Project URL (e.g., `https://xxxxx.supabase.co`)
     - Anon/Public Key

2. **Update `backend/src/main/resources/application.properties`:**
```properties
# Uncomment and fill in your values:
supabase.url=https://your-project-id.supabase.co
supabase.key=your-supabase-anon-key
```

3. **Restart the backend** for changes to take effect

4. **Create a new subscription** and check backend logs for sync confirmation

## Fix: Create Subscriptions Table in Supabase

Make sure the subscriptions table exists in Supabase:

1. Run `backend/sql/CREATE_SUBSCRIPTIONS_TABLE_SUPABASE.sql` in Supabase SQL Editor
2. Or run `backend/sql/ADD_USER_EMAIL_SUPABASE.sql` if table exists but missing `user_email` column

## About SQLite

**Note:** Subscriptions are NOT stored in the frontend SQLite database (`frontend/mahal.db`). They're stored in the backend H2 database. This is by design:

- **Frontend SQLite** = Local data (members, masjids, incomes, etc.)
- **Backend H2** = Subscriptions (managed by backend API)
- **Supabase** = Cloud sync (synced from backend)

If you need subscriptions in SQLite for some reason, that would require additional implementation, but it's not standard practice.


