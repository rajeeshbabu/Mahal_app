package com.mahal.controller.event;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import com.mahal.database.EventDAO;
import com.mahal.database.MasjidDAO;
import com.mahal.model.Event;
import com.mahal.model.Masjid;
import com.mahal.util.TableStyler;
import com.mahal.util.FormStyler;
import com.mahal.util.StyleHelper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.PauseTransition;
import javafx.stage.PopupWindow;

public class EventController {
    private VBox view;
    private StackPane contentPane;
    private VBox addEventViewPane;
    private VBox eventListViewPane;
    private VBox calendarViewPane;
    private EventDAO eventDAO;
    private MasjidDAO masjidDAO;

    private ObservableList<Event> eventList = FXCollections.observableArrayList();
    private ObservableList<Masjid> masjidList = FXCollections.observableArrayList();

    // Calendar state
    private LocalDate currentDate = LocalDate.now();
    private String calendarView = "month"; // month, week, day
    private StackPane calendarPane; // Calendar pane reference

    // Filter fields
    private TextField searchField;
    private ComboBox<Masjid> masjidCombo;
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;
    private ComboBox<String> visibilityCombo;
    private Label recordCountLabel;

    // Switcher buttons
    private Button addEventBtn;
    private Button eventListBtn;
    private Button calendarBtn;

    public EventController() {
        this.eventDAO = new EventDAO();
        this.masjidDAO = new MasjidDAO();
        createView();
        loadMasjids();
        loadEventList();
        loadCalendarEvents();

        // Subscribe to sync events
        com.mahal.util.EventBus.getInstance().subscribe("events", e -> javafx.application.Platform.runLater(() -> {
            loadEventList();
            loadCalendarEvents();
        }));
        com.mahal.util.EventBus.getInstance().subscribe("masjids",
                e -> javafx.application.Platform.runLater(this::loadMasjids));
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

        Label titleLabel = new Label("Events Management");
        titleLabel.setStyle(StyleHelper.getTitleStyle());

        // Emerald Pill Switcher
        HBox switcher = new HBox(8);
        switcher.setAlignment(Pos.CENTER_LEFT);
        switcher.setStyle(StyleHelper.getPillSwitcherContainerStyle());

        addEventBtn = new Button("Add Event");
        eventListBtn = new Button("Event List");
        calendarBtn = new Button("Calendar");

        styleSwitcherButton(addEventBtn, true);
        styleSwitcherButton(eventListBtn, false);
        styleSwitcherButton(calendarBtn, false);

        switcher.getChildren().addAll(addEventBtn, eventListBtn, calendarBtn);

        contentPane = new StackPane();
        contentPane.setPadding(new Insets(10, 0, 0, 0));
        addEventViewPane = createAddEventForm();
        eventListViewPane = createEventList();
        calendarViewPane = createCalendarView();
        contentPane.getChildren().setAll(addEventViewPane);

        addEventBtn.setOnAction(e -> {
            styleSwitcherButton(addEventBtn, true);
            styleSwitcherButton(eventListBtn, false);
            styleSwitcherButton(calendarBtn, false);
            contentPane.getChildren().setAll(addEventViewPane);
        });

        eventListBtn.setOnAction(e -> {
            styleSwitcherButton(addEventBtn, false);
            styleSwitcherButton(eventListBtn, true);
            styleSwitcherButton(calendarBtn, false);
            contentPane.getChildren().setAll(eventListViewPane);
        });

        calendarBtn.setOnAction(e -> {
            styleSwitcherButton(addEventBtn, false);
            styleSwitcherButton(eventListBtn, false);
            styleSwitcherButton(calendarBtn, true);
            contentPane.getChildren().setAll(calendarViewPane);
        });

        view.getChildren().addAll(titleLabel, switcher, contentPane);
    }

