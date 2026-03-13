package com.examverse.controller.contest;

import com.examverse.model.exam.*;
import com.examverse.model.exam.Contest.Theme;
import com.examverse.model.user.StudentRating;
import com.examverse.model.user.User;
import com.examverse.service.exam.ContestService;
import com.examverse.util.SceneManager;
import com.examverse.util.SessionManager;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * ContestResultController — FIXED v2
 *
 * BUG FIX: "Enter Contest button still showing / wrong participant data in Contest 2"
 *
 * Root cause:
 *   When handleBack() navigated to the lobby, SessionManager still held:
 *     - currentContest  → contest 1
 *     - currentParticipantId → contest 1's participant ID
 *
 *   So when the student entered contest 2, ContestLobbyController correctly
 *   called registerStudent(contest2Id, studentId) and set currentContest to
 *   contest 2. BUT ContestRoomController read currentParticipantId from the
 *   session which was still contest 1's value (not yet replaced because
 *   registerStudent's Platform.runLater hadn't run, or there was a race).
 *
 *   Fix: handleBack() now explicitly clears currentContest and
 *   currentParticipantId from the session before navigating to the lobby.
 *   ContestRoomController (fixed separately) also re-derives participantId
 *   from the DB as a second safety net.
 *
 *   Also fixed: handleLeaderboard() was navigating without clearing the
 *   session's leaderboard_mode attribute, which could cause the leaderboard
 *   to show in "global" mode instead of the contest-specific mode.
 */
public class ContestResultController implements Initializable {

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private VBox  rootVBox;
    @FXML private Label contestTitleLabel;
    @FXML private Label mcqScoreLabel;
    @FXML private Label writtenStatusLabel;
    @FXML private Label liveRankLabel;
    @FXML private Label finalRankLabel;
    @FXML private Label ratingChangeLabel;
    @FXML private Label newRatingLabel;
    @FXML private Label rankTitleLabel;
    @FXML private VBox  answersReviewContainer;
    @FXML private VBox  standingsContainer;
    @FXML private Button backBtn, leaderboardBtn;

    // ── State ─────────────────────────────────────────────────────────────────
    private final ContestService contestService = new ContestService();
    private User currentUser;
    private Contest contest;
    private int participantId;

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser   = SessionManager.getInstance().getCurrentUser();
        contest       = SessionManager.getInstance().getCurrentContest();
        participantId = SessionManager.getInstance().getCurrentParticipantId();

        if (contest == null || currentUser == null) return;

        applyTheme(contest.getTheme());
        contestTitleLabel.setText("📊  " + contest.getContestTitle() + " — Results");

