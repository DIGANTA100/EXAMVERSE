package com.examverse.model.exam;

/**
 * ContestQuestion - A question inside a contest.
 * Type: MCQ (auto-graded) or WRITTEN (teacher-graded, student uploads image).
 */
public class ContestQuestion {

    public enum QuestionType { MCQ, WRITTEN }

    // ─── Fields ──────────────────────────────────────────────────────────────
    private int          questionId;
    private int          contestId;
    private QuestionType type;
    private String       questionText;
    private int          marks;
    private int          orderIndex;   // display order in contest

    // MCQ-specific fields (null for WRITTEN)
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String correctAnswer; // "A","B","C","D"
    private String explanation;

    // ─── Constructors ─────────────────────────────────────────────────────────
    public ContestQuestion() {}

    public boolean isMcq()     { return type == QuestionType.MCQ; }
    public boolean isWritten() { return type == QuestionType.WRITTEN; }

    // ─── Getters / Setters ────────────────────────────────────────────────────
    public int getQuestionId()                               { return questionId; }
    public void setQuestionId(int questionId)                { this.questionId = questionId; }

    public int getContestId()                                { return contestId; }
    public void setContestId(int contestId)                  { this.contestId = contestId; }

    public QuestionType getType()                            { return type; }
    public void setType(QuestionType type)                   { this.type = type; }

    public String getQuestionText()                          { return questionText; }
    public void setQuestionText(String questionText)         { this.questionText = questionText; }

    public int getMarks()                                    { return marks; }
    public void setMarks(int marks)                          { this.marks = marks; }

    public int getOrderIndex()                               { return orderIndex; }
    public void setOrderIndex(int orderIndex)                { this.orderIndex = orderIndex; }

    public String getOptionA()                               { return optionA; }
    public void setOptionA(String optionA)                   { this.optionA = optionA; }

    public String getOptionB()                               { return optionB; }
    public void setOptionB(String optionB)                   { this.optionB = optionB; }

    public String getOptionC()                               { return optionC; }
    public void setOptionC(String optionC)                   { this.optionC = optionC; }

    public String getOptionD()                               { return optionD; }
    public void setOptionD(String optionD)                   { this.optionD = optionD; }

    public String getCorrectAnswer()                         { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer)       { this.correctAnswer = correctAnswer; }

    public String getExplanation()                           { return explanation; }
    public void setExplanation(String explanation)           { this.explanation = explanation; }
}