package com.classroomplus;

import com.classroomplus.service.SampleData;
import com.classroomplus.util.SceneManager;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        SampleData.load();
        SceneManager.init(stage, getHostServices());
        stage.setMinWidth(960);
        stage.setMinHeight(620);
        stage.getIcons().add(new Image(
                App.class.getResourceAsStream("/com/classroomplus/images/logo-icon.png")));
        SceneManager.switchTo("login.fxml", "ClassRoom+ | Sign in");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}