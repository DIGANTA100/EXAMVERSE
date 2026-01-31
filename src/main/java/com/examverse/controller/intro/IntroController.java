package com.examverse.controller.intro;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;
import com.examverse.util.SceneManager;
import com.examverse.util.AnimationUtil;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * IntroController - Controls the intro screen with video background
 * Handles video playback and navigation to next screen
 */
public class IntroController implements Initializable {

    @FXML
    private StackPane rootPane;

    @FXML
    private MediaView mediaView;

    @FXML
    private Button getStartedButton;

    private MediaPlayer mediaPlayer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupVideoPlayer();
        setupGetStartedButton();
    }

    /**
     * Setup video player with looping and auto-play
     */
    private void setupVideoPlayer() {
        try {
            // Load video from resources
            String videoPath = Objects.requireNonNull(
                    getClass().getResource("/com/examverse/assets/video/intro.mp4")
            ).toExternalForm();

            Media media = new Media(videoPath);
            mediaPlayer = new MediaPlayer(media);

            // Configure media player
            mediaPlayer.setAutoPlay(true);
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE); // Loop continuously
            mediaPlayer.setVolume(0.7); // 70% volume (adjust as needed)

            // Bind MediaView to the video
            mediaView.setMediaPlayer(mediaPlayer);

            // Make video fit the screen while maintaining aspect ratio
            mediaView.setPreserveRatio(true);
            mediaView.setSmooth(true);

            // Bind video dimensions to root pane
            mediaView.fitWidthProperty().bind(rootPane.widthProperty());
            mediaView.fitHeightProperty().bind(rootPane.heightProperty());

            // Handle video errors
            mediaPlayer.setOnError(() -> {
                System.err.println("Media Error: " + mediaPlayer.getError().getMessage());
            });

            // Optional: Seek to beginning when loop restarts for seamless transition
            mediaPlayer.setOnEndOfMedia(() -> {
                mediaPlayer.seek(Duration.ZERO);
            });

        } catch (Exception e) {
            System.err.println("Error loading intro video: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Setup Get Started button with hover effects
     */
    private void setupGetStartedButton() {
        // Add hover effect using CSS pseudo-classes (handled in CSS)
        // Button will have glow effect on hover

        getStartedButton.setOnAction(event -> handleGetStarted());
    }

    /**
     * Handle Get Started button click
     * Navigate to dashboard landing screen
     */
    @FXML
    private void handleGetStarted() {
        // Stop the video
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }

        // Navigate to dashboard landing page
        System.out.println("Get Started clicked! Navigating to dashboard...");
        SceneManager.switchScene("/com/examverse/fxml/dashboard/dashboard-landing.fxml");
    }

    /**
     * Cleanup resources when controller is destroyed
     */
    public void cleanup() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }
    }
}