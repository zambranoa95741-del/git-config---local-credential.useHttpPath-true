package com.classroomplus.controller;


import com.classroomplus.model.Classroom;
import com.classroomplus.model.Reservation;
import com.classroomplus.model.ReservationStatus;
import com.classroomplus.model.User;
import com.classroomplus.service.ClassroomService;
import com.classroomplus.service.ReservationService;
import com.classroomplus.util.Session;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;

import javafx.scene.layout.VBox;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class DashboardController {

    private static final LocalTime OPENING = LocalTime.of(7, 0);
    private static final LocalTime CLOSING = LocalTime.of(21, 0);
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    @FXML private Label greetingLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label totalRooms;
    @FXML private Label availableNow;
    @FXML private Label inUseNow;
    @FXML private Label upcomingCount;
    @FXML private Label hoursNote;

    @FXML private VBox upcomingCard;
    @FXML private VBox nextCard;
    @FXML private TableView<Classroom> roomTable;
    @FXML private TableColumn<Classroom, String> roomCol;
    @FXML private TableColumn<Classroom, String> buildingCol;
    @FXML private TableColumn<Classroom, Classroom> statusCol;

    @FXML private TableView<Reservation> nextTable;
    @FXML private TableColumn<Reservation, String> nRoomCol;
    @FXML private TableColumn<Reservation, String> nDateCol;
    @FXML private TableColumn<Reservation, String> nTimeCol;
    @FXML private TableColumn<Reservation, ReservationStatus> nStatusCol;

    private final ClassroomService classroomService = ClassroomService.getInstance();
    private final ReservationService reservationService = ReservationService.getInstance();
    private Timeline clock;

    @FXML
    private void initialize() {
        User user = Session.getCurrentUser();
        int hour = LocalTime.now().getHour();
        String part = hour < 12 ? "Good morning" : hour < 18 ? "Good afternoon" : "Good evening";
        greetingLabel.setText(part + ", " + user.getFullName());
        subtitleLabel.setText("Here is what is happening in the classrooms today.");
        if (user.isStudent()) {
            subtitleLabel.setText("Browse the classrooms and see which ones are free right now.");
            for (VBox card : new VBox[]{upcomingCard, nextCard}) {
                card.setVisible(false);
                card.setManaged(false);
            }
        }

        // Rooms table
        roomCol.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        buildingCol.setCellValueFactory(new PropertyValueFactory<>("building"));
        statusCol.setSortable(false);
        statusCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        statusCol.setCellFactory(column -> new TableCell<Classroom, Classroom>() {
            @Override
            protected void updateItem(Classroom room, boolean empty) {
                super.updateItem(room, empty);
                setGraphic(empty || room == null ? null : roomBadge(room));
            }
        });
        roomTable.setItems(classroomService.getClassrooms());

        // Next reservations table
        nRoomCol.setCellValueFactory(new PropertyValueFactory<>("roomLabel"));
        nDateCol.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getDate().format(DATE_FORMAT)));
        nTimeCol.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getStartTime().format(TIME_FORMAT) + " - "
                        + cell.getValue().getEndTime().format(TIME_FORMAT)));
        nStatusCol.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(cell.getValue().getStatus()));
        nStatusCol.setCellFactory(column -> new TableCell<Reservation, ReservationStatus>() {
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

        refresh();

        // Everything depends on the clock, so the page refreshes itself every 30 seconds.
        clock = new Timeline(new KeyFrame(Duration.seconds(30), event -> {
            if (roomTable.getScene() == null) {
                clock.stop(); // the page was closed
                return;
            }
            refresh();
        }));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    private boolean isOpen() {
        LocalTime now = LocalTime.now();
        return !now.isBefore(OPENING) && now.isBefore(CLOSING);
    }

    private Label roomBadge(Classroom room) {
        Label badge;
        if (!isOpen()) {
            badge = new Label("Closed");
            badge.getStyleClass().addAll("badge", "badge-completed");
        } else if (reservationService.isAvailableNow(room)) {
            badge = new Label("Available now");
            badge.getStyleClass().addAll("badge", "badge-available");
        } else {
            badge = new Label("In use");
            badge.getStyleClass().addAll("badge", "badge-unavailable");
        }
        return badge;
    }

    private void refresh() {
        User user = Session.getCurrentUser();
        List<Classroom> rooms = classroomService.getClassrooms();
        boolean open = isOpen();

        long available = open ? rooms.stream().filter(reservationService::isAvailableNow).count() : 0;
        long inUse = open ? rooms.size() - available : 0;

        totalRooms.setText(String.valueOf(rooms.size()));
        availableNow.setText(String.valueOf(available));
        inUseNow.setText(String.valueOf(inUse));
        upcomingCount.setText(String.valueOf(
                reservationService.countFor(user, ReservationStatus.UPCOMING)));
        hoursNote.setText(open
                ? "Rooms are open from 7:00 AM to 9:00 PM."
                : "Rooms are closed right now. Opening hours are 7:00 AM to 9:00 PM.");

        List<Reservation> next = reservationService.getReservationsFor(user).stream()
                .filter(r -> r.getStatus() == ReservationStatus.UPCOMING
                        || r.getStatus() == ReservationStatus.ONGOING)
                .sorted(Comparator.comparing(Reservation::getDate)
                        .thenComparing(Reservation::getStartTime))
                .limit(5)
                .toList();
        nextTable.setItems(FXCollections.observableArrayList(next));

        roomTable.refresh();
    }
}