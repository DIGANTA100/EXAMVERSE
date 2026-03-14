package com.examverse.controller.contest;

import com.examverse.model.exam.Contest;
import com.examverse.model.exam.Contest.Theme;
import com.examverse.util.SceneManager;
import com.examverse.util.SessionManager;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * ContestIntroController
 * ══════════════════════════════════════════════════════════════════════
 *
 * Drives the full-screen arena intro that plays between clicking
 * "Enter Contest" and entering the contest room.
 *
 * Scene structure (all layers inside the StackPane root):
 *   ① backgroundPane  — deep gradient tinted to the theme colour
 *   ② particlePane    — drifting glow-circles (animated, mouse-transparent)
 *   ③ cardWrapper     — the 3-D perspective card with arena image + info strip
 *   ④ bottomBar       — thin music-progress bar pinned to the window bottom
 *
 * Sequence of events after initialize():
 *   1. Theme colours, arena image, title labels applied instantly.
 *   2. Card entrance: scale 0.6→1.0 + fade in over 700 ms.
 *   3. Card 3-D tilt: a continuous slow RotateTransition on Y-axis (±8°)
 *      simulating the Clash-Royale card-hover feel.
 *   4. Particle layer: 12 drifting translucent circles drift upward with
 *      random delays.
 *   5. Music: theme song plays once. Progress bar fills in real time.
 *      When the song ends → enterContestRoom().
 *   6. Fallback: if no music file is found, a 3.5 s PauseTransition fires
 *      instead so the student still sees the card.
 *   7. "Skip" hint appears after 1 s (mouse-transparent label becomes
 *      interactive; pressing it skips the intro immediately).
 *
 * Arena image naming convention (one PNG per theme, stored in assets/images/arenas/):
 *   cosmic_arena.png  · neon_circuit.png  · dragon_realm.png
 *   frozen_peaks.png  · shadow_temple.png · cyber_storm.png
 *   volcanic_forge.png · ocean_depths.png
 *
 * If an arena image is missing a solid-colour placeholder is shown so the
 * scene never looks broken.
 */
public class ContestIntroController implements Initializable {

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private StackPane rootPane;
    @FXML private VBox      backgroundPane;
    @FXML private Pane      particlePane;
    @FXML private VBox      cardContainer;
    @FXML private StackPane cardWrapper;
    @FXML private VBox      arenaCard;
    @FXML private ImageView arenaImage;
    @FXML private VBox      cardInfoStrip;
    @FXML private Label     themeNameLabel;
    @FXML private Label     contestTitleLabel;
    @FXML private HBox      musicRow;
    @FXML private Label     musicIcon;
    @FXML private Label     musicLabel;
    @FXML private Label     enteringLabel;
    @FXML private VBox      bottomBar;
    @FXML private Label     skipLabel;
    @FXML private ProgressBar musicProgressBar;

    // ── State ─────────────────────────────────────────────────────────────────
    private Contest      contest;
    private Theme        theme;
    private MediaPlayer  mediaPlayer;
    private Timeline     progressTimeline;

    /** Base path for arena images inside the resource folder. */
    private static final String ARENA_IMAGE_BASE =
            "/com/examverse/assets/images/arenas/";

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        contest = SessionManager.getInstance().getCurrentContest();

        if (contest == null) {
            // Safety: should never happen, but bail gracefully
            enterContestRoom();
            return;
        }

        theme = contest.getTheme();

        applyTheme();
        loadArenaImage();
        animateCardEntrance();
        spawnParticles();

