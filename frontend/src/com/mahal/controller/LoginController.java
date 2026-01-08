package com.mahal.controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import com.mahal.service.AuthService;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.Group;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.io.InputStream;
import java.io.OutputStream;

public class LoginController {
    private AuthService authService;
    private Stage stage;

    // Color scheme matching the web app
    private static final String PRIMARY_600 = "#16a34a";
    private static final String PRIMARY_700 = "#15803d";
    private static final String PRIMARY_800 = "#166534";
    private static final String BG_GRAY_100 = "#f5f5f5";
    private static final String BG_WHITE = "#ffffff";

    private static final Path SESSION_FILE = Paths.get(System.getProperty("user.home"), ".mahal_session.properties");

    private CheckBox rememberMeCheck;

    public LoginController() {
        this.authService = new AuthService();
    }

    // Helper method to create eye icon with diagonal line (for hidden password
    // state)
    private Group createEyeIconWithLine() {
        // Create eye shape (horizontal ellipse) - centered at (0,0) for easier
        // positioning
        Ellipse eye = new Ellipse(7, 4.5);
        eye.setFill(null);
        eye.setStroke(Color.valueOf("#6b7280"));
        eye.setStrokeWidth(1.8);

        // Create pupil (circle) in the center
        Circle pupil = new Circle(0, 0, 2.5);
        pupil.setFill(Color.valueOf("#6b7280"));

        // Create diagonal line through the eye (from top-left to bottom-right)
        Line line = new Line(-5, -3, 5, 3);
        line.setStroke(Color.valueOf("#6b7280"));
        line.setStrokeWidth(1.8);

        Group icon = new Group(eye, pupil, line);
        return icon;
    }

    // Helper method to create eye icon without line (for visible password state)
    private Group createEyeIcon() {
        // Create eye shape (horizontal ellipse)
        Ellipse eye = new Ellipse(7, 4.5);
        eye.setFill(null);
        eye.setStroke(Color.valueOf("#6b7280"));
        eye.setStrokeWidth(1.8);

        // Create pupil (circle) in the center
        Circle pupil = new Circle(0, 0, 2.5);
        pupil.setFill(Color.valueOf("#6b7280"));

        Group icon = new Group(eye, pupil);
        return icon;
    }

