package com.examverse.service.exam;

import com.examverse.config.DatabaseConfig;
import com.examverse.model.exam.Question;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * QuestionService - Service layer for question operations
 * Handles CRUD operations for exam questions
 */
public class QuestionService {

    /**
     * Get all questions for a specific exam
     */
    public List<Question> getQuestionsByExamId(int examId) {
        List<Question> questions = new ArrayList<>();
        String sql = "SELECT * FROM questions WHERE exam_id = ? ORDER BY question_id ASC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, examId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                questions.add(mapResultSetToQuestion(rs));
            }

            System.out.println("✅ Loaded " + questions.size() + " questions for exam " + examId);

        } catch (SQLException e) {
            System.err.println("❌ Error loading questions!");
            e.printStackTrace();
        }

        return questions;
    }

    /**
     * Get a single question by ID
     */
    public Question getQuestionById(int questionId) {
        String sql = "SELECT * FROM questions WHERE question_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, questionId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToQuestion(rs);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Create a new question
     */
    public boolean createQuestion(Question question) {
        String sql = """
            INSERT INTO questions (exam_id, question_text, option_a, option_b, 
                                 option_c, option_d, correct_answer, marks, explanation)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, question.getExamId());
            pstmt.setString(2, question.getQuestionText());
            pstmt.setString(3, question.getOptionA());
            pstmt.setString(4, question.getOptionB());
            pstmt.setString(5, question.getOptionC());
            pstmt.setString(6, question.getOptionD());
            pstmt.setString(7, question.getCorrectAnswer());
            pstmt.setInt(8, question.getMarks());
            pstmt.setString(9, question.getExplanation());

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    question.setQuestionId(generatedKeys.getInt(1));
                }
                System.out.println("✅ Question created for exam " + question.getExamId());
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ Failed to create question!");
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Update an existing question
     */
    public boolean updateQuestion(Question question) {
        String sql = """
            UPDATE questions SET 
                question_text = ?, 
                option_a = ?, 
                option_b = ?, 
                option_c = ?, 
                option_d = ?,
                correct_answer = ?, 
                marks = ?, 
                explanation = ?
            WHERE question_id = ?
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, question.getQuestionText());
            pstmt.setString(2, question.getOptionA());
            pstmt.setString(3, question.getOptionB());
            pstmt.setString(4, question.getOptionC());
            pstmt.setString(5, question.getOptionD());
            pstmt.setString(6, question.getCorrectAnswer());
            pstmt.setInt(7, question.getMarks());
            pstmt.setString(8, question.getExplanation());
            pstmt.setInt(9, question.getQuestionId());

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Question updated: " + question.getQuestionId());
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ Failed to update question!");
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Delete a question
     */
    public boolean deleteQuestion(int questionId) {
        String sql = "DELETE FROM questions WHERE question_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, questionId);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Question deleted: " + questionId);
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ Failed to delete question!");
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Get total questions count for an exam
     */
    public int getQuestionCountForExam(int examId) {
        String sql = "SELECT COUNT(*) FROM questions WHERE exam_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, examId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Get total questions count across all exams
     */
    public int getTotalQuestionsCount() {
        String sql = "SELECT COUNT(*) FROM questions";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Bulk insert questions for an exam
     */
    public boolean addQuestionsToExam(int examId, List<Question> questions) {
        int successCount = 0;

        for (Question question : questions) {
            question.setExamId(examId);
            if (createQuestion(question)) {
                successCount++;
            }
        }

        System.out.println("✅ Added " + successCount + "/" + questions.size() + " questions to exam " + examId);
        return successCount == questions.size();
    }

    /**
     * Map ResultSet to Question object
     */
    private Question mapResultSetToQuestion(ResultSet rs) throws SQLException {
        Question question = new Question();
        question.setQuestionId(rs.getInt("question_id"));
        question.setExamId(rs.getInt("exam_id"));
        question.setQuestionText(rs.getString("question_text"));
        question.setOptionA(rs.getString("option_a"));
        question.setOptionB(rs.getString("option_b"));
        question.setOptionC(rs.getString("option_c"));
        question.setOptionD(rs.getString("option_d"));
        question.setCorrectAnswer(rs.getString("correct_answer"));
        question.setMarks(rs.getInt("marks"));
        question.setExplanation(rs.getString("explanation"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            question.setCreatedAt(createdAt.toLocalDateTime());
        }

        return question;
    }
}