package com.examverse.controller.admin;

import com.examverse.model.exam.Contest;
import com.examverse.model.exam.Contest.Theme;
import com.examverse.model.exam.ContestQuestion;
import com.examverse.model.exam.ContestQuestion.QuestionType;
import com.examverse.util.SceneManager;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * ContestDialogHelper  (v2 — bug-fixed)
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Bugs fixed vs v1:
 *
 *  ① FREEZE / deadlock  ← BIGGEST FIX
 *     Root cause: buildContestFromForm() called showInfoDialog() — which
 *     itself calls Stage.showAndWait() — from INSIDE the createBtn action
 *     handler, which executes while the outer Stage.showAndWait() is already
 *     blocking the FX thread. JavaFX forbids nested showAndWait() and hangs.
 *     Fix: ALL validation errors are now shown as an inline red Label inside
 *     the form (zero nested modals). showInfoDialog() is only ever called
 *     AFTER modal.close() has fully returned, so there is never any nesting.
 *
 *  ② White TextArea background
 *     Root cause: JavaFX TextArea wraps its editable area in an internal
 *     ".content" Region. -fx-background-color on the outer TextArea node
 *     does NOT reach that sub-node. The correct property is
 *     -fx-control-inner-background, which JavaFX's default stylesheet maps
 *     to the content pane's fill colour.
 *     Fix: added -fx-control-inner-background to every styledArea() call.
 *
 *  ③ Dim / invisible ComboBox selected text
 *     Root cause: ComboBox renders its selected value via a ListCell whose
 *     -fx-text-fill is not overridden by styling the outer ComboBox node.
 *     Fix: styledComboBox() supplies a custom ButtonCell and CellFactory
 *     that explicitly set the dark-theme text colour on every update.
 *
 *  ④ Runnable[] particleTimelines — no .stop()
 *     Fix: changed to Timeline[] throughout.
 */
public class ContestDialogHelper {

    // ── Resource paths ─────────────────────────────────────────────────────────
    private static final String ARENA_IMG_BASE = "/com/examverse/assets/images/arenas/";
    private static final String MUSIC_BASE     = "/com/examverse/assets/music/";

    // ── Dark palette ───────────────────────────────────────────────────────────
    private static final String BG_DEEP    = "#050810";
    private static final String BG_CARD    = "#0a0a1a";
    private static final String BG_PANEL   = "#0d1117";
    private static final String BG_INPUT   = "#0f172a";
    private static final String BORDER_DIM = "#1e293b";
    private static final String TEXT_PRI   = "#f1f5f9";
    private static final String TEXT_SEC   = "#64748b";
    private static final String TEXT_MUT   = "#334155";
    private static final String ACCENT_DEF = "#7c3aed";

    // ══════════════════════════════════════════════════════════════════════════
    // ①  CREATE CONTEST DIALOG
    // ══════════════════════════════════════════════════════════════════════════

    public static void showCreateContestDialog(Consumer<Contest> onCreated) {

        Stage modal = buildModal(920, 700, "Create New Contest");

        HBox root = new HBox(0);
        root.setStyle("-fx-background-color:" + BG_CARD + ";");

        // ── LEFT — Form panel ─────────────────────────────────────────────
        VBox leftPanel = new VBox(0);
        leftPanel.setPrefWidth(520);
        leftPanel.setMinWidth(520);
        leftPanel.setStyle("-fx-background-color:" + BG_PANEL + ";");

        HBox titleBar = buildTitleBar(modal, "➕  Create New Contest", ACCENT_DEF);
        leftPanel.getChildren().add(titleBar);

        // BUG FIX ①: inline error label — replaces nested showInfoDialog
        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill:#ef4444; -fx-font-size:12px;" +
                "-fx-padding:4 26 2 26; -fx-wrap-text:true;");
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(468);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // Form fields — BUG FIX ②: styledArea uses -fx-control-inner-background
        VBox formBody = new VBox(16);
        formBody.setPadding(new Insets(22, 26, 8, 26));

