package com.mahal.controller.member;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import com.mahal.database.MemberDAO;
import com.mahal.database.HouseDAO;
import com.mahal.model.Member;
import com.mahal.model.House;
import com.mahal.util.StyleHelper;
import com.mahal.util.TableStyler;
import com.mahal.util.FormStyler;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.geometry.Pos;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;
import javafx.util.StringConverter;

public class MemberController {
    private VBox view;
    private StackPane contentPane;
    private VBox memberViewPane;
    private VBox houseViewPane;
    private TableView<Member> memberTable;
    private TableView<House> houseTable;
    private TextField memberSearchField;
    private TextField houseSearchField;
    private ObservableList<Member> memberList;
    private ObservableList<House> houseList;
    private MemberDAO memberDAO;
    private HouseDAO houseDAO;

    private void setMaxLength(TextInputControl control, int maxLen) {
        control.setTextFormatter(new TextFormatter<String>(change -> {
            String newText = change.getControlNewText();
            if (newText.length() <= maxLen) {
                return change;
            }
            return null;
        }));
    }

    private int getMonthNumber(String monthName) {
        String[] monthNames = { "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December" };
        for (int i = 0; i < monthNames.length; i++) {
            if (monthNames[i].equals(monthName)) {
                return i + 1; // Return 1-12
            }
        }
        return 1; // Default to January if not found
    }

    private VBox createAddressFormField(String labelText, HBox fieldBox) {
        VBox fieldContainer = new VBox(3);
        Label label = new Label(labelText);
        label.setStyle("-fx-font-size: 12px; -fx-font-weight: 500; -fx-text-fill: " + StyleHelper.TEXT_GRAY_700 + ";");
        fieldContainer.getChildren().addAll(label, fieldBox);
        return fieldContainer;
    }

    public MemberController() {
        this.memberDAO = new MemberDAO();
        this.houseDAO = new HouseDAO();
        this.memberList = FXCollections.observableArrayList();
        this.houseList = FXCollections.observableArrayList();
        createView();
        loadData();
        loadHouseData();

        // Subscribe to sync events
        com.mahal.util.EventBus.getInstance().subscribe("members",
                e -> javafx.application.Platform.runLater(this::loadData));
        com.mahal.util.EventBus.getInstance().subscribe("houses",
                e -> javafx.application.Platform.runLater(this::loadHouseData));
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
        Label titleLabel = new Label("Member Management");
        titleLabel.setStyle(StyleHelper.getTitleStyle());
        Label subtitleLabel = new Label("Manage families, house registrations and member profiles");
        subtitleLabel.setStyle(StyleHelper.getSubtitleStyle());
        header.getChildren().addAll(titleLabel, subtitleLabel);

        // Navigation Pill Switcher
        HBox pillContainer = new HBox(10);
        pillContainer.setAlignment(Pos.CENTER_LEFT);
        pillContainer.setPadding(new Insets(10, 0, 10, 0));

        HBox switcher = new HBox(0); // 0 spacing for pill look
        switcher.setStyle(StyleHelper.getPillSwitcherContainerStyle());

        Button membersPill = new Button("Members");
        Button housesPill = new Button("Houses");

        membersPill.setStyle(StyleHelper.getPillButtonStyle(true));
        housesPill.setStyle(StyleHelper.getPillButtonStyle(false));

        membersPill.setOnAction(e -> {
            contentPane.getChildren().clear();
            contentPane.getChildren().add(memberViewPane);
            membersPill.setStyle(StyleHelper.getPillButtonStyle(true));
            housesPill.setStyle(StyleHelper.getPillButtonStyle(false));
            loadData();
        });

        housesPill.setOnAction(e -> {
            contentPane.getChildren().clear();
            contentPane.getChildren().add(houseViewPane);
            membersPill.setStyle(StyleHelper.getPillButtonStyle(false));
            housesPill.setStyle(StyleHelper.getPillButtonStyle(true));
            loadHouseData();
        });

        switcher.getChildren().addAll(membersPill, housesPill);
        pillContainer.getChildren().add(switcher);

        contentPane = new StackPane();
        VBox.setVgrow(contentPane, Priority.ALWAYS);

        // Initialized Panes
        createMemberView();
        createHouseView();

        // Default view
        contentPane.getChildren().add(memberViewPane);

        // Wrap everything in a card
        VBox mainCard = new VBox(20);
        mainCard.setStyle(StyleHelper.getCardStyle());
        mainCard.getChildren().addAll(pillContainer, contentPane);
        VBox.setVgrow(mainCard, Priority.ALWAYS);

        view.getChildren().addAll(header, mainCard);
    }

