package com.examverse.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * SceneManager - Utility class for managing scene transitions
 * Handles switching between different screens in the application
 *
 * BUG FIX: sceneCache was returning the same Scene object for exam-taking.fxml
 * on every retake in the same session. Because JavaFX only calls a controller's
 * initialize() when FXMLLoader.load() is invoked, serving a cached scene meant
 * ExamController.initialize() NEVER ran on retake — the old frozen timer, old
 * question text, and old radio-button selections all stayed on screen unchanged.
 *
 * Fix: any path listed in NEVER_CACHE is always loaded via a fresh FXMLLoader,
 * guaranteeing initialize() runs every time. All other scenes still benefit
 * from caching as before — nothing else changes.
 */
public class SceneManager {

    private static Stage primaryStage;
    private static Map<String, Scene> sceneCache = new HashMap<>();
    private static boolean cacheEnabled = true;

    /**
     * Paths that must NEVER be served from cache — their controllers must
     * re-initialize on every visit (e.g. exam state must always be fresh).
     */
    private static final String[] NEVER_CACHE = {
            "exam-taking",          // ExamController must re-initialize on every new/retake
            "exam-result",          // ResultController must always show the latest result
            "login",                // LoginController must reset fields/button on every visit
            "signup",               // SignupController must reset fields/button on every visit
            "terms-and-conditions"  // TermsController — always fade in fresh
    };

    /** Returns true if this path should always bypass the cache. */
    private static boolean neverCache(String fxmlPath) {
        if (fxmlPath == null) return false;
        for (String key : NEVER_CACHE) {
            if (fxmlPath.contains(key)) return true;
        }
        return false;
    }

    /**
     * Initialize the SceneManager with primary stage
     */
    public static void initialize(Stage stage) {
        primaryStage = stage;
    }

    /**
     * Switch to a new scene by loading FXML file
     * @param fxmlPath Path to FXML file (e.g., "/com/examverse/fxml/auth/login.fxml")
     */
    public static void switchScene(String fxmlPath) {
        switchScene(fxmlPath, null);
    }

    /**
     * Switch to a new scene with custom CSS
     * @param fxmlPath Path to FXML file
     * @param cssPath Path to CSS file (optional)
     */
    public static void switchScene(String fxmlPath, String cssPath) {
        try {
            Scene scene;

            // BUG FIX: skip cache for scenes that must re-initialize every time.
            // For all other scenes, caching behaviour is unchanged.
            boolean useCache = cacheEnabled && !neverCache(fxmlPath);

            if (useCache && sceneCache.containsKey(fxmlPath)) {
                // Serve from cache (login, dashboard, etc.)
                scene = sceneCache.get(fxmlPath);
            } else {
                // Always use a brand-new FXMLLoader so initialize() is called.
                // Previously: FXMLLoader.load(url) — static method, no controller access.
                // Now:        new FXMLLoader(url).load() — same result, fresh every time.
                FXMLLoader loader = new FXMLLoader(
                        Objects.requireNonNull(SceneManager.class.getResource(fxmlPath))
                );
                Parent root = loader.load();
                scene = new Scene(root, primaryStage.getWidth(), primaryStage.getHeight());

                // Apply CSS if provided
                if (cssPath != null && !cssPath.isEmpty()) {
                    scene.getStylesheets().add(
                            Objects.requireNonNull(
                                    SceneManager.class.getResource(cssPath)).toExternalForm()
                    );
                }

                // Only cache if allowed for this path
                if (useCache) {
                    sceneCache.put(fxmlPath, scene);
                }
            }

            primaryStage.setScene(scene);

        } catch (IOException e) {
            System.err.println("Error switching scene: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Switch scene with fade transition
     * @param fxmlPath Path to FXML file
     * @param duration Duration of fade in milliseconds
     */
    public static void switchSceneWithFade(String fxmlPath, double duration) {
        AnimationUtil.fadeTransition(primaryStage.getScene().getRoot(), () -> {
            switchScene(fxmlPath);
        }, duration);
    }

    /**
     * Get the primary stage
     */
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Clear scene cache
     */
    public static void clearCache() {
        sceneCache.clear();
    }

    /**
     * Enable or disable scene caching
     */
    public static void setCacheEnabled(boolean enabled) {
        cacheEnabled = enabled;
        if (!enabled) {
            clearCache();
        }
    }

    /**
     * Get current scene
     */
    public static Scene getCurrentScene() {
        return primaryStage.getScene();
    }
}