package com.examverse.controller.admin;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import com.examverse.model.user.User;
import com.examverse.service.exam.ReportService;
import com.examverse.util.SceneManager;
import com.examverse.util.SessionManager;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * ExamReportsController - Displays exam statistics and reports
 * Shows overall statistics and per-exam detailed reports
 */
public class ExamReportsController implements Initializable {

    @FXML private Label totalExamsLabel;
    @FXML private Label totalStudentsLabel;
    @FXML private Label totalAttemptsLabel;
    @FXML private Label avgScoreLabel;
    @FXML private Label passRateLabel;
    @FXML private ProgressBar passRateProgressBar;

    @FXML private VBox examsReportContainer;
    @FXML private Button backButton;

    private User currentUser;
    private ReportService reportService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = SessionManager.getInstance().getCurrentUser();
        reportService = new ReportService();

        if (currentUser == null || !currentUser.isAdmin()) {
            showError("Unauthorized access!");
            return;
        }

        // Load data
        loadOverallStatistics();
        loadExamReports();

        System.out.println("✅ Reports page loaded for: " + currentUser.getUsername());
    }

    /**
     * Load overall statistics
     */
    private void loadOverallStatistics() {
        try {
            Map<String, Object> stats = reportService.getOverallStatistics();

            int totalExams = (int) stats.getOrDefault("totalExams", 0);
            int totalStudents = (int) stats.getOrDefault("totalStudents", 0);
            int totalAttempts = (int) stats.getOrDefault("totalAttempts", 0);
            double avgScore = (double) stats.getOrDefault("avgScore", 0.0);
            double passRate = (double) stats.getOrDefault("passRate", 0.0);

            totalExamsLabel.setText(String.valueOf(totalExams));
            totalStudentsLabel.setText(String.valueOf(totalStudents));
            totalAttemptsLabel.setText(String.valueOf(totalAttempts));
            avgScoreLabel.setText(String.format("%.1f%%", avgScore));
            passRateLabel.setText(String.format("%.1f%%", passRate));
            passRateProgressBar.setProgress(passRate / 100.0);

            System.out.println("✅ Overall statistics loaded");

        } catch (Exception e) {
            System.err.println("❌ Error loading overall statistics");
            e.printStackTrace();
        }
    }

    /**
     * Load exam reports
     */
    private void loadExamReports() {
        examsReportContainer.getChildren().clear();

        try {
            List<Map<String, Object>> examStats = reportService.getExamStatistics();

            if (examStats.isEmpty()) {
                Label noDataLabel = new Label("No exam data available yet.");
                noDataLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 16px;");
                examsReportContainer.getChildren().add(noDataLabel);
                return;
            }

            for (Map<String, Object> examStat : examStats) {
                VBox examCard = createExamReportCard(examStat);
                examsReportContainer.getChildren().add(examCard);
            }

            System.out.println("✅ Loaded " + examStats.size() + " exam reports");

        } catch (Exception e) {
            System.err.println("❌ Error loading exam reports");
            e.printStackTrace();
        }
    }

    /**
     * Create exam report card
     */
    private VBox createExamReportCard(Map<String, Object> examStat) {
        VBox card = new VBox(15);
        card.getStyleClass().add("exam-card");
        card.setPadding(new Insets(25));

        // Header row
        HBox headerRow = new HBox(15);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        // Exam title
        VBox titleBox = new VBox(5);
        Label titleLabel = new Label((String) examStat.get("examTitle"));
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");

        Label subjectLabel = new Label((String) examStat.get("subject"));
        subjectLabel.getStyleClass().addAll("badge", "badge-primary");

        titleBox.getChildren().addAll(titleLabel, subjectLabel);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        // Difficulty badge
        String difficulty = (String) examStat.get("difficulty");
        Label difficultyBadge = new Label(difficulty);
        switch (difficulty.toUpperCase()) {
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

        headerRow.getChildren().addAll(titleBox, difficultyBadge);

        // Statistics grid
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(30);
        statsGrid.setVgap(15);
        statsGrid.setPadding(new Insets(15, 0, 0, 0));

        int totalAttempts = (int) examStat.getOrDefault("totalAttempts", 0);
        int uniqueStudents = (int) examStat.getOrDefault("uniqueStudents", 0);
        double avgScore = (double) examStat.getOrDefault("avgScore", 0.0);
        int totalMarks = (int) examStat.get("totalMarks");
        double passRate = (double) examStat.getOrDefault("passRate", 0.0);
        int highestScore = (int) examStat.getOrDefault("highestScore", 0);
        int lowestScore = (int) examStat.getOrDefault("lowestScore", 0);
        double avgTime = (double) examStat.getOrDefault("avgTime", 0.0);

        // Row 1
        addStatLabel(statsGrid, "📊 Attempts:", String.valueOf(totalAttempts), 0, 0);
        addStatLabel(statsGrid, "👥 Students:", String.valueOf(uniqueStudents), 1, 0);
        addStatLabel(statsGrid, "📈 Avg Score:", String.format("%.1f/%d", avgScore, totalMarks), 2, 0);

        // Row 2
        addStatLabel(statsGrid, "✅ Pass Rate:", String.format("%.1f%%", passRate), 0, 1);
        addStatLabel(statsGrid, "🏆 Highest:", String.valueOf(highestScore), 1, 1);
        addStatLabel(statsGrid, "📉 Lowest:", String.valueOf(lowestScore), 2, 1);

        // Row 3
        addStatLabel(statsGrid, "⏱️ Avg Time:", String.format("%.0f min", avgTime), 0, 2);

        // Pass rate progress bar
        VBox passRateBox = new VBox(5);
        Label passRateLabel = new Label(String.format("Pass Rate: %.1f%%", passRate));
        passRateLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");

        ProgressBar progressBar = new ProgressBar(passRate / 100.0);
        progressBar.setPrefWidth(300);
        progressBar.setStyle("-fx-accent: " + getPassRateColor(passRate) + ";");

        passRateBox.getChildren().addAll(passRateLabel, progressBar);

        // Action buttons
        HBox actionRow = new HBox(10);
        actionRow.setAlignment(Pos.CENTER_LEFT);
        actionRow.setPadding(new Insets(10, 0, 0, 0));

        int examId = (int) examStat.get("examId");

        Button detailsBtn = new Button("📊 View Details");
        detailsBtn.getStyleClass().add("btn-primary");
        detailsBtn.setOnAction(e -> showExamDetails(examId));

        Button studentsBtn = new Button("👥 Student List");
        studentsBtn.getStyleClass().add("btn-secondary");
        studentsBtn.setOnAction(e -> showStudentList(examId));

        actionRow.getChildren().addAll(detailsBtn, studentsBtn);

        card.getChildren().addAll(headerRow, statsGrid, passRateBox, actionRow);

        return card;
    }

    /**
     * Add stat label to grid
     */
    private void addStatLabel(GridPane grid, String label, String value, int col, int row) {
        VBox statBox = new VBox(3);

        Label labelLabel = new Label(label);
        labelLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        statBox.getChildren().addAll(labelLabel, valueLabel);
        grid.add(statBox, col, row);
    }

    /**
     * Get color based on pass rate
     */
    private String getPassRateColor(double passRate) {
        if (passRate >= 75) return "#22c55e"; // Green
        if (passRate >= 50) return "#f59e0b"; // Orange
        return "#ef4444"; // Red
    }

    /**
     * Show detailed exam statistics
     */
    private void showExamDetails(int examId) {
        try {
            Map<String, Object> stats = reportService.getExamStatisticsById(examId);
            List<Map<String, Object>> questionStats = reportService.getQuestionStatistics(examId);

            // Create dialog
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Exam Details - " + stats.get("examTitle"));
            dialog.setHeaderText("Detailed Statistics");

            DialogPane dialogPane = dialog.getDialogPane();
            dialogPane.getButtonTypes().add(ButtonType.CLOSE);
            dialogPane.setPrefSize(700, 600);

            // Content
            VBox content = new VBox(20);
            content.setPadding(new Insets(20));

            // Overall stats
            Label statsHeader = new Label("📊 Overall Statistics");
            statsHeader.setFont(Font.font("System", FontWeight.BOLD, 16));

            GridPane statsGrid = new GridPane();
            statsGrid.setHgap(20);
            statsGrid.setVgap(10);

            addDetailStat(statsGrid, "Total Attempts:", String.valueOf(stats.get("totalAttempts")), 0, 0);
            addDetailStat(statsGrid, "Unique Students:", String.valueOf(stats.get("uniqueStudents")), 1, 0);
            addDetailStat(statsGrid, "Pass Rate:", String.format("%.1f%%", (double) stats.get("passRate")), 0, 1);
            addDetailStat(statsGrid, "Average Score:", String.format("%.1f/%d", (double) stats.get("avgScore"), (int) stats.get("totalMarks")), 1, 1);

            // Question analysis
            Label questionHeader = new Label("❓ Question Analysis (Sorted by Difficulty)");
            questionHeader.setFont(Font.font("System", FontWeight.BOLD, 16));

            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setFitToWidth(true);
            scrollPane.setPrefHeight(300);

            VBox questionsBox = new VBox(10);
            for (int i = 0; i < Math.min(10, questionStats.size()); i++) {
                Map<String, Object> qStat = questionStats.get(i);
                questionsBox.getChildren().add(createQuestionStatCard(qStat, i + 1));
            }

            scrollPane.setContent(questionsBox);

            content.getChildren().addAll(statsHeader, statsGrid, new Separator(), questionHeader, scrollPane);
            dialogPane.setContent(content);

            dialog.showAndWait();

        } catch (Exception e) {
            System.err.println("❌ Error showing exam details");
            e.printStackTrace();
        }
    }

    /**
     * Add detail stat to grid
     */
    private void addDetailStat(GridPane grid, String label, String value, int col, int row) {
        Label labelLabel = new Label(label);
        labelLabel.setStyle("-fx-font-weight: bold;");

        Label valueLabel = new Label(value);

        grid.add(labelLabel, col * 2, row);
        grid.add(valueLabel, col * 2 + 1, row);
    }

    /**
     * Create question stat card
     */
    private HBox createQuestionStatCard(Map<String, Object> qStat, int index) {
        HBox card = new HBox(10);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-padding: 10; -fx-background-color: #f5f5f5; -fx-background-radius: 5;");

        double successRate = (double) qStat.getOrDefault("successRate", 0.0);

        Label indexLabel = new Label("Q" + index);
        indexLabel.setStyle("-fx-font-weight: bold; -fx-min-width: 30;");

        Label questionLabel = new Label(truncate((String) qStat.get("questionText"), 50));
        questionLabel.setStyle("-fx-wrap-text: true;");
        HBox.setHgrow(questionLabel, Priority.ALWAYS);

        Label rateLabel = new Label(String.format("%.0f%% correct", successRate));
        rateLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + getSuccessRateColor(successRate));

        card.getChildren().addAll(indexLabel, questionLabel, rateLabel);
        return card;
    }

    /**
     * Truncate text
     */
    private String truncate(String text, int length) {
        if (text == null) return "";
        return text.length() > length ? text.substring(0, length) + "..." : text;
    }

    /**
     * Get color for success rate
     */
    private String getSuccessRateColor(double rate) {
        if (rate >= 70) return "#22c55e";
        if (rate >= 40) return "#f59e0b";
        return "#ef4444";
    }

    /**
     * Show student list for exam
     */
    private void showStudentList(int examId) {
        try {
            List<Map<String, Object>> students = reportService.getStudentPerformance(examId);

            // Create dialog
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Student Performance");
            dialog.setHeaderText("All Students Who Took This Exam");

            DialogPane dialogPane = dialog.getDialogPane();
            dialogPane.getButtonTypes().add(ButtonType.CLOSE);
            dialogPane.setPrefSize(800, 600);

            // Table
            TableView<Map<String, Object>> table = new TableView<>();
            table.setItems(javafx.collections.FXCollections.observableArrayList(students));

            TableColumn<Map<String, Object>, String> nameCol = new TableColumn<>("Student Name");
            nameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty((String) data.getValue().get("fullName")));
            nameCol.setPrefWidth(200);

            TableColumn<Map<String, Object>, String> scoreCol = new TableColumn<>("Score");
            scoreCol.setCellValueFactory(data -> {
                int obtained = (int) data.getValue().get("obtainedMarks");
                int total = (int) data.getValue().get("totalMarks");
                return new javafx.beans.property.SimpleStringProperty(obtained + "/" + total);
            });
            scoreCol.setPrefWidth(100);

            TableColumn<Map<String, Object>, String> percentCol = new TableColumn<>("Percentage");
            percentCol.setCellValueFactory(data -> {
                int obtained = (int) data.getValue().get("obtainedMarks");
                int total = (int) data.getValue().get("totalMarks");
                double percent = (obtained * 100.0) / total;
                return new javafx.beans.property.SimpleStringProperty(String.format("%.1f%%", percent));
            });
            percentCol.setPrefWidth(100);

            TableColumn<Map<String, Object>, String> resultCol = new TableColumn<>("Result");
            resultCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty((String) data.getValue().get("result")));
            resultCol.setPrefWidth(100);

            TableColumn<Map<String, Object>, String> timeCol = new TableColumn<>("Time Taken");
            timeCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().get("timeSpent") + " min"));
            timeCol.setPrefWidth(100);

            table.getColumns().addAll(nameCol, scoreCol, percentCol, resultCol, timeCol);

            dialogPane.setContent(table);
            dialog.showAndWait();

        } catch (Exception e) {
            System.err.println("❌ Error showing student list");
            e.printStackTrace();
        }
    }

    /**
     * Handle back to dashboard
     */
    @FXML
    private void handleBack() {
        SceneManager.switchScene("/com/examverse/fxml/dashboard/admin-dashboard.fxml");
    }

    /**
     * Show error alert
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();

        SceneManager.switchScene("/com/examverse/fxml/dashboard/admin-dashboard.fxml");
    }
}