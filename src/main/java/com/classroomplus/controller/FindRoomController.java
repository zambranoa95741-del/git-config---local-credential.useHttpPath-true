package com.classroomplus.controller;

import com.classroomplus.model.Classroom;
import com.classroomplus.model.Reservation;
import com.classroomplus.model.ScheduleSlot;
import com.classroomplus.service.ClassroomService;
import com.classroomplus.service.ReservationService;
import com.classroomplus.service.ScheduleService;
import com.classroomplus.util.TimeUtil;
import com.classroomplus.util.Toast;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** The student's page: browse and search rooms and classes. It never reserves anything. */
public class FindRoomController {

    private static final LocalTime OPENING = LocalTime.of(7, 0);
    private static final LocalTime CLOSING = LocalTime.of(21, 0);
    private static final int DAY_MINUTES = 14 * 60;
    private static final double TIMELINE_BAR = 40;

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH);
    private static final DateTimeFormatter HOUR_FORMAT =
            DateTimeFormatter.ofPattern("h a", Locale.ENGLISH);

    /** One row of the room list: a room and the reason it cannot be used (null when it is free). */
    private record RoomResult(Classroom room, String reason, boolean tooSmall) {
        boolean available() {
            return reason == null;
        }
    }

    @FXML private DatePicker datePicker;
    @FXML private ComboBox<LocalTime> startBox;
    @FXML private ComboBox<LocalTime> endBox;
    @FXML private Spinner<Integer> seatsSpinner;
    @FXML private TextField classSearchField;
    @FXML private Label classInfoLabel;
    @FXML private Label hintLabel;
    @FXML private Label summaryLabel;
    @FXML private ListView<RoomResult> roomList;
    @FXML private VBox emptyDetail;
    @FXML private VBox detailCard;
    @FXML private Label detailTitle;
    @FXML private Label detailInfo;
    @FXML private Label detailBanner;
    @FXML private Pane timeline;

    private final ClassroomService classroomService = ClassroomService.getInstance();
    private final ScheduleService scheduleService = ScheduleService.getInstance();
    private final ReservationService reservationService = ReservationService.getInstance();
    private final ContextMenu classMenu = new ContextMenu();

    private boolean updating;

    @FXML
    private void initialize() {
        datePicker.setEditable(false);
        datePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });

        var times = FXCollections.<LocalTime>observableArrayList();
        for (LocalTime t = OPENING; !t.isAfter(CLOSING); t = t.plusMinutes(15)) {
            times.add(t);
        }
        StringConverter<LocalTime> converter = new StringConverter<>() {
            @Override
            public String toString(LocalTime time) {
                return time == null ? "" : time.format(TIME_FORMAT);
            }

            @Override
            public LocalTime fromString(String text) {
                return null;
            }
        };
        startBox.setItems(times);
        startBox.setConverter(converter);
        endBox.setItems(times);
        endBox.setConverter(converter);

        seatsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 500, 1));
        seatsSpinner.setEditable(true);
        seatsSpinner.getEditor().textProperty().addListener((observable, oldText, text) -> {
            if (text != null && text.matches("\\d{1,3}")) {
                int value = Integer.parseInt(text);
                if (value >= 1 && value <= 500) {
                    seatsSpinner.getValueFactory().setValue(value);
                }
            }
        });

        roomList.setCellFactory(list -> new ListCell<RoomResult>() {
            {
                setPrefWidth(0);
            }

            @Override
            protected void updateItem(RoomResult item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || item == null ? null : buildCard(item));
            }
        });
        roomList.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldItem, item) -> showDetail(item));
        timeline.widthProperty().addListener((observable, oldWidth, width) -> drawTimeline());

        // Search a class: typing shows the matching classes, and picking one jumps to its room.
        classSearchField.textProperty().addListener((observable, oldText, text) -> showClassMatches(text));
        classSearchField.setOnAction(event -> {
            List<List<ScheduleSlot>> groups = findClassGroups(classSearchField.getText());
            if (!groups.isEmpty()) {
                chooseClass(groups.get(0));
            }
        });

        applyDefaultTimes();

        datePicker.valueProperty().addListener((observable, oldValue, value) -> refreshResults());
        startBox.valueProperty().addListener((observable, oldValue, start) -> {
            LocalTime end = endBox.getValue();
            if (start != null && (end == null || !end.isAfter(start))) {
                boolean wasUpdating = updating;
                updating = true;
                LocalTime later = start.plusHours(1);
                endBox.setValue(later.isAfter(CLOSING) ? CLOSING : later);
                updating = wasUpdating;
            }
            refreshResults();
        });
        endBox.valueProperty().addListener((observable, oldValue, value) -> refreshResults());
        seatsSpinner.valueProperty().addListener((observable, oldValue, value) -> refreshResults());

        refreshResults();
    }

    /** Starts on the next full hour, or tomorrow morning when the day is nearly over. */
    private void applyDefaultTimes() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate date = now.toLocalDate();
        LocalTime time = now.toLocalTime();
        LocalTime start;
        if (time.isBefore(OPENING)) {
            start = OPENING.plusHours(1);
        } else if (!time.isBefore(CLOSING.minusHours(1))) {
            date = date.plusDays(1);
            start = OPENING.plusHours(1);
        } else {
            start = time.truncatedTo(ChronoUnit.HOURS).plusHours(1);
        }
        datePicker.setValue(date);
        startBox.setValue(start);
        endBox.setValue(start.plusHours(1));
    }

    @FXML
    private void onUseCurrentTime() {
        LocalTime now = LocalTime.now();
        if (!now.isBefore(CLOSING.minusMinutes(15))) {
            Toast.error(classSearchField, "It is too late to look for a room today. "
                    + "Rooms are open from 7:00 AM to 9:00 PM.");
            return;
        }
        LocalTime start = now.isBefore(OPENING)
                ? OPENING
                : now.truncatedTo(ChronoUnit.HOURS).plusMinutes((now.getMinute() / 15) * 15L);
        LocalTime end = start.plusHours(1);
        if (end.isAfter(CLOSING)) {
            end = CLOSING;
        }

        updating = true;
        datePicker.setValue(LocalDate.now());
        startBox.setValue(start);
        endBox.setValue(end);
        updating = false;
        refreshResults();
    }

    // ------------------------------------------------------- Search a class

    /** Every class whose name contains the text. The same class on several days counts once. */
    private List<List<ScheduleSlot>> findClassGroups(String text) {
        String query = text == null ? "" : text.trim().toLowerCase();
        if (query.isEmpty()) {
            return List.of();
        }
        Map<String, List<ScheduleSlot>> groups = new LinkedHashMap<>();
        for (ScheduleSlot slot : scheduleService.getSlots()) {
            if (slot.getSubject().toLowerCase().contains(query)) {
                String key = slot.getClassroomId() + "|" + slot.getSubject() + "|"
                        + slot.getStartTime() + "|" + slot.getEndTime();
                groups.computeIfAbsent(key, k -> new ArrayList<>()).add(slot);
            }
        }
        List<List<ScheduleSlot>> result = new ArrayList<>();
        for (List<ScheduleSlot> group : groups.values()) {
            group.sort(Comparator.comparing(ScheduleSlot::getDay));
            result.add(group);
            if (result.size() == 8) {
                break;
            }
        }
        return result;
    }

    private String shortDays(List<ScheduleSlot> group) {
        return group.stream()
                .map(slot -> slot.getDay().getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                .collect(Collectors.joining(", "));
    }

    private String roomName(ScheduleSlot slot) {
        return classroomService.findById(slot.getClassroomId())
                .map(Classroom::toString)
                .orElse("a removed room");
    }

    private void showClassMatches(String text) {
        classMenu.hide();
        classMenu.getItems().clear();
        if (text == null || text.isBlank()) {
            classInfoLabel.setVisible(false);
            classInfoLabel.setManaged(false);
            return;
        }
        List<List<ScheduleSlot>> groups = findClassGroups(text);
        if (groups.isEmpty()) {
            MenuItem none = new MenuItem("No class found");
            none.setDisable(true);
            classMenu.getItems().add(none);
        } else {
            for (List<ScheduleSlot> group : groups) {
                ScheduleSlot first = group.get(0);
                MenuItem item = new MenuItem(first.getSubject() + "  |  Room " + roomName(first)
                        + "  |  " + shortDays(group) + "  " + first.getTimeLabel());
                item.setOnAction(event -> chooseClass(group));
                classMenu.getItems().add(item);
            }
        }
        if (classSearchField.getScene() != null) {
            classMenu.show(classSearchField, Side.BOTTOM, 0, 0);
        }
    }

    /** Jumps to the next day the class meets and selects its room. */
    private void chooseClass(List<ScheduleSlot> group) {
        classMenu.hide();
        ScheduleSlot first = group.get(0);
        Classroom room = classroomService.findById(first.getClassroomId()).orElse(null);
        if (room == null) {
            return;
        }

        LocalDate soonest = null;
        for (ScheduleSlot slot : group) {
            LocalDate date = nextOccurrence(slot);
            if (soonest == null || date.isBefore(soonest)) {
                soonest = date;
            }
        }

        updating = true;
        datePicker.setValue(soonest);
        startBox.setValue(first.getStartTime());
        endBox.setValue(first.getEndTime());
        seatsSpinner.getValueFactory().setValue(1);
        updating = false;
        refreshResults();

        roomList.getItems().stream()
                .filter(r -> r.room().getId() == room.getId())
                .findFirst()
                .ifPresent(r -> {
                    roomList.getSelectionModel().select(r);
                    roomList.scrollTo(r);
                });

        setBanner(classInfoLabel, "info", first.getSubject() + " is in Room " + room
                + ", " + shortDays(group) + ", " + first.getTimeLabel()
                + ". Showing the next one: " + soonest.format(DATE_FORMAT) + ".");
        classInfoLabel.setVisible(true);
        classInfoLabel.setManaged(true);
    }

    private LocalDate nextOccurrence(ScheduleSlot slot) {
        LocalDate today = LocalDate.now();
        LocalDate date = today.with(TemporalAdjusters.nextOrSame(slot.getDay()));
        if (date.equals(today) && !slot.getEndTime().isAfter(LocalTime.now())) {
            date = date.plusWeeks(1);
        }
        return date;
    }

    // ------------------------------------------------------------ The results

    private void refreshResults() {
        if (updating) {
            return;
        }
        RoomResult previous = roomList.getSelectionModel().getSelectedItem();
        Integer previousId = previous == null ? null : previous.room().getId();

        LocalDate date = datePicker.getValue();
        LocalTime start = startBox.getValue();
        LocalTime end = endBox.getValue();

        String problem = validateWindow(date, start, end);
        if (problem != null) {
            setBanner(hintLabel, "bad", problem);
            hintLabel.setVisible(true);
            hintLabel.setManaged(true);
            roomList.getItems().clear();
            summaryLabel.setText("");
            showDetail(null);
            return;
        }
        hintLabel.setVisible(false);
        hintLabel.setManaged(false);

        Integer seatsValue = seatsSpinner.getValue();
        int seats = seatsValue == null ? 1 : seatsValue;

        List<RoomResult> results = classroomService.getClassrooms().stream()
                .map(room -> evaluate(room, date, start, end, seats))
                .sorted(Comparator.comparing((RoomResult r) -> !r.available())
                        .thenComparing(r -> r.room().getBuilding())
                        .thenComparing(r -> r.room().getRoomNumber()))
                .toList();
        roomList.getItems().setAll(results);

        long free = results.stream().filter(RoomResult::available).count();
        summaryLabel.setText(free + " of " + results.size() + " available");

        if (previousId != null) {
            results.stream()
                    .filter(r -> r.room().getId() == previousId)
                    .findFirst()
                    .ifPresent(r -> roomList.getSelectionModel().select(r));
        }
    }

    private RoomResult evaluate(Classroom room, LocalDate date, LocalTime start,
                                LocalTime end, int seats) {
        if (room.getCapacity() < seats) {
            return new RoomResult(room, "Seats only " + room.getCapacity()
                    + ", you need " + seats + ".", true);
        }
        return new RoomResult(room, conflictFor(room, date, start, end), false);
    }

    /** Why the room is taken, or null when it is free. Teachers' names and purposes stay private. */
    private String conflictFor(Classroom room, LocalDate date, LocalTime start, LocalTime end) {
        for (ScheduleSlot slot : scheduleService.getSlotsFor(room.getId())) {
            if (slot.getDay() == date.getDayOfWeek()
                    && TimeUtil.overlaps(start, end, slot.getStartTime(), slot.getEndTime())) {
                return "Regular class: " + slot.getSubject() + " (" + slot.getTimeLabel() + ")";
            }
        }
        for (Reservation r : reservationService.getReservations()) {
            if (!r.isCancelled()
                    && r.getClassroom().getId() == room.getId()
                    && r.getDate().equals(date)
                    && TimeUtil.overlaps(start, end, r.getStartTime(), r.getEndTime())) {
                return "Reserved (" + r.getTimeLabel() + ")";
            }
        }
        return null;
    }

    private String validateWindow(LocalDate date, LocalTime start, LocalTime end) {
        if (date == null) {
            return "Select a date.";
        }
        if (start == null || end == null) {
            return "Select a start and end time.";
        }
        if (!end.isAfter(start)) {
            return "End time must be after start time.";
        }
        if (!LocalDateTime.of(date, end).isAfter(LocalDateTime.now())) {
            return "That time has already passed.";
        }
        return null;
    }

    private VBox buildCard(RoomResult result) {
        Classroom room = result.room();

        Label title = new Label("Room " + room.getRoomNumber());
        title.getStyleClass().add("room-title");

        String badgeText = result.tooSmall() ? "Too small" : result.available() ? "Available" : "Unavailable";
        String badgeStyle = result.tooSmall() ? "badge-completed"
                : result.available() ? "badge-available" : "badge-unavailable";
        Label badge = new Label(badgeText);
        badge.getStyleClass().addAll("badge", badgeStyle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(8, title, spacer, badge);
        top.setAlignment(Pos.CENTER_LEFT);

        Label info = new Label(room.getBuilding() + "  |  " + room.getCapacity() + " seats");
        info.getStyleClass().add("muted");

        VBox card = new VBox(4, top, info);
        card.getStyleClass().add("room-card");
        card.prefWidthProperty().bind(roomList.widthProperty().subtract(40));

        if (!result.available()) {
            Label reason = new Label(result.reason());
            reason.getStyleClass().add("room-reason");
            reason.setWrapText(true);
            card.getChildren().add(reason);
        }
        return card;
    }

    // ------------------------------------------------------------ The details

    private void showDetail(RoomResult result) {
        boolean show = result != null;
        detailCard.setVisible(show);
        detailCard.setManaged(show);
        emptyDetail.setVisible(!show);
        emptyDetail.setManaged(!show);
        if (!show) {
            timeline.getChildren().clear();
            return;
        }

        Classroom room = result.room();
        detailTitle.setText("Room " + room.getRoomNumber() + "  |  " + room.getBuilding());
        detailInfo.setText(room.getCapacity() + " seats  |  "
                + (room.getFacilities().isBlank() ? "No facilities listed" : room.getFacilities()));

        String when = datePicker.getValue().format(DATE_FORMAT) + ", "
                + startBox.getValue().format(TIME_FORMAT) + " - "
                + endBox.getValue().format(TIME_FORMAT);
        if (result.available()) {
            setBanner(detailBanner, "ok", "Available. This room is free on " + when + ".");
        } else {
            setBanner(detailBanner, "bad", "Not available. " + result.reason());
        }
        drawTimeline();
    }

    private void drawTimeline() {
        timeline.getChildren().clear();
        RoomResult result = roomList.getSelectionModel().getSelectedItem();
        LocalDate date = datePicker.getValue();
        LocalTime start = startBox.getValue();
        LocalTime end = endBox.getValue();
        double width = timeline.getWidth();
        if (result == null || date == null || start == null || end == null || width <= 0) {
            return;
        }

        double perMinute = width / DAY_MINUTES;
        Classroom room = result.room();

        Rectangle track = new Rectangle(0, 0, width, TIMELINE_BAR);
        track.setArcWidth(10);
        track.setArcHeight(10);
        track.getStyleClass().add("tl-track");
        timeline.getChildren().add(track);

        for (ScheduleSlot slot : scheduleService.getSlotsFor(room.getId())) {
            if (slot.getDay() == date.getDayOfWeek()) {
                addBlock(perMinute, slot.getStartTime(), slot.getEndTime(), "tl-class",
                        slot.getSubject() + "\n" + slot.getTimeLabel());
            }
        }
        for (Reservation r : reservationService.getReservations()) {
            if (!r.isCancelled() && r.getClassroom().getId() == room.getId()
                    && r.getDate().equals(date)) {
                addBlock(perMinute, r.getStartTime(), r.getEndTime(), "tl-reservation",
                        "Reserved\n" + r.getTimeLabel());
            }
        }
        addBlock(perMinute, start, end, result.available() ? "tl-selection-ok" : "tl-selection-bad",
                "Your selection\n" + start.format(TIME_FORMAT) + " - " + end.format(TIME_FORMAT));

        for (int hour = 7; hour <= 21; hour += 2) {
            double x = (hour - 7) * 60 * perMinute;
            Line tick = new Line(x, TIMELINE_BAR, x, TIMELINE_BAR + 6);
            tick.getStyleClass().add("tl-tick");
            Label label = new Label(LocalTime.of(hour, 0).format(HOUR_FORMAT));
            label.getStyleClass().add("tl-label");
            label.setLayoutX(Math.max(0, Math.min(width - 34, x - 14)));
            label.setLayoutY(TIMELINE_BAR + 8);
            timeline.getChildren().addAll(tick, label);
        }
    }

    private void addBlock(double perMinute, LocalTime from, LocalTime to, String styleClass, String tip) {
        double startMinute = Math.max(0, minutesFromOpening(from));
        double endMinute = Math.min(DAY_MINUTES, minutesFromOpening(to));
        if (endMinute <= startMinute) {
            return;
        }
        Rectangle block = new Rectangle(startMinute * perMinute, 0,
                (endMinute - startMinute) * perMinute, TIMELINE_BAR);
        block.setArcWidth(6);
        block.setArcHeight(6);
        block.getStyleClass().add(styleClass);
        Tooltip.install(block, new Tooltip(tip));
        timeline.getChildren().add(block);
    }

    private double minutesFromOpening(LocalTime time) {
        return time.getHour() * 60 + time.getMinute() - OPENING.getHour() * 60;
    }

    private void setBanner(Label label, String kind, String text) {
        label.getStyleClass().removeAll("banner", "banner-info", "banner-ok", "banner-bad");
        label.getStyleClass().addAll("banner", "banner-" + kind);
        label.setText(text);
    }
}