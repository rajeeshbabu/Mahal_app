package com.mahal.database;

import com.mahal.model.StaffSalary;
import com.mahal.sync.SyncHelper;
import com.mahal.util.SessionManager;
import com.mahal.model.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.math.BigDecimal;
import java.util.List;

public class StaffSalaryDAO {
    private DatabaseService dbService;

    public StaffSalaryDAO() {
        this.dbService = DatabaseService.getInstance();
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS staff_salaries (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id TEXT NOT NULL, " + // CRITICAL: User isolation
                "staff_id INTEGER NOT NULL, " +
                "salary REAL, " +
                "paid_date TEXT, " +
                "paid_amount REAL, " +
                "payment_mode TEXT, " +
                "remarks TEXT, " +
                "balance REAL, " +
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                ")";
        try {
            dbService.executeUpdate(sql, null);
            DAOBase.ensureUserIdColumn(dbService, "staff_salaries");
            try {
                dbService.executeUpdate(
                        "CREATE INDEX IF NOT EXISTS idx_staff_salaries_user_id ON staff_salaries(user_id)", null);
            } catch (Exception e) {
            }
        } catch (Exception e) {
            System.err.println("Error creating staff_salaries table: " + e.getMessage());
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

    public List<StaffSalary> getAll() {
        String userId = getCurrentUserId();
        String sql = "SELECT ss.id, ss.user_id, ss.staff_id, s.name as staff_name, s.designation, ss.salary, " +
                "ss.paid_date, ss.paid_amount, ss.payment_mode, ss.remarks, ss.balance, " +
                "ss.created_at, ss.updated_at " +
                "FROM staff_salaries ss " +
                "JOIN staff s ON ss.staff_id = s.id AND s.user_id = ? " +
                "WHERE ss.user_id = ? " +
                "ORDER BY ss.paid_date DESC";
        List<StaffSalary> results = dbService.executeQuery(sql, new Object[] { userId, userId }, this::mapResultSet);
        System.out.println(
                "StaffSalaryDAO.getAll(): Retrieved " + results.size() + " salary records for user_id: " + userId);
        return results;
    }

    public StaffSalary getById(Long id) {
        String userId = getCurrentUserId();
        String sql = "SELECT ss.id, ss.user_id, ss.staff_id, s.name as staff_name, s.designation, ss.salary, " +
                "ss.paid_date, ss.paid_amount, ss.payment_mode, ss.remarks, ss.balance, " +
                "ss.created_at, ss.updated_at " +
                "FROM staff_salaries ss " +
                "JOIN staff s ON ss.staff_id = s.id AND s.user_id = ? " +
                "WHERE ss.id = ? AND ss.user_id = ?";
        List<StaffSalary> results = dbService.executeQuery(sql, new Object[] { userId, id, userId },
                this::mapResultSet);
        return results.isEmpty() ? null : results.get(0);
    }

    public Long create(StaffSalary salary) {
        if (salary == null || salary.getStaffId() == null) {
            System.err.println("StaffSalaryDAO.create: Salary or staffId is null");
            return null;
        }

        String userId = getCurrentUserId();
        String sql = "INSERT INTO staff_salaries (user_id, staff_id, salary, paid_date, paid_amount, payment_mode, remarks, balance, created_at, updated_at) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))";
        // Convert LocalDate to string format YYYY-MM-DD for SQLite TEXT field
        String paidDateStr = salary.getPaidDate() != null ? salary.getPaidDate().toString() : null;
        Object[] params = {
                userId, // CRITICAL: Set user_id for user isolation
                salary.getStaffId(),
                salary.getSalary(),
                paidDateStr,
                salary.getPaidAmount(),
                salary.getPaymentMode(),
                salary.getRemarks(),
                salary.getBalance()
        };

        try {
            Long newId = dbService.executeInsert(sql, params);
            if (newId != null) {
                System.out.println("StaffSalaryDAO.create: Successfully created salary record with ID: " + newId);
                // Queue for sync if record was created successfully
                salary.setId(newId);
                SyncHelper.queueInsert("staff_salaries", newId, salary);
            } else {
                System.err.println("StaffSalaryDAO.create: Failed to create salary record");
            }
            return newId;
        } catch (Exception e) {
            System.err.println("StaffSalaryDAO.create: Exception occurred: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public boolean update(StaffSalary salary) {
        if (salary == null || salary.getId() == null) {
            System.err.println("StaffSalaryDAO.update: Salary or ID is null");
            return false;
        }

        String userId = getCurrentUserId();
        String sql = "UPDATE staff_salaries SET staff_id = ?, salary = ?, paid_date = ?, paid_amount = ?, " +
                "payment_mode = ?, remarks = ?, balance = ?, updated_at = datetime('now') WHERE id = ? AND user_id = ?";
        // Convert LocalDate to string format YYYY-MM-DD for SQLite TEXT field
        String paidDateStr = salary.getPaidDate() != null ? salary.getPaidDate().toString() : null;
        Object[] params = {
                salary.getStaffId(),
                salary.getSalary(),
                paidDateStr,
                salary.getPaidAmount(),
                salary.getPaymentMode(),
                salary.getRemarks(),
                salary.getBalance(),
                salary.getId(),
                userId // CRITICAL: User isolation check
        };

        try {
            int rowsAffected = dbService.executeUpdate(sql, params);
            boolean success = rowsAffected > 0;

            // Queue for sync if update was successful
            if (success) {
                SyncHelper.queueUpdate("staff_salaries", salary.getId(), salary);
            }

            return success;
        } catch (Exception e) {
            System.err.println("StaffSalaryDAO.update: Exception occurred: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean delete(Long id) {
        if (id == null) {
            System.err.println("StaffSalaryDAO.delete: ID is null");
            return false;
        }

        String userId = getCurrentUserId();
        String sql = "DELETE FROM staff_salaries WHERE id = ? AND user_id = ?";
        try {
            int rowsAffected = dbService.executeUpdate(sql, new Object[] { id, userId });
            boolean success = rowsAffected > 0;
            if (success) {
                System.out.println("StaffSalaryDAO.delete: Successfully deleted salary with ID: " + id);
                // Queue for sync if delete was successful
                SyncHelper.queueDelete("staff_salaries", id);
            } else {
                System.err.println("StaffSalaryDAO.delete: No rows affected. Salary ID: " + id);
            }
            return success;
        } catch (Exception e) {
            System.err.println("StaffSalaryDAO.delete: Exception occurred: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private StaffSalary mapResultSet(ResultSet rs) {
        try {
            StaffSalary salary = new StaffSalary();
            salary.setId(rs.getLong("id"));
            salary.setStaffId(rs.getLong("staff_id"));

            String staffName = rs.getString("staff_name");
            if (!rs.wasNull()) {
                salary.setStaffName(staffName);
            }

            String designation = rs.getString("designation");
            if (!rs.wasNull()) {
                salary.setDesignation(designation);
            }

            BigDecimal sal = rs.getBigDecimal("salary");
            if (!rs.wasNull() && sal != null) {
                salary.setSalary(sal);
            }

            // Handle date from SQLite TEXT field - read as string and parse
            try {
                String dateStr = rs.getString("paid_date");
                if (dateStr != null && !dateStr.isEmpty() && !rs.wasNull()) {
                    try {
                        // Handle potential timestamp format (YYYY-MM-DD HH:MM:SS) by taking first 10
                        // chars
                        String cleanDateStr = dateStr;
                        if (dateStr.length() > 10) {
                            cleanDateStr = dateStr.substring(0, 10);
                        }
                        salary.setPaidDate(java.time.LocalDate.parse(cleanDateStr));
                    } catch (java.time.format.DateTimeParseException e) {
                        System.err.println("StaffSalaryDAO: Date parse error for '" + dateStr + "': " + e.getMessage());
                    }
                }
            } catch (SQLException dateEx) {
                System.err.println("Error reading paid_date: " + dateEx.getMessage());
            }

            BigDecimal paidAmount = rs.getBigDecimal("paid_amount");
            if (!rs.wasNull() && paidAmount != null) {
                salary.setPaidAmount(paidAmount);
            }

            String paymentMode = rs.getString("payment_mode");
            if (!rs.wasNull()) {
                salary.setPaymentMode(paymentMode);
            }

            String remarks = rs.getString("remarks");
            if (!rs.wasNull()) {
                salary.setRemarks(remarks);
            }

            BigDecimal balance = rs.getBigDecimal("balance");
            if (!rs.wasNull() && balance != null) {
                salary.setBalance(balance);
            }

            return salary;
        } catch (SQLException e) {
            System.err.println("Error mapping ResultSet to StaffSalary: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Insert or update a record coming from Supabase sync.
     * Used by SyncManager during pull sync.
     */
    public boolean upsertFromSupabase(StaffSalary salary, String supabaseUpdatedAt) {
        if (salary == null || salary.getId() == null) {
            return false;
        }

        String userId = getCurrentUserId();

        // Check if record exists
        StaffSalary existing = getById(salary.getId());

        // Format dates
        String paidDateStr = salary.getPaidDate() != null ? salary.getPaidDate().toString() : null;

        if (existing == null) {
            // INSERT (forcing the ID from Supabase)
            String sql = "INSERT INTO staff_salaries (id, user_id, staff_id, salary, paid_date, paid_amount, payment_mode, balance, remarks, created_at, updated_at) "
                    +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), ?)";

            Object[] params = {
                    salary.getId(),
                    userId,
                    salary.getStaffId(),
                    salary.getSalary(),
                    paidDateStr,
                    salary.getPaidAmount(),
                    salary.getPaymentMode(),
                    salary.getBalance(),
                    salary.getRemarks(),
                    supabaseUpdatedAt != null ? supabaseUpdatedAt : "datetime('now')"
            };

            try {
                int rows = dbService.executeUpdate(sql, params);
                return rows > 0;
            } catch (Exception e) {
                System.err.println("StaffSalaryDAO.upsertFromSupabase (INSERT): " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        } else {
            // UPDATE
            String sql = "UPDATE staff_salaries SET staff_id = ?, salary = ?, paid_date = ?, paid_amount = ?, " +
                    "payment_mode = ?, balance = ?, remarks = ?, updated_at = ? WHERE id = ? AND user_id = ?";

            Object[] params = {
                    salary.getStaffId(),
                    salary.getSalary(),
                    paidDateStr,
                    salary.getPaidAmount(),
                    salary.getPaymentMode(),
                    salary.getBalance(),
                    salary.getRemarks(),
                    supabaseUpdatedAt != null ? supabaseUpdatedAt : "datetime('now')",
                    salary.getId(),
                    userId
            };

            try {
                int rows = dbService.executeUpdate(sql, params);
                return rows > 0;
            } catch (Exception e) {
                System.err.println("StaffSalaryDAO.upsertFromSupabase (UPDATE): " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }
    }
}
