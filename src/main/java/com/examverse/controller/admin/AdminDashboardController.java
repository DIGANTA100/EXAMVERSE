package com.examverse.controller.admin;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import com.examverse.model.exam.Exam;
import com.examverse.model.user.User;
import com.examverse.service.exam.ExamService;
import com.examverse.service.exam.QuestionService;
import com.examverse.util.SceneManager;
import com.examverse.util.SessionManager;

import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

/**
 * AdminDashboardController - Controls the admin dashboard interface
 * COMPLETE IMPLEMENTATION with exam management
 */
public class AdminDashboardController implements Initializable {

    @FXML
    private BorderPane rootPane;

    @FXML
    private VBox sidebarPane;

    @FXML
    private Button dashboardBtn, examsBtn, createExamBtn, questionsBtn, studentsBtn, resultsBtn, logoutBtn;

    @FXML
    private Label welcomeLabel, dateTimeLabel, userAvatar;

    @FXML
    private Label totalExamsLabel, totalQuestionsLabel, totalStudentsLabel, totalAttemptsLabel;

    @FXML
    private ScrollPane contentScroll;

    @FXML
    private VBox contentPane, examsListContainer;

    @FXML
    private Button contestsBtn;

    // Services
    private ExamService examService;
    private QuestionService questionService;

    // Data
    private User currentUser;
    private Timer dateTimeTimer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize services
        examService = new ExamService();
        questionService = new QuestionService();

        // Get current user
        currentUser = SessionManager.getInstance().getCurrentUser();

        if (currentUser != null) {
            welcomeLabel.setText("Welcome back, " + currentUser.getFullName());
            userAvatar.setText(currentUser.getUsername().substring(0, 1).toUpperCase());
        }

        // Start datetime update
        startDateTimeUpdater();

        // Load dashboard data
        loadDashboardStats();
        loadRecentExams();

