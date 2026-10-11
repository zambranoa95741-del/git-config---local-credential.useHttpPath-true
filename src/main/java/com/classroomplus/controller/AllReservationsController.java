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
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.util.Duration;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class AllReservationsController {

    private static final String ALL = "All statuses";
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Label countLabel;
    @FXML private Button cancelButton;

    @FXML private TableView<Reservation> table;
    @FXML private TableColumn<Reservation, String> teacherCol;
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
        teacherCol.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getReservedBy().getFullName()));
        roomCol.setCellValueFactory(new PropertyValueFactory<>("roomLabel"));
        dateCol.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getDate().format(DATE_FORMAT)));
        timeCol.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getStartTime().format(TIME_FORMAT) + " - "
                        + cell.getValue().getEndTime().format(TIME_FORMAT)));
        purposeCol.setCellValueFactory(new PropertyValueFactory<>("purpose"));

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
                .addListener((observable, oldValue, newValue) -> updateButton());
        table.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE) {
                onCancel();
            }
        });

        statusFilter.getItems().addAll(ALL, "Upcoming", "Ongoing", "Completed", "Cancelled");
        statusFilter.setValue(ALL);
        statusFilter.valueProperty().addListener((observable, oldValue, newValue) -> refresh());
        searchField.textProperty().addListener((observable, oldValue, newValue) -> refresh());

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
        List<Reservation> all = reservationService.getReservations().stream()
                .sorted(Comparator.comparing(Reservation::getDate)
                        .thenComparing(Reservation::getStartTime)
                        .reversed())
                .toList();

        String status = statusFilter.getValue();
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();

        List<Reservation> matching = all.stream()
                .filter(r -> status == null || status.equals(ALL) || r.getStatus().toString().equals(status))
                .filter(r -> query.isEmpty()
                        || r.getReservedBy().getFullName().toLowerCase().contains(query)
                        || r.getRoomLabel().toLowerCase().contains(query)
                        || r.getPurpose().toLowerCase().contains(query))
                .toList();

        shown.setAll(matching);
        countLabel.setText(matching.size() + " of " + all.size() + " reservations");

        if (selected != null && shown.contains(selected)) {
            table.getSelectionModel().select(selected);
        }
        updateButton();
    }

    private void updateButton() {
        Reservation selected = table.getSelectionModel().getSelectedItem();
        boolean canCancel = selected != null
                && (selected.getStatus() == ReservationStatus.UPCOMING
                || selected.getStatus() == ReservationStatus.ONGOING);
        cancelButton.setDisable(!canCancel);
    }

    @FXML
    private void onCancel() {
        Reservation reservation = table.getSelectionModel().getSelectedItem();
        if (reservation == null || cancelButton.isDisabled()) {
            return;
        }

        ButtonType yes = new ButtonType("Cancel reservation", ButtonBar.ButtonData.OK_DONE);
        ButtonType no = new ButtonType("Keep it", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                reservation.getReservedBy().getFullName() + "\n"
                        + "Room " + reservation.getRoomLabel() + "\n"
                        + reservation.getDate().format(DATE_FORMAT) + ", "
                        + reservation.getStartTime().format(TIME_FORMAT) + " - "
                        + reservation.getEndTime().format(TIME_FORMAT)
                        + "\n\nThe teacher will see this reservation marked as Cancelled.",
                yes, no);
        confirm.setTitle("Cancel reservation");
        confirm.setHeaderText("Cancel this teacher's reservation?");
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