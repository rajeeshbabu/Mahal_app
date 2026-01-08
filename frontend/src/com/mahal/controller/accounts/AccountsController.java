package com.mahal.controller.accounts;

import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import com.mahal.database.*;
import com.mahal.model.*;
import com.mahal.util.StyleHelper;
import com.mahal.util.TableStyler;
import com.mahal.util.FormStyler;
import com.mahal.util.FormatUtil;
import javafx.geometry.Pos;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.PropertyValueFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class AccountsController {
    private VBox view;
    private StackPane contentPane;
    private VBox incomeTypeViewPane;
    private VBox incomeTypeListViewPane;
    private VBox dueTypeViewPane;
    private VBox collectionViewPane;
    private VBox expenseViewPane;
    private VBox unpaidListViewPane;
    private VBox dueReportViewPane;
    private VBox expenseReportViewPane;

    // DAOs
    private IncomeTypeDAO incomeTypeDAO;
    private IncomeDAO incomeDAO;
    private DueTypeDAO dueTypeDAO;
    private DueCollectionDAO dueCollectionDAO;
    private ExpenseDAO expenseDAO;
    private MasjidDAO masjidDAO;
    private MemberDAO memberDAO;

    // Data lists
    private ObservableList<IncomeType> incomeTypeList;
    private ObservableList<Income> incomeList;
    private ObservableList<DueType> dueTypeList;
    private ObservableList<DueCollection> dueCollectionList;
    private ObservableList<Expense> expenseList;
    private ObservableList<Masjid> masjidList;
    private ObservableList<Member> memberList;
    private ObservableList<java.util.Map<String, Object>> unpaidList;

    // Tables
    private TableView<IncomeType> incomeTypeTable;
    private TableView<Income> incomeTable;
    private TableView<DueType> dueTypeTable;
    private TableView<DueCollection> dueCollectionTable;
    private TableView<Expense> expenseTable;
    private TableView<java.util.Map<String, Object>> unpaidTable;

    // Summary labels for reports
    private Label dueTotalLabel;
    private Label dueCountLabel;
    private Label expenseTotalLabel;
    private Label expenseCountLabel;

    // Filter references for unpaid list
    private ComboBox<String> unpaidAddressFilter;
    private ComboBox<String> dueReportAddressFilter;
    private ComboBox<DueType> unpaidDueTypeFilter;
    private ComboBox<String> unpaidStatusFilter;

    public AccountsController() {
        this.incomeTypeDAO = new IncomeTypeDAO();
        this.incomeDAO = new IncomeDAO();
        this.dueTypeDAO = new DueTypeDAO();
        this.dueCollectionDAO = new DueCollectionDAO();
        this.expenseDAO = new ExpenseDAO();
        this.masjidDAO = new MasjidDAO();
        this.memberDAO = new MemberDAO();

        this.incomeTypeList = FXCollections.observableArrayList();
        this.incomeList = FXCollections.observableArrayList();
        this.dueTypeList = FXCollections.observableArrayList();
        this.dueCollectionList = FXCollections.observableArrayList();
        this.expenseList = FXCollections.observableArrayList();
        this.masjidList = FXCollections.observableArrayList();
        this.memberList = FXCollections.observableArrayList();
        this.unpaidList = FXCollections.observableArrayList();

        createView();
        loadAllData();

        // Subscribe to sync events
        com.mahal.util.EventBus.getInstance().subscribe("income_types",
                e -> javafx.application.Platform.runLater(this::loadIncomeTypes));
        com.mahal.util.EventBus.getInstance().subscribe("incomes",
                e -> javafx.application.Platform.runLater(this::loadIncomes));
        com.mahal.util.EventBus.getInstance().subscribe("due_types",
                e -> javafx.application.Platform.runLater(this::loadDueTypes));
        com.mahal.util.EventBus.getInstance().subscribe("due_collections",
                e -> javafx.application.Platform.runLater(this::loadCollections));
        com.mahal.util.EventBus.getInstance().subscribe("expenses",
                e -> javafx.application.Platform.runLater(this::loadExpenses));
        com.mahal.util.EventBus.getInstance().subscribe("members",
                e -> javafx.application.Platform.runLater(this::loadMembers));
        com.mahal.util.EventBus.getInstance().subscribe("masjids",
                e -> javafx.application.Platform.runLater(this::loadMasjids));
    }

    public VBox getView() {
        return view;
    }

    private void createView() {
        view = new VBox(20);
        view.setPadding(new Insets(24));
        view.setStyle("-fx-background-color: transparent;");

        // Header Section
        VBox header = new VBox(4);
        Label titleLabel = new Label("Account Management");
        titleLabel.setStyle(StyleHelper.getTitleStyle());
        Label subtitleLabel = new Label("Manage income types, expenses, dues and financial reports");
        subtitleLabel.setStyle(StyleHelper.getSubtitleStyle());
        header.getChildren().addAll(titleLabel, subtitleLabel);

        // Navigation Pill Switcher
        HBox pillContainer = new HBox(10);
        pillContainer.setAlignment(Pos.CENTER_LEFT);
        pillContainer.setPadding(new Insets(10, 0, 10, 0));

        HBox switcher = new HBox(0);
        switcher.setStyle(StyleHelper.getPillSwitcherContainerStyle());

        Button incomeBtn = new Button("Income Types");
        Button incomeListBtn = new Button("Income List");
        Button dueTypeBtn = new Button("Due Types");
        Button collectionBtn = new Button("Collections");
        Button expenseBtn = new Button("Expenses");
        Button unpaidBtn = new Button("Unpaid");
        Button dueReportBtn = new Button("Due Report");
        Button expenseReportBtn = new Button("Expense Report");

        incomeBtn.setStyle(StyleHelper.getPillButtonStyle(true));
        incomeListBtn.setStyle(StyleHelper.getPillButtonStyle(false));
        dueTypeBtn.setStyle(StyleHelper.getPillButtonStyle(false));
        collectionBtn.setStyle(StyleHelper.getPillButtonStyle(false));
        expenseBtn.setStyle(StyleHelper.getPillButtonStyle(false));
        unpaidBtn.setStyle(StyleHelper.getPillButtonStyle(false));
        dueReportBtn.setStyle(StyleHelper.getPillButtonStyle(false));
        expenseReportBtn.setStyle(StyleHelper.getPillButtonStyle(false));

        switcher.getChildren().addAll(
                incomeBtn, incomeListBtn, dueTypeBtn, collectionBtn,
                expenseBtn, unpaidBtn, dueReportBtn, expenseReportBtn);
        pillContainer.getChildren().add(switcher);

        contentPane = new StackPane();
        VBox.setVgrow(contentPane, Priority.ALWAYS);

        // Initialize all sub-views
        incomeTypeViewPane = createIncomeTypeView();
        incomeTypeListViewPane = createIncomeTypeListView();
        dueTypeViewPane = createDueTypeView();
        collectionViewPane = createCollectionView();
        expenseViewPane = createExpenseView();
        unpaidListViewPane = createUnpaidListView();
        dueReportViewPane = createDueReportView();
        expenseReportViewPane = createExpenseReportView();
        contentPane.getChildren().setAll(incomeTypeViewPane);

        incomeBtn.setOnAction(e -> {
            incomeBtn.setStyle(StyleHelper.getPillButtonStyle(true));
            incomeListBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            dueTypeBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            collectionBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            expenseBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            unpaidBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            dueReportBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            expenseReportBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            contentPane.getChildren().setAll(incomeTypeViewPane);
        });

        incomeListBtn.setOnAction(e -> {
            incomeBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            incomeListBtn.setStyle(StyleHelper.getPillButtonStyle(true));
            dueTypeBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            collectionBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            expenseBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            unpaidBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            dueReportBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            expenseReportBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            contentPane.getChildren().setAll(incomeTypeListViewPane);
        });

        dueTypeBtn.setOnAction(e -> {
            incomeBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            incomeListBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            dueTypeBtn.setStyle(StyleHelper.getPillButtonStyle(true));
            collectionBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            expenseBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            unpaidBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            dueReportBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            expenseReportBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            contentPane.getChildren().setAll(dueTypeViewPane);
        });

        collectionBtn.setOnAction(e -> {
            incomeBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            incomeListBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            dueTypeBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            collectionBtn.setStyle(StyleHelper.getPillButtonStyle(true));
            expenseBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            unpaidBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            dueReportBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            expenseReportBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            contentPane.getChildren().setAll(collectionViewPane);
        });

        expenseBtn.setOnAction(e -> {
            incomeBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            incomeListBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            dueTypeBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            collectionBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            expenseBtn.setStyle(StyleHelper.getPillButtonStyle(true));
            unpaidBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            dueReportBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            expenseReportBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            contentPane.getChildren().setAll(expenseViewPane);
        });

        unpaidBtn.setOnAction(e -> {
            incomeBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            incomeListBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            dueTypeBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            collectionBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            expenseBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            unpaidBtn.setStyle(StyleHelper.getPillButtonStyle(true));
            dueReportBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            expenseReportBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            contentPane.getChildren().setAll(unpaidListViewPane);
        });

        dueReportBtn.setOnAction(e -> {
            incomeBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            incomeListBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            dueTypeBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            collectionBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            expenseBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            unpaidBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            dueReportBtn.setStyle(StyleHelper.getPillButtonStyle(true));
            expenseReportBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            loadCollections();
            updateDueReportSummary();
            contentPane.getChildren().setAll(dueReportViewPane);
        });

        expenseReportBtn.setOnAction(e -> {
            incomeBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            incomeListBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            dueTypeBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            collectionBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            expenseBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            unpaidBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            dueReportBtn.setStyle(StyleHelper.getPillButtonStyle(false));
            expenseReportBtn.setStyle(StyleHelper.getPillButtonStyle(true));
            loadExpenses();
            updateExpenseReportSummary();
            contentPane.getChildren().setAll(expenseReportViewPane);
        });

        // Wrap everything in a card
        VBox mainCard = new VBox(20);
        mainCard.setStyle(StyleHelper.getCardStyle());
        mainCard.getChildren().addAll(pillContainer, contentPane);
        VBox.setVgrow(mainCard, Priority.ALWAYS);

        view.getChildren().addAll(header, mainCard);
    }

    // Removed styleSwitcherButton - now using StyleHelper.getPillButtonStyle()

    private VBox createIncomeTypeView() {
        VBox view = new VBox(20);
        view.setPadding(new Insets(0));

        // Modern Action Row
        HBox actionRow = new HBox(15);
        actionRow.setAlignment(Pos.CENTER_LEFT);
        actionRow.setPadding(new Insets(0, 0, 10, 0));

        // Search Field
        TextField searchField = new TextField();
        searchField.setPromptText("🔍 Search income types...");
        searchField.setPrefWidth(300);
        StyleHelper.styleTextField(searchField);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Add Income Type Action Pill
        Button addButton = new Button("➕ Add Income Type");
        addButton.setStyle(StyleHelper.getActionPillButtonStyle());
        addButton.setOnAction(e -> showIncomeTypeDialog(null));

        actionRow.getChildren().addAll(searchField, spacer, addButton);

        // Table
        incomeTypeTable = new TableView<>();
        TableStyler.applyModernStyling(incomeTypeTable);
        VBox.setVgrow(incomeTypeTable, Priority.ALWAYS);

        TableColumn<IncomeType, String> nameCol = new TableColumn<>("NAME");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableStyler.styleTableColumn(nameCol);

        TableColumn<IncomeType, String> amountCol = new TableColumn<>("DEFAULT AMOUNT");
        amountCol.setCellValueFactory(cell -> {
            BigDecimal amount = cell.getValue().getDefaultAmount();
            return new javafx.beans.property.SimpleStringProperty(
                    amount != null ? FormatUtil.formatCurrency(amount) : "-");
        });
        TableStyler.styleTableColumn(amountCol);

        TableColumn<IncomeType, String> descCol = new TableColumn<>("DESCRIPTION");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        TableStyler.styleTableColumn(descCol);

        TableColumn<IncomeType, String> actionsCol = new TableColumn<>("ACTIONS");
        actionsCol.setCellFactory(param -> new TableCell<IncomeType, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    IncomeType incomeType = getTableView().getItems().get(getIndex());

                    Hyperlink editLink = new Hyperlink("Edit");
                    editLink.setStyle("-fx-text-fill: #2563eb; -fx-font-weight: 600; -fx-underline: false;");
                    editLink.setOnAction(e -> showIncomeTypeDialog(incomeType));

                    Hyperlink deleteLink = new Hyperlink("Delete");
                    deleteLink.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: 600; -fx-underline: false;");
                    deleteLink.setOnAction(e -> deleteIncomeType(incomeType));

                    HBox box = new HBox(12);
                    box.setAlignment(Pos.CENTER);
                    box.getChildren().addAll(editLink, deleteLink);
                    setGraphic(box);
                }
            }
        });
        TableStyler.styleTableColumn(actionsCol);

        incomeTypeTable.getColumns().addAll(nameCol, amountCol, descCol, actionsCol);

        // Real-time Search
        FilteredList<IncomeType> filteredData = new FilteredList<>(incomeTypeList, p -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(incomeType -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                if (incomeType.getName() != null && incomeType.getName().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                if (incomeType.getDescription() != null
                        && incomeType.getDescription().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                return false;
            });
        });
        SortedList<IncomeType> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(incomeTypeTable.comparatorProperty());
        incomeTypeTable.setItems(sortedData);

        view.getChildren().addAll(actionRow, incomeTypeTable);
        return view;
    }

    private VBox createIncomeTypeListView() {
        VBox view = new VBox(20);
        view.setPadding(new Insets(0));

        // Modern Action Row
        HBox actionRow = new HBox(15);
        actionRow.setAlignment(Pos.CENTER_LEFT);
        actionRow.setPadding(new Insets(0, 0, 10, 0));

        TextField searchField = new TextField();
        searchField.setPromptText("🔍 Search income records...");
        searchField.setPrefWidth(300);
        StyleHelper.styleTextField(searchField);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addButton = new Button("➕ Add Income");
        addButton.setStyle(StyleHelper.getActionPillButtonStyle());
        addButton.setOnAction(e -> showIncomeDialog(null));

        actionRow.getChildren().addAll(searchField, spacer, addButton);

        incomeTable = new TableView<>();
        TableStyler.applyModernStyling(incomeTable);
        VBox.setVgrow(incomeTable, Priority.ALWAYS);

        TableColumn<Income, String> dateCol = new TableColumn<>("DATE");
        dateCol.setCellValueFactory(cell -> {
            LocalDate date = cell.getValue().getDate();
            return new javafx.beans.property.SimpleStringProperty(
                    date != null ? date.format(DateTimeFormatter.ISO_LOCAL_DATE) : "-");
        });
        TableStyler.styleTableColumn(dateCol);

        TableColumn<Income, String> incomeTypeCol = new TableColumn<>("INCOME TYPE");
        incomeTypeCol.setCellValueFactory(new PropertyValueFactory<>("incomeTypeName"));
        TableStyler.styleTableColumn(incomeTypeCol);

        TableColumn<Income, String> masjidCol = new TableColumn<>("MASJID");
        masjidCol.setCellValueFactory(new PropertyValueFactory<>("masjidName"));
        TableStyler.styleTableColumn(masjidCol);

        TableColumn<Income, String> memberCol = new TableColumn<>("MEMBER");
        memberCol.setCellValueFactory(new PropertyValueFactory<>("memberName"));
        TableStyler.styleTableColumn(memberCol);

        TableColumn<Income, String> amountCol = new TableColumn<>("AMOUNT");
        amountCol.setCellValueFactory(cell -> {
            BigDecimal amount = cell.getValue().getAmount();
            return new javafx.beans.property.SimpleStringProperty(
                    amount != null ? FormatUtil.formatCurrency(amount) : "-");
        });
        TableStyler.styleTableColumn(amountCol);

        TableColumn<Income, String> paymentModeCol = new TableColumn<>("PAYMENT MODE");
        paymentModeCol.setCellValueFactory(new PropertyValueFactory<>("paymentMode"));
        TableStyler.styleTableColumn(paymentModeCol);

        TableColumn<Income, String> receiptNoCol = new TableColumn<>("RECEIPT NO");
        receiptNoCol.setCellValueFactory(new PropertyValueFactory<>("receiptNo"));
        TableStyler.styleTableColumn(receiptNoCol);

        TableColumn<Income, String> remarksCol = new TableColumn<>("REMARKS");
        remarksCol.setCellValueFactory(new PropertyValueFactory<>("remarks"));
        TableStyler.styleTableColumn(remarksCol);

        TableColumn<Income, String> actionsCol = new TableColumn<>("ACTIONS");
        actionsCol.setCellFactory(param -> new TableCell<Income, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Income income = getTableView().getItems().get(getIndex());

                    Hyperlink editLink = new Hyperlink("Edit");
                    editLink.setStyle("-fx-text-fill: #2563eb; -fx-font-weight: 600; -fx-underline: false;");
                    editLink.setOnAction(e -> showIncomeDialog(income));

                    Hyperlink deleteLink = new Hyperlink("Delete");
                    deleteLink.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: 600; -fx-underline: false;");
                    deleteLink.setOnAction(e -> deleteIncome(income));

                    HBox box = new HBox(12);
                    box.setAlignment(Pos.CENTER);
                    box.getChildren().addAll(editLink, deleteLink);
                    setGraphic(box);
                }
            }
        });
        TableStyler.styleTableColumn(actionsCol);

        incomeTable.getColumns().addAll(dateCol, incomeTypeCol, masjidCol, memberCol,
                amountCol, paymentModeCol, receiptNoCol, remarksCol, actionsCol);

        // Real-time Search
        FilteredList<Income> filteredData = new FilteredList<>(incomeList, p -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(income -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                if (income.getIncomeTypeName() != null
                        && income.getIncomeTypeName().toLowerCase().contains(lowerCaseFilter))
                    return true;
                if (income.getMasjidName() != null && income.getMasjidName().toLowerCase().contains(lowerCaseFilter))
                    return true;
                if (income.getMemberName() != null && income.getMemberName().toLowerCase().contains(lowerCaseFilter))
                    return true;
                if (income.getPaymentMode() != null && income.getPaymentMode().toLowerCase().contains(lowerCaseFilter))
                    return true;
                if (income.getReceiptNo() != null && income.getReceiptNo().toLowerCase().contains(lowerCaseFilter))
                    return true;
                return false;
            });
        });
        SortedList<Income> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(incomeTable.comparatorProperty());
        incomeTable.setItems(sortedData);

        view.getChildren().addAll(actionRow, incomeTable);
        return view;
    }

    private VBox createDueTypeView() {
        VBox view = new VBox(20);
        view.setPadding(new Insets(0));

        // Modern Action Row
        HBox actionRow = new HBox(15);
        actionRow.setAlignment(Pos.CENTER_LEFT);
        actionRow.setPadding(new Insets(0, 0, 10, 0));

        TextField searchField = new TextField();
        searchField.setPromptText("🔍 Search due types...");
        searchField.setPrefWidth(300);
        StyleHelper.styleTextField(searchField);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addButton = new Button("➕ Add Due Type");
        addButton.setStyle(StyleHelper.getActionPillButtonStyle());
        addButton.setOnAction(e -> showDueTypeDialog(null));

        actionRow.getChildren().addAll(searchField, spacer, addButton);

        dueTypeTable = new TableView<>();
        TableStyler.applyModernStyling(dueTypeTable);
        VBox.setVgrow(dueTypeTable, Priority.ALWAYS);

        TableColumn<DueType, String> nameCol = new TableColumn<>("DUE NAME");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("dueName"));
        TableStyler.styleTableColumn(nameCol);

        TableColumn<DueType, String> frequencyCol = new TableColumn<>("FREQUENCY");
        frequencyCol.setCellValueFactory(new PropertyValueFactory<>("frequency"));
        TableStyler.styleTableColumn(frequencyCol);

        TableColumn<DueType, String> amountCol = new TableColumn<>("AMOUNT");
        amountCol.setCellValueFactory(cell -> {
            BigDecimal amount = cell.getValue().getAmount();
            return new javafx.beans.property.SimpleStringProperty(
                    amount != null ? FormatUtil.formatCurrency(amount) : "-");
        });
        TableStyler.styleTableColumn(amountCol);

        TableColumn<DueType, String> descCol = new TableColumn<>("DESCRIPTION");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        TableStyler.styleTableColumn(descCol);

        TableColumn<DueType, String> actionsCol = new TableColumn<>("ACTIONS");
        actionsCol.setCellFactory(param -> new TableCell<DueType, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    DueType dueType = getTableView().getItems().get(getIndex());

                    Hyperlink editLink = new Hyperlink("Edit");
                    editLink.setStyle("-fx-text-fill: #2563eb; -fx-font-weight: 600; -fx-underline: false;");
                    editLink.setOnAction(e -> showDueTypeDialog(dueType));

                    Hyperlink deleteLink = new Hyperlink("Delete");
                    deleteLink.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: 600; -fx-underline: false;");
                    deleteLink.setOnAction(e -> deleteDueType(dueType));

                    HBox box = new HBox(12);
                    box.setAlignment(Pos.CENTER);
                    box.getChildren().addAll(editLink, deleteLink);
                    setGraphic(box);
                }
            }
        });
        TableStyler.styleTableColumn(actionsCol);

        dueTypeTable.getColumns().addAll(nameCol, frequencyCol, amountCol, descCol, actionsCol);

        // Real-time Search
        FilteredList<DueType> filteredData = new FilteredList<>(dueTypeList, p -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(dueType -> {
                if (newValue == null || newValue.isEmpty())
                    return true;
                String lowerCaseFilter = newValue.toLowerCase();
                if (dueType.getDueName() != null && dueType.getDueName().toLowerCase().contains(lowerCaseFilter))
                    return true;
                if (dueType.getFrequency() != null && dueType.getFrequency().toLowerCase().contains(lowerCaseFilter))
                    return true;
                if (dueType.getDescription() != null
                        && dueType.getDescription().toLowerCase().contains(lowerCaseFilter))
                    return true;
                return false;
            });
        });
        SortedList<DueType> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(dueTypeTable.comparatorProperty());
        dueTypeTable.setItems(sortedData);

        view.getChildren().addAll(actionRow, dueTypeTable);
        return view;
    }

    private VBox createCollectionView() {
        VBox view = new VBox(20);
        view.setPadding(new Insets(0));

        // Modern Action Row
        HBox actionRow = new HBox(15);
        actionRow.setAlignment(Pos.CENTER_LEFT);
        actionRow.setPadding(new Insets(0, 0, 10, 0));

        TextField searchField = new TextField();
        searchField.setPromptText("🔍 Search collections...");
        searchField.setPrefWidth(300);
        StyleHelper.styleTextField(searchField);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addButton = new Button("➕ Add Collection");
        addButton.setStyle(StyleHelper.getActionPillButtonStyle());
        addButton.setOnAction(e -> showCollectionDialog(null));

        actionRow.getChildren().addAll(searchField, spacer, addButton);

        dueCollectionTable = new TableView<>();
        TableStyler.applyModernStyling(dueCollectionTable);
        VBox.setVgrow(dueCollectionTable, Priority.ALWAYS);

        TableColumn<DueCollection, String> dateCol = new TableColumn<>("DATE");
        dateCol.setCellValueFactory(cell -> {
            LocalDate date = cell.getValue().getDate();
            return new javafx.beans.property.SimpleStringProperty(
                    date != null ? date.format(DateTimeFormatter.ISO_LOCAL_DATE) : "-");
        });
        TableStyler.styleTableColumn(dateCol);

        TableColumn<DueCollection, String> typeCol = new TableColumn<>("DUE TYPE");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("dueTypeName"));
        TableStyler.styleTableColumn(typeCol);

        TableColumn<DueCollection, String> addressCol = new TableColumn<>("ADDRESS");
        addressCol.setCellValueFactory(new PropertyValueFactory<>("address"));
        TableStyler.styleTableColumn(addressCol);

        TableColumn<DueCollection, String> amountCol = new TableColumn<>("AMOUNT");
        amountCol.setCellValueFactory(cell -> {
            BigDecimal amount = cell.getValue().getAmount();
            return new javafx.beans.property.SimpleStringProperty(
                    amount != null ? FormatUtil.formatCurrency(amount) : "-");
        });
        TableStyler.styleTableColumn(amountCol);

        TableColumn<DueCollection, String> modeCol = new TableColumn<>("PAYMENT MODE");
        modeCol.setCellValueFactory(new PropertyValueFactory<>("paymentMode"));
        TableStyler.styleTableColumn(modeCol);

        TableColumn<DueCollection, String> actionsCol = new TableColumn<>("ACTIONS");
        actionsCol.setCellFactory(param -> new TableCell<DueCollection, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    DueCollection collection = getTableView().getItems().get(getIndex());

                    Hyperlink editLink = new Hyperlink("Edit");
                    editLink.setStyle("-fx-text-fill: #2563eb; -fx-font-weight: 600; -fx-underline: false;");
                    editLink.setOnAction(e -> showCollectionDialog(collection));

                    Hyperlink deleteLink = new Hyperlink("Delete");
                    deleteLink.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: 600; -fx-underline: false;");
                    deleteLink.setOnAction(e -> deleteCollection(collection));

                    HBox box = new HBox(12);
                    box.setAlignment(Pos.CENTER);
                    box.getChildren().addAll(editLink, deleteLink);
                    setGraphic(box);
                }
            }
        });
        TableStyler.styleTableColumn(actionsCol);

        dueCollectionTable.getColumns().addAll(dateCol, typeCol, addressCol, amountCol, modeCol, actionsCol);

        // Real-time Search
        FilteredList<DueCollection> filteredData = new FilteredList<>(dueCollectionList, p -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(collection -> {
                if (newValue == null || newValue.isEmpty())
                    return true;
                String lowerCaseFilter = newValue.toLowerCase();
                if (collection.getDueTypeName() != null
                        && collection.getDueTypeName().toLowerCase().contains(lowerCaseFilter))
                    return true;
                if (collection.getAddress() != null && collection.getAddress().toLowerCase().contains(lowerCaseFilter))
                    return true;
                if (collection.getPaymentMode() != null
                        && collection.getPaymentMode().toLowerCase().contains(lowerCaseFilter))
                    return true;
                return false;
            });
        });
        SortedList<DueCollection> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(dueCollectionTable.comparatorProperty());
        dueCollectionTable.setItems(sortedData);

        view.getChildren().addAll(actionRow, dueCollectionTable);
        return view;
    }

    private VBox createExpenseView() {
        VBox view = new VBox(20);
        view.setPadding(new Insets(0));

        // Modern Action Row
        HBox actionRow = new HBox(15);
        actionRow.setAlignment(Pos.CENTER_LEFT);
        actionRow.setPadding(new Insets(0, 0, 10, 0));

        TextField searchField = new TextField();
        searchField.setPromptText("🔍 Search expenses...");
        searchField.setPrefWidth(300);
        StyleHelper.styleTextField(searchField);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addButton = new Button("➕ Add Expense");
        addButton.setStyle(StyleHelper.getActionPillButtonStyle());
        addButton.setOnAction(e -> showExpenseDialog(null));

        actionRow.getChildren().addAll(searchField, spacer, addButton);

        expenseTable = new TableView<>();
        TableStyler.applyModernStyling(expenseTable);
        VBox.setVgrow(expenseTable, Priority.ALWAYS);

        TableColumn<Expense, String> dateCol = new TableColumn<>("DATE");
        dateCol.setCellValueFactory(cell -> {
            LocalDate date = cell.getValue().getDate();
            return new javafx.beans.property.SimpleStringProperty(
                    date != null ? date.format(DateTimeFormatter.ISO_LOCAL_DATE) : "-");
        });
        TableStyler.styleTableColumn(dateCol);

        TableColumn<Expense, String> typeCol = new TableColumn<>("EXPENSE TYPE");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("expenseType"));
        TableStyler.styleTableColumn(typeCol);

        TableColumn<Expense, String> masjidCol = new TableColumn<>("MASJID");
        masjidCol.setCellValueFactory(new PropertyValueFactory<>("masjidName"));
        TableStyler.styleTableColumn(masjidCol);

        TableColumn<Expense, String> amountCol = new TableColumn<>("AMOUNT");
        amountCol.setCellValueFactory(cell -> {
            BigDecimal amount = cell.getValue().getAmount();
            return new javafx.beans.property.SimpleStringProperty(
                    amount != null ? FormatUtil.formatCurrency(amount) : "-");
        });
        TableStyler.styleTableColumn(amountCol);

        TableColumn<Expense, String> notesCol = new TableColumn<>("NOTES");
        notesCol.setCellValueFactory(new PropertyValueFactory<>("notes"));
        TableStyler.styleTableColumn(notesCol);

        TableColumn<Expense, String> actionsCol = new TableColumn<>("ACTIONS");
        actionsCol.setCellFactory(param -> new TableCell<Expense, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Expense expense = getTableView().getItems().get(getIndex());

                    Hyperlink editLink = new Hyperlink("Edit");
                    editLink.setStyle("-fx-text-fill: #2563eb; -fx-font-weight: 600; -fx-underline: false;");
                    editLink.setOnAction(e -> showExpenseDialog(expense));

                    Hyperlink deleteLink = new Hyperlink("Delete");
                    deleteLink.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: 600; -fx-underline: false;");
                    deleteLink.setOnAction(e -> deleteExpense(expense));

                    HBox box = new HBox(12);
                    box.setAlignment(Pos.CENTER);
                    box.getChildren().addAll(editLink, deleteLink);
                    setGraphic(box);
                }
            }
        });
        TableStyler.styleTableColumn(actionsCol);

        expenseTable.getColumns().addAll(dateCol, typeCol, masjidCol, amountCol, notesCol, actionsCol);

        // Real-time Search
        FilteredList<Expense> filteredData = new FilteredList<>(expenseList, p -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(expense -> {
                if (newValue == null || newValue.isEmpty())
                    return true;
                String lowerCaseFilter = newValue.toLowerCase();
                if (expense.getExpenseType() != null
                        && expense.getExpenseType().toLowerCase().contains(lowerCaseFilter))
                    return true;
                if (expense.getMasjidName() != null && expense.getMasjidName().toLowerCase().contains(lowerCaseFilter))
                    return true;
                if (expense.getNotes() != null && expense.getNotes().toLowerCase().contains(lowerCaseFilter))
                    return true;
                return false;
            });
        });
        SortedList<Expense> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(expenseTable.comparatorProperty());
        expenseTable.setItems(sortedData);

        view.getChildren().addAll(actionRow, expenseTable);
        return view;
    }

    private VBox createUnpaidListView() {
        VBox view = new VBox(15);
        view.setPadding(new Insets(15));

        // Filter section
        VBox filterBox = new VBox(10);
        filterBox.setPadding(new Insets(15));
        filterBox.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 8;");

        Label filterTitle = new Label("Filters");
        filterTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        HBox filterRow = new HBox(15);

        // Get unique addresses from member table (case-insensitive, keep first seen
        // display)
        java.util.Map<String, String> addressMap = new java.util.LinkedHashMap<>();
        for (Member member : memberList) {
            if (member.getAddress() != null && !member.getAddress().trim().isEmpty()) {
                String display = member.getAddress().trim();
                String canon = display.toLowerCase();
                addressMap.putIfAbsent(canon, display);
            }
        }
        javafx.collections.ObservableList<String> uniqueAddresses = FXCollections
                .observableArrayList(addressMap.values());
        uniqueAddresses.sort(String::compareToIgnoreCase);

        unpaidAddressFilter = new ComboBox<>();
        unpaidAddressFilter.setItems(uniqueAddresses);
        unpaidAddressFilter.setPromptText("All Addresses");
        unpaidAddressFilter.setPrefWidth(200);

        unpaidDueTypeFilter = new ComboBox<>();
        unpaidDueTypeFilter.setItems(dueTypeList);
        unpaidDueTypeFilter.setPromptText("All Due Types");
        unpaidDueTypeFilter.setPrefWidth(200);
        unpaidDueTypeFilter.setCellFactory(param -> new ListCell<DueType>() {
            @Override
            protected void updateItem(DueType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDueName());
                }
            }
        });
        unpaidDueTypeFilter.setButtonCell(new ListCell<DueType>() {
            @Override
            protected void updateItem(DueType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDueName());
                }
            }
        });

        unpaidStatusFilter = new ComboBox<>();
        unpaidStatusFilter.getItems().addAll("All Status", "PAID", "PARTIALLY_PAID", "NOT_PAID");
        unpaidStatusFilter.setValue("All Status");
        unpaidStatusFilter.setPrefWidth(150);

        Button applyFilterBtn = new Button("Apply Filters");
        applyFilterBtn.setStyle(StyleHelper.getActionPillButtonStyle());
        applyFilterBtn.setOnAction(e -> refreshUnpaidList());

        Button clearFilterBtn = new Button("Clear");
        StyleHelper.styleSecondaryButton(clearFilterBtn);
        clearFilterBtn.setOnAction(e -> {
            unpaidAddressFilter.setValue(null);
            unpaidDueTypeFilter.setValue(null);
            unpaidStatusFilter.setValue("All Status");
            refreshUnpaidList();
        });

        filterRow.getChildren().addAll(
                new Label("Address:"), unpaidAddressFilter,
                new Label("Due Type:"), unpaidDueTypeFilter,
                new Label("Status:"), unpaidStatusFilter,
                applyFilterBtn, clearFilterBtn);

        filterBox.getChildren().addAll(filterTitle, filterRow);

        // Unpaid list table
        unpaidTable = new TableView<>();
        TableStyler.applyModernStyling(unpaidTable);

        TableColumn<java.util.Map<String, Object>, String> addressCol = new TableColumn<>("ADDRESS");
        addressCol.setCellValueFactory(cell -> {
            Object value = cell.getValue().get("address");
            return new javafx.beans.property.SimpleStringProperty(value != null ? value.toString() : "-");
        });
        TableStyler.styleTableColumn(addressCol);

        TableColumn<java.util.Map<String, Object>, String> dueTypeCol = new TableColumn<>("DUE TYPE");
        dueTypeCol.setCellValueFactory(cell -> {
            Object value = cell.getValue().get("dueTypeName");
            return new javafx.beans.property.SimpleStringProperty(value != null ? value.toString() : "-");
        });
        TableStyler.styleTableColumn(dueTypeCol);

        TableColumn<java.util.Map<String, Object>, String> frequencyCol = new TableColumn<>("FREQUENCY");
        frequencyCol.setCellValueFactory(cell -> {
            Object value = cell.getValue().get("dueTypeFrequency");
            return new javafx.beans.property.SimpleStringProperty(value != null ? value.toString() : "-");
        });
        TableStyler.styleTableColumn(frequencyCol);

        TableColumn<java.util.Map<String, Object>, String> expectedCol = new TableColumn<>("EXPECTED AMOUNT");
        expectedCol.setCellValueFactory(cell -> {
            Object value = cell.getValue().get("expectedAmount");
            return new javafx.beans.property.SimpleStringProperty(value != null ? value.toString() : "0");
        });
        TableStyler.styleTableColumn(expectedCol);

        TableColumn<java.util.Map<String, Object>, String> paidCol = new TableColumn<>("PAID AMOUNT");
        paidCol.setCellValueFactory(cell -> {
            Object value = cell.getValue().get("paidAmount");
            return new javafx.beans.property.SimpleStringProperty(value != null ? value.toString() : "0");
        });
        TableStyler.styleTableColumn(paidCol);

        TableColumn<java.util.Map<String, Object>, String> balanceCol = new TableColumn<>("BALANCE");
        balanceCol.setCellValueFactory(cell -> {
            Object value = cell.getValue().get("balance");
            return new javafx.beans.property.SimpleStringProperty(value != null ? value.toString() : "0");
        });
        TableStyler.styleTableColumn(balanceCol);

        TableColumn<java.util.Map<String, Object>, String> statusCol = new TableColumn<>("STATUS");
        statusCol.setCellValueFactory(cell -> {
            Object value = cell.getValue().get("status");
            String status = value != null ? value.toString() : "NOT_PAID";
            String displayStatus = status.equals("PAID") ? "Paid"
                    : status.equals("PARTIALLY_PAID") ? "Partially Paid" : "Not Paid";
            return new javafx.beans.property.SimpleStringProperty(displayStatus);
        });
        TableStyler.styleTableColumn(statusCol);

        TableColumn<java.util.Map<String, Object>, String> remarksCol = new TableColumn<>("Remarks");
        remarksCol.setCellValueFactory(cell -> {
            Object value = cell.getValue().get("remarks");
            return new javafx.beans.property.SimpleStringProperty(value != null ? value.toString() : "-");
        });
        TableStyler.styleTableColumn(remarksCol);

        unpaidTable.getColumns().addAll(addressCol, dueTypeCol, frequencyCol, expectedCol, paidCol, balanceCol,
                statusCol, remarksCol);
        unpaidTable.setItems(unpaidList);

        view.getChildren().addAll(filterBox, unpaidTable);

        // Initialize unpaid list with no filters
        refreshUnpaidList();

        return view;
    }

    private VBox createDueReportView() {
        VBox view = new VBox(15);
        view.setPadding(new Insets(15));

        // Filter section
        VBox filterBox = new VBox(10);
        filterBox.setPadding(new Insets(15));
        filterBox.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 8;");

        Label filterTitle = new Label("Due Collection Report Filters");
        filterTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        HBox filterRow = new HBox(15);

        // Address filter (case-insensitive, preserve display) - items populated via
        // refreshAddressFilters()
        dueReportAddressFilter = new ComboBox<>();
        dueReportAddressFilter.setPromptText("All Addresses");
        dueReportAddressFilter.setPrefWidth(200);

        ComboBox<DueType> dueTypeFilter = new ComboBox<>();
        dueTypeFilter.setItems(dueTypeList);
        dueTypeFilter.setPromptText("All Due Types");
        dueTypeFilter.setPrefWidth(200);
        dueTypeFilter.setCellFactory(param -> new ListCell<DueType>() {
            @Override
            protected void updateItem(DueType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDueName());
                }
            }
        });

        Button applyFilterBtn = new Button("Generate Report");
        applyFilterBtn.setStyle(StyleHelper.getActionPillButtonStyle());
        applyFilterBtn.setOnAction(e -> loadDueReport(dueReportAddressFilter.getValue(), dueTypeFilter.getValue()));

        Button clearFilterBtn = new Button("Clear");
        StyleHelper.styleSecondaryButton(clearFilterBtn);
        clearFilterBtn.setOnAction(e -> {
            dueReportAddressFilter.setValue(null);
            dueTypeFilter.setValue(null);
            loadCollections(); // Load all
        });

        filterRow.getChildren().addAll(
                new Label("Address:"), dueReportAddressFilter,
                new Label("Due Type:"), dueTypeFilter,
                applyFilterBtn, clearFilterBtn);

        filterBox.getChildren().addAll(filterTitle, filterRow);

        // Summary section
        HBox summaryBox = new HBox(20);
        summaryBox.setPadding(new Insets(15));
        summaryBox.setStyle("-fx-background-color: #e8f5e9; -fx-background-radius: 8;");

        dueTotalLabel = new Label("Total Collection: " + FormatUtil.formatCurrency(BigDecimal.ZERO));
        dueTotalLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        dueCountLabel = new Label("Total Records: 0");
        dueCountLabel.setStyle("-fx-font-size: 14px;");

        summaryBox.getChildren().addAll(dueTotalLabel, dueCountLabel);

        view.getChildren().addAll(filterBox, summaryBox, dueCollectionTable);
        return view;
    }

    private VBox createExpenseReportView() {
        VBox view = new VBox(15);
        view.setPadding(new Insets(15));

        // Filter section
        VBox filterBox = new VBox(10);
        filterBox.setPadding(new Insets(15));
        filterBox.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 8;");

        Label filterTitle = new Label("Expense Report Filters");
        filterTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        HBox filterRow = new HBox(15);

        ComboBox<Masjid> masjidFilter = new ComboBox<>();
        masjidFilter.setItems(masjidList);
        masjidFilter.setPromptText("All Masjids");
        masjidFilter.setPrefWidth(200);
        masjidFilter.setCellFactory(param -> new ListCell<Masjid>() {
            @Override
            protected void updateItem(Masjid item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });
        masjidFilter.setButtonCell(new ListCell<Masjid>() {
            @Override
            protected void updateItem(Masjid item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });
        TextField expenseTypeFilter = new TextField();
        expenseTypeFilter.setPromptText("Expense Type");
        expenseTypeFilter.setPrefWidth(200);

        Button applyFilterBtn = new Button("Generate Report");
        applyFilterBtn.setStyle(StyleHelper.getActionPillButtonStyle());
        applyFilterBtn.setOnAction(e -> loadExpenseReport(masjidFilter.getValue(), expenseTypeFilter.getText()));

        Button clearFilterBtn = new Button("Clear");
        StyleHelper.styleSecondaryButton(clearFilterBtn);
        clearFilterBtn.setOnAction(e -> {
            masjidFilter.setValue(null);
            expenseTypeFilter.clear();
            loadExpenses(); // Load all
        });

        filterRow.getChildren().addAll(
                new Label("Masjid:"), masjidFilter,
                new Label("Expense Type:"), expenseTypeFilter,
                applyFilterBtn, clearFilterBtn);

        filterBox.getChildren().addAll(filterTitle, filterRow);

        // Summary section
        HBox summaryBox = new HBox(20);
        summaryBox.setPadding(new Insets(15));
        summaryBox.setStyle("-fx-background-color: #ffebee; -fx-background-radius: 8;");

        expenseTotalLabel = new Label("Total Expense: " + FormatUtil.formatCurrency(BigDecimal.ZERO));
        expenseTotalLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        expenseCountLabel = new Label("Total Records: 0");
        expenseCountLabel.setStyle("-fx-font-size: 14px;");

        summaryBox.getChildren().addAll(expenseTotalLabel, expenseCountLabel);

        view.getChildren().addAll(filterBox, summaryBox, expenseTable);
        return view;
    }

    private void loadAllData() {
        loadIncomeTypes();
        loadIncomes();
        loadDueTypes();
        loadCollections();
        loadExpenses();
        loadMasjids();
        loadMembers();
        // Unpaid list will be loaded when the view is created via refreshUnpaidList()
    }

    private void loadIncomes() {
        new Thread(() -> {
            try {
                java.util.List<Income> incomes = incomeDAO.getAll();
                System.out.println("loadIncomes(): Retrieved " + incomes.size() + " incomes from database");
                javafx.application.Platform.runLater(() -> {
                    incomeList.clear();
                    incomeList.addAll(incomes);
                    System.out.println("loadIncomes(): Updated table with " + incomeList.size() + " incomes");
                });
            } catch (Exception e) {
                System.err.println("Error loading income data: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void loadIncomeTypes() {
        new Thread(() -> {
            java.util.List<IncomeType> types = incomeTypeDAO.getAll();
            javafx.application.Platform.runLater(() -> {
                incomeTypeList.clear();
                incomeTypeList.addAll(types);
            });
        }).start();
    }

    private void loadDueTypes() {
        new Thread(() -> {
            java.util.List<DueType> types = dueTypeDAO.getAll();
            javafx.application.Platform.runLater(() -> {
                dueTypeList.clear();
                dueTypeList.addAll(types);
            });
        }).start();
    }

    private void loadCollections() {
        new Thread(() -> {
            try {
                java.util.List<DueCollection> collections = dueCollectionDAO.getAll();
                System.out.println("loadCollections(): Retrieved " + collections.size() + " collections from database");
                javafx.application.Platform.runLater(() -> {
                    dueCollectionList.clear();
                    dueCollectionList.addAll(collections);
                    System.out.println(
                            "loadCollections(): Updated table with " + dueCollectionList.size() + " collections");
                    updateDueReportSummary();
                    refreshAddressFilters();
                });
            } catch (Exception e) {
                System.err.println("Error loading collection data: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void loadExpenses() {
        new Thread(() -> {
            try {
                java.util.List<Expense> expenses = expenseDAO.getAll();
                System.out.println("loadExpenses(): Retrieved " + expenses.size() + " expenses from database");
                javafx.application.Platform.runLater(() -> {
                    expenseList.clear();
                    expenseList.addAll(expenses);
                    System.out.println("loadExpenses(): Updated table with " + expenseList.size() + " expenses");
                    updateExpenseReportSummary();
                });
            } catch (Exception e) {
                System.err.println("Error loading expense data: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void updateDueReportSummary() {
        if (dueTotalLabel != null && dueCountLabel != null) {
            BigDecimal total = dueCollectionList.stream()
                    .map(DueCollection::getAmount)
                    .filter(a -> a != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            dueTotalLabel.setText("Total Collection: " + FormatUtil.formatCurrency(total));
            dueCountLabel.setText("Total Records: " + dueCollectionList.size());
        }
    }

    private void updateExpenseReportSummary() {
        if (expenseTotalLabel != null && expenseCountLabel != null) {
            BigDecimal total = expenseList.stream()
                    .map(Expense::getAmount)
                    .filter(a -> a != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            expenseTotalLabel.setText("Total Expense: " + FormatUtil.formatCurrency(total));
            expenseCountLabel.setText("Total Records: " + expenseList.size());
        }
    }

    private void loadMasjids() {
        new Thread(() -> {
            java.util.List<Masjid> masjids = masjidDAO.getAll();
            javafx.application.Platform.runLater(() -> {
                masjidList.clear();
                masjidList.addAll(masjids);
            });
        }).start();
    }

    private void loadMembers() {
        new Thread(() -> {
            java.util.List<Member> members = memberDAO.getAll();
            javafx.application.Platform.runLater(() -> {
                memberList.clear();
                memberList.addAll(members);
                refreshAddressFilters();
            });
        }).start();
    }

    /**
     * Rebuilds address filter options (case-insensitive, preserves first seen
     * display)
     */
    private void refreshAddressFilters() {
        java.util.Map<String, String> addressMap = new java.util.LinkedHashMap<>();
        for (Member member : memberList) {
            if (member.getAddress() != null && !member.getAddress().trim().isEmpty()) {
                String display = member.getAddress().trim();
                String canon = display.toLowerCase();
                addressMap.putIfAbsent(canon, display);
            }
        }
        javafx.collections.ObservableList<String> addresses = FXCollections.observableArrayList(addressMap.values());
        addresses.sort(String::compareToIgnoreCase);

        if (unpaidAddressFilter != null) {
            String current = unpaidAddressFilter.getValue();
            unpaidAddressFilter.setItems(addresses);
            if (current != null && !current.isEmpty()) {
                unpaidAddressFilter.setValue(current);
            }
        }

        if (dueReportAddressFilter != null) {
            String current = dueReportAddressFilter.getValue();
            dueReportAddressFilter.setItems(addresses);
            if (current != null && !current.isEmpty()) {
                dueReportAddressFilter.setValue(current);
            }
        }
    }

    private void showIncomeTypeDialog(IncomeType incomeType) {
        Stage dialog = new Stage();
        dialog.setTitle(incomeType == null ? "Add Income Type" : "Edit Income Type");

        VBox root = new VBox(8); // Reduced spacing
        root.setPadding(new Insets(10)); // Reduced padding
        root.setPrefWidth(600); // Increased width
        root.setPrefHeight(550); // Increased height
        FormStyler.styleFormDialog(root);

        Label titleLabel = FormStyler.createFormLabel("Income Type Details");
        titleLabel.setStyle(
                "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + StyleHelper.TEXT_GRAY_900 + ";"); // Smaller
                                                                                                                  // title

        // Compact field styles
        String fieldStyle = "-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: "
                + StyleHelper.BORDER_GRAY + "; -fx-border-width: 1; -fx-padding: 6 10; -fx-font-size: 12px;";
        String focusStyle = "-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: "
                + StyleHelper.PRIMARY_600 + "; -fx-border-width: 2; -fx-padding: 6 10; -fx-font-size: 12px;";
        String comboStyle = "-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: "
                + StyleHelper.BORDER_GRAY + "; -fx-border-width: 1; -fx-padding: 6 10; -fx-font-size: 12px;";
        String comboFocusStyle = "-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: "
                + StyleHelper.PRIMARY_600 + "; -fx-border-width: 2; -fx-padding: 6 10; -fx-font-size: 12px;";

        TextField nameField = new TextField();
        nameField.setPromptText("Name *");
        nameField.setPrefHeight(32); // Smaller height
        nameField.setStyle(fieldStyle);
        nameField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> nameField.setStyle(newVal ? focusStyle : fieldStyle));
        if (incomeType != null)
            nameField.setText(incomeType.getName());

        TextField amountField = new TextField();
        amountField.setPromptText("Default Amount");
        amountField.setPrefHeight(32);
        amountField.setStyle(fieldStyle);
        amountField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> amountField.setStyle(newVal ? focusStyle : fieldStyle));
        if (incomeType != null && incomeType.getDefaultAmount() != null) {
            amountField.setText(incomeType.getDefaultAmount().toString());
        }

        TextArea descField = new TextArea();
        descField.setPromptText("Description");
        descField.setPrefRowCount(3);
        descField.setPrefHeight(70); // Smaller height
        descField.setStyle(fieldStyle);
        descField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> descField.setStyle(newVal ? focusStyle : fieldStyle));
        if (incomeType != null)
            descField.setText(incomeType.getDescription());

        Button saveButton = new Button("Save");
        saveButton.setStyle(
                "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
        saveButton.setOnMouseEntered(e -> saveButton.setStyle(
                "-fx-background-color: #1d4ed8; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        saveButton.setOnMouseExited(e -> saveButton.setStyle(
                "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle(
                "-fx-background-color: #6b7280; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
        cancelButton.setOnMouseEntered(e -> cancelButton.setStyle(
                "-fx-background-color: #4b5563; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        cancelButton.setOnMouseExited(e -> cancelButton.setStyle(
                "-fx-background-color: #6b7280; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(8, 0, 0, 0)); // Reduced padding
        buttonBox.getChildren().addAll(cancelButton, saveButton);
        cancelButton.setOnAction(e -> dialog.close());

        saveButton.setOnAction(e -> {
            // Get and trim all field values
            String name = nameField.getText().trim();
            String amount = amountField.getText().trim();
            String description = descField.getText().trim();

            // Check if all fields are empty
            boolean allEmpty = name.isEmpty() && amount.isEmpty() && description.isEmpty();

            if (allEmpty) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Validation Error");
                alert.setContentText(
                        "Please fill at least one field before saving. Cannot create a record with all empty fields.");
                alert.showAndWait();
                return;
            }

            // Convert empty strings to null to prevent blank values in database
            IncomeType data = new IncomeType();
            data.setName(name.isEmpty() ? null : name);
            data.setType(null); // Type field removed from UI, set to null
            if (!amount.isEmpty()) {
                data.setDefaultAmount(new BigDecimal(amount));
            }
            data.setDescription(description.isEmpty() ? null : description);

            // Final safety check: Ensure at least one field has a non-null, non-empty value
            boolean hasAnyValue = (data.getName() != null && !data.getName().trim().isEmpty()) ||
                    data.getDefaultAmount() != null ||
                    (data.getDescription() != null && !data.getDescription().trim().isEmpty());

            if (!hasAnyValue) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Validation Error");
                alert.setContentText("Cannot save: All fields are empty. Please fill at least one field.");
                alert.showAndWait();
                return;
            }

            new Thread(() -> {
                boolean success;
                if (incomeType == null) {
                    Long newId = incomeTypeDAO.create(data);
                    success = newId != null;
                } else {
                    data.setId(incomeType.getId());
                    success = incomeTypeDAO.update(data);
                }
                javafx.application.Platform.runLater(() -> {
                    if (success) {
                        dialog.close();
                        loadIncomeTypes();
                    }
                });
            }).start();
        });

        root.getChildren().addAll(
                titleLabel,
                FormStyler.createCompactFormField("Name *", nameField),
                FormStyler.createCompactFormField("Default Amount", amountField),
                FormStyler.createCompactFormField("Description", descField),
                buttonBox);

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPrefViewportWidth(640); // Increased width
        scrollPane.setPrefViewportHeight(600); // Increased height

        Scene scene = new Scene(scrollPane, 640, 600);
        dialog.setScene(scene);
        dialog.show();
    }

    private void showDueTypeDialog(DueType dueType) {
        Stage dialog = new Stage();
        dialog.setTitle(dueType == null ? "Add Due Type" : "Edit Due Type");

        VBox root = new VBox(8); // Reduced spacing
        root.setPadding(new Insets(10)); // Reduced padding
        root.setPrefWidth(600); // Increased width
        root.setPrefHeight(550); // Increased height
        FormStyler.styleFormDialog(root);

        Label titleLabel = FormStyler.createFormLabel("Due Type Details");
        titleLabel.setStyle(
                "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + StyleHelper.TEXT_GRAY_900 + ";"); // Smaller
                                                                                                                  // title

        // Compact field styles
        String fieldStyle = "-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: "
                + StyleHelper.BORDER_GRAY + "; -fx-border-width: 1; -fx-padding: 6 10; -fx-font-size: 12px;";
        String focusStyle = "-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: "
                + StyleHelper.PRIMARY_600 + "; -fx-border-width: 2; -fx-padding: 6 10; -fx-font-size: 12px;";
        String comboStyle = "-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: "
                + StyleHelper.BORDER_GRAY + "; -fx-border-width: 1; -fx-padding: 6 10; -fx-font-size: 12px;";
        String comboFocusStyle = "-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: "
                + StyleHelper.PRIMARY_600 + "; -fx-border-width: 2; -fx-padding: 6 10; -fx-font-size: 12px;";

        TextField nameField = new TextField();
        nameField.setPromptText("Due Name *");
        nameField.setPrefHeight(32); // Smaller height
        nameField.setStyle(fieldStyle);
        nameField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> nameField.setStyle(newVal ? focusStyle : fieldStyle));
        if (dueType != null)
            nameField.setText(dueType.getDueName());

        ComboBox<String> frequencyCombo = new ComboBox<>();
        frequencyCombo.getItems().addAll("MONTHLY", "ANNUAL", "ONE_TIME");
        frequencyCombo.setPromptText("Frequency *");
        frequencyCombo.setPrefHeight(32);
        frequencyCombo.setStyle(comboStyle);
        frequencyCombo.focusedProperty()
                .addListener((obs, oldVal, newVal) -> frequencyCombo.setStyle(newVal ? comboFocusStyle : comboStyle));
        if (dueType != null)
            frequencyCombo.setValue(dueType.getFrequency());

        TextField amountField = new TextField();
        amountField.setPromptText("Amount");
        amountField.setPrefHeight(32);
        amountField.setStyle(fieldStyle);
        amountField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> amountField.setStyle(newVal ? focusStyle : fieldStyle));
        if (dueType != null && dueType.getAmount() != null) {
            amountField.setText(dueType.getAmount().toString());
        }

        TextArea descField = new TextArea();
        descField.setPromptText("Description");
        descField.setPrefRowCount(3);
        descField.setPrefHeight(70); // Smaller height
        descField.setStyle(fieldStyle);
        descField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> descField.setStyle(newVal ? focusStyle : fieldStyle));
        if (dueType != null)
            descField.setText(dueType.getDescription());

        Button saveButton = new Button("Save");
        saveButton.setStyle(
                "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
        saveButton.setOnMouseEntered(e -> saveButton.setStyle(
                "-fx-background-color: #1d4ed8; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        saveButton.setOnMouseExited(e -> saveButton.setStyle(
                "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle(
                "-fx-background-color: #6b7280; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
        cancelButton.setOnMouseEntered(e -> cancelButton.setStyle(
                "-fx-background-color: #4b5563; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        cancelButton.setOnMouseExited(e -> cancelButton.setStyle(
                "-fx-background-color: #6b7280; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(8, 0, 0, 0)); // Reduced padding
        buttonBox.getChildren().addAll(cancelButton, saveButton);
        cancelButton.setOnAction(e -> dialog.close());

        saveButton.setOnAction(e -> {
            // Get and trim all field values
            String dueName = nameField.getText().trim();
            String frequency = frequencyCombo.getValue();
            String amount = amountField.getText().trim();
            String description = descField.getText().trim();

            // Check if all fields are empty
            boolean allEmpty = dueName.isEmpty() && frequency == null && amount.isEmpty() && description.isEmpty();

            if (allEmpty) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Validation Error");
                alert.setContentText(
                        "Please fill at least one field before saving. Cannot create a record with all empty fields.");
                alert.showAndWait();
                return;
            }

            // Convert empty strings to null to prevent blank values in database
            DueType data = new DueType();
            data.setDueName(dueName.isEmpty() ? null : dueName);
            data.setFrequency(frequency);
            if (!amount.isEmpty()) {
                data.setAmount(new BigDecimal(amount));
            }
            data.setDescription(description.isEmpty() ? null : description);

            // Final safety check: Ensure at least one field has a non-null, non-empty value
            boolean hasAnyValue = (data.getDueName() != null && !data.getDueName().trim().isEmpty()) ||
                    data.getFrequency() != null ||
                    data.getAmount() != null ||
                    (data.getDescription() != null && !data.getDescription().trim().isEmpty());

            if (!hasAnyValue) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Validation Error");
                alert.setContentText("Cannot save: All fields are empty. Please fill at least one field.");
                alert.showAndWait();
                return;
            }

            new Thread(() -> {
                boolean success;
                if (dueType == null) {
                    Long newId = dueTypeDAO.create(data);
                    success = newId != null;
                } else {
                    data.setId(dueType.getId());
                    success = dueTypeDAO.update(data);
                }
                javafx.application.Platform.runLater(() -> {
                    if (success) {
                        dialog.close();
                        loadDueTypes();
                    }
                });
            }).start();
        });

        root.getChildren().addAll(
                titleLabel,
                FormStyler.createCompactFormField("Due Name *", nameField),
                FormStyler.createCompactFormField("Frequency *", frequencyCombo),
                FormStyler.createCompactFormField("Amount", amountField),
                FormStyler.createCompactFormField("Description", descField),
                buttonBox);

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPrefViewportWidth(640); // Increased width
        scrollPane.setPrefViewportHeight(600); // Increased height

        Scene scene = new Scene(scrollPane, 640, 600);
        dialog.setScene(scene);
        dialog.show();
    }

    private void showCollectionDialog(DueCollection collection) {
        Stage dialog = new Stage();
        dialog.setTitle(collection == null ? "Add Collection" : "Edit Collection");

        VBox root = new VBox(8); // Reduced spacing
        root.setPadding(new Insets(10)); // Reduced padding
        root.setPrefWidth(600); // Increased width
        root.setPrefHeight(550); // Increased height
        FormStyler.styleFormDialog(root);

        Label titleLabel = FormStyler.createFormLabel("Collection Details");
        titleLabel.setStyle(
                "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + StyleHelper.TEXT_GRAY_900 + ";"); // Smaller
                                                                                                                  // title

        // Compact field styles
        String fieldStyle = "-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: "
                + StyleHelper.BORDER_GRAY + "; -fx-border-width: 1; -fx-padding: 6 10; -fx-font-size: 12px;";
        String focusStyle = "-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: "
                + StyleHelper.PRIMARY_600 + "; -fx-border-width: 2; -fx-padding: 6 10; -fx-font-size: 12px;";
        String comboStyle = "-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: "
                + StyleHelper.BORDER_GRAY + "; -fx-border-width: 1; -fx-padding: 6 10; -fx-font-size: 12px;";
        String comboFocusStyle = "-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: "
                + StyleHelper.PRIMARY_600 + "; -fx-border-width: 2; -fx-padding: 6 10; -fx-font-size: 12px;";
        String datePickerStyle = "-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: "
                + StyleHelper.BORDER_GRAY + "; -fx-border-width: 1; -fx-padding: 6 10; -fx-font-size: 12px;";
        String datePickerFocusStyle = "-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: "
                + StyleHelper.PRIMARY_600 + "; -fx-border-width: 2; -fx-padding: 6 10; -fx-font-size: 12px;";

        // Get unique/distinct addresses from member table
        // Unique addresses (case-insensitive, preserve first seen display)
        java.util.Map<String, String> addressMap = new java.util.LinkedHashMap<>();
        for (Member member : memberList) {
            if (member.getAddress() != null && !member.getAddress().trim().isEmpty()) {
                String display = member.getAddress().trim();
                String canon = display.toLowerCase();
                addressMap.putIfAbsent(canon, display);
            }
        }
        javafx.collections.ObservableList<String> uniqueAddresses = FXCollections
                .observableArrayList(addressMap.values());
        uniqueAddresses.sort(String::compareToIgnoreCase); // Sort alphabetically

        ComboBox<String> addressCombo = new ComboBox<>();
        addressCombo.setItems(uniqueAddresses);
        addressCombo.setPromptText("Select Address *");
        addressCombo.setPrefHeight(32);
        addressCombo.setStyle(comboStyle);
        addressCombo.focusedProperty()
                .addListener((obs, oldVal, newVal) -> addressCombo.setStyle(newVal ? comboFocusStyle : comboStyle));

        // Set initial address if editing existing collection
        if (collection != null && collection.getAddress() != null) {
            addressCombo.setValue(collection.getAddress());
        }

        ComboBox<DueType> dueTypeCombo = new ComboBox<>();
        dueTypeCombo.setItems(dueTypeList);
        dueTypeCombo.setPromptText("Due Type *");
        dueTypeCombo.setPrefHeight(32);
        dueTypeCombo.setStyle(comboStyle);
        dueTypeCombo.focusedProperty()
                .addListener((obs, oldVal, newVal) -> dueTypeCombo.setStyle(newVal ? comboFocusStyle : comboStyle));
        dueTypeCombo.setCellFactory(param -> new ListCell<DueType>() {
            @Override
            protected void updateItem(DueType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDueName());
                }
            }
        });
        dueTypeCombo.setButtonCell(new ListCell<DueType>() {
            @Override
            protected void updateItem(DueType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDueName());
                }
            }
        });
        if (collection != null && collection.getDueTypeId() != null) {
            dueTypeList.stream().filter(dt -> dt.getId().equals(collection.getDueTypeId())).findFirst()
                    .ifPresent(dueTypeCombo::setValue);
        }

        TextField amountField = new TextField();
        amountField.setPromptText("Amount *");
        amountField.setPrefHeight(32);
        amountField.setStyle(fieldStyle);
        amountField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> amountField.setStyle(newVal ? focusStyle : fieldStyle));
        if (collection != null && collection.getAmount() != null) {
            amountField.setText(collection.getAmount().toString());
        }

        DatePicker datePicker = new DatePicker();
        datePicker.setPrefHeight(32);
        datePicker.setStyle(datePickerStyle);
        datePicker.focusedProperty().addListener(
                (obs, oldVal, newVal) -> datePicker.setStyle(newVal ? datePickerFocusStyle : datePickerStyle));
        if (collection != null && collection.getDate() != null) {
            datePicker.setValue(collection.getDate());
        }

        ComboBox<String> paymentModeCombo = new ComboBox<>();
        paymentModeCombo.getItems().addAll("CASH", "ONLINE", "CHEQUE");
        paymentModeCombo.setValue("CASH");
        paymentModeCombo.setPrefHeight(32);
        paymentModeCombo.setStyle(comboStyle);
        paymentModeCombo.focusedProperty()
                .addListener((obs, oldVal, newVal) -> paymentModeCombo.setStyle(newVal ? comboFocusStyle : comboStyle));
        if (collection != null)
            paymentModeCombo.setValue(collection.getPaymentMode());

        TextField receiptNoField = new TextField();
        receiptNoField.setPromptText("Receipt No");
        receiptNoField.setPrefHeight(32);
        receiptNoField.setStyle(fieldStyle);
        receiptNoField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> receiptNoField.setStyle(newVal ? focusStyle : fieldStyle));

        // Auto-generate receipt number for new collection entries
        if (collection == null) {
            // Generate receipt number: REC-YYYYMMDD-HHMMSS format
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            String receiptNo = String.format("REC-%s-%02d%02d%02d",
                    now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")),
                    now.getHour(), now.getMinute(), now.getSecond());
            receiptNoField.setText(receiptNo);
            receiptNoField.setEditable(false); // Make read-only for auto-generated receipt
            receiptNoField.setStyle(fieldStyle + " -fx-background-color: #f3f4f6;"); // Gray background to indicate
                                                                                     // read-only
        } else {
            // For editing, allow modification
            receiptNoField.setText(collection.getReceiptNo());
            receiptNoField.setEditable(true);
        }

        TextArea remarksField = new TextArea();
        remarksField.setPromptText("Remarks");
        remarksField.setPrefRowCount(2);
        remarksField.setPrefHeight(50); // Smaller height
        remarksField.setStyle(fieldStyle);
        remarksField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> remarksField.setStyle(newVal ? focusStyle : fieldStyle));
        if (collection != null)
            remarksField.setText(collection.getRemarks());

        Button saveButton = new Button("Save");
        saveButton.setStyle(
                "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
        saveButton.setOnMouseEntered(e -> saveButton.setStyle(
                "-fx-background-color: #1d4ed8; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        saveButton.setOnMouseExited(e -> saveButton.setStyle(
                "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle(
                "-fx-background-color: #6b7280; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
        cancelButton.setOnMouseEntered(e -> cancelButton.setStyle(
                "-fx-background-color: #4b5563; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        cancelButton.setOnMouseExited(e -> cancelButton.setStyle(
                "-fx-background-color: #6b7280; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(8, 0, 0, 0)); // Reduced padding
        buttonBox.getChildren().addAll(cancelButton, saveButton);
        cancelButton.setOnAction(e -> dialog.close());

        saveButton.setOnAction(e -> {
            // Validate required fields
            if (addressCombo.getValue() == null || addressCombo.getValue().trim().isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Validation Error");
                alert.setContentText("Address is required.");
                alert.showAndWait();
                return;
            }

            // Get and trim all field values
            String selectedAddress = addressCombo.getValue().trim();
            String amount = amountField.getText() != null ? amountField.getText().trim() : "";
            String receiptNo = receiptNoField.getText() != null ? receiptNoField.getText().trim() : "";
            String remarks = remarksField.getText() != null ? remarksField.getText().trim() : "";

            // Validate that at least amount or date is provided (address is already
            // required)
            // receiptNo is auto-generated for new entries, so we don't check it here
            boolean allEmpty = amount.isEmpty() && datePicker.getValue() == null &&
                    dueTypeCombo.getValue() == null && paymentModeCombo.getValue() == null &&
                    remarks.isEmpty();

            if (allEmpty) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Validation Error");
                alert.setContentText(
                        "Please fill at least one additional field (Amount, Date, Due Type, Payment Mode, Receipt No, or Remarks) before saving.");
                alert.showAndWait();
                return;
            }

            // Convert empty strings to null to prevent blank values in database
            DueCollection data = new DueCollection();
            data.setAddress(selectedAddress);
            if (dueTypeCombo.getValue() != null)
                data.setDueTypeId(dueTypeCombo.getValue().getId());
            if (!amount.isEmpty()) {
                data.setAmount(new BigDecimal(amount));
            }
            data.setDate(datePicker.getValue());
            data.setPaymentMode(paymentModeCombo.getValue());
            data.setReceiptNo(receiptNo.isEmpty() ? null : receiptNo);
            data.setRemarks(remarks.isEmpty() ? null : remarks);

            saveButton.setDisable(true);
            saveButton.setText("Saving...");

            new Thread(() -> {
                try {
                    // Find a member with the selected address to set member_id
                    // Query database directly to ensure we find the member
                    Member memberWithAddress = memberDAO.getByAddress(selectedAddress);
                    if (memberWithAddress != null) {
                        data.setMemberId(memberWithAddress.getId());
                    }

                    boolean success;
                    if (collection == null) {
                        System.out.println("Creating new collection: amount=" + data.getAmount() + ", date="
                                + data.getDate() + ", receiptNo=" + data.getReceiptNo());
                        Long newId = dueCollectionDAO.create(data);
                        success = newId != null;
                        System.out.println("Collection creation result: " + success + ", ID: " + newId);
                    } else {
                        System.out.println("Updating collection: id=" + collection.getId());
                        data.setId(collection.getId());
                        success = dueCollectionDAO.update(data);
                        System.out.println("Collection update result: " + success);
                    }
                    javafx.application.Platform.runLater(() -> {
                        saveButton.setDisable(false);
                        saveButton.setText("Save");
                        if (success) {
                            dialog.close();
                            loadCollections(); // Reload data to refresh table
                            refreshUnpaidList(); // Refresh unpaid list to update status and paid amount
                            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                            successAlert.setTitle("Success");
                            successAlert.setHeaderText(null);
                            successAlert.setContentText(
                                    "Collection " + (collection == null ? "added" : "updated") + " successfully!");
                            successAlert.show();
                        } else {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Error");
                            alert.setHeaderText("Failed to save collection");
                            alert.setContentText(
                                    "The collection could not be saved. Please check the console for error details and try again.");
                            alert.show();
                        }
                    });
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        saveButton.setDisable(false);
                        saveButton.setText("Save");
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setContentText("Failed to save collection: " + ex.getMessage());
                        alert.show();
                        ex.printStackTrace();
                    });
                }
            }).start();
        });

        root.getChildren().addAll(
                titleLabel,
                FormStyler.createCompactFormField("Address *", addressCombo),
                FormStyler.createCompactFormField("Due Type *", dueTypeCombo),
                FormStyler.createCompactFormField("Amount *", amountField),
                FormStyler.createCompactFormField("Date", datePicker),
                FormStyler.createCompactFormField("Payment Mode", paymentModeCombo),
                FormStyler.createCompactFormField("Receipt No", receiptNoField),
                FormStyler.createCompactFormField("Remarks", remarksField),
                buttonBox);

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPrefViewportWidth(640); // Increased width
        scrollPane.setPrefViewportHeight(600); // Increased height

        Scene scene = new Scene(scrollPane, 640, 600);
        dialog.setScene(scene);
        dialog.show();
    }

    private void showExpenseDialog(Expense expense) {
        Stage dialog = new Stage();
        dialog.setTitle(expense == null ? "Add Expense" : "Edit Expense");

        VBox root = new VBox(8); // Reduced spacing
        root.setPadding(new Insets(10)); // Reduced padding
        root.setPrefWidth(600); // Increased width
        root.setPrefHeight(550); // Increased height
        FormStyler.styleFormDialog(root);

        Label titleLabel = FormStyler.createFormLabel("Expense Details");
        titleLabel.setStyle(
                "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + StyleHelper.TEXT_GRAY_900 + ";"); // Smaller
                                                                                                                  // title

        // Compact field styles
        String fieldStyle = "-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: "
                + StyleHelper.BORDER_GRAY + "; -fx-border-width: 1; -fx-padding: 6 10; -fx-font-size: 12px;";
        String focusStyle = "-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: "
                + StyleHelper.PRIMARY_600 + "; -fx-border-width: 2; -fx-padding: 6 10; -fx-font-size: 12px;";
        String comboStyle = "-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: "
                + StyleHelper.BORDER_GRAY + "; -fx-border-width: 1; -fx-padding: 6 10; -fx-font-size: 12px;";
        String comboFocusStyle = "-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: "
                + StyleHelper.PRIMARY_600 + "; -fx-border-width: 2; -fx-padding: 6 10; -fx-font-size: 12px;";
        String datePickerStyle = "-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: "
                + StyleHelper.BORDER_GRAY + "; -fx-border-width: 1; -fx-padding: 6 10; -fx-font-size: 12px;";
        String datePickerFocusStyle = "-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: "
                + StyleHelper.PRIMARY_600 + "; -fx-border-width: 2; -fx-padding: 6 10; -fx-font-size: 12px;";

        TextField typeField = new TextField();
        typeField.setPromptText("Expense Type *");
        typeField.setPrefHeight(32); // Smaller height
        typeField.setStyle(fieldStyle);
        typeField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> typeField.setStyle(newVal ? focusStyle : fieldStyle));
        if (expense != null)
            typeField.setText(expense.getExpenseType());

        TextField amountField = new TextField();
        amountField.setPromptText("Amount *");
        amountField.setPrefHeight(32);
        amountField.setStyle(fieldStyle);
        amountField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> amountField.setStyle(newVal ? focusStyle : fieldStyle));
        if (expense != null && expense.getAmount() != null) {
            amountField.setText(expense.getAmount().toString());
        }

        DatePicker datePicker = new DatePicker();
        datePicker.setPrefHeight(32);
        datePicker.setStyle(datePickerStyle);
        datePicker.focusedProperty().addListener(
                (obs, oldVal, newVal) -> datePicker.setStyle(newVal ? datePickerFocusStyle : datePickerStyle));
        if (expense != null && expense.getDate() != null) {
            datePicker.setValue(expense.getDate());
        }

        ComboBox<Masjid> masjidCombo = new ComboBox<>();
        masjidCombo.setItems(masjidList);
        masjidCombo.setPromptText("Masjid (Optional)");
        masjidCombo.setPrefHeight(32);
        masjidCombo.setStyle(comboStyle);
        masjidCombo.focusedProperty()
                .addListener((obs, oldVal, newVal) -> masjidCombo.setStyle(newVal ? comboFocusStyle : comboStyle));
        masjidCombo.setCellFactory(param -> new ListCell<Masjid>() {
            @Override
            protected void updateItem(Masjid item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });
        masjidCombo.setButtonCell(new ListCell<Masjid>() {
            @Override
            protected void updateItem(Masjid item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });
        if (expense != null && expense.getMasjidId() != null) {
            masjidList.stream().filter(m -> m.getId().equals(expense.getMasjidId())).findFirst()
                    .ifPresent(masjidCombo::setValue);
        }

        TextArea notesField = new TextArea();
        notesField.setPromptText("Notes");
        notesField.setPrefRowCount(3);
        notesField.setPrefHeight(70); // Smaller height
        notesField.setStyle(fieldStyle);
        notesField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> notesField.setStyle(newVal ? focusStyle : fieldStyle));
        if (expense != null)
            notesField.setText(expense.getNotes());

        Button saveButton = new Button("Save");
        saveButton.setStyle(
                "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
        saveButton.setOnMouseEntered(e -> saveButton.setStyle(
                "-fx-background-color: #1d4ed8; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        saveButton.setOnMouseExited(e -> saveButton.setStyle(
                "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle(
                "-fx-background-color: #6b7280; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
        cancelButton.setOnMouseEntered(e -> cancelButton.setStyle(
                "-fx-background-color: #4b5563; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        cancelButton.setOnMouseExited(e -> cancelButton.setStyle(
                "-fx-background-color: #6b7280; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(8, 0, 0, 0)); // Reduced padding
        buttonBox.getChildren().addAll(cancelButton, saveButton);
        cancelButton.setOnAction(e -> dialog.close());

        saveButton.setOnAction(e -> {
            // Get and trim all field values
            String expenseType = typeField.getText().trim();
            String amount = amountField.getText().trim();
            String notes = notesField.getText().trim();

            // Check if all fields are empty
            boolean allEmpty = expenseType.isEmpty() && amount.isEmpty() && notes.isEmpty() &&
                    datePicker.getValue() == null && masjidCombo.getValue() == null;

            if (allEmpty) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Validation Error");
                alert.setContentText(
                        "Please fill at least one field before saving. Cannot create a record with all empty fields.");
                alert.showAndWait();
                return;
            }

            // Convert empty strings to null to prevent blank values in database
            Expense data = new Expense();
            data.setExpenseType(expenseType.isEmpty() ? null : expenseType);
            if (!amount.isEmpty()) {
                data.setAmount(new BigDecimal(amount));
            } else {
                data.setAmount(BigDecimal.ZERO);
            }
            data.setDate(datePicker.getValue());
            if (masjidCombo.getValue() != null)
                data.setMasjidId(masjidCombo.getValue().getId());
            data.setNotes(notes.isEmpty() ? null : notes);

            // Final safety check: Ensure at least one field has a non-null, non-empty value
            boolean hasAnyValue = (data.getExpenseType() != null && !data.getExpenseType().trim().isEmpty()) ||
                    (data.getAmount() != null && data.getAmount().compareTo(BigDecimal.ZERO) != 0) ||
                    data.getDate() != null ||
                    data.getMasjidId() != null ||
                    (data.getNotes() != null && !data.getNotes().trim().isEmpty());

            if (!hasAnyValue) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Validation Error");
                alert.setContentText("Cannot save: All fields are empty. Please fill at least one field.");
                alert.showAndWait();
                return;
            }

            saveButton.setDisable(true);
            saveButton.setText("Saving...");

            new Thread(() -> {
                try {
                    boolean success;
                    if (expense == null) {
                        System.out.println(
                                "Creating new expense: amount=" + data.getAmount() + ", date=" + data.getDate());
                        Long newId = expenseDAO.create(data);
                        success = newId != null;
                        System.out.println("Expense creation result: " + success + ", ID: " + newId);
                    } else {
                        System.out.println("Updating expense: id=" + expense.getId());
                        data.setId(expense.getId());
                        success = expenseDAO.update(data);
                        System.out.println("Expense update result: " + success);
                    }
                    javafx.application.Platform.runLater(() -> {
                        saveButton.setDisable(false);
                        saveButton.setText("Save");
                        if (success) {
                            dialog.close();
                            loadExpenses(); // Reload data to refresh table
                            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                            successAlert.setTitle("Success");
                            successAlert.setHeaderText(null);
                            successAlert.setContentText(
                                    "Expense " + (expense == null ? "added" : "updated") + " successfully!");
                            successAlert.show();
                        } else {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Error");
                            alert.setHeaderText("Failed to save expense");
                            alert.setContentText(
                                    "The expense could not be saved. Please check the console for error details and try again.");
                            alert.show();
                        }
                    });
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        saveButton.setDisable(false);
                        saveButton.setText("Save");
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setContentText("Failed to save expense: " + ex.getMessage());
                        alert.show();
                        ex.printStackTrace();
                    });
                }
            }).start();
        });

        root.getChildren().addAll(
                titleLabel,
                FormStyler.createCompactFormField("Expense Type *", typeField),
                FormStyler.createCompactFormField("Amount *", amountField),
                FormStyler.createCompactFormField("Date", datePicker),
                FormStyler.createCompactFormField("Masjid (Optional)", masjidCombo),
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
        dialog.show();
    }

    private void deleteIncomeType(IncomeType incomeType) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setContentText("Are you sure you want to delete this income type?");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                new Thread(() -> {
                    boolean success = incomeTypeDAO.delete(incomeType.getId());
                    javafx.application.Platform.runLater(() -> {
                        if (success) {
                            loadIncomeTypes();
                        }
                    });
                }).start();
            }
        });
    }

    private void deleteDueType(DueType dueType) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setContentText("Are you sure you want to delete this due type?");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                new Thread(() -> {
                    boolean success = dueTypeDAO.delete(dueType.getId());
                    javafx.application.Platform.runLater(() -> {
                        if (success) {
                            loadDueTypes();
                        }
                    });
                }).start();
            }
        });
    }

    private void deleteCollection(DueCollection collection) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setContentText("Are you sure you want to delete this collection?");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                new Thread(() -> {
                    boolean success = dueCollectionDAO.delete(collection.getId());
                    javafx.application.Platform.runLater(() -> {
                        if (success) {
                            loadCollections();
                            refreshUnpaidList(); // Refresh unpaid list to update status and paid amount
                        }
                    });
                }).start();
            }
        });
    }

    private void deleteExpense(Expense expense) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setContentText("Are you sure you want to delete this expense?");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                new Thread(() -> {
                    boolean success = expenseDAO.delete(expense.getId());
                    javafx.application.Platform.runLater(() -> {
                        if (success) {
                            loadExpenses();
                        }
                    });
                }).start();
            }
        });
    }

    private void showIncomeDialog(Income income) {
        Stage dialog = new Stage();
        dialog.setTitle(income == null ? "Assign Income" : "Edit Income");

        VBox root = new VBox(8); // Reduced spacing
        root.setPadding(new Insets(10)); // Reduced padding
        root.setPrefWidth(600); // Increased width
        root.setPrefHeight(550); // Increased height
        FormStyler.styleFormDialog(root);

        Label titleLabel = FormStyler.createFormLabel("Income Details");
        titleLabel.setStyle(
                "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + StyleHelper.TEXT_GRAY_900 + ";"); // Smaller
                                                                                                                  // title

        // Compact field styles
        String fieldStyle = "-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: "
                + StyleHelper.BORDER_GRAY + "; -fx-border-width: 1; -fx-padding: 6 10; -fx-font-size: 12px;";
        String focusStyle = "-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: "
                + StyleHelper.PRIMARY_600 + "; -fx-border-width: 2; -fx-padding: 6 10; -fx-font-size: 12px;";
        String comboStyle = "-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: "
                + StyleHelper.BORDER_GRAY + "; -fx-border-width: 1; -fx-padding: 6 10; -fx-font-size: 12px;";
        String comboFocusStyle = "-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: "
                + StyleHelper.PRIMARY_600 + "; -fx-border-width: 2; -fx-padding: 6 10; -fx-font-size: 12px;";
        String datePickerStyle = "-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: "
                + StyleHelper.BORDER_GRAY + "; -fx-border-width: 1; -fx-padding: 6 10; -fx-font-size: 12px;";
        String datePickerFocusStyle = "-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: "
                + StyleHelper.PRIMARY_600 + "; -fx-border-width: 2; -fx-padding: 6 10; -fx-font-size: 12px;";

        ComboBox<Masjid> masjidCombo = new ComboBox<>();
        masjidCombo.setItems(masjidList);
        masjidCombo.setPromptText("Masjid (Optional)");
        masjidCombo.setPrefHeight(32);
        masjidCombo.setStyle(comboStyle);
        masjidCombo.focusedProperty()
                .addListener((obs, oldVal, newVal) -> masjidCombo.setStyle(newVal ? comboFocusStyle : comboStyle));
        masjidCombo.setCellFactory(param -> new ListCell<Masjid>() {
            @Override
            protected void updateItem(Masjid item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });
        masjidCombo.setButtonCell(new ListCell<Masjid>() {
            @Override
            protected void updateItem(Masjid item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });
        if (income != null && income.getMasjidId() != null) {
            masjidList.stream().filter(m -> m.getId().equals(income.getMasjidId())).findFirst()
                    .ifPresent(masjidCombo::setValue);
        }

        ComboBox<Member> memberCombo = new ComboBox<>();
        memberCombo.setItems(memberList);
        memberCombo.setPromptText("Member (Optional)");
        memberCombo.setPrefHeight(32);
        memberCombo.setStyle(comboStyle);
        memberCombo.focusedProperty()
                .addListener((obs, oldVal, newVal) -> memberCombo.setStyle(newVal ? comboFocusStyle : comboStyle));
        memberCombo.setCellFactory(param -> new ListCell<Member>() {
            @Override
            protected void updateItem(Member item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });
        memberCombo.setButtonCell(new ListCell<Member>() {
            @Override
            protected void updateItem(Member item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });
        memberCombo.setButtonCell(new ListCell<Member>() {
            @Override
            protected void updateItem(Member item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });
        if (income != null && income.getMemberId() != null) {
            memberList.stream().filter(m -> m.getId().equals(income.getMemberId())).findFirst()
                    .ifPresent(memberCombo::setValue);
        }

        ComboBox<IncomeType> incomeTypeCombo = new ComboBox<>();
        incomeTypeCombo.setItems(incomeTypeList);
        incomeTypeCombo.setPromptText("Income Type *");
        incomeTypeCombo.setPrefHeight(32);
        incomeTypeCombo.setStyle(comboStyle);
        incomeTypeCombo.focusedProperty()
                .addListener((obs, oldVal, newVal) -> incomeTypeCombo.setStyle(newVal ? comboFocusStyle : comboStyle));
        incomeTypeCombo.setCellFactory(param -> new ListCell<IncomeType>() {
            @Override
            protected void updateItem(IncomeType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    // Only show type in parentheses if it's not null
                    String displayText = item.getName();
                    if (item.getType() != null && !item.getType().trim().isEmpty()) {
                        displayText += " (" + item.getType() + ")";
                    }
                    setText(displayText);
                }
            }
        });
        incomeTypeCombo.setButtonCell(new ListCell<IncomeType>() {
            @Override
            protected void updateItem(IncomeType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    // Only show type in parentheses if it's not null
                    String displayText = item.getName();
                    if (item.getType() != null && !item.getType().trim().isEmpty()) {
                        displayText += " (" + item.getType() + ")";
                    }
                    setText(displayText);
                }
            }
        });
        if (income != null && income.getIncomeTypeId() != null) {
            incomeTypeList.stream().filter(it -> it.getId().equals(income.getIncomeTypeId())).findFirst()
                    .ifPresent(incomeTypeCombo::setValue);
        }

        TextField amountField = new TextField();
        amountField.setPromptText("Amount *");
        amountField.setPrefHeight(32);
        amountField.setStyle(fieldStyle);
        amountField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> amountField.setStyle(newVal ? focusStyle : fieldStyle));
        if (income != null && income.getAmount() != null) {
            amountField.setText(income.getAmount().toString());
        }

        DatePicker datePicker = new DatePicker();
        datePicker.setPrefHeight(32);
        datePicker.setStyle(datePickerStyle);
        datePicker.focusedProperty().addListener(
                (obs, oldVal, newVal) -> datePicker.setStyle(newVal ? datePickerFocusStyle : datePickerStyle));
        if (income != null && income.getDate() != null) {
            datePicker.setValue(income.getDate());
        } else {
            datePicker.setValue(LocalDate.now());
        }

        ComboBox<String> paymentModeCombo = new ComboBox<>();
        paymentModeCombo.getItems().addAll("CASH", "ONLINE", "CHEQUE");
        paymentModeCombo.setValue("CASH");
        paymentModeCombo.setPrefHeight(32);
        paymentModeCombo.setStyle(comboStyle);
        paymentModeCombo.focusedProperty()
                .addListener((obs, oldVal, newVal) -> paymentModeCombo.setStyle(newVal ? comboFocusStyle : comboStyle));
        if (income != null)
            paymentModeCombo.setValue(income.getPaymentMode());

        TextField receiptNoField = new TextField();
        receiptNoField.setPromptText("Receipt No");
        receiptNoField.setPrefHeight(32);
        receiptNoField.setStyle(fieldStyle);
        receiptNoField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> receiptNoField.setStyle(newVal ? focusStyle : fieldStyle));

        // Auto-generate receipt number for new income entries
        if (income == null) {
            // Generate receipt number: REC-YYYYMMDD-HHMMSS format
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            String receiptNo = String.format("REC-%s-%02d%02d%02d",
                    now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")),
                    now.getHour(), now.getMinute(), now.getSecond());
            receiptNoField.setText(receiptNo);
            receiptNoField.setEditable(false); // Make read-only for auto-generated receipt
            receiptNoField.setStyle(fieldStyle + " -fx-background-color: #f3f4f6;"); // Gray background to indicate
                                                                                     // read-only
        } else {
            // For editing, allow modification
            receiptNoField.setText(income.getReceiptNo());
            receiptNoField.setEditable(true);
        }

        TextArea remarksField = new TextArea();
        remarksField.setPromptText("Remarks");
        remarksField.setPrefRowCount(2);
        remarksField.setPrefHeight(50); // Smaller height
        remarksField.setStyle(fieldStyle);
        remarksField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> remarksField.setStyle(newVal ? focusStyle : fieldStyle));
        if (income != null)
            remarksField.setText(income.getRemarks());

        Button saveButton = new Button("Save");
        saveButton.setStyle(
                "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
        saveButton.setOnMouseEntered(e -> saveButton.setStyle(
                "-fx-background-color: #1d4ed8; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        saveButton.setOnMouseExited(e -> saveButton.setStyle(
                "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle(
                "-fx-background-color: #6b7280; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
        cancelButton.setOnMouseEntered(e -> cancelButton.setStyle(
                "-fx-background-color: #4b5563; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        cancelButton.setOnMouseExited(e -> cancelButton.setStyle(
                "-fx-background-color: #6b7280; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(8, 0, 0, 0)); // Reduced padding
        buttonBox.getChildren().addAll(cancelButton, saveButton);
        cancelButton.setOnAction(e -> dialog.close());

        saveButton.setOnAction(e -> {
            // Get and trim all field values
            String amount = amountField.getText().trim();
            String receiptNo = receiptNoField.getText().trim();
            String remarks = remarksField.getText().trim();

            // Check if all fields are empty (receiptNo is auto-generated, so we don't check
            // it here)
            boolean allEmpty = amount.isEmpty() && remarks.isEmpty() &&
                    datePicker.getValue() == null && masjidCombo.getValue() == null &&
                    memberCombo.getValue() == null && incomeTypeCombo.getValue() == null &&
                    paymentModeCombo.getValue() == null;

            if (allEmpty) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Validation Error");
                alert.setContentText(
                        "Please fill at least one field before saving. Cannot create a record with all empty fields.");
                alert.showAndWait();
                return;
            }

            // Convert empty strings to null to prevent blank values in database
            Income data = new Income();
            if (masjidCombo.getValue() != null)
                data.setMasjidId(masjidCombo.getValue().getId());
            if (memberCombo.getValue() != null)
                data.setMemberId(memberCombo.getValue().getId());
            if (incomeTypeCombo.getValue() != null)
                data.setIncomeTypeId(incomeTypeCombo.getValue().getId());
            if (!amount.isEmpty()) {
                data.setAmount(new BigDecimal(amount));
            } else {
                data.setAmount(BigDecimal.ZERO);
            }
            data.setDate(datePicker.getValue());
            data.setPaymentMode(paymentModeCombo.getValue());
            // Use receipt number from field (auto-generated for new, editable for edit)
            data.setReceiptNo(receiptNo.isEmpty() ? null : receiptNo);
            data.setRemarks(remarks.isEmpty() ? null : remarks);

            // Final safety check: Ensure at least one field has a non-null, non-empty value
            boolean hasAnyValue = data.getMasjidId() != null ||
                    data.getMemberId() != null ||
                    data.getIncomeTypeId() != null ||
                    (data.getAmount() != null && data.getAmount().compareTo(BigDecimal.ZERO) != 0) ||
                    data.getDate() != null ||
                    data.getPaymentMode() != null ||
                    (data.getReceiptNo() != null && !data.getReceiptNo().trim().isEmpty()) ||
                    (data.getRemarks() != null && !data.getRemarks().trim().isEmpty());

            if (!hasAnyValue) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Validation Error");
                alert.setContentText("Cannot save: All fields are empty. Please fill at least one field.");
                alert.showAndWait();
                return;
            }

            new Thread(() -> {
                try {
                    boolean success;
                    if (income == null) {
                        System.out.println(
                                "Creating new income: amount=" + data.getAmount() + ", date=" + data.getDate());
                        Long newId = incomeDAO.create(data);
                        success = newId != null;
                        System.out.println("Income creation result: " + success + ", ID: " + newId);
                    } else {
                        System.out.println("Updating income: id=" + income.getId());
                        data.setId(income.getId());
                        success = incomeDAO.update(data);
                        System.out.println("Income update result: " + success);
                    }
                    javafx.application.Platform.runLater(() -> {
                        if (success) {
                            dialog.close();
                            loadIncomes(); // Reload data to refresh table
                            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                            successAlert.setTitle("Success");
                            successAlert.setHeaderText(null);
                            successAlert.setContentText(
                                    "Income " + (income == null ? "added" : "updated") + " successfully!");
                            successAlert.show();
                        } else {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Error");
                            alert.setHeaderText("Failed to save income");
                            alert.setContentText(
                                    "The income could not be saved. Please check the console for error details and try again.");
                            alert.show();
                        }
                    });
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setContentText("Failed to save income: " + ex.getMessage());
                        alert.show();
                        ex.printStackTrace();
                    });
                }
            }).start();
        });

        root.getChildren().addAll(
                titleLabel,
                FormStyler.createCompactFormField("Masjid (Optional)", masjidCombo),
                FormStyler.createCompactFormField("Member (Optional)", memberCombo),
                FormStyler.createCompactFormField("Income Type *", incomeTypeCombo),
                FormStyler.createCompactFormField("Amount *", amountField),
                FormStyler.createCompactFormField("Date", datePicker),
                FormStyler.createCompactFormField("Payment Mode", paymentModeCombo),
                FormStyler.createCompactFormField("Receipt No", receiptNoField),
                FormStyler.createCompactFormField("Remarks", remarksField),
                buttonBox);

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPrefViewportWidth(640); // Increased width
        scrollPane.setPrefViewportHeight(600); // Increased height

        Scene scene = new Scene(scrollPane, 640, 600);
        dialog.setScene(scene);
        dialog.show();
    }

    private void deleteIncome(Income income) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setContentText("Are you sure you want to delete this income record?");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                new Thread(() -> {
                    boolean success = incomeDAO.delete(income.getId());
                    javafx.application.Platform.runLater(() -> {
                        if (success) {
                            loadIncomes();
                        }
                    });
                }).start();
            }
        });
    }

    private void refreshUnpaidList() {
        // Get current filter values and refresh the unpaid list
        String address = unpaidAddressFilter != null ? unpaidAddressFilter.getValue() : null;
        DueType dueType = unpaidDueTypeFilter != null ? unpaidDueTypeFilter.getValue() : null;
        String status = unpaidStatusFilter != null && unpaidStatusFilter.getValue() != null &&
                !unpaidStatusFilter.getValue().equals("All Status") ? unpaidStatusFilter.getValue() : null;
        loadUnpaidList(address, dueType, status);
    }

    private void loadUnpaidList(String addressFilter, DueType dueTypeFilter, String statusFilter) {
        new Thread(() -> {
            java.util.List<DueCollection> allCollections = dueCollectionDAO.getAll();
            java.util.List<Member> members = memberDAO.getAll();
            java.util.List<DueType> dueTypes = dueTypeDAO.getAll();

            // Get unique addresses (no duplicates)
            java.util.Set<String> uniqueAddresses = new java.util.HashSet<>();
            for (Member member : members) {
                if (member.getAddress() != null && !member.getAddress().trim().isEmpty()) {
                    uniqueAddresses.add(member.getAddress().trim().toLowerCase());
                }
            }

            // Filter by address (case-insensitive)
            if (addressFilter != null && !addressFilter.trim().isEmpty()) {
                final String addrFilter = addressFilter.trim().toLowerCase();
                uniqueAddresses = uniqueAddresses.stream()
                        .filter(addr -> addr != null && addr.trim().toLowerCase().equals(addrFilter))
                        .collect(java.util.stream.Collectors.toSet());
            }

            java.util.List<java.util.Map<String, Object>> unpaid = new java.util.ArrayList<>();

            // Group by unique address (no duplication)
            for (String address : uniqueAddresses) {
                for (DueType dueType : dueTypes) {
                    if (dueTypeFilter != null && !dueType.getId().equals(dueTypeFilter.getId())) {
                        continue;
                    }

                    BigDecimal expectedAmount = dueType.getAmount() != null ? dueType.getAmount() : BigDecimal.ZERO;

                    // Calculate total paid amount for this address and due type (no duplication)
                    BigDecimal paidAmount = allCollections.stream()
                            .filter(dc -> dc.getAddress() != null && dc.getAddress().trim().equalsIgnoreCase(address)
                                    && dc.getDueTypeId() != null && dc.getDueTypeId().equals(dueType.getId()))
                            .map(DueCollection::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    // Expected amount shown once per address (no duplication)
                    BigDecimal totalExpectedAmount = expectedAmount;
                    BigDecimal balance = totalExpectedAmount.subtract(paidAmount);

                    String status;
                    if (paidAmount.compareTo(BigDecimal.ZERO) == 0) {
                        status = "NOT_PAID";
                    } else if (balance.compareTo(BigDecimal.ZERO) <= 0) {
                        status = "PAID";
                    } else {
                        status = "PARTIALLY_PAID";
                    }

                    if (statusFilter != null && !status.equals(statusFilter)) {
                        continue;
                    }

                    String remarks = allCollections.stream()
                            .filter(dc -> dc.getAddress() != null && dc.getAddress().trim().equalsIgnoreCase(address)
                                    && dc.getDueTypeId() != null && dc.getDueTypeId().equals(dueType.getId()))
                            .sorted((a, b) -> {
                                if (a.getDate() == null)
                                    return 1;
                                if (b.getDate() == null)
                                    return -1;
                                return b.getDate().compareTo(a.getDate());
                            })
                            .findFirst()
                            .map(DueCollection::getRemarks)
                            .orElse(null);

                    if (totalExpectedAmount.compareTo(BigDecimal.ZERO) > 0
                            || paidAmount.compareTo(BigDecimal.ZERO) > 0) {
                        java.util.Map<String, Object> item = new java.util.HashMap<>();
                        item.put("address", address);
                        item.put("dueTypeId", dueType.getId());
                        item.put("dueTypeName", dueType.getDueName());
                        item.put("dueTypeFrequency", dueType.getFrequency());
                        item.put("expectedAmount", totalExpectedAmount);
                        item.put("paidAmount", paidAmount);
                        item.put("balance", balance);
                        item.put("status", status);
                        item.put("remarks", remarks);
                        unpaid.add(item);
                    }
                }
            }

            javafx.application.Platform.runLater(() -> {
                unpaidList.clear();
                unpaidList.addAll(unpaid);
            });
        }).start();
    }

    private void loadDueReport(String addressFilter, DueType dueTypeFilter) {
        new Thread(() -> {
            java.util.List<DueCollection> collections = dueCollectionDAO.getAll();

            // Apply filters
            if (addressFilter != null && !addressFilter.trim().isEmpty()) {
                final String address = addressFilter.trim().toLowerCase();
                collections = collections.stream()
                        .filter(dc -> dc.getAddress() != null && dc.getAddress().trim().toLowerCase().equals(address))
                        .collect(java.util.stream.Collectors.toList());
            }

            if (dueTypeFilter != null) {
                final Long dueTypeId = dueTypeFilter.getId();
                collections = collections.stream()
                        .filter(dc -> dc.getDueTypeId() != null && dc.getDueTypeId().equals(dueTypeId))
                        .collect(java.util.stream.Collectors.toList());
            }

            final java.util.List<DueCollection> finalCollections = collections;
            BigDecimal total = finalCollections.stream()
                    .map(DueCollection::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            final BigDecimal finalTotal = total;
            final int finalCount = finalCollections.size();

            javafx.application.Platform.runLater(() -> {
                dueCollectionList.clear();
                dueCollectionList.addAll(finalCollections);

                // Update summary
                if (dueTotalLabel != null)
                    dueTotalLabel.setText("Total Collection: ₹" + finalTotal.toString());
                if (dueCountLabel != null)
                    dueCountLabel.setText("Total Records: " + finalCount);
            });
        }).start();
    }

    private void loadExpenseReport(Masjid masjidFilter, String expenseTypeFilter) {
        new Thread(() -> {
            java.util.List<Expense> expenses = expenseDAO.getAll();

            // Apply filters
            if (masjidFilter != null) {
                final Long masjidId = masjidFilter.getId();
                expenses = expenses.stream()
                        .filter(e -> e.getMasjidId() != null && e.getMasjidId().equals(masjidId))
                        .collect(java.util.stream.Collectors.toList());
            }

            if (expenseTypeFilter != null && !expenseTypeFilter.isEmpty()) {
                final String filterText = expenseTypeFilter.toLowerCase();
                expenses = expenses.stream()
                        .filter(e -> e.getExpenseType() != null
                                && e.getExpenseType().toLowerCase().contains(filterText))
                        .collect(java.util.stream.Collectors.toList());
            }

            final java.util.List<Expense> finalExpenses = expenses;
            BigDecimal total = finalExpenses.stream()
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            final BigDecimal finalTotal = total;
            final int finalCount = finalExpenses.size();

            javafx.application.Platform.runLater(() -> {
                expenseList.clear();
                expenseList.addAll(finalExpenses);

                // Update summary
                if (expenseTotalLabel != null)
                    expenseTotalLabel.setText("Total Expense: ₹" + finalTotal.toString());
                if (expenseCountLabel != null)
                    expenseCountLabel.setText("Total Records: " + finalCount);
            });
        }).start();
    }

}
