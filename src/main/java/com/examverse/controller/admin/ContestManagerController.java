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
 * ContestManagerController — v3 (Sectioned View)
 *
 * Changes in this version:
 *
 *  NEW — Sectioned contest display:
 *    Instead of one flat list, contests are now grouped into four collapsible
 *    sections rendered in a single scrollable VBox:
 *
 *      🔴 Ongoing       — status = LIVE
 *      ⏳ Upcoming      — status = UPCOMING
 *      ✍️ Pending Eval  — status = EVALUATION  (written answers await review)
 *      ✅ Finished       — status = FINISHED or CANCELLED
 *
 *    Each section has a sticky header showing the section name and the count
 *    of contests in that section. Empty sections show a quiet placeholder.
 *    Auto-refresh every 10 s keeps all sections up-to-date in real time.
 *
 *  All existing functionality (add questions, status toggle, review written,
 *  leaderboard, create contest dialog) is fully preserved.
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

        refreshTimer = new Timer("admin-contest-refresh", true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                Platform.runLater(() -> loadContests());
            }
        }, 10_000, 10_000);
    }

    // ── Load Contests — Sectioned ─────────────────────────────────────────────
    private void loadContests() {
        contestListContainer.getChildren().clear();

        List<Contest> live       = contestService.getContestsByStatus(Contest.Status.LIVE);
        List<Contest> upcoming   = contestService.getContestsByStatus(Contest.Status.UPCOMING);
        List<Contest> evaluation = contestService.getContestsByStatus(Contest.Status.EVALUATION);
        List<Contest> finished   = contestService.getFinishedContests(); // FINISHED + CANCELLED

        addSection("🔴  Ongoing", "#ef4444", live,       "No live contests right now.");
        addSection("⏳  Upcoming", "#3b82f6", upcoming,  "No upcoming contests scheduled.");
        addSection("✍️  Pending Evaluation", "#f59e0b", evaluation,
                "No contests awaiting evaluation.");
        addSection("✅  Finished", "#6b7280", finished,  "No finished contests yet.");
    }

    // ── Section Builder ───────────────────────────────────────────────────────
    /**
     * Appends a collapsible section to contestListContainer.
     *
     * @param title      Section heading text
     * @param color      Accent hex color for the header pill
     * @param contests   List of contests for this section
     * @param emptyMsg   Message shown when the list is empty
     */
    private void addSection(String title, String color,
                            List<Contest> contests, String emptyMsg) {

        // ── Section Header ──
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 16, 14, 16));
        header.setStyle("-fx-background-color:#0f172a;" +
                "-fx-border-color:transparent transparent #1e293b transparent;" +
                "-fx-border-width:0 0 2 0;" +
                "-fx-cursor:hand;");

        Label dot = new Label("●");
        dot.setStyle("-fx-text-fill:" + color + "; -fx-font-size:10px;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill:#e2e8f0; -fx-font-size:15px; -fx-font-weight:bold;");

        Label countBadge = new Label(String.valueOf(contests.size()));
        countBadge.setStyle("-fx-background-color:" + color + "33;" +
                "-fx-text-fill:" + color + ";" +
                "-fx-font-size:12px; -fx-font-weight:bold;" +
                "-fx-padding:2 10 2 10; -fx-background-radius:20;");

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        Label chevron = new Label("▾");
        chevron.setStyle("-fx-text-fill:#64748b; -fx-font-size:14px;");

        header.getChildren().addAll(dot, titleLabel, countBadge, spacer, chevron);

        // ── Section Body ──
        VBox body = new VBox(12);
        body.setPadding(new Insets(12, 0, 20, 0));

        if (contests.isEmpty()) {
            Label empty = new Label(emptyMsg);
            empty.setStyle("-fx-text-fill:#475569; -fx-font-size:13px; -fx-padding:4 16 4 16;");
            body.getChildren().add(empty);
        } else {
            for (Contest c : contests) {
                body.getChildren().add(buildContestCard(c));
            }
        }

        // ── Collapse / Expand toggle ──
        final boolean[] collapsed = {false};
        header.setOnMouseClicked(e -> {
            collapsed[0] = !collapsed[0];
            body.setVisible(!collapsed[0]);
            body.setManaged(!collapsed[0]);
            chevron.setText(collapsed[0] ? "▸" : "▾");
            chevron.setStyle("-fx-text-fill:" + (collapsed[0] ? color : "#64748b") +
                    "; -fx-font-size:14px;");
        });

        // Hover highlight on header
        header.setOnMouseEntered(e -> header.setStyle(header.getStyle()
                .replace("-fx-background-color:#0f172a;", "-fx-background-color:#1e293b;")));
        header.setOnMouseExited(e -> header.setStyle(header.getStyle()
                .replace("-fx-background-color:#1e293b;", "-fx-background-color:#0f172a;")));

        // ── Wrap in a VBox section container ──
        VBox section = new VBox(0);
        section.setStyle("-fx-background-color:#0d1117;" +
                "-fx-background-radius:12;" +
                "-fx-border-color:#1e293b;" +
                "-fx-border-radius:12; -fx-border-width:1;");
        section.setMaxWidth(Double.MAX_VALUE);
        section.getChildren().addAll(header, body);

        VBox.setMargin(section, new Insets(0, 0, 14, 0));
        contestListContainer.getChildren().add(section);
    }

    // ── Contest Card ──────────────────────────────────────────────────────────
    private VBox buildContestCard(Contest c) {
        Theme t = c.getTheme();
        VBox card = new VBox(12);
        card.setPadding(new Insets(18, 20, 18, 20));
        card.setStyle("-fx-background-color:" + t.getBgColor() +
                "; -fx-background-radius:10;" +
                "-fx-border-color:" + t.getAccentColor() + ";" +
                "-fx-border-radius:10; -fx-border-width:1.5;");

        // ── Title row ──
        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label themeLabel = new Label(t.getDisplayName());
        themeLabel.setStyle("-fx-text-fill:" + t.getAccentColor() +
                "; -fx-font-size:11px; -fx-font-weight:bold;" +
                "-fx-background-color:" + t.getAccentColor() + "22;" +
                "-fx-padding:2 8 2 8; -fx-background-radius:20;");

        Label titleLabel = new Label(c.getContestTitle());
        titleLabel.setStyle("-fx-text-fill:#ffffff; -fx-font-size:17px; -fx-font-weight:bold;");
        titleLabel.setWrapText(true);

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label statusBadge = buildStatusBadge(c.getStatus());
        titleRow.getChildren().addAll(titleLabel, sp, themeLabel, statusBadge);

        // ── Meta row ──
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        int mcqAdded     = contestService.getQuestionCountByType(c.getContestId(), QuestionType.MCQ);
        int writtenAdded = contestService.getQuestionCountByType(c.getContestId(), QuestionType.WRITTEN);
        boolean qComplete = mcqAdded >= c.getTotalMcqQuestions()
                && writtenAdded >= c.getTotalWrittenQuestions();

        Label metaLabel = new Label(
                "⏰ " + (c.getStartTime() != null ? c.getStartTime().format(fmt) : "—") +
                        "  •  ⌛ " + c.getDurationMinutes() + " min" +
                        "  •  🏆 " + c.getTotalMarks() + " marks"
        );
        metaLabel.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:12px;");

        Label qCountLabel = new Label(
                "📝 MCQ: " + mcqAdded + "/" + c.getTotalMcqQuestions() +
                        "  ✍️ Written: " + writtenAdded + "/" + c.getTotalWrittenQuestions()
        );
        qCountLabel.setStyle(qComplete
                ? "-fx-text-fill:#22c55e; -fx-font-size:12px;"
                : "-fx-text-fill:#f59e0b; -fx-font-size:12px;");

        // ── Action row ──
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(4, 0, 0, 0));

        // Add Questions — only for UPCOMING
        if (c.getStatus() == Contest.Status.UPCOMING) {
            Button addQBtn = new Button("➕ Add Questions");
            addQBtn.setStyle("-fx-background-color:" + t.getAccentColor() +
                    "; -fx-text-fill:#000; -fx-font-weight:bold;" +
                    "-fx-background-radius:8; -fx-padding:7 14 7 14;");
            addQBtn.setOnAction(e -> showAddQuestionDialog(c));
            actions.getChildren().add(addQBtn);
        }

        // Leaderboard — always visible
        Button viewLbBtn = new Button("🏆 Leaderboard");
        viewLbBtn.setStyle("-fx-background-color:transparent;" +
                "-fx-border-color:" + t.getAccentColor() + ";" +
                "-fx-text-fill:" + t.getAccentColor() + ";" +
                "-fx-background-radius:8; -fx-border-radius:8;" +
                "-fx-padding:6 12 6 12;");
        viewLbBtn.setOnAction(e -> openLeaderboard(c));

        // Status toggle — UPCOMING / LIVE / EVALUATION
        Button statusBtn = buildStatusToggleButton(c, t);

        // Review Written — only for EVALUATION
        if (c.getStatus() == Contest.Status.EVALUATION) {
            Button reviewBtn = new Button("✍️ Review Written");
            reviewBtn.setStyle("-fx-background-color:transparent;" +
                    "-fx-border-color:#fbbf24; -fx-text-fill:#fbbf24;" +
                    "-fx-background-radius:8; -fx-border-radius:8;" +
                    "-fx-padding:6 12 6 12;");
            reviewBtn.setOnAction(e -> openWrittenReview(c));
            actions.getChildren().addAll(viewLbBtn, statusBtn, reviewBtn);
        } else {
            actions.getChildren().addAll(viewLbBtn, statusBtn);
        }

        card.getChildren().addAll(titleRow, metaLabel, qCountLabel, actions);
        return card;
    }

    private Label buildStatusBadge(Contest.Status status) {
        String color = switch (status) {
            case UPCOMING   -> "#3b82f6";
            case LIVE       -> "#22c55e";
            case EVALUATION -> "#f59e0b";
            case FINISHED   -> "#6b7280";
            case CANCELLED  -> "#ef4444";
        };
        Label l = new Label(status.name());
        l.setStyle("-fx-background-color:" + color + ";" +
                "-fx-text-fill:#fff; -fx-font-size:11px; -fx-font-weight:bold;" +
                "-fx-padding:3 10 3 10; -fx-background-radius:20;");
        return l;
    }

    private Button buildStatusToggleButton(Contest c, Theme t) {
        String label = switch (c.getStatus()) {
            case UPCOMING   -> "🚀 Launch";
            case LIVE       -> "⏹ End Contest";
            case EVALUATION -> "✅ Mark Finished";
            default         -> "—";
        };
        Button b = new Button(label);
        b.setStyle("-fx-background-color:#1e293b; -fx-text-fill:#e2e8f0;" +
                "-fx-background-radius:8; -fx-padding:6 12 6 12;");
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
                        contestService.distributeRatingChanges(c.getContestId());
                    }
                    Platform.runLater(this::loadContests);
                }
            }
        });
    }

    // ── Add Question Dialog ───────────────────────────────────────────────────
    private void showAddQuestionDialog(Contest contest) {
        int mcqAdded     = contestService.getQuestionCountByType(contest.getContestId(), QuestionType.MCQ);
        int writtenAdded = contestService.getQuestionCountByType(contest.getContestId(), QuestionType.WRITTEN);
        int mcqLimit     = contest.getTotalMcqQuestions();
        int writtenLimit = contest.getTotalWrittenQuestions();

        boolean mcqFull     = mcqAdded >= mcqLimit;
        boolean writtenFull = writtenAdded >= writtenLimit;

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

        Label typeLabel = new Label("Question Type:");
        typeLabel.setStyle("-fx-font-weight:bold;");
        ToggleGroup typeGroup = new ToggleGroup();

        RadioButton mcqRadio     = new RadioButton(
                "MCQ (Auto-graded)  [" + mcqAdded + "/" + mcqLimit + " added]");
        RadioButton writtenRadio = new RadioButton(
                "Written (Image Upload)  [" + writtenAdded + "/" + writtenLimit + " added]");
        mcqRadio.setToggleGroup(typeGroup);
        writtenRadio.setToggleGroup(typeGroup);
        mcqRadio.setDisable(mcqFull);
        writtenRadio.setDisable(writtenFull);
        if (!mcqFull) mcqRadio.setSelected(true);
        else          writtenRadio.setSelected(true);

        HBox typeRow = new HBox(20, mcqRadio, writtenRadio);

        TextArea questionTA = new TextArea();
        questionTA.setPromptText("Question text...");
        questionTA.setPrefRowCount(3);

        TextField marksField = new TextField();
        marksField.setPromptText("Marks");

        Runnable updateMarks = () -> {
            if (mcqRadio.isSelected()) marksField.setText(String.valueOf(contest.getMcqMarksEach()));
            else                       marksField.setText(String.valueOf(contest.getWrittenMarksEach()));
        };
        mcqRadio.selectedProperty().addListener((obs, ov, nv) -> { if (nv) updateMarks.run(); });
        writtenRadio.selectedProperty().addListener((obs, ov, nv) -> { if (nv) updateMarks.run(); });
        updateMarks.run();

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
                new Label("Option A:"), optA, new Label("Option B:"), optB,
                new Label("Option C:"), optC, new Label("Option D:"), optD,
                new Label("Correct Answer:"), correctAns,
                new Label("Explanation (optional):"), explanationTA);

        writtenRadio.selectedProperty().addListener((obs, ov, nv) -> mcqSection.setVisible(!nv));
        mcqSection.setVisible(!writtenRadio.isSelected());

        form.getChildren().addAll(typeLabel, typeRow,
                new Label("Question:"), questionTA,
                new Label("Marks:"), marksField, mcqSection);

        ScrollPane sp = new ScrollPane(form);
        sp.setFitToWidth(true);
        sp.setPrefViewportHeight(500);
        dialog.getDialogPane().setContent(sp);

        dialog.setResultConverter(bt -> {
            if (bt != addBtn) return null;
            boolean isMcq = mcqRadio.isSelected();
            QuestionType selectedType = isMcq ? QuestionType.MCQ : QuestionType.WRITTEN;

            int currentCount = contestService.getQuestionCountByType(
                    contest.getContestId(), selectedType);
            int limit = isMcq ? mcqLimit : writtenLimit;
            if (currentCount >= limit) {
                showAlert("Limit Reached", "Cannot add more " + selectedType.name() +
                        " questions. Limit is " + limit + ".");
                return null;
            }

            ContestQuestion q = new ContestQuestion();
            q.setContestId(contest.getContestId());
            q.setQuestionText(questionTA.getText().trim());
            q.setType(selectedType);
            try { q.setMarks(Integer.parseInt(marksField.getText())); }
            catch (NumberFormatException e) {
                q.setMarks(isMcq ? contest.getMcqMarksEach() : contest.getWrittenMarksEach());
            }
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
            if (q.getQuestionText().isEmpty()) { showAlert("Error", "Question text cannot be empty."); return; }
            int id = contestService.addQuestion(q);
            if (id > 0) {
                int newMcq     = contestService.getQuestionCountByType(contest.getContestId(), QuestionType.MCQ);
                int newWritten = contestService.getQuestionCountByType(contest.getContestId(), QuestionType.WRITTEN);
                showAlert("Question Added ✅",
                        q.getType().name() + " question added!\n\n" +
                                "MCQ: " + newMcq + "/" + mcqLimit +
                                "   Written: " + newWritten + "/" + writtenLimit);
                loadContests();
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
        stopTimer();
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
        grid.setHgap(15); grid.setVgap(15);
        grid.setPadding(new Insets(20));

        TextField titleField  = new TextField(); titleField.setPromptText("Contest Title");
        TextArea  descTA      = new TextArea();  descTA.setPromptText("Description"); descTA.setPrefRowCount(2);
        TextField durationFld = new TextField("60");
        TextField mcqCountFld = new TextField("10");
        TextField wrCountFld  = new TextField("2");
        TextField mcqMarksFld = new TextField("5");
        TextField wrMarksFld  = new TextField("10");
        TextField maxGainFld  = new TextField("100");
        TextField maxLossFld  = new TextField("50");
        TextField startFld    = new TextField(
                LocalDateTime.now().plusHours(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        startFld.setPromptText("Start (yyyy-MM-dd HH:mm)");
        TextField evalFld = new TextField(
                LocalDateTime.now().plusHours(3).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        evalFld.setPromptText("Eval deadline (yyyy-MM-dd HH:mm)");

        // Theme picker
        ToggleGroup themeGroup = new ToggleGroup();
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
                    "; -fx-padding:6 12 6 12; -fx-font-weight:bold;");
            if (i == 0) tb.setSelected(true);
            tb.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (!isSelected && themeGroup.getSelectedToggle() == null) tb.setSelected(true);
            });
            if (i < 4) themeRow1.getChildren().add(tb);
            else        themeRow2.getChildren().add(tb);
        }

        int row = 0;
        grid.add(new Label("Title:"),            0, row); grid.add(titleField,  1, row++);
        grid.add(new Label("Description:"),      0, row); grid.add(descTA,      1, row++);
        grid.add(new Label("Start Time:"),       0, row); grid.add(startFld,    1, row++);
        grid.add(new Label("Duration (min):"),   0, row); grid.add(durationFld, 1, row++);
        grid.add(new Label("Eval Deadline:"),    0, row); grid.add(evalFld,     1, row++);
        grid.add(new Label("MCQ Count:"),        0, row); grid.add(mcqCountFld, 1, row++);
        grid.add(new Label("Written Count:"),    0, row); grid.add(wrCountFld,  1, row++);
        grid.add(new Label("MCQ Marks each:"),   0, row); grid.add(mcqMarksFld, 1, row++);
        grid.add(new Label("Written Marks:"),    0, row); grid.add(wrMarksFld,  1, row++);
        grid.add(new Label("Max Rating Gain:"),  0, row); grid.add(maxGainFld,  1, row++);
        grid.add(new Label("Max Rating Loss:"),  0, row); grid.add(maxLossFld,  1, row++);
        grid.add(new Label("Theme:"),            0, row);
        grid.add(new VBox(6, themeRow1, themeRow2), 1, row);

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
                        "\"" + c.getContestTitle() + "\" created successfully! ID: " + id + "\n\n" +
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
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
}