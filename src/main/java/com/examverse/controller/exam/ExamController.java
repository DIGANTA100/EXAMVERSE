package com.examverse.controller.exam;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import com.examverse.model.exam.Answer;
import com.examverse.model.exam.Exam;
import com.examverse.model.exam.Question;
import com.examverse.model.user.User;
import com.examverse.service.exam.AnswerService;
import com.examverse.service.exam.EvaluationService;
import com.examverse.service.exam.ExamService;
import com.examverse.service.exam.QuestionService;
import com.examverse.util.SceneManager;
import com.examverse.util.SessionManager;

import java.net.URL;
import java.util.*;

/**
 * ExamController - Controls the exam taking interface
 * Handles questions display, timer, navigation, and submission
 */
public class ExamController implements Initializable {

    @FXML private BorderPane rootPane;
    @FXML private Label examTitleLabel;
    @FXML private Label timerLabel;
    @FXML private Label questionCounterLabel;
    @FXML private ProgressBar progressBar;

    @FXML private Label questionNumberLabel;
    @FXML private Label questionTextLabel;
    @FXML private RadioButton optionA, optionB, optionC, optionD;
    @FXML private ToggleGroup optionsGroup;

    @FXML private Button previousBtn, nextBtn, submitBtn;
    @FXML private FlowPane questionPalettePane;

    // Data
    private int attemptId;
    private Exam exam;
    private List<Question> questions;
    private Map<Integer, String> studentAnswers; // questionId -> selectedAnswer
    private int currentQuestionIndex = 0;
    private User currentUser;

    // Services
    private ExamService examService;
    private QuestionService questionService;
    private AnswerService answerService;
    private EvaluationService evaluationService;

    // Timer
    private Timeline timeline;
    private int remainingSeconds;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize services
        examService = new ExamService();
        questionService = new QuestionService();
        answerService = new AnswerService();
        evaluationService = new EvaluationService();

        studentAnswers = new HashMap<>();
        currentUser = SessionManager.getInstance().getCurrentUser();

        // Get exam data from session (passed from StudentDashboardController)
        attemptId = (int) SessionManager.getInstance().getAttribute("attemptId");
        int examId = (int) SessionManager.getInstance().getAttribute("examId");

        // Load exam and questions
        exam = examService.getExamById(examId);
        questions = questionService.getQuestionsByExamId(examId);

        if (exam == null || questions.isEmpty()) {
            showError("Failed to load exam data");
            return;
        }

        // Setup UI
        setupExamHeader();
        setupQuestionPalette();
        startTimer();

        // Load first question
        loadQuestion(0);

