package com.examverse.controller.dashboard.sections;

import com.examverse.model.user.User;
import com.examverse.service.dashboard.NotificationService;
import com.examverse.service.dashboard.NotificationService.Notification;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

/**
 * NotificationPanel — DB-backed floating popup.
 *
 * ROOT CAUSE OF THE ORIGINAL BUG:
 * Adding the panel to a BorderPane's children list puts it in the
 * "unmanaged" pool — BorderPane ignores unmanaged children for layout,
 * so they render at (0,0) behind everything else. The "3 dots" the
 * student saw were the AI assistant typing indicator bled through.
 *
 * FIX: use JavaFX Popup, which creates a lightweight floating window
 * that always renders above the scene regardless of layout tree depth,
 * z-order, background layers, or particle overlays.
 */
public class NotificationPanel {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("MMM d, h:mm a");

    private final User                user;
    private final Label               badgeLabel;
    private final Node                anchorNode;
    private final NotificationService service = new NotificationService();
    private final Consumer<Integer>   onUnreadCountChanged;

    private Popup   popup;
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
        if (visible) close();
        else         open();
    }

    public void refreshBadge() {
        int count = service.getUnreadCount(user.getId());
        Platform.runLater(() -> {
            if (badgeLabel != null) {
                badgeLabel.setText(String.valueOf(count));
                badgeLabel.setVisible(count > 0);
            }
            if (onUnreadCountChanged != null) onUnreadCountChanged.accept(count);
        });
    }

    // ── Open ──────────────────────────────────────────────────────────────────

    private void open() {
        visible = true;

        List<Notification> notifications = service.getNotificationsForStudent(user.getId());

        // ── Root card ─────────────────────────────────────────────────────────
        VBox card = new VBox(0);
        card.setPrefWidth(400);
        card.setMaxWidth(400);
        card.setMaxHeight(520);
        card.setStyle(
                "-fx-background-color: #0d1428;" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: #22d3ee33;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-border-radius: 16;" +
                        "-fx-effect: dropshadow(gaussian,#000000cc,36,0.55,0,8);"
        );

        // ── Header ────────────────────────────────────────────────────────────
        HBox header = new HBox(0);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 18, 14, 18));
        header.setStyle(
                "-fx-background-color: #0a1020;" +
                        "-fx-background-radius: 16 16 0 0;" +
                        "-fx-border-color: transparent transparent #1e2a46 transparent;" +
                        "-fx-border-width: 0 0 1 0;"
        );

        Label title = new Label("🔔  Notifications");
        title.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 15px; -fx-font-weight: 700;");

        long unread = notifications.stream().filter(n -> !n.isRead()).count();
        if (unread > 0) {
            Label chip = new Label(String.valueOf(unread));
            chip.setStyle(
                    "-fx-background-color: #ef4444;" +
                            "-fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: 800;" +
                            "-fx-padding: 2 7 2 7; -fx-background-radius: 10;"
            );
            HBox.setMargin(chip, new Insets(0, 0, 0, 8));
            header.getChildren().add(title);
            header.getChildren().add(chip);
        } else {
            header.getChildren().add(title);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button markAllBtn = new Button("Mark all read");
        markAllBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #06b6d4; -fx-font-size: 12px;" +
                        "-fx-cursor: hand; -fx-font-weight: 600; -fx-border-width: 0;"
        );
        markAllBtn.setOnAction(e -> {
            service.markAllRead(user.getId());
            refreshBadge();
            close();
            open();
        });

        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #475569; -fx-font-size: 13px;" +
                        "-fx-cursor: hand; -fx-padding: 2 8; -fx-border-width: 0;"
        );
        closeBtn.setOnMouseEntered(e ->
                closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e2e8f0;" +
                        "-fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 8; -fx-border-width: 0;"));
        closeBtn.setOnMouseExited(e ->
                closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #475569;" +
                        "-fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 8; -fx-border-width: 0;"));
        closeBtn.setOnAction(e -> close());
        HBox.setMargin(closeBtn, new Insets(0, 0, 0, 8));

        header.getChildren().addAll(spacer, markAllBtn, closeBtn);

        // ── Items ─────────────────────────────────────────────────────────────
        VBox itemsBox = new VBox(0);
        itemsBox.setStyle("-fx-background-color: transparent;");

        if (notifications.isEmpty()) {
            VBox emptyState = new VBox(12);
            emptyState.setAlignment(Pos.CENTER);
            emptyState.setPadding(new Insets(36, 24, 36, 24));

            Label emptyIcon = new Label("🔔");
            emptyIcon.setStyle("-fx-font-size: 36px; -fx-opacity: 0.5;");

            Label emptyTitle = new Label("No notifications yet");
            emptyTitle.setStyle(
                    "-fx-text-fill: #94a3b8; -fx-font-size: 14px; -fx-font-weight: 600;");

            Label emptySub = new Label("Admin messages will appear here.");
            emptySub.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");

            emptyState.getChildren().addAll(emptyIcon, emptyTitle, emptySub);
            itemsBox.getChildren().add(emptyState);
        } else {
            for (Notification n : notifications) {
                itemsBox.getChildren().add(buildItem(n));
            }
        }

        ScrollPane scroll = new ScrollPane(itemsBox);
        scroll.setFitToWidth(true);
        scroll.setMaxHeight(410);
        scroll.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-background: transparent;" +
                        "-fx-border-width: 0;"
        );
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        // Style the scrollbar inside
        scroll.getStyleClass().add("notification-scroll");

        card.getChildren().addAll(header, scroll);

        // ── Popup setup ───────────────────────────────────────────────────────
        popup = new Popup();
        popup.setAutoHide(true);  // clicking outside closes it
        popup.setAutoFix(false);
        popup.getContent().add(card);

        // Close popup on auto-hide
        popup.setOnHidden(e -> {
            visible = false;
            popup = null;
        });

        // Inject CSS after show() so the popup scene exists.
        // Fixes scroll bar and any residual white Modena sub-nodes.
        Window window = anchorNode.getScene().getWindow();
        double screenX = window.getX() + window.getWidth() - 420;
        double screenY = window.getY() + 72;
        popup.show(window, screenX, screenY);

        javafx.application.Platform.runLater(() -> {
            if (popup != null && popup.getScene() != null) {
                popup.getScene().getStylesheets().add(
                        "data:text/css," + java.net.URLEncoder.encode(
                                ".scroll-bar { -fx-background-color: #0d1428; }" +
                                        ".scroll-bar .thumb { -fx-background-color: #1e3a5a; -fx-background-radius: 4; }" +
                                        ".scroll-bar .track { -fx-background-color: #0a1020; -fx-background-radius: 4; }" +
                                        ".scroll-bar .track-background { -fx-background-color: #0a1020; }" +
                                        ".scroll-bar .increment-button,.scroll-bar .decrement-button { -fx-background-color: transparent; -fx-padding: 0; }" +
                                        ".scroll-bar .increment-arrow,.scroll-bar .decrement-arrow { -fx-shape: ' '; -fx-padding: 0; }" +
                                        ".scroll-pane { -fx-background-color: transparent; }" +
                                        ".scroll-pane .viewport { -fx-background-color: transparent; }",
                                java.nio.charset.StandardCharsets.UTF_8)
                );
            }
        });

        // Keep position on window move/resize
        window.xProperty().addListener((obs, ov, nv) -> {
            if (popup != null && popup.isShowing())
                popup.setX(nv.doubleValue() + window.getWidth() - 420);
        });
        window.widthProperty().addListener((obs, ov, nv) -> {
            if (popup != null && popup.isShowing())
                popup.setX(window.getX() + nv.doubleValue() - 420);
        });

        // Animate in
        card.setOpacity(0);
        card.setTranslateY(-8);
        FadeTransition ft = new FadeTransition(Duration.millis(200), card);
        ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(200), card);
        tt.setToY(0);
        new ParallelTransition(ft, tt).play();
    }

    // ── Close ─────────────────────────────────────────────────────────────────

    public void close() {
        if (popup == null || !visible) return;
        visible = false;
        Popup p = popup;
        popup = null;
        // Animate out then hide
        if (!p.getContent().isEmpty()) {
            Node card = p.getContent().get(0);
            FadeTransition ft = new FadeTransition(Duration.millis(150), card);
            ft.setToValue(0);
            ft.setOnFinished(e -> p.hide());
            ft.play();
        } else {
            p.hide();
        }
    }

    // ── Item builder ──────────────────────────────────────────────────────────

    private HBox buildItem(Notification n) {
        HBox item = new HBox(12);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(14, 18, 14, 18));

        // Unread items get a subtle tinted left accent bar
        String baseStyle = n.isRead()
                ? "-fx-background-color: transparent;" +
                "-fx-border-color: transparent transparent #1a2236 transparent;" +
                "-fx-border-width: 0 0 1 0;"
                : "-fx-background-color: rgba(6,182,212,0.04);" +
                "-fx-border-color: #06b6d4 transparent #1a2236 transparent;" +
                "-fx-border-width: 0 0 1 3;";

        item.setStyle(baseStyle);

        // Read / unread dot
        Circle dot = new Circle(4);
        if (n.isRead()) {
            dot.setFill(Color.web("#1e293b"));
            dot.setStroke(Color.web("#2d3f5a"));
            dot.setStrokeWidth(1.5);
        } else {
            dot.setFill(Color.web(n.getAccentColor()));
            dot.setEffect(new javafx.scene.effect.DropShadow(
                    6, Color.web(n.getAccentColor())));
        }

        VBox body = new VBox(4);
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label iconLbl = new Label(n.getIcon());
        iconLbl.setStyle("-fx-font-size: 15px;");

        Label titleLbl = new Label(n.getTitle());
        titleLbl.setStyle(
                "-fx-text-fill: " + (n.isRead() ? "#94a3b8" : "#f1f5f9") + ";" +
                        "-fx-font-size: 13px; -fx-font-weight: " + (n.isRead() ? "600" : "700") + ";"
        );

        // Type badge
        Label typeBadge = new Label(n.getType());
        typeBadge.setStyle(
                "-fx-text-fill: " + n.getAccentColor() + ";" +
                        "-fx-font-size: 9px; -fx-font-weight: 800;" +
                        "-fx-padding: 1 6 1 6; -fx-background-radius: 8; -fx-border-radius: 8;" +
                        "-fx-border-color: " + n.getAccentColor() + "; -fx-border-width: 1;"
        );
        typeBadge.setBackground(new Background(new BackgroundFill(
                Color.web(n.getAccentColor(), 0.15), new CornerRadii(8), Insets.EMPTY)));

        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);
        topRow.getChildren().addAll(iconLbl, titleLbl, titleSpacer, typeBadge);

        Label msgLbl = new Label(n.getMessage());
        msgLbl.setStyle("-fx-text-fill: " + (n.isRead() ? "#64748b" : "#cbd5e1") +
                "; -fx-font-size: 12.5px;");
        msgLbl.setWrapText(true);
        msgLbl.setMaxWidth(330);

        Label timeLbl = new Label(n.getCreatedAt() != null
                ? n.getCreatedAt().format(FMT) : "");
        timeLbl.setStyle("-fx-text-fill: #475569; -fx-font-size: 10px;");

        body.getChildren().addAll(topRow, msgLbl, timeLbl);
        HBox.setHgrow(body, Priority.ALWAYS);
        item.getChildren().addAll(dot, body);

        // Hover
        String hoverStyle =
                "-fx-background-color: rgba(6,182,212,0.07);" +
                        "-fx-border-color: #06b6d4 transparent #1a2236 transparent;" +
                        "-fx-border-width: 0 0 1 3;";
        item.setOnMouseEntered(ev -> item.setStyle(hoverStyle));
        item.setOnMouseExited(ev  -> item.setStyle(baseStyle));
        item.setCursor(javafx.scene.Cursor.HAND);

        // Click = mark read and refresh
        item.setOnMouseClicked(ev -> {
            if (!n.isRead()) {
                service.markRead(n.getId(), user.getId());
                refreshBadge();
                close();
                open();
            }
        });

        return item;
    }
}