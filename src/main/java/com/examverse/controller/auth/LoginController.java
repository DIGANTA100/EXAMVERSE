package com.examverse.controller.auth;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import com.examverse.model.user.User;
import com.examverse.service.auth.UserDAO;
import com.examverse.util.SceneManager;
import com.examverse.util.SessionManager;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * LoginController - Handles login screen logic
 */
public class LoginController implements Initializable {

    @FXML
    private VBox rootPane;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Button signupLinkButton;

    @FXML
    private Label errorLabel;

    @FXML
    private CheckBox rememberMeCheckbox;

    private UserDAO userDAO;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userDAO = new UserDAO();
        errorLabel.setVisible(false);

        // Setup enter key listener
        passwordField.setOnAction(e -> handleLogin());
        usernameField.setOnAction(e -> passwordField.requestFocus());
    }

    /**
     * Handle login button click
     */
    @FXML
    private void handleLogin() {
        // Clear previous error
        errorLabel.setVisible(false);

        // Get input values
        String usernameOrEmail = usernameField.getText().trim();
        String password = passwordField.getText();

        // Validate inputs
        if (usernameOrEmail.isEmpty()) {
            showError("Please enter username or email");
            usernameField.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            showError("Please enter password");
            passwordField.requestFocus();
            return;
        }

        // Disable login button during authentication
        loginButton.setDisable(true);
        loginButton.setText("Logging in...");

        // Attempt login
        User user = userDAO.loginUser(usernameOrEmail, password);

        if (user != null) {
            // Login successful
            SessionManager.getInstance().setCurrentUser(user);

            // Navigate based on user type
            if (user.isAdmin()) {
                System.out.println("🔐 Admin logged in - Navigating to admin dashboard");
                // TODO: Navigate to admin dashboard when ready
                showSuccess("Welcome Admin " + user.getFullName() + "!");
                // SceneManager.switchScene("/com/examverse/fxml/admin/admin_dashboard.fxml");
            } else {
                System.out.println("🔐 Student logged in - Navigating to student dashboard");
                // TODO: Navigate to student dashboard when ready
                showSuccess("Welcome " + user.getFullName() + "!");
                // SceneManager.switchScene("/com/examverse/fxml/dashboard/student_dashboard.fxml");
            }

            // For now, go back to landing page (temporary)
            new Thread(() -> {
                try {
                    Thread.sleep(1500);
                    javafx.application.Platform.runLater(() -> {
                        SceneManager.switchScene("/com/examverse/fxml/dashboard/dashboard-landing.fxml");
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

        } else {
            // Login failed
            showError("Invalid username/email or password");
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
        SceneManager.switchScene("/com/examverse/fxml/auth/signup.fxml");
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
}