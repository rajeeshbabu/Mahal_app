package com.mahal.controller.subscription;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import java.awt.Desktop;
import java.net.URI;
import javafx.stage.Stage;
import com.mahal.service.SubscriptionService;
import java.util.Map;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Netflix-style subscription screen
 * Shows Monthly and Yearly subscription options
 * Opens Razorpay hosted checkout in system browser
 */
public class SubscriptionController {
    private Stage stage;
    private SubscriptionService subscriptionService;
    private Runnable onSubscriptionSuccess;

    // Color scheme matching the app
    private static final String PRIMARY_600 = "#16a34a";
    private static final String PRIMARY_700 = "#15803d";
    private static final String PRIMARY_800 = "#166534";
    private static final String BG_GRAY_100 = "#f5f5f5";
    private static final String BG_WHITE = "#ffffff";
    private static final String TEXT_GRAY_700 = "#374151";

    public SubscriptionController(Stage stage, Runnable onSubscriptionSuccess) {
        this.stage = stage;
        this.subscriptionService = SubscriptionService.getInstance();
        this.onSubscriptionSuccess = onSubscriptionSuccess;
    }

    public void show() {
        // Fetch dynamic prices
        Map<String, String> prices = subscriptionService.getPricing();
        String monthlyPrice = prices.getOrDefault("monthly", "₹1");
        String yearlyPrice = prices.getOrDefault("yearly", "₹1");

        stage.setTitle("Mahal Management System - Subscription Required");
        stage.setResizable(true);

        VBox root = new VBox(0);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(0));
        root.setStyle("-fx-background-color: " + BG_GRAY_100 + ";");

        // Header
        VBox headerBox = new VBox(8);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setPadding(new Insets(40, 20, 30, 20));
        headerBox.setStyle("-fx-background-color: " + PRIMARY_800 + ";");

