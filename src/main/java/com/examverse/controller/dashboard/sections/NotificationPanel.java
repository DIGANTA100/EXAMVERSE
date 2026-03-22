package com.examverse.controller.dashboard.sections;

import com.examverse.model.user.User;
import com.examverse.service.dashboard.NotificationService;
import com.examverse.service.dashboard.NotificationService.Notification;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

/**
 * NotificationPanel — DB-backed popup overlay.
 *
 * ROOT CAUSE FIX:
 * BorderPane.getChildren().add() does NOT work for free-floating overlays —
 * BorderPane manages its children via region slots (left/center/right/top/bottom)
 * and ignores direct children additions for layout purposes, so the panel
 * never appears.
 *
 * SOLUTION: attach to the Scene root Pane via Platform.runLater, which
 * always works for overlay/popup positioning regardless of the scene layout type.
 */
public class NotificationPanel {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("MMM d, h:mm a");

    private final User                user;
    private final Label               badgeLabel;
    private final Node                anchorNode;     // any in-scene node → used to reach scene root
    private final NotificationService service = new NotificationService();
    private final Consumer<Integer>   onUnreadCountChanged;

    private VBox    overlay;
    private boolean visible = false;

    public NotificationPanel(User user, Label badgeLabel, Node anchorNode,
                             Consumer<Integer> onUnreadCountChanged) {
        this.user                 = user;
        this.badgeLabel           = badgeLabel;
        this.anchorNode           = anchorNode;
        this.onUnreadCountChanged = onUnreadCountChanged;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void toggle() {
        if (visible) close(); else open();
    }

    public void refreshBadge() {
        int count = service.getUnreadCount(user.getId());
        if (badgeLabel != null) {
            badgeLabel.setText(String.valueOf(count));
            badgeLabel.setVisible(count > 0);
        }
        if (onUnreadCountChanged != null) onUnreadCountChanged.accept(count);
    }

    // ── Resolve scene root ────────────────────────────────────────────────────

    private Pane getSceneRoot() {
        if (anchorNode == null || anchorNode.getScene() == null) return null;
        javafx.scene.Parent root = anchorNode.getScene().getRoot();
        return (root instanceof Pane) ? (Pane) root : null;
    }

    // ── Open ──────────────────────────────────────────────────────────────────

    private void open() {
        visible = true;
        List<Notification> notifications = service.getNotificationsForStudent(user.getId());

        overlay = new VBox(0);
        overlay.setStyle(
                "-fx-background-color: #06080dfb;" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: #14b8a64d;" +
                        "-fx-border-width: 1; -fx-border-radius: 16;" +
                        "-fx-effect: dropshadow(gaussian,#000000d9,32,0.5,0,10);"
        );
        overlay.setPrefWidth(390);
        overlay.setMaxHeight(500);
        overlay.setPickOnBounds(true);

        // ── Header ────────────────────────────────────────────────────────────
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 18, 14, 18));
        header.setStyle("-fx-border-color: #1e2a4699; -fx-border-width: 0 0 1 0;");

        Label title = new Label("🔔  Notifications");
        title.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 15px; -fx-font-weight: 700;");
        header.getChildren().add(title);

