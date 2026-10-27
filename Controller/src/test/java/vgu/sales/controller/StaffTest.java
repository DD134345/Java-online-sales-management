package vgu.sales.controller;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import vgu.sales.controller.model.Role;
import vgu.sales.controller.model.Staff;
import vgu.sales.database.StaffRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for Staff domain entity and Person parent validation.
 * Package-private: only JUnit runs it.
 */
class StaffTest {

    @Test
    void constructorLowerCasesAndTrimsUsername() {
        Staff staff = new Staff(
            1,
            "  Alice.Admin  ",
            "Alice Admin",
            "alice@example.com",
            "0901234567",
            Role.ADMIN,
            true,
            LocalDateTime.now()
        );
        assertEquals("alice.admin", staff.getUsername());
    }

    @Test
    void validationRejectsInvalidUsername() {
        LocalDateTime now = LocalDateTime.now();

        IllegalArgumentException exNull = assertThrows(IllegalArgumentException.class, () ->
            new Staff(1, null, "Alice Admin", "alice@example.com", "0901234567", Role.ADMIN, true, now)
        );
        assertEquals("Username must be 3 to 50 letters, digits, dots or underscores.", exNull.getMessage());

        IllegalArgumentException exShort = assertThrows(IllegalArgumentException.class, () ->
            new Staff(1, "ab", "Alice Admin", "alice@example.com", "0901234567", Role.ADMIN, true, now)
        );
        assertEquals("Username must be 3 to 50 letters, digits, dots or underscores.", exShort.getMessage());

        IllegalArgumentException exInvalid = assertThrows(IllegalArgumentException.class, () ->
            new Staff(1, "user@invalid!", "Alice Admin", "alice@example.com", "0901234567", Role.ADMIN, true, now)
        );
        assertEquals("Username must be 3 to 50 letters, digits, dots or underscores.", exInvalid.getMessage());
    }

    @Test
    void validationRejectsInvalidFullName() {
        LocalDateTime now = LocalDateTime.now();

        IllegalArgumentException exNull = assertThrows(IllegalArgumentException.class, () ->
            new Staff(1, "alice", null, "alice@example.com", "0901234567", Role.ADMIN, true, now)
        );
        assertEquals("Full name must be 2 to 100 characters.", exNull.getMessage());

        IllegalArgumentException exShort = assertThrows(IllegalArgumentException.class, () ->
            new Staff(1, "alice", "A", "alice@example.com", "0901234567", Role.ADMIN, true, now)
        );
        assertEquals("Full name must be 2 to 100 characters.", exShort.getMessage());

        Staff staff = new Staff(1, "alice", "Alice Admin", "alice@example.com", "0901234567", Role.ADMIN, true, now);
        IllegalArgumentException exSetter = assertThrows(IllegalArgumentException.class, () ->
            staff.setFullName(" ")
        );
        assertEquals("Full name must be 2 to 100 characters.", exSetter.getMessage());
    }

    @Test
    void validationRejectsInvalidEmail() {
        LocalDateTime now = LocalDateTime.now();

        IllegalArgumentException exNull = assertThrows(IllegalArgumentException.class, () ->
            new Staff(1, "alice", "Alice Admin", null, "0901234567", Role.ADMIN, true, now)
        );
        assertEquals("Enter a valid email address.", exNull.getMessage());

        IllegalArgumentException exFormat = assertThrows(IllegalArgumentException.class, () ->
            new Staff(1, "alice", "Alice Admin", "not-an-email", "0901234567", Role.ADMIN, true, now)
        );
        assertEquals("Enter a valid email address.", exFormat.getMessage());

        Staff staff = new Staff(1, "alice", "Alice Admin", "alice@example.com", "0901234567", Role.ADMIN, true, now);
        IllegalArgumentException exSetter = assertThrows(IllegalArgumentException.class, () ->
            staff.setEmail("invalid@com")
        );
        assertEquals("Enter a valid email address.", exSetter.getMessage());
    }

