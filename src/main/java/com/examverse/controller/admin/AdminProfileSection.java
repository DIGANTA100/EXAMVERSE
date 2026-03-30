package com.examverse.controller.admin;

import com.examverse.config.DatabaseConfig;
import com.examverse.model.user.User;
import com.examverse.util.SceneManager;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * AdminProfileSection — builds the admin profile page.
 *
 * Displayed when the admin clicks the avatar circle in the header.
 * Mirrors the pattern of ProfileSection.java used for students,
 * but shows admin-specific stats and uses the cyan admin theme.
 *
 * Stats shown:
 *   • Total exams created by this admin
 *   • Total questions created by this admin
 *   • Total active students in the system
 *   • Total exam attempts across the system
 *   • Account information block
 *   • Security block (Change Password)
 */
public class AdminProfileSection {

    private static final String ACCENT       = "#06b6d4";   // cyan
    private static final String ACCENT_DARK  = "#0891b2";
    private static final String GOLD         = "#f59e0b";   // admin gold
    private static final String BG_CARD      = "#13151c";
    private static final String BG_DEEP      = "#0f1117";
    private static final String BORDER       = "#1e2231";
    private static final String TEXT_PRIMARY = "#e2e8f0";
    private static final String TEXT_MUTED   = "#64748b";
    private static final String TEXT_DIM     = "#334155";

    private final User currentUser;

    public AdminProfileSection(User currentUser) {
        this.currentUser = currentUser;
    }

    // ── Public entry point ────────────────────────────────────────────────────

    public VBox build() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: transparent;");

        // Show a skeleton loader while stats load
        VBox skeleton = buildSkeleton();
        root.getChildren().add(skeleton);

        // Load stats off UI thread
        Task<int[]> statsTask = new Task<>() {
            @Override
            protected int[] call() throws Exception {
                return loadAdminStats();
            }
        };

        statsTask.setOnSucceeded(e -> {
            int[] stats = statsTask.getValue();
            VBox content = buildContent(stats);

            // Fade-slide in
            content.setOpacity(0);
            root.getChildren().setAll(content);
            FadeTransition ft = new FadeTransition(Duration.millis(300), content);
            ft.setFromValue(0); ft.setToValue(1);
            TranslateTransition tt = new TranslateTransition(Duration.millis(300), content);
            tt.setFromY(16); tt.setToY(0);
            new ParallelTransition(ft, tt).play();
        });

        statsTask.setOnFailed(e -> {
            int[] empty = new int[4];
            VBox content = buildContent(empty);
            root.getChildren().setAll(content);
        });

        Thread t = new Thread(statsTask, "admin-profile-stats");
        t.setDaemon(true);
        t.start();

