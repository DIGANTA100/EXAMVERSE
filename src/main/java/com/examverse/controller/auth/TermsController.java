package com.examverse.controller.auth;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.VBox;
import com.examverse.util.SceneManager;
import javafx.animation.FadeTransition;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * TermsController - Handles the Terms & Conditions page
 * Navigates back to signup on back button click
 */
public class TermsController implements Initializable {

    @FXML
    private VBox rootPane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        applyFadeInAnimation();
    }

    /**
     * Apply fade-in animation to the root pane
     */
    private void applyFadeInAnimation() {
        if (rootPane != null) {
            FadeTransition fade = new FadeTransition(Duration.millis(400), rootPane);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            fade.play();
        }
    }

    /**
     * Apply fade-out transition before scene change
     */
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

    /**
     * Handle back button — returns to signup page
     */
    @FXML
    private void handleBack() {
        System.out.println("Back to signup page");
        applyFadeOutTransition(() -> {
            SceneManager.switchScene("/com/examverse/fxml/auth/signup.fxml");
        });
    }
}