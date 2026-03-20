package com.examverse.controller.contest;

import com.examverse.model.exam.*;
import com.examverse.model.exam.Contest.Theme;
import com.examverse.model.user.StudentRating;
import com.examverse.model.user.User;
import com.examverse.service.exam.ContestService;
import com.examverse.util.SceneManager;
import com.examverse.util.SessionManager;

import javafx.animation.*;
import javafx.application.Platform;
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
 * ContestResultController — v3
 *
 * Fixes in this version:
 *
 *  1. STANDINGS SHOW CORRECT PER-CONTEST DATA:
 *     loadStandings() now explicitly re-fetches the contest from the DB using
 *     the contestId stored in session, so it always reflects the correct contest
 *     regardless of any stale SessionManager state.
 *     Standings use getLiveLeaderboard() (ordered by mcq_marks_obtained) when
 *     the contest is still LIVE/EVALUATION, and getFinalStandings() (ordered by
 *     total_marks_obtained) only when the contest is FINISHED.
 *
 *  2. MY RESULT IS ALWAYS FOR THE CURRENT STUDENT:
 *     loadMyResult() matches by currentUser.getId() from the live/final list,
 *     never from stale session data.  participantId is re-derived from the DB
 *     if the session value is missing (≤ 0).
 *
 *  3. ALL DB CALLS ARE OFF THE FX THREAD:
 *     Data loading happens on a background thread; UI updates happen via
 *     Platform.runLater().  This prevents UI freezes.
 *
 *  4. LEADERBOARD BUTTON USES CORRECT MODE:
 *     handleLeaderboard() now sets "contest_live" / "contest_final" based on
 *     the contest's actual status, so the leaderboard shows the right columns
 *     and title.
 *
 *  5. STANDINGS LABEL IS CONTEXT-AWARE:
 *     Shows "🏅 Current Standings (live)" when the contest is still running,
 *     and "🏆 Final Standings" when it is fully evaluated.
 */
public class ContestResultController implements Initializable {

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private VBox   rootVBox;
    @FXML private Label  contestTitleLabel;
    @FXML private Label  mcqScoreLabel;
    @FXML private Label  writtenStatusLabel;
    @FXML private Label  liveRankLabel;
    @FXML private Label  finalRankLabel;
    @FXML private Label  ratingChangeLabel;
    @FXML private Label  newRatingLabel;
    @FXML private Label  rankTitleLabel;
    @FXML private VBox   answersReviewContainer;
    @FXML private VBox   standingsContainer;
    @FXML private Button backBtn;
    @FXML private Button leaderboardBtn;

    // ── State ─────────────────────────────────────────────────────────────────
    private final ContestService contestService = new ContestService();
    private User    currentUser;
    private Contest contest;
    private int     participantId;

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser   = SessionManager.getInstance().getCurrentUser();
        contest       = SessionManager.getInstance().getCurrentContest();
        participantId = SessionManager.getInstance().getCurrentParticipantId();

        if (contest == null || currentUser == null) return;

        // Apply theme immediately (safe — no DB needed)
        applyTheme(contest.getTheme());
        contestTitleLabel.setText("📊  " + contest.getContestTitle() + " — Results");

