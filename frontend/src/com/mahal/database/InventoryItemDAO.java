package com.mahal.database;

import com.mahal.model.InventoryItem;
import com.mahal.sync.SyncHelper;
import com.mahal.util.SessionManager;
import com.mahal.model.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class InventoryItemDAO {
    private final DatabaseService db;

    public InventoryItemDAO() {
        this.db = DatabaseService.getInstance();
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS inventory_items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id TEXT NOT NULL, " + // CRITICAL: User isolation
                "item_name TEXT NOT NULL, " +
                "sku_code TEXT, " +
                "quantity INTEGER, " +
                "location TEXT, " +
                "purchase_date TEXT, " +
                "supplier TEXT, " +
                "value REAL, " +
                "notes TEXT, " +
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                ")";
        try {
            db.executeUpdate(sql, null);
            DAOBase.ensureUserIdColumn(db, "inventory_items");
            try {
                db.executeUpdate("CREATE INDEX IF NOT EXISTS idx_inventory_items_user_id ON inventory_items(user_id)",
                        null);
            } catch (Exception e) {
            }
        } catch (Exception e) {
            System.err.println("Error creating inventory_items table: " + e.getMessage());
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

    public List<InventoryItem> getAll() {
        String userId = getCurrentUserId();
        String sql = "SELECT id, user_id, item_name, sku_code, quantity, location, purchase_date, " +
                "supplier, value, notes, created_at, updated_at " +
                "FROM inventory_items WHERE user_id = ? ORDER BY item_name ASC, id DESC";
        List<InventoryItem> results = db.executeQuery(sql, new Object[] { userId }, this::mapResultSet);
        System.out.println("InventoryItemDAO.getAll(): Retrieved " + results.size()
                + " inventory item records for user_id: " + userId);
        return results;
    }

    public InventoryItem getById(Long id) {
        String userId = getCurrentUserId();
        String sql = "SELECT id, user_id, item_name, sku_code, quantity, location, purchase_date, " +
                "supplier, value, notes, created_at, updated_at " +
                "FROM inventory_items WHERE id = ? AND user_id = ?";
        var result = db.executeQuery(sql, new Object[] { id, userId }, this::mapResultSet);
        return result.isEmpty() ? null : result.get(0);
    }

    public Long create(InventoryItem item) {
        String userId = getCurrentUserId();
        String sql = "INSERT INTO inventory_items (user_id, item_name, sku_code, quantity, location, " +
                "purchase_date, supplier, value, notes, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))";
        Object[] params = {
                userId, // CRITICAL: Set user_id for user isolation
                item.getItemName(),
                item.getSkuCode(),
                item.getQuantity(),
                item.getLocation(),
                item.getPurchaseDate() != null ? Date.valueOf(item.getPurchaseDate()) : null,
                item.getSupplier(),
                item.getValue(),
                item.getNotes()
        };
        Long newId = db.executeInsert(sql, params);

        // Queue for sync if record was created successfully
        if (newId != null) {
            item.setId(newId);
            SyncHelper.queueInsert("inventory_items", newId, item);
        }

        return newId;
    }

    public boolean update(InventoryItem item) {
        String userId = getCurrentUserId();
        String sql = "UPDATE inventory_items SET item_name = ?, sku_code = ?, quantity = ?, " +
                "location = ?, purchase_date = ?, supplier = ?, value = ?, notes = ?, " +
                "updated_at = datetime('now') WHERE id = ? AND user_id = ?";
        Object[] params = {
                item.getItemName(),
                item.getSkuCode(),
                item.getQuantity(),
                item.getLocation(),
                item.getPurchaseDate() != null ? Date.valueOf(item.getPurchaseDate()) : null,
                item.getSupplier(),
                item.getValue(),
                item.getNotes(),
                item.getId(),
                userId // CRITICAL: User isolation check
        };
        boolean success = db.executeUpdate(sql, params) > 0;

        // Queue for sync if update was successful
        if (success && item.getId() != null) {
            SyncHelper.queueUpdate("inventory_items", item.getId(), item);
        }

        return success;
    }

    public boolean delete(Long id) {
        String userId = getCurrentUserId();
        String sql = "DELETE FROM inventory_items WHERE id = ? AND user_id = ?";
        boolean success = db.executeUpdate(sql, new Object[] { id, userId }) > 0;

        // Queue for sync if delete was successful
        if (success && id != null) {
            SyncHelper.queueDelete("inventory_items", id);
        }

        return success;
    }

    private InventoryItem mapResultSet(ResultSet rs) {
        try {
            InventoryItem item = new InventoryItem();
            item.setId(rs.getLong("id"));
            item.setItemName(rs.getString("item_name"));
            item.setSkuCode(rs.getString("sku_code"));

            int qty = rs.getInt("quantity");
            if (!rs.wasNull())
                item.setQuantity(qty);

            item.setLocation(rs.getString("location"));

            // Parse purchase_date as TEXT (SQLite stores dates as TEXT)
            // Parse purchase_date as TEXT (SQLite stores dates as TEXT)
            String purchaseDateStr = rs.getString("purchase_date");
            if (purchaseDateStr != null && !purchaseDateStr.isEmpty()) {
                try {
                    String cleanDateStr = purchaseDateStr.trim();
                    if (cleanDateStr.length() > 10) {
                        // Check if it's numeric (timestamp) or date string
                        if (!cleanDateStr.matches("\\d+")) {
                            cleanDateStr = cleanDateStr.substring(0, 10);
                        }
                    }

                    // First, try parsing as yyyy-MM-dd format (standard SQLite date format)
                    LocalDate purchaseDate = LocalDate.parse(cleanDateStr,
                            DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    item.setPurchaseDate(purchaseDate);
                } catch (DateTimeParseException e) {
                    // If that fails, try parsing as timestamp (milliseconds since epoch)
                    try {
                        long timestamp = Long.parseLong(purchaseDateStr.trim());
                        LocalDate purchaseDate = new java.util.Date(timestamp).toInstant()
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate();
                        item.setPurchaseDate(purchaseDate);
                    } catch (NumberFormatException ex) {
                        // If timestamp parsing fails, try using java.sql.Date as fallback
                        try {
                            Date purchaseDate = rs.getDate("purchase_date");
                            if (purchaseDate != null) {
                                item.setPurchaseDate(purchaseDate.toLocalDate());
                            }
                        } catch (SQLException sqlEx) {
                            System.err.println("InventoryItemDAO: Could not parse purchase_date: " + purchaseDateStr);
                        }
                    }
                }
            }

            item.setSupplier(rs.getString("supplier"));

            BigDecimal value = rs.getBigDecimal("value");
            if (value != null)
                item.setValue(value);

            item.setNotes(rs.getString("notes"));
            return item;
        } catch (SQLException e) {
            System.err.println("Map inventory item failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Insert or update a record coming from Supabase sync.
     * Used by SyncManager during pull sync.
     */
    public boolean upsertFromSupabase(InventoryItem item, String supabaseUpdatedAt) {
        if (item == null || item.getId() == null) {
            return false;
        }

        String userId = getCurrentUserId();

        // Check if record exists
        InventoryItem existing = getById(item.getId());

        // Format dates
        java.sql.Date purchaseDate = item.getPurchaseDate() != null ? java.sql.Date.valueOf(item.getPurchaseDate())
                : null;

        if (existing == null) {
            // INSERT (forcing the ID from Supabase)
            String sql = "INSERT INTO inventory_items (id, user_id, item_name, sku_code, quantity, location, " +
                    "purchase_date, supplier, value, notes, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), ?)";

            Object[] params = {
                    item.getId(),
                    userId,
                    item.getItemName(),
                    item.getSkuCode(),
                    item.getQuantity(),
                    item.getLocation(),
                    purchaseDate,
                    item.getSupplier(),
                    item.getValue(),
                    item.getNotes(),
                    supabaseUpdatedAt != null ? supabaseUpdatedAt : "datetime('now')"
            };

            try {
                int newId = db.executeUpdate(sql, params);
                return newId > 0;
            } catch (Exception e) {
                System.err.println("InventoryItemDAO.upsertFromSupabase (INSERT): " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        } else {
            // UPDATE
            String sql = "UPDATE inventory_items SET item_name = ?, sku_code = ?, quantity = ?, " +
                    "location = ?, purchase_date = ?, supplier = ?, value = ?, notes = ?, " +
                    "updated_at = ? WHERE id = ? AND user_id = ?";

            Object[] params = {
                    item.getItemName(),
                    item.getSkuCode(),
                    item.getQuantity(),
                    item.getLocation(),
                    purchaseDate,
                    item.getSupplier(),
                    item.getValue(),
                    item.getNotes(),
                    supabaseUpdatedAt != null ? supabaseUpdatedAt : "datetime('now')",
                    item.getId(),
                    userId
            };

            try {
                int rows = db.executeUpdate(sql, params);
                return rows > 0;
            } catch (Exception e) {
                System.err.println("InventoryItemDAO.upsertFromSupabase (UPDATE): " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }
    }
}
