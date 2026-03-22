package com.examverse.controller.dashboard.sections;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;

/**
 * DashboardUIFactory
 * Shared UI component factory for all Student Dashboard sections.
 * All methods are static — import once, call anywhere.
 */
public final class DashboardUIFactory {

    private DashboardUIFactory() {}

    // ── Stat card ─────────────────────────────────────────────────────────────

    public static VBox statCard(String icon, String label, String value,
                                String mainColor, String gradTo) {
        VBox card = new VBox(10);
        card.setPrefWidth(220);
        card.setPrefHeight(120);
        card.setPadding(new Insets(20));
        card.setAlignment(Pos.CENTER_LEFT);
        String base = """
            -fx-background-color: linear-gradient(135deg,rgba(30,41,59,0.85)0%%,rgba(15,23,42,0.9)100%%);
            -fx-background-radius: 14;
            -fx-border-color: rgba(51,65,85,0.45);
            -fx-border-width: 1; -fx-border-radius: 14;
            -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.35),12,0.3,0,3);
            """;
        card.setStyle(base);

        Label iconL  = new Label(icon);  iconL.setStyle("-fx-font-size: 28px;");
        Label nameL  = new Label(label); nameL.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-font-weight: 600;");
        Label valL   = new Label(value);
        valL.setStyle("-fx-text-fill: " + mainColor + "; -fx-font-size: 26px; -fx-font-weight: 800;");

        card.getChildren().addAll(iconL, nameL, valL);

        String hover = String.format("""
            -fx-background-color: linear-gradient(135deg,rgba(30,41,59,0.9)0%%,rgba(15,23,42,0.95)100%%);
            -fx-background-radius: 14;
            -fx-border-color: %s;
            -fx-border-width: 1; -fx-border-radius: 14;
            -fx-effect: dropshadow(gaussian,%s,18,0.4,0,4);
            """, mainColor, mainColor);

        card.setOnMouseEntered(e -> card.setStyle(hover));
        card.setOnMouseExited(e  -> card.setStyle(base));
        return card;
    }

    // ── Glass card ────────────────────────────────────────────────────────────

    public static VBox glassCard() {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20));
        card.setStyle("""
            -fx-background-color: rgba(30,41,59,0.65);
            -fx-background-radius: 12;
            -fx-border-color: rgba(51,65,85,0.4);
            -fx-border-width: 1; -fx-border-radius: 12;
            """);
        return card;
    }

    // ── Section labels ────────────────────────────────────────────────────────

    public static Label sectionTitle(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: white; -fx-font-size: 26px; -fx-font-weight: 800;");
        return l;
    }

    public static Label sectionSubtitle(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 17px; -fx-font-weight: 700;");
        return l;
    }

    // ── Divider ───────────────────────────────────────────────────────────────

    public static Separator divider() {
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: rgba(51,65,85,0.3); -fx-opacity: 0.5;");
        return sep;
    }

    // ── Info chip (icon + text row) ───────────────────────────────────────────

    public static HBox infoChip(String icon, String value) {
        HBox box = new HBox(5);
        box.setAlignment(Pos.CENTER_LEFT);
        Label ic = new Label(icon); ic.setStyle("-fx-font-size: 13px;");
        Label vl = new Label(value);
        vl.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px; -fx-font-weight: 600;");
        box.getChildren().addAll(ic, vl);
        return box;
    }

    // ── Badge pill ────────────────────────────────────────────────────────────

    public static Label badge(String text, String bg, String fg) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg
                + "; -fx-padding: 4 12; -fx-background-radius: 20;"
                + " -fx-font-size: 12px; -fx-font-weight: 600;");
        return l;
    }

    // ── Buttons ───────────────────────────────────────────────────────────────

    public static Button quickActionBtn(String text, String bg, String hoverBg) {
        Button btn = new Button(text);
        String base = "-fx-background-color: " + bg + "; -fx-text-fill: #0f172a;"
                + " -fx-font-size: 13px; -fx-font-weight: 700;"
                + " -fx-padding: 12 22; -fx-background-radius: 9; -fx-cursor: hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(base.replace(bg, hoverBg)));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        return btn;
    }

    public static Button miniBtn(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: rgba(34,211,238,0.12);"
                + " -fx-text-fill: " + color + "; -fx-font-size: 13px; -fx-font-weight: 700;"
                + " -fx-padding: 8 16; -fx-background-radius: 7; -fx-cursor: hand;"
                + " -fx-border-color: " + color + "; -fx-border-width: 1; -fx-border-radius: 7;");
        return btn;
    }

    // ── Tab buttons ───────────────────────────────────────────────────────────

    public static Button tabButton(String text) {
        Button btn = new Button(text);
        applyTabStyle(btn, false);
        return btn;
    }

    public static void setTabActive(Button active, Button... others) {
        applyTabStyle(active, true);
        for (Button b : others) applyTabStyle(b, false);
    }

    private static void applyTabStyle(Button btn, boolean active) {
        if (active) {
            btn.setStyle("""
                -fx-background-color: #22d3ee; -fx-text-fill: #0f172a;
                -fx-font-size: 14px; -fx-font-weight: 700;
                -fx-padding: 10 22; -fx-background-radius: 8; -fx-cursor: hand;
                -fx-effect: dropshadow(gaussian,rgba(34,211,238,0.4),10,0.4,0,2);
                """);
        } else {
            btn.setStyle("""
                -fx-background-color: rgba(30,41,59,0.6); -fx-text-fill: #94a3b8;
                -fx-font-size: 14px; -fx-font-weight: 600;
                -fx-padding: 10 22; -fx-background-radius: 8; -fx-cursor: hand;
                -fx-border-color: rgba(51,65,85,0.5); -fx-border-width: 1; -fx-border-radius: 8;
                """);
        }
    }

    // ── Styled combo box ──────────────────────────────────────────────────────

    public static ComboBox<String> styledCombo() {
        ComboBox<String> cb = new ComboBox<>();
        cb.setStyle("""
            -fx-background-color: rgba(30,41,59,0.85); -fx-text-fill: white;
            -fx-font-size: 13px; -fx-background-radius: 7;
            -fx-border-color: rgba(51,65,85,0.55); -fx-border-width: 1;
            -fx-border-radius: 7; -fx-padding: 6 10;
            """);
        cb.setPrefWidth(170);
        return cb;
    }
}