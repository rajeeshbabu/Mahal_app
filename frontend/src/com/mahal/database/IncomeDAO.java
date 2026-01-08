package com.mahal.database;

import com.mahal.model.Income;
import com.mahal.sync.SyncHelper;
import com.mahal.util.SessionManager;
import com.mahal.model.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class IncomeDAO {
    private DatabaseService dbService;

    public IncomeDAO() {
        this.dbService = DatabaseService.getInstance();
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS incomes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id TEXT NOT NULL, " + // CRITICAL: User isolation
                "masjid_id INTEGER, " +
                "member_id INTEGER, " +
                "income_type_id INTEGER, " +
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
            DAOBase.ensureUserIdColumn(dbService, "incomes");
            try {
                dbService.executeUpdate("CREATE INDEX IF NOT EXISTS idx_incomes_user_id ON incomes(user_id)", null);
            } catch (Exception e) {
            }
        } catch (Exception e) {
            System.err.println("Error creating incomes table: " + e.getMessage());
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

    public List<Income> getAll() {
        String userId = getCurrentUserId();
        String sql = "SELECT i.id, i.user_id, i.masjid_id, m.name as masjid_name, i.member_id, mem.name as member_name, "
                +
                "i.income_type_id, it.name as income_type_name, i.amount, i.date, i.payment_mode, " +
                "i.receipt_no, i.remarks, i.created_at, i.updated_at " +
                "FROM incomes i " +
                "LEFT JOIN masjids m ON i.masjid_id = m.id " +
                "LEFT JOIN members mem ON i.member_id = mem.id " +
                "LEFT JOIN income_types it ON i.income_type_id = it.id " +
                "WHERE i.user_id = ? " +
                "ORDER BY i.date DESC";
        List<Income> results = dbService.executeQuery(sql, new Object[] { userId }, this::mapResultSet);
        System.out
                .println("IncomeDAO.getAll(): Retrieved " + results.size() + " income records for user_id: " + userId);
        return results;
    }

    public List<Income> getByDateRange(LocalDate startDate, LocalDate endDate) {
        String userId = getCurrentUserId();
        String sql = "SELECT i.id, i.user_id, i.masjid_id, m.name as masjid_name, i.member_id, mem.name as member_name, "
                +
                "i.income_type_id, it.name as income_type_name, i.amount, i.date, i.payment_mode, " +
                "i.receipt_no, i.remarks, i.created_at, i.updated_at " +
                "FROM incomes i " +
                "LEFT JOIN masjids m ON i.masjid_id = m.id " +
                "LEFT JOIN members mem ON i.member_id = mem.id " +
                "LEFT JOIN income_types it ON i.income_type_id = it.id " +
                "WHERE i.user_id = ? AND i.date BETWEEN ? AND ? " +
                "ORDER BY i.date DESC";
        // Convert LocalDate to string format YYYY-MM-DD for SQLite TEXT field
        String startDateStr = startDate != null ? startDate.toString() : null;
        String endDateStr = endDate != null ? endDate.toString() : null;
        return dbService.executeQuery(sql, new Object[] { userId, startDateStr, endDateStr }, this::mapResultSet);
    }

    public Long create(Income income) {
        String userId = getCurrentUserId();
        String sql = "INSERT INTO incomes (user_id, masjid_id, member_id, income_type_id, amount, date, payment_mode, receipt_no, remarks, created_at, updated_at) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))";
        // Convert LocalDate to string format YYYY-MM-DD for SQLite TEXT field
        String dateStr = income.getDate() != null ? income.getDate().toString() : null;
        Object[] params = {
                userId, // CRITICAL: Set user_id for user isolation
                income.getMasjidId(),
                income.getMemberId(),
                income.getIncomeTypeId(),
                income.getAmount(),
                dateStr,
                income.getPaymentMode(),
                income.getReceiptNo(),
                income.getRemarks()
        };
        Long newId = dbService.executeInsert(sql, params);

        // Queue for sync if record was created successfully
        if (newId != null) {
            income.setId(newId);
            SyncHelper.queueInsert("incomes", newId, income);
        }

        return newId;
    }

    public boolean update(Income income) {
        String userId = getCurrentUserId();
        String sql = "UPDATE incomes SET masjid_id = ?, member_id = ?, income_type_id = ?, amount = ?, date = ?, " +
                "payment_mode = ?, receipt_no = ?, remarks = ?, updated_at = datetime('now') " +
                "WHERE id = ? AND user_id = ?";
        // Convert LocalDate to string format YYYY-MM-DD for SQLite TEXT field
        String dateStr = income.getDate() != null ? income.getDate().toString() : null;
        Object[] params = {
                income.getMasjidId(),
                income.getMemberId(),
                income.getIncomeTypeId(),
                income.getAmount(),
                dateStr,
                income.getPaymentMode(),
                income.getReceiptNo(),
                income.getRemarks(),
                income.getId(),
                userId // CRITICAL: User isolation check
        };
        boolean success = dbService.executeUpdate(sql, params) > 0;

        // Queue for sync if update was successful
        if (success && income.getId() != null) {
            SyncHelper.queueUpdate("incomes", income.getId(), income);
        }

        return success;
    }

    public boolean delete(Long id) {
        String userId = getCurrentUserId();
        String sql = "DELETE FROM incomes WHERE id = ? AND user_id = ?";
        boolean success = dbService.executeUpdate(sql, new Object[] { id, userId }) > 0;

        // Queue for sync if delete was successful
        if (success && id != null) {
            SyncHelper.queueDelete("incomes", id);
        }

        return success;
    }

    private Income mapResultSet(ResultSet rs) {
        try {
            Income income = new Income();
            income.setId(rs.getLong("id"));

            Long masjidId = rs.getLong("masjid_id");
            if (!rs.wasNull())
                income.setMasjidId(masjidId);
            String masjidName = rs.getString("masjid_name");
            if (masjidName != null && !rs.wasNull()) {
                income.setMasjidName(masjidName);
            }

            Long memberId = rs.getLong("member_id");
            if (!rs.wasNull())
                income.setMemberId(memberId);
            String memberName = rs.getString("member_name");
            if (memberName != null && !rs.wasNull()) {
                income.setMemberName(memberName);
            }

            Long incomeTypeId = rs.getLong("income_type_id");
            if (!rs.wasNull())
                income.setIncomeTypeId(incomeTypeId);
            String incomeTypeName = rs.getString("income_type_name");
            if (incomeTypeName != null && !rs.wasNull()) {
                income.setIncomeTypeName(incomeTypeName);
            }

            BigDecimal amount = rs.getBigDecimal("amount");
            if (amount != null)
                income.setAmount(amount);

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
                        income.setDate(java.time.LocalDate.parse(cleanDateStr));
                    } catch (java.time.format.DateTimeParseException e) {
                        System.err.println("IncomeDAO: Date parse error for '" + dateStr + "': " + e.getMessage());
                    }
                }
            } catch (SQLException dateEx) {
                System.err.println("Error reading date: " + dateEx.getMessage());
            }

            String paymentMode = rs.getString("payment_mode");
            if (paymentMode != null && !rs.wasNull()) {
                income.setPaymentMode(paymentMode);
            }

            String receiptNo = rs.getString("receipt_no");
            if (receiptNo != null && !rs.wasNull()) {
                income.setReceiptNo(receiptNo);
            }

            String remarks = rs.getString("remarks");
            if (remarks != null && !rs.wasNull()) {
                income.setRemarks(remarks);
            }

            return income;
        } catch (SQLException e) {
            System.err.println("Error mapping ResultSet to Income: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Insert or update a record coming from Supabase sync.
     * Used by SyncManager during pull sync.
     */
    public boolean upsertFromSupabase(Income income, String supabaseUpdatedAt) {
        if (income == null || income.getId() == null) {
            return false;
        }

        String userId = getCurrentUserId();

        // Check if record exists
        boolean exists = false;
        String checkSql = "SELECT id FROM incomes WHERE id = ? AND user_id = ?";
        List<Object> checkResults = dbService.executeQuery(checkSql, new Object[] { income.getId(), userId }, rs -> {
            try {
                return rs.getObject("id");
            } catch (SQLException e) {
                return null;
            }
        });
        exists = !checkResults.isEmpty();

        // Format date
        String dateStr = income.getDate() != null ? income.getDate().toString() : null;

        if (!exists) {
            // INSERT (forcing the ID from Supabase)
            String sql = "INSERT INTO incomes (id, user_id, masjid_id, member_id, income_type_id, amount, date, payment_mode, receipt_no, remarks, created_at, updated_at) "
                    +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), ?)";

            Object[] params = {
                    income.getId(),
                    userId,
                    income.getMasjidId(),
                    income.getMemberId(),
                    income.getIncomeTypeId(),
                    income.getAmount(),
                    dateStr,
                    income.getPaymentMode(),
                    income.getReceiptNo(),
                    income.getRemarks(),
                    supabaseUpdatedAt != null ? supabaseUpdatedAt : "datetime('now')"
            };

            try {
                int rows = dbService.executeUpdate(sql, params);
                return rows > 0;
            } catch (Exception e) {
                System.err.println("IncomeDAO.upsertFromSupabase (INSERT): " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        } else {
            // UPDATE
            String sql = "UPDATE incomes SET masjid_id = ?, member_id = ?, income_type_id = ?, amount = ?, date = ?, " +
                    "payment_mode = ?, receipt_no = ?, remarks = ?, updated_at = ? " +
                    "WHERE id = ? AND user_id = ?";

            Object[] params = {
                    income.getMasjidId(),
                    income.getMemberId(),
                    income.getIncomeTypeId(),
                    income.getAmount(),
                    dateStr,
                    income.getPaymentMode(),
                    income.getReceiptNo(),
                    income.getRemarks(),
                    supabaseUpdatedAt != null ? supabaseUpdatedAt : "datetime('now')",
                    income.getId(),
                    userId
            };

            try {
                int rows = dbService.executeUpdate(sql, params);
                return rows > 0;
            } catch (Exception e) {
                System.err.println("IncomeDAO.upsertFromSupabase (UPDATE): " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }
    }
}
