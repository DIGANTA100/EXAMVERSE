package com.examverse.model.exam;

import java.time.LocalDateTime;

/**
 * StudentExamAttempt - Represents a student's attempt at an exam
 */
public class StudentExamAttempt {

    private int attemptId;
    private int studentId;
    private int examId;
    private String examTitle;
    private String subject;
    private int totalQuestions;
    private int totalMarks;
    private int obtainedMarks;
    private int passingMarks;
    private String status; // ONGOING, COMPLETED, ABANDONED
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private int timeSpentMinutes;
    private double accuracy;
    private String result; // PASSED, FAILED, PENDING

    // Constructors
    public StudentExamAttempt() {
    }

    public StudentExamAttempt(int attemptId, int studentId, int examId,
                              String examTitle, String status) {
        this.attemptId = attemptId;
        this.studentId = studentId;
        this.examId = examId;
        this.examTitle = examTitle;
        this.status = status;
        this.startedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public int getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(int attemptId) {
        this.attemptId = attemptId;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

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

    public int getObtainedMarks() {
        return obtainedMarks;
    }

    public void setObtainedMarks(int obtainedMarks) {
        this.obtainedMarks = obtainedMarks;
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

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public int getTimeSpentMinutes() {
        return timeSpentMinutes;
    }

    public void setTimeSpentMinutes(int timeSpentMinutes) {
        this.timeSpentMinutes = timeSpentMinutes;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    // Utility methods
    public boolean isCompleted() {
        return "COMPLETED".equalsIgnoreCase(status);
    }

    public boolean isOngoing() {
        return "ONGOING".equalsIgnoreCase(status);
    }

    public boolean isPassed() {
        return "PASSED".equalsIgnoreCase(result);
    }

    public double getPercentage() {
        if (totalMarks == 0) return 0.0;
        return (obtainedMarks * 100.0) / totalMarks;
    }

    public String getResultBadge() {
        if (result == null) return "â³";
        switch (result.toUpperCase()) {
            case "PASSED":
                return "âœ…";
            case "FAILED":
                return "âŒ";
            default:
                return "â³";
        }
    }

    public String getResultColor() {
        if (result == null) return "#64748b";
        switch (result.toUpperCase()) {
            case "PASSED":
                return "#22c55e";
            case "FAILED":
                return "#ef4444";
            default:
                return "#64748b";
        }
    }

    @Override
    public String toString() {
        return "StudentExamAttempt{" +
                "attemptId=" + attemptId +
                ", examTitle='" + examTitle + '\'' +
                ", status='" + status + '\'' +
                ", obtainedMarks=" + obtainedMarks +
                ", result='" + result + '\'' +
                '}';
    }
}