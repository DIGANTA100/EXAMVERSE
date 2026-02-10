package com.examverse.controller.admin;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import com.examverse.model.exam.Exam;
import com.examverse.model.exam.Question;
import com.examverse.service.exam.QuestionService;

import java.util.List;

/**
 * QuestionManagerDialog - Interface for managing exam questions
 * Allows adding, editing, and viewing questions for an exam
 */
public class QuestionManagerDialog extends Dialog<ButtonType> {

    private Exam exam;
    private QuestionService questionService;

    // UI Components
    private VBox questionsListContainer;
    private Label questionCountLabel;
    private int currentQuestionCount = 0;

    public QuestionManagerDialog(Exam exam) {
        this.exam = exam;
        this.questionService = new QuestionService();

        setTitle("Manage Questions - " + exam.getExamTitle());
        setHeaderText("Add and manage questions for this exam");
        setResizable(true);

        // Set dialog size
        getDialogPane().setPrefSize(900, 700);

        // Style the dialog
        DialogPane dialogPane = getDialogPane();
        try {
            dialogPane.getStylesheets().add(
                    getClass().getResource("/com/examverse/css/student-dashboard.css").toExternalForm()
            );
            dialogPane.getStyleClass().add("alert");
        } catch (Exception e) {
            System.err.println("Could not load CSS: " + e.getMessage());
        }

        // Add buttons
        dialogPane.getButtonTypes().addAll(ButtonType.CLOSE);

        // Create content
        VBox content = createContent();
        dialogPane.setContent(content);

        // Load existing questions
        loadExistingQuestions();
    }

    private VBox createContent() {
        VBox mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(20));

        // Header section
        HBox headerSection = createHeaderSection();

        // Add Question button
        Button addQuestionBtn = new Button("➕ Add New Question");
        addQuestionBtn.getStyleClass().add("btn-primary");
        addQuestionBtn.setOnAction(e -> showAddQuestionDialog());
        addQuestionBtn.setPadding(new Insets(12, 25, 12, 25));

        // Questions list
        Label questionsLabel = new Label("Questions List");
        questionsLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        questionsLabel.setStyle("-fx-text-fill: white;");

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(450);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        questionsListContainer = new VBox(15);
        questionsListContainer.setPadding(new Insets(10));
        scrollPane.setContent(questionsListContainer);

        mainContainer.getChildren().addAll(headerSection, addQuestionBtn, questionsLabel, scrollPane);