        Label titleLabel = new Label("Mahal Management System");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 32));
        titleLabel.setTextFill(Color.WHITE);

        Label subtitleLabel = new Label("Subscribe to continue");
        subtitleLabel.setFont(Font.font("System", 16));
        subtitleLabel.setTextFill(Color.web(PRIMARY_300));

        headerBox.getChildren().addAll(titleLabel, subtitleLabel);

        // Content area
        VBox contentBox = new VBox(30);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setPadding(new Insets(50, 40, 50, 40));
        contentBox.setMaxWidth(600);

        Label descriptionLabel = new Label(
                "Get full access to all features with a subscription.\n" +
                        "Choose a plan that works for you:");
        descriptionLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #374151; -fx-text-alignment: center;");
        descriptionLabel.setWrapText(true);

        // Subscription plans
        HBox plansBox = new HBox(20);
        plansBox.setAlignment(Pos.CENTER);

        // Monthly Plan
        VBox monthlyPlan = createPlanCard("Monthly", monthlyPrice, "/month", "Billed monthly", "monthly");
        monthlyPlan.setPrefWidth(250);

        // Yearly Plan (with savings badge)
        VBox yearlyPlan = createPlanCard("Yearly", yearlyPrice, "/year", "Billed annually", "yearly");
        yearlyPlan.setPrefWidth(250);
        yearlyPlan.setStyle("-fx-border-color: " + PRIMARY_600 + "; -fx-border-width: 2; -fx-border-radius: 12;");

        // Add "Best Value" badge to yearly plan
        Label bestValueBadge = new Label("BEST VALUE");
        bestValueBadge.setStyle("-fx-background-color: " + PRIMARY_600 + "; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 10px; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 4 12; " +
                "-fx-background-radius: 12;");
        StackPane yearlyPlanWithBadge = new StackPane();
        yearlyPlanWithBadge.getChildren().addAll(yearlyPlan, bestValueBadge);
        StackPane.setAlignment(bestValueBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(bestValueBadge, new Insets(10, 10, 0, 0));

        plansBox.getChildren().addAll(monthlyPlan, yearlyPlanWithBadge);

        // Add a "Check Status" button for manual refresh
        Button checkStatusButton = new Button("Check Subscription Status");
        checkStatusButton.setStyle("-fx-background-color: " + PRIMARY_600 + "; " +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 8; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 14px; " +
                "-fx-padding: 10 20; " +
                "-fx-cursor: hand;");
        checkStatusButton.setOnMouseEntered(e -> checkStatusButton.setStyle(
                "-fx-background-color: " + PRIMARY_700 + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-background-radius: 8; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 14px; " +
                        "-fx-padding: 10 20; " +
                        "-fx-cursor: hand;"));
        checkStatusButton.setOnMouseExited(e -> checkStatusButton.setStyle(
                "-fx-background-color: " + PRIMARY_600 + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-background-radius: 8; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 14px; " +
                        "-fx-padding: 10 20; " +
                        "-fx-cursor: hand;"));
        checkStatusButton.setOnAction(e -> checkSubscriptionStatusManually());

        contentBox.getChildren().addAll(descriptionLabel, plansBox, checkStatusButton);

        root.getChildren().addAll(headerBox, contentBox);

        Scene scene = new Scene(root, 700, 650);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    private void checkSubscriptionStatusManually() {
        // Show loading indicator
        System.out.println("Manual subscription status check triggered");

        new Thread(() -> {
            try {
                SubscriptionService.SubscriptionStatus status = subscriptionService.checkSubscriptionStatus();
                System.out
                        .println("Manual check result: active=" + status.isActive() + ", status=" + status.getStatus());

                Platform.runLater(() -> {
                    if (status.isActive()) {
                        System.out.println("Subscription is active! Navigating to dashboard...");
                        // Execute callback to navigate to dashboard
                        if (onSubscriptionSuccess != null) {
                            onSubscriptionSuccess.run();
                        }
                        showInfoNonBlocking("Payment successful! Your subscription is now active.");
                    } else {
                        showInfo("Subscription Status: " + status.getStatus() +
                                "\n\nYour subscription is not yet active. Please wait a moment and try again, or complete the payment if you haven't already.");
                    }
                });
            } catch (Exception e) {
                System.err.println("Error checking subscription status: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> {
                    showError("Error checking subscription status: " + e.getMessage());
                });
            }
        }).start();
    }

    private VBox createPlanCard(String planName, String price, String period, String billingInfo, String planDuration) {
        VBox card = new VBox(15);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(30, 20, 30, 20));
        card.setStyle("-fx-background-color: " + BG_WHITE + "; " +
                "-fx-background-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        Label nameLabel = new Label(planName);
        nameLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + PRIMARY_800 + ";");

        HBox priceBox = new HBox(4);
        priceBox.setAlignment(Pos.CENTER);
        Label priceLabel = new Label(price);
        priceLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: " + PRIMARY_600 + ";");
        Label periodLabel = new Label(period);
        periodLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #6b7280;");
        priceBox.getChildren().addAll(priceLabel, periodLabel);

        Label billingLabel = new Label(billingInfo);
        billingLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

        Button subscribeButton = new Button("Subscribe");
        subscribeButton.setPrefWidth(Double.MAX_VALUE);
        subscribeButton.setPrefHeight(44);
        subscribeButton.setStyle("-fx-background-color: " + PRIMARY_600 + "; " +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 8; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 15px; " +
                "-fx-cursor: hand;");
        subscribeButton.setOnMouseEntered(e -> subscribeButton.setStyle(
                "-fx-background-color: " + PRIMARY_700 + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-background-radius: 8; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 15px; " +
                        "-fx-cursor: hand;"));
        subscribeButton.setOnMouseExited(e -> subscribeButton.setStyle(
                "-fx-background-color: " + PRIMARY_600 + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-background-radius: 8; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 15px; " +
                        "-fx-cursor: hand;"));

        subscribeButton.setOnAction(e -> handleSubscribe(planDuration));

        card.getChildren().addAll(nameLabel, priceBox, billingLabel, subscribeButton);
        return card;
    }

    private void handleSubscribe(String planDuration) {
        // Show loading indicator
        Stage loadingStage = new Stage();
        loadingStage.initOwner(stage);
        loadingStage.setTitle("Creating Subscription...");

        VBox loadingBox = new VBox(20);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPadding(new Insets(40));
        loadingBox.setStyle("-fx-background-color: " + BG_WHITE + ";");

        ProgressIndicator progress = new ProgressIndicator();
        Label loadingLabel = new Label("Creating subscription...");
        loadingLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #374151;");

        loadingBox.getChildren().addAll(progress, loadingLabel);

        Scene loadingScene = new Scene(loadingBox, 300, 150);
        loadingStage.setScene(loadingScene);
        loadingStage.centerOnScreen();
        loadingStage.show();

        // Create subscription in background thread
        new Thread(() -> {
            try {
                String checkoutUrl = subscriptionService.createSubscription(planDuration);

                Platform.runLater(() -> {
                    loadingStage.close();
                    showPaymentWebView(checkoutUrl);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    loadingStage.close();
                    String errorMessage = e.getMessage();
                    String userFriendlyMessage;

                    // Check if it's a connection error
                    if (errorMessage != null &&
                            (errorMessage.contains("Connection refused") ||
                                    errorMessage.contains("connect") ||
                                    errorMessage.contains("localhost:8080"))) {
                        userFriendlyMessage = "The application could not connect to its internal service.\n\n" +
                                "This can happen if:\n" +
                                "1. Another program is using port 8080.\n" +
                                "2. A firewall is blocking the application.\n" +
                                "3. The application is still starting up (please wait 30 seconds).\n\n" +
                                "Please try restarting the application as Administrator. If the issue persists, contact support.\n\n"
                                +
                                "Error details: " + errorMessage;
                    } else {
                        userFriendlyMessage = "Failed to create subscription: " + errorMessage;
                    }

                    showError(userFriendlyMessage);
                });
            }
        }).start();
    }

    private void showPaymentWebView(String checkoutUrl) {
        // Open payment URL in system's default browser - more robust method for Windows
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                // Windows specific robust command
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + checkoutUrl);
                System.out.println("Opened browser using rundll32 for URL: " + checkoutUrl);
            } else if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(checkoutUrl));
            } else {
                // Linux/Mac fallback
                String[] browsers = { "xdg-open", "open", "google-chrome", "firefox" };
                boolean opened = false;
                for (String browser : browsers) {
                    try {
                        Runtime.getRuntime().exec(new String[] { browser, checkoutUrl });
                        opened = true;
                        break;
                    } catch (Exception ignore) {
                    }
                }
                if (!opened)
                    throw new Exception("No suitable browser launcher found");
            }

            // Show dialog with instructions
            showPaymentInstructionsDialog();

            // Start polling for subscription status
            startSubscriptionStatusPolling();

        } catch (Exception e) {
            System.err.println("Failed to open browser: " + e.getMessage());
            // Fallback: show URL in a dialog if automatic opening fails
            showUrlDialog(checkoutUrl);
        }
    }

    private void showPaymentInstructionsDialog() {
        Stage dialogStage = new Stage();
        dialogStage.setTitle("Complete Payment");
        dialogStage.setWidth(500);
        dialogStage.setHeight(350);

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: " + BG_WHITE + ";");

        Label titleLabel = new Label("Payment Page Opened");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + PRIMARY_800 + ";");

        Label instructionLabel = new Label(
                "The payment page has been opened in your browser.\n\n" +
                        "Please complete the payment there.\n\n" +
                        "This window will automatically detect when your\n" +
                        "subscription is activated.\n\n" +
                        "You can close this dialog - the app will continue\n" +
                        "checking in the background.");
        instructionLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + TEXT_GRAY_700 + ";");
        instructionLabel.setAlignment(Pos.CENTER);
        instructionLabel.setWrapText(true);

        Button closeButton = new Button("OK");
        closeButton.setStyle(getPrimaryButtonStyle());
        closeButton.setPrefWidth(120);
        closeButton.setOnAction(e -> dialogStage.close());

        root.getChildren().addAll(titleLabel, instructionLabel, closeButton);

        Scene scene = new Scene(root);
        dialogStage.setScene(scene);
        dialogStage.centerOnScreen();
        dialogStage.show();
    }

    private void showUrlDialog(String url) {
        Stage dialogStage = new Stage();
        dialogStage.setTitle("Payment URL");
        dialogStage.setWidth(600);
        dialogStage.setHeight(250);

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: " + BG_WHITE + ";");

        Label titleLabel = new Label("Copy this URL to your browser:");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + PRIMARY_800 + ";");

        javafx.scene.control.TextField urlField = new javafx.scene.control.TextField(url);
        urlField.setEditable(false);
        urlField.setStyle("-fx-font-size: 12px;");
        urlField.setPrefWidth(550);

        Button copyButton = new Button("Copy URL");
        copyButton.setStyle(getPrimaryButtonStyle());
        copyButton.setOnAction(e -> {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(url);
            clipboard.setContent(content);
            showInfo("URL copied to clipboard!");
        });

        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> dialogStage.close());

        HBox buttonBox = new HBox(10, copyButton, closeButton);
        buttonBox.setAlignment(Pos.CENTER);

        root.getChildren().addAll(titleLabel, urlField, buttonBox);

        Scene scene = new Scene(root);
        dialogStage.setScene(scene);
        dialogStage.centerOnScreen();
        dialogStage.show();

        // Start polling for subscription status
        startSubscriptionStatusPolling();
    }

    private void startSubscriptionStatusPolling() {
        // Check subscription status periodically
        new Thread(() -> {
            try {
                // Initial check after 3 seconds (shorter wait)
                Thread.sleep(3000);

                // Check more frequently at first, then slow down
                // First 5 checks: every 2 seconds (for immediate response)
                for (int i = 0; i < 5; i++) {
                    SubscriptionService.SubscriptionStatus status = subscriptionService.checkSubscriptionStatus();
                    System.out.println("Polling attempt " + (i + 1) + ": active=" + status.isActive() + ", status="
                            + status.getStatus());

                    if (status.isActive()) {
                        Platform.runLater(() -> {
                            System.out.println("Subscription is active! Navigating to dashboard...");
                            // Execute callback to navigate to dashboard first (this will replace the scene)
                            if (onSubscriptionSuccess != null) {
                                onSubscriptionSuccess.run();
                            }

                            // Show success message (non-blocking) after navigation
                            showInfoNonBlocking("Payment successful! Your subscription is now active.");
                        });
                        return; // Exit the thread
                    }
                    Thread.sleep(2000); // 2 seconds between initial checks
                }

                // Continue checking every 3 seconds for up to 10 minutes
                // Total time: 5 checks * 2s = 10s, then 197 checks * 3s = 591s, total ≈ 10
                // minutes
                for (int i = 0; i < 197; i++) {
                    SubscriptionService.SubscriptionStatus status = subscriptionService.checkSubscriptionStatus();
                    System.out.println("Polling attempt " + (i + 6) + ": active=" + status.isActive() + ", status="
                            + status.getStatus());

                    if (status.isActive()) {
                        Platform.runLater(() -> {
                            System.out.println("Subscription is active! Navigating to dashboard...");
                            // Execute callback to navigate to dashboard first (this will replace the scene)
                            if (onSubscriptionSuccess != null) {
                                onSubscriptionSuccess.run();
                            }

                            // Show success message (non-blocking) after navigation
                            showInfoNonBlocking("Payment successful! Your subscription is now active.");
                        });
                        return; // Exit the thread
                    }
                    Thread.sleep(3000); // 3 seconds between checks
                }

                System.out.println("Polling timeout: Subscription status not active after 10 minutes");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.err.println("Error during subscription status polling: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void showError(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfoNonBlocking(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show(); // Non-blocking - doesn't wait for user to close
    }

    private String getPrimaryButtonStyle() {
        return "-fx-background-color: " + PRIMARY_600 + "; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 14px; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 10 20; " +
                "-fx-background-radius: 6; " +
                "-fx-cursor: hand;";
    }

    private static final String PRIMARY_300 = "#86efac";
}
