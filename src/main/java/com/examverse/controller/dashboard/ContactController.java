package com.examverse.controller.dashboard;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import com.examverse.service.auth.EmailService;
import com.examverse.util.SceneManager;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * ContactController - Contact page with real email delivery.
 *
 * On submit, the message is sent via EmailService.sendContactMessageEmail()
 * to ajmainfayekdiganta@gmail.com using the same SMTP session already
 * configured in EmailConfig. The Reply-To header is set to the user's
 * email so hitting "Reply" in Gmail goes straight back to them.
 */
public class ContactController implements Initializable {

    @FXML private VBox rootPane;
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField subjectField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private TextArea messageArea;
    @FXML private Label statusLabel;
    @FXML private Button sendButton;

    private EmailService emailService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        emailService = EmailService.getInstance();

        categoryCombo.getItems().addAll(
                "General Inquiry",
                "Bug Report",
                "Feature Request",
                "Account Issue",
                "Exam Issue",
                "Other"
        );

        hideStatus();
        applyFadeInAnimation();
    }

    @FXML
    private void handleSend() {
        hideStatus();

        String name     = nameField.getText().trim();
        String email    = emailField.getText().trim();
        String subject  = subjectField.getText().trim();
        String category = categoryCombo.getValue();
        String message  = messageArea.getText().trim();

        if (name.isEmpty()) {
            showError("❌ Please enter your name.");
            nameField.requestFocus();
            return;
        }
        if (email.isEmpty() || !email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
            showError("❌ Please enter a valid email address.");
            emailField.requestFocus();
            return;
        }
        if (subject.isEmpty()) {
            showError("❌ Please enter a subject.");
            subjectField.requestFocus();
            return;
        }
        if (category == null) {
            showError("❌ Please select a category.");
            categoryCombo.requestFocus();
            return;
        }
        if (message.length() < 10) {
            showError("❌ Please enter a message (at least 10 characters).");
            messageArea.requestFocus();
            return;
        }

        sendButton.setDisable(true);
        sendButton.setText("Sending...");
        showInfo("⏳ Sending your message, please wait...");

        final String finalName     = name;
        final String finalEmail    = email;
        final String finalSubject  = subject;
        final String finalCategory = category;
        final String finalMessage  = message;

        new Thread(() -> {
            boolean success = emailService.sendContactMessageEmail(
                    finalName, finalEmail, finalSubject, finalCategory, finalMessage);

            javafx.application.Platform.runLater(() -> {
                sendButton.setDisable(false);
                sendButton.setText("Send Message →");

                if (success) {
                    showSuccess("✅ Message sent! We'll get back to you within 24–48 hours.");
                    clearForm();
                } else {
                    showError("❌ Delivery failed. Email us directly: ajmainfayekdiganta@gmail.com");
                }
            });
        }).start();
    }

    private void clearForm() {
        nameField.clear();
        emailField.clear();
        subjectField.clear();
        categoryCombo.getSelectionModel().clearSelection();
        messageArea.clear();
    }

    private void showError(String msg) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: #ef4444;");
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    private void showSuccess(String msg) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: #22d3ee;");
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    private void showInfo(String msg) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: #94a3b8;");
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    private void hideStatus() {
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
    }

    private void applyFadeInAnimation() {
        if (rootPane != null) {
            FadeTransition fade = new FadeTransition(Duration.millis(500), rootPane);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            fade.play();
        }
    }

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

    @FXML
    private void handleBack() {
        applyFadeOutTransition(() ->
                SceneManager.switchScene("/com/examverse/fxml/dashboard/dashboard-landing.fxml"));
    }
}