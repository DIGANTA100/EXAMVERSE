package com.examverse.service.auth;

import com.examverse.config.DatabaseConfig;
import com.examverse.model.user.User;

import java.sql.*;
import java.time.LocalDateTime;

/**
 * UserDAO - Data Access Object for User operations
 * Handles all database operations related to users
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

            pstmt.setString(1, username);
            pstmt.setString(2, email);
            pstmt.setString(3, password); // In production, hash this password!
            pstmt.setString(4, fullName);

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ User registered successfully: " + username);
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
        String sql = "SELECT * FROM users WHERE (username = ? OR email = ?) AND password = ? AND is_active = TRUE";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, usernameOrEmail);
            pstmt.setString(2, usernameOrEmail);
            pstmt.setString(3, password); // In production, compare hashed password!

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // Update last login time
                updateLastLogin(rs.getInt("id"));

                // Create User object
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setFullName(rs.getString("full_name"));
                user.setUserType(rs.getString("user_type"));

                System.out.println("✅ Login successful: " + user.getUsername());
                return user;
            } else {
                System.err.println("❌ Invalid credentials!");
            }

        } catch (SQLException e) {
            System.err.println("❌ Login failed!");
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

            pstmt.setString(1, username);
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

            pstmt.setString(1, email);
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
}