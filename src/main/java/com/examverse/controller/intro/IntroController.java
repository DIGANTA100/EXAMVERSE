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

import java.io.File;
import java.net.URL;
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
     * Enhanced error handling and multiple path resolution methods
     */
    private void setupVideoPlayer() {
        try {
            Media media = null;
            String videoPath = null;

            // Method 1: Try loading from resources using getResource
            try {
                URL videoURL = getClass().getResource("/com/examverse/assets/video/intro.mp4");
                if (videoURL != null) {
                    videoPath = videoURL.toExternalForm();
                    System.out.println("Video path (Method 1): " + videoPath);
                    media = new Media(videoPath);
                }
            } catch (Exception e) {
                System.err.println("Method 1 failed: " + e.getMessage());
            }

            // Method 2: Try loading from file system (fallback)
            if (media == null) {
                try {
                    File videoFile = new File("src/main/resources/com/examverse/assets/video/intro.mp4");
                    if (videoFile.exists()) {
                        videoPath = videoFile.toURI().toString();
                        System.out.println("Video path (Method 2): " + videoPath);
                        media = new Media(videoPath);
                    }
                } catch (Exception e) {
                    System.err.println("Method 2 failed: " + e.getMessage());
                }
            }

            // Method 3: Try classpath loader (another fallback)
            if (media == null) {
                try {
                    ClassLoader classLoader = getClass().getClassLoader();
                    URL videoURL = classLoader.getResource("com/examverse/assets/video/intro.mp4");
                    if (videoURL != null) {
                        videoPath = videoURL.toExternalForm();
                        System.out.println("Video path (Method 3): " + videoPath);
                        media = new Media(videoPath);
                    }
                } catch (Exception e) {
                    System.err.println("Method 3 failed: " + e.getMessage());
                }
            }

            if (media == null) {
                System.err.println("CRITICAL: Could not load intro video from any method!");
                System.err.println("Please ensure intro.mp4 is located at: src/main/resources/com/examverse/assets/video/intro.mp4");
                return;
            }

            // Create media player
            mediaPlayer = new MediaPlayer(media);

            // Configure media player
            mediaPlayer.setAutoPlay(true);
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE); // Loop continuously
            mediaPlayer.setVolume(0.7); // 70% volume

            // Bind MediaView to the video
            mediaView.setMediaPlayer(mediaPlayer);

            // Make video fit the screen while maintaining aspect ratio
            mediaView.setPreserveRatio(true);
            mediaView.setSmooth(true);

            // Bind video dimensions to root pane
            mediaView.fitWidthProperty().bind(rootPane.widthProperty());
            mediaView.fitHeightProperty().bind(rootPane.heightProperty());

            // Handle video errors with detailed logging
            mediaPlayer.setOnError(() -> {
                if (mediaPlayer.getError() != null) {
                    System.err.println("Media Error: " + mediaPlayer.getError().getMessage());
                    System.err.println("Error Type: " + mediaPlayer.getError().getType());
                }
            });

            // Handle successful video ready
            mediaPlayer.setOnReady(() -> {
                System.out.println("Intro video loaded successfully!");
                System.out.println("Video duration: " + mediaPlayer.getTotalDuration());
            });

            // Seamless loop
            mediaPlayer.setOnEndOfMedia(() -> {
                mediaPlayer.seek(Duration.ZERO);
                mediaPlayer.play();
            });

            System.out.println("Intro video player setup complete");

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