package com.examverse.model.exam;

import java.time.LocalDateTime;

/**
 * Exam Model - Represents an exam in the system
 */
public class Exam {

    private int examId;
    private String examTitle;
    private String subject;
    private String description;
    private String difficulty; // EASY, MEDIUM, HARD
    private int totalQuestions;
    private int totalMarks;
    private int durationMinutes;
    private int passingMarks;
    private String status; // ACTIVE, INACTIVE, ARCHIVED
    private LocalDateTime createdAt;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private int createdBy; // Admin user ID

    // Constructors
    public Exam() {
    }

    public Exam(int examId, String examTitle, String subject, String description,
                String difficulty, int totalQuestions, int totalMarks,
                int durationMinutes, int passingMarks) {
        this.examId = examId;
        this.examTitle = examTitle;
        this.subject = subject;
        this.description = description;
        this.difficulty = difficulty;
        this.totalQuestions = totalQuestions;
        this.totalMarks = totalMarks;
        this.durationMinutes = durationMinutes;
        this.passingMarks = passingMarks;
        this.status = "ACTIVE";
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public int getExamId() {
        return examId;
    }

    public void setExamId(int examId) {
        this.examId = examId;
    }

    public String getExamTitle() {
        return examTitle;
    }

    public void setExamTitle(String examTitle) {
        this.examTitle = examTitle;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public int getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(int totalMarks) {
        this.totalMarks = totalMarks;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public int getPassingMarks() {
        return passingMarks;
    }

    public void setPassingMarks(int passingMarks) {
        this.passingMarks = passingMarks;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public int getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(int createdBy) {
        this.createdBy = createdBy;
    }

    // Utility methods
    public String getDifficultyColor() {
        switch (difficulty.toUpperCase()) {
            case "EASY":
                return "#22c55e"; // Green
            case "MEDIUM":
                return "#f59e0b"; // Orange
            case "HARD":
                return "#ef4444"; // Red
            default:
                return "#64748b"; // Gray
        }
    }

    public String getDifficultyBadge() {
        switch (difficulty.toUpperCase()) {
            case "EASY":
                return "🟢";
            case "MEDIUM":
                return "🟡";
            case "HARD":
                return "🔴";
            default:
                return "⚪";
        }
    }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    public String getFormattedDuration() {
        if (durationMinutes < 60) {
            return durationMinutes + " min";
        } else {
            int hours = durationMinutes / 60;
            int mins = durationMinutes % 60;
            return hours + "h " + (mins > 0 ? mins + "m" : "");
        }
    }

    @Override
    public String toString() {
        return "Exam{" +
                "examId=" + examId +
                ", examTitle='" + examTitle + '\'' +
                ", subject='" + subject + '\'' +
                ", difficulty='" + difficulty + '\'' +
                ", totalQuestions=" + totalQuestions +
                ", durationMinutes=" + durationMinutes +
                '}';
    }
}