    // ========== ADD EVENT FORM ==========
    private VBox createAddEventForm() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(12));

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent;");

        VBox form = new VBox(12);
        form.setSpacing(12);
        form.setPadding(new Insets(12, 12, 16, 12));
        form.setStyle("-fx-background-color: #ffffff;");

        Label title = FormStyler.createFormLabel("Add Event");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + StyleHelper.TEXT_GRAY_900 + ";");

        String fieldStyle = "-fx-background-color: #ffffff; -fx-background-radius: 8; -fx-border-radius: 8; "
                + "-fx-border-color: #d1d5db; -fx-border-width: 1; -fx-padding: 8 12; -fx-font-size: 12px;";
        String focusStyle = "-fx-background-color: #ffffff; -fx-background-radius: 8; -fx-border-radius: 8; "
                + "-fx-border-color: #cbd5e1; -fx-border-width: 1; -fx-padding: 8 12; -fx-font-size: 12px;";
        String datePickerStyle = fieldStyle;
        String datePickerFocusStyle = focusStyle;

        TextField eventNameField = new TextField();
        eventNameField.setPromptText("Event Name *");
        eventNameField.setPrefHeight(32);
        eventNameField.setStyle(fieldStyle);
        eventNameField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> eventNameField.setStyle(newVal ? focusStyle : fieldStyle));

        TextField organizerField = new TextField();
        organizerField.setPromptText("Organizer");
        organizerField.setPrefHeight(32);
        organizerField.setStyle(fieldStyle);
        organizerField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> organizerField.setStyle(newVal ? focusStyle : fieldStyle));

        DatePicker startDatePicker = new DatePicker();
        startDatePicker.setPromptText("Start Date");
        startDatePicker.setPrefHeight(32);
        startDatePicker.setStyle(datePickerStyle);
        startDatePicker.focusedProperty().addListener(
                (obs, oldVal, newVal) -> startDatePicker.setStyle(newVal ? datePickerFocusStyle : datePickerStyle));
        styleDatePickerPopup(startDatePicker);

        ComboBox<String> startTimeCombo = new ComboBox<>();
        startTimeCombo.getItems().addAll(generateTimeOptions());
        startTimeCombo.setPromptText("Start Time");
        startTimeCombo.setPrefHeight(32);
        startTimeCombo.setStyle(fieldStyle);
        startTimeCombo.focusedProperty()
                .addListener((obs, oldVal, newVal) -> startTimeCombo.setStyle(newVal ? focusStyle : fieldStyle));

        DatePicker endDatePicker = new DatePicker();
        endDatePicker.setPromptText("End Date");
        endDatePicker.setPrefHeight(32);
        endDatePicker.setStyle(datePickerStyle);
        endDatePicker.focusedProperty().addListener(
                (obs, oldVal, newVal) -> endDatePicker.setStyle(newVal ? datePickerFocusStyle : datePickerStyle));
        styleDatePickerPopup(endDatePicker);

        ComboBox<String> endTimeCombo = new ComboBox<>();
        endTimeCombo.getItems().addAll(generateTimeOptions());
        endTimeCombo.setPromptText("End Time");
        endTimeCombo.setPrefHeight(32);
        endTimeCombo.setStyle(fieldStyle);
        endTimeCombo.focusedProperty()
                .addListener((obs, oldVal, newVal) -> endTimeCombo.setStyle(newVal ? focusStyle : fieldStyle));

        ComboBox<Masjid> masjidCombo = new ComboBox<>();
        masjidCombo.setPromptText("Select Masjid (Optional)");
        masjidCombo.setItems(masjidList);
        masjidCombo.setPrefHeight(32);
        masjidCombo.setCellFactory(listView -> new ListCell<Masjid>() {
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
        masjidCombo.setStyle(fieldStyle);
        masjidCombo.focusedProperty()
                .addListener((obs, oldVal, newVal) -> masjidCombo.setStyle(newVal ? focusStyle : fieldStyle));

        TextField eventPlaceField = new TextField();
        eventPlaceField.setPromptText("Event Place (or enter manually)");
        eventPlaceField.setPrefHeight(32);
        eventPlaceField.setStyle(fieldStyle);
        eventPlaceField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> eventPlaceField.setStyle(newVal ? focusStyle : fieldStyle));

        TextField contactField = new TextField();
        contactField.setPromptText("Contact");
        contactField.setPrefHeight(32);
        contactField.setStyle(fieldStyle);
        contactField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> contactField.setStyle(newVal ? focusStyle : fieldStyle));

        ComboBox<String> visibilityCombo = new ComboBox<>();
        visibilityCombo.getItems().addAll("Public", "Private");
        visibilityCombo.setValue("Public");
        visibilityCombo.setPrefHeight(32);
        visibilityCombo.setStyle(fieldStyle);
        visibilityCombo.focusedProperty()
                .addListener((obs, oldVal, newVal) -> visibilityCombo.setStyle(newVal ? focusStyle : fieldStyle));

        TextArea eventDetailsField = new TextArea();
        eventDetailsField.setPromptText("Event Details");
        eventDetailsField.setPrefRowCount(2);
        eventDetailsField.setPrefHeight(50);
        eventDetailsField.setStyle(fieldStyle);
        eventDetailsField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> eventDetailsField.setStyle(newVal ? focusStyle : fieldStyle));

        Button save = new Button("Save Event");
        save.setStyle(
                "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
        save.setOnMouseEntered(e -> save.setStyle(
                "-fx-background-color: #1d4ed8; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        save.setOnMouseExited(e -> save.setStyle(
                "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        save.setOnAction(e -> {
            if (eventNameField.getText().isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Event Name is required", ButtonType.OK).showAndWait();
                return;
            }
            if (startDatePicker.getValue() == null || startTimeCombo.getValue() == null) {
                new Alert(Alert.AlertType.WARNING, "Start Date & Time are required", ButtonType.OK).showAndWait();
                return;
            }
            if (endDatePicker.getValue() == null || endTimeCombo.getValue() == null) {
                new Alert(Alert.AlertType.WARNING, "End Date & Time are required", ButtonType.OK).showAndWait();
                return;
            }

            Event event = new Event();
            event.setEventName(eventNameField.getText());
            event.setOrganizer(organizerField.getText());

            LocalTime startTime = parseTime(startTimeCombo.getValue());
            event.setStartDateTime(LocalDateTime.of(startDatePicker.getValue(), startTime));

            LocalTime endTime = parseTime(endTimeCombo.getValue());
            event.setEndDateTime(LocalDateTime.of(endDatePicker.getValue(), endTime));

            if (masjidCombo.getValue() != null) {
                event.setMasjidId(masjidCombo.getValue().getId());
            }
            event.setEventPlace(eventPlaceField.getText());
            event.setContact(contactField.getText());
            event.setEventDetails(eventDetailsField.getText());
            event.setIsPublic("Public".equals(visibilityCombo.getValue()));

            // Disable save button while saving
            save.setDisable(true);
            save.setText("Saving...");

            saveEvent(event, null, eventNameField, organizerField, startDatePicker, startTimeCombo,
                    endDatePicker, endTimeCombo, masjidCombo, eventPlaceField, contactField,
                    visibilityCombo, eventDetailsField, save);
        });

        Button clear = new Button("Clear");
        clear.setStyle(
                "-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
        clear.setOnMouseEntered(e -> clear.setStyle(
                "-fx-background-color: #dc2626; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        clear.setOnMouseExited(e -> clear.setStyle(
                "-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        clear.setOnAction(e -> {
            eventNameField.clear();
            organizerField.clear();
            startDatePicker.setValue(null);
            startTimeCombo.getSelectionModel().clearSelection();
            endDatePicker.setValue(null);
            endTimeCombo.getSelectionModel().clearSelection();
            masjidCombo.getSelectionModel().clearSelection();
            eventPlaceField.clear();
            contactField.clear();
            visibilityCombo.setValue("Public");
            eventDetailsField.clear();
        });

        HBox buttons = new HBox(10, clear, save);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(8, 0, 0, 0));

        form.getChildren().setAll(
                title,
                FormStyler.createCompactFormField("Event Name *", eventNameField),
                FormStyler.createCompactFormField("Organizer", organizerField),
                FormStyler.createCompactFormField("Start Date", startDatePicker),
                FormStyler.createCompactFormField("Start Time", startTimeCombo),
                FormStyler.createCompactFormField("End Date", endDatePicker),
                FormStyler.createCompactFormField("End Time", endTimeCombo),
                FormStyler.createCompactFormField("Masjid (Optional)", masjidCombo),
                FormStyler.createCompactFormField("Event Place", eventPlaceField),
                FormStyler.createCompactFormField("Contact", contactField),
                FormStyler.createCompactFormField("Visibility", visibilityCombo),
                FormStyler.createCompactFormField("Event Details", eventDetailsField),
                buttons);

        scrollPane.setContent(form);
        box.getChildren().addAll(scrollPane);
        return box;
    }

    private List<String> generateTimeOptions() {
        List<String> times = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            for (int minute = 0; minute < 60; minute += 15) {
                times.add(String.format("%02d:%02d", hour, minute));
            }
        }
        return times;
    }

    private LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty())
            return LocalTime.of(0, 0);
        String[] parts = timeStr.split(":");
        return LocalTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }

    // Removed month/year pickers to keep filters minimal

    // ========== EVENT LIST ==========
    private VBox createEventList() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(12));

        Button addBtn = new Button("Add Event");
        addBtn.setStyle(StyleHelper.getActionPillButtonStyle());
        addBtn.setOnAction(e -> addEventBtn.fire());

        searchField = new TextField();
        searchField.setPromptText("Search events...");
        searchField.setPrefWidth(250);
        StyleHelper.styleTextField(searchField);
        searchField.textProperty().addListener((obs, old, val) -> {
            loadEventList();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        recordCountLabel = new Label("Showing 0 of 0 events");
        recordCountLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px; -fx-font-weight: 500;");

        HBox actionRow = new HBox(12);
        actionRow.setAlignment(Pos.CENTER_LEFT);
        actionRow.setPadding(new Insets(0, 0, 10, 0));
        actionRow.getChildren().addAll(addBtn, searchField, spacer, recordCountLabel);

        TableView<Event> table = new TableView<>();
        TableStyler.applyModernStyling(table);

        TableColumn<Event, String> nameCol = new TableColumn<>("NAME");
        nameCol.setCellValueFactory(cell -> {
            String name = cell.getValue().getEventName();
            return new javafx.beans.property.SimpleStringProperty(name != null ? name : "");
        });
        TableStyler.styleTableColumn(nameCol);

        TableColumn<Event, String> startCol = new TableColumn<>("START");
        startCol.setCellValueFactory(cell -> {
            LocalDateTime start = cell.getValue().getStartDateTime();
            return new javafx.beans.property.SimpleStringProperty(
                    start != null ? start.format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")) : "");
        });
        TableStyler.styleTableColumn(startCol);

        TableColumn<Event, String> endCol = new TableColumn<>("END");
        endCol.setCellValueFactory(cell -> {
            LocalDateTime end = cell.getValue().getEndDateTime();
            return new javafx.beans.property.SimpleStringProperty(
                    end != null ? end.format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")) : "");
        });
        TableStyler.styleTableColumn(endCol);

        TableColumn<Event, String> placeCol = new TableColumn<>("PLACE");
        placeCol.setCellValueFactory(cell -> {
            String place = cell.getValue().getMasjidName() != null ? cell.getValue().getMasjidName()
                    : cell.getValue().getEventPlace();
            return new javafx.beans.property.SimpleStringProperty(place != null ? place : "");
        });
        TableStyler.styleTableColumn(placeCol);

        TableColumn<Event, String> organizerCol = new TableColumn<>("ORGANIZER");
        organizerCol.setCellValueFactory(cell -> {
            String org = cell.getValue().getOrganizer();
            return new javafx.beans.property.SimpleStringProperty(org != null ? org : "");
        });
        TableStyler.styleTableColumn(organizerCol);

        TableColumn<Event, String> actionsCol = createActionsColumn(table);
        actionsCol.setText("ACTIONS");
        table.getColumns().addAll(nameCol, startCol, endCol, placeCol, organizerCol, actionsCol);
        table.setItems(eventList);

        box.getChildren().addAll(actionRow, table);
        loadEventList();
        return box;
    }

    private void loadEventList() {
        new Thread(() -> {
            try {
                // Get search text
                String searchText = (searchField != null) ? searchField.getText() : "";

                var data = eventDAO.getAllWithFilters(searchText, null, null, null, null);
                System.out.println("Loaded " + data.size() + " events from database");
                javafx.application.Platform.runLater(() -> {
                    eventList.setAll(data);
                    if (recordCountLabel != null) {
                        recordCountLabel.setText("Showing " + data.size() + " events");
                    }
                });
            } catch (Exception e) {
                System.err.println("Error loading events: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    // ========== CALENDAR VIEW ==========
    private VBox createCalendarView() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(12));

        calendarPane = new StackPane();
        calendarPane.setMinHeight(400);
        calendarPane.setMinWidth(800); // medium calendar view

        box.getChildren().addAll(calendarPane);

        // Load calendar content
        loadCalendarContent(calendarPane);

        return box;
    }

    private void loadCalendarContent(StackPane calendarPane) {
        if ("month".equals(calendarView)) {
            calendarPane.getChildren().setAll(renderMonthView());
        } else if ("week".equals(calendarView)) {
            calendarPane.getChildren().setAll(renderWeekView());
        } else {
            calendarPane.getChildren().setAll(renderDayView());
        }
    }

    private void loadCalendarEvents() {
        new Thread(() -> {
            try {
                LocalDateTime start, end;
                if ("month".equals(calendarView)) {
                    start = currentDate.withDayOfMonth(1).atStartOfDay();
                    end = currentDate.withDayOfMonth(currentDate.lengthOfMonth()).atTime(23, 59, 59);
                } else if ("week".equals(calendarView)) {
                    LocalDate weekStart = currentDate.minusDays(currentDate.getDayOfWeek().getValue() - 1);
                    start = weekStart.atStartOfDay();
                    end = weekStart.plusDays(6).atTime(23, 59, 59);
                } else {
                    start = currentDate.atStartOfDay();
                    end = currentDate.atTime(23, 59, 59);
                }

                var data = eventDAO.getCalendarEvents(start, end);
                javafx.application.Platform.runLater(() -> {
                    eventList.setAll(data);
                    // Reload calendar view
                    if (calendarPane != null) {
                        loadCalendarContent(calendarPane);
                    }
                });
            } catch (Exception e) {
                System.err.println("Error loading calendar events: " + e.getMessage());
            }
        }).start();
    }

    private VBox renderMonthView() {
        VBox monthView = new VBox(16);
        monthView.setPadding(new Insets(24));
        monthView.setStyle(
                "-fx-background-color: #ffffff; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 10, 0, 0, 4);");
        monthView.setMaxWidth(Double.MAX_VALUE);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(0, 0, 20, 0));

        Button prevBtn = new Button("◀");
        prevBtn.setStyle(
                "-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-background-radius: 8; -fx-font-weight: 600; -fx-font-size: 13px; -fx-padding: 8 12; -fx-cursor: hand; -fx-min-width: 40; -fx-min-height: 36;");
        prevBtn.setOnMouseEntered(e -> prevBtn.setStyle(
                "-fx-background-color: #e2e8f0; -fx-text-fill: #334155; -fx-background-radius: 8; -fx-font-weight: 600; -fx-font-size: 13px; -fx-padding: 8 12; -fx-cursor: hand; -fx-min-width: 40; -fx-min-height: 36;"));
        prevBtn.setOnMouseExited(e -> prevBtn.setStyle(
                "-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-background-radius: 8; -fx-font-weight: 600; -fx-font-size: 13px; -fx-padding: 8 12; -fx-cursor: hand; -fx-min-width: 40; -fx-min-height: 36;"));
        prevBtn.setOnAction(e -> {
            currentDate = currentDate.minusMonths(1);
            loadCalendarEvents();
        });

        // Month/Year selectors
        ComboBox<String> monthCombo = new ComboBox<>();
        monthCombo.getItems().addAll(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December");
        monthCombo.setValue(
                currentDate.getMonth().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault()));
        monthCombo.setStyle(
                "-fx-background-color: #ffffff; -fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-padding: 8 12; -fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: #000000;");
        monthCombo.setMinWidth(110);
        monthCombo.setPrefHeight(36);

        ComboBox<Integer> yearCombo = new ComboBox<>();
        int currentYear = LocalDate.now().getYear();
        for (int y = currentYear - 10; y <= currentYear + 10; y++) {
            yearCombo.getItems().add(y);
        }
        yearCombo.setValue(currentDate.getYear());
        yearCombo.setStyle(
                "-fx-background-color: #ffffff; -fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-padding: 8 12; -fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: #000000;");
        yearCombo.setMinWidth(75);
        yearCombo.setPrefHeight(36);

        monthCombo.valueProperty().addListener((obs, old, val) -> {
            if (val != null) {
                int monthIndex = monthCombo.getSelectionModel().getSelectedIndex() + 1;
                currentDate = currentDate.withMonth(monthIndex);
                loadCalendarEvents();
            }
        });
        yearCombo.valueProperty().addListener((obs, old, val) -> {
            if (val != null) {
                currentDate = currentDate.withYear(val);
                loadCalendarEvents();
            }
        });

        Button nextBtn = new Button("▶");
        nextBtn.setStyle(
                "-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-background-radius: 8; -fx-font-weight: 600; -fx-font-size: 13px; -fx-padding: 8 12; -fx-cursor: hand; -fx-min-width: 40; -fx-min-height: 36;");
        nextBtn.setOnMouseEntered(e -> nextBtn.setStyle(
                "-fx-background-color: #e2e8f0; -fx-text-fill: #334155; -fx-background-radius: 8; -fx-font-weight: 600; -fx-font-size: 13px; -fx-padding: 8 12; -fx-cursor: hand; -fx-min-width: 40; -fx-min-height: 36;"));
        nextBtn.setOnMouseExited(e -> nextBtn.setStyle(
                "-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-background-radius: 8; -fx-font-weight: 600; -fx-font-size: 13px; -fx-padding: 8 12; -fx-cursor: hand; -fx-min-width: 40; -fx-min-height: 36;"));
        nextBtn.setOnAction(e -> {
            currentDate = currentDate.plusMonths(1);
            loadCalendarEvents();
        });

        header.getChildren().addAll(prevBtn, monthCombo, yearCombo, nextBtn);

        GridPane calendarGrid = new GridPane();
        calendarGrid.setHgap(0);
        calendarGrid.setVgap(0);
        calendarGrid.setPadding(new Insets(8));
        calendarGrid.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(calendarGrid, Priority.ALWAYS);

        // Make columns fill available width
        for (int i = 0; i < 7; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setHgrow(Priority.ALWAYS);
            col.setPercentWidth(100.0 / 7);
            calendarGrid.getColumnConstraints().add(col);
        }

        // Week day headers - modern light styling
        String[] weekDays = { "SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT" };

        for (int i = 0; i < 7; i++) {
            Label dayHeader = new Label(weekDays[i]);
            dayHeader.setStyle(
                    "-fx-font-weight: 600; -fx-font-size: 12px; -fx-text-fill: #94a3b8; -fx-padding: 12 4; -fx-alignment: center; -fx-background-color: transparent;");
            GridPane.setFillWidth(dayHeader, true);
            calendarGrid.add(dayHeader, i, 0);
        }

        // Calendar days - only show days from current month
        LocalDate monthStart = currentDate.withDayOfMonth(1);
        LocalDate monthEnd = currentDate.withDayOfMonth(currentDate.lengthOfMonth());

        // Calculate which day of week the month starts on (0 = Sunday, 6 = Saturday)
        int firstDayOfWeek = monthStart.getDayOfWeek().getValue() % 7;

        // Start from the first day of the month
        int row = 1;
        int col = firstDayOfWeek;

        for (LocalDate date = monthStart; !date.isAfter(monthEnd); date = date.plusDays(1)) {
            VBox dayCell = new VBox(3);
            dayCell.setPadding(new Insets(4));
            dayCell.setMinHeight(60);
            dayCell.setPrefHeight(Region.USE_COMPUTED_SIZE);
            boolean isCurrentMonth = true; // All dates shown are from current month
            boolean isToday = date.equals(LocalDate.now());
            boolean isWeekend = date.getDayOfWeek().getValue() % 7 == 0 || date.getDayOfWeek().getValue() % 7 == 6;

            String cellBgColor = "#ffffff";
            String hoverBgColor = "#f8fafc";

            dayCell.setStyle("-fx-background-color: " + cellBgColor
                    + "; -fx-border-color: transparent; -fx-border-width: 0; -fx-cursor: hand;");

            // Add hover effect
            dayCell.setOnMouseEntered(e -> {
                if (!isToday) {
                    dayCell.setStyle("-fx-background-color: " + hoverBgColor
                            + "; -fx-border-color: transparent; -fx-border-width: 0; -fx-cursor: hand;");
                }
            });
            dayCell.setOnMouseExited(e -> {
                dayCell.setStyle("-fx-background-color: " + cellBgColor
                        + "; -fx-border-color: transparent; -fx-border-width: 0; -fx-cursor: hand;");
            });

            HBox dayLabelBox = new HBox();
            dayLabelBox.setAlignment(Pos.CENTER);

            Label dayLabel = new Label(String.valueOf(date.getDayOfMonth()));
            String dayLabelStyle = "-fx-font-size: 14px; -fx-font-weight: " + (isToday ? "700" : "500")
                    + "; -fx-text-fill: " + (isToday ? "#ffffff" : "#334155") + ";";

            if (isToday) {
                // Circular blue background with subtle glow for today
                dayLabelStyle += " -fx-background-color: #3b82f6; -fx-text-fill: white; -fx-background-radius: 18; -fx-padding: 6 10; -fx-min-width: 36; -fx-min-height: 36; -fx-alignment: center;";
                dayLabel.setAlignment(Pos.CENTER);
                dayLabelBox.setAlignment(Pos.CENTER);
            } else {
                dayLabelStyle += " -fx-padding: 4 8;";
            }
            dayLabel.setStyle(dayLabelStyle);

            dayLabelBox.getChildren().add(dayLabel);
            dayCell.getChildren().add(dayLabelBox);

            // Add events for this day
            List<Event> dayEvents = getEventsForDate(date);
            if (!dayEvents.isEmpty()) {
                VBox eventsContainer = new VBox(1);
                eventsContainer.setSpacing(1);

                for (int i = 0; i < Math.min(dayEvents.size(), 2); i++) {
                    Event event = dayEvents.get(i);
                    HBox eventBox = new HBox(4);
                    eventBox.setAlignment(Pos.CENTER_LEFT);
                    eventBox.setPadding(new Insets(0));

                    Label eventLabel = new Label(event.getEventName());
                    // Modern rounded rectangular background for events
                    String[] eventColors = { "#e0e7ff", "#fce7f3", "#fef3c7", "#d1fae5" }; // Soft purple, pink, yellow,
                                                                                           // green
                    String[] eventTextColors = { "#6366f1", "#ec4899", "#f59e0b", "#10b981" };
                    int colorIndex = i % eventColors.length;
                    eventLabel.setStyle("-fx-font-size: 10px; -fx-background-color: " + eventColors[colorIndex]
                            + "; -fx-text-fill: " + eventTextColors[colorIndex] + "; " +
                            "-fx-padding: 4 8; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-weight: 500; -fx-background-insets: 0;");
                    eventLabel.setOnMouseClicked(e -> showEventDetails(event));
                    eventLabel.setMaxWidth(Double.MAX_VALUE);
                    HBox.setHgrow(eventLabel, Priority.ALWAYS);

                    final int colorIdx = colorIndex;
                    // Darker versions for hover
                    String[] eventHoverColors = { "#c7d2fe", "#fbcfe8", "#fde68a", "#a7f3d0" };
                    eventBox.setOnMouseEntered(e -> {
                        eventLabel.setStyle("-fx-font-size: 10px; -fx-background-color: " + eventHoverColors[colorIdx]
                                + "; -fx-text-fill: " + eventTextColors[colorIdx] + "; " +
                                "-fx-padding: 4 8; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-weight: 600; -fx-background-insets: 0;");
                    });
                    eventBox.setOnMouseExited(e -> {
                        eventLabel.setStyle("-fx-font-size: 10px; -fx-background-color: " + eventColors[colorIdx]
                                + "; -fx-text-fill: " + eventTextColors[colorIdx] + "; " +
                                "-fx-padding: 4 8; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-weight: 500; -fx-background-insets: 0;");
                    });

                    eventBox.getChildren().addAll(eventLabel);
                    eventsContainer.getChildren().add(eventBox);
                }

                if (dayEvents.size() > 2) {
                    Label moreLabel = new Label("+" + (dayEvents.size() - 2) + " more");
                    moreLabel.setStyle(
                            "-fx-font-size: 8px; -fx-text-fill: #6b7280; -fx-font-weight: 600; -fx-padding: 1 0 0 0;");
                    eventsContainer.getChildren().add(moreLabel);
                }

                dayCell.getChildren().add(eventsContainer);
            }

            GridPane.setFillWidth(dayCell, true);
            GridPane.setFillHeight(dayCell, true);
            calendarGrid.add(dayCell, col, row);

            col++;
            if (col > 6) {
                col = 0;
                row++;
            }
        }

        // Fill remaining cells in the last row with empty cells if needed
        while (col < 7) {
            VBox emptyCell = new VBox();
            emptyCell.setMinHeight(60);
            emptyCell.setStyle("-fx-background-color: #ffffff; -fx-border-color: transparent; -fx-border-width: 0;");
            GridPane.setFillWidth(emptyCell, true);
            GridPane.setFillHeight(emptyCell, true);
            calendarGrid.add(emptyCell, col, row);
            col++;
        }

        VBox contentWrapper = new VBox();
        contentWrapper.getChildren().addAll(header, calendarGrid);
        contentWrapper.setMaxWidth(Double.MAX_VALUE);

        // Remove borders from grid, make it seamless
        calendarGrid.setStyle("-fx-background-color: transparent;");

        monthView.getChildren().add(contentWrapper);
        return monthView;
    }

    private VBox renderWeekView() {
        VBox weekView = new VBox(10);
        weekView.setPadding(new Insets(12));
        weekView.setStyle(StyleHelper.getFormSectionStyle());

        LocalDate weekStart = currentDate.minusDays(currentDate.getDayOfWeek().getValue() - 1);
        LocalDate weekEnd = weekStart.plusDays(6);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER);

        Button prevBtn = new Button("◀ Previous Week");
        prevBtn.setStyle(StyleHelper.getSecondaryButtonStyle());
        prevBtn.setOnAction(e -> {
            currentDate = currentDate.minusWeeks(1);
            loadCalendarEvents();
        });

        Label weekLabel = new Label(weekStart.format(DateTimeFormatter.ofPattern("MMM d")) + " - " +
                weekEnd.format(DateTimeFormatter.ofPattern("MMM d, yyyy")));
        weekLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Button nextBtn = new Button("Next Week ▶");
        nextBtn.setStyle(StyleHelper.getSecondaryButtonStyle());
        nextBtn.setOnAction(e -> {
            currentDate = currentDate.plusWeeks(1);
            loadCalendarEvents();
        });

        header.getChildren().addAll(prevBtn, weekLabel, nextBtn);

        HBox weekDays = new HBox(5);
        weekDays.setSpacing(5);

        for (int i = 0; i < 7; i++) {
            LocalDate day = weekStart.plusDays(i);
            VBox dayColumn = new VBox(5);
            dayColumn.setPadding(new Insets(8));
            dayColumn.setMinWidth(150);
            dayColumn.setStyle("-fx-border-color: #d1d5db; -fx-border-width: 1; -fx-background-radius: 4;");

            boolean isToday = day.equals(LocalDate.now());
            Label dayLabel = new Label(day.format(DateTimeFormatter.ofPattern("EEE d")));
            dayLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; " +
                    (isToday ? "-fx-text-fill: #3b82f6;" : "-fx-text-fill: #374151;"));
            dayColumn.getChildren().add(dayLabel);

            List<Event> dayEvents = getEventsForDate(day);
            for (Event event : dayEvents) {
                VBox eventBox = new VBox(2);
                eventBox.setPadding(new Insets(4));
                eventBox.setStyle("-fx-background-color: #dbeafe; -fx-background-radius: 3; -fx-cursor: hand;");
                eventBox.setOnMouseClicked(e -> showEventDetails(event));

                Label eventName = new Label(event.getEventName());
                eventName.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");

                if (event.getStartDateTime() != null) {
                    Label eventTime = new Label(event.getStartDateTime().format(DateTimeFormatter.ofPattern("h:mm a")));
                    eventTime.setStyle("-fx-font-size: 9px; -fx-text-fill: #6b7280;");
                    eventBox.getChildren().addAll(eventName, eventTime);
                } else {
                    eventBox.getChildren().add(eventName);
                }

                dayColumn.getChildren().add(eventBox);
            }

            weekDays.getChildren().add(dayColumn);
        }

        weekView.getChildren().addAll(header, weekDays);
        return weekView;
    }

    private VBox renderDayView() {
        VBox dayView = new VBox(10);
        dayView.setPadding(new Insets(12));
        dayView.setStyle(StyleHelper.getFormSectionStyle());

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER);

        Button prevBtn = new Button("◀ Previous Day");
        prevBtn.setStyle(StyleHelper.getSecondaryButtonStyle());
        prevBtn.setOnAction(e -> {
            currentDate = currentDate.minusDays(1);
            loadCalendarEvents();
        });

        Label dayLabel = new Label(currentDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));
        dayLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Button nextBtn = new Button("Next Day ▶");
        nextBtn.setStyle(StyleHelper.getSecondaryButtonStyle());
        nextBtn.setOnAction(e -> {
            currentDate = currentDate.plusDays(1);
            loadCalendarEvents();
        });

        header.getChildren().addAll(prevBtn, dayLabel, nextBtn);

        VBox eventsList = new VBox(10);
        List<Event> dayEvents = getEventsForDate(currentDate);

        if (dayEvents.isEmpty()) {
            Label noEvents = new Label("No events for this day");
            noEvents.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 14px; -fx-padding: 20;");
            noEvents.setAlignment(Pos.CENTER);
            eventsList.getChildren().add(noEvents);
        } else {
            for (Event event : dayEvents) {
                VBox eventBox = new VBox(5);
                eventBox.setPadding(new Insets(12));
                eventBox.setStyle("-fx-border-color: #d1d5db; -fx-border-width: 1; -fx-background-radius: 8; " +
                        "-fx-cursor: hand;");
                eventBox.setOnMouseClicked(e -> showEventDetails(event));

                Label eventName = new Label(event.getEventName());
                eventName.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

                if (event.getStartDateTime() != null && event.getEndDateTime() != null) {
                    Label eventTime = new Label(
                            event.getStartDateTime().format(DateTimeFormatter.ofPattern("h:mm a")) + " - " +
                                    event.getEndDateTime().format(DateTimeFormatter.ofPattern("h:mm a")));
                    eventTime.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
                    eventBox.getChildren().add(eventTime);
                }

                if (event.getEventPlace() != null || event.getMasjidName() != null) {
                    Label eventPlace = new Label(
                            "📍 " + (event.getMasjidName() != null ? event.getMasjidName() : event.getEventPlace()));
                    eventPlace.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
                    eventBox.getChildren().add(eventPlace);
                }

                if (event.getOrganizer() != null) {
                    Label organizer = new Label("👤 " + event.getOrganizer());
                    organizer.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
                    eventBox.getChildren().add(organizer);
                }

                eventBox.getChildren().add(0, eventName);
                eventsList.getChildren().add(eventBox);
            }
        }

        dayView.getChildren().addAll(header, eventsList);
        return dayView;
    }

    private List<Event> getEventsForDate(LocalDate date) {
        List<Event> dayEvents = new ArrayList<>();
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.atTime(23, 59, 59);

        for (Event event : eventList) {
            if (event.getStartDateTime() != null && event.getEndDateTime() != null) {
                if ((event.getStartDateTime().isBefore(dayEnd) || event.getStartDateTime().isEqual(dayEnd)) &&
                        (event.getEndDateTime().isAfter(dayStart) || event.getEndDateTime().isEqual(dayStart))) {
                    dayEvents.add(event);
                }
            }
        }
        return dayEvents;
    }

    private TableColumn<Event, String> createActionsColumn(TableView<Event> table) {
        TableColumn<Event, String> actionsCol = new TableColumn<>("Actions");
        actionsCol.setCellFactory(col -> new TableCell<Event, String>() {
            private final Hyperlink viewLink = new Hyperlink("View");
            private final Hyperlink editLink = new Hyperlink("Edit");
            private final Hyperlink deleteLink = new Hyperlink("Delete");
            private final HBox container = new HBox(12, viewLink, editLink, deleteLink);

            {
                container.setAlignment(Pos.CENTER);
                viewLink.setStyle(
                        "-fx-text-fill: #10b981; -fx-font-weight: 600; -fx-font-size: 13px; -fx-underline: false;");
                editLink.setStyle(
                        "-fx-text-fill: #2563eb; -fx-font-weight: 600; -fx-font-size: 13px; -fx-underline: false;");
                deleteLink.setStyle(
                        "-fx-text-fill: #ef4444; -fx-font-weight: 600; -fx-font-size: 13px; -fx-underline: false;");

                viewLink.setOnAction(e -> {
                    Event event = getTableView().getItems().get(getIndex());
                    showEventDetails(event);
                });

                editLink.setOnAction(e -> {
                    Event event = getTableView().getItems().get(getIndex());
                    showEditDialog(event);
                });

                deleteLink.setOnAction(e -> {
                    Event event = getTableView().getItems().get(getIndex());
                    deleteEvent(event);
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
        return actionsCol;
    }

    // ========== ACTION METHODS ==========
    private void showEventDetails(Event event) {
        Stage dialog = new Stage();
        dialog.setTitle("Event Details");

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent;");

        VBox root = new VBox(12);
        root.setSpacing(12);
        root.setPadding(new Insets(16, 16, 20, 16));
        root.setStyle("-fx-background-color: #ffffff;");

        Label title = FormStyler.createFormLabel(event.getEventName() != null ? event.getEventName() : "Event");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + StyleHelper.TEXT_GRAY_900
                + "; -fx-padding: 0 0 8 0;");

        VBox details = new VBox(12);
        details.setPadding(new Insets(0));

        if (event.getStartDateTime() != null && event.getEndDateTime() != null) {
            Label dateTimeLabel = new Label("Date & Time:");
            dateTimeLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #6b7280;");

            Label dateTime = new Label(
                    event.getStartDateTime().format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")) + " - " +
                            event.getEndDateTime().format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")));
            dateTime.setStyle("-fx-font-size: 13px; -fx-text-fill: #111827;");

            VBox dateTimeBox = new VBox(4);
            dateTimeBox.getChildren().addAll(dateTimeLabel, dateTime);
            details.getChildren().add(dateTimeBox);
        }

        if (event.getEventPlace() != null || event.getMasjidName() != null) {
            Label placeLabel = new Label("Place:");
            placeLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #6b7280;");

            Label place = new Label(event.getMasjidName() != null ? event.getMasjidName() : event.getEventPlace());
            place.setStyle("-fx-font-size: 13px; -fx-text-fill: #111827;");

            VBox placeBox = new VBox(4);
            placeBox.getChildren().addAll(placeLabel, place);
            details.getChildren().add(placeBox);
        }

        if (event.getOrganizer() != null && !event.getOrganizer().isEmpty()) {
            Label organizerLabel = new Label("Organizer:");
            organizerLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #6b7280;");

            Label organizer = new Label(event.getOrganizer());
            organizer.setStyle("-fx-font-size: 13px; -fx-text-fill: #111827;");

            VBox organizerBox = new VBox(4);
            organizerBox.getChildren().addAll(organizerLabel, organizer);
            details.getChildren().add(organizerBox);
        }

        if (event.getContact() != null && !event.getContact().isEmpty()) {
            Label contactLabel = new Label("Contact:");
            contactLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #6b7280;");

            Label contact = new Label(event.getContact());
            contact.setStyle("-fx-font-size: 13px; -fx-text-fill: #111827;");

            VBox contactBox = new VBox(4);
            contactBox.getChildren().addAll(contactLabel, contact);
            details.getChildren().add(contactBox);
        }

        if (event.getEventDetails() != null && !event.getEventDetails().isEmpty()) {
            Label detailsLabel = new Label("Details:");
            detailsLabel.setStyle(
                    "-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #6b7280; -fx-padding: 8 0 4 0;");
            details.getChildren().add(detailsLabel);

            Label detailsText = new Label(event.getEventDetails());
            detailsText.setStyle("-fx-font-size: 13px; -fx-text-fill: #111827; -fx-wrap-text: true;");
            detailsText.setWrapText(true);
            detailsText.setMaxWidth(460);
            details.getChildren().add(detailsText);
        }

        HBox buttons = new HBox(10);
        buttons.setPadding(new Insets(12, 0, 0, 0));

        Button editBtn = new Button("Edit");
        editBtn.setStyle(
                "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
        editBtn.setOnMouseEntered(e -> editBtn.setStyle(
                "-fx-background-color: #1d4ed8; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        editBtn.setOnMouseExited(e -> editBtn.setStyle(
                "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        editBtn.setOnAction(e -> {
            dialog.close();
            showEditDialog(event);
        });

        Button closeBtn = new Button("Close");
        closeBtn.setStyle(
                "-fx-background-color: #6b7280; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(
                "-fx-background-color: #4b5563; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle(
                "-fx-background-color: #6b7280; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        closeBtn.setOnAction(e -> dialog.close());

        buttons.getChildren().addAll(closeBtn, editBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(title, details, buttons);
        scrollPane.setContent(root);

        Scene scene = new Scene(scrollPane, 500, 450);
        dialog.setScene(scene);
        dialog.show();
    }

    private void showEditDialog(Event existing) {
        // Similar to add form but pre-filled
        Stage dialog = new Stage();
        dialog.setTitle("Edit Event");

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent;");

        VBox form = new VBox(12);
        form.setSpacing(12);
        form.setPadding(new Insets(12, 12, 16, 12));
        form.setStyle("-fx-background-color: #ffffff;");

        Label title = FormStyler.createFormLabel("Edit Event");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + StyleHelper.TEXT_GRAY_900 + ";");

        String fieldStyle = "-fx-background-color: #ffffff; -fx-background-radius: 8; -fx-border-radius: 8; "
                + "-fx-border-color: #d1d5db; -fx-border-width: 1; -fx-padding: 8 12; -fx-font-size: 12px;";
        String focusStyle = "-fx-background-color: #ffffff; -fx-background-radius: 8; -fx-border-radius: 8; "
                + "-fx-border-color: #cbd5e1; -fx-border-width: 1; -fx-padding: 8 12; -fx-font-size: 12px;";
        String datePickerStyle = fieldStyle;
        String datePickerFocusStyle = focusStyle;

        TextField eventNameField = new TextField(existing.getEventName());
        eventNameField.setPromptText("Event Name *");
        eventNameField.setPrefHeight(32);
        eventNameField.setStyle(fieldStyle);
        eventNameField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> eventNameField.setStyle(newVal ? focusStyle : fieldStyle));

        TextField organizerField = new TextField(existing.getOrganizer());
        organizerField.setPromptText("Organizer");
        organizerField.setPrefHeight(32);
        organizerField.setStyle(fieldStyle);
        organizerField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> organizerField.setStyle(newVal ? focusStyle : fieldStyle));

        DatePicker startDatePicker = new DatePicker(
                existing.getStartDateTime() != null ? existing.getStartDateTime().toLocalDate() : null);
        startDatePicker.setPromptText("Start Date");
        startDatePicker.setPrefHeight(32);
        startDatePicker.setStyle(datePickerStyle);
        startDatePicker.focusedProperty().addListener(
                (obs, oldVal, newVal) -> startDatePicker.setStyle(newVal ? datePickerFocusStyle : datePickerStyle));
        styleDatePickerPopup(startDatePicker);

        ComboBox<String> startTimeCombo = new ComboBox<>();
        startTimeCombo.getItems().addAll(generateTimeOptions());
        if (existing.getStartDateTime() != null) {
            startTimeCombo
                    .setValue(existing.getStartDateTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        }
        startTimeCombo.setPromptText("Start Time");
        startTimeCombo.setPrefHeight(32);
        startTimeCombo.setStyle(fieldStyle);
        startTimeCombo.focusedProperty()
                .addListener((obs, oldVal, newVal) -> startTimeCombo.setStyle(newVal ? focusStyle : fieldStyle));

        DatePicker endDatePicker = new DatePicker(
                existing.getEndDateTime() != null ? existing.getEndDateTime().toLocalDate() : null);
        endDatePicker.setPromptText("End Date");
        endDatePicker.setPrefHeight(32);
        endDatePicker.setStyle(datePickerStyle);
        endDatePicker.focusedProperty().addListener(
                (obs, oldVal, newVal) -> endDatePicker.setStyle(newVal ? datePickerFocusStyle : datePickerStyle));
        styleDatePickerPopup(endDatePicker);

        ComboBox<String> endTimeCombo = new ComboBox<>();
        endTimeCombo.getItems().addAll(generateTimeOptions());
        if (existing.getEndDateTime() != null) {
            endTimeCombo.setValue(existing.getEndDateTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        }
        endTimeCombo.setPromptText("End Time");
        endTimeCombo.setPrefHeight(32);
        endTimeCombo.setStyle(fieldStyle);
        endTimeCombo.focusedProperty()
                .addListener((obs, oldVal, newVal) -> endTimeCombo.setStyle(newVal ? focusStyle : fieldStyle));

        ComboBox<Masjid> masjidCombo = new ComboBox<>();
        masjidCombo.setItems(masjidList);
        masjidCombo.setPromptText("Select Masjid (Optional)");
        if (existing.getMasjidId() != null) {
            for (Masjid m : masjidList) {
                if (m.getId().equals(existing.getMasjidId())) {
                    masjidCombo.setValue(m);
                    break;
                }
            }
        }
        masjidCombo.setPrefHeight(32);
        masjidCombo.setCellFactory(listView -> new ListCell<Masjid>() {
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
        masjidCombo.setStyle(fieldStyle);
        masjidCombo.focusedProperty()
                .addListener((obs, oldVal, newVal) -> masjidCombo.setStyle(newVal ? focusStyle : fieldStyle));

        TextField eventPlaceField = new TextField(existing.getEventPlace());
        eventPlaceField.setPromptText("Event Place");
        eventPlaceField.setPrefHeight(32);
        eventPlaceField.setStyle(fieldStyle);
        eventPlaceField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> eventPlaceField.setStyle(newVal ? focusStyle : fieldStyle));

        TextField contactField = new TextField(existing.getContact());
        contactField.setPromptText("Contact");
        contactField.setPrefHeight(32);
        contactField.setStyle(fieldStyle);
        contactField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> contactField.setStyle(newVal ? focusStyle : fieldStyle));

        ComboBox<String> visibilityCombo = new ComboBox<>();
        visibilityCombo.getItems().addAll("Public", "Private");
        visibilityCombo.setValue(existing.getIsPublic() ? "Public" : "Private");
        visibilityCombo.setPrefHeight(32);
        visibilityCombo.setStyle(fieldStyle);
        visibilityCombo.focusedProperty()
                .addListener((obs, oldVal, newVal) -> visibilityCombo.setStyle(newVal ? focusStyle : fieldStyle));

        TextArea eventDetailsField = new TextArea(existing.getEventDetails());
        eventDetailsField.setPromptText("Event Details");
        eventDetailsField.setPrefRowCount(2);
        eventDetailsField.setPrefHeight(50);
        eventDetailsField.setStyle(fieldStyle);
        eventDetailsField.focusedProperty()
                .addListener((obs, oldVal, newVal) -> eventDetailsField.setStyle(newVal ? focusStyle : fieldStyle));

        Button save = new Button("Update");
        save.setStyle(
                "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
        save.setOnMouseEntered(e -> save.setStyle(
                "-fx-background-color: #1d4ed8; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        save.setOnMouseExited(e -> save.setStyle(
                "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        save.setOnAction(e -> {
            existing.setEventName(eventNameField.getText());
            existing.setOrganizer(organizerField.getText());

            LocalTime startTime = parseTime(startTimeCombo.getValue());
            existing.setStartDateTime(LocalDateTime.of(startDatePicker.getValue(), startTime));

            LocalTime endTime = parseTime(endTimeCombo.getValue());
            existing.setEndDateTime(LocalDateTime.of(endDatePicker.getValue(), endTime));

            if (masjidCombo.getValue() != null) {
                existing.setMasjidId(masjidCombo.getValue().getId());
            } else {
                existing.setMasjidId(null);
            }
            existing.setEventPlace(eventPlaceField.getText());
            existing.setContact(contactField.getText());
            existing.setEventDetails(eventDetailsField.getText());
            existing.setIsPublic("Public".equals(visibilityCombo.getValue()));

            saveEvent(existing, existing, dialog);
        });

        Button cancel = new Button("Cancel");
        cancel.setStyle(
                "-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
        cancel.setOnMouseEntered(e -> cancel.setStyle(
                "-fx-background-color: #dc2626; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        cancel.setOnMouseExited(e -> cancel.setStyle(
                "-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;"));
        cancel.setOnAction(e -> dialog.close());

        HBox buttons = new HBox(10, cancel, save);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(8, 0, 0, 0));

        form.getChildren().setAll(
                title,
                FormStyler.createCompactFormField("Event Name *", eventNameField),
                FormStyler.createCompactFormField("Organizer", organizerField),
                FormStyler.createCompactFormField("Start Date", startDatePicker),
                FormStyler.createCompactFormField("Start Time", startTimeCombo),
                FormStyler.createCompactFormField("End Date", endDatePicker),
                FormStyler.createCompactFormField("End Time", endTimeCombo),
                FormStyler.createCompactFormField("Masjid (Optional)", masjidCombo),
                FormStyler.createCompactFormField("Event Place", eventPlaceField),
                FormStyler.createCompactFormField("Contact", contactField),
                FormStyler.createCompactFormField("Visibility", visibilityCombo),
                FormStyler.createCompactFormField("Event Details", eventDetailsField),
                buttons);

        scrollPane.setContent(form);
        Scene scene = new Scene(scrollPane, 500, 600);
        dialog.setScene(scene);
        dialog.show();
    }

    private void deleteEvent(Event event) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete this event?",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                new Thread(() -> {
                    boolean ok = eventDAO.delete(event.getId());
                    javafx.application.Platform.runLater(() -> {
                        if (ok) {
                            loadEventList();
                            loadCalendarEvents();
                            new Alert(Alert.AlertType.INFORMATION, "Event deleted successfully!", ButtonType.OK)
                                    .showAndWait();
                        } else {
                            new Alert(Alert.AlertType.ERROR, "Failed to delete event", ButtonType.OK).showAndWait();
                        }
                    });
                }).start();
            }
        });
    }

    private void saveEvent(Event event, Event existing, Object... formFields) {
        // Extract dialog from formFields if present (for edit dialog)
        final Stage dialog;
        if (formFields != null && formFields.length > 0) {
            Stage foundDialog = null;
            for (Object field : formFields) {
                if (field instanceof Stage) {
                    foundDialog = (Stage) field;
                    break;
                }
            }
            dialog = foundDialog;
        } else {
            dialog = null;
        }
        new Thread(() -> {
            try {
                boolean ok;
                if (existing == null) {
                    Long newId = eventDAO.create(event);
                    ok = newId != null;
                    System.out.println("Event creation result: " + ok + ", ID: " + newId);
                } else {
                    ok = eventDAO.update(event);
                    System.out.println("Event update result: " + ok);
                }
                final boolean success = ok;
                javafx.application.Platform.runLater(() -> {
                    // Re-enable save button
                    if (formFields != null && formFields.length > 11) {
                        Button saveButton = (Button) formFields[11];
                        saveButton.setDisable(false);
                        saveButton.setText("Save Event");
                    }

                    if (success) {
                        // Close dialog if editing
                        if (dialog != null) {
                            dialog.close();
                        }

                        // Reload the list to show the new/updated event
                        // Add a small delay to ensure database commit is complete
                        javafx.util.Duration delay = javafx.util.Duration.millis(100);
                        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(delay);
                        pause.setOnFinished(ev -> {
                            loadEventList();
                            loadCalendarEvents();
                        });
                        pause.play();

                        // Clear form fields if creating new event
                        if (existing == null && formFields != null && formFields.length >= 11) {
                            try {
                                TextField eventNameField = (TextField) formFields[0];
                                TextField organizerField = (TextField) formFields[1];
                                DatePicker startDatePicker = (DatePicker) formFields[2];
                                ComboBox<String> startTimeCombo = (ComboBox<String>) formFields[3];
                                DatePicker endDatePicker = (DatePicker) formFields[4];
                                ComboBox<String> endTimeCombo = (ComboBox<String>) formFields[5];
                                ComboBox<Masjid> masjidCombo = (ComboBox<Masjid>) formFields[6];
                                TextField eventPlaceField = (TextField) formFields[7];
                                TextField contactField = (TextField) formFields[8];
                                ComboBox<String> visibilityCombo = (ComboBox<String>) formFields[9];
                                TextArea eventDetailsField = (TextArea) formFields[10];

                                eventNameField.clear();
                                organizerField.clear();
                                startDatePicker.setValue(null);
                                startTimeCombo.getSelectionModel().clearSelection();
                                endDatePicker.setValue(null);
                                endTimeCombo.getSelectionModel().clearSelection();
                                masjidCombo.getSelectionModel().clearSelection();
                                eventPlaceField.clear();
                                contactField.clear();
                                visibilityCombo.setValue("Public");
                                eventDetailsField.clear();
                            } catch (Exception ex) {
                                System.err.println("Error clearing form fields: " + ex.getMessage());
                            }
                        }

                        if (existing == null) {
                            new Alert(Alert.AlertType.INFORMATION, "Event created successfully!", ButtonType.OK)
                                    .showAndWait();
                        } else {
                            new Alert(Alert.AlertType.INFORMATION, "Event updated successfully!", ButtonType.OK)
                                    .showAndWait();
                        }
                    } else {
                        new Alert(Alert.AlertType.ERROR, "Save failed", ButtonType.OK).showAndWait();
                    }
                });
            } catch (Exception ex) {
                System.err.println("Error saving event: " + ex.getMessage());
                ex.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    if (formFields != null && formFields.length > 10) {
                        Button saveButton = (Button) formFields[10];
                        saveButton.setDisable(false);
                        saveButton.setText("Save Event");
                    }
                    new Alert(Alert.AlertType.ERROR, "Error saving event: " + ex.getMessage(), ButtonType.OK)
                            .showAndWait();
                });
            }
        }).start();
    }

    private void loadMasjids() {
        new Thread(() -> {
            try {
                var data = masjidDAO.getAll();
                javafx.application.Platform.runLater(() -> {
                    masjidList.setAll(data);
                });
            } catch (Exception e) {
                System.err.println("Error loading masjids: " + e.getMessage());
            }
        }).start();
    }
}
