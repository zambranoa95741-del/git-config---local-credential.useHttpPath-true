package com.classroomplus.util;

import javafx.application.HostServices;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public final class SceneManager {

    private static final String VIEW_PATH = "/com/classroomplus/view/";
    private static final String CSS_PATH = "/com/classroomplus/css/app.css";

    private static Stage stage;
    private static HostServices hostServices;

    private SceneManager() {
    }

    public static void init(Stage primaryStage, HostServices services) {
        stage = primaryStage;
        hostServices = services;
    }

    public static void openLink(String url) {
        hostServices.showDocument(url);
    }

    public static void setMaximized(boolean maximized) {
        stage.setMaximized(maximized);
    }

    /** Loads a page without showing it (used for the pages inside the main window). */
    public static Parent loadView(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(SceneManager.class.getResource(VIEW_PATH + fxmlFile),
                            "FXML not found: " + fxmlFile));
            return loader.load();
        } catch (IOException e) {
            throw new IllegalStateException("Could not load view: " + fxmlFile, e);
        }
    }

    /** Replaces the whole window content (sign in, register, main window). */
    public static void switchTo(String fxmlFile, String title) {
        Parent root = loadView(fxmlFile);
        Scene scene = stage.getScene();
        if (scene == null) {
            scene = new Scene(root, 1100, 700);
            scene.getStylesheets().add(
                    Objects.requireNonNull(SceneManager.class.getResource(CSS_PATH)).toExternalForm());
            stage.setScene(scene);
        } else {
            scene.setRoot(root);
        }
        stage.setTitle(title);
    }
}