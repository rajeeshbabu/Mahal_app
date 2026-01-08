package com.mahal.database;

import com.mahal.model.Committee;
import com.mahal.sync.SyncHelper;
import com.mahal.util.SessionManager;
import com.mahal.model.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class CommitteeDAO {
    private DatabaseService dbService;

    public CommitteeDAO() {
        this.dbService = DatabaseService.getInstance();
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS committees (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id TEXT NOT NULL, " + // CRITICAL: User isolation
                "member_name TEXT NOT NULL, " +
                "mobile TEXT, " +
                "designation TEXT, " +
                "other_details TEXT, " +
                "masjid_id INTEGER, " +
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                ")";
        try {
            dbService.executeUpdate(sql, null);
            DAOBase.ensureUserIdColumn(dbService, "committees");
            try {
                dbService.executeUpdate("CREATE INDEX IF NOT EXISTS idx_committees_user_id ON committees(user_id)",
                        null);
            } catch (Exception e) {
            }
        } catch (Exception e) {
            System.err.println("Error creating committees table: " + e.getMessage());
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

    public List<Committee> getAll() {
        String userId = getCurrentUserId();
        String sql = "SELECT c.id, c.user_id, c.member_name, c.mobile, c.designation, c.other_details, " +
                "c.masjid_id, m.name as masjid_name, c.created_at, c.updated_at " +
                "FROM committees c " +
                "LEFT JOIN masjids m ON c.masjid_id = m.id " +
                "WHERE c.user_id = ? " +
                "ORDER BY c.member_name";
        List<Committee> results = dbService.executeQuery(sql, new Object[] { userId }, this::mapResultSet);
        System.out.println(
                "CommitteeDAO.getAll(): Retrieved " + results.size() + " committee records for user_id: " + userId);
        return results;
    }

    public Committee getById(Long id) {
        String userId = getCurrentUserId();
        String sql = "SELECT c.id, c.user_id, c.member_name, c.mobile, c.designation, c.other_details, " +
                "c.masjid_id, m.name as masjid_name, c.created_at, c.updated_at " +
                "FROM committees c " +
                "LEFT JOIN masjids m ON c.masjid_id = m.id " +
                "WHERE c.id = ? AND c.user_id = ?";
        List<Committee> results = dbService.executeQuery(sql, new Object[] { id, userId }, this::mapResultSet);
        return results.isEmpty() ? null : results.get(0);
    }

    public Long create(Committee committee) {
        String userId = getCurrentUserId();
        String sql = "INSERT INTO committees (user_id, member_name, mobile, designation, other_details, masjid_id, created_at, updated_at) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))";
        Object[] params = {
                userId, // CRITICAL: Set user_id for user isolation
                committee.getMemberName(),
                committee.getMobile(),
                committee.getDesignation(),
                committee.getOtherDetails(),
                committee.getMasjidId()
        };
        Long newId = dbService.executeInsert(sql, params);

        // Queue for sync if record was created successfully
        if (newId != null) {
            committee.setId(newId);
            SyncHelper.queueInsert("committees", newId, committee);
        }

        return newId;
    }

    public boolean update(Committee committee) {
        String userId = getCurrentUserId();
        String sql = "UPDATE committees SET member_name = ?, mobile = ?, designation = ?, " +
                "other_details = ?, masjid_id = ?, updated_at = datetime('now') WHERE id = ? AND user_id = ?";
        Object[] params = {
                committee.getMemberName(),
                committee.getMobile(),
                committee.getDesignation(),
                committee.getOtherDetails(),
                committee.getMasjidId(),
                committee.getId(),
                userId // CRITICAL: User isolation check
        };
        boolean success = dbService.executeUpdate(sql, params) > 0;

        // Queue for sync if update was successful
        if (success) {
            SyncHelper.queueUpdate("committees", committee.getId(), committee);
        }

        return success;
    }

    public boolean delete(Long id) {
        String userId = getCurrentUserId();
        String sql = "DELETE FROM committees WHERE id = ? AND user_id = ?";
        boolean success = dbService.executeUpdate(sql, new Object[] { id, userId }) > 0;

        // Queue for sync if delete was successful
        if (success) {
            SyncHelper.queueDelete("committees", id);
        }

        return success;
    }

    private Committee mapResultSet(ResultSet rs) {
        try {
            Committee committee = new Committee();
            committee.setId(rs.getLong("id"));
            committee.setMemberName(rs.getString("member_name"));
            committee.setMobile(rs.getString("mobile"));
            committee.setDesignation(rs.getString("designation"));
            committee.setOtherDetails(rs.getString("other_details"));

            Long masjidId = rs.getLong("masjid_id");
            if (!rs.wasNull()) {
                committee.setMasjidId(masjidId);
            }
            committee.setMasjidName(rs.getString("masjid_name"));

            return committee;
        } catch (SQLException e) {
            System.err.println("Error mapping ResultSet: " + e.getMessage());
            return null;
        }
    }
}
