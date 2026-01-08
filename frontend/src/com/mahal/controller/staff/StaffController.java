package com.mahal.controller.staff;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import com.mahal.database.StaffDAO;
import com.mahal.database.StaffSalaryDAO;
import java.io.File;
import com.mahal.model.Staff;
import com.mahal.model.StaffSalary;
import com.mahal.util.StyleHelper;
import com.mahal.util.TableStyler;
import com.mahal.util.FormStyler;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;
import javafx.geometry.Pos;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.mahal.util.EventBus;
import javafx.application.Platform;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.stage.FileChooser;
import com.mahal.service.SalaryReportPDFService;

public class StaffController {
    private VBox view;
    private StackPane contentPane;
    private VBox staffViewPane;
    private VBox salaryViewPane;
    private VBox reportViewPane;
    private TableView<Staff> staffTable;
    private TableView<StaffSalary> salaryTable;
    private ObservableList<Staff> staffList;
    private ObservableList<StaffSalary> salaryList;
    private StaffDAO staffDAO;
    private StaffSalaryDAO salaryDAO;
    private TextField searchField;
    private TextField salarySearchField;
    private TextField reportSearchField;
    private DatePicker reportFromDate;
    private DatePicker reportToDate;
    private ComboBox<Staff> reportStaffFilter;

    private void setMaxLength(TextInputControl control, int maxLen) {
        control.setTextFormatter(new TextFormatter<String>(change -> {
            String newText = change.getControlNewText();
            if (newText.length() <= maxLen) {
                return change;
            }
            return null;
        }));
    }

    public StaffController() {
        this.staffDAO = new StaffDAO();
        this.salaryDAO = new StaffSalaryDAO();
        this.staffList = FXCollections.observableArrayList();
        this.salaryList = FXCollections.observableArrayList();
        createView();
        loadStaffData();
        loadSalaryData();

        // Subscribe to sync events
        EventBus.getInstance().subscribe("staff", event -> Platform.runLater(this::loadStaffData));
        EventBus.getInstance().subscribe("staff_salaries", event -> Platform.runLater(this::loadSalaryData));
    }

    public VBox getView() {
        return view;
    }

    private void createView() {
        view = new VBox(20);
        view.setPadding(new Insets(32));
        view.setStyle("-fx-background-color: transparent;");

        VBox mainCard = new VBox(24);
        mainCard.setStyle(StyleHelper.getCardStyle());
        VBox.setVgrow(mainCard, Priority.ALWAYS);

        // Header Section
        VBox header = new VBox(8);
        Label titleLabel = new Label("Staff Management");
        titleLabel.setStyle(StyleHelper.getTitleStyle());
        Label subtitleLabel = new Label("Manage your staff members and their salary records");
        subtitleLabel.setStyle(StyleHelper.getSubtitleStyle());
        header.getChildren().addAll(titleLabel, subtitleLabel);

        // Navigation Switcher (Pill Style)
        HBox switcherContainer = new HBox();
        switcherContainer.setStyle(StyleHelper.getPillSwitcherContainerStyle());
        switcherContainer.setMaxWidth(Region.USE_PREF_SIZE);

        Button staffBtn = new Button("Staff Members");
        Button salaryBtn = new Button("Salary Payments");
        Button reportBtn = new Button("Salary Reports");

        staffBtn.setStyle(StyleHelper.getPillButtonStyle(true));
        salaryBtn.setStyle(StyleHelper.getPillButtonStyle(false));
        reportBtn.setStyle(StyleHelper.getPillButtonStyle(false));

        switcherContainer.getChildren().addAll(staffBtn, salaryBtn, reportBtn);

        contentPane = new StackPane();
        staffViewPane = createStaffView();
        salaryViewPane = createSalaryView();
        reportViewPane = createReportView();
        contentPane.getChildren().setAll(staffViewPane);

        staffBtn.setOnAction(e -> {
            staffBtn.setStyle(StyleHelper.getPillButtonStyle(true));
            salaryBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            reportBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            contentPane.getChildren().setAll(staffViewPane);
        });

        salaryBtn.setOnAction(e -> {
            staffBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            salaryBtn.setStyle(StyleHelper.getPillButtonStyle(true));
            reportBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            contentPane.getChildren().setAll(salaryViewPane);
        });

        reportBtn.setOnAction(e -> {
            staffBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            salaryBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            reportBtn.setStyle(StyleHelper.getPillButtonStyle(true));
            contentPane.getChildren().setAll(reportViewPane);
        });

        mainCard.getChildren().addAll(header, switcherContainer, contentPane);
        view.getChildren().add(mainCard);
    }

