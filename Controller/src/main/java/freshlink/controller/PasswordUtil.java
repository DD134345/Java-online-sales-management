package freshlink.controller;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Hashes and checks passwords. The sample data in Database/seed.sql was made with
 * exactly these settings, so the demo logins (admin / Admin@123 ...) work with this class.
 * Do not change the settings, or nobody can log in any more.
 */
public class PasswordUtil {

    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;

    /** Makes a new random salt (32 hex characters). Use one new salt per user. */
    public static String newSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return HexFormat.of().formatHex(salt);
    }

    /** Returns the hash (64 hex characters) to store in users.password_hash. */
    public static String hash(String password, String salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), HexFormat.of().parseHex(salt),
                    ITERATIONS, KEY_LENGTH);
            byte[] key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return HexFormat.of().formatHex(key);
        } catch (Exception e) {
            throw new RuntimeException("Cannot hash password", e);
        }
    }

    /** True if the typed password matches the stored hash and salt. */
    public static boolean check(String password, String storedHash, String salt) {
        return hash(password, salt).equals(storedHash);
    }
}
