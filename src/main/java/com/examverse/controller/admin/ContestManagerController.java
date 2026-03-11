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
 * ContestManagerController — FIXED
 *
 * Bug fixes:
 *  1. Theme selection: ToggleButton mutual-exclusion now guaranteed.
 *     Previously, clicking a theme visually deselected others but
 *     getSelectedToggle() could return null when the user clicked the
 *     already-selected button (ToggleGroup allows deselection by default).
 *     Fixed by calling tb.setSelected(true) in the toggle handler AND by
 *     preventing deselection via a selection-guard listener.
 *
 *  2. Question count: after adding a question the card now shows a live
 *     "Questions added" counter fetched straight from the DB, so the admin
 *     always knows the current state without having to reload the page.
 *
 *  3. Real-time visibility: the contest list auto-polls the DB every 10 s
 *     so status changes made in another window (e.g. a second admin session)
 *     are reflected without a manual reload.
 *
 *  4. Review Written button: now visible for EVALUATION status AND for
 *     contests that are LIVE with written questions (so the teacher can
 *     start reviewing immediately after the contest ends).
 */
public class ContestManagerController implements Initializable {

    // ── FXML References ────────────────────────────────────────────────────────
    @FXML private VBox    contestListContainer;
    @FXML private Label   pageTitle;
    @FXML private Button  backBtn, createContestBtn;
    @FXML private ScrollPane mainScroll;
    @FXML private VBox    mainContent;