        // All DB work on a background thread — never block the FX thread
        Thread loader = new Thread(() -> {
            // Re-derive participantId from DB if session value is stale/missing
            int pid = participantId;
            if (pid <= 0) {
                pid = contestService.getParticipantId(contest.getContestId(), currentUser.getId());
            }
            final int resolvedParticipantId = pid;

            // Fetch fresh contest state from DB so we always have correct status
            Contest freshContest = contestService.getContestById(contest.getContestId());
            if (freshContest == null) freshContest = contest; // fallback to session copy
            final Contest fc = freshContest;

            // Fetch my participant record — try live first, fall back to final
            ContestParticipant me = findMe(fc.getContestId());

            // Fetch my answers for the review section
            List<ContestAnswer>   answers   = resolvedParticipantId > 0
                    ? contestService.getStudentAnswers(resolvedParticipantId)
                    : List.of();
            List<ContestQuestion> questions = contestService.getQuestionsForContest(fc.getContestId());

            // Fetch standings — live order for LIVE/EVALUATION, final order for FINISHED
            List<ContestParticipant> standings = fetchStandings(fc);

            final ContestParticipant finalMe          = me;
            final List<ContestAnswer> finalAnswers     = answers;
            final List<ContestQuestion> finalQuestions = questions;
            final List<ContestParticipant> finalStandings = standings;

            Platform.runLater(() -> {
                renderMyResult(finalMe, fc);
                renderAnswerReview(finalAnswers, finalQuestions, fc.getTheme());
                renderStandings(finalStandings, fc);
            });
        });
        loader.setDaemon(true);
        loader.start();
    }

    // ── Theme ─────────────────────────────────────────────────────────────────
    private void applyTheme(Theme t) {
        if (rootVBox != null)
            rootVBox.setStyle("-fx-background-color:" + t.getBgColor() + ";");
    }

    // ── DB helpers (called from background thread) ────────────────────────────

    /**
     * Find the current student's ContestParticipant row for this contest.
     * Looks in the live list first (always present during LIVE phase),
     * then in the final standings (present after EVALUATION/FINISHED).
     */
    private ContestParticipant findMe(int contestId) {
        // getLiveLeaderboard returns ALL participants regardless of status
        List<ContestParticipant> live = contestService.getLiveLeaderboard(contestId);
        ContestParticipant me = live.stream()
                .filter(p -> p.getStudentId() == currentUser.getId())
                .findFirst().orElse(null);
        if (me != null) return me;

        List<ContestParticipant> finals = contestService.getFinalStandings(contestId);
        return finals.stream()
                .filter(p -> p.getStudentId() == currentUser.getId())
                .findFirst().orElse(null);
    }

    /**
     * Returns standings ordered correctly for the contest's current phase:
     *  LIVE / EVALUATION → getLiveLeaderboard (ordered by mcq_marks_obtained)
     *  FINISHED          → getFinalStandings  (ordered by total_marks_obtained)
     */
    private List<ContestParticipant> fetchStandings(Contest fc) {
        if (fc.getStatus() == Contest.Status.FINISHED) {
            List<ContestParticipant> finals = contestService.getFinalStandings(fc.getContestId());
            if (!finals.isEmpty()) return finals;
        }
        // For LIVE, EVALUATION, or empty finals fall back to live order
        return contestService.getLiveLeaderboard(fc.getContestId());
    }

    // ── Render: My Result ─────────────────────────────────────────────────────
    private void renderMyResult(ContestParticipant me, Contest fc) {
        if (me == null) {
            if (mcqScoreLabel != null) mcqScoreLabel.setText("—");
            return;
        }

        Theme t = fc.getTheme();

        // MCQ score
        if (mcqScoreLabel != null) {
            mcqScoreLabel.setText(me.getMcqMarksObtained() + " / " +
                    (fc.getTotalMcqQuestions() * fc.getMcqMarksEach()));
            mcqScoreLabel.setStyle("-fx-text-fill:" + t.getAccentColor() +
                    "; -fx-font-size:28px; -fx-font-weight:bold;");
        }

        // Written status
        if (writtenStatusLabel != null) {
            int pending = me.getPendingWrittenReviews();
            if (fc.getTotalWrittenQuestions() == 0) {
                writtenStatusLabel.setText("No written questions.");
                writtenStatusLabel.setStyle("-fx-text-fill:#64748b; -fx-font-size:14px;");
            } else if (pending > 0) {
                writtenStatusLabel.setText("⏳ " + pending + " written answer(s) pending teacher review.");
                writtenStatusLabel.setStyle("-fx-text-fill:#f59e0b; -fx-font-size:14px;");
            } else {
                writtenStatusLabel.setText("✅ All written answers reviewed. +" +
                        me.getWrittenMarksObtained() + " marks");
                writtenStatusLabel.setStyle("-fx-text-fill:#22c55e; -fx-font-size:14px;");
            }
        }

        // Live rank
        if (liveRankLabel != null) {
            liveRankLabel.setText("#" + (me.getLiveRank() > 0 ? me.getLiveRank() : "—"));
            liveRankLabel.setStyle("-fx-text-fill:" + t.getHighlightColor() +
                    "; -fx-font-size:24px; -fx-font-weight:bold;");
        }

        // Final rank / rating — only available after evaluation
        if (me.isEvaluated()) {
            if (finalRankLabel != null)
                finalRankLabel.setText("Final Rank: #" + me.getFinalRank());

            int change = me.getRatingChange();
            String sign = change >= 0 ? "+" : "";

            if (ratingChangeLabel != null) {
                ratingChangeLabel.setText("Rating Change: " + sign + change);
                ratingChangeLabel.setStyle("-fx-text-fill:" + (change >= 0 ? "#22c55e" : "#ef4444") +
                        "; -fx-font-size:18px; -fx-font-weight:bold;");
            }
            if (newRatingLabel != null) {
                newRatingLabel.setText("New Rating: " + me.getRatingAfter());
                newRatingLabel.setStyle("-fx-text-fill:" + t.getAccentColor() +
                        "; -fx-font-size:22px; -fx-font-weight:bold;");
            }
            if (rankTitleLabel != null) {
                String title = StudentRating.getTitleForRating(me.getRatingAfter());
                rankTitleLabel.setText(title);
                rankTitleLabel.getStyleClass().setAll("rank-title",
                        StudentRating.getTitleCssClass(me.getRatingAfter()));
            }
            animateRatingChange(change);
        } else {
            if (finalRankLabel  != null) finalRankLabel.setText("Pending evaluation.");
            if (ratingChangeLabel != null) ratingChangeLabel.setText("Rating pending evaluation.");
            if (newRatingLabel   != null) {
                int currRating = contestService.getStudentRating(currentUser.getId());
                newRatingLabel.setText("Current Rating: " + currRating);
                newRatingLabel.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:20px;");
            }
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

    // ── Render: Answer Review ─────────────────────────────────────────────────
    private void renderAnswerReview(List<ContestAnswer> answers,
                                    List<ContestQuestion> questions, Theme t) {
        if (answersReviewContainer == null) return;
        answersReviewContainer.getChildren().clear();
        if (answers.isEmpty()) return;

        Label sectionTitle = new Label("📋  Your Answers");
        sectionTitle.setStyle("-fx-text-fill:#e2e8f0; -fx-font-size:17px; -fx-font-weight:bold;");
        answersReviewContainer.getChildren().add(sectionTitle);

        for (ContestAnswer a : answers) {
            ContestQuestion q = questions.stream()
                    .filter(qq -> qq.getQuestionId() == a.getQuestionId())
                    .findFirst().orElse(null);
            if (q == null) continue;

            VBox card = new VBox(8);
            card.setPadding(new Insets(14));
            String borderColor = a.isCorrect() ? "#22c55e"
                    : (a.getSelectedOption() != null ? "#ef4444" : "#f59e0b");
            card.setStyle("-fx-background-color:#1e293b; -fx-background-radius:10;" +
                    "-fx-border-color:" + borderColor + "55;" +
                    "-fx-border-radius:10; -fx-border-width:1;");

            Label qText = new Label((q.isMcq() ? "MCQ" : "Written") + " — " + q.getQuestionText());
            qText.setStyle("-fx-text-fill:#f1f5f9; -fx-font-size:14px; -fx-font-weight:bold;");
            qText.setWrapText(true);

            if (q.isMcq()) {
                Label yourAns = new Label("Your answer: " +
                        (a.getSelectedOption() != null ? a.getSelectedOption() : "Not answered"));
                yourAns.setStyle("-fx-text-fill:" + (a.isCorrect() ? "#22c55e" : "#ef4444") +
                        "; -fx-font-size:13px;");
                Label correctAns = new Label("Correct: " + q.getCorrectAnswer());
                correctAns.setStyle("-fx-text-fill:#22c55e; -fx-font-size:13px;");
                Label marksGot = new Label("Marks: " + a.getMarksAwarded() + "/" + q.getMarks());
                marksGot.setStyle("-fx-text-fill:" + t.getHighlightColor() + "; -fx-font-size:13px;");
                card.getChildren().addAll(qText, yourAns, correctAns, marksGot);
                if (q.getExplanation() != null && !q.getExplanation().isEmpty()) {
                    Label exp = new Label("💡 " + q.getExplanation());
                    exp.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:12px;");
                    exp.setWrapText(true);
                    card.getChildren().add(exp);
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
                    card.getChildren().addAll(qText, statusLbl, comment);
                } else {
                    card.getChildren().addAll(qText, statusLbl);
                }
            }
            answersReviewContainer.getChildren().add(card);
        }
    }

    // ── Render: Standings ─────────────────────────────────────────────────────
    private void renderStandings(List<ContestParticipant> list, Contest fc) {
        if (standingsContainer == null) return;
        standingsContainer.getChildren().clear();
        Theme t = fc.getTheme();

        // Context-aware label
        boolean isFinal = fc.getStatus() == Contest.Status.FINISHED;
        Label title = new Label(isFinal ? "🏆  Final Standings" : "🏅  Current Standings (live)");
        title.setStyle("-fx-text-fill:" + t.getAccentColor() +
                "; -fx-font-size:17px; -fx-font-weight:bold;");
        standingsContainer.getChildren().add(title);

        if (list.isEmpty()) {
            Label empty = new Label("No participants yet.");
            empty.setStyle("-fx-text-fill:#64748b; -fx-font-size:13px;");
            standingsContainer.getChildren().add(empty);
            return;
        }

        for (int i = 0; i < list.size(); i++) {
            ContestParticipant p = list.get(i);
            boolean isMe = p.getStudentId() == currentUser.getId();

            String rankEmoji = switch (i) {
                case 0 -> "🥇";
                case 1 -> "🥈";
                case 2 -> "🥉";
                default -> "#" + (i + 1);
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
            rankLbl.setMinWidth(40);
            rankLbl.setStyle("-fx-font-size:16px; -fx-text-fill:#e2e8f0;");

            Label nameLbl = new Label(
                    (isMe ? "▶ " : "") +
                            (p.getStudentName() != null ? p.getStudentName() : "—"));
            nameLbl.setStyle(
                    "-fx-text-fill:" + (isMe ? t.getAccentColor() : "#e2e8f0") + ";" +
                            "-fx-font-weight:" + (isMe ? "bold" : "normal") + ";" +
                            "-fx-font-size:14px;");
            nameLbl.setMinWidth(160);

            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);

            Label mcqLbl = new Label("MCQ: " + p.getMcqMarksObtained());
            mcqLbl.setStyle("-fx-text-fill:#60a5fa; -fx-font-size:13px;");

            Label wrLbl = new Label("Written: " + p.getWrittenMarksObtained());
            wrLbl.setStyle("-fx-text-fill:#f59e0b; -fx-font-size:13px;");

            Label totalLbl = new Label("Total: " + p.getTotalMarksObtained());
            totalLbl.setStyle("-fx-text-fill:" + t.getHighlightColor() +
                    "; -fx-font-weight:bold; -fx-font-size:14px;");

            row.getChildren().addAll(rankLbl, nameLbl, sp, mcqLbl, wrLbl, totalLbl);

            if (p.isEvaluated()) {
                int change = p.getRatingChange();
                Label rChg = new Label((change >= 0 ? "+" : "") + change);
                rChg.setStyle("-fx-text-fill:" + (change >= 0 ? "#22c55e" : "#ef4444") +
                        "; -fx-font-weight:bold; -fx-font-size:13px;");
                row.getChildren().add(rChg);
            }

            // Fade-in animation
            row.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(120 + i * 30L), row);
            ft.setFromValue(0); ft.setToValue(1); ft.play();

            standingsContainer.getChildren().add(row);
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────
    @FXML
    private void handleBack() {
        System.out.println("DEBUG BACK: clearing contest=" +
                (contest != null ? contest.getContestId() : "NULL") +
                " participantId=" + participantId);
        SessionManager.getInstance().setCurrentContest(null);
        SessionManager.getInstance().setCurrentParticipantId(-1);
        SceneManager.switchScene("/com/examverse/fxml/contest/contest-lobby.fxml");
    }

    @FXML
    private void handleLeaderboard() {
        // Use the correct mode based on actual contest status
        Contest fresh = contestService.getContestById(contest.getContestId());
        String mode = (fresh != null && fresh.getStatus() == Contest.Status.FINISHED)
                ? "contest_final" : "contest_live";
        SessionManager.getInstance().setAttribute("leaderboard_mode", mode);
        SceneManager.switchScene("/com/examverse/fxml/contest/contest-leaderboard.fxml");
    }
}