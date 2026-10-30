package com.campus.examscheduler.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Salted PBKDF2 password hashing. Plaintext passwords are never stored. */
public final class PasswordUtil {
    public static final int ITERATIONS = 120_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() { }

    public static String newSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String hash(String password, String saltBase64, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), Base64.getDecoder().decode(saltBase64), iterations, 256);
            byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Password hashing is not available on this Java runtime.", e);
        }
    }

    public static boolean verify(String password, String saltBase64, int iterations, String expectedHash) {
        byte[] a = hash(password, saltBase64, iterations).getBytes();
        return MessageDigest.isEqual(a, expectedHash.getBytes());     // constant-time comparison
    }

    /** Returns a problem description, or null when the password is acceptable. */
    public static String checkStrength(String password) {
        if (password == null || password.length() < 8) return "The password must be at least 8 characters long.";
        boolean letter = false, digit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) letter = true;
            if (Character.isDigit(c)) digit = true;
        }
        if (!letter || !digit) return "The password must contain at least one letter and one digit.";
        return null;
    }
}
