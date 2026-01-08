package com.mahal;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import com.mahal.controller.LoginController;
import com.mahal.controller.subscription.SubscriptionController;
import com.mahal.service.SubscriptionService;
import com.mahal.util.SessionManager;
import com.mahal.sync.SyncManager;
import com.mahal.sync.SupabaseConfig;
import com.mahal.sync.SyncHelper;

public class MahalApplication extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            java.io.InputStream iconStream = getClass().getResourceAsStream("/resources/app_icon.png");
            if (iconStream != null) {
                primaryStage.getIcons().add(new javafx.scene.image.Image(iconStream));
            }
        } catch (Exception e) {
            System.err.println("Could not load application icon: " + e.getMessage());
        }

        // 1. Migrate bundled Supabase config to AppData if needed
        migrateSupabaseConfig();

        // 2. Load and initialize Supabase configuration
        SupabaseConfig supabaseConfig = SupabaseConfig.getInstance();
        supabaseConfig.loadConfig(); // Force reload now that migration is done

        if (supabaseConfig.isConfigured()) {
            SyncHelper.configureSupabase(supabaseConfig.getUrl(), supabaseConfig.getApiKey());
            System.out.println("Supabase configuration loaded from properties file.");

            // Pass configuration to backend via system properties before starting it
            System.setProperty("SUPABASE_URL", supabaseConfig.getUrl());
            System.setProperty("SUPABASE_KEY", supabaseConfig.getApiKey());
        } else {
            System.out.println("Supabase not configured. Please configure it in Settings to enable sync.");
        }

        // 3. Initialize sync manager (after config is ready)
        SyncManager.getInstance();

        // 4. Start Spring Boot Backend
        new Thread(() -> {
            try {
                System.out.println("Starting Embedded Spring Boot Backend...");
                com.mahal.MahalBackendApplication.main(new String[0]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        // 5. Perform initial sync in background
        if (supabaseConfig.isConfigured()) {
            System.out.println("Starting initial sync of all existing data to Supabase...");
            new Thread(() -> {
                try {
                    // Wait for backend to be ready
                    com.mahal.service.ApiService.getInstance().waitForServer(30);
                    SyncHelper.performInitialSync();
                } catch (Exception e) {
                    System.err.println("Error during initial sync: " + e.getMessage());
                }
            }).start();
        }

        // Check if user is already logged in
        if (SessionManager.getInstance().isLoggedIn()) {
            // Check subscription status before showing dashboard
            checkSubscriptionAndProceed(primaryStage);
        } else {
            showLoginScreen(primaryStage);
        }
    }

    /**
     * Check subscription status and either show dashboard or subscription screen
     */
    private void checkSubscriptionAndProceed(Stage primaryStage) {
        // Check subscription in background thread
        new Thread(() -> {
            try {
                // Wait for backend to be ready
                com.mahal.service.ApiService.getInstance().waitForServer(30);
                SubscriptionService subscriptionService = SubscriptionService.getInstance();
                SubscriptionService.SubscriptionStatus status = subscriptionService.checkSubscriptionStatus();

                Platform.runLater(() -> {
                    if (status.isActive()) {
                        // Subscription is active - show dashboard
                        showMainDashboard(primaryStage);
                    } else {
                        // Subscription inactive/expired/not found - show subscription screen
                        showSubscriptionScreen(primaryStage);
                    }
                });
            } catch (Exception e) {
                // Error checking subscription - lock the app
                System.err.println("Error checking subscription: " + e.getMessage());
                Platform.runLater(() -> {
                    showSubscriptionScreen(primaryStage);
                });
            }
        }).start();
    }

    /**
     * Show subscription screen with callback to check again after payment
     */
    private void showSubscriptionScreen(Stage primaryStage) {
        SubscriptionController subscriptionController = new SubscriptionController(
                primaryStage,
                () -> {
                    // After successful subscription, check again and show dashboard
                    checkSubscriptionAndProceed(primaryStage);
                });
        subscriptionController.show();
    }

    @Override
    public void stop() {
        // Shutdown sync manager when application closes
        SyncManager.getInstance().shutdown();
    }

    /**
     * One-time migration of supabase.properties from classpath to AppData.
     * This ensures bundled keys are used on first run while allowing user overrides
     * later.
     */
    private void migrateSupabaseConfig() {
        try {
            String appDataPath = System.getProperty("user.home") + "/AppData/Roaming/MahalApp/data/";
            java.io.File targetFile = new java.io.File(appDataPath, "supabase.properties");

            // Only migrate if the file doesn't exist in AppData yet
            if (!targetFile.exists()) {
                java.io.InputStream is = getClass().getResourceAsStream("/" + "supabase.properties");
                if (is != null) {
                    java.io.File dir = new java.io.File(appDataPath);
                    if (!dir.exists())
                        dir.mkdirs();

                    try (java.io.FileOutputStream os = new java.io.FileOutputStream(targetFile)) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                    }
                    System.out.println("Migrated bundled Supabase config to: " + targetFile.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to migrate Supabase config: " + e.getMessage());
        }
    }

    private void showLoginScreen(Stage primaryStage) {
        LoginController loginController = new LoginController();
        loginController.show(primaryStage);
    }

    private void showMainDashboard(Stage primaryStage) {
        com.mahal.controller.DashboardController dashboardController = new com.mahal.controller.DashboardController();
        dashboardController.show(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
