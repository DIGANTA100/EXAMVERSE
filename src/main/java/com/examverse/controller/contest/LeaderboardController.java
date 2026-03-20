package com.examverse.controller.contest;

import com.examverse.model.exam.Contest;
import com.examverse.model.exam.ContestParticipant;
import com.examverse.model.user.StudentRating;
import com.examverse.service.exam.ContestService;
import com.examverse.util.SceneManager;
import com.examverse.util.SessionManager;
import com.examverse.model.user.User;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * LeaderboardController — v4
 *
 * Fixes in this version:
 *
 *  1. FULL ROW COLORING (global mode):
 *     Every element of a player's row — name, rating, peak, contests, wins —
 *     is now tinted with that player's rank-title color. Previously only the
 *     "Title" and "Rating" labels were colored; everything else was grey.
 *
 *  2. MY-STATS PILL COLORING + VISIBILITY:
 *     The top-right "Your Rank / Rating / Title" pill now uses the student's
 *     rank-title color for all three labels. The title text is explicitly
 *     visible with proper font-size + inline color — no more invisible text.
 *
 *  3. THREE DISTINCT LEADERBOARD MODES:
 *     "global"        → "🌐 Global Leaderboard"             (global columns)
 *     "contest_live"  → "ContestName — Current Standings"   (contest columns)
 *     "contest_final" → "ContestName — Final Standings"     (contest columns)
 *
 *  4. "YOU ARE HERE" POINTER:
 *     In all modes the current student's row gets a blinking "▶" indicator,
 *     a colored glow border, and the view auto-scrolls to that row after
 *     the layout pass completes.
 *
 *  5. CONTEST ROWS — ALL ELEMENTS COLORED:
 *     Every value in a contest-standings row uses the player's title color.
 */
public class LeaderboardController implements Initializable {

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private VBox       rootVBox;
    @FXML private VBox       leaderboardContent;
    @FXML private Label      myRankLabel;
    @FXML private Label      myRatingLabel;
    @FXML private Label      myTitleLabel;
    @FXML private HBox       myStatsRow;      // hidden entirely for admins
    @FXML private Button     backBtn;
    @FXML private TextField  searchField;
    @FXML private Label      pageTitleLabel;  // set dynamically

    // ── State ─────────────────────────────────────────────────────────────────
    private final ContestService contestService = new ContestService();
    private User   currentUser;
    private List<StudentRating>      allRatings;
    private List<ContestParticipant> contestParticipants;

    // Three modes: "global" | "contest_live" | "contest_final"
    private String  leaderboardMode = "global";
    private boolean isContestMode   = false;
    private Contest currentContest  = null;

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser    = SessionManager.getInstance().getCurrentUser();
        currentContest = SessionManager.getInstance().getCurrentContest();

        String mode = (String) SessionManager.getInstance().getAttribute("leaderboard_mode");
        leaderboardMode = (mode != null) ? mode : "global";
        isContestMode   = leaderboardMode.startsWith("contest") && currentContest != null;

        if (rootVBox != null) rootVBox.setStyle("-fx-background-color:#0a0a1a;");

        // ── Page title ────────────────────────────────────────────────────────
        if (pageTitleLabel != null) {
            if (isContestMode) {
                String suffix = "contest_final".equals(leaderboardMode)
                        ? "Final Standings"
                        : "Current Standings";
                pageTitleLabel.setText("🏆  " + currentContest.getContestTitle() + " — " + suffix);
            } else {
                pageTitleLabel.setText("🌐  Global Leaderboard");
            }
        }

        loadLeaderboard();
        setupSearch();

