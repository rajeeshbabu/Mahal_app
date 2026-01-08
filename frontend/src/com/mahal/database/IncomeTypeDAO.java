package com.mahal.database;

import com.mahal.model.IncomeType;
import com.mahal.sync.SyncHelper;
import com.mahal.util.SessionManager;
import com.mahal.model.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.util.List;

public class IncomeTypeDAO {
    private DatabaseService dbService;

    public IncomeTypeDAO() {
        this.dbService = DatabaseService.getInstance();
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS income_types (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id TEXT NOT NULL, " + // CRITICAL: User isolation
                "name TEXT NOT NULL, " +
                "type TEXT, " +
                "default_amount REAL, " +
                "description TEXT, " +
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                ")";
        try {
            dbService.executeUpdate(sql, null);
            DAOBase.ensureUserIdColumn(dbService, "income_types");
            try {
                dbService.executeUpdate("CREATE INDEX IF NOT EXISTS idx_income_types_user_id ON income_types(user_id)",
                        null);
            } catch (Exception e) {
            }
        } catch (Exception e) {
            System.err.println("Error creating income_types table: " + e.getMessage());
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

    public List<IncomeType> getAll() {
        String userId = getCurrentUserId();
        String sql = "SELECT id, user_id, name, type, default_amount, description, created_at, updated_at FROM income_types WHERE user_id = ? ORDER BY name";
        List<IncomeType> results = dbService.executeQuery(sql, new Object[] { userId }, this::mapResultSet);
        System.out.println(
                "IncomeTypeDAO.getAll(): Retrieved " + results.size() + " income type records for user_id: " + userId);
        return results;
    }

    public Long create(IncomeType incomeType) {
        String userId = getCurrentUserId();
        String sql = "INSERT INTO income_types (user_id, name, type, default_amount, description, created_at, updated_at) "
                +
                "VALUES (?, ?, ?, ?, ?, datetime('now'), datetime('now'))";
        Object[] params = {
                userId, // CRITICAL: Set user_id for user isolation
                incomeType.getName(),
                incomeType.getType(),
                incomeType.getDefaultAmount(),
                incomeType.getDescription()
        };
        Long newId = dbService.executeInsert(sql, params);

        // Queue for sync if record was created successfully
        if (newId != null) {
            incomeType.setId(newId);
            SyncHelper.queueInsert("income_types", newId, incomeType);
        }

        return newId;
    }

    public boolean update(IncomeType incomeType) {
        String userId = getCurrentUserId();
        String sql = "UPDATE income_types SET name = ?, type = ?, default_amount = ?, description = ?, updated_at = datetime('now') WHERE id = ? AND user_id = ?";
        Object[] params = {
                incomeType.getName(),
                incomeType.getType(),
                incomeType.getDefaultAmount(),
                incomeType.getDescription(),
                incomeType.getId(),
                userId // CRITICAL: User isolation check
        };
        boolean success = dbService.executeUpdate(sql, params) > 0;

        // Queue for sync if update was successful
        if (success) {
            SyncHelper.queueUpdate("income_types", incomeType.getId(), incomeType);
        }

        return success;
    }

    public boolean delete(Long id) {
        String userId = getCurrentUserId();
        String sql = "DELETE FROM income_types WHERE id = ? AND user_id = ?";
        boolean success = dbService.executeUpdate(sql, new Object[] { id, userId }) > 0;

        // Queue for sync if delete was successful
        if (success) {
            SyncHelper.queueDelete("income_types", id);
        }

        return success;
    }

    private IncomeType mapResultSet(ResultSet rs) {
        try {
            IncomeType incomeType = new IncomeType();
            incomeType.setId(rs.getLong("id"));
            incomeType.setName(rs.getString("name"));
            incomeType.setType(rs.getString("type"));

            BigDecimal amount = rs.getBigDecimal("default_amount");
            if (amount != null)
                incomeType.setDefaultAmount(amount);

            incomeType.setDescription(rs.getString("description"));
            return incomeType;
        } catch (SQLException e) {
            System.err.println("Error mapping ResultSet: " + e.getMessage());
            return null;
        }
    }

    /**
     * Insert or update a record coming from Supabase sync.
     * Used by SyncManager during pull sync.
     */
    public boolean upsertFromSupabase(IncomeType incomeType, String supabaseUpdatedAt) {
        if (incomeType == null || incomeType.getId() == null) {
            return false;
        }

        String userId = getCurrentUserId();

        // Check if record exists
        boolean exists = false;
        String checkSql = "SELECT id FROM income_types WHERE id = ? AND user_id = ?";
        List<Object> checkResults = dbService.executeQuery(checkSql, new Object[] { incomeType.getId(), userId },
                rs -> {
                    try {
                        return rs.getObject("id");
                    } catch (SQLException e) {
                        return null;
                    }
                });
        exists = !checkResults.isEmpty();

        if (!exists) {
            // INSERT (forcing the ID from Supabase)
            String sql = "INSERT INTO income_types (id, user_id, name, type, default_amount, description, created_at, updated_at) "
                    +
                    "VALUES (?, ?, ?, ?, ?, ?, datetime('now'), ?)";

            Object[] params = {
                    incomeType.getId(),
                    userId,
                    incomeType.getName(),
                    incomeType.getType(),
                    incomeType.getDefaultAmount(),
                    incomeType.getDescription(),
                    supabaseUpdatedAt != null ? supabaseUpdatedAt : "datetime('now')"
            };

            try {
                int rows = dbService.executeUpdate(sql, params);
                return rows > 0;
            } catch (Exception e) {
                System.err.println("IncomeTypeDAO.upsertFromSupabase (INSERT): " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        } else {
            // UPDATE
            String sql = "UPDATE income_types SET name = ?, type = ?, default_amount = ?, description = ?, updated_at = ? "
                    +
                    "WHERE id = ? AND user_id = ?";

            Object[] params = {
                    incomeType.getName(),
                    incomeType.getType(),
                    incomeType.getDefaultAmount(),
                    incomeType.getDescription(),
                    supabaseUpdatedAt != null ? supabaseUpdatedAt : "datetime('now')",
                    incomeType.getId(),
                    userId
            };

            try {
                int rows = dbService.executeUpdate(sql, params);
                return rows > 0;
            } catch (Exception e) {
                System.err.println("IncomeTypeDAO.upsertFromSupabase (UPDATE): " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }
    }
}
