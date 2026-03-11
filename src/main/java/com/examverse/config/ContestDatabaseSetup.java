package com.examverse.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * ContestDatabaseSetup
 * Run this once (or call from DatabaseConfig.initializeTables()) to create
 * all contest-related tables: contests, contest_questions,
 * contest_participants, contest_answers, student_ratings, contest_rating_history.
 */
public class ContestDatabaseSetup {

    /**
     * Creates all contest tables. Safe to call multiple times (IF NOT EXISTS).
     */
    public static void initializeContestTables() {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {

            // ═══════════════════════════════════════════════════════════════
            // 1. CONTESTS TABLE
            // ═══════════════════════════════════════════════════════════════
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS contests (
                    contest_id         INT PRIMARY KEY AUTO_INCREMENT,
                    contest_title      VARCHAR(200) NOT NULL,
                    description        TEXT,
                    theme              VARCHAR(50) NOT NULL,
                    status             ENUM('UPCOMING','LIVE','EVALUATION','FINISHED','CANCELLED')
                                       DEFAULT 'UPCOMING',
                    created_by         INT NOT NULL,
                    start_time         DATETIME NOT NULL,
                    end_time           DATETIME NOT NULL,
                    eval_deadline      DATETIME NOT NULL,
                    duration_minutes   INT NOT NULL,
                    total_mcq          INT DEFAULT 0,
                    total_written      INT DEFAULT 0,
                    mcq_marks_each     INT DEFAULT 5,
                    written_marks_each INT DEFAULT 10,
                    total_marks        INT NOT NULL,
                    max_gain           INT DEFAULT 100,
                    max_loss           INT DEFAULT 50,
                    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
                    INDEX idx_status (status),
                    INDEX idx_start_time (start_time)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
            System.out.println("✅ contests table ready");

            // ═══════════════════════════════════════════════════════════════
            // 2. CONTEST_QUESTIONS TABLE
            // ═══════════════════════════════════════════════════════════════
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS contest_questions (
                    question_id   INT PRIMARY KEY AUTO_INCREMENT,
                    contest_id    INT NOT NULL,
                    question_type ENUM('MCQ','WRITTEN') NOT NULL,
                    question_text TEXT NOT NULL,
                    marks         INT NOT NULL DEFAULT 5,
                    order_index   INT DEFAULT 0,
                    option_a      VARCHAR(1000),
                    option_b      VARCHAR(1000),
                    option_c      VARCHAR(1000),
                    option_d      VARCHAR(1000),
                    correct_answer ENUM('A','B','C','D'),
                    explanation   TEXT,
                    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (contest_id) REFERENCES contests(contest_id) ON DELETE CASCADE,
                    INDEX idx_contest (contest_id),
                    INDEX idx_type (question_type)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
            System.out.println("✅ contest_questions table ready");

            // ═══════════════════════════════════════════════════════════════
            // 3. CONTEST_PARTICIPANTS TABLE
            // ═══════════════════════════════════════════════════════════════
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS contest_participants (
                    participant_id          INT PRIMARY KEY AUTO_INCREMENT,
                    contest_id              INT NOT NULL,
                    student_id              INT NOT NULL,
                    status                  ENUM('REGISTERED','ACTIVE','SUBMITTED','EVALUATED')
                                            DEFAULT 'REGISTERED',
                    joined_at               TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    submitted_at            TIMESTAMP NULL,
                    mcq_marks_obtained      INT DEFAULT 0,
                    written_marks_obtained  INT DEFAULT 0,
                    total_marks_obtained    INT DEFAULT 0,
                    live_rank               INT DEFAULT 0,
                    final_rank              INT DEFAULT 0,
                    rating_before           INT DEFAULT 800,
                    rating_after            INT DEFAULT 800,
                    rating_change           INT DEFAULT 0,
                    pending_written_reviews INT DEFAULT 0,
                    UNIQUE KEY unique_participant (contest_id, student_id),
                    FOREIGN KEY (contest_id) REFERENCES contests(contest_id) ON DELETE CASCADE,
                    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
                    INDEX idx_contest (contest_id),
                    INDEX idx_student (student_id),
                    INDEX idx_live_rank (live_rank)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
            System.out.println("✅ contest_participants table ready");

            // ═══════════════════════════════════════════════════════════════
            // 4. CONTEST_ANSWERS TABLE
            // ═══════════════════════════════════════════════════════════════
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS contest_answers (
                    answer_id        INT PRIMARY KEY AUTO_INCREMENT,
                    participant_id   INT NOT NULL,
                    contest_id       INT NOT NULL,
                    question_id      INT NOT NULL,
                    student_id       INT NOT NULL,
                    selected_option  ENUM('A','B','C','D'),
                    is_correct       BOOLEAN DEFAULT FALSE,
                    image_path       VARCHAR(500),
                    teacher_comment  TEXT,
                    review_status    ENUM('PENDING','REVIEWED','REJECTED') DEFAULT 'PENDING',
                    reviewed_by      INT,
                    marks_awarded    INT DEFAULT 0,
                    answered_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    reviewed_at      TIMESTAMP NULL,
                    UNIQUE KEY unique_answer (participant_id, question_id),
                    FOREIGN KEY (participant_id) REFERENCES contest_participants(participant_id) ON DELETE CASCADE,
                    FOREIGN KEY (contest_id) REFERENCES contests(contest_id) ON DELETE CASCADE,
                    FOREIGN KEY (question_id) REFERENCES contest_questions(question_id) ON DELETE CASCADE,
                    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
                    INDEX idx_participant (participant_id),
                    INDEX idx_review_status (review_status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
            System.out.println("✅ contest_answers table ready");

            // ═══════════════════════════════════════════════════════════════
            // 5. STUDENT_RATINGS TABLE
            // ═══════════════════════════════════════════════════════════════
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS student_ratings (
                    rating_id             INT PRIMARY KEY AUTO_INCREMENT,
                    student_id            INT NOT NULL UNIQUE,
                    current_rating        INT DEFAULT 800,
                    peak_rating           INT DEFAULT 800,
                    contests_participated INT DEFAULT 0,
                    contests_won          INT DEFAULT 0,
                    total_score           INT DEFAULT 0,
                    last_updated          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                                          ON UPDATE CURRENT_TIMESTAMP,
                    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
                    INDEX idx_rating (current_rating DESC)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
            System.out.println("✅ student_ratings table ready");

            // ═══════════════════════════════════════════════════════════════
            // 6. CONTEST_RATING_HISTORY TABLE
            // ═══════════════════════════════════════════════════════════════
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS contest_rating_history (
                    history_id     INT PRIMARY KEY AUTO_INCREMENT,
                    student_id     INT NOT NULL,
                    contest_id     INT NOT NULL,
                    rating_before  INT NOT NULL,
                    rating_after   INT NOT NULL,
                    rating_change  INT NOT NULL,
                    final_rank     INT NOT NULL,
                    total_score    INT NOT NULL,
                    recorded_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY (contest_id) REFERENCES contests(contest_id) ON DELETE CASCADE,
                    INDEX idx_student (student_id),
                    INDEX idx_contest (contest_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
            System.out.println("✅ contest_rating_history table ready");

            System.out.println("✅ All contest tables initialized successfully!");

        } catch (SQLException e) {
            System.err.println("❌ Failed to initialize contest tables: " + e.getMessage());
            e.printStackTrace();
        }
    }
}