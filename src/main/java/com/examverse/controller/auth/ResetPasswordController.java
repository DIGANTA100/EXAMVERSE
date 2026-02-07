package com.examverse.controller.auth;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import com.examverse.model.user.User;
import com.examverse.service.auth.EmailService;
import com.examverse.service.auth.PasswordResetService;
import com.examverse.service.auth.UserDAO;
import com.examverse.util.SceneManager;
import com.examverse.util.Validator;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * ResetPasswordController - Handles password reset screen
 * Validates reset code and updates password
 */
public class ResetPasswordController implements Initializable {

    @FXML
    private TextField codeField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Button resetButton;

    @FXML
    private Button resendCodeButton;

    @FXML
    private Label messageLabel;

    @FXML
    private Label emailLabel;

    @FXML
    private Label timerLabel;

    private UserDAO userDAO;
    private EmailService emailService;
    private PasswordResetService resetService;

    // Static variable to pass email between screens
    private static String userEmail = "";

    // Timer for countdown
    private Timeline countdown;
    private int remainingSeconds = 600; // 10 minutes

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userDAO = new UserDAO();
        emailService = EmailService.getInstance();
        resetService = PasswordResetService.getInstance();

        // Display masked email
        if (!userEmail.isEmpty()) {
            emailLabel.setText("Code sent to " + maskEmail(userEmail));
        }

        // Setup enter key listener
        confirmPasswordField.setOnAction(e -> handleResetPassword());

        // Start countdown timer
        startCountdown();

        // Limit code field to 6 digits
        codeField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                codeField.setText(newValue.replaceAll("[^\\d]", ""));
            }
            if (newValue.length() > 6) {
                codeField.setText(newValue.substring(0, 6));
            }
        });
    }

    /**
     * Set user email (called from ForgotPasswordController)
     */
    public static void setUserEmail(String email) {
        userEmail = email;
    }

    /**
     * Handle reset password button
     */
    @FXML
    private void handleResetPassword() {
        // Clear previous messages
        messageLabel.setVisible(false);

        // Get input values
        String code = codeField.getText().trim();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Validate inputs
        if (code.isEmpty()) {
            showError("Please enter the verification code");
            codeField.requestFocus();
            return;
        }

        if (code.length() != 6) {
            showError("Verification code must be 6 digits");
            codeField.requestFocus();
            return;
        }

        if (newPassword.isEmpty()) {
            showError("Please enter a new password");
            newPasswordField.requestFocus();
            return;
        }

        if (!Validator.isValidPassword(newPassword)) {
            showError("Password must be at least 6 characters");
            newPasswordField.requestFocus();
            return;
        }

        if (confirmPassword.isEmpty()) {
            showError("Please confirm your new password");
            confirmPasswordField.requestFocus();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showError("Passwords do not match");
            confirmPasswordField.clear();
            confirmPasswordField.requestFocus();
            return;
        }

        // Disable button during processing
        resetButton.setDisable(true);
        resetButton.setText("Resetting...");

        // Validate reset code
        boolean isValidCode = resetService.validateResetCode(userEmail, code);

        if (!isValidCode) {
            showError("Invalid or expired verification code");
            resetButton.setDisable(false);
            resetButton.setText("Reset Password");
            codeField.clear();
            codeField.requestFocus();
            return;
        }

        // Update password in database
        boolean passwordUpdated = userDAO.updatePassword(userEmail, newPassword);

        if (passwordUpdated) {
            // Remove reset code
            resetService.removeResetCode(userEmail);

            // Stop timer
            if (countdown != null) {
                countdown.stop();
            }

            // Show success message
            showSuccess("Password reset successful! Redirecting to login...");

            // Navigate to login after 2 seconds
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    javafx.application.Platform.runLater(() -> {
                        SceneManager.switchScene("/com/examverse/fxml/auth/login.fxml");
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

        } else {
            showError("Failed to reset password. Please try again.");
            resetButton.setDisable(false);
            resetButton.setText("Reset Password");
        }
    }

    /**
     * Handle resend code button
     */
    @FXML
    private void handleResendCode() {
        // Disable button
        resendCodeButton.setDisable(true);
        resendCodeButton.setText("Sending...");

        // Get user info
        User user = userDAO.getUserByEmail(userEmail);

        if (user != null) {
            // Generate new code
            String newCode = resetService.generateResetCode(userEmail);

            // Send email
            boolean emailSent = emailService.sendPasswordResetEmail(
                    userEmail,
                    user.getFullName(),
                    newCode
            );

            if (emailSent) {
                showSuccess("New verification code sent!");

                // Reset timer
                remainingSeconds = 600;
                startCountdown();

                // Re-enable button after delay
                new Thread(() -> {
                    try {
                        Thread.sleep(3000);
                        javafx.application.Platform.runLater(() -> {
                            resendCodeButton.setDisable(false);
                            resendCodeButton.setText("Resend");
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                showError("Failed to resend code");
                resendCodeButton.setDisable(false);
                resendCodeButton.setText("Resend");
            }
        }
    }

    /**
     * Handle back to login button
     */
    @FXML
    private void handleBackToLogin() {
        if (countdown != null) {
            countdown.stop();
        }
        SceneManager.switchScene("/com/examverse/fxml/auth/login.fxml");
    }

    /**
     * Start countdown timer
     */
    private void startCountdown() {
        if (countdown != null) {
            countdown.stop();
        }

        countdown = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            remainingSeconds--;

            int minutes = remainingSeconds / 60;
            int seconds = remainingSeconds % 60;

            timerLabel.setText(String.format("⏱️ %d:%02d", minutes, seconds));

            if (remainingSeconds <= 60) {
                // Last minute - change color to red
                timerLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #ef4444; -fx-font-weight: 600;");
            }

            if (remainingSeconds <= 0) {
                countdown.stop();
                timerLabel.setText("⏱️ Expired");
                showError("Verification code expired. Please request a new one.");
                resetButton.setDisable(true);
            }
        }));

        countdown.setCycleCount(Timeline.INDEFINITE);
        countdown.play();
    }

    /**
     * Mask email for privacy (show only first char and domain)
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }

        String[] parts = email.split("@");
        String username = parts[0];
        String domain = parts[1];

        if (username.length() <= 2) {
            return username.charAt(0) + "***@" + domain;
        }

        return username.charAt(0) + "***" + username.charAt(username.length() - 1) + "@" + domain;
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