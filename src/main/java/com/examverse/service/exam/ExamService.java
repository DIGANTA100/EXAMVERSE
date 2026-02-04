package com.examverse.service.exam;

import com.examverse.config.DatabaseConfig;
import com.examverse.model.exam.Exam;
import com.examverse.model.exam.StudentExamAttempt;
import com.examverse.model.user.StudentStats;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ExamService - Handles all exam-related database operations
 */
public class ExamService {

    /**
     * Get all available/active exams
     */
    public List<Exam> getAllActiveExams() {
        List<Exam> exams = new ArrayList<>();
        String sql = "SELECT * FROM exams WHERE status = 'ACTIVE' ORDER BY created_at DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Exam exam = extractExamFromResultSet(rs);
                exams.add(exam);
            }

            System.out.println("✅ Fetched " + exams.size() + " active exams");

        } catch (SQLException e) {
            System.err.println("❌ Error fetching active exams: " + e.getMessage());
            e.printStackTrace();
        }

        return exams;
    }

    /**
     * Get exams by subject
     */
    public List<Exam> getExamsBySubject(String subject) {
        List<Exam> exams = new ArrayList<>();
        String sql = "SELECT * FROM exams WHERE status = 'ACTIVE' AND subject = ? ORDER BY created_at DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, subject);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Exam exam = extractExamFromResultSet(rs);
                exams.add(exam);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error fetching exams by subject: " + e.getMessage());
            e.printStackTrace();
        }

        return exams;
    }

    /**
     * Get exams by difficulty
     */
    public List<Exam> getExamsByDifficulty(String difficulty) {
        List<Exam> exams = new ArrayList<>();
        String sql = "SELECT * FROM exams WHERE status = 'ACTIVE' AND difficulty = ? ORDER BY created_at DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, difficulty);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Exam exam = extractExamFromResultSet(rs);
                exams.add(exam);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error fetching exams by difficulty: " + e.getMessage());
            e.printStackTrace();
        }

        return exams;
    }

    /**
     * Get exam by ID
     */
    public Exam getExamById(int examId) {
        String sql = "SELECT * FROM exams WHERE exam_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, examId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return extractExamFromResultSet(rs);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error fetching exam by ID: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Get student's ongoing exam attempts
     */
    public List<StudentExamAttempt> getOngoingExams(int studentId) {
        List<StudentExamAttempt> attempts = new ArrayList<>();
        String sql = """
            SELECT sea.*, e.exam_title, e.subject, e.total_questions, e.total_marks, e.passing_marks
            FROM student_exam_attempts sea
            JOIN exams e ON sea.exam_id = e.exam_id
            WHERE sea.student_id = ? AND sea.status = 'ONGOING'
            ORDER BY sea.started_at DESC
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                StudentExamAttempt attempt = extractAttemptFromResultSet(rs);
                attempts.add(attempt);
            }

            System.out.println("✅ Fetched " + attempts.size() + " ongoing exams for student " + studentId);

        } catch (SQLException e) {
            System.err.println("❌ Error fetching ongoing exams: " + e.getMessage());
            e.printStackTrace();
        }

        return attempts;
    }

    /**
     * Get student's completed exam attempts
     */
    public List<StudentExamAttempt> getCompletedExams(int studentId) {
        List<StudentExamAttempt> attempts = new ArrayList<>();
        String sql = """
            SELECT sea.*, e.exam_title, e.subject, e.total_questions
            FROM student_exam_attempts sea
            JOIN exams e ON sea.exam_id = e.exam_id
            WHERE sea.student_id = ? AND sea.status = 'COMPLETED'
            ORDER BY sea.submitted_at DESC
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                StudentExamAttempt attempt = extractAttemptFromResultSet(rs);
                attempts.add(attempt);
            }

            System.out.println("✅ Fetched " + attempts.size() + " completed exams for student " + studentId);

        } catch (SQLException e) {
            System.err.println("❌ Error fetching completed exams: " + e.getMessage());
            e.printStackTrace();
        }

        return attempts;
    }

    /**
     * Get student statistics for dashboard
     */
    public StudentStats getStudentStats(int studentId) {
        StudentStats stats = new StudentStats();

        String sql = """
            SELECT 
                COUNT(*) as total_attempted,
                SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as total_completed,
                SUM(CASE WHEN result = 'PASSED' THEN 1 ELSE 0 END) as total_passed,
                SUM(CASE WHEN result = 'FAILED' THEN 1 ELSE 0 END) as total_failed,
                AVG(CASE WHEN status = 'COMPLETED' THEN (obtained_marks * 100.0 / total_marks) ELSE NULL END) as avg_score,
                AVG(CASE WHEN status = 'COMPLETED' THEN accuracy ELSE NULL END) as avg_accuracy,
                SUM(time_spent_minutes) as total_time
            FROM student_exam_attempts
            WHERE student_id = ?
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                stats.setTotalExamsAttempted(rs.getInt("total_attempted"));
                stats.setTotalExamsCompleted(rs.getInt("total_completed"));
                stats.setTotalExamsPassed(rs.getInt("total_passed"));
                stats.setTotalExamsFailed(rs.getInt("total_failed"));
                stats.setAverageScore(rs.getDouble("avg_score"));
                stats.setOverallAccuracy(rs.getDouble("avg_accuracy"));
                stats.setTotalTimeSpentMinutes(rs.getInt("total_time"));
            }

            System.out.println("✅ Fetched stats for student " + studentId);

        } catch (SQLException e) {
            System.err.println("❌ Error fetching student stats: " + e.getMessage());
            e.printStackTrace();
        }

        return stats;
    }

    /**
     * Start a new exam attempt
     */
    public int startExamAttempt(int studentId, int examId) {
        // First get exam details
        Exam exam = getExamById(examId);
        if (exam == null) {
            System.err.println("❌ Exam not found: " + examId);
            return -1;
        }

        String sql = """
            INSERT INTO student_exam_attempts 
            (student_id, exam_id, status, total_marks, passing_marks, started_at)
            VALUES (?, ?, 'ONGOING', ?, ?, ?)
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, studentId);
            pstmt.setInt(2, examId);
            pstmt.setInt(3, exam.getTotalMarks());
            pstmt.setInt(4, exam.getPassingMarks());
            pstmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    int attemptId = rs.getInt(1);
                    System.out.println("✅ Exam attempt started: " + attemptId);
                    return attemptId;
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Error starting exam attempt: " + e.getMessage());
            e.printStackTrace();
        }

        return -1;
    }

    /**
     * Get all unique subjects from exams
     */
    public List<String> getAllSubjects() {
        List<String> subjects = new ArrayList<>();
        String sql = "SELECT DISTINCT subject FROM exams WHERE status = 'ACTIVE' ORDER BY subject";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                subjects.add(rs.getString("subject"));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error fetching subjects: " + e.getMessage());
            e.printStackTrace();
        }

        return subjects;
    }

    /**
     * Extract Exam object from ResultSet
     */
    private Exam extractExamFromResultSet(ResultSet rs) throws SQLException {
        Exam exam = new Exam();
        exam.setExamId(rs.getInt("exam_id"));
        exam.setExamTitle(rs.getString("exam_title"));
        exam.setSubject(rs.getString("subject"));
        exam.setDescription(rs.getString("description"));
        exam.setDifficulty(rs.getString("difficulty"));
        exam.setTotalQuestions(rs.getInt("total_questions"));
        exam.setTotalMarks(rs.getInt("total_marks"));
        exam.setDurationMinutes(rs.getInt("duration_minutes"));
        exam.setPassingMarks(rs.getInt("passing_marks"));
        exam.setStatus(rs.getString("status"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            exam.setCreatedAt(createdAt.toLocalDateTime());
        }

        return exam;
    }

    /**
     * Extract StudentExamAttempt object from ResultSet
     */
    private StudentExamAttempt extractAttemptFromResultSet(ResultSet rs) throws SQLException {
        StudentExamAttempt attempt = new StudentExamAttempt();
        attempt.setAttemptId(rs.getInt("attempt_id"));
        attempt.setStudentId(rs.getInt("student_id"));
        attempt.setExamId(rs.getInt("exam_id"));
        attempt.setExamTitle(rs.getString("exam_title"));
        attempt.setSubject(rs.getString("subject"));
        attempt.setTotalQuestions(rs.getInt("total_questions"));
        attempt.setTotalMarks(rs.getInt("total_marks"));
        attempt.setObtainedMarks(rs.getInt("obtained_marks"));
        attempt.setPassingMarks(rs.getInt("passing_marks"));
        attempt.setStatus(rs.getString("status"));
        attempt.setTimeSpentMinutes(rs.getInt("time_spent_minutes"));
        attempt.setAccuracy(rs.getDouble("accuracy"));
        attempt.setResult(rs.getString("result"));

        Timestamp startedAt = rs.getTimestamp("started_at");
        if (startedAt != null) {
            attempt.setStartedAt(startedAt.toLocalDateTime());
        }

        Timestamp submittedAt = rs.getTimestamp("submitted_at");
        if (submittedAt != null) {
            attempt.setSubmittedAt(submittedAt.toLocalDateTime());
        }

        return attempt;
    }
}