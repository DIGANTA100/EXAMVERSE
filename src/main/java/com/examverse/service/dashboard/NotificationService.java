package com.examverse.service.dashboard;

import com.examverse.config.DatabaseConfig;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * NotificationService — DB-backed admin → student notification system.
 *
 * Table: notifications
 *   id            INT PK AUTO_INCREMENT
 *   title         VARCHAR(200)
 *   message       TEXT
 *   type          ENUM('INFO','SUCCESS','WARNING','CONTEST','EXAM')
 *   target_user   INT NULL  (NULL = broadcast to all students)
 *   created_at    TIMESTAMP
 *   is_read       BOOLEAN (per-student tracking via notification_reads)
 *
 * Call NotificationService.ensureTablesExist() once at app startup
 * (alongside DatabaseConfig.initializeTables()).
 */
public class NotificationService {

    // ── DDL ───────────────────────────────────────────────────────────────────

    public static void ensureTablesExist() {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS notifications (
                    id           INT PRIMARY KEY AUTO_INCREMENT,
                    title        VARCHAR(200)  NOT NULL,
                    message      TEXT          NOT NULL,
                    type         ENUM('INFO','SUCCESS','WARNING','CONTEST','EXAM') DEFAULT 'INFO',
                    target_user  INT NULL,
                    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (target_user) REFERENCES users(id) ON DELETE CASCADE,
                    INDEX idx_target (target_user),
                    INDEX idx_created (created_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS notification_reads (
                    notification_id INT NOT NULL,
                    user_id         INT NOT NULL,
                    read_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (notification_id, user_id),
                    FOREIGN KEY (notification_id) REFERENCES notifications(id) ON DELETE CASCADE,
                    FOREIGN KEY (user_id)         REFERENCES users(id)         ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

            System.out.println("✅ Notification tables created/verified!");

        } catch (SQLException e) {
            System.err.println("❌ NotificationService.ensureTablesExist: " + e.getMessage());
        }
    }

    // ── Data model ────────────────────────────────────────────────────────────

    public static class Notification {
        private int    id;
        private String title;
        private String message;
        private String type;          // INFO / SUCCESS / WARNING / CONTEST / EXAM
        private boolean read;
        private LocalDateTime createdAt;

        public int     getId()        { return id; }
        public String  getTitle()     { return title; }
        public String  getMessage()   { return message; }
        public String  getType()      { return type; }
        public boolean isRead()       { return read; }
        public LocalDateTime getCreatedAt() { return createdAt; }

        /** Emoji prefix matching type */
        public String getIcon() {
            return switch (type) {
                case "SUCCESS"  -> "✅";
                case "WARNING"  -> "⚠️";
                case "CONTEST"  -> "⚔️";
                case "EXAM"     -> "📝";
                default         -> "📢";
            };
        }

        /** CSS accent color matching type */
        public String getAccentColor() {
            return switch (type) {
                case "SUCCESS"  -> "#22c55e";
                case "WARNING"  -> "#f59e0b";
                case "CONTEST"  -> "#a78bfa";
                case "EXAM"     -> "#22d3ee";
                default         -> "#94a3b8";
            };
        }
    }

    // ── Read: fetch for student (broadcast + personal, unread first) ──────────

    public List<Notification> getNotificationsForStudent(int studentId) {
        List<Notification> list = new ArrayList<>();
        String sql = """
            SELECT n.id, n.title, n.message, n.type, n.created_at,
                   (nr.user_id IS NOT NULL) AS is_read
            FROM notifications n
            LEFT JOIN notification_reads nr
                   ON nr.notification_id = n.id AND nr.user_id = ?
            WHERE n.target_user IS NULL OR n.target_user = ?
            ORDER BY is_read ASC, n.created_at DESC
            LIMIT 50
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, studentId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Notification n = new Notification();
                n.id        = rs.getInt("id");
                n.title     = rs.getString("title");
                n.message   = rs.getString("message");
                n.type      = rs.getString("type");
                n.read      = rs.getBoolean("is_read");
                n.createdAt = rs.getTimestamp("created_at").toLocalDateTime();
                list.add(n);
            }
        } catch (SQLException e) {
            System.err.println("❌ getNotificationsForStudent: " + e.getMessage());
        }
        return list;
    }

    public int getUnreadCount(int studentId) {
        String sql = """
            SELECT COUNT(*) FROM notifications n
            LEFT JOIN notification_reads nr
                   ON nr.notification_id = n.id AND nr.user_id = ?
            WHERE (n.target_user IS NULL OR n.target_user = ?)
              AND nr.user_id IS NULL
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, studentId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("❌ getUnreadCount: " + e.getMessage());
        }
        return 0;
    }

    // ── Mark read ─────────────────────────────────────────────────────────────

    public void markRead(int notificationId, int userId) {
        String sql = "INSERT IGNORE INTO notification_reads (notification_id, user_id) VALUES (?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, notificationId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ markRead: " + e.getMessage());
        }
    }

    public void markAllRead(int userId) {
        String sql = """
            INSERT IGNORE INTO notification_reads (notification_id, user_id)
            SELECT n.id, ? FROM notifications n
            WHERE n.target_user IS NULL OR n.target_user = ?
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ markAllRead: " + e.getMessage());
        }
    }

    // ── Admin: send notification ──────────────────────────────────────────────

    /**
     * Admin sends a broadcast (targetUserId = null) or personal notification.
     * @param targetUserId null = all students
     */
    public boolean sendNotification(String title, String message,
                                    String type, Integer targetUserId) {
        String sql = "INSERT INTO notifications (title, message, type, target_user) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, message);
            ps.setString(3, type);
            if (targetUserId != null) ps.setInt(4, targetUserId);
            else ps.setNull(4, java.sql.Types.INTEGER);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("❌ sendNotification: " + e.getMessage());
            return false;
        }
    }
}