        System.out.println("✅ Exam started - Attempt ID: " + attemptId);
    }

    /**
     * Setup exam header with title and info
     */
    private void setupExamHeader() {
        examTitleLabel.setText(exam.getExamTitle());
        updateQuestionCounter();
        progressBar.setProgress(0.0);
    }

    /**
     * Setup question navigation palette
     */
    private void setupQuestionPalette() {
        questionPalettePane.getChildren().clear();
        questionPalettePane.setHgap(8);
        questionPalettePane.setVgap(8);

        for (int i = 0; i < questions.size(); i++) {
            final int questionIndex = i;
            Button btn = new Button(String.valueOf(i + 1));
            btn.setPrefSize(40, 40);
            btn.getStyleClass().add("palette-btn");
            btn.setOnAction(e -> loadQuestion(questionIndex));
            questionPalettePane.getChildren().add(btn);
        }
    }

    /**
     * Start countdown timer
     */
    private void startTimer() {
        remainingSeconds = exam.getDurationMinutes() * 60;

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remainingSeconds--;
            updateTimerDisplay();

            if (remainingSeconds <= 0) {
                timeline.stop();
                autoSubmitExam();
            }
        }));

        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    /**
     * Update timer display
     */
    private void updateTimerDisplay() {
        int hours = remainingSeconds / 3600;
        int minutes = (remainingSeconds % 3600) / 60;
        int seconds = remainingSeconds % 60;

        String timeText = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        timerLabel.setText("⏱️ " + timeText);

        // Change color when time is running out
        if (remainingSeconds <= 300) { // Last 5 minutes
            timerLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
        } else if (remainingSeconds <= 600) { // Last 10 minutes
            timerLabel.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
        }
    }

    /**
     * Load a question by index
     */
    private void loadQuestion(int index) {
        if (index < 0 || index >= questions.size()) return;

        // Save current answer before loading new question
        if (currentQuestionIndex >= 0) {
            saveCurrentAnswer();
        }

        currentQuestionIndex = index;
        Question question = questions.get(index);

        // Update question display
        questionNumberLabel.setText("Question " + (index + 1));
        questionTextLabel.setText(question.getQuestionText());
        questionTextLabel.setWrapText(true);

        // Set options
        optionA.setText("A) " + question.getOptionA());
        optionB.setText("B) " + question.getOptionB());
        optionC.setText("C) " + question.getOptionC());
        optionD.setText("D) " + question.getOptionD());

        // Load previously selected answer
        String savedAnswer = studentAnswers.get(question.getQuestionId());
        optionsGroup.selectToggle(null);

        if (savedAnswer != null) {
            switch (savedAnswer.toUpperCase()) {
                case "A": optionA.setSelected(true); break;
                case "B": optionB.setSelected(true); break;
                case "C": optionC.setSelected(true); break;
                case "D": optionD.setSelected(true); break;
            }
        }

        // Update navigation buttons
        previousBtn.setDisable(index == 0);
        nextBtn.setText(index == questions.size() - 1 ? "Review" : "Next →");

        // Update counter and progress
        updateQuestionCounter();
        updatePaletteButtons();
    }

    /**
     * Save current answer
     */
    private void saveCurrentAnswer() {
        Question question = questions.get(currentQuestionIndex);
        RadioButton selected = (RadioButton) optionsGroup.getSelectedToggle();

        if (selected != null) {
            String answer = selected.getText().substring(0, 1); // Get A, B, C, or D
            studentAnswers.put(question.getQuestionId(), answer);

            // Save to database immediately
            Answer answerObj = new Answer(attemptId, question.getQuestionId(), answer);
            answerService.saveAnswer(answerObj);
        }
    }

    /**
     * Update question counter
     */
    private void updateQuestionCounter() {
        int answered = studentAnswers.size();
        questionCounterLabel.setText("Progress: " + answered + "/" + questions.size());
        progressBar.setProgress((double) answered / questions.size());
    }

    /**
     * Update question palette button colors
     */
    private void updatePaletteButtons() {
        for (int i = 0; i < questionPalettePane.getChildren().size(); i++) {
            Button btn = (Button) questionPalettePane.getChildren().get(i);
            Question q = questions.get(i);

            btn.getStyleClass().removeAll("palette-btn-answered", "palette-btn-current");

            if (i == currentQuestionIndex) {
                btn.getStyleClass().add("palette-btn-current");
            } else if (studentAnswers.containsKey(q.getQuestionId())) {
                btn.getStyleClass().add("palette-btn-answered");
            }
        }
    }

    /**
     * Handle previous button
     */
    @FXML
    private void handlePrevious() {
        if (currentQuestionIndex > 0) {
            loadQuestion(currentQuestionIndex - 1);
        }
    }

    /**
     * Handle next button
     */
    @FXML
    private void handleNext() {
        if (currentQuestionIndex < questions.size() - 1) {
            loadQuestion(currentQuestionIndex + 1);
        }
    }

    /**
     * Handle submit exam
     */
    @FXML
    private void handleSubmit() {
        // Save current answer first
        saveCurrentAnswer();

        int unanswered = questions.size() - studentAnswers.size();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Submit Exam");
        confirm.setHeaderText("Are you sure you want to submit?");

        String message = String.format(
                "Answered: %d/%d\nUnanswered: %d\n\n" +
                        "You cannot change answers after submission!",
                studentAnswers.size(), questions.size(), unanswered
        );

        confirm.setContentText(message);

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                submitExam();
            }
        });
    }

    /**
     * Submit exam and calculate results
     */
    private void submitExam() {
        // Stop timer
        if (timeline != null) {
            timeline.stop();
        }

        System.out.println("📝 Submitting exam - Attempt ID: " + attemptId);

        // Evaluate exam
        boolean success = evaluationService.evaluateExam(attemptId);

        if (success) {
            System.out.println("✅ Exam evaluated successfully");

            // Navigate to results
            SessionManager.getInstance().setAttribute("attemptId", attemptId);
            SceneManager.switchScene("/com/examverse/fxml/exam/exam-result.fxml");
        } else {
            showError("Failed to evaluate exam. Please contact administrator.");
        }
    }

    /**
     * Auto-submit when time runs out
     */
    private void autoSubmitExam() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Time's Up!");
            alert.setHeaderText("Exam time has ended");
            alert.setContentText("Your exam will be submitted automatically.");
            alert.show();

            submitExam();
        });
    }

    /**
     * Show error alert
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();

        // Return to dashboard
        SceneManager.switchScene("/com/examverse/fxml/dashboard/student-dashboard.fxml");
    }
}