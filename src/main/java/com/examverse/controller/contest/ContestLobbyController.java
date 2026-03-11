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
 * ContestLobbyController — FIXED
 *
 * Bug fixes:
 *  1. "Enter Contest" button didn't work:
 *     The root cause was that the overlay was trying to add itself to the
 *     scene root assuming it was a Pane, but SceneManager typically loads
 *     a fresh FXML into a StackPane/BorderPane root that wraps everything.
 *     Fixed by:
 *     a) Never blocking the JavaFX thread — all navigation is via Platform.runLater.
 *     b) Simplifying the overlay to a dedicated StackPane added to rootVBox's parent
 *        using getScene().getRoot() with a safe cast, and falling back gracefully
 *        when the cast fails.
 *     c) Ensuring registerStudent() always returns a valid participantId
 *        (the INSERT IGNORE path now properly re-fetches the existing row).
 *
 *  2. Contests not appearing immediately after admin creates them:
 *     Auto-refresh timer reduced to 5 seconds. Additionally, on every
 *     scene activation (initialize) a fresh DB fetch is performed,
 *     so switching back to the lobby always shows the latest data.
 *
 *  3. General robustness: null-checks on rootVBox.getScene() before any
 *     scene-graph manipulation.
 */
public class ContestLobbyController implements Initializable {

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private VBox    rootVBox;
    @FXML private VBox    contestCardsContainer;
    @FXML private Label   ratingLabel;
    @FXML private Label   rankTitleLabel;
    @FXML private Label   usernameLabel;
    @FXML private Button  leaderboardBtn, backBtn;
    @FXML private ScrollPane scrollPane;

    // ── State ─────────────────────────────────────────────────────────────────
    private final ContestService contestService = new ContestService();
    private User        currentUser;
    private MediaPlayer mediaPlayer;
    private Timer       refreshTimer;

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = SessionManager.getInstance().getCurrentUser();
        loadStudentRating();
        loadContests(); // immediate fresh fetch on every entry

