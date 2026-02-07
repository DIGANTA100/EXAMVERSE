package com.examverse.service.auth;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * PasswordResetService - Manages password reset codes
 * Handles generation, validation, and expiration of reset codes
 */
public class PasswordResetService {

    private static PasswordResetService instance;

    // Store reset codes in memory: email -> ResetCode
    private Map<String, ResetCode> resetCodes = new HashMap<>();

    // Code expiration time in minutes
    private static final int CODE_EXPIRY_MINUTES = 10;

    /**
     * Inner class to store reset code data
     */
    private static class ResetCode {
        String code;
        LocalDateTime expiryTime;
        int attempts;

        ResetCode(String code, LocalDateTime expiryTime) {
            this.code = code;
            this.expiryTime = expiryTime;
            this.attempts = 0;
        }

        boolean isExpired() {
            return LocalDateTime.now().isAfter(expiryTime);
        }

        boolean isValid(String inputCode) {
            return this.code.equals(inputCode) && !isExpired();
        }
    }

    /**
     * Private constructor for Singleton pattern
     */
    private PasswordResetService() {
    }

    /**
     * Get PasswordResetService instance (Singleton)
     */
    public static PasswordResetService getInstance() {
        if (instance == null) {
            instance = new PasswordResetService();
        }
        return instance;
    }

    /**
     * Generate a 6-digit reset code
     * @param email User's email address
     * @return Generated 6-digit code
     */
    public String generateResetCode(String email) {
        // Generate random 6-digit code
        Random random = new Random();
        String code = String.format("%06d", random.nextInt(1000000));

        // Set expiry time
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(CODE_EXPIRY_MINUTES);

        // Store the code
        resetCodes.put(email.toLowerCase().trim(), new ResetCode(code, expiryTime));

        System.out.println("🔐 Generated reset code for " + email + ": " + code);
        System.out.println("⏰ Code expires at: " + expiryTime);

        return code;
    }

    /**
     * Validate reset code
     * @param email User's email
     * @param inputCode Code entered by user
     * @return true if valid, false otherwise
     */
    public boolean validateResetCode(String email, String inputCode) {
        email = email.toLowerCase().trim();

        if (!resetCodes.containsKey(email)) {
            System.err.println("❌ No reset code found for: " + email);
            return false;
        }

        ResetCode resetCode = resetCodes.get(email);

        // Check expiration
        if (resetCode.isExpired()) {
            System.err.println("❌ Reset code expired for: " + email);
            resetCodes.remove(email); // Clean up expired code
            return false;
        }

        // Increment attempts
        resetCode.attempts++;

        // Check if too many attempts (max 5)
        if (resetCode.attempts > 5) {
            System.err.println("❌ Too many attempts for: " + email);
            resetCodes.remove(email);
            return false;
        }

        // Validate code
        boolean isValid = resetCode.isValid(inputCode);

        if (isValid) {
            System.out.println("✅ Reset code validated for: " + email);
        } else {
            System.err.println("❌ Invalid reset code for: " + email);
            System.err.println("   Expected: " + resetCode.code);
            System.err.println("   Got: " + inputCode);
        }

        return isValid;
    }

    /**
     * Remove reset code after successful password reset
     * @param email User's email
     */
    public void removeResetCode(String email) {
        email = email.toLowerCase().trim();
        resetCodes.remove(email);
        System.out.println("🗑️ Reset code removed for: " + email);
    }

    /**
     * Check if a valid reset code exists for email
     * @param email User's email
     * @return true if exists and not expired
     */
    public boolean hasValidCode(String email) {
        email = email.toLowerCase().trim();

        if (!resetCodes.containsKey(email)) {
            return false;
        }

        ResetCode resetCode = resetCodes.get(email);

        if (resetCode.isExpired()) {
            resetCodes.remove(email);
            return false;
        }

        return true;
    }

    /**
     * Get remaining time for code (for display purposes)
     * @param email User's email
     * @return Remaining minutes, or 0 if expired/not found
     */
    public long getRemainingMinutes(String email) {
        email = email.toLowerCase().trim();

        if (!resetCodes.containsKey(email)) {
            return 0;
        }

        ResetCode resetCode = resetCodes.get(email);

        if (resetCode.isExpired()) {
            return 0;
        }

        long remainingSeconds = java.time.Duration.between(
                LocalDateTime.now(),
                resetCode.expiryTime
        ).getSeconds();

        return remainingSeconds / 60;
    }

    /**
     * Clean up expired codes (should be called periodically)
     */
    public void cleanupExpiredCodes() {
        resetCodes.entrySet().removeIf(entry -> entry.getValue().isExpired());
        System.out.println("🧹 Cleaned up expired reset codes");
    }
}