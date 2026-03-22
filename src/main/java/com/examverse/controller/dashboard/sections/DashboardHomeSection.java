package com.examverse.controller.dashboard.sections;

import com.examverse.model.exam.Exam;
import com.examverse.model.exam.StudentExamAttempt;
import com.examverse.model.user.StudentStats;
import com.examverse.model.user.User;
import com.examverse.service.exam.ExamService;
import com.examverse.util.SceneManager;
import com.examverse.util.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * DashboardHomeSection
 * Renders the "Dashboard Overview" content panel.
 * Extracted from StudentDashboardController to keep each feature in its own class.
 */
public class DashboardHomeSection {

    private final ExamService examService;
    private final User currentUser;
    private final ScrollPane contentScrollPane;

    // Callbacks into the parent controller
    private final Consumer<Exam>               onStartExam;
    private final Consumer<StudentExamAttempt> onResumeExam;
    private final Runnable                     onOpenMyExams;
    private final Runnable                     onOpenMyExamsOngoing;
    private final Runnable                     onOpenPractice;
    private final Runnable                     onOpenResults;
    private final Runnable                     onOpenContests;

    public DashboardHomeSection(
            ExamService examService,
            User currentUser,
            ScrollPane contentScrollPane,
            Consumer<Exam> onStartExam,
            Consumer<StudentExamAttempt> onResumeExam,
            Runnable onOpenMyExams,
            Runnable onOpenMyExamsOngoing,
            Runnable onOpenPractice,
            Runnable onOpenResults,
            Runnable onOpenContests) {

        this.examService         = examService;
        this.currentUser         = currentUser;
        this.contentScrollPane   = contentScrollPane;
        this.onStartExam         = onStartExam;
        this.onResumeExam        = onResumeExam;
        this.onOpenMyExams       = onOpenMyExams;
        this.onOpenMyExamsOngoing= onOpenMyExamsOngoing;
        this.onOpenPractice      = onOpenPractice;
        this.onOpenResults       = onOpenResults;
        this.onOpenContests      = onOpenContests;
    }

    public VBox build() {
        VBox content = new VBox(28);
        content.setPadding(new Insets(32, 36, 32, 36));
        content.setStyle("-fx-background-color: transparent;");

        Label titleLabel = DashboardUIFactory.sectionTitle("📊  Dashboard Overview");

        StudentStats stats = examService.getStudentStats(currentUser.getId());

        HBox statsRow = new HBox(20);
        statsRow.setAlignment(Pos.CENTER_LEFT);
        statsRow.getChildren().addAll(
                DashboardUIFactory.statCard("📝", "Attempted",  String.valueOf(stats.getTotalExamsAttempted()), "#22d3ee", "#0ea5e9"),
                DashboardUIFactory.statCard("📈", "Avg Score",  stats.getFormattedAverageScore(),              "#10b981", "#059669"),
                DashboardUIFactory.statCard("🎯", "Accuracy",   stats.getFormattedAccuracy(),                  "#a78bfa", "#7c3aed"),
                DashboardUIFactory.statCard("✅", "Passed",     String.valueOf(stats.getTotalExamsPassed()),   "#22c55e", "#16a34a")
        );

        Label actTitle = DashboardUIFactory.sectionSubtitle("⚡  Quick Actions");

        HBox actions = new HBox(14);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button a1 = DashboardUIFactory.quickActionBtn("🚀  Start New Exam", "#22d3ee", "#0e7490");
        Button a2 = DashboardUIFactory.quickActionBtn("▶️  Resume Exam",    "#f59e0b", "#b45309");
        Button a3 = DashboardUIFactory.quickActionBtn("💪  Practice Mode",  "#a78bfa", "#6d28d9");
        Button a4 = DashboardUIFactory.quickActionBtn("📊  My Results",     "#34d399", "#059669");
        Button a5 = DashboardUIFactory.quickActionBtn("⚔️  Live Contests",  "#e879f9", "#a21caf");

        a1.setOnAction(e -> onOpenMyExams.run());
        a2.setOnAction(e -> onOpenMyExamsOngoing.run());
        a3.setOnAction(e -> onOpenPractice.run());
        a4.setOnAction(e -> onOpenResults.run());
        a5.setOnAction(e -> onOpenContests.run());

        actions.getChildren().addAll(a5, a1, a2, a3, a4);

        Label recentTitle   = DashboardUIFactory.sectionSubtitle("🕐  Recent Activity");
        VBox  recentBox     = buildRecentActivity();

        Label upcomingTitle = DashboardUIFactory.sectionSubtitle("📅  Available Exams");
        VBox  upcomingBox   = buildUpcomingExams();

        content.getChildren().addAll(
                titleLabel, statsRow,
                DashboardUIFactory.divider(),
                actTitle, actions,
                DashboardUIFactory.divider(),
                recentTitle, recentBox,
                DashboardUIFactory.divider(),
                upcomingTitle, upcomingBox
        );

        return content;
    }

