package com.examverse.controller.auth;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import com.examverse.model.user.User;
import com.examverse.service.auth.UserDAO;
import com.examverse.util.SceneManager;
import com.examverse.util.SessionManager;
import javafx.animation.FadeTransition;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * LoginController - Handles login screen logic
 * IMPROVED: Added field reset, better error messages, and smooth transitions
 */
public class LoginController implements Initializable {

    @FXML
    private VBox rootPane;

    @FXML
    private ComboBox<String> roleComboBox;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Label errorLabel;

    @FXML
    private CheckBox rememberMeCheckbox;

    private UserDAO userDAO;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userDAO = new UserDAO();

        // Reset all fields when the page loads
        resetFields();

        // Hide error label initially
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // Populate ComboBox with role options
        roleComboBox.getItems().addAll("Student", "Admin/Teacher");

        // Set default role to "Student"
        roleComboBox.getSelectionModel().selectFirst();

        // Setup enter key listener
        passwordField.setOnAction(e -> handleLogin());
        usernameField.setOnAction(e -> passwordField.requestFocus());

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
        if (usernameField != null) usernameField.clear();
        if (passwordField != null) passwordField.clear();
        if (rememberMeCheckbox != null) rememberMeCheckbox.setSelected(false);
        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
        if (loginButton != null) {
            loginButton.setDisable(false);
            loginButton.setText("Log In");
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
     * Handle login button click with role validation
     */
    @FXML
    private void handleLogin() {
        // Clear previous error
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // Get selected role
        String selectedRole = roleComboBox.getValue();
        if (selectedRole == null || selectedRole.isEmpty()) {
            showError("❌ Please select your role (Student or Admin)");
            roleComboBox.requestFocus();
            return;
        }

        // Get input values
        String usernameOrEmail = usernameField.getText().trim();
        String password = passwordField.getText();

        // Validate inputs
        if (usernameOrEmail.isEmpty()) {
            showError("❌ Please enter your username or email");
            usernameField.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            showError("❌ Please enter your password");
            passwordField.requestFocus();
            return;
        }

        // Disable login button during authentication
        loginButton.setDisable(true);
        loginButton.setText("Logging in...");

        // Attempt login
        User user = userDAO.loginUser(usernameOrEmail, password);

        if (user != null) {
            // Check if user role matches selected role
            boolean isAdminLogin = selectedRole.equals("Admin/Teacher");
            boolean isUserAdmin = user.isAdmin();

            if (isAdminLogin && !isUserAdmin) {
                // User selected Admin but account is Student
                showError("❌ This account is not an Admin account. Please select 'Student' role.");
                loginButton.setDisable(false);
                loginButton.setText("Log In");
                return;
            } else if (!isAdminLogin && isUserAdmin) {
                // User selected Student but account is Admin
                showError("❌ This is an Admin account. Please select 'Admin/Teacher' role.");
                loginButton.setDisable(false);
                loginButton.setText("Log In");
                return;
            }

            // Role matches - Login successful
            SessionManager.getInstance().setCurrentUser(user);

            // Navigate based on user type
            if (user.isAdmin()) {
                System.out.println("🔐 Admin logged in - Navigating to admin dashboard");
                showSuccess("✅ Welcome Admin " + user.getFullName() + "!");

                // Navigate to ADMIN dashboard with fade transition
                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                        javafx.application.Platform.runLater(() -> {
                            applyFadeOutTransition(() -> {
                                SceneManager.switchScene("/com/examverse/fxml/dashboard/admin-dashboard.fxml");
                            });
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                System.out.println("🔐 Student logged in - Navigating to student dashboard");
                showSuccess("✅ Welcome " + user.getFullName() + "!");

                // Navigate to STUDENT dashboard with fade transition
                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                        javafx.application.Platform.runLater(() -> {
                            applyFadeOutTransition(() -> {
                                SceneManager.switchScene("/com/examverse/fxml/dashboard/student-dashboard.fxml");
                            });
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            }

        } else {
            // Login failed
            showError("❌ Invalid username/email or password. Please try again.");
            loginButton.setDisable(false);
            loginButton.setText("Log In");
            passwordField.clear();
            passwordField.requestFocus();
        }
    }

    /**
     * Handle signup link click
     */
    @FXML
    private void handleSignupLink() {
        System.out.println("Navigate to signup page");
        applyFadeOutTransition(() -> {
            SceneManager.switchScene("/com/examverse/fxml/auth/signup.fxml");
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
     * Handle forgot password
     */
    @FXML
    private void handleForgotPassword() {
        System.out.println("Navigate to forgot password page");
        applyFadeOutTransition(() -> {
            SceneManager.switchScene("/com/examverse/fxml/auth/forgot-password.fxml");
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
}