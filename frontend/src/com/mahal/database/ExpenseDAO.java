package com.mahal.database;

import com.mahal.model.Expense;
import com.mahal.sync.SyncHelper;
import com.mahal.util.SessionManager;
import com.mahal.model.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.util.List;

public class ExpenseDAO {
    private DatabaseService dbService;

    public ExpenseDAO() {
        this.dbService = DatabaseService.getInstance();
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS expenses (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id TEXT NOT NULL, " + // CRITICAL: User isolation
                "expense_type TEXT, " +
                "amount REAL, " +
                "date TEXT, " +
                "masjid_id INTEGER, " +
                "notes TEXT, " +
                "receipt_path TEXT, " +
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                ")";
        try {
            dbService.executeUpdate(sql, null);
            DAOBase.ensureUserIdColumn(dbService, "expenses");
            try {
                dbService.executeUpdate("CREATE INDEX IF NOT EXISTS idx_expenses_user_id ON expenses(user_id)", null);
            } catch (Exception e) {
            }
        } catch (Exception e) {
            System.err.println("Error creating expenses table: " + e.getMessage());
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

    public List<Expense> getAll() {
        String userId = getCurrentUserId();
        String sql = "SELECT e.id, e.user_id, e.expense_type, e.amount, e.date, e.masjid_id, m.name as masjid_name, " +
                "e.notes, e.receipt_path, e.created_at, e.updated_at " +
                "FROM expenses e " +
                "LEFT JOIN masjids m ON e.masjid_id = m.id AND m.user_id = ? " +
                "WHERE e.user_id = ? " +
                "ORDER BY e.date DESC";
        List<Expense> results = dbService.executeQuery(sql, new Object[] { userId, userId }, this::mapResultSet);
        System.out.println(
                "ExpenseDAO.getAll(): Retrieved " + results.size() + " expense records for user_id: " + userId);
        return results;
    }

    public Long create(Expense expense) {
        String userId = getCurrentUserId();
        String sql = "INSERT INTO expenses (user_id, expense_type, amount, date, masjid_id, notes, receipt_path, created_at, updated_at) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))";
        // Convert LocalDate to string format YYYY-MM-DD for SQLite TEXT field
        String dateStr = expense.getDate() != null ? expense.getDate().toString() : null;
        Object[] params = {
                userId, // CRITICAL: Set user_id for user isolation
                expense.getExpenseType(),
                expense.getAmount(),
                dateStr,
                expense.getMasjidId(),
                expense.getNotes(),
                expense.getReceiptPath()
        };
        Long newId = dbService.executeInsert(sql, params);

        // Queue for sync if record was created successfully
        if (newId != null) {
            expense.setId(newId);
            SyncHelper.queueInsert("expenses", newId, expense);
        }

        return newId;
    }

    public boolean update(Expense expense) {
        String userId = getCurrentUserId();
        String sql = "UPDATE expenses SET expense_type = ?, amount = ?, date = ?, masjid_id = ?, notes = ?, receipt_path = ?, updated_at = datetime('now') "
                +
                "WHERE id = ? AND user_id = ?";
        // Convert LocalDate to string format YYYY-MM-DD for SQLite TEXT field
        String dateStr = expense.getDate() != null ? expense.getDate().toString() : null;
        Object[] params = {
                expense.getExpenseType(),
                expense.getAmount(),
                dateStr,
                expense.getMasjidId(),
                expense.getNotes(),
                expense.getReceiptPath(),
                expense.getId(),
                userId // CRITICAL: User isolation check
        };
        boolean success = dbService.executeUpdate(sql, params) > 0;

        // Queue for sync if update was successful
        if (success && expense.getId() != null) {
            SyncHelper.queueUpdate("expenses", expense.getId(), expense);
        }

        return success;
    }

    public boolean delete(Long id) {
        String userId = getCurrentUserId();
        String sql = "DELETE FROM expenses WHERE id = ? AND user_id = ?";
        boolean success = dbService.executeUpdate(sql, new Object[] { id, userId }) > 0;

        // Queue for sync if delete was successful
        if (success && id != null) {
            SyncHelper.queueDelete("expenses", id);
        }

        return success;
    }

    private Expense mapResultSet(ResultSet rs) {
        try {
            Expense expense = new Expense();
            expense.setId(rs.getLong("id"));
            expense.setExpenseType(rs.getString("expense_type"));

            BigDecimal amount = rs.getBigDecimal("amount");
            if (amount != null)
                expense.setAmount(amount);

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
                        expense.setDate(java.time.LocalDate.parse(cleanDateStr));
                    } catch (java.time.format.DateTimeParseException e) {
                        System.err.println("ExpenseDAO: Date parse error for '" + dateStr + "': " + e.getMessage());
                    }
                }
            } catch (SQLException dateEx) {
                System.err.println("Error reading date: " + dateEx.getMessage());
            }

            Long masjidId = rs.getLong("masjid_id");
            if (!rs.wasNull())
                expense.setMasjidId(masjidId);
            String masjidName = rs.getString("masjid_name");
            if (masjidName != null && !rs.wasNull()) {
                expense.setMasjidName(masjidName);
            }

            String notes = rs.getString("notes");
            if (notes != null && !rs.wasNull()) {
                expense.setNotes(notes);
            }

            String receiptPath = rs.getString("receipt_path");
            if (receiptPath != null && !rs.wasNull()) {
                expense.setReceiptPath(receiptPath);
            }

            return expense;
        } catch (SQLException e) {
            System.err.println("Error mapping ResultSet to Expense: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Insert or update a record coming from Supabase sync.
     * Used by SyncManager during pull sync.
     */
    public boolean upsertFromSupabase(Expense expense, String supabaseUpdatedAt) {
        if (expense == null || expense.getId() == null) {
            return false;
        }

        String userId = getCurrentUserId();

        // Check if record exists
        boolean exists = false;
        String checkSql = "SELECT id FROM expenses WHERE id = ? AND user_id = ?";
        List<Object> checkResults = dbService.executeQuery(checkSql, new Object[] { expense.getId(), userId }, rs -> {
            try {
                return rs.getObject("id");
            } catch (SQLException e) {
                return null;
            }
        });
        exists = !checkResults.isEmpty();

        // Format date
        String dateStr = expense.getDate() != null ? expense.getDate().toString() : null;

        if (!exists) {
            // INSERT (forcing the ID from Supabase)
            String sql = "INSERT INTO expenses (id, user_id, expense_type, amount, date, masjid_id, notes, receipt_path, created_at, updated_at) "
                    +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), ?)";

            Object[] params = {
                    expense.getId(),
                    userId,
                    expense.getExpenseType(),
                    expense.getAmount(),
                    dateStr,
                    expense.getMasjidId(),
                    expense.getNotes(),
                    expense.getReceiptPath(),
                    supabaseUpdatedAt != null ? supabaseUpdatedAt : "datetime('now')"
            };

            try {
                int rows = dbService.executeUpdate(sql, params);
                return rows > 0;
            } catch (Exception e) {
                System.err.println("ExpenseDAO.upsertFromSupabase (INSERT): " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        } else {
            // UPDATE
            String sql = "UPDATE expenses SET expense_type = ?, amount = ?, date = ?, masjid_id = ?, notes = ?, receipt_path = ?, updated_at = ? "
                    +
                    "WHERE id = ? AND user_id = ?";

            Object[] params = {
                    expense.getExpenseType(),
                    expense.getAmount(),
                    dateStr,
                    expense.getMasjidId(),
                    expense.getNotes(),
                    expense.getReceiptPath(),
                    supabaseUpdatedAt != null ? supabaseUpdatedAt : "datetime('now')",
                    expense.getId(),
                    userId
            };

            try {
                int rows = dbService.executeUpdate(sql, params);
                return rows > 0;
            } catch (Exception e) {
                System.err.println("ExpenseDAO.upsertFromSupabase (UPDATE): " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }
    }
}
