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
 */
public class SceneManager {

    private static Stage primaryStage;
    private static Map<String, Scene> sceneCache = new HashMap<>();
    private static boolean cacheEnabled = true;

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

            // Check cache first
            if (cacheEnabled && sceneCache.containsKey(fxmlPath)) {
                scene = sceneCache.get(fxmlPath);
            } else {
                // Load FXML
                Parent root = FXMLLoader.load(
                        Objects.requireNonNull(SceneManager.class.getResource(fxmlPath))
                );

                scene = new Scene(root);

                // Apply CSS if provided
                if (cssPath != null && !cssPath.isEmpty()) {
                    scene.getStylesheets().add(
                            Objects.requireNonNull(SceneManager.class.getResource(cssPath)).toExternalForm()
                    );
                }

                // Cache the scene
                if (cacheEnabled) {
                    sceneCache.put(fxmlPath, scene);
                }
            }

            // Set the scene
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