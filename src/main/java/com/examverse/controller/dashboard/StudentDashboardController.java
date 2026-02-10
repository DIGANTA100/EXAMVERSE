package com.examverse.controller.dashboard;

import com.examverse.model.exam.Exam;
import com.examverse.model.exam.StudentExamAttempt;
import com.examverse.model.user.StudentStats;
import com.examverse.model.user.User;
import com.examverse.service.exam.ExamService;
import com.examverse.util.SceneManager;
import com.examverse.util.SessionManager;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * StudentDashboardController - Main dashboard for students after login
 * Features: Dashboard Home, My Exams (Available, Ongoing, Completed)
 */
public class StudentDashboardController implements Initializable {

    // ============================================
    // FXML INJECTED COMPONENTS
    // ============================================

    @FXML private BorderPane rootPane;
    @FXML private VBox sidebarPane;
    @FXML private VBox contentPane;

    // Sidebar Navigation Buttons
    @FXML private Button dashboardBtn;
    @FXML private Button myExamsBtn;
    @FXML private Button practiceBtn;
    @FXML private Button resultsBtn;
    @FXML private Button profileBtn;
    @FXML private Button logoutBtn;

    // Header Components
    @FXML private Label welcomeLabel;
    @FXML private Label dateTimeLabel;
    @FXML private Label notificationBadge;

    // Content Areas (dynamically loaded)
    @FXML private ScrollPane contentScrollPane;

    // ============================================
    // SERVICES AND DATA
    // ============================================

    private ExamService examService;
    private User currentUser;
    private StudentStats studentStats;
    private Timeline clockTimeline;

    // Active section tracker
    private String activeSection = "dashboard";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize services
        examService = new ExamService();

        // Get current user from session
        currentUser = SessionManager.getInstance().getCurrentUser();

        if (currentUser == null) {
            System.err.println("❌ No user in session! Redirecting to login...");
            SceneManager.switchScene("/com/examverse/fxml/auth/login.fxml");
            return;
        }

        // Setup UI
        setupHeader();
        setupSidebar();
        setupAnimations();

        // Load default section (Dashboard Home)
        loadDashboardHome();

