package com.classroomplus.model;

public class User {

    private final int id;
    private final String idNumber;
    private String fullName;
    private String department;
    private final String username;
    private String passwordHash;
    private final Role role;

    public User(int id, String idNumber, String fullName, String department,
                String username, String passwordHash, Role role) {
        this.id = id;
        this.idNumber = idNumber;
        this.fullName = fullName;
        this.department = department;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public int getId() { return id; }
    public String getIdNumber() { return idNumber; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Role getRole() { return role; }
    public boolean isAdmin() { return role == Role.ADMIN; }
    public boolean isStudent() { return role == Role.STUDENT; }
}