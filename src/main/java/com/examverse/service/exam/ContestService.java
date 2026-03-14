package com.examverse.service.exam;

import com.examverse.config.DatabaseConfig;
import com.examverse.model.exam.*;
import com.examverse.model.exam.Contest.Status;
import com.examverse.model.exam.Contest.Theme;
import com.examverse.model.exam.ContestQuestion.QuestionType;
import com.examverse.model.exam.ContestAnswer.ReviewStatus;
import com.examverse.model.exam.ContestParticipant.ParticipantStatus;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * ContestService - Full CRUD + business logic for the live contest system.
 *
 * Fixes in this version:
 *  1. startContestAutoLauncher(): background timer auto-sets UPCOMING → LIVE
 *     when start_time passes, and auto-sets LIVE → EVALUATION when end_time passes.
 *     Call once at app startup.
 *
 *  2. getContestLeaderboard(contestId): returns ONLY students who actually
 *     participated in that specific contest. Never shows non-participants.
 *
 *  3. getGlobalLeaderboard(): now filters WHERE user_type = 'STUDENT' so
 *     admins/teachers never appear on the leaderboard.
 *
 *  4. getQuestionCountByType(): used to enforce question limits when admin
 *     adds questions.
 *
 *  5. checkAndFinalizeContest(): guard added — only runs when status = EVALUATION,
 *     prevents premature finalization while contest is still LIVE.
 *
 *  6. registerStudent(): single-connection fix — INSERT IGNORE + fallback SELECT
 *     on the same connection so participant_id is always returned correctly.
 */
public class ContestService {

    // ── Auto-Launcher (call once at app startup) ──────────────────────────────

    private static Timer autoLaunchTimer;

