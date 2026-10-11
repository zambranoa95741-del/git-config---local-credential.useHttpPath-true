package com.classroomplus.controller;

import com.classroomplus.model.Classroom;
import com.classroomplus.service.ClassroomService;
import com.classroomplus.service.ReservationService;
import com.classroomplus.service.ScheduleService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;

import java.util.TreeSet;

public class ManageRoomsController {

    @FXML private TextField searchField;
    @FXML private Label countLabel;
    @FXML private Button editButton;
    @FXML private Button deleteButton;

    @FXML private TableView<Classroom> table;
    @FXML private TableColumn<Classroom, String> roomCol;
    @FXML private TableColumn<Classroom, String> buildingCol;
    @FXML private TableColumn<Classroom, Integer> capacityCol;
    @FXML private TableColumn<Classroom, String> facilitiesCol;
    @FXML private TableColumn<Classroom, String> classesCol;

    private final ClassroomService classroomService = ClassroomService.getInstance();
    private final ScheduleService scheduleService = ScheduleService.getInstance();
    private final ReservationService reservationService = ReservationService.getInstance();

    private FilteredList<Classroom> filtered;

    @FXML
    private void initialize() {
        roomCol.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        buildingCol.setCellValueFactory(new PropertyValueFactory<>("building"));
        capacityCol.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        facilitiesCol.setCellValueFactory(new PropertyValueFactory<>("facilities"));
        classesCol.setCellValueFactory(cell -> new SimpleStringProperty(
                String.valueOf(scheduleService.getSlotsFor(cell.getValue().getId()).size())));

        filtered = new FilteredList<>(classroomService.getClassrooms(), room -> true);
        SortedList<Classroom> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilter());
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

    private void applyFilter() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        filtered.setPredicate(room -> query.isEmpty()
                || room.getRoomNumber().toLowerCase().contains(query)
                || room.getBuilding().toLowerCase().contains(query)
                || room.getFacilities().toLowerCase().contains(query));
        updateCount();
    }

    private void updateCount() {
        countLabel.setText(filtered.size() + " of " + classroomService.getClassrooms().size() + " rooms");
    }

    private void updateButtons() {
        boolean none = table.getSelectionModel().getSelectedItem() == null;
        editButton.setDisable(none);
        deleteButton.setDisable(none);
    }

    // ---------------------------------------------------------------- Create

    @FXML
    private void onAdd() {
        showRoomDialog(null);
    }

    // ---------------------------------------------------------------- Update

    @FXML
    private void onEdit() {
        Classroom room = table.getSelectionModel().getSelectedItem();
        if (room != null) {
            showRoomDialog(room);
        }
    }

    /** One form for both adding (existing is null) and editing a room. */
    private void showRoomDialog(Classroom existing) {
        boolean editing = existing != null;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(editing ? "Edit room" : "Add room");
        dialog.setHeaderText(editing ? "Edit room " + existing : "Add a new classroom");
        style(dialog);

        ButtonType saveType = new ButtonType(editing ? "Save changes" : "Add room",
                ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        TextField roomNumber = new TextField(editing ? existing.getRoomNumber() : "");
        roomNumber.setPromptText("e.g. 204");

        ComboBox<String> building = new ComboBox<>(
                FXCollections.observableArrayList(existingBuildings()));
        building.setEditable(true);
        building.setMaxWidth(Double.MAX_VALUE);
        building.setPromptText("Pick a building or type a new one");
        if (editing) {
            building.setValue(existing.getBuilding());
        }

        TextField capacity = new TextField(editing ? String.valueOf(existing.getCapacity()) : "");
        capacity.setPromptText("Number of seats, e.g. 40");

        TextField facilities = new TextField(editing ? existing.getFacilities() : "");
        facilities.setPromptText("e.g. Projector, Whiteboard, Air-conditioned");

        Label error = new Label();
        error.getStyleClass().add("error-label");
        error.setWrapText(true);
        error.setMaxWidth(340);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 4, 0, 4));
        grid.add(new Label("Room number"), 0, 0);
        grid.add(roomNumber, 1, 0);
        grid.add(new Label("Building"), 0, 1);
        grid.add(building, 1, 1);
        grid.add(new Label("Capacity"), 0, 2);
        grid.add(capacity, 1, 2);
        grid.add(new Label("Facilities"), 0, 3);
        grid.add(facilities, 1, 3);
        grid.add(error, 0, 4, 2, 1);
        dialog.getDialogPane().setContent(grid);

        Classroom[] saved = new Classroom[1];

        // Keep the form open and show the reason when the input is not valid.
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                int seats;
                try {
                    seats = Integer.parseInt(capacity.getText().trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Capacity must be a whole number.");
                }
                String buildingName = building.getEditor().getText();
                if (editing) {
                    classroomService.update(existing, roomNumber.getText(), buildingName,
                            seats, facilities.getText());
                } else {
                    saved[0] = classroomService.add(roomNumber.getText(), buildingName,
                            seats, facilities.getText());
                }
            } catch (IllegalArgumentException e) {
                error.setText(e.getMessage());
                event.consume();
            }
        });

        dialog.showAndWait();

        updateCount();
        table.refresh();
        if (saved[0] != null) {
            table.getSelectionModel().select(saved[0]);
            table.scrollTo(saved[0]);
        }
    }

    private TreeSet<String> existingBuildings() {
        TreeSet<String> buildings = new TreeSet<>();
        for (Classroom room : classroomService.getClassrooms()) {
            buildings.add(room.getBuilding());
        }
        return buildings;
    }

    // ---------------------------------------------------------------- Delete

    @FXML
    private void onDelete() {
        Classroom room = table.getSelectionModel().getSelectedItem();
        if (room == null) {
            return;
        }

        if (reservationService.hasActiveReservations(room)) {
            Alert warning = new Alert(Alert.AlertType.WARNING,
                    "This room has upcoming or ongoing reservations. "
                            + "Cancel them or wait until they finish, then delete the room.");
            warning.setTitle("Delete room");
            warning.setHeaderText("This room cannot be deleted yet");
            style(warning);
            warning.showAndWait();
            return;
        }

        int classes = scheduleService.getSlotsFor(room.getId()).size();
        String message = "Room " + room + " will be removed."
                + (classes > 0 ? "\nIts " + classes + " regular weekly class(es) will be removed too." : "");

        ButtonType yes = new ButtonType("Delete room", ButtonBar.ButtonData.OK_DONE);
        ButtonType no = new ButtonType("Keep it", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, message, yes, no);
        confirm.setTitle("Delete room");
        confirm.setHeaderText("Delete this room?");
        style(confirm);

        confirm.showAndWait().filter(button -> button == yes).ifPresent(button -> {
            scheduleService.deleteForClassroom(room.getId());
            classroomService.delete(room);
            updateCount();
        });
    }

    private void style(Dialog<?> dialog) {
        dialog.initOwner(table.getScene().getWindow());
        dialog.getDialogPane().getStylesheets().addAll(table.getScene().getStylesheets());
    }
}