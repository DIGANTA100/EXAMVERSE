package com.examverse.controller.dashboard.sections;

import com.examverse.config.DatabaseConfig;
import com.examverse.model.user.StudentRating;
import com.examverse.model.user.User;
import com.examverse.service.exam.ExamService;
import com.examverse.util.SceneManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * ProfileSection — student profile page with contest rating display.
 *
 * Rank colour table (matches the spec exactly):
 *   3000+   👑 Legend    #fbbf24  gold
 *   2600-   🚀 Champion  #a78bfa  purple
 *   2200-   🧠 Expert    #60a5fa  blue
 *   1800-   🔥 Advanced  #f97316  orange
 *   1400-   ⚡ Skilled   #34d399  green
 *   1000-   🧑 Learner   #94a3b8  slate
 *   0-      🌱 Beginner  #6b7280  grey
 */
public class ProfileSection {

    private final ExamService examService;
    private final User        currentUser;

    public ProfileSection(ExamService examService, User currentUser) {
        this.examService = examService;
        this.currentUser = currentUser;
    }

    // ── Correct rank colour table ─────────────────────────────────────────────

    public static String getRankColor(int rating) {
        if (rating >= 3000) return "#fbbf24"; // gold    — Legend
        if (rating >= 2600) return "#a78bfa"; // purple  — Champion
        if (rating >= 2200) return "#60a5fa"; // blue    — Expert
        if (rating >= 1800) return "#f97316"; // orange  — Advanced
        if (rating >= 1400) return "#34d399"; // green   — Skilled
        if (rating >= 1000) return "#94a3b8"; // slate   — Learner
        return "#6b7280";                     // grey    — Beginner
    }

    // rank data: {emoji+name, colorHex, minRating}
    private static final String[][] RANKS = {
            {"🌱 Beginner",  "#6b7280", "0"},
            {"🧑‍🎓 Learner", "#94a3b8", "1000"},
            {"⚡ Skilled",   "#34d399", "1400"},
            {"🔥 Advanced",  "#f97316", "1800"},
            {"🧠 Expert",    "#60a5fa", "2200"},
            {"🚀 Champion",  "#a78bfa", "2600"},
            {"👑 Legend",    "#fbbf24", "3000"}
    };

    // ── Build ─────────────────────────────────────────────────────────────────

