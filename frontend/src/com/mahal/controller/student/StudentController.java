package com.mahal.controller.student;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import com.mahal.database.StudentDAO;
import com.mahal.model.Student;
import com.mahal.util.StyleHelper;
import com.mahal.util.TableStyler;
import com.mahal.util.FormStyler;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;
import javafx.geometry.Pos;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.mahal.util.EventBus;
import javafx.application.Platform;

public class StudentController {
    private VBox view;
    private TableView<Student> studentTable;
    private ObservableList<Student> studentList;
    private StudentDAO studentDAO;
    private TextField searchField;

    private void setMaxLength(TextInputControl control, int maxLen) {
        control.setTextFormatter(new TextFormatter<String>(change -> {
            String newText = change.getControlNewText();
            if (newText.length() <= maxLen) {
                return change;
            }
            return null;
        }));
    }

    public StudentController() {
        this.studentDAO = new StudentDAO();
        this.studentList = FXCollections.observableArrayList();
        createView();
        loadStudentData();

        // Subscribe to sync events
        EventBus.getInstance().subscribe("students", event -> Platform.runLater(this::loadStudentData));
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
        Label titleLabel = new Label("Student Management");
        titleLabel.setStyle(StyleHelper.getTitleStyle());
        Label subtitleLabel = new Label("Manage student records, admissions and information");
        subtitleLabel.setStyle(StyleHelper.getSubtitleStyle());
        header.getChildren().addAll(titleLabel, subtitleLabel);

        // Student View
        VBox studentView = createStudentView();

        mainCard.getChildren().addAll(header, studentView);
        view.getChildren().add(mainCard);
    }

    private VBox createStudentView() {
        VBox studentView = new VBox(20);

        // Action Row: Search and Add Button
        HBox actionRow = new HBox(15);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        searchField = new TextField();
        searchField.setPromptText("Search by name, course, admission number or mobile...");
        searchField.setPrefWidth(350);
        StyleHelper.styleTextField(searchField);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addButton = new Button("Add Student");
        addButton.setStyle(StyleHelper.getActionPillButtonStyle());
        addButton.setOnAction(e -> showStudentDialog(null));

        actionRow.getChildren().addAll(searchField, spacer, addButton);

        studentTable = new TableView<>();
        TableStyler.applyModernStyling(studentTable);

        TableColumn<Student, String> nameCol = new TableColumn<>("NAME");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableStyler.styleTableColumn(nameCol);

        TableColumn<Student, String> courseCol = new TableColumn<>("COURSE");
        courseCol.setCellValueFactory(new PropertyValueFactory<>("course"));
        TableStyler.styleTableColumn(courseCol);

        TableColumn<Student, String> admissionNumberCol = new TableColumn<>("ADMISSION NO.");
        admissionNumberCol.setCellValueFactory(new PropertyValueFactory<>("admissionNumber"));
        TableStyler.styleTableColumn(admissionNumberCol);

        TableColumn<Student, String> admissionDateCol = new TableColumn<>("ADMISSION DATE");
        admissionDateCol.setCellValueFactory(cell -> {
            LocalDate date = cell.getValue().getAdmissionDate();
            return new javafx.beans.property.SimpleStringProperty(
                    date != null ? date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) : "-");
        });
        TableStyler.styleTableColumn(admissionDateCol);

        TableColumn<Student, String> mobileCol = new TableColumn<>("MOBILE");
        mobileCol.setCellValueFactory(new PropertyValueFactory<>("mobile"));
        TableStyler.styleTableColumn(mobileCol);

        TableColumn<Student, String> emailCol = new TableColumn<>("EMAIL");
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        TableStyler.styleTableColumn(emailCol);

        TableColumn<Student, String> addressCol = new TableColumn<>("ADDRESS");
        addressCol.setCellValueFactory(new PropertyValueFactory<>("address"));
        TableStyler.styleTableColumn(addressCol);

