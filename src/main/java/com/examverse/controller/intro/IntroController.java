package com.examverse.controller.intro;

import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import com.examverse.util.SceneManager;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * IntroController - Enhanced intro screen with animated quotes
 * Features professional typewriter animation for academic quotes
 * Using IMAGE SEQUENCE (300 frames @ 30fps) instead of video
 */
public class IntroController implements Initializable {

    @FXML
    private StackPane rootPane;

    @FXML
    private ImageView mediaView;  // Using same fx:id as before for compatibility

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

    // Image sequence data
    private List<Image> frames;
    private AnimationTimer animationTimer;
    private int currentFrameIndex = 0;
    private long lastFrameTime = 0;

    // 30 FPS = 33.33ms per frame
    private static final long FRAME_DURATION_NANOS = 16_666_667L;

    // Frame configuration
    private static final int TOTAL_FRAMES = 331;
    private static final String FRAME_PATH_PATTERN = "/com/examverse/assets/video/frames/frame_%04d.png";

    // Loading state
    private boolean isLoading = true;
    private ExecutorService executor;

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
        setupImageView();
        loadFramesAsync();
        setupGetStartedButton();
        startAnimations();
    }

    /**
     * Setup ImageView for frame display
     */
    private void setupImageView() {
        // Make ImageView fill the entire pane
        mediaView.setPreserveRatio(true);
        mediaView.setSmooth(true);
        mediaView.fitWidthProperty().bind(rootPane.widthProperty());
        mediaView.fitHeightProperty().bind(rootPane.heightProperty());

        System.out.println("ImageView setup complete - ready for frame sequence");
    }

    /**
     * Load all frames asynchronously to avoid blocking UI
     */
    private void loadFramesAsync() {
        executor = Executors.newSingleThreadExecutor();

        executor.submit(() -> {
            try {
                System.out.println("Loading " + TOTAL_FRAMES + " frames...");
                frames = new ArrayList<>(TOTAL_FRAMES);

                int loadedCount = 0;
                int failedCount = 0;

                for (int i = 1; i <= TOTAL_FRAMES; i++) {
                    String framePath = String.format(FRAME_PATH_PATTERN, i);

                    try {
                        URL frameURL = getClass().getResource(framePath);

                        if (frameURL != null) {
                            Image image = new Image(frameURL.toExternalForm(), true); // Background loading
                            frames.add(image);
                            loadedCount++;

                            // Update progress every 50 frames
                            if (i % 50 == 0) {
                                final int progress = i;
                                Platform.runLater(() ->
                                        System.out.println("Loading progress: " + progress + "/" + TOTAL_FRAMES)
                                );
                            }
                        } else {
                            System.err.println("Frame not found: " + framePath);
                            // Add a placeholder or skip
                            frames.add(null);
                            failedCount++;
                        }
                    } catch (Exception e) {
                        System.err.println("Error loading frame " + i + ": " + e.getMessage());
                        frames.add(null);
                        failedCount++;
                    }
                }

                final int finalLoaded = loadedCount;
                final int finalFailed = failedCount;

                Platform.runLater(() -> {
                    System.out.println("✅ Frame loading complete!");
                    System.out.println("   Loaded: " + finalLoaded + " frames");
                    if (finalFailed > 0) {
                        System.out.println("   Failed: " + finalFailed + " frames");
                    }

                    isLoading = false;
                    startFrameAnimation();
                });

            } catch (Exception e) {
                System.err.println("Error during frame loading: " + e.getMessage());
                e.printStackTrace();

                Platform.runLater(() -> {
                    System.err.println("❌ Frame loading failed! Check frame paths.");
                    isLoading = false;
                });
            }
        });
    }

    /**
     * Start the frame-by-frame animation
     */
    private void startFrameAnimation() {
        if (frames == null || frames.isEmpty()) {
            System.err.println("No frames loaded - cannot start animation");
            return;
        }

        // Display first frame immediately
        if (frames.get(0) != null) {
            mediaView.setImage(frames.get(0));
        }

        // Start animation timer
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Check if enough time has passed for next frame
                if (now - lastFrameTime >= FRAME_DURATION_NANOS) {
                    lastFrameTime = now;

                    // Get current frame
                    if (currentFrameIndex < frames.size() && frames.get(currentFrameIndex) != null) {
                        mediaView.setImage(frames.get(currentFrameIndex));
                    }

                    // Move to next frame
                    currentFrameIndex++;

                    // Loop back to start
                    if (currentFrameIndex >= frames.size()) {
                        currentFrameIndex = 0;
                    }
                }
            }
        };

        animationTimer.start();
        System.out.println("Frame animation started - playing at 30 FPS (looping)");
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
        // Stop the animation
        cleanup();

        // Navigate to dashboard landing page
        System.out.println("Get Started clicked! Navigating to dashboard...");
        SceneManager.switchScene("/com/examverse/fxml/dashboard/dashboard-landing.fxml");
    }

    /**
     * Cleanup resources when controller is destroyed
     */
    public void cleanup() {
        if (animationTimer != null) {
            animationTimer.stop();
        }

        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }

        // Clear frames from memory
        if (frames != null) {
            frames.clear();
        }
    }
}