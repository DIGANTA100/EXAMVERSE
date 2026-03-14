package com.examverse.controller.contest;

import com.examverse.model.exam.Contest;
import com.examverse.model.exam.Contest.Theme;
import com.examverse.model.exam.ContestParticipant;
import com.examverse.model.exam.ContestQuestion;
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
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

/**
 * ContestLobbyController — final
 *
 * Combines everything:
 *  • Tab bar: "⚔️ Live & Upcoming" / "📜 Past Contests"
 *  • Past contests tab: shows FINISHED/CANCELLED contests with participation
 *    badge (rank, score, rating Δ) and a "View Questions" dialog
 *  • Enter Contest → navigates to contest-intro.fxml (3-D card + theme song)
 *    instead of showing an inline overlay
 *  • All original bug fixes (no DB on FX thread, countdownExpired flag,
 *    hasStudentSubmitted check) preserved
 */
public class ContestLobbyController implements Initializable {

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private VBox        rootVBox;
    @FXML private VBox        contestCardsContainer;
    @FXML private Label       ratingLabel;
    @FXML private Label       rankTitleLabel;
    @FXML private Label       usernameLabel;
    @FXML private Button      leaderboardBtn;
    @FXML private Button      backBtn;
    @FXML private ScrollPane  scrollPane;

    // ── State ─────────────────────────────────────────────────────────────────
    private final ContestService contestService = new ContestService();
    private User             currentUser;
    private Timer            refreshTimer;
    private volatile boolean countdownExpired = false;

    // ── Tab state ─────────────────────────────────────────────────────────────
    private enum Tab { ACTIVE, PAST }
    private Tab    currentTab   = Tab.ACTIVE;
    private Button activeTabBtn;
    private Button pastTabBtn;

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = SessionManager.getInstance().getCurrentUser();
        loadStudentRating();
        buildTabBar();
        loadContests();

