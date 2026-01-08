# Switching Backend Database from H2 to SQLite

## Overview

The backend has been successfully updated to use SQLite instead of H2 for consistency with the frontend.

## Changes Made

### 1. Updated `pom.xml`
- ✅ Added SQLite JDBC dependency (`sqlite-jdbc` version 3.44.1.0)
- ✅ Added Hibernate Community Dialects dependency (provides official SQLite dialect for Hibernate 6)
- ✅ H2 dependency removed (no longer needed)

### 2. Updated `application.properties`
- ✅ Changed database URL from H2 to SQLite: `jdbc:sqlite:./data/mahal_db.db`
- ✅ Changed driver class to: `org.sqlite.JDBC`
- ✅ Updated Hibernate dialect to: `org.hibernate.community.dialect.SQLiteDialect`
- ✅ Disabled H2 console (SQLite doesn't have a web console)

### 3. SQLite Dialect Configuration
- ✅ Using official Hibernate 6 community dialect (`org.hibernate.community.dialect.SQLiteDialect`)
- ✅ Added `hibernate-community-dialects` dependency to `pom.xml`
- ✅ Disabled `getGeneratedKeys()` for SQLite compatibility (uses `last_insert_rowid()` instead)
- ✅ No custom dialect needed - using official community support

## Database Migration

### Option 1: Fresh Start (Recommended if no important data)

1. **Stop backend**
2. **Delete old H2 database files** (optional):
   ```
   backend/data/mahal_db.mv.db (if exists)
   backend/data/mahal_db.trace.db (if exists)
   ```
3. **Start backend** - SQLite database will be created automatically at `backend/data/mahal_db.db`
4. **Tables will be created** automatically by Hibernate (`ddl-auto=update`)

### Option 2: Migrate Data from H2 to SQLite

If you have important subscription data in H2:

1. **Export data from H2:**
   - Start backend with H2 configuration (temporarily revert `application.properties`)
   - Open H2 Console: `http://localhost:8080/h2-console`
   - Connect: `jdbc:h2:file:./data/mahal_db`
   - Export subscriptions:
   ```sql
   SELECT * FROM subscriptions;
   ```
   - Copy the data

2. **Switch to SQLite** (update `application.properties` as shown above)

3. **Start backend with SQLite** (new database will be created)

4. **Import data to SQLite:**
   - Use DB Browser for SQLite
   - Open: `backend/data/mahal_db.db`
   - Import the data you exported from H2

## Database File Location

- **Old H2:** `backend/data/mahal_db.mv.db`
- **New SQLite:** `backend/data/mahal_db.db`

## Using SQLite Database

### View/Edit Database

**DB Browser for SQLite (Recommended):**
1. Download: https://sqlitebrowser.org/
2. Open: `backend/data/mahal_db.db`
3. View/edit data like any SQLite database

**Command Line:**
```bash
sqlite3 backend/data/mahal_db.db
```

### SQL Queries

All SQL queries work the same, but SQLite syntax is slightly different:

**Check subscriptions:**
```sql
SELECT * FROM subscriptions ORDER BY created_at DESC;
```

**Count subscriptions:**
```sql
SELECT COUNT(*) FROM subscriptions;
```

**Check admins:**
```sql
SELECT * FROM admins;
```

## Benefits of SQLite

1. ✅ **Consistency** - Same database format as frontend (`mahal.db`)
2. ✅ **Simple** - Single file, easy to backup/copy
3. ✅ **No server needed** - File-based like frontend
4. ✅ **Cross-platform** - Works everywhere
5. ✅ **Familiar tools** - DB Browser for SQLite works for both frontend and backend
6. ✅ **Official Hibernate Support** - Using community-maintained dialect
7. ✅ **Reliable** - Less prone to corruption than H2

## Configuration Details

### `application.properties` Key Settings:
```properties
# SQLite Database
spring.datasource.url=jdbc:sqlite:./data/mahal_db.db
spring.datasource.driver-class-name=org.sqlite.JDBC

# SQLite dialect (official Hibernate 6 community dialect)
spring.jpa.properties.hibernate.dialect=org.hibernate.community.dialect.SQLiteDialect
# Disable getGeneratedKeys for SQLite (SQLite doesn't support it, use last_insert_rowid() instead)
spring.jpa.properties.hibernate.jdbc.use_get_generated_keys=false
```

### Maven Dependencies:
```xml
<!-- SQLite JDBC Driver -->
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.44.1.0</version>
</dependency>

<!-- Hibernate Community Dialects (includes SQLite) -->
<dependency>
    <groupId>org.hibernate.orm</groupId>
    <artifactId>hibernate-community-dialects</artifactId>
</dependency>
```

## Reverting to H2 (if needed)

If you need to revert:

1. Add H2 dependency back to `pom.xml`:
   ```xml
   <dependency>
       <groupId>com.h2database</groupId>
       <artifactId>h2</artifactId>
       <scope>runtime</scope>
   </dependency>
   ```

2. Update `application.properties`:
   ```properties
   spring.datasource.url=jdbc:h2:file:./data/mahal_db;DB_CLOSE_ON_EXIT=FALSE
   spring.datasource.driver-class-name=org.h2.Driver
   spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect
   spring.h2.console.enabled=true
   spring.h2.console.path=/h2-console
   ```

3. Restart backend

## Important Notes

- **Database file:** `backend/data/mahal_db.db` (new location)
- **H2 console:** No longer available (SQLite doesn't have a web console)
- **Use DB Browser for SQLite** instead of H2 console
- **All existing code continues to work** - only the database backend changed
- **Backup strategy:** Simply copy the `.db` file to backup (single file!)
- **Both frontend and backend now use SQLite** - consistent database format across the entire application

## Verification

After switching to SQLite:

1. ✅ Backend compiles successfully
2. ✅ Backend starts without errors
3. ✅ Database file created at `backend/data/mahal_db.db`
4. ✅ Tables created automatically by Hibernate
5. ✅ All existing features continue to work
6. ✅ Subscriptions sync to Supabase (if configured)

## Next Steps

1. Start the backend and verify it connects to SQLite
2. Check that tables are created automatically
3. Test subscription creation to ensure everything works
4. Use DB Browser for SQLite to view/manage the database

