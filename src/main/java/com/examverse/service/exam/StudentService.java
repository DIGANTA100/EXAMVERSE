package com.examverse.service.exam;

import com.examverse.config.DatabaseConfig;
import com.examverse.model.user.StudentStats;
import com.examverse.model.user.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * StudentService - Service for student data operations
 * Handles student statistics, performance tracking, and management
 */
public class StudentService {

    /**
     * Get all students with their statistics
     */
    public List<Map<String, Object>> getAllStudentsWithStats() {
        List<Map<String, Object>> students = new ArrayList<>();

        String sql = """
            SELECT 
                u.id,
                u.username,
                u.email,
                u.full_name,
                u.is_active,
                u.created_at,
                u.last_login,
                COUNT(DISTINCT sea.attempt_id) as total_attempts,
                COUNT(DISTINCT CASE WHEN sea.status = 'COMPLETED' THEN sea.attempt_id END) as completed_exams,
                AVG(CASE WHEN sea.status = 'COMPLETED' 
                    THEN sea.obtained_marks * 100.0 / NULLIF(sea.total_marks, 0) END) as avg_score,
                SUM(CASE WHEN sea.result = 'PASSED' THEN 1 ELSE 0 END) * 100.0 / 
                    NULLIF(COUNT(CASE WHEN sea.status = 'COMPLETED' THEN 1 END), 0) as pass_rate,
                AVG(CASE WHEN sea.status = 'COMPLETED' THEN sea.accuracy END) as avg_accuracy,
                SUM(CASE WHEN sea.status = 'COMPLETED' THEN sea.time_spent_minutes ELSE 0 END) as total_time_spent
            FROM users u
            LEFT JOIN student_exam_attempts sea ON u.id = sea.student_id
            WHERE u.user_type = 'STUDENT'
            GROUP BY u.id
            ORDER BY u.created_at DESC
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> student = new HashMap<>();
                student.put("id", rs.getInt("id"));
                student.put("username", rs.getString("username"));
                student.put("email", rs.getString("email"));
                student.put("fullName", rs.getString("full_name"));
                student.put("isActive", rs.getBoolean("is_active"));
                student.put("createdAt", rs.getTimestamp("created_at"));
                student.put("lastLogin", rs.getTimestamp("last_login"));
                student.put("totalAttempts", rs.getInt("total_attempts"));
                student.put("completedExams", rs.getInt("completed_exams"));
                student.put("avgScore", rs.getDouble("avg_score"));
                student.put("passRate", rs.getDouble("pass_rate"));
                student.put("avgAccuracy", rs.getDouble("avg_accuracy"));
                student.put("totalTimeSpent", rs.getInt("total_time_spent"));

                students.add(student);
            }

            System.out.println("✅ Loaded " + students.size() + " students with statistics");

        } catch (SQLException e) {
            System.err.println("❌ Error fetching students with stats");
            e.printStackTrace();
        }

        return students;
    }

    /**
     * Get detailed exam history for a student
     */
    public List<Map<String, Object>> getStudentExamHistory(int studentId) {
        List<Map<String, Object>> history = new ArrayList<>();

        String sql = """
            SELECT 
                sea.attempt_id,
                sea.exam_id,
                e.exam_title,
                e.subject,
                e.difficulty,
                sea.obtained_marks,
                sea.total_marks,
                sea.passing_marks,
                sea.accuracy,
                sea.result,
                sea.time_spent_minutes,
                sea.started_at,
                sea.submitted_at,
                sea.status
            FROM student_exam_attempts sea
            JOIN exams e ON sea.exam_id = e.exam_id
            WHERE sea.student_id = ?
            ORDER BY sea.started_at DESC
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> attempt = new HashMap<>();
                attempt.put("attemptId", rs.getInt("attempt_id"));
                attempt.put("examId", rs.getInt("exam_id"));
                attempt.put("examTitle", rs.getString("exam_title"));
                attempt.put("subject", rs.getString("subject"));
                attempt.put("difficulty", rs.getString("difficulty"));
                attempt.put("obtainedMarks", rs.getInt("obtained_marks"));
                attempt.put("totalMarks", rs.getInt("total_marks"));
                attempt.put("passingMarks", rs.getInt("passing_marks"));
                attempt.put("accuracy", rs.getDouble("accuracy"));
                attempt.put("result", rs.getString("result"));
                attempt.put("timeSpent", rs.getInt("time_spent_minutes"));
                attempt.put("startedAt", rs.getTimestamp("started_at"));
                attempt.put("submittedAt", rs.getTimestamp("submitted_at"));
                attempt.put("status", rs.getString("status"));

                history.add(attempt);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error fetching student exam history");
            e.printStackTrace();
        }

        return history;
    }

    /**
     * Get student by ID
     */
    public User getStudentById(int studentId) {
        String sql = "SELECT * FROM users WHERE id = ? AND user_type = 'STUDENT'";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                User student = new User();
                student.setId(rs.getInt("id"));
                student.setUsername(rs.getString("username"));
                student.setEmail(rs.getString("email"));
                student.setFullName(rs.getString("full_name"));
                student.setUserType(rs.getString("user_type"));
                student.setActive(rs.getBoolean("is_active"));

                Timestamp createdAt = rs.getTimestamp("created_at");
                if (createdAt != null) {
                    student.setCreatedAt(createdAt.toLocalDateTime());
                }

                Timestamp lastLogin = rs.getTimestamp("last_login");
                if (lastLogin != null) {
                    student.setLastLogin(lastLogin.toLocalDateTime());
                }

                return student;
            }

        } catch (SQLException e) {
            System.err.println("❌ Error fetching student by ID");
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Toggle student account status (activate/deactivate)
     */
    public boolean toggleStudentStatus(int studentId) {
        String sql = "UPDATE users SET is_active = NOT is_active WHERE id = ? AND user_type = 'STUDENT'";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, studentId);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Student status toggled for ID: " + studentId);
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ Error toggling student status");
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Get total count of students
     */
    public int getTotalStudentsCount() {
        String sql = "SELECT COUNT(*) FROM users WHERE user_type = 'STUDENT'";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error counting students");
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Get active students count
     */
    public int getActiveStudentsCount() {
        String sql = "SELECT COUNT(*) FROM users WHERE user_type = 'STUDENT' AND is_active = TRUE";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error counting active students");
            e.printStackTrace();
        }

        return 0;
    }
}