    private VBox createStaffView() {
        VBox staffView = new VBox(20);

        // Action Row: Search and Add Button
        HBox actionRow = new HBox(15);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        searchField = new TextField();
        searchField.setPromptText("Search by name, designation or mobile...");
        searchField.setPrefWidth(350);
        StyleHelper.styleTextField(searchField);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addButton = new Button("Add Staff");
        addButton.setStyle(StyleHelper.getActionPillButtonStyle());
        addButton.setOnAction(e -> showStaffDialog(null));

        actionRow.getChildren().addAll(searchField, spacer, addButton);

        staffTable = new TableView<>();
        TableStyler.applyModernStyling(staffTable);

        TableColumn<Staff, String> nameCol = new TableColumn<>("NAME");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableStyler.styleTableColumn(nameCol);

        TableColumn<Staff, String> designationCol = new TableColumn<>("DESIGNATION");
        designationCol.setCellValueFactory(new PropertyValueFactory<>("designation"));
        TableStyler.styleTableColumn(designationCol);

        TableColumn<Staff, String> salaryCol = new TableColumn<>("BASE SALARY");
        salaryCol.setCellValueFactory(cell -> {
            BigDecimal salary = cell.getValue().getSalary();
            return new javafx.beans.property.SimpleStringProperty(
                    salary != null ? salary.toString() : "-");
        });
        TableStyler.styleTableColumn(salaryCol);

        TableColumn<Staff, String> mobileCol = new TableColumn<>("MOBILE");
        mobileCol.setCellValueFactory(new PropertyValueFactory<>("mobile"));
        TableStyler.styleTableColumn(mobileCol);

        TableColumn<Staff, String> joiningDateCol = new TableColumn<>("JOINING DATE");
        joiningDateCol.setCellValueFactory(cell -> {
            LocalDate date = cell.getValue().getJoiningDate();
            return new javafx.beans.property.SimpleStringProperty(
                    date != null ? date.format(DateTimeFormatter.ISO_LOCAL_DATE) : "-");
        });
        TableStyler.styleTableColumn(joiningDateCol);

        TableColumn<Staff, String> actionsCol = new TableColumn<>("ACTIONS");
        actionsCol.setCellFactory(param -> new TableCell<Staff, String>() {
            private final Hyperlink editLink = new Hyperlink("Edit");
            private final Hyperlink deleteLink = new Hyperlink("Delete");
            private final Hyperlink payLink = new Hyperlink("Pay");

            {
                editLink.setStyle("-fx-text-fill: #2563eb; -fx-font-weight: bold;");
                deleteLink.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                payLink.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");

                editLink.setOnAction(e -> {
                    Staff staff = getTableView().getItems().get(getIndex());
                    if (staff != null)
                        showStaffDialog(staffDAO.getById(staff.getId()));
                });

                deleteLink.setOnAction(e -> {
                    deleteStaff(getTableView().getItems().get(getIndex()));
                });

                payLink.setOnAction(e -> {
                    Staff staff = getTableView().getItems().get(getIndex());
                    if (staff != null) {
                        StaffSalary newSal = new StaffSalary();
                        newSal.setStaffId(staff.getId());
                        newSal.setStaffName(staff.getName());
                        newSal.setSalary(staff.getSalary());
                        newSal.setPaidDate(LocalDate.now());
                        showSalaryDialog(newSal);
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(12);
                    box.setAlignment(Pos.CENTER);
                    box.getChildren().addAll(editLink, deleteLink, payLink);
                    setGraphic(box);
                }
            }
        });
        TableStyler.styleTableColumn(actionsCol);

        staffTable.getColumns().addAll(nameCol, designationCol, salaryCol, mobileCol, joiningDateCol, actionsCol);

        // Filtered List Implementation
        FilteredList<Staff> filteredData = new FilteredList<>(staffList, p -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(staff -> {
                if (newValue == null || newValue.isEmpty())
                    return true;
                String lowerCaseFilter = newValue.toLowerCase();
                if (staff.getName() != null && staff.getName().toLowerCase().contains(lowerCaseFilter))
                    return true;
                if (staff.getDesignation() != null && staff.getDesignation().toLowerCase().contains(lowerCaseFilter))
                    return true;
                if (staff.getMobile() != null && staff.getMobile().toLowerCase().contains(lowerCaseFilter))
                    return true;
                return false;
            });
        });

        SortedList<Staff> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(staffTable.comparatorProperty());
        staffTable.setItems(sortedData);

        staffView.getChildren().addAll(actionRow, staffTable);
        return staffView;
    }

    private VBox createSalaryView() {
        VBox salaryView = new VBox(20);

        // Action Row: Search and Add Button
        HBox actionRow = new HBox(15);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        salarySearchField = new TextField();
        salarySearchField.setPromptText("Search transactions by staff name or remarks...");
        salarySearchField.setPrefWidth(350);
        StyleHelper.styleTextField(salarySearchField);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addPaymentButton = new Button("Add Salary Payment");
        addPaymentButton.setStyle(StyleHelper.getActionPillButtonStyle());
        addPaymentButton.setOnAction(e -> showSalaryDialog(null));

        actionRow.getChildren().addAll(salarySearchField, spacer, addPaymentButton);

        salaryTable = new TableView<>();
        TableStyler.applyModernStyling(salaryTable);

        TableColumn<StaffSalary, String> staffCol = new TableColumn<>("STAFF");
        staffCol.setCellValueFactory(new PropertyValueFactory<>("staffName"));
        TableStyler.styleTableColumn(staffCol);

        TableColumn<StaffSalary, String> paidDateCol = new TableColumn<>("PAID DATE");
        paidDateCol.setCellValueFactory(cell -> {
            LocalDate date = cell.getValue().getPaidDate();
            return new javafx.beans.property.SimpleStringProperty(
                    date != null ? date.format(DateTimeFormatter.ISO_LOCAL_DATE) : "-");
        });
        TableStyler.styleTableColumn(paidDateCol);

        TableColumn<StaffSalary, String> salaryCol = new TableColumn<>("SALARY");
        salaryCol.setCellValueFactory(cell -> {
            BigDecimal sal = cell.getValue().getSalary();
            return new javafx.beans.property.SimpleStringProperty(
                    sal != null ? sal.toString() : "-");
        });
        TableStyler.styleTableColumn(salaryCol);

        TableColumn<StaffSalary, String> paidCol = new TableColumn<>("PAID");
        paidCol.setCellValueFactory(cell -> {
            BigDecimal paid = cell.getValue().getPaidAmount();
            return new javafx.beans.property.SimpleStringProperty(
                    paid != null ? paid.toString() : "-");
        });
        TableStyler.styleTableColumn(paidCol);

        TableColumn<StaffSalary, String> modeCol = new TableColumn<>("MODE");
        modeCol.setCellValueFactory(new PropertyValueFactory<>("paymentMode"));
        TableStyler.styleTableColumn(modeCol);

        TableColumn<StaffSalary, String> balanceCol = new TableColumn<>("BALANCE");
        balanceCol.setCellValueFactory(cell -> {
            BigDecimal balance = cell.getValue().getBalance();
            return new javafx.beans.property.SimpleStringProperty(
                    balance != null ? balance.toString() : "-");
        });
        TableStyler.styleTableColumn(balanceCol);

        TableColumn<StaffSalary, String> remarksCol = new TableColumn<>("REMARKS");
        remarksCol.setCellValueFactory(new PropertyValueFactory<>("remarks"));
        TableStyler.styleTableColumn(remarksCol);

        TableColumn<StaffSalary, String> actionsCol = new TableColumn<>("ACTIONS");
        actionsCol.setCellFactory(param -> new TableCell<StaffSalary, String>() {
            private final Hyperlink editLink = new Hyperlink("Edit");
            private final Hyperlink deleteLink = new Hyperlink("Delete");

            {
                editLink.setStyle("-fx-text-fill: #2563eb; -fx-font-weight: bold;");
                deleteLink.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");

                editLink.setOnAction(e -> {
                    StaffSalary sal = getTableView().getItems().get(getIndex());
                    if (sal != null)
                        showSalaryDialog(salaryDAO.getById(sal.getId()));
                });

                deleteLink.setOnAction(e -> {
                    deleteSalary(getTableView().getItems().get(getIndex()));
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(12);
                    box.setAlignment(Pos.CENTER);
                    box.getChildren().addAll(editLink, deleteLink);
                    setGraphic(box);
                }
            }
        });
        TableStyler.styleTableColumn(actionsCol);

        salaryTable.getColumns().addAll(staffCol, paidDateCol, salaryCol, paidCol, modeCol, balanceCol, remarksCol,
                actionsCol);

        // Filtered List Implementation
        FilteredList<StaffSalary> filteredData = new FilteredList<>(salaryList, p -> true);
        salarySearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(sal -> {
                if (newValue == null || newValue.isEmpty())
                    return true;
                String lowerCaseFilter = newValue.toLowerCase();
                if (sal.getStaffName() != null && sal.getStaffName().toLowerCase().contains(lowerCaseFilter))
                    return true;
                if (sal.getRemarks() != null && sal.getRemarks().toLowerCase().contains(lowerCaseFilter))
                    return true;
                if (sal.getPaymentMode() != null && sal.getPaymentMode().toLowerCase().contains(lowerCaseFilter))
                    return true;
                return false;
            });
        });

        SortedList<StaffSalary> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(salaryTable.comparatorProperty());
        salaryTable.setItems(sortedData);

        salaryView.getChildren().addAll(actionRow, salaryTable);
        return salaryView;
    }

