package com.examverse.config;

/**
 * AppConfig — Central configuration constants for ExamVerse.
 *
 * Placement: src/main/java/com/examverse/config/AppConfig.java
 *
 * HOW TO USE:
 *   - Fill in your actual database credentials below.
 *   - Paste your Gemini API key from https://aistudio.google.com/app/apikey
 *   - All other values can stay as-is for a typical local dev setup.
 */
public final class AppConfig {

    // ── Prevent instantiation ────────────────────────────────────────────────
    private AppConfig() {}

    // ════════════════════════════════════════════════════════════════════════
    //  APPLICATION INFO
    // ════════════════════════════════════════════════════════════════════════

    public static final String APP_NAME        = "ExamVerse";
    public static final String APP_VERSION     = "1.0.0";
    public static final String APP_DESCRIPTION = "Student Examination Portal";

    // ════════════════════════════════════════════════════════════════════════
    //  DATABASE CONFIGURATION
    //  ⚠️  Fill in your actual MySQL credentials before running.
    // ════════════════════════════════════════════════════════════════════════

    public static final String DB_HOST     = "localhost";
    public static final int    DB_PORT     = 3306;
    public static final String DB_NAME     = "examverse";
    public static final String DB_USER     = "root";
    public static final String DB_PASSWORD = "Jls32wkksedie@..sdk";           // ← your MySQL password

    /** Full JDBC connection URL — built from the constants above. */
    public static final String DB_URL =
            "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME
                    + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

    // ════════════════════════════════════════════════════════════════════════
    //  GEMINI AI CONFIGURATION
    //  1. Go to https://aistudio.google.com/app/apikey
    //  2. Click "Create API Key"
    //  3. Paste the key below (starts with "AIza...")
    // ════════════════════════════════════════════════════════════════════════

    public static final String GEMINI_API_KEY = "AIzaSyAeUfoUro_SghRR9fC6Tbjg9M6Sgy0YsLw";   // ← paste here

    // ════════════════════════════════════════════════════════════════════════
    //  EMAIL / SMTP CONFIGURATION (for OTP / password reset)
    //  ⚠️  Fill in your SMTP credentials if you use the email feature.
    //      For Gmail: enable "App Passwords" in your Google account settings.
    // ════════════════════════════════════════════════════════════════════════

    public static final String EMAIL_HOST     = "smtp.gmail.com";
    public static final int    EMAIL_PORT     = 587;
    public static final String EMAIL_USERNAME = "ajmainfayekdiganta@gmail.com"; // ← your email
    public static final String EMAIL_PASSWORD = "dnlvwbartnmtqeeq";    // ← app password
    public static final boolean EMAIL_TLS     = true;

    // ════════════════════════════════════════════════════════════════════════
    //  SESSION / SECURITY
    // ════════════════════════════════════════════════════════════════════════

    /** OTP expires after this many minutes. */
    public static final int OTP_EXPIRY_MINUTES = 10;

    /** Password reset token validity in minutes. */
    public static final int RESET_TOKEN_EXPIRY_MINUTES = 30;

    /** Minimum password length enforced by Validator. */
    public static final int MIN_PASSWORD_LENGTH = 8;

    // ════════════════════════════════════════════════════════════════════════
    //  EXAM SETTINGS
    // ════════════════════════════════════════════════════════════════════════

    /** Default exam duration in minutes when none is specified. */
    public static final int DEFAULT_EXAM_DURATION_MINUTES = 60;

    /** Default number of questions per practice session. */
    public static final int DEFAULT_PRACTICE_QUESTION_COUNT = 10;

    /** Passing percentage threshold (e.g. 60 means 60%). */
    public static final double PASSING_PERCENTAGE = 60.0;

    // ════════════════════════════════════════════════════════════════════════
    //  UI / WINDOW
    // ════════════════════════════════════════════════════════════════════════

    public static final double WINDOW_MIN_WIDTH  = 1024;
    public static final double WINDOW_MIN_HEIGHT = 680;
    public static final double WINDOW_PREF_WIDTH = 1440;
    public static final double WINDOW_PREF_HEIGHT = 900;
}