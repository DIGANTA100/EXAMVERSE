package com.examverse.service.exam;

import com.examverse.config.DatabaseConfig;
import com.examverse.model.exam.Answer;
import com.examverse.model.exam.Question;

import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * EvaluationService - Service for evaluating exams and calculating scores
 * Auto-grades MCQ exams
 * FIXED: Proper database connection handling
 */
public class EvaluationService {

    private AnswerService answerService;
    private QuestionService questionService;

    public EvaluationService() {
        this.answerService = new AnswerService();
        this.questionService = new QuestionService();
    }

    /**
     * Evaluate all answers for an attempt and update the attempt record
     * @param attemptId The exam attempt ID
     * @return true if evaluation successful
     */
    public boolean evaluateExam(int attemptId) {
        try {
            // Get all answers for this attempt
            List<Answer> answers = answerService.getDetailedAnswersByAttemptId(attemptId);

            int totalMarks = 0;
            int obtainedMarks = 0;
            int correctAnswers = 0;
            int totalQuestions = 0;

            // Evaluate each answer
            for (Answer answer : answers) {
                Question question = questionService.getQuestionById(answer.getQuestionId());

                if (question != null) {
                    totalQuestions++;
                    totalMarks += question.getMarks();

                    // Check if answer is correct
                    boolean isCorrect = question.getCorrectAnswer().equalsIgnoreCase(answer.getSelectedAnswer());
                    answer.setCorrect(isCorrect);

                    if (isCorrect) {
                        correctAnswers++;
                        answer.setMarksObtained(question.getMarks());
                        obtainedMarks += question.getMarks();
                    } else {
                        answer.setMarksObtained(0);
                    }

                    // Update answer in database
                    answerService.saveAnswer(answer);
                }
            }

            // Calculate accuracy
            double accuracy = totalQuestions > 0 ? (correctAnswers * 100.0 / totalQuestions) : 0.0;

            // Update attempt record with a FRESH connection
            updateAttemptResults(attemptId, obtainedMarks, accuracy);

            System.out.println("✅ Exam evaluated - Obtained: " + obtainedMarks + "/" + totalMarks);
            return true;

        } catch (Exception e) {
            System.err.println("❌ Evaluation failed!");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update exam attempt with results
     * FIXED: Uses its own fresh database connection
     */
    private void updateAttemptResults(int attemptId, int obtainedMarks, double accuracy) {
        // IMPORTANT: Get start time FIRST with its own connection
        LocalDateTime startTime = getAttemptStartTime(attemptId);
        LocalDateTime now = LocalDateTime.now();
        long minutesSpent = Duration.between(startTime, now).toMinutes();

        // THEN update with a FRESH connection
        String sql = """
            UPDATE student_exam_attempts SET 
                obtained_marks = ?,
                accuracy = ?,
                status = 'COMPLETED',
                submitted_at = ?,
                time_spent_minutes = ?,
                result = CASE 
                    WHEN ? >= passing_marks THEN 'PASSED'
                    ELSE 'FAILED'
                END
            WHERE attempt_id = ?
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, obtainedMarks);
            pstmt.setDouble(2, accuracy);
            pstmt.setTimestamp(3, Timestamp.valueOf(now));
            pstmt.setLong(4, minutesSpent);
            pstmt.setInt(5, obtainedMarks);  // For CASE comparison
            pstmt.setInt(6, attemptId);

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Attempt results updated successfully!");
            } else {
                System.err.println("❌ No rows updated - attempt ID might be invalid");
            }

        } catch (SQLException e) {
            System.err.println("❌ Failed to update attempt results!");
            e.printStackTrace();
        }
    }

    /**
     * Get the start time of an attempt
     * Uses its OWN connection that gets properly closed
     */
    private LocalDateTime getAttemptStartTime(int attemptId) {
        String sql = "SELECT started_at FROM student_exam_attempts WHERE attempt_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, attemptId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Timestamp timestamp = rs.getTimestamp("started_at");
                if (timestamp != null) {
                    return timestamp.toLocalDateTime();
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting attempt start time");
            e.printStackTrace();
        }

        return LocalDateTime.now(); // Fallback
    }

    /**
     * Check answer immediately (for instant feedback)
     */
    public boolean checkAnswer(int questionId, String selectedAnswer) {
        Question question = questionService.getQuestionById(questionId);

        if (question != null) {
            return question.getCorrectAnswer().equalsIgnoreCase(selectedAnswer);
        }

        return false;
    }

    /**
     * Get marks for a question
     */
    public int getQuestionMarks(int questionId, String selectedAnswer) {
        Question question = questionService.getQuestionById(questionId);

        if (question != null && question.getCorrectAnswer().equalsIgnoreCase(selectedAnswer)) {
            return question.getMarks();
        }

        return 0;
    }
}