package com.examverse.controller.auth;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import com.examverse.service.auth.EmailService;
import com.examverse.service.auth.UserDAO;
import com.examverse.util.SceneManager;
import com.examverse.util.Validator;
import javafx.animation.FadeTransition;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * SignupController - Handles signup/registration screen logic
 * IMPROVED: Added field reset, better validation messages, and smooth transitions
 */
public class SignupController implements Initializable {

    @FXML
    private VBox rootPane;

    @FXML
    private ComboBox<String> roleComboBox;

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

        // Reset all fields when the page loads
        resetFields();

        // Hide error label initially
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // Populate role ComboBox
        roleComboBox.getItems().addAll("Student", "Admin/Teacher");

        // Set default to Student
        roleComboBox.getSelectionModel().selectFirst();

        // Setup enter key listeners
        confirmPasswordField.setOnAction(e -> handleSignup());

        // Add fade-in animation
        applyFadeInAnimation();
    }

    /**
     * Reset all form fields
     */
    private void resetFields() {
        if (roleComboBox != null) {
            roleComboBox.getSelectionModel().selectFirst();
        }
        if (fullNameField != null) fullNameField.clear();
        if (usernameField != null) usernameField.clear();
        if (emailField != null) emailField.clear();
        if (passwordField != null) passwordField.clear();
        if (confirmPasswordField != null) confirmPasswordField.clear();
        if (termsCheckbox != null) termsCheckbox.setSelected(false);
        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
        if (signupButton != null) {
            signupButton.setDisable(false);
            signupButton.setText("Sign Up");
        }
    }

    /**
     * Apply fade-in animation to the root pane
     */
    private void applyFadeInAnimation() {
        if (rootPane != null) {
            FadeTransition fade = new FadeTransition(Duration.millis(400), rootPane);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            fade.play();
        }
    }

    /**
     * Handle signup button click
     */
    @FXML
    private void handleSignup() {
        // Clear previous messages
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // Get selected role
        String selectedRole = roleComboBox.getValue();
        if (selectedRole == null || selectedRole.isEmpty()) {
            showError("❌ Please select your role (Student or Admin)");
            roleComboBox.requestFocus();
            return;
        }

        // Determine if signing up as admin
        boolean isAdminSignup = selectedRole.equals("Admin/Teacher");

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
            showError("❌ Please accept the Terms & Conditions to continue");
            termsCheckbox.requestFocus();
            return;
        }

        // Disable signup button during registration
        signupButton.setDisable(true);
        signupButton.setText("Creating account...");

        // Check if username already exists
        if (userDAO.usernameExists(username)) {
            showError("❌ Username already taken. Please choose another one.");
            signupButton.setDisable(false);
            signupButton.setText("Sign Up");
            usernameField.requestFocus();
            return;
        }

        // Check if email already exists
        if (userDAO.emailExists(email)) {
            showError("❌ Email already registered. Please log in instead.");
            signupButton.setDisable(false);
            signupButton.setText("Sign Up");
            emailField.requestFocus();
            return;
        }

        // Perform registration in a background thread
        new Thread(() -> {
            try {
                // Attempt registration with role
                boolean registrationSuccess = userDAO.registerUser(username, email, password, fullName, isAdminSignup);

                if (registrationSuccess) {
                    // Update UI on JavaFX thread
                    javafx.application.Platform.runLater(() -> {
                        signupButton.setText("Sending confirmation email...");
                    });

                    // Send welcome email
                    String roleText = isAdminSignup ? "Admin/Teacher" : "Student";
                    boolean emailSent = emailService.sendWelcomeEmail(email, fullName, username);

                    // Update UI based on results
                    javafx.application.Platform.runLater(() -> {
                        if (emailSent) {
                            showSuccess("✅ " + roleText + " account created! Check your email for confirmation.");
                        } else {
                            showWarning("⚠️ " + roleText + " account created, but email notification failed.");
                        }

                        // Redirect to login after delay with fade transition
                        new Thread(() -> {
                            try {
                                Thread.sleep(1500);
                                javafx.application.Platform.runLater(() -> {
                                    applyFadeOutTransition(() -> {
                                        SceneManager.switchScene("/com/examverse/fxml/auth/login.fxml");
                                    });
                                });
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }).start();
                    });

                } else {
                    // Registration failed
                    javafx.application.Platform.runLater(() -> {
                        showError("❌ Registration failed. Please try again.");
                        signupButton.setDisable(false);
                        signupButton.setText("Sign Up");
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    showError("❌ An error occurred. Please try again.");
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
            return "❌ Please enter your full name";
        }

        if (fullName.length() < 3) {
            fullNameField.requestFocus();
            return "❌ Full name must be at least 3 characters long";
        }

        // Username validation
        if (username.isEmpty()) {
            usernameField.requestFocus();
            return "❌ Please enter a username";
        }

        if (!Validator.isValidUsername(username)) {
            usernameField.requestFocus();
            return "❌ Username must be 3-20 characters (letters, numbers, underscore only)";
        }

        // Email validation
        if (email.isEmpty()) {
            emailField.requestFocus();
            return "❌ Please enter your email address";
        }

        if (!Validator.isValidEmail(email)) {
            emailField.requestFocus();
            return "❌ Please enter a valid email address";
        }

        // Password validation
        if (password.isEmpty()) {
            passwordField.requestFocus();
            return "❌ Please enter a password";
        }

        if (password.length() < 6) {
            passwordField.requestFocus();
            return "❌ Password must be at least 6 characters long";
        }

        // Confirm password validation
        if (confirmPassword.isEmpty()) {
            confirmPasswordField.requestFocus();
            return "❌ Please confirm your password";
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordField.requestFocus();
            confirmPasswordField.clear();
            return "❌ Passwords do not match. Please try again.";
        }

        return null; // All validations passed
    }

    /**
     * Handle Terms & Conditions link click — opens the terms page
     */
    @FXML
    private void handleTermsLink() {
        System.out.println("Navigate to terms and conditions page");
        applyFadeOutTransition(() -> {
            SceneManager.switchScene("/com/examverse/fxml/auth/terms-and-conditions.fxml");
        });
    }

    /**
     * Handle login link click
     */
    @FXML
    private void handleLoginLink() {
        System.out.println("Navigate to login page");
        applyFadeOutTransition(() -> {
            SceneManager.switchScene("/com/examverse/fxml/auth/login.fxml");
        });
    }

    /**
     * Handle back button
     */
    @FXML
    private void handleBack() {
        System.out.println("Back to landing page");
        applyFadeOutTransition(() -> {
            SceneManager.switchScene("/com/examverse/fxml/dashboard/dashboard-landing.fxml");
        });
    }

    /**
     * Apply fade-out transition before scene change
     */
    private void applyFadeOutTransition(Runnable onFinished) {
        if (rootPane != null) {
            FadeTransition fade = new FadeTransition(Duration.millis(300), rootPane);
            fade.setFromValue(1.0);
            fade.setToValue(0.0);
            fade.setOnFinished(e -> onFinished.run());
            fade.play();
        } else {
            onFinished.run();
        }
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        errorLabel.setStyle("-fx-text-fill: #ef4444;");
    }

    /**
     * Show success message
     */
    private void showSuccess(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        errorLabel.setStyle("-fx-text-fill: #22d3ee;");
    }

    /**
     * Show warning message
     */
    private void showWarning(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        errorLabel.setStyle("-fx-text-fill: #f59e0b;");
    }
}