        TableColumn<Student, String> actionsCol = new TableColumn<>("ACTIONS");
        actionsCol.setCellFactory(param -> new TableCell<Student, String>() {
            private final Hyperlink editLink = new Hyperlink("Edit");
            private final Hyperlink deleteLink = new Hyperlink("Delete");

            {
                editLink.setStyle("-fx-text-fill: #2563eb; -fx-font-weight: bold;");
                deleteLink.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");

                editLink.setOnAction(e -> {
                    Student student = getTableView().getItems().get(getIndex());
                    if (student != null)
                        showStudentDialog(studentDAO.getById(student.getId()));
                });

                deleteLink.setOnAction(e -> {
                    deleteStudent(getTableView().getItems().get(getIndex()));
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

        studentTable.getColumns().addAll(nameCol, courseCol, admissionNumberCol, admissionDateCol, mobileCol, emailCol, addressCol, actionsCol);

        // Filtered List Implementation
        FilteredList<Student> filteredData = new FilteredList<>(studentList, p -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(student -> {
                if (newValue == null || newValue.isEmpty())
                    return true;
                String lowerCaseFilter = newValue.toLowerCase();
                if (student.getName() != null && student.getName().toLowerCase().contains(lowerCaseFilter))
                    return true;
                if (student.getCourse() != null && student.getCourse().toLowerCase().contains(lowerCaseFilter))
                    return true;
                if (student.getAdmissionNumber() != null && student.getAdmissionNumber().toLowerCase().contains(lowerCaseFilter))
                    return true;
                if (student.getMobile() != null && student.getMobile().toLowerCase().contains(lowerCaseFilter))
                    return true;
                if (student.getEmail() != null && student.getEmail().toLowerCase().contains(lowerCaseFilter))
                    return true;
                return false;
            });
        });

        SortedList<Student> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(studentTable.comparatorProperty());
        studentTable.setItems(sortedData);

        studentView.getChildren().addAll(actionRow, studentTable);
        return studentView;
    }

    private void loadStudentData() {
        new Thread(() -> {
            try {
                java.util.List<Student> students = studentDAO.getAll();
                System.out.println("loadStudentData: Retrieved " + students.size() + " student records from database");
                javafx.application.Platform.runLater(() -> {
                    studentList.clear();
                    studentList.addAll(students);
                    System.out.println("loadStudentData: Added " + students.size() + " records to table (studentList size: "
                            + studentList.size() + ")");
                });
            } catch (Exception e) {
                System.err.println("Error loading student data: " + e.getMessage());
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setContentText("Failed to load student data: " + e.getMessage());
                    alert.show();
                });
            }
        }).start();
    }

    private void showStudentDialog(Student student) {
        Stage dialog = new Stage();
        dialog.setTitle(student == null ? "Add Student" : "Edit Student");

        // Store the original student ID for editing
        final Long originalStudentId = (student != null) ? student.getId() : null;

        VBox root = new VBox(8);
        root.setPadding(new Insets(10));
        root.setPrefWidth(600);
        root.setPrefHeight(650);
        FormStyler.styleFormDialog(root);

        Label titleLabel = FormStyler.createFormLabel("Student Details");
        titleLabel.setStyle(
                "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + StyleHelper.TEXT_GRAY_900 + ";");

        TextField nameField = new TextField();
        nameField.setPromptText("Name *");
        StyleHelper.styleTextField(nameField);
        setMaxLength(nameField, 100);
        if (student != null && student.getName() != null) {
            nameField.setText(student.getName());
        }

        TextField courseField = new TextField();
        courseField.setPromptText("Course");
        StyleHelper.styleTextField(courseField);
        setMaxLength(courseField, 100);
        if (student != null && student.getCourse() != null) {
            courseField.setText(student.getCourse());
        }

        TextField admissionNumberField = new TextField();
        admissionNumberField.setPromptText("Admission Number");
        StyleHelper.styleTextField(admissionNumberField);
        setMaxLength(admissionNumberField, 50);
        if (student != null && student.getAdmissionNumber() != null) {
            admissionNumberField.setText(student.getAdmissionNumber());
        }

        DatePicker admissionDatePicker = new DatePicker();
        StyleHelper.styleDatePicker(admissionDatePicker);
        if (student != null && student.getAdmissionDate() != null) {
            admissionDatePicker.setValue(student.getAdmissionDate());
        }

        TextField mobileField = new TextField();
        mobileField.setPromptText("Mobile");
        StyleHelper.styleTextField(mobileField);
        setMaxLength(mobileField, 15);
        if (student != null && student.getMobile() != null) {
            mobileField.setText(student.getMobile());
        }

        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        StyleHelper.styleTextField(emailField);
        setMaxLength(emailField, 100);
        if (student != null && student.getEmail() != null) {
            emailField.setText(student.getEmail());
        }

        TextArea addressField = new TextArea();
        addressField.setPromptText("Address");
        addressField.setPrefRowCount(2);
        addressField.setPrefHeight(60);
        StyleHelper.styleTextArea(addressField);
        setMaxLength(addressField, 200);
        if (student != null && student.getAddress() != null) {
            addressField.setText(student.getAddress());
        }

        TextField fatherNameField = new TextField();
        fatherNameField.setPromptText("Father's Name");
        StyleHelper.styleTextField(fatherNameField);
        setMaxLength(fatherNameField, 100);
        if (student != null && student.getFatherName() != null) {
            fatherNameField.setText(student.getFatherName());
        }

        TextField motherNameField = new TextField();
        motherNameField.setPromptText("Mother's Name");
        StyleHelper.styleTextField(motherNameField);
        setMaxLength(motherNameField, 100);
        if (student != null && student.getMotherName() != null) {
            motherNameField.setText(student.getMotherName());
        }

        TextField guardianMobileField = new TextField();
        guardianMobileField.setPromptText("Guardian Mobile");
        StyleHelper.styleTextField(guardianMobileField);
        setMaxLength(guardianMobileField, 15);
        if (student != null && student.getGuardianMobile() != null) {
            guardianMobileField.setText(student.getGuardianMobile());
        }

        TextArea notesField = new TextArea();
        notesField.setPromptText("Notes");
        notesField.setPrefRowCount(2);
        notesField.setPrefHeight(60);
        StyleHelper.styleTextArea(notesField);
        setMaxLength(notesField, 500);
        if (student != null && student.getNotes() != null) {
            notesField.setText(student.getNotes());
        }

        Button saveButton = new Button("Save");
        saveButton.setStyle(StyleHelper.getEditButtonStyle());

        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle(StyleHelper.getSecondaryButtonStyle());
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(8, 0, 0, 0));
        buttonBox.getChildren().addAll(cancelButton, saveButton);
        cancelButton.setOnAction(e -> dialog.close());

        saveButton.setOnAction(e -> {
            // Get and trim all field values
            String name = nameField.getText().trim();
            String course = courseField.getText().trim();
            String admissionNumber = admissionNumberField.getText().trim();
            String mobile = mobileField.getText().trim();
            String email = emailField.getText().trim();
            String address = addressField.getText().trim();
            String fatherName = fatherNameField.getText().trim();
            String motherName = motherNameField.getText().trim();
            String guardianMobile = guardianMobileField.getText().trim();
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

            // Convert empty strings to null to prevent blank values in database
            Student studentData = new Student();
            studentData.setName(name);
            studentData.setCourse(course.isEmpty() ? null : course);
            studentData.setAdmissionNumber(admissionNumber.isEmpty() ? null : admissionNumber);
            studentData.setAdmissionDate(admissionDatePicker.getValue());
            studentData.setMobile(mobile.isEmpty() ? null : mobile);
            studentData.setEmail(email.isEmpty() ? null : email);
            studentData.setAddress(address.isEmpty() ? null : address);
            studentData.setFatherName(fatherName.isEmpty() ? null : fatherName);
            studentData.setMotherName(motherName.isEmpty() ? null : motherName);
            studentData.setGuardianMobile(guardianMobile.isEmpty() ? null : guardianMobile);
            studentData.setNotes(notes.isEmpty() ? null : notes);

            // Disable button during save
            saveButton.setDisable(true);
            saveButton.setText("Saving...");

            new Thread(() -> {
                try {
                    boolean success;
                    if (student == null) {
                        Long newId = studentDAO.create(studentData);
                        success = newId != null;
                    } else {
                        if (originalStudentId == null) {
                            javafx.application.Platform.runLater(() -> {
                                saveButton.setDisable(false);
                                saveButton.setText("Save");
                                Alert alert = new Alert(Alert.AlertType.ERROR);
                                alert.setTitle("Error");
                                alert.setContentText("Cannot update: Student ID is missing. Please try editing again.");
                                alert.show();
                            });
                            return;
                        }
                        studentData.setId(originalStudentId);
                        System.out.println("Updating student with ID: " + studentData.getId());
                        success = studentDAO.update(studentData);
                        System.out.println("Update result: " + success);

                        if (!success) {
                            System.err.println("Update failed for student ID: " + originalStudentId);
                            System.err.println("Student data: name=" + studentData.getName() + ", course="
                                    + studentData.getCourse());
                        }
                    }
                    javafx.application.Platform.runLater(() -> {
                        saveButton.setDisable(false);
                        saveButton.setText("Save");
                        if (success) {
                            dialog.close();
                            loadStudentData();
                            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                            successAlert.setTitle("Success");
                            successAlert.setHeaderText(null);
                            successAlert.setContentText(
                                    "Student " + (student == null ? "created" : "updated") + " successfully!");
                            successAlert.showAndWait();
                        } else {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Error");
                            alert.setHeaderText("Failed to " + (student == null ? "create" : "update") + " student");
                            alert.setContentText(
                                    "The student record was not saved. Please check the console for error details and try again.\n\nMake sure all required fields are filled correctly.");
                            alert.show();
                        }
                    });
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        saveButton.setDisable(false);
                        saveButton.setText("Save");
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setContentText("Failed to save student: " + ex.getMessage());
                        alert.show();
                        ex.printStackTrace();
                    });
                }
            }).start();
        });

