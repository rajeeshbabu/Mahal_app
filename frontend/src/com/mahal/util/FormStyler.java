package com.mahal.util;

import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

/**
 * Utility class for styling forms and dialogs
 */
public class FormStyler {

    // Compact field styles (aligned with Masjid form) - MUST include
    // background-color for borders to show
    private static final String COMPACT_FIELD_STYLE = "-fx-background-color: white; " +
            "-fx-background-radius: 6; " +
            "-fx-border-radius: 6; " +
            "-fx-border-color: " + StyleHelper.BORDER_GRAY + "; " +
            "-fx-border-width: 1; " +
            "-fx-padding: 6 10; " +
            "-fx-font-size: 12px;";
    private static final String COMPACT_FOCUS_STYLE = "-fx-background-color: white; " +
            "-fx-background-radius: 6; " +
            "-fx-border-radius: 6; " +
            "-fx-border-color: " + StyleHelper.PRIMARY_600 + "; " +
            "-fx-border-width: 2; " +
            "-fx-padding: 6 10; " +
            "-fx-font-size: 12px;";

    public static void styleFormDialog(VBox root) {
        root.setStyle(StyleHelper.getFormSectionStyle());
        root.setSpacing(15);
    }

    public static Label createFormLabel(String text) {
        Label label = new Label(text);
        label.setStyle(StyleHelper.getFormLabelStyle());
        return label;
    }

    public static void styleFormField(TextField field) {
        StyleHelper.styleInputField(field);
    }

    public static void styleFormField(TextArea area) {
        StyleHelper.styleTextArea(area);
    }

    public static void styleFormField(ComboBox<?> combo) {
        StyleHelper.styleComboBox(combo);
    }

    public static void styleFormField(DatePicker picker) {
        StyleHelper.styleDatePicker(picker);
    }

    public static HBox createFormButtonBox(Button primaryButton, Button cancelButton) {
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        if (cancelButton != null) {
            cancelButton.setStyle(StyleHelper.getSecondaryButtonStyle());
        }

        primaryButton.setStyle(StyleHelper.getPrimaryButtonStyle());
        primaryButton.setOnMouseEntered(e -> primaryButton.setStyle(StyleHelper.getPrimaryButtonHoverStyle()));
        primaryButton.setOnMouseExited(e -> primaryButton.setStyle(StyleHelper.getPrimaryButtonStyle()));

        if (cancelButton != null) {
            buttonBox.getChildren().addAll(cancelButton, primaryButton);
        } else {
            buttonBox.getChildren().add(primaryButton);
        }

        return buttonBox;
    }

    public static VBox createFormField(String labelText, Control field) {
        VBox fieldBox = new VBox(5);
        Label label = createFormLabel(labelText);
        fieldBox.getChildren().addAll(label, field);
        return fieldBox;
    }

    public static VBox createCompactFormField(String labelText, Control field) {
        VBox fieldBox = new VBox(3); // Reduced spacing
        Label label = new Label(labelText);
        label.setStyle("-fx-font-size: 12px; -fx-font-weight: 500; -fx-text-fill: " + StyleHelper.TEXT_GRAY_700 + ";");
        fieldBox.getChildren().addAll(label, field);
        applyCompactFieldStyle(field);
        return fieldBox;
    }

    /**
     * Applies compact field styling (font-size 12px, smaller padding/height) and
     * focus handling.
     */
    public static void applyCompactFieldStyle(Control field) {
        if (field == null)
            return;
        field.setStyle(COMPACT_FIELD_STYLE);
        if (field instanceof TextField) {
            ((TextField) field).setPrefHeight(32);
        } else if (field instanceof ComboBox<?>) {
            ((ComboBox<?>) field).setPrefHeight(32);
        } else if (field instanceof DatePicker) {
            ((DatePicker) field).setPrefHeight(32);
        } else if (field instanceof TextArea) {
            ((TextArea) field).setPrefRowCount(Math.max(((TextArea) field).getPrefRowCount(), 3));
        }

        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            field.setStyle(newVal ? COMPACT_FOCUS_STYLE : COMPACT_FIELD_STYLE);
        });
    }
}