        return root;
    }

    // ── Full content builder ──────────────────────────────────────────────────

    private VBox buildContent(int[] stats) {
        VBox content = new VBox(28);
        content.setPadding(new Insets(32, 36, 40, 36));
        content.setStyle("-fx-background-color: transparent;");

        // ── Page title ────────────────────────────────────────────────────────
        Label pageTitle = sectionTitle("🛡️  Admin Profile");

        // ── Hero card ─────────────────────────────────────────────────────────
        VBox heroCard = buildHeroCard();

        // ── Stats row ─────────────────────────────────────────────────────────
        Label statsTitle = sectionSubtitle("📊  System Overview");
        HBox statsRow = new HBox(16);
        statsRow.getChildren().addAll(
                statCard("📚", "Exams Created",   String.valueOf(stats[0]), ACCENT,     ACCENT_DARK),
                statCard("❓", "Questions Made",  String.valueOf(stats[1]), "#a78bfa",  "#7c3aed"),
                statCard("👥", "Total Students",  String.valueOf(stats[2]), "#34d399",  "#059669"),
                statCard("📈", "Total Attempts",  String.valueOf(stats[3]), GOLD,       "#d97706")
        );

        // ── Account info card ─────────────────────────────────────────────────
        Label infoTitle = sectionSubtitle("📋  Account Information");
        VBox infoCard = glassCard();
        infoCard.setSpacing(0);
        addInfoRow(infoCard, "👤  Full Name",    currentUser.getFullName(),  true);
        addInfoRow(infoCard, "📧  Email",        currentUser.getEmail(),     true);
        addInfoRow(infoCard, "🔑  Username",     currentUser.getUsername(),  true);
        addInfoRow(infoCard, "🛡️  Role",         "Administrator",            true);
        addInfoRow(infoCard, "✅  Status",
                currentUser.isActive() ? "Active" : "Inactive",             false);

        // ── Last login card ───────────────────────────────────────────────────
        Label activityTitle = sectionSubtitle("🕐  Session Info");
        VBox activityCard = glassCard();
        activityCard.setSpacing(0);
        String now = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("EEEE, MMMM dd yyyy  •  hh:mm a"));
        addInfoRow(activityCard, "🕐  Current Session", now,     true);
        addInfoRow(activityCard, "🌐  User Type",       "ADMIN", false);

        // ── Security card ─────────────────────────────────────────────────────
        Label secTitle = sectionSubtitle("🔒  Security");
        VBox secCard = glassCard();

        Button changePwdBtn = new Button("🔑  Change Password");
        changePwdBtn.setStyle(
                "-fx-background-color: rgba(6,182,212,0.08);" +
                        "-fx-text-fill: " + ACCENT + ";" +
                        "-fx-font-size: 14px; -fx-font-weight: 600; -fx-padding: 11 24;" +
                        "-fx-background-radius: 8; -fx-cursor: hand;" +
                        "-fx-border-color: rgba(6,182,212,0.35); -fx-border-width: 1; -fx-border-radius: 8;"
        );
        changePwdBtn.setOnMouseEntered(e -> changePwdBtn.setStyle(
                "-fx-background-color: rgba(6,182,212,0.16);" +
                        "-fx-text-fill: " + ACCENT + ";" +
                        "-fx-font-size: 14px; -fx-font-weight: 600; -fx-padding: 11 24;" +
                        "-fx-background-radius: 8; -fx-cursor: hand;" +
                        "-fx-border-color: " + ACCENT + "; -fx-border-width: 1; -fx-border-radius: 8;"
        ));
        changePwdBtn.setOnMouseExited(e -> changePwdBtn.setStyle(
                "-fx-background-color: rgba(6,182,212,0.08);" +
                        "-fx-text-fill: " + ACCENT + ";" +
                        "-fx-font-size: 14px; -fx-font-weight: 600; -fx-padding: 11 24;" +
                        "-fx-background-radius: 8; -fx-cursor: hand;" +
                        "-fx-border-color: rgba(6,182,212,0.35); -fx-border-width: 1; -fx-border-radius: 8;"
        ));
        changePwdBtn.setOnAction(e ->
                SceneManager.switchScene("/com/examverse/fxml/auth/reset-password.fxml"));
        secCard.getChildren().add(changePwdBtn);

        // ── Assemble ──────────────────────────────────────────────────────────
        content.getChildren().addAll(
                pageTitle,
                heroCard,
                divider(),
                statsTitle, statsRow,
                divider(),
                infoTitle, infoCard,
                divider(),
                activityTitle, activityCard,
                divider(),
                secTitle, secCard
        );
        return content;
    }

    // ── Hero card ─────────────────────────────────────────────────────────────

    private VBox buildHeroCard() {
        VBox card = new VBox(14);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(40, 40, 36, 40));

        // Gold border for admin (distinguishes from student's rank-colored border)
        card.setStyle(
                "-fx-background-radius: 18;" +
                        "-fx-border-color: " + GOLD + ";" +
                        "-fx-border-width: 1.5; -fx-border-radius: 18;"
        );
        card.setBackground(new Background(new BackgroundFill(
                Color.web(BG_CARD, 0.85), new CornerRadii(18), Insets.EMPTY)));

        // Gold glow
        DropShadow glow = new DropShadow();
        glow.setColor(Color.web(GOLD, 0.3));
        glow.setRadius(30);
        glow.setSpread(0.12);
        card.setEffect(glow);

        // Avatar — gold ring with cyan initials inside
        StackPane avatarWrap = buildAvatarCircle(54);

        // Name
        Label nameLbl = new Label(currentUser.getFullName());
        nameLbl.setStyle(
                "-fx-text-fill: " + TEXT_PRIMARY + ";" +
                        "-fx-font-size: 22px; -fx-font-weight: 700;"
        );

        // Admin badge
        Label adminBadge = new Label("🛡️  ADMINISTRATOR");
        adminBadge.setStyle(
                "-fx-text-fill: " + GOLD + ";" +
                        "-fx-font-size: 12px; -fx-font-weight: 800; -fx-letter-spacing: 1px;" +
                        "-fx-padding: 5 16 5 16;" +
                        "-fx-border-color: " + GOLD + "; -fx-border-width: 1;" +
                        "-fx-border-radius: 20; -fx-background-radius: 20;"
        );
        adminBadge.setBackground(new Background(new BackgroundFill(
                Color.web(GOLD, 0.1), new CornerRadii(20), Insets.EMPTY)));

        // Username chip
        Label usernameLbl = new Label("@" + currentUser.getUsername());
        usernameLbl.setStyle(
                "-fx-text-fill: " + TEXT_MUTED + ";" +
                        "-fx-font-size: 13px; -fx-padding: 3 12 3 12;" +
                        "-fx-background-color: rgba(255,255,255,0.04);" +
                        "-fx-background-radius: 12; -fx-border-color: " + BORDER + ";" +
                        "-fx-border-width: 1; -fx-border-radius: 12;"
        );

        // Email chip
        Label emailLbl = new Label("✉  " + currentUser.getEmail());
        emailLbl.setStyle(
                "-fx-text-fill: " + TEXT_MUTED + ";" +
                        "-fx-font-size: 13px;"
        );

        card.getChildren().addAll(avatarWrap, nameLbl, adminBadge, usernameLbl, emailLbl);
        return card;
    }

    // ── Avatar circle (reusable — same style as header avatar) ───────────────

    public StackPane buildAvatarCircle(double radius) {
        Circle outerRing = new Circle(radius);
        outerRing.setFill(Color.web(GOLD));

        Circle innerDark = new Circle(radius - 7);
        innerDark.setFill(Color.web(BG_DEEP));

        // Subtle cyan inner ring
        Circle cyanRing = new Circle(radius - 9);
        cyanRing.setFill(Color.TRANSPARENT);
        cyanRing.setStroke(Color.web(ACCENT, 0.4));
        cyanRing.setStrokeWidth(1.5);

        Label initLbl = new Label(getInitials(currentUser.getFullName()));
        initLbl.setStyle(
                "-fx-text-fill: " + GOLD + ";" +
                        "-fx-font-size: " + (int)(radius * 0.52) + "px; -fx-font-weight: 800;"
        );

        StackPane wrap = new StackPane(outerRing, innerDark, cyanRing, initLbl);
        wrap.setMinSize(radius * 2, radius * 2);
        wrap.setMaxSize(radius * 2, radius * 2);

        // Glow on outer ring
        DropShadow avatarGlow = new DropShadow();
        avatarGlow.setColor(Color.web(GOLD, 0.55));
        avatarGlow.setRadius(20);
        avatarGlow.setSpread(0.2);
        outerRing.setEffect(avatarGlow);

        return wrap;
    }

    // ── DB stats loader ───────────────────────────────────────────────────────

    private int[] loadAdminStats() {
        int[] stats = new int[4]; // [examsCreated, questionsCreated, totalStudents, totalAttempts]
        try (Connection conn = DatabaseConfig.getConnection()) {

            // Exams created by this admin
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM exams WHERE created_by = ?")) {
                ps.setInt(1, currentUser.getId());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) stats[0] = rs.getInt(1);
            }

            // Questions created (via exams this admin created)
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM questions q " +
                            "JOIN exams e ON q.exam_id = e.exam_id " +
                            "WHERE e.created_by = ?")) {
                ps.setInt(1, currentUser.getId());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) stats[1] = rs.getInt(1);
            }

            // Total students in system
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM users WHERE user_type = 'STUDENT'")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) stats[2] = rs.getInt(1);
            }

            // Total exam attempts in system
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM student_exam_attempts WHERE status = 'COMPLETED'")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) stats[3] = rs.getInt(1);
            }

        } catch (Exception e) {
            System.err.println("❌ AdminProfileSection.loadAdminStats: " + e.getMessage());
        }
        return stats;
    }

    // ── Skeleton loader ───────────────────────────────────────────────────────

    private VBox buildSkeleton() {
        VBox sk = new VBox(20);
        sk.setPadding(new Insets(32, 36, 40, 36));
        sk.setStyle("-fx-background-color: transparent;");

        // Hero placeholder
        VBox hero = new VBox();
        hero.setAlignment(Pos.CENTER);
        hero.setPrefHeight(200);
        hero.setStyle(
                "-fx-background-color: " + BG_CARD + ";" +
                        "-fx-background-radius: 18; -fx-border-color: " + BORDER + ";" +
                        "-fx-border-width: 1; -fx-border-radius: 18;"
        );
        Label loading = new Label("Loading profile…");
        loading.setStyle("-fx-text-fill: " + TEXT_DIM + "; -fx-font-size: 14px;");
        hero.getChildren().add(loading);

        // Stats row placeholders
        HBox row = new HBox(16);
        for (int i = 0; i < 4; i++) {
            VBox ph = new VBox();
            ph.setPrefHeight(90);
            ph.setStyle(
                    "-fx-background-color: " + BG_CARD + ";" +
                            "-fx-background-radius: 12; -fx-border-color: " + BORDER + ";" +
                            "-fx-border-width: 1; -fx-border-radius: 12;"
            );
            HBox.setHgrow(ph, Priority.ALWAYS);
            row.getChildren().add(ph);
        }

        sk.getChildren().addAll(hero, row);
        return sk;
    }

    // ── Shared UI helpers ─────────────────────────────────────────────────────

    private VBox statCard(String icon, String label, String value,
                          String colorTop, String colorBottom) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(22, 24, 22, 24));
        card.setStyle(
                "-fx-background-color: " + BG_CARD + ";" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12;"
        );
        HBox.setHgrow(card, Priority.ALWAYS);

        // Hover effect
        DropShadow hover = new DropShadow();
        hover.setColor(Color.web(colorTop, 0.2));
        hover.setRadius(16);
        card.setOnMouseEntered(e -> {
            card.setEffect(hover);
            card.setStyle(
                    "-fx-background-color: " + BG_CARD + ";" +
                            "-fx-border-color: " + colorTop + ";" +
                            "-fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12;"
            );
        });
        card.setOnMouseExited(e -> {
            card.setEffect(null);
            card.setStyle(
                    "-fx-background-color: " + BG_CARD + ";" +
                            "-fx-border-color: " + BORDER + ";" +
                            "-fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12;"
            );
        });

        Label iconLbl = new Label(icon + "  " + label);
        iconLbl.setStyle("-fx-text-fill: " + TEXT_MUTED + "; -fx-font-size: 12px;");

        Label valueLbl = new Label(value);
        valueLbl.setStyle(
                "-fx-text-fill: " + colorTop + ";" +
                        "-fx-font-size: 34px; -fx-font-weight: 800;"
        );

        card.getChildren().addAll(iconLbl, valueLbl);
        return card;
    }

    private VBox glassCard() {
        VBox card = new VBox(0);
        card.setStyle(
                "-fx-background-color: " + BG_CARD + ";" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-width: 1; -fx-border-radius: 14; -fx-background-radius: 14;"
        );
        return card;
    }

    private void addInfoRow(VBox parent, String label, String value, boolean divider) {
        HBox row = new HBox(20);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(15, 22, 15, 22));
        if (divider) {
            row.setStyle(
                    "-fx-border-color: rgba(30,34,49,0.8) transparent transparent transparent;" +
                            "-fx-border-width: 0 0 1 0;"
            );
        }

        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: " + TEXT_MUTED + "; -fx-font-size: 13px;");
        lbl.setPrefWidth(220);

        Label val = new Label(value != null ? value : "—");
        val.setStyle("-fx-text-fill: " + TEXT_PRIMARY + "; -fx-font-size: 14px; -fx-font-weight: 600;");

        row.getChildren().addAll(lbl, val);
        parent.getChildren().add(row);
    }

    private Label sectionTitle(String text) {
        Label l = new Label(text);
        l.setStyle(
                "-fx-text-fill: " + TEXT_PRIMARY + ";" +
                        "-fx-font-size: 24px; -fx-font-weight: 800;"
        );
        return l;
    }

    private Label sectionSubtitle(String text) {
        Label l = new Label(text);
        l.setStyle(
                "-fx-text-fill: " + TEXT_PRIMARY + ";" +
                        "-fx-font-size: 16px; -fx-font-weight: 700;"
        );
        VBox.setMargin(l, new Insets(4, 0, 4, 0));
        return l;
    }

    private Region divider() {
        Region r = new Region();
        r.setPrefHeight(1);
        r.setStyle("-fx-background-color: " + BORDER + ";");
        VBox.setMargin(r, new Insets(4, 0, 4, 0));
        return r;
    }

    private static String getInitials(String name) {
        if (name == null || name.isBlank()) return "A";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1)
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}