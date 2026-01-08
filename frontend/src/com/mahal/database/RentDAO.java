package com.mahal.database;

import com.mahal.model.Rent;
import com.mahal.model.RentItem;
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

public class RentDAO {
    private final DatabaseService db;

    public RentDAO() {
        this.db = DatabaseService.getInstance();
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS rents (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id TEXT NOT NULL, " + // CRITICAL: User isolation
                "rent_item_id INTEGER, " +
                "renter_name TEXT, " +
                "renter_mobile TEXT, " +
                "rent_start_date TEXT, " +
                "rent_end_date TEXT, " +
                "amount REAL, " +
                "deposit REAL, " +
                "status TEXT DEFAULT 'BOOKED', " +
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                ")";
        try {
            db.executeUpdate(sql, null);
            DAOBase.ensureUserIdColumn(db, "rents");
            try {
                db.executeUpdate("CREATE INDEX IF NOT EXISTS idx_rents_user_id ON rents(user_id)", null);
            } catch (Exception e) {
            }
        } catch (Exception e) {
            System.err.println("Error creating rents table: " + e.getMessage());
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

    public List<Rent> getAll() {
        String userId = getCurrentUserId();
        String sql = "SELECT r.id, r.user_id, r.rent_item_id, ri.inventory_item_id, i.item_name as rent_item_name, " +
                "r.renter_name, r.renter_mobile, r.rent_start_date, r.rent_end_date, " +
                "r.amount, r.deposit, r.status, r.created_at, r.updated_at " +
                "FROM rents r " +
                "LEFT JOIN rent_items ri ON r.rent_item_id = ri.id AND ri.user_id = ? " +
                "LEFT JOIN inventory_items i ON ri.inventory_item_id = i.id AND i.user_id = ? " +
                "WHERE r.user_id = ? " +
                "ORDER BY r.rent_start_date DESC, r.id DESC";
        List<Rent> results = db.executeQuery(sql, new Object[] { userId, userId, userId }, this::mapResultSet);
        System.out.println("RentDAO.getAll(): Retrieved " + results.size() + " rent records for user_id: " + userId);
        return results;
    }

    public Rent getById(Long id) {
        String userId = getCurrentUserId();
        String sql = "SELECT r.id, r.user_id, r.rent_item_id, ri.inventory_item_id, i.item_name as rent_item_name, " +
                "r.renter_name, r.renter_mobile, r.rent_start_date, r.rent_end_date, " +
                "r.amount, r.deposit, r.status, r.created_at, r.updated_at " +
                "FROM rents r " +
                "LEFT JOIN rent_items ri ON r.rent_item_id = ri.id AND ri.user_id = ? " +
                "LEFT JOIN inventory_items i ON ri.inventory_item_id = i.id AND i.user_id = ? " +
                "WHERE r.id = ? AND r.user_id = ?";
        var result = db.executeQuery(sql, new Object[] { userId, userId, id, userId }, this::mapResultSet);
        return result.isEmpty() ? null : result.get(0);
    }

    public Long create(Rent rent) {
        String userId = getCurrentUserId();
        String sql = "INSERT INTO rents (user_id, rent_item_id, renter_name, renter_mobile, rent_start_date, " +
                "rent_end_date, amount, deposit, status, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))";
        Object[] params = {
                userId, // CRITICAL: Set user_id for user isolation
                rent.getRentItemId(),
                rent.getRenterName(),
                rent.getRenterMobile(),
                rent.getRentStartDate() != null ? Date.valueOf(rent.getRentStartDate()) : null,
                rent.getRentEndDate() != null ? Date.valueOf(rent.getRentEndDate()) : null,
                rent.getAmount(),
                rent.getDeposit(),
                rent.getStatus() != null ? rent.getStatus() : "BOOKED"
        };
        Long id = db.executeInsert(sql, params);

        // Adjust inventory and rent item availability
        if (id != null && "BOOKED".equals(rent.getStatus())) {
            adjustInventoryOnStatusChange(rent.getRentItemId(), null, "BOOKED");

            // Queue for sync if record was created successfully
            rent.setId(id);
            SyncHelper.queueInsert("rents", id, rent);
        } else if (id != null) {
            // Queue for sync if record was created successfully
            rent.setId(id);
            SyncHelper.queueInsert("rents", id, rent);
        }

        return id;
    }

    public boolean update(Rent rent) {
        // Get previous status
        Rent existing = getById(rent.getId());
        String previousStatus = existing != null ? existing.getStatus() : null;

        String userId = getCurrentUserId();
        String sql = "UPDATE rents SET rent_item_id = ?, renter_name = ?, renter_mobile = ?, " +
                "rent_start_date = ?, rent_end_date = ?, amount = ?, deposit = ?, status = ?, " +
                "updated_at = datetime('now') WHERE id = ? AND user_id = ?";
        Object[] params = {
                rent.getRentItemId(),
                rent.getRenterName(),
                rent.getRenterMobile(),
                rent.getRentStartDate() != null ? Date.valueOf(rent.getRentStartDate()) : null,
                rent.getRentEndDate() != null ? Date.valueOf(rent.getRentEndDate()) : null,
                rent.getAmount(),
                rent.getDeposit(),
                rent.getStatus() != null ? rent.getStatus() : "BOOKED",
                rent.getId(),
                userId // CRITICAL: User isolation check
        };
        boolean updated = db.executeUpdate(sql, params) > 0;

        // Adjust inventory on status change
        if (updated && previousStatus != null && !previousStatus.equals(rent.getStatus())) {
            adjustInventoryOnStatusChange(rent.getRentItemId(), previousStatus, rent.getStatus());
        }

        // Queue for sync if update was successful
        if (updated && rent.getId() != null) {
            SyncHelper.queueUpdate("rents", rent.getId(), rent);
        }

        return updated;
    }

    public boolean delete(Long id) {
        Rent rent = getById(id);
        if (rent != null && "BOOKED".equals(rent.getStatus())) {
            // Return item to inventory
            adjustInventoryOnStatusChange(rent.getRentItemId(), "BOOKED", "RETURNED");
        }
        String userId = getCurrentUserId();
        String sql = "DELETE FROM rents WHERE id = ? AND user_id = ?";
        boolean success = db.executeUpdate(sql, new Object[] { id, userId }) > 0;

        // Queue for sync if delete was successful
        if (success && id != null) {
            SyncHelper.queueDelete("rents", id);
        }

        return success;
    }

    private void adjustInventoryOnStatusChange(Long rentItemId, String previousStatus, String newStatus) {
        if (rentItemId == null)
            return;

        RentItemDAO rentItemDAO = new RentItemDAO();
        RentItem rentItem = rentItemDAO.getById(rentItemId);
        if (rentItem == null || rentItem.getInventoryItemId() == null)
            return;

        InventoryItemDAO itemDAO = new InventoryItemDAO();
        var inventoryItem = itemDAO.getById(rentItem.getInventoryItemId());
        if (inventoryItem == null)
            return;

        int qty = inventoryItem.getQuantity() != null ? inventoryItem.getQuantity() : 0;

        // If moving to BOOKED from a non-booked state, decrement qty and set
        // unavailable
        if ("BOOKED".equals(newStatus) &&
                (previousStatus == null || "RETURNED".equals(previousStatus))) {
            inventoryItem.setQuantity(Math.max(0, qty - 1));
            rentItem.setAvailable(false);
            itemDAO.update(inventoryItem);
            rentItemDAO.update(rentItem);
        }

        // If moving to RETURNED from BOOKED/OVERDUE, increment qty and set available
        if ("RETURNED".equals(newStatus) &&
                ("BOOKED".equals(previousStatus) || "OVERDUE".equals(previousStatus))) {
            inventoryItem.setQuantity(qty + 1);
            rentItem.setAvailable(true);
            itemDAO.update(inventoryItem);
            rentItemDAO.update(rentItem);
        }
    }

    private Rent mapResultSet(ResultSet rs) {
        try {
            Rent rent = new Rent();
            rent.setId(rs.getLong("id"));

            Long rentItemId = rs.getLong("rent_item_id");
            if (!rs.wasNull())
                rent.setRentItemId(rentItemId);

            rent.setRentItemName(rs.getString("rent_item_name"));
            rent.setRenterName(rs.getString("renter_name"));
            rent.setRenterMobile(rs.getString("renter_mobile"));

            // Parse rent_start_date as TEXT (SQLite stores dates as TEXT)
            // Parse rent_start_date as TEXT (SQLite stores dates as TEXT)
            String startDateStr = rs.getString("rent_start_date");
            if (startDateStr != null && !startDateStr.isEmpty()) {
                try {
                    String cleanStartStr = startDateStr.trim();
                    if (cleanStartStr.length() > 10) {
                        // Check if it's numeric (timestamp) or date string
                        if (!cleanStartStr.matches("\\d+")) {
                            cleanStartStr = cleanStartStr.substring(0, 10);
                        }
                    }

                    // First, try parsing as yyyy-MM-dd format
                    LocalDate startDate = LocalDate.parse(cleanStartStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    rent.setRentStartDate(startDate);
                } catch (DateTimeParseException e) {
                    // If that fails, try parsing as timestamp (milliseconds since epoch)
                    try {
                        long timestamp = Long.parseLong(startDateStr.trim());
                        LocalDate startDate = new java.util.Date(timestamp).toInstant()
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate();
                        rent.setRentStartDate(startDate);
                    } catch (NumberFormatException ex) {
                        System.err.println("RentDAO: Could not parse rent_start_date: " + startDateStr);
                    }
                }
            }

            // Parse rent_end_date as TEXT (SQLite stores dates as TEXT)
            String endDateStr = rs.getString("rent_end_date");
            if (endDateStr != null && !endDateStr.isEmpty()) {
                try {
                    String cleanEndStr = endDateStr.trim();
                    if (cleanEndStr.length() > 10) {
                        // Check if it's numeric (timestamp) or date string
                        if (!cleanEndStr.matches("\\d+")) {
                            cleanEndStr = cleanEndStr.substring(0, 10);
                        }
                    }

                    // First, try parsing as yyyy-MM-dd format
                    LocalDate endDate = LocalDate.parse(cleanEndStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    rent.setRentEndDate(endDate);
                } catch (DateTimeParseException e) {
                    // If that fails, try parsing as timestamp (milliseconds since epoch)
                    try {
                        long timestamp = Long.parseLong(endDateStr.trim());
                        LocalDate endDate = new java.util.Date(timestamp).toInstant()
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate();
                        rent.setRentEndDate(endDate);
                    } catch (NumberFormatException ex) {
                        System.err.println("RentDAO: Could not parse rent_end_date: " + endDateStr);
                    }
                }
            }

            BigDecimal amount = rs.getBigDecimal("amount");
            if (amount != null)
                rent.setAmount(amount);

            BigDecimal deposit = rs.getBigDecimal("deposit");
            if (deposit != null)
                rent.setDeposit(deposit);

            String status = rs.getString("status");
            rent.setStatus(status != null ? status : "BOOKED");

            return rent;
        } catch (SQLException e) {
            System.err.println("Map rent failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Insert or update a record coming from Supabase sync.
     * Used by SyncManager during pull sync.
     */
    public boolean upsertFromSupabase(Rent rent, String supabaseUpdatedAt) {
        if (rent == null || rent.getId() == null) {
            return false;
        }

        String userId = getCurrentUserId();

        // Check if record exists
        Rent existing = getById(rent.getId());

        // Format dates
        java.sql.Date startDate = rent.getRentStartDate() != null ? java.sql.Date.valueOf(rent.getRentStartDate())
                : null;
        java.sql.Date endDate = rent.getRentEndDate() != null ? java.sql.Date.valueOf(rent.getRentEndDate()) : null;

        if (existing == null) {
            // INSERT (forcing the ID from Supabase)
            String sql = "INSERT INTO rents (id, user_id, rent_item_id, renter_name, renter_mobile, rent_start_date, " +
                    "rent_end_date, amount, deposit, status, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), ?)";

            Object[] params = {
                    rent.getId(),
                    userId,
                    rent.getRentItemId(),
                    rent.getRenterName(),
                    rent.getRenterMobile(),
                    startDate,
                    endDate,
                    rent.getAmount(),
                    rent.getDeposit(),
                    rent.getStatus() != null ? rent.getStatus() : "BOOKED",
                    supabaseUpdatedAt != null ? supabaseUpdatedAt : "datetime('now')"
            };

            try {
                // Determine if we need to adjust inventory
                // Ideally, sync logic shouldn't trigger side effects like inventory adjustment
                // automatically
                // because those side effects probably already happened on the source device.
                // However, to keep local state consistent, we might need to.
                // But for simplicity and to avoid double counting if logic is complex,
                // we might skip side effects or assume the synced data (RentItem availability)
                // is also being synced.
                // Since RentItem is also synced, its 'available' status will be updated by
                // RentItemDAO sync.
                // So we DO NOT call adjustInventoryOnStatusChange here.

                int rows = db.executeUpdate(sql, params);
                return rows > 0;
            } catch (Exception e) {
                System.err.println("RentDAO.upsertFromSupabase (INSERT): " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        } else {
            // UPDATE
            String sql = "UPDATE rents SET rent_item_id = ?, renter_name = ?, renter_mobile = ?, " +
                    "rent_start_date = ?, rent_end_date = ?, amount = ?, deposit = ?, status = ?, " +
                    "updated_at = ? WHERE id = ? AND user_id = ?";

            Object[] params = {
                    rent.getRentItemId(),
                    rent.getRenterName(),
                    rent.getRenterMobile(),
                    startDate,
                    endDate,
                    rent.getAmount(),
                    rent.getDeposit(),
                    rent.getStatus() != null ? rent.getStatus() : "BOOKED",
                    supabaseUpdatedAt != null ? supabaseUpdatedAt : "datetime('now')",
                    rent.getId(),
                    userId
            };

            try {
                int rows = db.executeUpdate(sql, params);
                return rows > 0;
            } catch (Exception e) {
                System.err.println("RentDAO.upsertFromSupabase (UPDATE): " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }
    }
}
