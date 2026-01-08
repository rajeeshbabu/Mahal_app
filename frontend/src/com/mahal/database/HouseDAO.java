package com.mahal.database;

import com.mahal.model.House;
import com.mahal.sync.SyncHelper;
import com.mahal.util.SessionManager;
import com.mahal.model.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class HouseDAO {
    private DatabaseService dbService;

    public HouseDAO() {
        this.dbService = DatabaseService.getInstance();
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS houses (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id TEXT NOT NULL, " + // CRITICAL: User isolation
                "address TEXT NOT NULL, " +
                "house_number TEXT, " +
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                ")";
        try {
            dbService.executeUpdate(sql, null);
            DAOBase.ensureUserIdColumn(dbService, "houses");
            try {
                dbService.executeUpdate("CREATE INDEX IF NOT EXISTS idx_houses_user_id ON houses(user_id)", null);
            } catch (Exception e) {
            }
        } catch (Exception e) {
            System.err.println("Error creating houses table: " + e.getMessage());
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

    public List<House> getAll() {
        String userId = getCurrentUserId();
        String sql = "SELECT id, user_id, address, house_number FROM houses WHERE user_id = ? ORDER BY address, house_number";
        List<House> results = dbService.executeQuery(sql, new Object[] { userId }, this::mapResultSet);
        System.out.println("HouseDAO.getAll(): Retrieved " + results.size() + " house records for user_id: " + userId);
        return results;
    }

    public List<House> searchByAddress(String searchTerm) {
        String userId = getCurrentUserId();
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAll();
        }
        String sql = "SELECT id, user_id, address, house_number FROM houses " +
                "WHERE user_id = ? AND (LOWER(address) LIKE LOWER(?) OR LOWER(house_number) LIKE LOWER(?)) " +
                "ORDER BY address, house_number";
        String searchPattern = "%" + searchTerm.trim() + "%";
        return dbService.executeQuery(sql, new Object[] { userId, searchPattern, searchPattern }, this::mapResultSet);
    }

    public House getById(Long id) {
        String userId = getCurrentUserId();
        String sql = "SELECT id, user_id, address, house_number FROM houses WHERE id = ? AND user_id = ?";
        List<House> results = dbService.executeQuery(sql, new Object[] { id, userId }, this::mapResultSet);
        return results.isEmpty() ? null : results.get(0);
    }

    public Long create(House house) {
        String userId = getCurrentUserId();
        String sql = "INSERT INTO houses (user_id, address, house_number, created_at, updated_at) " +
                "VALUES (?, ?, ?, datetime('now'), datetime('now'))";
        Object[] params = {
                userId, // CRITICAL: Set user_id for user isolation
                house.getAddress(),
                house.getHouseNumber()
        };
        Long newId = dbService.executeInsert(sql, params);

        // Queue for sync if record was created successfully
        if (newId != null) {
            house.setId(newId);
            SyncHelper.queueInsert("houses", newId, house);
        }

        return newId;
    }

    public boolean update(House house) {
        String userId = getCurrentUserId();
        String sql = "UPDATE houses SET address = ?, house_number = ?, updated_at = datetime('now') WHERE id = ? AND user_id = ?";
        Object[] params = {
                house.getAddress(),
                house.getHouseNumber(),
                house.getId(),
                userId // CRITICAL: User isolation check
        };
        boolean success = dbService.executeUpdate(sql, params) > 0;

        // Queue for sync if update was successful
        if (success) {
            SyncHelper.queueUpdate("houses", house.getId(), house);
        }

        return success;
    }

    public boolean delete(Long id) {
        String userId = getCurrentUserId();
        String sql = "DELETE FROM houses WHERE id = ? AND user_id = ?";
        boolean success = dbService.executeUpdate(sql, new Object[] { id, userId }) > 0;

        // Queue for sync if delete was successful
        if (success) {
            SyncHelper.queueDelete("houses", id);
        }

        return success;
    }

    private House mapResultSet(ResultSet rs) {
        try {
            House house = new House();
            house.setId(rs.getLong("id"));
            house.setAddress(rs.getString("address"));
            house.setHouseNumber(rs.getString("house_number"));
            return house;
        } catch (SQLException e) {
            System.err.println("Error mapping ResultSet: " + e.getMessage());
            return null;
        }
    }

    /**
     * Insert or update a record coming from Supabase sync.
     * Used by SyncManager during pull sync.
     */
    public boolean upsertFromSupabase(House house, String supabaseUpdatedAt) {
        if (house == null || house.getId() == null) {
            return false;
        }

        String userId = getCurrentUserId();

        // Check if record exists
        House existing = getById(house.getId());

        if (existing == null) {
            // INSERT (forcing the ID from Supabase)
            String sql = "INSERT INTO houses (id, user_id, address, house_number, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, datetime('now'), ?)";

            Object[] params = {
                    house.getId(),
                    userId,
                    house.getAddress(),
                    house.getHouseNumber(),
                    supabaseUpdatedAt != null ? supabaseUpdatedAt : "datetime('now')"
            };

            try {
                int rows = dbService.executeUpdate(sql, params);
                return rows > 0;
            } catch (Exception e) {
                System.err.println("HouseDAO.upsertFromSupabase (INSERT): " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        } else {
            // UPDATE
            String sql = "UPDATE houses SET address = ?, house_number = ?, updated_at = ? " +
                    "WHERE id = ? AND user_id = ?";

            Object[] params = {
                    house.getAddress(),
                    house.getHouseNumber(),
                    supabaseUpdatedAt != null ? supabaseUpdatedAt : "datetime('now')",
                    house.getId(),
                    userId
            };

            try {
                int rows = dbService.executeUpdate(sql, params);
                return rows > 0;
            } catch (Exception e) {
                System.err.println("HouseDAO.upsertFromSupabase (UPDATE): " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }
    }
}