    public VBox build() {
        VBox content = new VBox(28);
        content.setPadding(new Insets(32, 36, 32, 36));
        content.setStyle("-fx-background-color: transparent;");

        StudentRating rating = loadRating(currentUser.getId());
        String rankColor     = getRankColor(rating.getCurrentRating());

        Label pageTitle = DashboardUIFactory.sectionTitle("👤  My Profile");

        VBox heroCard    = buildHeroCard(rating, rankColor);
        VBox ratingCard  = buildRatingCard(rating, rankColor);

        Label infoTitle = DashboardUIFactory.sectionSubtitle("📋  Account Information");
        VBox  infoCard  = DashboardUIFactory.glassCard();
        infoCard.setSpacing(0);
        addInfoRow(infoCard, "👤  Full Name",      currentUser.getFullName(),                          true);
        addInfoRow(infoCard, "📧  Email",          currentUser.getEmail(),                             true);
        addInfoRow(infoCard, "🔑  Username",       currentUser.getUsername(),                          true);
        addInfoRow(infoCard, "🎭  Role",           currentUser.getUserType(),                          true);
        addInfoRow(infoCard, "✅  Account Status", currentUser.isActive() ? "Active" : "Inactive",   false);

        Label statsTitle = DashboardUIFactory.sectionSubtitle("📊  Exam Statistics");
        var stats = examService.getStudentStats(currentUser.getId());
        HBox statsRow = new HBox(20);
        statsRow.getChildren().addAll(
                DashboardUIFactory.statCard("📝", "Attempted", String.valueOf(stats.getTotalExamsAttempted()), "#22d3ee", "#0ea5e9"),
                DashboardUIFactory.statCard("✅", "Passed",    String.valueOf(stats.getTotalExamsPassed()),    "#22c55e", "#16a34a"),
                DashboardUIFactory.statCard("📈", "Avg Score", stats.getFormattedAverageScore(),               "#a78bfa", "#7c3aed"),
                DashboardUIFactory.statCard("⏱", "Time Spent", stats.getTotalTimeSpentMinutes() + " min",     "#f59e0b", "#d97706")
        );

        Label secTitle = DashboardUIFactory.sectionSubtitle("🔒  Security");
        VBox  secCard  = DashboardUIFactory.glassCard();
        Button changePwdBtn = new Button("🔑  Change Password");
        changePwdBtn.setStyle(
                "-fx-background-color: #14b8a614; -fx-text-fill: #14b8a6;" +
                        "-fx-font-size: 14px; -fx-font-weight: 600; -fx-padding: 11 24;" +
                        "-fx-background-radius: 8; -fx-cursor: hand;" +
                        "-fx-border-color: #14b8a666; -fx-border-width: 1; -fx-border-radius: 8;"
        );
        changePwdBtn.setOnAction(e ->
                SceneManager.switchScene("/com/examverse/fxml/auth/reset-password.fxml"));
        secCard.getChildren().add(changePwdBtn);

        content.getChildren().addAll(
                pageTitle,
                heroCard,
                DashboardUIFactory.divider(),
                ratingCard,
                DashboardUIFactory.divider(),
                infoTitle, infoCard,
                DashboardUIFactory.divider(),
                statsTitle, statsRow,
                DashboardUIFactory.divider(),
                secTitle, secCard
        );
        return content;
    }

    // ── Hero card ─────────────────────────────────────────────────────────────

    private VBox buildHeroCard(StudentRating rating, String rankColor) {
        VBox card = new VBox(16);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(40, 40, 36, 40));

        // Dark glass base with rank-colored border
        card.setStyle(
                "-fx-background-radius: 18;" +
                        "-fx-border-color: " + rankColor + ";" +
                        "-fx-border-width: 1.5; -fx-border-radius: 18;"
        );
        // Background fill via Java API (supports real alpha)
        card.setBackground(new Background(new BackgroundFill(
                Color.web("#080e1e", 0.75), new CornerRadii(18), Insets.EMPTY)));
        // Glow effect via Java API (no CSS dropshadow 8-digit hex issue)
        javafx.scene.effect.DropShadow cardGlow = new javafx.scene.effect.DropShadow();
        cardGlow.setColor(Color.web(rankColor, 0.35));
        cardGlow.setRadius(28);
        cardGlow.setSpread(0.15);
        card.setEffect(cardGlow);

        // Avatar circle — filled with rank color
        StackPane avatarWrap = new StackPane();
        Circle avatarBg = new Circle(54);
        avatarBg.setFill(Color.web(rankColor));
        // Inner dark circle to create ring effect
        Circle avatarInner = new Circle(46);
        avatarInner.setFill(Color.web("#0d1428"));

        Label initLbl = new Label(getInitials(currentUser.getFullName()));
        initLbl.setStyle(
                "-fx-text-fill: " + rankColor + ";" +
                        "-fx-font-size: 28px; -fx-font-weight: 800;"
        );
        avatarWrap.getChildren().addAll(avatarBg, avatarInner, initLbl);
        // Glow on avatar
        avatarBg.setEffect(new javafx.scene.effect.DropShadow(18,
                Color.web(rankColor + "80")));

        Label nameLabel = new Label(currentUser.getFullName());
        nameLabel.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 22px; -fx-font-weight: 700;");