        loadMyResult();
        loadAnswerReview();
        loadStandings();
    }

    // ── Apply Theme ───────────────────────────────────────────────────────────
    private void applyTheme(Theme t) {
        rootVBox.setStyle("-fx-background-color:" + t.getBgColor() + ";");
    }

    // ── My Result ─────────────────────────────────────────────────────────────
    private void loadMyResult() {
        List<ContestParticipant> standings = contestService.getFinalStandings(contest.getContestId());
        ContestParticipant me = standings.stream()
                .filter(p -> p.getStudentId() == currentUser.getId())
                .findFirst().orElse(null);

        if (me == null) {
            List<ContestParticipant> live = contestService.getLiveLeaderboard(contest.getContestId());
            me = live.stream().filter(p -> p.getStudentId() == currentUser.getId())
                    .findFirst().orElse(null);
        }

        if (me == null) {
            mcqScoreLabel.setText("—");
            return;
        }

        Theme t = contest.getTheme();

        mcqScoreLabel.setText(me.getMcqMarksObtained() + " / " +
                (contest.getTotalMcqQuestions() * contest.getMcqMarksEach()));
        mcqScoreLabel.setStyle("-fx-text-fill:" + t.getAccentColor() +
                "; -fx-font-size:28px; -fx-font-weight:bold;");

        int pending = me.getPendingWrittenReviews();
        if (contest.getTotalWrittenQuestions() == 0) {
            writtenStatusLabel.setText("No written questions.");
        } else if (pending > 0) {
            writtenStatusLabel.setText("⏳ " + pending + " written answer(s) pending teacher review.");
            writtenStatusLabel.setStyle("-fx-text-fill:#f59e0b; -fx-font-size:14px;");
        } else {
            writtenStatusLabel.setText("✅ All written answers reviewed. +" +
                    me.getWrittenMarksObtained() + " marks");
            writtenStatusLabel.setStyle("-fx-text-fill:#22c55e; -fx-font-size:14px;");
        }

        liveRankLabel.setText("#" + me.getLiveRank());
        liveRankLabel.setStyle("-fx-text-fill:" + t.getHighlightColor() +
                "; -fx-font-size:24px; -fx-font-weight:bold;");

        if (me.isEvaluated()) {
            finalRankLabel.setText("Final Rank: #" + me.getFinalRank());

            int change = me.getRatingChange();
            String sign = change >= 0 ? "+" : "";
            ratingChangeLabel.setText("Rating Change: " + sign + change);
            ratingChangeLabel.setStyle("-fx-text-fill:" + (change >= 0 ? "#22c55e" : "#ef4444") +
                    "; -fx-font-size:18px; -fx-font-weight:bold;");

            newRatingLabel.setText("New Rating: " + me.getRatingAfter());
            newRatingLabel.setStyle("-fx-text-fill:" + t.getAccentColor() +
                    "; -fx-font-size:22px; -fx-font-weight:bold;");

            String title = StudentRating.getTitleForRating(me.getRatingAfter());
            rankTitleLabel.setText(title);
            rankTitleLabel.getStyleClass().setAll("rank-title",
                    StudentRating.getTitleCssClass(me.getRatingAfter()));

            animateRatingChange(change);
        } else {
            finalRankLabel.setText("Final rank pending evaluation.");
            ratingChangeLabel.setText("Rating pending evaluation.");
            newRatingLabel.setText("Current Rating: " + contestService.getStudentRating(currentUser.getId()));
        }
    }

    private void animateRatingChange(int change) {
        if (ratingChangeLabel == null) return;
        ScaleTransition st = new ScaleTransition(Duration.millis(400), ratingChangeLabel);
        st.setFromX(0.8); st.setToX(1.0);
        st.setFromY(0.8); st.setToY(1.0);
        st.play();

        FadeTransition ft = new FadeTransition(Duration.millis(400), ratingChangeLabel);
        ft.setFromValue(0); ft.setToValue(1);
        ft.play();
    }

    // ── Answer Review ─────────────────────────────────────────────────────────
    private void loadAnswerReview() {
        answersReviewContainer.getChildren().clear();
        if (participantId <= 0) return;

        List<ContestAnswer> answers = contestService.getStudentAnswers(participantId);
        List<ContestQuestion> questions = contestService.getQuestionsForContest(contest.getContestId());
        Theme t = contest.getTheme();

        Label sectionTitle = new Label("📋  Your Answers");
        sectionTitle.setStyle("-fx-text-fill:#e2e8f0; -fx-font-size:17px; -fx-font-weight:bold;");
        answersReviewContainer.getChildren().add(sectionTitle);

        for (ContestAnswer a : answers) {
            ContestQuestion q = questions.stream()
                    .filter(qq -> qq.getQuestionId() == a.getQuestionId())
                    .findFirst().orElse(null);
            if (q == null) continue;

            VBox answerCard = new VBox(8);
            answerCard.setPadding(new Insets(14));
            answerCard.setStyle("-fx-background-color:#1e293b; -fx-background-radius:10;" +
                    "-fx-border-color:" + (a.isCorrect() ? "#22c55e" :
                    a.getSelectedOption() != null ? "#ef4444" : "#f59e0b") +
                    "55; -fx-border-radius:10; -fx-border-width:1;");

            Label qText = new Label((q.isMcq() ? "MCQ " : "Written ") + " — " + q.getQuestionText());
            qText.setStyle("-fx-text-fill:#f1f5f9; -fx-font-size:14px; -fx-font-weight:bold;");
            qText.setWrapText(true);

            if (q.isMcq()) {
                Label yourAns = new Label("Your answer: " + (a.getSelectedOption() != null
                        ? a.getSelectedOption() : "Not answered"));
                yourAns.setStyle("-fx-text-fill:" + (a.isCorrect() ? "#22c55e" : "#ef4444") +
                        "; -fx-font-size:13px;");

                Label correctAns = new Label("Correct: " + q.getCorrectAnswer());
                correctAns.setStyle("-fx-text-fill:#22c55e; -fx-font-size:13px;");

                Label marksGot = new Label("Marks: " + a.getMarksAwarded() + "/" + q.getMarks());
                marksGot.setStyle("-fx-text-fill:" + t.getHighlightColor() + "; -fx-font-size:13px;");

                answerCard.getChildren().addAll(qText, yourAns, correctAns, marksGot);

                if (q.getExplanation() != null && !q.getExplanation().isEmpty()) {
                    Label exp = new Label("💡 " + q.getExplanation());
                    exp.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:12px;");
                    exp.setWrapText(true);
                    answerCard.getChildren().add(exp);
                }
            } else {
                String statusText = switch (a.getReviewStatus()) {
                    case PENDING  -> "⏳ Awaiting teacher review";
                    case REVIEWED -> "✅ Reviewed — " + a.getMarksAwarded() + "/" + q.getMarks() + " marks";
                    case REJECTED -> "❌ Rejected";
                };
                Label statusLbl = new Label(statusText);
                statusLbl.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:13px;");

                if (a.getTeacherComment() != null && !a.getTeacherComment().isEmpty()) {
                    Label comment = new Label("Teacher: " + a.getTeacherComment());
                    comment.setStyle("-fx-text-fill:#fbbf24; -fx-font-size:12px;");
                    comment.setWrapText(true);
                    answerCard.getChildren().addAll(qText, statusLbl, comment);
                } else {
                    answerCard.getChildren().addAll(qText, statusLbl);
                }
            }

            answersReviewContainer.getChildren().add(answerCard);
        }
    }

    // ── Final Standings ───────────────────────────────────────────────────────
    private void loadStandings() {
        standingsContainer.getChildren().clear();
        Theme t = contest.getTheme();

        Label title = new Label("🏆  Contest Standings");
        title.setStyle("-fx-text-fill:" + t.getAccentColor() +
                "; -fx-font-size:17px; -fx-font-weight:bold;");
        standingsContainer.getChildren().add(title);

        List<ContestParticipant> list = contestService.getFinalStandings(contest.getContestId());
        if (list.isEmpty()) {
            list = contestService.getLiveLeaderboard(contest.getContestId());
        }

        for (int i = 0; i < list.size(); i++) {
            ContestParticipant p = list.get(i);
            boolean isMe = p.getStudentId() == currentUser.getId();
            String rankEmoji = switch (i) {
                case 0 -> "🥇";
                case 1 -> "🥈";
                case 2 -> "🥉";
                default -> (i + 1) + ".  ";
            };

            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 14, 10, 14));
            row.setStyle(
                    "-fx-background-color:" + (isMe ? t.getAccentColor() + "22" : "#1e293b") + ";" +
                            "-fx-background-radius:8;" +
                            (isMe ? "-fx-border-color:" + t.getAccentColor() + ";" +
                                    "-fx-border-radius:8; -fx-border-width:1;" : "")
            );

            Label rankLbl = new Label(rankEmoji);
            rankLbl.setMinWidth(36);
            rankLbl.setStyle("-fx-font-size:16px;");

            Label nameLbl = new Label(p.getStudentName() != null ? p.getStudentName() : "—");
            nameLbl.setStyle("-fx-text-fill:" + (isMe ? t.getAccentColor() : "#e2e8f0") +
                    "; -fx-font-weight:" + (isMe ? "bold" : "normal") +
                    "; -fx-font-size:14px;");
            nameLbl.setMinWidth(160);

            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);

            Label mcqLbl = new Label("MCQ: " + p.getMcqMarksObtained());
            mcqLbl.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:13px;");

            Label wrLbl = new Label("WR: " + p.getWrittenMarksObtained());
            wrLbl.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:13px;");

            Label totalLbl = new Label("Total: " + p.getTotalMarksObtained());
            totalLbl.setStyle("-fx-text-fill:" + t.getHighlightColor() +
                    "; -fx-font-weight:bold; -fx-font-size:14px;");

            if (p.isEvaluated()) {
                int change = p.getRatingChange();
                Label ratingChg = new Label((change >= 0 ? "+" : "") + change);
                ratingChg.setStyle("-fx-text-fill:" + (change >= 0 ? "#22c55e" : "#ef4444") +
                        "; -fx-font-weight:bold; -fx-font-size:13px;");
                row.getChildren().addAll(rankLbl, nameLbl, sp, mcqLbl, wrLbl, totalLbl, ratingChg);
            } else {
                row.getChildren().addAll(rankLbl, nameLbl, sp, mcqLbl, wrLbl, totalLbl);
            }

            standingsContainer.getChildren().add(row);
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────
    @FXML
    private void handleBack() {
        // ── FIX: Clear the contest session state before going back to lobby.
        //
        // Without this, SessionManager holds onto the old contest and participant ID.
        // When the student enters a NEW contest, ContestRoomController.initialize()
        // reads the stale participantId (contest 1's) from the session and loads
        // the wrong data. Now the lobby gets a clean slate every time.
        System.out.println("DEBUG BACK: clearing contest=" + (contest != null ? contest.getContestId() : "NULL")
                + " participantId=" + participantId);
        SessionManager.getInstance().setCurrentContest(null);
        SessionManager.getInstance().setCurrentParticipantId(-1);

        SceneManager.switchScene("/com/examverse/fxml/contest/contest-lobby.fxml");
    }

    @FXML
    private void handleLeaderboard() {
        // Show this contest's specific leaderboard from the result screen
        SessionManager.getInstance().setAttribute("leaderboard_mode", "contest");
        SceneManager.switchScene("/com/examverse/fxml/contest/contest-leaderboard.fxml");
    }
}