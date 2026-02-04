package com.examverse.model.user;

/**
 * StudentStats - Dashboard statistics for a student
 */
public class StudentStats {

    private int totalExamsAttempted;
    private int totalExamsCompleted;
    private int totalExamsPassed;
    private int totalExamsFailed;
    private double averageScore;
    private double overallAccuracy;
    private int totalTimeSpentMinutes;
    private int currentRank; // Optional for future

    // Constructors
    public StudentStats() {
    }

    public StudentStats(int totalExamsAttempted, int totalExamsCompleted,
                        int totalExamsPassed, double averageScore, double overallAccuracy) {
        this.totalExamsAttempted = totalExamsAttempted;
        this.totalExamsCompleted = totalExamsCompleted;
        this.totalExamsPassed = totalExamsPassed;
        this.averageScore = averageScore;
        this.overallAccuracy = overallAccuracy;
    }

    // Getters and Setters
    public int getTotalExamsAttempted() {
        return totalExamsAttempted;
    }

    public void setTotalExamsAttempted(int totalExamsAttempted) {
        this.totalExamsAttempted = totalExamsAttempted;
    }

    public int getTotalExamsCompleted() {
        return totalExamsCompleted;
    }

    public void setTotalExamsCompleted(int totalExamsCompleted) {
        this.totalExamsCompleted = totalExamsCompleted;
    }

    public int getTotalExamsPassed() {
        return totalExamsPassed;
    }

    public void setTotalExamsPassed(int totalExamsPassed) {
        this.totalExamsPassed = totalExamsPassed;
    }

    public int getTotalExamsFailed() {
        return totalExamsFailed;
    }

    public void setTotalExamsFailed(int totalExamsFailed) {
        this.totalExamsFailed = totalExamsFailed;
    }

    public double getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(double averageScore) {
        this.averageScore = averageScore;
    }

    public double getOverallAccuracy() {
        return overallAccuracy;
    }

    public void setOverallAccuracy(double overallAccuracy) {
        this.overallAccuracy = overallAccuracy;
    }

    public int getTotalTimeSpentMinutes() {
        return totalTimeSpentMinutes;
    }

    public void setTotalTimeSpentMinutes(int totalTimeSpentMinutes) {
        this.totalTimeSpentMinutes = totalTimeSpentMinutes;
    }

    public int getCurrentRank() {
        return currentRank;
    }

    public void setCurrentRank(int currentRank) {
        this.currentRank = currentRank;
    }

    // Utility methods
    public String getFormattedAverageScore() {
        return String.format("%.1f%%", averageScore);
    }

    public String getFormattedAccuracy() {
        return String.format("%.1f%%", overallAccuracy);
    }

    public int getOngoingExams() {
        return totalExamsAttempted - totalExamsCompleted;
    }

    public double getPassRate() {
        if (totalExamsCompleted == 0) return 0.0;
        return (totalExamsPassed * 100.0) / totalExamsCompleted;
    }

    @Override
    public String toString() {
        return "StudentStats{" +
                "totalExamsAttempted=" + totalExamsAttempted +
                ", averageScore=" + averageScore +
                ", accuracy=" + overallAccuracy +
                '}';
    }
}