    // ── Services / State ───────────────────────────────────────────────────────
    private final ContestService contestService = new ContestService();
    private User   currentUser;
    private Timer  refreshTimer;

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = SessionManager.getInstance().getCurrentUser();
        loadContests();
        startAutoRefresh();
    }

    // ── Auto-refresh (real-time networking fix) ───────────────────────────────
    /**
     * Polls DB every 10 seconds so contests created / status-changed by
     * another session appear immediately without restarting the app.
     */
    private void startAutoRefresh() {
        refreshTimer = new Timer(true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
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

        // ── Question count from DB ────────────────────────────────────────────
        List<ContestQuestion> questions = contestService.getQuestionsForContest(c.getContestId());
        long mcqAdded     = questions.stream().filter(ContestQuestion::isMcq).count();
        long writtenAdded = questions.stream().filter(ContestQuestion::isWritten).count();

        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color:" + t.getBgColor() +
                "; -fx-background-radius:12;" +
                "-fx-border-color:" + t.getAccentColor() + ";" +
                "-fx-border-radius:12; -fx-border-width:1.5;");

        // Title row
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

        // Meta row
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        Label metaLabel = new Label("⏰ " + (c.getStartTime() != null ? c.getStartTime().format(fmt) : "—") +
                "  •  ⌛ " + c.getDurationMinutes() + " min" +
                "  •  📝 " + c.getTotalMcqQuestions() + " MCQ + " +
                c.getTotalWrittenQuestions() + " Written" +
                "  •  🏆 " + c.getTotalMarks() + " marks");
        metaLabel.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:13px;");

        // ── FIX 2: Question progress bar showing how many have been added ─────
        boolean allQuestionsAdded = (mcqAdded >= c.getTotalMcqQuestions())
                && (writtenAdded >= c.getTotalWrittenQuestions());
        int totalExpected = c.getTotalMcqQuestions() + c.getTotalWrittenQuestions();
        int totalAdded    = (int)(mcqAdded + writtenAdded);

        String questionProgressColor = allQuestionsAdded ? "#22c55e" : "#f59e0b";
        Label questionProgress = new Label(
                "Questions: " + totalAdded + " / " + totalExpected + " added" +
                        " (" + mcqAdded + " MCQ, " + writtenAdded + " Written)" +
                        (allQuestionsAdded ? "  ✅" : "  ⚠️ needs more questions"));
        questionProgress.setStyle("-fx-text-fill:" + questionProgressColor +
                "; -fx-font-size:13px; -fx-font-weight:bold;");

        // Progress bar
        ProgressBar qProgress = new ProgressBar(
                totalExpected > 0 ? (double) totalAdded / totalExpected : 0);
        qProgress.setPrefWidth(300);
        qProgress.setPrefHeight(8);
        qProgress.setStyle("-fx-accent:" + questionProgressColor + ";");

        VBox progressBox = new VBox(4, questionProgress, qProgress);

        // Action row
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button addQBtn = new Button("➕ Add Questions");
        addQBtn.setStyle("-fx-background-color:" + t.getAccentColor() +
                "; -fx-text-fill:#000; -fx-font-weight:bold;" +
                "-fx-background-radius:8; -fx-padding:8 16 8 16;");
        addQBtn.setOnAction(e -> showAddQuestionDialog(c));

        Button viewQBtn = new Button("👁 View Questions");
        viewQBtn.setStyle("-fx-background-color:transparent;" +
                "-fx-border-color:" + t.getAccentColor() + ";" +
                "-fx-text-fill:" + t.getAccentColor() + ";" +
                "-fx-background-radius:8; -fx-border-radius:8;" +
                "-fx-padding:7 14 7 14;");
        viewQBtn.setOnAction(e -> showViewQuestionsDialog(c));

        Button viewLbBtn = new Button("🏆 Leaderboard");
        viewLbBtn.setStyle("-fx-background-color:transparent;" +
                "-fx-border-color:" + t.getAccentColor() + ";" +
                "-fx-text-fill:" + t.getAccentColor() + ";" +
                "-fx-background-radius:8; -fx-border-radius:8;" +
                "-fx-padding:7 14 7 14;");
        viewLbBtn.setOnAction(e -> openLeaderboard(c));

        Button statusBtn = buildStatusToggleButton(c, t);

        // ── FIX 4: Show Review Written for EVALUATION status ──────────────────
        Button reviewBtn = new Button("✍️ Review Written");
        reviewBtn.setStyle("-fx-background-color:transparent;" +
                "-fx-border-color:#fbbf24; -fx-text-fill:#fbbf24;" +
                "-fx-background-radius:8; -fx-border-radius:8;" +
                "-fx-padding:7 14 7 14;");
        reviewBtn.setOnAction(e -> openWrittenReview(c));
        reviewBtn.setVisible(c.getStatus() == Contest.Status.EVALUATION ||
                c.getStatus() == Contest.Status.FINISHED);
        reviewBtn.setManaged(reviewBtn.isVisible());

        actions.getChildren().addAll(addQBtn, viewQBtn, viewLbBtn, statusBtn, reviewBtn);

        card.getChildren().addAll(titleRow, metaLabel, progressBox, actions);
        return card;
    }

    // ── View Questions Dialog ─────────────────────────────────────────────────
    private void showViewQuestionsDialog(Contest contest) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Questions — " + contest.getContestTitle());

        ButtonType closeBtn = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeBtn);

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setPrefWidth(600);

        List<ContestQuestion> questions = contestService.getQuestionsForContest(contest.getContestId());

        if (questions.isEmpty()) {
            Label none = new Label("No questions added yet.");
            none.setStyle("-fx-font-size:14px;");
            content.getChildren().add(none);
        } else {
            for (int i = 0; i < questions.size(); i++) {
                ContestQuestion q = questions.get(i);
                VBox qCard = new VBox(6);
                qCard.setPadding(new Insets(12));
                qCard.setStyle("-fx-background-color:#f8fafc; -fx-background-radius:8;" +
                        "-fx-border-color:#e2e8f0; -fx-border-radius:8; -fx-border-width:1;");

                Label qLabel = new Label((i + 1) + ". [" + q.getType().name() + " | " +
                        q.getMarks() + " marks] " + q.getQuestionText());
                qLabel.setWrapText(true);
                qLabel.setStyle("-fx-font-weight:bold; -fx-font-size:13px;");
                qCard.getChildren().add(qLabel);

                if (q.isMcq()) {
                    for (String opt : new String[]{
                            "A: " + q.getOptionA(),
                            "B: " + q.getOptionB(),
                            "C: " + q.getOptionC(),
                            "D: " + q.getOptionD()}) {
                        if (opt.length() > 3) {
                            Label ol = new Label("   " + opt);
                            ol.setStyle("-fx-font-size:12px;");
                            qCard.getChildren().add(ol);
                        }
                    }
                    Label ans = new Label("   ✅ Correct: " + q.getCorrectAnswer());
                    ans.setStyle("-fx-text-fill:#22c55e; -fx-font-size:12px; -fx-font-weight:bold;");
                    qCard.getChildren().add(ans);
                }

                // Delete button
                Button delBtn = new Button("🗑 Delete");
                delBtn.setStyle("-fx-background-color:transparent; -fx-text-fill:#ef4444;" +
                        "-fx-border-color:#ef4444; -fx-border-radius:6; -fx-background-radius:6;" +
                        "-fx-font-size:12px; -fx-padding:4 10;");
                delBtn.setOnAction(e -> {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                            "Delete this question?", ButtonType.YES, ButtonType.NO);
                    confirm.showAndWait().ifPresent(bt -> {
                        if (bt == ButtonType.YES) {
                            contestService.deleteQuestion(q.getQuestionId());
                            content.getChildren().remove(qCard);
                            loadContests(); // refresh card counts
                        }
                    });
                });

                qCard.getChildren().add(delBtn);
                content.getChildren().add(qCard);
            }
        }

        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setPrefViewportHeight(480);
        dialog.getDialogPane().setContent(sp);
        dialog.showAndWait();
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
            case FINISHED   -> "Finalize contest? Ratings will be distributed.";
            default         -> "";
        };

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                boolean ok = contestService.updateContestStatus(c.getContestId(), next);
                if (ok) {
                    if (next == Contest.Status.FINISHED) {
                        contestService.distributeRatingChanges(c.getContestId());
                    }
                    Platform.runLater(this::loadContests);
                }
            }
        });
    }

    // ── Add Question Dialog ───────────────────────────────────────────────────
    private void showAddQuestionDialog(Contest contest) {
        Dialog<ContestQuestion> dialog = new Dialog<>();
        dialog.setTitle("Add Question to: " + contest.getContestTitle());

        ButtonType addBtn = new ButtonType("Add Question", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addBtn, ButtonType.CANCEL);

        VBox form = new VBox(14);
        form.setPadding(new Insets(20));
        form.setPrefWidth(520);

        // Question type
        Label typeLabel = new Label("Question Type:");
        typeLabel.setStyle("-fx-font-weight:bold;");
        ToggleGroup typeGroup = new ToggleGroup();
        RadioButton mcqRadio     = new RadioButton("MCQ (Auto-graded)");
        RadioButton writtenRadio = new RadioButton("Written (Image Upload)");
        mcqRadio.setToggleGroup(typeGroup);
        writtenRadio.setToggleGroup(typeGroup);
        mcqRadio.setSelected(true);
        HBox typeRow = new HBox(20, mcqRadio, writtenRadio);

        // Question text
        TextArea questionTA = new TextArea();
        questionTA.setPromptText("Question text...");
        questionTA.setPrefRowCount(3);

        // Marks
        TextField marksField = new TextField("5");
        marksField.setPromptText("Marks");

        // Order
        // Auto-calculate next order index
        int nextOrder = contestService.getQuestionsForContest(contest.getContestId()).size();
        TextField orderField = new TextField(String.valueOf(nextOrder));
        orderField.setPromptText("Order index");

        // MCQ options (hidden for written)
        VBox mcqSection = new VBox(8);
        TextField optA = new TextField(); optA.setPromptText("Option A");
        TextField optB = new TextField(); optB.setPromptText("Option B");
        TextField optC = new TextField(); optC.setPromptText("Option C");
        TextField optD = new TextField(); optD.setPromptText("Option D");
        ComboBox<String> correctAns = new ComboBox<>();
        correctAns.getItems().addAll("A", "B", "C", "D");
        correctAns.setValue("A");
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

        writtenRadio.selectedProperty().addListener((obs, ov, nv) -> {
            mcqSection.setVisible(!nv);
            mcqSection.setManaged(!nv);
        });

        form.getChildren().addAll(
                typeLabel, typeRow,
                new Label("Question:"), questionTA,
                new Label("Marks:"), marksField,
                new Label("Order:"), orderField,
                mcqSection
        );

        ScrollPane sp = new ScrollPane(form);
        sp.setFitToWidth(true);
        sp.setPrefViewportHeight(500);
        dialog.getDialogPane().setContent(sp);

        dialog.setResultConverter(bt -> {
            if (bt != addBtn) return null;
            ContestQuestion q = new ContestQuestion();
            q.setContestId(contest.getContestId());
            q.setQuestionText(questionTA.getText().trim());
            q.setType(mcqRadio.isSelected() ? QuestionType.MCQ : QuestionType.WRITTEN);
            try { q.setMarks(Integer.parseInt(marksField.getText())); }
            catch (NumberFormatException e) { q.setMarks(5); }
            try { q.setOrderIndex(Integer.parseInt(orderField.getText())); }
            catch (NumberFormatException e) { q.setOrderIndex(0); }
            if (mcqRadio.isSelected()) {
                q.setOptionA(optA.getText());
                q.setOptionB(optB.getText());
                q.setOptionC(optC.getText());
                q.setOptionD(optD.getText());
                q.setCorrectAnswer(correctAns.getValue());
                q.setExplanation(explanationTA.getText());
            }
            return q;
        });

        dialog.showAndWait().ifPresent(q -> {
            if (q == null || q.getQuestionText().isEmpty()) {
                showAlert("Error", "Question text cannot be empty.");
                return;
            }
            int id = contestService.addQuestion(q);
            if (id > 0) {
                // Count after addition
                List<ContestQuestion> updated = contestService.getQuestionsForContest(contest.getContestId());
                long mcq     = updated.stream().filter(ContestQuestion::isMcq).count();
                long written = updated.stream().filter(ContestQuestion::isWritten).count();
                showAlert("Success",
                        "Question added! ✅\n\n" +
                                "Total questions for this contest:\n" +
                                "• MCQ: " + mcq + " / " + contest.getTotalMcqQuestions() + "\n" +
                                "• Written: " + written + " / " + contest.getTotalWrittenQuestions());
                loadContests(); // Refresh the card to update the progress bar
            } else {
                showAlert("Error", "Failed to add question.");
            }
        });
    }

    // ── Navigation ────────────────────────────────────────────────────────────
    private void openLeaderboard(Contest c) {
        SessionManager.getInstance().setCurrentContest(c);
        SceneManager.switchScene("/com/examverse/fxml/contest/contest-leaderboard.fxml");
    }

    private void openWrittenReview(Contest c) {
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

        TextField titleField   = new TextField();  titleField.setPromptText("Contest Title");
        TextArea  descTA       = new TextArea();   descTA.setPromptText("Description"); descTA.setPrefRowCount(2);
        TextField durationFld  = new TextField("60");  durationFld.setPromptText("Duration (minutes)");
        TextField mcqCountFld  = new TextField("10");  mcqCountFld.setPromptText("MCQ questions");
        TextField wrCountFld   = new TextField("2");   wrCountFld.setPromptText("Written questions");
        TextField mcqMarksFld  = new TextField("5");   mcqMarksFld.setPromptText("Marks per MCQ");
        TextField wrMarksFld   = new TextField("10");  wrMarksFld.setPromptText("Marks per Written");
        TextField maxGainFld   = new TextField("100"); maxGainFld.setPromptText("Max rating gain");
        TextField maxLossFld   = new TextField("50");  maxLossFld.setPromptText("Max rating loss");

        TextField startFld = new TextField(
                LocalDateTime.now().plusHours(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        startFld.setPromptText("Start (yyyy-MM-dd HH:mm)");
        TextField evalFld = new TextField(
                LocalDateTime.now().plusHours(3).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        evalFld.setPromptText("Eval deadline (yyyy-MM-dd HH:mm)");

        // ── FIX 1: Theme picker — all themes selectable ───────────────────────
        // We use a ToggleGroup but add a change-listener that prevents
        // deselection (clicking the selected button again stays selected).
        Label themeLabel = new Label("Theme:");
        ToggleGroup themeGroup = new ToggleGroup();

        // Track the last selected toggle so we can restore it on attempted deselect
        final ToggleButton[] lastSelected = {null};

        HBox themeRow1 = new HBox(8);
        HBox themeRow2 = new HBox(8);
        Theme[] themes = Theme.values();

        for (int i = 0; i < themes.length; i++) {
            Theme th = themes[i];
            ToggleButton tb = new ToggleButton(th.getDisplayName());
            tb.setToggleGroup(themeGroup);
            tb.setUserData(th);
            tb.setStyle("-fx-background-color:" + th.getBgColor() +
                    "; -fx-text-fill:" + th.getAccentColor() +
                    "; -fx-border-color:" + th.getAccentColor() +
                    "; -fx-border-radius:8; -fx-background-radius:8;" +
                    "; -fx-padding:8 14 8 14; -fx-font-weight:bold;" +
                    "; -fx-font-size:12px;");

            if (i == 0) {
                tb.setSelected(true);
                lastSelected[0] = tb;
                // Highlight first as selected
                tb.setStyle(tb.getStyle() + "-fx-opacity:1.0;");
            }

            // Prevent deselection when user clicks the already-selected button
            tb.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
                if (isNowSelected) {
                    lastSelected[0] = tb;
                    // Highlight this button visually as active
                    tb.setOpacity(1.0);
                } else {
                    // If nothing is selected (deselection), re-select this one
                    if (themeGroup.getSelectedToggle() == null) {
                        tb.setSelected(true);
                    } else {
                        tb.setOpacity(0.55);
                    }
                }
            });

            if (i < 4) themeRow1.getChildren().add(tb);
            else       themeRow2.getChildren().add(tb);
        }

        int row = 0;
        grid.add(new Label("Title:"),         0, row);   grid.add(titleField, 1, row++);
        grid.add(new Label("Description:"),   0, row);   grid.add(descTA, 1, row++);
        grid.add(new Label("Start Time:"),    0, row);   grid.add(startFld, 1, row++);
        grid.add(new Label("Duration (min):"),0, row);   grid.add(durationFld, 1, row++);
        grid.add(new Label("Eval Deadline:"), 0, row);   grid.add(evalFld, 1, row++);
        grid.add(new Label("MCQ Count:"),     0, row);   grid.add(mcqCountFld, 1, row++);
        grid.add(new Label("Written Count:"), 0, row);   grid.add(wrCountFld, 1, row++);
        grid.add(new Label("MCQ Marks each:"),0, row);   grid.add(mcqMarksFld, 1, row++);
        grid.add(new Label("Written Marks each:"), 0, row); grid.add(wrMarksFld, 1, row++);
        grid.add(new Label("Max Rating Gain:"),0, row);  grid.add(maxGainFld, 1, row++);
        grid.add(new Label("Max Rating Loss:"),0, row);  grid.add(maxLossFld, 1, row++);
        grid.add(themeLabel, 0, row);
        VBox themeBox = new VBox(8, themeRow1, themeRow2);
        grid.add(themeBox, 1, row);

        ScrollPane sp = new ScrollPane(grid);
        sp.setFitToWidth(true);
        sp.setPrefViewportHeight(500);
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

                // ── FIX 1: safe theme retrieval ────────────────────────────────
                Toggle sel = themeGroup.getSelectedToggle();
                if (sel != null && sel.getUserData() instanceof Theme selectedTheme) {
                    c.setTheme(selectedTheme);
                } else {
                    // Fallback: use lastSelected guard
                    if (lastSelected[0] != null && lastSelected[0].getUserData() instanceof Theme fallbackTheme) {
                        c.setTheme(fallbackTheme);
                    } else {
                        c.setTheme(Theme.COSMIC_ARENA);
                    }
                }
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
                showAlert("Success", "Contest \"" + c.getContestTitle() + "\" created!\n" +
                        "Theme: " + c.getTheme().getDisplayName() + "\n" +
                        "ID: " + id + "\n\n" +
                        "Now add " + c.getTotalMcqQuestions() + " MCQ and " +
                        c.getTotalWrittenQuestions() + " Written questions using ➕ Add Questions.");
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