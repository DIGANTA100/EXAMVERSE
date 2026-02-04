package com.examverse.service.auth;

import com.examverse.config.EmailConfig;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * EmailService - Handles sending emails using JavaMail API
 * Supports HTML emails, welcome emails, password reset, etc.
 */
public class EmailService {

    private static EmailService instance;
    private Session mailSession;

    /**
     * Private constructor for Singleton pattern
     */
    private EmailService() {
        initializeMailSession();
    }

    /**
     * Get EmailService instance (Singleton)
     */
    public static EmailService getInstance() {
        if (instance == null) {
            instance = new EmailService();
        }
        return instance;
    }

    /**
     * Initialize mail session with SMTP configuration
     */
    private void initializeMailSession() {
        Properties properties = new Properties();

        // SMTP Server Configuration
        properties.put("mail.smtp.host", EmailConfig.SMTP_HOST);
        properties.put("mail.smtp.port", EmailConfig.SMTP_PORT);
        properties.put("mail.smtp.auth", String.valueOf(EmailConfig.ENABLE_AUTH));

        // TLS/SSL Configuration
        if (EmailConfig.ENABLE_TLS) {
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.starttls.required", "true");
        }

        if (EmailConfig.ENABLE_SSL) {
            properties.put("mail.smtp.ssl.enable", "true");
            properties.put("mail.smtp.socketFactory.port", EmailConfig.SMTP_PORT);
            properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        }

        // Additional settings
        properties.put("mail.smtp.ssl.protocols", "TLSv1.2");
        properties.put("mail.smtp.ssl.trust", EmailConfig.SMTP_HOST);

        // Debug mode
        if (EmailConfig.DEBUG_MODE) {
            properties.put("mail.debug", "true");
        }

        // Create mail session with authentication
        mailSession = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(
                        EmailConfig.SENDER_EMAIL,
                        EmailConfig.SENDER_PASSWORD
                );
            }
        });

        // Enable debug output
        if (EmailConfig.DEBUG_MODE) {
            mailSession.setDebug(true);
        }

        System.out.println("✅ Email service initialized successfully!");
    }

    /**
     * Send welcome email to newly registered user
     *
     * @param recipientEmail User's email address
     * @param fullName User's full name
     * @param username User's username
     * @return true if email sent successfully, false otherwise
     */
    public boolean sendWelcomeEmail(String recipientEmail, String fullName, String username) {
        try {
            System.out.println("📧 Preparing welcome email for: " + recipientEmail);

            // Create message
            Message message = new MimeMessage(mailSession);

            // Set sender
            message.setFrom(new InternetAddress(
                    EmailConfig.SENDER_EMAIL,
                    EmailConfig.SENDER_NAME
            ));

            // Set recipient
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(recipientEmail)
            );

            // Set subject
            message.setSubject(EmailConfig.WELCOME_SUBJECT);

            // Set HTML content
            String htmlContent = EmailConfig.getWelcomeEmailTemplate(fullName, username);
            message.setContent(htmlContent, "text/html; charset=utf-8");

            // Send message
            System.out.println("📤 Sending email...");
            Transport.send(message);

            System.out.println("✅ Welcome email sent successfully to: " + recipientEmail);
            return true;

        } catch (Exception e) {
            System.err.println("❌ Failed to send welcome email to: " + recipientEmail);
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Send a generic email (for future use)
     *
     * @param recipientEmail Recipient's email
     * @param subject Email subject
     * @param htmlContent HTML content
     * @return true if sent successfully
     */
    public boolean sendEmail(String recipientEmail, String subject, String htmlContent) {
        try {
            Message message = new MimeMessage(mailSession);

            message.setFrom(new InternetAddress(
                    EmailConfig.SENDER_EMAIL,
                    EmailConfig.SENDER_NAME
            ));

            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(recipientEmail)
            );

            message.setSubject(subject);
            message.setContent(htmlContent, "text/html; charset=utf-8");

            Transport.send(message);

            System.out.println("✅ Email sent successfully to: " + recipientEmail);
            return true;

        } catch (Exception e) {
            System.err.println("❌ Failed to send email to: " + recipientEmail);
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Send password reset email (placeholder for future implementation)
     */
    public boolean sendPasswordResetEmail(String recipientEmail, String resetToken) {
        // TODO: Implement password reset email
        System.out.println("🔐 Password reset email feature - Coming soon!");
        return false;
    }

    /**
     * Validate email configuration
     * Checks if all required settings are configured
     */
    public static boolean isConfigured() {
        if (EmailConfig.SENDER_EMAIL.equals("your-email@gmail.com") ||
                EmailConfig.SENDER_PASSWORD.equals("your-app-password")) {

            System.err.println("❌ Email service not configured!");
            System.err.println("Please update EmailConfig.java with your SMTP credentials");
            return false;
        }
        return true;
    }

    /**
     * Test email configuration
     * Sends a test email to verify settings
     */
    public boolean testEmailConfiguration(String testRecipient) {
        if (!isConfigured()) {
            return false;
        }

        try {
            String testHtml = """
                <h2>ExamVerse Email Test</h2>
                <p>If you receive this email, your email configuration is working correctly!</p>
                <p><strong>SMTP Host:</strong> """ + EmailConfig.SMTP_HOST + """
                </p>
                <p><strong>Sender:</strong> """ + EmailConfig.SENDER_EMAIL + """
                </p>
                """;

            return sendEmail(testRecipient, "ExamVerse - Email Test", testHtml);

        } catch (Exception e) {
            System.err.println("❌ Email configuration test failed!");
            e.printStackTrace();
            return false;
        }
    }
}