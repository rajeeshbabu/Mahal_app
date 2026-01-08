package com.mahal.controller.about;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;

public class AboutController {
    private VBox view;

    public AboutController() {
        createView();
    }

    public VBox getView() {
        return view;
    }

    private void createView() {
        view = new VBox(20);
        view.setPadding(new Insets(20));
        view.setAlignment(javafx.geometry.Pos.CENTER);

        Label titleLabel = new Label("Mahal Management System");
        titleLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold;");

        Label versionLabel = new Label("Version 1.1.0");
        versionLabel.setStyle("-fx-font-size: 16px;");

        Label descriptionLabel = new Label(
                "Digital Mahal Registry & Community Management System\n\n" +
                        "Built with JavaFX Desktop Application\n" +
                        "Connects to Spring Boot Backend API\n\n" +
                        "Modules:\n" +
                        "• Masjid Management\n" +
                        "• Staff Management\n" +
                        "• Member Management\n" +
                        "• Accounts (Income, Expenses, Dues)\n" +
                        "• Certificates\n" +
                        "• Events\n" +
                        "• Inventory\n" +
                        "• General Settings\n\n" +
                        "© 2025 All Rights Reserved");
        descriptionLabel.setTextAlignment(TextAlignment.CENTER);
        descriptionLabel.setStyle("-fx-font-size: 14px;");

        view.getChildren().addAll(titleLabel, versionLabel, descriptionLabel);
    }
}
