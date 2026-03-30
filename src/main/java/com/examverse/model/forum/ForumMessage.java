package com.examverse.model.forum;

import java.time.LocalDateTime;

/**
 * ForumMessage — represents a single chat message in the discussion forum.
 *
 * channel values:
 *   "GENERAL"  — student ↔ all (visible to everyone)
 *   "ADMIN"    — admin ↔ admin only
 */
public class ForumMessage {

    private int           messageId;
    private int           senderId;
    private String        senderName;
    private String        senderUsername;
    private String        senderRole;      // "STUDENT" | "ADMIN"
    private int           senderRating;    // used for avatar colour
    private String        channel;         // "GENERAL" | "ADMIN"
    private String        content;
    private LocalDateTime sentAt;

    // ── Constructors ─────────────────────────────────────────────────────────

    public ForumMessage() {}

    public ForumMessage(int senderId, String senderName, String senderUsername,
                        String senderRole, int senderRating,
                        String channel, String content, LocalDateTime sentAt) {
        this.senderId       = senderId;
        this.senderName     = senderName;
        this.senderUsername = senderUsername;
        this.senderRole     = senderRole;
        this.senderRating   = senderRating;
        this.channel        = channel;
        this.content        = content;
        this.sentAt         = sentAt;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public int           getMessageId()       { return messageId; }
    public void          setMessageId(int v)  { messageId = v; }

    public int           getSenderId()        { return senderId; }
    public void          setSenderId(int v)   { senderId = v; }

    public String        getSenderName()      { return senderName; }
    public void          setSenderName(String v) { senderName = v; }

    public String        getSenderUsername()  { return senderUsername; }
    public void          setSenderUsername(String v) { senderUsername = v; }

    public String        getSenderRole()      { return senderRole; }
    public void          setSenderRole(String v) { senderRole = v; }

    public int           getSenderRating()    { return senderRating; }
    public void          setSenderRating(int v) { senderRating = v; }

    public String        getChannel()         { return channel; }
    public void          setChannel(String v) { channel = v; }

    public String        getContent()         { return content; }
    public void          setContent(String v) { content = v; }

    public LocalDateTime getSentAt()          { return sentAt; }
    public void          setSentAt(LocalDateTime v) { sentAt = v; }

    public boolean isAdmin()   { return "ADMIN".equalsIgnoreCase(senderRole); }
    public boolean isStudent() { return "STUDENT".equalsIgnoreCase(senderRole); }
}