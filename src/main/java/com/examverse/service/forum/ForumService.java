package com.examverse.service.forum;

import com.examverse.config.DatabaseConfig;
import com.examverse.model.forum.ForumMessage;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ForumService — all database operations for the discussion forum.
 *
 * Table: forum_messages
 *   message_id   INT PK AUTO_INCREMENT
 *   sender_id    INT (FK → users.id)
 *   sender_name  VARCHAR(100)
 *   sender_username VARCHAR(60)
 *   sender_role  ENUM('STUDENT','ADMIN')
 *   sender_rating INT DEFAULT 0
 *   channel      ENUM('GENERAL','ADMIN')
 *   content      TEXT
 *   sent_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
 *
 * Polling strategy: callers pass the last known message_id;
 * fetchNewMessages() returns only messages with id > lastId.
 * This is lightweight and works without WebSockets.
 */
public class ForumService {

    // ── Table bootstrap ───────────────────────────────────────────────────────

    public static void ensureTableExists() {
        String sql = """
            CREATE TABLE IF NOT EXISTS forum_messages (
                message_id    INT PRIMARY KEY AUTO_INCREMENT,
                sender_id     INT NOT NULL,
                sender_name   VARCHAR(100) NOT NULL,
                sender_username VARCHAR(60) NOT NULL,
                sender_role   ENUM('STUDENT','ADMIN') NOT NULL DEFAULT 'STUDENT',
                sender_rating INT NOT NULL DEFAULT 0,
                channel       ENUM('GENERAL','ADMIN') NOT NULL DEFAULT 'GENERAL',
                content       TEXT NOT NULL,
                sent_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_channel (channel),
                INDEX idx_sent_at (sent_at),
                FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             Statement  stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            System.out.println("✅ forum_messages table ready.");
        } catch (Exception e) {
            System.err.println("❌ Could not create forum_messages table: " + e.getMessage());
        }
    }

    // ── Send a message ────────────────────────────────────────────────────────

    /**
     * Persist a message and return the generated message_id, or -1 on failure.
     */
    public int sendMessage(ForumMessage msg) {
        String sql = """
            INSERT INTO forum_messages
                (sender_id, sender_name, sender_username, sender_role, sender_rating, channel, content)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1,    msg.getSenderId());
            ps.setString(2, msg.getSenderName());
            ps.setString(3, msg.getSenderUsername());
            ps.setString(4, msg.getSenderRole());
            ps.setInt(5,    msg.getSenderRating());
            ps.setString(6, msg.getChannel());
            ps.setString(7, msg.getContent());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        } catch (Exception e) {
            System.err.println("❌ ForumService.sendMessage: " + e.getMessage());
        }
        return -1;
    }

    // ── Fetch recent history (initial load) ───────────────────────────────────

    /**
     * Returns the last {@code limit} messages for the given channel,
     * ordered oldest-first for display.
     */
    public List<ForumMessage> fetchHistory(String channel, int limit) {
        String sql = """
            SELECT * FROM (
                SELECT * FROM forum_messages
                WHERE channel = ?
                ORDER BY sent_at DESC
                LIMIT ?
            ) sub ORDER BY sent_at ASC
            """;
        List<ForumMessage> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, channel);
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (Exception e) {
            System.err.println("❌ ForumService.fetchHistory: " + e.getMessage());
        }
        return list;
    }

    // ── Poll for new messages ─────────────────────────────────────────────────

    /**
     * Returns messages with message_id > {@code lastId} in the given channel.
     * Called every ~2 s by the controller's polling Timeline.
     */
    public List<ForumMessage> fetchNewMessages(String channel, int lastId) {
        String sql = """
            SELECT * FROM forum_messages
            WHERE channel = ? AND message_id > ?
            ORDER BY sent_at ASC
            """;
        List<ForumMessage> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, channel);
            ps.setInt(2, lastId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (Exception e) {
            System.err.println("❌ ForumService.fetchNewMessages: " + e.getMessage());
        }
        return list;
    }

    // ── Active users in channel (last 5 min) ─────────────────────────────────

    /**
     * Returns distinct senders who posted in the given channel
     * within the last 5 minutes — used for the "online" user list.
     */
    public List<ForumMessage> fetchRecentSenders(String channel) {
        String sql = """
            SELECT sender_id, sender_name, sender_username, sender_role, sender_rating,
                   MAX(message_id) AS message_id, '' AS content,
                   MAX(sent_at) AS sent_at, channel
            FROM forum_messages
            WHERE channel = ?
              AND sent_at >= NOW() - INTERVAL 5 MINUTE
            GROUP BY sender_id, sender_name, sender_username, sender_role, sender_rating, channel
            ORDER BY MAX(sent_at) DESC
            """;
        List<ForumMessage> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, channel);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (Exception e) {
            System.err.println("❌ ForumService.fetchRecentSenders: " + e.getMessage());
        }
        return list;
    }

    // ── Delete a message (admin only) ─────────────────────────────────────────

    public boolean deleteMessage(int messageId) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM forum_messages WHERE message_id = ?")) {
            ps.setInt(1, messageId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("❌ ForumService.deleteMessage: " + e.getMessage());
            return false;
        }
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private ForumMessage map(ResultSet rs) throws SQLException {
        ForumMessage m = new ForumMessage();
        m.setMessageId(rs.getInt("message_id"));
        m.setSenderId(rs.getInt("sender_id"));
        m.setSenderName(rs.getString("sender_name"));
        m.setSenderUsername(rs.getString("sender_username"));
        m.setSenderRole(rs.getString("sender_role"));
        m.setSenderRating(rs.getInt("sender_rating"));
        m.setChannel(rs.getString("channel"));
        m.setContent(rs.getString("content"));
        Timestamp ts = rs.getTimestamp("sent_at");
        m.setSentAt(ts != null ? ts.toLocalDateTime() : LocalDateTime.now());
        return m;
    }
}