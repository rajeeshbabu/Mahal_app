package com.mahal.database;

import com.mahal.model.Certificate;
import com.mahal.sync.SyncHelper;
import com.mahal.util.SessionManager;
import com.mahal.model.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

public class CertificateDAO {
    private final DatabaseService db;

    public CertificateDAO() {
        this.db = DatabaseService.getInstance();
        createTablesIfNotExists();
    }

    private void createTablesIfNotExists() {
        createMarriageTable();
        createDeathTable();
        createJamathTable();
        createCustomTable();
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

    private void createMarriageTable() {
        String sql = "CREATE TABLE IF NOT EXISTS marriage_certificates (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id TEXT NOT NULL, " + // CRITICAL: User isolation
                "certificate_no TEXT, " +
                "groom_name TEXT, " +
                "bride_name TEXT, " +
                "parent_name_of_groom TEXT, " +
                "parent_name_of_bride TEXT, " +
                "address_of_groom TEXT, " +
                "address_of_bride TEXT, " +
                "place_of_marriage TEXT, " +
                "marriage_status TEXT, " +
                "marriage_date TEXT, " +
                "additional_notes TEXT, " +
                "supporting_docs_path TEXT, " +
                "pdf_path TEXT, " +
                "qr_code TEXT, " +
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                ")";
        try {
            db.executeUpdate(sql, null);
            DAOBase.ensureUserIdColumn(db, "marriage_certificates");
            try {
                db.executeUpdate(
                        "CREATE INDEX IF NOT EXISTS idx_marriage_certificates_user_id ON marriage_certificates(user_id)",
                        null);
            } catch (Exception e) {
            }
        } catch (Exception e) {
            System.err.println("Error creating marriage_certificates table: " + e.getMessage());
        }
    }

    private void createDeathTable() {
        String sql = "CREATE TABLE IF NOT EXISTS death_certificates (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id TEXT NOT NULL, " + // CRITICAL: User isolation
                "certificate_no TEXT, " +
                "name TEXT, " +
                "parent_name TEXT, " +
                "address TEXT, " +
                "thalook TEXT, " +
                "date_of_death TEXT, " +
                "cause TEXT, " +
                "place_of_death TEXT, " +
                "issued_date TEXT, " +
                "pdf_path TEXT, " +
                "qr_code TEXT, " +
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                ")";
        try {
            db.executeUpdate(sql, null);
            DAOBase.ensureUserIdColumn(db, "death_certificates");
            try {
                db.executeUpdate(
                        "CREATE INDEX IF NOT EXISTS idx_death_certificates_user_id ON death_certificates(user_id)",
                        null);
            } catch (Exception e) {
            }
        } catch (Exception e) {
            System.err.println("Error creating death_certificates table: " + e.getMessage());
        }
    }

    private void createJamathTable() {
        String sql = "CREATE TABLE IF NOT EXISTS jamath_certificates (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id TEXT NOT NULL, " + // CRITICAL: User isolation
                "certificate_no TEXT, " +
                "name TEXT, " +
                "parent_name TEXT, " +
                "address TEXT, " +
                "thalook TEXT, " +
                "date TEXT, " +
                "remarks TEXT, " +
                "pdf_path TEXT, " +
                "qr_code TEXT, " +
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                ")";
        try {
            db.executeUpdate(sql, null);
            DAOBase.ensureUserIdColumn(db, "jamath_certificates");
            try {
                db.executeUpdate(
                        "CREATE INDEX IF NOT EXISTS idx_jamath_certificates_user_id ON jamath_certificates(user_id)",
                        null);
            } catch (Exception e) {
            }
        } catch (Exception e) {
            System.err.println("Error creating jamath_certificates table: " + e.getMessage());
        }
    }

    private void createCustomTable() {
        String sql = "CREATE TABLE IF NOT EXISTS custom_certificates (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id TEXT NOT NULL, " + // CRITICAL: User isolation
                "certificate_no TEXT, " +
                "template_name TEXT, " +
                "template_content TEXT, " +
                "field_data TEXT, " +
                "issued_date TEXT, " +
                "pdf_path TEXT, " +
                "qr_code TEXT, " +
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                ")";
        try {
            db.executeUpdate(sql, null);
            DAOBase.ensureUserIdColumn(db, "custom_certificates");
            try {
                db.executeUpdate(
                        "CREATE INDEX IF NOT EXISTS idx_custom_certificates_user_id ON custom_certificates(user_id)",
                        null);
            } catch (Exception e) {
            }
        } catch (Exception e) {
            System.err.println("Error creating custom_certificates table: " + e.getMessage());
        }
    }

    private String getTableName(String type) {
        switch (type) {
            case "Marriage":
                return "marriage_certificates";
            case "Death":
                return "death_certificates";
            case "Jamath":
                return "jamath_certificates";
            case "Custom":
                return "custom_certificates";
            default:
                return "marriage_certificates";
        }
    }

    public long countByType(String type) {
        String userId = getCurrentUserId();
        String tableName = getTableName(type);
        String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE user_id = ?";
        try {
            var result = db.executeQuery(sql, new Object[] { userId }, rs -> {
                try {
                    return rs.getLong(1);
                } catch (SQLException e) {
                    return 0L;
                }
            });
            return result.isEmpty() ? 0L : result.get(0);
        } catch (Exception e) {
            return 0L;
        }
    }

    public List<Certificate> getByType(String type) {
        return getByTypeWithFilters(type, null, null, null, null, null);
    }

    public List<Certificate> getByTypeWithFilters(String type, String search, String certificateNo,
            String name, String groomName, String brideName) {
        String userId = getCurrentUserId();
        String tableName = getTableName(type);
        String sql;
        List<Object> params = new ArrayList<>();
        params.add(userId); // Add user_id first

        if ("Marriage".equals(type)) {
            sql = "SELECT id, user_id, certificate_no, groom_name, bride_name, parent_name_of_groom, parent_name_of_bride, "
                    +
                    "address_of_groom, address_of_bride, place_of_marriage, marriage_status, marriage_date, " +
                    "additional_notes, supporting_docs_path, pdf_path, qr_code, created_at, updated_at " +
                    "FROM " + tableName + " WHERE user_id = ?";

            if (search != null && !search.isEmpty()) {
                sql += " AND (certificate_no LIKE ? OR groom_name LIKE ? OR bride_name LIKE ?)";
                String searchPattern = "%" + search + "%";
                params.add(searchPattern);
                params.add(searchPattern);
                params.add(searchPattern);
            }
            if (certificateNo != null && !certificateNo.isEmpty()) {
                sql += " AND certificate_no = ?";
                params.add(certificateNo);
            }
            if (groomName != null && !groomName.isEmpty()) {
                sql += " AND groom_name LIKE ?";
                params.add("%" + groomName + "%");
            }
            if (brideName != null && !brideName.isEmpty()) {
                sql += " AND bride_name LIKE ?";
                params.add("%" + brideName + "%");
            }

            sql += " ORDER BY marriage_date DESC, id DESC";
        } else if ("Death".equals(type)) {
            sql = "SELECT id, user_id, certificate_no, name, parent_name, address, thalook, date_of_death, cause, " +
                    "place_of_death, issued_date, pdf_path, qr_code, created_at, updated_at " +
                    "FROM " + tableName + " WHERE user_id = ?";

            if (search != null && !search.isEmpty()) {
                sql += " AND (certificate_no LIKE ? OR name LIKE ?)";
                String searchPattern = "%" + search + "%";
                params.add(searchPattern);
                params.add(searchPattern);
            }
            if (certificateNo != null && !certificateNo.isEmpty()) {
                sql += " AND certificate_no = ?";
                params.add(certificateNo);
            }
            if (name != null && !name.isEmpty()) {
                sql += " AND name LIKE ?";
                params.add("%" + name + "%");
            }

            sql += " ORDER BY date_of_death DESC, id DESC";
        } else if ("Jamath".equals(type)) {
            sql = "SELECT id, user_id, certificate_no, name, parent_name, address, thalook, date, remarks, " +
                    "pdf_path, qr_code, created_at, updated_at " +
                    "FROM " + tableName + " WHERE user_id = ?";

            if (search != null && !search.isEmpty()) {
                sql += " AND (certificate_no LIKE ? OR name LIKE ?)";
                String searchPattern = "%" + search + "%";
                params.add(searchPattern);
                params.add(searchPattern);
            }
            if (certificateNo != null && !certificateNo.isEmpty()) {
                sql += " AND certificate_no = ?";
                params.add(certificateNo);
            }
            if (name != null && !name.isEmpty()) {
                sql += " AND name LIKE ?";
                params.add("%" + name + "%");
            }

            sql += " ORDER BY date DESC, id DESC";
        } else { // Custom
            sql = "SELECT id, user_id, certificate_no, template_name, template_content, field_data, issued_date, " +
                    "pdf_path, qr_code, created_at, updated_at " +
                    "FROM " + tableName + " WHERE user_id = ?";

            if (search != null && !search.isEmpty()) {
                sql += " AND (certificate_no LIKE ? OR template_name LIKE ?)";
                String searchPattern = "%" + search + "%";
                params.add(searchPattern);
                params.add(searchPattern);
            }
            if (certificateNo != null && !certificateNo.isEmpty()) {
                sql += " AND certificate_no = ?";
                params.add(certificateNo);
            }
            if (name != null && !name.isEmpty()) {
                sql += " AND template_name LIKE ?";
                params.add("%" + name + "%");
            }

            sql += " ORDER BY issued_date DESC, id DESC";
        }

        return db.executeQuery(sql, params.toArray(), this::mapResultSet);
    }

    public Certificate getById(Long id, String type) {
        String userId = getCurrentUserId();
        String tableName = getTableName(type);
        String sql;

        if ("Marriage".equals(type)) {
            sql = "SELECT id, user_id, certificate_no, groom_name, bride_name, parent_name_of_groom, parent_name_of_bride, "
                    +
                    "address_of_groom, address_of_bride, place_of_marriage, marriage_status, marriage_date, " +
                    "additional_notes, supporting_docs_path, pdf_path, qr_code, created_at, updated_at " +
                    "FROM " + tableName + " WHERE id = ? AND user_id = ?";
        } else if ("Death".equals(type)) {
            sql = "SELECT id, user_id, certificate_no, name, parent_name, address, thalook, date_of_death, cause, " +
                    "place_of_death, issued_date, pdf_path, qr_code, created_at, updated_at " +
                    "FROM " + tableName + " WHERE id = ? AND user_id = ?";
        } else if ("Jamath".equals(type)) {
            sql = "SELECT id, user_id, certificate_no, name, parent_name, address, thalook, date, remarks, " +
                    "pdf_path, qr_code, created_at, updated_at " +
                    "FROM " + tableName + " WHERE id = ? AND user_id = ?";
        } else { // Custom
            sql = "SELECT id, user_id, certificate_no, template_name, template_content, field_data, issued_date, " +
                    "pdf_path, qr_code, created_at, updated_at " +
                    "FROM " + tableName + " WHERE id = ? AND user_id = ?";
        }

        var result = db.executeQuery(sql, new Object[] { id, userId }, this::mapResultSet);
        return result.isEmpty() ? null : result.get(0);
    }

    public Long create(Certificate c) {
        String userId = getCurrentUserId();
        String tableName = getTableName(c.getType());
        String sql;
        Object[] params;

        if ("Marriage".equals(c.getType())) {
            sql = "INSERT INTO " + tableName
                    + " (user_id, certificate_no, groom_name, bride_name, parent_name_of_groom, " +
                    "parent_name_of_bride, address_of_groom, address_of_bride, place_of_marriage, marriage_status, " +
                    "marriage_date, additional_notes, supporting_docs_path, pdf_path, qr_code, created_at, updated_at) "
                    +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))";
            params = new Object[] {
                    userId, // CRITICAL: Set user_id for user isolation
                    c.getCertificateNo(), c.getGroomName(), c.getBrideName(), c.getParentNameOfGroom(),
                    c.getParentNameOfBride(), c.getAddressOfGroom(), c.getAddressOfBride(), c.getPlaceOfMarriage(),
                    c.getMarriageStatus(), c.getMarriageDate() != null ? c.getMarriageDate().toString() : null,
                    c.getAdditionalNotes(), c.getSupportingDocsPath(), c.getPdfPath(), c.getQrCode()
            };
        } else if ("Death".equals(c.getType())) {
            sql = "INSERT INTO " + tableName
                    + " (user_id, certificate_no, name, parent_name, address, thalook, date_of_death, " +
                    "cause, place_of_death, issued_date, pdf_path, qr_code, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))";
            params = new Object[] {
                    userId, // CRITICAL: Set user_id for user isolation
                    c.getCertificateNo(), c.getName(), c.getParentName(), c.getAddress(), c.getThalook(),
                    c.getDateOfDeath() != null ? c.getDateOfDeath().toString() : null, c.getCause(),
                    c.getPlaceOfDeath(), c.getIssueDate() != null ? c.getIssueDate().toString() : null,
                    c.getPdfPath(), c.getQrCode()
            };
        } else if ("Jamath".equals(c.getType())) {
            sql = "INSERT INTO " + tableName + " (user_id, certificate_no, name, parent_name, address, thalook, date, "
                    +
                    "remarks, pdf_path, qr_code, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))";
            params = new Object[] {
                    userId, // CRITICAL: Set user_id for user isolation
                    c.getCertificateNo(), c.getName(), c.getParentName(), c.getAddress(), c.getThalook(),
                    c.getIssueDate() != null ? c.getIssueDate().toString() : null, c.getRemarks(),
                    c.getPdfPath(), c.getQrCode()
            };
        } else { // Custom
            sql = "INSERT INTO " + tableName
                    + " (user_id, certificate_no, template_name, template_content, field_data, " +
                    "issued_date, pdf_path, qr_code, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))";
            params = new Object[] {
                    userId, // CRITICAL: Set user_id for user isolation
                    c.getCertificateNo(), c.getTemplateName(), c.getTemplateContent(), c.getFieldData(),
                    c.getIssueDate() != null ? c.getIssueDate().toString() : null,
                    c.getPdfPath(), c.getQrCode()
            };
        }