    private void createMemberView() {
        memberViewPane = new VBox(20);

        // Action Row
        HBox actionRow = new HBox(15);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        memberSearchField = new TextField();
        memberSearchField.setPromptText("Search members by name, mobile, id...");
        memberSearchField.setPrefWidth(350);
        StyleHelper.styleTextField(memberSearchField);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addButton = new Button("Add Member");
        addButton.setStyle(StyleHelper.getActionPillButtonStyle());
        addButton.setOnAction(e -> showMemberDialog(null));

        actionRow.getChildren().addAll(memberSearchField, spacer, addButton);

        memberTable = new TableView<>();
        TableStyler.applyModernStyling(memberTable);

        TableColumn<Member, String> nameCol = new TableColumn<>("NAME");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setMinWidth(150);
        TableStyler.styleTableColumn(nameCol);

        TableColumn<Member, String> fatherCol = new TableColumn<>("FATHER NAME");
        fatherCol.setCellValueFactory(new PropertyValueFactory<>("fatherName"));
        fatherCol.setMinWidth(150);
        TableStyler.styleTableColumn(fatherCol);

        TableColumn<Member, String> mobileCol = new TableColumn<>("MOBILE");
        mobileCol.setCellValueFactory(new PropertyValueFactory<>("mobile"));
        mobileCol.setMinWidth(120);
        TableStyler.styleTableColumn(mobileCol);

        TableColumn<Member, String> ageCol = new TableColumn<>("DOB");
        ageCol.setCellValueFactory(cell -> {
            LocalDate dob = cell.getValue().getDateOfBirth();
            return new javafx.beans.property.SimpleStringProperty(
                    dob != null ? dob.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) : "-");
        });
        ageCol.setMinWidth(120);
        TableStyler.styleTableColumn(ageCol);

        TableColumn<Member, String> addressCol = new TableColumn<>("ADDRESS");
        addressCol.setCellValueFactory(new PropertyValueFactory<>("address"));
        addressCol.setMinWidth(200);
        TableStyler.styleTableColumn(addressCol);

        TableColumn<Member, String> actionsCol = new TableColumn<>("ACTIONS");
        actionsCol.setMinWidth(150);
        actionsCol.setCellFactory(param -> new TableCell<Member, String>() {
            private final Hyperlink editLink = new Hyperlink("Edit");
            private final Hyperlink deleteLink = new Hyperlink("Delete");

            {
                editLink.setStyle("-fx-text-fill: #2563eb; -fx-font-weight: bold;");
                deleteLink.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");

                editLink.setOnAction(e -> {
                    Member member = getTableView().getItems().get(getIndex());
                    Member fullMember = memberDAO.getById(member.getId());
                    if (fullMember != null)
                        showMemberDialog(fullMember);
                });

                deleteLink.setOnAction(e -> {
                    Member member = getTableView().getItems().get(getIndex());
                    deleteMember(member);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(15);
                    box.setAlignment(Pos.CENTER);
                    box.getChildren().addAll(editLink, deleteLink);
                    setGraphic(box);
                }
            }
        });
        TableStyler.styleTableColumn(actionsCol);

        memberTable.getColumns().addAll(nameCol, fatherCol, mobileCol, ageCol, addressCol, actionsCol);
        VBox.setVgrow(memberTable, Priority.ALWAYS);

        // Filtering logic
        FilteredList<Member> filteredData = new FilteredList<>(memberList, p -> true);
        memberSearchField.textProperty().addListener((obs, old, newValue) -> {
            filteredData.setPredicate(mem -> {
                if (newValue == null || newValue.isEmpty())
                    return true;
                String lower = newValue.toLowerCase();
                if (mem.getName() != null && mem.getName().toLowerCase().contains(lower))
                    return true;
                if (mem.getMobile() != null && mem.getMobile().contains(lower))
                    return true;
                if (mem.getAddress() != null && mem.getAddress().toLowerCase().contains(lower))
                    return true;
                return false;
            });
        });

        memberTable.setItems(filteredData);
        memberViewPane.getChildren().addAll(actionRow, memberTable);
    }

