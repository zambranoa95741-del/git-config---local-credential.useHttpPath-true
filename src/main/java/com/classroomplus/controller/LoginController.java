package com.classroomplus.controller;

import com.classroomplus.model.User;
import com.classroomplus.service.UserService;
import com.classroomplus.util.SceneManager;
import com.classroomplus.util.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.Optional;

public class LoginController {

    private static final String UNIVERSITY_URL = "https://liceo.edu.ph";

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    private void onLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isBlank() || password.isEmpty()) {
            showError("Please enter your username and password.");
            return;
        }

        Optional<User> user = UserService.getInstance().login(username, password);
        if (user.isEmpty()) {
            showError("Incorrect username or password.");
            passwordField.clear();
            passwordField.requestFocus();
            return;
        }

        Session.start(user.get());
        SceneManager.switchTo("main.fxml", "ClassRoom+");
        SceneManager.setMaximized(true);
    }

    @FXML
    private void onRegister() {
        SceneManager.switchTo("register.fxml", "ClassRoom+ | Create account");
    }


    @FXML
    private void onOpenWebsite() {
        SceneManager.openLink(UNIVERSITY_URL);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}