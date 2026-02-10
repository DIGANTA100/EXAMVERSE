package com.examverse.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DatabaseConfig - Manages MySQL database connection
 * Handles connection pooling and database initialization
 * UPDATED WITH EXAM TABLES
 */
public class DatabaseConfig {

    // Database credentials - CHANGE THESE to match your MySQL setup
    private static final String DB_URL = "jdbc:mysql://localhost:3306/examverse_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "#Ihatenone2103"; // Change this to your MySQL password

    // Alternative: If database doesn't exist yet, use this for initial connection
    private static final String DB_URL_NO_DB = "jdbc:mysql://localhost:3306/";

    private static Connection connection = null;

    /**
     * Get database connection
     * Creates database if it doesn't exist
     */
    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                // First, try to connect to the database
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                    System.out.println("✅ Connected to ExamVerse database successfully!");
                } catch (SQLException e) {
                    // Database doesn't exist, create it
                    System.out.println("Database doesn't exist. Creating...");
                    createDatabase();
                    // Try connecting again
                    connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                    System.out.println("✅ Database created and connected!");
                }
            }
            return connection;
        } catch (ClassNotFoundException e) {
            System.err.println("❌ MySQL JDBC Driver not found!");
            System.err.println("Add MySQL Connector to build.gradle.kts:");
            System.err.println("implementation(\"com.mysql:mysql-connector-j:8.2.0\")");
            e.printStackTrace();
            return null;
        } catch (SQLException e) {
            System.err.println("❌ Database connection failed!");
            System.err.println("Check your MySQL credentials in DatabaseConfig.java");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Create database if it doesn't exist
     */
    private static void createDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL_NO_DB, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement()) {

            String sql = "CREATE DATABASE IF NOT EXISTS examverse_db";
            stmt.executeUpdate(sql);
            System.out.println("✅ Database 'examverse_db' created!");

        } catch (SQLException e) {
            System.err.println("❌ Failed to create database!");
            e.printStackTrace();
        }
    }

    /**
     * Initialize database tables
     * Call this once when app starts
     */
    public static void initializeTables() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // ============================================
            // 1. USERS TABLE
            // ============================================
            String createUsersTable = """
                CREATE TABLE IF NOT EXISTS users (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    username VARCHAR(50) UNIQUE NOT NULL,
                    email VARCHAR(100) UNIQUE NOT NULL,
                    password VARCHAR(255) NOT NULL,
                    full_name VARCHAR(100) NOT NULL,
                    user_type ENUM('STUDENT', 'ADMIN') DEFAULT 'STUDENT',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    last_login TIMESTAMP NULL,
                    is_active BOOLEAN DEFAULT TRUE,
                    INDEX idx_username (username),
                    INDEX idx_email (email)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """;
            stmt.executeUpdate(createUsersTable);
            System.out.println("✅ Users table created/verified!");

            // ============================================
            // 2. EXAMS TABLE
            // ============================================
            String createExamsTable = """
                CREATE TABLE IF NOT EXISTS exams (
                    exam_id INT PRIMARY KEY AUTO_INCREMENT,
                    exam_title VARCHAR(200) NOT NULL,
                    subject VARCHAR(100) NOT NULL,
                    description TEXT,
                    difficulty ENUM('EASY', 'MEDIUM', 'HARD') DEFAULT 'MEDIUM',
                    total_questions INT NOT NULL,
                    total_marks INT NOT NULL,
                    duration_minutes INT NOT NULL,
                    passing_marks INT NOT NULL,
                    status ENUM('ACTIVE', 'INACTIVE', 'ARCHIVED') DEFAULT 'ACTIVE',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    start_date DATETIME NULL,
                    end_date DATETIME NULL,
                    created_by INT,
                    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
                    INDEX idx_subject (subject),
                    INDEX idx_difficulty (difficulty),
                    INDEX idx_status (status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """;
            stmt.executeUpdate(createExamsTable);
            System.out.println("✅ Exams table created/verified!");

            // ============================================
            // 3. STUDENT_EXAM_ATTEMPTS TABLE
            // ============================================
            String createAttemptsTable = """
                CREATE TABLE IF NOT EXISTS student_exam_attempts (
                    attempt_id INT PRIMARY KEY AUTO_INCREMENT,
                    student_id INT NOT NULL,
                    exam_id INT NOT NULL,
                    status ENUM('ONGOING', 'COMPLETED', 'ABANDONED') DEFAULT 'ONGOING',
                    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    submitted_at TIMESTAMP NULL,
                    time_spent_minutes INT DEFAULT 0,
                    obtained_marks INT DEFAULT 0,
                    total_marks INT NOT NULL,
                    passing_marks INT NOT NULL,
                    accuracy DECIMAL(5,2) DEFAULT 0.00,
                    result ENUM('PASSED', 'FAILED', 'PENDING') DEFAULT 'PENDING',
                    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY (exam_id) REFERENCES exams(exam_id) ON DELETE CASCADE,
                    INDEX idx_student (student_id),
                    INDEX idx_exam (exam_id),
                    INDEX idx_status (status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """;
            stmt.executeUpdate(createAttemptsTable);
            System.out.println("✅ Student exam attempts table created/verified!");

            // ============================================
            // 4. QUESTIONS TABLE
            // ============================================
            String createQuestionsTable = """
                CREATE TABLE IF NOT EXISTS questions (
                    question_id INT PRIMARY KEY AUTO_INCREMENT,
                    exam_id INT NOT NULL,
                    question_text TEXT NOT NULL,
                    option_a VARCHAR(500) NOT NULL,
                    option_b VARCHAR(500) NOT NULL,
                    option_c VARCHAR(500) NOT NULL,
                    option_d VARCHAR(500) NOT NULL,
                    correct_answer ENUM('A', 'B', 'C', 'D') NOT NULL,
                    marks INT DEFAULT 1,
                    explanation TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (exam_id) REFERENCES exams(exam_id) ON DELETE CASCADE,
                    INDEX idx_exam (exam_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """;
            stmt.executeUpdate(createQuestionsTable);
            System.out.println("✅ Questions table created/verified!");

            // ============================================
            // 5. STUDENT_ANSWERS TABLE
            // ============================================
            String createAnswersTable = """
                CREATE TABLE IF NOT EXISTS student_answers (
                    answer_id INT PRIMARY KEY AUTO_INCREMENT,
                    attempt_id INT NOT NULL,
                    question_id INT NOT NULL,
                    selected_answer ENUM('A', 'B', 'C', 'D'),
                    is_correct BOOLEAN DEFAULT FALSE,
                    marks_obtained INT DEFAULT 0,
                    time_spent_seconds INT DEFAULT 0,
                    answered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (attempt_id) REFERENCES student_exam_attempts(attempt_id) ON DELETE CASCADE,
                    FOREIGN KEY (question_id) REFERENCES questions(question_id) ON DELETE CASCADE,
                    UNIQUE KEY unique_attempt_question (attempt_id, question_id),
                    INDEX idx_attempt (attempt_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """;
            stmt.executeUpdate(createAnswersTable);
            System.out.println("✅ Student answers table created/verified!");

            // ============================================
            // DEFAULT DATA INSERTION
            // ============================================

            // Check and create default admin account
            String checkAdmin = "SELECT COUNT(*) FROM users WHERE user_type = 'ADMIN'";
            var rs = stmt.executeQuery(checkAdmin);
            rs.next();

            if (rs.getInt(1) == 0) {
                String insertAdmin = """
                    INSERT INTO users (username, email, password, full_name, user_type)
                    VALUES ('admin', 'admin@examverse.com', 'admin123', 'System Administrator', 'ADMIN')
                    """;
                stmt.executeUpdate(insertAdmin);
                System.out.println("✅ Default admin account created!");
                System.out.println("   Username: admin | Password: admin123");
            }

            // Check and insert sample exams
            String checkExams = "SELECT COUNT(*) FROM exams";
            rs = stmt.executeQuery(checkExams);
            rs.next();

            if (rs.getInt(1) == 0) {
                // Sample exam data
                String insertSampleExams = """
                    INSERT INTO exams (exam_title, subject, description, difficulty, total_questions, 
                                      total_marks, duration_minutes, passing_marks, created_by) VALUES
                    ('Java Fundamentals Quiz', 'Programming', 'Test your knowledge of Java basics', 'EASY', 20, 100, 30, 40, 1),
                    ('Data Structures Assessment', 'Computer Science', 'Arrays, Linked Lists, Trees, and more', 'MEDIUM', 25, 100, 45, 50, 1),
                    ('Advanced Algorithms', 'Computer Science', 'Sorting, Searching, Dynamic Programming', 'HARD', 30, 150, 60, 75, 1),
                    ('Database Management Quiz', 'Database', 'SQL, Normalization, Transactions', 'MEDIUM', 20, 100, 40, 50, 1),
                    ('Web Development Basics', 'Web Development', 'HTML, CSS, JavaScript fundamentals', 'EASY', 15, 75, 25, 30, 1)
                    """;
                stmt.executeUpdate(insertSampleExams);
                System.out.println("✅ Sample exams created!");
            }

        } catch (SQLException e) {
            System.err.println("❌ Failed to initialize tables!");
            e.printStackTrace();
        }
    }

    /**
     * Test database connection
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Close database connection
     */
    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("✅ Database connection closed!");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error closing database connection!");
            e.printStackTrace();
        }
    }
}