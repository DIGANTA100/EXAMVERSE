package com.examverse.controller.dashboard;

import com.examverse.controller.dashboard.sections.*;
import com.examverse.model.exam.Exam;
import com.examverse.model.exam.Question;
import com.examverse.model.exam.StudentExamAttempt;
import com.examverse.model.user.User;
import com.examverse.service.ai.GeminiService;
import com.examverse.service.dashboard.NotificationService;
import com.examverse.service.exam.AnswerService;
import com.examverse.service.exam.ExamService;
import com.examverse.service.exam.QuestionService;
import com.examverse.util.SceneManager;
import com.examverse.util.SessionManager;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import com.examverse.controller.forum.DiscussionForumController;
import javafx.fxml.FXMLLoader;
/**
 * StudentDashboardController — Refactored slim orchestrator.
 *
 * Feature logic has been extracted to dedicated section classes:
 *   DashboardHomeSection   — dashboard home panel
 *   ProfileSection         — profile + StudentRating display
 *   NotificationPanel      — DB-backed notification popup
 *   DashboardUIFactory     — shared UI helpers
 *
 * This class only:
 *   1. Wires FXML injections
 *   2. Calls section builders
 *   3. Handles sidebar navigation & active-state
 *   4. Manages the background image + particle layer
 *   5. Keeps exam actions (start, resume) because they need session writes
 *   6. Keeps AI assistant (heavy Gemini chat state)
 *   7. Keeps Results & Practice (standalone, reasonable length)
 */
public class StudentDashboardController implements Initializable {

    // ── FXML injections ──────────────────────────────────────────────────────

    @FXML private BorderPane rootPane;
    @FXML private VBox       sidebarPane;
    @FXML private VBox       contentPane;

    @FXML private Button dashboardBtn;
    @FXML private Button myExamsBtn;
    @FXML private Button practiceBtn;
    @FXML private Button resultsBtn;
    @FXML private Button profileBtn;
    @FXML private Button logoutBtn;
    @FXML private Button aiAssistantBtn;
    @FXML private Button contestsBtn;

    @FXML private Label  welcomeLabel;
    @FXML private Label  dateTimeLabel;
    @FXML private Label  notificationBadge;
    @FXML private Button notificationBtn;
    @FXML private Button avatarBtn;

    @FXML private ScrollPane contentScrollPane;
    @FXML private Button forumBtn;

    // ── Services ─────────────────────────────────────────────────────────────

    private ExamService     examService;
    private QuestionService questionService;
    private AnswerService   answerService;
    private GeminiService   geminiService;
    private NotificationService notificationService;

    // ── State ─────────────────────────────────────────────────────────────────

    private User    currentUser;
    private Timeline clockTimeline;

    // ── Section helpers ───────────────────────────────────────────────────────

    private NotificationPanel notificationPanel;

    // ── AI chat state ─────────────────────────────────────────────────────────

    private VBox       chatMessagesBox;
    private ScrollPane chatScrollPane;
    private TextField  chatInputField;
    private Button     chatSendBtn;

    // ─────────────────────────────────────────────────────────────────────────
    //  INITIALIZE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        examService         = new ExamService();
        questionService     = new QuestionService();
        answerService       = new AnswerService();
        geminiService       = new GeminiService();
        notificationService = new NotificationService();

        // Ensure notification tables exist
        NotificationService.ensureTablesExist();

        currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            System.err.println("❌ No user in session — redirecting to login");
            SceneManager.switchScene("/com/examverse/fxml/auth/login.fxml");
            return;
        }

        setupBackground();
        setupHeader();
        setActiveButton(dashboardBtn);
        loadDashboardHome();

        System.out.println("✅ Dashboard loaded for: " + currentUser.getFullName());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  BACKGROUND  — fixed image + particle overlay
    // ─────────────────────────────────────────────────────────────────────────

    private void setupBackground() {
        /*
         * Strategy: place a full-size ImageView at index 0 inside the BorderPane's
         * children list so it sits behind all UI. The rootPane keeps its CSS
         * background-color as a dark fallback that shows through when no image
         * is present. Using an ImageView (rather than BackgroundImage or setStyle)
         * means the CSS gradient overlay we apply in-code won't erase the image.
         *
         * ➤ Name your background image  dashboard-bg.jpg  (or .png) and place it at:
         *     src/main/resources/com/examverse/assets/images/dashboard-bg.jpg
         */

        // 1. Set a solid dark fallback on rootPane (CSS file says transparent — override here)
        rootPane.setStyle(
                "-fx-background-color: linear-gradient(" +
                        "to bottom right,#020617 0%,#0f172a 40%,#1a1040 100%);"
        );

        // 2. Try loading the bg image; if found, wire it as a fixed ImageView
        try {
            URL imgUrl = getClass().getResource("/com/examverse/assets/images/dashboard-bg.jpg");
            if (imgUrl == null)
                imgUrl = getClass().getResource("/com/examverse/assets/images/dashboard-bg.png");

            if (imgUrl != null) {
                Image bgImage = new Image(imgUrl.toExternalForm(), true); // background loading

                javafx.scene.image.ImageView bgView = new javafx.scene.image.ImageView(bgImage);
                bgView.setPreserveRatio(false);
                bgView.setSmooth(true);

                // Bind size to rootPane so it always fills and stays fixed on scroll
                bgView.fitWidthProperty().bind(rootPane.widthProperty());
                bgView.fitHeightProperty().bind(rootPane.heightProperty());

                // Dark gradient overlay ON TOP of the image (semi-transparent Pane)
                Pane overlay = new Pane();
                overlay.setMouseTransparent(true);
                overlay.setStyle(
                        "-fx-background-color: linear-gradient(" +
                                "to bottom right," +
                                "#020617d1 0%," +   // #RRGGBBAA — ~82% opacity
                                "#0f172ac7 40%," +  // ~78% opacity
                                "#1a1040bf 100%);"  // ~75% opacity
                );
                overlay.prefWidthProperty().bind(rootPane.widthProperty());
                overlay.prefHeightProperty().bind(rootPane.heightProperty());

                // Insert bg image then overlay at the very bottom of the children stack
                rootPane.getChildren().add(0, overlay);
                rootPane.getChildren().add(0, bgView);

                // Make the pane itself transparent so the ImageView shows through
                rootPane.setStyle("-fx-background-color: transparent;");

                System.out.println("✅ Dashboard background image loaded.");
            } else {
                System.out.println("ℹ️ No dashboard-bg image found — using gradient fallback.");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Background image load failed: " + e.getMessage());
        }

        // Animated particle layer (always on top of background, behind UI)
        Pane particleLayer = new Pane();
        particleLayer.setMouseTransparent(true);
        particleLayer.setStyle("-fx-background-color: transparent;");

        String[] particleColors = {"#22d3ee", "#a78bfa", "#e879f9", "#34d399"};
        for (int i = 0; i < 30; i++) {
            double r     = 1.2 + Math.random() * 2.8;
            Circle p     = new Circle(r);
            String col   = particleColors[i % particleColors.length];
            double alpha = 0.10 + Math.random() * 0.22;
            p.setFill(Color.web(col, alpha));
            p.setLayoutX(Math.random() * 1440);
            p.setLayoutY(Math.random() * 900);

            TranslateTransition tt = new TranslateTransition(
                    Duration.seconds(9 + Math.random() * 13), p);
            tt.setByY(-(50 + Math.random() * 90));
            tt.setByX(-20 + Math.random() * 40);
            tt.setCycleCount(Animation.INDEFINITE);
            tt.setAutoReverse(true);
            tt.setInterpolator(Interpolator.EASE_BOTH);
            tt.play();

            FadeTransition ft = new FadeTransition(
                    Duration.seconds(2.5 + Math.random() * 3.5), p);
            ft.setFromValue(0.05);
            ft.setToValue(0.45);
            ft.setCycleCount(Animation.INDEFINITE);
            ft.setAutoReverse(true);
            ft.play();

            particleLayer.getChildren().add(p);
        }
        rootPane.getChildren().add(0, particleLayer);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HEADER SETUP
    // ─────────────────────────────────────────────────────────────────────────

    private void setupHeader() {
        welcomeLabel.setText("Welcome back, " + currentUser.getFullName() + " 👋");

        updateDateTime();
        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateDateTime()));
        clockTimeline.setCycleCount(Timeline.INDEFINITE);
        clockTimeline.play();

        // Notification panel — pass rootPane as anchor node (panel resolves scene root itself)
        notificationPanel = new NotificationPanel(
                currentUser,
                notificationBadge,
                rootPane,
                count -> {}
        );
        notificationPanel.refreshBadge();

        if (notificationBtn != null) {
            notificationBtn.setOnAction(e -> notificationPanel.toggle());
        }

        // Avatar button — initials + rank-matching color loaded from DB
        if (avatarBtn != null) {
            avatarBtn.setText(getInitials(currentUser.getFullName()));
            avatarBtn.setOnAction(e -> { setActiveButton(profileBtn); loadProfile(); });

            // Load rank color off the UI thread so startup isn't blocked
            Task<String> rankColorTask = new Task<>() {
                @Override protected String call() {
                    try (java.sql.Connection conn = com.examverse.config.DatabaseConfig.getConnection();
                         java.sql.PreparedStatement ps = conn.prepareStatement(
                                 "SELECT current_rating FROM student_ratings WHERE student_id = ?")) {
                        ps.setInt(1, currentUser.getId());
                        java.sql.ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                            return com.examverse.controller.dashboard.sections.ProfileSection
                                    .getRankColor(rs.getInt(1));
                        }
                    } catch (Exception ignored) {}
                    return "#6b7280"; // Beginner default
                }
            };
            rankColorTask.setOnSucceeded(e -> {
                String c = rankColorTask.getValue();
                avatarBtn.setStyle(
                        "-fx-background-color: linear-gradient(to bottom," + c + "cc," + c + "88);" +
                                "-fx-text-fill: #0f172a;" +
                                "-fx-font-size: 13px; -fx-font-weight: 800;" +
                                "-fx-padding: 9 13; -fx-background-radius: 50%; -fx-cursor: hand;" +
                                "-fx-border-color: " + c + "99; -fx-border-width: 2; -fx-border-radius: 50%;" +
                                "-fx-effect: dropshadow(gaussian," + c + "66,10,0.4,0,0);" +
                                "-fx-min-width: 40; -fx-min-height: 40;"
                );
            });
            Thread t = new Thread(rankColorTask, "rank-color-loader");
            t.setDaemon(true);
            t.start();
        }
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] p = name.trim().split("\\s+");
        if (p.length == 1) return p[0].substring(0, Math.min(2, p[0].length())).toUpperCase();
        return (p[0].charAt(0) + "" + p[p.length - 1].charAt(0)).toUpperCase();
    }

    private void updateDateTime() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE, MMM dd yyyy  •  hh:mm:ss a");
        dateTimeLabel.setText(LocalDateTime.now().format(fmt));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  NAVIGATION HANDLERS
    // ─────────────────────────────────────────────────────────────────────────

    @FXML private void handleDashboard()    { setActiveButton(dashboardBtn);    loadDashboardHome(); }
    @FXML private void handleMyExams()      { setActiveButton(myExamsBtn);      loadMyExams(); }
    @FXML private void handlePractice()     { setActiveButton(practiceBtn);     loadPracticeMode(); }
    @FXML private void handleResults()      { setActiveButton(resultsBtn);      loadResultsAnalytics(); }
    @FXML private void handleProfile()      { setActiveButton(profileBtn);      loadProfile(); }
    @FXML private void handleAiAssistant()  { setActiveButton(aiAssistantBtn);  loadAiAssistant(); }
    @FXML private void handleNotification() { if (notificationPanel != null) notificationPanel.toggle(); }

    @FXML private void handleContests() {
        setActiveButton(contestsBtn);
        SceneManager.switchScene("/com/examverse/fxml/contest/contest-lobby.fxml");
    }

    @FXML private void handleLogout() {
        if (clockTimeline != null) clockTimeline.stop();
        if (notificationPanel != null) notificationPanel.close();

        Alert dlg = styledAlert(Alert.AlertType.CONFIRMATION,
                "Logout", "Are you sure you want to logout?",
                "Your session will be cleared.");
        dlg.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                if (activeForum != null) activeForum.stopPolling();
                SessionManager.getInstance().clearSession();
                SceneManager.switchScene("/com/examverse/fxml/auth/login.fxml");
            }
        });
    }
    @FXML
    private void handleForum() {
        setActiveButton(forumBtn);
        loadForum();
    }


    // ─────────────────────────────────────────────────────────────────────────
    //  ACTIVE BUTTON STATE
    // ─────────────────────────────────────────────────────────────────────────

    private void setActiveButton(Button active) {
        Button[] all = {dashboardBtn, myExamsBtn, practiceBtn, resultsBtn,
                profileBtn, aiAssistantBtn, contestsBtn, forumBtn};
        for (Button b : all) {
            if (b == null) continue;
            b.getStyleClass().removeAll("sidebar-btn-active");
            if (!b.getStyleClass().contains("sidebar-btn"))
                b.getStyleClass().add("sidebar-btn");
        }
        if (active != null && !active.getStyleClass().contains("sidebar-btn-active"))
            active.getStyleClass().add("sidebar-btn-active");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CONTENT ANIMATION
    // ─────────────────────────────────────────────────────────────────────────

    private void setContentWithAnimation(VBox content) {
        content.setOpacity(0);
        contentPane.getChildren().setAll(content);
        FadeTransition fade  = new FadeTransition(Duration.millis(250), content);
        fade.setFromValue(0); fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(250), content);
        slide.setFromY(14); slide.setToY(0);
        new ParallelTransition(fade, slide).play();
    }

    private DiscussionForumController activeForum; // keep reference to stop polling on nav away

    private void loadForum() {
        if (activeForum != null) {
            activeForum.stopPolling();
            activeForum = null;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/examverse/fxml/forum/discussion-forum.fxml"));
            // Root is now BorderPane — place it directly as center of rootPane
            // so it fills the full screen height (no ScrollPane wrapper)
            javafx.scene.layout.BorderPane forumView = loader.load();
            activeForum = loader.getController();

            // Bind width to the content-container area (exclude sidebar)
            rootPane.setCenter(forumView);

        } catch (Exception e) {
            System.err.println("❌ Failed to load discussion forum: " + e.getMessage());
            e.printStackTrace();
        }
    }


    // ═══════════════════════════════════════════════════════════════════════
    //  SECTION: DASHBOARD HOME  — delegated to DashboardHomeSection
    // ═══════════════════════════════════════════════════════════════════════

    private void loadDashboardHome() {
        DashboardHomeSection section = new DashboardHomeSection(
                examService, currentUser, contentScrollPane,
                this::handleStartExam,
                this::handleResumeExam,
                () -> { setActiveButton(myExamsBtn); loadMyExams(); },
                () -> { setActiveButton(myExamsBtn); loadMyExamsTab("ongoing"); },
                () -> { setActiveButton(practiceBtn); loadPracticeMode(); },
                () -> { setActiveButton(resultsBtn); loadResultsAnalytics(); },
                () -> { setActiveButton(contestsBtn); handleContests(); }
        );
        setContentWithAnimation(section.build());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SECTION: PROFILE  — delegated to ProfileSection
    // ═══════════════════════════════════════════════════════════════════════

    private void loadProfile() {
        ProfileSection section = new ProfileSection(examService, currentUser);
        setContentWithAnimation(section.build());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SECTION: MY EXAMS
    // ═══════════════════════════════════════════════════════════════════════

    private void loadMyExams() { loadMyExamsTab("available"); }

    private void loadMyExamsTab(String defaultTab) {
        VBox content = new VBox(24);
        content.setPadding(new Insets(32, 36, 32, 36));
        content.setStyle("-fx-background-color: transparent;");

        Label titleLabel = DashboardUIFactory.sectionTitle("📚  My Exams");

        Button availableTab = DashboardUIFactory.tabButton("📘  Available");
        Button ongoingTab   = DashboardUIFactory.tabButton("📕  Ongoing");
        Button completedTab = DashboardUIFactory.tabButton("📗  Completed");

        HBox tabNav = new HBox(10, availableTab, ongoingTab, completedTab);
        tabNav.setAlignment(Pos.CENTER_LEFT);

        VBox tabContent = new VBox(16);

        Runnable showAvailable = () -> {
            DashboardUIFactory.setTabActive(availableTab, ongoingTab, completedTab);
            tabContent.getChildren().setAll(createAvailableExamsTab());
        };
        Runnable showOngoing = () -> {
            DashboardUIFactory.setTabActive(ongoingTab, availableTab, completedTab);
            tabContent.getChildren().setAll(createOngoingExamsTab());
        };
        Runnable showCompleted = () -> {
            DashboardUIFactory.setTabActive(completedTab, availableTab, ongoingTab);
            tabContent.getChildren().setAll(createCompletedExamsTab());
        };

        availableTab.setOnAction(e -> showAvailable.run());
        ongoingTab.setOnAction(e   -> showOngoing.run());
        completedTab.setOnAction(e -> showCompleted.run());

        switch (defaultTab) {
            case "ongoing"   -> showOngoing.run();
            case "completed" -> showCompleted.run();
            default          -> showAvailable.run();
        }

        content.getChildren().addAll(titleLabel, tabNav, tabContent);
        setContentWithAnimation(content);
    }

    private VBox createAvailableExamsTab() {
        VBox container = new VBox(16);

        HBox filtersRow = new HBox(14);
        filtersRow.setAlignment(Pos.CENTER_LEFT);
        Label filterLbl = new Label("🔍  Filter:");
        filterLbl.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 14px; -fx-font-weight: 600;");

        ComboBox<String> subjectFilter    = DashboardUIFactory.styledCombo();
        ComboBox<String> difficultyFilter = DashboardUIFactory.styledCombo();
        subjectFilter.getItems().add("All Subjects");
        subjectFilter.getItems().addAll(examService.getAllSubjects());
        subjectFilter.setValue("All Subjects");
        difficultyFilter.getItems().addAll("All Levels", "EASY", "MEDIUM", "HARD");
        difficultyFilter.setValue("All Levels");
        filtersRow.getChildren().addAll(filterLbl, subjectFilter, difficultyFilter);

        FlowPane examGrid = new FlowPane();
        examGrid.setHgap(20); examGrid.setVgap(20);
        examGrid.setAlignment(Pos.TOP_LEFT);
        examGrid.setColumnHalignment(HPos.LEFT);
        examGrid.setMaxWidth(Double.MAX_VALUE);
        examGrid.setPrefWrapLength(900);
        contentScrollPane.widthProperty().addListener((o, ov, nv) ->
                examGrid.setPrefWrapLength(nv.doubleValue() - 72));
        if (contentScrollPane.getWidth() > 0)
            examGrid.setPrefWrapLength(contentScrollPane.getWidth() - 72);

        List<Exam> allExams = examService.getAllActiveExams();
        Runnable renderGrid = () -> {
            examGrid.getChildren().clear();
            String selSubject    = subjectFilter.getValue();
            String selDifficulty = difficultyFilter.getValue();
            List<Exam> filtered  = allExams.stream()
                    .filter(ex -> "All Subjects".equals(selSubject)  || selSubject.equalsIgnoreCase(ex.getSubject()))
                    .filter(ex -> "All Levels".equals(selDifficulty) || selDifficulty.equalsIgnoreCase(ex.getDifficulty()))
                    .collect(Collectors.toList());
            if (filtered.isEmpty()) {
                Label none = new Label("📭  No exams match the selected filters.");
                none.setStyle("-fx-text-fill: #64748b; -fx-font-size: 15px;");
                examGrid.getChildren().add(none);
            } else {
                filtered.forEach(ex -> examGrid.getChildren().add(createExamCard(ex)));
            }
        };
        subjectFilter.setOnAction(e    -> renderGrid.run());
        difficultyFilter.setOnAction(e -> renderGrid.run());
        renderGrid.run();

        container.getChildren().addAll(filtersRow, examGrid);
        return container;
    }

    private VBox createOngoingExamsTab() {
        VBox container = new VBox(14);
        List<StudentExamAttempt> ongoing = examService.getOngoingExams(currentUser.getId());
        if (ongoing.isEmpty()) {
            Label none = new Label("📭  No ongoing exams. Start a new exam!");
            none.setStyle("-fx-text-fill: #64748b; -fx-font-size: 15px;");
            container.getChildren().add(none);
        } else {
            ongoing.forEach(a -> container.getChildren().add(createOngoingExamCard(a)));
        }
        return container;
    }

    private VBox createCompletedExamsTab() {
        VBox container = new VBox(14);
        List<StudentExamAttempt> completed = examService.getCompletedExams(currentUser.getId());
        if (completed.isEmpty()) {
            Label none = new Label("📭  No completed exams yet.");
            none.setStyle("-fx-text-fill: #64748b; -fx-font-size: 15px;");
            container.getChildren().add(none);
        } else {
            completed.forEach(a -> container.getChildren().add(createCompletedExamCard(a)));
        }
        return container;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  EXAM CARDS
    // ═══════════════════════════════════════════════════════════════════════

    private VBox createExamCard(Exam exam) {
        VBox card = new VBox(14);
        card.setPrefWidth(440); card.setMaxWidth(520); card.setMinWidth(300);
        card.setPadding(new Insets(22));
        String base = """
            -fx-background-color: rgba(15,22,40,0.80);
            -fx-background-radius: 16;
            -fx-border-color: rgba(51,65,85,0.5);
            -fx-border-width: 1; -fx-border-radius: 16;
            -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.5),16,0.3,0,4);
            """;
        card.setStyle(base);

        Rectangle topBar = new Rectangle();
        topBar.setHeight(3); topBar.setArcWidth(6); topBar.setArcHeight(6);
        topBar.setFill(Color.web(exam.getDifficultyColor()));
        topBar.widthProperty().bind(card.widthProperty().subtract(44));

        Label title = new Label(exam.getExamTitle());
        title.setStyle("-fx-text-fill: white; -fx-font-size: 17px; -fx-font-weight: 700;");
        title.setWrapText(true);

        HBox badges = new HBox(8);
        badges.setAlignment(Pos.CENTER_LEFT);
        badges.getChildren().addAll(
                DashboardUIFactory.badge("📚 " + exam.getSubject(), "rgba(34,211,238,0.15)", "#22d3ee"),
                DashboardUIFactory.badge(exam.getDifficultyBadge() + " " + exam.getDifficulty(),
                        "rgba(255,255,255,0.06)", exam.getDifficultyColor())
        );

        Label desc = new Label(exam.getDescription() != null ? exam.getDescription() : "");
        desc.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");
        desc.setWrapText(true); desc.setMaxHeight(36);

        HBox infoRow = new HBox(22);
        infoRow.setAlignment(Pos.CENTER_LEFT);
        infoRow.getChildren().addAll(
                DashboardUIFactory.infoChip("📝", exam.getTotalQuestions() + " Qs"),
                DashboardUIFactory.infoChip("⏱", exam.getFormattedDuration()),
                DashboardUIFactory.infoChip("🏆", exam.getTotalMarks() + " marks"),
                DashboardUIFactory.infoChip("✅", exam.getPassingMarks() + " to pass")
        );

        Button startBtn = new Button("🚀  Start Exam");
        startBtn.setMaxWidth(Double.MAX_VALUE);
        String btnBase = """
            -fx-background-color: linear-gradient(90deg,#22d3ee,#06b6d4);
            -fx-text-fill: #0f172a; -fx-font-size: 14px; -fx-font-weight: 700;
            -fx-padding: 12 0; -fx-background-radius: 9; -fx-cursor: hand;
            -fx-effect: dropshadow(gaussian,rgba(34,211,238,0.45),14,0.4,0,2);
            """;
        startBtn.setStyle(btnBase);
        startBtn.setOnAction(e -> handleStartExam(exam));
        startBtn.setOnMouseEntered(ev -> startBtn.setStyle(btnBase
                .replace("linear-gradient(90deg,#22d3ee,#06b6d4)",
                        "linear-gradient(90deg,#06b6d4,#0891b2)")));
        startBtn.setOnMouseExited(ev -> startBtn.setStyle(btnBase));

        card.getChildren().addAll(topBar, title, badges, desc, infoRow, startBtn);
        card.setOnMouseEntered(ev -> card.setStyle("""
            -fx-background-color: rgba(34,211,238,0.07);
            -fx-background-radius: 16;
            -fx-border-color: rgba(34,211,238,0.55);
            -fx-border-width: 1; -fx-border-radius: 16;
            -fx-effect: dropshadow(gaussian,rgba(34,211,238,0.25),20,0.4,0,4);
            """));
        card.setOnMouseExited(ev -> card.setStyle(base));
        return card;
    }

    private VBox createOngoingExamCard(StudentExamAttempt attempt) {
        VBox card = new VBox(14);
        card.setPadding(new Insets(22));
        String base = """
            -fx-background-color: rgba(15,22,40,0.80);
            -fx-background-radius: 16;
            -fx-border-color: rgba(245,158,11,0.5);
            -fx-border-width: 2; -fx-border-radius: 16;
            -fx-effect: dropshadow(gaussian,rgba(245,158,11,0.15),14,0.3,0,3);
            """;
        card.setStyle(base);

        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        Label pulse = new Label("⏳"); pulse.setStyle("-fx-font-size: 24px;");
        VBox titleBox = new VBox(4);
        Label title = new Label(attempt.getExamTitle());
        title.setStyle("-fx-text-fill: white; -fx-font-size: 17px; -fx-font-weight: 700;");
        Label sub = new Label(attempt.getSubject() != null ? "📚 " + attempt.getSubject() : "");
        sub.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
        titleBox.getChildren().addAll(title, sub);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label statusBadge = new Label("● ONGOING");
        statusBadge.setStyle("""
            -fx-background-color: rgba(245,158,11,0.18); -fx-text-fill: #f59e0b;
            -fx-padding: 5 14; -fx-background-radius: 20;
            -fx-font-size: 12px; -fx-font-weight: 700;
            """);
        header.getChildren().addAll(pulse, titleBox, sp, statusBadge);

        int answered = answerService.getAnsweredCount(attempt.getAttemptId());
        HBox info = new HBox(24);
        info.setAlignment(Pos.CENTER_LEFT);
        info.getChildren().addAll(
                DashboardUIFactory.infoChip("📝", answered + "/" + attempt.getTotalQuestions() + " answered"),
                DashboardUIFactory.infoChip("🏆", attempt.getTotalMarks() + " marks")
        );

        Button resumeBtn = new Button("▶️  Resume Exam");
        String rbBase = """
            -fx-background-color: linear-gradient(90deg,#f59e0b,#d97706);
            -fx-text-fill: #0f172a; -fx-font-size: 14px; -fx-font-weight: 700;
            -fx-padding: 11 28; -fx-background-radius: 9; -fx-cursor: hand;
            -fx-effect: dropshadow(gaussian,rgba(245,158,11,0.4),12,0.4,0,2);
            """;
        resumeBtn.setStyle(rbBase);
        resumeBtn.setOnAction(e -> handleResumeExam(attempt));
        resumeBtn.setOnMouseEntered(ev -> resumeBtn.setStyle(rbBase
                .replace("linear-gradient(90deg,#f59e0b,#d97706)",
                        "linear-gradient(90deg,#d97706,#b45309)")));
        resumeBtn.setOnMouseExited(ev -> resumeBtn.setStyle(rbBase));

        card.getChildren().addAll(header, info, resumeBtn);
        return card;
    }

    private VBox createCompletedExamCard(StudentExamAttempt attempt) {
        VBox card = new VBox(14);
        card.setPadding(new Insets(22));
        boolean passed    = "PASSED".equalsIgnoreCase(attempt.getResult());
        String borderColor= passed ? "rgba(34,197,94,0.4)" : "rgba(239,68,68,0.35)";
        String base = "-fx-background-color: rgba(15,22,40,0.80);"
                + " -fx-background-radius: 16;"
                + " -fx-border-color: " + borderColor + ";"
                + " -fx-border-width: 1; -fx-border-radius: 16;";
        card.setStyle(base);

        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox titleBox = new VBox(4);
        Label title = new Label(attempt.getExamTitle());
        title.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: 700;");
        Label sub = new Label(attempt.getSubject() != null ? "📚 " + attempt.getSubject() : "");
        sub.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
        titleBox.getChildren().addAll(title, sub);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        VBox scoreBox = new VBox(3);
        scoreBox.setAlignment(Pos.CENTER_RIGHT);
        Label pct = new Label(String.format("%.1f%%", attempt.getPercentage()));
        pct.setStyle("-fx-text-fill: " + attempt.getResultColor()
                + "; -fx-font-size: 26px; -fx-font-weight: 800;");
        Label res = new Label(passed ? "✅ PASSED" : "❌ FAILED");
        res.setStyle("-fx-text-fill: " + attempt.getResultColor()
                + "; -fx-font-size: 13px; -fx-font-weight: 600;");
        scoreBox.getChildren().addAll(pct, res);
        header.getChildren().addAll(titleBox, sp, scoreBox);

        HBox stats = new HBox(28);
        stats.setAlignment(Pos.CENTER_LEFT);
        stats.getChildren().addAll(
                DashboardUIFactory.infoChip("🏆", attempt.getObtainedMarks() + "/" + attempt.getTotalMarks()),
                DashboardUIFactory.infoChip("🎯", String.format("%.1f%% accuracy", attempt.getAccuracy())),
                DashboardUIFactory.infoChip("⏱", attempt.getTimeSpentMinutes() + " min")
        );

        Button viewBtn = new Button("📊  View Details");
        viewBtn.setStyle("""
            -fx-background-color: rgba(34,211,238,0.10); -fx-text-fill: #22d3ee;
            -fx-font-size: 13px; -fx-font-weight: 600; -fx-padding: 9 18;
            -fx-background-radius: 7; -fx-cursor: hand;
            -fx-border-color: rgba(34,211,238,0.4); -fx-border-width: 1; -fx-border-radius: 7;
            """);
        viewBtn.setOnAction(e -> {
            SessionManager.getInstance().setAttribute("attemptId", (Integer) attempt.getAttemptId());
            SceneManager.switchScene("/com/examverse/fxml/exam/exam-result.fxml");
        });

        card.getChildren().addAll(header, stats, viewBtn);
        return card;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SECTION: RESULTS & ANALYTICS
    // ═══════════════════════════════════════════════════════════════════════

    private void loadResultsAnalytics() {
        VBox content = new VBox(28);
        content.setPadding(new Insets(32, 36, 32, 36));
        content.setStyle("-fx-background-color: transparent;");

        Label title = DashboardUIFactory.sectionTitle("📊  Results & Analytics");
        var stats   = examService.getStudentStats(currentUser.getId());

        HBox statsRow = new HBox(20);
        statsRow.getChildren().addAll(
                DashboardUIFactory.statCard("📝", "Total Attempted", String.valueOf(stats.getTotalExamsAttempted()), "#22d3ee", "#0ea5e9"),
                DashboardUIFactory.statCard("✅", "Passed",          String.valueOf(stats.getTotalExamsPassed()),   "#22c55e", "#16a34a"),
                DashboardUIFactory.statCard("❌", "Failed",          String.valueOf(stats.getTotalExamsFailed()),   "#ef4444", "#dc2626"),
                DashboardUIFactory.statCard("📈", "Pass Rate",       String.format("%.1f%%", stats.getPassRate()), "#a78bfa", "#7c3aed")
        );

        Label perfTitle = DashboardUIFactory.sectionSubtitle("📈  Performance Overview");
        VBox perfCard   = DashboardUIFactory.glassCard();
        addAnalyticsRow(perfCard, "Average Score",    stats.getAverageScore(),    "#22d3ee");
        addAnalyticsRow(perfCard, "Overall Accuracy", stats.getOverallAccuracy(), "#10b981");
        addAnalyticsRow(perfCard, "Pass Rate",        stats.getPassRate(),        "#a78bfa");

        Label tableTitle = DashboardUIFactory.sectionSubtitle("📋  Recent Exam Results");
        VBox tableBox = new VBox(0);
        tableBox.setStyle("""
            -fx-background-color: rgba(10,17,32,0.8);
            -fx-background-radius: 12;
            -fx-border-color: rgba(51,65,85,0.4);
            -fx-border-width: 1; -fx-border-radius: 12;
            """);

        HBox tableHeader = new HBox();
        tableHeader.setPadding(new Insets(12, 20, 12, 20));
        tableHeader.setStyle("-fx-background-color: rgba(30,41,59,0.7); -fx-background-radius: 12 12 0 0;");
        for (String col : new String[]{"Exam", "Subject", "Score", "Accuracy", "Result"}) {
            Label h = new Label(col);
            h.setStyle("-fx-text-fill: #475569; -fx-font-size: 12px; -fx-font-weight: 700;");
            HBox.setHgrow(h, Priority.ALWAYS); h.setMaxWidth(Double.MAX_VALUE);
            tableHeader.getChildren().add(h);
        }
        tableBox.getChildren().add(tableHeader);

        List<StudentExamAttempt> results = examService.getCompletedExams(currentUser.getId());
        if (results.isEmpty()) {
            Label empty = new Label("  No completed exams yet.");
            empty.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px; -fx-padding: 20;");
            tableBox.getChildren().add(empty);
        } else {
            for (int i = 0; i < results.size(); i++) {
                StudentExamAttempt a = results.get(i);
                HBox row = new HBox();
                row.setPadding(new Insets(13, 20, 13, 20));
                row.setAlignment(Pos.CENTER_LEFT);
                if (i % 2 == 0) row.setStyle("-fx-background-color: rgba(30,41,59,0.25);");
                Label[] cells = {
                        new Label(a.getExamTitle()),
                        new Label(a.getSubject() != null ? a.getSubject() : "-"),
                        new Label(a.getObtainedMarks() + "/" + a.getTotalMarks()),
                        new Label(String.format("%.1f%%", a.getAccuracy())),
                        new Label(a.getResult() != null ? a.getResult() : "PENDING")
                };
                for (int j = 0; j < cells.length; j++) {
                    Label c = cells[j];
                    c.setStyle("-fx-text-fill: " + (j == 4 ? a.getResultColor() : "#cbd5e1")
                            + "; -fx-font-size: 13px;" + (j == 4 ? " -fx-font-weight: 700;" : ""));
                    HBox.setHgrow(c, Priority.ALWAYS); c.setMaxWidth(Double.MAX_VALUE);
                    row.getChildren().add(c);
                }
                tableBox.getChildren().add(row);
            }
        }

        content.getChildren().addAll(title, statsRow,
                DashboardUIFactory.divider(), perfTitle, perfCard,
                DashboardUIFactory.divider(), tableTitle, tableBox);
        setContentWithAnimation(content);
    }

    private void addAnalyticsRow(VBox parent, String label, double value, String color) {
        VBox row = new VBox(6);
        row.setPadding(new Insets(8, 0, 8, 0));
        HBox labelRow = new HBox();
        labelRow.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label valLbl = new Label(String.format("%.1f%%", value));
        valLbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 13px; -fx-font-weight: 700;");
        labelRow.getChildren().addAll(lbl, sp, valLbl);
        ProgressBar pb = new ProgressBar(Math.min(value / 100.0, 1.0));
        pb.setMaxWidth(Double.MAX_VALUE); pb.setPrefHeight(10);
        pb.setStyle("-fx-accent: " + color
                + "; -fx-background-color: rgba(51,65,85,0.4); -fx-background-radius: 5;");
        row.getChildren().addAll(labelRow, pb);
        parent.getChildren().add(row);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SECTION: PRACTICE MODE
    // ═══════════════════════════════════════════════════════════════════════

    private void loadPracticeMode() {
        VBox content = new VBox(28);
        content.setPadding(new Insets(32, 36, 32, 36));
        content.setStyle("-fx-background-color: transparent;");

        Label title    = DashboardUIFactory.sectionTitle("💪  Practice Mode");
        Label subtitle = new Label("Choose a subject to start an un-timed practice session.");
        subtitle.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");
        subtitle.setWrapText(true);

        Label selectLbl = DashboardUIFactory.sectionSubtitle("📚  Select Subject");

        FlowPane subjectGrid = new FlowPane();
        subjectGrid.setHgap(16); subjectGrid.setVgap(16);

        List<String> subjects = examService.getAllSubjects();
        String[] icons = {"⚡","🗄️","🌐","💻","🔬","🧮","📐","🔧"};
        String[] colors = {"#22d3ee","#a78bfa","#34d399","#f59e0b","#f43f5e","#60a5fa","#fb7185","#e879f9"};

        if (subjects.isEmpty()) {
            Label none = new Label("No subjects available.");
            none.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px;");
            subjectGrid.getChildren().add(none);
        } else {
            for (int i = 0; i < subjects.size(); i++) {
                String s    = subjects.get(i);
                String icon = icons[i % icons.length];
                String col  = colors[i % colors.length];

                VBox card = new VBox(10);
                card.setAlignment(Pos.CENTER);
                card.setPrefSize(180, 115);
                card.setPadding(new Insets(16));
                String cs = "-fx-background-color: rgba(15,22,40,0.8);"
                        + " -fx-background-radius: 14;"
                        + " -fx-border-color: rgba(51,65,85,0.5);"
                        + " -fx-border-width: 1; -fx-border-radius: 14; -fx-cursor: hand;";
                card.setStyle(cs);

                Label ic = new Label(icon); ic.setStyle("-fx-font-size: 28px;");
                Label sl = new Label(s);
                sl.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 13px; -fx-font-weight: 600;");
                sl.setWrapText(true); sl.setAlignment(Pos.CENTER);
                card.getChildren().addAll(ic, sl);

                String hoverCs = "-fx-background-color: " + col + "1a;"
                        + " -fx-background-radius: 14;"
                        + " -fx-border-color: " + col + "99;"
                        + " -fx-border-width: 1; -fx-border-radius: 14; -fx-cursor: hand;"
                        + " -fx-effect: dropshadow(gaussian," + col + "44,14,0.4,0,3);";

                card.setOnMouseEntered(e -> card.setStyle(hoverCs));
                card.setOnMouseExited(e  -> card.setStyle(cs));
                card.setOnMouseClicked(e -> startPracticeBySubject(s));
                subjectGrid.getChildren().add(card);
            }
        }

        Label howTitle = DashboardUIFactory.sectionSubtitle("ℹ️  How Practice Mode Works");
        VBox howBox = DashboardUIFactory.glassCard();
        for (String line : new String[]{
                "• 📖  Questions are shuffled each session",
                "• ⏱  No time limit — take your time to think",
                "• ✅  Correct answer is shown right after you answer",
                "• 💡  Explanations shown where available",
                "• 🔄  Re-attempt the same subject as many times as you like"
        }) {
            Label l = new Label(line);
            l.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
            l.setPadding(new Insets(3, 0, 3, 0));
            howBox.getChildren().add(l);
        }

        content.getChildren().addAll(title, subtitle,
                DashboardUIFactory.divider(), selectLbl, subjectGrid,
                DashboardUIFactory.divider(), howTitle, howBox);
        setContentWithAnimation(content);
    }

    private void startPracticeBySubject(String subject) {
        List<Exam> exams = examService.getExamsBySubject(subject);
        if (exams.isEmpty()) {
            showInfoAlert("No Exams Found",
                    "No available exams for subject: " + subject + ".");
            return;
        }
        Exam exam = exams.get(0);
        Alert confirm = styledAlert(Alert.AlertType.CONFIRMATION,
                "Start Practice",
                "Practice: " + exam.getExamTitle(),
                "Subject: " + subject + "\nQuestions: " + exam.getTotalQuestions()
                        + "\n\nThis is practice mode — no marks will be recorded.\nReady?");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                SessionManager.getInstance().setAttribute("practiceMode", true);
                int attemptId = examService.startExamAttempt(currentUser.getId(), exam.getExamId());
                if (attemptId > 0) {
                    SessionManager.getInstance().setAttribute("attemptId", attemptId);
                    SessionManager.getInstance().setAttribute("examId", exam.getExamId());
                    SceneManager.switchScene("/com/examverse/fxml/exam/exam-taking.fxml");
                } else {
                    showErrorAlert("Failed to start practice session.");
                }
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  EXAM ACTIONS
    // ═══════════════════════════════════════════════════════════════════════

    private void handleStartExam(Exam exam) {
        if (exam == null) { showErrorAlert("Exam data is missing."); return; }
        Alert confirm = styledAlert(Alert.AlertType.CONFIRMATION,
                "Start Exam", "📝  " + exam.getExamTitle(),
                String.format("Subject: %s\nDifficulty: %s\nDuration: %s\n"
                                + "Questions: %d\nTotal Marks: %d\nPassing Marks: %d\n\n"
                                + "⚠ The timer starts immediately. Ready?",
                        exam.getSubject(), exam.getDifficulty(), exam.getFormattedDuration(),
                        exam.getTotalQuestions(), exam.getTotalMarks(), exam.getPassingMarks()));

        confirm.showAndWait().ifPresent(response -> {
            if (response != ButtonType.OK) return;
            int attemptId = examService.startExamAttempt(currentUser.getId(), exam.getExamId());
            if (attemptId <= 0) { showErrorAlert("Failed to start exam."); return; }
            List<Question> qs = questionService.getQuestionsByExamId(exam.getExamId());
            if (qs == null || qs.isEmpty()) {
                showErrorAlert("This exam has no questions. Contact your administrator.");
                examService.deleteAttempt(attemptId);
                return;
            }
            SessionManager.getInstance().setAttribute("attemptId",    (Integer) attemptId);
            SessionManager.getInstance().setAttribute("examId",       (Integer) exam.getExamId());
            SessionManager.getInstance().setAttribute("practiceMode", false);
            SessionManager.getInstance().setAttribute("resumeMode",   false);
            SceneManager.switchScene("/com/examverse/fxml/exam/exam-taking.fxml");
        });
    }

    private void handleResumeExam(StudentExamAttempt attempt) {
        if (attempt == null) { showErrorAlert("Attempt data is missing."); return; }
        Alert confirm = styledAlert(Alert.AlertType.CONFIRMATION,
                "Resume Exam", "▶️  Resume: " + attempt.getExamTitle(),
                "Your previous answers will be preserved.\n\nContinue?");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                SessionManager.getInstance().setAttribute("attemptId",   (Integer) attempt.getAttemptId());
                SessionManager.getInstance().setAttribute("examId",      (Integer) attempt.getExamId());
                SessionManager.getInstance().setAttribute("practiceMode", false);
                SessionManager.getInstance().setAttribute("resumeMode",   true);
                SceneManager.switchScene("/com/examverse/fxml/exam/exam-taking.fxml");
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SECTION: AI ASSISTANT  (kept here — owns heavy Gemini chat state)
    // ═══════════════════════════════════════════════════════════════════════

    private void loadAiAssistant() {
        chatMessagesBox = null; chatScrollPane = null;
        chatInputField  = null; chatSendBtn    = null;

        VBox root = new VBox(0);
        root.setPadding(new Insets(30));

        HBox titleRow = new HBox(14);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(titleRow, new Insets(0, 0, 20, 0));

        Label icon = new Label("🤖"); icon.setStyle("-fx-font-size: 32px;");
        VBox titleText = new VBox(2);
        Label title = DashboardUIFactory.sectionTitle("AI Assistant");
        Label sub   = new Label("Powered by Gemini • Ask anything about your studies");
        sub.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");
        titleText.getChildren().addAll(title, sub);

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button newChatBtn = DashboardUIFactory.miniBtn("✨  New Chat", "#22d3ee");
        newChatBtn.setOnAction(e -> { geminiService.clearHistory(); loadAiAssistant(); });
        titleRow.getChildren().addAll(icon, titleText, spacer, newChatBtn);

        HBox chips = new HBox(10);
        chips.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(chips, new Insets(0, 0, 18, 0));
        for (String s : new String[]{"📖  Explain a concept","📝  Help me study","🧮  Solve a problem","💡  Study tips"}) {
            Button chip = new Button(s);
            chip.getStyleClass().add("suggestion-chip");
            chip.setOnAction(e -> sendAiMessage(s.replaceAll("^[^ ]+ {2}", "")));
            chips.getChildren().add(chip);
        }

        chatMessagesBox = new VBox(14);
        chatMessagesBox.setPadding(new Insets(20));
        chatMessagesBox.setFillWidth(true);

        chatScrollPane = new ScrollPane(chatMessagesBox);
        chatScrollPane.setFitToWidth(true); chatScrollPane.setPrefHeight(420);
        chatScrollPane.setMinHeight(300);
        VBox.setVgrow(chatScrollPane, Priority.ALWAYS);
        chatScrollPane.setStyle("""
            -fx-background-color: rgba(10,16,30,0.7);
            -fx-background-radius: 14;
            -fx-border-color: rgba(51,65,85,0.4);
            -fx-border-width: 1; -fx-border-radius: 14; -fx-padding: 0;
            """);
        chatScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        chatScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        appendAiMessage("👋  Hello! I'm **ExamVerse AI**, your personal study assistant.\n\n"
                + "I can help you understand concepts, solve practice problems, "
                + "create study plans, and answer questions about any subject. "
                + "What would you like to explore today?");

        HBox inputBar = new HBox(10);
        inputBar.setAlignment(Pos.CENTER);
        VBox.setMargin(inputBar, new Insets(14, 0, 0, 0));
        chatInputField = new TextField();
        chatInputField.setPromptText("Ask anything about your studies…");
        chatInputField.getStyleClass().add("chat-input-field");
        HBox.setHgrow(chatInputField, Priority.ALWAYS);
        chatInputField.setOnAction(e -> sendAiMessage(chatInputField.getText()));
        chatSendBtn = new Button("➤");
        chatSendBtn.getStyleClass().add("chat-send-btn");
        chatSendBtn.setOnAction(e -> sendAiMessage(chatInputField.getText()));
        inputBar.getChildren().addAll(chatInputField, chatSendBtn);

        root.getChildren().addAll(titleRow, chips, chatScrollPane, inputBar);
        setContentWithAnimation(root);
        Platform.runLater(() -> chatInputField.requestFocus());
    }

    private void sendAiMessage(String text) {
        if (text == null || text.isBlank() || chatMessagesBox == null) return;
        String message = text.trim();
        appendUserMessage(message);
        chatInputField.clear(); chatInputField.setDisable(true); chatSendBtn.setDisable(true);
        HBox typingRow = buildTypingIndicator();
        chatMessagesBox.getChildren().add(typingRow);
        scrollChatToBottom();

        Task<String> task = new Task<>() {
            @Override protected String call() { return geminiService.sendMessage(message); }
        };
        task.setOnSucceeded(e -> {
            chatMessagesBox.getChildren().remove(typingRow);
            String reply = task.getValue();
            if (reply != null && reply.startsWith("ERROR:")) appendErrorMessage(reply.substring(6).trim());
            else appendAiMessage(reply != null ? reply : "No response received.");
            chatInputField.setDisable(false); chatSendBtn.setDisable(false);
            chatInputField.requestFocus(); scrollChatToBottom();
        });
        task.setOnFailed(e -> {
            chatMessagesBox.getChildren().remove(typingRow);
            appendErrorMessage("Connection failed. Check your internet and try again.");
            chatInputField.setDisable(false); chatSendBtn.setDisable(false);
            scrollChatToBottom();
        });
        new Thread(task, "gemini-api-thread").start();
    }

    private void appendUserMessage(String text) {
        Label bubble = new Label(text);
        bubble.getStyleClass().add("chat-bubble-user"); bubble.setWrapText(true);
        HBox row = new HBox(bubble); row.setAlignment(Pos.CENTER_RIGHT);
        chatMessagesBox.getChildren().add(row); scrollChatToBottom();
    }

    private void appendAiMessage(String text) {
        String clean = text.replaceAll("\\*\\*(.*?)\\*\\*", "$1");
        Label avatar = new Label("🤖"); avatar.setStyle("-fx-font-size: 20px; -fx-padding: 2 0 0 0;");
        Label bubble = new Label(clean);
        bubble.getStyleClass().add("chat-bubble-ai"); bubble.setWrapText(true);
        HBox avatarBox = new HBox(8, avatar, bubble); avatarBox.setAlignment(Pos.TOP_LEFT);
        HBox row = new HBox(avatarBox); row.setAlignment(Pos.CENTER_LEFT);
        chatMessagesBox.getChildren().add(row); scrollChatToBottom();
    }

    private void appendErrorMessage(String errorText) {
        Label bubble = new Label("⚠️  " + errorText);
        bubble.getStyleClass().add("chat-bubble-error"); bubble.setWrapText(true);
        HBox row = new HBox(bubble); row.setAlignment(Pos.CENTER_LEFT);
        chatMessagesBox.getChildren().add(row); scrollChatToBottom();
    }

    private HBox buildTypingIndicator() {
        HBox dotsBox = new HBox(5);
        dotsBox.setAlignment(Pos.CENTER_LEFT); dotsBox.setPadding(new Insets(10, 16, 10, 16));
        dotsBox.setStyle("""
            -fx-background-color: rgba(30,41,59,0.85);
            -fx-background-radius: 18 18 18 4;
            -fx-border-color: rgba(51,65,85,0.5);
            -fx-border-width: 1; -fx-border-radius: 18 18 18 4;
            """);
        for (int i = 0; i < 3; i++) {
            Circle dot = new Circle(4); dot.setFill(Color.web("#22d3ee"));
            FadeTransition ft = new FadeTransition(Duration.millis(500), dot);
            ft.setFromValue(0.2); ft.setToValue(1.0);
            ft.setCycleCount(Animation.INDEFINITE); ft.setAutoReverse(true);
            ft.setDelay(Duration.millis(i * 180)); ft.play();
            dotsBox.getChildren().add(dot);
        }
        Label avatar = new Label("🤖"); avatar.setStyle("-fx-font-size: 20px; -fx-padding: 2 8 0 0;");
        return new HBox(8, avatar, dotsBox);
    }

    private void scrollChatToBottom() {
        Platform.runLater(() -> {
            if (chatScrollPane != null) {
                chatScrollPane.layout(); chatScrollPane.setVvalue(1.0);
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  STYLED ALERTS  (dark-themed — no more plain white dialogs)
    // ═══════════════════════════════════════════════════════════════════════

    private Alert styledAlert(Alert.AlertType type, String title,
                              String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        DialogPane dp = alert.getDialogPane();

        // Apply our dark stylesheet
        try {
            URL css = getClass().getResource("/com/examverse/css/student-dashboard.css");
            if (css != null) dp.getStylesheets().add(css.toExternalForm());
        } catch (Exception ignored) {}

        dp.setStyle(
                "-fx-background-color: #080e1e;" +
                        "-fx-border-color: #22d3ee59;" +
                        "-fx-border-width: 1; -fx-border-radius: 14; -fx-background-radius: 14;"
        );

        // setOnShown fires after JavaFX has rendered all dialog buttons —
        // this is the only reliable hook for styling them
        alert.setOnShown(e -> {
            for (ButtonType bt : alert.getButtonTypes()) {
                javafx.scene.Node btn = dp.lookupButton(bt);
                if (btn == null) continue;
                if (bt == ButtonType.OK || bt == ButtonType.YES) {
                    // Cyan gradient — clearly distinct from green
                    btn.setStyle(
                            "-fx-background-color: linear-gradient(to right,#22d3ee,#06b6d4);" +
                                    "-fx-text-fill: #030712; -fx-font-weight: 700; -fx-font-size: 13px;" +
                                    "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 9 22;" +
                                    "-fx-effect: dropshadow(gaussian,#22d3ee80,10,0.4,0,2);"
                    );
                } else {
                    // Cancel / Close — dark ghost
                    btn.setStyle(
                            "-fx-background-color: #0d1428;" +
                                    "-fx-text-fill: #94a3b8; -fx-font-size: 13px; -fx-font-weight: 600;" +
                                    "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 9 22;" +
                                    "-fx-border-color: #334155; -fx-border-width: 1; -fx-border-radius: 8;" +
                                    "-fx-effect: none;"
                    );
                }
            }
        });

        return alert;
    }

    private void showErrorAlert(String msg) {
        styledAlert(Alert.AlertType.ERROR, "Error", null, msg).showAndWait();
    }

    private void showInfoAlert(String title, String msg) {
        styledAlert(Alert.AlertType.INFORMATION, title, null, msg).showAndWait();
    }
}