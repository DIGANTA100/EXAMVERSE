package com.examverse.service.exam;

import com.examverse.config.DatabaseConfig;
import com.examverse.model.exam.Question;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * QuestionService - Service layer for question CRUD operations.
 *
 * Bug fixes applied:
 * 1. getQuestionsByExamId() — ResultSet was not closed; now uses try-with-resources
 *    for the ResultSet too, preventing connection pool leaks under heavy load.
 * 2. createQuestion() — generated keys ResultSet was never closed.
 * 3. addQuestionsToExam() — now uses a single Connection + batch INSERT for
 *    performance; previously opened N separate connections for N questions.
 * 4. mapResultSetToQuestion() — explanation and created_at columns are optional
 *    (may be NULL in DB); both are now read safely.
 * 5. getQuestionCountForExam() — ResultSet not closed; fixed with try-with-resources.
 * 6. deleteQuestion() — now also deletes related student_answers rows first to
 *    avoid FK constraint violations when a question is removed after students
 *    have already answered it.
 * 7. updateQuestion() — exam_id update was missing; questions should be
 *    non-movable between exams, so exam_id is intentionally excluded from UPDATE
 *    (no change needed, was already correct).
 */
public class QuestionService {

    /**
     * Get all questions for a specific exam, ordered by question_id.
     */
    public List<Question> getQuestionsByExamId(int examId) {
        List<Question> questions = new ArrayList<>();
        String sql = "SELECT * FROM questions WHERE exam_id = ? ORDER BY question_id ASC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, examId);

            // FIX: ResultSet explicitly closed via try-with-resources
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    questions.add(mapResultSetToQuestion(rs));
                }
            }

            System.out.println("✅ Loaded " + questions.size() + " questions for exam " + examId);

        } catch (SQLException e) {
            System.err.println("❌ Error loading questions for exam " + examId + ": " + e.getMessage());
            e.printStackTrace();
        }

        return questions;
    }

    /**
     * Get a single question by its ID.
     */
    public Question getQuestionById(int questionId) {
        String sql = "SELECT * FROM questions WHERE question_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, questionId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToQuestion(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Error fetching question " + questionId + ": " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Create a new question and set the generated ID on the Question object.
     */
    public boolean createQuestion(Question question) {
        if (question == null) return false;

        String sql = """
            INSERT INTO questions
                (exam_id, question_text, option_a, option_b, option_c, option_d,
                 correct_answer, marks, explanation)
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
            pstmt.setInt(8, question.getMarks() > 0 ? question.getMarks() : 1); // default 1 mark
            pstmt.setString(9, question.getExplanation()); // NULL is fine

            int rows = pstmt.executeUpdate();

            if (rows > 0) {
                // FIX: generated keys ResultSet is now properly closed
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        question.setQuestionId(generatedKeys.getInt(1));
                    }
                }
                System.out.println("✅ Question created (id=" + question.getQuestionId()
                        + ") for exam " + question.getExamId());
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ Failed to create question for exam "
                    + question.getExamId() + ": " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Update an existing question.
     * Note: exam_id is intentionally NOT updatable — questions belong to one exam.
     */
    public boolean updateQuestion(Question question) {
        if (question == null || question.getQuestionId() <= 0) return false;

        String sql = """
            UPDATE questions SET
                question_text  = ?,
                option_a       = ?,
                option_b       = ?,
                option_c       = ?,
                option_d       = ?,
                correct_answer = ?,
                marks          = ?,
                explanation    = ?
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
            pstmt.setInt(7, question.getMarks() > 0 ? question.getMarks() : 1);
            pstmt.setString(8, question.getExplanation());
            pstmt.setInt(9, question.getQuestionId());

            int rows = pstmt.executeUpdate();

            if (rows > 0) {
                System.out.println("✅ Question updated: " + question.getQuestionId());
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ Failed to update question " + question.getQuestionId()
                    + ": " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Delete a question.
     *
     * BUG FIX: student_answers rows that reference this question_id must be
     * deleted first; otherwise a FK constraint violation is thrown if the DB
     * has a FK from student_answers(question_id) → questions(question_id).
     * Uses a transaction so both deletes succeed or both are rolled back.
     */
    public boolean deleteQuestion(int questionId) {
        String deleteAnswers  = "DELETE FROM student_answers WHERE question_id = ?";
        String deleteQuestion = "DELETE FROM questions WHERE question_id = ?";

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pa = conn.prepareStatement(deleteAnswers);
                 PreparedStatement pq = conn.prepareStatement(deleteQuestion)) {

                pa.setInt(1, questionId);
                pa.executeUpdate();

                pq.setInt(1, questionId);
                int rows = pq.executeUpdate();

                conn.commit();

                if (rows > 0) {
                    System.out.println("✅ Question deleted: " + questionId);
                    return true;
                }

            } catch (SQLException inner) {
                conn.rollback();
                throw inner;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("❌ Failed to delete question " + questionId + ": " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Get total questions count for a specific exam.
     */
    public int getQuestionCountForExam(int examId) {
        String sql = "SELECT COUNT(*) FROM questions WHERE exam_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, examId);

            // FIX: ResultSet closed via try-with-resources
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error counting questions for exam " + examId + ": " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Get total questions count across ALL exams (admin dashboard stat).
     */
    public int getTotalQuestionsCount() {
        String sql = "SELECT COUNT(*) FROM questions";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) return rs.getInt(1);

        } catch (SQLException e) {
            System.err.println("❌ Error counting total questions: " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Bulk insert questions for an exam.
     *
     * BUG FIX: original code opened a new DB connection for every single question
     * via createQuestion().  Replaced with a single-connection batch INSERT that
     * is orders of magnitude faster and uses far fewer resources.
     * Falls back to the sequential path if batch fails.
     */
    public boolean addQuestionsToExam(int examId, List<Question> questions) {
        if (questions == null || questions.isEmpty()) return true;

        String sql = """
            INSERT INTO questions
                (exam_id, question_text, option_a, option_b, option_c, option_d,
                 correct_answer, marks, explanation)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                for (Question q : questions) {
                    q.setExamId(examId);
                    pstmt.setInt(1, examId);
                    pstmt.setString(2, q.getQuestionText());
                    pstmt.setString(3, q.getOptionA());
                    pstmt.setString(4, q.getOptionB());
                    pstmt.setString(5, q.getOptionC());
                    pstmt.setString(6, q.getOptionD());
                    pstmt.setString(7, q.getCorrectAnswer());
                    pstmt.setInt(8, q.getMarks() > 0 ? q.getMarks() : 1);
                    pstmt.setString(9, q.getExplanation());
                    pstmt.addBatch();
                }

                pstmt.executeBatch();
                conn.commit();

                // Retrieve generated keys and assign back to Question objects
                try (ResultSet keys = pstmt.getGeneratedKeys()) {
                    int idx = 0;
                    while (keys.next() && idx < questions.size()) {
                        questions.get(idx).setQuestionId(keys.getInt(1));
                        idx++;
                    }
                }

                System.out.println("✅ Bulk inserted " + questions.size()
                        + " questions for exam " + examId);
                return true;

            } catch (SQLException batchEx) {
                conn.rollback();
                System.err.println("⚠ Batch insert failed, falling back to sequential: "
                        + batchEx.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("❌ DB error in addQuestionsToExam: " + e.getMessage());
            e.printStackTrace();
        }

        // Fallback: sequential one-by-one (original behaviour)
        int successCount = 0;
        for (Question q : questions) {
            q.setExamId(examId);
            if (createQuestion(q)) successCount++;
        }
        System.out.println("✅ Sequential fallback: added " + successCount
                + "/" + questions.size() + " questions to exam " + examId);
        return successCount == questions.size();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  MAPPING
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Map a ResultSet row to a Question object.
     *
     * BUG FIX: explanation is NULL-safe (column may not exist in older schemas);
     * created_at is read safely and skipped if absent.
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

        // explanation is optional — may be NULL
        String explanation = rs.getString("explanation");
        question.setExplanation(explanation); // null is acceptable

        // created_at may not exist in all schemas
        try {
            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                question.setCreatedAt(createdAt.toLocalDateTime());
            }
        } catch (SQLException ignored) {
            // Column absent — not a problem
        }

        return question;
    }
}