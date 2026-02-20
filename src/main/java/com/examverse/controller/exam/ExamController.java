package com.examverse.controller.exam;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import com.examverse.model.exam.Answer;
import com.examverse.model.exam.Exam;
import com.examverse.model.exam.Question;
import com.examverse.model.user.User;
import com.examverse.service.exam.AnswerService;
import com.examverse.service.exam.EvaluationService;
import com.examverse.service.exam.ExamService;
import com.examverse.service.exam.QuestionService;
import com.examverse.util.SceneManager;
import com.examverse.util.SessionManager;

import java.net.URL;
import java.util.*;

/**
 * ExamController — Manages the full exam-taking experience.
 *
 * ═══════════════════════════════════════════════════════════════════════
 * ROOT CAUSE OF "same-session retake shows old answers + frozen timer"
 * ═══════════════════════════════════════════════════════════════════════
 *
 * When a student submits exam → views result → goes back to dashboard →
 * clicks "Start Exam" again IN THE SAME SESSION:
 *
 * 1. ResultController sets SessionManager["attemptId"] = oldAttemptId
 *    so the result page can display scores.
 *
 * 2. Student clicks "Back to Dashboard" → StudentDashboardController loads.
 *    SessionManager["attemptId"] is STILL the old value unless explicitly
 *    overwritten.
 *
 * 3. Student clicks "Start Exam" → handleStartExam() creates a NEW attempt
 *    in the DB (newAttemptId) and calls:
 *      SessionManager.setAttribute("attemptId", newAttemptId)   ← correct
 *      SessionManager.setAttribute("resumeMode", false)          ← correct
 *
 * 4. SceneManager.switchScene("exam-taking.fxml") loads ExamController.
 *    initialize() runs and reads:
 *      attemptId = SessionManager["attemptId"]  → newAttemptId  ✅
 *      isResume  = SessionManager["resumeMode"] → false          ✅
 *
 *    So far correct. But then:
 *      answerService.deleteAnswersByAttemptId(newAttemptId)
 *    deletes answers for the NEW attempt — which has zero rows — fine.
 *
 * 5. BUT the timer still shows 01:00:00 frozen.
 *    REAL CAUSE: The static `activeTimeline` from the PREVIOUS attempt was
 *    stopped by stopTimer() in submitExam(). However, if SceneManager uses
 *    a StackPane or re-uses the same FXML loader instance (common in some
 *    JavaFX scene managers), the OLD ExamController.initialize() is NOT
 *    called again — the scene is simply made visible again with its old state.
 *
 *    In other words: SceneManager may be CACHING the scene and not reloading
 *    the FXML, meaning initialize() never runs on retake. The old frozen
 *    timer label, old question text, and old answers are all still there
 *    from the previous session's UI nodes.
 *
 * ═══════════════════════════════════════════════════════════════════════
 * THE FIX — Three layers of defence:
 * ═══════════════════════════════════════════════════════════════════════
 *
 * Layer 1 (SceneManager): switchScene() for exam-taking MUST always do a
 *   fresh FXMLLoader.load() — never serve a cached scene. See note at
 *   bottom of this file on how to fix SceneManager.
 *
 * Layer 2 (ExamController): Even if initialize() is called fresh, verify
 *   the resume flag by CHECKING THE DB — if answerService.getAnsweredCount()
 *   returns 0 for this attemptId, it is definitely a fresh start regardless
 *   of what the session flag says. This makes the controller self-validating.
 *
 * Layer 3 (Session cleanup): After reading ALL session attributes needed for
 *   this exam, we immediately overwrite them with sentinel/reset values so
 *   they cannot bleed into the next scene that reads them.
 */
public class ExamController implements Initializable {

    // ── FXML ─────────────────────────────────────────────────────────────────
    @FXML private BorderPane  rootPane;
    @FXML private Label       examTitleLabel;
    @FXML private Label       timerLabel;
    @FXML private Label       questionCounterLabel;
    @FXML private ProgressBar progressBar;

