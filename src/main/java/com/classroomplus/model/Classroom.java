package com.classroomplus.model;

public class Classroom {

    private final int id;
    private String roomNumber;
    private String building;
    private int capacity;
    private String facilities;

    public Classroom(int id, String roomNumber, String building, int capacity, String facilities) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.building = building;
        this.capacity = capacity;
        this.facilities = facilities;
    }

    public int getId() { return id; }
    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public String getFacilities() { return facilities; }
    public void setFacilities(String facilities) { this.facilities = facilities; }

    @Override
    public String toString() {
        return roomNumber + " (" + building + ")";
    }
}