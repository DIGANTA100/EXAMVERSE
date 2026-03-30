package com.examverse.controller.forum;

import com.examverse.controller.dashboard.sections.ProfileSection;
import com.examverse.model.forum.ForumMessage;
import com.examverse.model.user.User;
import com.examverse.service.forum.ForumService;
import com.examverse.util.SessionManager;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * DiscussionForumController — GENERAL channel chat for students and admins.
 *
 * Features:
 *  • Live polling every 2 s via a JavaFX Timeline (no WebSocket needed)
 *  • Avatar circle colour based on sender's contest rating / rank
 *  • STUDENT badge (cyan) vs ADMIN badge (gold) on every message
 *  • Click avatar → context menu with "View Profile" (student only)
 *  • Online-users sidebar (active in last 5 min)
 *  • Admin can delete any message; students can only delete their own
 */
public class DiscussionForumController implements Initializable {

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private VBox       messagesBox;
    @FXML private ScrollPane messagesScroll;
    @FXML private TextField  inputField;
    @FXML private Button     sendBtn;
    @FXML private VBox       onlineUsersBox;
    @FXML private Label      onlineCountLabel;

    // ── State ─────────────────────────────────────────────────────────────────
    private ForumService forumService;
    private User         currentUser;
    private int          lastMessageId  = 0;
    private int          currentRating  = 0;
    private Timeline     pollTimeline;

    private static final String CHANNEL = "GENERAL";
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm");

    // ── Init ──────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        forumService = new ForumService();
        ForumService.ensureTableExists();

        currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;

        // Load own rating for outgoing messages
        loadOwnRating();

        // Load message history (last 60 messages)
        Task<List<ForumMessage>> histTask = new Task<>() {
            @Override protected List<ForumMessage> call() {
                return forumService.fetchHistory(CHANNEL, 60);
            }
        };
        histTask.setOnSucceeded(e -> {
            List<ForumMessage> history = histTask.getValue();
            for (ForumMessage m : history) appendMessage(m);
            if (!history.isEmpty())
                lastMessageId = history.get(history.size() - 1).getMessageId();
            scrollToBottom();
        });
        new Thread(histTask, "forum-history").start();

        // Wire send button + Enter key
        sendBtn.setOnAction(e -> sendMessage());
        inputField.setOnAction(e -> sendMessage());

