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
 * UPDATED: Fixed admin dashboard navigation
 */
public class LoginController implements Initializable {

    @FXML
    private VBox rootPane;

    @FXML
    private ComboBox<String> roleComboBox;  // Role selection dropdown

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
        errorLabel.setVisible(false);

        // Populate ComboBox with role options
        roleComboBox.getItems().addAll("Student", "Admin/Teacher");

        // Set default role to "Student"
        roleComboBox.getSelectionModel().selectFirst();

        // Setup enter key listener
        passwordField.setOnAction(e -> handleLogin());
        usernameField.setOnAction(e -> passwordField.requestFocus());
    }

    /**
     * Handle login button click with role validation
     */
    @FXML
    private void handleLogin() {
        // Clear previous error
        errorLabel.setVisible(false);

        // Get selected role
        String selectedRole = roleComboBox.getValue();
        if (selectedRole == null || selectedRole.isEmpty()) {
            showError("Please select your role (Student or Admin)");
            roleComboBox.requestFocus();
            return;
        }

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
                showSuccess("Welcome Admin " + user.getFullName() + "!");

                // Navigate to ADMIN dashboard
                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                        javafx.application.Platform.runLater(() -> {
                            // FIXED: Navigate to admin-dashboard.fxml (NOT student-dashboard.fxml)
                            SceneManager.switchScene("/com/examverse/fxml/dashboard/admin-dashboard.fxml");
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                System.out.println("🔐 Student logged in - Navigating to student dashboard");
                showSuccess("Welcome " + user.getFullName() + "!");

                // Navigate to STUDENT dashboard
                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                        javafx.application.Platform.runLater(() -> {
                            SceneManager.switchScene("/com/examverse/fxml/dashboard/student-dashboard.fxml");
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            }

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
     * Handle forgot password
     */
    @FXML
    private void handleForgotPassword() {
        System.out.println("Navigate to forgot password page");
        SceneManager.switchScene("/com/examverse/fxml/auth/forgot-password.fxml");
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setStyle("-fx-text-fill: #ef4444;");
    }

    /**
     * Show success message
     */
    private void showSuccess(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setStyle("-fx-text-fill: #22d3ee;");
    }
}