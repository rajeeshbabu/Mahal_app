package com.mahal.controller.certificate;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.stage.PopupWindow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import com.mahal.database.CertificateDAO;
import com.mahal.model.Certificate;
import com.mahal.service.CertificateNumberService;
import com.mahal.service.CertificatePDFService;
import com.mahal.util.TableStyler;
import com.mahal.util.StyleHelper;
import com.mahal.util.FormStyler;
import java.time.LocalDate;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class CertificateController {
    private VBox view;
    private StackPane contentPane;
    private VBox marriageFormViewPane;
    private VBox deathFormViewPane;
    private VBox jamathFormViewPane;
    private VBox customFormViewPane;
    private VBox marriageListViewPane;
    private VBox deathListViewPane;
    private VBox jamathListViewPane;
    private VBox customListViewPane;
    private CertificateDAO certificateDAO;
    private CertificateNumberService certNumberService;

    // Lists for each certificate type
    private ObservableList<Certificate> marriageList = FXCollections.observableArrayList();
    private ObservableList<Certificate> deathList = FXCollections.observableArrayList();
    private ObservableList<Certificate> jamathList = FXCollections.observableArrayList();
    private ObservableList<Certificate> customList = FXCollections.observableArrayList();

    // Filter fields
    private TextField marriageSearch;
    private TextField deathSearch;
    private TextField jamathSearch;
    private TextField customSearch, customCertNo, customTemplateName;

    public CertificateController() {
        this.certificateDAO = new CertificateDAO();
        this.certNumberService = new CertificateNumberService();
        createView();

        // Subscribe to sync events
        com.mahal.util.EventBus.getInstance().subscribe("marriage_certificates",
                e -> javafx.application.Platform.runLater(this::loadMarriageList));
        com.mahal.util.EventBus.getInstance().subscribe("death_certificates",
                e -> javafx.application.Platform.runLater(this::loadDeathList));
        com.mahal.util.EventBus.getInstance().subscribe("jamath_certificates",
                e -> javafx.application.Platform.runLater(this::loadJamathList));
        com.mahal.util.EventBus.getInstance().subscribe("custom_certificates",
                e -> javafx.application.Platform.runLater(this::loadCustomList));
    }

    public VBox getView() {
        return view;
    }

    private void styleSwitcherButton(Button btn, boolean active) {
        btn.setStyle(StyleHelper.getPillButtonStyle(active));
    }

    private void styleFormContainer(VBox form) {
        form.setSpacing(12);
        // Keep existing width as-is; add soft card look similar to filters
        form.setPadding(new Insets(12, 12, 16, 12));
        form.setStyle("-fx-background-color: #ffffff;");
    }

    private void createView() {
        view = new VBox(20);
        view.setPadding(new Insets(20));
        view.setStyle(StyleHelper.getCardStyle());

        Label titleLabel = new Label("Certificate Management");
        titleLabel.setStyle(StyleHelper.getTitleStyle());

        // Emerald Pill Switcher
        HBox switcher = new HBox(8);
        switcher.setAlignment(Pos.CENTER_LEFT);
        switcher.setStyle(StyleHelper.getPillSwitcherContainerStyle());

        Button marriageFormBtn = new Button("Marriage Form");
        Button marriageListBtn = new Button("Marriage List");
        Button deathFormBtn = new Button("Death Form");
        Button deathListBtn = new Button("Death List");
        Button jamathFormBtn = new Button("Jamath Form");
        Button jamathListBtn = new Button("Jamath List");
        Button customFormBtn = new Button("Custom Form");
        Button customListBtn = new Button("Custom List");

        styleSwitcherButton(marriageFormBtn, true);
        styleSwitcherButton(marriageListBtn, false);
        styleSwitcherButton(deathFormBtn, false);
        styleSwitcherButton(deathListBtn, false);
        styleSwitcherButton(jamathFormBtn, false);
        styleSwitcherButton(jamathListBtn, false);
        styleSwitcherButton(customFormBtn, false);
        styleSwitcherButton(customListBtn, false);

        switcher.getChildren().addAll(
                marriageFormBtn, marriageListBtn, deathFormBtn, deathListBtn,
                jamathFormBtn, jamathListBtn, customFormBtn, customListBtn);

        contentPane = new StackPane();
        contentPane.setPadding(new Insets(10, 0, 0, 0));
        marriageFormViewPane = createMarriageForm();
        marriageListViewPane = createMarriageList();
        deathFormViewPane = createDeathForm();
        deathListViewPane = createDeathList();
        jamathFormViewPane = createJamathForm();
        jamathListViewPane = createJamathList();
        customFormViewPane = createCustomForm();
        customListViewPane = createCustomList();
        contentPane.getChildren().setAll(marriageFormViewPane);

        marriageFormBtn.setOnAction(e -> {
            styleSwitcherButton(marriageFormBtn, true);
            styleSwitcherButton(marriageListBtn, false);
            styleSwitcherButton(deathFormBtn, false);
            styleSwitcherButton(deathListBtn, false);
            styleSwitcherButton(jamathFormBtn, false);
            styleSwitcherButton(jamathListBtn, false);
            styleSwitcherButton(customFormBtn, false);
            styleSwitcherButton(customListBtn, false);
            contentPane.getChildren().setAll(marriageFormViewPane);
        });

        marriageListBtn.setOnAction(e -> {
            styleSwitcherButton(marriageFormBtn, false);
            styleSwitcherButton(marriageListBtn, true);
            styleSwitcherButton(deathFormBtn, false);
            styleSwitcherButton(deathListBtn, false);
            styleSwitcherButton(jamathFormBtn, false);
            styleSwitcherButton(jamathListBtn, false);
            styleSwitcherButton(customFormBtn, false);
            styleSwitcherButton(customListBtn, false);
            contentPane.getChildren().setAll(marriageListViewPane);
        });

        deathFormBtn.setOnAction(e -> {
            styleSwitcherButton(marriageFormBtn, false);
            styleSwitcherButton(marriageListBtn, false);
            styleSwitcherButton(deathFormBtn, true);
            styleSwitcherButton(deathListBtn, false);
            styleSwitcherButton(jamathFormBtn, false);
            styleSwitcherButton(jamathListBtn, false);
            styleSwitcherButton(customFormBtn, false);
            styleSwitcherButton(customListBtn, false);
            contentPane.getChildren().setAll(deathFormViewPane);
        });

        deathListBtn.setOnAction(e -> {
            styleSwitcherButton(marriageFormBtn, false);
            styleSwitcherButton(marriageListBtn, false);
            styleSwitcherButton(deathFormBtn, false);
            styleSwitcherButton(deathListBtn, true);
            styleSwitcherButton(jamathFormBtn, false);
            styleSwitcherButton(jamathListBtn, false);
            styleSwitcherButton(customFormBtn, false);
            styleSwitcherButton(customListBtn, false);
            contentPane.getChildren().setAll(deathListViewPane);
        });

        jamathFormBtn.setOnAction(e -> {
            styleSwitcherButton(marriageFormBtn, false);
            styleSwitcherButton(marriageListBtn, false);
            styleSwitcherButton(deathFormBtn, false);
            styleSwitcherButton(deathListBtn, false);
            styleSwitcherButton(jamathFormBtn, true);
            styleSwitcherButton(jamathListBtn, false);
            styleSwitcherButton(customFormBtn, false);
            styleSwitcherButton(customListBtn, false);
            contentPane.getChildren().setAll(jamathFormViewPane);
        });

        jamathListBtn.setOnAction(e -> {
            styleSwitcherButton(marriageFormBtn, false);
            styleSwitcherButton(marriageListBtn, false);
            styleSwitcherButton(deathFormBtn, false);
            styleSwitcherButton(deathListBtn, false);
            styleSwitcherButton(jamathFormBtn, false);
            styleSwitcherButton(jamathListBtn, true);
            styleSwitcherButton(customFormBtn, false);
            styleSwitcherButton(customListBtn, false);
            contentPane.getChildren().setAll(jamathListViewPane);
        });

        customFormBtn.setOnAction(e -> {
            styleSwitcherButton(marriageFormBtn, false);
            styleSwitcherButton(marriageListBtn, false);
            styleSwitcherButton(deathFormBtn, false);
            styleSwitcherButton(deathListBtn, false);
            styleSwitcherButton(jamathFormBtn, false);
            styleSwitcherButton(jamathListBtn, false);
            styleSwitcherButton(customFormBtn, true);
            styleSwitcherButton(customListBtn, false);
            contentPane.getChildren().setAll(customFormViewPane);
        });

        customListBtn.setOnAction(e -> {
            styleSwitcherButton(marriageFormBtn, false);
            styleSwitcherButton(marriageListBtn, false);
            styleSwitcherButton(deathFormBtn, false);
            styleSwitcherButton(deathListBtn, false);
            styleSwitcherButton(jamathFormBtn, false);
            styleSwitcherButton(jamathListBtn, false);
            styleSwitcherButton(customFormBtn, false);
            styleSwitcherButton(customListBtn, true);
            contentPane.getChildren().setAll(customListViewPane);
        });

        view.getChildren().addAll(titleLabel, switcher, contentPane);
    }

    // ========== MARRIAGE FORM ==========
    private VBox createMarriageForm() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(12));

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent;");

        VBox form = new VBox(12);
        styleFormContainer(form);

        Label title = FormStyler.createFormLabel("Add Marriage Certificate");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + StyleHelper.TEXT_GRAY_900 + ";"); // Smaller
                                                                                                                         // title

        String fieldStyle = "-fx-background-color: #ffffff; -fx-background-radius: 8; -fx-border-radius: 8; "
                + "-fx-border-color: #d1d5db; -fx-border-width: 1; -fx-padding: 8 12; -fx-font-size: 12px;";
        String focusStyle = "-fx-background-color: #ffffff; -fx-background-radius: 8; -fx-border-radius: 8; "
                + "-fx-border-color: #cbd5e1; -fx-border-width: 1; -fx-padding: 8 12; -fx-font-size: 12px;";
        String datePickerStyle = fieldStyle;
        String datePickerFocusStyle = focusStyle;

        TextField certNoField = new TextField();
        certNoField.setPromptText("Certificate No (Auto-generated if empty)");
        certNoField.setPrefHeight(32);
        certNoField.setStyle(fieldStyle);
        certNoField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> certNoField.setStyle(newVal ? focusStyle : fieldStyle));
        Button generateBtn = new Button("Auto-Generate");
        generateBtn.setStyle(
                "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
        generateBtn.setOnMouseEntered(e -> generateBtn.setStyle(
                "-fx-background-color: #1d4ed8; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        generateBtn.setOnMouseExited(e -> generateBtn.setStyle(
                "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        generateBtn.setOnAction(e -> certNoField.setText(certNumberService.generateMarriageCertificateNumber()));
        HBox certNoBox = new HBox(10, certNoField, generateBtn);
        certNoBox.setAlignment(Pos.CENTER_LEFT);

        TextField groomField = new TextField();
        groomField.setPromptText("Groom Name *");
        groomField.setPrefHeight(32);
        groomField.setStyle(fieldStyle + " -fx-text-inner-color: #111827;");
        groomField.focusedProperty().addListener((obs, oldVal, newVal) -> groomField
                .setStyle((newVal ? focusStyle : fieldStyle) + " -fx-text-inner-color: #111827;"));

        TextField brideField = new TextField();
        brideField.setPromptText("Bride Name *");
        brideField.setPrefHeight(32);
        brideField.setStyle(fieldStyle);
        brideField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> brideField.setStyle(newVal ? focusStyle : fieldStyle));

        TextField groomParentField = new TextField();
        groomParentField.setPromptText("Groom's Parent Name");
        groomParentField.setPrefHeight(32);
        groomParentField.setStyle(fieldStyle);
        groomParentField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> groomParentField.setStyle(newVal ? focusStyle : fieldStyle));

        TextField brideParentField = new TextField();
        brideParentField.setPromptText("Bride's Parent Name");
        brideParentField.setPrefHeight(32);
        brideParentField.setStyle(fieldStyle + " -fx-text-inner-color: #111827;");
        brideParentField.focusedProperty().addListener((obs, oldVal, newVal) -> brideParentField
                .setStyle((newVal ? focusStyle : fieldStyle) + " -fx-text-inner-color: #111827;"));

        TextArea groomAddressField = new TextArea();
        groomAddressField.setPromptText("Groom's Address");
        groomAddressField.setPrefRowCount(2);
        groomAddressField.setPrefHeight(50);
        groomAddressField.setStyle(fieldStyle);
        groomAddressField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> groomAddressField.setStyle(newVal ? focusStyle : fieldStyle));

        TextArea brideAddressField = new TextArea();
        brideAddressField.setPromptText("Bride's Address");
        brideAddressField.setPrefRowCount(2);
        brideAddressField.setPrefHeight(50);
        brideAddressField.setStyle(fieldStyle);
        brideAddressField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> brideAddressField.setStyle(newVal ? focusStyle : fieldStyle));

        TextField placeField = new TextField();
        placeField.setPromptText("Place of Marriage");
        placeField.setPrefHeight(32);
        placeField.setStyle(fieldStyle);
        placeField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> placeField.setStyle(newVal ? focusStyle : fieldStyle));

        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("Registered", "Unregistered");
        statusCombo.setPromptText("Marriage Status");
        statusCombo.setPrefHeight(32);
        statusCombo.setStyle(fieldStyle);
        statusCombo.focusedProperty()
                .addListener((obs, oldVal, newVal) -> statusCombo.setStyle(newVal ? focusStyle : fieldStyle));

        DatePicker marriageDatePicker = new DatePicker();
        marriageDatePicker.setPromptText("Marriage Date");
        marriageDatePicker.setPrefHeight(32);
        marriageDatePicker.setStyle(datePickerStyle + " -fx-text-fill: #111827;");
        marriageDatePicker.focusedProperty().addListener((obs, oldVal, newVal) -> marriageDatePicker
                .setStyle((newVal ? datePickerFocusStyle : datePickerStyle) + " -fx-text-fill: #111827;"));
        marriageDatePicker.getEditor().setStyle("-fx-text-fill: #111827;");

        // Style the month and year labels in the popup programmatically
        marriageDatePicker.showingProperty().addListener((obs, wasShowing, isShowing) -> {
            if (isShowing) {
                javafx.application.Platform.runLater(() -> {
                    try {
                        // Use a small delay to ensure popup is fully rendered
                        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                                javafx.util.Duration.millis(50));
                        pause.setOnFinished(e -> {
                            try {
                                // Look up all scenes (including popup windows)
                                for (javafx.stage.Window window : javafx.stage.Window.getWindows()) {
                                    if (window instanceof javafx.stage.PopupWindow) {
                                        javafx.scene.Scene scene = ((javafx.stage.PopupWindow) window).getScene();
                                        if (scene != null) {
                                            java.util.Set<javafx.scene.Node> labels = scene.getRoot()
                                                    .lookupAll(".label");
                                            for (javafx.scene.Node node : labels) {
                                                if (node instanceof javafx.scene.control.Label) {
                                                    javafx.scene.control.Label label = (javafx.scene.control.Label) node;
                                                    String text = label.getText();
                                                    // Style month/year labels (typically contain month names or years)
                                                    if ((text != null && (text.matches("\\d{4}") ||
                                                            java.util.Arrays
                                                                    .asList("January", "February", "March", "April",
                                                                            "May", "June",
                                                                            "July", "August", "September", "October",
                                                                            "November", "December")
                                                                    .contains(text)))) {
                                                        label.setStyle("-fx-text-fill: #111827;");
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (Exception ex) {
                                // Ignore styling errors
                            }
                        });
                        pause.play();
                    } catch (Exception e) {
                        // Ignore styling errors
                    }
                });
            }
        });

        TextArea notesField = new TextArea();
        notesField.setPromptText("Additional Notes");
        notesField.setPrefRowCount(2);
        notesField.setPrefHeight(50);
        notesField.setStyle(fieldStyle);
        notesField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> notesField.setStyle(newVal ? focusStyle : fieldStyle));

        Button save = new Button("Generate Certificate");
        save.setStyle(
                "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
        save.setOnMouseEntered(e -> save.setStyle(
                "-fx-background-color: #1d4ed8; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        save.setOnMouseExited(e -> save.setStyle(
                "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        save.setOnAction(e -> {
            if (groomField.getText().isEmpty() || brideField.getText().isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Groom and Bride names are required", ButtonType.OK).showAndWait();
                return;
            }

            Certificate c = new Certificate();
            c.setType("Marriage");
            String certNo = certNoField.getText().trim();
            if (certNo.isEmpty()) {
                certNo = certNumberService.generateMarriageCertificateNumber();
            }
            c.setCertificateNo(certNo);
            c.setGroomName(groomField.getText());
            c.setBrideName(brideField.getText());
            c.setParentNameOfGroom(groomParentField.getText());
            c.setParentNameOfBride(brideParentField.getText());
            c.setAddressOfGroom(groomAddressField.getText());
            c.setAddressOfBride(brideAddressField.getText());
            c.setPlaceOfMarriage(placeField.getText());
            c.setMarriageStatus(statusCombo.getValue());
            c.setMarriageDate(marriageDatePicker.getValue());
            c.setAdditionalNotes(notesField.getText());
            c.setQrCode("https://mahal.com/verify/" + certNo);

            // Save certificate - form will be cleared after successful save
            saveCertificate(c, null, null, certNoField, groomField, brideField, groomParentField,
                    brideParentField, groomAddressField, brideAddressField, placeField,
                    statusCombo, marriageDatePicker, notesField);
        });

        Button cancel = new Button("Clear");
        cancel.setStyle(
                "-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
        cancel.setOnMouseEntered(e -> cancel.setStyle(
                "-fx-background-color: #dc2626; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        cancel.setOnMouseExited(e -> cancel.setStyle(
                "-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        cancel.setOnAction(e -> {
            certNoField.clear();
            groomField.clear();
            brideField.clear();
            groomParentField.clear();
            brideParentField.clear();
            groomAddressField.clear();
            brideAddressField.clear();
            placeField.clear();
            statusCombo.getSelectionModel().clearSelection();
            marriageDatePicker.setValue(null);
            notesField.clear();
        });

        HBox buttons = new HBox(10, cancel, save);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(8, 0, 0, 0));

        Label certNoLabel = FormStyler.createFormLabel("Certificate No");
        form.getChildren().addAll(
                title,
                certNoLabel,
                certNoBox,
                FormStyler.createCompactFormField("Groom Name *", groomField),
                FormStyler.createCompactFormField("Bride Name *", brideField),
                FormStyler.createCompactFormField("Groom's Parent Name", groomParentField),
                FormStyler.createCompactFormField("Bride's Parent Name", brideParentField),
                FormStyler.createCompactFormField("Groom's Address", groomAddressField),
                FormStyler.createCompactFormField("Bride's Address", brideAddressField),
                FormStyler.createCompactFormField("Place of Marriage", placeField),
                FormStyler.createCompactFormField("Marriage Status", statusCombo),
                FormStyler.createCompactFormField("Marriage Date", marriageDatePicker),
                FormStyler.createCompactFormField("Additional Notes", notesField),
                buttons);

        scrollPane.setContent(form);
        box.getChildren().addAll(scrollPane);
        return box;
    }

    // ========== DEATH FORM ==========
    private VBox createDeathForm() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(12));

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent;");

        VBox form = new VBox(12);
        styleFormContainer(form);

        Label title = FormStyler.createFormLabel("Add Death Certificate");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + StyleHelper.TEXT_GRAY_900 + ";");

        String fieldStyle = "-fx-background-color: #ffffff; -fx-background-radius: 8; -fx-border-radius: 8; "
                + "-fx-border-color: #d1d5db; -fx-border-width: 1; -fx-padding: 8 12; -fx-font-size: 12px;";
        String focusStyle = "-fx-background-color: #ffffff; -fx-background-radius: 8; -fx-border-radius: 8; "
                + "-fx-border-color: #cbd5e1; -fx-border-width: 1; -fx-padding: 8 12; -fx-font-size: 12px;";
        String datePickerStyle = fieldStyle;
        String datePickerFocusStyle = focusStyle;

        TextField certNoField = new TextField();
        certNoField.setPromptText("Certificate No (Auto-generated if empty)");
        certNoField.setPrefHeight(32);
        certNoField.setStyle(fieldStyle);
        certNoField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> certNoField.setStyle(newVal ? focusStyle : fieldStyle));
        Button generateBtn = new Button("Auto-Generate");
        generateBtn.setStyle(
                "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
        generateBtn.setOnMouseEntered(e -> generateBtn.setStyle(
                "-fx-background-color: #1d4ed8; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        generateBtn.setOnMouseExited(e -> generateBtn.setStyle(
                "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        generateBtn.setOnAction(e -> certNoField.setText(certNumberService.generateDeathCertificateNumber()));
        HBox certNoBox = new HBox(10, certNoField, generateBtn);
        certNoBox.setAlignment(Pos.CENTER_LEFT);

        TextField nameField = new TextField();
        nameField.setPromptText("Name *");
        nameField.setPrefHeight(32);
        nameField.setStyle(fieldStyle);
        nameField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> nameField.setStyle(newVal ? focusStyle : fieldStyle));

        TextField parentField = new TextField();
        parentField.setPromptText("Parent Name");
        parentField.setPrefHeight(32);
        parentField.setStyle(fieldStyle);
        parentField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> parentField.setStyle(newVal ? focusStyle : fieldStyle));

        TextArea addressField = new TextArea();
        addressField.setPromptText("Address");
        addressField.setPrefRowCount(2);
        addressField.setPrefHeight(50);
        addressField.setStyle(fieldStyle);
        addressField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> addressField.setStyle(newVal ? focusStyle : fieldStyle));

        TextField thalookField = new TextField();
        thalookField.setPromptText("Thalook");
        thalookField.setPrefHeight(32);
        thalookField.setStyle(fieldStyle);
        thalookField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> thalookField.setStyle(newVal ? focusStyle : fieldStyle));

        DatePicker deathDatePicker = new DatePicker();
        deathDatePicker.setPromptText("Date of Death");
        deathDatePicker.setPrefHeight(32);
        deathDatePicker.setStyle(datePickerStyle + " -fx-text-fill: #111827;");
        deathDatePicker.focusedProperty().addListener((obs, oldVal, newVal) -> deathDatePicker
                .setStyle((newVal ? datePickerFocusStyle : datePickerStyle) + " -fx-text-fill: #111827;"));
        deathDatePicker.getEditor().setStyle("-fx-text-fill: #111827;");

        // Style the month and year labels in the popup programmatically
        deathDatePicker.showingProperty().addListener((obs, wasShowing, isShowing) -> {
            if (isShowing) {
                javafx.application.Platform.runLater(() -> {
                    try {
                        // Use a small delay to ensure popup is fully rendered
                        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                                javafx.util.Duration.millis(50));
                        pause.setOnFinished(e -> {
                            try {
                                // Look up all scenes (including popup windows)
                                for (javafx.stage.Window window : javafx.stage.Window.getWindows()) {
                                    if (window instanceof javafx.stage.PopupWindow) {
                                        javafx.scene.Scene scene = ((javafx.stage.PopupWindow) window).getScene();
                                        if (scene != null) {
                                            java.util.Set<javafx.scene.Node> labels = scene.getRoot()
                                                    .lookupAll(".label");
                                            for (javafx.scene.Node node : labels) {
                                                if (node instanceof javafx.scene.control.Label) {
                                                    javafx.scene.control.Label label = (javafx.scene.control.Label) node;
                                                    String text = label.getText();
                                                    // Style month/year labels (typically contain month names or years)
                                                    if ((text != null && (text.matches("\\d{4}") ||
                                                            java.util.Arrays
                                                                    .asList("January", "February", "March", "April",
                                                                            "May", "June",
                                                                            "July", "August", "September", "October",
                                                                            "November", "December")
                                                                    .contains(text)))) {
                                                        label.setStyle("-fx-text-fill: #111827;");
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (Exception ex) {
                                // Ignore styling errors
                            }
                        });
                        pause.play();
                    } catch (Exception e) {
                        // Ignore styling errors
                    }
                });
            }
        });

        TextField causeField = new TextField();
        causeField.setPromptText("Cause");
        causeField.setPrefHeight(32);
        causeField.setStyle(fieldStyle);
        causeField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> causeField.setStyle(newVal ? focusStyle : fieldStyle));

        TextField placeField = new TextField();
        placeField.setPromptText("Place of Death");
        placeField.setPrefHeight(32);
        placeField.setStyle(fieldStyle);
        placeField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> placeField.setStyle(newVal ? focusStyle : fieldStyle));

        DatePicker issueDatePicker = new DatePicker();
        issueDatePicker.setPromptText("Issued Date");
        issueDatePicker.setPrefHeight(32);
        issueDatePicker.setStyle(datePickerStyle + " -fx-text-fill: #111827;");
        issueDatePicker.focusedProperty().addListener((obs, oldVal, newVal) -> issueDatePicker
                .setStyle((newVal ? datePickerFocusStyle : datePickerStyle) + " -fx-text-fill: #111827;"));
        issueDatePicker.getEditor().setStyle("-fx-text-fill: #111827;");

        // Style the month and year labels in the popup programmatically
        issueDatePicker.showingProperty().addListener((obs, wasShowing, isShowing) -> {
            if (isShowing) {
                javafx.application.Platform.runLater(() -> {
                    try {
                        // Use a small delay to ensure popup is fully rendered
                        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                                javafx.util.Duration.millis(50));
                        pause.setOnFinished(e -> {
                            try {
                                // Look up all scenes (including popup windows)
                                for (javafx.stage.Window window : javafx.stage.Window.getWindows()) {
                                    if (window instanceof javafx.stage.PopupWindow) {
                                        javafx.scene.Scene scene = ((javafx.stage.PopupWindow) window).getScene();
                                        if (scene != null) {
                                            java.util.Set<javafx.scene.Node> labels = scene.getRoot()
                                                    .lookupAll(".label");
                                            for (javafx.scene.Node node : labels) {
                                                if (node instanceof javafx.scene.control.Label) {
                                                    javafx.scene.control.Label label = (javafx.scene.control.Label) node;
                                                    String text = label.getText();
                                                    // Style month/year labels (typically contain month names or years)
                                                    if ((text != null && (text.matches("\\d{4}") ||
                                                            java.util.Arrays
                                                                    .asList("January", "February", "March", "April",
                                                                            "May", "June",
                                                                            "July", "August", "September", "October",
                                                                            "November", "December")
                                                                    .contains(text)))) {
                                                        label.setStyle("-fx-text-fill: #111827;");
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (Exception ex) {
                                // Ignore styling errors
                            }
                        });
                        pause.play();
                    } catch (Exception e) {
                        // Ignore styling errors
                    }
                });
            }
        });

        Button save = new Button("Generate Certificate");
        save.setStyle(
                "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
        save.setOnMouseEntered(e -> save.setStyle(
                "-fx-background-color: #1d4ed8; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        save.setOnMouseExited(e -> save.setStyle(
                "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        save.setOnAction(e -> {
            if (nameField.getText().isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Name is required", ButtonType.OK).showAndWait();
                return;
            }

            Certificate c = new Certificate();
            c.setType("Death");
            String certNo = certNoField.getText().trim();
            if (certNo.isEmpty()) {
                certNo = certNumberService.generateDeathCertificateNumber();
            }
            c.setCertificateNo(certNo);
            c.setName(nameField.getText());
            c.setParentName(parentField.getText());
            c.setAddress(addressField.getText());
            c.setThalook(thalookField.getText());
            c.setDateOfDeath(deathDatePicker.getValue());
            c.setCause(causeField.getText());
            c.setPlaceOfDeath(placeField.getText());
            c.setIssueDate(issueDatePicker.getValue());
            c.setQrCode("https://mahal.com/verify/" + certNo);

            saveCertificate(c, null, null);
            // Clear form
            certNoField.clear();
            nameField.clear();
            parentField.clear();
            addressField.clear();
            thalookField.clear();
            deathDatePicker.setValue(null);
            causeField.clear();
            placeField.clear();
            issueDatePicker.setValue(null);
        });

        Button cancel = new Button("Clear");
        cancel.setStyle(
                "-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
        cancel.setOnMouseEntered(e -> cancel.setStyle(
                "-fx-background-color: #dc2626; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        cancel.setOnMouseExited(e -> cancel.setStyle(
                "-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        cancel.setOnAction(e -> {
            certNoField.clear();
            nameField.clear();
            parentField.clear();
            addressField.clear();
            thalookField.clear();
            deathDatePicker.setValue(null);
            causeField.clear();
            placeField.clear();
            issueDatePicker.setValue(null);
        });

        HBox buttons = new HBox(10, cancel, save);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(8, 0, 0, 0));

        Label certNoLabel = FormStyler.createFormLabel("Certificate No");
        form.getChildren().addAll(
                title,
                certNoLabel,
                certNoBox,
                FormStyler.createCompactFormField("Name *", nameField),
                FormStyler.createCompactFormField("Parent Name", parentField),
                FormStyler.createCompactFormField("Address", addressField),
                FormStyler.createCompactFormField("Thalook", thalookField),
                FormStyler.createCompactFormField("Date of Death", deathDatePicker),
                FormStyler.createCompactFormField("Cause", causeField),
                FormStyler.createCompactFormField("Place of Death", placeField),
                FormStyler.createCompactFormField("Issued Date", issueDatePicker),
                buttons);

        scrollPane.setContent(form);
        box.getChildren().addAll(scrollPane);
        return box;
    }

    // ========== JAMATH FORM ==========
    private VBox createJamathForm() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(12));

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent;");

        VBox form = new VBox(12);
        styleFormContainer(form);

        Label title = FormStyler.createFormLabel("Add Jamath Certificate");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + StyleHelper.TEXT_GRAY_900 + ";");

        String fieldStyle = "-fx-background-color: #ffffff; -fx-background-radius: 8; -fx-border-radius: 8; "
                + "-fx-border-color: #d1d5db; -fx-border-width: 1; -fx-padding: 8 12; -fx-font-size: 12px;";
        String focusStyle = "-fx-background-color: #ffffff; -fx-background-radius: 8; -fx-border-radius: 8; "
                + "-fx-border-color: #cbd5e1; -fx-border-width: 1; -fx-padding: 8 12; -fx-font-size: 12px;";
        String datePickerStyle = fieldStyle;
        String datePickerFocusStyle = focusStyle;

        TextField certNoField = new TextField();
        certNoField.setPromptText("Certificate No (Auto-generated if empty)");
        certNoField.setPrefHeight(32);
        certNoField.setStyle(fieldStyle);
        certNoField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> certNoField.setStyle(newVal ? focusStyle : fieldStyle));
        Button generateBtn = new Button("Auto-Generate");
        generateBtn.setStyle(
                "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
        generateBtn.setOnMouseEntered(e -> generateBtn.setStyle(
                "-fx-background-color: #1d4ed8; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        generateBtn.setOnMouseExited(e -> generateBtn.setStyle(
                "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        generateBtn.setOnAction(e -> certNoField.setText(certNumberService.generateJamathCertificateNumber()));
        HBox certNoBox = new HBox(10, certNoField, generateBtn);
        certNoBox.setAlignment(Pos.CENTER_LEFT);

        TextField nameField = new TextField();
        nameField.setPromptText("Name *");
        nameField.setPrefHeight(32);
        nameField.setStyle(fieldStyle);
        nameField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> nameField.setStyle(newVal ? focusStyle : fieldStyle));

        TextField parentField = new TextField();
        parentField.setPromptText("Parent Name");
        parentField.setPrefHeight(32);
        parentField.setStyle(fieldStyle);
        parentField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> parentField.setStyle(newVal ? focusStyle : fieldStyle));

        TextArea addressField = new TextArea();
        addressField.setPromptText("Address");
        addressField.setPrefRowCount(2);
        addressField.setPrefHeight(50);
        addressField.setStyle(fieldStyle);
        addressField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> addressField.setStyle(newVal ? focusStyle : fieldStyle));

        TextField thalookField = new TextField();
        thalookField.setPromptText("Thalook");
        thalookField.setPrefHeight(32);
        thalookField.setStyle(fieldStyle);
        thalookField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> thalookField.setStyle(newVal ? focusStyle : fieldStyle));

        DatePicker datePicker = new DatePicker();
        datePicker.setPromptText("Date");
        datePicker.setPrefHeight(32);
        datePicker.setStyle(datePickerStyle + " -fx-text-fill: #111827;");
        datePicker.focusedProperty().addListener((obs, oldVal, newVal) -> datePicker
                .setStyle((newVal ? datePickerFocusStyle : datePickerStyle) + " -fx-text-fill: #111827;"));
        datePicker.getEditor().setStyle("-fx-text-fill: #111827;");

        // Style the month and year labels in the popup programmatically
        datePicker.showingProperty().addListener((obs, wasShowing, isShowing) -> {
            if (isShowing) {
                javafx.application.Platform.runLater(() -> {
                    try {
                        // Use a small delay to ensure popup is fully rendered
                        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                                javafx.util.Duration.millis(50));
                        pause.setOnFinished(e -> {
                            try {
                                // Look up all scenes (including popup windows)
                                for (javafx.stage.Window window : javafx.stage.Window.getWindows()) {
                                    if (window instanceof javafx.stage.PopupWindow) {
                                        javafx.scene.Scene scene = ((javafx.stage.PopupWindow) window).getScene();
                                        if (scene != null) {
                                            java.util.Set<javafx.scene.Node> labels = scene.getRoot()
                                                    .lookupAll(".label");
                                            for (javafx.scene.Node node : labels) {
                                                if (node instanceof javafx.scene.control.Label) {
                                                    javafx.scene.control.Label label = (javafx.scene.control.Label) node;
                                                    String text = label.getText();
                                                    // Style month/year labels (typically contain month names or years)
                                                    if ((text != null && (text.matches("\\d{4}") ||
                                                            java.util.Arrays
                                                                    .asList("January", "February", "March", "April",
                                                                            "May", "June",
                                                                            "July", "August", "September", "October",
                                                                            "November", "December")
                                                                    .contains(text)))) {
                                                        label.setStyle("-fx-text-fill: #111827;");
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (Exception ex) {
                                // Ignore styling errors
                            }
                        });
                        pause.play();
                    } catch (Exception e) {
                        // Ignore styling errors
                    }
                });
            }
        });

        TextArea remarksField = new TextArea();
        remarksField.setPromptText("Remarks");
        remarksField.setPrefRowCount(2);
        remarksField.setPrefHeight(50);
        remarksField.setStyle(fieldStyle);
        remarksField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> remarksField.setStyle(newVal ? focusStyle : fieldStyle));

        Button save = new Button("Generate Certificate");
        save.setStyle(
                "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
        save.setOnMouseEntered(e -> save.setStyle(
                "-fx-background-color: #1d4ed8; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        save.setOnMouseExited(e -> save.setStyle(
                "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        save.setOnAction(e -> {
            if (nameField.getText().isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Name is required", ButtonType.OK).showAndWait();
                return;
            }

            Certificate c = new Certificate();
            c.setType("Jamath");
            String certNo = certNoField.getText().trim();
            if (certNo.isEmpty()) {
                certNo = certNumberService.generateJamathCertificateNumber();
            }
            c.setCertificateNo(certNo);
            c.setName(nameField.getText());
            c.setParentName(parentField.getText());
            c.setAddress(addressField.getText());
            c.setThalook(thalookField.getText());
            c.setIssueDate(datePicker.getValue());
            c.setRemarks(remarksField.getText());
            c.setQrCode("https://mahal.com/verify/" + certNo);

            saveCertificate(c, null, null);
            // Clear form
            certNoField.clear();
            nameField.clear();
            parentField.clear();
            addressField.clear();
            thalookField.clear();
            datePicker.setValue(null);
            remarksField.clear();
        });

        Button cancel = new Button("Clear");
        cancel.setStyle(
                "-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
        cancel.setOnMouseEntered(e -> cancel.setStyle(
                "-fx-background-color: #dc2626; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        cancel.setOnMouseExited(e -> cancel.setStyle(
                "-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        cancel.setOnAction(e -> {
            certNoField.clear();
            nameField.clear();
            parentField.clear();
            addressField.clear();
            thalookField.clear();
            datePicker.setValue(null);
            remarksField.clear();
        });

        HBox buttons = new HBox(10, cancel, save);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(8, 0, 0, 0));

        Label certNoLabel = FormStyler.createFormLabel("Certificate No");
        form.getChildren().addAll(
                title,
                certNoLabel,
                certNoBox,
                FormStyler.createCompactFormField("Name *", nameField),
                FormStyler.createCompactFormField("Parent Name", parentField),
                FormStyler.createCompactFormField("Address", addressField),
                FormStyler.createCompactFormField("Thalook", thalookField),
                FormStyler.createCompactFormField("Date", datePicker),
                FormStyler.createCompactFormField("Remarks", remarksField),
                buttons);

        scrollPane.setContent(form);
        box.getChildren().addAll(scrollPane);
        return box;
    }

    // ========== CUSTOM FORM ==========
    private VBox createCustomForm() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(12));

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent;");

        VBox form = new VBox(12);
        styleFormContainer(form);

        Label title = FormStyler.createFormLabel("Add Custom Certificate");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + StyleHelper.TEXT_GRAY_900 + ";");

        String fieldStyle = "-fx-background-color: #ffffff; -fx-background-radius: 8; -fx-border-radius: 8; "
                + "-fx-border-color: #d1d5db; -fx-border-width: 1; -fx-padding: 8 12; -fx-font-size: 12px;";
        String focusStyle = "-fx-background-color: #ffffff; -fx-background-radius: 8; -fx-border-radius: 8; "
                + "-fx-border-color: #cbd5e1; -fx-border-width: 1; -fx-padding: 8 12; -fx-font-size: 12px;";
        String datePickerStyle = fieldStyle;
        String datePickerFocusStyle = focusStyle;

        TextField certNoField = new TextField();
        certNoField.setPromptText("Certificate No (Auto-generated if empty)");
        certNoField.setPrefHeight(32);
        certNoField.setStyle(fieldStyle);
        certNoField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> certNoField.setStyle(newVal ? focusStyle : fieldStyle));
        Button generateBtn = new Button("Auto-Generate");
        generateBtn.setStyle(
                "-fx-background-color: #6b7280; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
        generateBtn.setOnMouseEntered(e -> generateBtn.setStyle(
                "-fx-background-color: #4b5563; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        generateBtn.setOnMouseExited(e -> generateBtn.setStyle(
                "-fx-background-color: #6b7280; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        generateBtn.setOnAction(e -> certNoField.setText(certNumberService.generateCustomCertificateNumber()));
        HBox certNoBox = new HBox(10, certNoField, generateBtn);
        certNoBox.setAlignment(Pos.CENTER_LEFT);

        TextField templateNameField = new TextField();
        templateNameField.setPromptText("Template Name *");
        templateNameField.setPrefHeight(32);
        templateNameField.setStyle(fieldStyle);
        templateNameField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> templateNameField.setStyle(newVal ? focusStyle : fieldStyle));

        TextArea templateContentField = new TextArea();
        templateContentField.setPromptText("Template Content (HTML/Text). This content will be displayed in the PDF.");
        templateContentField.setPrefRowCount(8);
        templateContentField.setPrefHeight(200);
        templateContentField.setStyle(fieldStyle);
        templateContentField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> templateContentField.setStyle(newVal ? focusStyle : fieldStyle));

        DatePicker issueDatePicker = new DatePicker();
        issueDatePicker.setPromptText("Issued Date");
        issueDatePicker.setPrefHeight(32);
        issueDatePicker.setStyle(datePickerStyle);
        issueDatePicker.focusedProperty().addListener(
                (obs, oldVal, newVal) -> issueDatePicker.setStyle(newVal ? datePickerFocusStyle : datePickerStyle));

        Button save = new Button("Generate Certificate");
        save.setStyle(
                "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
        save.setOnMouseEntered(e -> save.setStyle(
                "-fx-background-color: #1d4ed8; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        save.setOnMouseExited(e -> save.setStyle(
                "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        save.setOnAction(e -> {
            if (templateNameField.getText().isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Template Name is required", ButtonType.OK).showAndWait();
                return;
            }

            Certificate c = new Certificate();
            c.setType("Custom");
            String certNo = certNoField.getText().trim();
            if (certNo.isEmpty()) {
                certNo = certNumberService.generateCustomCertificateNumber();
            }
            c.setCertificateNo(certNo);
            c.setTemplateName(templateNameField.getText());
            c.setTemplateContent(templateContentField.getText());
            c.setIssueDate(issueDatePicker.getValue());
            c.setQrCode("https://mahal.com/verify/" + certNo);

            saveCertificate(c, null, null);
            // Clear form
            certNoField.clear();
            templateNameField.clear();
            templateContentField.clear();
            issueDatePicker.setValue(null);
        });

        Button cancel = new Button("Clear");
        cancel.setStyle(
                "-fx-background-color: #6b7280; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
        cancel.setOnMouseEntered(e -> cancel.setStyle(
                "-fx-background-color: #4b5563; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        cancel.setOnMouseExited(e -> cancel.setStyle(
                "-fx-background-color: #6b7280; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        cancel.setOnAction(e -> {
            certNoField.clear();
            templateNameField.clear();
            templateContentField.clear();
            issueDatePicker.setValue(null);
        });

        HBox buttons = new HBox(10, cancel, save);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(8, 0, 0, 0));

        Label certNoLabel = FormStyler.createFormLabel("Certificate No");
        form.getChildren().addAll(
                title,
                certNoLabel,
                certNoBox,
                FormStyler.createCompactFormField("Template Name *", templateNameField),
                FormStyler.createCompactFormField("Template Content", templateContentField),
                FormStyler.createCompactFormField("Issued Date", issueDatePicker),
                buttons);

        scrollPane.setContent(form);
        box.getChildren().addAll(scrollPane);
        return box;
    }

    // ========== MARRIAGE LIST ==========
    private VBox createMarriageList() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(12));

        // Modern Action Row
        HBox actionRow = new HBox(15);
        actionRow.setAlignment(Pos.CENTER_LEFT);
        actionRow.setPadding(new Insets(0, 0, 10, 0));

        marriageSearch = new TextField();
        marriageSearch.setPromptText("🔍 Search certificates...");
        marriageSearch.setPrefWidth(300);
        StyleHelper.styleTextField(marriageSearch);
        marriageSearch.textProperty().addListener((obs, old, val) -> {
            javafx.application.Platform.runLater(() -> {
                javafx.util.Duration delay = javafx.util.Duration.millis(500);
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(delay);
                pause.setOnFinished(e -> loadMarriageList());
                pause.play();
            });
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        actionRow.getChildren().addAll(marriageSearch, spacer);

        TableView<Certificate> table = new TableView<>();
        TableStyler.applyModernStyling(table);

        TableColumn<Certificate, String> certNoCol = new TableColumn<>("CERTIFICATE NO");
        certNoCol.setCellValueFactory(cell -> {
            String no = cell.getValue().getCertificateNo();
            return new javafx.beans.property.SimpleStringProperty(no != null ? no : "");
        });
        TableStyler.styleTableColumn(certNoCol);

        TableColumn<Certificate, String> groomCol = new TableColumn<>("GROOM");
        groomCol.setCellValueFactory(cell -> {
            String name = cell.getValue().getGroomName();
            return new javafx.beans.property.SimpleStringProperty(name != null ? name : "");
        });
        TableStyler.styleTableColumn(groomCol);

        TableColumn<Certificate, String> brideCol = new TableColumn<>("BRIDE");
        brideCol.setCellValueFactory(cell -> {
            String name = cell.getValue().getBrideName();
            return new javafx.beans.property.SimpleStringProperty(name != null ? name : "");
        });
        TableStyler.styleTableColumn(brideCol);

        TableColumn<Certificate, String> dateCol = new TableColumn<>("MARRIAGE DATE");
        dateCol.setCellValueFactory(cell -> {
            LocalDate d = cell.getValue().getMarriageDate();
            return new javafx.beans.property.SimpleStringProperty(d != null ? d.toString() : "");
        });
        TableStyler.styleTableColumn(dateCol);

        TableColumn<Certificate, String> statusCol = new TableColumn<>("STATUS");
        statusCol.setCellValueFactory(cell -> {
            String status = cell.getValue().getMarriageStatus();
            return new javafx.beans.property.SimpleStringProperty(status != null ? status : "");
        });
        TableStyler.styleTableColumn(statusCol);

        TableColumn<Certificate, String> actionsCol = createActionsColumn("Marriage", table);
        table.getColumns().addAll(certNoCol, groomCol, brideCol, dateCol, statusCol, actionsCol);
        table.setItems(marriageList);

        box.getChildren().addAll(actionRow, table);
        loadMarriageList();
        return box;
    }

    private void loadMarriageList() {
        new Thread(() -> {
            try {
                var data = certificateDAO.getByTypeWithFilters("Marriage",
                        marriageSearch.getText(), null,
                        null, null, null);
                javafx.application.Platform.runLater(() -> {
                    marriageList.setAll(data);
                });
            } catch (Exception e) {
                System.err.println("Error loading marriage certificates: " + e.getMessage());
            }
        }).start();
    }

    // ========== DEATH LIST ==========
    private VBox createDeathList() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(12));

        // Modern Action Row
        HBox actionRow = new HBox(15);
        actionRow.setAlignment(Pos.CENTER_LEFT);
        actionRow.setPadding(new Insets(0, 0, 10, 0));

        deathSearch = new TextField();
        deathSearch.setPromptText("🔍 Search certificates...");
        deathSearch.setPrefWidth(300);
        StyleHelper.styleTextField(deathSearch);
        deathSearch.textProperty().addListener((obs, old, val) -> {
            javafx.application.Platform.runLater(() -> {
                javafx.util.Duration delay = javafx.util.Duration.millis(500);
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(delay);
                pause.setOnFinished(e -> loadDeathList());
                pause.play();
            });
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        actionRow.getChildren().addAll(deathSearch, spacer);

        TableView<Certificate> table = new TableView<>();
        TableStyler.applyModernStyling(table);

        TableColumn<Certificate, String> certNoCol = new TableColumn<>("CERTIFICATE NO");
        certNoCol.setCellValueFactory(cell -> {
            String no = cell.getValue().getCertificateNo();
            return new javafx.beans.property.SimpleStringProperty(no != null ? no : "");
        });
        TableStyler.styleTableColumn(certNoCol);

        TableColumn<Certificate, String> nameCol = new TableColumn<>("NAME");
        nameCol.setCellValueFactory(cell -> {
            String name = cell.getValue().getName();
            return new javafx.beans.property.SimpleStringProperty(name != null ? name : "");
        });
        TableStyler.styleTableColumn(nameCol);

        TableColumn<Certificate, String> parentCol = new TableColumn<>("PARENT NAME");
        parentCol.setCellValueFactory(cell -> {
            String name = cell.getValue().getParentName();
            return new javafx.beans.property.SimpleStringProperty(name != null ? name : "");
        });
        TableStyler.styleTableColumn(parentCol);

        TableColumn<Certificate, String> dateCol = new TableColumn<>("DEATH DATE");
        dateCol.setCellValueFactory(cell -> {
            LocalDate d = cell.getValue().getDateOfDeath();
            return new javafx.beans.property.SimpleStringProperty(d != null ? d.toString() : "");
        });
        TableStyler.styleTableColumn(dateCol);

        TableColumn<Certificate, String> placeCol = new TableColumn<>("PLACE");
        placeCol.setCellValueFactory(cell -> {
            String place = cell.getValue().getPlaceOfDeath();
            return new javafx.beans.property.SimpleStringProperty(place != null ? place : "");
        });
        TableStyler.styleTableColumn(placeCol);

        TableColumn<Certificate, String> issueDateCol = new TableColumn<>("ISSUED DATE");
        issueDateCol.setCellValueFactory(cell -> {
            LocalDate d = cell.getValue().getIssueDate();
            return new javafx.beans.property.SimpleStringProperty(d != null ? d.toString() : "");
        });
        TableStyler.styleTableColumn(issueDateCol);

        TableColumn<Certificate, String> actionsCol = createActionsColumn("Death", table);
        table.getColumns().addAll(certNoCol, nameCol, parentCol, dateCol, placeCol, issueDateCol, actionsCol);
        table.setItems(deathList);

        box.getChildren().addAll(actionRow, table);
        loadDeathList();
        return box;
    }

    private void loadDeathList() {
        new Thread(() -> {
            try {
                var data = certificateDAO.getByTypeWithFilters("Death",
                        deathSearch.getText(), null,
                        null, null, null);
                javafx.application.Platform.runLater(() -> {
                    deathList.setAll(data);
                });
            } catch (Exception e) {
                System.err.println("Error loading death certificates: " + e.getMessage());
            }
        }).start();
    }

    // ========== JAMATH LIST ==========
    private VBox createJamathList() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(12));

        // Modern Action Row
        HBox actionRow = new HBox(15);
        actionRow.setAlignment(Pos.CENTER_LEFT);
        actionRow.setPadding(new Insets(0, 0, 10, 0));

        jamathSearch = new TextField();
        jamathSearch.setPromptText("🔍 Search certificates...");
        jamathSearch.setPrefWidth(300);
        StyleHelper.styleTextField(jamathSearch);
        jamathSearch.textProperty().addListener((obs, old, val) -> {
            javafx.application.Platform.runLater(() -> {
                javafx.util.Duration delay = javafx.util.Duration.millis(500);
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(delay);
                pause.setOnFinished(e -> loadJamathList());
                pause.play();
            });
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        actionRow.getChildren().addAll(jamathSearch, spacer);

        TableView<Certificate> table = new TableView<>();
        TableStyler.applyModernStyling(table);

        TableColumn<Certificate, String> certNoCol = new TableColumn<>("CERTIFICATE NO");
        certNoCol.setCellValueFactory(cell -> {
            String no = cell.getValue().getCertificateNo();
            return new javafx.beans.property.SimpleStringProperty(no != null ? no : "");
        });
        TableStyler.styleTableColumn(certNoCol);

        TableColumn<Certificate, String> nameCol = new TableColumn<>("NAME");
        nameCol.setCellValueFactory(cell -> {
            String name = cell.getValue().getName();
            return new javafx.beans.property.SimpleStringProperty(name != null ? name : "");
        });
        TableStyler.styleTableColumn(nameCol);

        TableColumn<Certificate, String> parentCol = new TableColumn<>("PARENT NAME");
        parentCol.setCellValueFactory(cell -> {
            String name = cell.getValue().getParentName();
            return new javafx.beans.property.SimpleStringProperty(name != null ? name : "");
        });
        TableStyler.styleTableColumn(parentCol);

        TableColumn<Certificate, String> thalookCol = new TableColumn<>("THALOOK");
        thalookCol.setCellValueFactory(cell -> {
            String thalook = cell.getValue().getThalook();
            return new javafx.beans.property.SimpleStringProperty(thalook != null ? thalook : "");
        });
        TableStyler.styleTableColumn(thalookCol);

        TableColumn<Certificate, String> dateCol = new TableColumn<>("DATE");
        dateCol.setCellValueFactory(cell -> {
            LocalDate d = cell.getValue().getIssueDate();
            return new javafx.beans.property.SimpleStringProperty(d != null ? d.toString() : "");
        });
        TableStyler.styleTableColumn(dateCol);

        TableColumn<Certificate, String> remarksCol = new TableColumn<>("REMARKS");
        remarksCol.setCellValueFactory(cell -> {
            String remarks = cell.getValue().getRemarks();
            return new javafx.beans.property.SimpleStringProperty(remarks != null ? remarks : "");
        });
        TableStyler.styleTableColumn(remarksCol);

        TableColumn<Certificate, String> actionsCol = createActionsColumn("Jamath", table);
        table.getColumns().addAll(certNoCol, nameCol, parentCol, thalookCol, dateCol, remarksCol, actionsCol);
        table.setItems(jamathList);

        box.getChildren().addAll(actionRow, table);
        loadJamathList();
        return box;
    }

    private void loadJamathList() {
        new Thread(() -> {
            try {
                var data = certificateDAO.getByTypeWithFilters("Jamath",
                        jamathSearch.getText(), null,
                        null, null, null);
                javafx.application.Platform.runLater(() -> {
                    jamathList.setAll(data);
                });
            } catch (Exception e) {
                System.err.println("Error loading jamath certificates: " + e.getMessage());
            }
        }).start();
    }

    // ========== CUSTOM LIST ==========
    private VBox createCustomList() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(12));

        // Modern Action Row
        HBox actionRow = new HBox(15);
        actionRow.setAlignment(Pos.CENTER_LEFT);
        actionRow.setPadding(new Insets(0, 0, 10, 0));

        customSearch = new TextField();
        customSearch.setPromptText("🔍 Search certificates...");
        customSearch.setPrefWidth(300);
        StyleHelper.styleTextField(customSearch);
        customSearch.textProperty().addListener((obs, old, val) -> {
            javafx.application.Platform.runLater(() -> {
                javafx.util.Duration delay = javafx.util.Duration.millis(500);
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(delay);
                pause.setOnFinished(e -> loadCustomList());
                pause.play();
            });
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        actionRow.getChildren().addAll(customSearch, spacer);

        TableView<Certificate> table = new TableView<>();
        TableStyler.applyModernStyling(table);

        TableColumn<Certificate, String> certNoCol = new TableColumn<>("CERTIFICATE NO");
        certNoCol.setCellValueFactory(cell -> {
            String no = cell.getValue().getCertificateNo();
            return new javafx.beans.property.SimpleStringProperty(no != null ? no : "");
        });
        TableStyler.styleTableColumn(certNoCol);

        TableColumn<Certificate, String> templateCol = new TableColumn<>("TEMPLATE NAME");
        templateCol.setCellValueFactory(cell -> {
            String name = cell.getValue().getTemplateName();
            return new javafx.beans.property.SimpleStringProperty(name != null ? name : "");
        });
        TableStyler.styleTableColumn(templateCol);
        templateCol.setPrefWidth(200);

        TableColumn<Certificate, String> contentCol = new TableColumn<>("CONTENT PREVIEW");
        contentCol.setCellValueFactory(cell -> {
            String content = cell.getValue().getTemplateContent();
            if (content != null && content.length() > 100) {
                content = content.substring(0, 100) + "...";
            }
            return new javafx.beans.property.SimpleStringProperty(content != null ? content : "");
        });
        TableStyler.styleTableColumn(contentCol);
        contentCol.setPrefWidth(300);

        TableColumn<Certificate, String> issueDateCol = new TableColumn<>("ISSUED DATE");
        issueDateCol.setCellValueFactory(cell -> {
            LocalDate d = cell.getValue().getIssueDate();
            return new javafx.beans.property.SimpleStringProperty(d != null ? d.toString() : "");
        });
        TableStyler.styleTableColumn(issueDateCol);
        issueDateCol.setPrefWidth(120);

        TableColumn<Certificate, String> actionsCol = createActionsColumn("Custom", table);
        table.getColumns().addAll(certNoCol, templateCol, contentCol, issueDateCol, actionsCol);
        table.setItems(customList);

        box.getChildren().addAll(actionRow, table);
        loadCustomList();
        return box;
    }

    private void loadCustomList() {
        new Thread(() -> {
            try {
                String searchText = customSearch != null ? customSearch.getText() : null;
                String certNoText = customCertNo != null ? customCertNo.getText() : null;
                String templateNameText = customTemplateName != null ? customTemplateName.getText() : null;

                var data = certificateDAO.getByTypeWithFilters("Custom",
                        searchText, certNoText, templateNameText, null, null);
                javafx.application.Platform.runLater(() -> {
                    customList.setAll(data);
                });
            } catch (Exception e) {
                System.err.println("Error loading custom certificates: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    // ========== ACTIONS COLUMN ==========
    private TableColumn<Certificate, String> createActionsColumn(String type, TableView<Certificate> table) {
        TableColumn<Certificate, String> actionsCol = new TableColumn<>("Actions");
        actionsCol.setMinWidth(220);
        actionsCol.setCellFactory(col -> new TableCell<Certificate, String>() {
            private final Button downloadBtn = new Button("Download");
            private final Button regenerateBtn = new Button("Regenerate");
            private final Button editBtn = new Button("Edit");
            private final Button revokeBtn = new Button("Revoke");

            {
                downloadBtn.setPrefWidth(80);
                regenerateBtn.setPrefWidth(95);
                editBtn.setPrefWidth(60);
                revokeBtn.setPrefWidth(60);

                downloadBtn.setStyle(
                        "-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 3;");
                downloadBtn.setOnMouseEntered(e -> downloadBtn.setStyle(
                        "-fx-background-color: #2980b9; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 3;"));
                downloadBtn.setOnMouseExited(e -> downloadBtn.setStyle(
                        "-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 3;"));

                regenerateBtn.setStyle(
                        "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 3;");
                regenerateBtn.setOnMouseEntered(e -> regenerateBtn.setStyle(
                        "-fx-background-color: #229954; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 3;"));
                regenerateBtn.setOnMouseExited(e -> regenerateBtn.setStyle(
                        "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 3;"));

                editBtn.setStyle(StyleHelper.getEditButtonStyle());
                editBtn.setOnMouseEntered(e -> editBtn.setStyle(StyleHelper.getEditButtonHoverStyle()));
                editBtn.setOnMouseExited(e -> editBtn.setStyle(StyleHelper.getEditButtonStyle()));

                revokeBtn.setStyle(StyleHelper.getDangerButtonStyle());
                revokeBtn.setOnMouseEntered(e -> revokeBtn.setStyle(StyleHelper.getDangerButtonHoverStyle()));
                revokeBtn.setOnMouseExited(e -> revokeBtn.setStyle(StyleHelper.getDangerButtonStyle()));

                downloadBtn.setOnAction(e -> {
                    Certificate c = getTableView().getItems().get(getIndex());
                    downloadCertificate(c, type);
                });

                regenerateBtn.setOnAction(e -> {
                    Certificate c = getTableView().getItems().get(getIndex());
                    regenerateCertificate(c, type);
                });

                editBtn.setOnAction(e -> {
                    Certificate c = getTableView().getItems().get(getIndex());
                    showEditDialog(type, c);
                });

                revokeBtn.setOnAction(e -> {
                    Certificate c = getTableView().getItems().get(getIndex());
                    revokeCertificate(c, type);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox h = new HBox(5);
                    h.setAlignment(Pos.CENTER);
                    h.getChildren().addAll(downloadBtn, regenerateBtn, editBtn, revokeBtn);
                    setGraphic(h);
                }
            }
        });
        TableStyler.styleTableColumn(actionsCol);
        return actionsCol;
    }

    // ========== ACTION METHODS ==========
    private void downloadCertificate(Certificate c, String type) {
        if (c.getPdfPath() == null || c.getPdfPath().isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "PDF not generated yet. Please regenerate the certificate.",
                    ButtonType.OK).showAndWait();
            return;
        }

        File pdfFile = new File(c.getPdfPath());
        if (!pdfFile.exists()) {
            new Alert(Alert.AlertType.WARNING, "PDF file not found. Please regenerate the certificate.", ButtonType.OK)
                    .showAndWait();
            return;
        }

        // Check if file is readable
        if (!pdfFile.canRead()) {
            new Alert(Alert.AlertType.ERROR, "Cannot read PDF file. Please check file permissions.", ButtonType.OK)
                    .showAndWait();
            return;
        }

        // Verify it's a valid PDF
        try {
            byte[] header = new byte[4];
            java.io.FileInputStream fis = new java.io.FileInputStream(pdfFile);
            fis.read(header);
            fis.close();
            String headerStr = new String(header);
            if (!headerStr.equals("%PDF")) {
                new Alert(Alert.AlertType.ERROR, "PDF file appears to be corrupted. Please regenerate the certificate.",
                        ButtonType.OK).showAndWait();
                return;
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Error reading PDF file: " + e.getMessage(), ButtonType.OK).showAndWait();
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Certificate PDF");
        fileChooser
                .setInitialFileName(c.getCertificateNo() != null ? c.getCertificateNo() + ".pdf" : "certificate.pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        File saveFile = fileChooser.showSaveDialog(null);
        if (saveFile != null) {
            try {
                // Ensure parent directory exists
                File parentDir = saveFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }

                // Check if target file is locked (try to delete if exists and replace is
                // needed)
                if (saveFile.exists() && !saveFile.canWrite()) {
                    new Alert(Alert.AlertType.ERROR,
                            "Cannot write to selected location. The file may be open in another program.",
                            ButtonType.OK).showAndWait();
                    return;
                }

                // Copy the file
                Files.copy(pdfFile.toPath(), saveFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                // Verify the copy was successful
                if (saveFile.exists() && saveFile.length() > 0) {
                    new Alert(Alert.AlertType.INFORMATION,
                            "Certificate downloaded successfully to:\n" + saveFile.getAbsolutePath(), ButtonType.OK)
                            .showAndWait();
                } else {
                    new Alert(Alert.AlertType.ERROR, "Download completed but file verification failed.", ButtonType.OK)
                            .showAndWait();
                }
            } catch (java.nio.file.AccessDeniedException e) {
                new Alert(Alert.AlertType.ERROR,
                        "Access denied: You don't have permission to write to the selected location.", ButtonType.OK)
                        .showAndWait();
            } catch (java.nio.file.FileSystemException e) {
                new Alert(Alert.AlertType.ERROR,
                        "File access error: The file may be open in another program (e.g., Adobe Reader). Please close it and try again.",
                        ButtonType.OK).showAndWait();
            } catch (java.io.IOException e) {
                new Alert(Alert.AlertType.ERROR, "IO Error: " + e.getMessage()
                        + "\n\nPlease ensure:\n- The file is not open in another program\n- You have write permissions to the destination",
                        ButtonType.OK).showAndWait();
            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR, "Failed to download certificate: " + e.getMessage()
                        + "\n\nError type: " + e.getClass().getSimpleName(), ButtonType.OK).showAndWait();
                e.printStackTrace();
            }
        }
    }

    private void regenerateCertificate(Certificate c, String type) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Regenerate PDF for this certificate?", ButtonType.OK,
                ButtonType.CANCEL);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        Certificate updated = certificateDAO.getById(c.getId(), type);
                        if (updated != null) {
                            // Regenerate PDF for marriage certificates
                            if ("Marriage".equals(type)) {
                                String pdfPath = CertificatePDFService.saveMarriageCertificateHTML(updated);
                                updated.setPdfPath(pdfPath);
                                certificateDAO.update(updated);
                            }
                            // Regenerate PDF for death certificates
                            else if ("Death".equals(type)) {
                                String pdfPath = CertificatePDFService.saveDeathCertificateHTML(updated);
                                updated.setPdfPath(pdfPath);
                                certificateDAO.update(updated);
                            }
                            // Regenerate PDF for Jamath certificates
                            else if ("Jamath".equals(type)) {
                                String pdfPath = CertificatePDFService.saveJamathCertificateHTML(updated);
                                updated.setPdfPath(pdfPath);
                                certificateDAO.update(updated);
                            }
                            // Regenerate PDF for Custom certificates
                            else if ("Custom".equals(type) && updated.getTemplateContent() != null
                                    && !updated.getTemplateContent().isEmpty()) {
                                String pdfPath = CertificatePDFService.saveCustomCertificateHTML(updated);
                                updated.setPdfPath(pdfPath);
                                certificateDAO.update(updated);
                            }
                            javafx.application.Platform.runLater(() -> {
                                new Alert(Alert.AlertType.INFORMATION, "Certificate PDF regenerated successfully!",
                                        ButtonType.OK).showAndWait();
                                if ("Marriage".equals(type))
                                    loadMarriageList();
                                else if ("Death".equals(type))
                                    loadDeathList();
                                else if ("Jamath".equals(type))
                                    loadJamathList();
                                else if ("Custom".equals(type))
                                    loadCustomList();
                            });
                        }
                    } catch (Exception e) {
                        javafx.application.Platform.runLater(() -> {
                            new Alert(Alert.AlertType.ERROR, "Failed to regenerate certificate: " + e.getMessage(),
                                    ButtonType.OK).showAndWait();
                        });
                    }
                }).start();
            }
        });
    }

    private void showEditDialog(String type, Certificate existing) {
        Stage dialog = new Stage();
        dialog.setTitle("Edit " + type + " Certificate");

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-border-color: white; -fx-border-width: 0;");

        VBox root = new VBox(12);
        styleFormContainer(root);

        Label titleLabel = new Label("Edit " + type + " Certificate");
        titleLabel.setStyle(
                "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + StyleHelper.TEXT_GRAY_900 + ";");

        if ("Marriage".equals(type)) {
            TextField certNoField = new TextField(existing.getCertificateNo());
            certNoField.setEditable(false);
            FormStyler.applyCompactFieldStyle(certNoField);

            TextField groomField = new TextField(existing.getGroomName());
            groomField.setPromptText("Groom Name *");

            TextField brideField = new TextField(existing.getBrideName());
            brideField.setPromptText("Bride Name *");

            TextField groomParentField = new TextField(existing.getParentNameOfGroom());
            groomParentField.setPromptText("Groom's Parent Name");

            TextField brideParentField = new TextField(existing.getParentNameOfBride());
            brideParentField.setPromptText("Bride's Parent Name");

            TextArea groomAddressField = new TextArea(existing.getAddressOfGroom());
            groomAddressField.setPromptText("Groom's Address");
            groomAddressField.setPrefRowCount(2);
            groomAddressField.setPrefHeight(50);

            TextArea brideAddressField = new TextArea(existing.getAddressOfBride());
            brideAddressField.setPromptText("Bride's Address");
            brideAddressField.setPrefRowCount(2);
            brideAddressField.setPrefHeight(50);

            TextField placeField = new TextField(existing.getPlaceOfMarriage());
            placeField.setPromptText("Place of Marriage");

            ComboBox<String> statusCombo = new ComboBox<>();
            statusCombo.getItems().addAll("Registered", "Unregistered");
            statusCombo.setValue(existing.getMarriageStatus());
            statusCombo.setPromptText("Marriage Status");

            DatePicker marriageDatePicker = new DatePicker(existing.getMarriageDate());
            marriageDatePicker.setPromptText("Marriage Date");

            TextArea notesField = new TextArea(existing.getAdditionalNotes());
            notesField.setPromptText("Additional Notes");
            notesField.setPrefRowCount(2);
            notesField.setPrefHeight(50);

            Button save = new Button("Update");
            save.setStyle(
                    "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
            save.setOnMouseEntered(e -> save.setStyle(
                    "-fx-background-color: #1d4ed8; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
            save.setOnMouseExited(e -> save.setStyle(
                    "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
            save.setOnAction(e -> {
                existing.setGroomName(groomField.getText());
                existing.setBrideName(brideField.getText());
                existing.setParentNameOfGroom(groomParentField.getText());
                existing.setParentNameOfBride(brideParentField.getText());
                existing.setAddressOfGroom(groomAddressField.getText());
                existing.setAddressOfBride(brideAddressField.getText());
                existing.setPlaceOfMarriage(placeField.getText());
                existing.setMarriageStatus(statusCombo.getValue());
                existing.setMarriageDate(marriageDatePicker.getValue());
                existing.setAdditionalNotes(notesField.getText());
                saveCertificate(existing, existing, dialog);
            });

            Button cancel = new Button("Cancel");
            cancel.setStyle(
                    "-fx-background-color: #6b7280; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
            cancel.setOnMouseEntered(e -> cancel.setStyle(
                    "-fx-background-color: #4b5563; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
            cancel.setOnMouseExited(e -> cancel.setStyle(
                    "-fx-background-color: #6b7280; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
            cancel.setOnAction(e -> dialog.close());

            HBox buttons = new HBox(10, cancel, save);
            buttons.setAlignment(Pos.CENTER_RIGHT);
            buttons.setPadding(new Insets(8, 0, 0, 0));

            Label certNoLabel = FormStyler.createFormLabel("Certificate No");
            root.getChildren().addAll(
                    titleLabel,
                    certNoLabel,
                    certNoField,
                    FormStyler.createCompactFormField("Groom Name *", groomField),
                    FormStyler.createCompactFormField("Bride Name *", brideField),
                    FormStyler.createCompactFormField("Groom's Parent Name", groomParentField),
                    FormStyler.createCompactFormField("Bride's Parent Name", brideParentField),
                    FormStyler.createCompactFormField("Groom's Address", groomAddressField),
                    FormStyler.createCompactFormField("Bride's Address", brideAddressField),
                    FormStyler.createCompactFormField("Place of Marriage", placeField),
                    FormStyler.createCompactFormField("Marriage Status", statusCombo),
                    FormStyler.createCompactFormField("Marriage Date", marriageDatePicker),
                    FormStyler.createCompactFormField("Additional Notes", notesField),
                    buttons);
        } else if ("Death".equals(type)) {
            TextField certNoField = new TextField(existing.getCertificateNo());
            certNoField.setEditable(false);
            FormStyler.applyCompactFieldStyle(certNoField);

            TextField nameField = new TextField(existing.getName());
            nameField.setPromptText("Name *");

            TextField parentField = new TextField(existing.getParentName());
            parentField.setPromptText("Parent Name");

            TextArea addressField = new TextArea(existing.getAddress());
            addressField.setPromptText("Address");
            addressField.setPrefRowCount(2);
            addressField.setPrefHeight(50);

            TextField thalookField = new TextField(existing.getThalook());
            thalookField.setPromptText("Thalook");

            DatePicker deathDatePicker = new DatePicker(existing.getDateOfDeath());
            deathDatePicker.setPromptText("Date of Death");

            TextField causeField = new TextField(existing.getCause());
            causeField.setPromptText("Cause");

            TextField placeField = new TextField(existing.getPlaceOfDeath());
            placeField.setPromptText("Place of Death");

            DatePicker issueDatePicker = new DatePicker(existing.getIssueDate());
            issueDatePicker.setPromptText("Issued Date");

            Button save = new Button("Update");
            save.setStyle(
                    "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
            save.setOnMouseEntered(e -> save.setStyle(
                    "-fx-background-color: #1d4ed8; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
            save.setOnMouseExited(e -> save.setStyle(
                    "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
            save.setOnAction(e -> {
                existing.setName(nameField.getText());
                existing.setParentName(parentField.getText());
                existing.setAddress(addressField.getText());
                existing.setThalook(thalookField.getText());
                existing.setDateOfDeath(deathDatePicker.getValue());
                existing.setCause(causeField.getText());
                existing.setPlaceOfDeath(placeField.getText());
                existing.setIssueDate(issueDatePicker.getValue());
                saveCertificate(existing, existing, dialog);
            });

            Button cancel = new Button("Cancel");
            cancel.setStyle(
                    "-fx-background-color: #6b7280; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
            cancel.setOnMouseEntered(e -> cancel.setStyle(
                    "-fx-background-color: #4b5563; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
            cancel.setOnMouseExited(e -> cancel.setStyle(
                    "-fx-background-color: #6b7280; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
            cancel.setOnAction(e -> dialog.close());

            HBox buttons = new HBox(10, cancel, save);
            buttons.setAlignment(Pos.CENTER_RIGHT);
            buttons.setPadding(new Insets(8, 0, 0, 0));

            Label certNoLabel = FormStyler.createFormLabel("Certificate No");
            root.getChildren().addAll(
                    titleLabel,
                    certNoLabel,
                    certNoField,
                    FormStyler.createCompactFormField("Name *", nameField),
                    FormStyler.createCompactFormField("Parent Name", parentField),
                    FormStyler.createCompactFormField("Address", addressField),
                    FormStyler.createCompactFormField("Thalook", thalookField),
                    FormStyler.createCompactFormField("Date of Death", deathDatePicker),
                    FormStyler.createCompactFormField("Cause", causeField),
                    FormStyler.createCompactFormField("Place of Death", placeField),
                    FormStyler.createCompactFormField("Issued Date", issueDatePicker),
                    buttons);
        } else if ("Jamath".equals(type)) {
            TextField certNoField = new TextField(existing.getCertificateNo());
            certNoField.setEditable(false);
            FormStyler.applyCompactFieldStyle(certNoField);

            TextField nameField = new TextField(existing.getName());
            nameField.setPromptText("Name *");

            TextField parentField = new TextField(existing.getParentName());
            parentField.setPromptText("Parent Name");

            TextArea addressField = new TextArea(existing.getAddress());
            addressField.setPromptText("Address");
            addressField.setPrefRowCount(2);
            addressField.setPrefHeight(50);

            TextField thalookField = new TextField(existing.getThalook());
            thalookField.setPromptText("Thalook");

            DatePicker datePicker = new DatePicker(existing.getIssueDate());
            datePicker.setPromptText("Date");

            TextArea remarksField = new TextArea(existing.getRemarks());
            remarksField.setPromptText("Remarks");
            remarksField.setPrefRowCount(2);
            remarksField.setPrefHeight(50);

            Button save = new Button("Update");
            save.setStyle(
                    "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
            save.setOnMouseEntered(e -> save.setStyle(
                    "-fx-background-color: #1d4ed8; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
            save.setOnMouseExited(e -> save.setStyle(
                    "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
            save.setOnAction(e -> {
                existing.setName(nameField.getText());
                existing.setParentName(parentField.getText());
                existing.setAddress(addressField.getText());
                existing.setThalook(thalookField.getText());
                existing.setIssueDate(datePicker.getValue());
                existing.setRemarks(remarksField.getText());
                saveCertificate(existing, existing, dialog);
            });

            Button cancel = new Button("Cancel");
            cancel.setStyle(
                    "-fx-background-color: #6b7280; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
            cancel.setOnMouseEntered(e -> cancel.setStyle(
                    "-fx-background-color: #4b5563; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
            cancel.setOnMouseExited(e -> cancel.setStyle(
                    "-fx-background-color: #6b7280; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
            cancel.setOnAction(e -> dialog.close());

            HBox buttons = new HBox(10, cancel, save);
            buttons.setAlignment(Pos.CENTER_RIGHT);
            buttons.setPadding(new Insets(8, 0, 0, 0));

            Label certNoLabel = FormStyler.createFormLabel("Certificate No");
            root.getChildren().addAll(
                    titleLabel,
                    certNoLabel,
                    certNoField,
                    FormStyler.createCompactFormField("Name *", nameField),
                    FormStyler.createCompactFormField("Parent Name", parentField),
                    FormStyler.createCompactFormField("Address", addressField),
                    FormStyler.createCompactFormField("Thalook", thalookField),
                    FormStyler.createCompactFormField("Date", datePicker),
                    FormStyler.createCompactFormField("Remarks", remarksField),
                    buttons);
        } else { // Custom
            TextField certNoField = new TextField(existing.getCertificateNo());
            certNoField.setEditable(false);
            FormStyler.applyCompactFieldStyle(certNoField);

            TextField templateNameField = new TextField(existing.getTemplateName());
            templateNameField.setPromptText("Template Name *");

            TextArea templateContentField = new TextArea(existing.getTemplateContent());
            templateContentField.setPromptText("Template Content");
            templateContentField.setPrefRowCount(6);

            DatePicker issueDatePicker = new DatePicker(existing.getIssueDate());
            issueDatePicker.setPromptText("Issued Date");

            Button save = new Button("Update");
            save.setStyle(
                    "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
            save.setOnMouseEntered(e -> save.setStyle(
                    "-fx-background-color: #1d4ed8; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
            save.setOnMouseExited(e -> save.setStyle(
                    "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
            save.setOnAction(e -> {
                existing.setTemplateName(templateNameField.getText());
                existing.setTemplateContent(templateContentField.getText());

                existing.setIssueDate(issueDatePicker.getValue());
                saveCertificate(existing, existing, dialog);
            });

            Button cancel = new Button("Cancel");
            cancel.setStyle(
                    "-fx-background-color: #6b7280; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
            cancel.setOnMouseEntered(e -> cancel.setStyle(
                    "-fx-background-color: #4b5563; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
            cancel.setOnMouseExited(e -> cancel.setStyle(
                    "-fx-background-color: #6b7280; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
            cancel.setOnAction(e -> dialog.close());

            HBox buttons = new HBox(10, cancel, save);
            buttons.setAlignment(Pos.CENTER_RIGHT);
            buttons.setPadding(new Insets(8, 0, 0, 0));

            Label certNoLabel = FormStyler.createFormLabel("Certificate No");
            root.getChildren().addAll(
                    titleLabel,
                    certNoLabel,
                    certNoField,
                    FormStyler.createCompactFormField("Template Name *", templateNameField),
                    FormStyler.createCompactFormField("Template Content", templateContentField),

                    FormStyler.createCompactFormField("Issued Date", issueDatePicker),
                    buttons);
        }

        scrollPane.setContent(root);
        int height = "Custom".equals(type) ? 450 : 600;
        Scene scene = new Scene(scrollPane, 500, height);
        dialog.setScene(scene);
        dialog.show();
    }

    private void revokeCertificate(Certificate c, String type) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to revoke this certificate?",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                new Thread(() -> {
                    boolean ok = certificateDAO.delete(c.getId(), type);
                    javafx.application.Platform.runLater(() -> {
                        if (ok) {
                            new Alert(Alert.AlertType.INFORMATION, "Certificate revoked successfully!", ButtonType.OK)
                                    .showAndWait();
                            if ("Marriage".equals(type))
                                loadMarriageList();
                            else if ("Death".equals(type))
                                loadDeathList();
                            else if ("Jamath".equals(type))
                                loadJamathList();
                            else if ("Custom".equals(type))
                                loadCustomList();
                        } else {
                            new Alert(Alert.AlertType.ERROR, "Failed to revoke certificate", ButtonType.OK)
                                    .showAndWait();
                        }
                    });
                }).start();
            }
        });
    }

    private void saveCertificate(Certificate c, Certificate existing, Stage dialog, Object... formFields) {
        new Thread(() -> {
            try {
                // Generate PDF for marriage certificates
                if ("Marriage".equals(c.getType())) {
                    String pdfPath = CertificatePDFService.saveMarriageCertificateHTML(c);
                    c.setPdfPath(pdfPath); // Store PDF path
                }
                // Generate PDF for death certificates
                else if ("Death".equals(c.getType())) {
                    String pdfPath = CertificatePDFService.saveDeathCertificateHTML(c);
                    c.setPdfPath(pdfPath); // Store PDF path
                }
                // Generate PDF for Jamath certificates
                else if ("Jamath".equals(c.getType())) {
                    String pdfPath = CertificatePDFService.saveJamathCertificateHTML(c);
                    c.setPdfPath(pdfPath); // Store PDF path
                }
                // Generate PDF for Custom certificates
                else if ("Custom".equals(c.getType()) && c.getTemplateContent() != null
                        && !c.getTemplateContent().isEmpty()) {
                    String pdfPath = CertificatePDFService.saveCustomCertificateHTML(c);
                    c.setPdfPath(pdfPath); // Store PDF path
                }

                boolean ok;
                if (existing == null) {
                    Long newId = certificateDAO.create(c);
                    ok = newId != null;
                    if (ok && newId != null) {
                        c.setId(newId); // Set the ID for the newly created certificate
                    }
                } else {
                    ok = certificateDAO.update(c);
                }
                javafx.application.Platform.runLater(() -> {
                    if (ok) {
                        if (dialog != null)
                            dialog.close();

                        // Refresh the appropriate list based on certificate type
                        if ("Marriage".equals(c.getType())) {
                            loadMarriageList();
                            // Clear form fields if provided (for new certificates from form)
                            if (existing == null && formFields.length >= 11) {
                                try {
                                    javafx.scene.control.TextField certNoField = (javafx.scene.control.TextField) formFields[0];
                                    javafx.scene.control.TextField groomField = (javafx.scene.control.TextField) formFields[1];
                                    javafx.scene.control.TextField brideField = (javafx.scene.control.TextField) formFields[2];
                                    javafx.scene.control.TextField groomParentField = (javafx.scene.control.TextField) formFields[3];
                                    javafx.scene.control.TextField brideParentField = (javafx.scene.control.TextField) formFields[4];
                                    javafx.scene.control.TextArea groomAddressField = (javafx.scene.control.TextArea) formFields[5];
                                    javafx.scene.control.TextArea brideAddressField = (javafx.scene.control.TextArea) formFields[6];
                                    javafx.scene.control.TextField placeField = (javafx.scene.control.TextField) formFields[7];
                                    javafx.scene.control.ComboBox<String> statusCombo = (javafx.scene.control.ComboBox<String>) formFields[8];
                                    javafx.scene.control.DatePicker marriageDatePicker = (javafx.scene.control.DatePicker) formFields[9];
                                    javafx.scene.control.TextArea notesField = (javafx.scene.control.TextArea) formFields[10];

                                    certNoField.clear();
                                    groomField.clear();
                                    brideField.clear();
                                    groomParentField.clear();
                                    brideParentField.clear();
                                    groomAddressField.clear();
                                    brideAddressField.clear();
                                    placeField.clear();
                                    statusCombo.getSelectionModel().clearSelection();
                                    marriageDatePicker.setValue(null);
                                    notesField.clear();
                                } catch (Exception e) {
                                    System.err.println("Error clearing form fields: " + e.getMessage());
                                }
                            }
                        } else if ("Death".equals(c.getType())) {
                            loadDeathList();
                        } else if ("Jamath".equals(c.getType())) {
                            loadJamathList();
                        } else if ("Custom".equals(c.getType())) {
                            loadCustomList();
                        }

                        if (existing == null) {
                            new Alert(Alert.AlertType.INFORMATION, "Certificate created successfully!", ButtonType.OK)
                                    .showAndWait();
                        } else {
                            new Alert(Alert.AlertType.INFORMATION, "Certificate updated successfully!", ButtonType.OK)
                                    .showAndWait();
                        }
                    } else {
                        new Alert(Alert.AlertType.ERROR, "Save failed", ButtonType.OK).showAndWait();
                    }
                });
            } catch (Exception e) {
                System.err.println("Error saving certificate: " + e.getMessage());
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    new Alert(Alert.AlertType.ERROR, "Error generating certificate: " + e.getMessage(), ButtonType.OK)
                            .showAndWait();
                });
            }
        }).start();
    }
}
