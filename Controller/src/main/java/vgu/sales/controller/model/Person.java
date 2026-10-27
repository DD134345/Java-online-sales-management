package vgu.sales.controller.model;

import java.util.regex.Pattern;

/**
 * Abstract base class holding shared identity and contact details for people in the system.
 *
 * Public: base class accessible by controller services, model subclasses, and UI.
 */
public abstract class Person {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^0\\d{9}$");

    private final Integer id;
    private String fullName;
    private String email;
    private String phone;

    /**
     * Initializes a person with an identifier and validated contact attributes.
     *
     * @param id the entity identifier, or null if not yet persisted
     * @param fullName the full name (2 to 100 characters)
     * @param email valid email address (up to 100 characters)
     * @param phone 10-digit phone number starting with 0
     * Protected: only subclasses instantiate Person.
     */
    protected Person(Integer id, String fullName, String email, String phone) {
        this.id = id;
        setFullName(fullName);
        setEmail(email);
        setPhone(phone);
    }

    /**
     * Returns the database identifier of this person.
     *
     * @return the id, or null if not yet persisted
     * Public: services and the UI read it.
     */
    public Integer getId() {
        return id;
    }

    /**
     * Returns the full name of this person.
     *
     * @return full name
     * Public: services and the UI read it.
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Sets and validates the full name.
     *
     * @param fullName full name between 2 and 100 characters
     * Public: services and the UI update it.
     */
    public void setFullName(String fullName) {
        if (fullName == null) {
            throw new IllegalArgumentException("Full name must be 2 to 100 characters.");
        }
        String trimmed = fullName.trim();
        if (trimmed.length() < 2 || trimmed.length() > 100) {
            throw new IllegalArgumentException("Full name must be 2 to 100 characters.");
        }
        this.fullName = trimmed;
    }

    /**
     * Returns the email address.
     *
     * @return email address
     * Public: services and the UI read it.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets and validates the email address.
     *
     * @param email email matching standard pattern up to 100 characters
     * Public: services and the UI update it.
     */
    public void setEmail(String email) {
        if (email == null) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        String trimmed = email.trim();
        if (trimmed.length() > 100 || !EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        this.email = trimmed;
    }

    /**
     * Returns the phone number.
     *
     * @return phone number
     * Public: services and the UI read it.
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Sets and validates the phone number.
     *
     * @param phone 10-digit phone number starting with 0
     * Public: services and the UI update it.
     */
    public void setPhone(String phone) {
        if (phone == null) {
            throw new IllegalArgumentException("Phone must be 10 digits starting with 0.");
        }
        String trimmed = phone.trim();
        if (!PHONE_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Phone must be 10 digits starting with 0.");
        }
        this.phone = trimmed;
    }

    /**
     * Returns a human-readable role description specific to the subclass.
     *
     * @return role description string
     * Public: overridden by subclasses and invoked polymorphically by callers.
     */
    public abstract String describeRole();

    /**
     * Returns a formatted display name containing full name and role.
     *
     * @return formatted display name
     * Public: services and UI read it for display.
     */
    public String getDisplayName() {
        return fullName + " (" + describeRole() + ")";
    }
}
