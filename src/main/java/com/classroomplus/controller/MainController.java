package com.classroomplus.controller;

import com.classroomplus.model.User;
import com.classroomplus.util.SceneManager;
import com.classroomplus.util.Session;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class MainController {

    @FXML private Button dashboardButton;
    @FXML private Button classroomsButton;
    @FXML private Button reserveButton;
    @FXML private Button myReservationsButton;
    @FXML private Button findRoomButton;
    @FXML private Button campusMapButton;
    @FXML private Button manageRoomsButton;
    @FXML private Button manageSchedulesButton;
    @FXML private Button allReservationsButton;
    @FXML private VBox adminSection;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;
    @FXML private Label pageTitleLabel;
    @FXML private Label dateLabel;
    @FXML private Label statusLabel;
    @FXML private StackPane contentArea;

    private Button activeButton;

    @FXML
    private void initialize() {
        User user = Session.getCurrentUser();
        String role = user.isAdmin() ? "Administrator" : user.isStudent() ? "Student" : "Teacher";

        userNameLabel.setText(user.getFullName());
        userRoleLabel.setText(role);
        statusLabel.setText("Signed in as " + user.getUsername() + "  |  " + role);
        dateLabel.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));

        setShown(adminSection, user.isAdmin());

        // Students browse with Find a Room. Teachers and the admin reserve with Reserve a Room.
        boolean student = user.isStudent();
        setShown(findRoomButton, student);
        setShown(classroomsButton, !student);
        setShown(reserveButton, !student);
        setShown(myReservationsButton, !student);

        showDashboard();
    }

    private void setShown(Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }

    @FXML private void showDashboard() { showPage("Dashboard", dashboardButton, "dashboard.fxml"); }
    @FXML private void showClassrooms() { showPage("Classrooms", classroomsButton, "classrooms.fxml"); }
    @FXML private void showReserve() { showPage("Reserve a Room", reserveButton, "reserve.fxml"); }
    @FXML private void showMyReservations() { showPage("My Reservations", myReservationsButton, "my-reservations.fxml"); }
    @FXML private void showFindRoom() { showPage("Find a Room", findRoomButton, "find-room.fxml"); }
    @FXML private void showCampusMap() { showPage("Campus Map", campusMapButton, "campus-map.fxml"); }
    @FXML private void showManageRooms() { showPage("Manage Rooms", manageRoomsButton, "manage-rooms.fxml"); }
    @FXML private void showManageSchedules() { showPage("Manage Schedules", manageSchedulesButton, "manage-schedules.fxml"); }
    @FXML private void showAllReservations() { showPage("All Reservations", allReservationsButton, "all-reservations.fxml"); }

    @FXML
    private void onLogout() {
        Session.end();
        SceneManager.setMaximized(false);
        SceneManager.switchTo("login.fxml", "ClassRoom+ | Sign in");
    }

    private void showPage(String title, Button button, String fxmlFile) {
        setActive(button);
        pageTitleLabel.setText(title);
        Node page = SceneManager.loadView(fxmlFile);
        contentArea.getChildren().setAll(page);
        fadeIn(page);
    }

    private void fadeIn(Node node) {
        node.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(220), node);
        fade.setToValue(1);
        fade.play();
    }

    private void setActive(Button button) {
        if (activeButton != null) {
            activeButton.getStyleClass().remove("active");
        }
        activeButton = button;
        if (!button.getStyleClass().contains("active")) {
            button.getStyleClass().add("active");
        }
    }
}