package com.examverse.service.auth;

import com.examverse.config.DatabaseConfig;
import com.examverse.model.user.User;

import java.sql.*;
import java.time.LocalDateTime;

/**
 * UserDAO - Data Access Object for User operations
 * Handles all database operations related to users
 * UPDATED: Added debugging and password trimming
 */
public class UserDAO {

    /**
     * Register a new user
     * @return true if registration successful, false otherwise
     */
    public boolean registerUser(String username, String email, String password, String fullName) {
        String sql = "INSERT INTO users (username, email, password, full_name, user_type) VALUES (?, ?, ?, ?, 'STUDENT')";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username.trim());
            pstmt.setString(2, email.trim());
            pstmt.setString(3, password); // Store password as-is (trim handled in controller)
            pstmt.setString(4, fullName.trim());

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ User registered successfully: " + username);
                System.out.println("📝 Stored password length: " + password.length() + " chars");
                return true;
            }

        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) { // Duplicate entry
                System.err.println("❌ Username or email already exists!");
            } else {
                System.err.println("❌ Registration failed!");
                e.printStackTrace();
            }
        }

        return false;
    }

    /**
     * Authenticate user login
     * @return User object if login successful, null otherwise
     */
    public User loginUser(String usernameOrEmail, String password) {
        // Debugging output
        System.out.println("🔍 Login attempt:");
        System.out.println("   Username/Email: '" + usernameOrEmail + "'");
        System.out.println("   Password length: " + password.length() + " chars");

        String sql = "SELECT * FROM users WHERE (username = ? OR email = ?) AND is_active = TRUE";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, usernameOrEmail.trim());
            pstmt.setString(2, usernameOrEmail.trim());

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("password");
                System.out.println("✅ User found in database!");
                System.out.println("   Stored password length: " + storedPassword.length() + " chars");
                System.out.println("   Entered password: '" + password + "'");
                System.out.println("   Stored password: '" + storedPassword + "'");

                // Compare passwords
                if (storedPassword.equals(password)) {
                    System.out.println("✅ Password match!");

                    // Extract all data from ResultSet BEFORE closing it
                    int userId = rs.getInt("id");
                    String username = rs.getString("username");
                    String email = rs.getString("email");
                    String fullName = rs.getString("full_name");
                    String userType = rs.getString("user_type");

                    // Now safe to update last login (this closes the connection)
                    updateLastLogin(userId);

                    // Create User object with extracted data
                    User user = new User();
                    user.setId(userId);
                    user.setUsername(username);
                    user.setEmail(email);
                    user.setFullName(fullName);
                    user.setUserType(userType);

                    System.out.println("✅ Login successful: " + user.getUsername());
                    return user;
                } else {
                    System.err.println("❌ Password mismatch!");
                    System.err.println("   Expected: '" + storedPassword + "'");
                    System.err.println("   Got: '" + password + "'");

                    // Character by character comparison for debugging
                    int minLen = Math.min(storedPassword.length(), password.length());
                    for (int i = 0; i < minLen; i++) {
                        if (storedPassword.charAt(i) != password.charAt(i)) {
                            System.err.println("   First difference at position " + i);
                            System.err.println("   Expected char: '" + storedPassword.charAt(i) + "' (ASCII: " + (int)storedPassword.charAt(i) + ")");
                            System.err.println("   Got char: '" + password.charAt(i) + "' (ASCII: " + (int)password.charAt(i) + ")");
                            break;
                        }
                    }
                }
            } else {
                System.err.println("❌ User not found in database!");
                System.err.println("   Searched for: '" + usernameOrEmail + "'");
            }

        } catch (SQLException e) {
            System.err.println("❌ Login failed - Database error!");
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Check if username exists
     */
    public boolean usernameExists(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username.trim());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Check if email exists
     */
    public boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email.trim());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Update last login timestamp
     */
    private void updateLastLogin(int userId) {
        String sql = "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get user by ID
     */
    public User getUserById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setFullName(rs.getString("full_name"));
                user.setUserType(rs.getString("user_type"));
                return user;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * DEBUG METHOD: List all users and their passwords
     * REMOVE THIS IN PRODUCTION!
     */
    public void debugListAllUsers() {
        String sql = "SELECT id, username, email, password FROM users";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            System.out.println("\n🔍 DEBUG: All users in database:");
            System.out.println("═══════════════════════════════════════════════");

            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("id"));
                System.out.println("Username: " + rs.getString("username"));
                System.out.println("Email: " + rs.getString("email"));
                System.out.println("Password: '" + rs.getString("password") + "' (length: " + rs.getString("password").length() + ")");
                System.out.println("───────────────────────────────────────────────");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}