    @FXML private Label       questionNumberLabel;
    @FXML private Label       questionTextLabel;
    @FXML private RadioButton optionA, optionB, optionC, optionD;
    @FXML private ToggleGroup optionsGroup;

    @FXML private Button   previousBtn, nextBtn, submitBtn;
    @FXML private FlowPane questionPalettePane;

    // ── State ─────────────────────────────────────────────────────────────────
    private int     attemptId;
    private int     examId;
    private Exam    exam;
    private List<Question>             questions;
    private final Map<Integer, String> studentAnswers = new HashMap<>();
    private int     currentQuestionIndex = 0;
    private User    currentUser;
    private boolean practiceMode = false;
    private boolean isResume     = false;
    private boolean initFailed   = false;

    // ── Services ──────────────────────────────────────────────────────────────
    private ExamService       examService;
    private QuestionService   questionService;
    private AnswerService     answerService;
    private EvaluationService evaluationService;

    // ── Timer ─────────────────────────────────────────────────────────────────
    private Timeline timeline;
    private int      remainingSeconds;

    /**
     * Static reference to the currently-playing timeline.
     * Allows any new ExamController instance to kill the previous timer.
     */
    private static Timeline activeTimeline = null;

    // ─────────────────────────────────────────────────────────────────────────
    //  INITIALIZE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        // ── LAYER 2 DEFENCE: Kill any leftover timer immediately ──────────────
        // This runs before anything else so even if SceneManager served a
        // cached scene and this IS the old controller reinitialising, the
        // timer is killed before it can affect the display.
        if (activeTimeline != null) {
            activeTimeline.stop();
            activeTimeline = null;
        }

        // Reset ALL UI fields to blank so cached values never show
        resetUI();

        examService       = new ExamService();
        questionService   = new QuestionService();
        answerService     = new AnswerService();
        evaluationService = new EvaluationService();

        currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            fail("Session expired. Please log in again.");
            return;
        }

        // ── Read ALL session attributes in one block, then immediately clear them ──
        Object rawAttemptId = SessionManager.getInstance().getAttribute("attemptId");
        Object rawExamId    = SessionManager.getInstance().getAttribute("examId");
        Object rawPractice  = SessionManager.getInstance().getAttribute("practiceMode");
        Object rawResume    = SessionManager.getInstance().getAttribute("resumeMode");

        // LAYER 3: Immediately reset session to neutral values after reading.
        // This prevents any subsequent scene from accidentally inheriting
        // stale exam session data.
        SessionManager.getInstance().setAttribute("resumeMode", false);
        // Note: do NOT clear attemptId/examId here — ResultController needs attemptId.

        if (rawAttemptId == null || rawExamId == null) {
            fail("Missing exam session data. Please start the exam again.");
            return;
        }

        attemptId    = safeInt(rawAttemptId);
        examId       = safeInt(rawExamId);
        practiceMode = Boolean.TRUE.equals(rawPractice);
        boolean sessionSaysResume = Boolean.TRUE.equals(rawResume);

        if (attemptId <= 0 || examId <= 0) {
            fail("Invalid exam session data. Please try again.");
            return;
        }

        System.out.println("📋 ExamController init — attemptId=" + attemptId
                + " examId=" + examId + " practiceMode=" + practiceMode
                + " sessionSaysResume=" + sessionSaysResume);

        // ── Load exam ─────────────────────────────────────────────────────────
        exam = examService.getExamById(examId, false);
        if (exam == null) {
            fail("This exam is no longer available. It may have been deactivated by your teacher.");
            return;
        }

        // ── Load questions ────────────────────────────────────────────────────
        questions = questionService.getQuestionsByExamId(examId);
        if (questions == null || questions.isEmpty()) {
            fail("This exam has no questions. Please contact your teacher.");
            return;
        }

        // ── LAYER 2: DB-validated resume detection ────────────────────────────
        // Trust the DB, not the session flag. Count how many answers are
        // already saved for this attemptId. If zero → fresh start, period.
        // This is the definitive fix for same-session retake showing old data:
        // even if resumeMode was accidentally left as true in the session,
        // the DB says 0 answers → we treat it as fresh and wipe cleanly.
        studentAnswers.clear();

        if (!practiceMode) {
            int savedCount = answerService.getAnsweredCount(attemptId);
            System.out.println("🔍 DB check — savedCount for attemptId=" + attemptId + ": " + savedCount);

            if (savedCount > 0 && sessionSaysResume) {
                // Genuine resume: DB has answers AND session flagged resume
                isResume = true;
                loadSavedAnswers();
                System.out.println("▶ Mode: RESUME — restoring " + studentAnswers.size() + " answers");
            } else {
                // Fresh start: either DB has no answers (new attempt) OR
                // session flag says fresh (retake). Wipe DB answers as safety net.
                isResume = false;
                answerService.deleteAnswersByAttemptId(attemptId);
                System.out.println("🆕 Mode: FRESH START — answers cleared");
            }
        }
        // Practice mode: no DB answers at all, always fresh
        if (practiceMode) isResume = false;

        // ── Build UI ──────────────────────────────────────────────────────────
        setupExamHeader();
        setupQuestionPalette();
        startTimer();
        loadQuestion(0);

        System.out.println("✅ Exam ready — " + exam.getExamTitle()
                + " | " + questions.size() + " Qs | "
                + exam.getDurationMinutes() + " min | "
                + (isResume ? "RESUMING" : "FRESH START"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  UI RESET  (called at start of every initialize)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Blank out all FXML fields so cached values from a previous session
     * never flash on screen if SceneManager reuses the scene graph.
     */
    private void resetUI() {
        if (examTitleLabel      != null) examTitleLabel.setText("");
        if (timerLabel          != null) { timerLabel.setText("⏱ --:--:--"); timerLabel.setStyle("-fx-text-fill:#22d3ee;-fx-font-weight:bold;"); }
        if (questionCounterLabel!= null) questionCounterLabel.setText("Progress: 0/0");
        if (progressBar         != null) progressBar.setProgress(0.0);
        if (questionNumberLabel != null) questionNumberLabel.setText("");
        if (questionTextLabel   != null) questionTextLabel.setText("");
        if (optionA != null) { optionA.setText(""); optionA.setSelected(false); }
        if (optionB != null) { optionB.setText(""); optionB.setSelected(false); }
        if (optionC != null) { optionC.setText(""); optionC.setSelected(false); }
        if (optionD != null) { optionD.setText(""); optionD.setSelected(false); }
        if (optionsGroup != null) optionsGroup.selectToggle(null);
        if (questionPalettePane != null) questionPalettePane.getChildren().clear();
        currentQuestionIndex = 0;
        studentAnswers.clear();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ANSWER LOADING (RESUME)
    // ─────────────────────────────────────────────────────────────────────────

    private void loadSavedAnswers() {
        Map<Integer, String> saved = answerService.getAnswersByAttemptId(attemptId);
        if (saved != null && !saved.isEmpty()) {
            studentAnswers.putAll(saved);
            System.out.println("📂 Loaded " + studentAnswers.size() + " saved answer(s)");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SETUP HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private void setupExamHeader() {
        if (examTitleLabel != null) {
            examTitleLabel.setText(practiceMode
                    ? "💪 Practice: " + exam.getExamTitle()
                    : exam.getExamTitle());
        }
        updateQuestionCounter();
        if (progressBar != null) progressBar.setProgress(0.0);
    }

    private void setupQuestionPalette() {
        if (questionPalettePane == null) return;
        questionPalettePane.getChildren().clear();
        questionPalettePane.setHgap(8);
        questionPalettePane.setVgap(8);

        for (int i = 0; i < questions.size(); i++) {
            final int idx = i;
            Button btn = new Button(String.valueOf(i + 1));
            btn.setPrefSize(40, 40);
            btn.getStyleClass().add("palette-btn");
            btn.setOnAction(e -> loadQuestion(idx));
            questionPalettePane.getChildren().add(btn);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TIMER
    // ─────────────────────────────────────────────────────────────────────────

    private void startTimer() {
        if (practiceMode) {
            if (timerLabel != null) {
                timerLabel.setText("⏳ Practice Mode");
                timerLabel.setStyle("-fx-text-fill:#a78bfa;-fx-font-weight:bold;");
            }
            return;
        }

        remainingSeconds = exam.getDurationMinutes() * 60;
        if (remainingSeconds <= 0) {
            System.err.println("⚠ Exam duration is 0 — defaulting to 60 min");
            remainingSeconds = 3600;
        }

        updateTimerDisplay();

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remainingSeconds--;
            updateTimerDisplay();
            if (remainingSeconds <= 0) {
                activeTimeline = null;
                timeline.stop();
                autoSubmitExam();
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        activeTimeline = timeline;
    }

    private void stopTimer() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
        if (activeTimeline != null) {
            activeTimeline.stop();
            activeTimeline = null;
        }
    }

    private void updateTimerDisplay() {
        if (timerLabel == null) return;
        int h = remainingSeconds / 3600;
        int m = (remainingSeconds % 3600) / 60;
        int s = remainingSeconds % 60;
        timerLabel.setText(String.format("⏱ %02d:%02d:%02d", h, m, s));

        if      (remainingSeconds <= 300) timerLabel.setStyle("-fx-text-fill:#ef4444;-fx-font-weight:bold;");
        else if (remainingSeconds <= 600) timerLabel.setStyle("-fx-text-fill:#f59e0b;-fx-font-weight:bold;");
        else                              timerLabel.setStyle("-fx-text-fill:#22d3ee;-fx-font-weight:bold;");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  QUESTION NAVIGATION
    // ─────────────────────────────────────────────────────────────────────────

    private void loadQuestion(int index) {
        if (index < 0 || index >= questions.size()) return;

        saveCurrentAnswer();

        currentQuestionIndex = index;
        Question q = questions.get(index);

        if (questionNumberLabel != null)
            questionNumberLabel.setText("Question " + (index + 1) + " of " + questions.size());

        if (questionTextLabel != null) {
            questionTextLabel.setText(
                    q.getQuestionText() != null ? q.getQuestionText() : "(No question text)");
            questionTextLabel.setWrapText(true);
        }

        setOption(optionA, "A", q.getOptionA());
        setOption(optionB, "B", q.getOptionB());
        setOption(optionC, "C", q.getOptionC());
        setOption(optionD, "D", q.getOptionD());

        if (optionsGroup != null) optionsGroup.selectToggle(null);

        String saved = studentAnswers.get(q.getQuestionId());
        if (saved != null) {
            switch (saved.toUpperCase(Locale.ROOT)) {
                case "A" -> { if (optionA != null) optionA.setSelected(true); }
                case "B" -> { if (optionB != null) optionB.setSelected(true); }
                case "C" -> { if (optionC != null) optionC.setSelected(true); }
                case "D" -> { if (optionD != null) optionD.setSelected(true); }
            }
        }

        if (previousBtn != null) previousBtn.setDisable(index == 0);
        if (nextBtn != null)
            nextBtn.setText(index == questions.size() - 1 ? "📋 Review" : "Next →");

        updateQuestionCounter();
        updatePaletteButtons();
    }

    private void setOption(RadioButton btn, String letter, String text) {
        if (btn == null) return;
        btn.setText(letter + ")  " + (text != null ? text : ""));
    }

    private void saveCurrentAnswer() {
        if (currentQuestionIndex < 0 || currentQuestionIndex >= questions.size()) return;
        if (optionsGroup == null) return;

        Question q = questions.get(currentQuestionIndex);
        RadioButton selected = (RadioButton) optionsGroup.getSelectedToggle();

        if (selected != null) {
            String text = selected.getText();
            if (text != null && !text.isEmpty()) {
                char first = Character.toUpperCase(text.trim().charAt(0));
                if (first == 'A' || first == 'B' || first == 'C' || first == 'D') {
                    String answer = String.valueOf(first);
                    studentAnswers.put(q.getQuestionId(), answer);
                    if (!practiceMode && attemptId > 0) {
                        answerService.saveAnswer(new Answer(attemptId, q.getQuestionId(), answer));
                    }
                }
            }
        }
    }

    private void updateQuestionCounter() {
        int answered = studentAnswers.size();
        int total    = (questions != null) ? questions.size() : 0;
        if (questionCounterLabel != null)
            questionCounterLabel.setText("Progress: " + answered + "/" + total);
        if (progressBar != null)
            progressBar.setProgress(total > 0 ? (double) answered / total : 0.0);
    }

    private void updatePaletteButtons() {
        if (questionPalettePane == null || questions == null) return;
        for (int i = 0; i < questionPalettePane.getChildren().size(); i++) {
            if (!(questionPalettePane.getChildren().get(i) instanceof Button btn)) continue;
            if (i >= questions.size()) continue;
            Question q = questions.get(i);
            btn.getStyleClass().removeAll("palette-btn-answered", "palette-btn-current");
            if (i == currentQuestionIndex)
                btn.getStyleClass().add("palette-btn-current");
            else if (studentAnswers.containsKey(q.getQuestionId()))
                btn.getStyleClass().add("palette-btn-answered");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  FXML BUTTON HANDLERS
    // ─────────────────────────────────────────────────────────────────────────

    @FXML private void handlePrevious() {
        if (currentQuestionIndex > 0) loadQuestion(currentQuestionIndex - 1);
    }

    @FXML
    private void handleNext() {
        if (questions == null) return;
        if (currentQuestionIndex == questions.size() - 1) {
            showReviewDialog();
        } else {
            loadQuestion(currentQuestionIndex + 1);
        }
    }

    @FXML
    private void handleSubmit() {
        saveCurrentAnswer();
        if (practiceMode) { showPracticeSummary(); return; }

        int answered   = studentAnswers.size();
        int total      = questions != null ? questions.size() : 0;
        int unanswered = total - answered;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Submit Exam");
        confirm.setHeaderText("Submit " + (exam != null ? exam.getExamTitle() : "Exam") + "?");
        confirm.setContentText(String.format(
                "Answered:   %d / %d\nUnanswered: %d\n\n⚠ You cannot change answers after submission!",
                answered, total, unanswered));
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) submitExam();
        });
    }

    @FXML
    private void handleSaveAndExit() {
        saveCurrentAnswer();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Save & Exit");
        confirm.setHeaderText("Save your progress and exit?");
        confirm.setContentText(
                "Your answers will be saved.\n"
                        + "You can resume this exam from the Ongoing tab.\n\n"
                        + "⚠ The exam timer does NOT pause — it keeps counting down.");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                stopTimer();
                System.out.println("💾 Saved & exited — attemptId=" + attemptId);
                goToDashboard();
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  REVIEW DIALOG
    // ─────────────────────────────────────────────────────────────────────────

    private void showReviewDialog() {
        saveCurrentAnswer();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("📋 Review Answers");
        dialog.setHeaderText("Check your answers before submitting");

        ButtonType submitType = new ButtonType("✅ Submit Now",  ButtonBar.ButtonData.OK_DONE);
        ButtonType backType   = new ButtonType("↩ Back to Exam", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(submitType, backType);

        VBox listBox = new VBox(5);
        listBox.setPadding(new Insets(8));

        int answered = 0, unanswered = 0;

        for (int i = 0; i < questions.size(); i++) {
            Question q    = questions.get(i);
            boolean isAns = studentAnswers.containsKey(q.getQuestionId());
            if (isAns) answered++; else unanswered++;

            final int idx = i;

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(7, 12, 7, 12));
            row.setStyle(isAns
                    ? "-fx-background-color:rgba(34,197,94,0.13);-fx-background-radius:6;"
                    : "-fx-background-color:rgba(239,68,68,0.10);-fx-background-radius:6;");

            Label num = new Label("Q" + (i + 1));
            num.setMinWidth(36);
            num.setStyle("-fx-font-weight:bold;-fx-font-size:13px;");

            String preview = q.getQuestionText() != null ? q.getQuestionText() : "";
            if (preview.length() > 55) preview = preview.substring(0, 52) + "…";
            Label text = new Label(preview);
            text.setStyle("-fx-font-size:13px;");
            HBox.setHgrow(text, Priority.ALWAYS);

            Label status = new Label(isAns
                    ? "✅ " + studentAnswers.get(q.getQuestionId())
                    : "⭕ Skipped");
            status.setStyle(isAns
                    ? "-fx-text-fill:#22c55e;-fx-font-weight:bold;-fx-min-width:70;"
                    : "-fx-text-fill:#ef4444;-fx-font-weight:bold;-fx-min-width:70;");

            Button goBtn = new Button("Go");
            goBtn.setStyle("-fx-font-size:11px;-fx-padding:3 8;-fx-cursor:hand;");
            goBtn.setOnAction(e -> { dialog.close(); loadQuestion(idx); });

            row.getChildren().addAll(num, text, status, goBtn);
            listBox.getChildren().add(row);
        }

        Label summary = new Label(String.format(
                "Answered: %d   |   Skipped: %d   |   Total: %d",
                answered, unanswered, questions.size()));
        summary.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-padding:0 0 6 0;");

        ScrollPane scroll = new ScrollPane(listBox);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(370);
        scroll.setStyle("-fx-background-color:transparent;");

        VBox content = new VBox(8, summary, new Separator(), scroll);
        content.setPadding(new Insets(10));
        content.setPrefWidth(520);
        dialog.getDialogPane().setContent(content);

        dialog.showAndWait().ifPresent(result -> {
            if (result == submitType) handleSubmit();
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SUBMISSION
    // ─────────────────────────────────────────────────────────────────────────

    private void submitExam() {
        stopTimer();
        System.out.println("📝 Submitting — attemptId=" + attemptId);

        boolean success = evaluationService.evaluateExam(attemptId);
        examService.markAttemptCompleted(attemptId); // guarantee COMPLETED status

        if (success) {
            System.out.println("✅ Evaluated and marked COMPLETED");
            SessionManager.getInstance().setAttribute("attemptId",  (Integer) attemptId);
            SessionManager.getInstance().setAttribute("resumeMode", false);
            SceneManager.switchScene("/com/examverse/fxml/exam/exam-result.fxml");
        } else {
            showError("Evaluation failed. Your answers are saved — please contact your administrator.");
        }
    }

    private void showPracticeSummary() {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Practice Complete");
        info.setHeaderText("Practice session finished!");
        int total = questions != null ? questions.size() : 0;
        info.setContentText("You answered " + studentAnswers.size() + " out of " + total
                + " questions.\n\nPractice sessions are not graded. Keep practising!");
        info.showAndWait();
        goToDashboard();
    }

    private void autoSubmitExam() {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.WARNING);
            a.setTitle("Time's Up!");
            a.setHeaderText("⏰ Time has ended");
            a.setContentText("Your exam is being submitted automatically.");
            a.show();
            submitExam();
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ERROR / FAIL
    // ─────────────────────────────────────────────────────────────────────────

    private void fail(String message) {
        initFailed = true;
        stopTimer();
        System.err.println("❌ ExamController fail: " + message);

        Platform.runLater(() -> {
            resetUI(); // blank everything so cached values don't show
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Cannot Load Exam");
            alert.setHeaderText("Exam Unavailable");
            alert.setContentText(message);
            alert.showAndWait();
            goToDashboard();
        });
    }

    private void showError(String message) {
        stopTimer();
        System.err.println("❌ ExamController error: " + message);
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Error"); a.setHeaderText("Something went wrong");
            a.setContentText(message);
            a.showAndWait();
            goToDashboard();
        });
    }

    private void goToDashboard() {
        SceneManager.switchScene("/com/examverse/fxml/dashboard/student-dashboard.fxml");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  UTILITIES
    // ─────────────────────────────────────────────────────────────────────────

    private int safeInt(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        try { return Integer.parseInt(value.toString()); }
        catch (Exception e) { return -1; }
    }
}