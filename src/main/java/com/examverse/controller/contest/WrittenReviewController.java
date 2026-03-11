package com.examverse.controller.contest;

import com.examverse.model.exam.*;
import com.examverse.model.exam.Contest.Theme;
import com.examverse.model.user.User;
import com.examverse.service.exam.ContestService;
import com.examverse.util.SceneManager;
import com.examverse.util.SessionManager;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * WrittenReviewController — FIXED
 *
 * Bug fixes:
 *  1. "All answers reviewed" shown even when contest is LIVE / no submissions:
 *     Added a clear informational message distinguishing between:
 *     (a) Contest not yet in EVALUATION — students may not have submitted yet
 *     (b) No written answers submitted at all
 *     (c) All submitted answers are already reviewed
 *
 *  2. Shows total submitted count vs total pending so teacher knows overall progress.
 *
 *  3. Added a "Refresh" button so the teacher can reload without navigating away.
 */
public class WrittenReviewController implements Initializable {

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private VBox     rootVBox;
    @FXML private VBox     answersContainer;
    @FXML private Label    contestTitleLabel;
    @FXML private Label    pendingCountLabel;
    @FXML private Button   backBtn;
    @FXML private ScrollPane mainScroll;

    // ── State ─────────────────────────────────────────────────────────────────
    private final ContestService contestService = new ContestService();
    private User    currentUser;
    private Contest contest;

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = SessionManager.getInstance().getCurrentUser();
        contest     = SessionManager.getInstance().getCurrentContest();

