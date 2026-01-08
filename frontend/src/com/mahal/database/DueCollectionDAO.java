package com.mahal.database;

import com.mahal.model.DueCollection;
import com.mahal.sync.SyncHelper;
import com.mahal.util.SessionManager;
import com.mahal.model.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.util.List;

public class DueCollectionDAO {
    private DatabaseService dbService;

    public DueCollectionDAO() {
        this.dbService = DatabaseService.getInstance();
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS due_collections (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id TEXT NOT NULL, " + // CRITICAL: User isolation
                "masjid_id INTEGER, " +
                "member_id INTEGER, " +
                "due_type_id INTEGER, " +
                "amount REAL, " +
                "date TEXT, " +
                "payment_mode TEXT, " +
                "receipt_no TEXT, " +
                "remarks TEXT, " +
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                ")";
        try {
            dbService.executeUpdate(sql, null);
            DAOBase.ensureUserIdColumn(dbService, "due_collections");
            try {
                dbService.executeUpdate(
                        "CREATE INDEX IF NOT EXISTS idx_due_collections_user_id ON due_collections(user_id)", null);
            } catch (Exception e) {
            }
        } catch (Exception e) {
            System.err.println("Error creating due_collections table: " + e.getMessage());
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

    public List<DueCollection> getAll() {
        String userId = getCurrentUserId();
        String sql = "SELECT dc.id, dc.user_id, dc.masjid_id, m.name as masjid_name, dc.member_id, mem.name as member_name, "
                +
                "mem.address as address, dc.due_type_id, dt.due_name as due_type_name, dc.amount, dc.date, " +
                "dc.payment_mode, dc.receipt_no, dc.remarks, dc.created_at, dc.updated_at " +
                "FROM due_collections dc " +
                "LEFT JOIN masjids m ON dc.masjid_id = m.id " +
                "LEFT JOIN members mem ON dc.member_id = mem.id " +
                "LEFT JOIN due_types dt ON dc.due_type_id = dt.id " +
                "WHERE dc.user_id = ? " +
                "ORDER BY dc.date DESC";
        List<DueCollection> results = dbService.executeQuery(sql, new Object[] { userId }, this::mapResultSet);
        System.out.println("DueCollectionDAO.getAll(): Retrieved " + results.size()
                + " collection records for user_id: " + userId);
        return results;
    }

    public Long create(DueCollection collection) {
        String userId = getCurrentUserId();
        String sql = "INSERT INTO due_collections (user_id, masjid_id, member_id, due_type_id, amount, date, payment_mode, receipt_no, remarks, created_at, updated_at) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))";
        // Convert LocalDate to string format YYYY-MM-DD for SQLite TEXT field
        String dateStr = collection.getDate() != null ? collection.getDate().toString() : null;
        Object[] params = {
                userId, // CRITICAL: Set user_id for user isolation
                collection.getMasjidId(),
                collection.getMemberId(),
                collection.getDueTypeId(),
                collection.getAmount(),
                dateStr,
                collection.getPaymentMode(),
                collection.getReceiptNo(),
                collection.getRemarks()
        };
        Long newId = dbService.executeInsert(sql, params);

        // Queue for sync if record was created successfully
        if (newId != null) {
            collection.setId(newId);
            SyncHelper.queueInsert("due_collections", newId, collection);
        }

        return newId;
    }

    public boolean update(DueCollection collection) {
        String userId = getCurrentUserId();
        String sql = "UPDATE due_collections SET masjid_id = ?, member_id = ?, due_type_id = ?, amount = ?, date = ?, "
                +
                "payment_mode = ?, receipt_no = ?, remarks = ?, updated_at = datetime('now') " +
                "WHERE id = ? AND user_id = ?";
        // Convert LocalDate to string format YYYY-MM-DD for SQLite TEXT field
        String dateStr = collection.getDate() != null ? collection.getDate().toString() : null;
        Object[] params = {
                collection.getMasjidId(),
                collection.getMemberId(),
                collection.getDueTypeId(),
                collection.getAmount(),
                dateStr,
                collection.getPaymentMode(),
                collection.getReceiptNo(),
                collection.getRemarks(),
                collection.getId(),
                userId // CRITICAL: User isolation check
        };
        boolean success = dbService.executeUpdate(sql, params) > 0;

        // Queue for sync if update was successful
        if (success && collection.getId() != null) {
            SyncHelper.queueUpdate("due_collections", collection.getId(), collection);
        }

        return success;
    }

    public boolean delete(Long id) {
        String userId = getCurrentUserId();
        String sql = "DELETE FROM due_collections WHERE id = ? AND user_id = ?";
        boolean success = dbService.executeUpdate(sql, new Object[] { id, userId }) > 0;

        // Queue for sync if delete was successful
        if (success && id != null) {
            SyncHelper.queueDelete("due_collections", id);
        }

        return success;
    }

    private DueCollection mapResultSet(ResultSet rs) {
        try {
            DueCollection collection = new DueCollection();
            collection.setId(rs.getLong("id"));

            Long masjidId = rs.getLong("masjid_id");
            if (!rs.wasNull())
                collection.setMasjidId(masjidId);
            collection.setMasjidName(rs.getString("masjid_name"));

            Long memberId = rs.getLong("member_id");
            if (!rs.wasNull())
                collection.setMemberId(memberId);
            collection.setMemberName(rs.getString("member_name"));
            String address = rs.getString("address");
            if (address != null) {
                collection.setAddress(address);
            }

            collection.setDueTypeId(rs.getLong("due_type_id"));
            collection.setDueTypeName(rs.getString("due_type_name"));

            BigDecimal amount = rs.getBigDecimal("amount");
            if (amount != null)
                collection.setAmount(amount);

            // Handle date from SQLite TEXT field - read as string and parse
            try {
                String dateStr = rs.getString("date");
                if (dateStr != null && !dateStr.isEmpty() && !rs.wasNull()) {
                    try {
                        // Handle potential timestamp format (YYYY-MM-DD HH:MM:SS) by taking first 10
                        // chars
                        String cleanDateStr = dateStr;
                        if (dateStr.length() > 10) {
                            cleanDateStr = dateStr.substring(0, 10);
                        }

                        // Try parsing as ISO date format (YYYY-MM-DD)
                        collection.setDate(java.time.LocalDate.parse(cleanDateStr));
                    } catch (java.time.format.DateTimeParseException e) {
                        System.err
                                .println("DueCollectionDAO: Date parse error for '" + dateStr + "': " + e.getMessage());
                    }
                }
            } catch (SQLException dateEx) {
                System.err.println("Error reading date: " + dateEx.getMessage());
            }

            String paymentMode = rs.getString("payment_mode");
            if (paymentMode != null && !rs.wasNull()) {
                collection.setPaymentMode(paymentMode);
            }

            String receiptNo = rs.getString("receipt_no");
            if (receiptNo != null && !rs.wasNull()) {
                collection.setReceiptNo(receiptNo);
            }

            String remarks = rs.getString("remarks");
            if (remarks != null && !rs.wasNull()) {
                collection.setRemarks(remarks);
            }

            return collection;
        } catch (SQLException e) {
            System.err.println("Error mapping ResultSet to DueCollection: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Insert or update a record coming from Supabase sync.
     * Used by SyncManager during pull sync.
     */
    public boolean upsertFromSupabase(DueCollection collection, String supabaseUpdatedAt) {
        if (collection == null || collection.getId() == null) {
            return false;
        }

        String userId = getCurrentUserId();

        // Check if record exists
        boolean exists = false;
        String checkSql = "SELECT id FROM due_collections WHERE id = ? AND user_id = ?";
        List<Object> checkResults = dbService.executeQuery(checkSql, new Object[] { collection.getId(), userId },
                rs -> {
                    try {
                        return rs.getObject("id");
                    } catch (SQLException e) {
                        return null;
                    }
                });
        exists = !checkResults.isEmpty();

        // Format date
        String dateStr = collection.getDate() != null ? collection.getDate().toString() : null;

        if (!exists) {
            // INSERT (forcing the ID from Supabase)
            String sql = "INSERT INTO due_collections (id, user_id, masjid_id, member_id, due_type_id, amount, date, payment_mode, receipt_no, remarks, created_at, updated_at) "
                    +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), ?)";

            Object[] params = {
                    collection.getId(),
                    userId,
                    collection.getMasjidId(),
                    collection.getMemberId(),
                    collection.getDueTypeId(),
                    collection.getAmount(),
                    dateStr,
                    collection.getPaymentMode(),
                    collection.getReceiptNo(),
                    collection.getRemarks(),
                    supabaseUpdatedAt != null ? supabaseUpdatedAt : "datetime('now')"
            };

            try {
                int rows = dbService.executeUpdate(sql, params);
                return rows > 0;
            } catch (Exception e) {
                System.err.println("DueCollectionDAO.upsertFromSupabase (INSERT): " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        } else {
            // UPDATE
            String sql = "UPDATE due_collections SET masjid_id = ?, member_id = ?, due_type_id = ?, amount = ?, date = ?, "
                    +
                    "payment_mode = ?, receipt_no = ?, remarks = ?, updated_at = ? " +
                    "WHERE id = ? AND user_id = ?";

            Object[] params = {
                    collection.getMasjidId(),
                    collection.getMemberId(),
                    collection.getDueTypeId(),
                    collection.getAmount(),
                    dateStr,
                    collection.getPaymentMode(),
                    collection.getReceiptNo(),
                    collection.getRemarks(),
                    supabaseUpdatedAt != null ? supabaseUpdatedAt : "datetime('now')",
                    collection.getId(),
                    userId
            };

            try {
                int rows = dbService.executeUpdate(sql, params);
                return rows > 0;
            } catch (Exception e) {
                System.err.println("DueCollectionDAO.upsertFromSupabase (UPDATE): " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }
    }
}
