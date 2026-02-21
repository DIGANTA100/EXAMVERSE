package com.examverse.controller.dashboard;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.VBox;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import com.examverse.util.SceneManager;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * FeaturesController - Displays ExamVerse feature overview
 */
public class FeaturesController implements Initializable {

    @FXML private VBox rootPane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        applyFadeInAnimation();
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