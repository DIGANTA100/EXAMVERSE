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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

/**
 * ContestManagerController — v4 (Styled Dialogs)
 *
 * ─────────────────────────────────────────────────────────────────
 * What changed from v3:
 *
 *   ALL default JavaFX Dialog / Alert popups have been replaced
 *   with fully-styled dark modal windows provided by
 *   {@link ContestDialogHelper}.
 *
 *   This means:
 *     • handleCreateContest()  → ContestDialogHelper.showCreateContestDialog()
 *     • showAddQuestionDialog() → ContestDialogHelper.showAddQuestionDialog()
 *     • handleStatusToggle()   → ContestDialogHelper.showConfirmDialog()
 *     • showAlert()            → ContestDialogHelper.showInfoDialog()
 *
 *   Everything else (sectioned list, auto-refresh, card builder,
 *   status badge, leaderboard / written-review navigation) is
 *   identical to v3.
 * ─────────────────────────────────────────────────────────────────
 */
public class ContestManagerController implements Initializable {

    // ── FXML References ────────────────────────────────────────────────────────
    @FXML private VBox   contestListContainer;
    @FXML private Label  pageTitle;
    @FXML private Button backBtn, createContestBtn;
    @FXML private ScrollPane mainScroll;
    @FXML private VBox   mainContent;

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
        List<Contest> finished   = contestService.getFinishedContests();

