package com.examverse.controller.dashboard;

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
 * DashboardLandingController - Main landing page after intro
 * Features video background with navigation buttons
 */
public class DashboardLandingController implements Initializable {

    @FXML
    private StackPane rootPane;

    @FXML
    private MediaView mediaView;

    // Primary Action Buttons
    @FXML
    private Button loginButton;

    @FXML
    private Button signupButton;

    @FXML
    private Button aboutButton;

    // Secondary Action Buttons
    @FXML
    private Button featuresButton;

    @FXML
    private Button demoButton;

    @FXML
    private Button docsButton;

    // Footer Buttons
    @FXML
    private Button contactButton;

    @FXML
    private Button faqButton;

    private MediaPlayer mediaPlayer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupVideoPlayer();
        setupButtonHandlers();
    }

    /**
     * Setup video player with looping background
     * Enhanced error handling and multiple path resolution methods
     */
    private void setupVideoPlayer() {
        try {
            Media media = null;
            String videoPath = null;

            // Method 1: Try loading from resources using getResource
            try {
                URL videoURL = getClass().getResource("/com/examverse/assets/video/dashboard-bg.mp4");
                if (videoURL != null) {
                    videoPath = videoURL.toExternalForm();
                    System.out.println("Dashboard video path (Method 1): " + videoPath);
                    media = new Media(videoPath);
                }
            } catch (Exception e) {
                System.err.println("Method 1 failed: " + e.getMessage());
            }

            // Method 2: Try loading from file system (fallback)
            if (media == null) {
                try {
                    File videoFile = new File("src/main/resources/com/examverse/assets/video/dashboard-bg.mp4");
                    if (videoFile.exists()) {
                        videoPath = videoFile.toURI().toString();
                        System.out.println("Dashboard video path (Method 2): " + videoPath);
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
                    URL videoURL = classLoader.getResource("com/examverse/assets/video/dashboard-bg.mp4");
                    if (videoURL != null) {
                        videoPath = videoURL.toExternalForm();
                        System.out.println("Dashboard video path (Method 3): " + videoPath);
                        media = new Media(videoPath);
                    }
                } catch (Exception e) {
                    System.err.println("Method 3 failed: " + e.getMessage());
                }
            }

            if (media == null) {
                System.err.println("CRITICAL: Could not load dashboard video from any method!");
                System.err.println("Please ensure dashboard-bg.mp4 is located at: src/main/resources/com/examverse/assets/video/dashboard-bg.mp4");
                return;
            }

            // Create media player
            mediaPlayer = new MediaPlayer(media);

            // Configure media player
            mediaPlayer.setAutoPlay(true);
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE); // Loop continuously
            mediaPlayer.setVolume(0.5); // 50% volume (lower for background)

            // Bind MediaView
            mediaView.setMediaPlayer(mediaPlayer);
            mediaView.setPreserveRatio(true);
            mediaView.setSmooth(true);

            // Bind to container size
            mediaView.fitWidthProperty().bind(rootPane.widthProperty());
            mediaView.fitHeightProperty().bind(rootPane.heightProperty());

            // Handle video errors with detailed logging
            mediaPlayer.setOnError(() -> {
                if (mediaPlayer.getError() != null) {
                    System.err.println("Dashboard video error: " + mediaPlayer.getError().getMessage());
                    System.err.println("Error Type: " + mediaPlayer.getError().getType());
                }
            });

            // Handle successful video ready
            mediaPlayer.setOnReady(() -> {
                System.out.println("Dashboard video loaded successfully!");
                System.out.println("Video duration: " + mediaPlayer.getTotalDuration());
            });

            // Seamless loop
            mediaPlayer.setOnEndOfMedia(() -> {
                mediaPlayer.seek(Duration.ZERO);
                mediaPlayer.play();
            });

            System.out.println("Dashboard video player setup complete");

        } catch (Exception e) {
            System.err.println("Error loading dashboard video: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Setup button click handlers
     */
    private void setupButtonHandlers() {
        // Primary actions
        loginButton.setOnAction(e -> handleLogin());
        signupButton.setOnAction(e -> handleSignup());
        aboutButton.setOnAction(e -> handleAbout());

        // Secondary actions
        featuresButton.setOnAction(e -> handleFeatures());
        demoButton.setOnAction(e -> handleDemo());
        docsButton.setOnAction(e -> handleDocs());

        // Footer actions
        contactButton.setOnAction(e -> handleContact());
        faqButton.setOnAction(e -> handleFAQ());
    }

    // ==================== Button Handlers ====================

    @FXML
    private void handleLogin() {
        System.out.println("Login clicked - Navigate to login screen");
        SceneManager.switchScene("/com/examverse/fxml/auth/login.fxml");  // ← ADD THIS LINE
    }

    @FXML
    private void handleSignup() {
        System.out.println("Signup clicked - Navigate to signup screen");
        SceneManager.switchScene("/com/examverse/fxml/auth/signup.fxml");  // ← ADD THIS LINE
    }

    @FXML
    private void handleAbout() {
        System.out.println("About clicked - Show about dialog/page");
        // TODO: Show about dialog or navigate to about page
    }

    @FXML
    private void handleFeatures() {
        System.out.println("Features clicked - Show features overview");
        // TODO: Navigate to features page or show dialog
    }

    @FXML
    private void handleDemo() {
        System.out.println("Demo clicked - Show demo/tutorial");
        // TODO: Start demo mode or show tutorial
    }

    @FXML
    private void handleDocs() {
        System.out.println("Docs clicked - Open documentation");
        // TODO: Navigate to documentation page or open external link
    }

    @FXML
    private void handleContact() {
        System.out.println("Contact clicked - Show contact form");
        // TODO: Show contact dialog or navigate to contact page
    }

    @FXML
    private void handleFAQ() {
        System.out.println("FAQ clicked - Show FAQ page");
        // TODO: Navigate to FAQ page
    }

    /**
     * Cleanup when leaving this screen
     */
    public void cleanup() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }
    }
}