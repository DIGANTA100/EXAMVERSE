package com.examverse.util;

import com.examverse.model.user.User;
import java.util.HashMap;
import java.util.Map;

/**
 * SessionManager - Manages the current logged-in user session
 * Singleton pattern to maintain user state across the application
 * MERGED: Contains both user session and attribute storage
 */
public class SessionManager {

    private static SessionManager instance;
    private User currentUser;
    private Map<String, Object> attributes; // For storing temporary session data

    private SessionManager() {
        // Private constructor for singleton
        attributes = new HashMap<>();
    }

    /**
     * Get SessionManager instance
     */
    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    // ==================== USER SESSION METHODS ====================

    /**
     * Set current logged-in user
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        System.out.println("✅ Session started for: " + user.getUsername());
    }

    /**
     * Get current logged-in user
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Check if user is logged in
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Check if current user is admin
     */
    public boolean isAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }

    /**
     * Check if current user is student
     */
    public boolean isStudent() {
        return currentUser != null && currentUser.isStudent();
    }

    /**
     * Logout current user
     */
    public void logout() {
        if (currentUser != null) {
            System.out.println("✅ User logged out: " + currentUser.getUsername());
            currentUser = null;
        }
        attributes.clear(); // Clear session attributes on logout
    }

    /**
     * Clear session (for app shutdown)
     */
    public void clearSession() {
        if (currentUser != null) {
            System.out.println("🧹 Clearing session for: " + currentUser.getUsername());
        }
        currentUser = null;
        attributes.clear();
        instance = null;
    }

    // ==================== SESSION ATTRIBUTES METHODS ====================

    /**
     * Store an attribute in session
     * Used for passing data between controllers (e.g., attemptId, examId)
     * @param key Attribute name
     * @param value Attribute value (can be any Object)
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    /**
     * Get an attribute from session
     * @param key Attribute name
     * @return Attribute value or null if not found
     */
    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    /**
     * Remove an attribute from session
     * @param key Attribute name
     */
    public void removeAttribute(String key) {
        attributes.remove(key);
    }

    /**
     * Check if attribute exists
     * @param key Attribute name
     * @return true if exists
     */
    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }

    /**
     * Clear all attributes (keeps user logged in)
     */
    public void clearAttributes() {
        attributes.clear();
    }
}