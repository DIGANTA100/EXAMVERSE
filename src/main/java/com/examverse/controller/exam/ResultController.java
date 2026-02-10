package com.examverse.controller.exam;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import com.examverse.model.exam.StudentExamAttempt;
import com.examverse.model.user.User;
import com.examverse.service.exam.ExamService;
import com.examverse.util.SceneManager;
import com.examverse.util.SessionManager;

import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;

/**
 * ResultController - Displays exam results after submission
 */
public class ResultController implements Initializable {

    @FXML private Label examTitleLabel;
    @FXML private Label scoreLabel;
    @FXML private Label percentageLabel;
    @FXML private Label resultLabel;
    @FXML private Label accuracyLabel;
    @FXML private Label timeSpentLabel;
    @FXML private Label totalQuestionsLabel;
    @FXML private Label correctAnswersLabel;
    @FXML private Label wrongAnswersLabel;
    @FXML private ProgressBar scoreProgressBar;

    private User currentUser;
    private int attemptId;
    private StudentExamAttempt attempt;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = SessionManager.getInstance().getCurrentUser();
        attemptId = (int) SessionManager.getInstance().getAttribute("attemptId");

        // Load attempt details
        loadAttemptDetails();
    }

    /**
     * Load attempt details from database
     */
    private void loadAttemptDetails() {
        String sql = """
            SELECT sea.*, e.exam_title, e.subject, e.total_questions
            FROM student_exam_attempts sea
            JOIN exams e ON sea.exam_id = e.exam_id
            WHERE sea.attempt_id = ?
            """;

        try (Connection conn = com.examverse.config.DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, attemptId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // Extract data
                String examTitle = rs.getString("exam_title");
                int obtainedMarks = rs.getInt("obtained_marks");
                int totalMarks = rs.getInt("total_marks");
                int totalQuestions = rs.getInt("total_questions");
                double accuracy = rs.getDouble("accuracy");
                String result = rs.getString("result");
                int timeSpent = rs.getInt("time_spent_minutes");

                // Calculate stats
                double percentage = (obtainedMarks * 100.0) / totalMarks;
                int correctAnswers = (int) ((accuracy / 100.0) * totalQuestions);
                int wrongAnswers = totalQuestions - correctAnswers;

                // Update UI
                examTitleLabel.setText(examTitle);
                scoreLabel.setText(obtainedMarks + "/" + totalMarks);
                percentageLabel.setText(String.format("%.1f%%", percentage));

                // Set result label and color
                if ("PASSED".equals(result)) {
                    resultLabel.setText("✅ PASSED");
                    resultLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 32px; -fx-font-weight: bold;");
                } else {
                    resultLabel.setText("❌ FAILED");
                    resultLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 32px; -fx-font-weight: bold;");
                }

                accuracyLabel.setText(String.format("%.1f%%", accuracy));
                timeSpentLabel.setText(timeSpent + " minutes");
                totalQuestionsLabel.setText(String.valueOf(totalQuestions));
                correctAnswersLabel.setText(String.valueOf(correctAnswers));
                wrongAnswersLabel.setText(String.valueOf(wrongAnswers));

                scoreProgressBar.setProgress(percentage / 100.0);

                System.out.println("✅ Results loaded: " + obtainedMarks + "/" + totalMarks + " (" + result + ")");
            }

        } catch (SQLException e) {
            System.err.println("❌ Failed to load results!");
            e.printStackTrace();
        }
    }

    /**
     * Handle back to dashboard
     */
    @FXML
    private void handleBackToDashboard() {
        SceneManager.switchScene("/com/examverse/fxml/dashboard/student-dashboard.fxml");
    }

    /**
     * Handle view answers
     */
    @FXML
    private void handleViewAnswers() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("View Answers");
        alert.setHeaderText("Detailed Answer Review");
        alert.setContentText("Answer review feature coming soon!");
        alert.showAndWait();
    }
}