        return mainContainer;
    }

    private HBox createHeaderSection() {
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(15));
        header.setStyle("-fx-background-color: rgba(30, 41, 59, 0.7); -fx-background-radius: 10;");

        VBox examInfo = new VBox(5);
        Label titleLabel = new Label(exam.getExamTitle());
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        titleLabel.setStyle("-fx-text-fill: white;");

        Label subjectLabel = new Label(exam.getSubject() + " • " + exam.getDifficulty());
        subjectLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");

        questionCountLabel = new Label("Questions: 0/" + exam.getTotalQuestions());
        questionCountLabel.setStyle("-fx-text-fill: #22d3ee; -fx-font-size: 14px; -fx-font-weight: bold;");

        examInfo.getChildren().addAll(titleLabel, subjectLabel, questionCountLabel);

        header.getChildren().add(examInfo);
        return header;
    }

    private void loadExistingQuestions() {
        questionsListContainer.getChildren().clear();

        List<Question> questions = questionService.getQuestionsByExamId(exam.getExamId());
        currentQuestionCount = questions.size();
        updateQuestionCount();

        if (questions.isEmpty()) {
            Label emptyLabel = new Label("No questions added yet. Click 'Add New Question' to get started!");
            emptyLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");
            questionsListContainer.getChildren().add(emptyLabel);
        } else {
            for (int i = 0; i < questions.size(); i++) {
                questionsListContainer.getChildren().add(createQuestionCard(questions.get(i), i + 1));
            }
        }
    }

    private VBox createQuestionCard(Question question, int questionNumber) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.7); " +
                        "-fx-background-radius: 10; " +
                        "-fx-border-color: rgba(51, 65, 85, 0.5); " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 10;"
        );

        // Question header
        HBox headerRow = new HBox(15);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label numberLabel = new Label("Q" + questionNumber);
        numberLabel.setStyle(
                "-fx-background-color: #22d3ee; " +
                        "-fx-text-fill: #0f172a; " +
                        "-fx-padding: 5 12; " +
                        "-fx-background-radius: 15; " +
                        "-fx-font-weight: bold;"
        );

        Label marksLabel = new Label(question.getMarks() + " marks");
        marksLabel.setStyle(
                "-fx-background-color: rgba(34, 197, 94, 0.2); " +
                        "-fx-text-fill: #22c55e; " +
                        "-fx-padding: 4 10; " +
                        "-fx-background-radius: 12; " +
                        "-fx-font-size: 12px;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button editBtn = new Button("✏️ Edit");
        editBtn.getStyleClass().add("btn-secondary");
        editBtn.setOnAction(e -> showEditQuestionDialog(question));

        Button deleteBtn = new Button("🗑️ Delete");
        deleteBtn.getStyleClass().add("btn-danger");
        deleteBtn.setOnAction(e -> deleteQuestion(question));

        headerRow.getChildren().addAll(numberLabel, marksLabel, spacer, editBtn, deleteBtn);

        // Question text
        Label questionLabel = new Label(question.getQuestionText());
        questionLabel.setWrapText(true);
        questionLabel.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: 500;");

        // Options
        VBox optionsBox = new VBox(8);
        optionsBox.setPadding(new Insets(10, 0, 0, 0));

        String[] options = {
                "A) " + question.getOptionA(),
                "B) " + question.getOptionB(),
                "C) " + question.getOptionC(),
                "D) " + question.getOptionD()
        };

        for (int i = 0; i < options.length; i++) {
            String optionLetter = String.valueOf((char) ('A' + i));
            Label optionLabel = new Label(options[i]);
            optionLabel.setWrapText(true);

            if (optionLetter.equals(question.getCorrectAnswer())) {
                optionLabel.setStyle(
                        "-fx-text-fill: #22c55e; " +
                                "-fx-font-size: 13px; " +
                                "-fx-font-weight: bold; " +
                                "-fx-padding: 8; " +
                                "-fx-background-color: rgba(34, 197, 94, 0.1); " +
                                "-fx-background-radius: 5;"
                );
            } else {
                optionLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px; -fx-padding: 8;");
            }

            optionsBox.getChildren().add(optionLabel);
        }

        card.getChildren().addAll(headerRow, questionLabel, optionsBox);

        return card;
    }

    private void showAddQuestionDialog() {
        Dialog<Question> dialog = new Dialog<>();
        dialog.setTitle("Add New Question");
        dialog.setHeaderText("Create a new MCQ question");

        DialogPane dialogPane = dialog.getDialogPane();
        try {
            dialogPane.getStylesheets().add(
                    getClass().getResource("/com/examverse/css/student-dashboard.css").toExternalForm()
            );
        } catch (Exception e) {
            System.err.println("Could not load CSS: " + e.getMessage());
        }

        ButtonType addButtonType = new ButtonType("Add Question", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        // Create form
        GridPane grid = createQuestionForm();

        // Get form fields
        TextArea questionField = (TextArea) grid.lookup("#questionField");
        TextField optionAField = (TextField) grid.lookup("#optionAField");
        TextField optionBField = (TextField) grid.lookup("#optionBField");
        TextField optionCField = (TextField) grid.lookup("#optionCField");
        TextField optionDField = (TextField) grid.lookup("#optionDField");
        ComboBox<String> correctAnswerBox = (ComboBox<String>) grid.lookup("#correctAnswerBox");
        TextField marksField = (TextField) grid.lookup("#marksField");
        TextArea explanationField = (TextArea) grid.lookup("#explanationField");

        dialog.getDialogPane().setContent(grid);
        dialog.setResizable(true);
        dialog.getDialogPane().setPrefSize(650, 600);

        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                try {
                    Question newQuestion = new Question();
                    newQuestion.setExamId(exam.getExamId());
                    newQuestion.setQuestionText(questionField.getText().trim());
                    newQuestion.setOptionA(optionAField.getText().trim());
                    newQuestion.setOptionB(optionBField.getText().trim());
                    newQuestion.setOptionC(optionCField.getText().trim());
                    newQuestion.setOptionD(optionDField.getText().trim());
                    newQuestion.setCorrectAnswer(correctAnswerBox.getValue());
                    newQuestion.setMarks(Integer.parseInt(marksField.getText().trim()));
                    newQuestion.setExplanation(explanationField.getText().trim());
                    return newQuestion;
                } catch (Exception e) {
                    showAlert("Error", "Please fill in all required fields correctly");
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(question -> {
            if (questionService.createQuestion(question)) {
                showAlert("Success", "Question added successfully!");
                loadExistingQuestions();
            } else {
                showAlert("Error", "Failed to add question");
            }
        });
    }

    private void showEditQuestionDialog(Question question) {
        Dialog<Question> dialog = new Dialog<>();
        dialog.setTitle("Edit Question");
        dialog.setHeaderText("Modify question details");

        DialogPane dialogPane = dialog.getDialogPane();
        try {
            dialogPane.getStylesheets().add(
                    getClass().getResource("/com/examverse/css/student-dashboard.css").toExternalForm()
            );
        } catch (Exception e) {
            System.err.println("Could not load CSS: " + e.getMessage());
        }

        ButtonType saveButtonType = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create form with existing data
        GridPane grid = createQuestionForm();

        // Populate fields with existing data
        TextArea questionField = (TextArea) grid.lookup("#questionField");
        TextField optionAField = (TextField) grid.lookup("#optionAField");
        TextField optionBField = (TextField) grid.lookup("#optionBField");
        TextField optionCField = (TextField) grid.lookup("#optionCField");
        TextField optionDField = (TextField) grid.lookup("#optionDField");
        ComboBox<String> correctAnswerBox = (ComboBox<String>) grid.lookup("#correctAnswerBox");
        TextField marksField = (TextField) grid.lookup("#marksField");
        TextArea explanationField = (TextArea) grid.lookup("#explanationField");

        questionField.setText(question.getQuestionText());
        optionAField.setText(question.getOptionA());
        optionBField.setText(question.getOptionB());
        optionCField.setText(question.getOptionC());
        optionDField.setText(question.getOptionD());
        correctAnswerBox.setValue(question.getCorrectAnswer());
        marksField.setText(String.valueOf(question.getMarks()));
        explanationField.setText(question.getExplanation() != null ? question.getExplanation() : "");

        dialog.getDialogPane().setContent(grid);
        dialog.setResizable(true);
        dialog.getDialogPane().setPrefSize(650, 600);

        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    question.setQuestionText(questionField.getText().trim());
                    question.setOptionA(optionAField.getText().trim());
                    question.setOptionB(optionBField.getText().trim());
                    question.setOptionC(optionCField.getText().trim());
                    question.setOptionD(optionDField.getText().trim());
                    question.setCorrectAnswer(correctAnswerBox.getValue());
                    question.setMarks(Integer.parseInt(marksField.getText().trim()));
                    question.setExplanation(explanationField.getText().trim());
                    return question;
                } catch (Exception e) {
                    showAlert("Error", "Please fill in all fields correctly");
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updatedQuestion -> {
            if (questionService.updateQuestion(updatedQuestion)) {
                showAlert("Success", "Question updated successfully!");
                loadExistingQuestions();
            } else {
                showAlert("Error", "Failed to update question");
            }
        });
    }

    private GridPane createQuestionForm() {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        int row = 0;

        // Question text
        Label questionLabel = new Label("Question:");
        questionLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        TextArea questionField = new TextArea();
        questionField.setId("questionField");
        questionField.setPromptText("Enter the question text...");
        questionField.setPrefRowCount(3);
        questionField.setWrapText(true);
        grid.add(questionLabel, 0, row);
        grid.add(questionField, 1, row);
        row++;

        // Option A
        Label optionALabel = new Label("Option A:");
        optionALabel.setStyle("-fx-text-fill: white;");
        TextField optionAField = new TextField();
        optionAField.setId("optionAField");
        optionAField.setPromptText("Enter option A");
        grid.add(optionALabel, 0, row);
        grid.add(optionAField, 1, row);
        row++;

        // Option B
        Label optionBLabel = new Label("Option B:");
        optionBLabel.setStyle("-fx-text-fill: white;");
        TextField optionBField = new TextField();
        optionBField.setId("optionBField");
        optionBField.setPromptText("Enter option B");
        grid.add(optionBLabel, 0, row);
        grid.add(optionBField, 1, row);
        row++;

        // Option C
        Label optionCLabel = new Label("Option C:");
        optionCLabel.setStyle("-fx-text-fill: white;");
        TextField optionCField = new TextField();
        optionCField.setId("optionCField");
        optionCField.setPromptText("Enter option C");
        grid.add(optionCLabel, 0, row);
        grid.add(optionCField, 1, row);
        row++;

        // Option D
        Label optionDLabel = new Label("Option D:");
        optionDLabel.setStyle("-fx-text-fill: white;");
        TextField optionDField = new TextField();
        optionDField.setId("optionDField");
        optionDField.setPromptText("Enter option D");
        grid.add(optionDLabel, 0, row);
        grid.add(optionDField, 1, row);
        row++;

        // Correct Answer
        Label correctAnswerLabel = new Label("Correct Answer:");
        correctAnswerLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        ComboBox<String> correctAnswerBox = new ComboBox<>();
        correctAnswerBox.setId("correctAnswerBox");
        correctAnswerBox.getItems().addAll("A", "B", "C", "D");
        correctAnswerBox.setValue("A");
        correctAnswerBox.setMaxWidth(Double.MAX_VALUE);
        grid.add(correctAnswerLabel, 0, row);
        grid.add(correctAnswerBox, 1, row);
        row++;

        // Marks
        Label marksLabel = new Label("Marks:");
        marksLabel.setStyle("-fx-text-fill: white;");
        TextField marksField = new TextField("1");
        marksField.setId("marksField");
        marksField.setPromptText("Enter marks (e.g., 1, 2, 5)");
        grid.add(marksLabel, 0, row);
        grid.add(marksField, 1, row);
        row++;

        // Explanation (optional)
        Label explanationLabel = new Label("Explanation (Optional):");
        explanationLabel.setStyle("-fx-text-fill: white;");
        TextArea explanationField = new TextArea();
        explanationField.setId("explanationField");
        explanationField.setPromptText("Add explanation for the correct answer (optional)");
        explanationField.setPrefRowCount(2);
        explanationField.setWrapText(true);
        grid.add(explanationLabel, 0, row);
        grid.add(explanationField, 1, row);

        return grid;
    }

    private void deleteQuestion(Question question) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Delete");
        confirmDialog.setHeaderText("Delete Question");
        confirmDialog.setContentText("Are you sure you want to delete this question?");

        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (questionService.deleteQuestion(question.getQuestionId())) {
                    showAlert("Success", "Question deleted successfully!");
                    loadExistingQuestions();
                } else {
                    showAlert("Error", "Failed to delete question");
                }
            }
        });
    }

    private void updateQuestionCount() {
        questionCountLabel.setText("Questions: " + currentQuestionCount + "/" + exam.getTotalQuestions());

        if (currentQuestionCount >= exam.getTotalQuestions()) {
            questionCountLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 14px; -fx-font-weight: bold;");
        } else {
            questionCountLabel.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 14px; -fx-font-weight: bold;");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}