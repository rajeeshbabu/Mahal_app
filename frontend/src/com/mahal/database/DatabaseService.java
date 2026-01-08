package com.mahal.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class DatabaseService {
    // SQLite database file will be created in the project root directory
    // Add busy_timeout to handle database locked errors (waits up to 5000ms before
    // failing)
    // Base directory for application data
    private static final String APP_DATA_PATH = System.getProperty("user.home") + "/AppData/Roaming/MahalApp/data/";
    private static final String DB_FILE_NAME = "mahal_db_v2.db";
    private static final String DB_URL = "jdbc:sqlite:" + APP_DATA_PATH + DB_FILE_NAME + "?busy_timeout=5000";
    private static DatabaseService instance;

    private DatabaseService() {
        // Ensure database directory exists
        ensureDirectoryExists();

        // Load SQLite JDBC driver
        try {
            Class.forName("org.sqlite.JDBC");
            System.out.println("SQLite Driver loaded successfully");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite Driver not found. Please ensure sqlite-jdbc.jar is in the classpath.");
            System.err.println("Error: " + e.getMessage());
        }
    }

    private void ensureDirectoryExists() {
        try {
            java.io.File directory = new java.io.File(APP_DATA_PATH);
            if (!directory.exists()) {
                if (directory.mkdirs()) {
                    System.out.println("Created database directory: " + APP_DATA_PATH);
                }
            }
        } catch (Exception e) {
            System.err.println("Could not create database directory: " + e.getMessage());
        }
    }

    public static DatabaseService getInstance() {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            return false;
        }
    }

    public <T> List<T> executeQuery(String sql, Function<ResultSet, T> mapper) {
        List<T> results = new ArrayList<>();
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                T item = mapper.apply(rs);
                if (item != null) {
                    results.add(item);
                }
            }
        } catch (SQLException e) {
            System.err.println("Query execution failed: " + e.getMessage());
            e.printStackTrace();
        }
        return results;
    }

    public <T> List<T> executeQuery(String sql, Object[] params, Function<ResultSet, T> mapper) {
        List<T> results = new ArrayList<>();
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
            }

            try (ResultSet rs = stmt.executeQuery()) {
                int rowCount = 0;
                while (rs.next()) {
                    rowCount++;
                    try {
                        T item = mapper.apply(rs);
                        if (item != null) {
                            results.add(item);
                        } else {
                            System.err.println("Warning: mapper returned null for row " + rowCount);
                        }
                    } catch (Exception mapperEx) {
                        System.err.println("Error applying mapper to row " + rowCount + ": " + mapperEx.getMessage());
                        mapperEx.printStackTrace();
                    }
                }
                System.out.println("executeQuery: Processed " + rowCount + " rows, added " + results.size() + " items");
            }
        } catch (SQLException e) {
            System.err.println("Query execution failed: " + e.getMessage());
            e.printStackTrace();
        }
        return results;
    }

    public int executeUpdate(String sql, Object[] params) {
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
            }

            return stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Update execution failed: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    public Long executeInsert(String sql, Object[] params) {
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
            }

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                // SQLite-safe way to get generated ID
                try (Statement idStmt = conn.createStatement();
                        ResultSet rs = idStmt.executeQuery("SELECT last_insert_rowid()")) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Insert execution failed: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}
