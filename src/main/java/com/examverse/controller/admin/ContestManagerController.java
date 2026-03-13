package com.examverse.controller.admin;

import com.examverse.model.exam.Contest;
import com.examverse.model.exam.Contest.Theme;
import com.examverse.model.exam.ContestQuestion;
import com.examverse.model.exam.ContestQuestion.QuestionType;
import com.examverse.service.exam.ContestService;
import com.examverse.util.SceneManager;
import com.examverse.util.SessionManager;
import com.examverse.model.user.User;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

/**
 * ContestManagerController — FIXED v2
 *
 * Bug fixes:
 *  1. "Add Questions" button is now hidden for EVALUATION, FINISHED, CANCELLED.
 *     Admin can only add questions while the contest is UPCOMING (before launch).
 *
 *  2. Question limits enforced: showAddQuestionDialog() now checks how many
 *     MCQ/Written questions already exist and blocks adding more than the
 *     contest's configured total_mcq / total_written limits.
 *
 *  3. "Review Written" button only shown for EVALUATION status.
 *     It is hidden for FINISHED (i.e. after admin clicks "Mark Finished").
 *
 *  4. Leaderboard button stores the contest in session so LeaderboardController
 *     can load only that contest's participants.
 *
 *  5. Auto-refresh every 10s so status changes (including auto-launch) are
 *     reflected without manual reload.
 */
public class ContestManagerController implements Initializable {

    // ── FXML References ────────────────────────────────────────────────────────
    @FXML private VBox  contestListContainer;
    @FXML private Label pageTitle;
    @FXML private Button backBtn, createContestBtn;
    @FXML private ScrollPane mainScroll;
    @FXML private VBox mainContent;

    // ── Services / State ───────────────────────────────────────────────────────
    private final ContestService contestService = new ContestService();
    private User currentUser;
    private Timer refreshTimer;

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = SessionManager.getInstance().getCurrentUser();
        loadContests();

