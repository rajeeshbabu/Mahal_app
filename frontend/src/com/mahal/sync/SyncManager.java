package com.mahal.sync;

import com.mahal.database.*;
import com.mahal.database.MasjidDAO;
import com.mahal.database.CommitteeDAO;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Main sync manager that coordinates synchronization between local SQLite and
 * Supabase.
 * Automatically syncs when internet is available and queues operations when
 * offline.
 */
public class SyncManager {
    private static SyncManager instance;
    private ConnectivityService connectivityService;
    private SupabaseSyncService supabaseService;
    private SyncQueueDAO syncQueueDAO;
    private ScheduledExecutorService scheduler;
    private boolean isSyncing = false;
    private static final int SYNC_INTERVAL_SECONDS = 60; // Sync every minute when online

    private SyncManager() {
        this.connectivityService = ConnectivityService.getInstance();
        this.supabaseService = SupabaseSyncService.getInstance();
        this.syncQueueDAO = new SyncQueueDAO();

        // Listen for connectivity changes
        connectivityService.setConnectivityListener(this::onConnectivityChanged);

        // Start periodic sync
        startPeriodicSync();
    }

    public static SyncManager getInstance() {
        if (instance == null) {
            instance = new SyncManager();
        }
        return instance;
    }

    /**
     * Configure Supabase connection.
     */
    public void configureSupabase(String url, String apiKey) {
        supabaseService.configure(url, apiKey);
    }

