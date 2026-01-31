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

import java.net.URL;
import java.util.Objects;
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
     */
    private void setupVideoPlayer() {
        try {
            // Load dashboard background video
            String videoPath = Objects.requireNonNull(
                    getClass().getResource("/com/examverse/assets/video/dashboard-bg.mp4")
            ).toExternalForm();

            Media media = new Media(videoPath);
            mediaPlayer = new MediaPlayer(media);

            // Configure media player
            mediaPlayer.setAutoPlay(true);
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            mediaPlayer.setVolume(0.5); // Lower volume for background

            // Bind MediaView
            mediaView.setMediaPlayer(mediaPlayer);
            mediaView.setPreserveRatio(true);
            mediaView.setSmooth(true);

            // Bind to container size
            mediaView.fitWidthProperty().bind(rootPane.widthProperty());
            mediaView.fitHeightProperty().bind(rootPane.heightProperty());

            // Error handling
            mediaPlayer.setOnError(() -> {
                System.err.println("Dashboard video error: " + mediaPlayer.getError().getMessage());
            });

            // Seamless loop
            mediaPlayer.setOnEndOfMedia(() -> {
                mediaPlayer.seek(Duration.ZERO);
            });

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
        // TODO: Implement navigation to login screen
        // SceneManager.switchScene("/com/examverse/fxml/auth/login.fxml");
    }

    @FXML
    private void handleSignup() {
        System.out.println("Signup clicked - Navigate to signup screen");
        // TODO: Implement navigation to signup screen
        // SceneManager.switchScene("/com/examverse/fxml/auth/signup.fxml");
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