package com.examverse.controller.admin;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import com.examverse.model.exam.Exam;
import com.examverse.model.user.User;
import com.examverse.service.exam.ExamService;
import com.examverse.service.exam.QuestionService;
import com.examverse.util.SceneManager;
import com.examverse.util.SessionManager;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * AllExamsController - Displays and manages all exams (not contests)
 * Features: View, Edit, Delete, Filter, Search, Manage Questions
 */
public class AllExamsController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> subjectFilter;
    @FXML private ComboBox<String> difficultyFilter;
    @FXML private VBox examsContainer;
    @FXML private Label totalExamsLabel;

    private User currentUser;
    private ExamService examService;
    private QuestionService questionService;
    private List<Exam> allExams;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = SessionManager.getInstance().getCurrentUser();
        examService = new ExamService();
        questionService = new QuestionService();

        if (currentUser == null || !currentUser.isAdmin()) {
            showError("Unauthorized access!");
            return;
        }

        // Setup filters
        setupFilters();

        // Load all exams
        loadAllExams();

        System.out.println("✅ All Exams page loaded");
    }

    /**
     * Setup filter dropdowns
     */
    private void setupFilters() {
        // Status filter
        statusFilter.getItems().addAll("All Status", "ACTIVE", "INACTIVE", "ARCHIVED");
        statusFilter.setValue("All Status");
        statusFilter.setOnAction(e -> applyFilters());

        // Difficulty filter
        difficultyFilter.getItems().addAll("All Difficulty", "EASY", "MEDIUM", "HARD");
        difficultyFilter.setValue("All Difficulty");
        difficultyFilter.setOnAction(e -> applyFilters());

        // Search field listener
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    /**
     * Load all exams from database
     */
    private void loadAllExams() {
        allExams = examService.getAllExams();

        // Get unique subjects
        List<String> subjects = allExams.stream()
                .map(Exam::getSubject)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        subjectFilter.getItems().clear();
        subjectFilter.getItems().add("All Subjects");
        subjectFilter.getItems().addAll(subjects);
        subjectFilter.setValue("All Subjects");
        subjectFilter.setOnAction(e -> applyFilters());

        // Display all exams
        applyFilters();
    }

    /**
     * Apply filters and search
     */
    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase();
        String statusValue = statusFilter.getValue();
        String subjectValue = subjectFilter.getValue();
        String difficultyValue = difficultyFilter.getValue();

        List<Exam> filteredExams = allExams.stream()
                .filter(exam -> {
                    // Search filter
                    boolean matchesSearch = searchText.isEmpty() ||
                            exam.getExamTitle().toLowerCase().contains(searchText) ||
                            exam.getSubject().toLowerCase().contains(searchText) ||
                            exam.getDescription().toLowerCase().contains(searchText);

                    // Status filter
                    boolean matchesStatus = statusValue.equals("All Status") ||
                            exam.getStatus().equalsIgnoreCase(statusValue);

                    // Subject filter
                    boolean matchesSubject = subjectValue.equals("All Subjects") ||
                            exam.getSubject().equalsIgnoreCase(subjectValue);

                    // Difficulty filter
                    boolean matchesDifficulty = difficultyValue.equals("All Difficulty") ||
                            exam.getDifficulty().equalsIgnoreCase(difficultyValue);

                    return matchesSearch && matchesStatus && matchesSubject && matchesDifficulty;
                })
                .collect(Collectors.toList());

        displayExams(filteredExams);
        totalExamsLabel.setText("Total: " + filteredExams.size() + " exam(s)");
    }

    /**
     * Display exams in the container
     */
    private void displayExams(List<Exam> exams) {
        examsContainer.getChildren().clear();

        if (exams.isEmpty()) {
            Label noExamsLabel = new Label("No exams found matching your filters.");
            noExamsLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 16px;");
            noExamsLabel.setPadding(new Insets(50));
            examsContainer.getChildren().add(noExamsLabel);
            return;
        }

        for (Exam exam : exams) {
            VBox examCard = createExamCard(exam);
            examsContainer.getChildren().add(examCard);
        }
    }

    /**
     * Create exam card
     */
    private VBox createExamCard(Exam exam) {
        VBox card = new VBox(15);
        card.getStyleClass().add("exam-card");
        card.setPadding(new Insets(25));

        // Header row
        HBox headerRow = new HBox(15);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        // Title and subject
        VBox titleBox = new VBox(5);
        Label titleLabel = new Label(exam.getExamTitle());
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");

        Label subjectLabel = new Label(exam.getSubject());
        subjectLabel.getStyleClass().addAll("badge", "badge-primary");

        titleBox.getChildren().addAll(titleLabel, subjectLabel);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        // Badges
        HBox badgesBox = new HBox(10);

        // Difficulty badge
        Label difficultyBadge = new Label(exam.getDifficulty());
        switch (exam.getDifficulty().toUpperCase()) {
            case "EASY":
                difficultyBadge.getStyleClass().addAll("badge", "badge-success");
                break;
            case "MEDIUM":
                difficultyBadge.getStyleClass().addAll("badge", "badge-warning");
                break;
            case "HARD":
                difficultyBadge.getStyleClass().addAll("badge", "badge-danger");
                break;
        }

        // Status badge
        Label statusBadge = new Label(exam.getStatus());
        if (exam.isActive()) {
            statusBadge.getStyleClass().addAll("badge", "badge-success");
        } else {
            statusBadge.getStyleClass().addAll("badge", "badge-secondary");
        }

        badgesBox.getChildren().addAll(difficultyBadge, statusBadge);

        headerRow.getChildren().addAll(titleBox, badgesBox);

        // Description
        Label descLabel = new Label(exam.getDescription());
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 14px;");

        // Stats row
        HBox statsRow = new HBox(30);
        statsRow.setAlignment(Pos.CENTER_LEFT);

        addStatLabel(statsRow, "❓", exam.getTotalQuestions() + " Questions");
        addStatLabel(statsRow, "📊", exam.getTotalMarks() + " Marks");
        addStatLabel(statsRow, "⏱️", exam.getFormattedDuration());
        addStatLabel(statsRow, "✅", exam.getPassingMarks() + " Passing");

        // Get question count
        int questionCount = questionService.getQuestionCountForExam(exam.getExamId());
        Label questionCountLabel = new Label("📝 " + questionCount + "/" + exam.getTotalQuestions() + " questions added");
        questionCountLabel.setStyle("-fx-text-fill: " + (questionCount >= exam.getTotalQuestions() ? "#22c55e" : "#f59e0b") + "; -fx-font-size: 12px; -fx-font-weight: bold;");
        statsRow.getChildren().add(questionCountLabel);

        // Created date
        if (exam.getCreatedAt() != null) {
            Label dateLabel = new Label("📅 Created: " + exam.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
            dateLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
            statsRow.getChildren().add(dateLabel);
        }

        // Action buttons
        HBox actionRow = new HBox(10);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        Button editBtn = new Button("✏️ Edit");
        editBtn.getStyleClass().add("btn-secondary");
        editBtn.setOnAction(e -> handleEditExam(exam));

        Button questionsBtn = new Button("❓ Questions");
        questionsBtn.getStyleClass().add("btn-primary");
        questionsBtn.setOnAction(e -> handleManageQuestions(exam));

        Button statsBtn = new Button("📊 Stats");
        statsBtn.getStyleClass().add("btn-secondary");
        statsBtn.setOnAction(e -> handleViewStats(exam));

        Button toggleStatusBtn = new Button(exam.isActive() ? "⏸️ Deactivate" : "▶️ Activate");
        toggleStatusBtn.getStyleClass().add("btn-secondary");
        toggleStatusBtn.setOnAction(e -> handleToggleStatus(exam));

        Button deleteBtn = new Button("🗑️ Delete");
        deleteBtn.getStyleClass().add("btn-danger");
        deleteBtn.setOnAction(e -> handleDeleteExam(exam));

        actionRow.getChildren().addAll( questionsBtn, statsBtn, toggleStatusBtn, deleteBtn);

        card.getChildren().addAll(headerRow, descLabel, statsRow, actionRow);

        return card;
    }

    /**
     * Add stat label to HBox
     */
    private void addStatLabel(HBox container, String icon, String text) {
        Label label = new Label(icon + " " + text);
        label.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
        container.getChildren().add(label);
    }

    /**
     * Handle edit exam
     */
    private void handleEditExam(Exam exam) {
        // Create edit dialog
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Exam");
        dialog.setHeaderText("Edit: " + exam.getExamTitle());

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogPane.setPrefSize(600, 500);

        // Form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField titleField = new TextField(exam.getExamTitle());
        TextField subjectField = new TextField(exam.getSubject());
        TextArea descField = new TextArea(exam.getDescription());
        descField.setPrefRowCount(3);

        ComboBox<String> difficultyBox = new ComboBox<>();
        difficultyBox.getItems().addAll("EASY", "MEDIUM", "HARD");
        difficultyBox.setValue(exam.getDifficulty());

        TextField questionsField = new TextField(String.valueOf(exam.getTotalQuestions()));
        TextField marksField = new TextField(String.valueOf(exam.getTotalMarks()));
        TextField durationField = new TextField(String.valueOf(exam.getDurationMinutes()));
        TextField passingField = new TextField(String.valueOf(exam.getPassingMarks()));

        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Subject:"), 0, 1);
        grid.add(subjectField, 1, 1);
        grid.add(new Label("Description:"), 0, 2);
        grid.add(descField, 1, 2);
        grid.add(new Label("Difficulty:"), 0, 3);
        grid.add(difficultyBox, 1, 3);
        grid.add(new Label("Total Questions:"), 0, 4);
        grid.add(questionsField, 1, 4);
        grid.add(new Label("Total Marks:"), 0, 5);
        grid.add(marksField, 1, 5);
        grid.add(new Label("Duration (min):"), 0, 6);
        grid.add(durationField, 1, 6);
        grid.add(new Label("Passing Marks:"), 0, 7);
        grid.add(passingField, 1, 7);

        dialogPane.setContent(grid);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    exam.setExamTitle(titleField.getText());
                    exam.setSubject(subjectField.getText());
                    exam.setDescription(descField.getText());
                    exam.setDifficulty(difficultyBox.getValue());
                    exam.setTotalQuestions(Integer.parseInt(questionsField.getText()));
                    exam.setTotalMarks(Integer.parseInt(marksField.getText()));
                    exam.setDurationMinutes(Integer.parseInt(durationField.getText()));
                    exam.setPassingMarks(Integer.parseInt(passingField.getText()));

                    boolean success = examService.updateExam(exam);
                    if (success) {
                        showSuccess("Exam updated successfully!");
                        loadAllExams();
                    } else {
                        showError("Failed to update exam");
                    }
                } catch (NumberFormatException e) {
                    showError("Please enter valid numbers");
                }
            }
        });
    }

    /**
     * Handle manage questions
     */
    private void handleManageQuestions(Exam exam) {
        QuestionManagerDialog dialog = new QuestionManagerDialog(exam);
        dialog.showAndWait();
        applyFilters(); // Refresh to show updated question count
    }

    /**
     * Handle view stats
     */
    private void handleViewStats(Exam exam) {
        SessionManager.getInstance().setAttribute("selectedExamId", exam.getExamId());
        SceneManager.switchScene("/com/examverse/fxml/dashboard/exam-reports.fxml");
    }

    /**
     * Handle toggle status
     */
    private void handleToggleStatus(Exam exam) {
        String newStatus = exam.isActive() ? "INACTIVE" : "ACTIVE";
        exam.setStatus(newStatus);

        boolean success = examService.updateExam(exam);
        if (success) {
            showSuccess("Exam status changed to: " + newStatus);
            applyFilters();
        } else {
            showError("Failed to change status");
        }
    }

    /**
     * Handle delete exam
     */
    private void handleDeleteExam(Exam exam) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Exam");
        confirm.setHeaderText("Delete " + exam.getExamTitle() + "?");
        confirm.setContentText("This will permanently delete the exam and all its questions.\nThis action cannot be undone!");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean success = examService.deleteExam(exam.getExamId());
                if (success) {
                    showSuccess("Exam deleted successfully");
                    loadAllExams();
                } else {
                    showError("Failed to delete exam");
                }
            }
        });
    }

    /**
     * Handle back to dashboard
     */
    @FXML
    private void handleBack() {
        SceneManager.switchScene("/com/examverse/fxml/dashboard/admin-dashboard.fxml");
    }

    /**
     * Handle create new exam
     */
    @FXML
    private void handleCreateExam() {
        SceneManager.switchScene("/com/examverse/fxml/dashboard/admin-dashboard.fxml");
        // The create exam dialog will be triggered from dashboard
    }

    /**
     * Show success message
     */
    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}