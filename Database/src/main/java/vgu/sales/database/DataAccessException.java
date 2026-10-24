package vgu.sales.database;

/**
 * Unchecked exception wrapping SQLException for the Database tier.
 */
public class DataAccessException extends RuntimeException {
    /** Serialization version UID. */
    private static final long serialVersionUID = 1L;

    /**
     * Create a new DataAccessException with a message.
     * @param message the error message
     */
    public DataAccessException(String message) {
        super(message);
    }

    /**
     * Create a new DataAccessException with a message and cause.
     * @param message the error message
     * @param cause the cause
     */
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}