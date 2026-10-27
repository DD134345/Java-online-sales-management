package vgu.sales.controller;

/**
 * Unchecked exception thrown when an authentication attempt fails.
 *
 * Public: thrown by AuthService and caught by UI components.
 */
public class AuthenticationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs an AuthenticationException with a descriptive error message.
     *
     * @param message user-facing failure description
     * Public: instantiated by authentication service implementations.
     */
    public AuthenticationException(String message) {
        super(message);
    }
}
