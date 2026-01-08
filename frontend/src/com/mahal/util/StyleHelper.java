package com.mahal.util;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;

/**
 * Utility class for consistent styling across the application
 * Provides centralized styling methods for UI components
 */
public class StyleHelper {

    // Color palette constants
    public static final String PRIMARY_50 = "#f0fdf4";
    public static final String PRIMARY_100 = "#dcfce7";
    public static final String PRIMARY_300 = "#86efac";
    public static final String PRIMARY_600 = "#16a34a";
    public static final String PRIMARY_700 = "#15803d";
    public static final String PRIMARY_800 = "#166534";
    public static final String EMERALD_500 = "#10b981";
    public static final String EMERALD_600 = "#059669";
    public static final String SLATE_100 = "#f1f5f9";
    public static final String SLATE_500 = "#64748b";

    // Background colors
    public static final String BG_GRAY_50 = "#f9fafb";
    public static final String BG_GRAY_100 = "#f5f5f5";
    public static final String BG_WHITE = "#ffffff";

    // Text colors
    public static final String TEXT_GRAY_500 = "#6b7280";
    public static final String TEXT_GRAY_700 = "#374151";
    public static final String TEXT_GRAY_900 = "#111827";

    // Border colors
    public static final String BORDER_GRAY = "#d1d5db";

    // Font stacks
    public static final String FONT_PRIMARY = "'Inter', 'Segoe UI', system-ui, sans-serif";
    public static final String FONT_SECONDARY = "'Roboto', 'Helvetica Neue', Arial, sans-serif";
    public static final String FONT_ACCENT = "'Outfit', 'Inter', sans-serif";

    // Private constructor to prevent instantiation
    private StyleHelper() {
        // Utility class - no instance needed
    }

    // Button styles
    public static String getPrimaryButtonStyle() {
        return "";
    }

    public static String getPrimaryButtonHoverStyle() {
        return "";
    }

    public static String getSecondaryButtonStyle() {
        return "-fx-background-color: #f1f5f9; " +
                "-fx-text-fill: #475569; " +
                "-fx-background-radius: 8; " +
                "-fx-font-weight: 600; " +
                "-fx-font-size: 13px; " +
                "-fx-padding: 6 12; " +
                "-fx-cursor: hand;";
    }

    public static String getDangerButtonStyle() {
        return "-fx-background-color: #ef4444; " +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 8; " +
                "-fx-font-weight: 600; " +
                "-fx-font-size: 13px; " +
                "-fx-padding: 6 12; " +
                "-fx-cursor: hand;";
    }

    public static String getDangerButtonHoverStyle() {
        return "-fx-background-color: #dc2626; " +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 8; " +
                "-fx-font-weight: 600; " +
                "-fx-font-size: 13px; " +
                "-fx-padding: 6 12; " +
                "-fx-cursor: hand;";
    }

    public static String getCardStyle() {
        return "-fx-background-color: " + BG_WHITE + "; " +
                "-fx-background-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2); " +
                "-fx-padding: 20;";
    }

    public static String getInputFieldStyle() {
        return "-fx-background-color: white; " +
                "-fx-background-radius: 8; " +
                "-fx-border-radius: 8; " +
                "-fx-border-color: " + BORDER_GRAY + "; " +
                "-fx-border-width: 1; " +
                "-fx-padding: 6 10; " +
                "-fx-font-size: 12px;";
    }

    public static String getInputFieldFocusStyle() {
        return "-fx-background-color: white; " +
                "-fx-background-radius: 8; " +
                "-fx-border-radius: 8; " +
                "-fx-border-color: " + EMERALD_500 + "; " +
                "-fx-border-width: 1.5; " +
                "-fx-padding: 6 10; " +
                "-fx-font-size: 12px;";
    }

    public static String getTableHeaderStyle() {
        return "-fx-background-color: #f8fafc; " +
                "-fx-text-fill: #64748b; " +
                "-fx-font-family: " + FONT_ACCENT + "; " +
                "-fx-font-weight: 800; " +
                "-fx-font-size: 11px; " +
                "-fx-padding: 12 15; " +
                "-fx-alignment: CENTER;";
    }

    public static String getTableStyle() {
        return "";
    }

    public static String getTableRowStyle() {
        return "";
    }

    public static String getTableRowHoverStyle() {
        return "";
    }

    public static String getTabPaneBaseStyle() {
        return "-fx-background-color: #ffffff; " +
                "-fx-control-inner-background: #ffffff; " +
                "-fx-tab-header-area-background: #ffffff; " +
                "-fx-tab-header-background: #ffffff; " +
                "-fx-base: #ffffff; " +
                "-fx-body-color: #ffffff; " +
                "-fx-color: #ffffff; " +
                "-fx-border-color: transparent; " +
                "-fx-background-insets: 0; " +
                "-fx-background: #ffffff; " +
                "-fx-tab-min-height: 30px; " +
                "-fx-tab-max-height: 30px; " +
                "-fx-tab-background: #ffffff; " +
                "-fx-padding: 0;";
    }

    public static String getFormLabelStyle() {
        return "-fx-font-size: 14px; " +
                "-fx-font-weight: 500; " +
                "-fx-text-fill: " + TEXT_GRAY_700 + ";";
    }

