package ph.edu.liceo.portal.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import ph.edu.liceo.portal.MainApp;
import ph.edu.liceo.portal.model.Student;

public class ProfileController {

    @FXML private Label initialsLabel;
    @FXML private Label nameLabel;
    @FXML private Label studentNoLabel;
    @FXML private Label courseLabel;
    @FXML private Label emailLabel;

    public void setStudent(Student student) {
        initialsLabel.setText(initialsOf(student.getFullName()));
        nameLabel.setText(student.getFullName());
        studentNoLabel.setText(student.getStudentNo());
        courseLabel.setText(student.getCourseAndYear());
        emailLabel.setText(student.getEmail());
    }

    @FXML
    private void handleLogout() {
        MainApp.showLogin();
    }

    private String initialsOf(String fullName) {
        String[] parts = fullName.split(" ");
        String first = parts[0].substring(0, 1);
        String last = parts[parts.length - 1].substring(0, 1);
        return (first + last).toUpperCase();
    }
}