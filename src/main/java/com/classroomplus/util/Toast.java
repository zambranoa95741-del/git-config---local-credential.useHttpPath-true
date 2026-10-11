package com.classroomplus.util;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

/** Small message that slides in at the bottom right of the window and fades away by itself. */
public final class Toast {

    private static Popup current;

    private Toast() {
    }

    public static void success(Node anchor, String message) {
        show(anchor, message, "toast-success");
    }

    public static void error(Node anchor, String message) {
        show(anchor, message, "toast-error");
    }

    private static void show(Node anchor, String message, String kind) {
        if (anchor == null || anchor.getScene() == null || anchor.getScene().getWindow() == null) {
            return;
        }
        Window owner = anchor.getScene().getWindow();

        if (current != null) {
            current.hide();
        }

        Label label = new Label(message);
        label.getStyleClass().addAll("toast", kind);
        label.setWrapText(true);
        label.setMaxWidth(380);
        label.setOpacity(0);

        Popup popup = new Popup();
        popup.getContent().add(label);
        popup.getScene().getStylesheets().setAll(anchor.getScene().getStylesheets());
        popup.show(owner, owner.getX() + owner.getWidth() - 420, owner.getY() + owner.getHeight() - 120);
        current = popup;

        FadeTransition fadeIn = new FadeTransition(Duration.millis(180), label);
        fadeIn.setToValue(1);
        PauseTransition hold = new PauseTransition(Duration.millis(2800));
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), label);
        fadeOut.setToValue(0);
        SequentialTransition sequence = new SequentialTransition(fadeIn, hold, fadeOut);
        sequence.setOnFinished(event -> {
            popup.hide();
            if (current == popup) {
                current = null;
            }
        });

        // Wait one moment so the label has its size, then place it in the corner.
        Platform.runLater(() -> {
            popup.setX(owner.getX() + owner.getWidth() - label.getWidth() - 32);
            popup.setY(owner.getY() + owner.getHeight() - label.getHeight() - 56);
            sequence.play();
        });
    }
}