package com.classroomplus.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;

import java.io.InputStream;

public class CampusMapController {

    private static final String[] MAP_FILES = {"campus-map.png", "campus-map.jpg", "campus-map.jpeg"};
    private static final double MIN_ZOOM = 1.0;
    private static final double MAX_ZOOM = 5.0;

    @FXML private ScrollPane mapScroll;
    @FXML private StackPane mapHolder;
    @FXML private ImageView mapView;
    @FXML private Label zoomLabel;
    @FXML private Label emptyLabel;

    private double zoom = 1.0;

    @FXML
    private void initialize() {
        Image image = loadMap();
        if (image == null) {
            mapScroll.setVisible(false);
            mapScroll.setManaged(false);
            emptyLabel.setText("The campus map picture has not been added yet. "
                    + "Save it as campus-map.png in the images folder of the project.");
            emptyLabel.setVisible(true);
            emptyLabel.setManaged(true);
            zoomLabel.setText("");
            return;
        }

        mapView.setImage(image);

        mapScroll.viewportBoundsProperty().addListener((observable, oldValue, value) -> applyZoom());
        mapScroll.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.isControlDown()) {
                zoomBy(event.getDeltaY() > 0 ? 1.15 : 1 / 1.15);
                event.consume();
            }
        });
        applyZoom();
    }

    private Image loadMap() {
        for (String name : MAP_FILES) {
            InputStream stream = CampusMapController.class
                    .getResourceAsStream("/com/classroomplus/images/" + name);
            if (stream != null) {
                return new Image(stream);
            }
        }
        return null;
    }

    @FXML
    private void onZoomIn() {
        zoomBy(1.25);
    }

    @FXML
    private void onZoomOut() {
        zoomBy(0.8);
    }

    @FXML
    private void onFit() {
        zoom = 1.0;
        applyZoom();
    }

    private void zoomBy(double factor) {
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom * factor));
        applyZoom();
    }

    private void applyZoom() {
        Image image = mapView.getImage();
        double viewWidth = mapScroll.getViewportBounds().getWidth();
        double viewHeight = mapScroll.getViewportBounds().getHeight();
        if (image == null || viewWidth <= 0 || viewHeight <= 0) {
            return;
        }
        double ratio = image.getHeight() / image.getWidth();
        double fitWidth = Math.min(viewWidth - 4, (viewHeight - 4) / ratio);
        mapView.setFitWidth(Math.max(50, fitWidth) * zoom);
        zoomLabel.setText(Math.round(zoom * 100) + "%");
    }
}