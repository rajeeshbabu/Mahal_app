package com.mahal.controller.inventory;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.beans.property.SimpleStringProperty;
import com.mahal.database.InventoryItemDAO;
import com.mahal.database.DamagedItemDAO;
import com.mahal.database.RentItemDAO;
import com.mahal.database.RentDAO;
import com.mahal.model.InventoryItem;
import com.mahal.model.DamagedItem;
import com.mahal.model.RentItem;
import com.mahal.model.Rent;
import com.mahal.util.TableStyler;
import com.mahal.util.FormStyler;
import com.mahal.util.StyleHelper;
import java.math.BigDecimal;
import java.time.LocalDate;

public class InventoryController {
    private VBox view;
    private StackPane contentPane;
    private VBox addItemsViewPane;
    private VBox damagedViewPane;
    private VBox rentItemsViewPane;
    private VBox rentViewPane;
    private VBox rentListViewPane;
    private InventoryItemDAO itemDAO;
    private DamagedItemDAO damagedDAO;
    private RentItemDAO rentItemDAO;
    private RentDAO rentDAO;

    private ObservableList<InventoryItem> itemList = FXCollections.observableArrayList();
    private ObservableList<DamagedItem> damagedList = FXCollections.observableArrayList();
    private ObservableList<RentItem> rentItemList = FXCollections.observableArrayList();
    private ObservableList<Rent> rentList = FXCollections.observableArrayList();

    private FilteredList<InventoryItem> filteredItems;
    private FilteredList<DamagedItem> filteredDamaged;
    private FilteredList<RentItem> filteredRentItems;
    private FilteredList<Rent> filteredRents;

    private Label itemCountLabel;
    private Label damagedCountLabel;
    private Label rentItemCountLabel;
    private Label rentCountLabel;

    public InventoryController() {
        this.itemDAO = new InventoryItemDAO();
        this.damagedDAO = new DamagedItemDAO();
        this.rentItemDAO = new RentItemDAO();
        this.rentDAO = new RentDAO();

        filteredItems = new FilteredList<>(itemList, p -> true);
        filteredDamaged = new FilteredList<>(damagedList, p -> true);
        filteredRentItems = new FilteredList<>(rentItemList, p -> true);
        filteredRents = new FilteredList<>(rentList, p -> true);

        createView();

        // Subscribe to sync events
        com.mahal.util.EventBus.getInstance().subscribe("inventory_items",
                e -> javafx.application.Platform.runLater(this::loadItems));
        com.mahal.util.EventBus.getInstance().subscribe("damaged_items",
                e -> javafx.application.Platform.runLater(() -> {
                    loadDamaged();
                    loadItems();
                }));
        com.mahal.util.EventBus.getInstance().subscribe("rent_items",
                e -> javafx.application.Platform.runLater(this::loadRentItems));
        com.mahal.util.EventBus.getInstance().subscribe("rents",
                e -> javafx.application.Platform.runLater(this::loadRents));
    }

    public VBox getView() {
        return view;
    }

    private void styleSwitcherButton(Button btn, boolean active) {
        btn.setStyle(StyleHelper.getPillButtonStyle(active));
    }