    /**
     * Queue an operation for sync (called by DAOs when data changes).
     */
    public void queueOperation(String tableName, String operation, Long recordId, Object data) {
        try {
            // Check if this operation for this record is already in the queue or has been
            // synced
            // We especially want to prevent multiple INSERTs for the same record
            if (recordId != null && "INSERT".equals(operation)) {
                if (syncQueueDAO.isOperationQueued(tableName, operation, recordId)) {
                    // Item already in queue (either pending or synced), don't add another insert
                    return;
                }
            }
            // If data is already a JSONObject, convert it to string directly
            // Otherwise, use JsonUtil.toJson() for other types
            String jsonData;
            if (data instanceof org.json.JSONObject) {
                jsonData = ((org.json.JSONObject) data).toString();
            } else {
                jsonData = JsonUtil.toJson(data);
            }

            // Always ensure user_id is included in the JSON
            // Parse JSON, add user_id if missing, then convert back to string
            String userId = null;
            try {
                org.json.JSONObject json = new org.json.JSONObject(jsonData);

                // For "admins" table, use the user_id from the JSON (admin user_id = admin id)
                // This allows queuing during registration when no user is logged in
                // "subscriptions" table also needs this to create initial pending subscription
                if ("admins".equals(tableName) || "subscriptions".equals(tableName)) {
                    if (json.has("user_id") && !json.isNull("user_id")) {
                        // Use the provided user_id from JSON
                        userId = String.valueOf(json.get("user_id")).trim();
                        System.out.println("Queued sync operation for table: " + tableName + ", operation: " + operation
                                + ", using provided user_id: " + userId);
                    } else {
                        // If no user_id in JSON, try to use the recordId as user_id (for new admins)
                        if (recordId != null) {
                            userId = String.valueOf(recordId).trim();
                            json.put("user_id", userId);
                            System.out.println(
                                    "Queued sync operation for table: " + tableName + ", operation: " + operation
                                            + ", using recordId as user_id: " + userId);
                        }
                    }
                } else {
                    // For other tables, require a logged-in user
                    com.mahal.util.SessionManager sessionManager = com.mahal.util.SessionManager.getInstance();
                    com.mahal.model.User currentUser = sessionManager.getCurrentUser();

                    if (currentUser == null || currentUser.getId() == null) {
                        System.err.println(
                                "Warning: Cannot queue sync operation for " + tableName + " - no user logged in");
                        return; // Don't queue if no user context
                    }

                    // Ensure user_id is always a string (handle Long, Integer, String, etc.)
                    userId = String.valueOf(currentUser.getId()).trim();
                    if (userId == null || userId.isEmpty() || "null".equals(userId)) {
                        System.err.println("ERROR: Invalid user_id from session: " + currentUser.getId());
                        return;
                    }
                    // Always set user_id as string - this ensures it's present even if model
                    // doesn't have the field
                    json.put("user_id", userId);
                    System.out.println("Queued sync operation for table: " + tableName + ", operation: " + operation
                            + ", user_id: " + userId);
                }
                jsonData = json.toString();
            } catch (Exception e) {
                System.err.println("Error: Could not add user_id to JSON: " + e.getMessage());
                e.printStackTrace();
                // For admins and subscriptions, we can still try to queue if we have recordId
                if (("admins".equals(tableName) || "subscriptions".equals(tableName)) && recordId != null) {
                    try {
                        org.json.JSONObject json = new org.json.JSONObject(jsonData);
                        json.put("user_id", String.valueOf(recordId));
                        jsonData = json.toString();
                        userId = String.valueOf(recordId);
                        System.out.println(
                                "Queued sync operation for " + tableName + " with recordId as user_id: " + userId);
                    } catch (Exception e2) {
                        System.err.println("ERROR: Could not add user_id to admin JSON even with recordId");
                        return; // Can't queue without user_id
                    }
                } else {
                    System.err.println("ERROR: Cannot queue sync operation without user_id");
                    return; // Can't queue without user_id
                }
            }

            // Final check - ensure we have a valid userId
            if (userId == null || userId.isEmpty() || "null".equals(userId)) {
                System.err.println("ERROR: No valid user_id available for queueing sync operation");
                return;
            }

            syncQueueDAO.queueOperation(tableName, operation, recordId, jsonData);

            // Automatically trigger sync if online and configured
            // Use a small delay to batch multiple rapid operations
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(500); // Wait 500ms for potential batch operations
                        if (supabaseService.isConfigured() && !isSyncing) {
                            // Try to sync even if connectivity check fails (slow connections may work)
                            boolean isConnected = connectivityService.isConnected();
                            if (!isConnected) {
                                System.out.println(
                                        "Connectivity check failed, but attempting sync anyway (may work with slow connection)...");
                            }
                            syncPendingOperations();
                        } else {
                            if (!supabaseService.isConfigured()) {
                                System.out.println(
                                        "Sync queued but Supabase not configured. Sync will happen when Supabase is configured.");
                            } else if (isSyncing) {
                                System.out.println(
                                        "Sync queued but sync already in progress. Will be processed shortly.");
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }).start();
        } catch (Exception e) {
            System.err.println("Error queueing sync operation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle connectivity changes.
     * Automatically syncs when connection is restored.
     */
    private void onConnectivityChanged(boolean isConnected) {
        if (isConnected && supabaseService.isConfigured()) {
            System.out.println("Internet connection restored. Automatically starting sync...");
            // Small delay to ensure connection is stable
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    if (!isSyncing) {
                        syncPendingOperations();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        } else if (!isConnected) {
            System.out.println(
                    "Internet connection lost. Operations will be automatically synced when connection is restored.");
        }
    }

    /**
     * Start periodic sync when online.
     * Automatically syncs every 60 seconds when conditions are met.
     */
    private void startPeriodicSync() {
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(
                () -> {
                    // Proceed with sync if configured, even if connectivity check fails
                    // (slow connections may cause false negatives, actual sync will handle errors)
                    if (!isSyncing && supabaseService.isConfigured()) {
                        List<SyncOperation> pendingOps = syncQueueDAO.getPendingOperations();
                        if (!pendingOps.isEmpty()) {
                            boolean isConnected = connectivityService.isConnected();
                            if (!isConnected) {
                                System.out.println(
                                        "Periodic sync: Connectivity check failed, but attempting sync anyway (slow connection may work)...");
                            }
                            System.out.println("Periodic sync: Found " + pendingOps.size()
                                    + " pending operations. Starting automatic sync...");
                            syncPendingOperations();
                            // TODO: Also sync down from Supabase periodically to get remote changes
                            // syncDownFromSupabase();
                        }
                    }
                },
                SYNC_INTERVAL_SECONDS,
                SYNC_INTERVAL_SECONDS,
                TimeUnit.SECONDS);
        System.out.println(
                "Automatic sync service started. Will sync every " + SYNC_INTERVAL_SECONDS + " seconds when online.");
    }

    /**
     * Sync all pending operations.
     */
    public void syncPendingOperations() {
        if (isSyncing) {
            System.out.println("Sync already in progress, skipping...");
            return; // Already syncing
        }

        boolean isConfigured = supabaseService.isConfigured();

        if (!isConfigured) {
            System.out.println("Cannot sync: Supabase not configured");
            return; // Supabase not configured
        }

        // Note: We proceed even if connectivity check fails, as slow connections
        // may cause false negatives. The actual sync operations will fail gracefully
        // if there's truly no connection.
        boolean isConnected = connectivityService.isConnected();
        if (!isConnected) {
            System.out.println(
                    "Warning: Connectivity check failed, but attempting sync anyway (may work with slow connection)...");
        }

        isSyncing = true;

        new Thread(() -> {
            try {
                List<SyncOperation> pendingOps = syncQueueDAO.getPendingOperations();
                System.out.println("Syncing " + pendingOps.size() + " pending operations...");

                for (SyncOperation op : pendingOps) {
                    syncQueueDAO.markAsSyncing(op.getId());

                    // Extract user_id from JSON data
                    String userId = extractUserIdFromJson(op.getData());
                    if (userId == null || userId.isEmpty()) {
                        System.err.println("ERROR: No user_id found in sync operation data for table: "
                                + op.getTableName() + ", operation: " + op.getOperation() + ", ID: " + op.getId());
                        System.err.println(
                                "JSON data: " + op.getData().substring(0, Math.min(200, op.getData().length())));
                        syncQueueDAO.markAsFailed(op.getId());
                        continue;
                    }

                    boolean success = false;
                    try {
                        System.out.println("Syncing " + op.getOperation() + " operation for table: " + op.getTableName()
                                + ", record ID: " + op.getRecordId() + ", user_id: " + userId);
                        switch (op.getOperation()) {
                            case "INSERT":
                                success = supabaseService.insert(op.getTableName(), op.getData(), userId);
                                break;
                            case "UPDATE":
                                success = supabaseService.update(op.getTableName(), String.valueOf(op.getRecordId()),
                                        op.getData(),
                                        userId);
                                break;
                            case "DELETE":
                                success = supabaseService.delete(op.getTableName(), String.valueOf(op.getRecordId()),
                                        userId);
                                break;
                        }

                        if (success) {
                            syncQueueDAO.markAsSynced(op.getId());
                            System.out.println("✓ Successfully synced " + op.getOperation() +
                                    " operation for " + op.getTableName() + " (ID: " + op.getRecordId() + ", user_id: "
                                    + userId + ")");
                        } else {
                            syncQueueDAO.markAsFailed(op.getId());
                            System.err.println(
                                    "✗ Failed to sync operation " + op.getId() + " for table: " + op.getTableName() +
                                            ", record ID: " + op.getRecordId() + ", user_id: " + userId);
                            System.err.println("  Check console logs above for detailed error messages.");
                        }
                    } catch (Exception e) {
                        syncQueueDAO.markAsFailed(op.getId());
                        System.err.println("Error syncing operation " + op.getId() + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                // Cleanup old synced operations
                syncQueueDAO.cleanupOldSyncedOperations();

            } finally {
                isSyncing = false;
            }
        }).start();
    }

    /**
     * Manually trigger sync (can be called from UI).
     * Forces connectivity check and immediately starts sync.
     */
    public void triggerSync() {
        // Force connectivity check first
        connectivityService.checkConnectivity();

        // Trigger sync immediately (it will check connectivity again inside)
        System.out.println("Manual sync triggered. Checking connectivity and starting sync...");
        syncPendingOperations();
    }

    /**
     * Perform initial sync of all existing data.
     * This queues all existing records from the local database for sync to
     * Supabase.
     * Only syncs records for the currently logged-in user.
     */
    public void performInitialSync() {
        if (!supabaseService.isConfigured()) {
            System.err.println("Cannot perform initial sync: Supabase not configured");
            return;
        }

        // Get current user - only sync current user's data
        com.mahal.util.SessionManager sessionManager = com.mahal.util.SessionManager.getInstance();
        com.mahal.model.User currentUser = sessionManager.getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            System.err.println("Cannot perform initial sync: No user logged in");
            return;
        }

        final String userId = String.valueOf(currentUser.getId()).trim();
        System.out.println("========================================");
        System.out
                .println("Starting initial sync for user_id: " + userId + " (User: " + currentUser.getFullName() + ")");
        System.out.println("This will sync all existing records for this user to Supabase.");
        System.out.println("========================================");

        new Thread(() -> {
            try {
                // Clear old pending/failed queue entries that may have incorrect JSON
                // (e.g., from previous code versions that skipped required fields)
                System.out.println("Clearing old sync queue entries to re-queue with correct field mappings...");
                syncQueueDAO.clearPendingAndFailedOperations();

                System.out.println("Starting initial sync of existing data...");
                int totalQueued = 0;

                // Sync Incomes
                IncomeDAO incomeDAO = new IncomeDAO();
                List<com.mahal.model.Income> incomes = incomeDAO.getAll();
                for (com.mahal.model.Income income : incomes) {
                    if (income.getId() != null) {
                        queueOperation("incomes", "INSERT", income.getId(), income);
                        totalQueued++;
                    }
                }
                System.out.println("Queued " + incomes.size() + " income records for sync");

                // Sync Expenses
                ExpenseDAO expenseDAO = new ExpenseDAO();
                List<com.mahal.model.Expense> expenses = expenseDAO.getAll();
                for (com.mahal.model.Expense expense : expenses) {
                    if (expense.getId() != null) {
                        queueOperation("expenses", "INSERT", expense.getId(), expense);
                        totalQueued++;
                    }
                }
                System.out.println("Queued " + expenses.size() + " expense records for sync");

                // Sync Members
                MemberDAO memberDAO = new MemberDAO();
                List<com.mahal.model.Member> members = memberDAO.getAll();
                for (com.mahal.model.Member member : members) {
                    if (member.getId() != null) {
                        queueOperation("members", "INSERT", member.getId(), member);
                        totalQueued++;
                    }
                }
                System.out.println("Queued " + members.size() + " member records for sync");

                // Sync Due Collections
                DueCollectionDAO dueCollectionDAO = new DueCollectionDAO();
                List<com.mahal.model.DueCollection> collections = dueCollectionDAO.getAll();
                for (com.mahal.model.DueCollection collection : collections) {
                    if (collection.getId() != null) {
                        queueOperation("due_collections", "INSERT", collection.getId(), collection);
                        totalQueued++;
                    }
                }
                System.out.println("Queued " + collections.size() + " due collection records for sync");

                // Sync Inventory Items
                InventoryItemDAO inventoryItemDAO = new InventoryItemDAO();
                List<com.mahal.model.InventoryItem> items = inventoryItemDAO.getAll();
                for (com.mahal.model.InventoryItem item : items) {
                    if (item.getId() != null) {
                        queueOperation("inventory_items", "INSERT", item.getId(), item);
                        totalQueued++;
                    }
                }
                System.out.println("Queued " + items.size() + " inventory item records for sync");

                // Sync Damaged Items
                DamagedItemDAO damagedItemDAO = new DamagedItemDAO();
                List<com.mahal.model.DamagedItem> damagedItems = damagedItemDAO.getAll();
                for (com.mahal.model.DamagedItem damaged : damagedItems) {
                    if (damaged.getId() != null) {
                        queueOperation("damaged_items", "INSERT", damaged.getId(), damaged);
                        totalQueued++;
                    }
                }
                System.out.println("Queued " + damagedItems.size() + " damaged item records for sync");

                // Sync Rent Items
                RentItemDAO rentItemDAO = new RentItemDAO();
                List<com.mahal.model.RentItem> rentItems = rentItemDAO.getAll();
                for (com.mahal.model.RentItem rentItem : rentItems) {
                    if (rentItem.getId() != null) {
                        queueOperation("rent_items", "INSERT", rentItem.getId(), rentItem);
                        totalQueued++;
                    }
                }
                System.out.println("Queued " + rentItems.size() + " rent item records for sync");

                // Sync Rents
                RentDAO rentDAO = new RentDAO();
                List<com.mahal.model.Rent> rents = rentDAO.getAll();
                for (com.mahal.model.Rent rent : rents) {
                    if (rent.getId() != null) {
                        queueOperation("rents", "INSERT", rent.getId(), rent);
                        totalQueued++;
                    }
                }
                System.out.println("Queued " + rents.size() + " rent records for sync");

                // Sync Events
                EventDAO eventDAO = new EventDAO();
                List<com.mahal.model.Event> events = eventDAO.getAll();
                for (com.mahal.model.Event event : events) {
                    if (event.getId() != null) {
                        queueOperation("events", "INSERT", event.getId(), event);
                        totalQueued++;
                    }
                }
                System.out.println("Queued " + events.size() + " event records for sync");

                // Sync Masjids
                MasjidDAO masjidDAO = new MasjidDAO();
                List<com.mahal.model.Masjid> masjids = masjidDAO.getAll();
                for (com.mahal.model.Masjid masjid : masjids) {
                    if (masjid.getId() != null) {
                        queueOperation("masjids", "INSERT", masjid.getId(), masjid);
                        totalQueued++;
                    }
                }
                System.out.println("Queued " + masjids.size() + " masjid records for sync");

                // Sync Staff
                StaffDAO staffDAO = new StaffDAO();
                List<com.mahal.model.Staff> staff = staffDAO.getAll();
                for (com.mahal.model.Staff s : staff) {
                    if (s.getId() != null) {
                        queueOperation("staff", "INSERT", s.getId(), s);
                        totalQueued++;
                    }
                }
                System.out.println("Queued " + staff.size() + " staff records for sync");

                // Sync Staff Salaries
                StaffSalaryDAO staffSalaryDAO = new StaffSalaryDAO();
                List<com.mahal.model.StaffSalary> salaries = staffSalaryDAO.getAll();
                for (com.mahal.model.StaffSalary salary : salaries) {
                    if (salary.getId() != null) {
                        queueOperation("staff_salaries", "INSERT", salary.getId(), salary);
                        totalQueued++;
                    }
                }
                System.out.println("Queued " + salaries.size() + " staff salary records for sync");

                // Sync Committees
                CommitteeDAO committeeDAO = new CommitteeDAO();
                List<com.mahal.model.Committee> committees = committeeDAO.getAll();
                for (com.mahal.model.Committee committee : committees) {
                    if (committee.getId() != null) {
                        queueOperation("committees", "INSERT", committee.getId(), committee);
                        totalQueued++;
                    }
                }
                System.out.println("Queued " + committees.size() + " committee records for sync");

                // Sync Admins
                com.mahal.database.AdminDAO adminDAO = new com.mahal.database.AdminDAO();
                List<org.json.JSONObject> adminJsons = adminDAO.getAllAsJson();
                for (org.json.JSONObject adminJson : adminJsons) {
                    Long id = adminJson.getLong("id");
                    queueOperation("admins", "INSERT", id, adminJson);
                    totalQueued++;
                }
                System.out.println("Queued " + adminJsons.size() + " admin records for sync");

                // Sync Houses
                HouseDAO houseDAO = new HouseDAO();
                List<com.mahal.model.House> houses = houseDAO.getAll();
                for (com.mahal.model.House house : houses) {
                    if (house.getId() != null) {
                        queueOperation("houses", "INSERT", house.getId(), house);
                        totalQueued++;
                    }
                }
                System.out.println("Queued " + houses.size() + " house records for sync");

                // Sync Income Types
                IncomeTypeDAO incomeTypeDAO = new IncomeTypeDAO();
                List<com.mahal.model.IncomeType> incomeTypes = incomeTypeDAO.getAll();
                for (com.mahal.model.IncomeType incomeType : incomeTypes) {
                    if (incomeType.getId() != null) {
                        queueOperation("income_types", "INSERT", incomeType.getId(), incomeType);
                        totalQueued++;
                    }
                }
                System.out.println("Queued " + incomeTypes.size() + " income type records for sync");

                // Sync Due Types
                DueTypeDAO dueTypeDAO = new DueTypeDAO();
                List<com.mahal.model.DueType> dueTypes = dueTypeDAO.getAll();
                for (com.mahal.model.DueType dueType : dueTypes) {
                    if (dueType.getId() != null) {
                        queueOperation("due_types", "INSERT", dueType.getId(), dueType);
                        totalQueued++;
                    }
                }
                System.out.println("Queued " + dueTypes.size() + " due type records for sync");

                // Note: Prayer Times are NOT synced to Supabase (not needed)

                // Sync Certificates (all types)
                CertificateDAO certificateDAO = new CertificateDAO();
                // Marriage certificates
                List<com.mahal.model.Certificate> marriageCerts = certificateDAO.getByType("Marriage");
                for (com.mahal.model.Certificate cert : marriageCerts) {
                    if (cert.getId() != null) {
                        queueOperation("marriage_certificates", "INSERT", cert.getId(), cert);
                        totalQueued++;
                    }
                }
                System.out.println("Queued " + marriageCerts.size() + " marriage certificate records for sync");

                // Death certificates
                List<com.mahal.model.Certificate> deathCerts = certificateDAO.getByType("Death");
                for (com.mahal.model.Certificate cert : deathCerts) {
                    if (cert.getId() != null) {
                        queueOperation("death_certificates", "INSERT", cert.getId(), cert);
                        totalQueued++;
                    }
                }
                System.out.println("Queued " + deathCerts.size() + " death certificate records for sync");

                // Jamath certificates
                List<com.mahal.model.Certificate> jamathCerts = certificateDAO.getByType("Jamath");
                for (com.mahal.model.Certificate cert : jamathCerts) {
                    if (cert.getId() != null) {
                        queueOperation("jamath_certificates", "INSERT", cert.getId(), cert);
                        totalQueued++;
                    }
                }
                System.out.println("Queued " + jamathCerts.size() + " jamath certificate records for sync");

                // Custom certificates
                List<com.mahal.model.Certificate> customCerts = certificateDAO.getByType("Custom");
                for (com.mahal.model.Certificate cert : customCerts) {
                    if (cert.getId() != null) {
                        queueOperation("custom_certificates", "INSERT", cert.getId(), cert);
                        totalQueued++;
                    }
                }
                System.out.println("Queued " + customCerts.size() + " custom certificate records for sync");

                System.out.println("Initial sync complete. Queued " + totalQueued + " total records for sync.");

                // Force connectivity check and wait for it to complete
                connectivityService.checkConnectivity();
                try {
                    Thread.sleep(2000); // Wait 2 seconds for connectivity check
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // Automatically trigger sync if online and configured
                boolean isConnected = connectivityService.isConnected();
                boolean isConfigured = supabaseService.isConfigured();
                System.out.println("Connectivity check: isConnected=" + isConnected + ", isConfigured=" + isConfigured);

                if (isConnected && isConfigured) {
                    System.out.println("Automatically starting sync of queued operations...");
                    syncPendingOperations();
                    // TODO: Also sync down from Supabase to get any remote changes
                    // System.out.println("Syncing changes from Supabase to local database...");
                    // syncDownFromSupabase();
                } else {
                    System.out.println("Sync will automatically start when internet connection is available.");
                    if (!isConnected) {
                        System.out.println(
                                "  - Waiting for internet connection. Periodic sync will attempt every 60 seconds.");
                    }
                    if (!isConfigured) {
                        System.out.println("  - Supabase not configured.");
                    }
                }

            } catch (Exception e) {
                System.err.println("Error during initial sync: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Shutdown the sync manager.
     */
    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
        connectivityService.shutdown();
    }

    /**
     * Extract user_id from JSON data.
     * Returns the user_id as a string, handling both string and number formats.
     */
    private String extractUserIdFromJson(String jsonData) {
        try {
            org.json.JSONObject json = new org.json.JSONObject(jsonData);
            if (json.has("user_id")) {
                Object userIdObj = json.get("user_id");
                // Handle both string and number formats
                String userId = String.valueOf(userIdObj).trim();
                if (!userId.isEmpty() && !"null".equals(userId)) {
                    return userId;
                }
            }
        } catch (Exception e) {
            System.err.println("Error extracting user_id from JSON: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Download all data for the user from Supabase and store locally.
     * Called after login.
     */
    public void syncDownAll(String userId) {
        if (!supabaseService.isConfigured()) {
            System.out.println("Supabase not configured, skipping initial download.");
            return;
        }

        System.out.println("⬇️ Starting full data download for user: " + userId);

        // Order matters for foreign keys: Types first, then entities, then transactions
        String[] tables = {
                "masjids", "committees", "income_types", "due_types",
                "staff", "members", "houses", "rents",
                "incomes", "expenses", "due_collections",
                "inventory_items", "events"
        };

        for (String table : tables) {
            try {
                System.out.println("Fetching data for table: " + table + "...");
                String response = supabaseService.fetch(table, "user_id=eq." + userId);

                if (response != null && !response.isEmpty() && !response.equals("[]")) {
                    org.json.JSONArray records = new org.json.JSONArray(response);
                    System.out.println("   - Found " + records.length() + " records for " + table);

                    int successCount = 0;
                    for (int i = 0; i < records.length(); i++) {
                        org.json.JSONObject record = records.getJSONObject(i);
                        if (upsertRecord(table, record)) {
                            successCount++;
                        }
                    }
                    System.out.println("   - Upserted " + successCount + "/" + records.length() + " records locally.");
                } else {
                    System.out.println("   - No remote data found for " + table);
                }
            } catch (Exception e) {
                System.err.println("   - Error fetching/saving " + table + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        System.out.println("✅ Full data download completed.");
    }

    /**
     * Upsert a record into local SQLite (Insert or Update).
     * constructs SQL dynamically based on JSON keys.
     */
    private boolean upsertRecord(String tableName, org.json.JSONObject record) {
        try {
            Long id = record.optLong("id", -1);
            if (id == -1)
                return false;

            DatabaseService db = DatabaseService.getInstance();

            // 1. Check if exists
            boolean exists = false;
            try {
                List<Integer> result = db.executeQuery("SELECT 1 FROM " + tableName + " WHERE id = ?",
                        new Object[] { id }, rs -> 1);
                exists = !result.isEmpty();
            } catch (Exception e) {
                // Table might not exist or error
                System.err.println("Error checking existence in " + tableName + ": " + e.getMessage());
                return false;
            }

            // 2. Construct Query
            StringBuilder sql = new StringBuilder();
            java.util.List<Object> params = new java.util.ArrayList<>();
            java.util.Iterator<String> keys = record.keys();

            if (exists) {
                // UPDATE
                sql.append("UPDATE ").append(tableName).append(" SET ");
                boolean first = true;
                while (keys.hasNext()) {
                    String key = keys.next();
                    if (key.equals("id"))
                        continue; // Don't update ID

                    if (!first)
                        sql.append(", ");
                    sql.append(key).append(" = ?");
                    params.add(record.get(key));
                    first = false;
                }
                sql.append(" WHERE id = ?");
                params.add(id);
            } else {
                // INSERT
                sql.append("INSERT INTO ").append(tableName).append(" (");
                StringBuilder values = new StringBuilder();
                boolean first = true;
                while (keys.hasNext()) {
                    String key = keys.next();
                    if (!first) {
                        sql.append(", ");
                        values.append(", ");
                    }
                    sql.append(key);
                    values.append("?");
                    params.add(record.get(key));
                    first = false;
                }
                sql.append(") VALUES (").append(values).append(")");
            }

            // 3. Execute
            int rows = db.executeUpdate(sql.toString(), params.toArray());
            return rows > 0;

        } catch (Exception e) {
            System.err.println("Local upsert failed for " + tableName + ": " + e.getMessage());
            return false;
        }
    }

}
