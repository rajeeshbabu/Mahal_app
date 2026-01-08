package com.mahal.controller.masjid;

import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.control.ScrollPane;
import javafx.stage.Stage;
import com.mahal.database.MasjidDAO;
import com.mahal.database.CommitteeDAO;
import com.mahal.model.Masjid;
import com.mahal.model.Committee;
import com.mahal.util.StyleHelper;
import com.mahal.util.TableStyler;
import com.mahal.util.FormStyler;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;
import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.StringConverter;

public class MasjidController {
    private VBox view;
    private StackPane contentPane;
    private VBox masjidViewPane;
    private VBox committeeViewPane;
    private TableView<Masjid> masjidTable;
    private TableView<Committee> committeeTable;
    private ObservableList<Masjid> masjidList;
    private ObservableList<Committee> committeeList;
    private MasjidDAO masjidDAO;
    private CommitteeDAO committeeDAO;
    private Label recordCountLabel;
    private Label committeeCountLabel;
    private FilteredList<Masjid> filteredMasjids;
    private FilteredList<Committee> filteredCommittees;

    private void setMaxLength(TextInputControl control, int maxLen) {
        control.setTextFormatter(new TextFormatter<String>(change -> {
            String newText = change.getControlNewText();
            if (newText.length() <= maxLen) {
                return change;
            }
            return null;
        }));
    }

    public MasjidController() {
        masjidDAO = new MasjidDAO();
        committeeDAO = new CommitteeDAO();
        masjidList = FXCollections.observableArrayList();
        committeeList = FXCollections.observableArrayList();

        filteredMasjids = new FilteredList<>(masjidList, p -> true);
        filteredCommittees = new FilteredList<>(committeeList, p -> true);

        createView();
        loadMasjidData();
        loadCommitteeData();
    }

    public VBox getView() {
        return view;
    }

    private void createView() {
        view = new VBox(24);
        view.setPadding(new Insets(24));
        view.setStyle("-fx-background-color: #f8fafc;");

        // Main Card
        VBox card = new VBox(24);
        card.setStyle(StyleHelper.getCardStyle());
        card.setPadding(new Insets(32));

        // 1. Title: Masjid Management
        Label title = new Label("Masjid Management");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");

        // 2. Pill Switcher
        HBox pillSwitcher = new HBox(0);
        pillSwitcher.setAlignment(Pos.CENTER_LEFT);
        pillSwitcher.setStyle(StyleHelper.getPillSwitcherContainerStyle());
        pillSwitcher.setMaxWidth(Region.USE_PREF_SIZE);

        Button masjidBtn = new Button("Masjid");
        Button committeeBtn = new Button("Committee");

        updatePillButtonStyle(masjidBtn, true);
        updatePillButtonStyle(committeeBtn, false);

        pillSwitcher.getChildren().addAll(masjidBtn, committeeBtn);

        // 3. Content Area
        contentPane = new StackPane();
        masjidViewPane = createMasjidView();
        committeeViewPane = createCommitteeView();
        contentPane.getChildren().setAll(masjidViewPane);

        masjidBtn.setOnAction(e -> {
            updatePillButtonStyle(masjidBtn, true);
            updatePillButtonStyle(committeeBtn, false);
            contentPane.getChildren().setAll(masjidViewPane);
        });

        committeeBtn.setOnAction(e -> {
            updatePillButtonStyle(masjidBtn, false);
            updatePillButtonStyle(committeeBtn, true);
            contentPane.getChildren().setAll(committeeViewPane);
        });

        card.getChildren().addAll(title, pillSwitcher, contentPane);
        view.getChildren().add(card);
    }

    private void updatePillButtonStyle(Button btn, boolean active) {
        btn.setStyle(StyleHelper.getPillButtonStyle(active));
    }

