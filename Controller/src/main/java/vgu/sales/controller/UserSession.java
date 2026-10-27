package vgu.sales.controller;

import java.util.Optional;
import vgu.sales.controller.model.Staff;

/**
 * In-memory session state holder for the currently authenticated staff member.
 *
 * Public: used across controller services and exposed to UI for session state.
 */
public final class UserSession {

    private Staff current;

    /**
     * Constructs a new empty user session.
     *
     * Public: created by authentication service implementations.
     */
    public UserSession() {
        this.current = null;
    }

    /**
     * Starts a session for the authenticated staff member.
     *
     * @param staff the signed-in staff entity
     * @throws IllegalArgumentException if staff is null
     * Public: called by auth service on successful login.
     */
    public void start(Staff staff) {
        if (staff == null) {
            throw new IllegalArgumentException("Staff must not be null.");
        }
        this.current = staff;
    }

    /**
     * Ends the active session by clearing the current staff member.
     *
     * Public: called by auth service on logout.
     */
    public void end() {
        this.current = null;
    }

    /**
     * Returns an Optional containing the current authenticated staff member if present.
     *
     * @return Optional containing current Staff, or empty if signed out
     * Public: inspected by UI and services to access active user info.
     */
    public Optional<Staff> current() {
        return Optional.ofNullable(current);
    }

    /**
     * Checks if a staff member is currently signed in.
     *
     * @return true if a session is active, false otherwise
     * Public: used for UI authorization checks.
     */
    public boolean isSignedIn() {
        return current != null;
    }

    /**
     * Checks if the currently signed-in staff member has the administrator role.
     *
     * @return true if signed in and staff has admin role, false otherwise
     * Public: used for UI access control on admin-only features.
     */
    public boolean isAdmin() {
        return current != null && current.isAdmin();
    }
}