        // ── Hide My Stats panel for admins ────────────────────────────────────
        boolean isAdmin = currentUser != null && currentUser.isAdmin();
        if (myStatsRow != null) {
            myStatsRow.setVisible(!isAdmin);
            myStatsRow.setManaged(!isAdmin);
        }
    }

    // ── Load ──────────────────────────────────────────────────────────────────
    private void loadLeaderboard() {
        if (isContestMode) {
            // Use the correct ordering for each mode:
            //   live  → ordered by mcq_marks_obtained (what the student scored in THIS contest)
            //   final → ordered by total_marks_obtained (mcq + written)
            // In both cases the ORDER comes from contest_participants, NOT global rating.
            if ("contest_final".equals(leaderboardMode)) {
                allRatings          = contestService.getContestLeaderboardFinal(currentContest.getContestId());
                contestParticipants = contestService.getFinalStandings(currentContest.getContestId());
                if (contestParticipants == null || contestParticipants.isEmpty())
                    contestParticipants = contestService.getLiveLeaderboard(currentContest.getContestId());
            } else {
                // contest_live: order by MCQ score
                allRatings          = contestService.getContestLeaderboard(currentContest.getContestId());
                contestParticipants = contestService.getLiveLeaderboard(currentContest.getContestId());
                if (contestParticipants == null || contestParticipants.isEmpty())
                    contestParticipants = contestService.getFinalStandings(currentContest.getContestId());
            }
        } else {
            allRatings          = contestService.getGlobalLeaderboard(200);
            contestParticipants = null;
        }
        renderList(allRatings);
        loadMyRank();
    }

    // ── My-stats pill ─────────────────────────────────────────────────────────
    private void loadMyRank() {
        if (currentUser == null || currentUser.isAdmin()) return;

        int    globalRating = contestService.getStudentRating(currentUser.getId());
        String titleColor   = rankColor(StudentRating.getTitleCssClass(globalRating));
        String titleText    = StudentRating.getTitleForRating(globalRating);

        for (int i = 0; i < allRatings.size(); i++) {
            StudentRating r = allRatings.get(i);
            if (r.getStudentId() == currentUser.getId()) {
                applyMyStatsPill(i + 1, r, titleColor, titleText);
                return;
            }
        }

        // Not ranked yet
        applyMyStatsPillUnranked(globalRating, titleColor, titleText);
    }

    private void applyMyStatsPill(int rank, StudentRating r, String titleColor, String titleText) {
        if (myRankLabel != null) {
            myRankLabel.setText("Your Rank: #" + rank);
            myRankLabel.setStyle("-fx-text-fill:" + titleColor + "; -fx-font-size:13px;");
        }
        if (myRatingLabel != null) {
            if (isContestMode) {
                ContestParticipant cp = findParticipant(r.getStudentId());
                int pts = cp != null ? cp.getTotalMarksObtained() : 0;
                myRatingLabel.setText("Points: " + pts);
            } else {
                myRatingLabel.setText("Rating: " + r.getCurrentRating());
            }
            myRatingLabel.setStyle("-fx-text-fill:" + titleColor +
                    "; -fx-font-size:15px; -fx-font-weight:bold;");
        }
        if (myTitleLabel != null) {
            myTitleLabel.setText(titleText);
            myTitleLabel.setStyle("-fx-text-fill:" + titleColor +
                    "; -fx-font-size:13px; -fx-font-weight:bold;");
        }
    }

    private void applyMyStatsPillUnranked(int rating, String titleColor, String titleText) {
        if (myRankLabel != null) {
            myRankLabel.setText("Your Rank: Unranked");
            myRankLabel.setStyle("-fx-text-fill:" + titleColor + "; -fx-font-size:13px;");
        }
        if (myRatingLabel != null) {
            myRatingLabel.setText(isContestMode ? "Points: 0" : "Rating: " + rating);
            myRatingLabel.setStyle("-fx-text-fill:" + titleColor +
                    "; -fx-font-size:15px; -fx-font-weight:bold;");
        }
        if (myTitleLabel != null) {
            myTitleLabel.setText(titleText);
            myTitleLabel.setStyle("-fx-text-fill:" + titleColor +
                    "; -fx-font-size:13px; -fx-font-weight:bold;");
        }
    }

    // ── Render ────────────────────────────────────────────────────────────────
    private void renderList(List<StudentRating> list) {
        leaderboardContent.getChildren().clear();
        leaderboardContent.getChildren().add(
                isContestMode ? buildContestHeaderRow() : buildGlobalHeaderRow());

        Node   myRowNode = null;
        String lastTitle = null;

        for (int i = 0; i < list.size(); i++) {
            StudentRating r = list.get(i);
            boolean isMe = currentUser != null
                    && !currentUser.isAdmin()
                    && r.getStudentId() == currentUser.getId();

            // Tier dividers — global mode only
            if (!isContestMode) {
                String tierTitle = StudentRating.getTitleForRating(r.getCurrentRating());
                if (!tierTitle.equals(lastTitle)) {
                    leaderboardContent.getChildren().add(
                            buildRankDivider(tierTitle, r.getCurrentRating()));
                    lastTitle = tierTitle;
                }
            }

            HBox row = isContestMode
                    ? buildContestRow(i + 1, r, isMe)
                    : buildGlobalRow(i + 1, r, isMe);

            row.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(150 + i * 18L), row);
            ft.setFromValue(0); ft.setToValue(1);
            ft.play();

            leaderboardContent.getChildren().add(row);
            if (isMe) myRowNode = row;
        }

        if (list.isEmpty()) {
            Label empty = new Label(isContestMode
                    ? "🏆 No participants yet for this contest."
                    : "🏆 No players ranked yet. Participate in a contest!");
            empty.setStyle("-fx-text-fill:#64748b; -fx-font-size:16px;");
            empty.setPadding(new Insets(60));
            leaderboardContent.getChildren().add(empty);
        }

        // Auto-scroll to "you are here" row (contest mode where it matters most)
        if (myRowNode != null) {
            final Node target = myRowNode;
            Platform.runLater(() -> scrollToNode(target));
        }
    }

    /** Smoothly scrolls the ancestor ScrollPane so targetNode is centered. */
    private void scrollToNode(Node targetNode) {
        double nodeY  = targetNode.getBoundsInParent().getMinY();
        double totalH = leaderboardContent.getBoundsInLocal().getHeight();
        double viewH  = (rootVBox != null && rootVBox.getScene() != null)
                ? rootVBox.getScene().getHeight() : 600;

        Node parent = (rootVBox != null) ? rootVBox.getParent() : null;
        while (parent != null && !(parent instanceof ScrollPane)) {
            parent = parent.getParent();
        }
        if (parent instanceof ScrollPane sp) {
            double ratio = Math.max(0.0, Math.min(1.0,
                    (nodeY - viewH / 2.0) / Math.max(1, totalH - viewH)));
            Timeline tl = new Timeline(
                    new KeyFrame(Duration.millis(420),
                            new KeyValue(sp.vvalueProperty(), ratio, Interpolator.EASE_BOTH)));
            tl.play();
        }
    }

    // ── Header rows ───────────────────────────────────────────────────────────
    private HBox buildContestHeaderRow() {
        HBox row = new HBox();
        row.setPadding(new Insets(8, 16, 8, 16));
        row.setStyle("-fx-background-color:#1e293b; -fx-background-radius:8;");
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().addAll(
                styledHeader("",           34),
                styledHeader("Rank",       55),
                styledHeader("Player",    185),
                styledHeader("Title",     145),
                styledHeader("MCQ Pts",    80),
                styledHeader("Written",    80),
                styledHeader("Total Pts",  90),
                styledHeader("Solved",     70),
                styledHeader("Status",    100)
        );
        return row;
    }

    private HBox buildGlobalHeaderRow() {
        HBox row = new HBox();
        row.setPadding(new Insets(8, 16, 8, 16));
        row.setStyle("-fx-background-color:#1e293b; -fx-background-radius:8;");
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().addAll(
                styledHeader("",           34),
                styledHeader("Rank",       60),
                styledHeader("Player",    195),
                styledHeader("Title",     155),
                styledHeader("Rating",     90),
                styledHeader("Peak",       90),
                styledHeader("Contests",   90),
                styledHeader("Wins",       70)
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

    // ── Contest-mode row — all elements use player's title color ──────────────
    private HBox buildContestRow(int rank, StudentRating r, boolean isMe) {
        ContestParticipant cp = findParticipant(r.getStudentId());
        String color          = rankColor(r.getRankCssClass());

        HBox row = new HBox();
        row.setPadding(new Insets(11, 16, 11, 16));
        row.setAlignment(Pos.CENTER_LEFT);

        if (isMe) {
            row.setStyle(
                    "-fx-background-color:" + color + "1a;" +
                            "-fx-background-radius:8;" +
                            "-fx-border-color:" + color + ";" +
                            "-fx-border-radius:8; -fx-border-width:1.5;" +
                            "-fx-effect: dropshadow(gaussian," + color + "88,14,0.4,0,0);"
            );
        } else {
            row.setStyle("-fx-background-color:#111827; -fx-background-radius:8;");
        }

        // Pointer
        Label pointerLbl = new Label(isMe ? "▶" : "");
        pointerLbl.setMinWidth(34);
        pointerLbl.setStyle("-fx-text-fill:" + color + "; -fx-font-size:14px; -fx-font-weight:bold;");
        if (isMe) {
            FadeTransition blink = new FadeTransition(Duration.millis(700), pointerLbl);
            blink.setFromValue(1.0); blink.setToValue(0.25);
            blink.setCycleCount(Animation.INDEFINITE); blink.setAutoReverse(true); blink.play();
        }

        // Rank
        String rankText = switch (rank) {
            case 1 -> "🥇"; case 2 -> "🥈"; case 3 -> "🥉"; default -> "#" + rank;
        };
        Label rankLbl = new Label(rankText);
        rankLbl.setMinWidth(55);
        rankLbl.setStyle("-fx-text-fill:" + color + "; -fx-font-size:" + (rank <= 3 ? "18" : "14") + "px;");

        // Name
        Label nameLbl = new Label(r.getStudentName() != null ? r.getStudentName() : "—");
        nameLbl.setMinWidth(185);
        nameLbl.setStyle("-fx-text-fill:" + color + ";" +
                "-fx-font-weight:" + (isMe ? "bold" : "normal") + "; -fx-font-size:14px;");

        // Title
        Label titleLbl = new Label(r.getRankTitle());
        titleLbl.setMinWidth(145);
        titleLbl.setStyle("-fx-text-fill:" + color + "; -fx-font-size:12px; -fx-font-weight:bold;");

        // Scores
        int mcqPts   = cp != null ? cp.getMcqMarksObtained()    : 0;
        int writePts = cp != null ? cp.getWrittenMarksObtained() : 0;
        int totalPts = cp != null ? cp.getTotalMarksObtained()   : 0;
        int marksEach = (currentContest != null && currentContest.getMcqMarksEach() > 0)
                ? currentContest.getMcqMarksEach() : 1;
        int solved = mcqPts / marksEach;

        Label mcqLbl = new Label(String.valueOf(mcqPts));
        mcqLbl.setMinWidth(80);
        mcqLbl.setStyle("-fx-text-fill:" + color + "; -fx-font-size:14px; -fx-font-weight:bold;");

        Label writeLbl = new Label(writePts > 0 ? String.valueOf(writePts) : "—");
        writeLbl.setMinWidth(80);
        writeLbl.setStyle("-fx-text-fill:" + color + "; -fx-font-size:14px;");

        Label totalLbl = new Label(String.valueOf(totalPts));
        totalLbl.setMinWidth(90);
        totalLbl.setStyle("-fx-text-fill:" + color + "; -fx-font-size:16px; -fx-font-weight:bold;");

        Label solvedLbl = new Label(solved + " ✔");
        solvedLbl.setMinWidth(70);
        solvedLbl.setStyle("-fx-text-fill:" + color + "; -fx-font-size:13px;");

        // Status
        String statusText = "—";
        if (cp != null) {
            statusText = switch (cp.getStatus()) {
                case REGISTERED -> "Registered";
                case ACTIVE     -> "🟢 Active";
                case SUBMITTED  -> "✅ Submitted";
                case EVALUATED  -> "🏁 Evaluated";
            };
        }
        Label statusLbl = new Label(statusText);
        statusLbl.setMinWidth(100);
        statusLbl.setStyle("-fx-text-fill:" + color + "; -fx-font-size:12px;");

        row.getChildren().addAll(
                pointerLbl, rankLbl, nameLbl, titleLbl,
                mcqLbl, writeLbl, totalLbl, solvedLbl, statusLbl);
        return row;
    }

    // ── Global-mode row — all fields colored by player's rank title ───────────
    private HBox buildGlobalRow(int rank, StudentRating r, boolean isMe) {
        String color = rankColor(r.getRankCssClass());

        HBox row = new HBox();
        row.setPadding(new Insets(11, 16, 11, 16));
        row.setAlignment(Pos.CENTER_LEFT);

        if (isMe) {
            row.setStyle(
                    "-fx-background-color:" + color + "1a;" +
                            "-fx-background-radius:8;" +
                            "-fx-border-color:" + color + ";" +
                            "-fx-border-radius:8; -fx-border-width:1.5;" +
                            "-fx-effect: dropshadow(gaussian," + color + "88,14,0.4,0,0);"
            );
        } else {
            row.setStyle("-fx-background-color:#111827; -fx-background-radius:8;");
        }

        // Pointer — blinking for "me"
        Label pointerLbl = new Label(isMe ? "▶" : "");
        pointerLbl.setMinWidth(34);
        pointerLbl.setStyle("-fx-text-fill:" + color + "; -fx-font-size:14px; -fx-font-weight:bold;");
        if (isMe) {
            FadeTransition blink = new FadeTransition(Duration.millis(700), pointerLbl);
            blink.setFromValue(1.0); blink.setToValue(0.25);
            blink.setCycleCount(Animation.INDEFINITE); blink.setAutoReverse(true); blink.play();
        }

        // Rank
        String rankText = switch (rank) {
            case 1 -> "🥇"; case 2 -> "🥈"; case 3 -> "🥉"; default -> "#" + rank;
        };
        Label rankLbl = new Label(rankText);
        rankLbl.setMinWidth(60);
        rankLbl.setStyle("-fx-text-fill:" + color + "; -fx-font-size:" + (rank <= 3 ? "18" : "14") + "px;");

        // Name — bold for me
        Label nameLbl = new Label(r.getStudentName() != null ? r.getStudentName() : "—");
        nameLbl.setMinWidth(195);
        nameLbl.setStyle("-fx-text-fill:" + color + ";" +
                "-fx-font-weight:" + (isMe ? "bold" : "normal") + "; -fx-font-size:14px;");

        // Title
        Label titleLbl = new Label(r.getRankTitle());
        titleLbl.setMinWidth(155);
        titleLbl.setStyle("-fx-text-fill:" + color + "; -fx-font-size:13px; -fx-font-weight:bold;");

        // Rating
        Label ratingLbl = new Label(String.valueOf(r.getCurrentRating()));
        ratingLbl.setMinWidth(90);
        ratingLbl.setStyle("-fx-text-fill:" + color + "; -fx-font-size:16px; -fx-font-weight:bold;");

        // Peak — same color, slightly dimmed
        Label peakLbl = new Label(String.valueOf(r.getPeakRating()));
        peakLbl.setMinWidth(90);
        peakLbl.setStyle("-fx-text-fill:" + color + "; -fx-font-size:13px; -fx-opacity:0.75;");

        // Contests participated — same color, dimmed
        Label contestsLbl = new Label(String.valueOf(r.getContestsParticipated()));
        contestsLbl.setMinWidth(90);
        contestsLbl.setStyle("-fx-text-fill:" + color + "; -fx-font-size:13px; -fx-opacity:0.80;");

        // Wins — keep gold for trophy; dimmed title-color for "—"
        Label winsLbl = new Label(r.getContestsWon() > 0 ? "🏆 " + r.getContestsWon() : "—");
        winsLbl.setMinWidth(70);
        winsLbl.setStyle(r.getContestsWon() > 0
                ? "-fx-text-fill:#fbbf24; -fx-font-size:13px; -fx-font-weight:bold;"
                : "-fx-text-fill:" + color + "; -fx-font-size:13px; -fx-opacity:0.55;");

        row.getChildren().addAll(
                pointerLbl, rankLbl, nameLbl, titleLbl, ratingLbl, peakLbl, contestsLbl, winsLbl);
        return row;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private ContestParticipant findParticipant(int studentId) {
        if (contestParticipants == null) return null;
        return contestParticipants.stream()
                .filter(p -> p.getStudentId() == studentId)
                .findFirst().orElse(null);
    }

    /**
     * Single source of truth for rank-title → hex color mapping.
     * Matches the palette defined in contest.css and rankColorForRating()
     * in ContestLobbyController.
     */
    private String rankColor(String cssClass) {
        return switch (cssClass) {
            case "rank-legend"   -> "#fbbf24"; // 👑 Legend   — gold
            case "rank-champion" -> "#a78bfa"; // 🚀 Champion — purple
            case "rank-expert"   -> "#60a5fa"; // 🧠 Expert   — blue
            case "rank-advanced" -> "#f97316"; // 🔥 Advanced — orange
            case "rank-skilled"  -> "#34d399"; // ⚡ Skilled  — green
            case "rank-learner"  -> "#94a3b8"; // 🧑‍🎓 Learner  — slate
            default              -> "#6b7280"; // 🌱 Beginner — grey
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
        SessionManager.getInstance().removeAttribute("leaderboard_mode");

        User u = SessionManager.getInstance().getCurrentUser();
        if (u != null && u.isAdmin()) {
            SceneManager.switchScene("/com/examverse/fxml/contest/contest-manager.fxml");
        } else {
            SceneManager.switchScene("/com/examverse/fxml/contest/contest-lobby.fxml");
        }
    }
}