package com.examverse.model.exam;

import java.time.LocalDateTime;

/**
 * Contest - Represents a live contest with theme, questions, and real-time ranking.
 * Supports MCQ (answered directly) and Written (image upload + teacher review).
 */
public class Contest {

    public enum Theme {
        COSMIC_ARENA("🌌 Cosmic Arena",      "cosmic_arena",    "cosmic_theme.mp3",    "#0a0a2e", "#7c3aed", "#e879f9"),
        NEON_CIRCUIT("⚡ Neon Circuit",       "neon_circuit",    "neon_circuit.mp3",    "#0d0d0d", "#00ff88", "#00ccff"),
        DRAGON_REALM("🐉 Dragon Realm",       "dragon_realm",    "dragon_realm.mp3",    "#1a0a00", "#ff6600", "#ffd700"),
        FROZEN_PEAKS("❄️ Frozen Peaks",       "frozen_peaks",    "frozen_peaks.mp3",    "#001a33", "#00cfff", "#ffffff"),
        SHADOW_TEMPLE("🏯 Shadow Temple",     "shadow_temple",   "shadow_temple.mp3",   "#0a0010", "#9f1239", "#fbbf24"),
        CYBER_STORM("🤖 Cyber Storm",         "cyber_storm",     "cyber_storm.mp3",     "#000a1a", "#38bdf8", "#f0abfc"),
        VOLCANIC_FORGE("🌋 Volcanic Forge",   "volcanic_forge",  "volcanic_forge.mp3",  "#1c0000", "#ef4444", "#f97316"),
        OCEAN_DEPTHS("🌊 Ocean Depths",       "ocean_depths",    "ocean_depths.mp3",    "#001020", "#0ea5e9", "#34d399");

        private final String displayName;
        private final String cssClass;
        private final String musicFile;
        private final String bgColor;
        private final String accentColor;
        private final String highlightColor;

        Theme(String displayName, String cssClass, String musicFile,
              String bgColor, String accentColor, String highlightColor) {
            this.displayName  = displayName;
            this.cssClass     = cssClass;
            this.musicFile    = musicFile;
            this.bgColor      = bgColor;
            this.accentColor  = accentColor;
            this.highlightColor = highlightColor;
        }

        public String getDisplayName()    { return displayName; }
        public String getCssClass()       { return cssClass; }
        public String getMusicFile()      { return musicFile; }
        public String getBgColor()        { return bgColor; }
        public String getAccentColor()    { return accentColor; }
        public String getHighlightColor() { return highlightColor; }
    }

    public enum Status {
        UPCOMING, LIVE, EVALUATION, FINISHED, CANCELLED
    }

    // ─── Fields ──────────────────────────────────────────────────────────────
    private int    contestId;
    private String contestTitle;
    private String description;
    private Theme  theme;
    private Status status;

    private int createdBy;        // admin/teacher user id
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime evalDeadline; // deadline for teachers to check written answers

    private int durationMinutes;
    private int totalMcqQuestions;
    private int totalWrittenQuestions;
    private int mcqMarksEach;
    private int writtenMarksEach;
    private int totalMarks;

    // Rating points change bounds
    private int maxGain;  // points a top performer can gain
    private int maxLoss;  // points a bottom performer can lose

    private LocalDateTime createdAt;

    // ─── Constructors ─────────────────────────────────────────────────────────
    public Contest() {
        this.status  = Status.UPCOMING;
        this.maxGain = 100;
        this.maxLoss = 50;
    }

    // ─── Computed helpers ─────────────────────────────────────────────────────
    public boolean isLive()       { return status == Status.LIVE; }
    public boolean isUpcoming()   { return status == Status.UPCOMING; }
    public boolean isInEval()     { return status == Status.EVALUATION; }
    public boolean isFinished()   { return status == Status.FINISHED; }

    public String getThemeMusicPath() {
        return "/com/examverse/assets/music/" + theme.getMusicFile();
    }

    public int computeTotalMarks() {
        return (totalMcqQuestions * mcqMarksEach) + (totalWrittenQuestions * writtenMarksEach);
    }

    // ─── Getters / Setters ────────────────────────────────────────────────────
    public int getContestId()                         { return contestId; }
    public void setContestId(int contestId)           { this.contestId = contestId; }

    public String getContestTitle()                   { return contestTitle; }
    public void setContestTitle(String contestTitle)  { this.contestTitle = contestTitle; }

    public String getDescription()                    { return description; }
    public void setDescription(String description)    { this.description = description; }

    public Theme getTheme()                           { return theme; }
    public void setTheme(Theme theme)                 { this.theme = theme; }

    public Status getStatus()                         { return status; }
    public void setStatus(Status status)              { this.status = status; }

    public int getCreatedBy()                         { return createdBy; }
    public void setCreatedBy(int createdBy)           { this.createdBy = createdBy; }

    public LocalDateTime getStartTime()               { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime()                 { return endTime; }
    public void setEndTime(LocalDateTime endTime)     { this.endTime = endTime; }

    public LocalDateTime getEvalDeadline()                        { return evalDeadline; }
    public void setEvalDeadline(LocalDateTime evalDeadline)       { this.evalDeadline = evalDeadline; }

    public int getDurationMinutes()                               { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes)           { this.durationMinutes = durationMinutes; }

    public int getTotalMcqQuestions()                             { return totalMcqQuestions; }
    public void setTotalMcqQuestions(int totalMcqQuestions)       { this.totalMcqQuestions = totalMcqQuestions; }

    public int getTotalWrittenQuestions()                         { return totalWrittenQuestions; }
    public void setTotalWrittenQuestions(int totalWrittenQuestions){ this.totalWrittenQuestions = totalWrittenQuestions; }

    public int getMcqMarksEach()                                  { return mcqMarksEach; }
    public void setMcqMarksEach(int mcqMarksEach)                 { this.mcqMarksEach = mcqMarksEach; }

    public int getWrittenMarksEach()                              { return writtenMarksEach; }
    public void setWrittenMarksEach(int writtenMarksEach)         { this.writtenMarksEach = writtenMarksEach; }

    public int getTotalMarks()                                    { return totalMarks; }
    public void setTotalMarks(int totalMarks)                     { this.totalMarks = totalMarks; }

    public int getMaxGain()                                       { return maxGain; }
    public void setMaxGain(int maxGain)                           { this.maxGain = maxGain; }

    public int getMaxLoss()                                       { return maxLoss; }
    public void setMaxLoss(int maxLoss)                           { this.maxLoss = maxLoss; }

    public LocalDateTime getCreatedAt()                           { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)             { this.createdAt = createdAt; }
}