    private VBox createMasjidView() {
        VBox container = new VBox(20);

        // Action Row: Add Button and Record Count
        HBox actionRow = new HBox(12);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        Button addBtn = new Button("Add Masjid");
        addBtn.setStyle(StyleHelper.getActionPillButtonStyle());
        addBtn.setOnAction(e -> showMasjidDialog(null));

        TextField searchField = new TextField();
        searchField.setPromptText("Search masjid...");
        StyleHelper.styleTextField(searchField);
        searchField.setPrefWidth(250);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredMasjids.setPredicate(masjid -> {
                if (newValue == null || newValue.isEmpty())
                    return true;
                String lowerCaseFilter = newValue.toLowerCase();
                if (masjid.getName().toLowerCase().contains(lowerCaseFilter))
                    return true;
                if (masjid.getAddress().toLowerCase().contains(lowerCaseFilter))
                    return true;
                if (masjid.getAbbreviation() != null
                        && masjid.getAbbreviation().toLowerCase().contains(lowerCaseFilter))
                    return true;
                return false;
            });
            updateMasjidCount();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        recordCountLabel = new Label("Showing 0 of 0 masjid");
        recordCountLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px; -fx-font-weight: 500;");
        updateMasjidCount();

        actionRow.getChildren().addAll(addBtn, searchField, spacer, recordCountLabel);

        // Table
        masjidTable = new TableView<>();
        TableStyler.applyModernStyling(masjidTable);
        masjidTable.setFixedCellSize(-1);

        TableColumn<Masjid, String> nameCol = new TableColumn<>("NAME");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableStyler.styleTableColumn(nameCol);

        TableColumn<Masjid, String> abbrevCol = new TableColumn<>("ABBREVIATION");
        abbrevCol.setCellValueFactory(new PropertyValueFactory<>("abbreviation"));
        TableStyler.styleTableColumn(abbrevCol);

        TableColumn<Masjid, String> addressCol = new TableColumn<>("ADDRESS");
        addressCol.setCellValueFactory(new PropertyValueFactory<>("address"));
        TableStyler.styleTableColumn(addressCol);

        TableColumn<Masjid, String> waqfCol = new TableColumn<>("WAQF NO");
        waqfCol.setCellValueFactory(new PropertyValueFactory<>("waqfBoardNo"));
        TableStyler.styleTableColumn(waqfCol);

        TableColumn<Masjid, String> stateCol = new TableColumn<>("STATE");
        stateCol.setCellValueFactory(new PropertyValueFactory<>("state"));
        TableStyler.styleTableColumn(stateCol);

        TableColumn<Masjid, String> mobileCol = new TableColumn<>("MOBILE");
        mobileCol.setCellValueFactory(new PropertyValueFactory<>("mobile"));
        TableStyler.styleTableColumn(mobileCol);

        TableColumn<Masjid, String> regNoCol = new TableColumn<>("REG NO");
        regNoCol.setCellValueFactory(new PropertyValueFactory<>("registrationNo"));
        TableStyler.styleTableColumn(regNoCol);

        TableColumn<Masjid, String> actionsCol = new TableColumn<>("ACTIONS");
        actionsCol.setMinWidth(120);
        actionsCol.setCellFactory(param -> new TableCell<Masjid, String>() {
            private final Hyperlink editLink = new Hyperlink("Edit");
            private final Hyperlink deleteLink = new Hyperlink("Delete");
            private final HBox container = new HBox(12, editLink, deleteLink);

            {
                container.setAlignment(Pos.CENTER);
                editLink.setStyle(
                        "-fx-text-fill: #2563eb; -fx-font-weight: 600; -fx-font-size: 13px; -fx-underline: false;");
                deleteLink.setStyle(
                        "-fx-text-fill: #ef4444; -fx-font-weight: 600; -fx-font-size: 13px; -fx-underline: false;");

                editLink.setOnAction(e -> {
                    Masjid masjid = getTableView().getItems().get(getIndex());
                    showMasjidDialog(masjid);
                });

                deleteLink.setOnAction(e -> {
                    Masjid masjid = getTableView().getItems().get(getIndex());
                    deleteMasjid(masjid);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(container);
                }
            }
        });
        TableStyler.styleTableColumn(actionsCol);

        masjidTable.getColumns().addAll(nameCol, abbrevCol, addressCol, waqfCol, stateCol, mobileCol, regNoCol,
                actionsCol);
        SortedList<Masjid> sortedData = new SortedList<>(filteredMasjids);
        sortedData.comparatorProperty().bind(masjidTable.comparatorProperty());
        masjidTable.setItems(sortedData);
        masjidTable.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(masjidTable, Priority.ALWAYS);

        container.getChildren().addAll(actionRow, masjidTable);
        return container;
    }

    private VBox createCommitteeView() {
        VBox container = new VBox(20);

        // Action Row
        HBox actionRow = new HBox(12);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        Button addBtn = new Button("Add Committee Member");
        addBtn.setStyle(StyleHelper.getActionPillButtonStyle());
        addBtn.setOnAction(e -> showCommitteeDialog(null));

        TextField searchField = new TextField();
        searchField.setPromptText("Search members...");
        StyleHelper.styleTextField(searchField);
        searchField.setPrefWidth(250);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredCommittees.setPredicate(member -> {
                if (newValue == null || newValue.isEmpty())
                    return true;
                String lowerCaseFilter = newValue.toLowerCase();
                if (member.getMemberName().toLowerCase().contains(lowerCaseFilter))
                    return true;
                if (member.getDesignation().toLowerCase().contains(lowerCaseFilter))
                    return true;
                if (member.getMasjidName() != null && member.getMasjidName().toLowerCase().contains(lowerCaseFilter))
                    return true;
                return false;
            });
            updateCommitteeCount();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        committeeCountLabel = new Label("Showing 0 members");
        committeeCountLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px; -fx-font-weight: 500;");
        updateCommitteeCount();

        actionRow.getChildren().addAll(addBtn, searchField, spacer, committeeCountLabel);

        // Table
        committeeTable = new TableView<>();
        TableStyler.applyModernStyling(committeeTable);

        TableColumn<Committee, String> nameCol = new TableColumn<>("MEMBER NAME");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("memberName"));
        TableStyler.styleTableColumn(nameCol);

        TableColumn<Committee, String> mobileCol = new TableColumn<>("MOBILE");
        mobileCol.setCellValueFactory(new PropertyValueFactory<>("mobile"));
        TableStyler.styleTableColumn(mobileCol);

        TableColumn<Committee, String> designationCol = new TableColumn<>("DESIGNATION");
        designationCol.setCellValueFactory(new PropertyValueFactory<>("designation"));
        TableStyler.styleTableColumn(designationCol);

        TableColumn<Committee, String> masjidCol = new TableColumn<>("MASJID");
        masjidCol.setCellValueFactory(new PropertyValueFactory<>("masjidName"));
        TableStyler.styleTableColumn(masjidCol);

        TableColumn<Committee, String> actionsCol = new TableColumn<>("ACTIONS");
        actionsCol.setMinWidth(120);
        actionsCol.setCellFactory(param -> new TableCell<Committee, String>() {
            private final Hyperlink editLink = new Hyperlink("Edit");
            private final Hyperlink deleteLink = new Hyperlink("Delete");
            private final HBox container = new HBox(12, editLink, deleteLink);

            {
                container.setAlignment(Pos.CENTER);
                editLink.setStyle(
                        "-fx-text-fill: #2563eb; -fx-font-weight: 600; -fx-font-size: 13px; -fx-underline: false;");
                deleteLink.setStyle(
                        "-fx-text-fill: #ef4444; -fx-font-weight: 600; -fx-font-size: 13px; -fx-underline: false;");

                editLink.setOnAction(e -> {
                    Committee committee = getTableView().getItems().get(getIndex());
                    showCommitteeDialog(committee);
                });

                deleteLink.setOnAction(e -> {
                    Committee committee = getTableView().getItems().get(getIndex());
                    deleteCommittee(committee);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(container);
                }
            }
        });
        TableStyler.styleTableColumn(actionsCol);

        committeeTable.getColumns().addAll(nameCol, mobileCol, designationCol, masjidCol, actionsCol);
        SortedList<Committee> sortedData = new SortedList<>(filteredCommittees);
        sortedData.comparatorProperty().bind(committeeTable.comparatorProperty());
        committeeTable.setItems(sortedData);
        committeeTable.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(committeeTable, Priority.ALWAYS);

        container.getChildren().addAll(actionRow, committeeTable);
        return container;
    }

    private void updateMasjidCount() {
        if (recordCountLabel != null) {
            int count = filteredMasjids.size();
            int total = masjidList.size();
            recordCountLabel.setText("Showing " + count + " of " + total + " masjid");
        }
    }

    private void updateCommitteeCount() {
        if (committeeCountLabel != null) {
            int count = filteredCommittees.size();
            int total = committeeList.size();
            committeeCountLabel.setText("Showing " + count + " of " + total + " members");
        }
    }

    private void loadMasjidData() {
        new Thread(() -> {
            java.util.List<Masjid> masjids = masjidDAO.getAll();
            javafx.application.Platform.runLater(() -> {
                masjidList.clear();
                masjidList.addAll(masjids);
                updateMasjidCount();
            });
        }).start();
    }

    private void loadCommitteeData() {
        new Thread(() -> {
            java.util.List<Committee> committees = committeeDAO.getAll();
            javafx.application.Platform.runLater(() -> {
                committeeList.clear();
                committeeList.addAll(committees);
                updateCommitteeCount();
            });
        }).start();
    }

    private void showMasjidDialog(Masjid masjid) {
        Stage dialog = new Stage();
        dialog.setTitle(masjid == null ? "Add New Masjid" : "Edit Masjid");

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label(masjid == null ? "Add New Masjid" : "Edit Masjid");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        TextField nameField = new TextField();
        TextField abbrevField = new TextField();
        TextField addressField = new TextField();
        TextField waqfField = new TextField();
        TextField stateField = new TextField();
        TextField emailField = new TextField();
        TextField mobileField = new TextField();
        TextField regNoField = new TextField();

        if (masjid != null) {
            nameField.setText(masjid.getName());
            abbrevField.setText(masjid.getAbbreviation());
            addressField.setText(masjid.getAddress());
            waqfField.setText(masjid.getWaqfBoardNo());
            stateField.setText(masjid.getState());
            emailField.setText(masjid.getEmail());
            mobileField.setText(masjid.getMobile());
            regNoField.setText(masjid.getRegistrationNo());
        }

        setMaxLength(nameField, 100);
        setMaxLength(abbrevField, 10);
        setMaxLength(addressField, 255);
        setMaxLength(waqfField, 50);
        setMaxLength(stateField, 50);
        setMaxLength(emailField, 100);
        setMaxLength(mobileField, 15);
        setMaxLength(regNoField, 50);

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        Button saveBtn = new Button("Save");
        saveBtn.setStyle(StyleHelper.getEditButtonStyle());
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(StyleHelper.getSecondaryButtonStyle());

        buttonBox.getChildren().addAll(cancelBtn, saveBtn);

        cancelBtn.setOnAction(e -> dialog.close());

        saveBtn.setOnAction(e -> {
            Masjid m = masjid == null ? new Masjid() : masjid;
            m.setName(nameField.getText());
            m.setAbbreviation(abbrevField.getText());
            m.setAddress(addressField.getText());
            m.setWaqfBoardNo(waqfField.getText());
            m.setState(stateField.getText());
            m.setEmail(emailField.getText());
            m.setMobile(mobileField.getText());
            m.setRegistrationNo(regNoField.getText());
            if (masjid == null)
                m.setStatus("active");

            saveBtn.setDisable(true);
            saveBtn.setText("Saving...");

            new Thread(() -> {
                boolean isEdit = masjid != null;
                System.out.println("Processing Masjid: " + (isEdit ? "UPDATE (ID: " + m.getId() + ")" : "CREATE"));
                boolean success = !isEdit ? masjidDAO.create(m) != null : masjidDAO.update(m);
                javafx.application.Platform.runLater(() -> {
                    if (success) {
                        dialog.close();
                        loadMasjidData();
                    } else {
                        saveBtn.setDisable(false);
                        saveBtn.setText("Save");
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setContentText("Failed to save masjid. Please try again.");
                        alert.show();
                    }
                });
            }).start();
        });

        root.getChildren().addAll(
                titleLabel,
                FormStyler.createCompactFormField("Masjid Name *", nameField),
                FormStyler.createCompactFormField("Abbreviation", abbrevField),
                FormStyler.createCompactFormField("Address", addressField),
                FormStyler.createCompactFormField("Waqf Board No", waqfField),
                FormStyler.createCompactFormField("State", stateField),
                FormStyler.createCompactFormField("Email", emailField),
                FormStyler.createCompactFormField("Mobile", mobileField),
                FormStyler.createCompactFormField("Registration No", regNoField),
                buttonBox);

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPrefViewportHeight(500);

        Scene scene = new Scene(scrollPane, 600, 500);
        dialog.setScene(scene);
        dialog.show();
    }

    private void showCommitteeDialog(Committee committee) {
        Stage dialog = new Stage();
        dialog.setTitle(committee == null ? "Add Committee Member" : "Edit Committee Member");

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label(committee == null ? "Add Committee Member" : "Edit Committee Member");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        TextField nameField = new TextField();
        TextField mobileField = new TextField();
        TextField designationField = new TextField();
        TextField otherDetailsField = new TextField();
        ComboBox<Masjid> masjidCombo = new ComboBox<>();
        masjidCombo.setItems(masjidList);
        masjidCombo.setPromptText("Select Masjid");

        masjidCombo.setConverter(new StringConverter<Masjid>() {
            @Override
            public String toString(Masjid masjid) {
                return masjid == null ? "" : masjid.getName();
            }

            @Override
            public Masjid fromString(String string) {
                return null; // Not needed for a non-editable ComboBox
            }
        });

        if (committee != null) {
            nameField.setText(committee.getMemberName());
            mobileField.setText(committee.getMobile());
            designationField.setText(committee.getDesignation());
            otherDetailsField.setText(committee.getOtherDetails());
            for (Masjid m : masjidList) {
                if (m.getId().equals(committee.getMasjidId())) {
                    masjidCombo.setValue(m);
                    break;
                }
            }
        }

        setMaxLength(nameField, 100);
        setMaxLength(mobileField, 15);
        setMaxLength(designationField, 100);
        setMaxLength(otherDetailsField, 255);

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        Button saveBtn = new Button("Save");
        saveBtn.setStyle(StyleHelper.getEditButtonStyle());
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(StyleHelper.getSecondaryButtonStyle());

        buttonBox.getChildren().addAll(cancelBtn, saveBtn);

        cancelBtn.setOnAction(e -> dialog.close());

        saveBtn.setOnAction(e -> {
            Committee c = committee == null ? new Committee() : committee;
            c.setMemberName(nameField.getText());
            c.setMobile(mobileField.getText());
            c.setDesignation(designationField.getText());
            c.setOtherDetails(otherDetailsField.getText());
            if (masjidCombo.getValue() != null) {
                c.setMasjidId(masjidCombo.getValue().getId());
                c.setMasjidName(masjidCombo.getValue().getName());
            }

            saveBtn.setDisable(true);
            saveBtn.setText("Saving...");

            new Thread(() -> {
                boolean isEdit = committee != null;
                System.out.println(
                        "Processing Committee Member: " + (isEdit ? "UPDATE (ID: " + c.getId() + ")" : "CREATE"));
                boolean success = !isEdit ? committeeDAO.create(c) != null : committeeDAO.update(c);
                javafx.application.Platform.runLater(() -> {
                    if (success) {
                        dialog.close();
                        loadCommitteeData();
                    } else {
                        saveBtn.setDisable(false);
                        saveBtn.setText("Save");
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setContentText("Failed to save committee member. Please try again.");
                        alert.show();
                    }
                });
            }).start();
        });

        root.getChildren().addAll(
                titleLabel,
                FormStyler.createCompactFormField("Member Name *", nameField),
                FormStyler.createCompactFormField("Mobile", mobileField),
                FormStyler.createCompactFormField("Designation", designationField),
                FormStyler.createCompactFormField("Masjid", masjidCombo),
                FormStyler.createCompactFormField("Other Details", otherDetailsField),
                buttonBox);

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPrefViewportWidth(640);
        scrollPane.setPrefViewportHeight(600);

        Scene scene = new Scene(scrollPane, 640, 600);
        dialog.setScene(scene);
        dialog.show();
    }

    private void deleteMasjid(Masjid masjid) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setContentText("Are you sure you want to delete this masjid?");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                new Thread(() -> {
                    boolean success = masjidDAO.delete(masjid.getId());
                    javafx.application.Platform.runLater(() -> {
                        if (success) {
                            loadMasjidData();
                            loadCommitteeData();
                        } else {
                            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                            errorAlert.setTitle("Error");
                            errorAlert.setContentText("Failed to delete masjid.");
                            errorAlert.show();
                        }
                    });
                }).start();
            }
        });
    }

    private void deleteCommittee(Committee committee) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setContentText("Are you sure you want to delete this committee member?");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                new Thread(() -> {
                    boolean success = committeeDAO.delete(committee.getId());
                    javafx.application.Platform.runLater(() -> {
                        if (success) {
                            loadCommitteeData();
                        } else {
                            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                            errorAlert.setTitle("Error");
                            errorAlert.setContentText("Failed to delete committee member.");
                            errorAlert.show();
                        }
                    });
                }).start();
            }
        });
    }
}
