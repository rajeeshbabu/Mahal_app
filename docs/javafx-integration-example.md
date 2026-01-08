# JavaFX Integration Example

## Application Startup (MahalApplication.java)

```java
package com.mahal;

import com.mahal.sync.EnhancedSyncManager;
import com.mahal.sync.JwtUtil;
import com.mahal.util.SessionManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class MahalApplication extends Application {
    
    private EnhancedSyncManager syncManager;
    
    @Override
    public void init() {
        // Initialize sync manager
        syncManager = EnhancedSyncManager.getInstance();
        
        // Configure Supabase
        // TODO: Load from properties file
        syncManager.configure(
            "https://your-project.supabase.co",
            "your-api-key"
        );
    }
    
    @Override
    public void start(Stage primaryStage) {
        // Your UI initialization...
        
        // Set JWT token after login
        // This should be called from LoginController after successful authentication
    }
    
    @Override
    public void stop() {
        // Cleanup sync manager
        if (syncManager != null) {
            syncManager.shutdown();
        }
    }
}
```

## Login Controller Integration

```java
package com.mahal.controller;

import com.mahal.service.ApiService;
import com.mahal.sync.EnhancedSyncManager;
import com.mahal.sync.JwtUtil;
import com.mahal.sync.UserContext;
import com.mahal.util.SessionManager;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

public class LoginController {
    
    @FXML
    private TextField emailField;
    
    @FXML
    private TextField passwordField;
    
    @FXML
    private Button loginButton;
    
    private EnhancedSyncManager syncManager = EnhancedSyncManager.getInstance();
    private ApiService apiService = ApiService.getInstance();
    
    @FXML
    private void handleLogin() {
        String email = emailField.getText();
        String password = passwordField.getText();
        
        // Disable UI during login
        loginButton.setDisable(true);
        
        // Run login in background thread (non-blocking)
        Task<Void> loginTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // Call Spring Boot API to authenticate
                ApiService.ApiResponse response = apiService.post("/auth/login", 
                    new JSONObject()
                        .put("email", email)
                        .put("password", password)
                );
                
                if (response.getStatusCode() == 200) {
                    JSONObject json = new JSONObject(response.getBody());
                    String jwtToken = json.getString("token");
                    String userId = json.getString("userId");
                    
                    // Store in session
                    SessionManager.getInstance().setUser(userId, jwtToken);
                    
                    // Set JWT token in sync manager (extracts user_id)
                    syncManager.setJwtToken(jwtToken);
                    
                    // Verify user_id extraction
                    String extractedUserId = JwtUtil.extractUserId(jwtToken);
                    if (!userId.equals(extractedUserId)) {
                        throw new Exception("User ID mismatch in JWT");
                    }
                    
                    // Trigger initial sync after login
                    syncManager.performSync();
                } else {
                    throw new Exception("Login failed: " + response.getBody());
                }
                
                return null;
            }
            
            @Override
            protected void succeeded() {
                // Navigate to main dashboard
                // SceneManager.switchToDashboard();
            }
            
            @Override
            protected void failed() {
                // Show error message
                // showErrorAlert("Login failed: " + getException().getMessage());
                loginButton.setDisable(false);
            }
        };
        
        // Run task in background thread
        new Thread(loginTask).start();
    }
}
```

## DAO Integration Example (MemberDAO with UUID and Sync)

