package com.mahal.database;

import com.mahal.model.DueType;
import com.mahal.sync.SyncHelper;
import com.mahal.util.SessionManager;
import com.mahal.model.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.util.List;

public class DueTypeDAO {
    private DatabaseService dbService;

    public DueTypeDAO() {
        this.dbService = DatabaseService.getInstance();
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS due_types (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id TEXT NOT NULL, " + // CRITICAL: User isolation
                "due_name TEXT NOT NULL, " +
                "frequency TEXT, " +
                "amount REAL, " +
                "description TEXT, " +
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                ")";
        try {
            dbService.executeUpdate(sql, null);
            DAOBase.ensureUserIdColumn(dbService, "due_types");
            try {
                dbService.executeUpdate("CREATE INDEX IF NOT EXISTS idx_due_types_user_id ON due_types(user_id)", null);
            } catch (Exception e) {
            }
        } catch (Exception e) {
            System.err.println("Error creating due_types table: " + e.getMessage());
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

    public List<DueType> getAll() {
        String userId = getCurrentUserId();
        String sql = "SELECT id, user_id, due_name, frequency, amount, description, created_at, updated_at FROM due_types WHERE user_id = ? ORDER BY due_name";
        List<DueType> results = dbService.executeQuery(sql, new Object[] { userId }, this::mapResultSet);
        System.out.println(
                "DueTypeDAO.getAll(): Retrieved " + results.size() + " due type records for user_id: " + userId);
        return results;
    }

    public Long create(DueType dueType) {
        String userId = getCurrentUserId();
        String sql = "INSERT INTO due_types (user_id, due_name, frequency, amount, description, created_at, updated_at) "
                +
                "VALUES (?, ?, ?, ?, ?, datetime('now'), datetime('now'))";
        Object[] params = {
                userId, // CRITICAL: Set user_id for user isolation
                dueType.getDueName(),
                dueType.getFrequency(),
                dueType.getAmount(),
                dueType.getDescription()
        };
        Long newId = dbService.executeInsert(sql, params);

        // Queue for sync if record was created successfully
        if (newId != null) {
            dueType.setId(newId);
            SyncHelper.queueInsert("due_types", newId, dueType);
        }

        return newId;
    }

    public boolean update(DueType dueType) {
        String userId = getCurrentUserId();
        String sql = "UPDATE due_types SET due_name = ?, frequency = ?, amount = ?, description = ?, updated_at = datetime('now') WHERE id = ? AND user_id = ?";
        Object[] params = {
                dueType.getDueName(),
                dueType.getFrequency(),
                dueType.getAmount(),
                dueType.getDescription(),
                dueType.getId(),
                userId // CRITICAL: User isolation check
        };
        boolean success = dbService.executeUpdate(sql, params) > 0;

        // Queue for sync if update was successful
        if (success) {
            SyncHelper.queueUpdate("due_types", dueType.getId(), dueType);
        }

        return success;
    }

    public boolean delete(Long id) {
        String userId = getCurrentUserId();
        String sql = "DELETE FROM due_types WHERE id = ? AND user_id = ?";
        boolean success = dbService.executeUpdate(sql, new Object[] { id, userId }) > 0;

        // Queue for sync if delete was successful
        if (success) {
            SyncHelper.queueDelete("due_types", id);
        }

        return success;
    }

    private DueType mapResultSet(ResultSet rs) {
        try {
            DueType dueType = new DueType();
            dueType.setId(rs.getLong("id"));
            dueType.setDueName(rs.getString("due_name"));
            dueType.setFrequency(rs.getString("frequency"));

            BigDecimal amount = rs.getBigDecimal("amount");
            if (amount != null)
                dueType.setAmount(amount);

            dueType.setDescription(rs.getString("description"));
            return dueType;
        } catch (SQLException e) {
            System.err.println("Error mapping ResultSet: " + e.getMessage());
            return null;
        }
    }

    /**
     * Insert or update a record coming from Supabase sync.
     * Used by SyncManager during pull sync.
     */
    public boolean upsertFromSupabase(DueType dueType, String supabaseUpdatedAt) {
        if (dueType == null || dueType.getId() == null) {
            return false;
        }

        String userId = getCurrentUserId();

        // Check if record exists
        boolean exists = false;
        String checkSql = "SELECT id FROM due_types WHERE id = ? AND user_id = ?";
        List<Object> checkResults = dbService.executeQuery(checkSql, new Object[] { dueType.getId(), userId }, rs -> {
            try {
                return rs.getObject("id");
            } catch (SQLException e) {
                return null;
            }
        });
        exists = !checkResults.isEmpty();

        if (!exists) {
            // INSERT (forcing the ID from Supabase)
            String sql = "INSERT INTO due_types (id, user_id, due_name, frequency, amount, description, created_at, updated_at) "
                    +
                    "VALUES (?, ?, ?, ?, ?, ?, datetime('now'), ?)";

            Object[] params = {
                    dueType.getId(),
                    userId,
                    dueType.getDueName(),
                    dueType.getFrequency(),
                    dueType.getAmount(),
                    dueType.getDescription(),
                    supabaseUpdatedAt != null ? supabaseUpdatedAt : "datetime('now')"
            };

            try {
                int rows = dbService.executeUpdate(sql, params);
                return rows > 0;
            } catch (Exception e) {
                System.err.println("DueTypeDAO.upsertFromSupabase (INSERT): " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        } else {
            // UPDATE
            String sql = "UPDATE due_types SET due_name = ?, frequency = ?, amount = ?, description = ?, updated_at = ? "
                    +
                    "WHERE id = ? AND user_id = ?";

            Object[] params = {
                    dueType.getDueName(),
                    dueType.getFrequency(),
                    dueType.getAmount(),
                    dueType.getDescription(),
                    supabaseUpdatedAt != null ? supabaseUpdatedAt : "datetime('now')",
                    dueType.getId(),
                    userId
            };

            try {
                int rows = dbService.executeUpdate(sql, params);
                return rows > 0;
            } catch (Exception e) {
                System.err.println("DueTypeDAO.upsertFromSupabase (UPDATE): " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }
    }
}
