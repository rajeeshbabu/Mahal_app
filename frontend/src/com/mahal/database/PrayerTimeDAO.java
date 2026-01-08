package com.mahal.database;

import com.mahal.model.PrayerTime;
import com.mahal.sync.SyncHelper;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * DAO for managing daily prayer times.
 * All dates are stored and interpreted in server time; application uses IST
 * (Asia/Kolkata).
 */
public class PrayerTimeDAO {

    private final DatabaseService dbService;

    public PrayerTimeDAO() {
        this.dbService = DatabaseService.getInstance();
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS prayer_times (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id TEXT NOT NULL," + // CRITICAL: User isolation
                "prayer_date TEXT NOT NULL," +
                "fajr TEXT," +
                "sunrise TEXT," +
                "dhuhr TEXT," +
                "asr TEXT," +
                "maghrib TEXT," +
                "isha TEXT," +
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP," +
                "updated_at TEXT DEFAULT CURRENT_TIMESTAMP," +
                "UNIQUE(prayer_date, user_id)" + // Unique per user and date
                ")";
        try {
            dbService.executeUpdate(sql, null);
            DAOBase.ensureUserIdColumn(dbService, "prayer_times");
            // Drop old unique index if it exists and creates conflict (SQLite might not let
            // us easily mod unique constraints without recreation)
            // But ensureUserIdColumn handles column addition.
            // The UNIQUE constraint on just 'prayer_date' from old schema might persist if
            // we don't handle it.
            // For now, we assume ensureUserIdColumn is safe.
            // Create index for user_id
            try {
                dbService.executeUpdate("CREATE INDEX IF NOT EXISTS idx_prayer_times_user_id ON prayer_times(user_id)",
                        null);
            } catch (Exception e) {
            }
        } catch (Exception e) {
            System.err.println("Error creating prayer_times table: " + e.getMessage());
        }
    }

    private String getCurrentUserId() {
        com.mahal.util.SessionManager sessionManager = com.mahal.util.SessionManager.getInstance();
        com.mahal.model.User currentUser = sessionManager.getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            throw new IllegalStateException(
                    "No user logged in. Cannot perform database operation without user context.");
        }
        return String.valueOf(currentUser.getId());
    }

