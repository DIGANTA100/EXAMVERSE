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
     * Admin inbox that receives all contact form submissions
     */
    public static final String CONTACT_RECIPIENT_EMAIL = "ajmainfayekdiganta@gmail.com";

    /**
     * Subject prefix prepended to every contact form email so it's easy
     * to filter/label in Gmail: "[ExamVerse Contact] <user subject>"
     */
    public static final String CONTACT_SUBJECT_PREFIX = "[ExamVerse Contact] ";

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

    /**
     * Professional HTML template for contact form submissions.
     * Delivered to the admin inbox — clean, scannable, colour-coded by category.
     *
     * @param senderName   User's name from the form
     * @param senderEmail  User's email from the form
     * @param subject      Subject entered by the user
     * @param category     Category selected by the user
     * @param messageBody  The full message text
     */
    public static String getContactMessageEmailTemplate(String senderName, String senderEmail,
                                                        String subject, String category,
                                                        String messageBody) {
        // Escape HTML entities in user-supplied content to prevent injection
        String safeName     = escapeHtml(senderName);
        String safeEmail    = escapeHtml(senderEmail);
        String safeSubject  = escapeHtml(subject);
        String safeCategory = escapeHtml(category);
        String safeMessage  = escapeHtml(messageBody).replace("\n", "<br>");

        // Timestamp
        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));

        // Category badge colour
        String categoryColor;
        switch (category.toLowerCase()) {
            case "bug report":      categoryColor = "#ef4444"; break;
            case "feature request": categoryColor = "#8b5cf6"; break;
            case "account issue":   categoryColor = "#f59e0b"; break;
            case "exam issue":      categoryColor = "#f97316"; break;
            default:                categoryColor = "#22d3ee"; break;
        }

        return "<!DOCTYPE html>" +
                "<html lang=\"en\">" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<title>ExamVerse Contact Message</title>" +
                "<style>" +
                "* { box-sizing: border-box; margin: 0; padding: 0; }" +
                "body { font-family: 'Segoe UI', Arial, sans-serif; background: #f1f5f9; color: #0f172a; }" +
                ".wrapper { padding: 40px 20px; }" +
                ".container { max-width: 620px; margin: 0 auto; background: #ffffff; border-radius: 12px;" +
                "             overflow: hidden; box-shadow: 0 8px 30px rgba(0,0,0,0.12); }" +
                ".header { background: linear-gradient(135deg, #020617 0%, #0f172a 60%, #1e293b 100%);" +
                "          padding: 36px 40px; text-align: center; }" +
                ".header-logo { font-size: 28px; font-weight: 300; color: #ffffff; letter-spacing: 3px; }" +
                ".header-logo span { color: #22d3ee; font-weight: 700; }" +
                ".header-tagline { color: rgba(148,163,184,0.8); font-size: 10px; letter-spacing: 4px;" +
                "                  margin-top: 6px; text-transform: uppercase; }" +
                ".header-badge { display: inline-block; margin-top: 18px;" +
                "                background: rgba(34,211,238,0.1); border: 1px solid rgba(34,211,238,0.3);" +
                "                color: #22d3ee; font-size: 10px; font-weight: 600; letter-spacing: 2px;" +
                "                padding: 5px 14px; border-radius: 20px; text-transform: uppercase; }" +
                ".category-strip { background: " + categoryColor + "; padding: 10px 40px; }" +
                ".category-strip span { color: #ffffff; font-size: 11px; font-weight: 700;" +
                "                       letter-spacing: 2px; text-transform: uppercase; }" +
                ".content { padding: 36px 40px; }" +
                ".section-label { font-size: 10px; font-weight: 700; color: #94a3b8;" +
                "                 letter-spacing: 2px; text-transform: uppercase; margin-bottom: 12px; }" +
                ".sender-card { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px;" +
                "               padding: 20px 24px; margin-bottom: 28px; }" +
                ".sender-name { font-size: 18px; font-weight: 600; color: #0f172a; }" +
                ".sender-email { font-size: 13px; color: #22d3ee; margin-top: 4px; }" +
                ".sender-meta { font-size: 11px; color: #94a3b8; margin-top: 8px; }" +
                ".subject-row { border-left: 3px solid " + categoryColor + "; padding: 10px 16px;" +
                "               background: #f8fafc; border-radius: 0 6px 6px 0; margin-bottom: 28px; }" +
                ".subject-text { font-size: 15px; font-weight: 600; color: #0f172a; }" +
                ".message-box { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px;" +
                "               padding: 24px; margin-bottom: 28px; }" +
                ".message-text { font-size: 14px; color: #334155; line-height: 1.9; }" +
                ".reply-box { background: linear-gradient(135deg, #ecfeff 0%, #f0f9ff 100%);" +
                "             border: 1px solid #a5f3fc; border-radius: 8px; padding: 18px 24px; margin-bottom: 20px; }" +
                ".reply-box p { font-size: 13px; color: #0e7490; line-height: 1.6; }" +
                ".reply-box strong { color: #0f172a; }" +
                ".reply-email { display: inline-block; margin-top: 8px; color: #22d3ee;" +
                "               font-weight: 600; font-size: 13px; text-decoration: none; }" +
                ".divider { border: none; border-top: 1px solid #e2e8f0; margin: 4px 0 24px 0; }" +
                ".footer { background: #f8fafc; padding: 24px 40px; text-align: center;" +
                "          border-top: 1px solid #e2e8f0; }" +
                ".footer p { font-size: 11px; color: #94a3b8; line-height: 1.7; }" +
                ".footer .brand { font-size: 13px; font-weight: 600; color: #64748b; margin-bottom: 6px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class=\"wrapper\">" +
                "<div class=\"container\">" +

                // Header
                "<div class=\"header\">" +
                "<div class=\"header-logo\">EXAM<span>VERSE</span></div>" +
                "<div class=\"header-tagline\">Intelligent Assessment Platform</div>" +
                "<div class=\"header-badge\">&#128236; New Contact Message</div>" +
                "</div>" +

                // Category strip
                "<div class=\"category-strip\">" +
                "<span>&#9679; &nbsp;" + safeCategory + "</span>" +
                "</div>" +

                // Body
                "<div class=\"content\">" +

                "<div class=\"section-label\">From</div>" +
                "<div class=\"sender-card\">" +
                "<div class=\"sender-name\">" + safeName + "</div>" +
                "<div class=\"sender-email\">" + safeEmail + "</div>" +
                "<div class=\"sender-meta\">Submitted via ExamVerse Contact Form &nbsp;&middot;&nbsp; " + timestamp + "</div>" +
                "</div>" +

                "<div class=\"section-label\">Subject</div>" +
                "<div class=\"subject-row\">" +
                "<div class=\"subject-text\">" + safeSubject + "</div>" +
                "</div>" +

                "<div class=\"section-label\">Message</div>" +
                "<div class=\"message-box\">" +
                "<div class=\"message-text\">" + safeMessage + "</div>" +
                "</div>" +

                "<div class=\"reply-box\">" +
                "<p><strong>&#128161; How to reply:</strong> Simply hit <strong>Reply</strong> in Gmail &mdash; " +
                "it goes directly to <strong>" + safeName + "</strong> at:</p>" +
                "<a class=\"reply-email\" href=\"mailto:" + safeEmail + "\">" + safeEmail + "</a>" +
                "</div>" +

                "<hr class=\"divider\">" +
                "</div>" +

                // Footer
                "<div class=\"footer\">" +
                "<div class=\"brand\">EXAMVERSE</div>" +
                "<p>This message was submitted through the ExamVerse contact form.<br>" +
                "Department of CSE &nbsp;&middot;&nbsp; Bangladesh University of Engineering and Technology &nbsp;&middot;&nbsp; 2026</p>" +
                "</div>" +

                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    /**
     * Escape HTML special characters in user-supplied strings.
     * Prevents HTML injection from contact form input.
     */
    private static String escapeHtml(String input) {
        if (input == null) return "";
        return input
                .replace("&",  "&amp;")
                .replace("<",  "&lt;")
                .replace(">",  "&gt;")
                .replace("\"", "&quot;")
                .replace("'",  "&#39;");
    }
}