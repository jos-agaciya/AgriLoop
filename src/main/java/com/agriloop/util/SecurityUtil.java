package com.agriloop.util;

import org.mindrot.jbcrypt.BCrypt;

import java.util.UUID;

/**
 * Security utility for cryptographic hashing and token generation.
 */
public class SecurityUtil {

    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }

    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (Exception e) {
            return false;
        }
    }

    public static String generateUuid() {
        return UUID.randomUUID().toString();
    }
}