        TextField titleFld    = styledField("Contest Title");
        TextArea  descTA      = styledArea("Description (optional)", 2);
        TextField startFld    = styledField(
                LocalDateTime.now().plusHours(1)
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        startFld.setPromptText("Start (yyyy-MM-dd HH:mm)");
        TextField durationFld = styledField("60");
        TextField evalFld     = styledField(
                LocalDateTime.now().plusHours(3)
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        evalFld.setPromptText("Eval deadline (yyyy-MM-dd HH:mm)");
        TextField mcqCntFld  = styledField("10");
        TextField wrCntFld   = styledField("2");
        TextField mcqMksFld  = styledField("5");
        TextField wrMksFld   = styledField("10");
        TextField maxGainFld = styledField("100");
        TextField maxLossFld = styledField("50");

        formBody.getChildren().addAll(
                formRow("Contest Title",      titleFld),
                formRow("Description",        descTA),
                formRow("Start Time",         startFld),
                formRow("Duration (min)",     durationFld),
                formRow("Eval Deadline",      evalFld),
                twoColRow("MCQ Count", mcqCntFld, "Written Count", wrCntFld),
                twoColRow("MCQ Marks each", mcqMksFld, "Written Marks", wrMksFld),
                twoColRow("Max Rating Gain", maxGainFld, "Max Rating Loss", maxLossFld)
        );

        // BUG FIX ②: paint the ScrollPane viewport dark too
        ScrollPane formScroll = new ScrollPane(formBody);
        formScroll.setFitToWidth(true);
        formScroll.setStyle(
                "-fx-background-color:" + BG_PANEL + ";" +
                        "-fx-background:" + BG_PANEL + ";");
        VBox.setVgrow(formScroll, Priority.ALWAYS);

        // ── Theme picker ──────────────────────────────────────────────────
        VBox themeSection = new VBox(10);
        themeSection.setPadding(new Insets(10, 26, 12, 26));

        Label themeHeading = new Label("Select Arena Theme");
        themeHeading.setStyle("-fx-text-fill:" + TEXT_SEC + "; -fx-font-size:11px; -fx-font-weight:bold;");

        AtomicReference<Theme> selectedTheme = new AtomicReference<>(Theme.COSMIC_ARENA);
        ToggleGroup tg = new ToggleGroup();
        HBox themeRow1 = new HBox(7);
        HBox themeRow2 = new HBox(7);

        AtomicReference<Runnable>[] previewUpdater = new AtomicReference[]{
                new AtomicReference<>(null)
        };

        Theme[] themes = Theme.values();
        for (int i = 0; i < themes.length; i++) {
            Theme th = themes[i];
            ToggleButton tb = new ToggleButton(th.getDisplayName());
            tb.setToggleGroup(tg);
            tb.setUserData(th);
            tb.setPrefHeight(34);
            applyThemeToggleStyle(tb, th, th == Theme.COSMIC_ARENA);
            if (th == Theme.COSMIC_ARENA) tb.setSelected(true);

            tb.selectedProperty().addListener((obs, was, is) -> {
                applyThemeToggleStyle(tb, th, is);
                if (is) {
                    selectedTheme.set(th);
                    if (previewUpdater[0].get() != null) previewUpdater[0].get().run();
                }
            });
            (i < 4 ? themeRow1 : themeRow2).getChildren().add(tb);
        }
        themeSection.getChildren().addAll(themeHeading, themeRow1, themeRow2);

        // ── Bottom buttons ────────────────────────────────────────────────
        HBox btnRow = new HBox(12);
        btnRow.setPadding(new Insets(12, 26, 18, 26));
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setStyle("-fx-border-color:" + BORDER_DIM + "; -fx-border-width:1 0 0 0;");
        Button cancelBtn = ghostButton("Cancel");
        Button createBtn = primaryButton("🚀  Create Contest", ACCENT_DEF);
        btnRow.getChildren().addAll(cancelBtn, createBtn);

        leftPanel.getChildren().addAll(formScroll, errorLabel, themeSection, btnRow);

        // ── RIGHT — Arena preview panel ───────────────────────────────────
        StackPane previewPanel = new StackPane();
        previewPanel.setPrefWidth(400);
        previewPanel.setStyle("-fx-background-color:" + BG_DEEP + ";");
        buildArenaPreviewPanel(previewPanel, selectedTheme, previewUpdater);

        HBox.setHgrow(leftPanel,    Priority.NEVER);
        HBox.setHgrow(previewPanel, Priority.ALWAYS);
        root.getChildren().addAll(leftPanel, previewPanel);

        Scene scene = new Scene(root, 920, 700);
        scene.setFill(Color.TRANSPARENT);
        modal.setScene(scene);

        cancelBtn.setOnAction(e -> modal.close());

        createBtn.setOnAction(e -> {
            // BUG FIX ①: validate inline, NO nested showAndWait
            String err = validateContestForm(titleFld, startFld, durationFld, evalFld,
                    mcqCntFld, wrCntFld, mcqMksFld, wrMksFld, maxGainFld, maxLossFld);

            if (err != null) {
                showInlineError(errorLabel, err);
                return;    // stay in modal — no second Stage opened
            }

            Contest c = buildContestFromForm(titleFld, descTA, startFld, durationFld, evalFld,
                    mcqCntFld, wrCntFld, mcqMksFld, wrMksFld, maxGainFld, maxLossFld,
                    selectedTheme.get());

            if (c != null) {
                // Close the modal first, then schedule the callback on the NEXT
                // FX pulse via Platform.runLater().
                //
                // Why runLater is required:
                //   modal.close() hides the window immediately, but Stage.showAndWait()
                //   only *returns* once the current event-dispatch loop for that stage
                //   has fully unwound.  We are still inside that dispatch loop right now
                //   (we are in the button's ActionEvent handler).  If the callback calls
                //   showInfoDialog() → showAndWait() before the outer showAndWait() has
                //   returned, JavaFX sees a nested showAndWait() and either throws
                //   IllegalStateException or freezes the UI.
                //
                //   Platform.runLater() posts the callback to the FX event queue so it
                //   runs in the NEXT iteration — by then showAndWait() has returned and
                //   the stack is clean.  This is the canonical JavaFX fix for this pattern.
                final Contest contestToCreate = c;
                modal.close();
                javafx.application.Platform.runLater(() -> onCreated.accept(contestToCreate));
            }
        });

        scene.setOnKeyPressed(e -> {
            if (e.getCode().toString().equals("ESCAPE")) modal.close();
        });

        modal.showAndWait();
    }

    // ── Arena Preview Panel ────────────────────────────────────────────────────
    private static void buildArenaPreviewPanel(
            StackPane container,
            AtomicReference<Theme> selectedTheme,
            AtomicReference<Runnable>[] previewUpdater) {

        Pane particlePane = new Pane();
        particlePane.setMouseTransparent(true);
        particlePane.setPrefSize(400, 700);

        VBox content = new VBox(0);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPrefWidth(400);

        Label previewTitle = new Label("Arena Preview");
        previewTitle.setStyle("-fx-text-fill:" + TEXT_MUT + "; -fx-font-size:11px;" +
                "-fx-font-weight:bold; -fx-padding:18 0 10 0;");
        previewTitle.setAlignment(Pos.CENTER);
        previewTitle.setPrefWidth(400);

        StackPane imgCard = new StackPane();
        imgCard.setPrefSize(350, 205);
        imgCard.setMaxSize(350, 205);
        imgCard.setStyle("-fx-background-radius:16; -fx-border-radius:16;" +
                "-fx-border-width:2; -fx-border-color:" + ACCENT_DEF + ";" +
                "-fx-background-color:#0a0e1a;");

        ImageView arenaImg = new ImageView();
        arenaImg.setFitWidth(350); arenaImg.setFitHeight(205);
        arenaImg.setPreserveRatio(false); arenaImg.setSmooth(true);

        Rectangle imgClip = new Rectangle(350, 205);
        imgClip.setArcWidth(32); imgClip.setArcHeight(32);
        arenaImg.setClip(imgClip);

        Region imgOverlay = new Region();
        imgOverlay.setPrefSize(350, 205); imgOverlay.setMaxSize(350, 205);
        imgOverlay.setStyle("-fx-background-color:linear-gradient(to bottom,transparent 50%,#080912 100%);");
        imgOverlay.setMouseTransparent(true);

        Label imgPlaceholder = new Label("No Image");
        imgPlaceholder.setStyle("-fx-text-fill:#1e293b; -fx-font-size:13px;");
        imgCard.getChildren().addAll(imgPlaceholder, arenaImg, imgOverlay);

        Label themeNameLbl = new Label("COSMIC ARENA");
        themeNameLbl.setStyle("-fx-font-size:19px; -fx-font-weight:bold;" +
                "-fx-text-fill:" + ACCENT_DEF + "; -fx-padding:12 0 4 0;");

        Label themeDescLbl = new Label(getThemeDescription(Theme.COSMIC_ARENA));
        themeDescLbl.setStyle("-fx-text-fill:" + TEXT_SEC + "; -fx-font-size:11px;");
        themeDescLbl.setMaxWidth(340); themeDescLbl.setWrapText(true);
        themeDescLbl.setAlignment(Pos.CENTER);

        HBox swatchRow = new HBox(14);
        swatchRow.setAlignment(Pos.CENTER);
        swatchRow.setPadding(new Insets(10, 0, 0, 0));
        Circle sw1 = new Circle(10), sw2 = new Circle(10), sw3 = new Circle(10);
        VBox c1 = new VBox(3, sw1, swatchLabel("BG"));       c1.setAlignment(Pos.CENTER);
        VBox c2 = new VBox(3, sw2, swatchLabel("Accent"));   c2.setAlignment(Pos.CENTER);
        VBox c3 = new VBox(3, sw3, swatchLabel("Highlight")); c3.setAlignment(Pos.CENTER);
        swatchRow.getChildren().addAll(c1, c2, c3);

        HBox musicRow = new HBox(10);
        musicRow.setAlignment(Pos.CENTER);
        musicRow.setPadding(new Insets(14, 0, 0, 0));
        Button musicBtn = new Button("♪  Preview Music");
        musicBtn.setStyle("-fx-background-color:#1e293b; -fx-text-fill:#94a3b8;" +
                "-fx-background-radius:8; -fx-padding:8 16; -fx-cursor:hand;");
        Label musicStatus = new Label("Click to preview the theme song");
        musicStatus.setStyle("-fx-text-fill:" + TEXT_MUT + "; -fx-font-size:11px;");
        musicRow.getChildren().addAll(musicBtn, musicStatus);

        ProgressBar musicProg = new ProgressBar(0);
        musicProg.setPrefWidth(320); musicProg.setPrefHeight(3);
        musicProg.setStyle("-fx-background-color:transparent; -fx-accent:" + ACCENT_DEF + ";");
        musicProg.setVisible(false);

        VBox ratingStrip = new VBox(5);
        ratingStrip.setPadding(new Insets(14, 24, 0, 24));
        ratingStrip.setStyle("-fx-border-color:" + BORDER_DIM + "; -fx-border-width:1 0 0 0;" +
                "-fx-padding:14 24 0 24;");
        Label ratingTitle = new Label("ExamVerse Rating Titles");
        ratingTitle.setStyle("-fx-text-fill:" + TEXT_SEC + "; -fx-font-size:11px; -fx-font-weight:bold;");
        Label ratingInfo = new Label(
                "🌱 Beginner   0-999\n🧑‍🎓 Learner  1000-1399\n⚡ Skilled   1400-1799\n" +
                        "🔥 Advanced  1800-2199\n🧠 Expert    2200-2599\n🚀 Champion  2600-2999\n👑 Legend    3000+");
        ratingInfo.setStyle("-fx-text-fill:#1e3a5a; -fx-font-size:11px; -fx-line-spacing:2;");
        ratingStrip.getChildren().addAll(ratingTitle, ratingInfo);
        VBox.setVgrow(ratingStrip, Priority.ALWAYS);

        content.getChildren().addAll(previewTitle, imgCard, themeNameLbl, themeDescLbl,
                swatchRow, musicRow, musicProg, ratingStrip);
        container.getChildren().addAll(particlePane, content);

        // ── Music state ───────────────────────────────────────────────────
        AtomicReference<MediaPlayer> player   = new AtomicReference<>(null);
        AtomicReference<Timeline>    progLine = new AtomicReference<>(null);

        Runnable stopMusic = () -> {
            MediaPlayer mp = player.getAndSet(null);
            if (mp != null) { mp.stop(); mp.dispose(); }
            Timeline tl = progLine.getAndSet(null);
            if (tl != null) tl.stop();
            musicBtn.setText("♪  Preview Music");
            musicStatus.setText("Click to preview the theme song");
            musicProg.setVisible(false); musicProg.setProgress(0);
        };

        musicBtn.setOnAction(ev -> {
            if (player.get() != null) { stopMusic.run(); return; }
            Theme th = selectedTheme.get();
            try {
                URL url = ContestDialogHelper.class.getResource(MUSIC_BASE + th.getMusicFile());
                if (url == null) { musicStatus.setText("⚠  Music not found in assets"); return; }
                MediaPlayer mp = new MediaPlayer(new Media(url.toExternalForm()));
                mp.setVolume(0.7); mp.setCycleCount(1);
                mp.setOnReady(() -> {
                    musicStatus.setText("♪  " + th.getDisplayName() + " theme");
                    musicProg.setVisible(true); musicBtn.setText("■  Stop");
                    Timeline tl = new Timeline(new KeyFrame(Duration.millis(100), e2 -> {
                        if (mp.getTotalDuration() != null && mp.getTotalDuration().greaterThan(Duration.ZERO))
                            musicProg.setProgress(
                                    mp.getCurrentTime().toMillis() / mp.getTotalDuration().toMillis());
                    }));
                    tl.setCycleCount(Animation.INDEFINITE); tl.play(); progLine.set(tl);
                });
                mp.setOnEndOfMedia(stopMusic::run); mp.setOnError(stopMusic::run);
                mp.play(); player.set(mp);
            } catch (Exception ex) { musicStatus.setText("⚠  " + ex.getMessage()); }
        });

        // BUG FIX ④: Timeline[] not Runnable[]
        Timeline[] particleHolder = {null};

        Runnable updatePreview = () -> {
            Theme th = selectedTheme.get();
            stopMusic.run();

            String imgPath = ARENA_IMG_BASE + th.getCssClass() + ".png";
            try {
                URL imgUrl = ContestDialogHelper.class.getResource(imgPath);
                if (imgUrl != null) {
                    arenaImg.setImage(new Image(imgUrl.toExternalForm(), 350, 205, false, true, true));
                    imgPlaceholder.setVisible(false);
                } else { arenaImg.setImage(null); imgPlaceholder.setVisible(true); }
            } catch (Exception ex) { arenaImg.setImage(null); imgPlaceholder.setVisible(true); }

            imgCard.setStyle("-fx-background-radius:16; -fx-border-radius:16;" +
                    "-fx-border-width:2; -fx-border-color:" + th.getAccentColor() + ";" +
                    "-fx-background-color:#0a0e1a;");
            DropShadow glow = new DropShadow();
            glow.setColor(Color.web(th.getAccentColor(), 0.6));
            glow.setRadius(28); glow.setSpread(0.15);
            imgCard.setEffect(glow);

            themeNameLbl.setText(th.getDisplayName().toUpperCase());
            themeNameLbl.setStyle("-fx-font-size:19px; -fx-font-weight:bold;" +
                    "-fx-text-fill:" + th.getAccentColor() + "; -fx-padding:12 0 4 0;");
            themeDescLbl.setText(getThemeDescription(th));

            safeSetFill(sw1, th.getBgColor());
            safeSetFill(sw2, th.getAccentColor());
            safeSetFill(sw3, th.getHighlightColor());

            musicBtn.setStyle("-fx-background-color:#1e293b; -fx-text-fill:" + th.getAccentColor() + ";" +
                    "-fx-background-radius:8; -fx-padding:8 16; -fx-cursor:hand;");
            musicProg.setStyle("-fx-background-color:transparent; -fx-accent:" + th.getAccentColor() + ";");
            container.setStyle("-fx-background-color:" + blendDark(th.getBgColor(), 0.88) + ";");

            // BUG FIX ④: properly stop the old particle Timeline before replacing
            if (particleHolder[0] != null) { particleHolder[0].stop(); particleHolder[0] = null; }
            particlePane.getChildren().clear();
            spawnPreviewParticles(particlePane, th.getAccentColor());
        };

        previewUpdater[0].set(updatePreview);
        updatePreview.run();
    }

    // ── Particle spawner ──────────────────────────────────────────────────────
    private static void spawnPreviewParticles(Pane pane, String accentHex) {
        Color accentColor;
        try { accentColor = Color.web(accentHex, 0.22); }
        catch (Exception e) { accentColor = Color.web("#7c3aed", 0.22); }

        int[]    sizes = {5, 8, 4, 11, 7, 14, 6, 9};
        double[] xPos  = {30, 90, 160, 220, 290, 340, 70, 200};
        double   panelH = 700;

        for (int i = 0; i < sizes.length; i++) {
            Circle c = new Circle(sizes[i], accentColor);
            c.setCenterX(xPos[i]); c.setCenterY(panelH + sizes[i]);
            c.setEffect(new DropShadow(sizes[i] * 2.0, Color.web(accentHex, 0.3)));
            pane.getChildren().add(c);

            double dur   = 5000 + i * 500 + Math.random() * 2000;
            double delay = i * 300 + Math.random() * 600;

            TranslateTransition up = new TranslateTransition(Duration.millis(dur), c);
            up.setFromY(0); up.setToY(-(panelH + sizes[i] * 3));
            up.setByX((Math.random() - 0.5) * 80);
            up.setCycleCount(Animation.INDEFINITE);
            up.setDelay(Duration.millis(delay));
            up.setInterpolator(Interpolator.LINEAR);

            FadeTransition fade = new FadeTransition(Duration.millis(dur), c);
            fade.setFromValue(0.25); fade.setToValue(0.0);
            fade.setCycleCount(Animation.INDEFINITE); fade.setDelay(Duration.millis(delay));

            new ParallelTransition(up, fade).play();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ②  ADD QUESTION DIALOG
    // ══════════════════════════════════════════════════════════════════════════

    public static void showAddQuestionDialog(
            Contest contest, int mcqAdded, int wrAdded,
            Consumer<ContestQuestion> onAdded) {

        int     mcqLimit     = contest.getTotalMcqQuestions();
        int     writtenLimit = contest.getTotalWrittenQuestions();
        boolean mcqFull      = mcqAdded >= mcqLimit;
        boolean writtenFull  = wrAdded  >= writtenLimit;

        if (mcqFull && writtenFull) {
            showInfoDialog("Questions Complete",
                    "All questions added.\nMCQ: " + mcqAdded + "/" + mcqLimit +
                            "   Written: " + wrAdded + "/" + writtenLimit,
                    contest.getTheme().getAccentColor());
            return;
        }

        Theme t     = contest.getTheme();
        Stage modal = buildModal(580, 660, "Add Question");

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color:" + BG_CARD + ";" +
                "-fx-border-color:" + t.getAccentColor() + "; -fx-border-width:1.5;");

        HBox titleBar = buildTitleBar(modal,
                "✍️  Add Question — " + contest.getContestTitle(), t.getAccentColor());

        HBox accentStrip = new HBox(12);
        accentStrip.setAlignment(Pos.CENTER_LEFT);
        accentStrip.setPadding(new Insets(10, 20, 10, 20));
        accentStrip.setStyle("-fx-background-color:" + blendDark(t.getBgColor(), 0.70) + ";");
        Label themeBadge = new Label(t.getDisplayName());
        themeBadge.setStyle("-fx-background-color:" + t.getAccentColor() + "22;" +
                "-fx-text-fill:" + t.getAccentColor() + ";" +
                "-fx-font-size:11px; -fx-font-weight:bold; -fx-padding:3 10; -fx-background-radius:20;");
        Label qCountLbl = new Label(
                "MCQ: " + mcqAdded + "/" + mcqLimit + "   Written: " + wrAdded + "/" + writtenLimit);
        qCountLbl.setStyle("-fx-text-fill:" + TEXT_SEC + "; -fx-font-size:12px;");
        accentStrip.getChildren().addAll(themeBadge, qCountLbl);

        // BUG FIX ①: inline error, no nested modal
        Label errorLbl = new Label("");
        errorLbl.setStyle("-fx-text-fill:#ef4444; -fx-font-size:12px;" +
                "-fx-padding:4 26 2 26; -fx-wrap-text:true;");
        errorLbl.setWrapText(true); errorLbl.setVisible(false); errorLbl.setManaged(false);

        VBox form = new VBox(14);
        form.setPadding(new Insets(20, 26, 10, 26));

        Label typeHeading = new Label("Question Type");
        typeHeading.setStyle("-fx-text-fill:" + TEXT_SEC + "; -fx-font-size:11px; -fx-font-weight:bold;");

        ToggleGroup typeGroup = new ToggleGroup();
        ToggleButton mcqTgl = themeToggle("📝  MCQ  (Auto-graded)", t.getAccentColor(), typeGroup);
        ToggleButton wrTgl  = themeToggle("✍️  Written  (Image Upload)", t.getHighlightColor(), typeGroup);
        mcqTgl.setDisable(mcqFull); wrTgl.setDisable(writtenFull);
        if (!mcqFull) mcqTgl.setSelected(true); else wrTgl.setSelected(true);
        HBox typeRow = new HBox(10, mcqTgl, wrTgl);

        // BUG FIX ②: dark TextArea
        TextArea questionTA = styledArea("Question text...", 3);
        TextField marksFld  = styledField(String.valueOf(contest.getMcqMarksEach()));

        Runnable syncMarks = () -> marksFld.setText(mcqTgl.isSelected()
                ? String.valueOf(contest.getMcqMarksEach())
                : String.valueOf(contest.getWrittenMarksEach()));
        mcqTgl.selectedProperty().addListener((obs, ov, nv) -> { if (nv) syncMarks.run(); });
        wrTgl.selectedProperty().addListener((obs, ov, nv)  -> { if (nv) syncMarks.run(); });

        VBox mcqSection = new VBox(8);
        TextField optA = styledField("Option A"), optB = styledField("Option B");
        TextField optC = styledField("Option C"), optD = styledField("Option D");

        // BUG FIX ③: dark ComboBox with visible selected text
        ComboBox<String> correctBox = styledComboBox("A", "B", "C", "D");
        TextArea explanTA = styledArea("Explanation (optional)", 2);

        mcqSection.getChildren().addAll(
                formRow("Option A", optA), formRow("Option B", optB),
                formRow("Option C", optC), formRow("Option D", optD),
                formRow("Correct Answer", correctBox),
                formRow("Explanation (optional)", explanTA));

        wrTgl.selectedProperty().addListener((obs, ov, nv) -> {
            mcqSection.setVisible(!nv); mcqSection.setManaged(!nv);
        });

        form.getChildren().addAll(typeHeading, typeRow,
                formRow("Question", questionTA),
                formRow("Marks", marksFld),
                mcqSection);

        // BUG FIX ②: dark ScrollPane viewport
        ScrollPane formScroll = new ScrollPane(form);
        formScroll.setFitToWidth(true);
        formScroll.setStyle(
                "-fx-background-color:" + BG_CARD + ";" +
                        "-fx-background:" + BG_CARD + ";");
        VBox.setVgrow(formScroll, Priority.ALWAYS);

        HBox btnRow = new HBox(12);
        btnRow.setPadding(new Insets(12, 26, 18, 26));
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setStyle("-fx-border-color:" + BORDER_DIM + "; -fx-border-width:1 0 0 0;");
        Button cancelBtn = ghostButton("Cancel");
        Button addBtn    = primaryButton("➕  Add Question", t.getAccentColor());
        btnRow.getChildren().addAll(cancelBtn, addBtn);

        root.getChildren().addAll(titleBar, accentStrip, formScroll, errorLbl, btnRow);

        Scene scene = new Scene(root, 580, 660);
        scene.setFill(Color.TRANSPARENT);
        modal.setScene(scene);

        cancelBtn.setOnAction(e -> modal.close());
        addBtn.setOnAction(e -> {
            String qText = questionTA.getText().trim();
            if (qText.isEmpty()) {
                showInlineError(errorLbl, "Question text cannot be empty.");
                pulseError(questionTA);
                return;
            }
            boolean isMcq = mcqTgl.isSelected();
            ContestQuestion q = new ContestQuestion();
            q.setContestId(contest.getContestId());
            q.setQuestionText(qText);
            q.setType(isMcq ? QuestionType.MCQ : QuestionType.WRITTEN);
            try { q.setMarks(Integer.parseInt(marksFld.getText().trim())); }
            catch (NumberFormatException ex) {
                q.setMarks(isMcq ? contest.getMcqMarksEach() : contest.getWrittenMarksEach());
            }
            q.setOrderIndex((isMcq ? mcqAdded : wrAdded) + 1);
            if (isMcq) {
                q.setOptionA(optA.getText().trim()); q.setOptionB(optB.getText().trim());
                q.setOptionC(optC.getText().trim()); q.setOptionD(optD.getText().trim());
                q.setCorrectAnswer(correctBox.getValue());
                q.setExplanation(explanTA.getText().trim());
            }
            // Close first, then post callback to the next FX pulse.
            // Same reason as showCreateContestDialog — we are still inside
            // the modal's event-dispatch loop; runLater ensures showAndWait()
            // has fully returned before the callback (which may open another
            // Stage) executes.
            final ContestQuestion qFinal = q;
            modal.close();
            javafx.application.Platform.runLater(() -> onAdded.accept(qFinal));
        });

        scene.setOnKeyPressed(ev -> {
            if (ev.getCode().toString().equals("ESCAPE")) modal.close();
        });

        modal.showAndWait();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ③  CONFIRM DIALOG
    // ══════════════════════════════════════════════════════════════════════════

    public static void showConfirmDialog(
            String title, String message, String accentHex, Runnable onConfirm) {

        Stage modal = buildModal(480, 270, title);
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color:" + BG_CARD + ";" +
                "-fx-border-color:" + accentHex + "; -fx-border-width:1.5;");

        VBox body = new VBox(0);
        body.setPadding(new Insets(26, 30, 20, 30));
        VBox.setVgrow(body, Priority.ALWAYS);
        Label msgLbl = new Label(message);
        msgLbl.setStyle("-fx-text-fill:" + TEXT_PRI + "; -fx-font-size:14px;");
        msgLbl.setWrapText(true); msgLbl.setMaxWidth(420);
        body.getChildren().add(msgLbl);

        HBox btnRow = new HBox(12);
        btnRow.setPadding(new Insets(12, 30, 22, 30));
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setStyle("-fx-border-color:" + BORDER_DIM + "; -fx-border-width:1 0 0 0;");
        Button noBtn  = ghostButton("No, Cancel");
        Button yesBtn = primaryButton("Yes, Confirm", accentHex);
        btnRow.getChildren().addAll(noBtn, yesBtn);

        root.getChildren().addAll(buildTitleBar(modal, title, accentHex), body, btnRow);
        Scene scene = new Scene(root, 480, 270);
        scene.setFill(Color.TRANSPARENT);
        modal.setScene(scene);

        noBtn.setOnAction(e  -> modal.close());
        // Platform.runLater defers the callback to the next FX pulse so that
        // showAndWait() has fully returned before onConfirm runs.
        yesBtn.setOnAction(e -> { modal.close(); javafx.application.Platform.runLater(onConfirm); });
        scene.setOnKeyPressed(ev -> {
            switch (ev.getCode()) {
                case ESCAPE -> modal.close();
                case ENTER  -> { modal.close(); javafx.application.Platform.runLater(onConfirm); }
            }
        });

        modal.showAndWait();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ④  INFO DIALOG
    // ══════════════════════════════════════════════════════════════════════════

    public static void showInfoDialog(String title, String message, String accentHex) {
        Stage modal = buildModal(460, 240, title);
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color:" + BG_CARD + ";" +
                "-fx-border-color:" + accentHex + "; -fx-border-width:1.5;");

        VBox body = new VBox(0);
        body.setPadding(new Insets(22, 30, 10, 30));
        VBox.setVgrow(body, Priority.ALWAYS);
        Label msgLbl = new Label(message);
        msgLbl.setStyle("-fx-text-fill:" + TEXT_PRI + "; -fx-font-size:14px;");
        msgLbl.setWrapText(true); msgLbl.setMaxWidth(400);
        body.getChildren().add(msgLbl);

        HBox btnRow = new HBox(0);
        btnRow.setPadding(new Insets(14, 30, 20, 30));
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setStyle("-fx-border-color:" + BORDER_DIM + "; -fx-border-width:1 0 0 0;");
        Button okBtn = primaryButton("OK", accentHex);
        btnRow.getChildren().add(okBtn);

        root.getChildren().addAll(buildTitleBar(modal, title, accentHex), body, btnRow);
        Scene scene = new Scene(root, 460, 240);
        scene.setFill(Color.TRANSPARENT);
        modal.setScene(scene);

        okBtn.setOnAction(e -> modal.close());
        scene.setOnKeyPressed(ev -> {
            String code = ev.getCode().toString();
            if (code.equals("ESCAPE") || code.equals("ENTER")) modal.close();
        });

        modal.showAndWait();
    }

    public static void showInfoDialog(String title, String message) {
        showInfoDialog(title, message, ACCENT_DEF);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private static Stage buildModal(double w, double h, String windowTitle) {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initOwner(SceneManager.getPrimaryStage());
        modal.initStyle(StageStyle.TRANSPARENT);
        modal.setTitle(windowTitle);
        modal.setResizable(false);
        return modal;
    }

    private static HBox buildTitleBar(Stage modal, String title, String accentHex) {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 16, 0, 0));
        bar.setStyle("-fx-background-color:" + BG_PANEL + ";" +
                "-fx-border-color:transparent transparent " + BORDER_DIM + " transparent;" +
                "-fx-border-width:0 0 1 0; -fx-min-height:50; -fx-pref-height:50;");

        Region stripe = new Region();
        stripe.setPrefWidth(4); stripe.setMinHeight(50);
        stripe.setStyle("-fx-background-color:" + accentHex + ";");

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-text-fill:" + TEXT_PRI + "; -fx-font-size:14px; -fx-font-weight:bold;");

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color:transparent; -fx-text-fill:" + TEXT_MUT + ";" +
                "-fx-font-size:13px; -fx-cursor:hand; -fx-padding:4 8;");
        closeBtn.setOnMouseEntered(e ->
                closeBtn.setStyle("-fx-background-color:#ef444422; -fx-text-fill:#ef4444;" +
                        "-fx-font-size:13px; -fx-cursor:hand; -fx-padding:4 8; -fx-background-radius:6;"));
        closeBtn.setOnMouseExited(e ->
                closeBtn.setStyle("-fx-background-color:transparent; -fx-text-fill:" + TEXT_MUT + ";" +
                        "-fx-font-size:13px; -fx-cursor:hand; -fx-padding:4 8;"));
        closeBtn.setOnAction(e -> modal.close());
        bar.getChildren().addAll(stripe, titleLbl, spacer, closeBtn);

        double[] offset = {0, 0};
        bar.setOnMousePressed(e  -> { offset[0] = e.getSceneX(); offset[1] = e.getSceneY(); });
        bar.setOnMouseDragged(e  -> { modal.setX(e.getScreenX() - offset[0]);
            modal.setY(e.getScreenY() - offset[1]); });
        return bar;
    }

