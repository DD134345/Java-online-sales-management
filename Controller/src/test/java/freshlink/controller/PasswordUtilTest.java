package freshlink.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks that PasswordUtil matches the demo accounts in Database/seed.sql (no database needed). */
class PasswordUtilTest {

    @Test
    void demoAdminPasswordMatchesSeedData() {
        String hash = "e6a2db3bc2db60eea0ccb1c6917e84cc28ec9c899c6b81997f0f45ffd8c083c1";
        String salt = "a1f3c9e27b4d8e60f2a5b7c913d4e6f8";
        assertTrue(PasswordUtil.check("Admin@123", hash, salt));
        assertFalse(PasswordUtil.check("admin@123", hash, salt));
    }

    @Test
    void newPasswordCanBeChecked() {
        String salt = PasswordUtil.newSalt();
        assertEquals(32, salt.length());
        String hash = PasswordUtil.hash("MyPass@1", salt);
        assertEquals(64, hash.length());
        assertTrue(PasswordUtil.check("MyPass@1", hash, salt));
    }
}