        // Auto-refresh every 10 s so auto-launched contests show their new status
        refreshTimer = new Timer("admin-contest-refresh", true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                Platform.runLater(() -> loadContests());
            }
        }, 10_000, 10_000);
    }

    // ── Load contests ─────────────────────────────────────────────────────────
    private void loadContests() {
        contestListContainer.getChildren().clear();
        List<Contest> contests = contestService.getAllContests();

        if (contests.isEmpty()) {
            Label empty = new Label("No contests yet. Create your first contest!");
            empty.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:14px;");
            contestListContainer.getChildren().add(empty);
            return;
        }

        for (Contest c : contests) {
            contestListContainer.getChildren().add(buildContestCard(c));
        }
    }

    // ── Contest Card ──────────────────────────────────────────────────────────
    private VBox buildContestCard(Contest c) {
        Theme t = c.getTheme();
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color:" + t.getBgColor() +
                "; -fx-background-radius:12;" +
                "-fx-border-color:" + t.getAccentColor() + ";" +
                "-fx-border-radius:12; -fx-border-width:1.5;");

        // ── Title row ──
        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label themeLabel = new Label(t.getDisplayName());
        themeLabel.setStyle("-fx-text-fill:" + t.getAccentColor() +
                "; -fx-font-size:12px; -fx-font-weight:bold;" +
                "-fx-background-color:" + t.getBgColor() +
                "; -fx-padding:3 8 3 8; -fx-background-radius:20;");
        Label titleLabel = new Label(c.getContestTitle());
        titleLabel.setStyle("-fx-text-fill:#ffffff; -fx-font-size:18px; -fx-font-weight:bold;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label statusBadge = buildStatusBadge(c.getStatus());
        titleRow.getChildren().addAll(titleLabel, sp, themeLabel, statusBadge);

        // ── Meta row ──
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

        // Live question count display
        int mcqAdded     = contestService.getQuestionCountByType(c.getContestId(), QuestionType.MCQ);
        int writtenAdded = contestService.getQuestionCountByType(c.getContestId(), QuestionType.WRITTEN);
        String qInfo = "📝 MCQ: " + mcqAdded + "/" + c.getTotalMcqQuestions() +
                "  ✍️ Written: " + writtenAdded + "/" + c.getTotalWrittenQuestions();
        String qInfoStyle = (mcqAdded < c.getTotalMcqQuestions() ||
                writtenAdded < c.getTotalWrittenQuestions())
                ? "-fx-text-fill:#f59e0b; -fx-font-size:12px;"
                : "-fx-text-fill:#22c55e; -fx-font-size:12px;";

        Label metaLabel = new Label("⏰ " + (c.getStartTime() != null ? c.getStartTime().format(fmt) : "—") +
                "  •  ⌛ " + c.getDurationMinutes() + " min" +
                "  •  🏆 " + c.getTotalMarks() + " marks");
        metaLabel.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:13px;");

        Label qCountLabel = new Label(qInfo);
        qCountLabel.setStyle(qInfoStyle);

        // ── Action row ──
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);

        // ─ "Add Questions" button
        // Only show for UPCOMING. Once launched (LIVE/EVALUATION/FINISHED), no more editing.
        boolean canAddQuestions = c.getStatus() == Contest.Status.UPCOMING;
        if (canAddQuestions) {
            Button addQBtn = new Button("➕ Add Questions");
            addQBtn.setStyle("-fx-background-color:" + t.getAccentColor() +
                    "; -fx-text-fill:#000; -fx-font-weight:bold;" +
                    "-fx-background-radius:8; -fx-padding:8 16 8 16;");
            addQBtn.setOnAction(e -> showAddQuestionDialog(c));
            actions.getChildren().add(addQBtn);
        }

        // ─ Leaderboard button (always visible)
        Button viewLbBtn = new Button("🏆 Leaderboard");
        viewLbBtn.setStyle("-fx-background-color:transparent;" +
                "-fx-border-color:" + t.getAccentColor() + ";" +
                "-fx-text-fill:" + t.getAccentColor() + ";" +
                "-fx-background-radius:8; -fx-border-radius:8;" +
                "-fx-padding:7 14 7 14;");
        viewLbBtn.setOnAction(e -> openLeaderboard(c));

        // ─ Status toggle button
        Button statusBtn = buildStatusToggleButton(c, t);

        // ─ "Review Written" button — ONLY for EVALUATION phase
        //   Hidden once admin clicks "Mark Finished" (status becomes FINISHED)
        if (c.getStatus() == Contest.Status.EVALUATION) {
            Button reviewBtn = new Button("✍️ Review Written");
            reviewBtn.setStyle("-fx-background-color:transparent;" +
                    "-fx-border-color:#fbbf24; -fx-text-fill:#fbbf24;" +
                    "-fx-background-radius:8; -fx-border-radius:8;" +
                    "-fx-padding:7 14 7 14;");
            reviewBtn.setOnAction(e -> openWrittenReview(c));
            actions.getChildren().addAll(viewLbBtn, statusBtn, reviewBtn);
        } else {
            actions.getChildren().addAll(viewLbBtn, statusBtn);
        }

        card.getChildren().addAll(titleRow, metaLabel, qCountLabel, actions);
        return card;
    }

    private Label buildStatusBadge(Contest.Status status) {
        Label l = new Label(status.name());
        String color = switch (status) {
            case UPCOMING   -> "#3b82f6";
            case LIVE       -> "#22c55e";
            case EVALUATION -> "#f59e0b";
            case FINISHED   -> "#6b7280";
            case CANCELLED  -> "#ef4444";
        };
        l.setStyle("-fx-background-color:" + color + ";" +
                "-fx-text-fill:#fff; -fx-font-size:11px; -fx-font-weight:bold;" +
                "-fx-padding:3 10 3 10; -fx-background-radius:20;");
        return l;
    }

    private Button buildStatusToggleButton(Contest c, Theme t) {
        String label = switch (c.getStatus()) {
            case UPCOMING   -> "🚀 Launch Contest";
            case LIVE       -> "⏹ End Contest";
            case EVALUATION -> "✅ Mark Finished";
            default         -> "—";
        };
        Button b = new Button(label);
        b.setStyle("-fx-background-color:#1e293b; -fx-text-fill:#e2e8f0;" +
                "-fx-background-radius:8; -fx-padding:7 14 7 14;");
        b.setOnAction(e -> handleStatusToggle(c));
        b.setDisable(c.isFinished() || c.getStatus() == Contest.Status.CANCELLED);
        return b;
    }

    // ── Status toggle ─────────────────────────────────────────────────────────
    private void handleStatusToggle(Contest c) {
        Contest.Status next = switch (c.getStatus()) {
            case UPCOMING   -> Contest.Status.LIVE;
            case LIVE       -> Contest.Status.EVALUATION;
            case EVALUATION -> Contest.Status.FINISHED;
            default         -> null;
        };
        if (next == null) return;

        String msg = switch (next) {
            case LIVE       -> "Launch this contest? Students will be able to join immediately.";
            case EVALUATION -> "End the contest? No more answers will be accepted.";
            case FINISHED   -> "Finalize contest? Ratings will be distributed and the contest will be closed.";
            default         -> "";
        };

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                boolean ok = contestService.updateContestStatus(c.getContestId(), next);
                if (ok) {
                    if (next == Contest.Status.FINISHED) {
                        // Trigger rating distribution (handles contests with no written questions)
                        contestService.distributeRatingChanges(c.getContestId());
                    }
                    Platform.runLater(this::loadContests);
                }
            }
        });
    }

    // ── Add Question Dialog ───────────────────────────────────────────────────
    private void showAddQuestionDialog(Contest contest) {
        // Fetch current counts before opening dialog
        int mcqAdded     = contestService.getQuestionCountByType(contest.getContestId(), QuestionType.MCQ);
        int writtenAdded = contestService.getQuestionCountByType(contest.getContestId(), QuestionType.WRITTEN);
        int mcqLimit     = contest.getTotalMcqQuestions();
        int writtenLimit = contest.getTotalWrittenQuestions();

        boolean mcqFull     = mcqAdded >= mcqLimit;
        boolean writtenFull = writtenAdded >= writtenLimit;

        // If both are full, show message and return
        if (mcqFull && writtenFull) {
            showAlert("Questions Complete",
                    "All questions have already been added for this contest.\n" +
                            "MCQ: " + mcqAdded + "/" + mcqLimit +
                            "   Written: " + writtenAdded + "/" + writtenLimit);
            return;
        }

        Dialog<ContestQuestion> dialog = new Dialog<>();
        dialog.setTitle("Add Question — " + contest.getContestTitle());

        ButtonType addBtn = new ButtonType("Add Question", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addBtn, ButtonType.CANCEL);

        VBox form = new VBox(14);
        form.setPadding(new Insets(20));
        form.setPrefWidth(520);

        // ── Question type selection ──
        Label typeLabel = new Label("Question Type:");
        typeLabel.setStyle("-fx-font-weight:bold;");
        ToggleGroup typeGroup = new ToggleGroup();

        // Show remaining slots so admin knows how many are left
        String mcqText     = "MCQ (Auto-graded)  [" + mcqAdded + "/" + mcqLimit + " added]";
        String writtenText = "Written (Image Upload)  [" + writtenAdded + "/" + writtenLimit + " added]";

        RadioButton mcqRadio     = new RadioButton(mcqText);
        RadioButton writtenRadio = new RadioButton(writtenText);
        mcqRadio.setToggleGroup(typeGroup);
        writtenRadio.setToggleGroup(typeGroup);

        // Disable the type that's already full
        mcqRadio.setDisable(mcqFull);
        writtenRadio.setDisable(writtenFull);

        // Default-select the one that still has slots
        if (!mcqFull) {
            mcqRadio.setSelected(true);
        } else {
            writtenRadio.setSelected(true);
        }

        HBox typeRow = new HBox(20, mcqRadio, writtenRadio);

        // ── Question text ──
        TextArea questionTA = new TextArea();
        questionTA.setPromptText("Question text...");
        questionTA.setPrefRowCount(3);

        // ── Marks (pre-filled from contest config) ──
        TextField marksField = new TextField();
        marksField.setPromptText("Marks");

        // Auto-fill marks based on selected type
        Runnable updateMarks = () -> {
            if (mcqRadio.isSelected()) {
                marksField.setText(String.valueOf(contest.getMcqMarksEach()));
            } else {
                marksField.setText(String.valueOf(contest.getWrittenMarksEach()));
            }
        };
        mcqRadio.selectedProperty().addListener((obs, ov, nv) -> { if (nv) updateMarks.run(); });
        writtenRadio.selectedProperty().addListener((obs, ov, nv) -> { if (nv) updateMarks.run(); });
        updateMarks.run(); // set initial value

        // ── MCQ options ──
        VBox mcqSection = new VBox(8);
        TextField optA = new TextField(); optA.setPromptText("Option A");
        TextField optB = new TextField(); optB.setPromptText("Option B");
        TextField optC = new TextField(); optC.setPromptText("Option C");
        TextField optD = new TextField(); optD.setPromptText("Option D");
        ComboBox<String> correctAns = new ComboBox<>();
        correctAns.getItems().addAll("A", "B", "C", "D");
        correctAns.setValue("A");
        correctAns.setPromptText("Correct Answer");
        TextArea explanationTA = new TextArea();
        explanationTA.setPromptText("Explanation (optional)");
        explanationTA.setPrefRowCount(2);
        mcqSection.getChildren().addAll(
                new Label("Option A:"), optA,
                new Label("Option B:"), optB,
                new Label("Option C:"), optC,
                new Label("Option D:"), optD,
                new Label("Correct Answer:"), correctAns,
                new Label("Explanation (optional):"), explanationTA
        );

        // Toggle MCQ section visibility based on type
        writtenRadio.selectedProperty().addListener((obs, ov, nv) -> mcqSection.setVisible(!nv));
        mcqSection.setVisible(!writtenRadio.isSelected());

        form.getChildren().addAll(
                typeLabel, typeRow,
                new Label("Question:"), questionTA,
                new Label("Marks:"), marksField,
                mcqSection
        );

        ScrollPane sp = new ScrollPane(form);
        sp.setFitToWidth(true);
        sp.setPrefViewportHeight(500);
        dialog.getDialogPane().setContent(sp);

        dialog.setResultConverter(bt -> {
            if (bt != addBtn) return null;

            boolean isMcq = mcqRadio.isSelected();
            QuestionType selectedType = isMcq ? QuestionType.MCQ : QuestionType.WRITTEN;

            // Final guard: re-check limits at submit time (race condition safety)
            int currentCount = contestService.getQuestionCountByType(
                    contest.getContestId(), selectedType);
            int limit = isMcq ? mcqLimit : writtenLimit;
            if (currentCount >= limit) {
                showAlert("Limit Reached",
                        "Cannot add more " + selectedType.name() + " questions. " +
                                "Limit is " + limit + " and " + currentCount + " already added.");
                return null;
            }

            ContestQuestion q = new ContestQuestion();
            q.setContestId(contest.getContestId());
            q.setQuestionText(questionTA.getText().trim());
            q.setType(selectedType);
            try { q.setMarks(Integer.parseInt(marksField.getText())); }
            catch (NumberFormatException e) { q.setMarks(isMcq ? contest.getMcqMarksEach() : contest.getWrittenMarksEach()); }

            // Auto-calculate order index
            q.setOrderIndex(currentCount + 1);

            if (isMcq) {
                q.setOptionA(optA.getText().trim());
                q.setOptionB(optB.getText().trim());
                q.setOptionC(optC.getText().trim());
                q.setOptionD(optD.getText().trim());
                q.setCorrectAnswer(correctAns.getValue());
                q.setExplanation(explanationTA.getText().trim());
            }
            return q;
        });

        dialog.showAndWait().ifPresent(q -> {
            if (q == null) return;
            if (q.getQuestionText().isEmpty()) {
                showAlert("Error", "Question text cannot be empty.");
                return;
            }
            int id = contestService.addQuestion(q);
            if (id > 0) {
                // Re-fetch counts for updated message
                int newMcq     = contestService.getQuestionCountByType(contest.getContestId(), QuestionType.MCQ);
                int newWritten = contestService.getQuestionCountByType(contest.getContestId(), QuestionType.WRITTEN);
                showAlert("Question Added ✅",
                        q.getType().name() + " question added successfully!\n\n" +
                                "MCQ: " + newMcq + "/" + mcqLimit +
                                "   Written: " + newWritten + "/" + writtenLimit);
                loadContests(); // refresh card to show updated counts
            } else {
                showAlert("Error", "Failed to add question. Check console.");
            }
        });
    }

    private void stopTimer() {
        if (refreshTimer != null) { refreshTimer.cancel(); refreshTimer = null; }
    }

    // ── Navigation ────────────────────────────────────────────────────────────
    private void openLeaderboard(Contest c) {
        stopTimer();
        // Store contest in session so LeaderboardController loads contest-specific participants
        SessionManager.getInstance().setCurrentContest(c);
        SessionManager.getInstance().setAttribute("leaderboard_mode", "contest");
        SceneManager.switchScene("/com/examverse/fxml/contest/contest-leaderboard.fxml");
    }

    private void openWrittenReview(Contest c) {
        stopTimer();
        SessionManager.getInstance().setCurrentContest(c);
        SceneManager.switchScene("/com/examverse/fxml/contest/written-review.fxml");
    }

    @FXML
    private void handleBack() {
        if (refreshTimer != null) refreshTimer.cancel();
        SceneManager.switchScene("/com/examverse/fxml/dashboard/admin-dashboard.fxml");
    }

    // ── Create Contest Dialog ─────────────────────────────────────────────────
    @FXML
    private void handleCreateContest() {
        Dialog<Contest> dialog = new Dialog<>();
        dialog.setTitle("Create New Contest");
        dialog.setHeaderText("Fill in contest details and pick a theme");

        ButtonType createBtn = new ButtonType("Create Contest", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        TextField titleField  = new TextField(); titleField.setPromptText("Contest Title");
        TextArea  descTA      = new TextArea();  descTA.setPromptText("Description"); descTA.setPrefRowCount(2);
        TextField durationFld = new TextField("60"); durationFld.setPromptText("Duration (minutes)");
        TextField mcqCountFld = new TextField("10"); mcqCountFld.setPromptText("MCQ questions");
        TextField wrCountFld  = new TextField("2");  wrCountFld.setPromptText("Written questions");
        TextField mcqMarksFld = new TextField("5");  mcqMarksFld.setPromptText("Marks per MCQ");
        TextField wrMarksFld  = new TextField("10"); wrMarksFld.setPromptText("Marks per Written");
        TextField maxGainFld  = new TextField("100"); maxGainFld.setPromptText("Max rating gain");
        TextField maxLossFld  = new TextField("50");  maxLossFld.setPromptText("Max rating loss");

        TextField startFld = new TextField(
                LocalDateTime.now().plusHours(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        startFld.setPromptText("Start (yyyy-MM-dd HH:mm)");
        TextField evalFld = new TextField(
                LocalDateTime.now().plusHours(3).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        evalFld.setPromptText("Eval deadline (yyyy-MM-dd HH:mm)");

        // ── Theme picker ──
        Label themePickerLabel = new Label("Theme:");
        ToggleGroup themeGroup = new ToggleGroup();
        HBox themeRow1 = new HBox(8);
        HBox themeRow2 = new HBox(8);
        Theme[] themes = Theme.values();

        // Prevent deselection: clicking the already-selected theme does nothing
        for (int i = 0; i < themes.length; i++) {
            Theme th = themes[i];
            ToggleButton tb = new ToggleButton(th.getDisplayName());
            tb.setToggleGroup(themeGroup);
            tb.setUserData(th);
            tb.setStyle("-fx-background-color:" + th.getBgColor() +
                    "; -fx-text-fill:" + th.getAccentColor() +
                    "; -fx-border-color:" + th.getAccentColor() +
                    "; -fx-border-radius:8; -fx-background-radius:8;" +
                    "; -fx-padding:6 12 6 12; -fx-font-weight:bold;");
            if (i == 0) tb.setSelected(true);

            // Guard: never allow deselection
            tb.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (!isSelected && themeGroup.getSelectedToggle() == null) {
                    tb.setSelected(true);
                }
            });

            if (i < 4) themeRow1.getChildren().add(tb);
            else       themeRow2.getChildren().add(tb);
        }

        int row = 0;
        grid.add(new Label("Title:"),        0, row); grid.add(titleField,  1, row++);
        grid.add(new Label("Description:"),  0, row); grid.add(descTA,      1, row++);
        grid.add(new Label("Start Time:"),   0, row); grid.add(startFld,    1, row++);
        grid.add(new Label("Duration (min):"),0, row);grid.add(durationFld, 1, row++);
        grid.add(new Label("Eval Deadline:"),0, row); grid.add(evalFld,     1, row++);
        grid.add(new Label("MCQ Count:"),    0, row); grid.add(mcqCountFld, 1, row++);
        grid.add(new Label("Written Count:"),0, row); grid.add(wrCountFld,  1, row++);
        grid.add(new Label("MCQ Marks each:"),0,row); grid.add(mcqMarksFld, 1, row++);
        grid.add(new Label("Written Marks each:"),0,row); grid.add(wrMarksFld,1,row++);
        grid.add(new Label("Max Rating Gain:"),0,row);grid.add(maxGainFld,  1, row++);
        grid.add(new Label("Max Rating Loss:"),0,row);grid.add(maxLossFld,  1, row++);
        grid.add(themePickerLabel, 0, row);
        VBox themeBox = new VBox(6, themeRow1, themeRow2);
        grid.add(themeBox, 1, row);

        ScrollPane sp = new ScrollPane(grid);
        sp.setFitToWidth(true);
        sp.setPrefViewportHeight(480);
        dialog.getDialogPane().setContent(sp);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        dialog.setResultConverter(bt -> {
            if (bt != createBtn) return null;
            try {
                Contest c = new Contest();
                c.setContestTitle(titleField.getText().trim());
                c.setDescription(descTA.getText().trim());
                c.setCreatedBy(currentUser.getId());
                c.setDurationMinutes(Integer.parseInt(durationFld.getText().trim()));
                c.setTotalMcqQuestions(Integer.parseInt(mcqCountFld.getText().trim()));
                c.setTotalWrittenQuestions(Integer.parseInt(wrCountFld.getText().trim()));
                c.setMcqMarksEach(Integer.parseInt(mcqMarksFld.getText().trim()));
                c.setWrittenMarksEach(Integer.parseInt(wrMarksFld.getText().trim()));
                c.setMaxGain(Integer.parseInt(maxGainFld.getText().trim()));
                c.setMaxLoss(Integer.parseInt(maxLossFld.getText().trim()));

                LocalDateTime start = LocalDateTime.parse(startFld.getText().trim(), dtf);
                c.setStartTime(start);
                c.setEndTime(start.plusMinutes(c.getDurationMinutes()));
                c.setEvalDeadline(LocalDateTime.parse(evalFld.getText().trim(), dtf));

                ToggleButton selected = (ToggleButton) themeGroup.getSelectedToggle();
                c.setTheme(selected != null ? (Theme) selected.getUserData() : Theme.COSMIC_ARENA);
                return c;
            } catch (Exception ex) {
                showAlert("Validation Error", "Please check all fields: " + ex.getMessage());
                return null;
            }
        });

        dialog.showAndWait().ifPresent(c -> {
            if (c == null) return;
            int id = contestService.createContest(c);
            if (id > 0) {
                showAlert("Contest Created ✅",
                        "\"" + c.getContestTitle() + "\" created successfully!\n" +
                                "ID: " + id + "\n\n" +
                                "Now add " + c.getTotalMcqQuestions() + " MCQ and " +
                                c.getTotalWrittenQuestions() + " Written questions.");
                loadContests();
            } else {
                showAlert("Error", "Failed to create contest. Check console for details.");
            }
        });
    }

    // ── Utility ───────────────────────────────────────────────────────────────
    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}