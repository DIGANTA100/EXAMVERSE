package com.examverse.model.exam;

import java.time.LocalDateTime;

/**
 * ContestAnswer - One student's answer to one contest question.
 * For MCQ: selectedOption is auto-graded immediately.
 * For WRITTEN: imagePath stores the uploaded image; teacher sets marksAwarded later.
 */
public class ContestAnswer {

    public enum ReviewStatus { PENDING, REVIEWED, REJECTED }

    // ─── Fields ──────────────────────────────────────────────────────────────
    private int    answerId;
    private int    participantId;
    private int    contestId;
    private int    questionId;
    private int    studentId;

    // MCQ fields
    private String selectedOption;   // "A","B","C","D" or null
    private boolean isCorrect;

    // Written fields
    private String imagePath;        // stored file path on server
    private String teacherComment;   // optional teacher feedback
    private ReviewStatus reviewStatus;
    private int    reviewedBy;       // teacher user id

    // Marks
    private int    marksAwarded;     // set immediately for MCQ, after review for written

    private LocalDateTime answeredAt;
    private LocalDateTime reviewedAt;

    // ─── Constructors ─────────────────────────────────────────────────────────
    public ContestAnswer() {
        this.reviewStatus = ReviewStatus.PENDING;
    }

    // ─── Getters / Setters ────────────────────────────────────────────────────
    public int getAnswerId()                                    { return answerId; }
    public void setAnswerId(int answerId)                       { this.answerId = answerId; }

    public int getParticipantId()                               { return participantId; }
    public void setParticipantId(int participantId)             { this.participantId = participantId; }

    public int getContestId()                                   { return contestId; }
    public void setContestId(int contestId)                     { this.contestId = contestId; }

    public int getQuestionId()                                  { return questionId; }
    public void setQuestionId(int questionId)                   { this.questionId = questionId; }

    public int getStudentId()                                   { return studentId; }
    public void setStudentId(int studentId)                     { this.studentId = studentId; }

    public String getSelectedOption()                           { return selectedOption; }
    public void setSelectedOption(String selectedOption)        { this.selectedOption = selectedOption; }

    public boolean isCorrect()                                  { return isCorrect; }
    public void setCorrect(boolean correct)                     { isCorrect = correct; }

    public String getImagePath()                                { return imagePath; }
    public void setImagePath(String imagePath)                  { this.imagePath = imagePath; }

    public String getTeacherComment()                           { return teacherComment; }
    public void setTeacherComment(String teacherComment)        { this.teacherComment = teacherComment; }

    public ReviewStatus getReviewStatus()                       { return reviewStatus; }
    public void setReviewStatus(ReviewStatus reviewStatus)      { this.reviewStatus = reviewStatus; }

    public int getReviewedBy()                                  { return reviewedBy; }
    public void setReviewedBy(int reviewedBy)                   { this.reviewedBy = reviewedBy; }

    public int getMarksAwarded()                                { return marksAwarded; }
    public void setMarksAwarded(int marksAwarded)               { this.marksAwarded = marksAwarded; }

    public LocalDateTime getAnsweredAt()                        { return answeredAt; }
    public void setAnsweredAt(LocalDateTime answeredAt)         { this.answeredAt = answeredAt; }

    public LocalDateTime getReviewedAt()                        { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt)         { this.reviewedAt = reviewedAt; }
}