package vgu.sales.controller.model;

/**
 * Roles granted to staff accounts determining access permissions.
 *
 * Public: used across the controller and UI tiers.
 */
public enum Role {

    /**
     * System administrator with full access to management features.
     */
    ADMIN,

    /**
     * Sales staff handling daily shop operations, products and orders.
     */
    SALES_STAFF
}
