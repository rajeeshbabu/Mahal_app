package com.mahal.controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import com.mahal.util.SessionManager;
import com.mahal.controller.home.HomeDashboardController;
import com.mahal.controller.masjid.MasjidController;
import com.mahal.controller.staff.StaffController;
import com.mahal.controller.member.MemberController;
import com.mahal.controller.accounts.AccountsController;
import com.mahal.controller.certificate.CertificateController;
import com.mahal.controller.event.EventController;
import com.mahal.controller.inventory.InventoryController;
import com.mahal.controller.settings.SettingsController;
import com.mahal.controller.about.AboutController;
import com.mahal.controller.reports.ReportsController;
import com.mahal.controller.student.StudentController;
import com.mahal.service.SubscriptionService;
import com.mahal.service.SubscriptionService.SubscriptionStatus;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.application.Platform;

public class DashboardController {
    private Stage stage;
    private BorderPane mainLayout;
    private VBox sidebar;
    private VBox topbar;
    private StackPane contentArea;
    private Button activeNavButton;
    private Timeline subscriptionGuard;

    // Color scheme matching the web app
    private static final String PRIMARY_800 = "#166534"; // Dark green
    private static final String PRIMARY_700 = "#15803d";
    private static final String PRIMARY_600 = "#16a34a";
    private static final String PRIMARY_300 = "#86efac";
    private static final String BG_GRAY_100 = "#ffffff";
    private static final String BG_GRAY_50 = "#ffffff";
    private static final String BG_WHITE = "#ffffff";

    public void show(Stage primaryStage) {
        this.stage = primaryStage;
        stage.setTitle("Mahal Management System");
        stage.setResizable(true);

        mainLayout = new BorderPane();
        mainLayout.setStyle("-fx-background-color: " + BG_GRAY_100 + ";");

        // Create topbar
        createTopbar();

        // Create sidebar
        createSidebar();

        // Create content area
        contentArea = new StackPane();
        contentArea.setPadding(new Insets(20));
        contentArea.setStyle("-fx-background-color: " + BG_GRAY_50 + ";");

        VBox rightLayout = new VBox();
        rightLayout.getChildren().addAll(topbar, contentArea);
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        mainLayout.setLeft(sidebar);
        mainLayout.setCenter(rightLayout);

        // Show default view (Home dashboard)
        showHomeDashboardView();

        // Start periodic subscription check
        startSubscriptionGuard();

        Scene scene = new Scene(mainLayout, 1400, 900);
        stage.setScene(scene);
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.setMaximized(true);
        stage.show();
    }

