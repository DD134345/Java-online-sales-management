package vgu.sales.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vgu.sales.controller.model.Role;
import vgu.sales.controller.model.Staff;
import vgu.sales.database.DataAccessException;
import vgu.sales.database.StaffRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for AuthServiceImpl logic.
 * Package-private: only JUnit runs it.
 */
class AuthServiceImplTest {

    private FakeStaffDao staffDao;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        staffDao = new FakeStaffDao();
        staffDao.addSeed("admin", "a1b2c3d4e5f60718293a4b5c6d7e8f90", "Admin@123", "ADMIN", true);
        staffDao.addSeed("old", "b2c3d4e5f60718293a4b5c6d7e8f90a1", "Passw0rd!", "SALES_STAFF", false);
        authService = new AuthServiceImpl(staffDao);
    }

    @Test
    void loginSuccessReturnsAdminAndCurrentStaffIsPresentCaseInsensitive() {
        Staff staff = authService.login("ADMIN", "Admin@123".toCharArray());

        assertNotNull(staff);
        assertEquals("admin", staff.getUsername());
        assertEquals(Role.ADMIN, staff.getRole());
        assertTrue(staff.isActive());
        assertTrue(authService.currentStaff().isPresent());
        assertEquals(staff.getId(), authService.currentStaff().get().getId());
    }

    @Test
    void loginFailsWithWrongPassword() {
        AuthenticationException ex = assertThrows(AuthenticationException.class, () ->
            authService.login("admin", "WrongPass123".toCharArray())
        );
        assertEquals("Wrong username or password.", ex.getMessage());
    }

    @Test
    void loginFailsWithUnknownUser() {
        AuthenticationException ex = assertThrows(AuthenticationException.class, () ->
            authService.login("unknown", "Admin@123".toCharArray())
        );
        assertEquals("Wrong username or password.", ex.getMessage());
    }

    @Test
    void loginFailsWithBlankUsernameOrEmptyPassword() {
        AuthenticationException ex1 = assertThrows(AuthenticationException.class, () ->
            authService.login("   ", "Admin@123".toCharArray())
        );
        assertEquals("Enter your username and password.", ex1.getMessage());

        AuthenticationException ex2 = assertThrows(AuthenticationException.class, () ->
            authService.login(null, "Admin@123".toCharArray())
        );
        assertEquals("Enter your username and password.", ex2.getMessage());

        AuthenticationException ex3 = assertThrows(AuthenticationException.class, () ->
            authService.login("admin", new char[0])
        );
        assertEquals("Enter your username and password.", ex3.getMessage());

        AuthenticationException ex4 = assertThrows(AuthenticationException.class, () ->
            authService.login("admin", null)
        );
        assertEquals("Enter your username and password.", ex4.getMessage());
    }

    @Test
    void loginInactiveAccountWithRightPasswordThrowsDeactivatedMessage() {
        AuthenticationException ex = assertThrows(AuthenticationException.class, () ->
            authService.login("old", "Passw0rd!".toCharArray())
        );
        assertEquals("Account has been deactivated. Please contact the administrator.", ex.getMessage());
    }

    @Test
    void loginInactiveAccountWithWrongPasswordThrowsWrongCredentialsMessage() {
        AuthenticationException ex = assertThrows(AuthenticationException.class, () ->
            authService.login("old", "WrongPassword".toCharArray())
        );
        assertEquals("Wrong username or password.", ex.getMessage());
    }

    @Test
    void logoutClearsCurrentStaff() {
        authService.login("admin", "Admin@123".toCharArray());
        assertTrue(authService.currentStaff().isPresent());

        authService.logout();
        assertFalse(authService.currentStaff().isPresent());
    }

    @Test
    void signUpCreatesActiveSalesStaffAndAllowsLoginWithoutAutoSignIn() {
        Staff created = authService.signUp(
            "newuser",
            "NewUser123".toCharArray(),
            "New User",
            "newuser@example.com",
            "0912345678"
        );

        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals("newuser", created.getUsername());
        assertEquals(Role.SALES_STAFF, created.getRole());
        assertTrue(created.isActive());

        assertFalse(authService.currentStaff().isPresent());

        StaffRecord stored = staffDao.findByUsername("newuser").orElseThrow();
        assertNotNull(stored.passwordSalt());
        assertEquals(32, stored.passwordSalt().length());
        assertTrue(stored.passwordSalt().matches("^[0-9a-f]{32}$"));
        assertFalse(stored.passwordHash().contains("NewUser123"));
        assertFalse(stored.passwordSalt().contains("NewUser123"));

        Staff loggedIn = authService.login("newuser", "NewUser123".toCharArray());
        assertNotNull(loggedIn);
        assertEquals(created.getId(), loggedIn.getId());
        assertTrue(authService.currentStaff().isPresent());
        assertEquals(created.getId(), authService.currentStaff().get().getId());
    }

    @Test
    void signUpDuplicateUsernameFailsDifferentCase() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            authService.signUp("ADMIN", "ValidPass1".toCharArray(), "Admin Duplicate", "dup@example.com", "0912345678")
        );
        assertEquals("Username is already taken.", ex.getMessage());
    }

    @Test
    void signUpWeakPasswordsFailWithExpectedMessage() {
        String expectedMessage = "Password must be at least 8 characters with a letter and a digit.";

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () ->
            authService.signUp("user1", "short1".toCharArray(), "User One", "user1@example.com", "0912345678")
        );
        assertEquals(expectedMessage, ex1.getMessage());

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () ->
            authService.signUp("user2", "longpassword".toCharArray(), "User Two", "user2@example.com", "0912345678")
        );
        assertEquals(expectedMessage, ex2.getMessage());

        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class, () ->
            authService.signUp("user3", "12345678".toCharArray(), "User Three", "user3@example.com", "0912345678")
        );
        assertEquals(expectedMessage, ex3.getMessage());
    }

    @Test
    void signUpInvalidPhoneFailsWithExpectedMessage() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            authService.signUp("user4", "ValidPass1".toCharArray(), "User Four", "user4@example.com", "1234567890")
        );
        assertEquals("Phone must be 10 digits starting with 0.", ex.getMessage());
    }

    @Test
    void signUpRaceOnInsertReportsUsernameTaken() {
        FakeStaffDao raceDao = new FakeStaffDao() {
            @Override
            public StaffRecord insert(StaffRecord record) {
                super.insert(record);
                throw new DataAccessException("duplicate key");
            }
        };
        AuthServiceImpl raceAuthService = new AuthServiceImpl(raceDao);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            raceAuthService.signUp(
                "racer",
                "ValidPass1".toCharArray(),
                "Race User",
                "race@example.com",
                "0912345678"
            )
        );

        assertEquals("Username is already taken.", ex.getMessage());
    }
}
