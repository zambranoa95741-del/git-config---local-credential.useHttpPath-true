package com.classroomplus.controller;

import com.classroomplus.model.Classroom;
import com.classroomplus.model.Reservation;
import com.classroomplus.model.ScheduleSlot;
import com.classroomplus.service.ClassroomService;
import com.classroomplus.service.ReservationService;
import com.classroomplus.service.ScheduleService;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import com.classroomplus.util.Session;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.Locale;
import java.util.TreeSet;

public class ClassroomsController {

    private static final String ALL_BUILDINGS = "All buildings";
    private static final LocalTime OPENING = LocalTime.of(7, 0);
    private static final LocalTime CLOSING = LocalTime.of(21, 0);
    private static final int HOURS = 14;
    private static final double GUTTER = 54;
    private static final double HEADER = 34;
    private static final DayOfWeek[] DAYS = {
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY};

    private static final DateTimeFormatter HOUR_FORMAT =
            DateTimeFormatter.ofPattern("h a", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH_DAY =
            DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH_DAY_YEAR =
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    @FXML private TextField searchField;
    @FXML private ComboBox<String> buildingFilter;
    @FXML private CheckBox availableOnly;
    @FXML private Label countLabel;
    @FXML private ListView<Classroom> roomList;

    @FXML private VBox emptyDetail;
    @FXML private VBox detailCard;
    @FXML private Label detailTitle;
    @FXML private Label detailInfo;
    @FXML private Label detailBadge;
    @FXML private FlowPane detailChips;
    @FXML private Label weekLabel;
    @FXML private Pane weekGrid;

    private final ClassroomService classroomService = ClassroomService.getInstance();
    private final ScheduleService scheduleService = ScheduleService.getInstance();
    private final ReservationService reservationService = ReservationService.getInstance();

    private FilteredList<Classroom> filtered;
    private LocalDate weekStart = mondayOf(LocalDate.now());
    private Timeline clock;

    @FXML
    private void initialize() {
        TreeSet<String> buildings = new TreeSet<>();
        for (Classroom room : classroomService.getClassrooms()) {
            buildings.add(room.getBuilding());
        }
        buildingFilter.getItems().add(ALL_BUILDINGS);
        buildingFilter.getItems().addAll(buildings);
        buildingFilter.setValue(ALL_BUILDINGS);

        filtered = new FilteredList<>(classroomService.getClassrooms(), room -> true);
        SortedList<Classroom> sorted = new SortedList<>(filtered,
                Comparator.comparing(Classroom::getBuilding).thenComparing(Classroom::getRoomNumber));
        roomList.setItems(sorted);

        roomList.setCellFactory(list -> new ListCell<Classroom>() {
            {
                setPrefWidth(0);
            }

            @Override
            protected void updateItem(Classroom room, boolean empty) {
                super.updateItem(room, empty);
                setText(null);
                setGraphic(empty || room == null ? null : buildCard(room));
            }
        });
        roomList.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldRoom, room) -> showDetail(room));

        searchField.textProperty().addListener((observable, oldValue, value) -> applyFilter());
        buildingFilter.valueProperty().addListener((observable, oldValue, value) -> applyFilter());
        availableOnly.selectedProperty().addListener((observable, oldValue, value) -> applyFilter());

        weekGrid.widthProperty().addListener((observable, oldValue, value) -> drawWeek());
        weekGrid.heightProperty().addListener((observable, oldValue, value) -> drawWeek());

        updateWeekLabel();
        applyFilter();
        roomList.getSelectionModel().selectFirst();

