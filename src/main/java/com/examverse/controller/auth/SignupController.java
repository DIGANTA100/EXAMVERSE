package com.examverse.controller.auth;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import com.examverse.service.auth.EmailService;
import com.examverse.service.auth.UserDAO;
import com.examverse.util.SceneManager;
import com.examverse.util.Validator;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * SignupController - Handles signup/registration screen logic
 * Updated with email notification feature
 */
public class SignupController implements Initializable {

    @FXML
    private VBox rootPane;

    @FXML
    private TextField fullNameField;

    @FXML
    private TextField usernameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Button signupButton;

    @FXML
    private Button loginLinkButton;

    @FXML
    private Label errorLabel;

    @FXML
    private CheckBox termsCheckbox;

    private UserDAO userDAO;
    private EmailService emailService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userDAO = new UserDAO();
        emailService = EmailService.getInstance();
        errorLabel.setVisible(false);

        // Setup enter key listeners
        confirmPasswordField.setOnAction(e -> handleSignup());
    }

    /**
     * Handle signup button click
     */
    @FXML
    private void handleSignup() {
        // Clear previous messages
        errorLabel.setVisible(false);

        // Get input values
        String fullName = fullNameField.getText().trim();
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Validate all inputs
        String validationError = validateInputs(fullName, username, email, password, confirmPassword);

        if (validationError != null) {
            showError(validationError);
            return;
        }

        // Check terms acceptance
        if (!termsCheckbox.isSelected()) {
            showError("Please accept the Terms & Conditions");
            return;
        }

        // Disable signup button during registration
        signupButton.setDisable(true);
        signupButton.setText("Creating account...");

        // Check if username already exists
        if (userDAO.usernameExists(username)) {
            showError("Username already taken. Please choose another.");
            signupButton.setDisable(false);
            signupButton.setText("Sign Up");
            usernameField.requestFocus();
            return;
        }

        // Check if email already exists
        if (userDAO.emailExists(email)) {
            showError("Email already registered. Please log in instead.");
            signupButton.setDisable(false);
            signupButton.setText("Sign Up");
            emailField.requestFocus();
            return;
        }

        // Perform registration in a background thread
        new Thread(() -> {
            try {
                // Attempt registration
                boolean registrationSuccess = userDAO.registerUser(username, email, password, fullName);

                if (registrationSuccess) {
                    // Update UI on JavaFX thread
                    javafx.application.Platform.runLater(() -> {
                        signupButton.setText("Sending confirmation email...");
                    });

                    // Send welcome email
                    boolean emailSent = emailService.sendWelcomeEmail(email, fullName, username);

                    // Update UI based on results
                    javafx.application.Platform.runLater(() -> {
                        if (emailSent) {
                            showSuccess("Account created! Check your email for confirmation.");
                        } else {
                            showWarning("Account created, but email notification failed.");
                        }

                        // Redirect to login after delay
                        new Thread(() -> {
                            try {
                                Thread.sleep(3000);
                                javafx.application.Platform.runLater(() -> {
                                    SceneManager.switchScene("/com/examverse/fxml/auth/login.fxml");
                                });
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }).start();
                    });

                } else {
                    // Registration failed
                    javafx.application.Platform.runLater(() -> {
                        showError("Registration failed. Please try again.");
                        signupButton.setDisable(false);
                        signupButton.setText("Sign Up");
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    showError("An error occurred. Please try again.");
                    signupButton.setDisable(false);
                    signupButton.setText("Sign Up");
                });
            }
        }).start();
    }

    /**
     * Validate all input fields
     * @return error message if validation fails, null if all valid
     */
    private String validateInputs(String fullName, String username, String email, String password, String confirmPassword) {

        // Full Name validation
        if (fullName.isEmpty()) {
            fullNameField.requestFocus();
            return "Please enter your full name";
        }

        if (fullName.length() < 3) {
            fullNameField.requestFocus();
            return "Full name must be at least 3 characters";
        }

        // Username validation
        if (username.isEmpty()) {
            usernameField.requestFocus();
            return "Please enter a username";
        }

        if (!Validator.isValidUsername(username)) {
            usernameField.requestFocus();
            return "Username must be 3-20 characters (letters, numbers, underscore only)";
        }

        // Email validation
        if (email.isEmpty()) {
            emailField.requestFocus();
            return "Please enter your email";
        }

        if (!Validator.isValidEmail(email)) {
            emailField.requestFocus();
            return "Please enter a valid email address";
        }

        // Password validation
        if (password.isEmpty()) {
            passwordField.requestFocus();
            return "Please enter a password";
        }

        if (password.length() < 6) {
            passwordField.requestFocus();
            return "Password must be at least 6 characters";
        }

        // Confirm password validation
        if (confirmPassword.isEmpty()) {
            confirmPasswordField.requestFocus();
            return "Please confirm your password";
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordField.requestFocus();
            confirmPasswordField.clear();
            return "Passwords do not match";
        }

        return null; // All validations passed
    }

    /**
     * Handle login link click
     */
    @FXML
    private void handleLoginLink() {
        System.out.println("Navigate to login page");
        SceneManager.switchScene("/com/examverse/fxml/auth/login.fxml");
    }

    /**
     * Handle back button
     */
    @FXML
    private void handleBack() {
        System.out.println("Back to landing page");
        SceneManager.switchScene("/com/examverse/fxml/dashboard/dashboard-landing.fxml");
    }

    /**
     * Handle forgot password (placeholder)
     */
    @FXML
    private void handleForgotPassword() {
        showError("Password recovery feature coming soon!");
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        errorLabel.setText("❌ " + message);
        errorLabel.setVisible(true);
        errorLabel.setStyle("-fx-text-fill: #ef4444;");
    }

    /**
     * Show success message
     */
    private void showSuccess(String message) {
        errorLabel.setText("✅ " + message);
        errorLabel.setVisible(true);
        errorLabel.setStyle("-fx-text-fill: #22d3ee;");
    }

    /**
     * Show warning message
     */
    private void showWarning(String message) {
        errorLabel.setText("⚠️ " + message);
        errorLabel.setVisible(true);
        errorLabel.setStyle("-fx-text-fill: #f59e0b;");
    }
}