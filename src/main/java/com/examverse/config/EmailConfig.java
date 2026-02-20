package com.examverse.config;

/**
 * EmailConfig - Email server configuration
 * Configure your SMTP settings here
 * UPDATED: Added password reset email template (No text blocks - Java 11+ compatible)
 */
public class EmailConfig {

    // ============================================
    // SMTP CONFIGURATION - UPDATE THESE VALUES
    // ============================================

    /**
     * SMTP Host (Gmail, Outlook, etc.)
     * Gmail: smtp.gmail.com
     * Outlook: smtp-mail.outlook.com
     * Yahoo: smtp.mail.yahoo.com
     */
    public static final String SMTP_HOST = "smtp.gmail.com";

    /**
     * SMTP Port
     * For Gmail with TLS: 587
     * For Gmail with SSL: 465
     */
    public static final String SMTP_PORT = "587";

    /**
     * Your email address (sender)
     * Example: your-email@gmail.com
     */
    public static final String SENDER_EMAIL = "ajmainfayekdiganta@gmail.com";

    /**
     * Your email password or App Password
     *
     * IMPORTANT FOR GMAIL USERS:
     * 1. Enable 2-Factor Authentication in your Google Account
     * 2. Generate an "App Password" at: https://myaccount.google.com/apppasswords
     * 3. Use that 16-character App Password here (not your regular Gmail password)
     *
     * Example: "abcd efgh ijkl mnop" (remove spaces)
     */
    public static final String SENDER_PASSWORD = "dnlvwbartnmtqeeq";

    /**
     * Sender display name
     */
    public static final String SENDER_NAME = "ExamVerse Team";

    /**
     * Enable TLS encryption
     */
    public static final boolean ENABLE_TLS = true;

    /**
     * Enable SSL encryption (alternative to TLS)
     */
    public static final boolean ENABLE_SSL = false;

    /**
     * Enable authentication
     */
    public static final boolean ENABLE_AUTH = true;

    /**
     * Enable debug mode (prints detailed email logs)
     */
    public static final boolean DEBUG_MODE = true;

    // ============================================
    // EMAIL TEMPLATES
    // ============================================

    /**
     * Welcome email subject
     */
    public static final String WELCOME_SUBJECT = "Welcome to ExamVerse! 🎓";

    /**
     * Password reset email subject
     */
    public static final String RESET_PASSWORD_SUBJECT = "Reset Your ExamVerse Password 🔐";