    public List<PrayerTime> getForMonth(LocalDate anyDateInMonth) {
        String userId = getCurrentUserId();
        LocalDate start = anyDateInMonth.withDayOfMonth(1);
        LocalDate end = start.plusMonths(1).minusDays(1);
        String sql = "SELECT id, user_id, prayer_date, fajr, sunrise, dhuhr, asr, maghrib, isha " +
                "FROM prayer_times WHERE user_id = ? AND prayer_date BETWEEN ? AND ? ORDER BY prayer_date";

        return dbService.executeQuery(sql, new Object[] { userId, Date.valueOf(start), Date.valueOf(end) }, rs -> {
            try {
                PrayerTime p = new PrayerTime();
                p.setId(rs.getLong("id"));
                p.setUserId(rs.getString("user_id"));
                Date d = rs.getDate("prayer_date");
                if (d != null) {
                    p.setDate(d.toLocalDate());
                }
                Time fajr = rs.getTime("fajr");
                if (fajr != null)
                    p.setFajr(fajr.toLocalTime());
                Time sunrise = rs.getTime("sunrise");
                if (sunrise != null)
                    p.setSunrise(sunrise.toLocalTime());
                Time dhuhr = rs.getTime("dhuhr");
                if (dhuhr != null)
                    p.setDhuhr(dhuhr.toLocalTime());
                Time asr = rs.getTime("asr");
                if (asr != null)
                    p.setAsr(asr.toLocalTime());
                Time maghrib = rs.getTime("maghrib");
                if (maghrib != null)
                    p.setMaghrib(maghrib.toLocalTime());
                Time isha = rs.getTime("isha");
                if (isha != null)
                    p.setIsha(isha.toLocalTime());
                return p;
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    public void saveOrUpdate(PrayerTime time) {
        String userId = getCurrentUserId();
        if (time.getId() == null) {
            // First try to get existing record
            List<PrayerTime> existing = getForMonth(time.getDate());
            PrayerTime existingTime = existing.stream()
                    .filter(pt -> pt.getDate().equals(time.getDate()))
                    .findFirst()
                    .orElse(null);

            if (existingTime != null) {
                time.setId(existingTime.getId());
                saveOrUpdate(time); // Recursively call with ID set
                return;
            }

            String insertSql = "INSERT INTO prayer_times " +
                    "(user_id, prayer_date, fajr, sunrise, dhuhr, asr, maghrib, isha, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))";
            Object[] params = {
                    userId,
                    Date.valueOf(time.getDate()),
                    toSqlTime(time.getFajr()),
                    toSqlTime(time.getSunrise()),
                    toSqlTime(time.getDhuhr()),
                    toSqlTime(time.getAsr()),
                    toSqlTime(time.getMaghrib()),
                    toSqlTime(time.getIsha())
            };
            Long newId = dbService.executeInsert(insertSql, params);

            // Queue for sync if record was created successfully
            if (newId != null) {
                time.setId(newId);
                SyncHelper.queueInsert("prayer_times", newId, time);
            }
        } else {
            String updateSql = "UPDATE prayer_times SET fajr = ?, sunrise = ?, dhuhr = ?, asr = ?, " +
                    "maghrib = ?, isha = ?, prayer_date = ?, updated_at = datetime('now') WHERE id = ? AND user_id = ?";
            Object[] params = {
                    toSqlTime(time.getFajr()),
                    toSqlTime(time.getSunrise()),
                    toSqlTime(time.getDhuhr()),
                    toSqlTime(time.getAsr()),
                    toSqlTime(time.getMaghrib()),
                    toSqlTime(time.getIsha()),
                    Date.valueOf(time.getDate()),
                    time.getId(),
                    userId
            };
            boolean success = dbService.executeUpdate(updateSql, params) > 0;

            // Queue for sync if update was successful
            if (success) {
                SyncHelper.queueUpdate("prayer_times", time.getId(), time);
            }
        }
    }

    public boolean upsertFromSupabase(PrayerTime time, String supabaseUpdatedAt) {
        if (time == null || time.getId() == null) {
            return false;
        }

        String userId = getCurrentUserId();

        // Check if record exists locally
        // We can't use filtered getForMonth effectively for single ID check without
        // fetching all.
        // Better to query directly.
        String checkSql = "SELECT id FROM prayer_times WHERE id = ? AND user_id = ?";
        List<Object> checkResults = dbService.executeQuery(checkSql, new Object[] { time.getId(), userId }, rs -> {
            try {
                return rs.getObject("id");
            } catch (SQLException e) {
                return null;
            }
        });

        boolean exists = !checkResults.isEmpty();

        if (!exists) {
            // INSERT
            String insertSql = "INSERT INTO prayer_times " +
                    "(id, user_id, prayer_date, fajr, sunrise, dhuhr, asr, maghrib, isha, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), ?)";
            Object[] params = {
                    time.getId(),
                    userId,
                    time.getDate() != null ? Date.valueOf(time.getDate()) : null,
                    toSqlTime(time.getFajr()),
                    toSqlTime(time.getSunrise()),
                    toSqlTime(time.getDhuhr()),
                    toSqlTime(time.getAsr()),
                    toSqlTime(time.getMaghrib()),
                    toSqlTime(time.getIsha()),
                    supabaseUpdatedAt != null ? supabaseUpdatedAt : "datetime('now')"
            };
            try {
                return dbService.executeUpdate(insertSql, params) > 0;
            } catch (Exception e) {
                System.err.println("PrayerTimeDAO.upsertFromSupabase (INSERT): " + e.getMessage());
                return false;
            }
        } else {
            // UPDATE
            String updateSql = "UPDATE prayer_times SET fajr = ?, sunrise = ?, dhuhr = ?, asr = ?, " +
                    "maghrib = ?, isha = ?, prayer_date = ?, updated_at = ? WHERE id = ? AND user_id = ?";
            Object[] params = {
                    toSqlTime(time.getFajr()),
                    toSqlTime(time.getSunrise()),
                    toSqlTime(time.getDhuhr()),
                    toSqlTime(time.getAsr()),
                    toSqlTime(time.getMaghrib()),
                    toSqlTime(time.getIsha()),
                    time.getDate() != null ? Date.valueOf(time.getDate()) : null,
                    supabaseUpdatedAt != null ? supabaseUpdatedAt : "datetime('now')",
                    time.getId(),
                    userId
            };
            try {
                return dbService.executeUpdate(updateSql, params) > 0;
            } catch (Exception e) {
                System.err.println("PrayerTimeDAO.upsertFromSupabase (UPDATE): " + e.getMessage());
                return false;
            }
        }
    }

    private Time toSqlTime(LocalTime time) {
        if (time == null)
            return null;
        return Time.valueOf(time);
    }
}