    /**
     * Starts a background daemon timer that:
     *  - Auto-launches UPCOMING contests whose start_time has passed → LIVE
     *  - Auto-ends LIVE contests whose end_time has passed → EVALUATION
     * Call this ONCE from your main Application class or DatabaseConfig init.
     */
    public static void startContestAutoLauncher() {
        if (autoLaunchTimer != null) {
            autoLaunchTimer.cancel();
        }
        autoLaunchTimer = new Timer("contest-auto-launcher", true); // daemon thread
        autoLaunchTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                autoLaunchDueContests();
                autoEndDueContests();
            }
        }, 0, 15_000); // check every 15 seconds
        System.out.println("✅ Contest auto-launcher started (checks every 15s).");
    }

    /** UPCOMING → LIVE when start_time <= NOW */
    private static void autoLaunchDueContests() {
        String sql = "UPDATE contests SET status='LIVE' " +
                "WHERE status='UPCOMING' AND start_time <= NOW()";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int updated = ps.executeUpdate();
            if (updated > 0) {
                System.out.println("🚀 Auto-launched " + updated + " contest(s) to LIVE.");
            }
        } catch (SQLException e) {
            System.err.println("❌ autoLaunchDueContests: " + e.getMessage());
        }
    }

    /** LIVE → EVALUATION when end_time <= NOW */
    private static void autoEndDueContests() {
        String sql = "UPDATE contests SET status='EVALUATION' " +
                "WHERE status='LIVE' AND end_time <= NOW()";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int updated = ps.executeUpdate();
            if (updated > 0) {
                System.out.println("⏹ Auto-ended " + updated + " contest(s) to EVALUATION.");
            }
        } catch (SQLException e) {
            System.err.println("❌ autoEndDueContests: " + e.getMessage());
        }
    }

    // ─── Contest CRUD ─────────────────────────────────────────────────────────

    /** Create a new contest. Returns the generated contest_id or -1 on failure. */
    public int createContest(Contest c) {
        String sql = """
            INSERT INTO contests
              (contest_title, description, theme, status, created_by,
               start_time, end_time, eval_deadline, duration_minutes,
               total_mcq, total_written, mcq_marks_each, written_marks_each,
               total_marks, max_gain, max_loss)
            VALUES (?,?,?,?,?, ?,?,?,?, ?,?,?,?, ?,?,?)
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, c.getContestTitle());
            ps.setString(2, c.getDescription());
            ps.setString(3, c.getTheme().name());
            ps.setString(4, c.getStatus().name());
            ps.setInt(5, c.getCreatedBy());
            ps.setTimestamp(6, Timestamp.valueOf(c.getStartTime()));
            ps.setTimestamp(7, Timestamp.valueOf(c.getEndTime()));
            ps.setTimestamp(8, Timestamp.valueOf(c.getEvalDeadline()));
            ps.setInt(9, c.getDurationMinutes());
            ps.setInt(10, c.getTotalMcqQuestions());
            ps.setInt(11, c.getTotalWrittenQuestions());
            ps.setInt(12, c.getMcqMarksEach());
            ps.setInt(13, c.getWrittenMarksEach());
            ps.setInt(14, c.computeTotalMarks());
            ps.setInt(15, c.getMaxGain());
            ps.setInt(16, c.getMaxLoss());

            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int id = keys.getInt(1);
                System.out.println("✅ Contest created: id=" + id);
                return id;
            }
        } catch (SQLException e) {
            System.err.println("❌ createContest failed: " + e.getMessage());
        }
        return -1;
    }

    /** Update contest status. */
    public boolean updateContestStatus(int contestId, Status status) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE contests SET status=? WHERE contest_id=?")) {
            ps.setString(1, status.name());
            ps.setInt(2, contestId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ updateContestStatus: " + e.getMessage());
            return false;
        }
    }

    /** Fetch all contests (admin view). */
    public List<Contest> getAllContests() {
        List<Contest> list = new ArrayList<>();
        String sql = "SELECT * FROM contests ORDER BY start_time DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapContest(rs));
        } catch (SQLException e) {
            System.err.println("❌ getAllContests: " + e.getMessage());
        }
        return list;
    }

    /** Fetch only UPCOMING and LIVE contests (student lobby). */
    public List<Contest> getActiveContests() {
        List<Contest> list = new ArrayList<>();
        String sql = "SELECT * FROM contests WHERE status IN ('UPCOMING','LIVE') ORDER BY start_time ASC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapContest(rs));
        } catch (SQLException e) {
            System.err.println("❌ getActiveContests: " + e.getMessage());
        }
        return list;
    }

    public List<Contest> getPendingEvaluationContests() {
        List<Contest> list = new ArrayList<>();
        String sql = "SELECT * FROM contests WHERE status = 'EVALUATION' ORDER BY end_time DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapContest(rs));
        } catch (SQLException e) {
            System.err.println("❌ getPendingEvaluationContests: " + e.getMessage());
        }
        return list;
    }

    /**
     * Fetch only FINISHED and CANCELLED contests.
     * Used by: student "Past Contests" tab, admin "Finished" section.
     */
    public List<Contest> getFinishedContests() {
        List<Contest> list = new ArrayList<>();
        String sql = "SELECT * FROM contests WHERE status IN ('FINISHED','CANCELLED') " +
                "ORDER BY end_time DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapContest(rs));
        } catch (SQLException e) {
            System.err.println("❌ getFinishedContests: " + e.getMessage());
        }
        return list;
    }

    /**
     * Generic: fetch contests filtered by a single status.
     * Used by: admin sectioned view (LIVE, UPCOMING sections).
     */
    public List<Contest> getContestsByStatus(Status status) {
        List<Contest> list = new ArrayList<>();
        String sql = "SELECT * FROM contests WHERE status=? ORDER BY start_time DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapContest(rs));
        } catch (SQLException e) {
            System.err.println("❌ getContestsByStatus(" + status + "): " + e.getMessage());
        }
        return list;
    }

    /**
     * Returns the participant row for a student in a given finished contest,
     * or null if they never participated.
     * Used by: student "Past Contests" card — shows their rank, score and
     * rating change as a participation badge.
     */
    public ContestParticipant getParticipantForStudent(int contestId, int studentId) {
        String sql = "SELECT cp.*, u.full_name, u.username " +
                "FROM contest_participants cp " +
                "JOIN users u ON cp.student_id = u.id " +
                "WHERE cp.contest_id=? AND cp.student_id=?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, contestId);
            ps.setInt(2, studentId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapParticipant(rs);
        } catch (SQLException e) {
            System.err.println("❌ getParticipantForStudent: " + e.getMessage());
        }
        return null;
    }


    /** Fetch a single contest by id. */
    public Contest getContestById(int contestId) {
        String sql = "SELECT * FROM contests WHERE contest_id=?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, contestId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapContest(rs);
        } catch (SQLException e) {
            System.err.println("❌ getContestById: " + e.getMessage());
        }
        return null;
    }

    // ─── Contest Questions CRUD ───────────────────────────────────────────────

    /** Add a question to a contest. Returns generated question_id or -1. */
    public int addQuestion(ContestQuestion q) {
        String sql = """
            INSERT INTO contest_questions
              (contest_id, question_type, question_text, marks, order_index,
               option_a, option_b, option_c, option_d, correct_answer, explanation)
            VALUES (?,?,?,?,?, ?,?,?,?,?,?)
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, q.getContestId());
            ps.setString(2, q.getType().name());
            ps.setString(3, q.getQuestionText());
            ps.setInt(4, q.getMarks());
            ps.setInt(5, q.getOrderIndex());
            ps.setString(6, q.getOptionA());
            ps.setString(7, q.getOptionB());
            ps.setString(8, q.getOptionC());
            ps.setString(9, q.getOptionD());
            ps.setString(10, q.getCorrectAnswer());
            ps.setString(11, q.getExplanation());

            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        } catch (SQLException e) {
            System.err.println("❌ addQuestion: " + e.getMessage());
        }
        return -1;
    }

    /** Delete a question. */
    public boolean deleteQuestion(int questionId) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM contest_questions WHERE question_id=?")) {
            ps.setInt(1, questionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ deleteQuestion: " + e.getMessage());
            return false;
        }
    }

    /** Get all questions for a contest, ordered. */
    public List<ContestQuestion> getQuestionsForContest(int contestId) {
        List<ContestQuestion> list = new ArrayList<>();
        String sql = "SELECT * FROM contest_questions WHERE contest_id=? ORDER BY order_index, question_id";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, contestId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapQuestion(rs));
        } catch (SQLException e) {
            System.err.println("❌ getQuestionsForContest: " + e.getMessage());
        }
        return list;
    }

    /**
     * Returns how many questions of a specific type already exist for a contest.
     * Used by ContestManagerController to enforce the MCQ/written limits set
     * when the contest was created.
     */
    public int getQuestionCountByType(int contestId, QuestionType type) {
        String sql = "SELECT COUNT(*) FROM contest_questions WHERE contest_id=? AND question_type=?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, contestId);
            ps.setString(2, type.name());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("❌ getQuestionCountByType: " + e.getMessage());
        }
        return 0;
    }

    // ─── Participation ────────────────────────────────────────────────────────

    /**
     * FIX: single-connection INSERT + fallback SELECT.
     * INSERT IGNORE returns 0 rows affected if row already exists.
     * We use the same connection for the fallback SELECT so it always works.
     */
    public int registerStudent(int contestId, int studentId) {
        int currentRating = getStudentRating(studentId);

        String insertSql = """
            INSERT IGNORE INTO contest_participants
              (contest_id, student_id, status, rating_before, rating_after)
            VALUES (?, ?, 'REGISTERED', ?, ?)
            """;
        String selectSql =
                "SELECT participant_id FROM contest_participants WHERE contest_id=? AND student_id=?";

        try (Connection conn = DatabaseConfig.getConnection()) {
            // Try to insert
            int generatedId = -1;
            try (PreparedStatement ps = conn.prepareStatement(
                    insertSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, contestId);
                ps.setInt(2, studentId);
                ps.setInt(3, currentRating);
                ps.setInt(4, currentRating);
                int affected = ps.executeUpdate();
                if (affected > 0) {
                    ResultSet keys = ps.getGeneratedKeys();
                    if (keys.next()) generatedId = keys.getInt(1);
                }
            }
            if (generatedId > 0) return generatedId;

            // Already registered — fetch existing id on same connection
            try (PreparedStatement ps2 = conn.prepareStatement(selectSql)) {
                ps2.setInt(1, contestId);
                ps2.setInt(2, studentId);
                ResultSet rs = ps2.executeQuery();
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ registerStudent: " + e.getMessage());
        }
        return -1;
    }

    public boolean activateParticipant(int participantId) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE contest_participants SET status='ACTIVE' WHERE participant_id=?")) {
            ps.setInt(1, participantId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ activateParticipant: " + e.getMessage());
            return false;
        }
    }

    public boolean submitContest(int participantId) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE contest_participants SET status='SUBMITTED', submitted_at=NOW() WHERE participant_id=?")) {
            ps.setInt(1, participantId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ submitContest: " + e.getMessage());
            return false;
        }
    }

    public int getParticipantId(int contestId, int studentId) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT participant_id FROM contest_participants WHERE contest_id=? AND student_id=?")) {
            ps.setInt(1, contestId);
            ps.setInt(2, studentId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("❌ getParticipantId: " + e.getMessage());
        }
        return -1;
    }

    public boolean hasStudentSubmitted(int contestId, int studentId) {
        String sql = """
            SELECT status FROM contest_participants
            WHERE contest_id = ? AND student_id = ?
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, contestId);
            ps.setInt(2, studentId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String status = rs.getString("status");
                // SUBMITTED or EVALUATED both mean the student is done
                return "SUBMITTED".equals(status) || "EVALUATED".equals(status);
            }
        } catch (SQLException e) {
            System.err.println("❌ hasStudentSubmitted: " + e.getMessage());
        }
        return false; // not registered = not submitted
    }

    // ─── Answer Submission ────────────────────────────────────────────────────

    public ContestAnswer submitMcqAnswer(int participantId, int contestId,
                                         int questionId, int studentId, String selectedOption) {
        ContestQuestion question = getQuestionById(questionId);
        if (question == null) return null;

        boolean correct = selectedOption != null
                && selectedOption.equalsIgnoreCase(question.getCorrectAnswer());
        int marks = correct ? question.getMarks() : 0;

        String sql = """
            INSERT INTO contest_answers
              (participant_id, contest_id, question_id, student_id,
               selected_option, is_correct, review_status, marks_awarded)
            VALUES (?,?,?,?,?,?,?,?)
            ON DUPLICATE KEY UPDATE
              selected_option=VALUES(selected_option),
              is_correct=VALUES(is_correct),
              marks_awarded=VALUES(marks_awarded)
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, participantId);
            ps.setInt(2, contestId);
            ps.setInt(3, questionId);
            ps.setInt(4, studentId);
            ps.setString(5, selectedOption);
            ps.setBoolean(6, correct);
            ps.setString(7, ReviewStatus.REVIEWED.name());
            ps.setInt(8, marks);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ submitMcqAnswer: " + e.getMessage());
            return null;
        }

        recalculateMcqScore(participantId);
        refreshLiveRanks(contestId);

        ContestAnswer answer = new ContestAnswer();
        answer.setParticipantId(participantId);
        answer.setContestId(contestId);
        answer.setQuestionId(questionId);
        answer.setStudentId(studentId);
        answer.setSelectedOption(selectedOption);
        answer.setCorrect(correct);
        answer.setMarksAwarded(marks);
        answer.setReviewStatus(ReviewStatus.REVIEWED);
        return answer;
    }

    public boolean submitWrittenAnswer(int participantId, int contestId,
                                       int questionId, int studentId, String imagePath) {
        String sql = """
            INSERT INTO contest_answers
              (participant_id, contest_id, question_id, student_id,
               image_path, review_status, marks_awarded)
            VALUES (?,?,?,?,?,'PENDING',0)
            ON DUPLICATE KEY UPDATE image_path=VALUES(image_path), review_status='PENDING'
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, participantId);
            ps.setInt(2, contestId);
            ps.setInt(3, questionId);
            ps.setInt(4, studentId);
            ps.setString(5, imagePath);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ submitWrittenAnswer: " + e.getMessage());
            return false;
        }
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE contest_participants " +
                             "SET pending_written_reviews = pending_written_reviews + 1 " +
                             "WHERE participant_id=?")) {
            ps.setInt(1, participantId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ incrementPendingWritten: " + e.getMessage());
        }
        return true;
    }

    public boolean reviewWrittenAnswer(int answerId, int teacherId,
                                       int marksAwarded, String comment) {
        String fetchSql = """
            SELECT ca.participant_id, ca.contest_id, cq.marks
            FROM contest_answers ca
            JOIN contest_questions cq ON ca.question_id = cq.question_id
            WHERE ca.answer_id=?
            """;
        int participantId = -1, contestId = -1, maxMarks = 0;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(fetchSql)) {
            ps.setInt(1, answerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                participantId = rs.getInt("participant_id");
                contestId     = rs.getInt("contest_id");
                maxMarks      = rs.getInt("marks");
            }
        } catch (SQLException e) {
            System.err.println("❌ reviewWrittenAnswer fetch: " + e.getMessage());
            return false;
        }

        marksAwarded = Math.max(0, Math.min(marksAwarded, maxMarks));

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                     UPDATE contest_answers
                     SET marks_awarded=?, teacher_comment=?, review_status='REVIEWED',
                         reviewed_by=?, reviewed_at=NOW()
                     WHERE answer_id=?
                     """)) {
            ps.setInt(1, marksAwarded);
            ps.setString(2, comment);
            ps.setInt(3, teacherId);
            ps.setInt(4, answerId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ reviewWrittenAnswer update: " + e.getMessage());
            return false;
        }

        if (participantId != -1) {
            recalculateWrittenScore(participantId);
            decrementPendingWritten(participantId);
            recalculateTotalScore(participantId);
        }
        if (contestId != -1) checkAndFinalizeContest(contestId);
        return true;
    }

    // ─── Leaderboard ──────────────────────────────────────────────────────────

    public List<ContestParticipant> getLiveLeaderboard(int contestId) {
        List<ContestParticipant> list = new ArrayList<>();
        String sql = """
            SELECT cp.*, u.full_name, u.username
            FROM contest_participants cp
            JOIN users u ON cp.student_id = u.id
            WHERE cp.contest_id=?
            ORDER BY cp.mcq_marks_obtained DESC, cp.submitted_at ASC
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, contestId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapParticipant(rs));
        } catch (SQLException e) {
            System.err.println("❌ getLiveLeaderboard: " + e.getMessage());
        }
        return list;
    }

    public List<ContestParticipant> getFinalStandings(int contestId) {
        List<ContestParticipant> list = new ArrayList<>();
        String sql = """
            SELECT cp.*, u.full_name, u.username
            FROM contest_participants cp
            JOIN users u ON cp.student_id = u.id
            WHERE cp.contest_id=?
            ORDER BY cp.total_marks_obtained DESC, cp.submitted_at ASC
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, contestId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapParticipant(rs));
        } catch (SQLException e) {
            System.err.println("❌ getFinalStandings: " + e.getMessage());
        }
        return list;
    }

    public List<ContestAnswer> getStudentAnswers(int participantId) {
        List<ContestAnswer> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM contest_answers WHERE participant_id=? ORDER BY question_id")) {
            ps.setInt(1, participantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapAnswer(rs));
        } catch (SQLException e) {
            System.err.println("❌ getStudentAnswers: " + e.getMessage());
        }
        return list;
    }

    public List<ContestAnswer> getPendingWrittenAnswers(int contestId) {
        List<ContestAnswer> list = new ArrayList<>();
        String sql = """
            SELECT ca.*
            FROM contest_answers ca
            JOIN contest_questions cq ON ca.question_id = cq.question_id
            WHERE ca.contest_id=? AND cq.question_type='WRITTEN' AND ca.review_status='PENDING'
            ORDER BY ca.answered_at ASC
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, contestId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapAnswer(rs));
        } catch (SQLException e) {
            System.err.println("❌ getPendingWrittenAnswers: " + e.getMessage());
        }
        return list;
    }

    public int getTotalWrittenAnswerCount(int contestId) {
        String sql = """
            SELECT COUNT(*) FROM contest_answers ca
            JOIN contest_questions cq ON ca.question_id = cq.question_id
            WHERE ca.contest_id=? AND cq.question_type='WRITTEN'
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, contestId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("❌ getTotalWrittenAnswerCount: " + e.getMessage());
        }
        return 0;
    }

    // ─── Global Leaderboard (rating page) ────────────────────────────────────

    /**
     * FIX: now filters WHERE u.user_type = 'STUDENT' — admins and teachers
     * never appear in the global rating leaderboard.
     */
    public List<com.examverse.model.user.StudentRating> getGlobalLeaderboard(int limit) {
        List<com.examverse.model.user.StudentRating> list = new ArrayList<>();
        String sql = """
            SELECT sr.*, u.full_name, u.username
            FROM student_ratings sr
            JOIN users u ON sr.student_id = u.id
            WHERE u.user_type = 'STUDENT'
            ORDER BY sr.current_rating DESC
            LIMIT ?
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                com.examverse.model.user.StudentRating r = new com.examverse.model.user.StudentRating();
                r.setRatingId(rs.getInt("rating_id"));
                r.setStudentId(rs.getInt("student_id"));
                r.setStudentName(rs.getString("full_name"));
                r.setUsername(rs.getString("username"));
                r.setCurrentRating(rs.getInt("current_rating"));
                r.setPeakRating(rs.getInt("peak_rating"));
                r.setContestsParticipated(rs.getInt("contests_participated"));
                r.setContestsWon(rs.getInt("contests_won"));
                r.setTotalScore(rs.getInt("total_score"));
                list.add(r);
            }
        } catch (SQLException e) {
            System.err.println("❌ getGlobalLeaderboard: " + e.getMessage());
        }
        return list;
    }

    /**
     * NEW — Contest-specific leaderboard.
     * Returns ONLY students who have a row in contest_participants for this
     * contestId, joined with their global rating data.
     * Used by LeaderboardController when navigated from admin contest manager.
     */
    public List<com.examverse.model.user.StudentRating> getContestLeaderboard(int contestId) {
        List<com.examverse.model.user.StudentRating> list = new ArrayList<>();
        String sql = """
            SELECT
                COALESCE(sr.rating_id, 0)              AS rating_id,
                cp.student_id,
                u.full_name,
                u.username,
                COALESCE(sr.current_rating, 800)        AS current_rating,
                COALESCE(sr.peak_rating, 800)           AS peak_rating,
                COALESCE(sr.contests_participated, 0)   AS contests_participated,
                COALESCE(sr.contests_won, 0)            AS contests_won,
                COALESCE(sr.total_score, 0)             AS total_score
            FROM contest_participants cp
            JOIN users u ON cp.student_id = u.id
            LEFT JOIN student_ratings sr ON sr.student_id = cp.student_id
            WHERE cp.contest_id = ?
            ORDER BY COALESCE(sr.current_rating, 800) DESC
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, contestId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                com.examverse.model.user.StudentRating r = new com.examverse.model.user.StudentRating();
                r.setRatingId(rs.getInt("rating_id"));
                r.setStudentId(rs.getInt("student_id"));
                r.setStudentName(rs.getString("full_name"));
                r.setUsername(rs.getString("username"));
                r.setCurrentRating(rs.getInt("current_rating"));
                r.setPeakRating(rs.getInt("peak_rating"));
                r.setContestsParticipated(rs.getInt("contests_participated"));
                r.setContestsWon(rs.getInt("contests_won"));
                r.setTotalScore(rs.getInt("total_score"));
                list.add(r);
            }
        } catch (SQLException e) {
            System.err.println("❌ getContestLeaderboard: " + e.getMessage());
        }
        return list;
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private void recalculateMcqScore(int participantId) {
        String sql = """
            UPDATE contest_participants cp
            SET mcq_marks_obtained = (
                SELECT COALESCE(SUM(ca.marks_awarded), 0)
                FROM contest_answers ca
                JOIN contest_questions cq ON ca.question_id = cq.question_id
                WHERE ca.participant_id = ? AND cq.question_type = 'MCQ'
            )
            WHERE cp.participant_id = ?
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, participantId);
            ps.setInt(2, participantId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ recalculateMcqScore: " + e.getMessage());
        }
    }

    private void recalculateWrittenScore(int participantId) {
        String sql = """
            UPDATE contest_participants cp
            SET written_marks_obtained = (
                SELECT COALESCE(SUM(ca.marks_awarded), 0)
                FROM contest_answers ca
                JOIN contest_questions cq ON ca.question_id = cq.question_id
                WHERE ca.participant_id = ? AND cq.question_type = 'WRITTEN'
            )
            WHERE cp.participant_id = ?
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, participantId);
            ps.setInt(2, participantId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ recalculateWrittenScore: " + e.getMessage());
        }
    }

    private void recalculateTotalScore(int participantId) {
        String sql = """
            UPDATE contest_participants
            SET total_marks_obtained = mcq_marks_obtained + written_marks_obtained
            WHERE participant_id = ?
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, participantId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ recalculateTotalScore: " + e.getMessage());
        }
    }

    private void decrementPendingWritten(int participantId) {
        String sql = """
            UPDATE contest_participants
            SET pending_written_reviews = GREATEST(0, pending_written_reviews - 1)
            WHERE participant_id = ?
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, participantId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ decrementPendingWritten: " + e.getMessage());
        }
    }

    private void refreshLiveRanks(int contestId) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps1 = conn.prepareStatement("SET @r=0");
             PreparedStatement ps2 = conn.prepareStatement(
                     "UPDATE contest_participants SET live_rank=(@r:=@r+1) " +
                             "WHERE contest_id=? ORDER BY mcq_marks_obtained DESC, submitted_at ASC")) {
            ps1.execute();
            ps2.setInt(1, contestId);
            ps2.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ refreshLiveRanks: " + e.getMessage());
        }
    }

    /**
     * FIX: guard added — only finalizes when status = EVALUATION.
     * Previously this could fire while contest was still LIVE if a student
     * happened to have no written answers, causing premature finalization.
     */
    private void checkAndFinalizeContest(int contestId) {
        Contest contest = getContestById(contestId);
        // Only finalize if we are actually in evaluation phase
        if (contest == null || contest.getStatus() != Status.EVALUATION) return;

        String checkSql = """
            SELECT COUNT(*) FROM contest_answers ca
            JOIN contest_questions cq ON ca.question_id = cq.question_id
            WHERE ca.contest_id=? AND cq.question_type='WRITTEN' AND ca.review_status='PENDING'
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setInt(1, contestId);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) == 0) {
                int totalWritten = getTotalWrittenAnswerCount(contestId);
                // Only auto-finalize if there ARE written answers reviewed,
                // or if there were no written questions at all
                if (totalWritten > 0 || contest.getTotalWrittenQuestions() == 0) {
                    assignFinalRanks(contestId);
                    distributeRatingChanges(contestId);
                    updateContestStatus(contestId, Status.FINISHED);
                    System.out.println("🏆 Contest " + contestId + " finalized automatically!");
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ checkAndFinalizeContest: " + e.getMessage());
        }
    }

    private void assignFinalRanks(int contestId) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps1 = conn.prepareStatement("SET @fr=0");
             PreparedStatement ps2 = conn.prepareStatement(
                     "UPDATE contest_participants SET final_rank=(@fr:=@fr+1) " +
                             "WHERE contest_id=? AND status IN ('SUBMITTED','EVALUATED','ACTIVE') " +
                             "ORDER BY total_marks_obtained DESC, submitted_at ASC")) {
            ps1.execute();
            ps2.setInt(1, contestId);
            ps2.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ assignFinalRanks: " + e.getMessage());
        }
    }

    public void distributeRatingChanges(int contestId) {
        Contest contest = getContestById(contestId);
        if (contest == null) return;

        List<ContestParticipant> standings = getFinalStandings(contestId);
        int n = standings.size();
        if (n == 0) return;

        int maxGain = contest.getMaxGain();
        int maxLoss = contest.getMaxLoss();

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (int i = 0; i < n; i++) {
                    ContestParticipant p = standings.get(i);
                    int rank = i + 1;
                    double percentile = (n == 1) ? 1.0 : (double)(n - rank) / (n - 1);
                    int change = (int) Math.round(maxGain * percentile - maxLoss * (1 - percentile));
                    if (rank == 1 && change < 1) change = 1;
                    if (rank == n && change > -1) change = -1;
                    change = Math.max(change, -maxLoss);
                    change = Math.min(change, maxGain);
                    int ratingAfter = Math.max(0, p.getRatingBefore() + change);

                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE contest_participants " +
                                    "SET rating_after=?, rating_change=?, status='EVALUATED', final_rank=? " +
                                    "WHERE participant_id=?")) {
                        ps.setInt(1, ratingAfter);
                        ps.setInt(2, change);
                        ps.setInt(3, rank);
                        ps.setInt(4, p.getParticipantId());
                        ps.executeUpdate();
                    }

                    try (PreparedStatement ps = conn.prepareStatement("""
                            INSERT INTO student_ratings
                              (student_id, current_rating, peak_rating,
                               contests_participated, contests_won, total_score)
                            VALUES (?,?,?,1,?,?)
                            ON DUPLICATE KEY UPDATE
                              current_rating        = VALUES(current_rating),
                              peak_rating           = GREATEST(peak_rating, VALUES(current_rating)),
                              contests_participated = contests_participated + 1,
                              contests_won          = contests_won + VALUES(contests_won),
                              total_score           = total_score + VALUES(total_score)
                            """)) {
                        ps.setInt(1, p.getStudentId());
                        ps.setInt(2, ratingAfter);
                        ps.setInt(3, ratingAfter);
                        ps.setInt(4, rank == 1 ? 1 : 0);
                        ps.setInt(5, p.getTotalMarksObtained());
                        ps.executeUpdate();
                    }

                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO contest_rating_history " +
                                    "(student_id,contest_id,rating_before,rating_after,rating_change,final_rank,total_score) " +
                                    "VALUES (?,?,?,?,?,?,?)")) {
                        ps.setInt(1, p.getStudentId());
                        ps.setInt(2, contestId);
                        ps.setInt(3, p.getRatingBefore());
                        ps.setInt(4, ratingAfter);
                        ps.setInt(5, change);
                        ps.setInt(6, rank);
                        ps.setInt(7, p.getTotalMarksObtained());
                        ps.executeUpdate();
                    }
                }
                conn.commit();
                System.out.println("✅ Ratings distributed for contest " + contestId);
            } catch (SQLException ex) {
                conn.rollback();
                System.err.println("❌ distributeRatingChanges rolled back: " + ex.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("❌ distributeRatingChanges connection: " + e.getMessage());
        }
    }

    // ─── Rating helpers ───────────────────────────────────────────────────────

    public int getStudentRating(int studentId) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT current_rating FROM student_ratings WHERE student_id=?")) {
            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("❌ getStudentRating: " + e.getMessage());
        }
        return 800; // default starting rating
    }

    // ─── RS Mappers ───────────────────────────────────────────────────────────

    private Contest mapContest(ResultSet rs) throws SQLException {
        Contest c = new Contest();
        c.setContestId(rs.getInt("contest_id"));
        c.setContestTitle(rs.getString("contest_title"));
        c.setDescription(rs.getString("description"));
        c.setTheme(Theme.valueOf(rs.getString("theme")));
        c.setStatus(Status.valueOf(rs.getString("status")));
        c.setCreatedBy(rs.getInt("created_by"));
        Timestamp st = rs.getTimestamp("start_time");
        if (st != null) c.setStartTime(st.toLocalDateTime());
        Timestamp et = rs.getTimestamp("end_time");
        if (et != null) c.setEndTime(et.toLocalDateTime());
        Timestamp ev = rs.getTimestamp("eval_deadline");
        if (ev != null) c.setEvalDeadline(ev.toLocalDateTime());
        c.setDurationMinutes(rs.getInt("duration_minutes"));
        c.setTotalMcqQuestions(rs.getInt("total_mcq"));
        c.setTotalWrittenQuestions(rs.getInt("total_written"));
        c.setMcqMarksEach(rs.getInt("mcq_marks_each"));
        c.setWrittenMarksEach(rs.getInt("written_marks_each"));
        c.setTotalMarks(rs.getInt("total_marks"));
        c.setMaxGain(rs.getInt("max_gain"));
        c.setMaxLoss(rs.getInt("max_loss"));
        return c;
    }

    private ContestQuestion mapQuestion(ResultSet rs) throws SQLException {
        ContestQuestion q = new ContestQuestion();
        q.setQuestionId(rs.getInt("question_id"));
        q.setContestId(rs.getInt("contest_id"));
        q.setType(QuestionType.valueOf(rs.getString("question_type")));
        q.setQuestionText(rs.getString("question_text"));
        q.setMarks(rs.getInt("marks"));
        q.setOrderIndex(rs.getInt("order_index"));
        q.setOptionA(rs.getString("option_a"));
        q.setOptionB(rs.getString("option_b"));
        q.setOptionC(rs.getString("option_c"));
        q.setOptionD(rs.getString("option_d"));
        q.setCorrectAnswer(rs.getString("correct_answer"));
        q.setExplanation(rs.getString("explanation"));
        return q;
    }

    private ContestParticipant mapParticipant(ResultSet rs) throws SQLException {
        ContestParticipant p = new ContestParticipant();
        p.setParticipantId(rs.getInt("participant_id"));
        p.setContestId(rs.getInt("contest_id"));
        p.setStudentId(rs.getInt("student_id"));
        try { p.setStudentName(rs.getString("full_name")); } catch (SQLException ignored) {}
        try { p.setUsername(rs.getString("username")); }    catch (SQLException ignored) {}
        p.setStatus(ParticipantStatus.valueOf(rs.getString("status")));
        Timestamp jt = rs.getTimestamp("joined_at");
        if (jt != null) p.setJoinedAt(jt.toLocalDateTime());
        Timestamp st = rs.getTimestamp("submitted_at");
        if (st != null) p.setSubmittedAt(st.toLocalDateTime());
        p.setMcqMarksObtained(rs.getInt("mcq_marks_obtained"));
        p.setWrittenMarksObtained(rs.getInt("written_marks_obtained"));
        p.setTotalMarksObtained(rs.getInt("total_marks_obtained"));
        p.setLiveRank(rs.getInt("live_rank"));
        p.setFinalRank(rs.getInt("final_rank"));
        p.setRatingBefore(rs.getInt("rating_before"));
        p.setRatingAfter(rs.getInt("rating_after"));
        p.setRatingChange(rs.getInt("rating_change"));
        p.setPendingWrittenReviews(rs.getInt("pending_written_reviews"));
        return p;
    }

    private ContestAnswer mapAnswer(ResultSet rs) throws SQLException {
        ContestAnswer a = new ContestAnswer();
        a.setAnswerId(rs.getInt("answer_id"));
        a.setParticipantId(rs.getInt("participant_id"));
        a.setContestId(rs.getInt("contest_id"));
        a.setQuestionId(rs.getInt("question_id"));
        a.setStudentId(rs.getInt("student_id"));
        a.setSelectedOption(rs.getString("selected_option"));
        a.setCorrect(rs.getBoolean("is_correct"));
        a.setImagePath(rs.getString("image_path"));
        a.setTeacherComment(rs.getString("teacher_comment"));
        String revStatus = rs.getString("review_status");
        if (revStatus != null) a.setReviewStatus(ReviewStatus.valueOf(revStatus));
        a.setReviewedBy(rs.getInt("reviewed_by"));
        a.setMarksAwarded(rs.getInt("marks_awarded"));
        Timestamp at = rs.getTimestamp("answered_at");
        if (at != null) a.setAnsweredAt(at.toLocalDateTime());
        Timestamp rt = rs.getTimestamp("reviewed_at");
        if (rt != null) a.setReviewedAt(rt.toLocalDateTime());
        return a;
    }

    private ContestQuestion getQuestionById(int questionId) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM contest_questions WHERE question_id=?")) {
            ps.setInt(1, questionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapQuestion(rs);
        } catch (SQLException e) {
            System.err.println("❌ getQuestionById: " + e.getMessage());
        }
        return null;
    }
}