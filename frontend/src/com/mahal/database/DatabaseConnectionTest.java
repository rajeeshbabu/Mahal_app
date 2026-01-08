package com.mahal.database;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class DatabaseConnectionTest extends Application {

    @Override
    public void start(Stage primaryStage) {
        DatabaseService dbService = DatabaseService.getInstance();

        Label statusLabel = new Label("Testing database connection...");
        VBox root = new VBox(20);
        root.getChildren().add(statusLabel);

        Scene scene = new Scene(root, 400, 200);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Database Connection Test");
        primaryStage.show();

        // Test connection in background
        new Thread(() -> {
            boolean connected = dbService.testConnection();
            javafx.application.Platform.runLater(() -> {
                if (connected) {
                    statusLabel.setText("✅ Database connection successful!");
                    statusLabel.setStyle("-fx-text-fill: green; -fx-font-size: 16px;");

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Connection Success");
                    alert.setHeaderText("Database Connected");
                    alert.setContentText("Successfully connected to SQLite database 'mahal_db.db'");
                    alert.showAndWait();
                } else {
                    statusLabel.setText("❌ Database connection failed!");
                    statusLabel.setStyle("-fx-text-fill: red; -fx-font-size: 16px;");

                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Connection Failed");
                    alert.setHeaderText("Database Connection Error");
                    alert.setContentText("Failed to connect to SQLite database.\n\nPlease check:\n" +
                            "1. sqlite-jdbc.jar is in the lib folder\n" +
                            "2. Application has write permissions to create mahal.db file");
                    alert.showAndWait();
                }
            });
        }).start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
