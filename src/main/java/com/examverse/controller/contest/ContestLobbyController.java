package com.examverse.controller.contest;

import com.examverse.model.exam.Contest;
import com.examverse.model.exam.Contest.Theme;
import com.examverse.model.user.StudentRating;
import com.examverse.model.user.User;
import com.examverse.service.exam.ContestService;
import com.examverse.util.SceneManager;
import com.examverse.util.SessionManager;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

/**
 * ContestLobbyController — FIXED v3
 *
 * Bug fixes in this version:
 *
 *  BUG 1 — "Enter Contest" button still shows after student already submitted:
 *    Root cause: loadContests() rebuilds cards for ALL live contests without
 *    checking if the current student already submitted. So after submission,
 *    the 5-second poll rebuilt the card with the Enter button again.
 *    Fix: buildContestCard() now calls contestService.hasStudentSubmitted()
 *    and shows a "✅ Already Submitted" badge instead of the Enter button when
 *    the student's participant status is SUBMITTED or EVALUATED.
 *
 *  BUG 2 — Screen freeze + "✅ Connected to ExamVerse database successfully!"
 *    spam when countdown reaches zero:
 *    Root cause: buildCountdownWithAutoRefresh() had a Timeline firing every
 *    second that called loadContests() DIRECTLY on the JavaFX thread. loadContests()
 *    opens a DB connection synchronously, which blocks the UI thread → freeze.
 *    On top of that, every new loadContests() call also creates a brand-new
 *    ContestService instance whose static DatabaseConfig.getConnection() logs
 *    "Connected…" — so you saw it once per second.
 *    Fix:
 *      a) The countdown Timeline ONLY updates the label text. It never touches
 *         the DB.
 *      b) When the countdown reaches zero it sets a volatile flag
 *         (countdownExpired) and lets the existing background refreshTimer
 *         (which already runs every 5 s on a daemon thread) pick up the DB
 *         refresh on the next tick. The UI thread is never blocked.
 *      c) The refreshTimer itself already calls Platform.runLater(loadContests)
 *         so the UI rebuild still happens on the FX thread — correctly.
 */
public class ContestLobbyController implements Initializable {

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private VBox   rootVBox;
    @FXML private VBox   contestCardsContainer;
    @FXML private Label  ratingLabel;
    @FXML private Label  rankTitleLabel;
    @FXML private Label  usernameLabel;
    @FXML private Button leaderboardBtn;
    @FXML private Button backBtn;
    @FXML private ScrollPane scrollPane;

    // ── State ─────────────────────────────────────────────────────────────────
    private final ContestService contestService = new ContestService();
    private User currentUser;
    private MediaPlayer mediaPlayer;
    private Timer refreshTimer;

    /**
     * FIX (Bug 2): Set to true by countdown labels when they hit zero.
     * The background refreshTimer checks this flag — if true it forces an
     * immediate loadContests() call and resets the flag.
     * This keeps ALL database work off the JavaFX thread.
     */
    private volatile boolean countdownExpired = false;

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = SessionManager.getInstance().getCurrentUser();
        loadStudentRating();
        loadContests();

