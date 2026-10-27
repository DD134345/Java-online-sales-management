package vgu.sales.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for PasswordHasher.
 * Package-private: only JUnit runs it.
 */
class PasswordHasherTest {

    @Test
    void hashMatchesContractTestVector() {
        String salt = "a1b2c3d4e5f60718293a4b5c6d7e8f90";
        char[] password = "Admin@123".toCharArray();
        String expectedHash = "8c8746be97b5fda756b26fb956ac8f714ffe6b9781563d9c65320acc221a8e7a";

        assertEquals(expectedHash, PasswordHasher.hash(salt, password));
    }

    @Test
    void newSaltProducesUnique32CharLowercaseHex() {
        String salt1 = PasswordHasher.newSalt();
        String salt2 = PasswordHasher.newSalt();

        assertNotNull(salt1);
        assertNotNull(salt2);
        assertEquals(32, salt1.length());
        assertEquals(32, salt2.length());
        assertTrue(salt1.matches("^[0-9a-f]{32}$"));
        assertTrue(salt2.matches("^[0-9a-f]{32}$"));
        assertNotEquals(salt1, salt2);
    }

    @Test
    void matchesVerifiesPasswordCorrectly() {
        String salt = "a1b2c3d4e5f60718293a4b5c6d7e8f90";
        char[] rightPassword = "Admin@123".toCharArray();
        char[] wrongPassword = "WrongPassword".toCharArray();
        String expectedHash = "8c8746be97b5fda756b26fb956ac8f714ffe6b9781563d9c65320acc221a8e7a";

        assertTrue(PasswordHasher.matches(rightPassword, salt, expectedHash));
        assertFalse(PasswordHasher.matches(wrongPassword, salt, expectedHash));
        assertFalse(PasswordHasher.matches(rightPassword, salt, null));
    }
}
