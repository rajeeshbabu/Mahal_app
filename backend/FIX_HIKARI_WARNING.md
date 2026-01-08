# Fix: HikariCP Thread Starvation Warning

## Warning Message

```
HikariPool-2 - Thread starvation or clock leap detected (housekeeper delta=2h41m35s)
```

## What This Means

This warning appears when:
1. **System clock was adjusted** (most common) - Your computer's time was changed
2. **JVM was paused** - Long garbage collection pause or system sleep
3. **Thread starvation** - Less common, but possible if system is overloaded

**Important**: This is usually just a **warning**, not an error. Your application should still work fine.

## Is This a Problem?

**Usually NO** - This is just HikariCP detecting that time jumped forward. It's harmless unless:
- You see actual connection pool errors
- Database connections are failing
- Application is slow or unresponsive

## Solutions

### Solution 1: Ignore It (Recommended)

If your application is working fine, you can safely ignore this warning. It's just HikariCP being cautious.

### Solution 2: Adjust HikariCP Configuration

I've already updated `application.properties` with better HikariCP settings for SQLite:

```properties
# HikariCP Connection Pool Configuration
spring.datasource.hikari.housekeeping-period-ms=30000
spring.datasource.hikari.maximum-pool-size=5
spring.datasource.hikari.minimum-idle=1
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

**After updating**, restart your backend to apply the changes.

### Solution 3: Disable Warning (Not Recommended)

You can suppress this specific warning in `application.properties`:

```properties
# Suppress HikariCP housekeeper warnings
logging.level.com.zaxxer.hikari.pool.HikariPool=ERROR
```

**Note**: This hides the warning but doesn't fix the underlying issue.

### Solution 4: Check System Clock

If you see this warning frequently:

1. **Windows**: 
   - Right-click clock → Adjust date/time
   - Ensure "Set time automatically" is enabled

2. **Check for time sync issues**:
   - Make sure your system time is correct
   - Avoid manually changing system time while the app is running

## Why This Happens with SQLite

SQLite uses file-based connections, and HikariCP's housekeeper thread checks connection health periodically. If your system clock jumps (e.g., time sync, manual adjustment), HikariCP detects this and logs a warning.

## Verification

After applying the fix:

1. **Restart backend**
2. **Monitor logs** - The warning should appear less frequently or not at all
3. **Check application** - Ensure everything still works normally

## When to Worry

Only worry if you see:
- ❌ Actual connection errors
- ❌ Database queries failing
- ❌ Application crashes
- ❌ Multiple warnings in quick succession

If you only see this warning occasionally, it's **completely normal** and can be ignored.

