package com.mahal.controller.general;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import com.mahal.service.ApiService;

public class GeneralController {
    private VBox view;
    private ApiService apiService;
    
    public GeneralController() {
        this.apiService = ApiService.getInstance();
        createView();
    }
    
    public VBox getView() {
        return view;
    }
    
    private void createView() {
        view = new VBox(20);
        view.setPadding(new Insets(20));
        
        Label titleLabel = new Label("General Settings");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        
        view.getChildren().addAll(titleLabel, new Label("Role and User Management - Connect to /api/general"));
    }
}

