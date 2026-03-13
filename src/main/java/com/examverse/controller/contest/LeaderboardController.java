package com.examverse.controller.contest;

import com.examverse.model.exam.Contest;
import com.examverse.model.user.StudentRating;
import com.examverse.service.exam.ContestService;
import com.examverse.util.SceneManager;
import com.examverse.util.SessionManager;
import com.examverse.model.user.User;

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
 * LeaderboardController — FIXED v2
 *
 * Bug fixes:
 *  1. When opened from ContestManagerController (leaderboard_mode = "contest"),
 *     loads ONLY students who participated in that specific contest via
 *     contestService.getContestLeaderboard(contestId). Non-participants like
 *     "Rafid" will never appear here.
 *
 *  2. When opened from student lobby (leaderboard_mode = "global" or unset),
 *     loads the global leaderboard as before.
 *
 *  3. "My Rating" panel at the bottom is hidden for admin/teacher users.
 *     Admins have no rating — the panel would show 800 (default) which is wrong.
 *
 *  4. getGlobalLeaderboard() now filters WHERE user_type = 'STUDENT' in the
 *     service, so admins never appear even if they have a student_ratings row.
 */
public class LeaderboardController implements Initializable {

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private VBox  rootVBox;
    @FXML private VBox  leaderboardContent;
    @FXML private Label myRankLabel;
    @FXML private Label myRatingLabel;
    @FXML private Label myTitleLabel;
    @FXML private HBox  myStatsRow;   // The bottom "my rating" panel — hide for admins
    @FXML private Button backBtn;
    @FXML private TextField searchField;
    @FXML private Label pageTitleLabel; // Optional: shows "Contest Leaderboard" vs "Global"

    // ── State ─────────────────────────────────────────────────────────────────
    private final ContestService contestService = new ContestService();
    private User currentUser;
    private List<StudentRating> allRatings;
    private boolean isContestMode = false;
    private Contest currentContest = null;

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser    = SessionManager.getInstance().getCurrentUser();
        currentContest = SessionManager.getInstance().getCurrentContest();

        // Determine mode: "contest" = opened from admin manager for a specific contest
        //                 "global"  = opened from student lobby
        String mode = (String) SessionManager.getInstance().getAttribute("leaderboard_mode");
        isContestMode = "contest".equals(mode) && currentContest != null;

        if (rootVBox != null) rootVBox.setStyle("-fx-background-color:#0a0a1a;");

        // Update page title if FXML has a label for it
        if (pageTitleLabel != null) {
            if (isContestMode) {
                pageTitleLabel.setText("🏆 " + currentContest.getContestTitle() + " — Leaderboard");
            } else {
                pageTitleLabel.setText("🌐 Global Leaderboard");
            }
        }

        loadLeaderboard();
        setupSearch();

