package vgu.sales.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Utility class providing secure password salting and SHA-256 hashing.
 *
 * Public: accessed by controller authentication services and tests.
 */
public final class PasswordHasher {

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Private constructor to prevent instantiation of utility class.
     * Private: utility class only contains static methods.
     */
    private PasswordHasher() {
    }

    /**
     * Generates 16 cryptographically secure random bytes as a 32-character lowercase hex string.
     *
     * @return 32-character lowercase hex salt
     * Public: called by auth services during user registration and password reset.
     */
    public static String newSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return HexFormat.of().formatHex(salt);
    }

    /**
     * Computes SHA-256 hash over UTF-8 bytes of salt concatenated with password.
     *
     * @param saltHex 32-character hex salt
     * @param password plain text password character array
     * @return 64-character lowercase hex hash
     * @throws IllegalArgumentException if saltHex or password is null
     * @throws IllegalStateException if SHA-256 algorithm is unavailable
     * Public: called during registration and credential verification.
     */
    public static String hash(String saltHex, char[] password) {
        if (saltHex == null || password == null) {
            throw new IllegalArgumentException("Salt and password must not be null.");
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String combined = saltHex + new String(password);
            byte[] digest = md.digest(combined.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Verifies a plain text password against expected hash using constant-time byte comparison.
     *
     * @param password plain text password character array
     * @param saltHex hex salt stored with user record
     * @param expectedHash stored password hash to compare against
     * @return true if hash matches expected hash, false otherwise or if expectedHash is null
     * Public: called during login to authenticate credentials.
     */
    public static boolean matches(char[] password, String saltHex, String expectedHash) {
        if (expectedHash == null) {
            return false;
        }
        String computed = hash(saltHex, password);
        return MessageDigest.isEqual(
            computed.getBytes(StandardCharsets.UTF_8),
            expectedHash.getBytes(StandardCharsets.UTF_8)
        );
    }
}
