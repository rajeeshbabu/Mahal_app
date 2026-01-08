package com.mahal.database;

import com.mahal.model.Staff;
import com.mahal.sync.SyncHelper;
import com.mahal.util.SessionManager;
import com.mahal.model.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.math.BigDecimal;
import java.util.List;

public class StaffDAO {
    private DatabaseService dbService;

    public StaffDAO() {
        this.dbService = DatabaseService.getInstance();
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS staff (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id TEXT NOT NULL, " + // CRITICAL: User isolation
                "name TEXT NOT NULL, " +
                "designation TEXT, " +
                "salary REAL, " +
                "address TEXT, " +
                "mobile TEXT, " +
                "email TEXT, " +
                "joining_date TEXT, " +
                "notes TEXT, " +
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                ")";
        try {
            dbService.executeUpdate(sql, null);
            DAOBase.ensureUserIdColumn(dbService, "staff");
            try {
                dbService.executeUpdate("CREATE INDEX IF NOT EXISTS idx_staff_user_id ON staff(user_id)", null);
            } catch (Exception e) {
            }
        } catch (Exception e) {
            System.err.println("Error creating staff table: " + e.getMessage());
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

    public List<Staff> getAll() {
        String userId = getCurrentUserId();
        String sql = "SELECT id, user_id, name, designation, salary, address, mobile, email, joining_date, notes, " +
                "created_at, updated_at FROM staff WHERE user_id = ? ORDER BY name";
        List<Staff> results = dbService.executeQuery(sql, new Object[] { userId }, this::mapResultSet);
        System.out.println("StaffDAO.getAll(): Retrieved " + results.size() + " staff records for user_id: " + userId);
        return results;
    }

    public Staff getById(Long id) {
        String userId = getCurrentUserId();
        String sql = "SELECT id, user_id, name, designation, salary, address, mobile, email, joining_date, notes, " +
                "created_at, updated_at FROM staff WHERE id = ? AND user_id = ?";
        List<Staff> results = dbService.executeQuery(sql, new Object[] { id, userId }, this::mapResultSet);
        return results.isEmpty() ? null : results.get(0);
    }

    public Long create(Staff staff) {
        String userId = getCurrentUserId();
        String sql = "INSERT INTO staff (user_id, name, designation, salary, address, mobile, email, joining_date, notes, created_at, updated_at) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))";
        // Convert LocalDate to string format YYYY-MM-DD for SQLite TEXT field
        String joiningDateStr = staff.getJoiningDate() != null ? staff.getJoiningDate().toString() : null;
        Object[] params = {
                userId, // CRITICAL: Set user_id for user isolation
                staff.getName(),
                staff.getDesignation(),
                staff.getSalary(),
                staff.getAddress(),
                staff.getMobile(),
                staff.getEmail(),
                joiningDateStr,
                staff.getNotes()
        };
        Long newId = dbService.executeInsert(sql, params);

        // Queue for sync if record was created successfully
        if (newId != null) {
            staff.setId(newId);
            SyncHelper.queueInsert("staff", newId, staff);
        }

        return newId;
    }

    public boolean update(Staff staff) {
        if (staff == null || staff.getId() == null) {
            System.err.println("StaffDAO.update: Staff or ID is null");
            return false;
        }

        String userId = getCurrentUserId();
        String sql = "UPDATE staff SET name = ?, designation = ?, salary = ?, address = ?, mobile = ?, " +
                "email = ?, joining_date = ?, notes = ?, updated_at = datetime('now') WHERE id = ? AND user_id = ?";
        // Convert LocalDate to string format YYYY-MM-DD for SQLite TEXT field
        String joiningDateStr = staff.getJoiningDate() != null ? staff.getJoiningDate().toString() : null;
        Object[] params = {
                staff.getName(),
                staff.getDesignation(),
                staff.getSalary(),
                staff.getAddress(),
                staff.getMobile(),
                staff.getEmail(),
                joiningDateStr,
                staff.getNotes(),
                staff.getId(),
                userId // CRITICAL: User isolation check
        };

        try {
            int rowsAffected = dbService.executeUpdate(sql, params);
            boolean success = rowsAffected > 0;

            // Queue for sync if update was successful
            if (success) {
                SyncHelper.queueUpdate("staff", staff.getId(), staff);
            }

            return success;
        } catch (Exception e) {
            System.err.println("StaffDAO.update: Exception occurred: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean delete(Long id) {
        String userId = getCurrentUserId();
        String sql = "DELETE FROM staff WHERE id = ? AND user_id = ?";
        boolean success = dbService.executeUpdate(sql, new Object[] { id, userId }) > 0;

        // Queue for sync if delete was successful
        if (success) {
            SyncHelper.queueDelete("staff", id);
        }

        return success;
    }

    private Staff mapResultSet(ResultSet rs) {
        try {
            Staff staff = new Staff();
            staff.setId(rs.getLong("id"));
            staff.setName(rs.getString("name"));

            String designation = rs.getString("designation");
            if (!rs.wasNull()) {
                staff.setDesignation(designation);
            }

            BigDecimal salary = rs.getBigDecimal("salary");
            if (!rs.wasNull() && salary != null) {
                staff.setSalary(salary);
            }

            String address = rs.getString("address");
            if (!rs.wasNull()) {
                staff.setAddress(address);
            }

            String mobile = rs.getString("mobile");
            if (!rs.wasNull()) {
                staff.setMobile(mobile);
            }

            String email = rs.getString("email");
            if (!rs.wasNull()) {
                staff.setEmail(email);
            }

            // Handle date from SQLite TEXT field - read as string and parse
            try {
                String dateStr = rs.getString("joining_date");
                if (dateStr != null && !dateStr.isEmpty() && !rs.wasNull()) {
                    try {
                        // Handle potential timestamp format (YYYY-MM-DD HH:MM:SS) by taking first 10
                        // chars
                        String cleanDateStr = dateStr;
                        if (dateStr.length() > 10) {
                            cleanDateStr = dateStr.substring(0, 10);
                        }

                        // Try parsing as ISO date format (YYYY-MM-DD)
                        staff.setJoiningDate(java.time.LocalDate.parse(cleanDateStr));
                    } catch (java.time.format.DateTimeParseException e) {
                        System.err.println("StaffDAO: Date parse error for '" + dateStr + "': " + e.getMessage());
                        // Fallback or leave null
                    }
                }
            } catch (SQLException dateEx) {
                System.err.println("Error reading joining_date: " + dateEx.getMessage());
            }

            String notes = rs.getString("notes");
            if (!rs.wasNull()) {
                staff.setNotes(notes);
            }

            return staff;
        } catch (SQLException e) {
            System.err.println("Error mapping ResultSet to Staff: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Insert or update a record coming from Supabase sync.
     * Used by SyncManager during pull sync.
     */
    public boolean upsertFromSupabase(Staff staff, String supabaseUpdatedAt) {
        if (staff == null || staff.getId() == null) {
            return false;
        }

        String userId = getCurrentUserId();

        // Check if record exists
        Staff existing = getById(staff.getId());

        if (existing == null) {
            // INSERT (forcing the ID from Supabase)
            String sql = "INSERT INTO staff (id, user_id, name, designation, salary, address, mobile, email, joining_date, notes, created_at, updated_at) "
                    +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), ?)";

            String joiningDateStr = staff.getJoiningDate() != null ? staff.getJoiningDate().toString() : null;

            Object[] params = {
                    staff.getId(),
                    userId,
                    staff.getName(),
                    staff.getDesignation(),
                    staff.getSalary(),
                    staff.getAddress(),
                    staff.getMobile(),
                    staff.getEmail(),
                    joiningDateStr,
                    staff.getNotes(),
                    supabaseUpdatedAt != null ? supabaseUpdatedAt : "datetime('now')"
            };

            try {
                // Use executeUpdate instead of executeInsert because we are forcing the ID
                int rows = dbService.executeUpdate(sql, params);
                return rows > 0;
            } catch (Exception e) {
                System.err.println("StaffDAO.upsertFromSupabase (INSERT): " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        } else {
            // UPDATE
            // Only update if remote version is newer or equal (simple strategy)
            // Ideally we check timestamps, but for now we trust the sync direction

            String sql = "UPDATE staff SET name = ?, designation = ?, salary = ?, address = ?, mobile = ?, " +
                    "email = ?, joining_date = ?, notes = ?, updated_at = ? WHERE id = ? AND user_id = ?";

            String joiningDateStr = staff.getJoiningDate() != null ? staff.getJoiningDate().toString() : null;

            Object[] params = {
                    staff.getName(),
                    staff.getDesignation(),
                    staff.getSalary(),
                    staff.getAddress(),
                    staff.getMobile(),
                    staff.getEmail(),
                    joiningDateStr,
                    staff.getNotes(),
                    supabaseUpdatedAt != null ? supabaseUpdatedAt : "datetime('now')",
                    staff.getId(),
                    userId
            };

            try {
                int rows = dbService.executeUpdate(sql, params);
                return rows > 0;
            } catch (Exception e) {
                System.err.println("StaffDAO.upsertFromSupabase (UPDATE): " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }
    }
}
