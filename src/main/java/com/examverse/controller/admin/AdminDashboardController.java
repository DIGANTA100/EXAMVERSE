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
import javafx.util.Duration;

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
import com.examverse.controller.forum.AdminForumController;
import javafx.fxml.FXMLLoader;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;

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

   private Label welcomeLabel, dateTimeLabel;

  @FXML
  private Button userAvatar;

    @FXML
    private Label totalExamsLabel, totalQuestionsLabel, totalStudentsLabel, totalAttemptsLabel;

    @FXML
    private ScrollPane contentScroll;

    @FXML
    private VBox contentPane, examsListContainer;

    @FXML
    private Button contestsBtn;

    @FXML private Button forumBtn;

    @FXML private Button sendNotifBtn;

    // Services
    private ExamService examService;
    private QuestionService questionService;
    private AdminForumController activeForum;
    private AdminNotificationSender notificationSender;
    // Data
    private User currentUser;
    private Timer dateTimeTimer;
    private javafx.scene.Node originalCenter;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize services
        examService = new ExamService();
        questionService = new QuestionService();

        // Get current user
        currentUser = SessionManager.getInstance().getCurrentUser();

        if (currentUser != null) {
            welcomeLabel.setText("Welcome back, " + currentUser.getFullName());

            // Avatar button — gold ring, initials, opens profile on click
            String initials = getAdminInitials(currentUser.getFullName());
            userAvatar.setText(initials);
            userAvatar.setStyle(
                    "-fx-background-color: linear-gradient(to bottom, #f59e0bcc, #d97706aa);" +
                            "-fx-text-fill: #0a0500;" +
                            "-fx-font-size: 14px; -fx-font-weight: 800;" +
                            "-fx-padding: 9 13; -fx-background-radius: 50%; -fx-cursor: hand;" +
                            "-fx-border-color: #f59e0b99; -fx-border-width: 2; -fx-border-radius: 50%;" +
                            "-fx-effect: dropshadow(gaussian,#f59e0b66,12,0.4,0,0);" +
                            "-fx-min-width: 42; -fx-min-height: 42;"
            );
            // Hover brighten
            userAvatar.setOnMouseEntered(e -> userAvatar.setStyle(
                    "-fx-background-color: linear-gradient(to bottom, #fbbf24dd, #f59e0bcc);" +
                            "-fx-text-fill: #0a0500;" +
                            "-fx-font-size: 14px; -fx-font-weight: 800;" +
                            "-fx-padding: 9 13; -fx-background-radius: 50%; -fx-cursor: hand;" +
                            "-fx-border-color: #fbbf24; -fx-border-width: 2; -fx-border-radius: 50%;" +
                            "-fx-effect: dropshadow(gaussian,#f59e0b99,16,0.5,0,0);" +
                            "-fx-min-width: 42; -fx-min-height: 42;"
            ));
            userAvatar.setOnMouseExited(e -> userAvatar.setStyle(
                    "-fx-background-color: linear-gradient(to bottom, #f59e0bcc, #d97706aa);" +
                            "-fx-text-fill: #0a0500;" +
                            "-fx-font-size: 14px; -fx-font-weight: 800;" +
                            "-fx-padding: 9 13; -fx-background-radius: 50%; -fx-cursor: hand;" +
                            "-fx-border-color: #f59e0b99; -fx-border-width: 2; -fx-border-radius: 50%;" +
                            "-fx-effect: dropshadow(gaussian,#f59e0b66,12,0.4,0,0);" +
                            "-fx-min-width: 42; -fx-min-height: 42;"
            ));
        }

        // Start datetime update
        startDateTimeUpdater();

        // Load dashboard data
        loadDashboardStats();
        loadRecentExams();

        System.out.println("✅ Admin Dashboard initialized for: " + currentUser.getUsername());
        notificationSender = new AdminNotificationSender(sendNotifBtn);
        originalCenter = rootPane.getCenter();
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
        // If the forum (or any other full-screen view) replaced the center,
        // restore the original content-container first.
        if (rootPane.getCenter() != originalCenter) {
            rootPane.setCenter(originalCenter);
        }

        // Stop forum polling if it was running
        if (activeForum != null) {
            activeForum.stopPolling();
            activeForum = null;
        }

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
        // ── Styled confirmation dialog ────────────────────────────────
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to logout?");

        DialogPane dp = alert.getDialogPane();

        // Load our admin stylesheet so all base colours apply
        try {
            java.net.URL css = getClass().getResource("/com/examverse/css/admin-dashboard.css");
            if (css != null) dp.getStylesheets().add(css.toExternalForm());
        } catch (Exception ignored) {}

        // Override the dialog pane background and border
        dp.setStyle(
                "-fx-background-color: #0d1428;" +
                        "-fx-border-color: #06b6d444;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-border-radius: 14;" +
                        "-fx-background-radius: 14;"
        );

        // Style the content label (the question text)
           javafx.scene.Node contentLbl = dp.lookup(".content.label");
   if (contentLbl != null)
       contentLbl.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 14px;");

        // Style buttons after the dialog is shown (only reliable hook)
        alert.setOnShown(e -> {
            javafx.scene.Node okBtn = dp.lookupButton(ButtonType.OK);
            javafx.scene.Node cancelBtn = dp.lookupButton(ButtonType.CANCEL);

            if (okBtn != null) okBtn.setStyle(
                    "-fx-background-color: linear-gradient(to right,#ef4444,#dc2626);" +
                            "-fx-text-fill: white; -fx-font-weight: 700; -fx-font-size: 13px;" +
                            "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 9 22;" +
                            "-fx-effect: dropshadow(gaussian,#ef444460,8,0.4,0,2);"
            );
            if (cancelBtn != null) cancelBtn.setStyle(
                    "-fx-background-color: #1e2231;" +
                            "-fx-text-fill: #94a3b8; -fx-font-size: 13px; -fx-font-weight: 600;" +
                            "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 9 22;" +
                            "-fx-border-color: #2d3348; -fx-border-width: 1; -fx-border-radius: 8;"
            );
        });

        // Only logout if the admin confirmed
        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                if (dateTimeTimer != null) dateTimeTimer.cancel();
                if (activeForum != null)   activeForum.stopPolling();
                if (notificationSender != null) notificationSender.close();
                SessionManager.getInstance().clearSession();
                SceneManager.switchScene("/com/examverse/fxml/dashboard/dashboard-landing.fxml");
            }
        });
    }
    @FXML
    private void handleForum() {
        setActiveButton(forumBtn);
        loadAdminForum();
    }


    private void loadAdminForum() {
        if (activeForum != null) {
            activeForum.stopPolling();
            activeForum = null;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/examverse/fxml/forum/admin-forum.fxml"));
            javafx.scene.layout.BorderPane forumView = loader.load();
            activeForum = loader.getController();

            // Place directly in rootPane center for full height
            rootPane.setCenter(forumView);

        } catch (Exception e) {
            System.err.println("❌ Failed to load admin forum: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @FXML
    private void handleAvatarClick() {
        loadAdminProfile();
    }

    @FXML
    private void handleSendNotification() {
        notificationSender.toggle();
    }


    private void loadAdminProfile() {
        AdminProfileSection profile = new AdminProfileSection(currentUser);
        VBox profileView = profile.build();

        // ── Back button ───────────────────────────────────────────────
        Button backBtn = new Button("← Back to Dashboard");
        backBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #06b6d4;" +
                        "-fx-font-size: 13px; -fx-font-weight: 600;" +
                        "-fx-cursor: hand; -fx-border-width: 0;" +
                        "-fx-padding: 0;"
        );
        backBtn.setOnMouseEntered(e -> backBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #22d3ee;" +
                        "-fx-font-size: 13px; -fx-font-weight: 600;" +
                        "-fx-cursor: hand; -fx-border-width: 0;" +
                        "-fx-padding: 0;"
        ));
        backBtn.setOnMouseExited(e -> backBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #06b6d4;" +
                        "-fx-font-size: 13px; -fx-font-weight: 600;" +
                        "-fx-cursor: hand; -fx-border-width: 0;" +
                        "-fx-padding: 0;"
        ));
        backBtn.setOnAction(e -> handleDashboard());

        // Wrap button in a padded HBox so it sits flush with the profile content
        HBox backBar = new HBox(backBtn);
        backBar.setPadding(new Insets(20, 36, 0, 36));

        // Insert back bar at the very top of the profile view
        profileView.getChildren().add(0, backBar);

        // ── Animate in ────────────────────────────────────────────────
        profileView.setOpacity(0);
        contentPane.getChildren().setAll(profileView);

        FadeTransition ft = new FadeTransition(javafx.util.Duration.millis(250), profileView);
        ft.setFromValue(0); ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(javafx.util.Duration.millis(250), profileView);
        tt.setFromY(14); tt.setToY(0);
        new ParallelTransition(ft, tt).play();

        if (contentScroll != null) {
            javafx.application.Platform.runLater(() -> contentScroll.setVvalue(0));
        }
    }

    private static String getAdminInitials(String name) {
        if (name == null || name.isBlank()) return "A";
        String[] p = name.trim().split("\\s+");
        if (p.length == 1) return p[0].substring(0, Math.min(2, p[0].length())).toUpperCase();
        return ("" + p[0].charAt(0) + p[p.length - 1].charAt(0)).toUpperCase();
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
        dialogPane.getStylesheets().add(getClass().getResource("/com/examverse/css/admin-dashboard.css").toExternalForm());
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
        forumBtn.getStyleClass().remove("sidebar-btn-active");

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