    private VBox createReportView() {
        VBox reportView = new VBox(20);

        // Action Row: Search and Export Button (Mock)
        HBox actionRow = new HBox(15);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        reportSearchField = new TextField();
        reportSearchField.setPromptText("Quick search...");
        reportSearchField.setPrefWidth(150);
        StyleHelper.styleTextField(reportSearchField);

        reportFromDate = new DatePicker();
        reportFromDate.setPromptText("From Date");
        reportFromDate.setPrefWidth(120);
        StyleHelper.styleDatePicker(reportFromDate);

        reportToDate = new DatePicker();
        reportToDate.setPromptText("To Date");
        reportToDate.setPrefWidth(120);
        StyleHelper.styleDatePicker(reportToDate);

        reportStaffFilter = new ComboBox<>();
        reportStaffFilter.setItems(staffList);
        reportStaffFilter.setPromptText("All Staff");
        reportStaffFilter.setPrefWidth(150);
        StyleHelper.styleComboBox(reportStaffFilter);
        // Clear filter option
        reportStaffFilter.setCellFactory(lv -> new ListCell<Staff>() {
            @Override
            protected void updateItem(Staff item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "All Staff" : item.getName());
            }
        });
        reportStaffFilter.setButtonCell(new ListCell<Staff>() {
            @Override
            protected void updateItem(Staff item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "All Staff" : item.getName());
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button clearButton = new Button("Clear");
        StyleHelper.styleSecondaryButton(clearButton);
        clearButton.setOnAction(e -> {
            reportSearchField.clear();
            reportFromDate.setValue(null);
            reportToDate.setValue(null);
            reportStaffFilter.setValue(null);
        });

        Button exportButton = new Button("Export PDF");
        exportButton.setStyle(StyleHelper.getActionPillButtonStyle());
        exportButton.setOnAction(e -> handleExportPDF());

        actionRow.getChildren().addAll(reportSearchField, reportFromDate, reportToDate, reportStaffFilter, clearButton,
                spacer,
                exportButton);

        TableView<StaffSalary> reportTable = new TableView<>();
        TableStyler.applyModernStyling(reportTable);

        TableColumn<StaffSalary, String> staffCol = new TableColumn<>("STAFF");
        staffCol.setCellValueFactory(new PropertyValueFactory<>("staffName"));
        TableStyler.styleTableColumn(staffCol);

        TableColumn<StaffSalary, String> designationCol = new TableColumn<>("DESIGNATION");
        designationCol.setCellValueFactory(new PropertyValueFactory<>("designation"));
        TableStyler.styleTableColumn(designationCol);

        TableColumn<StaffSalary, String> salaryCol = new TableColumn<>("SALARY");
        salaryCol.setCellValueFactory(cell -> {
            BigDecimal sal = cell.getValue().getSalary();
            return new javafx.beans.property.SimpleStringProperty(
                    sal != null ? sal.toString() : "-");
        });
        TableStyler.styleTableColumn(salaryCol);

        TableColumn<StaffSalary, String> paidDateCol = new TableColumn<>("PAID DATE");
        paidDateCol.setCellValueFactory(cell -> {
            LocalDate date = cell.getValue().getPaidDate();
            return new javafx.beans.property.SimpleStringProperty(
                    date != null ? date.format(DateTimeFormatter.ISO_LOCAL_DATE) : "-");
        });
        TableStyler.styleTableColumn(paidDateCol);

        TableColumn<StaffSalary, String> paidCol = new TableColumn<>("PAID AMOUNT");
        paidCol.setCellValueFactory(cell -> {
            BigDecimal paid = cell.getValue().getPaidAmount();
            return new javafx.beans.property.SimpleStringProperty(
                    paid != null ? paid.toString() : "-");
        });
        TableStyler.styleTableColumn(paidCol);

        TableColumn<StaffSalary, String> balanceCol = new TableColumn<>("BALANCE");
        balanceCol.setCellValueFactory(cell -> {
            BigDecimal balance = cell.getValue().getBalance();
            return new javafx.beans.property.SimpleStringProperty(
                    balance != null ? balance.toString() : "-");
        });
        TableStyler.styleTableColumn(balanceCol);

        TableColumn<StaffSalary, String> modeCol = new TableColumn<>("MODE");
        modeCol.setCellValueFactory(new PropertyValueFactory<>("paymentMode"));
        TableStyler.styleTableColumn(modeCol);

        TableColumn<StaffSalary, String> remarksCol = new TableColumn<>("REMARKS");
        remarksCol.setCellValueFactory(new PropertyValueFactory<>("remarks"));
        TableStyler.styleTableColumn(remarksCol);

        reportTable.getColumns().addAll(staffCol, designationCol, salaryCol, paidDateCol, paidCol, balanceCol, modeCol,
                remarksCol);

        // Filtered List Implementation
        FilteredList<StaffSalary> filteredData = new FilteredList<>(salaryList, p -> true);

        javafx.beans.value.ChangeListener<Object> filterListener = (obs, oldVal, newVal) -> {
            filteredData.setPredicate(sal -> {
                // Search Text Filter
                String searchText = reportSearchField.getText();
                if (searchText != null && !searchText.isEmpty()) {
                    String lower = searchText.toLowerCase();
                    boolean match = (sal.getStaffName() != null && sal.getStaffName().toLowerCase().contains(lower)) ||
                            (sal.getDesignation() != null && sal.getDesignation().toLowerCase().contains(lower)) ||
                            (sal.getRemarks() != null && sal.getRemarks().toLowerCase().contains(lower));
                    if (!match)
                        return false;
                }

                // Date Range Filter
                LocalDate from = reportFromDate.getValue();
                LocalDate to = reportToDate.getValue();
                if (sal.getPaidDate() != null) {
                    if (from != null && sal.getPaidDate().isBefore(from))
                        return false;
                    if (to != null && sal.getPaidDate().isAfter(to))
                        return false;
                } else if (from != null || to != null) {
                    return false;
                }

                // Staff Filter
                Staff selectedStaff = reportStaffFilter.getValue();
                if (selectedStaff != null && !selectedStaff.getId().equals(sal.getStaffId())) {
                    return false;
                }

                return true;
            });
        };

        reportSearchField.textProperty().addListener(filterListener);
        reportFromDate.valueProperty().addListener(filterListener);
        reportToDate.valueProperty().addListener(filterListener);
        reportStaffFilter.valueProperty().addListener(filterListener);

        SortedList<StaffSalary> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(reportTable.comparatorProperty());
        reportTable.setItems(sortedData);

        reportView.getChildren().addAll(actionRow, reportTable);
        return reportView;
    }

    private void loadStaffData() {
        new Thread(() -> {
            try {
                java.util.List<Staff> staffs = staffDAO.getAll();
                System.out.println("loadStaffData: Retrieved " + staffs.size() + " staff records from database");
                javafx.application.Platform.runLater(() -> {
                    staffList.clear();
                    staffList.addAll(staffs);
                    System.out.println("loadStaffData: Added " + staffs.size() + " records to table (staffList size: "
                            + staffList.size() + ")");
                });
            } catch (Exception e) {
                System.err.println("Error loading staff data: " + e.getMessage());
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setContentText("Failed to load staff data: " + e.getMessage());
                    alert.show();
                });
            }
        }).start();
    }

    private void loadSalaryData() {
        new Thread(() -> {
            java.util.List<StaffSalary> salaries = salaryDAO.getAll();
            javafx.application.Platform.runLater(() -> {
                salaryList.clear();
                salaryList.addAll(salaries);
            });
        }).start();
    }

    private void showStaffDialog(Staff staff) {
        Stage dialog = new Stage();
        dialog.setTitle(staff == null ? "Add Staff" : "Edit Staff");

        // Store the original staff ID for editing (in case staff object gets modified)
        final Long originalStaffId = (staff != null) ? staff.getId() : null;

        VBox root = new VBox(8); // Reduced spacing
        root.setPadding(new Insets(10)); // Reduced padding
        root.setPrefWidth(600); // Increased width
        root.setPrefHeight(550); // Increased height
        FormStyler.styleFormDialog(root);

        Label titleLabel = FormStyler.createFormLabel("Staff Details");
        titleLabel.setStyle(
                "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + StyleHelper.TEXT_GRAY_900 + ";"); // Smaller
                                                                                                                  // title

        TextField nameField = new TextField();
        nameField.setPromptText("Name *");
        StyleHelper.styleTextField(nameField);
        setMaxLength(nameField, 100);
        if (staff != null && staff.getName() != null) {
            nameField.setText(staff.getName());
        }

        TextField designationField = new TextField();
        designationField.setPromptText("Designation");
        StyleHelper.styleTextField(designationField);
        setMaxLength(designationField, 50);
        if (staff != null && staff.getDesignation() != null) {
            designationField.setText(staff.getDesignation());
        }

        TextField salaryField = new TextField();
        salaryField.setPromptText("Base Salary");
        StyleHelper.styleTextField(salaryField);
        setMaxLength(salaryField, 20);
        if (staff != null && staff.getSalary() != null) {
            salaryField.setText(staff.getSalary().toString());
        }

        TextField mobileField = new TextField();
        mobileField.setPromptText("Mobile");
        StyleHelper.styleTextField(mobileField);
        setMaxLength(mobileField, 15);
        if (staff != null && staff.getMobile() != null) {
            mobileField.setText(staff.getMobile());
        }

        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        StyleHelper.styleTextField(emailField);
        setMaxLength(emailField, 100);
        if (staff != null && staff.getEmail() != null) {
            emailField.setText(staff.getEmail());
        }

        DatePicker joiningDatePicker = new DatePicker();
        StyleHelper.styleDatePicker(joiningDatePicker);
        if (staff != null && staff.getJoiningDate() != null) {
            joiningDatePicker.setValue(staff.getJoiningDate());
        }

        TextArea addressField = new TextArea();
        addressField.setPromptText("Address");
        addressField.setPrefRowCount(2);
        addressField.setPrefHeight(60);
        StyleHelper.styleTextArea(addressField);
        setMaxLength(addressField, 200);
        if (staff != null && staff.getAddress() != null) {
            addressField.setText(staff.getAddress());
        }

        TextArea notesField = new TextArea();
        notesField.setPromptText("Notes");
        notesField.setPrefRowCount(2);
        notesField.setPrefHeight(60);
        StyleHelper.styleTextArea(notesField);
        setMaxLength(notesField, 500);
        if (staff != null && staff.getNotes() != null) {
            notesField.setText(staff.getNotes());
        }

        Button saveButton = new Button("Save");
        saveButton.setStyle(StyleHelper.getEditButtonStyle());

        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle(StyleHelper.getSecondaryButtonStyle());
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(8, 0, 0, 0)); // Reduced padding
        buttonBox.getChildren().addAll(cancelButton, saveButton);
        cancelButton.setOnAction(e -> dialog.close());

        saveButton.setOnAction(e -> {
            // Get and trim all field values
            String name = nameField.getText().trim();
            String designation = designationField.getText().trim();
            String salary = salaryField.getText().trim();
            String mobile = mobileField.getText().trim();
            String email = emailField.getText().trim();
            String address = addressField.getText().trim();
            String notes = notesField.getText().trim();

            // Validate required fields
            if (name.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Validation Error");
                alert.setContentText("Name is required. Please enter a name.");
                alert.showAndWait();
                nameField.requestFocus();
                return;
            }

            // Validate salary format if provided
            BigDecimal salaryValue = null;
            if (!salary.isEmpty()) {
                try {
                    salaryValue = new BigDecimal(salary);
                } catch (NumberFormatException ex) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Validation Error");
                    alert.setContentText("Invalid salary format. Please enter a valid number.");
                    alert.showAndWait();
                    salaryField.requestFocus();
                    return;
                }
            }

            // Convert empty strings to null to prevent blank values in database
            Staff staffData = new Staff();
            staffData.setName(name); // Name is required, so always set it
            staffData.setDesignation(designation.isEmpty() ? null : designation);
            staffData.setSalary(salaryValue);
            staffData.setMobile(mobile.isEmpty() ? null : mobile);
            staffData.setEmail(email.isEmpty() ? null : email);
            staffData.setJoiningDate(joiningDatePicker.getValue());
            staffData.setAddress(address.isEmpty() ? null : address);
            staffData.setNotes(notes.isEmpty() ? null : notes);

            // Disable button during save
            saveButton.setDisable(true);
            saveButton.setText("Saving...");

            new Thread(() -> {
                try {
                    boolean success;
                    if (staff == null) {
                        Long newId = staffDAO.create(staffData);
                        success = newId != null;
                    } else {
                        // Use the original staff ID that was stored when dialog was opened
                        if (originalStaffId == null) {
                            javafx.application.Platform.runLater(() -> {
                                saveButton.setDisable(false);
                                saveButton.setText("Save");
                                Alert alert = new Alert(Alert.AlertType.ERROR);
                                alert.setTitle("Error");
                                alert.setContentText("Cannot update: Staff ID is missing. Please try editing again.");
                                alert.show();
                            });
                            return;
                        }
                        staffData.setId(originalStaffId);
                        System.out.println("Updating staff with ID: " + staffData.getId());
                        success = staffDAO.update(staffData);
                        System.out.println("Update result: " + success);

                        if (!success) {
                            System.err.println("Update failed for staff ID: " + originalStaffId);
                            System.err.println("Staff data: name=" + staffData.getName() + ", designation="
                                    + staffData.getDesignation());
                        }
                    }
                    javafx.application.Platform.runLater(() -> {
                        saveButton.setDisable(false);
                        saveButton.setText("Save");
                        if (success) {
                            dialog.close();
                            // Only reload data after successful save
                            loadStaffData();
                            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                            successAlert.setTitle("Success");
                            successAlert.setHeaderText(null);
                            successAlert.setContentText(
                                    "Staff " + (staff == null ? "created" : "updated") + " successfully!");
                            successAlert.showAndWait();
                        } else {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Error");
                            alert.setHeaderText("Failed to " + (staff == null ? "create" : "update") + " staff");
                            alert.setContentText(
                                    "The staff record was not saved. Please check the console for error details and try again.\n\nMake sure all required fields are filled correctly.");
                            alert.show();
                            // Don't reload data on failure to avoid confusion
                        }
                    });
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        saveButton.setDisable(false);
                        saveButton.setText("Save");
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setContentText("Failed to save staff: " + ex.getMessage());
                        alert.show();
                        ex.printStackTrace();
                    });
                }
            }).start();
        });

        root.getChildren().addAll(
                titleLabel,
                FormStyler.createCompactFormField("Name *", nameField),
                FormStyler.createCompactFormField("Designation", designationField),
                FormStyler.createCompactFormField("Base Salary", salaryField),
                FormStyler.createCompactFormField("Mobile", mobileField),
                FormStyler.createCompactFormField("Email", emailField),
                FormStyler.createCompactFormField("Joining Date", joiningDatePicker),
                FormStyler.createCompactFormField("Address", addressField),
                FormStyler.createCompactFormField("Notes", notesField),
                buttonBox);

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPrefViewportWidth(640); // Increased width
        scrollPane.setPrefViewportHeight(600); // Increased height

        Scene scene = new Scene(scrollPane, 640, 600);
        dialog.setScene(scene);
        try {
            if (staffTable.getScene() != null && staffTable.getScene().getWindow() != null) {
                dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
                dialog.initOwner(staffTable.getScene().getWindow());
            }
        } catch (Exception ex) {
            // If we can't set the owner, continue without it
        }
        dialog.show();
    }

    private void deleteStaff(Staff staff) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setContentText("Are you sure you want to delete this staff member?");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                new Thread(() -> {
                    boolean success = staffDAO.delete(staff.getId());
                    javafx.application.Platform.runLater(() -> {
                        if (success) {
                            loadStaffData();
                        } else {
                            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                            errorAlert.setTitle("Error");
                            errorAlert.setContentText("Failed to delete staff.");
                            errorAlert.show();
                        }
                    });
                }).start();
            }
        });
    }

    private void showSalaryDialog(StaffSalary salary) {
        Stage dialog = new Stage();
        dialog.setTitle(salary == null ? "Add Salary Payment" : "Edit Salary Payment");

        // Store the original salary ID for editing
        final Long originalSalaryId = (salary != null) ? salary.getId() : null;

        VBox root = new VBox(8);
        root.setPadding(new Insets(10));
        root.setPrefWidth(500);
        root.setPrefHeight(450);
        FormStyler.styleFormDialog(root);

        Label titleLabel = FormStyler.createFormLabel("Salary Payment Details");
        titleLabel.setStyle(
                "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + StyleHelper.TEXT_GRAY_900 + ";");

        ComboBox<Staff> staffCombo = new ComboBox<>();
        staffCombo.setItems(staffList);
        staffCombo.setPromptText("Select Staff *");
        StyleHelper.styleComboBox(staffCombo);
        staffCombo.setCellFactory(param -> new ListCell<Staff>() {
            @Override
            protected void updateItem(Staff item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + (item.getDesignation() != null ? " (" + item.getDesignation() + ")" : ""));
                }
            }
        });
        staffCombo.setButtonCell(new ListCell<Staff>() {
            @Override
            protected void updateItem(Staff item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + (item.getDesignation() != null ? " (" + item.getDesignation() + ")" : ""));
                }
            }
        });
        TextField salaryField = new TextField();
        salaryField.setPromptText("Salary (Month/Period)");
        StyleHelper.styleTextField(salaryField);
        setMaxLength(salaryField, 20);
        if (salary != null && salary.getSalary() != null) {
            salaryField.setText(salary.getSalary().toString());
        }

        if (salary != null && salary.getStaffId() != null) {
            // Find and set the staff member
            staffList.stream()
                    .filter(s -> s.getId().equals(salary.getStaffId()))
                    .findFirst()
                    .ifPresent(staffCombo::setValue);

            // If it's a new payment (ID is null but staff is set), pre-fill salary
            if (salary.getId() == null && salary.getSalary() != null) {
                salaryField.setText(salary.getSalary().toString());
            }
        }

        DatePicker paidDatePicker = new DatePicker();
        StyleHelper.styleDatePicker(paidDatePicker);
        if (salary != null && salary.getPaidDate() != null) {
            paidDatePicker.setValue(salary.getPaidDate());
        } else {
            paidDatePicker.setValue(LocalDate.now());
        }

        TextField paidAmountField = new TextField();
        paidAmountField.setPromptText("Paid Amount *");
        StyleHelper.styleTextField(paidAmountField);
        setMaxLength(paidAmountField, 20);
        if (salary != null && salary.getPaidAmount() != null) {
            paidAmountField.setText(salary.getPaidAmount().toString());
        }

        ComboBox<String> paymentModeCombo = new ComboBox<>();
        paymentModeCombo.getItems().addAll("CASH", "ONLINE", "CHEQUE");
        StyleHelper.styleComboBox(paymentModeCombo);
        if (salary != null && salary.getPaymentMode() != null) {
            paymentModeCombo.setValue(salary.getPaymentMode());
        } else {
            paymentModeCombo.setValue("CASH");
        }

        TextArea remarksField = new TextArea();
        remarksField.setPromptText("Remarks");
        remarksField.setPrefRowCount(2);
        remarksField.setPrefHeight(60);
        StyleHelper.styleTextArea(remarksField);
        setMaxLength(remarksField, 500);
        if (salary != null && salary.getRemarks() != null) {
            remarksField.setText(salary.getRemarks());
        }

        Button saveButton = new Button("Save");
        saveButton.setStyle(StyleHelper.getEditButtonStyle());

        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle(StyleHelper.getSecondaryButtonStyle());
        cancelButton.setOnAction(e -> dialog.close());

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(8, 0, 0, 0));
        buttonBox.getChildren().addAll(cancelButton, saveButton);

        saveButton.setOnAction(e -> {
            // Validate required fields
            if (staffCombo.getValue() == null) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Validation Error");
                alert.setContentText("Please select a staff member.");
                alert.show();
                staffCombo.requestFocus();
                return;
            }

            String paidAmountText = paidAmountField.getText().trim();
            if (paidAmountText.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Validation Error");
                alert.setContentText("Paid Amount is required. Please enter the paid amount.");
                alert.show();
                paidAmountField.requestFocus();
                return;
            }

            BigDecimal paidAmount;
            try {
                paidAmount = new BigDecimal(paidAmountText);
                if (paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Validation Error");
                    alert.setContentText("Paid Amount must be greater than zero.");
                    alert.show();
                    paidAmountField.requestFocus();
                    return;
                }
            } catch (NumberFormatException ex) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Validation Error");
                alert.setContentText("Invalid paid amount format. Please enter a valid number.");
                alert.show();
                paidAmountField.requestFocus();
                return;
            }

            LocalDate paidDate = paidDatePicker.getValue();
            if (paidDate == null) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Validation Error");
                alert.setContentText("Please select a paid date.");
                alert.show();
                paidDatePicker.requestFocus();
                return;
            }

            saveButton.setDisable(true);
            saveButton.setText("Saving...");

            StaffSalary salaryData = new StaffSalary();
            salaryData.setStaffId(staffCombo.getValue().getId());

            try {
                if (!salaryField.getText().trim().isEmpty()) {
                    salaryData.setSalary(new BigDecimal(salaryField.getText().trim()));
                } else if (staffCombo.getValue().getSalary() != null) {
                    salaryData.setSalary(staffCombo.getValue().getSalary());
                }
            } catch (NumberFormatException ex) {
                javafx.application.Platform.runLater(() -> {
                    saveButton.setDisable(false);
                    saveButton.setText("Save");
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Validation Error");
                    alert.setContentText("Invalid salary format. Please enter a valid number.");
                    alert.show();
                    salaryField.requestFocus();
                });
                return;
            }

            salaryData.setPaidDate(paidDate);
            salaryData.setPaidAmount(paidAmount);
            salaryData.setPaymentMode(paymentModeCombo.getValue() != null ? paymentModeCombo.getValue() : "CASH");
            salaryData.setRemarks(remarksField.getText().trim());

            // Calculate balance
            BigDecimal salaryAmount = salaryData.getSalary() != null ? salaryData.getSalary() : BigDecimal.ZERO;
            salaryData.setBalance(salaryAmount.subtract(paidAmount));

            new Thread(() -> {
                try {
                    boolean success;
                    if (salary == null) {
                        Long newId = salaryDAO.create(salaryData);
                        success = newId != null;
                    } else {
                        if (originalSalaryId == null) {
                            javafx.application.Platform.runLater(() -> {
                                saveButton.setDisable(false);
                                saveButton.setText("Save");
                                Alert alert = new Alert(Alert.AlertType.ERROR);
                                alert.setTitle("Error");
                                alert.setContentText("Cannot update: Salary ID is missing. Please try editing again.");
                                alert.show();
                            });
                            return;
                        }
                        salaryData.setId(originalSalaryId);
                        success = salaryDAO.update(salaryData);
                    }
                    javafx.application.Platform.runLater(() -> {
                        saveButton.setDisable(false);
                        saveButton.setText("Save");
                        if (success) {
                            dialog.close();
                            loadSalaryData();
                            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                            successAlert.setTitle("Success");
                            successAlert.setHeaderText(null);
                            successAlert.setContentText(
                                    "Salary payment " + (salary == null ? "added" : "updated") + " successfully!");
                            successAlert.showAndWait();
                        } else {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Error");
                            alert.setHeaderText("Failed to " + (salary == null ? "add" : "update") + " salary payment");
                            alert.setContentText(
                                    "The salary payment could not be saved. Please check the console for error details and try again.");
                            alert.show();
                        }
                    });
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        saveButton.setDisable(false);
                        saveButton.setText("Save");
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setContentText("Failed to save salary payment: " + ex.getMessage());
                        alert.show();
                        ex.printStackTrace();
                    });
                }
            }).start();
        });

        root.getChildren().addAll(
                titleLabel,
                FormStyler.createCompactFormField("Staff *", staffCombo),
                FormStyler.createCompactFormField("Salary (Month/Period)", salaryField),
                FormStyler.createCompactFormField("Paid Date *", paidDatePicker),
                FormStyler.createCompactFormField("Paid Amount *", paidAmountField),
                FormStyler.createCompactFormField("Payment Mode", paymentModeCombo),
                FormStyler.createCompactFormField("Remarks", remarksField),
                buttonBox);

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPrefViewportWidth(520);
        scrollPane.setPrefViewportHeight(500);

        Scene scene = new Scene(scrollPane, 520, 500);
        dialog.setScene(scene);
        try {
            if (salaryTable.getScene() != null && salaryTable.getScene().getWindow() != null) {
                dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
                dialog.initOwner(salaryTable.getScene().getWindow());
            }
        } catch (Exception ex) {
            // If we can't set the owner, continue without it
        }
        dialog.show();
    }

    private void deleteSalary(StaffSalary salary) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setContentText("Are you sure you want to delete this salary payment record?");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                new Thread(() -> {
                    boolean success = salaryDAO.delete(salary.getId());
                    javafx.application.Platform.runLater(() -> {
                        if (success) {
                            loadSalaryData();
                            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                            successAlert.setTitle("Success");
                            successAlert.setHeaderText(null);
                            successAlert.setContentText("Salary payment deleted successfully.");
                            successAlert.show();
                        } else {
                            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                            errorAlert.setTitle("Error");
                            errorAlert.setContentText("Failed to delete salary payment.");
                            errorAlert.show();
                        }
                    });
                }).start();
            }
        });
    }

    private void handleExportPDF() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Salary Report");
        fileChooser.setInitialFileName("Salary_Report_" + LocalDate.now() + ".pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        File file = fileChooser.showSaveDialog(view.getScene().getWindow());
        if (file != null) {
            new Thread(() -> {
                try {
                    // Extract data currently in the table
                    java.util.List<StaffSalary> currentData = new java.util.ArrayList<>(salaryTable.getItems());

                    LocalDate from = reportFromDate.getValue();
                    LocalDate to = reportToDate.getValue();
                    Staff selectedStaff = reportStaffFilter.getValue();
                    String staffName = selectedStaff != null ? selectedStaff.getName() : "All Staff";

                    SalaryReportPDFService.generateSalaryReport(currentData, from, to, staffName, file);

                    javafx.application.Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Success");
                        alert.setHeaderText("Report Exported");
                        alert.setContentText("Salary report has been saved to: " + file.getAbsolutePath());
                        alert.show();
                    });
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setHeaderText("Export Failed");
                        alert.setContentText("Failed to export PDF: " + ex.getMessage());
                        alert.show();
                        ex.printStackTrace();
                    });
                }
            }).start();
        }
    }
}
