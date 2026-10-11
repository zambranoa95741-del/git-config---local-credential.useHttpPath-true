package com.classroomplus.controller;

import com.classroomplus.model.Reservation;
import com.classroomplus.model.ReservationStatus;
import com.classroomplus.service.ReservationService;
import com.classroomplus.util.Session;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class MyReservationsController {

    private static final String ALL = "All reservations";
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    @FXML private Label upcomingCount;
    @FXML private Label ongoingCount;
    @FXML private Label completedCount;
    @FXML private Label cancelledCount;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Button editButton;
    @FXML private Button cancelButton;

    @FXML private TableView<Reservation> table;
    @FXML private TableColumn<Reservation, String> roomCol;
    @FXML private TableColumn<Reservation, String> dateCol;
    @FXML private TableColumn<Reservation, String> timeCol;
    @FXML private TableColumn<Reservation, String> purposeCol;
    @FXML private TableColumn<Reservation, ReservationStatus> statusCol;

    private final ReservationService reservationService = ReservationService.getInstance();
    private final ObservableList<Reservation> shown = FXCollections.observableArrayList();
    private Timeline clock;

    @FXML
    private void initialize() {
        roomCol.setCellValueFactory(new PropertyValueFactory<>("roomLabel"));
        purposeCol.setCellValueFactory(new PropertyValueFactory<>("purpose"));
        dateCol.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getDate().format(DATE_FORMAT)));
        timeCol.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getStartTime().format(TIME_FORMAT) + " - "
                        + cell.getValue().getEndTime().format(TIME_FORMAT)));

        statusCol.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(cell.getValue().getStatus()));
        statusCol.setCellFactory(column -> new TableCell<Reservation, ReservationStatus>() {
            @Override
            protected void updateItem(ReservationStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    return;
                }
                Label badge = new Label(status.toString());
                badge.getStyleClass().addAll("badge", "badge-" + status.name().toLowerCase());
                setGraphic(badge);
            }
        });

        table.setItems(shown);
        table.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> updateButtons());

        statusFilter.getItems().addAll(ALL, "Upcoming", "Ongoing", "Completed", "Cancelled");
        statusFilter.setValue(ALL);
        statusFilter.valueProperty().addListener((observable, oldValue, newValue) -> refresh());

        refresh();

        // Statuses follow the clock, so the page refreshes itself every 30 seconds.
        clock = new Timeline(new KeyFrame(Duration.seconds(30), event -> {
            if (table.getScene() == null) {
                clock.stop(); // the page was closed
                return;
            }
            refresh();
        }));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    private void refresh() {
        Reservation selected = table.getSelectionModel().getSelectedItem();
        List<Reservation> all = reservationService.getReservationsFor(Session.getCurrentUser());

        upcomingCount.setText(String.valueOf(count(all, ReservationStatus.UPCOMING)));
        ongoingCount.setText(String.valueOf(count(all, ReservationStatus.ONGOING)));
        completedCount.setText(String.valueOf(count(all, ReservationStatus.COMPLETED)));
        cancelledCount.setText(String.valueOf(count(all, ReservationStatus.CANCELLED)));

        String filter = statusFilter.getValue();
        shown.setAll(all.stream()
                .filter(r -> filter == null || filter.equals(ALL)
                        || r.getStatus().toString().equals(filter))
                .toList());

        if (selected != null && shown.contains(selected)) {
            table.getSelectionModel().select(selected);
        }
        updateButtons();
    }

    private long count(List<Reservation> reservations, ReservationStatus status) {
        return reservations.stream().filter(r -> r.getStatus() == status).count();
    }

    private void updateButtons() {
        Reservation selected = table.getSelectionModel().getSelectedItem();
        boolean canChange = selected != null && selected.getStatus() == ReservationStatus.UPCOMING;
        editButton.setDisable(!canChange);
        cancelButton.setDisable(!canChange);
    }

    // ---------------------------------------------------------------- Update

    @FXML
    private void onEdit() {
        Reservation reservation = table.getSelectionModel().getSelectedItem();
        if (reservation == null) {
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit reservation");
        dialog.setHeaderText("Room " + reservation.getRoomLabel());
        dialog.initOwner(table.getScene().getWindow());
        dialog.getDialogPane().getStylesheets().addAll(table.getScene().getStylesheets());

        ButtonType saveType = new ButtonType("Save changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        DatePicker date = new DatePicker(reservation.getDate());
        date.setEditable(false);
        date.setMaxWidth(Double.MAX_VALUE);
        date.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setDisable(empty || item.isBefore(LocalDate.now()));
            }
        });
        ComboBox<LocalTime> start = timeBox(reservation.getStartTime());
        ComboBox<LocalTime> end = timeBox(reservation.getEndTime());
        TextField purpose = new TextField(reservation.getPurpose());
        Label error = new Label();
        error.getStyleClass().add("error-label");
        error.setWrapText(true);
        error.setMaxWidth(320);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 4, 0, 4));
        grid.add(new Label("Date"), 0, 0);
        grid.add(date, 1, 0);
        grid.add(new Label("Start time"), 0, 1);
        grid.add(start, 1, 1);
        grid.add(new Label("End time"), 0, 2);
        grid.add(end, 1, 2);
        grid.add(new Label("Purpose"), 0, 3);
        grid.add(purpose, 1, 3);
        grid.add(error, 0, 4, 2, 1);
        dialog.getDialogPane().setContent(grid);

        // Keep the dialog open and show the reason when the change is not allowed.
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                reservationService.update(reservation, Session.getCurrentUser(), date.getValue(),
                        start.getValue(), end.getValue(), purpose.getText());
            } catch (IllegalArgumentException e) {
                error.setText(e.getMessage());
                event.consume();
            }
        });

        dialog.showAndWait().ifPresent(result -> refresh());
    }

    private ComboBox<LocalTime> timeBox(LocalTime selected) {
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
        box.setValue(selected);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    // ---------------------------------------------------------------- Delete

    @FXML
    private void onCancel() {
        Reservation reservation = table.getSelectionModel().getSelectedItem();
        if (reservation == null) {
            return;
        }

        ButtonType yes = new ButtonType("Cancel reservation", ButtonBar.ButtonData.OK_DONE);
        ButtonType no = new ButtonType("Keep it", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Room " + reservation.getRoomLabel() + "\n"
                        + reservation.getDate().format(DATE_FORMAT) + ", "
                        + reservation.getStartTime().format(TIME_FORMAT) + " - "
                        + reservation.getEndTime().format(TIME_FORMAT),
                yes, no);
        confirm.setTitle("Cancel reservation");
        confirm.setHeaderText("Cancel this reservation?");
        confirm.initOwner(table.getScene().getWindow());
        confirm.getDialogPane().getStylesheets().addAll(table.getScene().getStylesheets());

        confirm.showAndWait().filter(button -> button == yes).ifPresent(button -> {
            try {
                reservationService.cancel(reservation, Session.getCurrentUser());
            } catch (IllegalArgumentException e) {
                Alert problem = new Alert(Alert.AlertType.WARNING, e.getMessage());
                problem.setHeaderText(null);
                problem.showAndWait();
            }
            refresh();
        });
    }
}