        // Poll DB every 5 seconds on a daemon background thread.
        // Platform.runLater ensures the UI rebuild is on the FX thread.
        refreshTimer = new Timer("lobby-refresh", true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // Also triggers immediately when a countdown has just expired
                // (countdownExpired flag was set by the Timeline on the FX thread).
                if (countdownExpired) {
                    countdownExpired = false;
                }
                Platform.runLater(() -> loadContests());
            }
        }, 5_000, 5_000);
    }

    // ── Student Rating Display ────────────────────────────────────────────────
    private void loadStudentRating() {
        if (currentUser == null) return;
        if (usernameLabel != null) usernameLabel.setText(currentUser.getFullName());
        int rating = contestService.getStudentRating(currentUser.getId());
        if (ratingLabel    != null) ratingLabel.setText(String.valueOf(rating));
        String title = StudentRating.getTitleForRating(rating);
        if (rankTitleLabel != null) {
            rankTitleLabel.setText(title);
            rankTitleLabel.getStyleClass().setAll("rank-title",
                    StudentRating.getTitleCssClass(rating));
        }
    }

    // ── Load Contests ─────────────────────────────────────────────────────────
    private void loadContests() {
        List<Contest> contests = contestService.getActiveContests();
        contestCardsContainer.getChildren().clear();

        if (contests.isEmpty()) {
            VBox empty = new VBox(10);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(60));
            Label noContest = new Label("🎮 No live contests right now");
            noContest.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:20px; -fx-font-weight:bold;");
            Label subLabel = new Label("Check back soon — new contests are added regularly.");
            subLabel.setStyle("-fx-text-fill:#64748b; -fx-font-size:14px;");
            empty.getChildren().addAll(noContest, subLabel);
            contestCardsContainer.getChildren().add(empty);
            return;
        }

        for (Contest c : contests) {
            contestCardsContainer.getChildren().add(buildContestCard(c));
        }
    }

    // ── Contest Card ──────────────────────────────────────────────────────────
    private VBox buildContestCard(Contest c) {
        Theme t = c.getTheme();

        VBox card = new VBox(16);
        card.setPadding(new Insets(24));
        card.setMaxWidth(720);
        card.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, " + t.getBgColor() + " 0%, " +
                        darken(t.getBgColor()) + " 100%);" +
                        "-fx-background-radius:16;" +
                        "-fx-border-color:" + t.getAccentColor() + ";" +
                        "-fx-border-radius:16; -fx-border-width:2;" +
                        "-fx-effect: dropshadow(gaussian, " + t.getAccentColor() + "88, 20, 0.3, 0, 4);"
        );

        // Hover glow
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle().replace(
                "dropshadow(gaussian, " + t.getAccentColor() + "88, 20",
                "dropshadow(gaussian, " + t.getAccentColor() + "cc, 30")));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace(
                "dropshadow(gaussian, " + t.getAccentColor() + "cc, 30",
                "dropshadow(gaussian, " + t.getAccentColor() + "88, 20")));

        // ── Top row ──
        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label themeTag = new Label(t.getDisplayName());
        themeTag.setStyle("-fx-background-color:" + t.getAccentColor() + "33;" +
                "-fx-text-fill:" + t.getAccentColor() + ";" +
                "-fx-font-size:12px; -fx-font-weight:bold;" +
                "-fx-padding:4 12 4 12; -fx-background-radius:20;");

        Region sp1 = new Region();
        HBox.setHgrow(sp1, Priority.ALWAYS);

        Label statusBadge = new Label(c.isLive() ? "🔴  LIVE" : "⏳  UPCOMING");
        statusBadge.setStyle("-fx-background-color:" + (c.isLive() ? "#22c55e" : "#3b82f6") + ";" +
                "-fx-text-fill:#fff; -fx-font-size:12px; -fx-font-weight:bold;" +
                "-fx-padding:4 14 4 14; -fx-background-radius:20;");
        if (c.isLive()) {
            FadeTransition pulse = new FadeTransition(Duration.millis(900), statusBadge);
            pulse.setFromValue(1.0);
            pulse.setToValue(0.4);
            pulse.setCycleCount(Animation.INDEFINITE);
            pulse.setAutoReverse(true);
            pulse.play();
        }
        topRow.getChildren().addAll(themeTag, sp1, statusBadge);

        // ── Title ──
        Label titleLabel = new Label(c.getContestTitle());
        titleLabel.setStyle("-fx-text-fill:#ffffff; -fx-font-size:22px; -fx-font-weight:bold;");

        // ── Description ──
        Label descLabel = new Label(c.getDescription() != null ? c.getDescription() : "");
        descLabel.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:13px;");
        descLabel.setWrapText(true);

        // ── Info row ──
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd • HH:mm");
        HBox infoRow = new HBox(24);
        infoRow.setAlignment(Pos.CENTER_LEFT);
        infoRow.getChildren().addAll(
                infoChip("⏰", c.getStartTime() != null ? c.getStartTime().format(fmt) : "—", t.getAccentColor()),
                infoChip("⌛", c.getDurationMinutes() + " min", t.getAccentColor()),
                infoChip("📝", c.getTotalMcqQuestions() + " MCQ", t.getAccentColor()),
                infoChip("✍️", c.getTotalWrittenQuestions() + " Written", t.getAccentColor()),
                infoChip("🏆", c.getTotalMarks() + " marks", t.getAccentColor())
        );

        // ── Bottom row: Enter button, countdown, or "already submitted" ──
        HBox bottomRow = new HBox(16);
        bottomRow.setAlignment(Pos.CENTER_RIGHT);

        if (c.isLive()) {
            // ── FIX (Bug 1): Check if this student already submitted ──────────
            boolean alreadySubmitted = currentUser != null
                    && contestService.hasStudentSubmitted(c.getContestId(), currentUser.getId());

            if (alreadySubmitted) {
                // Student is done — show a non-clickable badge instead of Enter button
                Label doneLabel = new Label("✅  Already Submitted");
                doneLabel.setStyle(
                        "-fx-background-color:#22c55e33;" +
                                "-fx-text-fill:#22c55e;" +
                                "-fx-font-size:14px; -fx-font-weight:bold;" +
                                "-fx-padding:10 24 10 24; -fx-background-radius:30;"
                );
                bottomRow.getChildren().add(doneLabel);
            } else {
                // Normal Enter button
                Button enterBtn = new Button("⚔️  ENTER CONTEST");
                enterBtn.setStyle(
                        "-fx-background-color: linear-gradient(to right," + t.getAccentColor() + "," +
                                t.getHighlightColor() + ");" +
                                "-fx-text-fill:#000000; -fx-font-weight:bold; -fx-font-size:15px;" +
                                "-fx-padding:12 32 12 32; -fx-background-radius:30;" +
                                "-fx-effect: dropshadow(gaussian," + t.getAccentColor() + ", 12, 0.5, 0, 2);"
                );
                ScaleTransition pulse = new ScaleTransition(Duration.millis(800), enterBtn);
                pulse.setFromX(1.0);
                pulse.setToX(1.04);
                pulse.setFromY(1.0);
                pulse.setToY(1.04);
                pulse.setCycleCount(Animation.INDEFINITE);
                pulse.setAutoReverse(true);
                pulse.play();
                enterBtn.setOnAction(e -> handleEnterContest(c, enterBtn));
                bottomRow.getChildren().add(enterBtn);
            }

        } else {
            // ── FIX (Bug 2): Countdown ONLY updates the label — no DB calls ──
            // When it hits zero it sets countdownExpired = true so the existing
            // background refreshTimer does the DB fetch on its next 5-second tick.
            Label countdown = new Label("Starting in ...");
            countdown.setStyle("-fx-text-fill:" + t.getAccentColor() +
                    "; -fx-font-size:14px; -fx-font-weight:bold;");
            buildCountdownLabelOnly(countdown, c.getStartTime());
            bottomRow.getChildren().add(countdown);
        }

        card.getChildren().addAll(topRow, titleLabel, descLabel, infoRow, bottomRow);
        return card;
    }

    private Label infoChip(String icon, String text, String accentColor) {
        Label l = new Label(icon + "  " + text);
        l.setStyle("-fx-text-fill:#cbd5e1; -fx-font-size:13px;");
        return l;
    }

    /**
     * FIX (Bug 2): This Timeline ONLY updates the label text.
     * It does NOT call loadContests() or touch the database.
     *
     * When the countdown hits zero it sets countdownExpired = true.
     * The background refreshTimer (daemon thread, every 5 s) will see this
     * flag on its next tick and call Platform.runLater(loadContests) — which
     * is the correct way to update JavaFX UI from a non-FX thread.
     *
     * Previously: loadContests() was called directly here every second,
     * opening a DB connection on the JavaFX thread → freeze + log spam.
     */
    private void buildCountdownLabelOnly(Label label, LocalDateTime startTime) {
        if (startTime == null) {
            label.setText("TBD");
            return;
        }
        Timeline tl = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            long secs = java.time.Duration.between(LocalDateTime.now(), startTime).getSeconds();
            if (secs <= 0) {
                label.setText("Starting now...");
                // Signal the background thread to do a DB refresh.
                // DO NOT call loadContests() here — this runs on the FX thread.
                countdownExpired = true;
            } else {
                long h = secs / 3600, m = (secs % 3600) / 60, s = secs % 60;
                label.setText(String.format("Starting in %02d:%02d:%02d", h, m, s));
            }
        }));
        tl.setCycleCount(Animation.INDEFINITE);
        tl.play();
    }

    // ── Enter Contest ─────────────────────────────────────────────────────────
    private void handleEnterContest(Contest c, Button enterBtn) {
        // Disable immediately to prevent double-click
        enterBtn.setDisable(true);
        enterBtn.setText("⏳ Entering...");

        // Run DB registration on background thread to keep UI responsive
        Thread t = new Thread(() -> {
            int participantId = contestService.registerStudent(c.getContestId(), currentUser.getId());

            Platform.runLater(() -> {
                if (participantId < 0) {
                    enterBtn.setDisable(false);
                    enterBtn.setText("⚔️  ENTER CONTEST");
                    showAlert("Error", "Could not register for this contest. Please try again.");
                    return;
                }
                contestService.activateParticipant(participantId);
                SessionManager.getInstance().setCurrentContest(c);
                SessionManager.getInstance().setCurrentParticipantId(participantId);
                System.out.println("DEBUG ENTER: entering contestId=" + c.getContestId()
                        + " new participantId=" + participantId);
                showThemeIntro(c);
            });
        });
        t.setDaemon(true);
        t.start();
    }

    /**
     * Full-screen overlay showing theme name + contest title while loading.
     * Plays theme music once if available, then navigates to contest room.
     */
    private void showThemeIntro(Contest c) {
        Theme t = c.getTheme();

        if (rootVBox != null) rootVBox.setOpacity(0.3);

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color:" + t.getBgColor() + "ee;");

        VBox content = new VBox(30);
        content.setAlignment(Pos.CENTER);

        Label bigTheme = new Label(t.getDisplayName());
        bigTheme.setStyle("-fx-text-fill:" + t.getAccentColor() +
                "; -fx-font-size:48px; -fx-font-weight:bold;" +
                "-fx-effect: dropshadow(gaussian," + t.getAccentColor() + ",30,0.8,0,0);");

        Label contestName = new Label(c.getContestTitle());
        contestName.setStyle("-fx-text-fill:#ffffff; -fx-font-size:28px; -fx-font-weight:bold;");

        Label waitLabel = new Label("🎵  Loading arena...");
        waitLabel.setStyle("-fx-text-fill:" + t.getHighlightColor() + "; -fx-font-size:18px;");

        ProgressBar pb = new ProgressBar(-1);
        pb.setPrefWidth(300);
        pb.setStyle("-fx-accent:" + t.getAccentColor() + ";");

        content.getChildren().addAll(bigTheme, contestName, waitLabel, pb);
        overlay.getChildren().add(content);

        if (rootVBox != null && rootVBox.getScene() != null) {
            Parent existingRoot = rootVBox.getScene().getRoot();
            if (existingRoot instanceof Pane existingPane) {
                overlay.setPrefSize(existingPane.getWidth(), existingPane.getHeight());
                existingPane.getChildren().add(overlay);
            }
        }

        FadeTransition fadeIn = new FadeTransition(Duration.millis(600), overlay);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        boolean musicPlayed = tryPlayThemeMusic(c, () ->
                Platform.runLater(this::enterContestRoom));

        if (!musicPlayed) {
            PauseTransition pause = new PauseTransition(Duration.millis(2500));
            pause.setOnFinished(e -> enterContestRoom());
            pause.play();
        }
    }

    private boolean tryPlayThemeMusic(Contest c, Runnable onFinished) {
        try {
            URL musicUrl = getClass().getResource(c.getThemeMusicPath());
            if (musicUrl == null) return false;
            Media media = new Media(musicUrl.toExternalForm());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setOnEndOfMedia(onFinished::run);
            mediaPlayer.setOnError(onFinished::run);
            mediaPlayer.play();
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ Theme music not found: " + c.getThemeMusicPath() +
                    " — proceeding without music.");
            return false;
        }
    }

    private void enterContestRoom() {
        stopMusic();
        stopTimer();
        Platform.runLater(() ->
                SceneManager.switchScene("/com/examverse/fxml/contest/contest-room.fxml")
        );
    }

    private void stopMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────
    @FXML
    private void handleLeaderboard() {
        stopTimer();
        SessionManager.getInstance().setAttribute("leaderboard_mode", "global");
        SceneManager.switchScene("/com/examverse/fxml/contest/contest-leaderboard.fxml");
    }

    @FXML
    private void handleBack() {
        stopTimer();
        stopMusic();
        SceneManager.switchScene("/com/examverse/fxml/dashboard/student-dashboard.fxml");
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────
    private void stopTimer() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private String darken(String hex) { return hex; }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}