        root.getChildren().addAll(
                titleLabel,
                FormStyler.createCompactFormField("Name *", nameField),
                FormStyler.createCompactFormField("Course", courseField),
                FormStyler.createCompactFormField("Admission Number", admissionNumberField),
                FormStyler.createCompactFormField("Admission Date", admissionDatePicker),
                FormStyler.createCompactFormField("Mobile", mobileField),
                FormStyler.createCompactFormField("Email", emailField),
                FormStyler.createCompactFormField("Address", addressField),
                FormStyler.createCompactFormField("Father's Name", fatherNameField),
                FormStyler.createCompactFormField("Mother's Name", motherNameField),
                FormStyler.createCompactFormField("Guardian Mobile", guardianMobileField),
                FormStyler.createCompactFormField("Notes", notesField),
                buttonBox);

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPrefViewportWidth(640);
        scrollPane.setPrefViewportHeight(700);

        Scene scene = new Scene(scrollPane, 640, 700);
        dialog.setScene(scene);
        try {
            if (studentTable.getScene() != null && studentTable.getScene().getWindow() != null) {
                dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
                dialog.initOwner(studentTable.getScene().getWindow());
            }
        } catch (Exception ex) {
            // If we can't set the owner, continue without it
        }
        dialog.show();
    }

    private void deleteStudent(Student student) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setContentText("Are you sure you want to delete this student?");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                new Thread(() -> {
                    boolean success = studentDAO.delete(student.getId());
                    javafx.application.Platform.runLater(() -> {
                        if (success) {
                            loadStudentData();
                        } else {
                            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                            errorAlert.setTitle("Error");
                            errorAlert.setContentText("Failed to delete student.");
                            errorAlert.show();
                        }
                    });
                }).start();
            }
        });
    }
}

