package vgu.sales.controller;

import java.util.Optional;
import vgu.sales.controller.model.Role;
import vgu.sales.controller.model.Staff;
import vgu.sales.database.DataAccessException;
import vgu.sales.database.StaffDao;
import vgu.sales.database.StaffRecord;

/**
 * Implementation of AuthService managing authentication, session state and registration.
 *
 * Public: instantiated by ControllerFactory to provide authentication services.
 */
public class AuthServiceImpl implements AuthService {

    private static final String MSG_BLANK_CREDENTIALS = "Enter your username and password.";
    private static final String MSG_WRONG_CREDENTIALS = "Wrong username or password.";
    private static final String MSG_DEACTIVATED = "Account has been deactivated. Please contact the administrator.";
    private static final String MSG_WEAK_PASSWORD = "Password must be at least 8 characters with a letter and a digit.";
    private static final String MSG_USERNAME_TAKEN = "Username is already taken.";

    private final StaffDao staffDao;
    private final UserSession session = new UserSession();

    /**
     * Constructs an AuthServiceImpl backed by the given StaffDao.
     *
     * @param staffDao DAO used for querying and persisting staff records
     * @throws IllegalArgumentException if staffDao is null
     * Public: instantiated by ControllerFactory and test suites.
     */
    public AuthServiceImpl(StaffDao staffDao) {
        if (staffDao == null) {
            throw new IllegalArgumentException("StaffDao must not be null.");
        }
        this.staffDao = staffDao;
    }

    /**
     * Authenticates a staff member against stored credentials.
     *
     * @param username staff login username
     * @param password staff account password
     * @return authenticated Staff member
     * @throws AuthenticationException if credentials are missing, invalid, or account is deactivated
     * Public: implements AuthService interface method.
     */
    @Override
    public Staff login(String username, char[] password) {
        if (username == null || username.trim().isEmpty() || password == null || password.length == 0) {
            throw new AuthenticationException(MSG_BLANK_CREDENTIALS);
        }

        Optional<StaffRecord> recordOpt = staffDao.findByUsername(username.trim());
        if (recordOpt.isEmpty()) {
            throw new AuthenticationException(MSG_WRONG_CREDENTIALS);
        }

        StaffRecord record = recordOpt.get();
        if (!PasswordHasher.matches(password, record.passwordSalt(), record.passwordHash())) {
            throw new AuthenticationException(MSG_WRONG_CREDENTIALS);
        }

        if (!Boolean.TRUE.equals(record.active())) {
            throw new AuthenticationException(MSG_DEACTIVATED);
        }

        Staff staff = Staff.fromRecord(record);
        session.start(staff);
        return staff;
    }

    /**
     * Logs out the currently signed-in staff member.
     *
     * Public: implements AuthService interface method.
     */
    @Override
    public void logout() {
        session.end();
    }

    /**
     * Retrieves the current authenticated staff member from the active session.
     *
     * @return Optional containing current Staff, or empty if signed out
     * Public: implements AuthService interface method.
     */
    @Override
    public Optional<Staff> currentStaff() {
        return session.current();
    }

    /**
     * Registers a new staff account with the SALES_STAFF role.
     * Handles concurrent registration races where a duplicate username is rejected at insert time.
     *
     * @param username desired username
     * @param password plain text password
     * @param fullName full name of the staff member
     * @param email valid email address
     * @param phone 10-digit phone number starting with 0
     * @return created Staff member
     * @throws IllegalArgumentException if field validation fails or username is already taken
     * Public: implements AuthService interface method.
     */
    @Override
    public Staff signUp(String username, char[] password, String fullName, String email, String phone) {
        Staff staff = new Staff(null, username, fullName, email, phone, Role.SALES_STAFF, true, null);
        validatePassword(password);

        if (staffDao.findByUsername(staff.getUsername()).isPresent()) {
            throw new IllegalArgumentException(MSG_USERNAME_TAKEN);
        }

        String salt = PasswordHasher.newSalt();
        String hash = PasswordHasher.hash(salt, password);
        StaffRecord saved;
        try {
            saved = staffDao.insert(staff.toRecord(hash, salt));
        } catch (DataAccessException ex) {
            if (staffDao.findByUsername(staff.getUsername()).isPresent()) {
                throw new IllegalArgumentException(MSG_USERNAME_TAKEN);
            }
            throw ex;
        }
        return Staff.fromRecord(saved);
    }

    /**
     * Validates that the password meets security constraints (at least 8 chars, one letter, one digit).
     *
     * @param password plain text password character array
     * @throws IllegalArgumentException if password fails policy
     * Private: internal validation helper.
     */
    private void validatePassword(char[] password) {
        if (password == null || password.length < 8) {
            throw new IllegalArgumentException(MSG_WEAK_PASSWORD);
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : password) {
            if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
            if (hasLetter && hasDigit) {
                break;
            }
        }
        if (!hasLetter || !hasDigit) {
            throw new IllegalArgumentException(MSG_WEAK_PASSWORD);
        }
    }
}
