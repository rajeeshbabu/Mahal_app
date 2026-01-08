package com.mahal.database;

import com.mahal.model.Masjid;
import com.mahal.sync.SyncHelper;
import com.mahal.util.SessionManager;
import com.mahal.model.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class MasjidDAO {
    private DatabaseService dbService;
    
    public MasjidDAO() {
        this.dbService = DatabaseService.getInstance();
        createTableIfNotExists();
    }
    
    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS masjids (" +
                     "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                     "user_id TEXT NOT NULL, " +  // CRITICAL: User isolation
                     "name TEXT NOT NULL, " +
                     "abbreviation TEXT, " +
                     "address TEXT, " +
                     "waqf_board_no TEXT, " +
                     "state TEXT, " +
                     "email TEXT, " +
                     "mobile TEXT, " +
                     "registration_no TEXT, " +
                     "created_at TEXT DEFAULT CURRENT_TIMESTAMP, " +
                     "updated_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                     ")";
        try {
            dbService.executeUpdate(sql, null);
            DAOBase.ensureUserIdColumn(dbService, "masjids");
            try {
                dbService.executeUpdate("CREATE INDEX IF NOT EXISTS idx_masjids_user_id ON masjids(user_id)", null);
            } catch (Exception e) {}
        } catch (Exception e) {
            System.err.println("Error creating masjids table: " + e.getMessage());
        }
    }
    
    private String getCurrentUserId() {
        SessionManager sessionManager = SessionManager.getInstance();
        User currentUser = sessionManager.getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            throw new IllegalStateException("No user logged in. Cannot perform database operation without user context.");
        }
        return String.valueOf(currentUser.getId());
    }
    
    public List<Masjid> getAll() {
        String userId = getCurrentUserId();
        String sql = "SELECT id, user_id, name, abbreviation, address, waqf_board_no, state, email, mobile, registration_no, " +
                     "created_at, updated_at FROM masjids WHERE user_id = ? ORDER BY name";
        List<Masjid> results = dbService.executeQuery(sql, new Object[]{userId}, this::mapResultSet);
        System.out.println("MasjidDAO.getAll(): Retrieved " + results.size() + " masjid records for user_id: " + userId);
        return results;
    }
    
    public Masjid getById(Long id) {
        String userId = getCurrentUserId();
        String sql = "SELECT id, user_id, name, abbreviation, address, waqf_board_no, state, email, mobile, registration_no, " +
                     "created_at, updated_at FROM masjids WHERE id = ? AND user_id = ?";
        List<Masjid> results = dbService.executeQuery(sql, new Object[]{id, userId}, this::mapResultSet);
        return results.isEmpty() ? null : results.get(0);
    }
    
    public Long create(Masjid masjid) {
        String userId = getCurrentUserId();
        String sql = "INSERT INTO masjids (user_id, name, abbreviation, address, waqf_board_no, state, email, mobile, registration_no, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))";
        Object[] params = {
            userId,  // CRITICAL: Set user_id for user isolation
            masjid.getName(),
            masjid.getAbbreviation(),
            masjid.getAddress(),
            masjid.getWaqfBoardNo(),
            masjid.getState(),
            masjid.getEmail(),
            masjid.getMobile(),
            masjid.getRegistrationNo()
        };
        Long newId = dbService.executeInsert(sql, params);
        
        // Queue for sync if record was created successfully
        if (newId != null) {
            masjid.setId(newId);
            SyncHelper.queueInsert("masjids", newId, masjid);
        }
        
        return newId;
    }
    
    public boolean update(Masjid masjid) {
        String userId = getCurrentUserId();
        String sql = "UPDATE masjids SET name = ?, abbreviation = ?, address = ?, waqf_board_no = ?, state = ?, " +
                     "email = ?, mobile = ?, registration_no = ?, updated_at = datetime('now') WHERE id = ? AND user_id = ?";
        Object[] params = {
            masjid.getName(),
            masjid.getAbbreviation(),
            masjid.getAddress(),
            masjid.getWaqfBoardNo(),
            masjid.getState(),
            masjid.getEmail(),
            masjid.getMobile(),
            masjid.getRegistrationNo(),
            masjid.getId(),
            userId  // CRITICAL: User isolation check
        };
        boolean success = dbService.executeUpdate(sql, params) > 0;
        
        // Queue for sync if update was successful
        if (success) {
            SyncHelper.queueUpdate("masjids", masjid.getId(), masjid);
        }
        
        return success;
    }
    
    public boolean delete(Long id) {
        String userId = getCurrentUserId();
        String sql = "DELETE FROM masjids WHERE id = ? AND user_id = ?";
        boolean success = dbService.executeUpdate(sql, new Object[]{id, userId}) > 0;
        
        // Queue for sync if delete was successful
        if (success) {
            SyncHelper.queueDelete("masjids", id);
        }
        
        return success;
    }
    
    private Masjid mapResultSet(ResultSet rs) {
        try {
            Masjid masjid = new Masjid();
            masjid.setId(rs.getLong("id"));
            masjid.setName(rs.getString("name"));
            masjid.setAbbreviation(rs.getString("abbreviation"));
            masjid.setAddress(rs.getString("address"));
            masjid.setWaqfBoardNo(rs.getString("waqf_board_no"));
            masjid.setState(rs.getString("state"));
            masjid.setEmail(rs.getString("email"));
            masjid.setMobile(rs.getString("mobile"));
            masjid.setRegistrationNo(rs.getString("registration_no"));
            return masjid;
        } catch (SQLException e) {
            System.err.println("Error mapping ResultSet: " + e.getMessage());
            return null;
        }
    }
}

