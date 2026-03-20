package com.examverse.controller.contest;

import com.examverse.model.exam.Contest.Theme;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * ContestSubmitDialog
 *
 * A fully-styled dark modal dialog that replaces the plain white JavaFX Alert
 * shown when a student clicks "Submit Contest".
 *
 * Usage in ContestRoomController (replace the existing Alert call):
 *
 *   ContestSubmitDialog.show(
 *       contest.getTheme(),
 *       contest.getContestTitle(),
 *       () -> {
 *           // existing submit logic here — e.g.:
 *           contestService.submitContest(participantId);
 *           SceneManager.switchScene(".../contest-result.fxml");
 *       }
 *   );
 *
 * The dialog:
 *   - Matches the dark UI palette of the rest of the contest system
 *   - Uses the contest's accent color for the confirm button glow
 *   - Shows the contest title so the student knows exactly what they're submitting
 *   - Has a "Cancel" button to go back without submitting
 *   - Blocks the owner window (APPLICATION_MODAL) until dismissed
 */
public class ContestSubmitDialog {

    /**
     * Shows the styled submit confirmation dialog.
     *
     * @param theme         The contest's theme (used for accent color + glow)
     * @param contestTitle  The contest title shown in the dialog body
     * @param onConfirm     Runnable called on the FX thread when the student
     *                      clicks "✅ Submit". Do all submit logic here.
     */
    public static void show(Theme theme, String contestTitle, Runnable onConfirm) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setResizable(false);

        String accent = theme.getAccentColor();

        // ── Root card ─────────────────────────────────────────────────────────
        VBox root = new VBox(0);
        root.setAlignment(Pos.CENTER);
        root.setStyle(
                "-fx-background-color:#0f172a;" +
                        "-fx-background-radius:16;" +
                        "-fx-border-color:" + accent + ";" +
                        "-fx-border-radius:16;" +
                        "-fx-border-width:1.5;" +
                        "-fx-effect: dropshadow(gaussian," + accent + "66,28,0.4,0,0);"
        );
        root.setMinWidth(420);
        root.setMaxWidth(420);

        // ── Accent top bar ────────────────────────────────────────────────────
        Region topBar = new Region();
        topBar.setPrefHeight(4);
        topBar.setStyle(
                "-fx-background-color: linear-gradient(to right," + accent + ", " +
                        theme.getHighlightColor() + ");" +
                        "-fx-background-radius:14 14 0 0;"
        );

        // ── Body ──────────────────────────────────────────────────────────────
        VBox body = new VBox(16);
        body.setAlignment(Pos.CENTER);
        body.setPadding(new Insets(32, 36, 28, 36));

        Label icon = new Label("⚠️");
        icon.setStyle("-fx-font-size:40px;");

        Label heading = new Label("Submit Contest?");
        heading.setStyle(
                "-fx-text-fill:#f1f5f9;" +
                        "-fx-font-size:20px;" +
                        "-fx-font-weight:bold;"
        );

        Label contestLbl = new Label(contestTitle);
        contestLbl.setStyle(
                "-fx-text-fill:" + accent + ";" +
                        "-fx-font-size:14px;" +
                        "-fx-font-weight:bold;"
        );
        contestLbl.setWrapText(true);
        contestLbl.setMaxWidth(340);
        contestLbl.setAlignment(Pos.CENTER);

        Label subtext = new Label(
                "Once submitted you cannot change your answers.\n" +
                        "Your MCQ results are final. Written answers\nwill be reviewed by the teacher.");
        subtext.setStyle(
                "-fx-text-fill:#64748b;" +
                        "-fx-font-size:13px;" +
                        "-fx-text-alignment:center;"
        );
        subtext.setWrapText(true);
        subtext.setMaxWidth(340);
        subtext.setAlignment(Pos.CENTER);

        // ── Button row ────────────────────────────────────────────────────────
        HBox btnRow = new HBox(14);
        btnRow.setAlignment(Pos.CENTER);
        btnRow.setPadding(new Insets(8, 0, 0, 0));

        // Cancel
        Button cancelBtn = new Button("✕  Cancel");
        String cancelBase =
                "-fx-background-color:#1e293b;" +
                        "-fx-text-fill:#94a3b8;" +
                        "-fx-font-size:14px;" +
                        "-fx-font-weight:bold;" +
                        "-fx-background-radius:10;" +
                        "-fx-padding:11 26 11 26;" +
                        "-fx-cursor:hand;";
        String cancelHover =
                "-fx-background-color:#263548;" +
                        "-fx-text-fill:#e2e8f0;" +
                        "-fx-font-size:14px;" +
                        "-fx-font-weight:bold;" +
                        "-fx-background-radius:10;" +
                        "-fx-padding:11 26 11 26;" +
                        "-fx-cursor:hand;";
        cancelBtn.setStyle(cancelBase);
        cancelBtn.setOnMouseEntered(e -> cancelBtn.setStyle(cancelHover));
        cancelBtn.setOnMouseExited(e  -> cancelBtn.setStyle(cancelBase));
        cancelBtn.setOnAction(e -> dialog.close());

        // Confirm
        Button confirmBtn = new Button("✅  Submit Contest");
        String confirmBase =
                "-fx-background-color: linear-gradient(to right," + accent + "," +
                        theme.getHighlightColor() + ");" +
                        "-fx-text-fill:#000000;" +
                        "-fx-font-size:14px;" +
                        "-fx-font-weight:bold;" +
                        "-fx-background-radius:10;" +
                        "-fx-padding:11 26 11 26;" +
                        "-fx-cursor:hand;" +
                        "-fx-effect: dropshadow(gaussian," + accent + "aa,10,0.5,0,2);";
        String confirmHover =
                "-fx-background-color: linear-gradient(to right," +
                        theme.getHighlightColor() + "," + accent + ");" +
                        "-fx-text-fill:#000000;" +
                        "-fx-font-size:14px;" +
                        "-fx-font-weight:bold;" +
                        "-fx-background-radius:10;" +
                        "-fx-padding:11 26 11 26;" +
                        "-fx-cursor:hand;" +
                        "-fx-effect: dropshadow(gaussian," + accent + "cc,16,0.6,0,3);";
        confirmBtn.setStyle(confirmBase);
        confirmBtn.setOnMouseEntered(e -> confirmBtn.setStyle(confirmHover));
        confirmBtn.setOnMouseExited(e  -> confirmBtn.setStyle(confirmBase));
        confirmBtn.setOnAction(e -> {
            dialog.close();
            onConfirm.run();       // execute the submit logic passed by ContestRoomController
        });

        btnRow.getChildren().addAll(cancelBtn, confirmBtn);
        body.getChildren().addAll(icon, heading, contestLbl, subtext, btnRow);
        root.getChildren().addAll(topBar, body);

        // ── Scene ─────────────────────────────────────────────────────────────
        Scene scene = new Scene(root);
        scene.setFill(null);   // transparent scene background so border-radius shows
        dialog.setScene(scene);

        // Allow Escape to cancel
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) dialog.close();
        });

        dialog.showAndWait();
    }
}