```java
package com.mahal.database;

import com.mahal.model.Member;
import com.mahal.sync.UuidUtil;
import com.mahal.sync.UserContext;
import com.mahal.sync.SyncHelper;
import java.time.Instant;

public class MemberDAO {
    private DatabaseService dbService;
    
    public MemberDAO() {
        this.dbService = DatabaseService.getInstance();
        createTableIfNotExists();
    }
    
    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS members (" +
                     "id TEXT PRIMARY KEY, " +                    // UUID
                     "user_id TEXT NOT NULL, " +                  // JWT user_id
                     "name TEXT NOT NULL, " +
                     "mobile TEXT, " +
                     // ... other fields
                     "updated_at TEXT NOT NULL, " +               // ISO 8601 UTC
                     "created_at TEXT NOT NULL, " +               // ISO 8601 UTC
                     "is_synced INTEGER DEFAULT 0, " +
                     "sync_version INTEGER DEFAULT 0" +
                     ")";
        dbService.executeUpdate(sql, null);
        
        // Create indexes
        dbService.executeUpdate("CREATE INDEX IF NOT EXISTS idx_members_user_id ON members(user_id)", null);
        dbService.executeUpdate("CREATE INDEX IF NOT EXISTS idx_members_updated_at ON members(updated_at)", null);
    }
    
    /**
     * Create new member with UUID and user_id from context.
     */
    public String create(Member member) {
        String userId = UserContext.getUserId();
        if (userId == null || userId.isEmpty()) {
            throw new IllegalStateException("User ID not available in context");
        }
        
        // Generate UUID for new record
        String id = UuidUtil.generateUuid();
        member.setId(id);
        member.setUserId(userId);
        
        Instant now = Instant.now();
        member.setUpdatedAt(now);
        member.setCreatedAt(now);
        member.setSynced(false); // Not yet synced
        
        String sql = "INSERT INTO members (id, user_id, name, mobile, updated_at, created_at, is_synced, sync_version) " +
                     "VALUES (?, ?, ?, ?, ?, ?, 0, 0)";
        Object[] params = {
            member.getId(),
            member.getUserId(),
            member.getName(),
            member.getMobile(),
            member.getUpdatedAt().toString(), // ISO 8601 UTC
            member.getCreatedAt().toString(), // ISO 8601 UTC
        };
        
        dbService.executeUpdate(sql, params);
        
        // Queue for sync (runs in background thread)
        SyncHelper.queueInsert("members", id, member);
        
        return id;
    }
    
    /**
     * Update member (increments sync_version, updates updated_at).
     */
    public boolean update(Member member) {
        String userId = UserContext.getUserId();
        if (userId == null || userId.isEmpty()) {
            throw new IllegalStateException("User ID not available in context");
        }
        
        // Verify user owns this record
        Member existing = getById(member.getId());
        if (existing == null || !userId.equals(existing.getUserId())) {
            throw new SecurityException("Cannot update record owned by another user");
        }
        
        // Update timestamp and version
        Instant now = Instant.now();
        member.setUpdatedAt(now);
        member.setSyncVersion(existing.getSyncVersion() + 1);
        member.setSynced(false); // Mark as unsynced (needs sync)
        
        String sql = "UPDATE members SET name = ?, mobile = ?, updated_at = ?, " +
                     "sync_version = ?, is_synced = 0 WHERE id = ? AND user_id = ?";
        Object[] params = {
            member.getName(),
            member.getMobile(),
            member.getUpdatedAt().toString(),
            member.getSyncVersion(),
            member.getId(),
            userId
        };
        
        boolean success = dbService.executeUpdate(sql, params) > 0;
        
        if (success) {
            // Queue for sync
            SyncHelper.queueUpdate("members", member.getId(), member);
        }
        
        return success;
    }
    
    /**
     * Get member by ID (only if owned by current user).
     */
    public Member getById(String id) {
        String userId = UserContext.getUserId();
        if (userId == null || userId.isEmpty()) {
            throw new IllegalStateException("User ID not available in context");
        }
        
        String sql = "SELECT * FROM members WHERE id = ? AND user_id = ?";
        var results = dbService.executeQuery(sql, new Object[]{id, userId}, this::mapResultSet);
        return results.isEmpty() ? null : results.get(0);
    }
    
    /**
     * Get all members for current user.
     */
    public List<Member> getAll() {
        String userId = UserContext.getUserId();
        if (userId == null || userId.isEmpty()) {
            throw new IllegalStateException("User ID not available in context");
        }
        
        String sql = "SELECT * FROM members WHERE user_id = ? ORDER BY updated_at DESC";
        return dbService.executeQuery(sql, new Object[]{userId}, this::mapResultSet);
    }
    
    /**
     * Get unsynced members (for push sync).
     */
    public List<Member> getUnsynced() {
        String userId = UserContext.getUserId();
        String sql = "SELECT * FROM members WHERE user_id = ? AND is_synced = 0 ORDER BY updated_at ASC";
        return dbService.executeQuery(sql, new Object[]{userId}, this::mapResultSet);
    }
    
    private Member mapResultSet(ResultSet rs) throws SQLException {
        Member member = new Member();
        member.setId(rs.getString("id"));
        member.setUserId(rs.getString("user_id"));
        member.setName(rs.getString("name"));
        member.setMobile(rs.getString("mobile"));
        // ... map other fields
        
        // Parse timestamps
        member.setUpdatedAt(Instant.parse(rs.getString("updated_at")));
        member.setCreatedAt(Instant.parse(rs.getString("created_at")));
        member.setSynced(rs.getInt("is_synced") == 1);
        member.setSyncVersion(rs.getLong("sync_version"));
        
        return member;
    }
}
```

## Sync Status UI Component

```java
package com.mahal.controller;

import com.mahal.sync.EnhancedSyncManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;

public class SyncStatusController {
    
    @FXML
    private Label syncStatusLabel;
    
    @FXML
    private ProgressIndicator syncProgressIndicator;
    
    private EnhancedSyncManager syncManager = EnhancedSyncManager.getInstance();
    
    @FXML
    private void initialize() {
        // Update UI based on sync status
        updateSyncStatus("Ready");
        
        // TODO: Add listener to sync manager for status updates
        // syncManager.addStatusListener(status -> {
        //     Platform.runLater(() -> {
        //         updateSyncStatus(status);
        //     });
        // });
    }
    
    @FXML
    private void handleManualSync() {
        // Run sync in background
        new Thread(() -> {
            Platform.runLater(() -> {
                updateSyncStatus("Syncing...");
                syncProgressIndicator.setVisible(true);
            });
            
            syncManager.triggerSync();
            
            // TODO: Wait for sync completion
            // In real implementation, use callbacks or listeners
            
            Platform.runLater(() -> {
                updateSyncStatus("Synced");
                syncProgressIndicator.setVisible(false);
            });
        }).start();
    }
    
    private void updateSyncStatus(String status) {
        syncStatusLabel.setText("Sync Status: " + status);
    }
}
```

## Key Integration Points

### 1. Set JWT Token After Login
```java
// After successful login
String jwtToken = loginResponse.getToken();
syncManager.setJwtToken(jwtToken); // Extracts user_id and sets context
```

### 2. All DAO Operations Use UserContext
```java
// Always get user_id from context (not from parameters)
String userId = UserContext.getUserId();
if (userId == null) {
    throw new IllegalStateException("User ID not available");
}
```

### 3. Generate UUIDs for New Records
```java
String id = UuidUtil.generateUuid();
member.setId(id);
```

### 4. Update Timestamps on Changes
```java
member.setUpdatedAt(Instant.now());
member.setSynced(false); // Mark as needing sync
```

### 5. Run Sync in Background Threads
```java
// ✅ CORRECT: Background thread
new Thread(() -> syncManager.performSync()).start();

// ❌ WRONG: UI thread
syncManager.performSync(); // Blocks UI!
```

### 6. Filter All Queries by user_id
```java
String sql = "SELECT * FROM members WHERE user_id = ? AND id = ?";
// Never query without user_id filter
```