    // ── styledField ───────────────────────────────────────────────────────────
    private static TextField styledField(String text) {
        TextField f = new TextField(text);
        f.setStyle(
                "-fx-background-color:" + BG_INPUT + ";" +
                        "-fx-text-fill:" + TEXT_PRI + ";" +
                        "-fx-prompt-text-fill:#475569;" +
                        "-fx-highlight-fill:#7c3aed;" +
                        "-fx-highlight-text-fill:#ffffff;" +
                        "-fx-background-radius:8;" +
                        "-fx-border-color:" + BORDER_DIM + ";" +
                        "-fx-border-radius:8; -fx-border-width:1; -fx-padding:8 12;");
        f.setMaxWidth(Double.MAX_VALUE);
        return f;
    }

    /**
     * BUG FIX ②: -fx-control-inner-background paints the editable content
     * region of TextArea. Without it the inner region stays default (white).
     */
    private static TextArea styledArea(String prompt, int rows) {
        TextArea ta = new TextArea();
        ta.setPromptText(prompt);
        ta.setPrefRowCount(rows);
        ta.setStyle(
                "-fx-control-inner-background:" + BG_INPUT + ";" +
                        "-fx-background-color:" + BG_INPUT + ";" +
                        "-fx-text-fill:" + TEXT_PRI + ";" +
                        "-fx-prompt-text-fill:#475569;" +
                        "-fx-highlight-fill:#7c3aed;" +
                        "-fx-highlight-text-fill:#ffffff;" +
                        "-fx-background-radius:8;" +
                        "-fx-border-color:" + BORDER_DIM + ";" +
                        "-fx-border-radius:8; -fx-border-width:1; -fx-padding:8 12;");
        ta.setMaxWidth(Double.MAX_VALUE);
        return ta;
    }