        Long newId = db.executeInsert(sql, params);

        // Queue for sync if record was created successfully
        if (newId != null) {
            c.setId(newId);
            SyncHelper.queueInsert(tableName, newId, c);
        }

        return newId;
    }

    public boolean update(Certificate c) {
        String userId = getCurrentUserId();
        String tableName = getTableName(c.getType());
        String sql;
        Object[] params;

        if ("Marriage".equals(c.getType())) {
            sql = "UPDATE " + tableName + " SET certificate_no = ?, groom_name = ?, bride_name = ?, " +
                    "parent_name_of_groom = ?, parent_name_of_bride = ?, address_of_groom = ?, address_of_bride = ?, " +
                    "place_of_marriage = ?, marriage_status = ?, marriage_date = ?, additional_notes = ?, " +
                    "supporting_docs_path = ?, pdf_path = ?, qr_code = ?, updated_at = datetime('now') WHERE id = ? AND user_id = ?";
            params = new Object[] {
                    c.getCertificateNo(), c.getGroomName(), c.getBrideName(), c.getParentNameOfGroom(),
                    c.getParentNameOfBride(), c.getAddressOfGroom(), c.getAddressOfBride(), c.getPlaceOfMarriage(),
                    c.getMarriageStatus(), c.getMarriageDate() != null ? c.getMarriageDate().toString() : null,
                    c.getAdditionalNotes(), c.getSupportingDocsPath(), c.getPdfPath(), c.getQrCode(), c.getId(), userId
            };
        } else if ("Death".equals(c.getType())) {
            sql = "UPDATE " + tableName + " SET certificate_no = ?, name = ?, parent_name = ?, address = ?, " +
                    "thalook = ?, date_of_death = ?, cause = ?, place_of_death = ?, issued_date = ?, " +
                    "pdf_path = ?, qr_code = ?, updated_at = datetime('now') WHERE id = ? AND user_id = ?";
            params = new Object[] {
                    c.getCertificateNo(), c.getName(), c.getParentName(), c.getAddress(), c.getThalook(),
                    c.getDateOfDeath() != null ? c.getDateOfDeath().toString() : null, c.getCause(),
                    c.getPlaceOfDeath(), c.getIssueDate() != null ? c.getIssueDate().toString() : null,
                    c.getPdfPath(), c.getQrCode(), c.getId(), userId
            };
        } else if ("Jamath".equals(c.getType())) {
            sql = "UPDATE " + tableName + " SET certificate_no = ?, name = ?, parent_name = ?, address = ?, " +
                    "thalook = ?, date = ?, remarks = ?, pdf_path = ?, qr_code = ?, updated_at = datetime('now') WHERE id = ? AND user_id = ?";
            params = new Object[] {
                    c.getCertificateNo(), c.getName(), c.getParentName(), c.getAddress(), c.getThalook(),
                    c.getIssueDate() != null ? c.getIssueDate().toString() : null, c.getRemarks(),
                    c.getPdfPath(), c.getQrCode(), c.getId(), userId
            };
        } else { // Custom
            sql = "UPDATE " + tableName + " SET certificate_no = ?, template_name = ?, template_content = ?, " +
                    "field_data = ?, issued_date = ?, pdf_path = ?, qr_code = ?, updated_at = datetime('now') WHERE id = ? AND user_id = ?";
            params = new Object[] {
                    c.getCertificateNo(), c.getTemplateName(), c.getTemplateContent(), c.getFieldData(),
                    c.getIssueDate() != null ? c.getIssueDate().toString() : null,
                    c.getPdfPath(), c.getQrCode(), c.getId(), userId
            };
        }

        String tableNameForSync = getTableName(c.getType());
        boolean success = db.executeUpdate(sql, params) > 0;

        // Queue for sync if update was successful
        if (success) {
            SyncHelper.queueUpdate(tableNameForSync, c.getId(), c);
        }

        return success;
    }

    public boolean delete(Long id, String type) {
        String userId = getCurrentUserId();
        String tableName = getTableName(type);
        String sql = "DELETE FROM " + tableName + " WHERE id = ? AND user_id = ?";
        boolean success = db.executeUpdate(sql, new Object[] { id, userId }) > 0;

        // Queue for sync if delete was successful
        if (success) {
            SyncHelper.queueDelete(tableName, id);
        }

        return success;
    }

    private Certificate mapResultSet(ResultSet rs) {
        try {
            Certificate c = new Certificate();
            c.setId(rs.getLong("id"));
            c.setCertificateNo(getString(rs, "certificate_no"));

            // Try to determine type from available columns
            if (hasColumn(rs, "groom_name")) {
                c.setType("Marriage");
                c.setGroomName(getString(rs, "groom_name"));
                c.setBrideName(getString(rs, "bride_name"));
                c.setParentNameOfGroom(getString(rs, "parent_name_of_groom"));
                c.setParentNameOfBride(getString(rs, "parent_name_of_bride"));
                c.setAddressOfGroom(getString(rs, "address_of_groom"));
                c.setAddressOfBride(getString(rs, "address_of_bride"));
                c.setPlaceOfMarriage(getString(rs, "place_of_marriage"));
                c.setMarriageStatus(getString(rs, "marriage_status"));
                // Handle date from SQLite TEXT field - read as string and parse
                try {
                    String dateStr = rs.getString("marriage_date");
                    if (dateStr != null && !dateStr.isEmpty() && !rs.wasNull()) {
                        try {
                            String cleanDateStr = dateStr;
                            if (dateStr.length() > 10) {
                                cleanDateStr = dateStr.substring(0, 10);
                            }
                            c.setMarriageDate(java.time.LocalDate.parse(cleanDateStr));
                        } catch (java.time.format.DateTimeParseException e) {
                            System.err.println("CertificateDAO: Date parse error for marriage_date '" + dateStr + "': "
                                    + e.getMessage());
                        }
                    }
                } catch (SQLException dateEx) {
                    System.err.println("Error reading marriage_date: " + dateEx.getMessage());
                }
                c.setAdditionalNotes(getString(rs, "additional_notes"));
                c.setSupportingDocsPath(getString(rs, "supporting_docs_path"));
            } else if (hasColumn(rs, "date_of_death")) {
                c.setType("Death");
                c.setName(getString(rs, "name"));
                c.setParentName(getString(rs, "parent_name"));
                c.setAddress(getString(rs, "address"));
                c.setThalook(getString(rs, "thalook"));
                // Handle date from SQLite TEXT field
                try {
                    String dateStr = rs.getString("date_of_death");
                    if (dateStr != null && !dateStr.isEmpty() && !rs.wasNull()) {
                        try {
                            String cleanDateStr = dateStr;
                            if (dateStr.length() > 10) {
                                cleanDateStr = dateStr.substring(0, 10);
                            }
                            c.setDateOfDeath(java.time.LocalDate.parse(cleanDateStr));
                        } catch (java.time.format.DateTimeParseException e) {
                            System.err.println("CertificateDAO: Date parse error for date_of_death '" + dateStr + "': "
                                    + e.getMessage());
                        }
                    }
                } catch (SQLException dateEx) {
                    System.err.println("Error reading date_of_death: " + dateEx.getMessage());
                }
                c.setCause(getString(rs, "cause"));
                c.setPlaceOfDeath(getString(rs, "place_of_death"));
                try {
                    String dateStr = rs.getString("issued_date");
                    if (dateStr != null && !dateStr.isEmpty() && !rs.wasNull()) {
                        try {
                            String cleanDateStr = dateStr;
                            if (dateStr.length() > 10) {
                                cleanDateStr = dateStr.substring(0, 10);
                            }
                            c.setIssueDate(java.time.LocalDate.parse(cleanDateStr));
                        } catch (java.time.format.DateTimeParseException e) {
                            System.err.println("CertificateDAO: Date parse error for issued_date '" + dateStr + "': "
                                    + e.getMessage());
                        }
                    }
                } catch (SQLException dateEx) {
                    System.err.println("Error reading issued_date: " + dateEx.getMessage());
                }
            } else if (hasColumn(rs, "template_name")) {
                c.setType("Custom");
                c.setTemplateName(getString(rs, "template_name"));
                c.setTemplateContent(getString(rs, "template_content"));
                c.setFieldData(getString(rs, "field_data"));
                // Handle date from SQLite TEXT field
                try {
                    String dateStr = rs.getString("issued_date");
                    if (dateStr != null && !dateStr.isEmpty() && !rs.wasNull()) {
                        try {
                            String cleanDateStr = dateStr;
                            if (dateStr.length() > 10) {
                                cleanDateStr = dateStr.substring(0, 10);
                            }
                            c.setIssueDate(java.time.LocalDate.parse(cleanDateStr));
                        } catch (java.time.format.DateTimeParseException e) {
                            System.err.println("CertificateDAO: Date parse error for custom issued_date '" + dateStr
                                    + "': " + e.getMessage());
                        }
                    }
                } catch (SQLException dateEx) {
                    System.err.println("Error reading issued_date: " + dateEx.getMessage());
                }
            } else {
                c.setType("Jamath");
                c.setName(getString(rs, "name"));
                c.setParentName(getString(rs, "parent_name"));
                c.setAddress(getString(rs, "address"));
                c.setThalook(getString(rs, "thalook"));
                // Handle date from SQLite TEXT field
                try {
                    String dateStr = rs.getString("date");
                    if (dateStr != null && !dateStr.isEmpty() && !rs.wasNull()) {
                        try {
                            String cleanDateStr = dateStr;
                            if (dateStr.length() > 10) {
                                cleanDateStr = dateStr.substring(0, 10);
                            }
                            c.setIssueDate(java.time.LocalDate.parse(cleanDateStr));
                        } catch (java.time.format.DateTimeParseException e) {
                            System.err.println("CertificateDAO: Date parse error for jamath date '" + dateStr + "': "
                                    + e.getMessage());
                        }
                    }
                } catch (SQLException dateEx) {
                    System.err.println("Error reading date: " + dateEx.getMessage());
                }
                c.setRemarks(getString(rs, "remarks"));
            }

            c.setPdfPath(getString(rs, "pdf_path"));
            c.setQrCode(getString(rs, "qr_code"));

            return c;
        } catch (SQLException e) {
            System.err.println("Map certificate failed: " + e.getMessage());
            return null;
        }
    }

    private String getString(ResultSet rs, String column) {
        try {
            if (hasColumn(rs, column)) {
                return rs.getString(column);
            }
        } catch (SQLException e) {
            // Column doesn't exist
        }
        return null;
    }

    private boolean hasColumn(ResultSet rs, String column) {
        try {
            rs.findColumn(column);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Insert or update a record coming from Supabase sync.
     * Used by SyncManager during pull sync.
     */
    public boolean upsertFromSupabase(Certificate c, String supabaseUpdatedAt) {
        if (c == null || c.getId() == null || c.getType() == null) {
            return false;
        }

        String userId = getCurrentUserId();
        String tableName = getTableName(c.getType());

        // Check if record exists
        boolean exists = false;
        String checkSql = "SELECT id FROM " + tableName + " WHERE id = ? AND user_id = ?";
        List<Object> checkResults = db.executeQuery(checkSql, new Object[] { c.getId(), userId }, rs -> {
            try {
                return rs.getObject("id");
            } catch (SQLException e) {
                return null;
            }
        });
        exists = !checkResults.isEmpty();

        if (!exists) {
            // INSERT
            String sql;
            Object[] params;

            if ("Marriage".equals(c.getType())) {
                sql = "INSERT INTO " + tableName
                        + " (id, user_id, certificate_no, groom_name, bride_name, parent_name_of_groom, " +
                        "parent_name_of_bride, address_of_groom, address_of_bride, place_of_marriage, marriage_status, "
                        +
                        "marriage_date, additional_notes, supporting_docs_path, pdf_path, qr_code, created_at, updated_at) "
                        +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), ?)";
                params = new Object[] {
                        c.getId(), userId,
                        c.getCertificateNo(), c.getGroomName(), c.getBrideName(), c.getParentNameOfGroom(),
                        c.getParentNameOfBride(), c.getAddressOfGroom(), c.getAddressOfBride(), c.getPlaceOfMarriage(),
                        c.getMarriageStatus(), c.getMarriageDate() != null ? c.getMarriageDate().toString() : null,
                        c.getAdditionalNotes(), c.getSupportingDocsPath(), c.getPdfPath(), c.getQrCode(),
                        supabaseUpdatedAt != null ? supabaseUpdatedAt : "datetime('now')"
                };
            } else if ("Death".equals(c.getType())) {
                sql = "INSERT INTO " + tableName
                        + " (id, user_id, certificate_no, name, parent_name, address, thalook, date_of_death, " +
                        "cause, place_of_death, issued_date, pdf_path, qr_code, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), ?)";
                params = new Object[] {
                        c.getId(), userId,
                        c.getCertificateNo(), c.getName(), c.getParentName(), c.getAddress(), c.getThalook(),
                        c.getDateOfDeath() != null ? c.getDateOfDeath().toString() : null, c.getCause(),
                        c.getPlaceOfDeath(), c.getIssueDate() != null ? c.getIssueDate().toString() : null,
                        c.getPdfPath(), c.getQrCode(),
                        supabaseUpdatedAt != null ? supabaseUpdatedAt : "datetime('now')"
                };
            } else if ("Jamath".equals(c.getType())) {
                sql = "INSERT INTO " + tableName
                        + " (id, user_id, certificate_no, name, parent_name, address, thalook, date, " +
                        "remarks, pdf_path, qr_code, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), ?)";
                params = new Object[] {
                        c.getId(), userId,
                        c.getCertificateNo(), c.getName(), c.getParentName(), c.getAddress(), c.getThalook(),
                        c.getIssueDate() != null ? c.getIssueDate().toString() : null, c.getRemarks(),
                        c.getPdfPath(), c.getQrCode(),
                        supabaseUpdatedAt != null ? supabaseUpdatedAt : "datetime('now')"
                };
            } else { // Custom
                sql = "INSERT INTO " + tableName
                        + " (id, user_id, certificate_no, template_name, template_content, field_data, " +
                        "issued_date, pdf_path, qr_code, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), ?)";
                params = new Object[] {
                        c.getId(), userId,
                        c.getCertificateNo(), c.getTemplateName(), c.getTemplateContent(), c.getFieldData(),
                        c.getIssueDate() != null ? c.getIssueDate().toString() : null,
                        c.getPdfPath(), c.getQrCode(),
                        supabaseUpdatedAt != null ? supabaseUpdatedAt : "datetime('now')"
                };
            }

            try {
                int rows = db.executeUpdate(sql, params);
                return rows > 0;
            } catch (Exception e) {
                System.err.println("CertificateDAO.upsertFromSupabase (INSERT): " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        } else {
            // UPDATE
            String sql;
            Object[] params;

            if ("Marriage".equals(c.getType())) {
                sql = "UPDATE " + tableName + " SET certificate_no = ?, groom_name = ?, bride_name = ?, " +
                        "parent_name_of_groom = ?, parent_name_of_bride = ?, address_of_groom = ?, address_of_bride = ?, "
                        +
                        "place_of_marriage = ?, marriage_status = ?, marriage_date = ?, additional_notes = ?, " +
                        "supporting_docs_path = ?, pdf_path = ?, qr_code = ?, updated_at = ? WHERE id = ? AND user_id = ?";
                params = new Object[] {
                        c.getCertificateNo(), c.getGroomName(), c.getBrideName(), c.getParentNameOfGroom(),
                        c.getParentNameOfBride(), c.getAddressOfGroom(), c.getAddressOfBride(), c.getPlaceOfMarriage(),
                        c.getMarriageStatus(), c.getMarriageDate() != null ? c.getMarriageDate().toString() : null,
                        c.getAdditionalNotes(), c.getSupportingDocsPath(), c.getPdfPath(), c.getQrCode(),
                        supabaseUpdatedAt != null ? supabaseUpdatedAt : "datetime('now')",
                        c.getId(), userId
                };
            } else if ("Death".equals(c.getType())) {
                sql = "UPDATE " + tableName + " SET certificate_no = ?, name = ?, parent_name = ?, address = ?, " +
                        "thalook = ?, date_of_death = ?, cause = ?, place_of_death = ?, issued_date = ?, " +
                        "pdf_path = ?, qr_code = ?, updated_at = ? WHERE id = ? AND user_id = ?";
                params = new Object[] {
                        c.getCertificateNo(), c.getName(), c.getParentName(), c.getAddress(), c.getThalook(),
                        c.getDateOfDeath() != null ? c.getDateOfDeath().toString() : null, c.getCause(),
                        c.getPlaceOfDeath(), c.getIssueDate() != null ? c.getIssueDate().toString() : null,
                        c.getPdfPath(), c.getQrCode(),
                        supabaseUpdatedAt != null ? supabaseUpdatedAt : "datetime('now')",
                        c.getId(), userId
                };
            } else if ("Jamath".equals(c.getType())) {
                sql = "UPDATE " + tableName + " SET certificate_no = ?, name = ?, parent_name = ?, address = ?, " +
                        "thalook = ?, date = ?, remarks = ?, pdf_path = ?, qr_code = ?, updated_at = ? WHERE id = ? AND user_id = ?";
                params = new Object[] {
                        c.getCertificateNo(), c.getName(), c.getParentName(), c.getAddress(), c.getThalook(),
                        c.getIssueDate() != null ? c.getIssueDate().toString() : null, c.getRemarks(),
                        c.getPdfPath(), c.getQrCode(),
                        supabaseUpdatedAt != null ? supabaseUpdatedAt : "datetime('now')",
                        c.getId(), userId
                };
            } else { // Custom
                sql = "UPDATE " + tableName + " SET certificate_no = ?, template_name = ?, template_content = ?, " +
                        "field_data = ?, issued_date = ?, pdf_path = ?, qr_code = ?, updated_at = ? WHERE id = ? AND user_id = ?";
                params = new Object[] {
                        c.getCertificateNo(), c.getTemplateName(), c.getTemplateContent(), c.getFieldData(),
                        c.getIssueDate() != null ? c.getIssueDate().toString() : null,
                        c.getPdfPath(), c.getQrCode(),
                        supabaseUpdatedAt != null ? supabaseUpdatedAt : "datetime('now')",
                        c.getId(), userId
                };
            }

            try {
                int rows = db.executeUpdate(sql, params);
                return rows > 0;
            } catch (Exception e) {
                System.err.println("CertificateDAO.upsertFromSupabase (UPDATE): " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }
    }
}