    private void createHouseView() {
        houseViewPane = new VBox(20);

        // Action Row
        HBox actionRow = new HBox(15);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        houseSearchField = new TextField();
        houseSearchField.setPromptText("Search houses by index or address...");
        houseSearchField.setPrefWidth(350);
        StyleHelper.styleTextField(houseSearchField);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addButton = new Button("Add House");
        addButton.setStyle(StyleHelper.getActionPillButtonStyle());
        addButton.setOnAction(e -> showHouseDialog(null));

        actionRow.getChildren().addAll(houseSearchField, spacer, addButton);

        houseTable = new TableView<>();
        TableStyler.applyModernStyling(houseTable);

        TableColumn<House, String> addressCol = new TableColumn<>("ADDRESS");
        addressCol.setCellValueFactory(new PropertyValueFactory<>("address"));
        addressCol.setMinWidth(300);
        TableStyler.styleTableColumn(addressCol);

        TableColumn<House, String> houseNumCol = new TableColumn<>("HOUSE NO.");
        houseNumCol.setCellValueFactory(new PropertyValueFactory<>("houseNumber"));
        houseNumCol.setMinWidth(150);
        TableStyler.styleTableColumn(houseNumCol);

        TableColumn<House, String> actionsCol = new TableColumn<>("ACTIONS");
        actionsCol.setMinWidth(150);
        actionsCol.setCellFactory(param -> new TableCell<House, String>() {
            private final Hyperlink editLink = new Hyperlink("Edit");
            private final Hyperlink deleteLink = new Hyperlink("Delete");

            {
                editLink.setStyle("-fx-text-fill: #2563eb; -fx-font-weight: bold;");
                deleteLink.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");

                editLink.setOnAction(e -> {
                    House house = getTableView().getItems().get(getIndex());
                    showHouseDialog(house);
                });

                deleteLink.setOnAction(e -> {
                    House house = getTableView().getItems().get(getIndex());
                    deleteHouse(house);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty)
                    setGraphic(null);
                else {
                    HBox box = new HBox(15);
                    box.setAlignment(Pos.CENTER);
                    box.getChildren().addAll(editLink, deleteLink);
                    setGraphic(box);
                }
            }
        });
        TableStyler.styleTableColumn(actionsCol);

        houseTable.getColumns().addAll(addressCol, houseNumCol, actionsCol);
        VBox.setVgrow(houseTable, Priority.ALWAYS);

        FilteredList<House> filteredData = new FilteredList<>(houseList, p -> true);
        houseSearchField.textProperty().addListener((obs, old, newValue) -> {
            filteredData.setPredicate(house -> {
                if (newValue == null || newValue.isEmpty())
                    return true;
                String lower = newValue.toLowerCase();
                if (house.getAddress() != null && house.getAddress().toLowerCase().contains(lower))
                    return true;
                if (house.getHouseNumber() != null && house.getHouseNumber().toLowerCase().contains(lower))
                    return true;
                return false;
            });
        });

        houseTable.setItems(filteredData);
        houseViewPane.getChildren().addAll(actionRow, houseTable);
    }

    private void loadData() {
        new Thread(() -> {
            try {
                java.util.List<Member> members = memberDAO.getAll();
                System.out.println("loadData(): Retrieved " + members.size() + " members from database");
                javafx.application.Platform.runLater(() -> {
                    memberList.clear();
                    memberList.addAll(members);
                    System.out.println("loadData(): Updated table with " + memberList.size() + " members");
                });
            } catch (Exception e) {
                System.err.println("Error loading member data: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void loadHouseData() {
        new Thread(() -> {
            java.util.List<House> houses = houseDAO.getAll();
            javafx.application.Platform.runLater(() -> {
                houseList.clear();
                houseList.addAll(houses);
            });
        }).start();
    }

    private void showMemberDialog(Member member) {
        Stage dialog = new Stage();
        dialog.setTitle(member == null ? "Add Member" : "Edit Member");

        // Store original member ID for edit mode
        final Long originalMemberId = (member != null && member.getId() != null) ? member.getId() : null;

        VBox root = new VBox(8); // Reduced spacing
        root.setPadding(new Insets(10)); // Reduced padding
        root.setPrefWidth(600); // Increased width
        root.setPrefHeight(550); // Increased height
        FormStyler.styleFormDialog(root);

        Label titleLabel = FormStyler.createFormLabel("Member Details");
        titleLabel.setStyle(
                "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + StyleHelper.TEXT_GRAY_900 + ";"); // Smaller
                                                                                                                  // title

        TextField nameField = new TextField();
        nameField.setPromptText("Name *");
        setMaxLength(nameField, 100);
        if (member != null)
            nameField.setText(member.getName());

        TextField qualificationField = new TextField();
        qualificationField.setPromptText("Qualification");
        setMaxLength(qualificationField, 50);
        if (member != null)
            qualificationField.setText(member.getQualification());

        TextField fatherNameField = new TextField();
        fatherNameField.setPromptText("Father's Name");
        setMaxLength(fatherNameField, 100);
        if (member != null)
            fatherNameField.setText(member.getFatherName());

        TextField motherNameField = new TextField();
        motherNameField.setPromptText("Mother's Name");
        setMaxLength(motherNameField, 100);
        if (member != null)
            motherNameField.setText(member.getMotherName());

        TextField districtField = new TextField();
        districtField.setPromptText("District");
        setMaxLength(districtField, 50);
        if (member != null)
            districtField.setText(member.getDistrict());

        TextField panchayatField = new TextField();
        panchayatField.setPromptText("Panchayat");
        setMaxLength(panchayatField, 50);
        if (member != null)
            panchayatField.setText(member.getPanchayat());

        // Day, Month and Year dropdowns for Date of Birth
        ComboBox<Integer> dayCombo = new ComboBox<>();
        for (int day = 1; day <= 31; day++) {
            dayCombo.getItems().add(day);
        }
        dayCombo.setPromptText("Day");
        dayCombo.setVisibleRowCount(10);
        dayCombo.setStyle(StyleHelper.getInputFieldStyle());
        dayCombo.setPrefWidth(80);
        // Set button cell to properly display prompt text
        dayCombo.setButtonCell(new javafx.scene.control.ListCell<Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                // Show "Day" placeholder when no value is selected
                if (empty || item == null) {
                    setText("Day");
                    setStyle("-fx-text-fill: #9ca3af;"); // Gray text for placeholder
                } else {
                    setText(item.toString());
                    setStyle("-fx-text-fill: black;");
                }
            }
        });

        ComboBox<String> monthCombo = new ComboBox<>();
        monthCombo.getItems().addAll("January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December");
        monthCombo.setPromptText("Month");
        monthCombo.setVisibleRowCount(10);
        monthCombo.setStyle(StyleHelper.getInputFieldStyle());
        monthCombo.setPrefWidth(120);

        ComboBox<Integer> yearCombo = new ComboBox<>();
        int currentYear = LocalDate.now().getYear();
        for (int year = currentYear; year >= currentYear - 120; year--) {
            yearCombo.getItems().add(year);
        }
        yearCombo.setPromptText("Year");
        yearCombo.setVisibleRowCount(10);
        yearCombo.setStyle(StyleHelper.getInputFieldStyle());
        yearCombo.setPrefWidth(100);

        // Update day options based on selected month and year to handle different month
        // lengths and leap years
        Runnable updateDayOptions = () -> {
            if (monthCombo.getValue() != null && yearCombo.getValue() != null) {
                int month = getMonthNumber(monthCombo.getValue());
                int year = yearCombo.getValue();
                // Preserve the currently selected day value
                Integer selectedDay = dayCombo.getValue();
                try {
                    LocalDate testDate = LocalDate.of(year, month, 1);
                    int maxDays = testDate.lengthOfMonth();
                    dayCombo.getItems().clear();
                    for (int day = 1; day <= maxDays; day++) {
                        dayCombo.getItems().add(day);
                    }
                    // Restore the selected day if it's still valid (within the new max days)
                    if (selectedDay != null && selectedDay <= maxDays && selectedDay >= 1) {
                        dayCombo.setValue(selectedDay);
                    } else if (selectedDay != null && selectedDay > maxDays) {
                        // If selected day exceeds max days, set to the last valid day
                        dayCombo.setValue(maxDays);
                    }
                } catch (Exception e) {
                    // If invalid date, use default 31 days
                    if (dayCombo.getItems().size() != 31) {
                        Integer currentSelected = dayCombo.getValue();
                        dayCombo.getItems().clear();
                        for (int day = 1; day <= 31; day++) {
                            dayCombo.getItems().add(day);
                        }
                        // Restore selection if valid
                        if (currentSelected != null && currentSelected >= 1 && currentSelected <= 31) {
                            dayCombo.setValue(currentSelected);
                        }
                    }
                }
            } else {
                // If month or year is not selected, ensure we have all 31 days available
                if (dayCombo.getItems().size() != 31) {
                    Integer currentSelected = dayCombo.getValue();
                    dayCombo.getItems().clear();
                    for (int day = 1; day <= 31; day++) {
                        dayCombo.getItems().add(day);
                    }
                    // Restore selection if valid
                    if (currentSelected != null && currentSelected >= 1 && currentSelected <= 31) {
                        dayCombo.setValue(currentSelected);
                    }
                }
            }
        };

        monthCombo.setOnAction(e -> updateDayOptions.run());
        yearCombo.setOnAction(e -> updateDayOptions.run());

        // Standardize styling
        StyleHelper.styleTextField(nameField);
        StyleHelper.styleTextField(qualificationField);
        StyleHelper.styleTextField(fatherNameField);
        StyleHelper.styleTextField(motherNameField);
        StyleHelper.styleTextField(districtField);
        StyleHelper.styleTextField(panchayatField);
        StyleHelper.styleComboBox(dayCombo);
        StyleHelper.styleComboBox(monthCombo);
        StyleHelper.styleComboBox(yearCombo);

        // Set values if editing existing member
        if (member != null && member.getDateOfBirth() != null) {
            LocalDate dob = member.getDateOfBirth();
            dayCombo.setValue(dob.getDayOfMonth());
            int monthValue = dob.getMonthValue();
            String[] monthNames = { "January", "February", "March", "April", "May", "June",
                    "July", "August", "September", "October", "November", "December" };
            monthCombo.setValue(monthNames[monthValue - 1]);
            yearCombo.setValue(dob.getYear());
            // Update day options after setting values
            updateDayOptions.run();
        }

        HBox dobBox = new HBox(8);
        dobBox.setAlignment(Pos.CENTER_LEFT);
        dobBox.getChildren().addAll(dayCombo, monthCombo, yearCombo);

        // Create searchable ComboBox for address
        ComboBox<House> addressCombo = new ComboBox<>();
        addressCombo.setEditable(true);
        addressCombo.setPromptText("Search or select address...");

        // Load houses from database
        ObservableList<House> allHouses = FXCollections.observableArrayList();
        FilteredList<House> filteredHouses = new FilteredList<>(allHouses, p -> true);
        addressCombo.setItems(filteredHouses);

        // Load houses in background
        new Thread(() -> {
            java.util.List<House> houses = houseDAO.getAll();
            javafx.application.Platform.runLater(() -> {
                allHouses.clear();
                allHouses.addAll(houses);
            });
        }).start();

        // Set converter to display house information
        addressCombo.setConverter(new StringConverter<House>() {
            @Override
            public String toString(House house) {
                if (house == null) {
                    return "";
                }
                return house.getDisplayString();
            }

            @Override
            public House fromString(String string) {
                if (string == null || string.trim().isEmpty()) {
                    return null;
                }
                // Try to find matching house
                for (House house : allHouses) {
                    if (house != null) {
                        String displayString = house.getDisplayString();
                        String address = house.getAddress();
                        if ((displayString != null && displayString.equals(string)) ||
                                (address != null && address.equals(string))) {
                            return house;
                        }
                    }
                }
                return null;
            }
        });

        // Set custom cell factory to display house information in dropdown
        addressCombo.setCellFactory(listView -> new ListCell<House>() {
            @Override
            protected void updateItem(House house, boolean empty) {
                super.updateItem(house, empty);
                if (empty || house == null) {
                    setText(null);
                } else {
                    setText(house.getDisplayString());
                }
            }
        });

        // Set button cell to display selected value
        addressCombo.setButtonCell(new ListCell<House>() {
            @Override
            protected void updateItem(House house, boolean empty) {
                super.updateItem(house, empty);
                if (empty || house == null) {
                    setText(null);
                } else {
                    setText(house.getDisplayString());
                }
            }
        });

        // Handle selection from dropdown - when user clicks on an item
        // Note: Don't interfere with the default selection behavior
        // The converter and button cell will handle the display

        // Enable search/filter functionality
        addressCombo.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            // Don't filter if a value is already selected (user might be editing)
            if (addressCombo.getValue() != null && newValue != null &&
                    newValue.equals(addressCombo.getValue().getDisplayString())) {
                return; // Value matches selected, don't filter
            }

            if (newValue == null || newValue.isEmpty()) {
                filteredHouses.setPredicate(p -> true);
            } else {
                String lowerCaseFilter = newValue.toLowerCase();
                filteredHouses.setPredicate(house -> {
                    if (house == null)
                        return false;
                    String address = house.getAddress() != null ? house.getAddress().toLowerCase() : "";
                    String houseNumber = house.getHouseNumber() != null ? house.getHouseNumber().toLowerCase() : "";
                    String displayString = house.getDisplayString().toLowerCase();
                    return address.contains(lowerCaseFilter) ||
                            houseNumber.contains(lowerCaseFilter) ||
                            displayString.contains(lowerCaseFilter);
                });
            }
        });

        // Ensure value is properly set when user selects from dropdown
        addressCombo.setOnHidden(e -> {
            House selected = addressCombo.getValue();
            if (selected != null) {
                // Ensure editor shows the selected value
                addressCombo.getEditor().setText(selected.getDisplayString());
            }
        });

        // Set selected value if editing existing member
        if (member != null && member.getAddress() != null && !member.getAddress().trim().isEmpty()) {
            String memberAddress = member.getAddress().trim();
            // Try to find matching house
            new Thread(() -> {
                java.util.List<House> houses = houseDAO.getAll();
                javafx.application.Platform.runLater(() -> {
                    for (House house : houses) {
                        if (house.getAddress() != null && house.getAddress().trim().equals(memberAddress)) {
                            addressCombo.setValue(house);
                            break;
                        }
                    }
                    // If no exact match, set the editor text
                    if (addressCombo.getValue() == null) {
                        addressCombo.getEditor().setText(memberAddress);
                    }
                });
            }).start();
        }

        // Add button to add new house
        Button addHouseBtn = new Button("➕ Add New House");
        addHouseBtn.setStyle(StyleHelper.getSecondaryButtonStyle());
        addHouseBtn.setOnAction(e -> {
            // Show house dialog and refresh address list when closed
            Stage houseDialog = new Stage();
            houseDialog.setTitle("Add House");

            VBox houseRoot = new VBox(10);
            houseRoot.setPadding(new Insets(20));
            houseRoot.setPrefWidth(500);
            FormStyler.styleFormDialog(houseRoot);

            Label houseTitleLabel = FormStyler.createFormLabel("Add New House");
            houseTitleLabel.setStyle(
                    "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + StyleHelper.TEXT_GRAY_900 + ";");

            TextArea houseAddressField = new TextArea();
            houseAddressField.setPromptText("Address *");
            houseAddressField.setPrefRowCount(3);
            setMaxLength(houseAddressField, 200);

            TextField houseNumberField = new TextField();
            houseNumberField.setPromptText("House Number");
            setMaxLength(houseNumberField, 50);

            Button saveHouseButton = new Button("Save");
            saveHouseButton.setStyle(
                    "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
            saveHouseButton.setOnMouseEntered(e2 -> saveHouseButton.setStyle(
                    "-fx-background-color: #1d4ed8; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
            saveHouseButton.setOnMouseExited(e2 -> saveHouseButton.setStyle(
                    "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));

            Button cancelHouseButton = new Button("Cancel");
            cancelHouseButton.setStyle(
                    "-fx-background-color: #6b7280; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
            cancelHouseButton.setOnMouseEntered(e2 -> cancelHouseButton.setStyle(
                    "-fx-background-color: #4b5563; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
            cancelHouseButton.setOnMouseExited(e2 -> cancelHouseButton.setStyle(
                    "-fx-background-color: #6b7280; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));

            HBox houseButtonBox = new HBox(10);
            houseButtonBox.setAlignment(Pos.CENTER_RIGHT);
            houseButtonBox.setPadding(new Insets(10, 0, 0, 0));
            houseButtonBox.getChildren().addAll(cancelHouseButton, saveHouseButton);

            cancelHouseButton.setOnAction(e2 -> houseDialog.close());

            saveHouseButton.setOnAction(e2 -> {
                String address = houseAddressField.getText().trim();
                String houseNumber = houseNumberField.getText().trim();

                if (address.isEmpty()) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Validation Error");
                    alert.setContentText("Address is required.");
                    alert.showAndWait();
                    return;
                }

                House newHouse = new House();
                newHouse.setAddress(address);
                newHouse.setHouseNumber(houseNumber.isEmpty() ? null : houseNumber);

                new Thread(() -> {
                    Long newId = houseDAO.create(newHouse);
                    javafx.application.Platform.runLater(() -> {
                        if (newId != null) {
                            // Reload houses list
                            java.util.List<House> houses = houseDAO.getAll();
                            allHouses.clear();
                            allHouses.addAll(houses);
                            loadHouseData(); // refresh main houses table
                            // Select the newly added house
                            for (House house : houses) {
                                if (house.getId().equals(newId)) {
                                    addressCombo.setValue(house);
                                    break;
                                }
                            }
                            houseDialog.close();
                        } else {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Error");
                            alert.setContentText("Failed to save house. Please try again.");
                            alert.show();
                        }
                    });
                }).start();
            });

            houseRoot.getChildren().addAll(
                    houseTitleLabel,
                    FormStyler.createCompactFormField("Address *", houseAddressField),
                    FormStyler.createCompactFormField("House Number", houseNumberField),
                    houseButtonBox);

            Scene houseScene = new Scene(houseRoot, 500, 300);
            houseDialog.setScene(houseScene);
            houseDialog.show();
        });

        // Create HBox for address field with add button
        HBox addressBox = new HBox(8);
        addressBox.setAlignment(Pos.CENTER_LEFT);
        addressBox.getChildren().addAll(addressCombo, addHouseBtn);
        HBox.setHgrow(addressCombo, Priority.ALWAYS);

        TextField mobileField = new TextField();
        mobileField.setPromptText("Mobile");
        setMaxLength(mobileField, 15);
        if (member != null)
            mobileField.setText(member.getMobile());

        ComboBox<String> genderCombo = new ComboBox<>();
        genderCombo.getItems().addAll("M", "F", "OTHER");
        genderCombo.setPromptText("Gender");
        if (member != null && member.getGender() != null) {
            genderCombo.setValue(member.getGender());
        }

        TextField idProofTypeField = new TextField();
        idProofTypeField.setPromptText("ID Proof Type (Aadhar/Passport)");
        setMaxLength(idProofTypeField, 50);
        if (member != null)
            idProofTypeField.setText(member.getIdProofType());

        TextField idProofNoField = new TextField();
        idProofNoField.setPromptText("ID Proof Number");
        setMaxLength(idProofNoField, 30);
        if (member != null)
            idProofNoField.setText(member.getIdProofNo());

        Button saveButton = new Button("Save");
        saveButton.setStyle(StyleHelper.getEditButtonStyle());
        saveButton.setOnMouseEntered(e -> saveButton.setStyle(StyleHelper.getEditButtonHoverStyle()));
        saveButton.setOnMouseExited(e -> saveButton.setStyle(StyleHelper.getEditButtonStyle()));

        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle(StyleHelper.getSecondaryButtonStyle());
        StyleHelper.styleSecondaryButton(cancelButton);
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(8, 0, 0, 0)); // Reduced padding
        buttonBox.getChildren().addAll(cancelButton, saveButton);
        cancelButton.setOnAction(e -> dialog.close());

        saveButton.setOnAction(e -> {
            // Get and trim all field values
            String name = nameField.getText().trim();

            // Validate name is required (database constraint)
            if (name.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Validation Error");
                alert.setContentText("Name is required. Please enter a name.");
                alert.show();
                nameField.requestFocus();
                return;
            }

            String qualification = qualificationField.getText().trim();
            String fatherName = fatherNameField.getText().trim();
            String motherName = motherNameField.getText().trim();
            String district = districtField.getText().trim();
            String panchayat = panchayatField.getText().trim();
            // Get address from ComboBox
            String address = "";
            House selectedHouse = addressCombo.getValue();
            if (selectedHouse != null && selectedHouse.getAddress() != null) {
                // Use the address from selected house
                address = selectedHouse.getAddress();
            } else {
                // If no selection, try to match editor text with a house
                String editorText = addressCombo.getEditor().getText().trim();
                if (!editorText.isEmpty()) {
                    // Try to find matching house from editor text
                    boolean found = false;
                    for (House house : allHouses) {
                        if (house != null && house.getAddress() != null) {
                            if (house.getDisplayString().equals(editorText) ||
                                    house.getAddress().equals(editorText)) {
                                address = house.getAddress();
                                found = true;
                                break;
                            }
                        }
                    }
                    // If no match found, use the editor text as-is (custom address)
                    if (!found) {
                        address = editorText;
                    }
                }
            }
            String mobile = mobileField.getText().trim();
            String gender = genderCombo.getValue();
            String idProofType = idProofTypeField.getText().trim();
            String idProofNo = idProofNoField.getText().trim();

            // Get date of birth from day, month and year combos
            LocalDate dateOfBirth = null;
            if (dayCombo.getValue() != null && monthCombo.getValue() != null && yearCombo.getValue() != null) {
                try {
                    int day = dayCombo.getValue();
                    String monthName = monthCombo.getValue();
                    int year = yearCombo.getValue();
                    int month = getMonthNumber(monthName);
                    dateOfBirth = LocalDate.of(year, month, day);
                } catch (Exception ex) {
                    System.err.println("Error parsing date: " + ex.getMessage());
                    ex.printStackTrace();
                    // Show user-friendly error message
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Invalid Date");
                    alert.setContentText("The selected date is invalid. Please check day, month, and year.");
                    alert.show();
                    return;
                }
            }

            // Check if all fields are empty
            boolean allEmpty = name.isEmpty() && qualification.isEmpty() && fatherName.isEmpty() &&
                    motherName.isEmpty() && district.isEmpty() && panchayat.isEmpty() &&
                    address.isEmpty() && mobile.isEmpty() && gender == null &&
                    idProofType.isEmpty() && idProofNo.isEmpty() && dateOfBirth == null;

            if (allEmpty) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Validation Error");
                alert.setContentText(
                        "Please fill at least one field before saving. Cannot create a record with all empty fields.");
                alert.showAndWait();
                return;
            }

            // Convert empty strings to null to prevent blank values in database
            Member memberData = new Member();
            memberData.setName(name.isEmpty() ? null : name);
            memberData.setQualification(qualification.isEmpty() ? null : qualification);
            memberData.setFatherName(fatherName.isEmpty() ? null : fatherName);
            memberData.setMotherName(motherName.isEmpty() ? null : motherName);
            memberData.setDistrict(district.isEmpty() ? null : district);
            memberData.setPanchayat(panchayat.isEmpty() ? null : panchayat);
            memberData.setDateOfBirth(dateOfBirth);
            memberData.setAddress(address.isEmpty() ? null : address);
            memberData.setMobile(mobile.isEmpty() ? null : mobile);
            memberData.setGender(gender);
            memberData.setIdProofType(idProofType.isEmpty() ? null : idProofType);
            memberData.setIdProofNo(idProofNo.isEmpty() ? null : idProofNo);

            // Final safety check: Ensure at least one field has a value
            boolean hasAnyValue = (memberData.getName() != null && !memberData.getName().isEmpty()) ||
                    (memberData.getQualification() != null && !memberData.getQualification().isEmpty()) ||
                    (memberData.getFatherName() != null && !memberData.getFatherName().isEmpty()) ||
                    (memberData.getMotherName() != null && !memberData.getMotherName().isEmpty()) ||
                    (memberData.getDistrict() != null && !memberData.getDistrict().isEmpty()) ||
                    (memberData.getPanchayat() != null && !memberData.getPanchayat().isEmpty()) ||
                    (memberData.getAddress() != null && !memberData.getAddress().isEmpty()) ||
                    (memberData.getMobile() != null && !memberData.getMobile().isEmpty()) ||
                    memberData.getGender() != null ||
                    (memberData.getIdProofType() != null && !memberData.getIdProofType().isEmpty()) ||
                    (memberData.getIdProofNo() != null && !memberData.getIdProofNo().isEmpty()) ||
                    memberData.getDateOfBirth() != null;

            if (!hasAnyValue) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Validation Error");
                alert.setContentText("Cannot save: All fields are empty. Please fill at least one field.");
                alert.showAndWait();
                return;
            }

            saveButton.setDisable(true);
            saveButton.setText("Saving...");

            // Capture originalMemberId in final variable for use in thread
            final Long memberIdToUse = originalMemberId;

            new Thread(() -> {
                try {
                    boolean success;
                    System.out.println("Save operation started. originalMemberId: " + memberIdToUse);

                    if (memberIdToUse == null) {
                        // Creating new member
                        System.out.println("Creating new member: name=" + memberData.getName());
                        Long newId = memberDAO.create(memberData);
                        success = newId != null;
                        System.out.println("Member creation result: " + success + ", ID: " + newId);
                    } else {
                        // Updating existing member
                        memberData.setId(memberIdToUse);
                        System.out.println("Updating member: id=" + memberIdToUse + ", name=" + memberData.getName());
                        System.out.println("Member data before update - ID: " + memberData.getId() + ", Name: "
                                + memberData.getName());
                        success = memberDAO.update(memberData);
                        System.out.println("Member update result: " + success);

                        if (!success) {
                            System.err.println("Update failed for member ID: " + memberIdToUse);
                            System.err.println(
                                    "Member data: name=" + memberData.getName() + ", id=" + memberData.getId());
                        }
                    }
                    javafx.application.Platform.runLater(() -> {
                        saveButton.setDisable(false);
                        saveButton.setText("Save");
                        if (success) {
                            dialog.close();
                            loadData(); // Reload data to refresh table
                            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                            successAlert.setTitle("Success");
                            successAlert.setHeaderText(null);
                            successAlert.setContentText(
                                    "Member " + (memberIdToUse == null ? "added" : "updated") + " successfully!");
                            successAlert.show();
                        } else {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Error");
                            alert.setHeaderText("Failed to save member");
                            alert.setContentText(
                                    "The member could not be saved. Please check the console for error details and try again.");
                            alert.show();
                        }
                    });
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        saveButton.setDisable(false);
                        saveButton.setText("Save");
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setContentText("Failed to save member: " + ex.getMessage());
                        alert.show();
                        ex.printStackTrace();
                    });
                }
            }).start();
        });

        root.getChildren().addAll(
                titleLabel,
                FormStyler.createCompactFormField("Name *", nameField),
                FormStyler.createCompactFormField("Qualification", qualificationField),
                FormStyler.createCompactFormField("Father's Name", fatherNameField),
                FormStyler.createCompactFormField("Mother's Name", motherNameField),
                FormStyler.createCompactFormField("District", districtField),
                FormStyler.createCompactFormField("Panchayat", panchayatField),
                createAddressFormField("Date of Birth (Day/Month/Year)", dobBox),
                createAddressFormField("Address", addressBox),
                FormStyler.createCompactFormField("Mobile", mobileField),
                FormStyler.createCompactFormField("Gender", genderCombo),
                FormStyler.createCompactFormField("ID Proof Type", idProofTypeField),
                FormStyler.createCompactFormField("ID Proof No", idProofNoField),
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

    private void deleteMember(Member member) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setContentText(
                "Are you sure you want to delete this member?\n\nNote: This will also delete related records in Due Collections and Incomes.");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                new Thread(() -> {
                    boolean success = memberDAO.delete(member.getId());
                    javafx.application.Platform.runLater(() -> {
                        if (success) {
                            loadData();
                            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                            successAlert.setTitle("Success");
                            successAlert.setContentText("Member deleted successfully.");
                            successAlert.show();
                        } else {
                            String errorMessage = memberDAO.getDeleteErrorMessage(member.getId());
                            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                            errorAlert.setTitle("Error");
                            errorAlert.setContentText(errorMessage);
                            errorAlert.show();
                        }
                    });
                }).start();
            }
        });
    }

    private void deleteHouse(House house) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setContentText("Delete this house?");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                new Thread(() -> {
                    boolean success = houseDAO.delete(house.getId());
                    javafx.application.Platform.runLater(() -> {
                        if (success) {
                            loadHouseData();
                        } else {
                            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                            errorAlert.setTitle("Error");
                            errorAlert.setContentText("Failed to delete house.");
                            errorAlert.show();
                        }
                    });
                }).start();
            }
        });
    }

    private void showHouseDialog(House house) {
        Stage dialog = new Stage();
        dialog.setTitle(house == null ? "Add House" : "Edit House");

        VBox root = new VBox(20);
        root.setPadding(new Insets(24));
        root.setPrefWidth(500);
        FormStyler.styleFormDialog(root);

        Label titleLabel = FormStyler.createFormLabel(house == null ? "Add New House" : "Edit House");

        TextArea addressField = new TextArea();
        addressField.setPromptText("Address *");
        addressField.setPrefRowCount(3);
        StyleHelper.styleTextArea(addressField);
        if (house != null)
            addressField.setText(house.getAddress());

        TextField houseNumberField = new TextField();
        houseNumberField.setPromptText("House Number");
        StyleHelper.styleTextField(houseNumberField);
        if (house != null)
            houseNumberField.setText(house.getHouseNumber());

        Button saveButton = new Button("Save");
        saveButton.setStyle(StyleHelper.getEditButtonStyle());
        saveButton.setOnMouseEntered(e -> saveButton.setStyle(StyleHelper.getEditButtonHoverStyle()));
        saveButton.setOnMouseExited(e -> saveButton.setStyle(StyleHelper.getEditButtonStyle()));

        Button cancelButton = new Button("Cancel");
        StyleHelper.styleSecondaryButton(cancelButton);

        HBox buttonBox = new HBox(12);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.getChildren().addAll(cancelButton, saveButton);

        cancelButton.setOnAction(e -> dialog.close());

        saveButton.setOnAction(e -> {
            String address = addressField.getText().trim();
            String houseNumber = houseNumberField.getText().trim();

            if (address.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Validation Error");
                alert.setContentText("Address is required.");
                alert.showAndWait();
                return;
            }

            House houseData = new House();
            houseData.setAddress(address);
            houseData.setHouseNumber(houseNumber.isEmpty() ? null : houseNumber);

            new Thread(() -> {
                boolean success;
                if (house == null) {
                    success = houseDAO.create(houseData) != null;
                } else {
                    houseData.setId(house.getId());
                    success = houseDAO.update(houseData);
                }
                javafx.application.Platform.runLater(() -> {
                    if (success) {
                        loadHouseData();
                        dialog.close();
                    } else {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setContentText("Failed to save house.");
                        alert.show();
                    }
                });
            }).start();
        });

        root.getChildren().addAll(
                titleLabel,
                FormStyler.createCompactFormField("Address *", addressField),
                FormStyler.createCompactFormField("House Number", houseNumberField),
                buttonBox);

        Scene scene = new Scene(root, 500, 320);
        dialog.setScene(scene);
        dialog.show();
    }
}
