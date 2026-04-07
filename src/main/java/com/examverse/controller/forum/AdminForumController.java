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
 * AdminForumController — Admin-side discussion forum.
 *
 * Two tabs:
 *  1. "Admin Chat"    — ADMIN channel: only admins/teachers see and post here
 *  2. "Student Forum" — GENERAL channel: same feed students see; admins can moderate
 *
 * Admins can delete any message in both channels.
 */
public class AdminForumController implements Initializable {

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private TabPane  tabPane;
    @FXML private Tab      adminChatTab;
    @FXML private Tab      studentChatTab;

    // Admin Chat (ADMIN channel)
    @FXML private VBox       adminMessagesBox;
    @FXML private ScrollPane adminMessagesScroll;
    @FXML private TextField  adminInputField;
    @FXML private Button     adminSendBtn;
    @FXML private VBox       adminOnlineBox;
    @FXML private Label      adminOnlineCount;

    // Student Forum (GENERAL channel)
    @FXML private VBox       genMessagesBox;
    @FXML private ScrollPane genMessagesScroll;
    @FXML private TextField  genInputField;
    @FXML private Button     genSendBtn;
    @FXML private VBox       genOnlineBox;
    @FXML private Label      genOnlineCount;

    // ── State ─────────────────────────────────────────────────────────────────
    private ForumService forumService;
    private User         currentUser;
    private int          lastAdminId = 0;
    private int          lastGenId   = 0;
    private Timeline     pollTimeline;

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm");

    // ── Init ──────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        forumService = new ForumService();
        ForumService.ensureTableExists();

        currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null || !currentUser.isAdmin()) return;

        loadHistory("ADMIN",   adminMessagesBox, 60, id -> lastAdminId = id);
        loadHistory("GENERAL", genMessagesBox,   60, id -> lastGenId   = id);

        adminSendBtn.setOnAction(e -> sendMessage("ADMIN",   adminInputField, adminMessagesBox, adminMessagesScroll));
        adminInputField.setOnAction(e -> sendMessage("ADMIN", adminInputField, adminMessagesBox, adminMessagesScroll));

        genSendBtn.setOnAction(e -> sendMessage("GENERAL", genInputField, genMessagesBox, genMessagesScroll));
        genInputField.setOnAction(e -> sendMessage("GENERAL", genInputField, genMessagesBox, genMessagesScroll));

        startPolling();
    }

    // ── History load ─────────────────────────────────────────────────────────

    @FunctionalInterface
    interface IntConsumer { void accept(int value); }

    private void loadHistory(String channel, VBox box, int limit, IntConsumer lastIdSetter) {
        Task<List<ForumMessage>> task = new Task<>() {
            @Override protected List<ForumMessage> call() {
                return forumService.fetchHistory(channel, limit);
            }
        };
        task.setOnSucceeded(e -> {
            List<ForumMessage> msgs = task.getValue();
            for (ForumMessage m : msgs) appendMessage(m, box, channel);
            if (!msgs.isEmpty()) lastIdSetter.accept(msgs.get(msgs.size() - 1).getMessageId());
            scrollToBottom(channel.equals("ADMIN") ? adminMessagesScroll : genMessagesScroll);
        });
        new Thread(task, "admin-forum-history-" + channel).start();
    }

    // ── Polling ───────────────────────────────────────────────────────────────

    private void startPolling() {
        pollTimeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            pollChannel("ADMIN",   adminMessagesBox, adminMessagesScroll, adminOnlineBox, adminOnlineCount);
            pollChannel("GENERAL", genMessagesBox,   genMessagesScroll,   genOnlineBox,   genOnlineCount);
        }));
        pollTimeline.setCycleCount(Timeline.INDEFINITE);
        pollTimeline.play();
    }

    private void pollChannel(String channel, VBox box, ScrollPane scroll,
                             VBox onlineBox, Label onlineCount) {
        int knownId = channel.equals("ADMIN") ? lastAdminId : lastGenId;
        Task<List<ForumMessage>> task = new Task<>() {
            @Override protected List<ForumMessage> call() {
                return forumService.fetchNewMessages(channel, knownId);
            }
        };
        task.setOnSucceeded(e -> {
            List<ForumMessage> msgs = task.getValue();
            for (ForumMessage m : msgs) {
                appendMessage(m, box, channel);
                if (channel.equals("ADMIN")) lastAdminId = Math.max(lastAdminId, m.getMessageId());
                else                         lastGenId   = Math.max(lastGenId,   m.getMessageId());
            }
            if (!msgs.isEmpty()) scrollToBottom(scroll);
            refreshOnline(channel, onlineBox, onlineCount);
        });
        new Thread(task, "admin-poll-" + channel).start();
    }

    public void stopPolling() {
        if (pollTimeline != null) pollTimeline.stop();
    }

    // ── Send ──────────────────────────────────────────────────────────────────

    private void sendMessage(String channel, TextField input, VBox box, ScrollPane scroll) {
        String text = input.getText();
        if (text == null || text.isBlank()) return;
        input.clear();

        ForumMessage msg = new ForumMessage(
                currentUser.getId(),
                currentUser.getFullName(),
                currentUser.getUsername(),
                "ADMIN",
                0, // admins don't have a student rating
                channel,
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
                appendMessage(msg, box, channel);
                if (channel.equals("ADMIN")) lastAdminId = Math.max(lastAdminId, id);
                else                         lastGenId   = Math.max(lastGenId,   id);
                scrollToBottom(scroll);
            }
        });
        new Thread(task, "admin-forum-send").start();
    }

    // ── Append message ────────────────────────────────────────────────────────

    private void appendMessage(ForumMessage msg, VBox box, String channel) {
        boolean isOwn   = msg.getSenderId() == currentUser.getId();
        boolean isAdmin = msg.isAdmin();

        HBox row = new HBox(10);
        row.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 16, 4, 16));

        String avatarColor = isAdmin ? "#f59e0b"
                : ProfileSection.getRankColor(msg.getSenderRating());

        StackPane avatar = buildAvatar(msg.getSenderName(), avatarColor,
                msg.getSenderId(), isAdmin, channel);

        VBox bubble = new VBox(4);
        bubble.setMaxWidth(480);
        bubble.setPadding(new Insets(10, 14, 10, 14));

        // Admin channel: gold theme; General channel: cyan for students
        String bubbleBg, borderColor;
        if (isOwn) {
            bubbleBg    = channel.equals("ADMIN")
                    ? "linear-gradient(to bottom right, #3b2500, #2d1e00)"
                    : "linear-gradient(to bottom right, #0e4a5a, #0c3547)";
            borderColor = channel.equals("ADMIN") ? "#f59e0b" : "#06b6d4";
        } else {
            bubbleBg    = isAdmin ? "#1a1200" : "#0f1b26";
            borderColor = isAdmin ? "#78350f"  : "#1e3a4a";
        }

        bubble.setStyle(
                "-fx-background-color: " + bubbleBg + ";" +
                        "-fx-border-color: " + borderColor + ";" +
                        "-fx-border-width: 1; -fx-border-radius: 12 12 " +
                        (isOwn ? "4 12" : "12 4") + ";" +
                        "-fx-background-radius: 12 12 " + (isOwn ? "4 12" : "12 4") + ";"
        );

        // Header
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label nameLbl = new Label(isOwn ? "You" : msg.getSenderName());
        nameLbl.setStyle("-fx-text-fill: " + avatarColor + "; -fx-font-size: 12px; -fx-font-weight: 700;");

        Label roleBadge = isAdmin
                ? buildBadge("ADMIN", "#f59e0b", "#3b2500")
                : buildBadge("STUDENT", "#06b6d4", "#0c2a36");

        Label timeLbl = new Label(msg.getSentAt().format(TIME_FMT));
        timeLbl.setStyle("-fx-text-fill: #475569; -fx-font-size: 10px;");

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        header.getChildren().addAll(nameLbl, roleBadge, sp, timeLbl);

        Label contentLbl = new Label(msg.getContent());
        contentLbl.setWrapText(true);
        contentLbl.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 13.5px;");

        bubble.getChildren().addAll(header, contentLbl);

        // Admin can delete any message
        ContextMenu cm = new ContextMenu();
        MenuItem delItem = new MenuItem("🗑️  Delete message");
        delItem.setStyle("-fx-text-fill: #f87171;");
        delItem.setOnAction(ev -> {
            int msgId = msg.getMessageId();
            Task<Boolean> delTask = new Task<>() {
                @Override protected Boolean call() {
                    return forumService.deleteMessage(msgId);
                }
            };
            delTask.setOnSucceeded(ev2 -> {
                if (delTask.getValue())
                    Platform.runLater(() -> box.getChildren().remove(row));
            });
            new Thread(delTask, "admin-delete").start();
        });
        cm.getItems().add(delItem);
        bubble.setOnContextMenuRequested(ev ->
                cm.show(bubble, ev.getScreenX(), ev.getScreenY()));

        if (isOwn) row.getChildren().addAll(bubble, avatar);
        else       row.getChildren().addAll(avatar, bubble);

        row.setOpacity(0);
        box.getChildren().add(row);
        FadeTransition ft = new FadeTransition(Duration.millis(200), row);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    // ── Avatar ────────────────────────────────────────────────────────────────

    private StackPane buildAvatar(String name, String color, int senderId,
                                  boolean senderIsAdmin, String channel) {
        Circle bg    = new Circle(20); bg.setFill(Color.web(color));
        Circle inner = new Circle(17); inner.setFill(Color.web("#0d1428"));
        Label  lbl   = new Label(getInitials(name));
        lbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11px; -fx-font-weight: 800;");

        StackPane stack = new StackPane(bg, inner, lbl);
        stack.setMinSize(40, 40);
        stack.setMaxSize(40, 40);
        stack.setCursor(Cursor.HAND);

        javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
        glow.setColor(Color.web(color, 0.5));
        glow.setRadius(10);
        stack.setEffect(glow);

        // Admins can view student profiles; students can't view admins
        if (!senderIsAdmin) {
            ContextMenu cm = new ContextMenu();
            MenuItem profileItem = new MenuItem("👤  View Profile");
            profileItem.setOnAction(e -> showStudentProfilePopup(senderId, name, color));
            cm.getItems().add(profileItem);
            stack.setOnMouseClicked(e -> cm.show(stack, e.getScreenX(), e.getScreenY()));
        }

        return stack;
    }

    // ── Student profile popup (admin view) ───────────────────────────────────

    private void showStudentProfilePopup(int studentId, String name, String rankColor) {
        Task<int[]> task = new Task<>() {
            @Override protected int[] call() throws Exception {
                int[] stats = new int[4];
                try (java.sql.Connection conn = com.examverse.config.DatabaseConfig.getConnection()) {
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
            String rc   = ProfileSection.getRankColor(stats[0]);

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Student Profile");
            DialogPane dp = dialog.getDialogPane();
            dp.setStyle(
                    "-fx-background-color: #0d1428;" +
                            "-fx-border-color: " + rc + ";" +
                            "-fx-border-width: 1.5; -fx-border-radius: 16; -fx-background-radius: 16;"
            );
            dp.getButtonTypes().add(ButtonType.CLOSE);
            dialog.setOnShown(ev -> {
                javafx.scene.Node btn = dp.lookupButton(ButtonType.CLOSE);
                if (btn != null) btn.setStyle(
                        "-fx-background-color: #1e2a46; -fx-text-fill: #94a3b8;" +
                                "-fx-font-size: 13px; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 20;");
            });

            VBox content = new VBox(18);
            content.setPadding(new Insets(28, 32, 12, 32));
            content.setAlignment(Pos.CENTER);

            StackPane avt = new StackPane();
            Circle c1 = new Circle(38); c1.setFill(Color.web(rc));
            Circle c2 = new Circle(32); c2.setFill(Color.web("#0d1428"));
            Label  il = new Label(getInitials(name));
            il.setStyle("-fx-text-fill: " + rc + "; -fx-font-size: 22px; -fx-font-weight: 800;");
            avt.getChildren().addAll(c1, c2, il);

            Label nameLbl = new Label(name);
            nameLbl.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 18px; -fx-font-weight: 700;");

            com.examverse.model.user.StudentRating sr = new com.examverse.model.user.StudentRating();
            sr.setCurrentRating(stats[0]);
            Label rankLbl = new Label(sr.getRankTitle());
            rankLbl.setStyle(
                    "-fx-text-fill: " + rc + "; -fx-font-size: 13px; -fx-font-weight: 700;" +
                            "-fx-padding: 4 12; -fx-background-radius: 20; -fx-border-radius: 20;" +
                            "-fx-border-color: " + rc + "; -fx-border-width: 1;"
            );
            rankLbl.setBackground(new Background(new BackgroundFill(
                    Color.web(rc, 0.12), new CornerRadii(20), Insets.EMPTY)));

            HBox statsRow = new HBox(14);
            statsRow.setAlignment(Pos.CENTER);
            statsRow.getChildren().addAll(
                    miniStat("⭐ Rating",   String.valueOf(stats[0]), rc),
                    miniStat("📝 Exams",    String.valueOf(stats[1]), "#22d3ee"),
                    miniStat("🏆 Contests", String.valueOf(stats[2]), "#f59e0b"),
                    miniStat("🥇 Wins",     String.valueOf(stats[3]), "#34d399")
            );
            content.getChildren().addAll(avt, nameLbl, rankLbl, statsRow);
            dp.setContent(content);
            dialog.showAndWait();
        });
        new Thread(task, "admin-profile-load").start();
    }

    // ── Online users panel ────────────────────────────────────────────────────

    private void refreshOnline(String channel, VBox box, Label countLbl) {
        Task<List<ForumMessage>> task = new Task<>() {
            @Override protected List<ForumMessage> call() {
                return forumService.fetchRecentSenders(channel);
            }
        };
        task.setOnSucceeded(e -> {
            List<ForumMessage> senders = task.getValue();
            Platform.runLater(() -> {
                box.getChildren().clear();
                countLbl.setText(senders.size() + " online");
                for (ForumMessage s : senders) {
                    boolean isAdm = s.isAdmin();
                    String col    = isAdm ? "#f59e0b"
                            : ProfileSection.getRankColor(s.getSenderRating());
                    HBox row = new HBox(10);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(6, 12, 6, 12));
                    row.setStyle("-fx-background-radius: 8;");
                    row.setOnMouseEntered(ev -> row.setStyle(
                            "-fx-background-color: rgba(6,182,212,0.07); -fx-background-radius: 8;"));
                    row.setOnMouseExited(ev -> row.setStyle("-fx-background-radius: 8;"));

                    StackPane av = new StackPane();
                    Circle c1 = new Circle(14); c1.setFill(Color.web(col));
                    Circle c2 = new Circle(11); c2.setFill(Color.web("#0d1428"));
                    Label  il = new Label(getInitials(s.getSenderName()));
                    il.setStyle("-fx-text-fill: " + col + "; -fx-font-size: 8px; -fx-font-weight: 800;");
                    av.getChildren().addAll(c1, c2, il);

                    Circle dot = new Circle(4, Color.web("#22c55e"));
                    Label  nl  = new Label(s.getSenderName());
                    nl.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px;");
                    Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
                    Label  rl  = isAdm ? buildBadge("ADMIN", "#f59e0b", "#3b2500")
                            : buildBadge("STU", "#06b6d4", "#0c2a36");

                    row.getChildren().addAll(av, dot, nl, sp, rl);
                    box.getChildren().add(row);
                }
            });
        });
        new Thread(task, "admin-online-" + channel).start();
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

    private void scrollToBottom(ScrollPane scroll) {
        Platform.runLater(() -> { scroll.layout(); scroll.setVvalue(1.0); });
    }

    private static String getInitials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] p = name.trim().split("\\s+");
        if (p.length == 1) return p[0].substring(0, Math.min(2, p[0].length())).toUpperCase();
        return ("" + p[0].charAt(0) + p[p.length - 1].charAt(0)).toUpperCase();
    }
}