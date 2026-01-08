package com.mahal.sync;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Service to detect internet connectivity and notify listeners when connectivity changes.
 */
public class ConnectivityService {
    private static ConnectivityService instance;
    private boolean isConnected = false;
    private ScheduledExecutorService scheduler;
    private Consumer<Boolean> connectivityListener;
    private static final String TEST_URL = "https://www.google.com";
    private static final int CHECK_INTERVAL_SECONDS = 30;

    private ConnectivityService() {
        checkConnectivity(); // Initial check
        startPeriodicCheck();
    }

    public static ConnectivityService getInstance() {
        if (instance == null) {
            instance = new ConnectivityService();
        }
        return instance;
    }

    /**
     * Check if internet connection is available.
     */
    public boolean isConnected() {
        return isConnected;
    }

    /**
     * Set a listener to be notified when connectivity changes.
     */
    public void setConnectivityListener(Consumer<Boolean> listener) {
        this.connectivityListener = listener;
    }

    /**
     * Manually check connectivity status.
     */
    public void checkConnectivity() {
        new Thread(() -> {
            boolean previousState = isConnected;
            isConnected = testConnection();
            
            if (previousState != isConnected && connectivityListener != null) {
                connectivityListener.accept(isConnected);
            }
        }).start();
    }

    /**
     * Test internet connection by attempting to connect to a reliable server.
     * Uses longer timeouts for slow connections. Returns true if connection succeeds,
     * but sync will proceed anyway even if this returns false (slow connections may cause false negatives).
     */
    private boolean testConnection() {
        try {
            URL url = new URL(TEST_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(20000); // 20 seconds timeout for slow connections
            connection.setReadTimeout(20000); // 20 seconds read timeout
            connection.connect();
            
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            
            return responseCode == 200;
        } catch (Exception e) {
            // Connectivity test failed - may be due to slow connection
            // Sync will still attempt, and actual sync operations will handle errors appropriately
            return false;
        }
    }

    /**
     * Start periodic connectivity checks.
     */
    private void startPeriodicCheck() {
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(
            this::checkConnectivity,
            CHECK_INTERVAL_SECONDS,
            CHECK_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        );
    }

    /**
     * Shutdown the service.
     */
    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }
}