        System.out.println("✅ Student Dashboard loaded for: " + currentUser.getFullName());
    }

    /**
     * Setup header with welcome message and clock
     */
    private void setupHeader() {
        // Welcome message
        welcomeLabel.setText("Welcome back, " + currentUser.getFullName());

        // Initialize real-time clock
        updateDateTime();
        clockTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> updateDateTime())
        );
        clockTimeline.setCycleCount(Timeline.INDEFINITE);
        clockTimeline.play();

        // Notification badge (placeholder)
        notificationBadge.setText("3");
        notificationBadge.setVisible(true);
    }

    /**
     * Update date and time label
     */
    private void updateDateTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy • hh:mm:ss a");
        dateTimeLabel.setText(now.format(formatter));
    }

    /**
     * Setup sidebar navigation
     */
    private void setupSidebar() {
        // Set active button style
        setActiveButton(dashboardBtn);
    }

    /**
     * Setup background animations
     */
    private void setupAnimations() {
        // Animated background particles
        createParticleAnimation();
    }

    /**
     * Create animated background particles
     */
    private void createParticleAnimation() {
        Pane particlePane = new Pane();
        particlePane.setStyle("-fx-background-color: transparent;");
        particlePane.setMouseTransparent(true);

        // Create 30 animated particles
        for (int i = 0; i < 30; i++) {
            Circle particle = new Circle(2 + Math.random() * 3);
            particle.setFill(Color.rgb(34, 211, 238, 0.3 + Math.random() * 0.3));

            // Random starting position
            particle.setLayoutX(Math.random() * 1400);
            particle.setLayoutY(Math.random() * 800);

            particlePane.getChildren().add(particle);

            // Floating animation
            TranslateTransition translate = new TranslateTransition(
                    Duration.seconds(10 + Math.random() * 10), particle
            );
            translate.setByY(-50 - Math.random() * 100);
            translate.setByX(-20 + Math.random() * 40);
            translate.setCycleCount(Animation.INDEFINITE);
            translate.setAutoReverse(true);
            translate.setInterpolator(Interpolator.EASE_BOTH);

            // Fade animation
            FadeTransition fade = new FadeTransition(
                    Duration.seconds(3 + Math.random() * 3), particle
            );
            fade.setFromValue(0.2);
            fade.setToValue(0.8);
            fade.setCycleCount(Animation.INDEFINITE);
            fade.setAutoReverse(true);

            translate.play();
            fade.play();
        }

        // Add particle pane as background
        if (!rootPane.getChildren().contains(particlePane)) {
            rootPane.getChildren().add(0, particlePane);
        }
    }

    // ============================================
    // NAVIGATION HANDLERS
    // ============================================

    @FXML
    private void handleDashboard() {
        setActiveButton(dashboardBtn);
        loadDashboardHome();
    }

    @FXML
    private void handleMyExams() {
        setActiveButton(myExamsBtn);
        loadMyExams();
    }

    @FXML
    private void handlePractice() {
        setActiveButton(practiceBtn);
        showComingSoon("Practice Mode");
    }

    @FXML
    private void handleResults() {
        setActiveButton(resultsBtn);
        showComingSoon("Results & Analytics");
    }

    @FXML
    private void handleProfile() {
        setActiveButton(profileBtn);
        showComingSoon("Profile");
    }

    @FXML
    private void handleLogout() {
        // Stop clock
        if (clockTimeline != null) {
            clockTimeline.stop();
        }

        // Clear session
        SessionManager.getInstance().clearSession();

        // Show confirmation
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Logged Out");
        alert.setHeaderText(null);
        alert.setContentText("You have been logged out successfully!");
        alert.showAndWait();

        // Navigate to login
        SceneManager.switchScene("/com/examverse/fxml/auth/login.fxml");
    }

    /**
     * Set active navigation button
     */
    private void setActiveButton(Button activeBtn) {
        // Remove active class from all buttons
        dashboardBtn.getStyleClass().remove("sidebar-btn-active");
        myExamsBtn.getStyleClass().remove("sidebar-btn-active");
        practiceBtn.getStyleClass().remove("sidebar-btn-active");
        resultsBtn.getStyleClass().remove("sidebar-btn-active");
        profileBtn.getStyleClass().remove("sidebar-btn-active");

        // Add active class to selected button
        if (!activeBtn.getStyleClass().contains("sidebar-btn-active")) {
            activeBtn.getStyleClass().add("sidebar-btn-active");
        }
    }

    // ============================================
    // CONTENT LOADERS
    // ============================================

    /**
     * Load Dashboard Home (default view)
     */
    private void loadDashboardHome() {
        activeSection = "dashboard";

        VBox content = new VBox(30);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: transparent;");

        // Section Title
        Label titleLabel = new Label("📊 Dashboard Overview");
        titleLabel.setStyle("""
            -fx-font-size: 28px;
            -fx-font-weight: bold;
            -fx-text-fill: white;
            """);

        // Fetch student statistics
        studentStats = examService.getStudentStats(currentUser.getId());

        // Stats Cards Container
        HBox statsContainer = new HBox(20);
        statsContainer.setAlignment(Pos.CENTER_LEFT);

        // Create stat cards
        VBox examsCard = createStatCard("📝", "Exams Attempted",
                String.valueOf(studentStats.getTotalExamsAttempted()), "#22d3ee");

        VBox scoreCard = createStatCard("📈", "Average Score",
                studentStats.getFormattedAverageScore(), "#10b981");

        VBox accuracyCard = createStatCard("🎯", "Accuracy",
                studentStats.getFormattedAccuracy(), "#8b5cf6");

        VBox passedCard = createStatCard("✅", "Exams Passed",
                String.valueOf(studentStats.getTotalExamsPassed()), "#22c55e");

        statsContainer.getChildren().addAll(examsCard, scoreCard, accuracyCard, passedCard);

        // Quick Actions Section
        Label actionsTitle = new Label("⚡ Quick Actions");
        actionsTitle.setStyle("""
            -fx-font-size: 20px;
            -fx-font-weight: 600;
            -fx-text-fill: #e2e8f0;
            """);

        HBox actionsContainer = new HBox(15);
        actionsContainer.setAlignment(Pos.CENTER_LEFT);

        Button startExamBtn = createActionButton("🚀 Start New Exam", "#22d3ee");
        startExamBtn.setOnAction(e -> handleMyExams());

        Button continueBtn = createActionButton("▶️ Continue Exam", "#f59e0b");
        continueBtn.setOnAction(e -> handleMyExams());

        Button practiceBtn = createActionButton("💪 Practice Mode", "#8b5cf6");
        practiceBtn.setOnAction(e -> handlePractice());

        Button resultsBtn = createActionButton("📊 View Results", "#06b6d4");
        resultsBtn.setOnAction(e -> handleResults());

        actionsContainer.getChildren().addAll(startExamBtn, continueBtn, practiceBtn, resultsBtn);

        // Recent Activity (placeholder)
        Label recentTitle = new Label("🕐 Recent Activity");
        recentTitle.setStyle("""
            -fx-font-size: 20px;
            -fx-font-weight: 600;
            -fx-text-fill: #e2e8f0;
            """);

        VBox recentBox = new VBox(10);
        recentBox.setStyle("""
            -fx-background-color: rgba(30, 41, 59, 0.6);
            -fx-background-radius: 12;
            -fx-padding: 20;
            -fx-border-color: rgba(51, 65, 85, 0.5);
            -fx-border-width: 1;
            -fx-border-radius: 12;
            """);

        Label recentPlaceholder = new Label("No recent activity yet. Start an exam to see your progress!");
        recentPlaceholder.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");
        recentBox.getChildren().add(recentPlaceholder);

        // Add all to content
        content.getChildren().addAll(
                titleLabel,
                statsContainer,
                new Separator(),
                actionsTitle,
                actionsContainer,
                new Separator(),
                recentTitle,
                recentBox
        );

        // Set content with fade-in animation
        setContentWithAnimation(content);
    }

    /**
     * Load My Exams section
     */
    private void loadMyExams() {
        activeSection = "myexams";

        VBox content = new VBox(25);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: transparent;");

        // Section Title
        Label titleLabel = new Label("📚 My Exams");
        titleLabel.setStyle("""
            -fx-font-size: 28px;
            -fx-font-weight: bold;
            -fx-text-fill: white;
            """);

        // Tab-like navigation
        HBox tabNav = new HBox(15);
        tabNav.setAlignment(Pos.CENTER_LEFT);

        Button availableTab = createTabButton("📘 Available", true);
        Button ongoingTab = createTabButton("📕 Ongoing", false);
        Button completedTab = createTabButton("📗 Completed", false);

        tabNav.getChildren().addAll(availableTab, ongoingTab, completedTab);

        // Content container for tabs
        VBox tabContent = new VBox(15);

        // Available Exams (default)
        VBox availableExams = createAvailableExamsTab();
        tabContent.getChildren().add(availableExams);

        // Tab switching logic
        availableTab.setOnAction(e -> {
            setActiveTab(availableTab, ongoingTab, completedTab);
            tabContent.getChildren().clear();
            tabContent.getChildren().add(createAvailableExamsTab());
        });

        ongoingTab.setOnAction(e -> {
            setActiveTab(ongoingTab, availableTab, completedTab);
            tabContent.getChildren().clear();
            tabContent.getChildren().add(createOngoingExamsTab());
        });

        completedTab.setOnAction(e -> {
            setActiveTab(completedTab, availableTab, ongoingTab);
            tabContent.getChildren().clear();
            tabContent.getChildren().add(createCompletedExamsTab());
        });

        content.getChildren().addAll(titleLabel, tabNav, tabContent);

        setContentWithAnimation(content);
    }

    /**
     * Create Available Exams tab content
     */
    private VBox createAvailableExamsTab() {
        VBox container = new VBox(15);

        // Filters
        HBox filters = new HBox(15);
        filters.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> subjectFilter = new ComboBox<>();
        subjectFilter.getItems().addAll("All Subjects", "Programming", "Computer Science",
                "Database", "Web Development");
        subjectFilter.setValue("All Subjects");
        subjectFilter.setStyle("-fx-background-color: rgba(30, 41, 59, 0.8); -fx-text-fill: white;");

        ComboBox<String> difficultyFilter = new ComboBox<>();
        difficultyFilter.getItems().addAll("All Levels", "EASY", "MEDIUM", "HARD");
        difficultyFilter.setValue("All Levels");
        difficultyFilter.setStyle("-fx-background-color: rgba(30, 41, 59, 0.8); -fx-text-fill: white;");

        filters.getChildren().addAll(
                new Label("🔍 Filter:") {{
                    setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 14px; -fx-font-weight: 600;");
                }},
                subjectFilter,
                difficultyFilter
        );

        // Exams Grid
        GridPane examGrid = new GridPane();
        examGrid.setHgap(20);
        examGrid.setVgap(20);

        List<Exam> exams = examService.getAllActiveExams();

        int col = 0;
        int row = 0;
        for (Exam exam : exams) {
            VBox examCard = createExamCard(exam);
            examGrid.add(examCard, col, row);

            col++;
            if (col == 2) { // 2 columns
                col = 0;
                row++;
            }
        }

        if (exams.isEmpty()) {
            Label noExams = new Label("📭 No exams available at the moment");
            noExams.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 16px;");
            container.getChildren().addAll(filters, noExams);
        } else {
            container.getChildren().addAll(filters, examGrid);
        }

        return container;
    }

    /**
     * Create Ongoing Exams tab content
     */
    private VBox createOngoingExamsTab() {
        VBox container = new VBox(15);

        List<StudentExamAttempt> ongoing = examService.getOngoingExams(currentUser.getId());

        if (ongoing.isEmpty()) {
            Label noExams = new Label("📭 No ongoing exams. Start a new exam!");
            noExams.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 16px;");
            container.getChildren().add(noExams);
        } else {
            for (StudentExamAttempt attempt : ongoing) {
                VBox attemptCard = createOngoingExamCard(attempt);
                container.getChildren().add(attemptCard);
            }
        }

        return container;
    }

    /**
     * Create Completed Exams tab content
     */
    private VBox createCompletedExamsTab() {
        VBox container = new VBox(15);

        List<StudentExamAttempt> completed = examService.getCompletedExams(currentUser.getId());

        if (completed.isEmpty()) {
            Label noExams = new Label("📭 No completed exams yet");
            noExams.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 16px;");
            container.getChildren().add(noExams);
        } else {
            for (StudentExamAttempt attempt : completed) {
                VBox attemptCard = createCompletedExamCard(attempt);
                container.getChildren().add(attemptCard);
            }
        }

        return container;
    }

    // ============================================
    // UI COMPONENT CREATORS
    // ============================================

    /**
     * Create stat card for dashboard
     */
    private VBox createStatCard(String icon, String label, String value, String accentColor) {
        VBox card = new VBox(10);
        card.setPrefWidth(250);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(20));
        card.setStyle(String.format("""
            -fx-background-color: rgba(30, 41, 59, 0.6);
            -fx-background-radius: 12;
            -fx-border-color: rgba(51, 65, 85, 0.5);
            -fx-border-width: 1;
            -fx-border-radius: 12;
            -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 10, 0.3, 0, 2);
            """));

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 32px;");

        Label titleLabel = new Label(label);
        titleLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px; -fx-font-weight: 500;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle(String.format(
                "-fx-text-fill: %s; -fx-font-size: 28px; -fx-font-weight: bold;", accentColor
        ));

        card.getChildren().addAll(iconLabel, titleLabel, valueLabel);

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle() +
                "-fx-scale-x: 1.03; -fx-scale-y: 1.03;"));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle() +
                "-fx-scale-x: 1.0; -fx-scale-y: 1.0;"));

        return card;
    }

    /**
     * Create action button
     */
    private Button createActionButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle(String.format("""
            -fx-background-color: %s;
            -fx-text-fill: #0f172a;
            -fx-font-size: 14px;
            -fx-font-weight: 600;
            -fx-padding: 12 24;
            -fx-background-radius: 8;
            -fx-cursor: hand;
            """, color));

        btn.setOnMouseEntered(e -> btn.setOpacity(0.9));
        btn.setOnMouseExited(e -> btn.setOpacity(1.0));

        return btn;
    }

    /**
     * Create tab button
     */
    private Button createTabButton(String text, boolean active) {
        Button btn = new Button(text);
        String style = active ?
                """
                -fx-background-color: #22d3ee;
                -fx-text-fill: #0f172a;
                -fx-font-size: 14px;
                -fx-font-weight: 600;
                -fx-padding: 10 20;
                -fx-background-radius: 6;
                -fx-cursor: hand;
                """ :
                """
                -fx-background-color: rgba(30, 41, 59, 0.6);
                -fx-text-fill: #94a3b8;
                -fx-font-size: 14px;
                -fx-font-weight: 600;
                -fx-padding: 10 20;
                -fx-background-radius: 6;
                -fx-cursor: hand;
                -fx-border-color: rgba(51, 65, 85, 0.5);
                -fx-border-width: 1;
                -fx-border-radius: 6;
                """;

        btn.setStyle(style);
        return btn;
    }

    /**
     * Set active tab styling
     */
    private void setActiveTab(Button active, Button... others) {
        active.setStyle("""
            -fx-background-color: #22d3ee;
            -fx-text-fill: #0f172a;
            -fx-font-size: 14px;
            -fx-font-weight: 600;
            -fx-padding: 10 20;
            -fx-background-radius: 6;
            -fx-cursor: hand;
            """);

        for (Button btn : others) {
            btn.setStyle("""
                -fx-background-color: rgba(30, 41, 59, 0.6);
                -fx-text-fill: #94a3b8;
                -fx-font-size: 14px;
                -fx-font-weight: 600;
                -fx-padding: 10 20;
                -fx-background-radius: 6;
                -fx-cursor: hand;
                -fx-border-color: rgba(51, 65, 85, 0.5);
                -fx-border-width: 1;
                -fx-border-radius: 6;
                """);
        }
    }

    /**
     * Create exam card for available exams
     */
    private VBox createExamCard(Exam exam) {
        VBox card = new VBox(12);
        card.setPrefWidth(450);
        card.setPadding(new Insets(20));
        card.setStyle("""
            -fx-background-color: rgba(30, 41, 59, 0.7);
            -fx-background-radius: 12;
            -fx-border-color: rgba(51, 65, 85, 0.5);
            -fx-border-width: 1;
            -fx-border-radius: 12;
            -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 10, 0.3, 0, 2);
            """);

        // Title
        Label title = new Label(exam.getExamTitle());
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        title.setWrapText(true);

        // Subject and Difficulty
        HBox badges = new HBox(10);
        badges.setAlignment(Pos.CENTER_LEFT);

        Label subjectBadge = new Label("📚 " + exam.getSubject());
        subjectBadge.setStyle("""
            -fx-background-color: rgba(34, 211, 238, 0.2);
            -fx-text-fill: #22d3ee;
            -fx-padding: 4 12;
            -fx-background-radius: 12;
            -fx-font-size: 12px;
            -fx-font-weight: 600;
            """);

        Label difficultyBadge = new Label(exam.getDifficultyBadge() + " " + exam.getDifficulty());
        difficultyBadge.setStyle(String.format("""
            -fx-background-color: rgba(255, 255, 255, 0.1);
            -fx-text-fill: %s;
            -fx-padding: 4 12;
            -fx-background-radius: 12;
            -fx-font-size: 12px;
            -fx-font-weight: 600;
            """, exam.getDifficultyColor()));

        badges.getChildren().addAll(subjectBadge, difficultyBadge);

        // Description
        Label description = new Label(exam.getDescription());
        description.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
        description.setWrapText(true);
        description.setMaxHeight(40);

        // Info grid
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(20);
        infoGrid.setVgap(8);

        addInfoRow(infoGrid, 0, "📝", "Questions", exam.getTotalQuestions() + "");
        addInfoRow(infoGrid, 1, "⏱️", "Duration", exam.getFormattedDuration());
        addInfoRow(infoGrid, 2, "🎯", "Total Marks", exam.getTotalMarks() + "");
        addInfoRow(infoGrid, 3, "✅", "Passing", exam.getPassingMarks() + "");

        // Start button
        Button startBtn = new Button("🚀 Start Exam");
        startBtn.setStyle("""
            -fx-background-color: #22d3ee;
            -fx-text-fill: #0f172a;
            -fx-font-size: 14px;
            -fx-font-weight: 600;
            -fx-padding: 10 0;
            -fx-background-radius: 8;
            -fx-cursor: hand;
            """);
        startBtn.setMaxWidth(Double.MAX_VALUE);

        startBtn.setOnAction(e -> handleStartExam(exam));

        card.getChildren().addAll(title, badges, description, new Separator(), infoGrid, startBtn);

        // Hover effect
        card.setOnMouseEntered(e -> {
            card.setStyle(card.getStyle() +
                    "-fx-border-color: rgba(34, 211, 238, 0.5); -fx-scale-x: 1.02; -fx-scale-y: 1.02;");
        });
        card.setOnMouseExited(e -> {
            card.setStyle(card.getStyle() +
                    "-fx-border-color: rgba(51, 65, 85, 0.5); -fx-scale-x: 1.0; -fx-scale-y: 1.0;");
        });

        return card;
    }

    /**
     * Create ongoing exam card
     */
    private VBox createOngoingExamCard(StudentExamAttempt attempt) {
        VBox card = new VBox(12);
        card.setPrefWidth(900);
        card.setPadding(new Insets(20));
        card.setStyle("""
            -fx-background-color: rgba(30, 41, 59, 0.7);
            -fx-background-radius: 12;
            -fx-border-color: rgba(251, 191, 36, 0.5);
            -fx-border-width: 2;
            -fx-border-radius: 12;
            """);

        Label title = new Label("⏳ " + attempt.getExamTitle());
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        Label status = new Label("Status: ONGOING");
        status.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 14px;");

        Button resumeBtn = new Button("▶️ Resume Exam");
        resumeBtn.setStyle("""
            -fx-background-color: #f59e0b;
            -fx-text-fill: #0f172a;
            -fx-font-size: 14px;
            -fx-font-weight: 600;
            -fx-padding: 10 24;
            -fx-background-radius: 8;
            -fx-cursor: hand;
            """);

        card.getChildren().addAll(title, status, resumeBtn);

        return card;
    }

    /**
     * Create completed exam card
     */
    private VBox createCompletedExamCard(StudentExamAttempt attempt) {
        VBox card = new VBox(12);
        card.setPrefWidth(900);
        card.setPadding(new Insets(20));
        card.setStyle("""
            -fx-background-color: rgba(30, 41, 59, 0.7);
            -fx-background-radius: 12;
            -fx-border-color: rgba(51, 65, 85, 0.5);
            -fx-border-width: 1;
            -fx-border-radius: 12;
            """);

        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(5);
        Label title = new Label(attempt.getExamTitle());
        title.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label subject = new Label("📚 " + attempt.getSubject());
        subject.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");

        titleBox.getChildren().addAll(title, subject);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox scoreBox = new VBox(5);
        scoreBox.setAlignment(Pos.CENTER_RIGHT);

        Label score = new Label(String.format("%.1f%%", attempt.getPercentage()));
        score.setStyle(String.format("-fx-text-fill: %s; -fx-font-size: 24px; -fx-font-weight: bold;",
                attempt.getResultColor()));

        Label result = new Label(attempt.getResultBadge() + " " + attempt.getResult());
        result.setStyle(String.format("-fx-text-fill: %s; -fx-font-size: 14px;",
                attempt.getResultColor()));

        scoreBox.getChildren().addAll(score, result);

        header.getChildren().addAll(titleBox, spacer, scoreBox);

        // Stats
        HBox stats = new HBox(30);
        stats.setAlignment(Pos.CENTER_LEFT);

        Label marksLabel = new Label(String.format("Score: %d/%d",
                attempt.getObtainedMarks(), attempt.getTotalMarks()));
        marksLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");

        Label accuracyLabel = new Label(String.format("Accuracy: %.1f%%", attempt.getAccuracy()));
        accuracyLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");

        stats.getChildren().addAll(marksLabel, accuracyLabel);

        Button viewBtn = new Button("📊 View Details");
        viewBtn.setStyle("""
            -fx-background-color: rgba(34, 211, 238, 0.2);
            -fx-text-fill: #22d3ee;
            -fx-font-size: 13px;
            -fx-font-weight: 600;
            -fx-padding: 8 16;
            -fx-background-radius: 6;
            -fx-cursor: hand;
            -fx-border-color: #22d3ee;
            -fx-border-width: 1;
            -fx-border-radius: 6;
            """);

        card.getChildren().addAll(header, stats, viewBtn);

        return card;
    }

    /**
     * Add info row to grid
     */
    private void addInfoRow(GridPane grid, int row, String icon, String label, String value) {
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 14px;");

        Label textLabel = new Label(label + ":");
        textLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 13px; -fx-font-weight: 600;");

        HBox row1 = new HBox(5, iconLabel, textLabel);
        row1.setAlignment(Pos.CENTER_LEFT);

        grid.add(row1, 0, row);
        grid.add(valueLabel, 1, row);
    }

    /**
     * Handle start exam
     */
    private void handleStartExam(Exam exam) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Start Exam");
        confirm.setHeaderText("Start " + exam.getExamTitle() + "?");
        confirm.setContentText(String.format(
                "Duration: %s\nQuestions: %d\nTotal Marks: %d\n\nAre you ready?",
                exam.getFormattedDuration(),
                exam.getTotalQuestions(),
                exam.getTotalMarks()
        ));

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Start exam attempt
                int attemptId = examService.startExamAttempt(currentUser.getId(), exam.getExamId());

                if (attemptId > 0) {
                    System.out.println("✅ Starting exam - Attempt ID: " + attemptId);

                    // Store data in session for ExamController
                    SessionManager.getInstance().setAttribute("attemptId", attemptId);
                    SessionManager.getInstance().setAttribute("examId", exam.getExamId());

                    // Navigate to exam screen
                    SceneManager.switchScene("/com/examverse/fxml/exam/exam-taking.fxml");
                } else {
                    Alert error = new Alert(Alert.AlertType.ERROR);
                    error.setContentText("Failed to start exam. Please try again.");
                    error.show();
                }
            }
        });
    }

    /**
     * Show coming soon message
     */
    private void showComingSoon(String feature) {
        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(100));
        content.setStyle("-fx-background-color: transparent;");

        Label icon = new Label("🚧");
        icon.setStyle("-fx-font-size: 64px;");

        Label title = new Label(feature + " - Coming Soon!");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

        Label message = new Label("This feature is under development");
        message.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 16px;");

        content.getChildren().addAll(icon, title, message);

        setContentWithAnimation(content);
    }

    /**
     * Set content with fade-in animation
     */
    private void setContentWithAnimation(VBox content) {
        content.setOpacity(0);
        contentPane.getChildren().clear();
        contentPane.getChildren().add(content);

        FadeTransition fade = new FadeTransition(Duration.millis(300), content);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }
}