        addSection("🔴  Ongoing",           "#ef4444", live,       "No live contests right now.");
        addSection("⏳  Upcoming",           "#3b82f6", upcoming,   "No upcoming contests scheduled.");
        addSection("✍️  Pending Evaluation", "#f59e0b", evaluation, "No contests awaiting evaluation.");
        addSection("✅  Finished",           "#6b7280", finished,   "No finished contests yet.");
    }

    // ── Section Builder ───────────────────────────────────────────────────────
    private void addSection(String title, String color,
                            List<Contest> contests, String emptyMsg) {

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 16, 14, 16));
        header.setStyle("-fx-background-color:#0f172a;" +
                "-fx-border-color:transparent transparent #1e293b transparent;" +
                "-fx-border-width:0 0 2 0; -fx-cursor:hand;");

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

        final boolean[] collapsed = {false};
        header.setOnMouseClicked(e -> {
            collapsed[0] = !collapsed[0];
            body.setVisible(!collapsed[0]);
            body.setManaged(!collapsed[0]);
            chevron.setText(collapsed[0] ? "▸" : "▾");
            chevron.setStyle("-fx-text-fill:" + (collapsed[0] ? color : "#64748b") +
                    "; -fx-font-size:14px;");
        });

        header.setOnMouseEntered(e -> header.setStyle(header.getStyle()
                .replace("-fx-background-color:#0f172a;", "-fx-background-color:#1e293b;")));
        header.setOnMouseExited(e -> header.setStyle(header.getStyle()
                .replace("-fx-background-color:#1e293b;", "-fx-background-color:#0f172a;")));

        VBox section = new VBox(0);
        section.setStyle("-fx-background-color:#0d1117;" +
                "-fx-background-radius:12; -fx-border-color:#1e293b;" +
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

        // Title row
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

        // Meta row
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        int mcqAdded     = contestService.getQuestionCountByType(c.getContestId(), QuestionType.MCQ);
        int writtenAdded = contestService.getQuestionCountByType(c.getContestId(), QuestionType.WRITTEN);
        boolean qComplete = mcqAdded >= c.getTotalMcqQuestions()
                && writtenAdded >= c.getTotalWrittenQuestions();

        Label metaLabel = new Label(
                "⏰ " + (c.getStartTime() != null ? c.getStartTime().format(fmt) : "—") +
                        "  •  ⌛ " + c.getDurationMinutes() + " min" +
                        "  •  🏆 " + c.getTotalMarks() + " marks");
        metaLabel.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:12px;");

        Label qCountLabel = new Label(
                "📝 MCQ: " + mcqAdded + "/" + c.getTotalMcqQuestions() +
                        "  ✍️ Written: " + writtenAdded + "/" + c.getTotalWrittenQuestions());
        qCountLabel.setStyle(qComplete
                ? "-fx-text-fill:#22c55e; -fx-font-size:12px;"
                : "-fx-text-fill:#f59e0b; -fx-font-size:12px;");

        // Action row
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(4, 0, 0, 0));

        if (c.getStatus() == Contest.Status.UPCOMING) {
            Button addQBtn = new Button("➕ Add Questions");
            addQBtn.setStyle("-fx-background-color:" + t.getAccentColor() +
                    "; -fx-text-fill:#000; -fx-font-weight:bold;" +
                    "-fx-background-radius:8; -fx-padding:7 14;");
            addQBtn.setOnAction(e -> showAddQuestionDialog(c));
            actions.getChildren().add(addQBtn);
        }

        Button viewLbBtn = new Button("🏆 Leaderboard");
        viewLbBtn.setStyle("-fx-background-color:transparent;" +
                "-fx-border-color:" + t.getAccentColor() + ";" +
                "-fx-text-fill:" + t.getAccentColor() + ";" +
                "-fx-background-radius:8; -fx-border-radius:8; -fx-padding:6 12;");
        viewLbBtn.setOnAction(e -> openLeaderboard(c));

        Button statusBtn = buildStatusToggleButton(c);

        if (c.getStatus() == Contest.Status.EVALUATION) {
            Button reviewBtn = new Button("✍️ Review Written");
            reviewBtn.setStyle("-fx-background-color:transparent;" +
                    "-fx-border-color:#fbbf24; -fx-text-fill:#fbbf24;" +
                    "-fx-background-radius:8; -fx-border-radius:8; -fx-padding:6 12;");
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

    private Button buildStatusToggleButton(Contest c) {
        String label = switch (c.getStatus()) {
            case UPCOMING   -> "🚀 Launch";
            case LIVE       -> "⏹ End Contest";
            case EVALUATION -> "✅ Mark Finished";
            default         -> "—";
        };
        Button b = new Button(label);
        b.setStyle("-fx-background-color:#1e293b; -fx-text-fill:#e2e8f0;" +
                "-fx-background-radius:8; -fx-padding:6 12;");
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
            case LIVE       -> "Launch this contest?\n\nStudents will be able to join immediately.";
            case EVALUATION -> "End the contest?\n\nNo more answers will be accepted.";
            case FINISHED   -> "Finalize contest?\n\nRating points will be distributed and the contest will be permanently closed.";
            default         -> "";
        };

        String accentHex = switch (next) {
            case LIVE       -> "#22c55e";
            case EVALUATION -> "#f59e0b";
            case FINISHED   -> "#6b7280";
            default         -> "#7c3aed";
        };

        // ✅ Uses ContestDialogHelper instead of JavaFX Alert
        ContestDialogHelper.showConfirmDialog(
                "Confirm Status Change", msg, accentHex,
                () -> {
                    boolean ok = contestService.updateContestStatus(c.getContestId(), next);
                    if (ok) {
                        if (next == Contest.Status.FINISHED) {
                            contestService.distributeRatingChanges(c.getContestId());
                        }
                        Platform.runLater(this::loadContests);
                    }
                });
    }

    // ── Add Question Dialog ───────────────────────────────────────────────────
    private void showAddQuestionDialog(Contest contest) {
        int mcqAdded     = contestService.getQuestionCountByType(contest.getContestId(), QuestionType.MCQ);
        int writtenAdded = contestService.getQuestionCountByType(contest.getContestId(), QuestionType.WRITTEN);

        // ✅ Uses ContestDialogHelper instead of JavaFX Dialog
        ContestDialogHelper.showAddQuestionDialog(
                contest, mcqAdded, writtenAdded,
                q -> {
                    if (q.getQuestionText().isEmpty()) {
                        ContestDialogHelper.showInfoDialog(
                                "Error", "Question text cannot be empty.", "#ef4444");
                        return;
                    }
                    int id = contestService.addQuestion(q);
                    if (id > 0) {
                        int newMcq     = contestService.getQuestionCountByType(
                                contest.getContestId(), QuestionType.MCQ);
                        int newWritten = contestService.getQuestionCountByType(
                                contest.getContestId(), QuestionType.WRITTEN);
                        ContestDialogHelper.showInfoDialog(
                                "Question Added ✅",
                                q.getType().name() + " question added!\n\n" +
                                        "MCQ: " + newMcq + "/" + contest.getTotalMcqQuestions() +
                                        "   Written: " + newWritten + "/" + contest.getTotalWrittenQuestions(),
                                contest.getTheme().getAccentColor());
                        loadContests();
                    } else {
                        ContestDialogHelper.showInfoDialog(
                                "Error", "Failed to add question. Check console.", "#ef4444");
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

    // ── Create Contest ────────────────────────────────────────────────────────
    @FXML
    private void handleCreateContest() {
        // ✅ Uses ContestDialogHelper instead of JavaFX Dialog
        ContestDialogHelper.showCreateContestDialog(c -> {
            c.setCreatedBy(currentUser.getId());
            c.setTotalMarks(c.computeTotalMarks());
            int id = contestService.createContest(c);
            if (id > 0) {
                ContestDialogHelper.showInfoDialog(
                        "Contest Created ✅",
                        "\"" + c.getContestTitle() + "\" has been created!\n" +
                                "ID: " + id + "\n\n" +
                                "Now add " + c.getTotalMcqQuestions() + " MCQ and " +
                                c.getTotalWrittenQuestions() + " written questions.",
                        c.getTheme().getAccentColor());
                loadContests();
            } else {
                ContestDialogHelper.showInfoDialog(
                        "Error", "Failed to create contest. Check console for details.", "#ef4444");
            }
        });
    }
}