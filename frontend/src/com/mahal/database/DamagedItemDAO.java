package com.mahal.database;

import com.mahal.model.DamagedItem;
import com.mahal.sync.SyncHelper;
import com.mahal.util.SessionManager;
import com.mahal.model.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class DamagedItemDAO {
    private final DatabaseService db;

    public DamagedItemDAO() {
        this.db = DatabaseService.getInstance();
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS damaged_items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id TEXT NOT NULL, " + // CRITICAL: User isolation
                "inventory_item_id INTEGER, " +
                "quantity INTEGER, " +
                "damage_date TEXT, " +
                "reason TEXT, " +
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                ")";
        try {
            db.executeUpdate(sql, null);
            DAOBase.ensureUserIdColumn(db, "damaged_items");
            try {
                db.executeUpdate("CREATE INDEX IF NOT EXISTS idx_damaged_items_user_id ON damaged_items(user_id)",
                        null);
            } catch (Exception e) {
            }
        } catch (Exception e) {
            System.err.println("Error creating damaged_items table: " + e.getMessage());
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

    public List<DamagedItem> getAll() {
        String userId = getCurrentUserId();
        String sql = "SELECT d.id, d.user_id, d.inventory_item_id, i.item_name as inventory_item_name, " +
                "d.quantity, d.damage_date, d.reason, d.created_at, d.updated_at " +
                "FROM damaged_items d " +
                "LEFT JOIN inventory_items i ON d.inventory_item_id = i.id AND i.user_id = ? " +
                "WHERE d.user_id = ? " +
                "ORDER BY d.damage_date DESC, d.id DESC";
        List<DamagedItem> results = db.executeQuery(sql, new Object[] { userId, userId }, this::mapResultSet);
        System.out.println("DamagedItemDAO.getAll(): Retrieved " + results.size()
                + " damaged item records for user_id: " + userId);
        return results;
    }

    public Long create(DamagedItem damaged) {
        String userId = getCurrentUserId();
        String sql = "INSERT INTO damaged_items (user_id, inventory_item_id, quantity, damage_date, reason, " +
                "created_at, updated_at) VALUES (?, ?, ?, ?, ?, datetime('now'), datetime('now'))";
        Object[] params = {
                userId, // CRITICAL: Set user_id for user isolation
                damaged.getInventoryItemId(),
                damaged.getQuantity(),
                damaged.getDamageDate() != null ? Date.valueOf(damaged.getDamageDate()) : null,
                damaged.getReason()
        };
        Long id = db.executeInsert(sql, params);

        // Queue for sync if record was created successfully
        if (id != null) {
            damaged.setId(id);
            SyncHelper.queueInsert("damaged_items", id, damaged);
        }

        // Update inventory quantity
        if (id != null && damaged.getInventoryItemId() != null) {
            InventoryItemDAO itemDAO = new InventoryItemDAO();
            var item = itemDAO.getById(damaged.getInventoryItemId());
            if (item != null) {
                int currentQty = item.getQuantity() != null ? item.getQuantity() : 0;
                int damagedQty = damaged.getQuantity() != null ? damaged.getQuantity() : 0;
                item.setQuantity(Math.max(0, currentQty - damagedQty));
                itemDAO.update(item);
            }
        }

        return id;
    }

    public boolean update(DamagedItem damaged) {
        String userId = getCurrentUserId();
        String sql = "UPDATE damaged_items SET inventory_item_id = ?, quantity = ?, damage_date = ?, reason = ?, " +
                "updated_at = datetime('now') WHERE id = ? AND user_id = ?";
        Object[] params = {
                damaged.getInventoryItemId(),
                damaged.getQuantity(),
                damaged.getDamageDate() != null ? Date.valueOf(damaged.getDamageDate()) : null,
                damaged.getReason(),
                damaged.getId(),
                userId // CRITICAL: User isolation check
        };
        boolean success = db.executeUpdate(sql, params) > 0;

        // Queue for sync if update was successful
        if (success && damaged.getId() != null) {
            SyncHelper.queueUpdate("damaged_items", damaged.getId(), damaged);
        }

        return success;
    }

    public boolean delete(Long id) {
        String userId = getCurrentUserId();
        String sql = "DELETE FROM damaged_items WHERE id = ? AND user_id = ?";
        boolean success = db.executeUpdate(sql, new Object[] { id, userId }) > 0;

        // Queue for sync if delete was successful
        if (success && id != null) {
            SyncHelper.queueDelete("damaged_items", id);
        }

        return success;
    }

    private DamagedItem mapResultSet(ResultSet rs) {
        try {
            DamagedItem damaged = new DamagedItem();
            damaged.setId(rs.getLong("id"));

            Long itemId = rs.getLong("inventory_item_id");
            if (!rs.wasNull())
                damaged.setInventoryItemId(itemId);

            damaged.setInventoryItemName(rs.getString("inventory_item_name"));

            int qty = rs.getInt("quantity");
            if (!rs.wasNull())
                damaged.setQuantity(qty);

            // Parse damage_date as TEXT (SQLite stores dates as TEXT)
            String damageDateStr = rs.getString("damage_date");
            if (damageDateStr != null && !damageDateStr.isEmpty()) {
                try {
                    // First, try parsing as yyyy-MM-dd format (standard SQLite date format)
                    LocalDate damageDate = LocalDate.parse(damageDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    damaged.setDamageDate(damageDate);
                } catch (DateTimeParseException e) {
                    // If that fails, try parsing as timestamp (milliseconds since epoch)
                    try {
                        long timestamp = Long.parseLong(damageDateStr.trim());
                        LocalDate damageDate = new java.util.Date(timestamp).toInstant()
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate();
                        damaged.setDamageDate(damageDate);
                    } catch (NumberFormatException ex) {
                        // If timestamp parsing fails, try using java.sql.Date as fallback
                        try {
                            Date damageDate = rs.getDate("damage_date");
                            if (damageDate != null) {
                                damaged.setDamageDate(damageDate.toLocalDate());
                            }
                        } catch (SQLException sqlEx) {
                            // Ignore if date parsing fails completely
                            System.err.println("Could not parse damage_date: " + damageDateStr);
                        }
                    }
                }
            }

            damaged.setReason(rs.getString("reason"));
            return damaged;
        } catch (SQLException e) {
            System.err.println("Map damaged item failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Insert or update a record coming from Supabase sync.
     * Used by SyncManager during pull sync.
     */
    public boolean upsertFromSupabase(DamagedItem damaged, String supabaseUpdatedAt) {
        if (damaged == null || damaged.getId() == null) {
            return false;
        }

        String userId = getCurrentUserId();

        // Check if record exists
        boolean exists = false;
        String checkSql = "SELECT id FROM damaged_items WHERE id = ? AND user_id = ?";
        List<Object> checkResults = db.executeQuery(checkSql, new Object[] { damaged.getId(), userId }, rs -> {
            try {
                return rs.getObject("id");
            } catch (SQLException e) {
                return null;
            }
        });
        exists = !checkResults.isEmpty();

        // Format dates
        java.sql.Date damageDate = damaged.getDamageDate() != null ? java.sql.Date.valueOf(damaged.getDamageDate())
                : null;

        if (!exists) {
            // INSERT (forcing the ID from Supabase)
            String sql = "INSERT INTO damaged_items (id, user_id, inventory_item_id, quantity, damage_date, reason, " +
                    "created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, datetime('now'), ?)";

            Object[] params = {
                    damaged.getId(),
                    userId,
                    damaged.getInventoryItemId(),
                    damaged.getQuantity(),
                    damageDate,
                    damaged.getReason(),
                    supabaseUpdatedAt != null ? supabaseUpdatedAt : "datetime('now')"
            };

            try {
                int rows = db.executeUpdate(sql, params);
                return rows > 0;
            } catch (Exception e) {
                System.err.println("DamagedItemDAO.upsertFromSupabase (INSERT): " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        } else {
            // UPDATE
            String sql = "UPDATE damaged_items SET inventory_item_id = ?, quantity = ?, damage_date = ?, reason = ?, " +
                    "updated_at = ? WHERE id = ? AND user_id = ?";

            Object[] params = {
                    damaged.getInventoryItemId(),
                    damaged.getQuantity(),
                    damageDate,
                    damaged.getReason(),
                    supabaseUpdatedAt != null ? supabaseUpdatedAt : "datetime('now')",
                    damaged.getId(),
                    userId
            };

            try {
                int rows = db.executeUpdate(sql, params);
                return rows > 0;
            } catch (Exception e) {
                System.err.println("DamagedItemDAO.upsertFromSupabase (UPDATE): " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }
    }
}
