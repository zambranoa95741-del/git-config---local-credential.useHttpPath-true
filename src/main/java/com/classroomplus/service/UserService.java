package com.classroomplus.service;

import com.classroomplus.model.Role;
import com.classroomplus.model.User;
import com.classroomplus.util.PasswordUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class UserService {

    // Student ID: 11 digits. The first 4 digits are the year.
    private static final int STUDENT_ID_FIRST_YEAR = 2025;
    private static final int STUDENT_ID_LAST_YEAR = 2025;

    // Employee ID: LDCU- followed by 5 digits, for example LDCU-10234.
    private static final Pattern EMPLOYEE_ID = Pattern.compile("LDCU-\\d{5}");

    private static final UserService INSTANCE = new UserService();

    public static UserService getInstance() {
        return INSTANCE;
    }

    private final List<User> users = new ArrayList<>();
    private int nextId = 1;

    private UserService() {
        // Built-in accounts. The admin does not register through the app.
        // CHANGE THE ADMIN PASSWORD below to one your group chooses.
        addUser("LDCU-00001", "System Administrator", "Administration",
                "admin", "ChangeThisPassword1", Role.ADMIN);

        // Test accounts for trying the app. Delete these two before the defense if you like.
        addUser("LDCU-10001", "Sample Teacher", "College of Information Technology",
                "teacher", "teacher123", Role.TEACHER);
        addUser("20250000001", "Sample Student", "BSIT 2",
                "student", "student123", Role.STUDENT);
    }

    /** Create: registers a teacher, staff, or student account. Throws IllegalArgumentException with a readable message. */
    public User register(Role role, String idNumber, String fullName, String department,
                         String username, String password) {
        if (role == null || role == Role.ADMIN) {
            throw new IllegalArgumentException("That type of account cannot be created here.");
        }
        boolean student = role == Role.STUDENT;
        String idName = student ? "Student ID" : "Employee ID";
        String departmentName = student ? "Program and year" : "Department";

        requireText(idNumber, idName + " is required.");
        requireText(fullName, "Full name is required.");
        requireText(department, departmentName + " is required.");
        requireText(username, "Username is required.");
        requireText(password, "Password is required.");

        String cleanId = cleanIdNumber(role, idNumber);
        if (findByIdNumber(cleanId).isPresent()) {
            throw new IllegalArgumentException("An account with that " + idName + " already exists.");
        }

        String cleanUsername = username.trim();
        if (cleanUsername.length() < 4 || cleanUsername.contains(" ")) {
            throw new IllegalArgumentException("Username must be at least 4 characters with no spaces.");
        }
        if (findByUsername(cleanUsername).isPresent()) {
            throw new IllegalArgumentException("That username is already taken.");
        }
        if (password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }
        return addUser(cleanId, fullName.trim(), department.trim(), cleanUsername, password, role);
    }

    /** Checks the ID against the format for the chosen account type. */
    private String cleanIdNumber(Role role, String idNumber) {
        String id = idNumber.trim().toUpperCase();
        if (role == Role.STUDENT) {
            if (!id.matches("\\d{11}")) {
                throw new IllegalArgumentException(
                        "Student ID must be 11 digits, for example 20250000001.");
            }
            int year = Integer.parseInt(id.substring(0, 4));
            if (year < STUDENT_ID_FIRST_YEAR || year > STUDENT_ID_LAST_YEAR) {
                throw new IllegalArgumentException(STUDENT_ID_FIRST_YEAR == STUDENT_ID_LAST_YEAR
                        ? "Student ID must start with " + STUDENT_ID_FIRST_YEAR + "."
                        : "Student ID must start with a year from "
                        + STUDENT_ID_FIRST_YEAR + " to " + STUDENT_ID_LAST_YEAR + ".");
            }
        } else if (!EMPLOYEE_ID.matcher(id).matches()) {
            throw new IllegalArgumentException(
                    "Employee ID must look like LDCU-10234 (LDCU- and 5 digits).");
        }
        return id;
    }

    /** Read: returns the user if the username and password are correct. */
    public Optional<User> login(String username, String password) {
        if (username == null || password == null) {
            return Optional.empty();
        }
        return findByUsername(username.trim())
                .filter(user -> PasswordUtil.matches(password, user.getPasswordHash()));
    }

    public Optional<User> findByUsername(String username) {
        return users.stream()
                .filter(user -> user.getUsername().equalsIgnoreCase(username))
                .findFirst();
    }

    public Optional<User> findByIdNumber(String idNumber) {
        return users.stream()
                .filter(user -> user.getIdNumber().equalsIgnoreCase(idNumber))
                .findFirst();
    }

    /** Update: edit name and department. */
    public void updateProfile(User user, String fullName, String department) {
        requireText(fullName, "Full name is required.");
        requireText(department, "Department is required.");
        user.setFullName(fullName.trim());
        user.setDepartment(department.trim());
    }

    /** Update: change password after checking the current one. */
    public void changePassword(User user, String currentPassword, String newPassword) {
        if (!PasswordUtil.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters.");
        }
        user.setPasswordHash(PasswordUtil.hash(newPassword));
    }

    private User addUser(String idNumber, String fullName, String department,
                         String username, String password, Role role) {
        User user = new User(nextId++, idNumber, fullName, department, username,
                PasswordUtil.hash(password), role);
        users.add(user);
        return user;
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}