        refreshTimer = new Timer("lobby-refresh", true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                if (countdownExpired) countdownExpired = false;
                Platform.runLater(() -> loadContests());
            }
        }, 5_000, 5_000);
    }

    // ── Tab bar ───────────────────────────────────────────────────────────────
    private void buildTabBar() {
        activeTabBtn = new Button("⚔️   Live & Upcoming");
        pastTabBtn   = new Button("📜   Past Contests");
        activeTabBtn.getStyleClass().add("lobby-tab-active");
        pastTabBtn.getStyleClass().add("lobby-tab");

        activeTabBtn.setOnAction(e -> selectTab(Tab.ACTIVE));
        pastTabBtn.setOnAction(e -> selectTab(Tab.PAST));

        HBox capsule = new HBox(2, activeTabBtn, pastTabBtn);
        capsule.getStyleClass().add("lobby-tab-bar");
        capsule.setAlignment(Pos.CENTER_LEFT);

        Label sub = new Label("Browse and join contests · Past contests include full question review");
        sub.getStyleClass().add("lobby-header-sub");
        sub.setStyle("-fx-text-fill:#344155; -fx-font-size:12px;");

        VBox headerBand = new VBox(10, capsule, sub);
        headerBand.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #0d1117, #090e19);" +
                        "-fx-border-color: transparent transparent #1e2d40 transparent;" +
                        "-fx-border-width: 0 0 1.5 0;" +
                        "-fx-padding: 22 26 20 26;"
        );

        if (contestCardsContainer.getParent() instanceof VBox parentVBox) {
            int idx = parentVBox.getChildren().indexOf(contestCardsContainer);
            parentVBox.getChildren().add(idx >= 0 ? idx : 0, headerBand);
        }
    }

    private void selectTab(Tab tab) {
        if (currentTab == tab) return;
        currentTab = tab;
        if (tab == Tab.ACTIVE) {
            activeTabBtn.getStyleClass().setAll("lobby-tab-active");
            pastTabBtn.getStyleClass().setAll("lobby-tab");
        } else {
            pastTabBtn.getStyleClass().setAll("lobby-tab-active");
            activeTabBtn.getStyleClass().setAll("lobby-tab");
        }
        loadContests();
    }

    // ── Load dispatcher ───────────────────────────────────────────────────────
    private void loadContests() {
        if (currentTab == Tab.ACTIVE) loadActiveContests();
        else                          loadPastContests();
    }

    // ── Active (Live + Upcoming) ──────────────────────────────────────────────
    private void loadActiveContests() {
        List<Contest> contests = contestService.getActiveContests();
        contestCardsContainer.getChildren().clear();

        if (contests.isEmpty()) {
            contestCardsContainer.getChildren().add(emptyState(
                    "🎮 No live contests right now",
                    "Check back soon — new contests are added regularly."));
            return;
        }
        for (Contest c : contests)
            contestCardsContainer.getChildren().add(buildActiveContestCard(c));
    }

    // ── Past Contests ─────────────────────────────────────────────────────────
    private void loadPastContests() {
        List<Contest> contests = contestService.getFinishedContests();
        contestCardsContainer.getChildren().clear();

        if (contests.isEmpty()) {
            contestCardsContainer.getChildren().add(emptyState(
                    "📜 No past contests yet",
                    "Finished contests will appear here."));
            return;
        }
        for (Contest c : contests)
            contestCardsContainer.getChildren().add(buildPastContestCard(c));
    }

    // ── Active contest card ───────────────────────────────────────────────────
    private VBox buildActiveContestCard(Contest c) {
        Theme t = c.getTheme();
        VBox card = new VBox(16);
        card.setPadding(new Insets(24));
        card.setMaxWidth(720);
        card.setStyle(
                "-fx-background-color: linear-gradient(to bottom right," + t.getBgColor() + " 0%," +
                        darken(t.getBgColor()) + " 100%);" +
                        "-fx-background-radius:16;" +
                        "-fx-border-color:" + t.getAccentColor() + ";" +
                        "-fx-border-radius:16; -fx-border-width:2;" +
                        "-fx-effect: dropshadow(gaussian," + t.getAccentColor() + "88,20,0.3,0,4);"
        );
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle().replace(
                "dropshadow(gaussian," + t.getAccentColor() + "88,20",
                "dropshadow(gaussian," + t.getAccentColor() + "cc,30")));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace(
                "dropshadow(gaussian," + t.getAccentColor() + "cc,30",
                "dropshadow(gaussian," + t.getAccentColor() + "88,20")));

        // Top row
        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label themeTag = new Label(t.getDisplayName());
        themeTag.setStyle("-fx-background-color:" + t.getAccentColor() + "33;" +
                "-fx-text-fill:" + t.getAccentColor() + ";" +
                "-fx-font-size:12px; -fx-font-weight:bold;" +
                "-fx-padding:4 12 4 12; -fx-background-radius:20;");
        Region spc = new Region(); HBox.setHgrow(spc, Priority.ALWAYS);
        Label statusBadge = new Label(c.isLive() ? "🔴  LIVE" : "⏳  UPCOMING");
        statusBadge.setStyle("-fx-background-color:" + (c.isLive() ? "#22c55e" : "#3b82f6") + ";" +
                "-fx-text-fill:#fff; -fx-font-size:12px; -fx-font-weight:bold;" +
                "-fx-padding:4 14 4 14; -fx-background-radius:20;");
        if (c.isLive()) {
            FadeTransition p = new FadeTransition(Duration.millis(900), statusBadge);
            p.setFromValue(1.0); p.setToValue(0.4);
            p.setCycleCount(Animation.INDEFINITE); p.setAutoReverse(true); p.play();
        }
        topRow.getChildren().addAll(themeTag, spc, statusBadge);

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
            boolean submitted = currentUser != null
                    && contestService.hasStudentSubmitted(c.getContestId(), currentUser.getId());
            if (submitted) {
                Label done = new Label("✅  Already Submitted");
                done.setStyle("-fx-background-color:#22c55e33; -fx-text-fill:#22c55e;" +
                        "-fx-font-size:14px; -fx-font-weight:bold;" +
                        "-fx-padding:10 24 10 24; -fx-background-radius:30;");
                bottomRow.getChildren().add(done);
            } else {
                Button enterBtn = new Button("⚔️  ENTER CONTEST");
                enterBtn.setStyle(
                        "-fx-background-color: linear-gradient(to right," + t.getAccentColor() + "," +
                                t.getHighlightColor() + ");" +
                                "-fx-text-fill:#000000; -fx-font-weight:bold; -fx-font-size:15px;" +
                                "-fx-padding:12 32 12 32; -fx-background-radius:30;" +
                                "-fx-effect: dropshadow(gaussian," + t.getAccentColor() + ",12,0.5,0,2);"
                );
                ScaleTransition pulse = new ScaleTransition(Duration.millis(800), enterBtn);
                pulse.setFromX(1.0); pulse.setToX(1.04);
                pulse.setFromY(1.0); pulse.setToY(1.04);
                pulse.setCycleCount(Animation.INDEFINITE); pulse.setAutoReverse(true); pulse.play();
                enterBtn.setOnAction(e -> handleEnterContest(c, enterBtn));
                bottomRow.getChildren().add(enterBtn);
            }
        } else {
            Label countdown = new Label("Starting in ...");
            countdown.setStyle("-fx-text-fill:" + t.getAccentColor() +
                    "; -fx-font-size:14px; -fx-font-weight:bold;");
            buildCountdownLabelOnly(countdown, c.getStartTime());
            bottomRow.getChildren().add(countdown);
        }

        card.getChildren().addAll(topRow, titleLabel, descLabel, infoRow, bottomRow);
        return card;
    }

    // ── Past contest card ─────────────────────────────────────────────────────
    private VBox buildPastContestCard(Contest c) {
        Theme t = c.getTheme();
        VBox card = new VBox(14);
        card.setPadding(new Insets(22));
        card.setMaxWidth(720);
        card.setStyle(
                "-fx-background-color: linear-gradient(to bottom right," + t.getBgColor() + " 0%," +
                        darken(t.getBgColor()) + " 100%);" +
                        "-fx-background-radius:16;" +
                        "-fx-border-color:" + t.getAccentColor() + "88;" +
                        "-fx-border-radius:16; -fx-border-width:1.5;" +
                        "-fx-opacity:0.92;" +
                        "-fx-effect: dropshadow(gaussian,#00000044,12,0.2,0,3);"
        );

        // Top row
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label themeTag = new Label(t.getDisplayName());
        themeTag.setStyle("-fx-background-color:" + t.getAccentColor() + "22;" +
                "-fx-text-fill:" + t.getAccentColor() + ";" +
                "-fx-font-size:12px; -fx-font-weight:bold;" +
                "-fx-padding:3 10 3 10; -fx-background-radius:20;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        String statusColor = c.getStatus() == Contest.Status.FINISHED ? "#6b7280" : "#ef4444";
        Label statusBadge = new Label(
                c.getStatus() == Contest.Status.FINISHED ? "✅  FINISHED" : "🚫  CANCELLED");
        statusBadge.setStyle("-fx-background-color:" + statusColor + ";" +
                "-fx-text-fill:#fff; -fx-font-size:11px; -fx-font-weight:bold;" +
                "-fx-padding:3 12 3 12; -fx-background-radius:20;");
        topRow.getChildren().addAll(themeTag, spacer, statusBadge);

        Label titleLabel = new Label(c.getContestTitle());
        titleLabel.setStyle("-fx-text-fill:#e2e8f0; -fx-font-size:20px; -fx-font-weight:bold;");
        card.getChildren().addAll(topRow, titleLabel);

        if (c.getDescription() != null && !c.getDescription().isBlank()) {
            Label desc = new Label(c.getDescription());
            desc.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:12px;");
            desc.setWrapText(true);
            card.getChildren().add(desc);
        }

        // Stats row
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd, yyyy • HH:mm");
        HBox statsRow = new HBox(20);
        statsRow.setAlignment(Pos.CENTER_LEFT);
        statsRow.getChildren().addAll(
                infoChip("⏰", c.getEndTime() != null ? "Ended " + c.getEndTime().format(fmt) : "—", t.getAccentColor()),
                infoChip("⌛", c.getDurationMinutes() + " min", t.getAccentColor()),
                infoChip("🏆", c.getTotalMarks() + " marks", t.getAccentColor())
        );
        card.getChildren().add(statsRow);

        // Participation badge
        if (currentUser != null) {
            ContestParticipant myRecord =
                    contestService.getParticipantForStudent(c.getContestId(), currentUser.getId());
            if (myRecord != null) {
                HBox pRow = new HBox(16);
                pRow.setAlignment(Pos.CENTER_LEFT);
                pRow.setPadding(new Insets(8, 14, 8, 14));
                pRow.setStyle("-fx-background-color:#ffffff08; -fx-background-radius:10;");
                String delta = myRecord.getRatingChange() >= 0
                        ? "+" + myRecord.getRatingChange()
                        : String.valueOf(myRecord.getRatingChange());
                String dc = myRecord.getRatingChange() >= 0 ? "#22c55e" : "#ef4444";
                Label youLbl   = new Label("🎮  Participated");
                youLbl.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:12px;");
                Label rankLbl  = new Label("Rank #" + myRecord.getFinalRank());
                rankLbl.setStyle("-fx-text-fill:#e2e8f0; -fx-font-weight:bold; -fx-font-size:12px;");
                Label scoreLbl = new Label("Score " + myRecord.getTotalMarksObtained() + "/" + c.getTotalMarks());
                scoreLbl.setStyle("-fx-text-fill:#e2e8f0; -fx-font-size:12px;");
                Label rdLbl    = new Label("Rating " + delta);
                rdLbl.setStyle("-fx-text-fill:" + dc + "; -fx-font-weight:bold; -fx-font-size:12px;");
                pRow.getChildren().addAll(youLbl, rankLbl, scoreLbl, rdLbl);
                card.getChildren().add(pRow);
            }
        }

        // View Questions button
        HBox bottomRow = new HBox();
        bottomRow.setAlignment(Pos.CENTER_RIGHT);
        bottomRow.setPadding(new Insets(4, 0, 0, 0));
        Button viewQBtn = new Button("📋  View Questions");
        viewQBtn.setStyle(
                "-fx-background-color:" + t.getAccentColor() + "22;" +
                        "-fx-border-color:" + t.getAccentColor() + ";" +
                        "-fx-text-fill:" + t.getAccentColor() + ";" +
                        "-fx-font-weight:bold; -fx-font-size:13px;" +
                        "-fx-padding:9 22 9 22; -fx-background-radius:30; -fx-border-radius:30;"
        );
        viewQBtn.setOnMouseEntered(e -> viewQBtn.setStyle(
                "-fx-background-color:" + t.getAccentColor() + "44;" +
                        "-fx-border-color:" + t.getAccentColor() + ";" +
                        "-fx-text-fill:" + t.getAccentColor() + ";" +
                        "-fx-font-weight:bold; -fx-font-size:13px;" +
                        "-fx-padding:9 22 9 22; -fx-background-radius:30; -fx-border-radius:30;"
        ));
        viewQBtn.setOnMouseExited(e -> viewQBtn.setStyle(
                "-fx-background-color:" + t.getAccentColor() + "22;" +
                        "-fx-border-color:" + t.getAccentColor() + ";" +
                        "-fx-text-fill:" + t.getAccentColor() + ";" +
                        "-fx-font-weight:bold; -fx-font-size:13px;" +
                        "-fx-padding:9 22 9 22; -fx-background-radius:30; -fx-border-radius:30;"
        ));
        viewQBtn.setOnAction(e -> showPastContestQuestions(c));
        bottomRow.getChildren().add(viewQBtn);
        card.getChildren().add(bottomRow);
        return card;
    }

    // ── Past contest questions dialog ─────────────────────────────────────────
    private void showPastContestQuestions(Contest c) {
        Theme t = c.getTheme();
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("📋  " + c.getContestTitle() + " — Questions");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefSize(700, 620);
        dialog.getDialogPane().setStyle("-fx-background-color:#0f172a;");

        VBox loadingBox = new VBox(20);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPadding(new Insets(60));
        ProgressIndicator pi = new ProgressIndicator(-1);
        pi.setStyle("-fx-progress-color:" + t.getAccentColor() + ";");
        Label loadingLbl = new Label("Loading questions...");
        loadingLbl.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:14px;");
        loadingBox.getChildren().addAll(pi, loadingLbl);
        dialog.getDialogPane().setContent(loadingBox);

        Thread fetchThread = new Thread(() -> {
            List<ContestQuestion> questions = contestService.getQuestionsForContest(c.getContestId());
            Platform.runLater(() -> {
                VBox content = new VBox(14);
                content.setPadding(new Insets(20));
                content.setStyle("-fx-background-color:#0f172a;");

                HBox header = new HBox(12);
                header.setAlignment(Pos.CENTER_LEFT);
                header.setStyle("-fx-border-color:transparent transparent #1e293b transparent;" +
                        "-fx-border-width:0 0 2 0; -fx-padding:0 0 12 0;");
                Label themeTag = new Label(t.getDisplayName());
                themeTag.setStyle("-fx-background-color:" + t.getAccentColor() + "33;" +
                        "-fx-text-fill:" + t.getAccentColor() + ";" +
                        "-fx-font-size:12px; -fx-font-weight:bold;" +
                        "-fx-padding:3 10 3 10; -fx-background-radius:20;");
                Label titleLbl = new Label(c.getContestTitle());
                titleLbl.setStyle("-fx-text-fill:#e2e8f0; -fx-font-size:18px; -fx-font-weight:bold;");
                Region hS = new Region(); HBox.setHgrow(hS, Priority.ALWAYS);
                Label qCount = new Label(questions.size() + " Questions");
                qCount.setStyle("-fx-text-fill:#64748b; -fx-font-size:12px;");
                header.getChildren().addAll(themeTag, titleLbl, hS, qCount);
                content.getChildren().add(header);

                if (questions.isEmpty()) {
                    Label noQ = new Label("No questions found for this contest.");
                    noQ.setStyle("-fx-text-fill:#64748b; -fx-font-size:14px;");
                    content.getChildren().add(noQ);
                } else {
                    int n = 1;
                    for (ContestQuestion q : questions)
                        content.getChildren().add(buildQuestionCard(q, n++, t));
                }
                ScrollPane sp = new ScrollPane(content);
                sp.setFitToWidth(true);
                sp.setStyle("-fx-background:#0f172a; -fx-background-color:#0f172a;" +
                        "-fx-border-color:transparent;");
                dialog.getDialogPane().setContent(sp);
            });
        });
        fetchThread.setDaemon(true);
        fetchThread.start();
        dialog.showAndWait();
    }

    private VBox buildQuestionCard(ContestQuestion q, int num, Theme t) {
        boolean isMcq = q.getType() == ContestQuestion.QuestionType.MCQ;
        VBox card = new VBox(10);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color:#1e293b; -fx-background-radius:12;" +
                "-fx-border-color:" + (isMcq ? t.getAccentColor() + "66" : "#f59e0b66") + ";" +
                "-fx-border-radius:12; -fx-border-width:1.5;");

        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label numLbl = new Label("Q" + num);
        numLbl.setStyle("-fx-text-fill:#64748b; -fx-font-size:12px; -fx-font-weight:bold;");
        Label typeBadge = new Label(isMcq ? "📝 MCQ" : "✍️ Written");
        typeBadge.setStyle("-fx-background-color:" + (isMcq ? t.getAccentColor() + "33" : "#f59e0b33") + ";" +
                "-fx-text-fill:" + (isMcq ? t.getAccentColor() : "#f59e0b") + ";" +
                "-fx-font-size:11px; -fx-font-weight:bold;" +
                "-fx-padding:2 8 2 8; -fx-background-radius:20;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label marksLbl = new Label("+" + q.getMarks() + " pts");
        marksLbl.setStyle("-fx-text-fill:#22c55e; -fx-font-size:12px; -fx-font-weight:bold;");
        topRow.getChildren().addAll(numLbl, typeBadge, sp, marksLbl);

        Label qText = new Label(q.getQuestionText());
        qText.setStyle("-fx-text-fill:#e2e8f0; -fx-font-size:14px;");
        qText.setWrapText(true);
        card.getChildren().addAll(topRow, qText);

        if (isMcq) {
            VBox optBox = new VBox(6);
            optBox.setPadding(new Insets(4, 0, 0, 0));
            String[][] opts = {{"A", q.getOptionA()}, {"B", q.getOptionB()},
                    {"C", q.getOptionC()}, {"D", q.getOptionD()}};
            for (String[] opt : opts) {
                if (opt[1] == null || opt[1].isBlank()) continue;
                boolean correct = opt[0].equals(q.getCorrectAnswer());
                Label ol = new Label((correct ? "✅ " : "   ") + opt[0] + ".  " + opt[1]);
                ol.setStyle("-fx-text-fill:" + (correct ? "#22c55e" : "#94a3b8") + ";" +
                        "-fx-font-size:13px;" + (correct ? "-fx-font-weight:bold;" : ""));
                ol.setWrapText(true);
                optBox.getChildren().add(ol);
            }
            card.getChildren().add(optBox);
            if (q.getExplanation() != null && !q.getExplanation().isBlank()) {
                Label exp = new Label("💡 " + q.getExplanation());
                exp.setStyle("-fx-text-fill:#64748b; -fx-font-size:12px; -fx-font-style:italic;");
                exp.setWrapText(true);
                card.getChildren().add(exp);
            }
        } else {
            Label note = new Label("✍️  Students submitted handwritten answers for this question.");
            note.setStyle("-fx-text-fill:#64748b; -fx-font-size:12px; -fx-font-style:italic;");
            card.getChildren().add(note);
        }
        return card;
    }

    // ── Countdown ─────────────────────────────────────────────────────────────
    private void buildCountdownLabelOnly(Label label, LocalDateTime startTime) {
        if (startTime == null) { label.setText("TBD"); return; }
        Timeline tl = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            long secs = java.time.Duration.between(LocalDateTime.now(), startTime).getSeconds();
            if (secs <= 0) { label.setText("Starting now..."); countdownExpired = true; }
            else {
                long h = secs / 3600, m = (secs % 3600) / 60, s = secs % 60;
                label.setText(String.format("Starting in %02d:%02d:%02d", h, m, s));
            }
        }));
        tl.setCycleCount(Animation.INDEFINITE);
        tl.play();
    }

    // ── Enter Contest → intro scene ───────────────────────────────────────────
    private void handleEnterContest(Contest c, Button enterBtn) {
        enterBtn.setDisable(true);
        enterBtn.setText("⏳ Entering...");

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

                System.out.println("DEBUG ENTER: contestId=" + c.getContestId()
                        + "  participantId=" + participantId);

                // Go to the dedicated 3-D intro scene.
                // ContestIntroController plays theme song then routes to contest-room.
                stopTimer();
                SceneManager.switchScene("/com/examverse/fxml/contest/contest-intro.fxml");
            });
        });
        t.setDaemon(true);
        t.start();
    }

    // ── Navigation ────────────────────────────────────────────────────────────
    @FXML private void handleLeaderboard() {
        stopTimer();
        SessionManager.getInstance().setAttribute("leaderboard_mode", "global");
        SceneManager.switchScene("/com/examverse/fxml/contest/contest-leaderboard.fxml");
    }

    @FXML private void handleBack() {
        stopTimer();
        SceneManager.switchScene("/com/examverse/fxml/dashboard/student-dashboard.fxml");
    }

    private void stopTimer() {
        if (refreshTimer != null) { refreshTimer.cancel(); refreshTimer = null; }
    }

    // ── Rating ────────────────────────────────────────────────────────────────
    private void loadStudentRating() {
        if (currentUser == null) return;
        if (usernameLabel  != null) usernameLabel.setText(currentUser.getFullName());
        int rating = contestService.getStudentRating(currentUser.getId());
        if (ratingLabel    != null) ratingLabel.setText(String.valueOf(rating));
        String title = StudentRating.getTitleForRating(rating);
        if (rankTitleLabel != null) {
            rankTitleLabel.setText(title);
            rankTitleLabel.getStyleClass().setAll("rank-title", StudentRating.getTitleCssClass(rating));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private VBox emptyState(String title, String sub) {
        VBox v = new VBox(10);
        v.setAlignment(Pos.CENTER);
        v.setPadding(new Insets(60));
        Label tl = new Label(title);
        tl.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:20px; -fx-font-weight:bold;");
        Label sl = new Label(sub);
        sl.setStyle("-fx-text-fill:#64748b; -fx-font-size:14px;");
        v.getChildren().addAll(tl, sl);
        return v;
    }

    private Label infoChip(String icon, String text, String accent) {
        Label l = new Label(icon + "  " + text);
        l.setStyle("-fx-text-fill:#cbd5e1; -fx-font-size:13px;");
        return l;
    }

    private String darken(String hex) { return hex; }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
}