    private void styleDatePickerPopup(DatePicker datePicker) {
        datePicker.setOnShowing(e -> {
            javafx.application.Platform.runLater(() -> {
                try {
                    // Wait a bit for popup to fully render
                    javafx.util.Duration delay = javafx.util.Duration.millis(50);
                    javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(delay);
                    pause.setOnFinished(ev -> {
                        try {
                            // Find all popup windows
                            for (javafx.stage.Window window : javafx.stage.Window.getWindows()) {
                                if (window instanceof javafx.stage.PopupWindow && window.isShowing()) {
                                    javafx.scene.Scene popupScene = window.getScene();
                                    if (popupScene != null && popupScene.getRoot() != null) {
                                        javafx.scene.Node popupRoot = popupScene.getRoot();
                                        // Style month/year labels - try multiple selectors
                                        popupRoot.lookupAll(".month-year-pane .label").forEach(node -> {
                                            if (node instanceof Label) {
                                                ((Label) node).setStyle("-fx-text-fill: #000000;");
                                            }
                                        });
                                        popupRoot.lookupAll(".spinner-label").forEach(node -> {
                                            if (node instanceof Label) {
                                                ((Label) node).setStyle("-fx-text-fill: #000000;");
                                            }
                                        });
                                        // Also try direct lookup of month-year-pane
                                        javafx.scene.Node monthYearPane = popupRoot.lookup(".month-year-pane");
                                        if (monthYearPane != null) {
                                            monthYearPane.lookupAll(".label").forEach(node -> {
                                                if (node instanceof Label) {
                                                    ((Label) node).setStyle("-fx-text-fill: #000000;");
                                                }
                                            });
                                        }
                                        // Try to find all labels in the popup and style month/year ones
                                        popupRoot.lookupAll(".label").forEach(node -> {
                                            if (node instanceof Label) {
                                                Label label = (Label) node;
                                                String text = label.getText();
                                                // Check if this looks like a month or year label
                                                if (text != null && (text.matches("\\d{4}") ||
                                                        text.matches(
                                                                "January|February|March|April|May|June|July|August|September|October|November|December"))) {
                                                    label.setStyle("-fx-text-fill: #000000;");
                                                }
                                            }
                                        });
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            // Ignore if popup structure is different
                        }
                    });
                    pause.play();
                } catch (Exception ex) {
                    // Ignore if popup structure is different
                }
            });
        });
    }

    private void createView() {
        view = new VBox(20);
        view.setPadding(new Insets(20));
        view.setStyle(StyleHelper.getCardStyle());

        Label titleLabel = new Label("Inventory Management");
        titleLabel.setStyle(StyleHelper.getTitleStyle());

        // Emerald Pill Switcher
        HBox switcher = new HBox(8);
        switcher.setAlignment(Pos.CENTER_LEFT);
        switcher.setStyle(StyleHelper.getPillSwitcherContainerStyle());

        Button addItemBtn = new Button("Add Items");
        Button damagedBtn = new Button("Damaged List");
        Button rentItemsBtn = new Button("Rent Items");
        Button rentBtn = new Button("Rent");
        Button rentListBtn = new Button("Rent List");

        styleSwitcherButton(addItemBtn, true);
        styleSwitcherButton(damagedBtn, false);
        styleSwitcherButton(rentItemsBtn, false);
        styleSwitcherButton(rentBtn, false);
        styleSwitcherButton(rentListBtn, false);

        switcher.getChildren().addAll(addItemBtn, damagedBtn, rentItemsBtn, rentBtn, rentListBtn);

        contentPane = new StackPane();
        contentPane.setPadding(new Insets(10, 0, 0, 0));
        addItemsViewPane = createAddItemsView();
        damagedViewPane = createDamagedView();
        rentItemsViewPane = createRentItemsView();
        rentViewPane = createRentView();
        rentListViewPane = createRentListView();
        contentPane.getChildren().setAll(addItemsViewPane);

        addItemBtn.setOnAction(e -> {
            styleSwitcherButton(addItemBtn, true);
            styleSwitcherButton(damagedBtn, false);
            styleSwitcherButton(rentItemsBtn, false);
            styleSwitcherButton(rentBtn, false);
            styleSwitcherButton(rentListBtn, false);
            contentPane.getChildren().setAll(addItemsViewPane);
        });

        damagedBtn.setOnAction(e -> {
            styleSwitcherButton(addItemBtn, false);
            styleSwitcherButton(damagedBtn, true);
            styleSwitcherButton(rentItemsBtn, false);
            styleSwitcherButton(rentBtn, false);
            styleSwitcherButton(rentListBtn, false);
            contentPane.getChildren().setAll(damagedViewPane);
        });

        rentItemsBtn.setOnAction(e -> {
            styleSwitcherButton(addItemBtn, false);
            styleSwitcherButton(damagedBtn, false);
            styleSwitcherButton(rentItemsBtn, true);
            styleSwitcherButton(rentBtn, false);
            styleSwitcherButton(rentListBtn, false);
            contentPane.getChildren().setAll(rentItemsViewPane);
        });

        rentBtn.setOnAction(e -> {
            styleSwitcherButton(addItemBtn, false);
            styleSwitcherButton(damagedBtn, false);
            styleSwitcherButton(rentItemsBtn, false);
            styleSwitcherButton(rentBtn, true);
            styleSwitcherButton(rentListBtn, false);
            contentPane.getChildren().setAll(rentViewPane);
        });

        rentListBtn.setOnAction(e -> {
            styleSwitcherButton(addItemBtn, false);
            styleSwitcherButton(damagedBtn, false);
            styleSwitcherButton(rentItemsBtn, false);
            styleSwitcherButton(rentBtn, false);
            styleSwitcherButton(rentListBtn, true);
            contentPane.getChildren().setAll(rentListViewPane);
        });

        view.getChildren().addAll(titleLabel, switcher, contentPane);
    }

    private void updateCountLabel(Label label, FilteredList<?> list, String itemType) {
        if (label == null)
            return;
        int count = list.size();
        label.setText("Showing " + count + " " + itemType + (count == 1 ? "" : "s"));
    }

    // ========== ADD ITEMS VIEW ==========
    private VBox createAddItemsView() {
        VBox container = new VBox(20);

        // Action Row: Add Button, Search, and Record Count
        HBox actionRow = new HBox(12);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        Button addBtn = new Button("Add Item");
        addBtn.setStyle(StyleHelper.getActionPillButtonStyle());
        addBtn.setOnAction(e -> showAddItemDialog());

        TextField searchField = new TextField();
        searchField.setPromptText("Search items...");
        StyleHelper.styleTextField(searchField);
        searchField.setPrefWidth(250);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredItems.setPredicate(item -> {
                if (newValue == null || newValue.isEmpty())
                    return true;
                String lowerCaseFilter = newValue.toLowerCase();
                if (item.getItemName().toLowerCase().contains(lowerCaseFilter))
                    return true;
                if (item.getSkuCode() != null && item.getSkuCode().toLowerCase().contains(lowerCaseFilter))
                    return true;
                if (item.getLocation() != null && item.getLocation().toLowerCase().contains(lowerCaseFilter))
                    return true;
                return false;
            });
            updateCountLabel(itemCountLabel, filteredItems, "item");
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        itemCountLabel = new Label("Showing 0 items");
        itemCountLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px; -fx-font-weight: 500;");

        actionRow.getChildren().addAll(addBtn, searchField, spacer, itemCountLabel);

        // Table
        TableView<InventoryItem> table = new TableView<>();
        TableStyler.applyModernStyling(table);

        TableColumn<InventoryItem, String> nameCol = new TableColumn<>("NAME");
        nameCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getItemName()));
        TableStyler.styleTableColumn(nameCol);

        TableColumn<InventoryItem, String> skuCol = new TableColumn<>("SKU");
        skuCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSkuCode()));
        TableStyler.styleTableColumn(skuCol);

