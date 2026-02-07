package com.examverse.controller.auth;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import com.examverse.model.user.User;
import com.examverse.service.auth.EmailService;
import com.examverse.service.auth.PasswordResetService;
import com.examverse.service.auth.UserDAO;
import com.examverse.util.SceneManager;
import com.examverse.util.Validator;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * ForgotPasswordController - Handles forgot password screen
 * Allows users to request a password reset code via email
 */
public class ForgotPasswordController implements Initializable {

    @FXML
    private TextField emailField;

    @FXML
    private Button sendCodeButton;

    @FXML
    private Button backToLoginButton;

    @FXML
    private Label messageLabel;

    private UserDAO userDAO;
    private EmailService emailService;
    private PasswordResetService resetService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userDAO = new UserDAO();
        emailService = EmailService.getInstance();
        resetService = PasswordResetService.getInstance();

        // Setup enter key listener
        emailField.setOnAction(e -> handleSendCode());
    }

    /**
     * Handle send verification code button
     */
    @FXML
    private void handleSendCode() {
        // Clear previous messages
        messageLabel.setVisible(false);

        // Get email input
        String email = emailField.getText().trim();

        // Validate email
        if (email.isEmpty()) {
            showError("Please enter your email address");
            emailField.requestFocus();
            return;
        }

        if (!Validator.isValidEmail(email)) {
            showError("Please enter a valid email address");
            emailField.requestFocus();
            return;
        }

        // Disable button during processing
        sendCodeButton.setDisable(true);
        sendCodeButton.setText("Sending...");

        // Check if user exists
        User user = userDAO.getUserByEmail(email);

        // SECURITY: Always show the same message regardless of whether email exists
        // This prevents attackers from discovering which emails are registered
        if (user != null) {
            // User exists - generate and send code
            String resetCode = resetService.generateResetCode(email);

            // Send email with reset code
            boolean emailSent = emailService.sendPasswordResetEmail(
                    email,
                    user.getFullName(),
                    resetCode
            );

            if (emailSent) {
                showSuccess("Verification code sent! Check your email.");

                // Navigate to reset password screen after 1.5 seconds
                new Thread(() -> {
                    try {
                        Thread.sleep(1500);
                        javafx.application.Platform.runLater(() -> {
                            // Pass email to reset password screen
                            ResetPasswordController.setUserEmail(email);
                            SceneManager.switchScene("/com/examverse/fxml/auth/reset-password.fxml");
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                showError("Failed to send email. Please try again later.");
                sendCodeButton.setDisable(false);
                sendCodeButton.setText("Send Verification Code");
            }
        } else {
            // User doesn't exist - but show the same message for security
            System.out.println("⚠️ Password reset attempt for non-existent email: " + email);
            showSuccess("If an account exists with this email, a verification code has been sent.");

            // Re-enable button after delay
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    javafx.application.Platform.runLater(() -> {
                        sendCodeButton.setDisable(false);
                        sendCodeButton.setText("Send Verification Code");
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    /**
     * Handle back to login button
     */
    @FXML
    private void handleBackToLogin() {
        System.out.println("Back to login page");
        SceneManager.switchScene("/com/examverse/fxml/auth/login.fxml");
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        messageLabel.setText("❌ " + message);
        messageLabel.setVisible(true);
        messageLabel.setStyle("-fx-text-fill: #ef4444;");
    }

    /**
     * Show success message
     */
    private void showSuccess(String message) {
        messageLabel.setText("✅ " + message);
        messageLabel.setVisible(true);
        messageLabel.setStyle("-fx-text-fill: #22d3ee;");
    }
}