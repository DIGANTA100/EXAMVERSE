package com.examverse.controller.admin;

import com.examverse.config.DatabaseConfig;
import com.examverse.service.dashboard.NotificationService;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AdminNotificationSender — floating panel for admins to send notifications.
 *
 * Features:
 *  • Broadcast to ALL students  or  target a specific student
 *  • Notification type: INFO / SUCCESS / WARNING / EXAM / CONTEST
 *  • Title + message fields
 *  • Uses JavaFX Popup for reliable z-order (same fix as NotificationPanel)
 *  • Positioned anchored to the "send" button in the admin header
 */
public class AdminNotificationSender {

    private final NotificationService service = new NotificationService();
    private final Node                anchorNode;

    private Popup   popup;
    private boolean visible = false;

    // Student name → id map (loaded once when panel opens)
    private final Map<String, Integer> studentMap = new LinkedHashMap<>();

    public AdminNotificationSender(Node anchorNode) {
        this.anchorNode = anchorNode;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void toggle() {
        if (visible) close();
        else         open();
    }

    public void close() {
        if (popup == null || !visible) return;
        visible = false;
        Popup p = popup;
        popup = null;
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

    // ── Open ──────────────────────────────────────────────────────────────────

    private void open() {
        visible = true;
        loadStudents();

        // ── Card ──────────────────────────────────────────────────────────────
        VBox card = new VBox(0);
        card.setPrefWidth(420);
        card.setMaxWidth(420);
        card.setStyle(
                "-fx-background-color: #0d1428;" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: #f59e0b55;" +
                        "-fx-border-width: 1.5; -fx-border-radius: 16;" +
                        "-fx-effect: dropshadow(gaussian,#000000cc,36,0.55,0,8);"
        );

        // ── Header ────────────────────────────────────────────────────────────
        HBox header = new HBox(0);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 18, 14, 18));
        header.setStyle(
                "-fx-background-color: #0a0e1a;" +
                        "-fx-background-radius: 16 16 0 0;" +
                        "-fx-border-color: transparent transparent #2d2000 transparent;" +
                        "-fx-border-width: 0 0 1 0;"
        );

        Label title = new Label("📢  Send Notification");
        title.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 15px; -fx-font-weight: 700;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #475569;" +
                        "-fx-font-size: 13px; -fx-cursor: hand; -fx-border-width: 0;"
        );
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #e2e8f0;" +
                        "-fx-font-size: 13px; -fx-cursor: hand; -fx-border-width: 0;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #475569;" +
                        "-fx-font-size: 13px; -fx-cursor: hand; -fx-border-width: 0;"));
        closeBtn.setOnAction(e -> close());

        header.getChildren().addAll(title, spacer, closeBtn);

        // ── Form body ─────────────────────────────────────────────────────────
        VBox body = new VBox(16);
        body.setPadding(new Insets(20, 20, 20, 20));
        body.setStyle("-fx-background-color: transparent;");

        // ── Target: All / Specific student ────────────────────────────────────
        Label targetLbl = fieldLabel("👥  Send to");

        ToggleGroup targetGroup = new ToggleGroup();
        RadioButton allBtn = styledRadio("All Students", targetGroup);
        RadioButton oneBtn = styledRadio("Specific Student", targetGroup);
        allBtn.setSelected(true);

        HBox targetRow = new HBox(16, allBtn, oneBtn);
        targetRow.setAlignment(Pos.CENTER_LEFT);

        // Student picker (hidden until "Specific Student" is selected)
        ComboBox<String> studentPicker = new ComboBox<>();
        studentPicker.setPromptText("Select a student…");
        studentPicker.setMaxWidth(Double.MAX_VALUE);
        styleComboBox(studentPicker);
        studentPicker.setVisible(false);
        studentPicker.setManaged(false);

        // Populate picker when opened
        studentPicker.setOnShowing(e -> {
            if (studentPicker.getItems().isEmpty()) {
                studentPicker.getItems().addAll(studentMap.keySet());
            }
        });

        oneBtn.setOnAction(e -> {
            studentPicker.setVisible(true);
            studentPicker.setManaged(true);
        });
        allBtn.setOnAction(e -> {
            studentPicker.setVisible(false);
            studentPicker.setManaged(false);
        });

        // ── Type ──────────────────────────────────────────────────────────────
        Label typeLbl = fieldLabel("🏷️  Type");
        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.getItems().addAll("INFO", "SUCCESS", "WARNING", "EXAM", "CONTEST");
        typeBox.setValue("INFO");
        typeBox.setMaxWidth(Double.MAX_VALUE);
        styleComboBox(typeBox);

        // ── Title field ───────────────────────────────────────────────────────
        Label titleLbl = fieldLabel("📌  Title");
        TextField titleField = new TextField();
        titleField.setPromptText("Notification title…");
        styleTextField(titleField);

        // ── Message field ─────────────────────────────────────────────────────
        Label msgLbl = fieldLabel("💬  Message");
        TextArea msgField = new TextArea();
        msgField.setPromptText("Write your message here…");
        msgField.setPrefRowCount(4);
        msgField.setWrapText(true);
        styleTextArea(msgField);

        // ── Status label (success/error feedback) ─────────────────────────────
        Label statusLbl = new Label("");
        statusLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: 600;");
        statusLbl.setWrapText(true);
        statusLbl.setVisible(false);

        // ── Send button ───────────────────────────────────────────────────────
        Button sendBtn = new Button("📢  Send Notification");
        sendBtn.setMaxWidth(Double.MAX_VALUE);
        sendBtn.setStyle(
                "-fx-background-color: linear-gradient(to right,#f59e0b,#d97706);" +
                        "-fx-text-fill: #0a0500; -fx-font-size: 14px; -fx-font-weight: 700;" +
                        "-fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 12 0 12 0;" +
                        "-fx-effect: dropshadow(gaussian,#f59e0b60,8,0.4,0,2);"
        );
        sendBtn.setOnMouseEntered(e -> sendBtn.setStyle(
                "-fx-background-color: linear-gradient(to right,#fbbf24,#f59e0b);" +
                        "-fx-text-fill: #0a0500; -fx-font-size: 14px; -fx-font-weight: 700;" +
                        "-fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 12 0 12 0;" +
                        "-fx-effect: dropshadow(gaussian,#f59e0b80,12,0.5,0,3);"));
        sendBtn.setOnMouseExited(e -> sendBtn.setStyle(
                "-fx-background-color: linear-gradient(to right,#f59e0b,#d97706);" +
                        "-fx-text-fill: #0a0500; -fx-font-size: 14px; -fx-font-weight: 700;" +
                        "-fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 12 0 12 0;" +
                        "-fx-effect: dropshadow(gaussian,#f59e0b60,8,0.4,0,2);"));

        sendBtn.setOnAction(e -> {
            String t   = titleField.getText().trim();
            String msg = msgField.getText().trim();
            String type = typeBox.getValue();
            boolean toAll = allBtn.isSelected();

            // Validation
            if (t.isEmpty()) {
                showStatus(statusLbl, "⚠️  Please enter a title.", "#f59e0b");
                return;
            }
            if (msg.isEmpty()) {
                showStatus(statusLbl, "⚠️  Please enter a message.", "#f59e0b");
                return;
            }
            if (!toAll && studentPicker.getValue() == null) {
                showStatus(statusLbl, "⚠️  Please select a student.", "#f59e0b");
                return;
            }

            // Determine target
            Integer targetId = null;
            if (!toAll) {
                String selectedName = studentPicker.getValue();
                targetId = studentMap.get(selectedName);
                if (targetId == null) {
                    showStatus(statusLbl, "⚠️  Student not found.", "#f59e0b");
                    return;
                }
            }

            // Send
            sendBtn.setDisable(true);
            sendBtn.setText("Sending…");
            final Integer finalTargetId = targetId;

            new Thread(() -> {
                boolean ok = service.sendNotification(t, msg, type, finalTargetId);
                Platform.runLater(() -> {
                    sendBtn.setDisable(false);
                    sendBtn.setText("📢  Send Notification");
                    if (ok) {
                        showStatus(statusLbl,
                                "✅  Notification sent to " +
                                        (toAll ? "all students" : studentPicker.getValue()) + ".",
                                "#22c55e");
                        titleField.clear();
                        msgField.clear();
                        typeBox.setValue("INFO");
                        allBtn.setSelected(true);
                        studentPicker.setVisible(false);
                        studentPicker.setManaged(false);
                    } else {
                        showStatus(statusLbl, "❌  Failed to send. Check DB connection.", "#ef4444");
                    }
                });
            }, "admin-notif-send").start();
        });

        body.getChildren().addAll(
                targetLbl, targetRow, studentPicker,
                typeLbl, typeBox,
                titleLbl, titleField,
                msgLbl, msgField,
                statusLbl,
                sendBtn
        );

        card.getChildren().addAll(header, body);

        // ── Popup setup ───────────────────────────────────────────────────────
        popup = new Popup();
        popup.setAutoHide(true);
        popup.setAutoFix(false);
        popup.getContent().add(card);
        popup.setOnHidden(e -> {
            visible = false;
            popup = null;
        });

        Window window = anchorNode.getScene().getWindow();
        double screenX = window.getX() + window.getWidth() - 460;
        double screenY = window.getY() + 72;
        popup.show(window, screenX, screenY);

        // Inject CSS into the Popup scene AFTER show() so the scene exists.
        // We write a real temp CSS file because JavaFX's TextArea .content
        // sub-node cannot be styled via inline setStyle() — Modena overrides it.
        // A proper stylesheet URL is the only reliable fix.
        javafx.application.Platform.runLater(() -> {
            if (popup == null || popup.getScene() == null) return;
            try {
                java.io.File tmp = java.io.File.createTempFile("ev_admin_notif_", ".css");
                tmp.deleteOnExit();
                java.nio.file.Files.writeString(tmp.toPath(),
                        ".text-area .content { -fx-background-color: #0a1020; }\n" +
                                ".text-area { -fx-background-color: #0a1020; -fx-text-fill: #e2e8f0; }\n" +
                                ".text-area .text { -fx-fill: #e2e8f0; }\n" +
                                ".text-area:focused .content { -fx-background-color: #0d1628; }\n" +
                                ".text-area .scroll-pane { -fx-background-color: #0a1020; }\n" +
                                ".text-area .scroll-pane .viewport { -fx-background-color: #0a1020; }\n" +
                                ".text-field { -fx-background-color: #0a1020; -fx-text-fill: #e2e8f0; }\n" +
                                ".text-field .text { -fx-fill: #e2e8f0; }\n" +
                                ".combo-box-base { -fx-background-color: #0a1020; }\n" +
                                ".combo-box-base .list-cell { -fx-background-color: transparent; -fx-text-fill: #e2e8f0; }\n" +
                                ".combo-box-base .arrow-button { -fx-background-color: transparent; }\n" +
                                ".combo-box-base .arrow { -fx-background-color: #64748b; }\n" +
                                ".scroll-bar { -fx-background-color: #0d1428; }\n" +
                                ".scroll-bar .thumb { -fx-background-color: #1e3a5a; -fx-background-radius: 4; }\n" +
                                ".scroll-bar .track { -fx-background-color: #0a1020; -fx-background-radius: 4; }\n" +
                                ".scroll-bar .track-background { -fx-background-color: #0a1020; }\n" +
                                ".scroll-bar .increment-button, .scroll-bar .decrement-button { -fx-background-color: transparent; -fx-padding: 0; }\n" +
                                ".scroll-bar .increment-arrow, .scroll-bar .decrement-arrow { -fx-shape: ' '; -fx-padding: 0; }\n" +
                                ".radio-button .radio { -fx-border-color: #334155; -fx-background-color: #0a1020; }\n" +
                                ".radio-button:selected .radio { -fx-border-color: #06b6d4; }\n" +
                                ".radio-button:selected .dot { -fx-background-color: #06b6d4; }\n"
                );
                popup.getScene().getStylesheets().add(tmp.toURI().toURL().toExternalForm());
            } catch (Exception ex) {
                System.err.println("❌ AdminNotificationSender CSS inject failed: " + ex.getMessage());
            }
        });

        window.xProperty().addListener((obs, ov, nv) -> {
            if (popup != null && popup.isShowing())
                popup.setX(nv.doubleValue() + window.getWidth() - 460);
        });
        window.widthProperty().addListener((obs, ov, nv) -> {
            if (popup != null && popup.isShowing())
                popup.setX(window.getX() + nv.doubleValue() - 460);
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

    // ── Load students from DB ─────────────────────────────────────────────────

    private void loadStudents() {
        studentMap.clear();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, full_name, username FROM users " +
                             "WHERE user_type = 'STUDENT' AND is_active = 1 " +
                             "ORDER BY full_name")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String display = rs.getString("full_name") +
                        "  (@" + rs.getString("username") + ")";
                studentMap.put(display, rs.getInt("id"));
            }
        } catch (Exception e) {
            System.err.println("❌ AdminNotificationSender.loadStudents: " + e.getMessage());
        }
    }

    // ── Status feedback ───────────────────────────────────────────────────────

    private void showStatus(Label lbl, String text, String color) {
        lbl.setText(text);
        lbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px; -fx-font-weight: 600;");
        lbl.setVisible(true);
        // Auto-hide after 4 s
        PauseTransition pt = new PauseTransition(Duration.seconds(4));
        pt.setOnFinished(e -> lbl.setVisible(false));
        pt.play();
    }

    // ── Style helpers ─────────────────────────────────────────────────────────

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px; -fx-font-weight: 700;");
        VBox.setMargin(l, new Insets(-4, 0, -8, 0));
        return l;
    }

    private void styleTextField(TextField f) {
        f.setStyle(
                "-fx-background-color: #0a1020;" +
                        "-fx-text-fill: #e2e8f0; -fx-prompt-text-fill: #2d4060;" +
                        "-fx-border-color: #1e2a46; -fx-border-width: 1;" +
                        "-fx-border-radius: 8; -fx-background-radius: 8;" +
                        "-fx-font-size: 13px; -fx-padding: 10 12 10 12;"
        );
        f.focusedProperty().addListener((obs, ov, focused) -> f.setStyle(
                "-fx-background-color: #0a1020;" +
                        "-fx-text-fill: #e2e8f0; -fx-prompt-text-fill: #2d4060;" +
                        "-fx-border-color: " + (focused ? "#06b6d4" : "#1e2a46") + ";" +
                        "-fx-border-width: 1;" +
                        (focused ? "-fx-effect: dropshadow(gaussian,#06b6d430,6,0,0,0);" : "") +
                        "-fx-border-radius: 8; -fx-background-radius: 8;" +
                        "-fx-font-size: 13px; -fx-padding: 10 12 10 12;"
        ));
    }

    private void styleTextArea(TextArea a) {
        a.setStyle(
                "-fx-background-color: #0a1020;" +
                        "-fx-text-fill: #e2e8f0; -fx-prompt-text-fill: #2d4060;" +
                        "-fx-border-color: #1e2a46; -fx-border-width: 1;" +
                        "-fx-border-radius: 8; -fx-background-radius: 8;" +
                        "-fx-font-size: 13px; -fx-padding: 10 12 10 12;"
        );
        a.focusedProperty().addListener((obs, ov, focused) -> a.setStyle(
                "-fx-background-color: #0a1020;" +
                        "-fx-text-fill: #e2e8f0; -fx-prompt-text-fill: #2d4060;" +
                        "-fx-border-color: " + (focused ? "#06b6d4" : "#1e2a46") + ";" +
                        "-fx-border-width: 1;" +
                        (focused ? "-fx-effect: dropshadow(gaussian,#06b6d430,6,0,0,0);" : "") +
                        "-fx-border-radius: 8; -fx-background-radius: 8;" +
                        "-fx-font-size: 13px; -fx-padding: 10 12 10 12;"
        ));
    }

    private void styleComboBox(ComboBox<String> cb) {
        cb.setStyle(
                "-fx-background-color: #0a1020;" +
                        "-fx-text-fill: #e2e8f0;" +
                        "-fx-border-color: #1e2a46; -fx-border-width: 1;" +
                        "-fx-border-radius: 8; -fx-background-radius: 8;" +
                        "-fx-font-size: 13px; -fx-padding: 4 8 4 8;"
        );
    }

    private RadioButton styledRadio(String text, ToggleGroup group) {
        RadioButton rb = new RadioButton(text);
        rb.setToggleGroup(group);
        rb.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
        return rb;
    }
}