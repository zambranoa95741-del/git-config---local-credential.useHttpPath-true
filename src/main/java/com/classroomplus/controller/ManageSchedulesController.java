package com.classroomplus.controller;

import com.classroomplus.model.Classroom;
import com.classroomplus.model.Reservation;
import com.classroomplus.model.ReservationStatus;
import com.classroomplus.model.ScheduleSlot;
import com.classroomplus.service.ClassroomService;
import com.classroomplus.service.ReservationService;
import com.classroomplus.service.ScheduleService;
import com.classroomplus.util.TimeUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

public class ManageSchedulesController {

    private static final String ALL_ROOMS = "All rooms";
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH);

    @FXML private ComboBox<String> roomFilter;
    @FXML private Label countLabel;
    @FXML private Button editButton;
    @FXML private Button deleteButton;

    @FXML private TableView<ScheduleSlot> table;
    @FXML private TableColumn<ScheduleSlot, String> roomCol;
    @FXML private TableColumn<ScheduleSlot, String> dayCol;
    @FXML private TableColumn<ScheduleSlot, String> timeCol;
    @FXML private TableColumn<ScheduleSlot, String> subjectCol;
    @FXML private TableColumn<ScheduleSlot, String> instructorCol;

    private final ClassroomService classroomService = ClassroomService.getInstance();
    private final ScheduleService scheduleService = ScheduleService.getInstance();
    private final ReservationService reservationService = ReservationService.getInstance();

    private FilteredList<ScheduleSlot> filtered;

    @FXML
    private void initialize() {
        roomFilter.getItems().add(ALL_ROOMS);
        for (Classroom room : classroomService.getClassrooms()) {
            roomFilter.getItems().add(room.toString());
        }
        roomFilter.setValue(ALL_ROOMS);

        roomCol.setCellValueFactory(cell ->
                new SimpleStringProperty(roomLabel(cell.getValue())));
        dayCol.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getDay().getDisplayName(TextStyle.FULL, Locale.ENGLISH)));
        timeCol.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getStartTime().format(TIME_FORMAT) + " - "
                        + cell.getValue().getEndTime().format(TIME_FORMAT)));
        subjectCol.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getSubject()));
        instructorCol.setCellValueFactory(cell -> {
            String name = cell.getValue().getInstructor();
            return new SimpleStringProperty(name == null || name.isBlank() ? "-" : name);
        });

        filtered = new FilteredList<>(scheduleService.getSlots(), slot -> true);
        SortedList<ScheduleSlot> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);

        roomFilter.valueProperty().addListener((observable, oldValue, newValue) -> applyFilter());
        table.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> updateButtons());

        // Desktop shortcuts: double-click a row to edit it, press Delete to remove it.
        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && table.getSelectionModel().getSelectedItem() != null) {
                onEdit();
            }
        });
        table.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE) {
                onDelete();
            }
        });

        applyFilter();
        updateButtons();
    }

    private String roomLabel(ScheduleSlot slot) {
        return classroomService.findById(slot.getClassroomId())
                .map(Classroom::toString)
                .orElse("Deleted room");
    }

    private void applyFilter() {
        String label = roomFilter.getValue();
        filtered.setPredicate(slot ->
                label == null || label.equals(ALL_ROOMS) || roomLabel(slot).equals(label));
        updateCount();
    }

    private void updateCount() {
        countLabel.setText(filtered.size() + " of " + scheduleService.getSlots().size() + " classes");
    }

    private void updateButtons() {
        boolean none = table.getSelectionModel().getSelectedItem() == null;
        editButton.setDisable(none);
        deleteButton.setDisable(none);
    }

    private Classroom roomFromFilter() {
        String label = roomFilter.getValue();
        if (label == null || label.equals(ALL_ROOMS)) {
            return null;
        }
        return classroomService.getClassrooms().stream()
                .filter(room -> room.toString().equals(label))
                .findFirst()
                .orElse(null);
    }

    // ---------------------------------------------------------------- Create

    @FXML
    private void onAdd() {
        if (classroomService.getClassrooms().isEmpty()) {
            Alert info = new Alert(Alert.AlertType.INFORMATION,
                    "Add a classroom in Manage Rooms first, then add its classes here.");
            info.setHeaderText(null);
            style(info);
            info.showAndWait();
            return;
        }
        showClassDialog(null);
    }

    // ---------------------------------------------------------------- Update

    @FXML
    private void onEdit() {
        ScheduleSlot slot = table.getSelectionModel().getSelectedItem();
        if (slot != null) {
            showClassDialog(slot);
        }
    }

    /** One form for both adding (existing is null) and editing a regular class. */
    private void showClassDialog(ScheduleSlot existing) {
        boolean editing = existing != null;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(editing ? "Edit class" : "Add class");
        dialog.setHeaderText(editing ? "Edit the regular class in " + roomLabel(existing)
                : "Add a regular class to the semester schedule");
        style(dialog);

        ButtonType saveType = new ButtonType(editing ? "Save changes" : "Add class",
                ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        ComboBox<Classroom> roomBox = new ComboBox<>(classroomService.getClassrooms());
        roomBox.setMaxWidth(Double.MAX_VALUE);
        roomBox.setPromptText("Select a classroom");
        if (editing) {
            roomBox.setValue(classroomService.findById(existing.getClassroomId()).orElse(null));
            roomBox.setDisable(true); // to move a class, delete it and add it to the other room
        } else {
            roomBox.setValue(roomFromFilter());
        }

        ComboBox<DayOfWeek> dayBox = new ComboBox<>();
        dayBox.getItems().addAll(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY);
        dayBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(DayOfWeek day) {
                return day == null ? "" : day.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            }

            @Override
            public DayOfWeek fromString(String text) {
                return null;
            }
        });
        dayBox.setMaxWidth(Double.MAX_VALUE);
        dayBox.setPromptText("Select a day");
        if (editing) {
            dayBox.setValue(existing.getDay());
        }

        ComboBox<LocalTime> startBox = timeBox(editing ? existing.getStartTime() : null, "Start");
        ComboBox<LocalTime> endBox = timeBox(editing ? existing.getEndTime() : null, "End");

        TextField subject = new TextField(editing ? existing.getSubject() : "");
        subject.setPromptText("e.g. BSIT-2105 Information Management");
        TextField instructor = new TextField(editing ? existing.getInstructor() : "");
        instructor.setPromptText("Optional");

        Label error = new Label();
        error.getStyleClass().add("error-label");
        error.setWrapText(true);
        error.setMaxWidth(360);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 4, 0, 4));
        grid.add(new Label("Room"), 0, 0);
        grid.add(roomBox, 1, 0);
        grid.add(new Label("Day"), 0, 1);
        grid.add(dayBox, 1, 1);
        grid.add(new Label("Start time"), 0, 2);
        grid.add(startBox, 1, 2);
        grid.add(new Label("End time"), 0, 3);
        grid.add(endBox, 1, 3);
        grid.add(new Label("Class"), 0, 4);
        grid.add(subject, 1, 4);
        grid.add(new Label("Instructor"), 0, 5);
        grid.add(instructor, 1, 5);
        grid.add(error, 0, 6, 2, 1);
        dialog.getDialogPane().setContent(grid);

        ScheduleSlot[] saved = new ScheduleSlot[1];

        // Keep the form open and show the reason when the input is not valid.
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                Classroom room = roomBox.getValue();
                if (room == null) {
                    throw new IllegalArgumentException("Select a classroom.");
                }
                checkReservations(room.getId(), dayBox.getValue(),
                        startBox.getValue(), endBox.getValue());
                if (editing) {
                    scheduleService.update(existing, dayBox.getValue(), startBox.getValue(),
                            endBox.getValue(), subject.getText(), instructor.getText());
                    saved[0] = existing;
                } else {
                    saved[0] = scheduleService.add(room.getId(), dayBox.getValue(),
                            startBox.getValue(), endBox.getValue(),
                            subject.getText(), instructor.getText());
                }
            } catch (IllegalArgumentException e) {
                error.setText(e.getMessage());
                event.consume();
            }
        });

        dialog.showAndWait();

        if (saved[0] != null) {
            // Show the room that was just changed, so the new row is visible.
            roomFilter.setValue(roomLabel(saved[0]));
            table.refresh();
            table.getSelectionModel().select(saved[0]);
            table.scrollTo(saved[0]);
        }
        updateCount();
    }

    /** A regular class must not land on top of a teacher's active reservation. */
    private void checkReservations(int classroomId, DayOfWeek day, LocalTime start, LocalTime end) {
        if (day == null || start == null || end == null) {
            return; // the schedule service reports the missing input
        }
        for (Reservation r : reservationService.getReservations()) {
            boolean active = r.getStatus() == ReservationStatus.UPCOMING
                    || r.getStatus() == ReservationStatus.ONGOING;
            if (active
                    && r.getClassroom().getId() == classroomId
                    && r.getDate().getDayOfWeek() == day
                    && TimeUtil.overlaps(start, end, r.getStartTime(), r.getEndTime())) {
                throw new IllegalArgumentException("A teacher already reserved this room on "
                        + r.getDate().format(DATE_FORMAT) + ", "
                        + r.getStartTime().format(TIME_FORMAT) + " - "
                        + r.getEndTime().format(TIME_FORMAT)
                        + ". Ask them to cancel it first.");
            }
        }
    }

    private ComboBox<LocalTime> timeBox(LocalTime selected, String prompt) {
        ComboBox<LocalTime> box = new ComboBox<>();
        for (LocalTime t = LocalTime.of(7, 0); !t.isAfter(LocalTime.of(21, 0)); t = t.plusMinutes(15)) {
            box.getItems().add(t);
        }
        box.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalTime time) {
                return time == null ? "" : time.format(TIME_FORMAT);
            }

            @Override
            public LocalTime fromString(String text) {
                return null;
            }
        });
        box.setPromptText(prompt);
        box.setValue(selected);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    // ---------------------------------------------------------------- Delete

    @FXML
    private void onDelete() {
        ScheduleSlot slot = table.getSelectionModel().getSelectedItem();
        if (slot == null) {
            return;
        }

        ButtonType yes = new ButtonType("Delete class", ButtonBar.ButtonData.OK_DONE);
        ButtonType no = new ButtonType("Keep it", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                slot.getSubject() + "\n" + roomLabel(slot) + "\n"
                        + slot.getDay().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + ", "
                        + slot.getStartTime().format(TIME_FORMAT) + " - "
                        + slot.getEndTime().format(TIME_FORMAT)
                        + "\n\nThe room becomes free at this time for reservations.",
                yes, no);
        confirm.setTitle("Delete class");
        confirm.setHeaderText("Delete this regular class?");
        style(confirm);

        confirm.showAndWait().filter(button -> button == yes).ifPresent(button -> {
            scheduleService.delete(slot);
            updateCount();
        });
    }

    private void style(Dialog<?> dialog) {
        dialog.initOwner(table.getScene().getWindow());
        dialog.getDialogPane().getStylesheets().addAll(table.getScene().getStylesheets());
    }
}