    private void createTopbar() {
        topbar = new VBox();
        topbar.setPadding(new Insets(12, 30, 12, 30));
        topbar.setStyle("-fx-background-color: " + BG_WHITE + "; " +
                "-fx-border-color: #f3f4f6; -fx-border-width: 0 0 1 0;");

        HBox topbarContent = new HBox(20);
        topbarContent.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(topbarContent, Priority.ALWAYS);

        // Sidebar Toggle Button (Hamburger menu)
        Button toggleBtn = new Button("☰");
        toggleBtn.setStyle("-fx-background-color: transparent; " +
                "-fx-font-size: 20px; " +
                "-fx-text-fill: #4b5563; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 0 10 0 0;");
        toggleBtn.setOnAction(e -> toggleSidebar());

        // Search bar (Modern looking)
        HBox searchContainer = new HBox(10);
        searchContainer.setAlignment(Pos.CENTER_LEFT);
        searchContainer.setPadding(new Insets(0, 15, 0, 15));
        searchContainer.setPrefHeight(40);
        searchContainer.setMaxWidth(500);
        searchContainer.setStyle("-fx-background-color: #f9fafb; -fx-background-radius: 20; " +
                "-fx-border-color: #e5e7eb; -fx-border-radius: 20; -fx-border-width: 1;");

        Label searchIcon = new Label("🔍"); // Simple emoji as icon for now
        searchIcon.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 14px;");

        TextField searchField = new TextField();
        searchField.setPromptText("Search anything...");
        searchField.setPrefWidth(400);
        searchField.setStyle("-fx-background-color: transparent; -fx-text-fill: #374151; " +
                "-fx-prompt-text-fill: #9ca3af; -fx-padding: 0; -fx-background-insets: 0;");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        searchContainer.getChildren().addAll(searchIcon, searchField);

        // User section with profile and logout
        HBox userSection = new HBox(25);
        userSection.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(userSection, Priority.ALWAYS);

        // Profile Avatar placeholder
        Label avatar = new Label("A");
        avatar.setAlignment(Pos.CENTER);
        avatar.setPrefSize(38, 38);
        avatar.setStyle("-fx-background-color: " + PRIMARY_600 + "; -fx-text-fill: white; " +
                "-fx-background-radius: 19; -fx-font-weight: bold; -fx-font-size: 16px;");

        VBox userInfo = new VBox(0);
        userInfo.setAlignment(Pos.CENTER_LEFT);

        Label userNameLabel = new Label();
        String userName = SessionManager.getInstance().getCurrentUser() != null
                ? SessionManager.getInstance().getCurrentUser().getFullName()
                : "Admin User";
        userNameLabel.setText(userName);
        userNameLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 14px; -fx-text-fill: #111827;");

        Label userRoleLabel = new Label();
        String userRole = SessionManager.getInstance().getCurrentUser() != null
                ? SessionManager.getInstance().getCurrentUser().getRole()
                : "Super Admin";
        userRoleLabel.setText(userRole);
        userRoleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280; -fx-font-weight: 500;");

        userInfo.getChildren().addAll(userNameLabel, userRoleLabel);

        HBox profileBox = new HBox(12, avatar, userInfo);
        profileBox.setAlignment(Pos.CENTER_LEFT);

        Button logoutBtn = new Button("Sign Out");
        logoutBtn.setPrefHeight(36);
        logoutBtn.setPadding(new Insets(0, 20, 0, 20));
        logoutBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; " +
                "-fx-background-radius: 8; -fx-font-weight: 600; -fx-border-color: #fee2e2; " +
                "-fx-border-width: 1; -fx-border-radius: 8; -fx-cursor: hand;");

        logoutBtn.setOnMouseEntered(e -> logoutBtn.setStyle(
                "-fx-background-color: #fef2f2; -fx-text-fill: #dc2626; " +
                        "-fx-background-radius: 8; -fx-font-weight: 600; -fx-border-color: #fecaca; " +
                        "-fx-border-width: 1; -fx-border-radius: 8; -fx-cursor: hand;"));