        System.out.println("✅ Admin Dashboard initialized for: " + currentUser.getUsername());
    }

    /**
     * Start datetime updater
     */
    private void startDateTimeUpdater() {
        dateTimeTimer = new Timer(true);
        dateTimeTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    LocalDateTime now = LocalDateTime.now();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy • hh:mm:ss a");
                    dateTimeLabel.setText(now.format(formatter));
                });
            }
        }, 0, 1000);
    }

    /**
     * Load dashboard statistics
     */
    private void loadDashboardStats() {
        try {
            // Total exams
            int totalExams = examService.getTotalExamsCount();
            totalExamsLabel.setText(String.valueOf(totalExams));

            // Total questions
            int totalQuestions = questionService.getTotalQuestionsCount();
            totalQuestionsLabel.setText(String.valueOf(totalQuestions));

            // Total students
            int totalStudents = getTotalStudentsCount();
            totalStudentsLabel.setText(String.valueOf(totalStudents));

            // Total attempts
            int totalAttempts = examService.getTotalAttemptsCount();
            totalAttemptsLabel.setText(String.valueOf(totalAttempts));

            System.out.println("✅ Dashboard stats loaded");

        } catch (Exception e) {
            System.err.println("❌ Error loading dashboard stats");
            e.printStackTrace();
        }
    }

    /**
     * Get total students count from database
     */
    private int getTotalStudentsCount() {
        try (Connection conn = com.examverse.config.DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE user_type = 'STUDENT'")) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Load recent exams
     */
    private void loadRecentExams() {
        examsListContainer.getChildren().clear();

        try {
            List<Exam> exams = examService.getAllExams();

            if (exams.isEmpty()) {
                Label noExamsLabel = new Label("No exams created yet. Create your first exam!");
                noExamsLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");
                examsListContainer.getChildren().add(noExamsLabel);
            } else {
                // Show only first 5 exams
                int displayCount = Math.min(5, exams.size());
                for (int i = 0; i < displayCount; i++) {
                    examsListContainer.getChildren().add(createExamCard(exams.get(i)));
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Error loading recent exams");
            e.printStackTrace();
        }
    }

    /**
     * Create exam card UI component
     */
    private VBox createExamCard(Exam exam) {
        VBox card = new VBox(12);
        card.getStyleClass().add("exam-card");
        card.setPadding(new Insets(20));

        // Header row
        HBox headerRow = new HBox(15);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        // Subject badge
        Label subjectBadge = new Label(exam.getSubject());
        subjectBadge.getStyleClass().addAll("badge", "badge-primary");

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

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Status badge
        Label statusBadge = new Label(exam.getStatus());
        statusBadge.getStyleClass().addAll("badge", "badge-primary");

        headerRow.getChildren().addAll(subjectBadge, difficultyBadge, spacer, statusBadge);

        // Title
        Label titleLabel = new Label(exam.getExamTitle());
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        // Description
        Label descLabel = new Label(exam.getDescription());
        descLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");
        descLabel.setWrapText(true);

        // Stats row
        HBox statsRow = new HBox(20);
        statsRow.setAlignment(Pos.CENTER_LEFT);

        Label questionsLabel = new Label("❓ " + exam.getTotalQuestions() + " Questions");
        questionsLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");

        Label durationLabel = new Label("⏱️ " + exam.getFormattedDuration());
        durationLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");

        Label marksLabel = new Label("📊 " + exam.getTotalMarks() + " Marks");
        marksLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");

        statsRow.getChildren().addAll(questionsLabel, durationLabel, marksLabel);

        // Action buttons row
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

        // Add all to card
        card.getChildren().addAll(headerRow, titleLabel, descLabel, statsRow, actionRow);

        return card;
    }

    // ==================== NAVIGATION HANDLERS ====================

    @FXML
    private void handleDashboard() {
        setActiveButton(dashboardBtn);
        loadDashboardStats();
        loadRecentExams();
    }

    @FXML
    private void handleExams() {
        setActiveButton(examsBtn);
        showAlert("All Exams", "All Exams view - Coming in next step!");
    }

    @FXML
    private void handleCreateExam() {
        setActiveButton(createExamBtn);
        showCreateExamDialog();
    }

    @FXML
    private void handleQuestions() {
        setActiveButton(questionsBtn);
        SceneManager.switchScene("/com/examverse/fxml/dashboard/question-manager.fxml");
    }

    @FXML
    private void handleStudents() {
        setActiveButton(studentsBtn);
        showAlert("Students", "Students view - Coming soon!");
    }

    @FXML
    private void handleResults() {
        setActiveButton(resultsBtn);
        // Navigate to exam reports page
        SceneManager.switchScene("/com/examverse/fxml/dashboard/exam-reports.fxml");
    }

    @FXML
    private void handleContests() {
        setActiveButton(contestsBtn);
        SceneManager.switchScene("/com/examverse/fxml/contest/contest-manager.fxml");
    }


    @FXML
    private void handleLogout() {
        if (dateTimeTimer != null) {
            dateTimeTimer.cancel();
        }
        SessionManager.getInstance().clearSession();
        SceneManager.switchScene("/com/examverse/fxml/dashboard/dashboard-landing.fxml");
    }

    // ==================== EXAM ACTIONS ====================

    private void handleEditExam(Exam exam) {
        showAlert("Edit Exam", "Edit exam: " + exam.getExamTitle() + " - Coming soon!");
    }

    private void handleManageQuestions(Exam exam) {
        QuestionManagerDialog dialog = new QuestionManagerDialog(exam);
        dialog.showAndWait();

        // Refresh stats after managing questions
        loadDashboardStats();
    }

    private void handleViewExam(Exam exam) {
        showAlert("View Exam", "Viewing exam: " + exam.getExamTitle());
    }

    // ==================== CREATE EXAM DIALOG ====================

    private void showCreateExamDialog() {
        Dialog<Exam> dialog = new Dialog<>();
        dialog.setTitle("Create New Exam");
        dialog.setHeaderText("Enter exam details");

        // Set dialog style
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/com/examverse/css/student-dashboard.css").toExternalForm());
        dialogPane.getStyleClass().add("alert");

        // Add buttons
        ButtonType createButtonType = new ButtonType("Create Exam", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        // Create form
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        TextField titleField = new TextField();
        titleField.setPromptText("Exam Title");

        TextField subjectField = new TextField();
        subjectField.setPromptText("Subject");

        TextArea descArea = new TextArea();
        descArea.setPromptText("Description");
        descArea.setPrefRowCount(3);

        ComboBox<String> difficultyBox = new ComboBox<>();
        difficultyBox.getItems().addAll("EASY", "MEDIUM", "HARD");
        difficultyBox.setValue("MEDIUM");

        TextField questionsField = new TextField();
        questionsField.setPromptText("Number of Questions");

        TextField marksField = new TextField();
        marksField.setPromptText("Total Marks");

        TextField durationField = new TextField();
        durationField.setPromptText("Duration (minutes)");

        TextField passingField = new TextField();
        passingField.setPromptText("Passing Marks");

        grid.add(new Label("Exam Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Subject:"), 0, 1);
        grid.add(subjectField, 1, 1);
        grid.add(new Label("Description:"), 0, 2);
        grid.add(descArea, 1, 2);
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

        dialog.getDialogPane().setContent(grid);

        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                try {
                    Exam newExam = new Exam();
                    newExam.setExamTitle(titleField.getText());
                    newExam.setSubject(subjectField.getText());
                    newExam.setDescription(descArea.getText());
                    newExam.setDifficulty(difficultyBox.getValue());
                    newExam.setTotalQuestions(Integer.parseInt(questionsField.getText()));
                    newExam.setTotalMarks(Integer.parseInt(marksField.getText()));
                    newExam.setDurationMinutes(Integer.parseInt(durationField.getText()));
                    newExam.setPassingMarks(Integer.parseInt(passingField.getText()));
                    newExam.setStatus("ACTIVE");
                    newExam.setCreatedBy(currentUser.getId());
                    return newExam;
                } catch (NumberFormatException e) {
                    showAlert("Error", "Please enter valid numbers for questions, marks, and duration");
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(exam -> {
            if (examService.createExam(exam)) {
                showAlert("Success", "Exam created successfully!");
                loadRecentExams();
                loadDashboardStats();
            } else {
                showAlert("Error", "Failed to create exam");
            }
        });
    }

    // ==================== UTILITY METHODS ====================

    private void setActiveButton(Button activeBtn) {
        // Remove active class from all buttons
        dashboardBtn.getStyleClass().remove("sidebar-btn-active");
        examsBtn.getStyleClass().remove("sidebar-btn-active");
        createExamBtn.getStyleClass().remove("sidebar-btn-active");
        questionsBtn.getStyleClass().remove("sidebar-btn-active");
        studentsBtn.getStyleClass().remove("sidebar-btn-active");
        resultsBtn.getStyleClass().remove("sidebar-btn-active");

        // Add active class to clicked button
        if (!activeBtn.getStyleClass().contains("sidebar-btn-active")) {
            activeBtn.getStyleClass().add("sidebar-btn-active");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}