    // ── Recent Activity ───────────────────────────────────────────────────────

    private VBox buildRecentActivity() {
        VBox box = DashboardUIFactory.glassCard();
        box.setSpacing(0);

        List<StudentExamAttempt> completed = examService.getCompletedExams(currentUser.getId());

        if (completed.isEmpty()) {
            Label empty = new Label("No recent activity yet. Start an exam to see your progress!");
            empty.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px;");
            empty.setPadding(new Insets(10, 0, 0, 0));
            box.getChildren().add(empty);
            return box;
        }

        int shown = Math.min(completed.size(), 5);
        for (int i = 0; i < shown; i++) {
            StudentExamAttempt a = completed.get(i);
            HBox row = new HBox(16);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(12, 16, 12, 16));
            if (i < shown - 1) {
                row.setStyle("-fx-border-color: rgba(51,65,85,0.3); -fx-border-width: 0 0 1 0;");
            }

            Circle dot = new Circle(6, Color.web(a.getResultColor()));

            VBox info = new VBox(3);
            Label name = new Label(a.getExamTitle());
            name.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 14px; -fx-font-weight: 600;");
            Label sub = new Label(a.getSubject() != null ? a.getSubject() : "");
            sub.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
            info.getChildren().addAll(name, sub);

            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);

            VBox scoreBox = new VBox(2);
            scoreBox.setAlignment(Pos.CENTER_RIGHT);
            Label pct = new Label(String.format("%.1f%%", a.getPercentage()));
            pct.setStyle("-fx-text-fill: " + a.getResultColor() + "; -fx-font-size: 16px; -fx-font-weight: 700;");
            Label res = new Label(a.getResult() != null ? a.getResult() : "PENDING");
            res.setStyle("-fx-text-fill: " + a.getResultColor() + "; -fx-font-size: 11px;");
            scoreBox.getChildren().addAll(pct, res);

            row.getChildren().addAll(dot, info, sp, scoreBox);
            box.getChildren().add(row);
        }
        return box;
    }

    // ── Upcoming / Available Exams ────────────────────────────────────────────

    private VBox buildUpcomingExams() {
        VBox box = new VBox(12);
        List<Exam> exams = examService.getAllActiveExams();
        int shown = Math.min(exams.size(), 3);

        if (exams.isEmpty()) {
            VBox empty = DashboardUIFactory.glassCard();
            Label lbl = new Label("No active exams available right now.");
            lbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px;");
            empty.getChildren().add(lbl);
            box.getChildren().add(empty);
            return box;
        }

        for (int i = 0; i < shown; i++) {
            Exam exam = exams.get(i);
            HBox card = new HBox(16);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setPadding(new Insets(16, 20, 16, 20));
            String baseStyle = """
                -fx-background-color: rgba(30,41,59,0.55);
                -fx-background-radius: 10;
                -fx-border-color: rgba(51,65,85,0.45);
                -fx-border-width: 1;
                -fx-border-radius: 10;
                """;
            card.setStyle(baseStyle);

            Rectangle strip = new Rectangle(4, 40);
            strip.setArcWidth(4); strip.setArcHeight(4);
            strip.setFill(Color.web(exam.getDifficultyColor()));

            VBox info = new VBox(4);
            Label name = new Label(exam.getExamTitle());
            name.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: 700;");
            Label meta = new Label("📚 " + exam.getSubject() + "   ⏱ "
                    + exam.getFormattedDuration() + "   📝 " + exam.getTotalQuestions() + " Qs");
            meta.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
            info.getChildren().addAll(name, meta);

            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);

            Button go = DashboardUIFactory.miniBtn("Start →", "#22d3ee");
            go.setOnAction(e -> onStartExam.accept(exam));

            card.getChildren().addAll(strip, info, sp, go);
            card.setOnMouseEntered(ev -> card.setStyle("""
                -fx-background-color: rgba(34,211,238,0.08);
                -fx-background-radius: 10;
                -fx-border-color: rgba(34,211,238,0.45);
                -fx-border-width: 1; -fx-border-radius: 10;
                """));
            card.setOnMouseExited(ev -> card.setStyle(baseStyle));
            box.getChildren().add(card);
        }
        return box;
    }
}