    public void show(Stage primaryStage) {
        this.stage = primaryStage;
        stage.setTitle("Mahal Management System - Login");

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: " + BG_GRAY_100 + ";");

        // Login Form Card
        VBox formBox = new VBox(20);
        formBox.setAlignment(Pos.CENTER);
        formBox.setPadding(new Insets(32));
        formBox.setStyle("-fx-background-color: " + BG_WHITE + "; " +
                "-fx-background-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        formBox.setMaxWidth(420);

        // Title
        Label titleLabel = new Label("Mahal Management System");
        titleLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + PRIMARY_800 + ";");

        Label subtitleLabel = new Label("Admin Portal");
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #6b7280;");

        VBox titleBox = new VBox(4);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.getChildren().addAll(titleLabel, subtitleLabel);

        // Form fields
        VBox fieldsBox = new VBox(16);
        fieldsBox.setPrefWidth(360);

        // Email field
        VBox emailBox = new VBox(4);
        Label emailLabel = new Label("Email");
        emailLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: #374151;");

        TextField emailField = new TextField();
        emailField.setPromptText("Enter your email");
        emailField.setPrefHeight(42);
        emailField.setStyle("-fx-background-radius: 8; " +
                "-fx-border-radius: 8; " +
                "-fx-border-color: #d1d5db; " +
                "-fx-border-width: 1; " +
                "-fx-padding: 10 14; " +
                "-fx-font-size: 14px;");
        emailField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                emailField.setStyle("-fx-background-radius: 8; " +
                        "-fx-border-radius: 8; " +
                        "-fx-border-color: " + PRIMARY_600 + "; " +
                        "-fx-border-width: 2; " +
                        "-fx-padding: 10 14; " +
                        "-fx-font-size: 14px;");
            } else {
                emailField.setStyle("-fx-background-radius: 8; " +
                        "-fx-border-radius: 8; " +
                        "-fx-border-color: #d1d5db; " +
                        "-fx-border-width: 1; " +
                        "-fx-padding: 10 14; " +
                        "-fx-font-size: 14px;");
            }
        });

        emailBox.getChildren().addAll(emailLabel, emailField);

        // Password field with label and visibility toggle
        VBox passwordBox = new VBox(4);
        Label passwordLabel = new Label("Password");
        passwordLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: #374151;");

        // Create password and text fields for toggle
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");
        passwordField.setPrefHeight(42);
        passwordField.setStyle(
                "-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #d1d5db; -fx-border-width: 1; -fx-padding: 10 40 10 14; -fx-font-size: 14px;");

        TextField passwordTextField = new TextField();
        passwordTextField.setPromptText("Enter your password");
        passwordTextField.setPrefHeight(42);
        passwordTextField.setStyle(
                "-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #d1d5db; -fx-border-width: 1; -fx-padding: 10 40 10 14; -fx-font-size: 14px;");
        passwordTextField.setVisible(false);
        passwordTextField.setManaged(false);

        // Eye button for password visibility toggle
        Button togglePasswordButton = new Button();
        togglePasswordButton.setGraphic(createEyeIcon()); // Eye icon when password is hidden
        togglePasswordButton.setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-background-radius: 4; " +
                        "-fx-padding: 4 8; " +
                        "-fx-cursor: hand;");
        togglePasswordButton.setOnMouseEntered(e -> togglePasswordButton.setStyle(
                "-fx-background-color: #f3f4f6; " +
                        "-fx-background-radius: 4; " +
                        "-fx-padding: 4 8; " +
                        "-fx-cursor: hand;"));
        togglePasswordButton.setOnMouseExited(e -> togglePasswordButton.setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-background-radius: 4; " +
                        "-fx-padding: 4 8; " +
                        "-fx-cursor: hand;"));

        // HBox to contain password field and toggle button
        HBox passwordFieldBox = new HBox(4);
        passwordFieldBox.setAlignment(Pos.CENTER_LEFT);

        // StackPane to overlay password fields (only one visible at a time)
        StackPane passwordContainer = new StackPane();
        passwordContainer.setPrefHeight(42);
        passwordContainer.getChildren().addAll(passwordField, passwordTextField);
        HBox.setHgrow(passwordContainer, Priority.ALWAYS);

        passwordFieldBox.getChildren().addAll(passwordContainer, togglePasswordButton);

        // Focus listeners for password fields
        passwordField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            String styleBase = "-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-width: "
                    + (newVal ? "2" : "1") + "; -fx-padding: 10 40 10 14; -fx-font-size: 14px;";
            String borderColor = newVal ? PRIMARY_600 : "#d1d5db";
            passwordField.setStyle(styleBase + "-fx-border-color: " + borderColor + ";");
            passwordTextField.setStyle(styleBase + "-fx-border-color: " + borderColor + ";");
        });
        passwordTextField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            String styleBase = "-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-width: "
                    + (newVal ? "2" : "1") + "; -fx-padding: 10 40 10 14; -fx-font-size: 14px;";
            String borderColor = newVal ? PRIMARY_600 : "#d1d5db";
            passwordField.setStyle(styleBase + "-fx-border-color: " + borderColor + ";");
            passwordTextField.setStyle(styleBase + "-fx-border-color: " + borderColor + ";");
        });

        // Toggle password visibility
        togglePasswordButton.setOnAction(e -> {
            if (passwordField.isVisible()) {
                // Switch to visible text
                String password = passwordField.getText();
                passwordTextField.setText(password);
                passwordField.setVisible(false);
                passwordField.setManaged(false);
                passwordTextField.setVisible(true);
                passwordTextField.setManaged(true);
                togglePasswordButton.setGraphic(createEyeIconWithLine());
            } else {
                // Switch to hidden password
                String password = passwordTextField.getText();
                passwordField.setText(password);
                passwordTextField.setVisible(false);
                passwordTextField.setManaged(false);
                passwordField.setVisible(true);
                passwordField.setManaged(true);
                togglePasswordButton.setGraphic(createEyeIcon());
            }
        });

        // Sync text between password fields
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (passwordTextField.isVisible()) {
                passwordTextField.setText(newVal);
            }
        });
        passwordTextField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (passwordField.isVisible()) {
                passwordField.setText(newVal);
            }
        });

        passwordBox.getChildren().addAll(passwordLabel, passwordFieldBox);

        fieldsBox.getChildren().addAll(emailBox, passwordBox);

        // Error label
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 13px;");
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(360);

        // Buttons
        VBox buttonsBox = new VBox(12);
        buttonsBox.setPrefWidth(360);

        // Remember me checkbox
        rememberMeCheck = new CheckBox("Remember this login");
        rememberMeCheck.setStyle("-fx-font-size: 12px; -fx-text-fill: #374151;");

        Button loginButton = new Button("Login");
        loginButton.setPrefWidth(Double.MAX_VALUE);
        loginButton.setPrefHeight(44);
        loginButton.setStyle("-fx-background-color: " + PRIMARY_600 + "; " +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 8; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 15px; " +
                "-fx-cursor: hand;");
        loginButton.setOnMouseEntered(e -> loginButton.setStyle(
                "-fx-background-color: " + PRIMARY_700 + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-background-radius: 8; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 15px; " +
                        "-fx-cursor: hand;"));
        loginButton.setOnMouseExited(e -> loginButton.setStyle(
                "-fx-background-color: " + PRIMARY_600 + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-background-radius: 8; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 15px; " +
                        "-fx-cursor: hand;"));

        Button registerButton = new Button("Create Account");
        registerButton.setPrefWidth(Double.MAX_VALUE);
        registerButton.setPrefHeight(44);
        registerButton.setStyle("-fx-background-color: transparent; " +
                "-fx-text-fill: " + PRIMARY_700 + "; " +
                "-fx-border-color: " + PRIMARY_600 + "; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 8; " +
                "-fx-font-weight: 500; " +
                "-fx-font-size: 15px; " +
                "-fx-cursor: hand;");
        registerButton.setOnMouseEntered(e -> registerButton.setStyle(
                "-fx-background-color: #f0fdf4; " +
                        "-fx-text-fill: " + PRIMARY_700 + "; " +
                        "-fx-border-color: " + PRIMARY_600 + "; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 8; " +
                        "-fx-font-weight: 500; " +
                        "-fx-font-size: 15px; " +
                        "-fx-cursor: hand;"));
        registerButton.setOnMouseExited(e -> registerButton.setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-text-fill: " + PRIMARY_700 + "; " +
                        "-fx-border-color: " + PRIMARY_600 + "; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 8; " +
                        "-fx-font-weight: 500; " +
                        "-fx-font-size: 15px; " +
                        "-fx-cursor: hand;"));

        buttonsBox.getChildren().addAll(rememberMeCheck, loginButton, registerButton);

        loginButton.setOnAction(e -> {
            String email = emailField.getText();
            String password = passwordField.isVisible() ? passwordField.getText() : passwordTextField.getText();

            if (email.isEmpty() || password.isEmpty()) {
                errorLabel.setText("Please fill in all fields");
                errorLabel.setVisible(true);
                return;
            }

            loginButton.setDisable(true);
            loginButton.setText("Logging in...");
            errorLabel.setVisible(false);

            // Run login in background thread
            new Thread(() -> {
                try {
                    boolean success = authService.login(email, password);

                    if (success) {
                        try {
                            com.mahal.util.SessionManager session = com.mahal.util.SessionManager.getInstance();
                            if (session.getCurrentUser() != null) {
                                String userId = String.valueOf(session.getCurrentUser().getId());
                                com.mahal.sync.SyncManager.getInstance().syncDownAll(userId);
                            }
                        } catch (Exception syncEx) {
                            System.err.println("Initial sync failed: " + syncEx.getMessage());
                            syncEx.printStackTrace();
                        }
                    }

                    javafx.application.Platform.runLater(() -> {
                        if (success) {
                            // Save session if requested
                            if (rememberMeCheck.isSelected()) {
                                saveSessionCredentials(email, password);
                            } else {
                                clearSessionCredentials();
                            }

                            // Trigger initial sync and sync of user data immediately after login
                            try {
                                System.out.println("Triggering initial sync after login...");
                                com.mahal.sync.SyncManager syncManager = com.mahal.sync.SyncManager.getInstance();
                                // Perform initial sync to queue all existing records (including admins)
                                syncManager.performInitialSync();
                                // Also trigger sync to execute any queued operations
                                syncManager.triggerSync();
                            } catch (Exception syncEx) {
                                System.err.println("Failed to trigger initial sync: " + syncEx.getMessage());
                                syncEx.printStackTrace();
                            }

                            // Check subscription status before showing dashboard
                            checkSubscriptionAndShowDashboard();
                        } else {
                            errorLabel.setText("Invalid email or password. Please check your credentials.");
                            errorLabel.setVisible(true);
                            loginButton.setDisable(false);
                            loginButton.setText("Login");
                        }
                    });
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        errorLabel.setText("Login error: " + ex.getMessage());
                        errorLabel.setVisible(true);
                        loginButton.setDisable(false);
                        loginButton.setText("Login");
                        ex.printStackTrace();
                    });
                }
            }).start();
        });

        registerButton.setOnAction(e ->

        showRegisterDialog());

        formBox.getChildren().addAll(titleBox, fieldsBox, errorLabel, buttonsBox);

        root.getChildren().add(formBox);

        // Pre-fill if stored
        loadSessionCredentials(emailField, passwordField, passwordTextField);
        rememberMeCheck.setSelected((passwordField.getText() != null && !passwordField.getText().isEmpty()) ||
                (passwordTextField.getText() != null && !passwordTextField.getText().isEmpty()));

        Scene scene = new Scene(root, 500, 650);
        stage.setScene(scene);
        stage.setResizable(true);
        stage.centerOnScreen();
        stage.show();
    }

    private void showRegisterDialog() {
        Stage registerStage = new Stage();
        registerStage.setTitle("Create Account");

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(32));
        root.setStyle("-fx-background-color: " + BG_WHITE + "; " +
                "-fx-background-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        root.setMaxWidth(420);

        Label titleLabel = new Label("Create Account");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + PRIMARY_800 + ";");

        VBox fieldsBox = new VBox(16);
        fieldsBox.setPrefWidth(360);

        // Name field with label
        VBox nameBox = new VBox(4);
        Label nameLabel = new Label("Full Name");
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: #374151;");
        TextField nameField = new TextField();
        nameField.setPromptText("Enter your full name");
        nameField.setPrefHeight(42);
        nameField.setStyle(
                "-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #d1d5db; -fx-border-width: 1; -fx-padding: 10 14; -fx-font-size: 14px;");
        nameField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                nameField.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: " + PRIMARY_600
                        + "; -fx-border-width: 2; -fx-padding: 10 14; -fx-font-size: 14px;");
            } else {
                nameField.setStyle(
                        "-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #d1d5db; -fx-border-width: 1; -fx-padding: 10 14; -fx-font-size: 14px;");
            }
        });
        nameBox.getChildren().addAll(nameLabel, nameField);

        // Email field with label
        VBox emailBox = new VBox(4);
        Label emailLabel = new Label("Email");
        emailLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: #374151;");
        TextField emailField = new TextField();
        emailField.setPromptText("Enter your email");
        emailField.setPrefHeight(42);
        emailField.setStyle(
                "-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #d1d5db; -fx-border-width: 1; -fx-padding: 10 14; -fx-font-size: 14px;");
        emailField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                emailField.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: " + PRIMARY_600
                        + "; -fx-border-width: 2; -fx-padding: 10 14; -fx-font-size: 14px;");
            } else {
                emailField.setStyle(
                        "-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #d1d5db; -fx-border-width: 1; -fx-padding: 10 14; -fx-font-size: 14px;");
            }
        });
        emailBox.getChildren().addAll(emailLabel, emailField);

        // Password field with label and visibility toggle
        VBox passwordBox = new VBox(4);
        Label passwordLabel = new Label("Password");
        passwordLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: #374151;");

        // Create password and text fields for toggle
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");
        passwordField.setPrefHeight(42);
        passwordField.setStyle(
                "-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #d1d5db; -fx-border-width: 1; -fx-padding: 10 40 10 14; -fx-font-size: 14px;");

        TextField passwordTextField = new TextField();
        passwordTextField.setPromptText("Enter your password");
        passwordTextField.setPrefHeight(42);
        passwordTextField.setStyle(
                "-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #d1d5db; -fx-border-width: 1; -fx-padding: 10 40 10 14; -fx-font-size: 14px;");
        passwordTextField.setVisible(false);
        passwordTextField.setManaged(false);

        // Eye button for password visibility toggle
        Button togglePasswordButton = new Button();
        togglePasswordButton.setGraphic(createEyeIcon()); // Eye icon when password is hidden
        togglePasswordButton.setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-background-radius: 4; " +
                        "-fx-padding: 4 8; " +
                        "-fx-cursor: hand;");
        togglePasswordButton.setOnMouseEntered(e -> togglePasswordButton.setStyle(
                "-fx-background-color: #f3f4f6; " +
                        "-fx-background-radius: 4; " +
                        "-fx-padding: 4 8; " +
                        "-fx-cursor: hand;"));
        togglePasswordButton.setOnMouseExited(e -> togglePasswordButton.setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-background-radius: 4; " +
                        "-fx-padding: 4 8; " +
                        "-fx-cursor: hand;"));

        // HBox to contain password field and toggle button
        HBox passwordFieldBox = new HBox(4);
        passwordFieldBox.setAlignment(Pos.CENTER_LEFT);

        // StackPane to overlay password fields (only one visible at a time)
        StackPane passwordContainer = new StackPane();
        passwordContainer.setPrefHeight(42);
        passwordContainer.getChildren().addAll(passwordField, passwordTextField);
        HBox.setHgrow(passwordContainer, Priority.ALWAYS);

        passwordFieldBox.getChildren().addAll(passwordContainer, togglePasswordButton);

        // Focus listeners for password fields
        passwordField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            String styleBase = "-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-width: "
                    + (newVal ? "2" : "1") + "; -fx-padding: 10 40 10 14; -fx-font-size: 14px;";
            String borderColor = newVal ? PRIMARY_600 : "#d1d5db";
            passwordField.setStyle(styleBase + "-fx-border-color: " + borderColor + ";");
            passwordTextField.setStyle(styleBase + "-fx-border-color: " + borderColor + ";");
        });
        passwordTextField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            String styleBase = "-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-width: "
                    + (newVal ? "2" : "1") + "; -fx-padding: 10 40 10 14; -fx-font-size: 14px;";
            String borderColor = newVal ? PRIMARY_600 : "#d1d5db";
            passwordField.setStyle(styleBase + "-fx-border-color: " + borderColor + ";");
            passwordTextField.setStyle(styleBase + "-fx-border-color: " + borderColor + ";");
        });

        // Toggle password visibility
        togglePasswordButton.setOnAction(e -> {
            if (passwordField.isVisible()) {
                // Switch to visible text
                String password = passwordField.getText();
                passwordTextField.setText(password);
                passwordField.setVisible(false);
                passwordField.setManaged(false);
                passwordTextField.setVisible(true);
                passwordTextField.setManaged(true);
                togglePasswordButton.setGraphic(createEyeIconWithLine());
            } else {
                // Switch to hidden password
                String password = passwordTextField.getText();
                passwordField.setText(password);
                passwordTextField.setVisible(false);
                passwordTextField.setManaged(false);
                passwordField.setVisible(true);
                passwordField.setManaged(true);
                togglePasswordButton.setGraphic(createEyeIcon());
            }
        });

        // Sync text between password fields
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (passwordTextField.isVisible()) {
                passwordTextField.setText(newVal);
            }
        });
        passwordTextField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (passwordField.isVisible()) {
                passwordField.setText(newVal);
            }
        });

        passwordBox.getChildren().addAll(passwordLabel, passwordFieldBox);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 13px;");
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);

        Button registerButton = new Button("Register");
        registerButton.setPrefWidth(Double.MAX_VALUE);
        registerButton.setPrefHeight(44);
        registerButton.setStyle("-fx-background-color: " + PRIMARY_600
                + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-font-size: 15px; -fx-cursor: hand;");
        registerButton.setOnMouseEntered(e -> registerButton.setStyle("-fx-background-color: " + PRIMARY_700
                + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-font-size: 15px; -fx-cursor: hand;"));
        registerButton.setOnMouseExited(e -> registerButton.setStyle("-fx-background-color: " + PRIMARY_600
                + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-font-size: 15px; -fx-cursor: hand;"));

        registerButton.setOnAction(e -> {
            String name = nameField.getText();
            String email = emailField.getText();
            String password = passwordField.isVisible() ? passwordField.getText() : passwordTextField.getText();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                errorLabel.setText("Please fill in all fields");
                errorLabel.setVisible(true);
                return;
            }

            registerButton.setDisable(true);
            registerButton.setText("Registering...");
            registerButton.setDisable(true);
            registerButton.setText("Registering...");
            new Thread(() -> {
                Long adminId = authService.register(email, password, name);
                javafx.application.Platform.runLater(() -> {
                    if (adminId != null) {
                        // Trigger sync to sync the newly created admin to Supabase
                        try {
                            System.out.println("Registration successful (ID: " + adminId
                                    + "), triggering sync and initializing subscription...");

                            // 1. Initialize pending subscription for the new user
                            new Thread(() -> {
                                try {
                                    com.mahal.service.SubscriptionService.getInstance()
                                            .createPendingSubscription(String.valueOf(adminId), email);
                                } catch (Exception subEx) {
                                    System.err.println("Failed to init subscription: " + subEx.getMessage());
                                }
                            }).start();

                            // 2. Trigger data sync
                            com.mahal.sync.SyncManager syncManager = com.mahal.sync.SyncManager.getInstance();
                            syncManager.triggerSync();
                        } catch (Exception syncEx) {
                            System.err.println("Failed to trigger sync after registration: " + syncEx.getMessage());
                            syncEx.printStackTrace();
                        }

                        registerStage.close();
                        errorLabel.setText("Registration successful! Please login.");
                        errorLabel.setStyle("-fx-text-fill: " + PRIMARY_600 + "; -fx-font-size: 13px;");
                        errorLabel.setVisible(true);
                    } else {
                        errorLabel.setText("Registration failed. Email may already exist.");
                        errorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 13px;");
                        errorLabel.setVisible(true);
                        registerButton.setDisable(false);
                        registerButton.setText("Register");
                    }
                });
            }).start();
        });

        fieldsBox.getChildren().addAll(nameBox, emailBox, passwordBox);

        root.getChildren().addAll(titleLabel, fieldsBox, errorLabel, registerButton);

        Scene scene = new Scene(root, 450, 480);
        registerStage.setScene(scene);
        registerStage.initOwner(stage);
        registerStage.setResizable(false);
        registerStage.centerOnScreen();
        registerStage.show();
    }

    private void checkSubscriptionAndShowDashboard() {
        // Check subscription in background thread
        System.out.println("🔍 [GATEKEEPER] Login successful. Verifying subscription status before dashboard...");
        new Thread(() -> {
            try {
                // Wait for backend to be ready if it's still starting (timeout 30s)
                com.mahal.service.ApiService.getInstance().waitForServer(30);

                com.mahal.service.SubscriptionService subscriptionService = com.mahal.service.SubscriptionService
                        .getInstance();
                com.mahal.service.SubscriptionService.SubscriptionStatus status = subscriptionService
                        .checkSubscriptionStatus();

                System.out
                        .println("🔍 [GATEKEEPER] Status returned: " + (status != null ? status.getStatus() : "NULL") +
                                ", Active: " + (status != null ? status.isActive() : "FALSE"));

                javafx.application.Platform.runLater(() -> {
                    if (status != null && status.isActive()) {
                        // Subscription is active - show dashboard
                        System.out.println("✅ [GATEKEEPER] Subscription is ACTIVE. Opening dashboard...");
                        showDashboard();
                    } else {
                        // Subscription inactive/expired/not found - show subscription screen
                        String reason = (status != null) ? status.getStatus() : "verification_failed";
                        System.out.println("🚫 [GATEKEEPER] Access BLOCKED. Reason: " + reason.toUpperCase() +
                                ". Showing subscription screen.");
                        showSubscriptionScreen();
                    }
                });
            } catch (Exception e) {
                // Error checking subscription - lock the app and show subscription screen
                System.err.println("❌ [GATEKEEPER] Error during verification: " + e.getMessage());
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    System.out.println("⚠️ [GATEKEEPER] Falling back to subscription screen due to error");
                    showSubscriptionScreen();
                });
            }
        }).start();
    }

    private void showDashboard() {
        DashboardController dashboardController = new DashboardController();
        dashboardController.show(stage);
    }

    private void showSubscriptionScreen() {
        com.mahal.controller.subscription.SubscriptionController subscriptionController = new com.mahal.controller.subscription.SubscriptionController(
                stage,
                () -> {
                    // After successful subscription, check again and show dashboard
                    checkSubscriptionAndShowDashboard();
                });
        subscriptionController.show();
    }

    private void saveSessionCredentials(String email, String password) {
        try {
            Properties props = new Properties();
            props.setProperty("email", email);
            props.setProperty("password", password);
            try (OutputStream os = Files.newOutputStream(SESSION_FILE)) {
                props.store(os, "Saved login");
            }
        } catch (Exception ignored) {
        }
    }

    private void clearSessionCredentials() {
        try {
            Files.deleteIfExists(SESSION_FILE);
        } catch (Exception ignored) {
        }
    }

    private void loadSessionCredentials(TextField emailField, PasswordField passwordField,
            TextField passwordTextField) {
        if (!Files.exists(SESSION_FILE))
            return;
        try (InputStream is = Files.newInputStream(SESSION_FILE)) {
            Properties props = new Properties();
            props.load(is);
            String email = props.getProperty("email");
            String password = props.getProperty("password");
            if (email != null)
                emailField.setText(email);
            if (password != null) {
                passwordField.setText(password);
                passwordTextField.setText(password);
            }
        } catch (Exception ignored) {
        }
    }
}
