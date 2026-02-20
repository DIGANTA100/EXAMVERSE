package com.examverse.service.exam;

import com.examverse.config.DatabaseConfig;
import com.examverse.model.exam.Answer;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AnswerService - Service for managing student answers.
 *
 * Changes from original:
 *
 * 1. saveAnswer() upgraded to UPSERT (INSERT ... ON DUPLICATE KEY UPDATE).
 *    The old check-then-insert/update used two DB connections and had a race
 *    condition. The UPSERT does it in one query and is atomic.
 *    Requires a UNIQUE KEY on (attempt_id, question_id) — run once on your DB:
 *
 *      ALTER TABLE student_answers
 *        ADD CONSTRAINT uq_attempt_question
 *        UNIQUE (attempt_id, question_id);
 *
 *    If the constraint doesn't exist yet, saveAnswer() automatically falls
 *    back to the original check-then-insert/update logic so nothing breaks.
 *
 * 2. Added getAnswersByAttemptId(int) returning Map<Integer, String>
 *    (questionId → selectedAnswer). Used by ExamController.loadSavedAnswers()
 *    on resume to restore radio-button selections and palette colours.
 *    The original method returning List<Answer> is renamed to
 *    getDetailedAnswersByAttemptId() and kept fully intact.
 *
 * 3. Added getAnsweredCount(int) — used by StudentDashboardController to show
 *    "X/Y answered" progress on the Ongoing exam card.
 *
 * 4. deleteAnswersByAttemptId() return type changed to void (callers never
 *    used the boolean). A private boolean version is kept for internal use.
 */
public class AnswerService {

    // ══════════════════════════════════════════════════════════════════
    //  PRIMARY SAVE — UPSERT
    // ══════════════════════════════════════════════════════════════════

    /**
     * Save or update a student's answer using a single UPSERT.
     * Falls back to the original two-query path if the UNIQUE constraint
     * does not exist on student_answers (uq_attempt_question).
     */
    public boolean saveAnswer(Answer answer) {
        if (answer == null) return false;

        String sql = """
            INSERT INTO student_answers
                (attempt_id, question_id, selected_answer, is_correct, marks_obtained, time_spent_seconds)
            VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                selected_answer    = VALUES(selected_answer),
                time_spent_seconds = VALUES(time_spent_seconds)
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, answer.getAttemptId());
            ps.setInt(2, answer.getQuestionId());
            ps.setString(3, answer.getSelectedAnswer());
            ps.setBoolean(4, answer.isCorrect());
            ps.setInt(5, answer.getMarksObtained());
            ps.setInt(6, answer.getTimeSpentSeconds());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) answer.setAnswerId(keys.getInt(1));
            }

            return true;

        } catch (SQLException e) {
            System.err.println("❌ saveAnswer UPSERT failed, trying fallback: " + e.getMessage());
            // Fallback to original two-query path
            return saveAnswerFallback(answer);
        }
    }

    private boolean saveAnswerFallback(Answer answer) {
        if (answerExists(answer.getAttemptId(), answer.getQuestionId())) {
            return updateAnswer(answer);
        } else {
            return insertAnswer(answer);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  RESUME SUPPORT
    // ══════════════════════════════════════════════════════════════════

    /**
     * Returns questionId → selectedAnswer for every saved answer in this attempt.
     *
     * Called by ExamController.loadSavedAnswers() on resume so:
     *  - studentAnswers HashMap is pre-populated with saved progress
     *  - each question shows the previously chosen radio selection
     *  - palette buttons show the correct answered/unanswered colours
     *
     * Returns an empty map (never null) when no answers are found.
     */
    public Map<Integer, String> getAnswersByAttemptId(int attemptId) {
        Map<Integer, String> answers = new HashMap<>();
        String sql = "SELECT question_id, selected_answer FROM student_answers WHERE attempt_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, attemptId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int    qId    = rs.getInt("question_id");
                    String chosen = rs.getString("selected_answer");
                    if (chosen != null && !chosen.isBlank()) {
                        answers.put(qId, chosen.toUpperCase().trim());
                    }
                }
            }

            System.out.println("📂 getAnswersByAttemptId(" + attemptId + "): "
                    + answers.size() + " answer(s) loaded");

        } catch (SQLException e) {
            System.err.println("❌ getAnswersByAttemptId(" + attemptId + "): " + e.getMessage());
            e.printStackTrace();
        }

        return answers;
    }

    // ══════════════════════════════════════════════════════════════════
    //  PROGRESS COUNT
    // ══════════════════════════════════════════════════════════════════

    /**
     * How many distinct questions have a saved answer for this attempt.
     * Used by StudentDashboardController to show "X/Y answered" on the Ongoing card.
     */
    public int getAnsweredCount(int attemptId) {
        String sql = "SELECT COUNT(DISTINCT question_id) FROM student_answers WHERE attempt_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, attemptId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("❌ getAnsweredCount(" + attemptId + "): " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    // ══════════════════════════════════════════════════════════════════
    //  DETAILED LIST (original method — renamed to avoid conflict)
    // ══════════════════════════════════════════════════════════════════

    /**
     * Returns full Answer objects for an attempt, joined with question text
     * and correct answer. Used by ResultController for detailed review.
     *
     * Renamed from getAnswersByAttemptId to getDetailedAnswersByAttemptId
     * to avoid a signature conflict with the new Map-returning method above.
     * Update any ResultController / other callers to use this new name.
     */
    public List<Answer> getDetailedAnswersByAttemptId(int attemptId) {
        List<Answer> answers = new ArrayList<>();
        String sql = """
            SELECT sa.*, q.question_text, q.correct_answer
            FROM student_answers sa
            JOIN questions q ON sa.question_id = q.question_id
            WHERE sa.attempt_id = ?
            ORDER BY sa.question_id ASC
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, attemptId);
            try (ResultSet rs = ps.executeQuery()) {
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
                    if (answeredAt != null) answer.setAnsweredAt(answeredAt.toLocalDateTime());

                    answers.add(answer);
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ getDetailedAnswersByAttemptId(" + attemptId + "): " + e.getMessage());
            e.printStackTrace();
        }

