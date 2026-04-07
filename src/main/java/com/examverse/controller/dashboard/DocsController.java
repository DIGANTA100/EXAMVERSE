package com.examverse.controller.dashboard;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import com.examverse.util.SceneManager;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * DocsController — handles sidebar tab switching for the docs page.
 * Each sidebar button maps to a content panel (VBox) in the StackPane.
 * Only one panel is visible/managed at a time.
 */
public class DocsController implements Initializable {

    @FXML private VBox rootPane;

    // ── Sidebar buttons ──
    @FXML private Button btnOverview;
    @FXML private Button btnInstallation;
    @FXML private Button btnFirstLogin;
    @FXML private Button btnTakingExam;
    @FXML private Button btnPractice;
    @FXML private Button btnContests;
    @FXML private Button btnLeaderboard;
    @FXML private Button btnForum;
    @FXML private Button btnResults;
    @FXML private Button btnAdminDash;
    @FXML private Button btnManageQ;
    @FXML private Button btnContestMgr;
    @FXML private Button btnReports;
    @FXML private Button btnConfig;
    @FXML private Button btnDatabase;
    @FXML private Button btnStructure;

    // ── Content panels ──
    @FXML private VBox panelOverview;
    @FXML private VBox panelInstallation;
    @FXML private VBox panelFirstLogin;
    @FXML private VBox panelTakingExam;
    @FXML private VBox panelPractice;
    @FXML private VBox panelContests;
    @FXML private VBox panelLeaderboard;
    @FXML private VBox panelForum;
    @FXML private VBox panelResults;
    @FXML private VBox panelAdminDash;
    @FXML private VBox panelManageQ;
    @FXML private VBox panelContestMgr;
    @FXML private VBox panelReports;
    @FXML private VBox panelConfig;
    @FXML private VBox panelDatabase;
    @FXML private VBox panelStructure;

    /** Maps each sidebar button to its corresponding content panel. */
    private Map<Button, VBox> tabMap;
    private Button activeButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        buildTabMap();
        applyFadeInAnimation();
        // Overview is active by default (already visible in FXML)
        activeButton = btnOverview;
    }

    private void buildTabMap() {
        tabMap = new LinkedHashMap<>();
        tabMap.put(btnOverview,     panelOverview);
        tabMap.put(btnInstallation, panelInstallation);
        tabMap.put(btnFirstLogin,   panelFirstLogin);
        tabMap.put(btnTakingExam,   panelTakingExam);
        tabMap.put(btnPractice,     panelPractice);
        tabMap.put(btnContests,     panelContests);
        tabMap.put(btnLeaderboard,  panelLeaderboard);
        tabMap.put(btnForum,        panelForum);
        tabMap.put(btnResults,      panelResults);
        tabMap.put(btnAdminDash,    panelAdminDash);
        tabMap.put(btnManageQ,      panelManageQ);
        tabMap.put(btnContestMgr,   panelContestMgr);
        tabMap.put(btnReports,      panelReports);
        tabMap.put(btnConfig,       panelConfig);
        tabMap.put(btnDatabase,     panelDatabase);
        tabMap.put(btnStructure,    panelStructure);
    }

    /**
     * Generic handler wired to every sidebar button via onAction="#handleTabSwitch".
     * Determines which button fired the event, then switches to its panel.
     */
    @FXML
    private void handleTabSwitch(javafx.event.ActionEvent event) {
        Button clicked = (Button) event.getSource();
        if (clicked == activeButton) return; // already on this tab

        // Hide all panels, deactivate all buttons
        for (Map.Entry<Button, VBox> entry : tabMap.entrySet()) {
            entry.getValue().setVisible(false);
            entry.getValue().setManaged(false);
            entry.getKey().getStyleClass().removeAll("docs-sidebar-item-active");
            if (!entry.getKey().getStyleClass().contains("docs-sidebar-item")) {
                entry.getKey().getStyleClass().add("docs-sidebar-item");
            }
        }

        // Show the clicked panel
        VBox target = tabMap.get(clicked);
        if (target != null) {
            target.setVisible(true);
            target.setManaged(true);

            // Fade in the newly shown panel
            FadeTransition ft = new FadeTransition(Duration.millis(200), target);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();
        }

        // Mark clicked button as active
        clicked.getStyleClass().remove("docs-sidebar-item");
        if (!clicked.getStyleClass().contains("docs-sidebar-item-active")) {
            clicked.getStyleClass().add("docs-sidebar-item-active");
        }

        activeButton = clicked;
    }

    private void applyFadeInAnimation() {
        if (rootPane != null) {
            FadeTransition fade = new FadeTransition(Duration.millis(500), rootPane);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            fade.play();
        }
    }

    private void applyFadeOutTransition(Runnable onFinished) {
        if (rootPane != null) {
            FadeTransition fade = new FadeTransition(Duration.millis(300), rootPane);
            fade.setFromValue(1.0);
            fade.setToValue(0.0);
            fade.setOnFinished(e -> onFinished.run());
            fade.play();
        } else {
            onFinished.run();
        }
    }

    @FXML
    private void handleBack() {
        applyFadeOutTransition(() ->
                SceneManager.switchScene("/com/examverse/fxml/dashboard/dashboard-landing.fxml"));
    }
}