        if (contest == null) return;
        applyTheme(contest.getTheme());
        contestTitleLabel.setText("✍️  Written Review — " + contest.getContestTitle());
        loadPendingAnswers();
    }

    private void applyTheme(Theme t) {
        rootVBox.setStyle("-fx-background-color:" + t.getBgColor() + ";");
    }

    // ── Load Answers ──────────────────────────────────────────────────────────
    private void loadPendingAnswers() {
        answersContainer.getChildren().clear();

        List<ContestAnswer> pendingAnswers = contestService.getPendingWrittenAnswers(contest.getContestId());
        List<ContestQuestion> questions    = contestService.getQuestionsForContest(contest.getContestId());
        Theme t = contest.getTheme();

        int totalSubmitted = contestService.getTotalWrittenAnswerCount(contest.getContestId());
        int pendingCount   = pendingAnswers.size();

        // ── Refresh button ────────────────────────────────────────────────────
        Button refreshBtn = new Button("🔄  Refresh");
        refreshBtn.setStyle("-fx-background-color:transparent;" +
                "-fx-border-color:" + t.getAccentColor() + ";" +
                "-fx-text-fill:" + t.getAccentColor() + ";" +
                "-fx-border-radius:8; -fx-background-radius:8;" +
                "-fx-padding:6 16; -fx-font-size:13px; -fx-cursor:hand;");
        refreshBtn.setOnAction(e -> loadPendingAnswers());

        // ── Status summary ────────────────────────────────────────────────────
        String statusMsg;
        String statusColor;

        if (contest.getStatus() == Contest.Status.LIVE) {
            statusMsg   = "⚠️ Contest is still LIVE. Students may still be uploading answers. " +
                    "Come back once the contest ends and moves to EVALUATION status.\n" +
                    "Submitted so far: " + totalSubmitted;
            statusColor = "#f59e0b";
        } else if (totalSubmitted == 0) {
            statusMsg   = "ℹ️ No written answers have been submitted yet for this contest.\n" +
                    "(Students may not have uploaded images, or the contest had no written questions.)";
            statusColor = "#94a3b8";
        } else if (pendingCount == 0) {
            statusMsg   = "✅ All " + totalSubmitted + " written answers have been reviewed!";
            statusColor = "#22c55e";
        } else {
            statusMsg   = pendingCount + " answer(s) pending review  |  " +
                    (totalSubmitted - pendingCount) + " / " + totalSubmitted + " reviewed";
            statusColor = "#f59e0b";
        }

        pendingCountLabel.setText(statusMsg);
        pendingCountLabel.setWrapText(true);
        pendingCountLabel.setStyle("-fx-text-fill:" + statusColor +
                "; -fx-font-size:14px; -fx-font-weight:bold;");

        HBox topRow = new HBox(12, pendingCountLabel, refreshBtn);
        topRow.setAlignment(Pos.CENTER_LEFT);
        answersContainer.getChildren().add(topRow);

        if (pendingAnswers.isEmpty()) {
            // Nothing to review right now
            return;
        }

        // ── Build answer cards ────────────────────────────────────────────────
        for (ContestAnswer a : pendingAnswers) {
            ContestQuestion q = questions.stream()
                    .filter(qq -> qq.getQuestionId() == a.getQuestionId())
                    .findFirst().orElse(null);
            if (q == null) continue;

            answersContainer.getChildren().add(buildAnswerCard(a, q, t));
        }
    }

    // ── Answer Card ───────────────────────────────────────────────────────────
    private VBox buildAnswerCard(ContestAnswer a, ContestQuestion q, Theme t) {
        VBox card = new VBox(14);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color:#1e293b; -fx-background-radius:12;" +
                "-fx-border-color:" + t.getAccentColor() + "44;" +
                "-fx-border-radius:12; -fx-border-width:1;");

        // Student info (fetched via participant)
        Label qLabel = new Label("Q: " + q.getQuestionText());
        qLabel.setStyle("-fx-text-fill:#f1f5f9; -fx-font-size:15px; -fx-font-weight:bold;");
        qLabel.setWrapText(true);

        Label studentInfo = new Label("Student ID: " + a.getStudentId() +
                "  |  Participant: #" + a.getParticipantId());
        studentInfo.setStyle("-fx-text-fill:#64748b; -fx-font-size:12px;");

        Label maxMarksLbl = new Label("Max marks: " + q.getMarks());
        maxMarksLbl.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:13px;");

        // Answer image
        VBox imageBox = new VBox(6);
        imageBox.setAlignment(Pos.CENTER);
        imageBox.setStyle("-fx-background-color:#0f172a; -fx-background-radius:8; -fx-min-height:200;");

        if (a.getImagePath() != null && !a.getImagePath().isEmpty()) {
            try {
                ImageView iv = new ImageView(new Image("file:" + a.getImagePath()));
                iv.setFitWidth(560);
                iv.setPreserveRatio(true);
                iv.setSmooth(true);
                imageBox.getChildren().add(iv);
            } catch (Exception e) {
                Label noImg = new Label("⚠️  Image not found: " + a.getImagePath());
                noImg.setStyle("-fx-text-fill:#ef4444; -fx-font-size:13px;");
                imageBox.getChildren().add(noImg);
            }
        } else {
            Label noImg = new Label("📷  No image uploaded by student.");
            noImg.setStyle("-fx-text-fill:#64748b; -fx-font-size:14px;");
            imageBox.getChildren().add(noImg);
        }

        // Marks input
        HBox marksRow = new HBox(12);
        marksRow.setAlignment(Pos.CENTER_LEFT);
        Label marksLbl = new Label("Marks (0–" + q.getMarks() + "):");
        marksLbl.setStyle("-fx-text-fill:#e2e8f0; -fx-font-size:14px;");
        Slider marksSlider = new Slider(0, q.getMarks(), 0);
        marksSlider.setShowTickLabels(true);
        marksSlider.setShowTickMarks(true);
        marksSlider.setMajorTickUnit(Math.max(1, q.getMarks()));
        marksSlider.setBlockIncrement(1);
        marksSlider.setPrefWidth(280);
        marksSlider.setStyle("-fx-control-inner-background:#1e293b;" +
                "-fx-highlight-fill:" + t.getAccentColor() + ";");

        Label marksValue = new Label("0");
        marksValue.setStyle("-fx-text-fill:" + t.getAccentColor() +
                "; -fx-font-size:18px; -fx-font-weight:bold; -fx-min-width:40;");
        marksSlider.valueProperty().addListener((obs, ov, nv) ->
                marksValue.setText(String.valueOf((int) Math.round(nv.doubleValue()))));

        marksRow.getChildren().addAll(marksLbl, marksSlider, marksValue);

        // Comment
        TextField commentField = new TextField();
        commentField.setPromptText("Optional feedback / comment for student...");
        commentField.setStyle("-fx-background-color:#0f172a; -fx-text-fill:#e2e8f0;" +
                "-fx-prompt-text-fill:#475569; -fx-background-radius:8;" +
                "-fx-padding:8 12;");

        // Submit
        Button submitBtn = new Button("✅  Save Review");
        submitBtn.setStyle("-fx-background-color:" + t.getAccentColor() + "; -fx-text-fill:#000;" +
                "-fx-font-weight:bold; -fx-background-radius:8; -fx-padding:10 24;");
        submitBtn.setOnAction(e -> {
            int marks   = (int) Math.round(marksSlider.getValue());
            String comment = commentField.getText();
            boolean ok  = contestService.reviewWrittenAnswer(
                    a.getAnswerId(), currentUser.getId(), marks, comment);
            if (ok) {
                showAlert("Saved", "Review saved! +" + marks + " marks awarded.");
                loadPendingAnswers();
            } else {
                showAlert("Error", "Failed to save review. Please try again.");
            }
        });

        // Reject
        Button rejectBtn = new Button("❌  Reject (0 marks)");
        rejectBtn.setStyle("-fx-background-color:transparent; -fx-text-fill:#ef4444;" +
                "-fx-border-color:#ef4444; -fx-border-radius:8;" +
                "-fx-background-radius:8; -fx-padding:9 20;");
        rejectBtn.setOnAction(e -> {
            boolean ok = contestService.reviewWrittenAnswer(
                    a.getAnswerId(), currentUser.getId(), 0, "Answer rejected.");
            if (ok) loadPendingAnswers();
        });

        HBox actionRow = new HBox(12, submitBtn, rejectBtn);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(qLabel, studentInfo, maxMarksLbl, imageBox, marksRow, commentField, actionRow);
        return card;
    }

    // ── Navigation ────────────────────────────────────────────────────────────
    @FXML
    private void handleBack() {
        SceneManager.switchScene("/com/examverse/fxml/contest/contest-manager.fxml");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
}