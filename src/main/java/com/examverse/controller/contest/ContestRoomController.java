package com.examverse.controller.contest;

import com.examverse.model.exam.*;
import com.examverse.model.exam.Contest.Theme;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * ContestRoomController — FIXED v2
 *
 * BUG FIX: "Seeing previous contest's answers in new contest"
 *
 * Root cause:
 *   The submitted / mcqAnswers / writtenPaths maps were NEVER seeded from the
 *   database on initialize(). They started empty every time. This meant:
 *
 *   1. On re-entry to the SAME contest (back nav, app restart), all previously
 *      answered questions appeared unanswered — student could re-answer them.
 *
 *   2. More critically: if SessionManager.currentParticipantId was stale (still
 *      holding contest-1's participant ID when entering contest-2), the contest
 *      room silently loaded under the wrong participant. The fix in
 *      ContestResultController.handleBack() now clears participantId from
 *      session on exit, and ContestRoomController.initialize() re-derives
 *      participantId from the DB using (contestId + studentId) as the canonical
 *      source of truth — never trusting a potentially stale session value.
 *
 * Additional fix:
 *   initialize() now calls seedAnswerStateFromDb() which loads all existing
 *   contest_answers rows for this participant and pre-populates the three maps.
 *   This makes previously submitted MCQ answers show as "already submitted"
 *   with their chosen option highlighted, and written uploads show their image.
 */
public class ContestRoomController implements Initializable {

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private BorderPane rootPane;
    @FXML private VBox questionNavPanel;
    @FXML private VBox questionContentArea;
    @FXML private VBox leaderboardPanel;
    @FXML private Label timerLabel;
    @FXML private Label contestTitleLabel;
    @FXML private Label liveScoreLabel;
    @FXML private Label questionCounterLabel;
    @FXML private Button submitAllBtn;
    @FXML private ScrollPane questionScroll;

    // ── State ─────────────────────────────────────────────────────────────────
    private final ContestService contestService = new ContestService();
    private User currentUser;
    private Contest contest;
    private int participantId;
    private List<ContestQuestion> questions;
    private int currentQuestionIndex = 0;

    // These three maps are now ALWAYS seeded from DB in initialize() so they
    // are never stale or polluted by a previous contest session.
    private final Map<Integer, String>  mcqAnswers   = new HashMap<>(); // questionId → "A/B/C/D"
    private final Map<Integer, String>  writtenPaths = new HashMap<>(); // questionId → local file path
    private final Map<Integer, Boolean> submitted    = new HashMap<>(); // questionId → answered?

    private Timeline countdownTimer;
    private Timeline leaderboardRefresh;
    private long remainingSeconds;

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = SessionManager.getInstance().getCurrentUser();
        contest     = SessionManager.getInstance().getCurrentContest();

        System.out.println("DEBUG ROOM: contestId=" + (contest != null ? contest.getContestId() : "NULL")
                + " participantId=" + SessionManager.getInstance().getCurrentParticipantId());

        if (contest == null || currentUser == null) {
            showAlert("Error", "Session expired. Please re-enter the contest.");
            return;
        }

        // ── FIX: Always derive participantId from DB using (contestId, studentId).
        // Never rely solely on SessionManager.currentParticipantId which can be
        // stale if the student came back from a previous contest result screen
        // without the session being cleared properly.
        int sessionParticipantId = SessionManager.getInstance().getCurrentParticipantId();
        int dbParticipantId      = contestService.getParticipantId(
                contest.getContestId(), currentUser.getId());

        if (dbParticipantId > 0) {
            // DB is the canonical source of truth
            participantId = dbParticipantId;
            // Correct the session if it was stale
            if (sessionParticipantId != dbParticipantId) {
                System.out.println("⚠️ Stale participantId in session (" + sessionParticipantId
                        + ") corrected to DB value (" + dbParticipantId + ")");
                SessionManager.getInstance().setCurrentParticipantId(dbParticipantId);
            }
        } else if (sessionParticipantId > 0) {
            // Fallback to session (should rarely happen)
            participantId = sessionParticipantId;
        } else {
            showAlert("Error", "Could not find your registration for this contest.");
            return;
        }

        applyTheme(contest.getTheme());
        contestTitleLabel.setText(contest.getContestTitle());

        questions = contestService.getQuestionsForContest(contest.getContestId());
        System.out.println("DEBUG QUESTIONS: contest=" + contest.getContestId()
                + " question count=" + questions.size()
                + " first questionId=" + (questions.isEmpty() ? "none" : questions.get(0).getQuestionId()));
        if (questions.isEmpty()) {
            showAlert("Notice", "This contest has no questions yet.");
            return;
        }

        // ── FIX: Seed answer maps from DB before building the UI.
        // This ensures previously answered questions show their saved state
        // and cannot be re-answered, regardless of how the room was entered.
        seedAnswerStateFromDb();

        buildQuestionNav();
        showQuestion(0);
        startCountdown();
        startLeaderboardRefresh();
        refreshLiveLeaderboard();
    }

    /**
     * Loads all existing answers for this participant from contest_answers
     * and populates mcqAnswers, writtenPaths, and submitted maps.
     *
     * This is critical for two scenarios:
     *  1. Student re-enters a contest room mid-contest (e.g. navigated away).
     *  2. Prevents stale data from a previous session polluting a new contest.
     *
     * Called ONCE during initialize(), before buildQuestionNav() / showQuestion().
     */
    private void seedAnswerStateFromDb() {
        mcqAnswers.clear();
        writtenPaths.clear();
        submitted.clear();


        List<ContestAnswer> existingAnswers = contestService.getStudentAnswers(participantId);
        System.out.println("DEBUG SEED: loading answers for participantId=" + participantId);
        for (ContestAnswer a : existingAnswers) {
            System.out.println("  → questionId=" + a.getQuestionId() + " contestId=" + a.getContestId());
        }
        for (ContestAnswer a : existingAnswers) {
            int qid = a.getQuestionId();
            submitted.put(qid, true);

            if (a.getSelectedOption() != null && !a.getSelectedOption().isEmpty()) {
                // MCQ answer
                mcqAnswers.put(qid, a.getSelectedOption());
            }
            if (a.getImagePath() != null && !a.getImagePath().isEmpty()) {
                // Written upload path
                writtenPaths.put(qid, a.getImagePath());
            }
        }

        System.out.println("✅ Seeded " + existingAnswers.size()
                + " existing answers for participant " + participantId
                + " in contest " + contest.getContestId());
    }

    // ── Theme ─────────────────────────────────────────────────────────────────
    private void applyTheme(Theme t) {
        rootPane.setStyle(
                "-fx-background-color:" + t.getBgColor() + ";" +
                        "-fx-font-family: 'Segoe UI', sans-serif;"
        );
        questionNavPanel.setStyle(
                "-fx-background-color:" + adjustAlpha(t.getBgColor(), "22") + ";" +
                        "-fx-border-color:" + t.getAccentColor() + "33; -fx-border-width:0 1 0 0;"
        );
        leaderboardPanel.setStyle(
                "-fx-background-color:" + adjustAlpha(t.getBgColor(), "22") + ";" +
                        "-fx-border-color:" + t.getAccentColor() + "33; -fx-border-width:0 0 0 1;"
        );
        timerLabel.setStyle("-fx-text-fill:" + t.getHighlightColor() +
                "; -fx-font-size:24px; -fx-font-weight:bold;");
        contestTitleLabel.setStyle("-fx-text-fill:#ffffff; -fx-font-size:18px; -fx-font-weight:bold;");
    }

    private String adjustAlpha(String color, String alpha) { return color; }

    // ── Question Navigation ───────────────────────────────────────────────────
    private void buildQuestionNav() {
        questionNavPanel.getChildren().clear();
        Theme t = contest.getTheme();

        Label navTitle = new Label("Questions");
        navTitle.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:12px; -fx-font-weight:bold;");
        questionNavPanel.getChildren().add(navTitle);

        for (int i = 0; i < questions.size(); i++) {
            final int idx = i;
            ContestQuestion q = questions.get(i);
            Button btn = new Button((i + 1) + (q.isMcq() ? " MCQ" : " WR"));
            btn.setPrefWidth(90);
            btn.setStyle(navBtnStyle(q, false, t));
            btn.setOnAction(e -> showQuestion(idx));
            questionNavPanel.getChildren().add(btn);
        }
    }

    private String navBtnStyle(ContestQuestion q, boolean active, Theme t) {
        String bg = active ? t.getAccentColor()
                : submitted.getOrDefault(q.getQuestionId(), false) ? "#22c55e33" : "#1e293b";
        String text = active ? "#000000" : "#e2e8f0";
        return "-fx-background-color:" + bg + "; -fx-text-fill:" + text + ";" +
                "-fx-background-radius:8; -fx-font-size:12px; -fx-padding:6 10;";
    }

    private void refreshNavButtonStyles() {
        Theme t = contest.getTheme();
        List<Button> btns = questionNavPanel.getChildren().stream()
                .filter(n -> n instanceof Button)
                .map(n -> (Button) n)
                .toList();
        for (int i = 0; i < btns.size() && i < questions.size(); i++) {
            ContestQuestion q = questions.get(i);
            btns.get(i).setStyle(navBtnStyle(q, i == currentQuestionIndex, t));
        }
    }

    // ── Show Question ─────────────────────────────────────────────────────────
    private void showQuestion(int index) {
        currentQuestionIndex = index;
        questionContentArea.getChildren().clear();
        refreshNavButtonStyles();

        ContestQuestion q = questions.get(index);
        Theme t = contest.getTheme();

        questionCounterLabel.setText("Question " + (index + 1) + " / " + questions.size());

        Label typeBadge = new Label(q.isMcq() ? "📝  MCQ" : "✍️  WRITTEN");
        typeBadge.setStyle("-fx-background-color:" + (q.isMcq() ? t.getAccentColor() : "#f59e0b") + ";" +
                "-fx-text-fill:#000; -fx-font-weight:bold; -fx-font-size:12px;" +
                "-fx-padding:3 12 3 12; -fx-background-radius:20;");

        Label marksLabel = new Label("+" + q.getMarks() + " marks");
        marksLabel.setStyle("-fx-text-fill:" + t.getHighlightColor() + "; -fx-font-size:13px;");

        HBox typeRow = new HBox(10, typeBadge, marksLabel);
        typeRow.setAlignment(Pos.CENTER_LEFT);

        Label qText = new Label(q.getQuestionText());
        qText.setStyle("-fx-text-fill:#f1f5f9; -fx-font-size:17px; -fx-font-weight:bold;");
        qText.setWrapText(true);

        VBox questionBox = new VBox(16, typeRow, qText);
        questionBox.setPadding(new Insets(24));
        questionBox.setStyle("-fx-background-color:#1e293b; -fx-background-radius:12;");

        if (q.isMcq()) {
            buildMcqSection(q, questionBox, t);
        } else {
            buildWrittenSection(q, questionBox, t);
        }

        HBox navBtns = new HBox(12);
        navBtns.setAlignment(Pos.CENTER);
        if (index > 0) {
            Button prev = new Button("← Previous");
            prev.setStyle(secondaryBtnStyle(t));
            prev.setOnAction(e -> showQuestion(index - 1));
            navBtns.getChildren().add(prev);
        }
        if (index < questions.size() - 1) {
            Button next = new Button("Next →");
            next.setStyle(primaryBtnStyle(t));
            next.setOnAction(e -> showQuestion(index + 1));
            navBtns.getChildren().add(next);
        }

        questionContentArea.getChildren().addAll(questionBox, navBtns);
        questionContentArea.setPadding(new Insets(24));
    }

    // ── MCQ Section ───────────────────────────────────────────────────────────
    private void buildMcqSection(ContestQuestion q, VBox questionBox, Theme t) {
        String[] optTexts = {q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD()};
        String[] optKeys  = {"A", "B", "C", "D"};

        ToggleGroup group = new ToggleGroup();
        String preSelected = mcqAnswers.get(q.getQuestionId());
        boolean alreadySubmitted = submitted.getOrDefault(q.getQuestionId(), false);

        for (int i = 0; i < 4; i++) {
            if (optTexts[i] == null || optTexts[i].isEmpty()) continue;
            final String key = optKeys[i];

            RadioButton rb = new RadioButton(key + ".  " + optTexts[i]);
            rb.setToggleGroup(group);
            rb.setUserData(key);
            rb.setDisable(alreadySubmitted); // Lock options if already answered
            rb.setStyle("-fx-text-fill:#e2e8f0; -fx-font-size:14px;");
            rb.setPadding(new Insets(10, 14, 10, 14));

            if (key.equals(preSelected)) {
                rb.setSelected(true);
                // Highlight the pre-selected option
                rb.setStyle("-fx-text-fill:#e2e8f0; -fx-font-size:14px;" +
                        "-fx-background-color:" + t.getAccentColor() + "22;" +
                        "-fx-background-radius:8;");
            }

            rb.selectedProperty().addListener((obs, ov, nv) -> {
                if (!alreadySubmitted) {
                    rb.setStyle("-fx-text-fill:#e2e8f0; -fx-font-size:14px;" +
                            (nv ? "-fx-background-color:" + t.getAccentColor() + "22;" +
                                    "-fx-background-radius:8;" : ""));
                }
            });

            questionBox.getChildren().add(rb);
        }

        Button submitMcq = new Button(alreadySubmitted ? "✓  Submitted" : "✓  Submit Answer");
        submitMcq.setStyle(primaryBtnStyle(t) + "-fx-padding:10 24;");
        submitMcq.setDisable(alreadySubmitted);

        if (!alreadySubmitted) {
            submitMcq.setOnAction(e -> {
                if (group.getSelectedToggle() == null) {
                    showAlert("Notice", "Please select an option first.");
                    return;
                }
                String chosen = (String) group.getSelectedToggle().getUserData();
                mcqAnswers.put(q.getQuestionId(), chosen);

                ContestAnswer answer = contestService.submitMcqAnswer(
                        participantId, contest.getContestId(),
                        q.getQuestionId(), currentUser.getId(), chosen);

                if (answer != null) {
                    submitted.put(q.getQuestionId(), true);
                    refreshNavButtonStyles();
                    updateLiveScore();
                    String feedback = answer.isCorrect()
                            ? "✅ Correct! +" + answer.getMarksAwarded() + " marks"
                            : "❌ Incorrect. The correct answer was " + q.getCorrectAnswer();
                    Label fb = new Label(feedback);
                    fb.setStyle("-fx-text-fill:" + (answer.isCorrect() ? "#22c55e" : "#ef4444") +
                            "; -fx-font-weight:bold; -fx-font-size:14px;");
                    questionBox.getChildren().add(fb);
                    submitMcq.setDisable(true);
                    submitMcq.setText("✓  Submitted");
                    // Lock all radio buttons after submission
                    group.getToggles().forEach(toggle ->
                            ((RadioButton) toggle).setDisable(true));
                }
            });
        }

        questionBox.getChildren().add(submitMcq);
    }

    // ── Written Section ───────────────────────────────────────────────────────
    private void buildWrittenSection(ContestQuestion q, VBox questionBox, Theme t) {
        Label instruction = new Label(
                "Write your answer in your notebook (khata), take a clear photo, then upload it below.");
        instruction.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:13px;");
        instruction.setWrapText(true);

        VBox previewBox = new VBox(8);
        previewBox.setStyle("-fx-background-color:#0f172a; -fx-background-radius:8;" +
                "-fx-min-height:180; -fx-alignment:center;");
        previewBox.setAlignment(Pos.CENTER);
        Label previewPlaceholder = new Label("📷  No image uploaded yet");
        previewPlaceholder.setStyle("-fx-text-fill:#475569; -fx-font-size:14px;");
        previewBox.getChildren().add(previewPlaceholder);

        // Show existing upload if any (seeded from DB)
        String existingPath = writtenPaths.get(q.getQuestionId());
        if (existingPath != null) {
            try {
                ImageView iv = new ImageView(new Image("file:" + existingPath));
                iv.setFitWidth(340);
                iv.setPreserveRatio(true);
                previewBox.getChildren().setAll(iv);
            } catch (Exception ignored) {}
        }

        Button uploadBtn = new Button(
                submitted.getOrDefault(q.getQuestionId(), false)
                        ? "🔄  Replace Image" : "📤  Upload Answer Image");
        uploadBtn.setStyle("-fx-background-color:#f59e0b; -fx-text-fill:#000;" +
                "-fx-font-weight:bold; -fx-background-radius:8; -fx-padding:10 24;");

        Label uploadStatus = new Label(
                submitted.getOrDefault(q.getQuestionId(), false)
                        ? "✅ Image uploaded. Awaiting teacher review." : "");
        uploadStatus.setStyle("-fx-text-fill:#22c55e; -fx-font-size:13px;");

        uploadBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Answer Image");
            fc.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp"));

            File chosen = fc.showOpenDialog(rootPane.getScene().getWindow());
            if (chosen == null) return;

            try {
                Path uploadDir = Paths.get(System.getProperty("user.home"),
                        "examverse_uploads", "contest_" + contest.getContestId());
                Files.createDirectories(uploadDir);
                String fileName = "p" + participantId + "_q" + q.getQuestionId() +
                        "_" + System.currentTimeMillis() +
                        chosen.getName().substring(chosen.getName().lastIndexOf('.'));
                Path dest = uploadDir.resolve(fileName);
                Files.copy(chosen.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                String savedPath = dest.toString();

                ImageView iv = new ImageView(new Image("file:" + savedPath));
                iv.setFitWidth(340);
                iv.setPreserveRatio(true);
                previewBox.getChildren().setAll(iv);

                boolean ok = contestService.submitWrittenAnswer(
                        participantId, contest.getContestId(),
                        q.getQuestionId(), currentUser.getId(), savedPath);

                if (ok) {
                    writtenPaths.put(q.getQuestionId(), savedPath);
                    submitted.put(q.getQuestionId(), true);
                    refreshNavButtonStyles();
                    uploadStatus.setText("✅ Image uploaded. Awaiting teacher review.");
                    uploadBtn.setText("🔄  Replace Image");
                } else {
                    uploadStatus.setText("❌ Upload failed. Please try again.");
                    uploadStatus.setStyle("-fx-text-fill:#ef4444; -fx-font-size:13px;");
                }
            } catch (IOException ex) {
                uploadStatus.setText("❌ File error: " + ex.getMessage());
                uploadStatus.setStyle("-fx-text-fill:#ef4444;");
                ex.printStackTrace();
            }
        });

        questionBox.getChildren().addAll(instruction, previewBox, uploadBtn, uploadStatus);
    }

    // ── Timer ─────────────────────────────────────────────────────────────────
    private void startCountdown() {
        if (contest.getEndTime() != null) {
            remainingSeconds = java.time.Duration.between(
                    LocalDateTime.now(), contest.getEndTime()).getSeconds();
        } else {
            remainingSeconds = (long) contest.getDurationMinutes() * 60;
        }
        if (remainingSeconds <= 0) remainingSeconds = 0;

        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remainingSeconds--;
            if (remainingSeconds <= 0) {
                countdownTimer.stop();
                timerLabel.setText("⏰ Time's Up!");
                handleAutoSubmit();
                return;
            }
            long h = remainingSeconds / 3600;
            long m = (remainingSeconds % 3600) / 60;
            long s = remainingSeconds % 60;
            timerLabel.setText(String.format("%02d:%02d:%02d", h, m, s));

            if (remainingSeconds < 300) {
                timerLabel.setStyle(timerLabel.getStyle().replace(
                        contest.getTheme().getHighlightColor(), "#ef4444"));
            }
        }));
        countdownTimer.setCycleCount(Animation.INDEFINITE);
        countdownTimer.play();
    }

    private void handleAutoSubmit() {
        boolean ok = contestService.submitContest(participantId);
        Platform.runLater(() -> {
            showAlert("Contest Over", "Time is up! Your answers have been submitted automatically.");
            navigateToResult();
        });
    }

    // ── Live Leaderboard ──────────────────────────────────────────────────────
    private void startLeaderboardRefresh() {
        leaderboardRefresh = new Timeline(new KeyFrame(Duration.seconds(15),
                e -> Platform.runLater(this::refreshLiveLeaderboard)));
        leaderboardRefresh.setCycleCount(Animation.INDEFINITE);
        leaderboardRefresh.play();
    }

    private void refreshLiveLeaderboard() {
        List<ContestParticipant> lb = contestService.getLiveLeaderboard(contest.getContestId());
        Theme t = contest.getTheme();

        leaderboardPanel.getChildren().clear();
        Label lbTitle = new Label("🏆  Live Rankings");
        lbTitle.setStyle("-fx-text-fill:" + t.getAccentColor() +
                "; -fx-font-size:15px; -fx-font-weight:bold;");
        leaderboardPanel.getChildren().add(lbTitle);

        for (int i = 0; i < Math.min(10, lb.size()); i++) {
            ContestParticipant p = lb.get(i);
            boolean isMe = p.getStudentId() == currentUser.getId();
            String rankEmoji = switch (i) {
                case 0 -> "🥇";
                case 1 -> "🥈";
                case 2 -> "🥉";
                default -> (i + 1) + ".";
            };
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(6, 10, 6, 10));
            row.setStyle(isMe ?
                    "-fx-background-color:" + t.getAccentColor() + "22;" +
                            "-fx-background-radius:8; -fx-border-color:" + t.getAccentColor() + ";" +
                            "-fx-border-radius:8; -fx-border-width:1;" : "");

            Label rankLbl = new Label(rankEmoji);
            rankLbl.setMinWidth(30);
            Label nameLbl = new Label(p.getStudentName() != null ? p.getStudentName() : "—");
            nameLbl.setStyle("-fx-text-fill:" + (isMe ? t.getAccentColor() : "#e2e8f0") +
                    "; -fx-font-weight:" + (isMe ? "bold" : "normal") + ";");
            nameLbl.setMaxWidth(120);
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            Label scoreLbl = new Label(p.getMcqMarksObtained() + " pts");
            scoreLbl.setStyle("-fx-text-fill:" + t.getHighlightColor() + "; -fx-font-weight:bold;");

            row.getChildren().addAll(rankLbl, nameLbl, sp, scoreLbl);
            leaderboardPanel.getChildren().add(row);
        }
    }

    private void updateLiveScore() {
        int myScore = mcqAnswers.keySet().stream().mapToInt(qid -> {
            String ans = mcqAnswers.get(qid);
            ContestQuestion q = questions.stream()
                    .filter(qq -> qq.getQuestionId() == qid).findFirst().orElse(null);
            if (q == null || ans == null) return 0;
            return ans.equalsIgnoreCase(q.getCorrectAnswer()) ? q.getMarks() : 0;
        }).sum();
        liveScoreLabel.setText("My Score: " + myScore);
    }

    // ── Submit All ────────────────────────────────────────────────────────────
    @FXML
    private void handleSubmitAll() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Submit the contest? You won't be able to change your answers.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Submit Contest");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                stopTimers();
                boolean ok = contestService.submitContest(participantId);
                if (ok) navigateToResult();
                else showAlert("Error", "Failed to submit. Please try again.");
            }
        });
    }

    private void navigateToResult() {
        stopTimers();
        SceneManager.switchScene("/com/examverse/fxml/contest/contest-result.fxml");
    }

    private void stopTimers() {
        if (countdownTimer != null) countdownTimer.stop();
        if (leaderboardRefresh != null) leaderboardRefresh.stop();
    }

    // ── Button styles ─────────────────────────────────────────────────────────
    private String primaryBtnStyle(Theme t) {
        return "-fx-background-color:" + t.getAccentColor() + "; -fx-text-fill:#000;" +
                "-fx-font-weight:bold; -fx-background-radius:8; -fx-padding:9 20;";
    }

    private String secondaryBtnStyle(Theme t) {
        return "-fx-background-color:#1e293b; -fx-text-fill:#e2e8f0;" +
                "-fx-background-radius:8; -fx-padding:9 20;";
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}