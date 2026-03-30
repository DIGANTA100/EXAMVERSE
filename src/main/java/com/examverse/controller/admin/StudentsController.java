package com.examverse.controller.admin;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import com.examverse.model.user.User;
import com.examverse.service.exam.StudentService;
import com.examverse.util.SceneManager;
import com.examverse.util.SessionManager;

import java.net.URL;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * StudentsController - Manages student accounts and views statistics
 * Features: View students, Filter, Search, View history, Toggle status
 */
public class StudentsController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Label totalStudentsLabel;
    @FXML private Label activeStudentsLabel;
    @FXML private Label inactiveStudentsLabel;
    @FXML private VBox studentsContainer;

    private User currentUser;
    private StudentService studentService;
    private List<Map<String, Object>> allStudents;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = SessionManager.getInstance().getCurrentUser();
        studentService = new StudentService();

        if (currentUser == null || !currentUser.isAdmin()) {
            showError("Unauthorized access!");
            return;
        }

        // Setup filters
        setupFilters();

        // Load students
        loadAllStudents();

        System.out.println("✅ Students management page loaded");
    }

    /**
     * Setup filters
     */
    private void setupFilters() {
        statusFilter.getItems().addAll("All Students", "Active", "Inactive");
        statusFilter.setValue("All Students");
        statusFilter.setOnAction(e -> applyFilters());

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    /**
     * Load all students
     */
    private void loadAllStudents() {
        allStudents = studentService.getAllStudentsWithStats();

        // Update counts
        int total = allStudents.size();
        long active = allStudents.stream().filter(s -> (Boolean) s.get("isActive")).count();
        long inactive = total - active;

        totalStudentsLabel.setText(String.valueOf(total));
        activeStudentsLabel.setText(String.valueOf(active));
        inactiveStudentsLabel.setText(String.valueOf(inactive));

        applyFilters();
    }

    /**
     * Apply filters
     */
    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase();
        String statusValue = statusFilter.getValue();

        List<Map<String, Object>> filtered = allStudents.stream()
                .filter(student -> {
                    // Search filter
                    String fullName = ((String) student.get("fullName")).toLowerCase();
                    String username = ((String) student.get("username")).toLowerCase();
                    String email = ((String) student.get("email")).toLowerCase();

                    boolean matchesSearch = searchText.isEmpty() ||
                            fullName.contains(searchText) ||
                            username.contains(searchText) ||
                            email.contains(searchText);

                    // Status filter
                    boolean isActive = (Boolean) student.get("isActive");
                    boolean matchesStatus = statusValue.equals("All Students") ||
                            (statusValue.equals("Active") && isActive) ||
                            (statusValue.equals("Inactive") && !isActive);

                    return matchesSearch && matchesStatus;
                })
                .collect(Collectors.toList());

        displayStudents(filtered);
    }

    /**
     * Display students
     */
    private void displayStudents(List<Map<String, Object>> students) {
        studentsContainer.getChildren().clear();

        if (students.isEmpty()) {
            Label noStudentsLabel = new Label("No students found matching your filters.");
            noStudentsLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 16px;");
            noStudentsLabel.setPadding(new Insets(50));
            studentsContainer.getChildren().add(noStudentsLabel);
            return;
        }

        for (Map<String, Object> student : students) {
            VBox studentCard = createStudentCard(student);
            studentsContainer.getChildren().add(studentCard);
        }
    }

    /**
     * Create student card
     */
    private VBox createStudentCard(Map<String, Object> student) {
        VBox card = new VBox(15);
        card.getStyleClass().add("exam-card");
        card.setPadding(new Insets(25));

        // Header row
        HBox headerRow = new HBox(15);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        // Student info
        VBox infoBox = new VBox(5);

        Label nameLabel = new Label("👤 " + student.get("fullName"));
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");

        Label usernameLabel = new Label("@" + student.get("username"));
        usernameLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");

        Label emailLabel = new Label("📧 " + student.get("email"));
        emailLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");

        infoBox.getChildren().addAll(nameLabel, usernameLabel, emailLabel);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        // Status badge
        boolean isActive = (Boolean) student.get("isActive");
        Label statusBadge = new Label(isActive ? "🟢 Active" : "🔴 Inactive");
        statusBadge.getStyleClass().addAll("badge", isActive ? "badge-success" : "badge-danger");

        headerRow.getChildren().addAll(infoBox, statusBadge);

        // Statistics row
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(30);
        statsGrid.setVgap(10);

        int totalAttempts = (int) student.get("totalAttempts");
        int completedExams = (int) student.get("completedExams");
        double avgScore = (double) student.getOrDefault("avgScore", 0.0);
        double passRate = (double) student.getOrDefault("passRate", 0.0);

        addStatToGrid(statsGrid, "📊 Exams Taken:", String.valueOf(completedExams), 0, 0);
        addStatToGrid(statsGrid, "📈 Avg Score:", String.format("%.1f%%", avgScore), 1, 0);
        addStatToGrid(statsGrid, "✅ Pass Rate:", String.format("%.1f%%", passRate), 2, 0);
        addStatToGrid(statsGrid, "🎯 Accuracy:", String.format("%.1f%%", student.getOrDefault("avgAccuracy", 0.0)), 3, 0);

        // Dates row
        HBox datesRow = new HBox(30);
        datesRow.setAlignment(Pos.CENTER_LEFT);

        Timestamp createdAt = (Timestamp) student.get("createdAt");
        if (createdAt != null) {
            Label joinedLabel = new Label("📅 Joined: " +
                    createdAt.toLocalDateTime().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
            joinedLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
            datesRow.getChildren().add(joinedLabel);
        }

        Timestamp lastLogin = (Timestamp) student.get("lastLogin");
        if (lastLogin != null) {
            Label lastLoginLabel = new Label("🕐 Last Login: " +
                    lastLogin.toLocalDateTime().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")));
            lastLoginLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
            datesRow.getChildren().add(lastLoginLabel);
        }

        // Action buttons
        HBox actionRow = new HBox(10);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        int studentId = (int) student.get("id");

        Button detailsBtn = new Button("📋 View Details");
        detailsBtn.getStyleClass().add("btn-primary");
        detailsBtn.setOnAction(e -> handleViewDetails(studentId));

        Button historyBtn = new Button("📊 Exam History");
        historyBtn.getStyleClass().add("btn-secondary");
        historyBtn.setOnAction(e -> handleViewHistory(studentId));

        Button toggleBtn = new Button(isActive ? "⏸️ Deactivate" : "▶️ Activate");
        toggleBtn.getStyleClass().add("btn-secondary");
        toggleBtn.setOnAction(e -> handleToggleStatus(studentId));

        actionRow.getChildren().addAll(detailsBtn, historyBtn, toggleBtn);

        card.getChildren().addAll(headerRow, statsGrid, datesRow, actionRow);

        return card;
    }

    /**
     * Add stat to grid
     */
    private void addStatToGrid(GridPane grid, String label, String value, int col, int row) {
        VBox statBox = new VBox(3);

        Label labelLabel = new Label(label);
        labelLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        statBox.getChildren().addAll(labelLabel, valueLabel);
        grid.add(statBox, col, row);
    }

    /**
     * Handle view details
     */
    private void handleViewDetails(int studentId) {
        User student = studentService.getStudentById(studentId);
        if (student == null) {
            showError("Student not found!");
            return;
        }

        // Create dialog
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Student Details");
        dialog.setHeaderText("Student Information");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().add(ButtonType.CLOSE);
        dialogPane.setPrefSize(500, 400);

        // Content
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(15);
        infoGrid.setVgap(10);

        addInfoRow(infoGrid, "Full Name:", student.getFullName(), 0);
        addInfoRow(infoGrid, "Username:", student.getUsername(), 1);
        addInfoRow(infoGrid, "Email:", student.getEmail(), 2);
        addInfoRow(infoGrid, "Status:", student.isActive() ? "Active" : "Inactive", 3);

        if (student.getCreatedAt() != null) {
            addInfoRow(infoGrid, "Joined:",
                    student.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")), 4);
        }

        if (student.getLastLogin() != null) {
            addInfoRow(infoGrid, "Last Login:",
                    student.getLastLogin().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")), 5);
        }

        content.getChildren().add(infoGrid);
        dialogPane.setContent(content);

        dialog.showAndWait();
    }

    /**
     * Add info row to grid
     */
    private void addInfoRow(GridPane grid, String label, String value, int row) {
        Label labelLabel = new Label(label);
        labelLabel.setStyle("-fx-font-weight: bold;");

        Label valueLabel = new Label(value);

        grid.add(labelLabel, 0, row);
        grid.add(valueLabel, 1, row);
    }

    /**
     * Handle view exam history
     */
    private void handleViewHistory(int studentId) {
        List<Map<String, Object>> history = studentService.getStudentExamHistory(studentId);

        // Create dialog
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Exam History");
        dialog.setHeaderText("Student's Exam Attempts");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().add(ButtonType.CLOSE);
        dialogPane.setPrefSize(900, 600);

        // Create table
        TableView<Map<String, Object>> table = new TableView<>();
        table.setItems(javafx.collections.FXCollections.observableArrayList(history));

        // Columns
        TableColumn<Map<String, Object>, String> titleCol = new TableColumn<>("Exam");
        titleCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty((String) data.getValue().get("examTitle")));
        titleCol.setPrefWidth(200);

        TableColumn<Map<String, Object>, String> subjectCol = new TableColumn<>("Subject");
        subjectCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty((String) data.getValue().get("subject")));
        subjectCol.setPrefWidth(120);

        TableColumn<Map<String, Object>, String> scoreCol = new TableColumn<>("Score");
        scoreCol.setCellValueFactory(data -> {
            int obtained = (int) data.getValue().get("obtainedMarks");
            int total = (int) data.getValue().get("totalMarks");
            return new javafx.beans.property.SimpleStringProperty(obtained + "/" + total);
        });
        scoreCol.setPrefWidth(80);

        TableColumn<Map<String, Object>, String> percentCol = new TableColumn<>("%");
        percentCol.setCellValueFactory(data -> {
            int obtained = (int) data.getValue().get("obtainedMarks");
            int total = (int) data.getValue().get("totalMarks");
            double percent = (obtained * 100.0) / total;
            return new javafx.beans.property.SimpleStringProperty(String.format("%.1f%%", percent));
        });
        percentCol.setPrefWidth(70);

        TableColumn<Map<String, Object>, String> resultCol = new TableColumn<>("Result");
        resultCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty((String) data.getValue().get("result")));
        resultCol.setPrefWidth(80);

        TableColumn<Map<String, Object>, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty((String) data.getValue().get("status")));
        statusCol.setPrefWidth(100);

        TableColumn<Map<String, Object>, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(data -> {
            Timestamp timestamp = (Timestamp) data.getValue().get("startedAt");
            String date = timestamp != null ?
                    timestamp.toLocalDateTime().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) : "N/A";
            return new javafx.beans.property.SimpleStringProperty(date);
        });
        dateCol.setPrefWidth(120);

        table.getColumns().addAll(titleCol, subjectCol, scoreCol, percentCol, resultCol, statusCol, dateCol);

        dialogPane.setContent(table);
        dialog.showAndWait();
    }

    /**
     * Handle toggle status
     */
    private void handleToggleStatus(int studentId) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Toggle Student Status");
        confirm.setHeaderText("Change student account status?");
        confirm.setContentText("This will activate/deactivate the student's account.\nInactive students cannot login.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean success = studentService.toggleStudentStatus(studentId);
                if (success) {
                    showSuccess("Student status changed successfully");
                    loadAllStudents();
                } else {
                    showError("Failed to change status");
                }
            }
        });
    }

    /**
     * Handle back
     */
    @FXML
    private void handleBack() {
        SceneManager.switchScene("/com/examverse/fxml/dashboard/admin-dashboard.fxml");
    }

    /**
     * Show success
     */
    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show error
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}