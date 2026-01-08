package com.mahal.database;

import com.mahal.model.RentItem;
import com.mahal.sync.SyncHelper;
import com.mahal.util.SessionManager;
import com.mahal.model.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.util.List;

public class RentItemDAO {
    private final DatabaseService db;

    public RentItemDAO() {
        this.db = DatabaseService.getInstance();
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS rent_items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id TEXT NOT NULL, " + // CRITICAL: User isolation
                "inventory_item_id INTEGER, " +
                "rate_per_day REAL, " +
                "deposit REAL, " +
                "available INTEGER DEFAULT 1, " +
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                ")";
        try {
            db.executeUpdate(sql, null);
            DAOBase.ensureUserIdColumn(db, "rent_items");
            try {
                db.executeUpdate("CREATE INDEX IF NOT EXISTS idx_rent_items_user_id ON rent_items(user_id)", null);
            } catch (Exception e) {
            }
        } catch (Exception e) {
            System.err.println("Error creating rent_items table: " + e.getMessage());
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

    public List<RentItem> getAll() {
        String userId = getCurrentUserId();
        String sql = "SELECT r.id, r.user_id, r.inventory_item_id, i.item_name as inventory_item_name, " +
                "r.rate_per_day, r.deposit, r.available, r.created_at, r.updated_at " +
                "FROM rent_items r " +
                "LEFT JOIN inventory_items i ON r.inventory_item_id = i.id AND i.user_id = ? " +
                "WHERE r.user_id = ? " +
                "ORDER BY i.item_name ASC, r.id DESC";
        List<RentItem> results = db.executeQuery(sql, new Object[] { userId, userId }, this::mapResultSet);
        System.out.println(
                "RentItemDAO.getAll(): Retrieved " + results.size() + " rent item records for user_id: " + userId);
        return results;
    }

    public RentItem getById(Long id) {
        String userId = getCurrentUserId();
        String sql = "SELECT r.id, r.user_id, r.inventory_item_id, i.item_name as inventory_item_name, " +
                "r.rate_per_day, r.deposit, r.available, r.created_at, r.updated_at " +
                "FROM rent_items r " +
                "LEFT JOIN inventory_items i ON r.inventory_item_id = i.id AND i.user_id = ? " +
                "WHERE r.id = ? AND r.user_id = ?";
        var result = db.executeQuery(sql, new Object[] { userId, id, userId }, this::mapResultSet);
        return result.isEmpty() ? null : result.get(0);
    }

    public Long create(RentItem rentItem) {
        String userId = getCurrentUserId();
        String sql = "INSERT INTO rent_items (user_id, inventory_item_id, rate_per_day, deposit, available, " +
                "created_at, updated_at) VALUES (?, ?, ?, ?, ?, datetime('now'), datetime('now'))";
        Object[] params = {
                userId, // CRITICAL: Set user_id for user isolation
                rentItem.getInventoryItemId(),
                rentItem.getRatePerDay(),
                rentItem.getDeposit(),
                rentItem.getAvailable() != null && rentItem.getAvailable() ? 1 : 0
        };
        Long newId = db.executeInsert(sql, params);

        // Queue for sync if record was created successfully
        if (newId != null) {
            rentItem.setId(newId);
            SyncHelper.queueInsert("rent_items", newId, rentItem);
        }

        return newId;
    }

    public boolean update(RentItem rentItem) {
        String userId = getCurrentUserId();
        String sql = "UPDATE rent_items SET inventory_item_id = ?, rate_per_day = ?, deposit = ?, " +
                "available = ?, updated_at = datetime('now') WHERE id = ? AND user_id = ?";
        Object[] params = {
                rentItem.getInventoryItemId(),
                rentItem.getRatePerDay(),
                rentItem.getDeposit(),
                rentItem.getAvailable() != null && rentItem.getAvailable() ? 1 : 0,
                rentItem.getId(),
                userId // CRITICAL: User isolation check
        };
        boolean success = db.executeUpdate(sql, params) > 0;

        // Queue for sync if update was successful
        if (success && rentItem.getId() != null) {
            SyncHelper.queueUpdate("rent_items", rentItem.getId(), rentItem);
        }

        return success;
    }

    public boolean delete(Long id) {
        String userId = getCurrentUserId();
        String sql = "DELETE FROM rent_items WHERE id = ? AND user_id = ?";
        boolean success = db.executeUpdate(sql, new Object[] { id, userId }) > 0;

        // Queue for sync if delete was successful
        if (success && id != null) {
            SyncHelper.queueDelete("rent_items", id);
        }

        return success;
    }

    private RentItem mapResultSet(ResultSet rs) {
        try {
            RentItem rentItem = new RentItem();
            rentItem.setId(rs.getLong("id"));

            Long itemId = rs.getLong("inventory_item_id");
            if (!rs.wasNull())
                rentItem.setInventoryItemId(itemId);

            rentItem.setInventoryItemName(rs.getString("inventory_item_name"));

            BigDecimal rate = rs.getBigDecimal("rate_per_day");
            if (rate != null)
                rentItem.setRatePerDay(rate);

            BigDecimal deposit = rs.getBigDecimal("deposit");
            if (deposit != null)
                rentItem.setDeposit(deposit);

            int available = rs.getInt("available");
            rentItem.setAvailable(available == 1);

            return rentItem;
        } catch (SQLException e) {
            System.err.println("Map rent item failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Insert or update a record coming from Supabase sync.
     * Used by SyncManager during pull sync.
     */
    public boolean upsertFromSupabase(RentItem rentItem, String supabaseUpdatedAt) {
        if (rentItem == null || rentItem.getId() == null) {
            return false;
        }

        String userId = getCurrentUserId();

        // Check if record exists
        RentItem existing = getById(rentItem.getId());

        if (existing == null) {
            // INSERT (forcing the ID from Supabase)
            String sql = "INSERT INTO rent_items (id, user_id, inventory_item_id, rate_per_day, deposit, available, " +
                    "created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, datetime('now'), ?)";

            Object[] params = {
                    rentItem.getId(),
                    userId,
                    rentItem.getInventoryItemId(),
                    rentItem.getRatePerDay(),
                    rentItem.getDeposit(),
                    rentItem.getAvailable() != null && rentItem.getAvailable() ? 1 : 0,
                    supabaseUpdatedAt != null ? supabaseUpdatedAt : "datetime('now')"
            };

            try {
                int rows = db.executeUpdate(sql, params);
                return rows > 0;
            } catch (Exception e) {
                System.err.println("RentItemDAO.upsertFromSupabase (INSERT): " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        } else {
            // UPDATE
            String sql = "UPDATE rent_items SET inventory_item_id = ?, rate_per_day = ?, deposit = ?, " +
                    "available = ?, updated_at = ? WHERE id = ? AND user_id = ?";

            Object[] params = {
                    rentItem.getInventoryItemId(),
                    rentItem.getRatePerDay(),
                    rentItem.getDeposit(),
                    rentItem.getAvailable() != null && rentItem.getAvailable() ? 1 : 0,
                    supabaseUpdatedAt != null ? supabaseUpdatedAt : "datetime('now')",
                    rentItem.getId(),
                    userId
            };

            try {
                int rows = db.executeUpdate(sql, params);
                return rows > 0;
            } catch (Exception e) {
                System.err.println("RentItemDAO.upsertFromSupabase (UPDATE): " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }
    }
}