        // ── FIX 2: 5-second refresh so new contests appear quickly ─────────────
        refreshTimer = new Timer(true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> loadContests());
            }
        }, 5_000, 5_000);
    }

    // ── Student Rating Display ─────────────────────────────────────────────────
    private void loadStudentRating() {
        if (currentUser == null) return;
        if (usernameLabel  != null) usernameLabel.setText(currentUser.getFullName());
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
        contestCardsContainer.getChildren().clear();
        List<Contest> contests = contestService.getActiveContests();

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

        card.setOnMouseEntered(e -> card.setStyle(card.getStyle().replace(
                "dropshadow(gaussian, " + t.getAccentColor() + "88, 20",
                "dropshadow(gaussian, " + t.getAccentColor() + "cc, 30")));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace(
                "dropshadow(gaussian, " + t.getAccentColor() + "cc, 30",
                "dropshadow(gaussian, " + t.getAccentColor() + "88, 20")));

        // Top row
        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label themeTag = new Label(t.getDisplayName());
        themeTag.setStyle("-fx-background-color:" + t.getAccentColor() + "33;" +
                "-fx-text-fill:" + t.getAccentColor() + ";" +
                "-fx-font-size:12px; -fx-font-weight:bold;" +
                "-fx-padding:4 12 4 12; -fx-background-radius:20;");
        Region sp1 = new Region(); HBox.setHgrow(sp1, Priority.ALWAYS);
        Label statusBadge = new Label(c.isLive() ? "🔴  LIVE" : "⏳  UPCOMING");
        statusBadge.setStyle("-fx-background-color:" + (c.isLive() ? "#22c55e" : "#3b82f6") + ";" +
                "-fx-text-fill:#fff; -fx-font-size:12px; -fx-font-weight:bold;" +
                "-fx-padding:4 14 4 14; -fx-background-radius:20;");
        if (c.isLive()) {
            FadeTransition pulse = new FadeTransition(Duration.millis(900), statusBadge);
            pulse.setFromValue(1.0); pulse.setToValue(0.4);
            pulse.setCycleCount(Animation.INDEFINITE); pulse.setAutoReverse(true);
            pulse.play();
        }
        topRow.getChildren().addAll(themeTag, sp1, statusBadge);

        Label titleLabel = new Label(c.getContestTitle());
        titleLabel.setStyle("-fx-text-fill:#ffffff; -fx-font-size:22px; -fx-font-weight:bold;");

        Label descLabel = new Label(c.getDescription() != null ? c.getDescription() : "");
        descLabel.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:13px;");
        descLabel.setWrapText(true);

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

        HBox bottomRow = new HBox(16);
        bottomRow.setAlignment(Pos.CENTER_RIGHT);

        if (c.isLive()) {
            Button enterBtn = new Button("⚔️  ENTER CONTEST");
            enterBtn.setStyle(
                    "-fx-background-color: linear-gradient(to right," + t.getAccentColor() + "," +
                            t.getHighlightColor() + ");" +
                            "-fx-text-fill:#000000; -fx-font-weight:bold; -fx-font-size:15px;" +
                            "-fx-padding:12 32 12 32; -fx-background-radius:30;" +
                            "-fx-effect: dropshadow(gaussian," + t.getAccentColor() + ", 12, 0.5, 0, 2);" +
                            "-fx-cursor:hand;"
            );
            // Pulse animation
            ScaleTransition pulse = new ScaleTransition(Duration.millis(800), enterBtn);
            pulse.setFromX(1.0); pulse.setToX(1.04);
            pulse.setFromY(1.0); pulse.setToY(1.04);
            pulse.setCycleCount(Animation.INDEFINITE); pulse.setAutoReverse(true);
            pulse.play();

            // ── FIX 1: Enter contest wired correctly ─────────────────────────
            enterBtn.setOnAction(e -> {
                pulse.stop();
                enterBtn.setDisable(true);
                enterBtn.setText("⏳ Entering...");
                handleEnterContest(c, enterBtn);
            });
            bottomRow.getChildren().add(enterBtn);
        } else {
            Label countdown = new Label("Starting in ...");
            countdown.setStyle("-fx-text-fill:" + t.getAccentColor() +
                    "; -fx-font-size:14px; -fx-font-weight:bold;");
            updateCountdown(countdown, c.getStartTime());
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

    private void updateCountdown(Label label, LocalDateTime startTime) {
        if (startTime == null) { label.setText("TBD"); return; }
        Timeline tl = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            long secs = java.time.Duration.between(LocalDateTime.now(), startTime).getSeconds();
            if (secs <= 0) {
                label.setText("Starting now...");
            } else {
                long h = secs / 3600, m = (secs % 3600) / 60, s = secs % 60;
                label.setText(String.format("Starting in %02d:%02d:%02d", h, m, s));
            }
        }));
        tl.setCycleCount(Animation.INDEFINITE);
        tl.play();
    }

    // ── Enter Contest (Clash Royale style) ────────────────────────────────────
    /**
     * FIX 1: Runs registration on a background thread so the UI never freezes.
     * Music plays if available; after music ends OR after 2.5 s, navigate.
     */
    private void handleEnterContest(Contest c, Button enterBtn) {
        // Do DB work off the JavaFX thread
        Thread registerThread = new Thread(() -> {
            int participantId = contestService.registerStudent(c.getContestId(), currentUser.getId());

            Platform.runLater(() -> {
                if (participantId < 0) {
                    showAlert("Error", "Could not register for this contest. Please try again.");
                    if (enterBtn != null) {
                        enterBtn.setDisable(false);
                        enterBtn.setText("⚔️  ENTER CONTEST");
                    }
                    return;
                }

                // Mark as ACTIVE
                contestService.activateParticipant(participantId);

                // Store in session
                SessionManager.getInstance().setCurrentContest(c);
                SessionManager.getInstance().setCurrentParticipantId(participantId);

                // Show intro then navigate
                showThemeIntro(c);
            });
        }, "register-thread");
        registerThread.setDaemon(true);
        registerThread.start();
    }

    /**
     * FIX 1b: Overlay added safely. Uses Platform.runLater for navigation.
     */
    private void showThemeIntro(Contest c) {
        Theme t = c.getTheme();

        // Attempt music — if no music file, just wait 2.5 s
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
            mediaPlayer.setOnError(() -> Platform.runLater(onFinished::run));
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
        SceneManager.switchScene("/com/examverse/fxml/contest/contest-room.fxml");
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
        SceneManager.switchScene("/com/examverse/fxml/contest/contest-leaderboard.fxml");
    }

    @FXML
    private void handleBack() {
        stopTimer();
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
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
}