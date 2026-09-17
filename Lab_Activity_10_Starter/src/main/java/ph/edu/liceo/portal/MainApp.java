package ph.edu.liceo.portal;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ph.edu.liceo.portal.controller.ProfileController;
import ph.edu.liceo.portal.model.Student;
import ph.edu.liceo.portal.model.StudentDirectory;

/**
 * GIVEN COMPLETE - do not change this file.
 *
 * It starts the program, keeps ONE window (Stage) open, and gives you two
 * ready-made methods for moving between the two screens:
 *
 *     MainApp.showLogin();            <- go back to the login screen
 *     MainApp.showProfile(student);   <- open the profile of that student
 */
public class MainApp extends Application {

    private static Stage stage;
    private static final StudentDirectory DIRECTORY = new StudentDirectory();

    @Override
    public void start(Stage primaryStage) throws Exception {
        stage = primaryStage;
        stage.setTitle("Liceo Student Portal");
        showLogin();
        stage.show();
    }

    /** Every controller gets the same directory from here. */
    public static StudentDirectory getDirectory() {
        return DIRECTORY;
    }

    /** Loads login.fxml and shows it in the window. */
    public static void showLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("login.fxml"));
            Parent root = loader.load();
            show(root, 420, 380);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Loads profile.fxml, hands the student to its controller, and shows it. */
    public static void showProfile(Student student) {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("profile.fxml"));
            Parent root = loader.load();

            ProfileController controller = loader.getController();
            controller.setStudent(student);

            show(root, 460, 440);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void show(Parent root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(
                MainApp.class.getResource("style.css").toExternalForm());
        stage.setScene(scene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
