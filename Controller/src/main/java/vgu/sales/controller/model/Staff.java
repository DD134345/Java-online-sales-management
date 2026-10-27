package vgu.sales.controller.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.regex.Pattern;
import vgu.sales.database.StaffRecord;

/**
 * Staff entity representing system users with assigned roles and credentials.
 *
 * Public: services and the UI interact directly with Staff objects.
 */
public class Staff extends Person {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9._]{3,50}$");

    private final String username;
    private final Role role;
    private final boolean active;
    private final LocalDateTime createdAt;

    /**
     * Constructs a validated Staff instance.
     *
     * @param id database identifier or null
     * @param username unique username (3 to 50 lowercase letters, digits, dots or underscores)
     * @param fullName full name of the staff member
     * @param email valid email address
     * @param phone 10-digit phone number starting with 0
     * @param role assigned authorization role
     * @param active whether the account is currently enabled
     * @param createdAt creation timestamp
     * Public: created by factory methods and services.
     */
    public Staff(
        Integer id,
        String username,
        String fullName,
        String email,
        String phone,
        Role role,
        boolean active,
        LocalDateTime createdAt
    ) {
        super(id, fullName, email, phone);
        if (username == null) {
            throw new IllegalArgumentException("Username must be 3 to 50 letters, digits, dots or underscores.");
        }
        String trimmedLower = username.trim().toLowerCase();
        if (!USERNAME_PATTERN.matcher(trimmedLower).matches()) {
            throw new IllegalArgumentException("Username must be 3 to 50 letters, digits, dots or underscores.");
        }
        if (role == null) {
            throw new IllegalArgumentException("Role must not be null.");
        }
        this.username = trimmedLower;
        this.role = role;
        this.active = active;
        this.createdAt = createdAt;
    }

    /**
     * Returns the username of the staff member.
     *
     * @return username
     * Public: services and the UI read it.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the role assigned to the staff member.
     *
     * @return role
     * Public: services and the UI inspect it.
     */
    public Role getRole() {
        return role;
    }

    /**
     * Returns whether the account is active.
     *
     * @return true if active, false otherwise
     * Public: services and the UI inspect it.
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Returns the timestamp when this staff account was created.
     *
     * @return creation timestamp
     * Public: services and the UI read it.
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Checks if the staff member has administrator privileges.
     *
     * @return true if the role is ADMIN, false otherwise
     * Public: UI and services call this to determine access rights.
     */
    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    /**
     * Returns a human-readable role name for this staff member.
     *
     * @return "Administrator" or "Sales staff"
     * Public: services and UI call it via polymorphism for display.
     */
    @Override
    public String describeRole() {
        return switch (role) {
            case ADMIN -> "Administrator";
            case SALES_STAFF -> "Sales staff";
        };
    }

    /**
     * Converts a database StaffRecord into a domain Staff instance.
     *
     * @param r database record to convert
     * @return newly constructed Staff instance
     * Public: called by services and DAOs to map database records.
     */
    public static Staff fromRecord(StaffRecord r) {
        Objects.requireNonNull(r, "StaffRecord must not be null.");
        return new Staff(
            r.staffId(),
            r.username(),
            r.fullName(),
            r.email(),
            r.phone(),
            Role.valueOf(r.role()),
            Boolean.TRUE.equals(r.active()),
            r.createdAt()
        );
    }

    /**
     * Converts this domain entity to a database StaffRecord.
     *
     * @param passwordHash hashed password string
     * @param passwordSalt password salt string
     * @return mapped StaffRecord
     * Public: called by services to prepare records for persistence.
     */
    public StaffRecord toRecord(String passwordHash, String passwordSalt) {
        return new StaffRecord(
            getId(),
            username,
            passwordHash,
            passwordSalt,
            getFullName(),
            getEmail(),
            getPhone(),
            role.name(),
            active,
            createdAt
        );
    }

    /**
     * Returns a string representation of the staff member without sensitive credential data.
     *
     * @return string representation
     * Public: standard Object method override for logging and debugging.
     */
    @Override
    public String toString() {
        return "Staff[id=" + getId() + ", username=" + username + ", role=" + role + ", active=" + active + "]";
    }

    /**
     * Checks equality based on staff username uniqueness.
     *
     * @param o target object for comparison
     * @return true if both are Staff instances with identical username
     * Public: standard Object method override for set and map collections.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Staff staff)) {
            return false;
        }
        return Objects.equals(username, staff.username);
    }

    /**
     * Generates hash code based on username.
     *
     * @return hash code value
     * Public: standard Object method override accompanying equals.
     */
    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
}
