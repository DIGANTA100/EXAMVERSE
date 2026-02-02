package com.examverse.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DatabaseConfig - Manages MySQL database connection
 * Handles connection pooling and database initialization
 */
public class DatabaseConfig {

    // Database credentials - CHANGE THESE to match your MySQL setup
    private static final String DB_URL = "jdbc:mysql://localhost:3306/examverse_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Jls32wkksedie@..sdk"; // Change this to your MySQL password

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

            // Create users table
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

            // Create default admin account if not exists
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