package com.examverse.service.exam;

import com.examverse.config.DatabaseConfig;
import com.examverse.model.exam.Answer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * AnswerService - Service for managing student answers
 * Handles CRUD operations for student_answers table
 */
public class AnswerService {

    /**
     * Save or update a student's answer
     */
    public boolean saveAnswer(Answer answer) {
        // Check if answer already exists
        if (answerExists(answer.getAttemptId(), answer.getQuestionId())) {
            return updateAnswer(answer);
        } else {
            return insertAnswer(answer);
        }
    }

    /**
     * Insert a new answer
     */
    private boolean insertAnswer(Answer answer) {
        String sql = """
            INSERT INTO student_answers 
            (attempt_id, question_id, selected_answer, is_correct, marks_obtained, time_spent_seconds)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, answer.getAttemptId());
            pstmt.setInt(2, answer.getQuestionId());
            pstmt.setString(3, answer.getSelectedAnswer());
            pstmt.setBoolean(4, answer.isCorrect());
            pstmt.setInt(5, answer.getMarksObtained());
            pstmt.setInt(6, answer.getTimeSpentSeconds());

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    answer.setAnswerId(rs.getInt(1));
                }
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ Failed to insert answer!");
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Update an existing answer
     */
    private boolean updateAnswer(Answer answer) {
        String sql = """
            UPDATE student_answers SET 
                selected_answer = ?, 
                is_correct = ?, 
                marks_obtained = ?,
                time_spent_seconds = ?
            WHERE attempt_id = ? AND question_id = ?
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, answer.getSelectedAnswer());
            pstmt.setBoolean(2, answer.isCorrect());
            pstmt.setInt(3, answer.getMarksObtained());
            pstmt.setInt(4, answer.getTimeSpentSeconds());
            pstmt.setInt(5, answer.getAttemptId());
            pstmt.setInt(6, answer.getQuestionId());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("❌ Failed to update answer!");
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Check if an answer already exists
     */
    private boolean answerExists(int attemptId, int questionId) {
        String sql = "SELECT COUNT(*) FROM student_answers WHERE attempt_id = ? AND question_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, attemptId);
            pstmt.setInt(2, questionId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Get all answers for an attempt
     */
    public List<Answer> getAnswersByAttemptId(int attemptId) {
        List<Answer> answers = new ArrayList<>();
        String sql = """
            SELECT sa.*, q.question_text, q.correct_answer
            FROM student_answers sa
            JOIN questions q ON sa.question_id = q.question_id
            WHERE sa.attempt_id = ?
            ORDER BY sa.question_id ASC
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, attemptId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Answer answer = new Answer();
                answer.setAnswerId(rs.getInt("answer_id"));
                answer.setAttemptId(rs.getInt("attempt_id"));
                answer.setQuestionId(rs.getInt("question_id"));
                answer.setSelectedAnswer(rs.getString("selected_answer"));
                answer.setCorrect(rs.getBoolean("is_correct"));
                answer.setMarksObtained(rs.getInt("marks_obtained"));
                answer.setTimeSpentSeconds(rs.getInt("time_spent_seconds"));
                answer.setQuestionText(rs.getString("question_text"));
                answer.setCorrectAnswer(rs.getString("correct_answer"));

                Timestamp answeredAt = rs.getTimestamp("answered_at");
                if (answeredAt != null) {
                    answer.setAnsweredAt(answeredAt.toLocalDateTime());
                }

                answers.add(answer);
            }

        } catch (SQLException e) {
            System.err.println("❌ Failed to get answers!");
            e.printStackTrace();
        }

        return answers;
    }

    /**
     * Get student's answer for a specific question
     */
    public Answer getAnswer(int attemptId, int questionId) {
        String sql = "SELECT * FROM student_answers WHERE attempt_id = ? AND question_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, attemptId);
            pstmt.setInt(2, questionId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Answer answer = new Answer();
                answer.setAnswerId(rs.getInt("answer_id"));
                answer.setAttemptId(rs.getInt("attempt_id"));
                answer.setQuestionId(rs.getInt("question_id"));
                answer.setSelectedAnswer(rs.getString("selected_answer"));
                answer.setCorrect(rs.getBoolean("is_correct"));
                answer.setMarksObtained(rs.getInt("marks_obtained"));
                answer.setTimeSpentSeconds(rs.getInt("time_spent_seconds"));
                return answer;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Delete all answers for an attempt
     */
    public boolean deleteAnswersByAttemptId(int attemptId) {
        String sql = "DELETE FROM student_answers WHERE attempt_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, attemptId);
            int rowsAffected = pstmt.executeUpdate();
            System.out.println("✅ Deleted " + rowsAffected + " answers for attempt " + attemptId);
            return true;

        } catch (SQLException e) {
            System.err.println("❌ Failed to delete answers!");
            e.printStackTrace();
        }

        return false;
    }
}