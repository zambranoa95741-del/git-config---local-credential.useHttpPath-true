package com.classroomplus.service;

import com.classroomplus.model.Classroom;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Optional;

public class ClassroomService {

    private static final ClassroomService INSTANCE = new ClassroomService();

    public static ClassroomService getInstance() {
        return INSTANCE;
    }

    private final ObservableList<Classroom> classrooms = FXCollections.observableArrayList();
    private int nextId = 1;

    private ClassroomService() {
    }

    /** Read: tables in the app can bind to this list and update automatically. */
    public ObservableList<Classroom> getClassrooms() {
        return FXCollections.unmodifiableObservableList(classrooms);
    }

    public Optional<Classroom> findById(int id) {
        return classrooms.stream().filter(room -> room.getId() == id).findFirst();
    }

    /** Create: throws IllegalArgumentException with a readable message if the input is invalid. */
    public Classroom add(String roomNumber, String building, int capacity, String facilities) {
        validate(null, roomNumber, building, capacity);
        Classroom room = new Classroom(nextId++, roomNumber.trim(), building.trim(),
                capacity, cleanFacilities(facilities));
        classrooms.add(room);
        return room;
    }

    /** Update: edits an existing room. */
    public void update(Classroom room, String roomNumber, String building,
                       int capacity, String facilities) {
        validate(room, roomNumber, building, capacity);
        room.setRoomNumber(roomNumber.trim());
        room.setBuilding(building.trim());
        room.setCapacity(capacity);
        room.setFacilities(cleanFacilities(facilities));
    }

    /** Delete: removes the room from the list. */
    public void delete(Classroom room) {
        classrooms.remove(room);
    }

    private void validate(Classroom ignore, String roomNumber, String building, int capacity) {
        if (roomNumber == null || roomNumber.isBlank()) {
            throw new IllegalArgumentException("Room number is required.");
        }
        if (building == null || building.isBlank()) {
            throw new IllegalArgumentException("Building is required.");
        }
        if (capacity < 1 || capacity > 500) {
            throw new IllegalArgumentException("Capacity must be between 1 and 500.");
        }
        boolean duplicate = classrooms.stream().anyMatch(room ->
                room != ignore
                        && room.getRoomNumber().equalsIgnoreCase(roomNumber.trim())
                        && room.getBuilding().equalsIgnoreCase(building.trim()));
        if (duplicate) {
            throw new IllegalArgumentException("That room already exists in this building.");
        }
    }

    private String cleanFacilities(String facilities) {
        return facilities == null ? "" : facilities.trim();
    }
}