package vgu.sales.controller;

import java.util.Optional;
import vgu.sales.controller.model.Staff;

/**
 * Service interface for staff authentication, session management, and self sign-up.
 *
 * Public: primary controller interface consumed by the UI tier.
 */
public interface AuthService {

    /**
     * Authenticates a staff member using their username and password.
     *
     * @param username staff login username
     * @param password staff account password
     * @return authenticated Staff member
     * @throws AuthenticationException if credentials are blank, invalid, or account is deactivated
     * Public: called by UI login dialog.
     */
    Staff login(String username, char[] password);

    /**
     * Logs out the currently signed-in staff member by ending the active session.
     *
     * Public: called by UI logout action.
     */
    void logout();

    /**
     * Retrieves the currently authenticated staff member, if any.
     *
     * @return Optional containing current Staff, or empty if signed out
     * Public: called by UI to query active session details.
     */
    Optional<Staff> currentStaff();

    /**
     * Registers a new staff account with the SALES_STAFF role.
     *
     * @param username unique username
     * @param password plain text password
     * @param fullName full name of the staff member
     * @param email valid email address
     * @param phone 10-digit phone number starting with 0
     * @return created Staff member
     * @throws IllegalArgumentException if field validation fails or username is already taken
     * Public: called by UI sign-up dialog.
     */
    Staff signUp(String username, char[] password, String fullName, String email, String phone);
}