        // Start polling every 2 seconds
        startPolling();
    }

    // ── Polling ───────────────────────────────────────────────────────────────

    private void startPolling() {
        pollTimeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> poll()));
        pollTimeline.setCycleCount(Timeline.INDEFINITE);
        pollTimeline.play();
    }

    private void poll() {
        int knownId = lastMessageId;
        Task<List<ForumMessage>> task = new Task<>() {
            @Override protected List<ForumMessage> call() {
                return forumService.fetchNewMessages(CHANNEL, knownId);
            }
        };
        task.setOnSucceeded(e -> {
            List<ForumMessage> msgs = task.getValue();
            for (ForumMessage m : msgs) {
                appendMessage(m);
                lastMessageId = Math.max(lastMessageId, m.getMessageId());
            }
            if (!msgs.isEmpty()) scrollToBottom();
            refreshOnlineUsers();
        });
        new Thread(task, "forum-poll").start();
    }

    public void stopPolling() {
        if (pollTimeline != null) pollTimeline.stop();
    }

    // ── Send ──────────────────────────────────────────────────────────────────

    private void sendMessage() {
        String text = inputField.getText();
        if (text == null || text.isBlank()) return;
        inputField.clear();

        ForumMessage msg = new ForumMessage(
                currentUser.getId(),
                currentUser.getFullName(),
                currentUser.getUsername(),
                currentUser.getUserType(),
                currentRating,
                CHANNEL,
                text.trim(),
                java.time.LocalDateTime.now()
        );

        Task<Integer> task = new Task<>() {
            @Override protected Integer call() {
                return forumService.sendMessage(msg);
            }
        };
        task.setOnSucceeded(e -> {
            int id = task.getValue();
            if (id > 0) {
                msg.setMessageId(id);
                appendMessage(msg);
                lastMessageId = Math.max(lastMessageId, id);
                scrollToBottom();
            }
        });
        new Thread(task, "forum-send").start();
    }

    // ── Append a message bubble ───────────────────────────────────────────────

    private void appendMessage(ForumMessage msg) {
        boolean isOwn   = msg.getSenderId() == currentUser.getId();
        boolean isAdmin = msg.isAdmin();

        // Outer row
        HBox row = new HBox(10);
        row.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 16, 4, 16));

        // Avatar circle
        String avatarColor = isAdmin
                ? "#f59e0b"  // gold for admins
                : ProfileSection.getRankColor(msg.getSenderRating());

        StackPane avatar = buildAvatar(
                msg.getSenderName(), avatarColor,
                msg.getSenderId(), isAdmin);

        // Message bubble VBox
        VBox bubble = new VBox(4);
        bubble.setMaxWidth(480);
        bubble.setPadding(new Insets(10, 14, 10, 14));

        String bubbleBg = isOwn
                ? "linear-gradient(to bottom right, #0e4a5a, #0c3547)"
                : (isAdmin ? "linear-gradient(to bottom right,#2d1e00,#3b2500)"
                : "#141e30");
        String borderColor = isOwn ? "#06b6d4" : (isAdmin ? "#f59e0b" : "#1e3a4a");

        bubble.setStyle(
                "-fx-background-color: " + bubbleBg + ";" +
                        "-fx-border-color: " + borderColor + ";" +
                        "-fx-border-width: 1; -fx-border-radius: 12 12 " +
                        (isOwn ? "4 12" : "12 4") + ";" +
                        "-fx-background-radius: 12 12 " + (isOwn ? "4 12" : "12 4") + ";"
        );

        // Header: name + role badge + time
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label nameLbl = new Label(isOwn ? "You" : msg.getSenderName());
        nameLbl.setStyle("-fx-text-fill: " + avatarColor + "; -fx-font-size: 12px; -fx-font-weight: 700;");

        // Role badge
        Label roleBadge = isAdmin
                ? buildBadge("ADMIN", "#f59e0b", "#3b2500")
                : buildBadge("STUDENT", "#06b6d4", "#0c2a36");

        Label timeLbl = new Label(msg.getSentAt().format(TIME_FMT));
        timeLbl.setStyle("-fx-text-fill: #475569; -fx-font-size: 10px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(nameLbl, roleBadge, spacer, timeLbl);

        // Content
        Label contentLbl = new Label(msg.getContent());
        contentLbl.setWrapText(true);
        contentLbl.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 13.5px;");

        bubble.getChildren().addAll(header, contentLbl);

        // Admin delete context menu on right-click
        boolean canDelete = currentUser.isAdmin() || isOwn;
        if (canDelete) {
            ContextMenu cm = new ContextMenu();
            MenuItem deleteItem = new MenuItem("🗑️  Delete message");
            deleteItem.setStyle("-fx-text-fill: #f87171;");
            deleteItem.setOnAction(ev -> {
                int msgId = msg.getMessageId();
                Task<Boolean> delTask = new Task<>() {
                    @Override protected Boolean call() {
                        return forumService.deleteMessage(msgId);
                    }
                };
                delTask.setOnSucceeded(ev2 -> {
                    if (delTask.getValue()) {
                        Platform.runLater(() -> messagesBox.getChildren().remove(row));
                    }
                });
                new Thread(delTask, "forum-delete").start();
            });
            cm.getItems().add(deleteItem);
            bubble.setOnContextMenuRequested(ev -> cm.show(bubble, ev.getScreenX(), ev.getScreenY()));
        }

        // Assemble row
        if (isOwn) {
            row.getChildren().addAll(bubble, avatar);
        } else {
            row.getChildren().addAll(avatar, bubble);
        }

        // Fade-in animation
        row.setOpacity(0);
        messagesBox.getChildren().add(row);
        FadeTransition ft = new FadeTransition(Duration.millis(200), row);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    // ── Avatar ────────────────────────────────────────────────────────────────

    private StackPane buildAvatar(String name, String color,
                                  int senderId, boolean isAdmin) {
        String initials = getInitials(name);

        Circle bg = new Circle(20);
        bg.setFill(Color.web(color));

        Circle inner = new Circle(17);
        inner.setFill(Color.web("#0d1428"));

        Label lbl = new Label(initials);
        lbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11px; -fx-font-weight: 800;");

        StackPane stack = new StackPane(bg, inner, lbl);
        stack.setMinSize(40, 40);
        stack.setMaxSize(40, 40);
        stack.setCursor(Cursor.HAND);

        // Glow
        javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
        glow.setColor(Color.web(color, 0.5));
        glow.setRadius(10);
        stack.setEffect(glow);

        // Context menu on click — students can view other students' profiles
        // Students cannot view admin profiles
        ContextMenu cm = new ContextMenu();

        if (!isAdmin) {
            // Show profile option for student senders
            MenuItem profileItem = new MenuItem("👤  View Profile");
            profileItem.setOnAction(e -> showStudentProfilePopup(senderId, name, color));
            cm.getItems().add(profileItem);
        }

        stack.setOnMouseClicked(e -> {
            if (!cm.getItems().isEmpty()) {
                cm.show(stack, e.getScreenX(), e.getScreenY());
            }
        });

        return stack;
    }

    // ── Student profile popup ─────────────────────────────────────────────────

    private void showStudentProfilePopup(int studentId, String name, String rankColor) {
        // Load stats off UI thread
        Task<int[]> task = new Task<>() {
            @Override protected int[] call() throws Exception {
                int[] stats = new int[4]; // [rating, exams, passed, rating]
                try (java.sql.Connection conn = com.examverse.config.DatabaseConfig.getConnection()) {
                    // Rating
                    try (java.sql.PreparedStatement ps = conn.prepareStatement(
                            "SELECT current_rating, contests_participated, contests_won FROM student_ratings WHERE student_id = ?")) {
                        ps.setInt(1, studentId);
                        java.sql.ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                            stats[0] = rs.getInt("current_rating");
                            stats[2] = rs.getInt("contests_participated");
                            stats[3] = rs.getInt("contests_won");
                        }
                    }
                    // Exam attempts
                    try (java.sql.PreparedStatement ps = conn.prepareStatement(
                            "SELECT COUNT(*) FROM student_exam_attempts WHERE student_id = ? AND status='COMPLETED'")) {
                        ps.setInt(1, studentId);
                        java.sql.ResultSet rs = ps.executeQuery();
                        if (rs.next()) stats[1] = rs.getInt(1);
                    }
                }
                return stats;
            }
        };

        task.setOnSucceeded(e -> {
            int[] stats = task.getValue();
            int rating = stats[0];
            String rc  = ProfileSection.getRankColor(rating);

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Student Profile");

            DialogPane dp = dialog.getDialogPane();
            dp.setStyle(
                    "-fx-background-color: #0d1428;" +
                            "-fx-border-color: " + rc + ";" +
                            "-fx-border-width: 1.5; -fx-border-radius: 16; -fx-background-radius: 16;"
            );
            dp.getButtonTypes().add(ButtonType.CLOSE);

            // Style close button
            dialog.setOnShown(ev -> {
                javafx.scene.Node closeBtn = dp.lookupButton(ButtonType.CLOSE);
                if (closeBtn != null) closeBtn.setStyle(
                        "-fx-background-color: #1e2a46; -fx-text-fill: #94a3b8;" +
                                "-fx-font-size: 13px; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 20;"
                );
            });

            // Content
            VBox content = new VBox(18);
            content.setPadding(new Insets(28, 32, 12, 32));
            content.setAlignment(Pos.CENTER);

            // Avatar
            StackPane avt = new StackPane();
            Circle c1 = new Circle(38); c1.setFill(Color.web(rc));
            Circle c2 = new Circle(32); c2.setFill(Color.web("#0d1428"));
            Label  il = new Label(getInitials(name));
            il.setStyle("-fx-text-fill: " + rc + "; -fx-font-size: 22px; -fx-font-weight: 800;");
            avt.getChildren().addAll(c1, c2, il);

            Label nameLbl = new Label(name);
            nameLbl.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 18px; -fx-font-weight: 700;");

            // Rank badge
            com.examverse.model.user.StudentRating sr = new com.examverse.model.user.StudentRating();
            sr.setCurrentRating(rating);
            Label rankLbl = new Label(sr.getRankTitle());
            rankLbl.setStyle(
                    "-fx-text-fill: " + rc + "; -fx-font-size: 13px; -fx-font-weight: 700;" +
                            "-fx-padding: 4 12; -fx-background-radius: 20; -fx-border-radius: 20;" +
                            "-fx-border-color: " + rc + "; -fx-border-width: 1;"
            );
            rankLbl.setBackground(new Background(new BackgroundFill(
                    Color.web(rc, 0.12), new CornerRadii(20), Insets.EMPTY)));

            // Stats grid
            HBox statsRow = new HBox(14);
            statsRow.setAlignment(Pos.CENTER);
            statsRow.getChildren().addAll(
                    miniStat("⭐ Rating",   String.valueOf(rating), rc),
                    miniStat("📝 Exams",    String.valueOf(stats[1]), "#22d3ee"),
                    miniStat("🏆 Contests", String.valueOf(stats[2]), "#f59e0b"),
                    miniStat("🥇 Wins",     String.valueOf(stats[3]), "#34d399")
            );

            content.getChildren().addAll(avt, nameLbl, rankLbl, statsRow);
            dp.setContent(content);

            dialog.showAndWait();
        });

        new Thread(task, "profile-load").start();
    }

    private VBox miniStat(String label, String value, String color) {
        VBox box = new VBox(4);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10, 14, 10, 14));
        box.setStyle(
                "-fx-background-color: rgba(6,182,212,0.06);" +
                        "-fx-border-color: #1e2a46; -fx-border-width: 1;" +
                        "-fx-border-radius: 10; -fx-background-radius: 10;"
        );
        Label v = new Label(value);
        v.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 18px; -fx-font-weight: 800;");
        Label l = new Label(label);
        l.setStyle("-fx-text-fill: #64748b; -fx-font-size: 10px;");
        box.getChildren().addAll(v, l);
        return box;
    }

    // ── Online users sidebar ──────────────────────────────────────────────────

    private void refreshOnlineUsers() {
        Task<List<ForumMessage>> task = new Task<>() {
            @Override protected List<ForumMessage> call() {
                return forumService.fetchRecentSenders(CHANNEL);
            }
        };
        task.setOnSucceeded(e -> {
            List<ForumMessage> senders = task.getValue();
            Platform.runLater(() -> {
                onlineUsersBox.getChildren().clear();
                onlineCountLabel.setText(senders.size() + " online");
                for (ForumMessage s : senders) {
                    boolean isAdm = s.isAdmin();
                    String col = isAdm
                            ? "#f59e0b"
                            : ProfileSection.getRankColor(s.getSenderRating());
                    HBox userRow = new HBox(10);
                    userRow.setAlignment(Pos.CENTER_LEFT);
                    userRow.setPadding(new Insets(6, 12, 6, 12));
                    userRow.setCursor(Cursor.HAND);
                    userRow.setStyle("-fx-background-radius: 8;");
                    userRow.setOnMouseEntered(ev -> userRow.setStyle(
                            "-fx-background-color: rgba(6,182,212,0.07); -fx-background-radius: 8;"));
                    userRow.setOnMouseExited(ev -> userRow.setStyle("-fx-background-radius: 8;"));

                    // Mini avatar
                    StackPane av = new StackPane();
                    Circle c1 = new Circle(14); c1.setFill(Color.web(col));
                    Circle c2 = new Circle(11); c2.setFill(Color.web("#0d1428"));
                    Label  il = new Label(getInitials(s.getSenderName()));
                    il.setStyle("-fx-text-fill: " + col + "; -fx-font-size: 8px; -fx-font-weight: 800;");
                    av.getChildren().addAll(c1, c2, il);

                    // Online dot
                    Circle onlineDot = new Circle(4, Color.web("#22c55e"));

                    Label nameLbl = new Label(s.getSenderName());
                    nameLbl.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px;");

                    Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

                    Label roleTag = isAdm
                            ? buildBadge("ADMIN", "#f59e0b", "#3b2500")
                            : buildBadge("STU", "#06b6d4", "#0c2a36");

                    userRow.getChildren().addAll(av, onlineDot, nameLbl, sp, roleTag);
                    onlineUsersBox.getChildren().add(userRow);
                }
            });
        });
        new Thread(task, "forum-online").start();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Label buildBadge(String text, String fg, String bg) {
        Label l = new Label(text);
        l.setStyle(
                "-fx-text-fill: " + fg + "; -fx-font-size: 9px; -fx-font-weight: 800;" +
                        "-fx-padding: 2 7 2 7; -fx-background-radius: 10; -fx-border-radius: 10;" +
                        "-fx-border-color: " + fg + "; -fx-border-width: 1;"
        );
        l.setBackground(new Background(new BackgroundFill(
                Color.web(fg, 0.15), new CornerRadii(10), Insets.EMPTY)));
        return l;
    }

    private void loadOwnRating() {
        Task<Integer> task = new Task<>() {
            @Override protected Integer call() throws Exception {
                try (java.sql.Connection conn = com.examverse.config.DatabaseConfig.getConnection();
                     java.sql.PreparedStatement ps = conn.prepareStatement(
                             "SELECT current_rating FROM student_ratings WHERE student_id = ?")) {
                    ps.setInt(1, currentUser.getId());
                    java.sql.ResultSet rs = ps.executeQuery();
                    if (rs.next()) return rs.getInt(1);
                } catch (Exception ignored) {}
                return 0;
            }
        };
        task.setOnSucceeded(e -> currentRating = task.getValue());
        new Thread(task, "forum-own-rating").start();
    }

    private void scrollToBottom() {
        Platform.runLater(() -> {
            if (messagesScroll != null) {
                messagesScroll.layout();
                messagesScroll.setVvalue(1.0);
            }
        });
    }

    private static String getInitials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] p = name.trim().split("\\s+");
        if (p.length == 1) return p[0].substring(0, Math.min(2, p[0].length())).toUpperCase();
        return ("" + p[0].charAt(0) + p[p.length - 1].charAt(0)).toUpperCase();
    }
}