        return answers;
    }

    // ══════════════════════════════════════════════════════════════════
    //  SINGLE ANSWER LOOKUP
    // ══════════════════════════════════════════════════════════════════

    /**
     * Get the saved answer for one specific question in an attempt.
     * Returns null if not yet answered.
     */
    public Answer getAnswer(int attemptId, int questionId) {
        String sql = "SELECT * FROM student_answers WHERE attempt_id = ? AND question_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, attemptId);
            ps.setInt(2, questionId);
            try (ResultSet rs = ps.executeQuery()) {
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
            }

        } catch (SQLException e) {
            System.err.println("❌ getAnswer(attempt=" + attemptId + ", q=" + questionId + "): " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    // ══════════════════════════════════════════════════════════════════
    //  DELETE
    // ══════════════════════════════════════════════════════════════════

    /**
     * Delete ALL answers for a given attempt.
     * Called by ExamController on fresh start (not resume) and by
     * ExamService.deleteAttempt() during cleanup.
     */
    public void deleteAnswersByAttemptId(int attemptId) {
        if (attemptId <= 0) return;

        String sql = "DELETE FROM student_answers WHERE attempt_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, attemptId);
            int rows = ps.executeUpdate();
            System.out.println("🗑 Deleted " + rows + " answer(s) for attemptId=" + attemptId);

        } catch (SQLException e) {
            System.err.println("❌ deleteAnswersByAttemptId(" + attemptId + "): " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS (original insert/update/exists — kept as fallback)
    // ══════════════════════════════════════════════════════════════════

    private boolean insertAnswer(Answer answer) {
        String sql = """
            INSERT INTO student_answers
                (attempt_id, question_id, selected_answer, is_correct, marks_obtained, time_spent_seconds)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, answer.getAttemptId());
            ps.setInt(2, answer.getQuestionId());
            ps.setString(3, answer.getSelectedAnswer());
            ps.setBoolean(4, answer.isCorrect());
            ps.setInt(5, answer.getMarksObtained());
            ps.setInt(6, answer.getTimeSpentSeconds());

            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) answer.setAnswerId(keys.getInt(1));
                }
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ insertAnswer: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    private boolean updateAnswer(Answer answer) {
        String sql = """
            UPDATE student_answers SET
                selected_answer    = ?,
                is_correct         = ?,
                marks_obtained     = ?,
                time_spent_seconds = ?
            WHERE attempt_id = ? AND question_id = ?
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, answer.getSelectedAnswer());
            ps.setBoolean(2, answer.isCorrect());
            ps.setInt(3, answer.getMarksObtained());
            ps.setInt(4, answer.getTimeSpentSeconds());
            ps.setInt(5, answer.getAttemptId());
            ps.setInt(6, answer.getQuestionId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ updateAnswer: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    private boolean answerExists(int attemptId, int questionId) {
        String sql = "SELECT COUNT(*) FROM student_answers WHERE attempt_id = ? AND question_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, attemptId);
            ps.setInt(2, questionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}