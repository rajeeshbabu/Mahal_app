package com.mahal.database;

import com.mahal.model.Event;
import com.mahal.sync.SyncHelper;
import com.mahal.util.SessionManager;
import com.mahal.model.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class EventDAO {
    private final DatabaseService db;

    public EventDAO() {
        this.db = DatabaseService.getInstance();
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS events (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id TEXT NOT NULL, " + // CRITICAL: User isolation
                "event_name TEXT NOT NULL, " +
                "start_date_time TEXT, " +
                "end_date_time TEXT, " +
                "event_place TEXT, " +
                "masjid_id INTEGER, " +
                "event_details TEXT, " +
                "organizer TEXT, " +
                "contact TEXT, " +
                "attachments_path TEXT, " +
                "is_public INTEGER DEFAULT 1, " +
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                ")";
        try {
            db.executeUpdate(sql, null);
            DAOBase.ensureUserIdColumn(db, "events");
            try {
                db.executeUpdate("CREATE INDEX IF NOT EXISTS idx_events_user_id ON events(user_id)", null);
            } catch (Exception e) {
            }
        } catch (Exception e) {
            System.err.println("Error creating events table: " + e.getMessage());
        }
    }

    private String getCurrentUserId() {
        SessionManager sessionManager = SessionManager.getInstance();
        User currentUser = sessionManager.getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            throw new IllegalStateException(
                    "No user logged in. Cannot perform database operation without user context.");
        }
        return String.valueOf(currentUser.getId());
    }

    public List<Event> getAll() {
        String userId = getCurrentUserId();
        String sql = "SELECT e.id, e.user_id, e.event_name, e.start_date_time, e.end_date_time, e.event_place, " +
                "e.masjid_id, m.name as masjid_name, e.event_details, e.organizer, e.contact, " +
                "e.attachments_path, e.is_public, e.created_at, e.updated_at " +
                "FROM events e " +
                "LEFT JOIN masjids m ON e.masjid_id = m.id " +
                "WHERE e.user_id = ? " +
                "ORDER BY e.start_date_time DESC, e.id DESC";
        List<Event> results = db.executeQuery(sql, new Object[] { userId }, this::mapResultSet);
        System.out.println("EventDAO.getAll(): Retrieved " + results.size() + " event records for user_id: " + userId);
        return results;
    }

    public List<Event> getAllWithFilters(String search, Long masjidId, LocalDateTime startDate,
            LocalDateTime endDate, Boolean isPublic) {
        String userId = getCurrentUserId();
        List<Object> params = new ArrayList<>();
        params.add(userId); // Add user_id first
        String sql = "SELECT e.id, e.user_id, e.event_name, e.start_date_time, e.end_date_time, e.event_place, " +
                "e.masjid_id, m.name as masjid_name, e.event_details, e.organizer, e.contact, " +
                "e.attachments_path, e.is_public, e.created_at, e.updated_at " +
                "FROM events e " +
                "LEFT JOIN masjids m ON e.masjid_id = m.id " +
                "WHERE e.user_id = ?";

        if (search != null && !search.isEmpty()) {
            sql += " AND (LOWER(e.event_name) LIKE ? OR LOWER(e.event_place) LIKE ? OR LOWER(e.organizer) LIKE ?)";
            String searchPattern = "%" + search.toLowerCase() + "%";
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
        }

        if (masjidId != null) {
            sql += " AND e.masjid_id = ?";
            params.add(masjidId);
        }

        if (startDate != null) {
            sql += " AND e.start_date_time >= ?";
            params.add(startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }

        if (endDate != null) {
            sql += " AND e.end_date_time <= ?";
            params.add(endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }

        if (isPublic != null) {
            sql += " AND e.is_public = ?";
            params.add(isPublic ? 1 : 0);
        }

        sql += " ORDER BY e.start_date_time DESC, e.id DESC";

        return db.executeQuery(sql, params.toArray(), this::mapResultSet);
    }

    public List<Event> getCalendarEvents(LocalDateTime startDate, LocalDateTime endDate) {
        String userId = getCurrentUserId();
        String sql = "SELECT e.id, e.user_id, e.event_name, e.start_date_time, e.end_date_time, e.event_place, " +
                "e.masjid_id, m.name as masjid_name, e.event_details, e.organizer, e.contact, " +
                "e.attachments_path, e.is_public, e.created_at, e.updated_at " +
                "FROM events e " +
                "LEFT JOIN masjids m ON e.masjid_id = m.id " +
                "WHERE e.user_id = ? AND e.start_date_time >= ? AND e.start_date_time <= ? " +
                "ORDER BY e.start_date_time ASC";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return db.executeQuery(sql, new Object[] {
                userId,
                startDate.format(formatter),
                endDate.format(formatter)
        }, this::mapResultSet);
    }

    public Event getById(Long id) {
        String userId = getCurrentUserId();
        String sql = "SELECT e.id, e.user_id, e.event_name, e.start_date_time, e.end_date_time, e.event_place, " +
                "e.masjid_id, m.name as masjid_name, e.event_details, e.organizer, e.contact, " +
                "e.attachments_path, e.is_public, e.created_at, e.updated_at " +
                "FROM events e " +
                "LEFT JOIN masjids m ON e.masjid_id = m.id " +
                "WHERE e.id = ? AND e.user_id = ?";
        var result = db.executeQuery(sql, new Object[] { id, userId }, this::mapResultSet);
        return result.isEmpty() ? null : result.get(0);
    }

    public Long create(Event event) {
        String userId = getCurrentUserId();
        String sql = "INSERT INTO events (user_id, event_name, start_date_time, end_date_time, event_place, " +
                "masjid_id, event_details, organizer, contact, attachments_path, is_public, " +
                "created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))";
        // Format LocalDateTime as string for SQLite TEXT storage
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        Object[] params = {
                userId, // CRITICAL: Set user_id for user isolation
                event.getEventName(),
                event.getStartDateTime() != null ? event.getStartDateTime().format(formatter) : null,
                event.getEndDateTime() != null ? event.getEndDateTime().format(formatter) : null,
                event.getEventPlace(),
                event.getMasjidId(),
                event.getEventDetails(),
                event.getOrganizer(),
                event.getContact(),
                event.getAttachmentsPath(),
                event.getIsPublic() != null && event.getIsPublic() ? 1 : 0
        };
        Long newId = db.executeInsert(sql, params);

        // Queue for sync if record was created successfully
        if (newId != null) {
            event.setId(newId);
            SyncHelper.queueInsert("events", newId, event);
        }

        return newId;
    }

    public boolean update(Event event) {
        String userId = getCurrentUserId();
        String sql = "UPDATE events SET event_name = ?, start_date_time = ?, end_date_time = ?, " +
                "event_place = ?, masjid_id = ?, event_details = ?, organizer = ?, contact = ?, " +
                "attachments_path = ?, is_public = ?, updated_at = datetime('now') WHERE id = ? AND user_id = ?";
        // Format LocalDateTime as string for SQLite TEXT storage
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        Object[] params = {
                event.getEventName(),
                event.getStartDateTime() != null ? event.getStartDateTime().format(formatter) : null,
                event.getEndDateTime() != null ? event.getEndDateTime().format(formatter) : null,
                event.getEventPlace(),
                event.getMasjidId(),
                event.getEventDetails(),
                event.getOrganizer(),
                event.getContact(),
                event.getAttachmentsPath(),
                event.getIsPublic() != null && event.getIsPublic() ? 1 : 0,
                event.getId(),
                userId // CRITICAL: User isolation check
        };
        boolean success = db.executeUpdate(sql, params) > 0;

        // Queue for sync if update was successful
        if (success) {
            SyncHelper.queueUpdate("events", event.getId(), event);
        }

        return success;
    }

    public boolean delete(Long id) {
        String userId = getCurrentUserId();
        String sql = "DELETE FROM events WHERE id = ? AND user_id = ?";
        boolean success = db.executeUpdate(sql, new Object[] { id, userId }) > 0;

        // Queue for sync if delete was successful
        if (success) {
            SyncHelper.queueDelete("events", id);
        }

        return success;
    }

    private Event mapResultSet(ResultSet rs) {
        try {
            Event e = new Event();
            e.setId(rs.getLong("id"));
            e.setEventName(rs.getString("event_name"));

            // Parse start_date_time from TEXT column
            // Since SQLite stores as TEXT, read as String and parse manually
            String startStr = rs.getString("start_date_time");
            if (startStr != null && !startStr.isEmpty()) {
                try {
                    // Format from Timestamp: "yyyy-MM-dd HH:mm:ss.SSSSSSSSS" or "yyyy-MM-dd
                    // HH:mm:ss"
                    String normalized = startStr.trim();
                    // Remove nanoseconds if present (keep only up to milliseconds)
                    if (normalized.contains(".")) {
                        int dotIndex = normalized.lastIndexOf(".");
                        String dateTimePart = normalized.substring(0, dotIndex);
                        // Parse as "yyyy-MM-dd HH:mm:ss"
                        e.setStartDateTime(LocalDateTime.parse(dateTimePart,
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    } else {
                        // No milliseconds/nanoseconds, parse directly
                        e.setStartDateTime(LocalDateTime.parse(normalized,
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    }
                } catch (Exception ex) {
                    // Fallback: try ISO format with space replaced by T
                    try {
                        e.setStartDateTime(LocalDateTime.parse(startStr.trim().replace(" ", "T").split("\\.")[0]));
                    } catch (Exception parseEx) {
                        System.err
                                .println("Failed to parse start_date_time: " + startStr + " - " + parseEx.getMessage());
                    }
                }
            }

            // Parse end_date_time from TEXT column
            String endStr = rs.getString("end_date_time");
            if (endStr != null && !endStr.isEmpty()) {
                try {
                    String normalized = endStr.trim();
                    if (normalized.contains(".")) {
                        int dotIndex = normalized.lastIndexOf(".");
                        String dateTimePart = normalized.substring(0, dotIndex);
                        e.setEndDateTime(LocalDateTime.parse(dateTimePart,
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    } else {
                        e.setEndDateTime(LocalDateTime.parse(normalized,
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    }
                } catch (Exception ex) {
                    try {
                        e.setEndDateTime(LocalDateTime.parse(endStr.trim().replace(" ", "T").split("\\.")[0]));
                    } catch (Exception parseEx) {
                        System.err.println("Failed to parse end_date_time: " + endStr + " - " + parseEx.getMessage());
                    }
                }
            }

            e.setEventPlace(rs.getString("event_place"));

            Long masjidId = rs.getLong("masjid_id");
            if (!rs.wasNull())
                e.setMasjidId(masjidId);

            e.setMasjidName(rs.getString("masjid_name"));
            e.setEventDetails(rs.getString("event_details"));
            e.setOrganizer(rs.getString("organizer"));
            e.setContact(rs.getString("contact"));
            e.setAttachmentsPath(rs.getString("attachments_path"));

            int isPublic = rs.getInt("is_public");
            e.setIsPublic(isPublic == 1);

            return e;
        } catch (SQLException ex) {
            System.err.println("Map event failed: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Insert or update a record coming from Supabase sync.
     * Used by SyncManager during pull sync.
     */
    public boolean upsertFromSupabase(Event event, String supabaseUpdatedAt) {
        if (event == null || event.getId() == null) {
            return false;
        }

        String userId = getCurrentUserId();

        // Check if record exists
        Event existing = getById(event.getId());

        // Format dates
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String startStr = event.getStartDateTime() != null ? event.getStartDateTime().format(formatter) : null;
        String endStr = event.getEndDateTime() != null ? event.getEndDateTime().format(formatter) : null;

        if (existing == null) {
            // INSERT (forcing the ID from Supabase)
            String sql = "INSERT INTO events (id, user_id, event_name, start_date_time, end_date_time, event_place, " +
                    "masjid_id, event_details, organizer, contact, attachments_path, is_public, " +
                    "created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), ?)";

            Object[] params = {
                    event.getId(),
                    userId,
                    event.getEventName(),
                    startStr,
                    endStr,
                    event.getEventPlace(),
                    event.getMasjidId(),
                    event.getEventDetails(),
                    event.getOrganizer(),
                    event.getContact(),
                    event.getAttachmentsPath(),
                    event.getIsPublic() != null && event.getIsPublic() ? 1 : 0,
                    supabaseUpdatedAt != null ? supabaseUpdatedAt : "datetime('now')"
            };

            try {
                int rows = db.executeUpdate(sql, params);
                return rows > 0;
            } catch (Exception e) {
                System.err.println("EventDAO.upsertFromSupabase (INSERT): " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        } else {
            // UPDATE
            String sql = "UPDATE events SET event_name = ?, start_date_time = ?, end_date_time = ?, " +
                    "event_place = ?, masjid_id = ?, event_details = ?, organizer = ?, contact = ?, " +
                    "attachments_path = ?, is_public = ?, updated_at = ? WHERE id = ? AND user_id = ?";

            Object[] params = {
                    event.getEventName(),
                    startStr,
                    endStr,
                    event.getEventPlace(),
                    event.getMasjidId(),
                    event.getEventDetails(),
                    event.getOrganizer(),
                    event.getContact(),
                    event.getAttachmentsPath(),
                    event.getIsPublic() != null && event.getIsPublic() ? 1 : 0,
                    supabaseUpdatedAt != null ? supabaseUpdatedAt : "datetime('now')",
                    event.getId(),
                    userId
            };

            try {
                int rows = db.executeUpdate(sql, params);
                return rows > 0;
            } catch (Exception e) {
                System.err.println("EventDAO.upsertFromSupabase (UPDATE): " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }
    }
}