        logoutBtn.setOnMouseExited(e -> logoutBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #ef4444; " +
                        "-fx-background-radius: 8; -fx-font-weight: 600; -fx-border-color: #fee2e2; " +
                        "-fx-border-width: 1; -fx-border-radius: 8; -fx-cursor: hand;"));

        logoutBtn.setOnAction(e -> logout());

        userSection.getChildren().addAll(profileBox, logoutBtn);

        topbarContent.getChildren().add(toggleBtn);
        topbarContent.getChildren().addAll(searchContainer, userSection);
        topbar.getChildren().add(topbarContent);
    }

    private void toggleSidebar() {
        if (mainLayout.getLeft() != null) {
            mainLayout.setLeft(null);
        } else {
            mainLayout.setLeft(sidebar);
        }
    }

    private void createSidebar() {
        sidebar = new VBox(0);
        sidebar.setStyle("-fx-background-color: " + PRIMARY_800 + ";");
        sidebar.setPrefWidth(260);

        // Header section
        VBox headerBox = new VBox(5);
        headerBox.setPadding(new Insets(24, 20, 20, 20));
        headerBox.setStyle("-fx-border-color: " + PRIMARY_700 + "; -fx-border-width: 0 0 1 0;");

        Label headerLabel = new Label("Mahal Management");
        headerLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label subHeaderLabel = new Label("Admin Portal");
        subHeaderLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + PRIMARY_300 + ";");

        headerBox.getChildren().addAll(headerLabel, subHeaderLabel);

        // Navigation section
        VBox navBox = new VBox(4);
        navBox.setPadding(new Insets(16, 12, 16, 12));
        navBox.setStyle("-fx-background-color: " + PRIMARY_800 + ";");

        // Navigation buttons
        Button dashboardBtn = createNavButton("🏠 Dashboard", () -> showHomeDashboardView());
        Button masjidBtn = createNavButton("🕌 Masjid", () -> showMasjidView());
        Button staffBtn = createNavButton("👥 Staff", () -> showStaffView());
        Button membersBtn = createNavButton("👤 Members", () -> showMembersView());
        Button studentsBtn = createNavButton("🎓 Students", () -> showStudentsView());
        Button accountsBtn = createNavButton("💰 Accounts", () -> showAccountsView());
        Button reportsBtn = createNavButton("📊 Reports", () -> showReportsView());
        Button certificatesBtn = createNavButton("📜 Certificates", () -> showCertificatesView());
        Button eventsBtn = createNavButton("📅 Events", () -> showEventsView());
        Button inventoryBtn = createNavButton("📦 Inventory", () -> showInventoryView());
        Button settingsBtn = createNavButton("🔧 Settings", () -> showSettingsView());
        Button aboutBtn = createNavButton("ℹ️ About Us", () -> showAboutView());

        navBox.getChildren().addAll(
                dashboardBtn, masjidBtn, staffBtn, membersBtn, studentsBtn, accountsBtn,
                reportsBtn,
                certificatesBtn, eventsBtn, inventoryBtn,
                settingsBtn, aboutBtn);

        // Set first button as active
        setActiveButton(dashboardBtn);

        VBox spacer = new VBox();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        sidebar.getChildren().addAll(headerBox, navBox, spacer);
    }

    private Button createNavButton(String text, Runnable action) {
        Button btn = new Button(text);
        btn.setPrefWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPrefHeight(42);
        btn.setPadding(new Insets(10, 16, 10, 16));
        btn.setStyle("-fx-background-color: transparent; " +
                "-fx-text-fill: #d1fae5; " +
                "-fx-background-radius: 8; " +
                "-fx-font-size: 14px; " +
                "-fx-cursor: hand;");

        btn.setOnMouseEntered(e -> {
            if (btn != activeNavButton) {
                btn.setStyle("-fx-background-color: " + PRIMARY_700 + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-background-radius: 8; " +
                        "-fx-font-size: 14px; " +
                        "-fx-cursor: hand;");
            }
        });

        btn.setOnMouseExited(e -> {
            if (btn != activeNavButton) {
                btn.setStyle("-fx-background-color: transparent; " +
                        "-fx-text-fill: #d1fae5; " +
                        "-fx-background-radius: 8; " +
                        "-fx-font-size: 14px; " +
                        "-fx-cursor: hand;");
            }
        });

        btn.setOnAction(e -> {
            if (validateSubscription()) {
                setActiveButton(btn);
                action.run();
            }
        });

        return btn;
    }

    private void setActiveButton(Button button) {
        if (activeNavButton != null) {
            activeNavButton.setStyle("-fx-background-color: transparent; " +
                    "-fx-text-fill: #d1fae5; " +
                    "-fx-background-radius: 8; " +
                    "-fx-font-size: 14px; " +
                    "-fx-cursor: hand;");
        }
        activeNavButton = button;
        button.setStyle("-fx-background-color: " + PRIMARY_600 + "; " +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 8; " +
                "-fx-font-size: 14px; " +
                "-fx-cursor: hand;");
    }

    private void showMasjidView() {
        MasjidController controller = new MasjidController();
        contentArea.getChildren().clear();
        ScrollPane scrollPane = new ScrollPane(controller.getView());
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: " + BG_GRAY_50 + ";");
        contentArea.getChildren().add(scrollPane);
    }

    private void showStaffView() {
        StaffController controller = new StaffController();
        contentArea.getChildren().clear();
        ScrollPane scrollPane = new ScrollPane(controller.getView());
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: " + BG_GRAY_50 + ";");
        contentArea.getChildren().add(scrollPane);
    }

    private void showMembersView() {
        MemberController controller = new MemberController();
        contentArea.getChildren().clear();
        ScrollPane scrollPane = new ScrollPane(controller.getView());
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: " + BG_GRAY_50 + ";");
        contentArea.getChildren().add(scrollPane);
    }

    private void showAccountsView() {
        AccountsController controller = new AccountsController();
        contentArea.getChildren().clear();
        ScrollPane scrollPane = new ScrollPane(controller.getView());
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: " + BG_GRAY_50 + ";");
        contentArea.getChildren().add(scrollPane);
    }

    private void showCertificatesView() {
        CertificateController controller = new CertificateController();
        contentArea.getChildren().clear();
        ScrollPane scrollPane = new ScrollPane(controller.getView());
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: " + BG_GRAY_50 + ";");
        contentArea.getChildren().add(scrollPane);
    }

    private void showEventsView() {
        EventController controller = new EventController();
        contentArea.getChildren().clear();
        ScrollPane scrollPane = new ScrollPane(controller.getView());
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: " + BG_GRAY_50 + ";");
        contentArea.getChildren().add(scrollPane);
    }

    private void showInventoryView() {
        InventoryController controller = new InventoryController();
        contentArea.getChildren().clear();
        ScrollPane scrollPane = new ScrollPane(controller.getView());
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: " + BG_GRAY_50 + ";");
        contentArea.getChildren().add(scrollPane);
    }

    private void showSettingsView() {
        SettingsController controller = new SettingsController();
        contentArea.getChildren().clear();
        ScrollPane scrollPane = new ScrollPane(controller.getView());
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: " + BG_GRAY_50 + ";");
        contentArea.getChildren().add(scrollPane);
    }

    private void showAboutView() {
        AboutController controller = new AboutController();
        contentArea.getChildren().clear();
        ScrollPane scrollPane = new ScrollPane(controller.getView());
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: " + BG_GRAY_50 + ";");
        contentArea.getChildren().add(scrollPane);
    }

    private void showHomeDashboardView() {
        HomeDashboardController controller = new HomeDashboardController();
        contentArea.getChildren().clear();
        ScrollPane scrollPane = new ScrollPane(controller.getView());
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: " + BG_GRAY_50 + ";");
        contentArea.getChildren().add(scrollPane);
    }

    private void showReportsView() {
        ReportsController controller = new ReportsController();
        contentArea.getChildren().clear();
        ScrollPane scrollPane = new ScrollPane(controller.getView());
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: " + BG_GRAY_50 + ";");
        contentArea.getChildren().add(scrollPane);
    }

    private void showStudentsView() {
        StudentController controller = new StudentController();
        contentArea.getChildren().clear();
        ScrollPane scrollPane = new ScrollPane(controller.getView());
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: " + BG_GRAY_50 + ";");
        contentArea.getChildren().add(scrollPane);
    }

    private void logout() {
        stopSubscriptionGuard();
        SessionManager.getInstance().logout();
        LoginController loginController = new LoginController();
        loginController.show(stage);
    }

    private void startSubscriptionGuard() {
        // Check every 5 minutes
        subscriptionGuard = new Timeline(new KeyFrame(Duration.minutes(5), e -> {
            validateSubscription();
        }));
        subscriptionGuard.setCycleCount(Timeline.INDEFINITE);
        subscriptionGuard.play();
    }

    private void stopSubscriptionGuard() {
        if (subscriptionGuard != null) {
            subscriptionGuard.stop();
        }
    }

    private boolean validateSubscription() {
        try {
            // This is a fast network call as it just returns the cached or quickly synced
            // status
            SubscriptionStatus status = SubscriptionService.getInstance().checkSubscriptionStatus();
            if (status == null || !status.isActive()) {
                String reason = (status != null) ? status.getStatus() : "could not be verified";
                System.out.println("🚫 DashboardController: Subscription validation failed (Reason: " + reason + ")");

                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Access Restricted");
                    alert.setHeaderText("Subscription Update Required");

                    String message = "Your access to the system has been restricted. ";
                    if (status != null && "expired".equalsIgnoreCase(status.getStatus())) {
                        message += "Your subscription has expired.";
                    } else if (status != null && "pending".equalsIgnoreCase(status.getStatus())) {
                        message += "Your subscription is currently pending approval/payment.";
                    } else {
                        message += "We could not verify an active subscription for your account.";
                    }
                    message += "\n\nPlease contact support or renew your subscription to continue.";

                    alert.setContentText(message);
                    alert.showAndWait();
                    logout();
                });
                return false;
            }
            return true;
        } catch (Exception e) {
            System.err.println("❌ Error validating subscription: " + e.getMessage());
            // LOCK DOWN on error - security first
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Security Error");
                alert.setHeaderText("Subscription Verification Failed");
                alert.setContentText(
                        "A technical error occurred while verifying your subscription. For security reasons, you have been logged out. Please try logging in again while connected to the internet.");
                alert.showAndWait();
                logout();
            });
            return false;
        }
    }
}
