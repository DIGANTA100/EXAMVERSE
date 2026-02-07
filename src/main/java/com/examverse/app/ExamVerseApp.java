package com.examverse.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.examverse.config.DatabaseConfig;
import com.examverse.util.SceneManager;

import java.util.Objects;

/**
 * ExamVerseApp - Main JavaFX Application class
 * Initializes the application, database, and loads the intro screen
 */
public class ExamVerseApp extends Application {

    private static Stage primaryStage;

    @Override
    public void init() {
        // Initialize database before JavaFX starts
        System.out.println("========================================");
        System.out.println("🚀 ExamVerse Starting...");
        System.out.println("========================================");

        try {
            System.out.println("📊 Initializing Database...");
            DatabaseConfig.initializeTables();

            if (DatabaseConfig.testConnection()) {
                System.out.println("✅ Database initialized successfully!");
            } else {
                System.err.println("❌ Database connection failed!");
                System.err.println("⚠️  App will continue, but features requiring database won't work.");
            }
        } catch (Exception e) {
            System.err.println("❌ Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("========================================");
    }

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        // Initialize SceneManager
        SceneManager.initialize(stage);

        // Load intro screen
        Parent root = FXMLLoader.load(
                Objects.requireNonNull(getClass().getResource("/com/examverse/fxml/intro/intro.fxml"))
        );

        Scene scene = new Scene(root, 1920, 1080);

        // Load CSS
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/com/examverse/css/intro.css")).toExternalForm()
        );

        // Configure stage
        stage.setTitle("ExamVerse - Redefining Academic Intelligence");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.setResizable(true);

        // Handle application close
        stage.setOnCloseRequest(event -> {
            System.out.println("🛑 Application closing...");
            DatabaseConfig.closeConnection();
        });

        stage.show();

        System.out.println("✅ ExamVerse loaded successfully!");
    }

    @Override
    public void stop() {
        // Cleanup when app closes
        System.out.println("🧹 Cleaning up...");
        DatabaseConfig.closeConnection();
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {

        launch(args);
    }
}