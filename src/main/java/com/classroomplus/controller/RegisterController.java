package com.classroomplus.controller;

import com.classroomplus.model.Role;
import com.classroomplus.service.UserService;
import com.classroomplus.util.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;

public class RegisterController {

    @FXML private ToggleGroup roleGroup;
    @FXML private ToggleButton teacherToggle;
    @FXML private ToggleButton studentToggle;
    @FXML private Label introLabel;
    @FXML private Label idLabel;
    @FXML private Label departmentLabel;
    @FXML private TextField idField;
    @FXML private TextField fullNameField;
    @FXML private TextField departmentField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private Label errorLabel;

    @FXML
    private void initialize() {
        // One of the two choices must always stay selected.
        roleGroup.selectedToggleProperty().addListener((observable, oldToggle, newToggle) -> {
            if (newToggle == null && oldToggle != null) {
                roleGroup.selectToggle(oldToggle);
                return;
            }
            updateLabels();
        });
        updateLabels();
    }

    private void updateLabels() {
        boolean student = studentToggle.isSelected();
        idLabel.setText(student ? "Student ID" : "Employee ID");
        idField.setPromptText(student ? "e.g. 20250000001" : "e.g. LDCU-10234");
        departmentLabel.setText(student ? "Program and year" : "Department");
        departmentField.setPromptText(student ? "e.g. BSIT 2" : "e.g. College of Information Technology");
        introLabel.setText(student
                ? "Students can browse classrooms and their schedules. Reservations are for teachers and staff."
                : "For teachers and staff of Liceo de Cagayan University.");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    @FXML
    private void onCreateAccount() {
        if (!passwordField.getText().equals(confirmField.getText())) {
            showError("Passwords do not match.");
            return;
        }

        Role role = studentToggle.isSelected() ? Role.STUDENT : Role.TEACHER;
        try {
            UserService.getInstance().register(
                    role,
                    idField.getText(),
                    fullNameField.getText(),
                    departmentField.getText(),
                    usernameField.getText(),
                    passwordField.getText());
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
            return;
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION,
                "Your account was created. You can now sign in.");
        alert.setHeaderText(null);
        alert.showAndWait();
        onBackToLogin();
    }

    @FXML
    private void onBackToLogin() {
        SceneManager.switchTo("login.fxml", "ClassRoom+ | Sign in");
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}