        // After the card entrance animation (700 ms), start music + tilt
        PauseTransition startDelay = new PauseTransition(Duration.millis(750));
        startDelay.setOnFinished(e -> {
            startCardTilt();
            startMusic();
            revealSkipHint();
        });
        startDelay.play();
    }

    // ── Theme application ─────────────────────────────────────────────────────
    private void applyTheme() {
        String bg    = theme.getBgColor();
        String acc   = theme.getAccentColor();
        String hi    = theme.getHighlightColor();

        // ① Background — deep gradient from theme-tinted dark to near-black
        backgroundPane.setStyle(
                "-fx-background-color: linear-gradient(to bottom, " +
                        blendDark(bg, 0.18) + " 0%, " +
                        blendDark(bg, 0.06) + " 50%, #020408 100%);"
        );

        // ② Card border + info strip
        arenaCard.setStyle(
                "-fx-background-color:#0a0e1a;" +
                        "-fx-background-radius:24;" +
                        "-fx-border-radius:24;" +
                        "-fx-border-width:2;" +
                        "-fx-border-color:" + acc + ";"
        );

        cardInfoStrip.setStyle(
                "-fx-padding:14 24 18 24;" +
                        "-fx-background-color:#08090f;" +
                        "-fx-background-radius:0 0 24 24;"
        );

        // ③ Glow on the card wrapper (NOT on arenaCard itself — applying DropShadow
        //    directly to arenaCard blurs all its children including the text labels).
        DropShadow cardGlow = new DropShadow();
        cardGlow.setColor(Color.web(acc, 0.65));
        cardGlow.setRadius(42);
        cardGlow.setSpread(0.18);
        cardWrapper.setEffect(cardGlow);

        // ④ Theme name label — no inline effect (glow is on cardWrapper, not text)
        themeNameLabel.setText(theme.getDisplayName().toUpperCase());
        themeNameLabel.setStyle(
                "-fx-font-size:30px;" +
                        "-fx-font-weight:bold;" +
                        "-fx-text-fill:" + acc + ";"
        );

        // ⑤ Contest title
        contestTitleLabel.setText(contest.getContestTitle());
        contestTitleLabel.setStyle(
                "-fx-font-size:15px;" +
                        "-fx-text-fill:#94a3b8;" +
                        "-fx-font-weight:normal;"
        );

        // ⑥ Music row colours
        musicIcon.setStyle("-fx-text-fill:" + acc + "; -fx-font-size:15px;");
        musicLabel.setStyle("-fx-text-fill:#475569; -fx-font-size:12px;");

        // ⑦ Entering label
        enteringLabel.setStyle(
                "-fx-text-fill:" + acc + "44;" +
                        "-fx-font-size:11px;" +
                        "-fx-font-weight:bold;"
        );

        // ⑧ Progress bar accent
        musicProgressBar.setStyle(
                "-fx-background-color:transparent;" +
                        "-fx-accent:" + acc + ";" +
                        "-fx-background-radius:0;"
        );

        // ⑨ Root background
        rootPane.setStyle("-fx-background-color:#020408;");
    }

    // ── Arena image ───────────────────────────────────────────────────────────
    private void loadArenaImage() {
        String imageName = theme.name().toLowerCase() + ".png";
        String fullPath  = ARENA_IMAGE_BASE + imageName;

        // Card dimensions must match the FXML values
        double cardW  = 680;
        double imgH   = 294;  // top 65% of 450px card

        try {
            URL imgUrl = getClass().getResource(fullPath);
            if (imgUrl != null) {
                Image img = new Image(imgUrl.toExternalForm(), cardW, imgH, false, true, true);
                arenaImage.setImage(img);
                arenaImage.setFitWidth(cardW);
                arenaImage.setFitHeight(imgH);
                arenaImage.setPreserveRatio(false);

                // Clip to top rounded corners of the card
                Rectangle clip = new Rectangle(cardW, imgH);
                clip.setArcWidth(48);
                clip.setArcHeight(48);
                arenaImage.setClip(clip);

                // Gradient overlay — fades image into the info strip below
                Region gradient = new Region();
                gradient.setPrefSize(cardW, imgH);
                gradient.setMaxSize(cardW, imgH);
                gradient.setStyle(
                        "-fx-background-color: linear-gradient(to bottom, " +
                                "transparent 40%, #08090f 100%);"
                );
                gradient.setMouseTransparent(true);

                // Insert gradient overlay right after the ImageView (index 1)
                arenaCard.getChildren().add(1, gradient);

            } else {
                showPlaceholderImage();
            }
        } catch (Exception e) {
            System.out.println("⚠️  Arena image not found: " + fullPath + " — using placeholder.");
            showPlaceholderImage();
        }
    }

    private void showPlaceholderImage() {
        arenaImage.setStyle(
                "-fx-background-color:" + theme.getBgColor() + ";"
        );
        // Put a placeholder label over the image area
        Label placeholder = new Label(theme.getDisplayName());
        placeholder.setStyle(
                "-fx-text-fill:" + theme.getAccentColor() + "88;" +
                        "-fx-font-size:20px;" +
                        "-fx-font-weight:bold;"
        );
        placeholder.setAlignment(Pos.CENTER);
        placeholder.setPrefSize(540, 234);
        arenaImage.fitWidthProperty().set(540);
        arenaImage.fitHeightProperty().set(234);

        // Wrap in StackPane and replace the ImageView slot
        StackPane imgHolder = new StackPane(placeholder);
        imgHolder.setPrefSize(540, 234);
        imgHolder.setStyle("-fx-background-color:" + blendDark(theme.getBgColor(), 0.4) + ";");
        arenaCard.getChildren().set(0, imgHolder);
    }

    // ── Card entrance animation ────────────────────────────────────────────────
    /**
     * Card starts invisible + scaled down, then scales up and fades in.
     * Mimics the Clash Royale card-reveal feel.
     */
    private void animateCardEntrance() {
        arenaCard.setOpacity(0);
        arenaCard.setScaleX(0.55);
        arenaCard.setScaleY(0.55);

        // Scale up
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(700), arenaCard);
        scaleUp.setFromX(0.55);
        scaleUp.setFromY(0.55);
        scaleUp.setToX(1.0);
        scaleUp.setToY(1.0);
        scaleUp.setInterpolator(Interpolator.SPLINE(0.34, 0.0, 0.64, 1.0)); // ease-out overshoot feel

        // Fade in
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), arenaCard);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1.0);

        ParallelTransition entrance = new ParallelTransition(scaleUp, fadeIn);
        entrance.play();

        // "PREPARING ARENA" letter-by-letter fade (staggered labels — simulated via opacity pulses)
        PauseTransition labelDelay = new PauseTransition(Duration.millis(300));
        labelDelay.setOnFinished(e -> {
            enteringLabel.setOpacity(0);
            FadeTransition labelFade = new FadeTransition(Duration.millis(600), enteringLabel);
            labelFade.setFromValue(0);
            labelFade.setToValue(1.0);
            labelFade.play();
        });
        labelDelay.play();
    }

    // ── 3-D tilt (Y-axis pendulum) ────────────────────────────────────────────
    /**
     * Continuously rocks the card gently on the Y axis (±8°), giving the
     * Clash-Royale hovering-card feel. Uses Rotate on the card node directly
     * so depth perspective works naturally with the scene's default camera.
     */
    private void startCardTilt() {
        // We set a perspective so the Y-rotation looks 3-D
        arenaCard.setStyle(arenaCard.getStyle()); // keep existing style

        RotateTransition tilt = new RotateTransition(Duration.millis(3200), arenaCard);
        tilt.setFromAngle(-8);
        tilt.setToAngle(8);
        tilt.setCycleCount(Animation.INDEFINITE);
        tilt.setAutoReverse(true);
        tilt.setInterpolator(Interpolator.EASE_BOTH);
        tilt.setAxis(javafx.geometry.Point3D.ZERO.add(0, 1, 0)); // rotate around Y axis
        tilt.play();

        // Subtle vertical float
        TranslateTransition float_ = new TranslateTransition(Duration.millis(2400), arenaCard);
        float_.setFromY(0);
        float_.setToY(-14);
        float_.setCycleCount(Animation.INDEFINITE);
        float_.setAutoReverse(true);
        float_.setInterpolator(Interpolator.EASE_BOTH);
        float_.play();
    }

    // ── Particle layer ────────────────────────────────────────────────────────
    /**
     * Spawns 14 translucent circles that drift upward with random X drift,
     * sized between 6–22 px, tinted to the theme accent colour, each on its
     * own infinite timeline with a random start delay so they look organic.
     */
    private void spawnParticles() {
        String acc = theme.getAccentColor();
        Color  accentColor;
        try { accentColor = Color.web(acc, 0.25); }
        catch (Exception e) { accentColor = Color.web("#7c3aed", 0.25); }

        double screenW = 1280, screenH = 800;
        int[] sizes  = {7, 10, 6, 14, 9, 22, 8, 13, 18, 7, 11, 16, 9, 12};
        double[] xPos = {80, 200, 340, 490, 620, 750, 880, 1000, 1140, 130, 420, 670, 960, 1200};

        for (int i = 0; i < sizes.length; i++) {
            Circle circle = new Circle(sizes[i], accentColor);
            circle.setCenterX(xPos[i]);
            circle.setCenterY(screenH + sizes[i]);

            // Soft glow on each particle
            DropShadow glow = new DropShadow(sizes[i] * 2.0, Color.web(acc, 0.35));
            circle.setEffect(glow);

            particlePane.getChildren().add(circle);

            double durationMs = 5000 + (i * 400) + (Math.random() * 2000);
            double delayMs    = i * 220 + (Math.random() * 800);
            double driftX     = (Math.random() - 0.5) * 120;

            TranslateTransition up = new TranslateTransition(
                    Duration.millis(durationMs), circle);
            up.setFromY(0);
            up.setToY(-(screenH + sizes[i] * 3));
            up.setByX(driftX);
            up.setCycleCount(Animation.INDEFINITE);
            up.setDelay(Duration.millis(delayMs));
            up.setInterpolator(Interpolator.LINEAR);

            FadeTransition fadeOut = new FadeTransition(Duration.millis(durationMs), circle);
            fadeOut.setFromValue(0.28);
            fadeOut.setToValue(0.0);
            fadeOut.setCycleCount(Animation.INDEFINITE);
            fadeOut.setDelay(Duration.millis(delayMs));

            ParallelTransition particle = new ParallelTransition(up, fadeOut);
            particle.play();
        }
    }

    // ── Skip hint ─────────────────────────────────────────────────────────────
    private void revealSkipHint() {
        PauseTransition delay = new PauseTransition(Duration.millis(1200));
        delay.setOnFinished(e -> {
            skipLabel.setText("Press anywhere to skip  ›");
            skipLabel.setStyle(
                    "-fx-text-fill:#334155;" +
                            "-fx-font-size:12px;" +
                            "-fx-cursor:hand;"
            );
            FadeTransition ft = new FadeTransition(Duration.millis(600), skipLabel);
            ft.setFromValue(0); ft.setToValue(1.0);
            ft.play();

            // Make root clickable for skip
            rootPane.setOnMouseClicked(ev -> skipIntro());
        });
        delay.play();
    }

    // ── Music ─────────────────────────────────────────────────────────────────
    private void startMusic() {
        try {
            URL musicUrl = getClass().getResource(contest.getThemeMusicPath());

            if (musicUrl == null) {
                System.out.println("⚠️  Theme music not found: " + contest.getThemeMusicPath());
                startFallbackTimer();
                return;
            }

            Media  media  = new Media(musicUrl.toExternalForm());
            mediaPlayer   = new MediaPlayer(media);

            mediaPlayer.setOnReady(() -> {
                musicLabel.setText("♪  " + theme.getDisplayName() + " — Arena Theme");
                startProgressBar();
                pulseMusiqIcon();
            });

            mediaPlayer.setOnEndOfMedia(this::enterContestRoom);
            mediaPlayer.setOnError(this::startFallbackTimer);

            mediaPlayer.setCycleCount(1);
            mediaPlayer.play();

        } catch (Exception e) {
            System.out.println("⚠️  Music error: " + e.getMessage());
            startFallbackTimer();
        }
    }

    private void startProgressBar() {
        if (mediaPlayer == null) return;

        progressTimeline = new Timeline(
                new KeyFrame(Duration.millis(100), e -> {
                    if (mediaPlayer == null) return;
                    javafx.util.Duration total   = mediaPlayer.getTotalDuration();
                    javafx.util.Duration current = mediaPlayer.getCurrentTime();
                    if (total != null && total.greaterThan(Duration.ZERO)) {
                        musicProgressBar.setProgress(current.toMillis() / total.toMillis());
                    }
                })
        );
        progressTimeline.setCycleCount(Animation.INDEFINITE);
        progressTimeline.play();
    }

    /** Rhythmic scale-pulse on the music icon ♪ to simulate a VU meter. */
    private void pulseMusiqIcon() {
        ScaleTransition pulse = new ScaleTransition(Duration.millis(600), musicIcon);
        pulse.setFromX(1.0); pulse.setToX(1.3);
        pulse.setFromY(1.0); pulse.setToY(1.3);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.setInterpolator(Interpolator.EASE_BOTH);
        pulse.play();
    }

    /** No music file → wait 3.5 s then proceed anyway. */
    private void startFallbackTimer() {
        Platform.runLater(() -> {
            musicLabel.setText("Preparing arena...");
            musicProgressBar.setProgress(-1); // indeterminate

            PauseTransition fallback = new PauseTransition(Duration.millis(3500));
            fallback.setOnFinished(e -> enterContestRoom());
            fallback.play();
        });
    }

    // ── Skip ──────────────────────────────────────────────────────────────────
    private void skipIntro() {
        stopMusic();
        enterContestRoom();
    }

    // ── Navigate to contest room ──────────────────────────────────────────────
    private void enterContestRoom() {
        stopMusic();
        Platform.runLater(() ->
                SceneManager.switchScene("/com/examverse/fxml/contest/contest-room.fxml")
        );
    }

    private void stopMusic() {
        if (progressTimeline != null) {
            progressTimeline.stop();
            progressTimeline = null;
        }
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
    }

    // ── Colour helper ─────────────────────────────────────────────────────────
    /**
     * Returns a hex string that blends the given hex colour toward black
     * at the supplied weight (0.0 = original colour, 1.0 = pure black).
     * Used to tint the background gradient per-theme without the Contest
     * model needing extra methods.
     */
    private String blendDark(String hex, double weight) {
        try {
            Color base = Color.web(hex);
            double r = base.getRed()   * (1 - weight);
            double g = base.getGreen() * (1 - weight);
            double b = base.getBlue()  * (1 - weight);
            return String.format("#%02x%02x%02x",
                    (int)(r * 255), (int)(g * 255), (int)(b * 255));
        } catch (Exception e) {
            return "#0a0e1a";
        }
    }
}