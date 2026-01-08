package com.mahal.database;

import com.mahal.model.Student;
import com.mahal.sync.SyncHelper;
import com.mahal.util.SessionManager;
import com.mahal.model.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class StudentDAO {
    private DatabaseService dbService;

    public StudentDAO() {
        this.dbService = DatabaseService.getInstance();
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS students (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id TEXT NOT NULL, " + // CRITICAL: User isolation
                "name TEXT NOT NULL, " +
                "course TEXT, " +
                "admission_number TEXT, " +
                "admission_date TEXT, " +
                "mobile TEXT, " +
                "email TEXT, " +
                "address TEXT, " +
                "father_name TEXT, " +
                "mother_name TEXT, " +
                "guardian_mobile TEXT, " +
                "notes TEXT, " +
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                ")";
        try {
            dbService.executeUpdate(sql, null);
            DAOBase.ensureUserIdColumn(dbService, "students");
            try {
                dbService.executeUpdate("CREATE INDEX IF NOT EXISTS idx_students_user_id ON students(user_id)", null);
            } catch (Exception e) {
            }
        } catch (Exception e) {
            System.err.println("Error creating students table: " + e.getMessage());
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

    public List<Student> getAll() {
        String userId = getCurrentUserId();
        String sql = "SELECT id, user_id, name, course, admission_number, admission_date, mobile, email, address, " +
                "father_name, mother_name, guardian_mobile, notes, created_at, updated_at " +
                "FROM students WHERE user_id = ? ORDER BY name";
        List<Student> results = dbService.executeQuery(sql, new Object[] { userId }, this::mapResultSet);
        System.out.println("StudentDAO.getAll(): Retrieved " + results.size() + " student records for user_id: " + userId);
        return results;
    }

    public Student getById(Long id) {
        String userId = getCurrentUserId();
        String sql = "SELECT id, user_id, name, course, admission_number, admission_date, mobile, email, address, " +
                "father_name, mother_name, guardian_mobile, notes, created_at, updated_at " +
                "FROM students WHERE id = ? AND user_id = ?";
        List<Student> results = dbService.executeQuery(sql, new Object[] { id, userId }, this::mapResultSet);
        return results.isEmpty() ? null : results.get(0);
    }

    public Long create(Student student) {
        String userId = getCurrentUserId();
        String sql = "INSERT INTO students (user_id, name, course, admission_number, admission_date, mobile, email, " +
                "address, father_name, mother_name, guardian_mobile, notes, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))";
        // Convert LocalDate to string format YYYY-MM-DD for SQLite TEXT field
        String admissionDateStr = student.getAdmissionDate() != null ? student.getAdmissionDate().toString() : null;
        Object[] params = {
                userId, // CRITICAL: Set user_id for user isolation
                student.getName(),
                student.getCourse(),
                student.getAdmissionNumber(),
                admissionDateStr,
                student.getMobile(),
                student.getEmail(),
                student.getAddress(),
                student.getFatherName(),
                student.getMotherName(),
                student.getGuardianMobile(),
                student.getNotes()
        };
        Long newId = dbService.executeInsert(sql, params);

        // Queue for sync if record was created successfully
        if (newId != null) {
            student.setId(newId);
            SyncHelper.queueInsert("students", newId, student);
        }

        return newId;
    }

    public boolean update(Student student) {
        if (student == null || student.getId() == null) {
            System.err.println("StudentDAO.update: Student or ID is null");
            return false;
        }

        String userId = getCurrentUserId();
        String sql = "UPDATE students SET name = ?, course = ?, admission_number = ?, admission_date = ?, " +
                "mobile = ?, email = ?, address = ?, father_name = ?, mother_name = ?, guardian_mobile = ?, " +
                "notes = ?, updated_at = datetime('now') WHERE id = ? AND user_id = ?";
        // Convert LocalDate to string format YYYY-MM-DD for SQLite TEXT field
        String admissionDateStr = student.getAdmissionDate() != null ? student.getAdmissionDate().toString() : null;
        Object[] params = {
                student.getName(),
                student.getCourse(),
                student.getAdmissionNumber(),
                admissionDateStr,
                student.getMobile(),
                student.getEmail(),
                student.getAddress(),
                student.getFatherName(),
                student.getMotherName(),
                student.getGuardianMobile(),
                student.getNotes(),
                student.getId(),
                userId // CRITICAL: User isolation check
        };

        try {
            int rowsAffected = dbService.executeUpdate(sql, params);
            boolean success = rowsAffected > 0;

            // Queue for sync if update was successful
            if (success) {
                SyncHelper.queueUpdate("students", student.getId(), student);
            }

            return success;
        } catch (Exception e) {
            System.err.println("StudentDAO.update: Exception occurred: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean delete(Long id) {
        String userId = getCurrentUserId();
        String sql = "DELETE FROM students WHERE id = ? AND user_id = ?";
        boolean success = dbService.executeUpdate(sql, new Object[] { id, userId }) > 0;

        // Queue for sync if delete was successful
        if (success) {
            SyncHelper.queueDelete("students", id);
        }

        return success;
    }

    private Student mapResultSet(ResultSet rs) {
        try {
            Student student = new Student();
            student.setId(rs.getLong("id"));
            student.setName(rs.getString("name"));

            String course = rs.getString("course");
            if (!rs.wasNull()) {
                student.setCourse(course);
            }

            String admissionNumber = rs.getString("admission_number");
            if (!rs.wasNull()) {
                student.setAdmissionNumber(admissionNumber);
            }

            // Handle date from SQLite TEXT field - read as string and parse
            try {
                String dateStr = rs.getString("admission_date");
                if (dateStr != null && !dateStr.isEmpty() && !rs.wasNull()) {
                    try {
                        // Handle potential timestamp format (YYYY-MM-DD HH:MM:SS) by taking first 10 chars
                        String cleanDateStr = dateStr;
                        if (dateStr.length() > 10) {
                            cleanDateStr = dateStr.substring(0, 10);
                        }
                        // Try parsing as ISO date format (YYYY-MM-DD)
                        student.setAdmissionDate(java.time.LocalDate.parse(cleanDateStr));
                    } catch (java.time.format.DateTimeParseException e) {
                        System.err.println("StudentDAO: Date parse error for '" + dateStr + "': " + e.getMessage());
                    }
                }
            } catch (SQLException dateEx) {
                System.err.println("Error reading admission_date: " + dateEx.getMessage());
            }

            String mobile = rs.getString("mobile");
            if (!rs.wasNull()) {
                student.setMobile(mobile);
            }

            String email = rs.getString("email");
            if (!rs.wasNull()) {
                student.setEmail(email);
            }

            String address = rs.getString("address");
            if (!rs.wasNull()) {
                student.setAddress(address);
            }

            String fatherName = rs.getString("father_name");
            if (!rs.wasNull()) {
                student.setFatherName(fatherName);
            }

            String motherName = rs.getString("mother_name");
            if (!rs.wasNull()) {
                student.setMotherName(motherName);
            }

            String guardianMobile = rs.getString("guardian_mobile");
            if (!rs.wasNull()) {
                student.setGuardianMobile(guardianMobile);
            }

            String notes = rs.getString("notes");
            if (!rs.wasNull()) {
                student.setNotes(notes);
            }

            return student;
        } catch (SQLException e) {
            System.err.println("Error mapping ResultSet to Student: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Insert or update a record coming from Supabase sync.
     * Used by SyncManager during pull sync.
     */
    public boolean upsertFromSupabase(Student student, String supabaseUpdatedAt) {
        if (student == null || student.getId() == null) {
            return false;
        }

        String userId = getCurrentUserId();

        // Check if record exists
        Student existing = getById(student.getId());

        if (existing == null) {
            // INSERT (forcing the ID from Supabase)
            String sql = "INSERT INTO students (id, user_id, name, course, admission_number, admission_date, mobile, " +
                    "email, address, father_name, mother_name, guardian_mobile, notes, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), ?)";

            String admissionDateStr = student.getAdmissionDate() != null ? student.getAdmissionDate().toString() : null;

            Object[] params = {
                    student.getId(),
                    userId,
                    student.getName(),
                    student.getCourse(),
                    student.getAdmissionNumber(),
                    admissionDateStr,
                    student.getMobile(),
                    student.getEmail(),
                    student.getAddress(),
                    student.getFatherName(),
                    student.getMotherName(),
                    student.getGuardianMobile(),
                    student.getNotes(),
                    supabaseUpdatedAt != null ? supabaseUpdatedAt : "datetime('now')"
            };

            try {
                int rows = dbService.executeUpdate(sql, params);
                return rows > 0;
            } catch (Exception e) {
                System.err.println("StudentDAO.upsertFromSupabase (INSERT): " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        } else {
            // UPDATE
            String sql = "UPDATE students SET name = ?, course = ?, admission_number = ?, admission_date = ?, " +
                    "mobile = ?, email = ?, address = ?, father_name = ?, mother_name = ?, guardian_mobile = ?, " +
                    "notes = ?, updated_at = ? WHERE id = ? AND user_id = ?";

            String admissionDateStr = student.getAdmissionDate() != null ? student.getAdmissionDate().toString() : null;

            Object[] params = {
                    student.getName(),
                    student.getCourse(),
                    student.getAdmissionNumber(),
                    admissionDateStr,
                    student.getMobile(),
                    student.getEmail(),
                    student.getAddress(),
                    student.getFatherName(),
                    student.getMotherName(),
                    student.getGuardianMobile(),
                    student.getNotes(),
                    supabaseUpdatedAt != null ? supabaseUpdatedAt : "datetime('now')",
                    student.getId(),
                    userId
            };

            try {
                int rows = dbService.executeUpdate(sql, params);
                return rows > 0;
            } catch (Exception e) {
                System.err.println("StudentDAO.upsertFromSupabase (UPDATE): " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }
    }
}

