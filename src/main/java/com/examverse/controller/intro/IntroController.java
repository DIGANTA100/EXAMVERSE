package com.examverse.controller.intro;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
 * IntroController - Enhanced intro screen with animated quotes
 * Features professional typewriter animation for academic quotes
 */
public class IntroController implements Initializable {

    @FXML
    private StackPane rootPane;

    @FXML
    private MediaView mediaView;

    @FXML
    private Button getStartedButton;

    // Left side quotes
    @FXML
    private Label leftQuote1;

    @FXML
    private Label leftQuote2;

    @FXML
    private Label leftQuote3;

    // Right side quotes
    @FXML
    private Label rightQuote1;

    @FXML
    private Label rightQuote2;

    @FXML
    private Label rightQuote3;

    // Tagline
    @FXML
    private Label tagline;

    private MediaPlayer mediaPlayer;

    // Academic quotes content
    private static final String[] LEFT_QUOTES = {
            "Education is not the learning of facts, but the training of the mind to think.",
            "Education is the most powerful weapon which you can use to change the world.",
            "The roots of education are bitter, but the fruit is sweet."
    };

    private static final String[] RIGHT_QUOTES = {
            "An investment in knowledge pays the best interest.",
            "Real knowledge is to know the extent of one's ignorance.",
            "The only way to do great work is to love what you do."
    };

    private static final String TAGLINE_TEXT = "Empowering minds through intelligent assessment";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupVideoPlayer();
        setupGetStartedButton();
        startAnimations();
    }

    /**
     * Setup video player with looping and auto-play
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
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            mediaPlayer.setVolume(0.7);

            // Bind MediaView to the video
            mediaView.setMediaPlayer(mediaPlayer);
            mediaView.setPreserveRatio(true);
            mediaView.setSmooth(true);

            // Bind video dimensions to root pane
            mediaView.fitWidthProperty().bind(rootPane.widthProperty());
            mediaView.fitHeightProperty().bind(rootPane.heightProperty());

            // Handle video errors
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
     * Setup Get Started button
     */
    private void setupGetStartedButton() {
        getStartedButton.setOnAction(event -> handleGetStarted());
    }

    /**
     * Start all text animations in sequence
     */
    private void startAnimations() {
        // Start tagline animation immediately
        animateTypewriter(tagline, TAGLINE_TEXT, 50, 500);

        // Left side quotes with delays
        animateTypewriter(leftQuote1, LEFT_QUOTES[0], 40, 1500);
        animateTypewriter(leftQuote2, LEFT_QUOTES[1], 40, 6000);
        animateTypewriter(leftQuote3, LEFT_QUOTES[2], 40, 11000);

        // Right side quotes with delays
        animateTypewriter(rightQuote1, RIGHT_QUOTES[0], 40, 2500);
        animateTypewriter(rightQuote2, RIGHT_QUOTES[1], 40, 7500);
        animateTypewriter(rightQuote3, RIGHT_QUOTES[2], 40, 12500);
    }

    /**
     * Typewriter animation effect
     * @param label Label to animate
     * @param fullText Complete text to display
     * @param charDelay Delay between characters in milliseconds
     * @param startDelay Initial delay before starting animation
     */
    private void animateTypewriter(Label label, String fullText, int charDelay, int startDelay) {
        // Clear initial text
        label.setText("");

        // Create timeline for character-by-character animation
        Timeline timeline = new Timeline();

        for (int i = 0; i <= fullText.length(); i++) {
            final int index = i;
            KeyFrame keyFrame = new KeyFrame(
                    Duration.millis(startDelay + (i * charDelay)),
                    event -> {
                        if (index <= fullText.length()) {
                            label.setText(fullText.substring(0, index));
                        }
                    }
            );
            timeline.getKeyFrames().add(keyFrame);
        }

        // Add pause at the end (1 second)
        KeyFrame pauseFrame = new KeyFrame(
                Duration.millis(startDelay + (fullText.length() * charDelay) + 1000),
                event -> {
                    // Animation complete, text stays visible
                }
        );
        timeline.getKeyFrames().add(pauseFrame);

        timeline.play();
    }

    /**
     * Handle Get Started button click
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