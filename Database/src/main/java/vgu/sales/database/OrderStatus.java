package vgu.sales.database;

/**
 * String constants for order statuses and transition validation.
 * This is a String-constant holder because the Database tier must not depend on the Controller's enum.
 */
public final class OrderStatus {
    /** Prevent instantiation. */
    private OrderStatus() {}

    /** Order is pending payment. */
    public static final String PENDING = "PENDING";

    /** Order has been paid. */
    public static final String PAID = "PAID";

    /** Order has been shipped. */
    public static final String SHIPPED = "SHIPPED";

    /** Order has been cancelled. */
    public static final String CANCELLED = "CANCELLED";

    /**
     * Check if a status transition is valid.
     * Valid transitions: PENDING->PAID, PENDING->CANCELLED, PAID->SHIPPED, PAID->CANCELLED.
     * @param from the current status
     * @param to the target status
     * @return true if the transition is allowed, false otherwise
     */
    public static boolean canTransition(String from, String to) {
        if (from == null || to == null) {
            return false;
        }
        return switch (from) {
            case PENDING -> to.equals(PAID) || to.equals(CANCELLED);
            case PAID -> to.equals(SHIPPED) || to.equals(CANCELLED);
            default -> false;
        };
    }
}