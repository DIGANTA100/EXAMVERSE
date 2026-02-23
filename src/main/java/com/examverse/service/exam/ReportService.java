package com.examverse.service.exam;

import com.examverse.config.DatabaseConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ReportService - Service for generating exam reports and statistics
 * Provides data for admin reports dashboard
 */
public class ReportService {

    /**
     * Get overall statistics across all exams
     */
    public Map<String, Object> getOverallStatistics() {
        Map<String, Object> stats = new HashMap<>();

        String sql = """
            SELECT 
                COUNT(DISTINCT exam_id) as total_exams,
                COUNT(DISTINCT student_id) as total_students,
                COUNT(*) as total_attempts,
                AVG(CASE WHEN status = 'COMPLETED' THEN obtained_marks * 100.0 / total_marks ELSE NULL END) as avg_score,
                SUM(CASE WHEN result = 'PASSED' THEN 1 ELSE 0 END) * 100.0 / 
                    NULLIF(SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END), 0) as pass_rate,
                AVG(CASE WHEN status = 'COMPLETED' THEN time_spent_minutes ELSE NULL END) as avg_time
            FROM student_exam_attempts
            WHERE status = 'COMPLETED'
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                stats.put("totalExams", rs.getInt("total_exams"));
                stats.put("totalStudents", rs.getInt("total_students"));
                stats.put("totalAttempts", rs.getInt("total_attempts"));
                stats.put("avgScore", rs.getDouble("avg_score"));
                stats.put("passRate", rs.getDouble("pass_rate"));
                stats.put("avgTime", rs.getDouble("avg_time"));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error fetching overall statistics");
            e.printStackTrace();
        }

        return stats;
    }

    /**
     * Get detailed statistics for each exam
     */
    public List<Map<String, Object>> getExamStatistics() {
        List<Map<String, Object>> examStats = new ArrayList<>();

        String sql = """
            SELECT 
                e.exam_id,
                e.exam_title,
                e.subject,
                e.difficulty,
                e.total_questions,
                e.total_marks,
                e.passing_marks,
                COUNT(sea.attempt_id) as total_attempts,
                COUNT(DISTINCT sea.student_id) as unique_students,
                AVG(CASE WHEN sea.status = 'COMPLETED' THEN sea.obtained_marks ELSE NULL END) as avg_score,
                MAX(sea.obtained_marks) as highest_score,
                MIN(CASE WHEN sea.status = 'COMPLETED' THEN sea.obtained_marks ELSE NULL END) as lowest_score,
                SUM(CASE WHEN sea.result = 'PASSED' THEN 1 ELSE 0 END) * 100.0 / 
                    NULLIF(SUM(CASE WHEN sea.status = 'COMPLETED' THEN 1 ELSE 0 END), 0) as pass_rate,
                AVG(CASE WHEN sea.status = 'COMPLETED' THEN sea.accuracy ELSE NULL END) as avg_accuracy,
                AVG(CASE WHEN sea.status = 'COMPLETED' THEN sea.time_spent_minutes ELSE NULL END) as avg_time
            FROM exams e
            LEFT JOIN student_exam_attempts sea ON e.exam_id = sea.exam_id
            GROUP BY e.exam_id
            ORDER BY e.created_at DESC
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Map<String, Object> examStat = new HashMap<>();
                examStat.put("examId", rs.getInt("exam_id"));
                examStat.put("examTitle", rs.getString("exam_title"));
                examStat.put("subject", rs.getString("subject"));
                examStat.put("difficulty", rs.getString("difficulty"));
                examStat.put("totalQuestions", rs.getInt("total_questions"));
                examStat.put("totalMarks", rs.getInt("total_marks"));
                examStat.put("passingMarks", rs.getInt("passing_marks"));
                examStat.put("totalAttempts", rs.getInt("total_attempts"));
                examStat.put("uniqueStudents", rs.getInt("unique_students"));
                examStat.put("avgScore", rs.getDouble("avg_score"));
                examStat.put("highestScore", rs.getInt("highest_score"));
                examStat.put("lowestScore", rs.getInt("lowest_score"));
                examStat.put("passRate", rs.getDouble("pass_rate"));
                examStat.put("avgAccuracy", rs.getDouble("avg_accuracy"));
                examStat.put("avgTime", rs.getDouble("avg_time"));

                examStats.add(examStat);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error fetching exam statistics");
            e.printStackTrace();
        }

