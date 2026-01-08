package com.mahal.database;

import com.mahal.model.Member;
import com.mahal.sync.SyncHelper;
import com.mahal.util.SessionManager;
import com.mahal.model.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class MemberDAO {
    private DatabaseService dbService;

    public MemberDAO() {
        this.dbService = DatabaseService.getInstance();
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS members (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id TEXT NOT NULL, " + // CRITICAL: User isolation
                "name TEXT NOT NULL, " +
                "qualification TEXT, " +
                "father_name TEXT, " +
                "mother_name TEXT, " +
                "district TEXT, " +
                "panchayat TEXT, " +
                "mahal TEXT, " +
                "date_of_birth TEXT, " +
                "address TEXT, " +
                "mobile TEXT, " +
                "gender TEXT, " +
                "id_proof_type TEXT, " +
                "id_proof_no TEXT, " +
                "photo_path TEXT, " +
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                ")";
        try {
            dbService.executeUpdate(sql, null);
            // Ensure user_id column exists (for existing databases)
            DAOBase.ensureUserIdColumn(dbService, "members");
            // Create index for better query performance
            try {
                dbService.executeUpdate("CREATE INDEX IF NOT EXISTS idx_members_user_id ON members(user_id)", null);
            } catch (Exception e) {
                // Index might already exist, ignore
            }
        } catch (Exception e) {
            System.err.println("Error creating members table: " + e.getMessage());
        }
    }

    /**
     * Get current user ID from session.
     */
    private String getCurrentUserId() {
        SessionManager sessionManager = SessionManager.getInstance();
        User currentUser = sessionManager.getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            throw new IllegalStateException(
                    "No user logged in. Cannot perform database operation without user context.");
        }
        return String.valueOf(currentUser.getId());
    }

    public List<Member> getAll() {
        String userId = getCurrentUserId();
        String sql = "SELECT id, user_id, name, qualification, father_name, mother_name, district, panchayat, mahal, " +
                "date_of_birth, address, mobile, gender, id_proof_type, id_proof_no, photo_path, " +
                "created_at, updated_at FROM members WHERE user_id = ? ORDER BY name";
        List<Member> results = dbService.executeQuery(sql, new Object[] { userId }, this::mapResultSet);
        System.out
                .println("MemberDAO.getAll(): Retrieved " + results.size() + " member records for user_id: " + userId);
        return results;
    }

    public Member getById(Long id) {
        String userId = getCurrentUserId();
        String sql = "SELECT id, user_id, name, qualification, father_name, mother_name, district, panchayat, mahal, " +
                "date_of_birth, address, mobile, gender, id_proof_type, id_proof_no, photo_path, " +
                "created_at, updated_at FROM members WHERE id = ? AND user_id = ?";
        List<Member> results = dbService.executeQuery(sql, new Object[] { id, userId }, this::mapResultSet);
        return results.isEmpty() ? null : results.get(0);
    }

    public Member getByAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            return null;
        }
        String userId = getCurrentUserId();
        String trimmedAddress = address.trim();
        // Try exact match first (case-sensitive)
        String sql = "SELECT id, user_id, name, qualification, father_name, mother_name, district, panchayat, mahal, " +
                "date_of_birth, address, mobile, gender, id_proof_type, id_proof_no, photo_path, " +
                "created_at, updated_at FROM members WHERE TRIM(address) = ? AND user_id = ? LIMIT 1";
        List<Member> results = dbService.executeQuery(sql, new Object[] { trimmedAddress, userId }, this::mapResultSet);
        if (!results.isEmpty()) {
            return results.get(0);
        }
        // If no exact match, try case-insensitive
        sql = "SELECT id, user_id, name, qualification, father_name, mother_name, district, panchayat, mahal, " +
                "date_of_birth, address, mobile, gender, id_proof_type, id_proof_no, photo_path, " +
                "created_at, updated_at FROM members WHERE TRIM(LOWER(address)) = LOWER(?) AND user_id = ? LIMIT 1";
        results = dbService.executeQuery(sql, new Object[] { trimmedAddress, userId }, this::mapResultSet);
        return results.isEmpty() ? null : results.get(0);
    }

    public Long create(Member member) {
        String userId = getCurrentUserId();
        String sql = "INSERT INTO members (user_id, name, qualification, father_name, mother_name, district, panchayat, mahal, "
                +
                "date_of_birth, address, mobile, gender, id_proof_type, id_proof_no, photo_path, created_at, updated_at) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))";
        // Convert LocalDate to string format YYYY-MM-DD for SQLite TEXT field
        String dateOfBirthStr = member.getDateOfBirth() != null ? member.getDateOfBirth().toString() : null;
        Object[] params = {
                userId, // CRITICAL: Set user_id for user isolation
                member.getName(),
                member.getQualification(),
                member.getFatherName(),
                member.getMotherName(),
                member.getDistrict(),
                member.getPanchayat(),
                member.getMahal(),
                dateOfBirthStr,
                member.getAddress(),
                member.getMobile(),
                member.getGender(),
                member.getIdProofType(),
                member.getIdProofNo(),
                member.getPhotoPath()
        };
        Long newId = dbService.executeInsert(sql, params);

        // Queue for sync if record was created successfully
        if (newId != null) {
            member.setId(newId);
            SyncHelper.queueInsert("members", newId, member);
        }

        return newId;
    }

    public boolean update(Member member) {
        String userId = getCurrentUserId();
        // CRITICAL: Filter by both id AND user_id to ensure user can only update their
        // own records
        String sql = "UPDATE members SET name = ?, qualification = ?, father_name = ?, mother_name = ?, district = ?, "
                +
                "panchayat = ?, mahal = ?, date_of_birth = ?, address = ?, mobile = ?, gender = ?, " +
                "id_proof_type = ?, id_proof_no = ?, photo_path = ?, updated_at = datetime('now') " +
                "WHERE id = ? AND user_id = ?";
        // Convert LocalDate to string format YYYY-MM-DD for SQLite TEXT field
        String dateOfBirthStr = member.getDateOfBirth() != null ? member.getDateOfBirth().toString() : null;
        Object[] params = {
                member.getName(),
                member.getQualification(),
                member.getFatherName(),
                member.getMotherName(),
                member.getDistrict(),
                member.getPanchayat(),
                member.getMahal(),
                dateOfBirthStr,
                member.getAddress(),
                member.getMobile(),
                member.getGender(),
                member.getIdProofType(),
                member.getIdProofNo(),
                member.getPhotoPath(),
                member.getId(),
                userId // CRITICAL: User isolation check
        };
        boolean success = dbService.executeUpdate(sql, params) > 0;

        // Queue for sync if update was successful
        if (success && member.getId() != null) {
            SyncHelper.queueUpdate("members", member.getId(), member);
        }

        return success;
    }

    public boolean delete(Long id) {
        try {
            String userId = getCurrentUserId();
            // First, check if member is referenced in other tables (only for this user's
            // records)
            // Delete or nullify references in due_collections
            String deleteDueCollections = "DELETE FROM due_collections WHERE member_id = ? AND user_id = ?";
            dbService.executeUpdate(deleteDueCollections, new Object[] { id, userId });

            // Delete or nullify references in incomes
            String deleteIncomes = "DELETE FROM incomes WHERE member_id = ? AND user_id = ?";
            dbService.executeUpdate(deleteIncomes, new Object[] { id, userId });

            // CRITICAL: Filter by both id AND user_id to ensure user can only delete their
            // own records
            String sql = "DELETE FROM members WHERE id = ? AND user_id = ?";
            boolean success = dbService.executeUpdate(sql, new Object[] { id, userId }) > 0;

            // Queue for sync if delete was successful
            if (success && id != null) {
                SyncHelper.queueDelete("members", id);
            }

            return success;
        } catch (Exception e) {
            System.err.println("Error deleting member: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public String getDeleteErrorMessage(Long id) {
        // Check which tables reference this member
        StringBuilder errorMsg = new StringBuilder("Cannot delete member. This member is referenced in: ");
        boolean hasReferences = false;

        try {
            // Check due_collections
            String checkDueCollections = "SELECT COUNT(*) as count FROM due_collections WHERE member_id = ?";
            List<Long> dueResults = dbService.executeQuery(checkDueCollections,
                    new Object[] { id }, rs -> {
                        try {
                            return rs.getLong("count");
                        } catch (SQLException e) {
                            return 0L;
                        }
                    });
            if (!dueResults.isEmpty() && dueResults.get(0) > 0) {
                errorMsg.append("Due Collections");
                hasReferences = true;
            }

            // Check incomes
            String checkIncomes = "SELECT COUNT(*) as count FROM incomes WHERE member_id = ?";
            List<Long> incomeResults = dbService.executeQuery(checkIncomes,
                    new Object[] { id }, rs -> {
                        try {
                            return rs.getLong("count");
                        } catch (SQLException e) {
                            return 0L;
                        }
                    });
            if (!incomeResults.isEmpty() && incomeResults.get(0) > 0) {
                if (hasReferences)
                    errorMsg.append(", ");
                errorMsg.append("Incomes");
                hasReferences = true;
            }

            if (!hasReferences) {
                return "Failed to delete member. Unknown error occurred.";
            }

            errorMsg.append(". Please remove these references first.");
            return errorMsg.toString();
        } catch (Exception e) {
            return "Failed to delete member: " + e.getMessage();
        }
    }

    private Member mapResultSet(ResultSet rs) {
        try {
            Member member = new Member();
            member.setId(rs.getLong("id"));
            member.setName(rs.getString("name"));
            member.setQualification(rs.getString("qualification"));
            member.setFatherName(rs.getString("father_name"));
            member.setMotherName(rs.getString("mother_name"));
            member.setDistrict(rs.getString("district"));
            member.setPanchayat(rs.getString("panchayat"));
            member.setMahal(rs.getString("mahal"));

            // Handle date from SQLite TEXT field - read as string and parse
            try {
                String dateStr = rs.getString("date_of_birth");
                if (dateStr != null && !dateStr.isEmpty() && !rs.wasNull()) {
                    try {
                        // Try parsing as ISO date format (YYYY-MM-DD)
                        member.setDateOfBirth(java.time.LocalDate.parse(dateStr));
                    } catch (java.time.format.DateTimeParseException e) {
                        // If that fails, try java.sql.Date format
                        try {
                            member.setDateOfBirth(java.sql.Date.valueOf(dateStr).toLocalDate());
                        } catch (IllegalArgumentException ex) {
                            System.err.println("Could not parse date_of_birth: '" + dateStr + "'");
                        }
                    }
                }
            } catch (SQLException dateEx) {
                System.err.println("Error reading date_of_birth: " + dateEx.getMessage());
            }

            member.setAddress(rs.getString("address"));
            member.setMobile(rs.getString("mobile"));
            member.setGender(rs.getString("gender"));
            member.setIdProofType(rs.getString("id_proof_type"));
            member.setIdProofNo(rs.getString("id_proof_no"));
            member.setPhotoPath(rs.getString("photo_path"));

            return member;
        } catch (SQLException e) {
            System.err.println("Error mapping ResultSet: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
