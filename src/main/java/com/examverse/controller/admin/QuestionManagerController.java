package com.examverse.controller.admin;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import com.examverse.model.exam.Exam;
import com.examverse.service.exam.ExamService;
import com.examverse.service.exam.QuestionService;
import com.examverse.util.SceneManager;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * QuestionManagerController - Shows all exams so admin can manage questions.
 * Both the "Questions?" sidebar button and "Add Questions" quick action
 * navigate here. From here, the admin clicks "❓ Questions" on any exam card
 * to open the existing QuestionManagerDialog.
 */
public class QuestionManagerController implements Initializable {

    @FXML private VBox examsContainer;
    @FXML private Label examCountLabel;
    @FXML private TextField searchField;

    private ExamService examService;
    private QuestionService questionService;
    private List<Exam> allExams;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        examService = new ExamService();
        questionService = new QuestionService();
        loadAllExams();

        // Live search filtering
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterExams(newVal));
    }

    // ── Data Loading ──────────────────────────────────────────────────

    private void loadAllExams() {
        allExams = examService.getAllExams();
        renderExams(allExams);
        examCountLabel.setText(allExams.size() + " exam(s) found");
        System.out.println("✅ QuestionManager: loaded " + allExams.size() + " exams");
    }

    private void filterExams(String query) {
        if (query == null || query.isBlank()) {
            renderExams(allExams);
            examCountLabel.setText(allExams.size() + " exam(s) found");
            return;
        }
        String lower = query.toLowerCase();
        List<Exam> filtered = allExams.stream()
                .filter(e -> e.getExamTitle().toLowerCase().contains(lower)
                        || e.getSubject().toLowerCase().contains(lower)
                        || e.getDifficulty().toLowerCase().contains(lower))
                .toList();
        renderExams(filtered);
        examCountLabel.setText(filtered.size() + " exam(s) found");
    }

    private void renderExams(List<Exam> exams) {
        examsContainer.getChildren().clear();

        if (exams.isEmpty()) {
            Label empty = new Label("No exams found.");
            empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");
            examsContainer.getChildren().add(empty);
            return;
        }

        for (Exam exam : exams) {
            examsContainer.getChildren().add(createExamCard(exam));
        }
    }

    // ── Exam Card (same style as AdminDashboardController) ────────────

    private VBox createExamCard(Exam exam) {
        VBox card = new VBox(12);
        card.getStyleClass().add("exam-card");
        card.setPadding(new Insets(20));

        // ── Header row: subject / difficulty / status badges ──
        HBox headerRow = new HBox(15);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label subjectBadge = new Label(exam.getSubject());
        subjectBadge.getStyleClass().addAll("badge", "badge-primary");

        Label difficultyBadge = new Label(exam.getDifficulty());
        switch (exam.getDifficulty().toUpperCase()) {
            case "EASY"   -> difficultyBadge.getStyleClass().addAll("badge", "badge-success");
            case "MEDIUM" -> difficultyBadge.getStyleClass().addAll("badge", "badge-warning");
            case "HARD"   -> difficultyBadge.getStyleClass().addAll("badge", "badge-danger");
            default       -> difficultyBadge.getStyleClass().addAll("badge", "badge-primary");
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Question count indicator
        int qCount = questionService.getQuestionsByExamId(exam.getExamId()).size();
        Label qCountBadge = new Label("❓ " + qCount + "/" + exam.getTotalQuestions() + " Questions");
        qCountBadge.setStyle(qCount >= exam.getTotalQuestions()
                ? "-fx-text-fill: #22c55e; -fx-font-size: 12px; -fx-font-weight: bold;"
                : "-fx-text-fill: #f59e0b; -fx-font-size: 12px; -fx-font-weight: bold;");

        Label statusBadge = new Label(exam.getStatus());
        statusBadge.getStyleClass().addAll("badge", "badge-primary");

        headerRow.getChildren().addAll(subjectBadge, difficultyBadge, spacer, qCountBadge, statusBadge);

        // ── Title ──
        Label titleLabel = new Label(exam.getExamTitle());
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        // ── Description ──
        Label descLabel = new Label(exam.getDescription());
        descLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");
        descLabel.setWrapText(true);

        // ── Stats row ──
        HBox statsRow = new HBox(20);
        statsRow.setAlignment(Pos.CENTER_LEFT);

        Label questionsLabel = new Label("❓ " + exam.getTotalQuestions() + " Questions");
        questionsLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");

        Label durationLabel = new Label("⏱️ " + exam.getDurationMinutes() + " min");
        durationLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");

        Label marksLabel = new Label("📊 " + exam.getTotalMarks() + " Marks");
        marksLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");

        statsRow.getChildren().addAll(questionsLabel, durationLabel, marksLabel);

        // ── Action buttons (same 3 as dashboard) ──
        HBox actionRow = new HBox(10);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        Button editBtn = new Button("✏️ Edit");
        editBtn.getStyleClass().add("btn-secondary");
        editBtn.setOnAction(e -> handleEditExam(exam));

        Button questionsBtn = new Button("❓ Questions");
        questionsBtn.getStyleClass().add("btn-secondary");
        questionsBtn.setOnAction(e -> handleManageQuestions(exam));

        Button viewBtn = new Button("👁️ View");
        viewBtn.getStyleClass().add("btn-primary");
        viewBtn.setOnAction(e -> handleViewExam(exam));

        actionRow.getChildren().addAll(editBtn, questionsBtn, viewBtn);

        card.getChildren().addAll(headerRow, titleLabel, descLabel, statsRow, actionRow);
        return card;
    }

    // ── Action Handlers ───────────────────────────────────────────────

    private void handleManageQuestions(Exam exam) {
        QuestionManagerDialog dialog = new QuestionManagerDialog(exam);
        dialog.showAndWait();
        // Refresh list so question count badges update after dialog closes
        loadAllExams();
    }

    private void handleEditExam(Exam exam) {
        showAlert("Edit Exam", "Edit exam: " + exam.getExamTitle() + " - Coming soon!");
    }

    private void handleViewExam(Exam exam) {
        showAlert("View Exam", "Viewing exam: " + exam.getExamTitle());
    }

    @FXML
    private void handleBack() {
        SceneManager.switchScene("/com/examverse/fxml/dashboard/admin-dashboard.fxml");
    }

    // ── Utilities ─────────────────────────────────────────────────────

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}