        return examStats;
    }

    /**
     * Get student performance for a specific exam
     */
    public List<Map<String, Object>> getStudentPerformance(int examId) {
        List<Map<String, Object>> students = new ArrayList<>();

        String sql = """
            SELECT 
                u.id as student_id,
                u.full_name,
                u.username,
                sea.attempt_id,
                sea.obtained_marks,
                sea.total_marks,
                sea.accuracy,
                sea.result,
                sea.time_spent_minutes,
                sea.submitted_at
            FROM student_exam_attempts sea
            JOIN users u ON sea.student_id = u.id
            WHERE sea.exam_id = ? AND sea.status = 'COMPLETED'
            ORDER BY sea.obtained_marks DESC, sea.submitted_at ASC
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, examId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> student = new HashMap<>();
                student.put("studentId", rs.getInt("student_id"));
                student.put("fullName", rs.getString("full_name"));
                student.put("username", rs.getString("username"));
                student.put("attemptId", rs.getInt("attempt_id"));
                student.put("obtainedMarks", rs.getInt("obtained_marks"));
                student.put("totalMarks", rs.getInt("total_marks"));
                student.put("accuracy", rs.getDouble("accuracy"));
                student.put("result", rs.getString("result"));
                student.put("timeSpent", rs.getInt("time_spent_minutes"));
                student.put("submittedAt", rs.getTimestamp("submitted_at"));

                students.add(student);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error fetching student performance");
            e.printStackTrace();
        }

        return students;
    }

    /**
     * Get statistics for a specific exam
     */
    public Map<String, Object> getExamStatisticsById(int examId) {
        Map<String, Object> stats = new HashMap<>();

        String sql = """
            SELECT 
                e.exam_id,
                e.exam_title,
                e.subject,
                e.difficulty,
                e.total_questions,
                e.total_marks,
                e.passing_marks,
                e.duration_minutes,
                COUNT(sea.attempt_id) as total_attempts,
                COUNT(DISTINCT sea.student_id) as unique_students,
                AVG(CASE WHEN sea.status = 'COMPLETED' THEN sea.obtained_marks ELSE NULL END) as avg_score,
                MAX(sea.obtained_marks) as highest_score,
                MIN(CASE WHEN sea.status = 'COMPLETED' THEN sea.obtained_marks ELSE NULL END) as lowest_score,
                SUM(CASE WHEN sea.result = 'PASSED' THEN 1 ELSE 0 END) as passed_count,
                SUM(CASE WHEN sea.result = 'FAILED' THEN 1 ELSE 0 END) as failed_count,
                SUM(CASE WHEN sea.result = 'PASSED' THEN 1 ELSE 0 END) * 100.0 / 
                    NULLIF(SUM(CASE WHEN sea.status = 'COMPLETED' THEN 1 ELSE 0 END), 0) as pass_rate,
                AVG(CASE WHEN sea.status = 'COMPLETED' THEN sea.accuracy ELSE NULL END) as avg_accuracy,
                AVG(CASE WHEN sea.status = 'COMPLETED' THEN sea.time_spent_minutes ELSE NULL END) as avg_time
            FROM exams e
            LEFT JOIN student_exam_attempts sea ON e.exam_id = sea.exam_id
            WHERE e.exam_id = ?
            GROUP BY e.exam_id
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, examId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                stats.put("examId", rs.getInt("exam_id"));
                stats.put("examTitle", rs.getString("exam_title"));
                stats.put("subject", rs.getString("subject"));
                stats.put("difficulty", rs.getString("difficulty"));
                stats.put("totalQuestions", rs.getInt("total_questions"));
                stats.put("totalMarks", rs.getInt("total_marks"));
                stats.put("passingMarks", rs.getInt("passing_marks"));
                stats.put("duration", rs.getInt("duration_minutes"));
                stats.put("totalAttempts", rs.getInt("total_attempts"));
                stats.put("uniqueStudents", rs.getInt("unique_students"));
                stats.put("avgScore", rs.getDouble("avg_score"));
                stats.put("highestScore", rs.getInt("highest_score"));
                stats.put("lowestScore", rs.getInt("lowest_score"));
                stats.put("passedCount", rs.getInt("passed_count"));
                stats.put("failedCount", rs.getInt("failed_count"));
                stats.put("passRate", rs.getDouble("pass_rate"));
                stats.put("avgAccuracy", rs.getDouble("avg_accuracy"));
                stats.put("avgTime", rs.getDouble("avg_time"));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error fetching exam statistics by ID");
            e.printStackTrace();
        }

        return stats;
    }

    /**
     * Get question-wise statistics for an exam (which questions were hardest)
     */
    public List<Map<String, Object>> getQuestionStatistics(int examId) {
        List<Map<String, Object>> questionStats = new ArrayList<>();

        String sql = """
            SELECT 
                q.question_id,
                q.question_text,
                q.correct_answer,
                q.marks,
                COUNT(sa.answer_id) as total_attempts,
                SUM(CASE WHEN sa.is_correct = TRUE THEN 1 ELSE 0 END) as correct_attempts,
                SUM(CASE WHEN sa.is_correct = TRUE THEN 1 ELSE 0 END) * 100.0 / 
                    NULLIF(COUNT(sa.answer_id), 0) as success_rate
            FROM questions q
            LEFT JOIN student_answers sa ON q.question_id = sa.question_id
            WHERE q.exam_id = ?
            GROUP BY q.question_id
            ORDER BY success_rate ASC, q.question_id ASC
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, examId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> qStat = new HashMap<>();
                qStat.put("questionId", rs.getInt("question_id"));
                qStat.put("questionText", rs.getString("question_text"));
                qStat.put("correctAnswer", rs.getString("correct_answer"));
                qStat.put("marks", rs.getInt("marks"));
                qStat.put("totalAttempts", rs.getInt("total_attempts"));
                qStat.put("correctAttempts", rs.getInt("correct_attempts"));
                qStat.put("successRate", rs.getDouble("success_rate"));

                questionStats.add(qStat);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error fetching question statistics");
            e.printStackTrace();
        }

        return questionStats;
    }
}