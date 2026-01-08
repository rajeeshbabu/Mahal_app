package com.mahal.sync;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

/**
 * Configuration service for Supabase settings.
 * Stores configuration in a properties file.
 */
public class SupabaseConfig {
    private static SupabaseConfig instance;
    private Properties properties;
    private File configFile;
    private static final String CONFIG_FILE_NAME = "supabase.properties";
    private static final String APP_DATA_PATH = System.getProperty("user.home") + "/AppData/Roaming/MahalApp/data/";

    private SupabaseConfig() {
        this.properties = new Properties();
        this.configFile = new File(APP_DATA_PATH, CONFIG_FILE_NAME);

        // Ensure directory exists
        File dir = new File(APP_DATA_PATH);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        loadConfig();
    }

    public static SupabaseConfig getInstance() {
        if (instance == null) {
            instance = new SupabaseConfig();
        }
        return instance;
    }

    /**
     * Load configuration from file.
     */
    public void loadConfig() {
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                properties.load(reader);
            } catch (IOException e) {
                System.err.println("Error loading Supabase config: " + e.getMessage());
            }
        }
    }

    /**
     * Save configuration to file.
     */
    public void saveConfig() {
        try (FileWriter writer = new FileWriter(configFile)) {
            properties.store(writer, "Supabase Configuration");
        } catch (IOException e) {
            System.err.println("Error saving Supabase config: " + e.getMessage());
        }
    }

    /**
     * Get Supabase URL.
     */
    public String getUrl() {
        return properties.getProperty("supabase.url", "");
    }

    /**
     * Set Supabase URL.
     */
    public void setUrl(String url) {
        properties.setProperty("supabase.url", url);
        saveConfig();
    }

    /**
     * Get Supabase API Key.
     */
    public String getApiKey() {
        return properties.getProperty("supabase.key", "");
    }

    /**
     * Set Supabase API Key.
     */
    public void setApiKey(String key) {
        properties.setProperty("supabase.key", key);
        saveConfig();
    }

    /**
     * Check if Supabase is configured.
     */
    public boolean isConfigured() {
        String url = getUrl();
        String key = getApiKey();
        return url != null && !url.isEmpty() && key != null && !key.isEmpty();
    }
}