    @Test
    void validationRejectsInvalidPhone() {
        LocalDateTime now = LocalDateTime.now();

        IllegalArgumentException exNull = assertThrows(IllegalArgumentException.class, () ->
            new Staff(1, "alice", "Alice Admin", "alice@example.com", null, Role.ADMIN, true, now)
        );
        assertEquals("Phone must be 10 digits starting with 0.", exNull.getMessage());

        IllegalArgumentException exWrongStart = assertThrows(IllegalArgumentException.class, () ->
            new Staff(1, "alice", "Alice Admin", "alice@example.com", "1234567890", Role.ADMIN, true, now)
        );
        assertEquals("Phone must be 10 digits starting with 0.", exWrongStart.getMessage());

        IllegalArgumentException exShort = assertThrows(IllegalArgumentException.class, () ->
            new Staff(1, "alice", "Alice Admin", "alice@example.com", "012345678", Role.ADMIN, true, now)
        );
        assertEquals("Phone must be 10 digits starting with 0.", exShort.getMessage());

        Staff staff = new Staff(1, "alice", "Alice Admin", "alice@example.com", "0901234567", Role.ADMIN, true, now);
        IllegalArgumentException exSetter = assertThrows(IllegalArgumentException.class, () ->
            staff.setPhone("09012345678")
        );
        assertEquals("Phone must be 10 digits starting with 0.", exSetter.getMessage());
    }

    @Test
    void validationRejectsNullRole() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            new Staff(1, "alice", "Alice Admin", "alice@example.com", "0901234567", null, true, LocalDateTime.now())
        );
        assertEquals("Role must not be null.", ex.getMessage());
    }

    @Test
    void describeRoleAndGetDisplayNameForBothRoles() {
        LocalDateTime now = LocalDateTime.now();
        Staff admin = new Staff(1, "admin", "Alice Admin", "admin@example.com", "0901234567", Role.ADMIN, true, now);
        assertEquals("Administrator", admin.describeRole());
        assertEquals("Alice Admin (Administrator)", admin.getDisplayName());
        assertTrue(admin.isAdmin());

        Staff sales = new Staff(2, "sales", "Bob Sales", "sales@example.com", "0907654321", Role.SALES_STAFF, true, now);
        assertEquals("Sales staff", sales.describeRole());
        assertEquals("Bob Sales (Sales staff)", sales.getDisplayName());
        assertFalse(sales.isAdmin());
    }

    @Test
    void toStringContainsUsernameAndNoHashOrSalt() {
        Staff staff = new Staff(
            1,
            "testuser",
            "Test User",
            "test@example.com",
            "0901234567",
            Role.SALES_STAFF,
            true,
            LocalDateTime.now()
        );
        String str = staff.toString();
        assertTrue(str.contains("testuser"));
        assertFalse(str.toLowerCase().contains("hash"));
        assertFalse(str.toLowerCase().contains("salt"));
    }

    @Test
    void fromRecordAndToRecordRoundTripEveryField() {
        LocalDateTime now = LocalDateTime.now();
        StaffRecord record = new StaffRecord(
            42,
            "admin",
            "hash123",
            "salt456",
            "Alice Admin",
            "admin@example.com",
            "0901234567",
            "ADMIN",
            true,
            now
        );

        Staff staff = Staff.fromRecord(record);
        assertEquals(42, staff.getId());
        assertEquals("admin", staff.getUsername());
        assertEquals("Alice Admin", staff.getFullName());
        assertEquals("admin@example.com", staff.getEmail());
        assertEquals("0901234567", staff.getPhone());
        assertEquals(Role.ADMIN, staff.getRole());
        assertTrue(staff.isActive());
        assertEquals(now, staff.getCreatedAt());

        StaffRecord roundTripped = staff.toRecord("hash123", "salt456");
        assertEquals(record.staffId(), roundTripped.staffId());
        assertEquals(record.username(), roundTripped.username());
        assertEquals(record.passwordHash(), roundTripped.passwordHash());
        assertEquals(record.passwordSalt(), roundTripped.passwordSalt());
        assertEquals(record.fullName(), roundTripped.fullName());
        assertEquals(record.email(), roundTripped.email());
        assertEquals(record.phone(), roundTripped.phone());
        assertEquals(record.role(), roundTripped.role());
        assertEquals(record.active(), roundTripped.active());
        assertEquals(record.createdAt(), roundTripped.createdAt());
        assertEquals(record, roundTripped);
    }
}
