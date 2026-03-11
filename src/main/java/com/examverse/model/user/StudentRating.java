package com.examverse.model.user;

/**
 * StudentRating - Tracks a student's cumulative rating points and rank title.
 * Rating changes after each contest based on performance (like Codeforces).
 *
 * Rank titles (ExamVerse style):
 *   0–999    🌱 Beginner
 *   1000–1399 🧑‍🎓 Learner
 *   1400–1799 ⚡ Skilled
 *   1800–2199 🔥 Advanced
 *   2200–2599 🧠 Expert
 *   2600–2999 🚀 Champion
 *   3000+     👑 Legend
 */
public class StudentRating {

    // ─── Static rank title logic ──────────────────────────────────────────────
    public static String getTitleForRating(int rating) {
        if (rating >= 3000) return "👑 Legend";
        if (rating >= 2600) return "🚀 Champion";
        if (rating >= 2200) return "🧠 Expert";
        if (rating >= 1800) return "🔥 Advanced";
        if (rating >= 1400) return "⚡ Skilled";
        if (rating >= 1000) return "🧑‍🎓 Learner";
        return "🌱 Beginner";
    }

    public static String getTitleCssClass(int rating) {
        if (rating >= 3000) return "rank-legend";
        if (rating >= 2600) return "rank-champion";
        if (rating >= 2200) return "rank-expert";
        if (rating >= 1800) return "rank-advanced";
        if (rating >= 1400) return "rank-skilled";
        if (rating >= 1000) return "rank-learner";
        return "rank-beginner";
    }

    // Starting rating for new students
    public static final int DEFAULT_RATING = 800;

    // ─── Fields ──────────────────────────────────────────────────────────────
    private int    ratingId;
    private int    studentId;
    private String studentName;
    private String username;
    private int    currentRating;
    private int    peakRating;
    private int    contestsParticipated;
    private int    contestsWon;        // times ranked #1
    private int    totalScore;         // cumulative MCQ + written marks across all contests

    // ─── Constructors ─────────────────────────────────────────────────────────
    public StudentRating() {
        this.currentRating = DEFAULT_RATING;
        this.peakRating    = DEFAULT_RATING;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────
    public String getRankTitle()    { return getTitleForRating(currentRating); }
    public String getRankCssClass() { return getTitleCssClass(currentRating); }

    // ─── Getters / Setters ────────────────────────────────────────────────────
    public int getRatingId()                                 { return ratingId; }
    public void setRatingId(int ratingId)                    { this.ratingId = ratingId; }

    public int getStudentId()                                { return studentId; }
    public void setStudentId(int studentId)                  { this.studentId = studentId; }

    public String getStudentName()                           { return studentName; }
    public void setStudentName(String studentName)           { this.studentName = studentName; }

    public String getUsername()                              { return username; }
    public void setUsername(String username)                 { this.username = username; }

    public int getCurrentRating()                            { return currentRating; }
    public void setCurrentRating(int currentRating)          {
        this.currentRating = currentRating;
        if (currentRating > peakRating) peakRating = currentRating;
    }

    public int getPeakRating()                               { return peakRating; }
    public void setPeakRating(int peakRating)                { this.peakRating = peakRating; }

    public int getContestsParticipated()                     { return contestsParticipated; }
    public void setContestsParticipated(int n)               { this.contestsParticipated = n; }

    public int getContestsWon()                              { return contestsWon; }
    public void setContestsWon(int contestsWon)              { this.contestsWon = contestsWon; }

    public int getTotalScore()                               { return totalScore; }
    public void setTotalScore(int totalScore)                { this.totalScore = totalScore; }
}