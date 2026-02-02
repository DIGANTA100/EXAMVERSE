package com.examverse.util;

import com.examverse.model.user.User;

/**
 * SessionManager - Manages the current logged-in user session
 * Singleton pattern to maintain user state across the application
 */
public class SessionManager {

    private static SessionManager instance;
    private User currentUser;

    private SessionManager() {
        // Private constructor for singleton
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
    }

    /**
     * Clear session (for app shutdown)
     */
    public void clearSession() {
        currentUser = null;
        instance = null;
    }
}