        // Hide "My Rating" panel for admins/teachers — they have no rating
        boolean isAdmin = currentUser != null && currentUser.isAdmin();
        if (myStatsRow != null) {
            myStatsRow.setVisible(!isAdmin);
            myStatsRow.setManaged(!isAdmin);
        }
    }

    // ── Load ──────────────────────────────────────────────────────────────────
    private void loadLeaderboard() {
        if (isContestMode) {
            // Load only participants of this specific contest
            allRatings = contestService.getContestLeaderboard(currentContest.getContestId());
        } else {
            // Load global leaderboard (already filtered to STUDENT users only in service)
            allRatings = contestService.getGlobalLeaderboard(200);
        }
        renderList(allRatings);
        loadMyRank();
    }

    private void loadMyRank() {
        if (currentUser == null) return;

        // Admins have no rating — hide the panel (already handled in initialize)
        if (currentUser.isAdmin()) return;

        for (int i = 0; i < allRatings.size(); i++) {
            StudentRating r = allRatings.get(i);
            if (r.getStudentId() == currentUser.getId()) {
                if (myRankLabel  != null) myRankLabel.setText("Your Rank: #" + (i + 1));
                if (myRatingLabel!= null) myRatingLabel.setText("Rating: " + r.getCurrentRating());
                if (myTitleLabel != null) {
                    myTitleLabel.setText(r.getRankTitle());
                    myTitleLabel.getStyleClass().setAll("rank-title", r.getRankCssClass());
                }
                return;
            }
        }

        // Student not ranked yet (hasn't participated in any contest)
        int rating = contestService.getStudentRating(currentUser.getId());
        if (myRankLabel  != null) myRankLabel.setText("Your Rank: Unranked");
        if (myRatingLabel!= null) myRatingLabel.setText("Rating: " + rating);
        if (myTitleLabel != null) {
            myTitleLabel.setText(StudentRating.getTitleForRating(rating));
        }
    }

    // ── Render ────────────────────────────────────────────────────────────────
    private void renderList(List<StudentRating> list) {
        leaderboardContent.getChildren().clear();

        // Header
        leaderboardContent.getChildren().add(buildHeaderRow());

        String lastTitle = null;

        for (int i = 0; i < list.size(); i++) {
            StudentRating r  = list.get(i);
            boolean isMe = currentUser != null
                    && !currentUser.isAdmin()
                    && r.getStudentId() == currentUser.getId();

            // Section divider when rank title changes
            String title = StudentRating.getTitleForRating(r.getCurrentRating());
            if (!title.equals(lastTitle)) {
                leaderboardContent.getChildren().add(buildRankDivider(title, r.getCurrentRating()));
                lastTitle = title;
            }

            HBox row = buildStudentRow(i + 1, r, isMe);
            row.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(150 + i * 20L), row);
            ft.setFromValue(0); ft.setToValue(1);
            ft.play();
            leaderboardContent.getChildren().add(row);
        }

        if (list.isEmpty()) {
            Label empty = new Label(isContestMode
                    ? "🏆 No participants yet for this contest."
                    : "🏆 No players ranked yet. Participate in a contest!");
            empty.setStyle("-fx-text-fill:#64748b; -fx-font-size:16px;");
            empty.setPadding(new Insets(60));
            leaderboardContent.getChildren().add(empty);
        }
    }

    private HBox buildHeaderRow() {
        HBox row = new HBox();
        row.setPadding(new Insets(8, 16, 8, 16));
        row.setStyle("-fx-background-color:#1e293b; -fx-background-radius:8;");
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().addAll(
                styledHeader("Rank",     60),
                styledHeader("Player",   200),
                styledHeader("Title",    160),
                styledHeader("Rating",   90),
                styledHeader("Peak",     90),
                styledHeader("Contests", 90),
                styledHeader("Wins",     70)
        );
        return row;
    }

    private Label styledHeader(String text, double width) {
        Label l = new Label(text);
        l.setMinWidth(width);
        l.setStyle("-fx-text-fill:#64748b; -fx-font-size:12px; -fx-font-weight:bold;");
        return l;
    }

    private Label buildRankDivider(String title, int rating) {
        String color = rankColor(StudentRating.getTitleCssClass(rating));
        Label l = new Label("  " + title);
        l.setStyle("-fx-text-fill:" + color + "; -fx-font-size:13px; -fx-font-weight:bold;" +
                "-fx-padding:12 16 4 16;");
        return l;
    }

    private HBox buildStudentRow(int rank, StudentRating r, boolean isMe) {
        String cssClass = r.getRankCssClass();
        String color    = rankColor(cssClass);

        HBox row = new HBox();
        row.setPadding(new Insets(12, 16, 12, 16));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle(
                "-fx-background-color:" + (isMe ? color + "18" : "#111827") + ";" +
                        "-fx-background-radius:8;" +
                        (isMe ? "-fx-border-color:" + color + "66; -fx-border-radius:8; -fx-border-width:1;" : "") +
                        "-fx-cursor:hand;"
        );

        String rankText = switch (rank) {
            case 1 -> "🥇";
            case 2 -> "🥈";
            case 3 -> "🥉";
            default -> "#" + rank;
        };
        Label rankLbl = new Label(rankText);
        rankLbl.setMinWidth(60);
        rankLbl.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:" + (rank <= 3 ? "18" : "14") + "px;");

        Label nameLbl = new Label((isMe ? "▶ " : "") +
                (r.getStudentName() != null ? r.getStudentName() : "—"));
        nameLbl.setMinWidth(200);
        nameLbl.setStyle("-fx-text-fill:" + (isMe ? color : "#f1f5f9") + ";" +
                "-fx-font-weight:" + (isMe ? "bold" : "normal") + ";" +
                "-fx-font-size:14px;");

        Label titleLbl = new Label(r.getRankTitle());
        titleLbl.setMinWidth(160);
        titleLbl.setStyle("-fx-text-fill:" + color + "; -fx-font-size:13px; -fx-font-weight:bold;");

        Label ratingLbl = new Label(String.valueOf(r.getCurrentRating()));
        ratingLbl.setMinWidth(90);
        ratingLbl.setStyle("-fx-text-fill:" + color + "; -fx-font-size:16px; -fx-font-weight:bold;");

        Label peakLbl = new Label(String.valueOf(r.getPeakRating()));
        peakLbl.setMinWidth(90);
        peakLbl.setStyle("-fx-text-fill:#64748b; -fx-font-size:13px;");

        Label contestsLbl = new Label(String.valueOf(r.getContestsParticipated()));
        contestsLbl.setMinWidth(90);
        contestsLbl.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:13px;");

        Label winsLbl = new Label(r.getContestsWon() > 0 ? "🏆 " + r.getContestsWon() : "—");
        winsLbl.setMinWidth(70);
        winsLbl.setStyle("-fx-text-fill:#fbbf24; -fx-font-size:13px;");

        row.getChildren().addAll(rankLbl, nameLbl, titleLbl, ratingLbl, peakLbl, contestsLbl, winsLbl);
        return row;
    }

    private String rankColor(String cssClass) {
        return switch (cssClass) {
            case "rank-legend"   -> "#fbbf24";
            case "rank-champion" -> "#a78bfa";
            case "rank-expert"   -> "#60a5fa";
            case "rank-advanced" -> "#f97316";
            case "rank-skilled"  -> "#34d399";
            case "rank-learner"  -> "#94a3b8";
            default              -> "#6b7280";
        };
    }

    // ── Search ────────────────────────────────────────────────────────────────
    private void setupSearch() {
        if (searchField == null) return;
        searchField.textProperty().addListener((obs, ov, nv) -> {
            if (nv == null || nv.isEmpty()) {
                renderList(allRatings);
            } else {
                String lower = nv.toLowerCase();
                List<StudentRating> filtered = allRatings.stream()
                        .filter(r -> (r.getStudentName() != null &&
                                r.getStudentName().toLowerCase().contains(lower)) ||
                                (r.getUsername() != null &&
                                        r.getUsername().toLowerCase().contains(lower)))
                        .toList();
                renderList(filtered);
            }
        });
    }

    // ── Navigation ────────────────────────────────────────────────────────────
    @FXML
    private void handleBack() {
        // Clear the mode attribute
        SessionManager.getInstance().removeAttribute("leaderboard_mode");

        User u = SessionManager.getInstance().getCurrentUser();
        if (u != null && u.isAdmin()) {
            SceneManager.switchScene("/com/examverse/fxml/contest/contest-manager.fxml");
        } else {
            SceneManager.switchScene("/com/examverse/fxml/contest/contest-lobby.fxml");
        }
    }
}