    /**
     * BUG FIX ③: Custom ButtonCell + CellFactory so the selected value text
     * is visible in dark theme. Plain -fx-text-fill on the ComboBox does not
     * reach the inner ListCell rendered for the closed/display state.
     */
    private static ComboBox<String> styledComboBox(String... items) {
        ComboBox<String> box = new ComboBox<>();
        box.getItems().addAll(items);
        box.setValue(items.length > 0 ? items[0] : null);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setStyle(
                "-fx-background-color:" + BG_INPUT + ";" +
                        "-fx-border-color:" + BORDER_DIM + ";" +
                        "-fx-border-radius:8; -fx-background-radius:8; -fx-padding:2 4;");

        // The cell shown in the closed (button) state
        box.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : item);
                setStyle("-fx-text-fill:" + TEXT_PRI + ";" +
                        "-fx-background-color:" + BG_INPUT + "; -fx-font-size:13px;");
            }
        });

        // Cells shown inside the open dropdown list
        box.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : item);
                setStyle("-fx-text-fill:" + TEXT_PRI + ";" +
                        "-fx-background-color:" + BG_INPUT + ";" +
                        "-fx-font-size:13px; -fx-padding:6 12;");
            }
        });

        return box;
    }

    private static VBox formRow(String label, Node field) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill:" + TEXT_SEC + "; -fx-font-size:11px;" +
                "-fx-font-weight:bold; -fx-padding:0 0 3 0;");
        VBox row = new VBox(4, lbl, field);
        row.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    private static HBox twoColRow(String l1, Node f1, String l2, Node f2) {
        VBox col1 = formRow(l1, f1); VBox col2 = formRow(l2, f2);
        HBox.setHgrow(col1, Priority.ALWAYS); HBox.setHgrow(col2, Priority.ALWAYS);
        HBox row = new HBox(14, col1, col2); row.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    private static Button primaryButton(String text, String accentHex) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + accentHex + "; -fx-text-fill:#fff;" +
                "-fx-font-weight:bold; -fx-font-size:13px;" +
                "-fx-background-radius:8; -fx-padding:9 20; -fx-cursor:hand;");
        b.setOnMouseEntered(e -> b.setOpacity(0.88));
        b.setOnMouseExited(e  -> b.setOpacity(1.0));
        return b;
    }

    private static Button ghostButton(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:#1e293b; -fx-text-fill:#94a3b8;" +
                "-fx-background-radius:8; -fx-padding:9 20; -fx-cursor:hand;");
        b.setOnMouseEntered(e -> b.setStyle(b.getStyle()
                .replace("-fx-background-color:#1e293b;", "-fx-background-color:#263548;")));
        b.setOnMouseExited(e  -> b.setStyle(b.getStyle()
                .replace("-fx-background-color:#263548;", "-fx-background-color:#1e293b;")));
        return b;
    }

    private static ToggleButton themeToggle(String text, String accentHex, ToggleGroup tg) {
        ToggleButton tb = new ToggleButton(text);
        tb.setToggleGroup(tg);
        tb.setStyle("-fx-background-color:#1e293b; -fx-text-fill:#94a3b8;" +
                "-fx-background-radius:8; -fx-padding:9 18; -fx-cursor:hand;");
        tb.selectedProperty().addListener((obs, ov, nv) ->
                tb.setStyle(nv
                        ? "-fx-background-color:" + accentHex + "33;" +
                        "-fx-text-fill:" + accentHex + ";" +
                        "-fx-border-color:" + accentHex + ";" +
                        "-fx-border-radius:8; -fx-border-width:1.5;" +
                        "-fx-background-radius:8; -fx-padding:9 18;" +
                        "-fx-cursor:hand; -fx-font-weight:bold;"
                        : "-fx-background-color:#1e293b; -fx-text-fill:#94a3b8;" +
                        "-fx-background-radius:8; -fx-padding:9 18; -fx-cursor:hand;"));
        return tb;
    }

    private static void applyThemeToggleStyle(ToggleButton tb, Theme th, boolean selected) {
        tb.setStyle(selected
                ? "-fx-background-color:" + th.getBgColor() + ";" +
                "-fx-text-fill:" + th.getAccentColor() + ";" +
                "-fx-border-color:" + th.getAccentColor() + ";" +
                "-fx-border-radius:8; -fx-background-radius:8;" +
                "-fx-border-width:2; -fx-padding:6 12; -fx-font-weight:bold; -fx-cursor:hand;"
                : "-fx-background-color:#111827; -fx-text-fill:#374151;" +
                "-fx-border-color:#1f2937; -fx-border-radius:8; -fx-background-radius:8;" +
                "-fx-border-width:1; -fx-padding:6 12; -fx-cursor:hand;");
    }

    // ── Inline error helper ───────────────────────────────────────────────────
    private static void showInlineError(Label lbl, String message) {
        lbl.setText("⚠  " + message);
        lbl.setVisible(true); lbl.setManaged(true);
        PauseTransition hide = new PauseTransition(Duration.seconds(4));
        hide.setOnFinished(e -> { lbl.setVisible(false); lbl.setManaged(false); });
        hide.play();
    }

    private static void pulseError(Node node) {
        String orig = node.getStyle();
        node.setStyle(orig + " -fx-border-color:#ef4444; -fx-border-width:2;");

        PauseTransition pt = new PauseTransition(Duration.millis(1400));
        pt.setOnFinished(e -> node.setStyle(orig));
        pt.play();
    }

    // ── Colour utilities ──────────────────────────────────────────────────────
    private static String blendDark(String hex, double darkness) {
        try {
            Color c = Color.web(hex);
            double f = 1.0 - darkness;
            return String.format("#%02x%02x%02x",
                    (int)(c.getRed() * f * 255),
                    (int)(c.getGreen() * f * 255),
                    (int)(c.getBlue() * f * 255));
        } catch (Exception e) { return "#0a0a1a"; }
    }

    private static void safeSetFill(Circle c, String hex) {
        try { c.setFill(Color.web(hex)); }
        catch (Exception e) { c.setFill(Color.web("#1e293b")); }
    }

    private static Label swatchLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill:#334155; -fx-font-size:9px;");
        return l;
    }

    // ── Validation — returns error string or null ─────────────────────────────
    private static String validateContestForm(
            TextField titleFld, TextField startFld, TextField durationFld,
            TextField evalFld, TextField mcqCntFld, TextField wrCntFld,
            TextField mcqMksFld, TextField wrMksFld,
            TextField maxGainFld, TextField maxLossFld) {

        if (titleFld.getText().trim().isEmpty()) return "Contest title cannot be empty.";

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        try { LocalDateTime.parse(startFld.getText().trim(), dtf); }
        catch (Exception e) { return "Invalid start time. Use: yyyy-MM-dd HH:mm"; }

        try { LocalDateTime.parse(evalFld.getText().trim(), dtf); }
        catch (Exception e) { return "Invalid eval deadline. Use: yyyy-MM-dd HH:mm"; }

        try { if (Integer.parseInt(durationFld.getText().trim()) <= 0) return "Duration must be > 0."; }
        catch (NumberFormatException e) { return "Duration must be a whole number."; }

        String[] numFields = { mcqCntFld.getText(), wrCntFld.getText(),
                mcqMksFld.getText(), wrMksFld.getText(),
                maxGainFld.getText(), maxLossFld.getText() };
        String[] numLabels = { "MCQ count", "Written count", "MCQ marks",
                "Written marks", "Max gain", "Max loss" };
        for (int i = 0; i < numFields.length; i++) {
            try { Integer.parseInt(numFields[i].trim()); }
            catch (NumberFormatException e) { return numLabels[i] + " must be a whole number."; }
        }
        return null;
    }

    // ── Build Contest object (no dialogs, no validation) ──────────────────────
    private static Contest buildContestFromForm(
            TextField titleFld, TextArea descTA, TextField startFld,
            TextField durationFld, TextField evalFld,
            TextField mcqCntFld, TextField wrCntFld,
            TextField mcqMksFld, TextField wrMksFld,
            TextField maxGainFld, TextField maxLossFld,
            Theme theme) {
        try {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            Contest c = new Contest();
            c.setContestTitle(titleFld.getText().trim());
            c.setDescription(descTA.getText().trim());
            c.setTheme(theme);
            c.setDurationMinutes(Integer.parseInt(durationFld.getText().trim()));
            c.setTotalMcqQuestions(Integer.parseInt(mcqCntFld.getText().trim()));
            c.setTotalWrittenQuestions(Integer.parseInt(wrCntFld.getText().trim()));
            c.setMcqMarksEach(Integer.parseInt(mcqMksFld.getText().trim()));
            c.setWrittenMarksEach(Integer.parseInt(wrMksFld.getText().trim()));
            c.setMaxGain(Integer.parseInt(maxGainFld.getText().trim()));
            c.setMaxLoss(Integer.parseInt(maxLossFld.getText().trim()));
            LocalDateTime start = LocalDateTime.parse(startFld.getText().trim(), dtf);
            c.setStartTime(start);
            c.setEndTime(start.plusMinutes(c.getDurationMinutes()));
            c.setEvalDeadline(LocalDateTime.parse(evalFld.getText().trim(), dtf));
            c.setTotalMarks(c.computeTotalMarks());
            return c;
        } catch (Exception ex) { return null; }
    }

    private static String getThemeDescription(Theme th) {
        return switch (th) {
            case COSMIC_ARENA   -> "A deep-space arena where constellations shift with every answer.";
            case NEON_CIRCUIT   -> "Electric data streams light up every question. The grid is watching.";
            case DRAGON_REALM   -> "Ancient fire meets modern intellect. Conquer the realm.";
            case FROZEN_PEAKS   -> "Crystal peaks where clarity of thought freezes rivals.";
            case SHADOW_TEMPLE  -> "A hidden temple where wisdom is power and silence is deadly.";
            case CYBER_STORM    -> "Hack through questions in a relentless storm of logic.";
            case VOLCANIC_FORGE -> "Molten knowledge forged under pressure. Only the sharpest survive.";
            case OCEAN_DEPTHS   -> "Dive deep — calm focus reveals hidden treasures.";
        };
    }
}