        // Rank badge
        Label rankBadge = new Label(rating.getRankTitle());
        rankBadge.setStyle(
                "-fx-text-fill: " + rankColor + ";" +
                        "-fx-padding: 7 22; -fx-background-radius: 24;" +
                        "-fx-font-size: 16px; -fx-font-weight: 800;" +
                        "-fx-border-color: " + rankColor + "; -fx-border-width: 1; -fx-border-radius: 24;"
        );
        rankBadge.setBackground(new Background(new BackgroundFill(
                Color.web(rankColor, 0.15), new CornerRadii(24), Insets.EMPTY)));

        Label ratingLbl = new Label("⭐  Rating: " + rating.getCurrentRating());
        ratingLbl.setStyle(
                "-fx-text-fill: " + rankColor + ";" +
                        "-fx-font-size: 15px; -fx-font-weight: 700;"
        );

        card.getChildren().addAll(avatarWrap, nameLabel, rankBadge, ratingLbl);
        return card;
    }

    // ── Rating / contest stats card ───────────────────────────────────────────

    private VBox buildRatingCard(StudentRating rating, String rankColor) {
        VBox card = new VBox(0);
        card.setStyle(
                "-fx-background-color: #080e1eb8;" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: #1e2a46b3;" +
                        "-fx-border-width: 1; -fx-border-radius: 14;"
        );

        // Card title row
        HBox cardHeader = new HBox();
        cardHeader.setAlignment(Pos.CENTER_LEFT);
        cardHeader.setPadding(new Insets(18, 22, 14, 22));
        cardHeader.setStyle("-fx-border-color: #1e2a4699; -fx-border-width: 0 0 1 0;");
        Label cardTitle = new Label("🏆  Contest Rating");
        cardTitle.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 17px; -fx-font-weight: 700;");
        cardHeader.getChildren().add(cardTitle);
        card.getChildren().add(cardHeader);

        // Rating rows
        addRatingRow(card, "Current Rating",        String.valueOf(rating.getCurrentRating()),       rankColor,   true);
        addRatingRow(card, "Peak Rating",            String.valueOf(rating.getPeakRating()),          "#fbbf24",   false);
        addRatingRow(card, "Rank Title",             rating.getRankTitle(),                           rankColor,   true);
        addRatingRow(card, "Contests Participated",  String.valueOf(rating.getContestsParticipated()),"#60a5fa",   false);
        addRatingRow(card, "Contests Won  🥇",       String.valueOf(rating.getContestsWon()),         "#34d399",   true);
        addRatingRow(card, "Total Contest Score",    String.valueOf(rating.getTotalScore()),           "#a78bfa",   false);

        // Rank progression ladder
        card.getChildren().add(buildRankLadder(rating.getCurrentRating()));
        return card;
    }

    /**
     * A single key/value row inside the rating card.
     * @param zebra  alternate background for readability
     */
    private void addRatingRow(VBox parent, String labelText,
                              String valueText, String valueColor, boolean zebra) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(13, 22, 13, 22));
        if (zebra) row.setStyle("-fx-background-color: #0d142814;");

        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px; -fx-font-weight: 500;");
        HBox.setHgrow(lbl, Priority.ALWAYS);
        lbl.setMaxWidth(Double.MAX_VALUE);

        Label val = new Label(valueText);
        val.setStyle(
                "-fx-text-fill: " + valueColor + ";" +
                        "-fx-font-size: 15px; -fx-font-weight: 700;"
        );

        row.getChildren().addAll(lbl, val);
        parent.getChildren().add(row);
    }

    // ── Rank progression ladder ───────────────────────────────────────────────

    private VBox buildRankLadder(int currentRating) {
        VBox wrapper = new VBox(16);
        wrapper.setPadding(new Insets(20, 22, 24, 22));
        wrapper.setStyle("-fx-border-color: #1e2a4699; -fx-border-width: 1 0 0 0;");

        Label sectionLbl = new Label("RANK PROGRESSION");
        sectionLbl.setStyle(
                "-fx-text-fill: #64748b; -fx-font-size: 11px;" +
                        "-fx-font-weight: 700; -fx-letter-spacing: 0.15em;"
        );
        wrapper.getChildren().add(sectionLbl);

        // Determine active rank index
        int activeIdx = 0;
        for (int i = 0; i < RANKS.length; i++) {
            int min = Integer.parseInt(RANKS[i][2]);
            int max = (i + 1 < RANKS.length) ? Integer.parseInt(RANKS[i + 1][2]) - 1 : Integer.MAX_VALUE;
            if (currentRating >= min && currentRating <= max) { activeIdx = i; break; }
        }

        // ── Horizontal pip row ────────────────────────────────────────────────
        HBox pipRow = new HBox(0);
        pipRow.setAlignment(Pos.CENTER);

        for (int i = 0; i < RANKS.length; i++) {
            String name    = RANKS[i][0];
            String color   = RANKS[i][1];
            boolean done    = i < activeIdx;
            boolean current = i == activeIdx;
            boolean future  = i > activeIdx;

            // Connector line before pip (not before first)
            if (i > 0) {
                Region line = new Region();
                line.setMinHeight(3);
                line.setPrefHeight(3);
                HBox.setHgrow(line, Priority.ALWAYS);
                // Completed segments are colored, future ones stay dim
                line.setStyle("-fx-background-color: " + (done ? color : "#1e2a46") + "; -fx-background-radius: 2;");
                pipRow.getChildren().add(line);
            }

            // Pip VBox: circle + emoji + name
            VBox pip = new VBox(5);
            pip.setAlignment(Pos.CENTER);
            pip.setMinWidth(66);
            pip.setPrefWidth(66);
            pip.setPadding(new Insets(10, 4, 10, 4));

            // ── Style the pip card using Java API (avoids 8-digit hex CSS issues) ──
            if (current) {
                // Vivid: solid semi-transparent fill + colored border + glow
                pip.setBackground(new Background(new BackgroundFill(
                        Color.web(color, 0.25), new CornerRadii(12), Insets.EMPTY)));
                pip.setStyle(
                        "-fx-border-color: " + color + ";" +
                                "-fx-border-width: 2; -fx-border-radius: 12; -fx-background-radius: 12;"
                );
                javafx.scene.effect.DropShadow cardGlow = new javafx.scene.effect.DropShadow();
                cardGlow.setColor(Color.web(color, 0.65));
                cardGlow.setRadius(20);
                cardGlow.setSpread(0.25);
                pip.setEffect(cardGlow);
            } else if (done) {
                // Subtle tint — achieved but past
                pip.setBackground(new Background(new BackgroundFill(
                        Color.web(color, 0.07), new CornerRadii(8), Insets.EMPTY)));
            }
            // future: no background, no style — completely unrevealed / dark

            // Dot circle
            Circle dot;
            if (current) {
                dot = new Circle(14);
                dot.setFill(Color.web(color));
                dot.setStroke(Color.web(color, 0.5));
                dot.setStrokeWidth(4);
                javafx.scene.effect.DropShadow dotGlow = new javafx.scene.effect.DropShadow();
                dotGlow.setColor(Color.web(color, 0.9));
                dotGlow.setRadius(22);
                dotGlow.setSpread(0.4);
                dot.setEffect(dotGlow);
            } else if (done) {
                dot = new Circle(8);
                dot.setFill(Color.web(color));
                dot.setStroke(Color.web(color, 0.5));
                dot.setStrokeWidth(1.5);
            } else {
                // Future: dark hollow — unrevealed
                dot = new Circle(7);
                dot.setFill(Color.web("#0d1428"));
                dot.setStroke(Color.web("#2d3748"));
                dot.setStrokeWidth(1.5);
            }

            // Emoji
            String emoji = name.split(" ")[0];
            Label emojiLbl = new Label(emoji);
            emojiLbl.setStyle("-fx-font-size: " + (current ? "17" : "13") + "px;");
            if (future) emojiLbl.setOpacity(0.18);

            // Rank name
            String rankNameOnly = name.contains(" ") ? name.substring(name.indexOf(' ') + 1) : name;
            Label nameLbl = new Label(rankNameOnly);
            if (current) {
                nameLbl.setStyle(
                        "-fx-text-fill: " + color + "; -fx-font-size: 10px; -fx-font-weight: 800;"
                );
            } else if (done) {
                nameLbl.setStyle(
                        "-fx-text-fill: " + color + "; -fx-font-size: 9px; -fx-font-weight: 600;"
                );
                nameLbl.setOpacity(0.7);
            } else {
                // Future: nearly invisible — unrevealed
                nameLbl.setStyle("-fx-text-fill: #2d3748; -fx-font-size: 9px; -fx-font-weight: 400;");
            }
            nameLbl.setAlignment(Pos.CENTER);
            nameLbl.setWrapText(true);

            pip.getChildren().addAll(dot, emojiLbl, nameLbl);
            pipRow.getChildren().add(pip);
        }

        wrapper.getChildren().add(pipRow);

        // ── Current rank callout strip ────────────────────────────────────────
        String rankColor = RANKS[activeIdx][1];
        String rankName  = RANKS[activeIdx][0];
        String nextInfo  = "";
        if (activeIdx < RANKS.length - 1) {
            int nextMin = Integer.parseInt(RANKS[activeIdx + 1][2]);
            int gap = nextMin - currentRating;
            nextInfo = "   ·   " + gap + " pts to reach " + RANKS[activeIdx + 1][0];
        }

        Label callout = new Label("You are  " + rankName + nextInfo);
        callout.setStyle(
                "-fx-text-fill: " + rankColor + ";" +
                        "-fx-font-size: 13px; -fx-font-weight: 700;" +
                        "-fx-padding: 10 16; -fx-background-radius: 8; -fx-border-radius: 8;" +
                        "-fx-border-color: " + rankColor + "; -fx-border-width: 1;"
        );
        callout.setBackground(new Background(new BackgroundFill(
                Color.web(rankColor, 0.12), new CornerRadii(8), Insets.EMPTY)));
        callout.setWrapText(true);
        wrapper.getChildren().add(callout);

        return wrapper;
    }

    // ── DB load ───────────────────────────────────────────────────────────────

    private StudentRating loadRating(int studentId) {
        StudentRating r = new StudentRating();
        r.setStudentId(studentId);
        r.setStudentName(currentUser.getFullName());
        r.setUsername(currentUser.getUsername());
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM student_ratings WHERE student_id = ?")) {
            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                r.setRatingId(rs.getInt("rating_id"));
                r.setCurrentRating(rs.getInt("current_rating"));
                r.setPeakRating(rs.getInt("peak_rating"));
                r.setContestsParticipated(rs.getInt("contests_participated"));
                r.setContestsWon(rs.getInt("contests_won"));
                r.setTotalScore(rs.getInt("total_score"));
            }
        } catch (Exception e) {
            System.out.println("ℹ️ student_ratings not loaded: " + e.getMessage());
        }
        return r;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String getInitials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] p = name.trim().split("\\s+");
        if (p.length == 1) return p[0].substring(0, Math.min(2, p[0].length())).toUpperCase();
        return (p[0].charAt(0) + "" + p[p.length - 1].charAt(0)).toUpperCase();
    }

    private void addInfoRow(VBox parent, String label, String value, boolean divider) {
        HBox row = new HBox(20);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(15, 22, 15, 22));
        if (divider) row.setStyle("-fx-border-color: #1e2a4666; -fx-border-width: 0 0 1 0;");

        Label l = new Label(label);
        l.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13.5px;");
        l.setPrefWidth(200);

        Label v = new Label(value != null ? value : "—");
        v.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 14px; -fx-font-weight: 600;");

        row.getChildren().addAll(l, v);
        parent.getChildren().add(row);
    }
}