        TableColumn<InventoryItem, String> qtyCol = new TableColumn<>("QTY");
        qtyCol.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().getQuantity())));
        TableStyler.styleTableColumn(qtyCol);

        TableColumn<InventoryItem, String> locCol = new TableColumn<>("LOCATION");
        locCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getLocation()));
        TableStyler.styleTableColumn(locCol);

        TableColumn<InventoryItem, String> actionsCol = new TableColumn<>("ACTIONS");
        actionsCol.setMinWidth(120);
        actionsCol.setCellFactory(param -> new TableCell<InventoryItem, String>() {
            private final Hyperlink editLink = new Hyperlink("Edit");
            private final Hyperlink deleteLink = new Hyperlink("Delete");
            private final HBox container = new HBox(12, editLink, deleteLink);
            {
                container.setAlignment(Pos.CENTER);
                editLink.setStyle(
                        "-fx-text-fill: #2563eb; -fx-font-weight: 600; -fx-font-size: 13px; -fx-underline: false;");
                deleteLink.setStyle(
                        "-fx-text-fill: #ef4444; -fx-font-weight: 600; -fx-font-size: 13px; -fx-underline: false;");
                editLink.setOnAction(e -> showEditItemDialog(getTableView().getItems().get(getIndex())));
                deleteLink.setOnAction(e -> deleteItem(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
        TableStyler.styleTableColumn(actionsCol);

        table.getColumns().addAll(nameCol, skuCol, qtyCol, locCol, actionsCol);

        SortedList<InventoryItem> sortedData = new SortedList<>(filteredItems);
        sortedData.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedData);
        VBox.setVgrow(table, Priority.ALWAYS);

        container.getChildren().addAll(actionRow, table);
        loadItems();
        return container;
    }

    // Add Item Dialog
    private void showAddItemDialog() {
        showEditItemDialog(null);
    }

    // Add Damaged Item Dialog
    private void showAddDamagedItemDialog() {
        showEditDamagedItemDialog(null);
    }

    // Add Rent Item Dialog
    private void showAddRentItemDialog() {
        showEditRentItemDialog(null);
    }

    // ========== DAMAGED VIEW ==========
    private VBox createDamagedView() {
        VBox container = new VBox(20);

        // Action Row
        HBox actionRow = new HBox(12);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        Button addBtn = new Button("Mark Damaged");
        addBtn.setStyle(StyleHelper.getActionPillButtonStyle());
        addBtn.setOnAction(e -> showAddDamagedItemDialog());

        TextField searchField = new TextField();
        searchField.setPromptText("Search damaged items...");
        StyleHelper.styleTextField(searchField);
        searchField.setPrefWidth(250);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredDamaged.setPredicate(damaged -> {
                if (newValue == null || newValue.isEmpty())
                    return true;
                String lowerCaseFilter = newValue.toLowerCase();
                if (damaged.getInventoryItemName() != null
                        && damaged.getInventoryItemName().toLowerCase().contains(lowerCaseFilter))
                    return true;
                if (damaged.getReason() != null && damaged.getReason().toLowerCase().contains(lowerCaseFilter))
                    return true;
                return false;
            });
            updateCountLabel(damagedCountLabel, filteredDamaged, "damaged item");
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        damagedCountLabel = new Label("Showing 0 damaged items");
        damagedCountLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px; -fx-font-weight: 500;");

        actionRow.getChildren().addAll(addBtn, searchField, spacer, damagedCountLabel);

        // Table
        TableView<DamagedItem> table = new TableView<>();
        TableStyler.applyModernStyling(table);

        TableColumn<DamagedItem, String> nameCol = new TableColumn<>("ITEM NAME");
        nameCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getInventoryItemName()));
        TableStyler.styleTableColumn(nameCol);

        TableColumn<DamagedItem, String> qtyCol = new TableColumn<>("QTY");
        qtyCol.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().getQuantity())));
        TableStyler.styleTableColumn(qtyCol);

        TableColumn<DamagedItem, String> dateCol = new TableColumn<>("DATE");
        dateCol.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getDamageDate() != null ? cell.getValue().getDamageDate().toString() : ""));
        TableStyler.styleTableColumn(dateCol);

        TableColumn<DamagedItem, String> reasonCol = new TableColumn<>("REASON");
        reasonCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getReason()));
        TableStyler.styleTableColumn(reasonCol);

        TableColumn<DamagedItem, String> actionsCol = new TableColumn<>("ACTIONS");
        actionsCol.setMinWidth(120);
        actionsCol.setCellFactory(param -> new TableCell<DamagedItem, String>() {
            private final Hyperlink editLink = new Hyperlink("Edit");
            private final Hyperlink deleteLink = new Hyperlink("Delete");
            private final HBox container = new HBox(12, editLink, deleteLink);
            {
                container.setAlignment(Pos.CENTER);
                editLink.setStyle(
                        "-fx-text-fill: #2563eb; -fx-font-weight: 600; -fx-font-size: 13px; -fx-underline: false;");
                deleteLink.setStyle(
                        "-fx-text-fill: #ef4444; -fx-font-weight: 600; -fx-font-size: 13px; -fx-underline: false;");
                editLink.setOnAction(e -> showEditDamagedItemDialog(getTableView().getItems().get(getIndex())));
                deleteLink.setOnAction(e -> deleteDamagedItem(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
        TableStyler.styleTableColumn(actionsCol);

        table.getColumns().addAll(nameCol, qtyCol, dateCol, reasonCol, actionsCol);

        SortedList<DamagedItem> sortedData = new SortedList<>(filteredDamaged);
        sortedData.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedData);
        VBox.setVgrow(table, Priority.ALWAYS);

        container.getChildren().addAll(actionRow, table);
        loadDamaged();
        return container;
    }

    // ========== RENT ITEMS VIEW ==========
    private VBox createRentItemsView() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(24));
        container.setStyle("-fx-background-color: #f8fafc;");

        // --- Action Row ---
        HBox actionRow = new HBox(15);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        Button addBtn = new Button("+ Add Rent Item");
        addBtn.setStyle(StyleHelper.getActionPillButtonStyle());
        addBtn.setOnAction(e -> showAddRentItemDialog());

        TextField searchField = new TextField();
        searchField.setPromptText("Search items...");
        searchField.setPrefWidth(300);
        StyleHelper.styleTextField(searchField);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredRentItems.setPredicate(item -> {
                if (newVal == null || newVal.isEmpty())
                    return true;
                String lower = newVal.toLowerCase();
                return (item.getInventoryItemName() != null
                        && item.getInventoryItemName().toLowerCase().contains(lower));
            });
            updateCountLabel(rentItemCountLabel, filteredRentItems, "Item");
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        rentItemCountLabel = new Label();
        rentItemCountLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b; -fx-font-weight: 500;");
        updateCountLabel(rentItemCountLabel, filteredRentItems, "Item");

        actionRow.getChildren().addAll(addBtn, searchField, spacer, rentItemCountLabel);

        // --- Table ---
        TableView<RentItem> table = new TableView<>();
        TableStyler.applyModernStyling(table);

        TableColumn<RentItem, String> itemCol = new TableColumn<>("ITEM NAME");
        itemCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getInventoryItemName()));
        TableStyler.styleTableColumn(itemCol);

        TableColumn<RentItem, String> rateCol = new TableColumn<>("RATE / DAY");
        rateCol.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getRatePerDay() != null ? "₹" + cell.getValue().getRatePerDay().toString() : "₹0"));
        TableStyler.styleTableColumn(rateCol);

        TableColumn<RentItem, String> depositCol = new TableColumn<>("DEPOSIT");
        depositCol.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getDeposit() != null ? "₹" + cell.getValue().getDeposit().toString() : "₹0"));
        TableStyler.styleTableColumn(depositCol);

        TableColumn<RentItem, String> statusCol = new TableColumn<>("AVAILABILITY");
        statusCol.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getAvailable() ? "Available" : "Not Available"));
        TableStyler.styleTableColumn(statusCol);

        TableColumn<RentItem, Void> actionsCol = new TableColumn<>("ACTIONS");
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Hyperlink editLink = new Hyperlink("Edit");
            private final Hyperlink deleteLink = new Hyperlink("Delete");
            private final HBox actions = new HBox(12, editLink, deleteLink);
            {
                actions.setAlignment(Pos.CENTER);
                editLink.setStyle("-fx-text-fill: #2563eb; -fx-underline: false; -fx-font-weight: 500;");
                deleteLink.setStyle("-fx-text-fill: #ef4444; -fx-underline: false; -fx-font-weight: 500;");
                editLink.setOnAction(e -> showEditRentItemDialog(getTableView().getItems().get(getIndex())));
                deleteLink.setOnAction(e -> deleteRentItem(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actions);
            }
        });
        TableStyler.styleTableColumn(actionsCol);
        actionsCol.setPrefWidth(120);

        table.getColumns().addAll(itemCol, rateCol, depositCol, statusCol, actionsCol);

        SortedList<RentItem> sortedList = new SortedList<>(filteredRentItems);
        sortedList.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedList);

        container.getChildren().addAll(actionRow, table);
        loadRentItems();
        return container;
    }

    // ========== RENT VIEW (Active Rentals) ==========
    private VBox createRentView() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(24));
        container.setStyle("-fx-background-color: #f8fafc;");

        // --- Action Row ---
        HBox actionRow = new HBox(15);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        Button addBtn = new Button("+ Register Rental");
        addBtn.setStyle(StyleHelper.getActionPillButtonStyle());
        addBtn.setOnAction(e -> showEditRentDialog(null));

        TextField searchField = new TextField();
        searchField.setPromptText("Search rentals...");
        searchField.setPrefWidth(300);
        StyleHelper.styleTextField(searchField);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredRents.setPredicate(rent -> {
                if (newVal == null || newVal.isEmpty())
                    return true;
                String lower = newVal.toLowerCase();
                return (rent.getRenterName() != null && rent.getRenterName().toLowerCase().contains(lower)) ||
                        (rent.getRentItemName() != null && rent.getRentItemName().toLowerCase().contains(lower));
            });
            updateCountLabel(rentCountLabel, filteredRents, "Rental");
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        rentCountLabel = new Label();
        rentCountLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b; -fx-font-weight: 500;");
        updateCountLabel(rentCountLabel, filteredRents, "Rental");

        actionRow.getChildren().addAll(addBtn, searchField, spacer, rentCountLabel);

        // --- Table ---
        TableView<Rent> table = new TableView<>();
        TableStyler.applyModernStyling(table);

        TableColumn<Rent, String> itemCol = new TableColumn<>("ITEM");
        itemCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getRentItemName()));
        TableStyler.styleTableColumn(itemCol);

        TableColumn<Rent, String> renterCol = new TableColumn<>("RENTER");
        renterCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getRenterName()));
        TableStyler.styleTableColumn(renterCol);

        TableColumn<Rent, String> datesCol = new TableColumn<>("RENTAL PERIOD");
        datesCol.setCellValueFactory(cell -> {
            String start = cell.getValue().getRentStartDate() != null ? cell.getValue().getRentStartDate().toString()
                    : "...";
            String end = cell.getValue().getRentEndDate() != null ? cell.getValue().getRentEndDate().toString() : "...";
            return new SimpleStringProperty(start + " to " + end);
        });
        TableStyler.styleTableColumn(datesCol);

        TableColumn<Rent, String> statusCol = new TableColumn<>("STATUS");
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Rent rent = getTableView().getItems().get(getIndex());
                    String status = rent.isOverdue() ? "OVERDUE"
                            : (rent.getStatus() != null ? rent.getStatus() : "BOOKED");
                    Label label = new Label(status);
                    if ("RETURNED".equals(status)) {
                        label.setStyle(
                                "-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-padding: 4 8; -fx-background-radius: 4; -fx-font-size: 11px; -fx-font-weight: bold;");
                    } else if ("OVERDUE".equals(status)) {
                        label.setStyle(
                                "-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-padding: 4 8; -fx-background-radius: 4; -fx-font-size: 11px; -fx-font-weight: bold;");
                    } else {
                        label.setStyle(
                                "-fx-background-color: #fef3c7; -fx-text-fill: #b45309; -fx-padding: 4 8; -fx-background-radius: 4; -fx-font-size: 11px; -fx-font-weight: bold;");
                    }
                    setGraphic(label);
                }
            }
        });
        TableStyler.styleTableColumn(statusCol);

        TableColumn<Rent, Void> actionsCol = new TableColumn<>("ACTIONS");
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Hyperlink editLink = new Hyperlink("Edit");
            private final Hyperlink deleteLink = new Hyperlink("Delete");
            private final HBox actions = new HBox(12, editLink, deleteLink);
            {
                actions.setAlignment(Pos.CENTER);
                editLink.setStyle("-fx-text-fill: #2563eb; -fx-underline: false; -fx-font-weight: 500;");
                deleteLink.setStyle("-fx-text-fill: #ef4444; -fx-underline: false; -fx-font-weight: 500;");
                editLink.setOnAction(e -> showEditRentDialog(getTableView().getItems().get(getIndex())));
                deleteLink.setOnAction(e -> deleteRent(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actions);
            }
        });
        TableStyler.styleTableColumn(actionsCol);

        table.getColumns().addAll(itemCol, renterCol, datesCol, statusCol, actionsCol);

        SortedList<Rent> sortedList = new SortedList<>(filteredRents);
        sortedList.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedList);

        container.getChildren().addAll(actionRow, table);
        loadRents();
        return container;
    }

    // ========== RENT LIST VIEW (History/Summary) ==========
    private VBox createRentListView() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(24));
        container.setStyle("-fx-background-color: #f8fafc;");

        // --- Action Row ---
        HBox actionRow = new HBox(15);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setPromptText("Search rentals...");
        searchField.setPrefWidth(300);
        StyleHelper.styleTextField(searchField);
        // Re-use same count label or create a specific one for this view if needed
        Label viewCountLabel = new Label();
        viewCountLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b; -fx-font-weight: 500;");
        updateCountLabel(viewCountLabel, filteredRents, "Rental");

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredRents.setPredicate(rent -> {
                if (newVal == null || newVal.isEmpty())
                    return true;
                String lower = newVal.toLowerCase();
                return (rent.getRenterName() != null && rent.getRenterName().toLowerCase().contains(lower)) ||
                        (rent.getRentItemName() != null && rent.getRentItemName().toLowerCase().contains(lower)) ||
                        (rent.getRenterMobile() != null && rent.getRenterMobile().contains(lower));
            });
            updateCountLabel(viewCountLabel, filteredRents, "Rental");
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        actionRow.getChildren().addAll(searchField, spacer, viewCountLabel);

        // --- Table ---
        TableView<Rent> table = new TableView<>();
        TableStyler.applyModernStyling(table);

        TableColumn<Rent, String> itemCol = new TableColumn<>("RENTED ITEM");
        itemCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getRentItemName()));
        TableStyler.styleTableColumn(itemCol);

        TableColumn<Rent, String> renterCol = new TableColumn<>("RENTER DETAILS");
        renterCol.setCellValueFactory(cell -> {
            String name = cell.getValue().getRenterName() != null ? cell.getValue().getRenterName() : "Unknown";
            String mobile = cell.getValue().getRenterMobile() != null ? "\n" + cell.getValue().getRenterMobile() : "";
            return new SimpleStringProperty(name + mobile);
        });
        TableStyler.styleTableColumn(renterCol);

        TableColumn<Rent, String> startCol = new TableColumn<>("START DATE");
        startCol.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getRentStartDate() != null ? cell.getValue().getRentStartDate().toString() : ""));
        TableStyler.styleTableColumn(startCol);

        TableColumn<Rent, String> endCol = new TableColumn<>("END DATE");
        endCol.setCellValueFactory(cell -> {
            LocalDate date = cell.getValue().getRentEndDate();
            String dateStr = date != null ? date.toString() : "";
            if (cell.getValue().isOverdue())
                dateStr += " (OVERDUE)";
            return new SimpleStringProperty(dateStr);
        });
        TableStyler.styleTableColumn(endCol);

        TableColumn<Rent, String> amountCol = new TableColumn<>("AMOUNT/DEPOSIT");
        amountCol.setCellValueFactory(cell -> {
            String amt = cell.getValue().getAmount() != null ? "₹" + cell.getValue().getAmount() : "₹0";
            String dep = cell.getValue().getDeposit() != null ? "\nDep: ₹" + cell.getValue().getDeposit() : "";
            return new SimpleStringProperty(amt + dep);
        });
        TableStyler.styleTableColumn(amountCol);

        TableColumn<Rent, String> statusCol = new TableColumn<>("STATUS");
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty)
                    setGraphic(null);
                else {
                    Rent rent = getTableView().getItems().get(getIndex());
                    String status = rent.isOverdue() ? "OVERDUE"
                            : (rent.getStatus() != null ? rent.getStatus() : "BOOKED");
                    Label label = new Label(status);
                    if ("RETURNED".equals(status))
                        label.setStyle(
                                "-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-padding: 4 8; -fx-background-radius: 4; -fx-font-size: 11px; -fx-font-weight: bold;");
                    else if ("OVERDUE".equals(status))
                        label.setStyle(
                                "-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-padding: 4 8; -fx-background-radius: 4; -fx-font-size: 11px; -fx-font-weight: bold;");
                    else
                        label.setStyle(
                                "-fx-background-color: #fef3c7; -fx-text-fill: #b45309; -fx-padding: 4 8; -fx-background-radius: 4; -fx-font-size: 11px; -fx-font-weight: bold;");
                    setGraphic(label);
                }
            }
        });
        TableStyler.styleTableColumn(statusCol);

        TableColumn<Rent, Void> actionsCol = new TableColumn<>("ACTIONS");
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Hyperlink editLink = new Hyperlink("Edit");
            private final Hyperlink deleteLink = new Hyperlink("Delete");
            private final HBox actions = new HBox(12, editLink, deleteLink);
            {
                actions.setAlignment(Pos.CENTER);
                editLink.setStyle("-fx-text-fill: #2563eb; -fx-underline: false; -fx-font-weight: 500;");
                deleteLink.setStyle("-fx-text-fill: #ef4444; -fx-underline: false; -fx-font-weight: 500;");
                editLink.setOnAction(e -> showEditRentDialog(getTableView().getItems().get(getIndex())));
                deleteLink.setOnAction(e -> deleteRent(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actions);
            }
        });
        TableStyler.styleTableColumn(actionsCol);

        table.getColumns().addAll(itemCol, renterCol, startCol, endCol, amountCol, statusCol, actionsCol);

        SortedList<Rent> sortedList = new SortedList<>(filteredRents);
        sortedList.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedList);

        container.getChildren().addAll(actionRow, table);
        loadRents();
        return container;
    }

    // ========== LOAD METHODS ==========
    private void loadItems() {
        new Thread(() -> {
            try {
                var data = itemDAO.getAll();
                javafx.application.Platform.runLater(() -> {
                    itemList.setAll(data);
                });
            } catch (Exception e) {
                System.err.println("Error loading items: " + e.getMessage());
            }
        }).start();
    }

    private void loadDamaged() {
        new Thread(() -> {
            try {
                var data = damagedDAO.getAll();
                javafx.application.Platform.runLater(() -> {
                    damagedList.setAll(data);
                });
            } catch (Exception e) {
                System.err.println("Error loading damaged items: " + e.getMessage());
            }
        }).start();
    }

    private void loadRentItems() {
        new Thread(() -> {
            try {
                var data = rentItemDAO.getAll();
                javafx.application.Platform.runLater(() -> {
                    rentItemList.setAll(data);
                });
            } catch (Exception e) {
                System.err.println("Error loading rent items: " + e.getMessage());
            }
        }).start();
    }

    private void loadRents() {
        new Thread(() -> {
            try {
                var data = rentDAO.getAll();
                javafx.application.Platform.runLater(() -> {
                    rentList.setAll(data);
                });
            } catch (Exception e) {
                System.err.println("Error loading rents: " + e.getMessage());
            }
        }).start();
    }

    // ========== SAVE METHODS ==========
    private void saveItem(InventoryItem item, InventoryItem existing) {
        new Thread(() -> {
            boolean ok = false;
            try {
                if (existing == null) {
                    Long id = itemDAO.create(item);
                    ok = id != null;
                } else {
                    ok = itemDAO.update(item);
                }
            } catch (Exception e) {
                System.err.println("Error saving item: " + e.getMessage());
            }

            final boolean success = ok;
            javafx.application.Platform.runLater(() -> {
                if (success) {
                    loadItems();
                    new Alert(Alert.AlertType.INFORMATION,
                            existing == null ? "Item saved successfully!" : "Item updated successfully!",
                            ButtonType.OK).show();
                } else {
                    new Alert(Alert.AlertType.ERROR, "Save failed. Please check the logs.", ButtonType.OK).show();
                }
            });
        }).start();
    }

    private void saveDamagedItem(DamagedItem damaged, DamagedItem existing) {
        new Thread(() -> {
            boolean ok;
            if (existing == null) {
                ok = damagedDAO.create(damaged) != null;
            } else {
                ok = damagedDAO.update(damaged);
            }

            final boolean success = ok;
            javafx.application.Platform.runLater(() -> {
                if (success) {
                    loadDamaged();
                    loadItems();
                    new Alert(Alert.AlertType.INFORMATION,
                            existing == null ? "Damaged item recorded successfully!"
                                    : "Damaged record updated successfully!",
                            ButtonType.OK).show();
                } else {
                    new Alert(Alert.AlertType.ERROR, "Save failed", ButtonType.OK).show();
                }
            });
        }).start();
    }

    private void saveRentItem(RentItem rentItem, RentItem existing) {
        new Thread(() -> {
            boolean ok = (existing == null) ? (rentItemDAO.create(rentItem) != null) : rentItemDAO.update(rentItem);
            javafx.application.Platform.runLater(() -> {
                if (ok) {
                    loadRentItems();
                    new Alert(Alert.AlertType.INFORMATION,
                            existing == null ? "Rent item saved successfully!" : "Rent item updated successfully!",
                            ButtonType.OK).show();
                } else {
                    new Alert(Alert.AlertType.ERROR, "Save failed", ButtonType.OK).show();
                }
            });
        }).start();
    }

    private void saveRent(Rent rent, Rent existing) {
        new Thread(() -> {
            boolean ok = (existing == null) ? (rentDAO.create(rent) != null) : rentDAO.update(rent);
            javafx.application.Platform.runLater(() -> {
                if (ok) {
                    loadRents();
                    loadRentItems();
                    loadItems();
                    new Alert(Alert.AlertType.INFORMATION,
                            existing == null ? "Rent record saved successfully!" : "Rent record updated successfully!",
                            ButtonType.OK).show();
                } else {
                    new Alert(Alert.AlertType.ERROR, "Save failed", ButtonType.OK).show();
                }
            });
        }).start();
    }

    // ========== DELETE METHODS ==========
    private void deleteItem(InventoryItem item) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete this item?", ButtonType.OK, ButtonType.CANCEL);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                new Thread(() -> {
                    boolean ok = itemDAO.delete(item.getId());
                    javafx.application.Platform.runLater(() -> {
                        if (ok) {
                            loadItems();
                            new Alert(Alert.AlertType.INFORMATION, "Item deleted successfully!", ButtonType.OK)
                                    .showAndWait();
                        } else {
                            new Alert(Alert.AlertType.ERROR, "Delete failed", ButtonType.OK).showAndWait();
                        }
                    });
                }).start();
            }
        });
    }

    private void deleteDamagedItem(DamagedItem damaged) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete this damaged item?", ButtonType.OK,
                ButtonType.CANCEL);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                new Thread(() -> {
                    boolean ok = damagedDAO.delete(damaged.getId());
                    javafx.application.Platform.runLater(() -> {
                        if (ok) {
                            loadDamaged();
                            loadItems();
                            new Alert(Alert.AlertType.INFORMATION, "Damaged item deleted successfully!", ButtonType.OK)
                                    .showAndWait();
                        } else {
                            new Alert(Alert.AlertType.ERROR, "Delete failed", ButtonType.OK).showAndWait();
                        }
                    });
                }).start();
            }
        });
    }

    private void deleteRentItem(RentItem rentItem) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete this rent item?", ButtonType.OK,
                ButtonType.CANCEL);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                new Thread(() -> {
                    boolean ok = rentItemDAO.delete(rentItem.getId());
                    javafx.application.Platform.runLater(() -> {
                        if (ok) {
                            loadRentItems();
                            new Alert(Alert.AlertType.INFORMATION, "Rent item deleted successfully!", ButtonType.OK)
                                    .showAndWait();
                        } else {
                            new Alert(Alert.AlertType.ERROR, "Delete failed", ButtonType.OK).showAndWait();
                        }
                    });
                }).start();
            }
        });
    }

    private void deleteRent(Rent rent) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete this rent?", ButtonType.OK, ButtonType.CANCEL);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                new Thread(() -> {
                    boolean ok = rentDAO.delete(rent.getId());
                    javafx.application.Platform.runLater(() -> {
                        if (ok) {
                            loadRents();
                            loadRentItems();
                            loadItems();
                            new Alert(Alert.AlertType.INFORMATION, "Rent deleted successfully!", ButtonType.OK)
                                    .showAndWait();
                        } else {
                            new Alert(Alert.AlertType.ERROR, "Delete failed", ButtonType.OK).showAndWait();
                        }
                    });
                }).start();
            }
        });
    }

    // ========== EDIT DIALOGS ==========
    private void showEditItemDialog(InventoryItem item) {
        boolean isEdit = (item != null);
        InventoryItem targetItem = isEdit ? item : new InventoryItem();

        Stage dialog = new Stage();
        dialog.setTitle(isEdit ? "Edit Inventory Item" : "Add New Item");

        VBox form = new VBox(20);
        form.setPadding(new Insets(24));
        form.setStyle("-fx-background-color: #ffffff;");

        Label title = new Label(isEdit ? "Edit Inventory Item" : "Add New Item");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        VBox fieldsBox = new VBox(16);

        TextField itemNameField = new TextField(isEdit ? targetItem.getItemName() : "");
        itemNameField.setPromptText("Enter item name");
        StyleHelper.styleTextField(itemNameField);

        TextField skuField = new TextField(isEdit ? targetItem.getSkuCode() : "");
        skuField.setPromptText("Enter SKU or code");
        StyleHelper.styleTextField(skuField);

        TextField quantityField = new TextField(
                isEdit && targetItem.getQuantity() != null ? targetItem.getQuantity().toString() : "");
        quantityField.setPromptText("Enter quantity");
        StyleHelper.styleTextField(quantityField);
        quantityField
                .setTextFormatter(new TextFormatter<>(change -> change.getText().matches("[0-9]*") ? change : null));

        TextField locationField = new TextField(isEdit ? targetItem.getLocation() : "");
        locationField.setPromptText("Enter storage location");
        StyleHelper.styleTextField(locationField);

        DatePicker purchaseDatePicker = new DatePicker(isEdit ? targetItem.getPurchaseDate() : LocalDate.now());
        purchaseDatePicker.setPromptText("Select purchase date");
        purchaseDatePicker.setMaxWidth(Double.MAX_VALUE);
        styleDatePickerPopup(purchaseDatePicker);

        TextField supplierField = new TextField(isEdit ? targetItem.getSupplier() : "");
        supplierField.setPromptText("Enter supplier name");
        StyleHelper.styleTextField(supplierField);

        TextField valueField = new TextField(
                isEdit && targetItem.getValue() != null ? targetItem.getValue().toString() : "");
        valueField.setPromptText("Enter value");
        StyleHelper.styleTextField(valueField);
        valueField.setTextFormatter(
                new TextFormatter<>(change -> change.getText().matches("[0-9]*\\.?[0-9]*") ? change : null));

        TextArea notesField = new TextArea(isEdit ? targetItem.getNotes() : "");
        notesField.setPromptText("Enter additional notes");
        notesField.setPrefRowCount(3);
        StyleHelper.styleTextArea(notesField);

        fieldsBox.getChildren().addAll(
                FormStyler.createFormField("Item Name *", itemNameField),
                FormStyler.createFormField("SKU / Code", skuField),
                FormStyler.createFormField("Quantity", quantityField),
                FormStyler.createFormField("Location", locationField),
                FormStyler.createFormField("Purchase Date", purchaseDatePicker),
                FormStyler.createFormField("Supplier", supplierField),
                FormStyler.createFormField("Value", valueField),
                FormStyler.createFormField("Notes", notesField));

        Button saveBtn = new Button(isEdit ? "Update Item" : "Save Item");
        saveBtn.setStyle(StyleHelper.getActionPillButtonStyle());
        saveBtn.setOnAction(e -> {
            if (itemNameField.getText().trim().isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Item Name is required").show();
                return;
            }

            targetItem.setItemName(itemNameField.getText().trim());
            targetItem.setSkuCode(skuField.getText().trim());
            try {
                targetItem
                        .setQuantity(quantityField.getText().isEmpty() ? 0 : Integer.parseInt(quantityField.getText()));
            } catch (Exception ex) {
                targetItem.setQuantity(0);
            }
            targetItem.setLocation(locationField.getText().trim());
            targetItem.setPurchaseDate(purchaseDatePicker.getValue());
            targetItem.setSupplier(supplierField.getText().trim());
            try {
                targetItem.setValue(valueField.getText().isEmpty() ? null : new BigDecimal(valueField.getText()));
            } catch (Exception ex) {
                targetItem.setValue(null);
            }
            targetItem.setNotes(notesField.getText().trim());

            saveItem(targetItem, isEdit ? targetItem : null);
            dialog.close();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(
                "-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-background-radius: 20; -fx-padding: 8 20; -fx-font-weight: 600; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> dialog.close());

        HBox actions = new HBox(12, cancelBtn, saveBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        form.getChildren().addAll(title, fieldsBox, actions);

        ScrollPane sp = new ScrollPane(form);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background: #ffffff; -fx-background-color: #ffffff;");

        Scene scene = new Scene(sp, 500, 650);
        dialog.setScene(scene);
        dialog.show();
    }

    private void showEditDamagedItemDialog(DamagedItem damaged) {
        boolean isEdit = (damaged != null);
        DamagedItem targetDamaged = isEdit ? damaged : new DamagedItem();

        Stage dialog = new Stage();
        dialog.setTitle(isEdit ? "Edit Damaged Item" : "Mark Damaged Item");

        VBox form = new VBox(20);
        form.setPadding(new Insets(24));
        form.setStyle("-fx-background-color: #ffffff;");

        Label title = new Label(isEdit ? "Edit Damaged Item" : "Mark Damaged Item");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        VBox fieldsBox = new VBox(16);

        ComboBox<InventoryItem> itemCombo = new ComboBox<>();
        itemCombo.setItems(itemList);
        itemCombo.setPromptText("Select inventory item");
        itemCombo.setMaxWidth(Double.MAX_VALUE);
        StyleHelper.styleComboBox(itemCombo);

        if (isEdit) {
            for (InventoryItem item : itemList) {
                if (item.getId().equals(targetDamaged.getInventoryItemId())) {
                    itemCombo.setValue(item);
                    break;
                }
            }
        }

        TextField quantityField = new TextField(
                isEdit && targetDamaged.getQuantity() != null ? targetDamaged.getQuantity().toString() : "");
        quantityField.setPromptText("Enter damaged quantity");
        StyleHelper.styleTextField(quantityField);
        quantityField
                .setTextFormatter(new TextFormatter<>(change -> change.getText().matches("[0-9]*") ? change : null));

        DatePicker damageDatePicker = new DatePicker(isEdit ? targetDamaged.getDamageDate() : LocalDate.now());
        damageDatePicker.setPromptText("Select damage date");
        damageDatePicker.setMaxWidth(Double.MAX_VALUE);
        styleDatePickerPopup(damageDatePicker);

        TextArea reasonField = new TextArea(isEdit ? targetDamaged.getReason() : "");
        reasonField.setPromptText("Enter reason for damage");
        reasonField.setPrefRowCount(3);
        StyleHelper.styleTextArea(reasonField);

        fieldsBox.getChildren().addAll(
                FormStyler.createFormField("Inventory Item *", itemCombo),
                FormStyler.createFormField("Quantity", quantityField),
                FormStyler.createFormField("Damage Date", damageDatePicker),
                FormStyler.createFormField("Reason", reasonField));

        Button saveBtn = new Button(isEdit ? "Update Record" : "Save Record");
        saveBtn.setStyle(StyleHelper.getActionPillButtonStyle());
        saveBtn.setOnAction(e -> {
            if (itemCombo.getValue() == null) {
                new Alert(Alert.AlertType.WARNING, "Inventory Item is required").show();
                return;
            }

            targetDamaged.setInventoryItemId(itemCombo.getValue().getId());
            try {
                targetDamaged
                        .setQuantity(quantityField.getText().isEmpty() ? 0 : Integer.parseInt(quantityField.getText()));
            } catch (Exception ex) {
                targetDamaged.setQuantity(0);
            }
            targetDamaged.setDamageDate(damageDatePicker.getValue());
            targetDamaged.setReason(reasonField.getText().trim().isEmpty() ? null : reasonField.getText().trim());

            saveDamagedItem(targetDamaged, isEdit ? targetDamaged : null);
            dialog.close();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(
                "-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-background-radius: 20; -fx-padding: 8 20; -fx-font-weight: 600; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> dialog.close());

        HBox actions = new HBox(12, cancelBtn, saveBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        form.getChildren().addAll(title, fieldsBox, actions);

        Scene scene = new Scene(form, 450, 500);
        dialog.setScene(scene);
        dialog.show();
    }

    private void showEditRentItemDialog(RentItem rentItem) {
        boolean isEdit = (rentItem != null);
        RentItem targetItem = isEdit ? rentItem : new RentItem();

        Stage dialog = new Stage();
        dialog.setTitle(isEdit ? "Edit Rent Item" : "Add Rent Item");

        VBox form = new VBox(20);
        form.setPadding(new Insets(24));
        form.setStyle("-fx-background-color: #ffffff;");

        Label title = new Label(isEdit ? "Edit Rent Item" : "Add Rent Item");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        VBox fieldsBox = new VBox(16);

        ComboBox<InventoryItem> itemCombo = new ComboBox<>();
        itemCombo.setItems(itemList);
        itemCombo.setPromptText("Select inventory item");
        itemCombo.setMaxWidth(Double.MAX_VALUE);
        StyleHelper.styleComboBox(itemCombo);

        if (isEdit) {
            for (InventoryItem item : itemList) {
                if (item.getId().equals(targetItem.getInventoryItemId())) {
                    itemCombo.setValue(item);
                    break;
                }
            }
        }

        TextField rateField = new TextField(
                isEdit && targetItem.getRatePerDay() != null ? targetItem.getRatePerDay().toString() : "");
        rateField.setPromptText("Enter rate per day");
        StyleHelper.styleTextField(rateField);
        rateField.setTextFormatter(
                new TextFormatter<>(change -> change.getText().matches("[0-9]*\\.?[0-9]*") ? change : null));

        TextField depositField = new TextField(
                isEdit && targetItem.getDeposit() != null ? targetItem.getDeposit().toString() : "");
        depositField.setPromptText("Enter deposit amount");
        StyleHelper.styleTextField(depositField);
        depositField.setTextFormatter(
                new TextFormatter<>(change -> change.getText().matches("[0-9]*\\.?[0-9]*") ? change : null));

        ComboBox<String> availableCombo = new ComboBox<>();
        availableCombo.getItems().addAll("Available", "Not Available");
        availableCombo.setValue(targetItem.getAvailable() ? "Available" : "Not Available");
        availableCombo.setMaxWidth(Double.MAX_VALUE);
        StyleHelper.styleComboBox(availableCombo);

        fieldsBox.getChildren().addAll(
                FormStyler.createFormField("Inventory Item *", itemCombo),
                FormStyler.createFormField("Rate per Day", rateField),
                FormStyler.createFormField("Deposit Amount", depositField),
                FormStyler.createFormField("Availability", availableCombo));

        Button saveBtn = new Button(isEdit ? "Update Item" : "Save Item");
        saveBtn.setStyle(StyleHelper.getActionPillButtonStyle());
        saveBtn.setOnAction(e -> {
            if (itemCombo.getValue() == null) {
                new Alert(Alert.AlertType.WARNING, "Inventory Item is required").show();
                return;
            }

            targetItem.setInventoryItemId(itemCombo.getValue().getId());
            try {
                targetItem.setRatePerDay(
                        rateField.getText().isEmpty() ? BigDecimal.ZERO : new BigDecimal(rateField.getText()));
            } catch (Exception ex) {
                targetItem.setRatePerDay(BigDecimal.ZERO);
            }
            try {
                targetItem.setDeposit(
                        depositField.getText().isEmpty() ? BigDecimal.ZERO : new BigDecimal(depositField.getText()));
            } catch (Exception ex) {
                targetItem.setDeposit(BigDecimal.ZERO);
            }
            targetItem.setAvailable("Available".equals(availableCombo.getValue()));

            saveRentItem(targetItem, isEdit ? targetItem : null);
            dialog.close();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(
                "-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-background-radius: 20; -fx-padding: 8 20; -fx-font-weight: 600; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> dialog.close());

        HBox actions = new HBox(12, cancelBtn, saveBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        form.getChildren().addAll(title, fieldsBox, actions);

        Scene scene = new Scene(form, 450, 480);
        dialog.setScene(scene);
        dialog.show();
    }

    private void showEditRentDialog(Rent rent) {
        boolean isEdit = (rent != null);
        Rent targetRent = isEdit ? rent : new Rent();

        Stage dialog = new Stage();
        dialog.setTitle(isEdit ? "Edit Rental" : "Register New Rental");

        VBox form = new VBox(20);
        form.setPadding(new Insets(24));
        form.setStyle("-fx-background-color: #ffffff;");

        Label title = new Label(isEdit ? "Edit Rental Record" : "New Rental Registration");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        VBox fieldsBox = new VBox(16);

        ComboBox<RentItem> rentItemCombo = new ComboBox<>();
        rentItemCombo.setItems(rentItemList);
        rentItemCombo.setPromptText("Select item for rent");
        rentItemCombo.setMaxWidth(Double.MAX_VALUE);
        StyleHelper.styleComboBox(rentItemCombo);

        if (isEdit) {
            for (RentItem item : rentItemList) {
                if (item.getId().equals(targetRent.getRentItemId())) {
                    rentItemCombo.setValue(item);
                    break;
                }
            }
        }

        TextField renterNameField = new TextField(isEdit ? targetRent.getRenterName() : "");
        renterNameField.setPromptText("Enter renter name");
        StyleHelper.styleTextField(renterNameField);

        TextField renterMobileField = new TextField(isEdit ? targetRent.getRenterMobile() : "");
        renterMobileField.setPromptText("Enter mobile number");
        StyleHelper.styleTextField(renterMobileField);

        DatePicker startDatePicker = new DatePicker(
                targetRent.getRentStartDate() != null ? targetRent.getRentStartDate() : LocalDate.now());
        startDatePicker.setMaxWidth(Double.MAX_VALUE);
        styleDatePickerPopup(startDatePicker);

        DatePicker endDatePicker = new DatePicker(targetRent.getRentEndDate());
        endDatePicker.setMaxWidth(Double.MAX_VALUE);
        styleDatePickerPopup(endDatePicker);

        TextField amountField = new TextField(
                isEdit && targetRent.getAmount() != null ? targetRent.getAmount().toString() : "");
        amountField.setPromptText("Enter rental amount");
        StyleHelper.styleTextField(amountField);
        amountField.setTextFormatter(
                new TextFormatter<>(change -> change.getText().matches("[0-9]*\\.?[0-9]*") ? change : null));

        TextField depositField = new TextField(
                isEdit && targetRent.getDeposit() != null ? targetRent.getDeposit().toString() : "");
        depositField.setPromptText("Enter deposit amount");
        StyleHelper.styleTextField(depositField);
        depositField.setTextFormatter(
                new TextFormatter<>(change -> change.getText().matches("[0-9]*\\.?[0-9]*") ? change : null));

        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("BOOKED", "RETURNED", "OVERDUE");
        statusCombo.setValue(isEdit ? targetRent.getStatus() : "BOOKED");
        statusCombo.setMaxWidth(Double.MAX_VALUE);
        StyleHelper.styleComboBox(statusCombo);

        fieldsBox.getChildren().addAll(
                FormStyler.createFormField("Rental Item *", rentItemCombo),
                FormStyler.createFormField("Renter Name *", renterNameField),
                FormStyler.createFormField("Renter Mobile", renterMobileField),
                FormStyler.createFormField("Start Date", startDatePicker),
                FormStyler.createFormField("End Date", endDatePicker),
                FormStyler.createFormField("Amount", amountField),
                FormStyler.createFormField("Deposit", depositField),
                FormStyler.createFormField("Status", statusCombo));

        Button saveBtn = new Button(isEdit ? "Update Rental" : "Register Rental");
        saveBtn.setStyle(StyleHelper.getActionPillButtonStyle());
        saveBtn.setOnAction(e -> {
            if (rentItemCombo.getValue() == null || renterNameField.getText().trim().isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Item and Renter Name are required").show();
                return;
            }

            targetRent.setRentItemId(rentItemCombo.getValue().getId());
            targetRent.setRenterName(renterNameField.getText().trim());
            targetRent.setRenterMobile(renterMobileField.getText().trim());
            targetRent.setRentStartDate(startDatePicker.getValue());
            targetRent.setRentEndDate(endDatePicker.getValue());
            try {
                targetRent.setAmount(
                        amountField.getText().isEmpty() ? BigDecimal.ZERO : new BigDecimal(amountField.getText()));
            } catch (Exception ex) {
                targetRent.setAmount(BigDecimal.ZERO);
            }
            try {
                targetRent.setDeposit(
                        depositField.getText().isEmpty() ? BigDecimal.ZERO : new BigDecimal(depositField.getText()));
            } catch (Exception ex) {
                targetRent.setDeposit(BigDecimal.ZERO);
            }
            targetRent.setStatus(statusCombo.getValue());

            saveRent(targetRent, isEdit ? targetRent : null);
            dialog.close();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(
                "-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-background-radius: 20; -fx-padding: 8 20; -fx-font-weight: 600; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> dialog.close());

        HBox actions = new HBox(12, cancelBtn, saveBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        form.getChildren().addAll(title, fieldsBox, actions);

        ScrollPane sp = new ScrollPane(form);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background: #ffffff; -fx-background-color: #ffffff;");

        Scene scene = new Scene(sp, 500, 650);
        dialog.setScene(scene);
        dialog.show();
    }

}