        // Availability depends on the clock, so the page refreshes itself every 30 seconds.
        clock = new Timeline(new KeyFrame(Duration.seconds(30), event -> {
            if (roomList.getScene() == null) {
                clock.stop(); // the page was closed
                return;
            }
            if (availableOnly.isSelected()) {
                applyFilter();
            }
            roomList.refresh();
            Classroom selected = roomList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                styleBadge(detailBadge, selected);
            }
            drawWeek();
        }));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    // ------------------------------------------------------------ The list

    private void applyFilter() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String building = buildingFilter.getValue();
        boolean freeOnly = availableOnly.isSelected();

        filtered.setPredicate(room -> {
            boolean inBuilding = building == null || building.equals(ALL_BUILDINGS)
                    || room.getBuilding().equals(building);
            boolean matches = query.isEmpty()
                    || room.getRoomNumber().toLowerCase().contains(query)
                    || room.getBuilding().toLowerCase().contains(query)
                    || room.getFacilities().toLowerCase().contains(query)
                    || scheduleService.getSlotsFor(room.getId()).stream()
                    .anyMatch(slot -> slot.getSubject().toLowerCase().contains(query));
            boolean free = !freeOnly || (isOpen() && reservationService.isAvailableNow(room));
            return inBuilding && matches && free;
        });
        countLabel.setText(filtered.size() + " of " + classroomService.getClassrooms().size() + " rooms");
    }

    private VBox buildCard(Classroom room) {
        Label title = new Label("Room " + room.getRoomNumber());
        title.getStyleClass().add("room-title");

        Label badge = new Label();
        styleBadge(badge, room);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(8, title, spacer, badge);
        top.setAlignment(Pos.CENTER_LEFT);

        Label info = new Label(room.getBuilding() + "  |  " + room.getCapacity() + " seats");
        info.getStyleClass().add("muted");

        VBox card = new VBox(6, top, info);
        card.getStyleClass().add("room-card");
        card.prefWidthProperty().bind(roomList.widthProperty().subtract(40));

        FlowPane chips = new FlowPane(6, 6);
        fillChips(chips, room.getFacilities());
        if (!chips.getChildren().isEmpty()) {
            chips.prefWrapLengthProperty().bind(roomList.widthProperty().subtract(70));
            card.getChildren().add(chips);
        }
        return card;
    }

    private void fillChips(FlowPane pane, String facilities) {
        pane.getChildren().clear();
        if (facilities == null) {
            return;
        }
        for (String part : facilities.split(",")) {
            String text = part.trim();
            if (!text.isEmpty()) {
                Label chip = new Label(text);
                chip.getStyleClass().add("chip");
                pane.getChildren().add(chip);
            }
        }
    }

    private boolean isOpen() {
        LocalTime now = LocalTime.now();
        return !now.isBefore(OPENING) && now.isBefore(CLOSING);
    }

    private void styleBadge(Label badge, Classroom room) {
        badge.getStyleClass().removeAll("badge", "badge-completed", "badge-available", "badge-unavailable");
        String text;
        String style;
        if (!isOpen()) {
            text = "Closed";
            style = "badge-completed";
        } else if (reservationService.isAvailableNow(room)) {
            text = "Available now";
            style = "badge-available";
        } else {
            text = "In use";
            style = "badge-unavailable";
        }
        badge.setText(text);
        badge.getStyleClass().addAll("badge", style);
    }

    // ------------------------------------------------------------ The details

    private void showDetail(Classroom room) {
        boolean show = room != null;
        detailCard.setVisible(show);
        detailCard.setManaged(show);
        emptyDetail.setVisible(!show);
        emptyDetail.setManaged(!show);
        if (!show) {
            weekGrid.getChildren().clear();
            return;
        }
        detailTitle.setText("Room " + room.getRoomNumber());
        detailInfo.setText(room.getBuilding() + "  |  " + room.getCapacity() + " seats");
        styleBadge(detailBadge, room);
        fillChips(detailChips, room.getFacilities());
        drawWeek();
    }

    // ------------------------------------------------------------ The week

    @FXML
    private void onPrevWeek() {
        weekStart = weekStart.minusWeeks(1);
        updateWeekLabel();
        drawWeek();
    }

    @FXML
    private void onNextWeek() {
        weekStart = weekStart.plusWeeks(1);
        updateWeekLabel();
        drawWeek();
    }

    @FXML
    private void onThisWeek() {
        weekStart = mondayOf(LocalDate.now());
        updateWeekLabel();
        drawWeek();
    }

    /** The week shown runs Monday to Saturday. On a Sunday, the coming week is shown. */
    private static LocalDate mondayOf(LocalDate date) {
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return date.plusDays(1);
        }
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private void updateWeekLabel() {
        LocalDate end = weekStart.plusDays(DAYS.length - 1);
        String text = weekStart.format(MONTH_DAY) + " - " + end.format(MONTH_DAY_YEAR);
        if (weekStart.equals(mondayOf(LocalDate.now()))) {
            text += "  (this week)";
        }
        weekLabel.setText(text);
    }

    private void drawWeek() {
        weekGrid.getChildren().clear();
        Classroom room = roomList.getSelectionModel().getSelectedItem();
        double width = weekGrid.getWidth();
        double height = weekGrid.getHeight();
        if (room == null || width <= GUTTER || height <= HEADER) {
            return;
        }

        double colWidth = (width - GUTTER) / DAYS.length;
        double hourHeight = (height - HEADER) / HOURS;
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        // Day headings, with today's column highlighted
        for (int d = 0; d < DAYS.length; d++) {
            LocalDate date = weekStart.plusDays(d);
            double x = GUTTER + d * colWidth;
            boolean isToday = date.equals(today);

            if (isToday) {
                Rectangle highlight = new Rectangle(x, HEADER, colWidth, height - HEADER);
                highlight.getStyleClass().add("wk-today-col");
                weekGrid.getChildren().add(highlight);
            }
            Label head = new Label(DAYS[d].getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                    + " " + date.getDayOfMonth());
            head.getStyleClass().add(isToday ? "wk-head-today" : "wk-head");
            head.setAlignment(Pos.CENTER);
            head.setLayoutX(x + 4);
            head.setLayoutY(5);
            head.setMinSize(colWidth - 8, 24);
            head.setPrefSize(colWidth - 8, 24);
            head.setMaxSize(colWidth - 8, 24);
            weekGrid.getChildren().add(head);
        }

        // Hour lines and labels
        for (int h = 0; h <= HOURS; h++) {
            double y = HEADER + h * hourHeight;
            Line line = new Line(GUTTER, y, width, y);
            line.getStyleClass().add("wk-line");
            Label label = new Label(LocalTime.of(7 + h, 0).format(HOUR_FORMAT));
            label.getStyleClass().add("tl-label");
            label.setLayoutX(6);
            label.setLayoutY(y - 8);
            weekGrid.getChildren().addAll(line, label);
        }
        for (int d = 0; d <= DAYS.length; d++) {
            double x = GUTTER + d * colWidth;
            Line separator = new Line(x, HEADER, x, height);
            separator.getStyleClass().add("wk-line");
            weekGrid.getChildren().add(separator);
        }

        // Regular classes repeat every week
        for (ScheduleSlot slot : scheduleService.getSlotsFor(room.getId())) {
            int column = slot.getDay().getValue() - 1;
            if (column >= 0 && column < DAYS.length) {
                addBlock(column, slot.getStartTime(), slot.getEndTime(), slot.getSubject(),
                        "", "wk-class", colWidth, hourHeight);
            }
        }

        // Reservations only appear in the week they belong to
        for (Reservation r : reservationService.getReservations()) {
            if (r.isCancelled() || r.getClassroom().getId() != room.getId()) {
                continue;
            }
            long column = ChronoUnit.DAYS.between(weekStart, r.getDate());
            if (column >= 0 && column < DAYS.length) {
                boolean student = Session.getCurrentUser() != null
                        && Session.getCurrentUser().isStudent();
                addBlock((int) column, r.getStartTime(), r.getEndTime(),
                        student ? "Reserved" : r.getPurpose(),
                        student ? "" : "\nReserved by " + r.getReservedBy().getFullName(),
                        "wk-reservation", colWidth, hourHeight);
            }
        }

        // A red line for the current time
        long todayColumn = ChronoUnit.DAYS.between(weekStart, today);
        if (todayColumn >= 0 && todayColumn < DAYS.length && isOpen()) {
            double y = HEADER + minutesFromOpening(now) / 60.0 * hourHeight;
            double x = GUTTER + todayColumn * colWidth;
            Line nowLine = new Line(x, y, x + colWidth, y);
            nowLine.getStyleClass().add("wk-now");
            Circle dot = new Circle(x, y, 4);
            dot.getStyleClass().add("wk-now-dot");
            weekGrid.getChildren().addAll(nowLine, dot);
        }
    }

    private void addBlock(int column, LocalTime from, LocalTime to, String title, String extra,
                          String styleClass, double colWidth, double hourHeight) {
        double top = HEADER + Math.max(0, minutesFromOpening(from)) / 60.0 * hourHeight;
        double bottom = HEADER + Math.min(HOURS * 60, minutesFromOpening(to)) / 60.0 * hourHeight;
        if (bottom - top < 6) {
            return;
        }
        double blockWidth = colWidth - 4;
        double blockHeight = bottom - top - 2;
        String timeText = com.classroomplus.util.TimeUtil.range(from, to);

        Label block = new Label(title + "\n" + timeText);
        block.getStyleClass().addAll("wk-block", styleClass);
        block.setWrapText(true);
        block.setAlignment(Pos.TOP_LEFT);
        block.setLayoutX(GUTTER + column * colWidth + 2);
        block.setLayoutY(top + 1);
        block.setMinSize(blockWidth, blockHeight);
        block.setPrefSize(blockWidth, blockHeight);
        block.setMaxSize(blockWidth, blockHeight);
        Tooltip.install(block, new Tooltip(title + "\n" + timeText + extra));
        weekGrid.getChildren().add(block);
    }

    private double minutesFromOpening(LocalTime time) {
        return time.getHour() * 60 + time.getMinute() - OPENING.getHour() * 60;
    }
}