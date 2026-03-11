package com.examverse.model.exam;

import java.time.LocalDateTime;

/**
 * ContestParticipant - Tracks a student's participation in a contest.
 * Holds live score, rank, submission status, and rating change after evaluation.
 */
public class ContestParticipant {

    public enum ParticipantStatus { REGISTERED, ACTIVE, SUBMITTED, EVALUATED }

    // ─── Fields ──────────────────────────────────────────────────────────────
    private int               participantId;
    private int               contestId;
    private int               studentId;
    private String            studentName;   // denormalized for display
    private String            username;

    private ParticipantStatus status;
    private LocalDateTime     joinedAt;
    private LocalDateTime     submittedAt;

    // Scores
    private int mcqMarksObtained;
    private int writtenMarksObtained; // filled after teacher evaluation
    private int totalMarksObtained;   // mcq + written (final)

    // Live rank (recalculated on each MCQ submission)
    private int liveRank;

    // Final rank (after full evaluation)
    private int finalRank;

    // Rating change
    private int ratingBefore;
    private int ratingAfter;
    private int ratingChange;   // can be negative

    // How many written answers are still pending review
    private int pendingWrittenReviews;

    // ─── Constructors ─────────────────────────────────────────────────────────
    public ContestParticipant() {
        this.status = ParticipantStatus.REGISTERED;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────
    public boolean isEvaluated() { return status == ParticipantStatus.EVALUATED; }
    public int     getScoreGap() { return ratingAfter - ratingBefore; }

    // ─── Getters / Setters ────────────────────────────────────────────────────
    public int getParticipantId()                                   { return participantId; }
    public void setParticipantId(int participantId)                 { this.participantId = participantId; }

    public int getContestId()                                       { return contestId; }
    public void setContestId(int contestId)                         { this.contestId = contestId; }

    public int getStudentId()                                       { return studentId; }
    public void setStudentId(int studentId)                         { this.studentId = studentId; }

    public String getStudentName()                                  { return studentName; }
    public void setStudentName(String studentName)                  { this.studentName = studentName; }

    public String getUsername()                                     { return username; }
    public void setUsername(String username)                        { this.username = username; }

    public ParticipantStatus getStatus()                            { return status; }
    public void setStatus(ParticipantStatus status)                 { this.status = status; }

    public LocalDateTime getJoinedAt()                              { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt)                 { this.joinedAt = joinedAt; }

    public LocalDateTime getSubmittedAt()                           { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt)           { this.submittedAt = submittedAt; }

    public int getMcqMarksObtained()                                { return mcqMarksObtained; }
    public void setMcqMarksObtained(int mcqMarksObtained)           { this.mcqMarksObtained = mcqMarksObtained; }

    public int getWrittenMarksObtained()                            { return writtenMarksObtained; }
    public void setWrittenMarksObtained(int writtenMarksObtained)   { this.writtenMarksObtained = writtenMarksObtained; }

    public int getTotalMarksObtained()                              { return totalMarksObtained; }
    public void setTotalMarksObtained(int totalMarksObtained)       { this.totalMarksObtained = totalMarksObtained; }

    public int getLiveRank()                                        { return liveRank; }
    public void setLiveRank(int liveRank)                           { this.liveRank = liveRank; }

    public int getFinalRank()                                       { return finalRank; }
    public void setFinalRank(int finalRank)                         { this.finalRank = finalRank; }

    public int getRatingBefore()                                    { return ratingBefore; }
    public void setRatingBefore(int ratingBefore)                   { this.ratingBefore = ratingBefore; }

    public int getRatingAfter()                                     { return ratingAfter; }
    public void setRatingAfter(int ratingAfter)                     { this.ratingAfter = ratingAfter; }

    public int getRatingChange()                                    { return ratingChange; }
    public void setRatingChange(int ratingChange)                   { this.ratingChange = ratingChange; }

    public int getPendingWrittenReviews()                           { return pendingWrittenReviews; }
    public void setPendingWrittenReviews(int pendingWrittenReviews) { this.pendingWrittenReviews = pendingWrittenReviews; }
}