    public static String getFilterLabelStyle() {
        return "-fx-text-fill: #9ca3af; -fx-font-size: 10px; -fx-font-weight: 800; -fx-letter-spacing: 0.5px;";
    }

    public static String getFormSectionStyle() {
        return "-fx-background-color: " + BG_WHITE + "; " +
                "-fx-background-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2); " +
                "-fx-padding: 24;";
    }

    public static String getTitleStyle() {
        return "-fx-font-family: " + FONT_ACCENT + "; -fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: "
                + TEXT_GRAY_900 + ";";
    }

    public static String getSubtitleStyle() {
        return "-fx-font-family: " + FONT_PRIMARY + "; -fx-font-size: 14px; -fx-text-fill: " + TEXT_GRAY_500
                + ";";
    }

    public static String getEditButtonStyle() {
        return "-fx-background-color: #2563eb; " +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 8; " +
                "-fx-font-weight: 600; " +
                "-fx-font-size: 13px; " +
                "-fx-padding: 6 12; " +
                "-fx-cursor: hand;";
    }

    public static String getEditButtonHoverStyle() {
        return "-fx-background-color: #1d4ed8; " +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 8; " +
                "-fx-font-weight: 600; " +
                "-fx-font-size: 13px; " +
                "-fx-padding: 6 12; " +
                "-fx-cursor: hand;";
    }

    // Utility methods for styling components
    public static void styleTable(TableView<?> table) {
        table.setStyle(getTableStyle());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    public static void styleTableColumn(TableColumn<?, ?> column) {
        column.setStyle(getTableHeaderStyle());
    }

    public static void styleInputField(TextField field) {
        field.setStyle(getInputFieldStyle());
        field.setPrefHeight(34);
        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            field.setStyle(newVal ? getInputFieldFocusStyle() : getInputFieldStyle());
        });
    }

    public static void styleTextField(TextField field) {
        styleInputField(field);
    }

    public static void styleTextArea(TextArea area) {
        area.setStyle(getInputFieldStyle());
        area.focusedProperty().addListener((obs, oldVal, newVal) -> {
            area.setStyle(newVal ? getInputFieldFocusStyle() : getInputFieldStyle());
        });
    }

    public static void styleComboBox(ComboBox<?> combo) {
        combo.setStyle(getInputFieldStyle());
        combo.setPrefHeight(34);
    }

    public static void styleDatePicker(DatePicker picker) {
        picker.setStyle(getInputFieldStyle());
        picker.setPrefHeight(34);
    }

    // Badge styles
    public static String getBadgeStyle(boolean active) {
        if (active) {
            return "-fx-background-color: #dcfce7; -fx-text-fill: #16a34a; -fx-background-radius: 12; -fx-padding: 4 12; -fx-font-size: 11px; -fx-font-weight: 700;";
        } else {
            return "-fx-background-color: #fee2e2; -fx-text-fill: #ef4444; -fx-background-radius: 12; -fx-padding: 4 12; -fx-font-size: 11px; -fx-font-weight: 700;";
        }
    }

    // Pill Switcher styles
    public static String getPillSwitcherContainerStyle() {
        return "-fx-background-color: #f1f5f9; " +
                "-fx-background-radius: 25; " +
                "-fx-padding: 4;";
    }

    public static String getPillButtonStyle(boolean active) {
        if (active) {
            return "-fx-background-color: " + EMERALD_500 + "; " +
                    "-fx-text-fill: #ffffff; " +
                    "-fx-background-radius: 20; " +
                    "-fx-font-weight: 700; " +
                    "-fx-font-size: 13px; " +
                    "-fx-padding: 8 20; " +
                    "-fx-cursor: hand;";
        } else {
            return "-fx-background-color: transparent; " +
                    "-fx-text-fill: #64748b; " +
                    "-fx-background-radius: 20; " +
                    "-fx-font-weight: 600; " +
                    "-fx-font-size: 13px; " +
                    "-fx-padding: 8 20; " +
                    "-fx-cursor: hand;";
        }
    }

    public static String getActionPillButtonStyle() {
        return "-fx-background-color: #2563eb; " +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 20; " +
                "-fx-font-weight: 700; " +
                "-fx-font-size: 13px; " +
                "-fx-padding: 10 24; " +
                "-fx-cursor: hand;";
    }

    public static String getApplyFilterButtonStyle() {
        return "-fx-background-color: " + EMERALD_500 + "; " +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 8; " +
                "-fx-font-weight: 700; " +
                "-fx-padding: 10 20; " +
                "-fx-cursor: hand;";
    }

    public static String getResetButtonStyle() {
        return "-fx-background-color: #f1f5f9; " +
                "-fx-text-fill: #475569; " +
                "-fx-background-radius: 8; " +
                "-fx-font-weight: 600; " +
                "-fx-padding: 10 20; " +
                "-fx-cursor: hand;";
    }

    public static void styleSecondaryButton(Button button) {
        button.setStyle(getSecondaryButtonStyle());
        button.setOnMouseEntered(e -> button.setStyle(getResetButtonStyle())); // Using similar look for hover
        button.setOnMouseExited(e -> button.setStyle(getSecondaryButtonStyle()));
    }
}