        int unread = (int) notifications.stream().filter(n -> !n.isRead()).count();
        if (unread > 0) {
            Label chip = new Label(String.valueOf(unread));
            chip.setStyle(
                    "-fx-background-color: #ef4444; -fx-text-fill: white;" +
                            "-fx-font-size: 10px; -fx-font-weight: 800;" +
                            "-fx-padding: 2 7; -fx-background-radius: 10;"
            );
            HBox.setMargin(chip, new Insets(0, 0, 0, 8));
            header.getChildren().add(chip);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button markAllBtn = new Button("Mark all read");
        markAllBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #14b8a6;" +
                        "-fx-font-size: 12px; -fx-cursor: hand; -fx-font-weight: 600;"
        );
        markAllBtn.setOnAction(e -> {
            service.markAllRead(user.getId());
            refreshBadge();
            close();
            open();
        });

        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #94a3b8;" +
                        "-fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2 8;" +
                        "-fx-border-width: 0;"
        );
        closeBtn.setOnAction(e -> close());
        HBox.setMargin(closeBtn, new Insets(0, 0, 0, 10));

        header.getChildren().addAll(spacer, markAllBtn, closeBtn);

        // ── Items ─────────────────────────────────────────────────────────────
        VBox itemsBox = new VBox(0);
        if (notifications.isEmpty()) {
            Label empty = new Label("🎉  No notifications yet.\nAdmin messages will appear here.");
            empty.setStyle(
                    "-fx-text-fill: #64748b; -fx-font-size: 13px; -fx-wrap-text: true; -fx-padding: 22 18;"
            );
            empty.setMaxWidth(354);
            empty.setWrapText(true);
            itemsBox.getChildren().add(empty);
        } else {
            for (Notification n : notifications) {
                itemsBox.getChildren().add(buildItem(n));
            }
        }

        ScrollPane scroll = new ScrollPane(itemsBox);
        scroll.setFitToWidth(true);
        scroll.setMaxHeight(390);
        scroll.setStyle(
                "-fx-background-color: transparent; -fx-background: transparent;" +
                        "-fx-border-width: 0;"
        );
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        overlay.getChildren().addAll(header, scroll);

        // ── Attach to SCENE ROOT — the only reliable way for floating overlays ──
        Platform.runLater(() -> {
            Pane sceneRoot = getSceneRoot();
            if (sceneRoot == null) {
                System.err.println("❌ NotificationPanel: scene root unavailable");
                return;
            }

            sceneRoot.getChildren().remove(overlay); // clear any stale one

            double pw = 390;
            overlay.setLayoutX(Math.max(sceneRoot.getWidth() - pw - 14, 4));
            overlay.setLayoutY(68);

            // Keep right-aligned on window resize
            sceneRoot.widthProperty().addListener((obs, ov, nv) -> {
                if (visible && overlay != null)
                    overlay.setLayoutX(nv.doubleValue() - pw - 14);
            });

            sceneRoot.getChildren().add(overlay);
            overlay.toFront();

            // Animate in
            overlay.setOpacity(0);
            overlay.setTranslateY(-10);
            FadeTransition ft = new FadeTransition(Duration.millis(200), overlay);
            ft.setToValue(1);
            TranslateTransition tt = new TranslateTransition(Duration.millis(200), overlay);
            tt.setByY(10);
            new ParallelTransition(ft, tt).play();
        });
    }

    // ── Close ─────────────────────────────────────────────────────────────────

    public void close() {
        if (overlay == null || !visible) return;
        visible = false;
        VBox toRemove = overlay;
        overlay = null;
        FadeTransition ft = new FadeTransition(Duration.millis(150), toRemove);
        ft.setToValue(0);
        ft.setOnFinished(e -> {
            Pane sceneRoot = getSceneRoot();
            if (sceneRoot != null) sceneRoot.getChildren().remove(toRemove);
        });
        ft.play();
    }

    // ── Item builder ──────────────────────────────────────────────────────────

    private HBox buildItem(Notification n) {
        HBox item = new HBox(12);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(13, 18, 13, 18));

        String baseBg = n.isRead() ? "" : " -fx-background-color: #14b8a608;";
        String baseStyle = "-fx-border-color: #1e2a4666; -fx-border-width: 0 0 1 0;" + baseBg;
        item.setStyle(baseStyle);

        Circle dot = new Circle(4);
        if (n.isRead()) {
            dot.setFill(Color.web("#1e293b"));
            dot.setStroke(Color.web("#334155"));
            dot.setStrokeWidth(1.5);
        } else {
            dot.setFill(Color.web(n.getAccentColor()));
            dot.setStroke(Color.web(n.getAccentColor()));
            dot.setStrokeWidth(0);
            dot.setEffect(new javafx.scene.effect.DropShadow(6, Color.web(n.getAccentColor())));
        }

        VBox body = new VBox(4);
        HBox topRow = new HBox(6);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label(n.getIcon());
        icon.setStyle("-fx-font-size: 15px;");

        Label titleLbl = new Label(n.getTitle());
        titleLbl.setStyle(
                "-fx-text-fill: " + (n.isRead() ? "#94a3b8" : "#f1f5f9") + ";" +
                        "-fx-font-size: 13px; -fx-font-weight: " + (n.isRead() ? "500" : "700") + ";"
        );
        topRow.getChildren().addAll(icon, titleLbl);

        Label msg = new Label(n.getMessage());
        msg.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
        msg.setWrapText(true);
        msg.setMaxWidth(310);

        Label time = new Label(n.getCreatedAt() != null ? n.getCreatedAt().format(FMT) : "");
        time.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px;");

        body.getChildren().addAll(topRow, msg, time);
        HBox.setHgrow(body, Priority.ALWAYS);
        item.getChildren().addAll(dot, body);

        String hoverStyle = "-fx-background-color: #14b8a610;" +
                "-fx-border-color: #1e2a4666; -fx-border-width: 0 0 1 0;";
        item.setOnMouseEntered(ev -> item.setStyle(hoverStyle));
        item.setOnMouseExited(ev  -> item.setStyle(baseStyle));
        item.setOnMouseClicked(ev -> {
            service.markRead(n.getId(), user.getId());
            refreshBadge();
            close();
            open();
        });

        return item;
    }
}