package com.examverse.controller.dashboard;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import com.examverse.util.SceneManager;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * AboutController - Displays supervisor info and developer profiles
 * Photos are loaded from assets/images/ with a graceful fallback
 * if the image file is not yet present.
 */
public class AboutController implements Initializable {

    @FXML private VBox rootPane;
    @FXML private ImageView prottoyImage;
    @FXML private ImageView digantaImage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadDeveloperPhotos();
        applyFadeInAnimation();
    }

    /**
     * Attempts to load each developer's photo from resources.
     * Falls back silently if the image file doesn't exist yet —
     * the avatar border and placeholder text handle the empty state.
     *
     * Place your photos at:
     *   src/main/resources/com/examverse/assets/images/prottoy.jpg
     *   src/main/resources/com/examverse/assets/images/diganta.jpg
     */
    private void loadDeveloperPhotos() {
        loadPhoto(prottoyImage, "/com/examverse/assets/images/prottoy.jpg");
        loadPhoto(digantaImage, "/com/examverse/assets/images/diganta.jpg");
    }

    private void loadPhoto(ImageView imageView, String resourcePath) {
        try {
            URL url = getClass().getResource(resourcePath);
            if (url != null) {
                Image image = new Image(url.toExternalForm(), true); // background load
                imageView.setImage(image);

                // Circular clip
                double radius = imageView.getFitWidth() / 2.0;
                Circle clip = new Circle(radius, radius, radius);
                imageView.setClip(clip);
            }
        } catch (Exception e) {
            System.err.println("Could not load photo: " + resourcePath + " — " + e.getMessage());
        }
    }

    private void applyFadeInAnimation() {
        if (rootPane != null) {
            FadeTransition fade = new FadeTransition(Duration.millis(500), rootPane);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            fade.play();
        }
    }

    private void applyFadeOutTransition(Runnable onFinished) {
        if (rootPane != null) {
            FadeTransition fade = new FadeTransition(Duration.millis(300), rootPane);
            fade.setFromValue(1.0);
            fade.setToValue(0.0);
            fade.setOnFinished(e -> onFinished.run());
            fade.play();
        } else {
            onFinished.run();
        }
    }

    @FXML
    private void handleBack() {
        applyFadeOutTransition(() ->
                SceneManager.switchScene("/com/examverse/fxml/dashboard/dashboard-landing.fxml"));
    }
}