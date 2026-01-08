package com.mahal.controller.settings;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import com.mahal.sync.SupabaseConfig;
import com.mahal.sync.SyncHelper;
import com.mahal.sync.ConnectivityService;
import com.mahal.util.FormStyler;
import com.mahal.util.StyleHelper;
import com.mahal.service.SubscriptionService;
import javafx.application.Platform;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class SettingsController {
    private VBox view;
    private SupabaseConfig supabaseConfig;
    private ConnectivityService connectivityService;
    private SubscriptionService subscriptionService;

    // Form fields
    private TextField supabaseUrlField;
    private PasswordField supabaseKeyField;
    private Label connectionStatusLabel;
    private Label syncStatusLabel;

    // Subscription fields
    private Label subscriptionStatusLabel;
    private Label subscriptionPlanLabel;
    private Label subscriptionEndDateLabel;

    public SettingsController() {
        this.supabaseConfig = SupabaseConfig.getInstance();
        this.connectivityService = ConnectivityService.getInstance();
        this.subscriptionService = SubscriptionService.getInstance();
        createView();
    }

    public VBox getView() {
        return view;
    }

    private void createView() {
        view = new VBox(20);
        view.setPadding(new Insets(20));
        view.setStyle("-fx-background-color: " + StyleHelper.BG_GRAY_50 + ";");

        Label titleLabel = new Label("Application Settings");
        titleLabel.setStyle(StyleHelper.getTitleStyle());

        // Subscription Information Section
        VBox subscriptionSection = createSubscriptionSection();

        // Supabase Configuration Section
        VBox supabaseSection = createSupabaseSection();

        // Sync Status Section
        VBox syncStatusSection = createSyncStatusSection();

        view.getChildren().addAll(titleLabel, subscriptionSection, supabaseSection, syncStatusSection);

        // Update status periodically
        updateSubscriptionInfo();
        updateStatusLabels();
    }

    private VBox createSupabaseSection() {
        VBox section = new VBox(16);
        section.setPadding(new Insets(20));
        section.setStyle(StyleHelper.getCardStyle());

        Label sectionTitle = new Label("Supabase Cloud Sync Configuration");
        sectionTitle.setStyle(
                "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + StyleHelper.TEXT_GRAY_900 + ";");

        Label description = new Label("Configure your Supabase connection to enable automatic cloud synchronization. " +
                "Once configured, all data changes will automatically sync to Supabase in the background " +
                "when internet is available. No manual intervention required.");
        description.setStyle("-fx-font-size: 12px; -fx-text-fill: " + StyleHelper.TEXT_GRAY_700 + ";");
        description.setWrapText(true);

        // Supabase URL field
        supabaseUrlField = new TextField();
        supabaseUrlField.setPromptText("https://your-project.supabase.co");
        supabaseUrlField.setText(supabaseConfig.getUrl());
        supabaseUrlField.setPrefHeight(36);
        supabaseUrlField.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 8; -fx-border-radius: 8; " +
                "-fx-border-color: #d1d5db; -fx-border-width: 1; -fx-padding: 8 12; -fx-font-size: 13px;");

        // Supabase API Key field
        supabaseKeyField = new PasswordField();
        supabaseKeyField.setPromptText("Enter your Supabase anon/public API key");
        supabaseKeyField.setText(supabaseConfig.getApiKey());
        supabaseKeyField.setPrefHeight(36);
        supabaseKeyField.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 8; -fx-border-radius: 8; " +
                "-fx-border-color: #d1d5db; -fx-border-width: 1; -fx-padding: 8 12; -fx-font-size: 13px;");

        // Save button
        Button saveBtn = new Button("Save Configuration");
        saveBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 8; " +
                "-fx-font-weight: 600; -fx-font-size: 13px; -fx-padding: 10 20; -fx-cursor: hand;");
        saveBtn.setOnMouseEntered(e -> saveBtn
                .setStyle("-fx-background-color: #1d4ed8; -fx-text-fill: white; -fx-background-radius: 8; " +
                        "-fx-font-weight: 600; -fx-font-size: 13px; -fx-padding: 10 20; -fx-cursor: hand;"));
        saveBtn.setOnMouseExited(e -> saveBtn
                .setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 8; " +
                        "-fx-font-weight: 600; -fx-font-size: 13px; -fx-padding: 10 20; -fx-cursor: hand;"));
        saveBtn.setOnAction(e -> saveSupabaseConfig());

        HBox buttonBox = new HBox(10);
        buttonBox.getChildren().add(saveBtn);

        VBox urlBox = FormStyler.createCompactFormField("Supabase URL", supabaseUrlField);
        VBox keyBox = FormStyler.createCompactFormField("Supabase API Key", supabaseKeyField);

        section.getChildren().addAll(sectionTitle, description, urlBox, keyBox, buttonBox);

        return section;
    }

    private VBox createSyncStatusSection() {
        VBox section = new VBox(16);
        section.setPadding(new Insets(20));
        section.setStyle(StyleHelper.getCardStyle());

        Label sectionTitle = new Label("Automatic Sync Status");
        sectionTitle.setStyle(
                "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + StyleHelper.TEXT_GRAY_900 + ";");

        Label description = new Label(
                "Sync happens automatically in the background. All data changes are automatically " +
                        "synced to Supabase when internet is available. Operations are queued when offline " +
                        "and synced when connection is restored.");
        description.setStyle("-fx-font-size: 12px; -fx-text-fill: " + StyleHelper.TEXT_GRAY_700 + ";");
        description.setWrapText(true);

        // Connection status
        connectionStatusLabel = new Label();
        connectionStatusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 500;");

        // Sync status
        syncStatusLabel = new Label();
        syncStatusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 500;");

        section.getChildren().addAll(sectionTitle, description, connectionStatusLabel, syncStatusLabel);

        return section;
    }

    private void saveSupabaseConfig() {
        String url = supabaseUrlField.getText().trim();
        String key = supabaseKeyField.getText().trim();

        if (url.isEmpty() || key.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Invalid Configuration");
            alert.setHeaderText("Missing Information");
            alert.setContentText("Please provide both Supabase URL and API Key.");
            alert.showAndWait();
            return;
        }

        // Save configuration
        supabaseConfig.setUrl(url);
        supabaseConfig.setApiKey(key);
        SyncHelper.configureSupabase(url, key);

        // Show success message
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Configuration Saved");
        alert.setHeaderText("Success");
        alert.setContentText("Supabase configuration has been saved. " +
                "Automatic sync is now enabled. All data changes will sync automatically in the background.");
        alert.showAndWait();

        updateStatusLabels();
    }

    private VBox createSubscriptionSection() {
        VBox section = new VBox(16);
        section.setPadding(new Insets(20));
        section.setStyle(StyleHelper.getCardStyle());

        Label sectionTitle = new Label("Subscription Information");
        sectionTitle.setStyle(
                "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + StyleHelper.TEXT_GRAY_900 + ";");

        Label description = new Label("View your current subscription status and expiration date.");
        description.setStyle("-fx-font-size: 12px; -fx-text-fill: " + StyleHelper.TEXT_GRAY_700 + ";");
        description.setWrapText(true);

        // Subscription status
        subscriptionStatusLabel = new Label("Loading...");
        subscriptionStatusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 500;");

        // Subscription plan
        subscriptionPlanLabel = new Label();
        subscriptionPlanLabel.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: " + StyleHelper.TEXT_GRAY_700 + ";");

        // Subscription end date
        subscriptionEndDateLabel = new Label();
        subscriptionEndDateLabel.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: " + StyleHelper.TEXT_GRAY_700 + ";");

        // Refresh button
        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 8; " +
                "-fx-font-weight: 600; -fx-font-size: 13px; -fx-padding: 8 16; -fx-cursor: hand;");
        refreshBtn.setOnMouseEntered(e -> refreshBtn
                .setStyle("-fx-background-color: #1d4ed8; -fx-text-fill: white; -fx-background-radius: 8; " +
                        "-fx-font-weight: 600; -fx-font-size: 13px; -fx-padding: 8 16; -fx-cursor: hand;"));
        refreshBtn.setOnMouseExited(e -> refreshBtn
                .setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 8; " +
                        "-fx-font-weight: 600; -fx-font-size: 13px; -fx-padding: 8 16; -fx-cursor: hand;"));
        refreshBtn.setOnAction(e -> updateSubscriptionInfo());

        HBox buttonBox = new HBox(10);
        buttonBox.getChildren().add(refreshBtn);

        VBox infoBox = new VBox(8);
        infoBox.getChildren().addAll(subscriptionStatusLabel, subscriptionPlanLabel, subscriptionEndDateLabel);

        section.getChildren().addAll(sectionTitle, description, infoBox, buttonBox);

        return section;
    }

    private void updateSubscriptionInfo() {
        // Fetch subscription info in background thread
        new Thread(() -> {
            try {
                SubscriptionService.SubscriptionStatus status = subscriptionService.checkSubscriptionStatus();

                Platform.runLater(() -> {
                    if (status.isActive()) {
                        // Active subscription
                        subscriptionStatusLabel.setText("✅ Subscription: Active");
                        subscriptionStatusLabel
                                .setStyle("-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: #10b981;");

                        // Plan information
                        String planText = status.getPlanDuration() != null
                                ? "Plan: " + capitalize(status.getPlanDuration())
                                : "Plan: Not specified";
                        subscriptionPlanLabel.setText(planText);

                        // End date information
                        if (status.getEndDate() != null) {
                            LocalDateTime endDate = status.getEndDate();
                            DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);
                            String formattedDate = endDate.format(formatter);

                            // Calculate days remaining
                            long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(
                                    LocalDateTime.now(), endDate);

                            String endDateText;
                            if (daysRemaining > 0) {
                                endDateText = String.format("Expires: %s (%d days remaining)", formattedDate,
                                        daysRemaining);
                            } else if (daysRemaining == 0) {
                                endDateText = String.format("Expires: %s (Today)", formattedDate);
                            } else {
                                endDateText = String.format("Expired: %s (%d days ago)", formattedDate,
                                        Math.abs(daysRemaining));
                            }

                            subscriptionEndDateLabel.setText(endDateText);
                            subscriptionEndDateLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 500; " +
                                    (daysRemaining > 7 ? "-fx-text-fill: #10b981;"
                                            : daysRemaining > 0 ? "-fx-text-fill: #f59e0b;"
                                                    : "-fx-text-fill: #ef4444;"));
                        } else {
                            subscriptionEndDateLabel.setText("Expires: Not specified");
                            subscriptionEndDateLabel
                                    .setStyle("-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: "
                                            + StyleHelper.TEXT_GRAY_700 + ";");
                        }
                    } else {
                        // Inactive or no subscription
                        String statusText = "❌ Subscription: " + capitalize(status.getStatus().replace("_", " "));
                        subscriptionStatusLabel.setText(statusText);
                        subscriptionStatusLabel
                                .setStyle("-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: #ef4444;");

                        subscriptionPlanLabel.setText("Plan: No active subscription");
                        subscriptionEndDateLabel.setText("Please subscribe to continue using the application.");
                        subscriptionEndDateLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: "
                                + StyleHelper.TEXT_GRAY_700 + ";");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    subscriptionStatusLabel.setText("❌ Error: Could not fetch subscription information");
                    subscriptionStatusLabel
                            .setStyle("-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: #ef4444;");
                    subscriptionPlanLabel.setText("Please check your internet connection and backend server.");
                    subscriptionEndDateLabel.setText("");
                });
                e.printStackTrace();
            }
        }).start();
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    private void updateStatusLabels() {
        // Update connection status
        boolean isConnected = connectivityService.isConnected();
        String connectionText = isConnected ? "✅ Internet: Connected" : "❌ Internet: Not Connected";
        String connectionColor = isConnected ? "#10b981" : "#ef4444";
        connectionStatusLabel.setText(connectionText);
        connectionStatusLabel
                .setStyle("-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: " + connectionColor + ";");

        // Update sync status
        boolean isConfigured = supabaseConfig.isConfigured();
        String syncText = isConfigured ? "✅ Supabase: Configured" : "⚠️ Supabase: Not Configured";
        String syncColor = isConfigured ? "#10b981" : "#f59e0b";
        syncStatusLabel.setText(syncText);
        syncStatusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: " + syncColor + ";");
    }
}
