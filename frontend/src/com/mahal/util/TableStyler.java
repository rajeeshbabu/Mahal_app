package com.mahal.util;

import javafx.geometry.Pos;
import javafx.scene.control.Control;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

/**
 * Utility class for styling tables with modern UI
 */
public class TableStyler {

    public static <T> void applyModernStyling(TableView<T> table) {
        table.setStyle("-fx-border-width: 0; " +
                "-fx-border-color: transparent; " +
                "-fx-table-cell-border-color: #f1f5f9; " +
                "-fx-table-header-border-color: transparent; " +
                "-fx-background-insets: 0; " +
                "-fx-padding: 0; " +
                "-fx-focus-color: transparent; " +
                "-fx-faint-focus-color: transparent;");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        // Default row styling (no colors, no extra padding/spacing)
        table.setRowFactory(null);
    }

    public static <T, S> void styleTableColumn(TableColumn<T, S> column) {
        column.setStyle(StyleHelper.getTableHeaderStyle());

        javafx.util.Callback<TableColumn<T, S>, TableCell<T, S>> existingFactory = column.getCellFactory();

        column.setCellFactory(tc -> {
            TableCell<T, S> cell;
            if (existingFactory != null) {
                cell = existingFactory.call(tc);
                // Keep existing alignment if already set by factory
            } else {
                cell = new TableCell<T, S>() {
                    @Override
                    protected void updateItem(S item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            setText(item.toString());
                            setWrapText(true);
                        }
                    }
                };
            }
            cell.setAlignment(Pos.CENTER);
            cell.setStyle(
                    "-fx-padding: 12 15; -fx-text-fill: #1e293b; -fx-font-size: 12px; -fx-font-family: "
                            + StyleHelper.FONT_SECONDARY + ";");
            return cell;
        });
    }
}
