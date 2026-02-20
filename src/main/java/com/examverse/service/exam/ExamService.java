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
 * ExamService — All exam-related database operations.
 *
 * Changes in this version:
 * 1. markAttemptCompleted() — new method called by ExamController after
 *    submission to GUARANTEE status='COMPLETED' is written to the DB.
 *    This is the fix for "completed exam stays in Ongoing tab".
 * 2. getAllActiveExams() — only shows exams that have ≥1 question.
 * 3. getExamById(id, adminView) — student view enforces status='ACTIVE'.
 * 4. startExamAttempt() — abandons old ONGOING rows before creating fresh.
 * 5. deleteAttempt() — cleans up orphaned rows when a question-less exam
 *    is caught before navigation.
 * 6. getStudentStats() — NULL-safe AVG handling.
 * 7. extractAttemptFromResultSet() — safe column reading with fallbacks.
 * 8. deleteExam() — cascading delete in a transaction.
 */
public class ExamService {

    // ══════════════════════════════════════════════════════════════════
    //  STUDENT — READ METHODS
    // ══════════════════════════════════════════════════════════════════

    /**
     * Get all ACTIVE exams that have at least one question.
     * Prevents students from seeing empty exams that would fail at runtime.
     */
    public List<Exam> getAllActiveExams() {
        List<Exam> exams = new ArrayList<>();
        String sql = """
            SELECT e.*
            FROM exams e
            WHERE e.status = 'ACTIVE'
              AND EXISTS (SELECT 1 FROM questions q WHERE q.exam_id = e.exam_id)
            ORDER BY e.created_at DESC
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) exams.add(extractExamFromResultSet(rs));
            System.out.println("✅ Fetched " + exams.size() + " active exams");

        } catch (SQLException e) {
            System.err.println("❌ getAllActiveExams: " + e.getMessage());
            e.printStackTrace();
        }
        return exams;
    }

    /**
     * Get exams by subject — students only (active + has questions).
     */
    public List<Exam> getExamsBySubject(String subject) {
        List<Exam> exams = new ArrayList<>();
        String sql = """
            SELECT e.*
            FROM exams e
            WHERE e.status = 'ACTIVE' AND e.subject = ?
              AND EXISTS (SELECT 1 FROM questions q WHERE q.exam_id = e.exam_id)
            ORDER BY e.created_at DESC
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, subject);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) exams.add(extractExamFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ getExamsBySubject: " + e.getMessage()); e.printStackTrace();
        }
        return exams;
    }

    /**
     * Get exams by difficulty — students only (active + has questions).
     */
    public List<Exam> getExamsByDifficulty(String difficulty) {
        List<Exam> exams = new ArrayList<>();
        String sql = """
            SELECT e.*
            FROM exams e
            WHERE e.status = 'ACTIVE' AND e.difficulty = ?
              AND EXISTS (SELECT 1 FROM questions q WHERE q.exam_id = e.exam_id)
            ORDER BY e.created_at DESC
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, difficulty);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) exams.add(extractExamFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ getExamsByDifficulty: " + e.getMessage()); e.printStackTrace();
        }
        return exams;
    }

    /**
     * Get exam by ID — student view (must be ACTIVE).
     */
    public Exam getExamById(int examId) {
        return getExamById(examId, false);
    }

    /**
     * Get exam by ID.
     * @param adminView true = any status (admin); false = ACTIVE only (student).
     */
    public Exam getExamById(int examId, boolean adminView) {
        String sql = adminView
                ? "SELECT * FROM exams WHERE exam_id = ?"
                : "SELECT * FROM exams WHERE exam_id = ? AND status = 'ACTIVE'";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, examId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return extractExamFromResultSet(rs);
            }
        } catch (SQLException e) {
            System.err.println("❌ getExamById(" + examId + "): " + e.getMessage()); e.printStackTrace();
        }
        return null;
    }

    /**
     * Get student's ONGOING attempts.
     */
    public List<StudentExamAttempt> getOngoingExams(int studentId) {
        List<StudentExamAttempt> list = new ArrayList<>();
        String sql = """
            SELECT sea.*, e.exam_title, e.subject, e.total_questions,
                   e.total_marks, e.passing_marks
            FROM student_exam_attempts sea
            JOIN exams e ON sea.exam_id = e.exam_id
            WHERE sea.student_id = ? AND sea.status = 'ONGOING'
            ORDER BY sea.started_at DESC
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(extractAttemptFromResultSet(rs));
            }
            System.out.println("✅ Ongoing exams for student " + studentId + ": " + list.size());
        } catch (SQLException e) {
            System.err.println("❌ getOngoingExams: " + e.getMessage()); e.printStackTrace();
        }
        return list;
    }

    /**
     * Get student's COMPLETED attempts.
     */
    public List<StudentExamAttempt> getCompletedExams(int studentId) {
        List<StudentExamAttempt> list = new ArrayList<>();
        String sql = """
            SELECT sea.*, e.exam_title, e.subject, e.total_questions, e.passing_marks
            FROM student_exam_attempts sea
            JOIN exams e ON sea.exam_id = e.exam_id
            WHERE sea.student_id = ? AND sea.status = 'COMPLETED'
            ORDER BY sea.submitted_at DESC
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(extractAttemptFromResultSet(rs));
            }
            System.out.println("✅ Completed exams for student " + studentId + ": " + list.size());
        } catch (SQLException e) {
            System.err.println("❌ getCompletedExams: " + e.getMessage()); e.printStackTrace();
        }
        return list;
    }

    /**
     * Student dashboard stats. NULL-safe AVG handling.
     */
    public StudentStats getStudentStats(int studentId) {
        StudentStats stats = new StudentStats();
        String sql = """
            SELECT
                COUNT(*) AS total_attempted,
                SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) AS total_completed,
                SUM(CASE WHEN result  = 'PASSED'   THEN 1 ELSE 0 END) AS total_passed,
                SUM(CASE WHEN result  = 'FAILED'   THEN 1 ELSE 0 END) AS total_failed,
                AVG(CASE WHEN status = 'COMPLETED'
                         THEN (obtained_marks * 100.0 / NULLIF(total_marks,0))
                         ELSE NULL END) AS avg_score,
                AVG(CASE WHEN status = 'COMPLETED' THEN accuracy ELSE NULL END) AS avg_accuracy,
                COALESCE(SUM(time_spent_minutes), 0) AS total_time
            FROM student_exam_attempts
            WHERE student_id = ?
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.setTotalExamsAttempted(rs.getInt("total_attempted"));
                    stats.setTotalExamsCompleted(rs.getInt("total_completed"));
                    stats.setTotalExamsPassed(rs.getInt("total_passed"));
                    stats.setTotalExamsFailed(rs.getInt("total_failed"));
                    double avgScore = rs.getDouble("avg_score");
                    stats.setAverageScore(rs.wasNull() ? 0.0 : avgScore);
                    double avgAcc = rs.getDouble("avg_accuracy");
                    stats.setOverallAccuracy(rs.wasNull() ? 0.0 : avgAcc);
                    stats.setTotalTimeSpentMinutes(rs.getInt("total_time"));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ getStudentStats: " + e.getMessage()); e.printStackTrace();
        }
        return stats;
    }

    // ══════════════════════════════════════════════════════════════════
    //  ATTEMPT LIFECYCLE
    // ══════════════════════════════════════════════════════════════════

    /**
     * Start a brand-new exam attempt.
     * Abandons any existing ONGOING attempts for this student+exam first so
     * the Ongoing tab doesn't fill up with stale rows.
     */
    public int startExamAttempt(int studentId, int examId) {
        Exam exam = getExamById(examId, false); // student view — ACTIVE only
        if (exam == null) {
            System.err.println("❌ startExamAttempt: exam not found or inactive — id=" + examId);
            return -1;
        }

        abandonOngoingAttempts(studentId, examId);

        String sql = """
            INSERT INTO student_exam_attempts
                (student_id, exam_id, status, total_marks, passing_marks, started_at)
            VALUES (?, ?, 'ONGOING', ?, ?, ?)
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, studentId);
            ps.setInt(2, examId);
            ps.setInt(3, exam.getTotalMarks());
            ps.setInt(4, exam.getPassingMarks());
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));

            if (ps.executeUpdate() > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        int id = keys.getInt(1);
                        System.out.println("✅ New attempt created: " + id
                                + " (student=" + studentId + " exam=" + examId + ")");
                        return id;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ startExamAttempt: " + e.getMessage()); e.printStackTrace();
        }
        return -1;
    }

    /**
     * Mark all ONGOING attempts for a student+exam as ABANDONED.
     * Private — called automatically by startExamAttempt().
     */
    private void abandonOngoingAttempts(int studentId, int examId) {
        String sql = """
            UPDATE student_exam_attempts
            SET status = 'ABANDONED', submitted_at = ?
            WHERE student_id = ? AND exam_id = ? AND status = 'ONGOING'
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(2, studentId);
            ps.setInt(3, examId);
            int rows = ps.executeUpdate();
            if (rows > 0)
                System.out.println("⚠ Abandoned " + rows + " stale attempt(s) — student="
                        + studentId + " exam=" + examId);
        } catch (SQLException e) {
            System.err.println("⚠ abandonOngoingAttempts (non-fatal): " + e.getMessage());
        }
    }

    /**
     * ═══════════════════════════════════════════════════════════════════
     * BUG FIX — markAttemptCompleted()
     * ═══════════════════════════════════════════════════════════════════
     * Called by ExamController immediately after EvaluationService runs.
     * Forces status='COMPLETED' and sets submitted_at so the attempt is
     * GUARANTEED to leave the Ongoing tab even if EvaluationService had
     * a bug or only partially updated the row.
     *
     * Why needed: getOngoingExams() filters status='ONGOING'. If status
     * never changes to 'COMPLETED', the attempt stays in Ongoing forever.
     */
    public void markAttemptCompleted(int attemptId) {
        String sql = """
            UPDATE student_exam_attempts
            SET status = 'COMPLETED',
                submitted_at = COALESCE(submitted_at, ?)
            WHERE attempt_id = ? AND status != 'COMPLETED'
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(2, attemptId);
            int rows = ps.executeUpdate();
            if (rows > 0)
                System.out.println("✅ markAttemptCompleted: attempt " + attemptId + " → COMPLETED");
        } catch (SQLException e) {
            System.err.println("❌ markAttemptCompleted: " + e.getMessage()); e.printStackTrace();
        }
    }

    /**
     * Delete an orphaned attempt (and its answers) in a transaction.
     * Used when question-less exam is caught before navigation.
     */
    public void deleteAttempt(int attemptId) {
        String delAnswers  = "DELETE FROM student_answers WHERE attempt_id = ?";
        String delAttempt  = "DELETE FROM student_exam_attempts WHERE attempt_id = ?";
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pa = conn.prepareStatement(delAnswers);
                 PreparedStatement pe = conn.prepareStatement(delAttempt)) {
                pa.setInt(1, attemptId); pa.executeUpdate();
                pe.setInt(1, attemptId); pe.executeUpdate();
                conn.commit();
                System.out.println("✅ Orphaned attempt deleted: " + attemptId);
            } catch (SQLException inner) {
                conn.rollback(); throw inner;
            } finally { conn.setAutoCommit(true); }
        } catch (SQLException e) {
            System.err.println("❌ deleteAttempt: " + e.getMessage()); e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  SUBJECTS
    // ══════════════════════════════════════════════════════════════════

    public List<String> getAllSubjects() {
        List<String> subjects = new ArrayList<>();
        String sql = """
            SELECT DISTINCT e.subject
            FROM exams e
            WHERE e.status = 'ACTIVE'
              AND EXISTS (SELECT 1 FROM questions q WHERE q.exam_id = e.exam_id)
            ORDER BY e.subject
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String s = rs.getString("subject");
                if (s != null && !s.isBlank()) subjects.add(s);
            }
        } catch (SQLException e) {
            System.err.println("❌ getAllSubjects: " + e.getMessage()); e.printStackTrace();
        }
        return subjects;
    }

    // ══════════════════════════════════════════════════════════════════
    //  ADMIN — CRUD
    // ══════════════════════════════════════════════════════════════════

    public List<Exam> getAllExams() {
        List<Exam> exams = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM exams ORDER BY created_at DESC")) {
            while (rs.next()) exams.add(extractExamFromResultSet(rs));
            System.out.println("✅ Admin: loaded " + exams.size() + " exams");
        } catch (SQLException e) {
            System.err.println("❌ getAllExams: " + e.getMessage()); e.printStackTrace();
        }
        return exams;
    }

    public List<Exam> getExamsByStatus(String status) {
        List<Exam> exams = new ArrayList<>();
        String sql = "SELECT * FROM exams WHERE status = ? ORDER BY created_at DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) exams.add(extractExamFromResultSet(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return exams;
    }

    public boolean createExam(Exam exam) {
        String sql = """
            INSERT INTO exams (exam_title, subject, description, difficulty,
                               total_questions, total_marks, duration_minutes,
                               passing_marks, status, created_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, exam.getExamTitle());
            ps.setString(2, exam.getSubject());
            ps.setString(3, exam.getDescription());
            ps.setString(4, exam.getDifficulty());
            ps.setInt(5, exam.getTotalQuestions());
            ps.setInt(6, exam.getTotalMarks());
            ps.setInt(7, exam.getDurationMinutes());
            ps.setInt(8, exam.getPassingMarks());
            ps.setString(9, exam.getStatus() != null ? exam.getStatus() : "ACTIVE");
            ps.setInt(10, exam.getCreatedBy());

            if (ps.executeUpdate() > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) exam.setExamId(keys.getInt(1));
                }
                System.out.println("✅ Exam created: " + exam.getExamTitle());
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ createExam: " + e.getMessage()); e.printStackTrace();
        }
        return false;
    }

    public boolean updateExam(Exam exam) {
        String sql = """
            UPDATE exams SET
                exam_title=?, subject=?, description=?, difficulty=?,
                total_questions=?, total_marks=?, duration_minutes=?,
                passing_marks=?, status=?
            WHERE exam_id=?
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, exam.getExamTitle());
            ps.setString(2, exam.getSubject());
            ps.setString(3, exam.getDescription());
            ps.setString(4, exam.getDifficulty());
            ps.setInt(5, exam.getTotalQuestions());
            ps.setInt(6, exam.getTotalMarks());
            ps.setInt(7, exam.getDurationMinutes());
            ps.setInt(8, exam.getPassingMarks());
            ps.setString(9, exam.getStatus());
            ps.setInt(10, exam.getExamId());
            if (ps.executeUpdate() > 0) {
                System.out.println("✅ Exam updated: " + exam.getExamTitle());
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ updateExam: " + e.getMessage()); e.printStackTrace();
        }
        return false;
    }

    /**
     * Cascading delete in a transaction: answers → attempts → questions → exam.
     */
    public boolean deleteExam(int examId) {
        String[] sqls = {
                "DELETE FROM student_answers WHERE attempt_id IN (SELECT attempt_id FROM student_exam_attempts WHERE exam_id=?)",
                "DELETE FROM student_exam_attempts WHERE exam_id=?",
                "DELETE FROM questions WHERE exam_id=?",
                "DELETE FROM exams WHERE exam_id=?"
        };
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (String sql : sqls) {
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, examId); ps.executeUpdate();
                    }
                }
                conn.commit();
                System.out.println("✅ Exam deleted (cascade): " + examId);
                return true;
            } catch (SQLException inner) {
                conn.rollback(); throw inner;
            } finally { conn.setAutoCommit(true); }
        } catch (SQLException e) {
            System.err.println("❌ deleteExam: " + e.getMessage()); e.printStackTrace();
        }
        return false;
    }

    public int getTotalExamsCount() {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM exams WHERE status='ACTIVE'")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public int getTotalAttemptsCount() {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM student_exam_attempts")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    // ══════════════════════════════════════════════════════════════════
    //  MAPPING HELPERS
    // ══════════════════════════════════════════════════════════════════

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
        exam.setCreatedBy(rs.getInt("created_by"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) exam.setCreatedAt(ts.toLocalDateTime());
        return exam;
    }

    /**
     * Maps ResultSet → StudentExamAttempt with safe reading for optional columns.
     * Some queries only SELECT a subset of columns (e.g. getOngoingExams vs
     * getCompletedExams) — safeString/safeInt prevent column-not-found exceptions.
     */
    private StudentExamAttempt extractAttemptFromResultSet(ResultSet rs) throws SQLException {
        StudentExamAttempt a = new StudentExamAttempt();
        a.setAttemptId(rs.getInt("attempt_id"));
        a.setStudentId(rs.getInt("student_id"));
        a.setExamId(rs.getInt("exam_id"));
        a.setStatus(rs.getString("status"));

        a.setExamTitle(safeString(rs, "exam_title", "Unknown"));
        a.setSubject(safeString(rs, "subject", null));
        a.setTotalQuestions(safeInt(rs, "total_questions", 0));
        a.setTotalMarks(safeInt(rs, "total_marks", 0));
        a.setPassingMarks(safeInt(rs, "passing_marks", 0));

        a.setObtainedMarks(rs.getInt("obtained_marks"));
        a.setTimeSpentMinutes(rs.getInt("time_spent_minutes"));

        double acc = rs.getDouble("accuracy");
        a.setAccuracy(rs.wasNull() ? 0.0 : acc);

        a.setResult(safeString(rs, "result", null));

        Timestamp started = rs.getTimestamp("started_at");
        if (started != null) a.setStartedAt(started.toLocalDateTime());
        Timestamp submitted = rs.getTimestamp("submitted_at");
        if (submitted != null) a.setSubmittedAt(submitted.toLocalDateTime());

        return a;
    }

    private String safeString(ResultSet rs, String col, String def) {
        try { String v = rs.getString(col); return v != null ? v : def; }
        catch (SQLException e) { return def; }
    }

    private int safeInt(ResultSet rs, String col, int def) {
        try { int v = rs.getInt(col); return rs.wasNull() ? def : v; }
        catch (SQLException e) { return def; }
    }
}