    /**
     * Get HTML template for welcome email
     */
    public static String getWelcomeEmailTemplate(String fullName, String username) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<style>" +
                "body {" +
                "font-family: 'Segoe UI', Arial, sans-serif;" +
                "line-height: 1.6;" +
                "color: #333;" +
                "background-color: #f4f4f4;" +
                "margin: 0;" +
                "padding: 0;" +
                "}" +
                ".container {" +
                "max-width: 600px;" +
                "margin: 30px auto;" +
                "background: white;" +
                "border-radius: 10px;" +
                "overflow: hidden;" +
                "box-shadow: 0 4px 6px rgba(0,0,0,0.1);" +
                "}" +
                ".header {" +
                "background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);" +
                "color: white;" +
                "padding: 40px 30px;" +
                "text-align: center;" +
                "}" +
                ".header h1 {" +
                "margin: 0;" +
                "font-size: 32px;" +
                "font-weight: 300;" +
                "letter-spacing: 2px;" +
                "}" +
                ".header .accent {" +
                "color: #22d3ee;" +
                "font-weight: 600;" +
                "}" +
                ".content {" +
                "padding: 40px 30px;" +
                "}" +
                ".content h2 {" +
                "color: #0f172a;" +
                "font-size: 24px;" +
                "margin-top: 0;" +
                "}" +
                ".content p {" +
                "color: #64748b;" +
                "font-size: 16px;" +
                "line-height: 1.8;" +
                "}" +
                ".info-box {" +
                "background: #f1f5f9;" +
                "border-left: 4px solid #22d3ee;" +
                "padding: 20px;" +
                "margin: 25px 0;" +
                "border-radius: 4px;" +
                "}" +
                ".info-box strong {" +
                "color: #0f172a;" +
                "}" +
                ".cta-button {" +
                "display: inline-block;" +
                "background: #22d3ee;" +
                "color: #0f172a;" +
                "padding: 14px 35px;" +
                "text-decoration: none;" +
                "border-radius: 6px;" +
                "font-weight: 600;" +
                "margin: 20px 0;" +
                "transition: all 0.3s;" +
                "}" +
                ".cta-button:hover {" +
                "background: #06b6d4;" +
                "}" +
                ".features {" +
                "margin: 30px 0;" +
                "}" +
                ".feature-item {" +
                "padding: 15px 0;" +
                "border-bottom: 1px solid #e2e8f0;" +
                "}" +
                ".feature-item:last-child {" +
                "border-bottom: none;" +
                "}" +
                ".feature-item strong {" +
                "color: #22d3ee;" +
                "}" +
                ".footer {" +
                "background: #f8fafc;" +
                "padding: 25px 30px;" +
                "text-align: center;" +
                "color: #94a3b8;" +
                "font-size: 13px;" +
                "border-top: 1px solid #e2e8f0;" +
                "}" +
                ".footer a {" +
                "color: #22d3ee;" +
                "text-decoration: none;" +
                "}" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class=\"container\">" +
                "<div class=\"header\">" +
                "<h1>EXAM<span class=\"accent\">VERSE</span></h1>" +
                "<p style=\"margin: 10px 0 0 0; opacity: 0.8; letter-spacing: 3px; font-size: 11px;\">INTELLIGENT ASSESSMENT PLATFORM</p>" +
                "</div>" +
                "<div class=\"content\">" +
                "<h2>Welcome aboard, " + fullName + "! 🎉</h2>" +
                "<p>" +
                "Thank you for joining <strong>ExamVerse</strong>! We're excited to have you as part of our " +
                "intelligent assessment platform. Your account has been successfully created and is ready to use." +
                "</p>" +
                "<div class=\"info-box\">" +
                "<strong>📋 Your Account Details:</strong><br>" +
                "<strong>Username:</strong> " + username + "<br>" +
                "<strong>Email:</strong> This email address<br>" +
                "<strong>Status:</strong> Active ✅" +
                "</div>" +
                "<div style=\"text-align: center;\">" +
                "<a href=\"#\" class=\"cta-button\">Start Taking Exams →</a>" +
                "</div>" +
                "<div class=\"features\">" +
                "<h3 style=\"color: #0f172a; font-size: 18px;\">What you can do with ExamVerse:</h3>" +
                "<div class=\"feature-item\">" +
                "<strong>📝 Take Exams</strong><br>" +
                "Access a wide variety of exams across different subjects and difficulty levels." +
                "</div>" +
                "<div class=\"feature-item\">" +
                "<strong>💪 Practice Mode</strong><br>" +
                "Sharpen your skills with unlimited practice questions and instant feedback." +
                "</div>" +
                "<div class=\"feature-item\">" +
                "<strong>📊 Track Progress</strong><br>" +
                "Monitor your performance with detailed analytics and personalized insights." +
                "</div>" +
                "<div class=\"feature-item\">" +
                "<strong>⏱️ Timed Assessments</strong><br>" +
                "Experience real exam conditions with our intelligent timer system." +
                "</div>" +
                "</div>" +
                "<p style=\"margin-top: 30px; padding-top: 20px; border-top: 1px solid #e2e8f0;\">" +
                "<strong>Need help getting started?</strong><br>" +
                "Check out our quick start guide or contact our support team at " +
                "<a href=\"mailto:support@examverse.com\" style=\"color: #22d3ee;\">support@examverse.com</a>" +
                "</p>" +
                "</div>" +
                "<div class=\"footer\">" +
                "<p>" +
                "This email was sent to you because you registered for an ExamVerse account.<br>" +
                "If you didn't create this account, please ignore this email." +
                "</p>" +
                "<p style=\"margin-top: 15px;\">" +
                "© 2024 ExamVerse. All rights reserved.<br>" +
                "<a href=\"#\">Privacy Policy</a> • <a href=\"#\">Terms of Service</a> • <a href=\"#\">Contact Us</a>" +
                "</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    /**
     * Get HTML template for password reset email
     */
    public static String getPasswordResetEmailTemplate(String fullName, String resetCode) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<style>" +
                "body {" +
                "font-family: 'Segoe UI', Arial, sans-serif;" +
                "line-height: 1.6;" +
                "color: #333;" +
                "background-color: #f4f4f4;" +
                "margin: 0;" +
                "padding: 0;" +
                "}" +
                ".container {" +
                "max-width: 600px;" +
                "margin: 30px auto;" +
                "background: white;" +
                "border-radius: 10px;" +
                "overflow: hidden;" +
                "box-shadow: 0 4px 6px rgba(0,0,0,0.1);" +
                "}" +
                ".header {" +
                "background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);" +
                "color: white;" +
                "padding: 40px 30px;" +
                "text-align: center;" +
                "}" +
                ".header h1 {" +
                "margin: 0;" +
                "font-size: 32px;" +
                "font-weight: 300;" +
                "letter-spacing: 2px;" +
                "}" +
                ".header .accent {" +
                "color: #22d3ee;" +
                "font-weight: 600;" +
                "}" +
                ".content {" +
                "padding: 40px 30px;" +
                "}" +
                ".content h2 {" +
                "color: #0f172a;" +
                "font-size: 24px;" +
                "margin-top: 0;" +
                "}" +
                ".content p {" +
                "color: #64748b;" +
                "font-size: 16px;" +
                "line-height: 1.8;" +
                "}" +
                ".code-box {" +
                "background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);" +
                "border: 2px solid #22d3ee;" +
                "padding: 30px;" +
                "margin: 30px 0;" +
                "border-radius: 12px;" +
                "text-align: center;" +
                "}" +
                ".reset-code {" +
                "font-size: 48px;" +
                "font-weight: 700;" +
                "color: #22d3ee;" +
                "letter-spacing: 8px;" +
                "font-family: 'Courier New', monospace;" +
                "margin: 10px 0;" +
                "text-shadow: 0 2px 4px rgba(0,0,0,0.3);" +
                "}" +
                ".code-label {" +
                "color: #94a3b8;" +
                "font-size: 14px;" +
                "text-transform: uppercase;" +
                "letter-spacing: 2px;" +
                "margin-bottom: 10px;" +
                "}" +
                ".warning-box {" +
                "background: #fef3c7;" +
                "border-left: 4px solid #f59e0b;" +
                "padding: 20px;" +
                "margin: 25px 0;" +
                "border-radius: 4px;" +
                "}" +
                ".warning-box strong {" +
                "color: #92400e;" +
                "}" +
                ".warning-box p {" +
                "color: #78350f;" +
                "margin: 5px 0;" +
                "}" +
                ".info-list {" +
                "background: #f1f5f9;" +
                "padding: 20px 25px;" +
                "border-radius: 8px;" +
                "margin: 20px 0;" +
                "}" +
                ".info-list li {" +
                "color: #475569;" +
                "margin: 10px 0;" +
                "line-height: 1.6;" +
                "}" +
                ".footer {" +
                "background: #f8fafc;" +
                "padding: 25px 30px;" +
                "text-align: center;" +
                "color: #94a3b8;" +
                "font-size: 13px;" +
                "border-top: 1px solid #e2e8f0;" +
                "}" +
                ".footer a {" +
                "color: #22d3ee;" +
                "text-decoration: none;" +
                "}" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class=\"container\">" +
                "<div class=\"header\">" +
                "<h1>EXAM<span class=\"accent\">VERSE</span></h1>" +
                "<p style=\"margin: 10px 0 0 0; opacity: 0.8; letter-spacing: 3px; font-size: 11px;\">INTELLIGENT ASSESSMENT PLATFORM</p>" +
                "</div>" +
                "<div class=\"content\">" +
                "<h2>Password Reset Request 🔐</h2>" +
                "<p>Hi " + fullName + ",</p>" +
                "<p>" +
                "We received a request to reset your ExamVerse password. " +
                "Use the verification code below to proceed with resetting your password." +
                "</p>" +
                "<div class=\"code-box\">" +
                "<div class=\"code-label\">Your Verification Code</div>" +
                "<div class=\"reset-code\">" + resetCode + "</div>" +
                "<p style=\"color: #94a3b8; font-size: 13px; margin-top: 15px;\">" +
                "Enter this code in the password reset screen" +
                "</p>" +
                "</div>" +
                "<div class=\"warning-box\">" +
                "<strong>⚠️ Important Security Information:</strong>" +
                "<p style=\"margin-top: 10px;\">" +
                "• This code will expire in <strong>10 minutes</strong><br>" +
                "• Do not share this code with anyone<br>" +
                "• ExamVerse will never ask for this code via phone or email" +
                "</p>" +
                "</div>" +
                "<div class=\"info-list\">" +
                "<strong style=\"color: #0f172a; font-size: 16px;\">Next Steps:</strong>" +
                "<ol style=\"padding-left: 20px; margin: 15px 0 0 0;\">" +
                "<li>Return to the ExamVerse password reset screen</li>" +
                "<li>Enter the 6-digit verification code above</li>" +
                "<li>Create your new password</li>" +
                "<li>Log in with your new credentials</li>" +
                "</ol>" +
                "</div>" +
                "<p style=\"margin-top: 30px; padding-top: 20px; border-top: 1px solid #e2e8f0; color: #64748b;\">" +
                "<strong>Didn't request this?</strong><br>" +
                "If you didn't request a password reset, you can safely ignore this email. " +
                "Your password will remain unchanged. For security concerns, contact us at " +
                "<a href=\"mailto:security@examverse.com\" style=\"color: #22d3ee;\">security@examverse.com</a>" +
                "</p>" +
                "</div>" +
                "<div class=\"footer\">" +
                "<p>" +
                "This is an automated security email from ExamVerse.<br>" +
                "Please do not reply to this email." +
                "</p>" +
                "<p style=\"margin-top: 15px;\">" +
                "© 2024 ExamVerse. All rights reserved.<br>" +
                "<a href=\"#\">Privacy Policy</a> • <a href=\"#\">Security Center</a> • <a href=\"#\">Contact Us</a>" +
                "</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
}