package vgu.sales.database;

/**
 * Exception thrown when an order item cannot be fulfilled due to insufficient inventory.
 * Public to allow the Controller and UI tiers to handle inventory errors.
 */
public class InsufficientStockException extends DataAccessException {

    /** Identifier of the product with insufficient inventory. Private for encapsulation. */
    private final Integer productId;

    /** Quantity of the product requested by the customer. Private for encapsulation. */
    private final int requested;

    /** Current quantity available in stock. Private for encapsulation. */
    private final int available;

    /**
     * Constructs a new InsufficientStockException.
     * Public so DAOs can raise this error when checking stock during checkout.
     *
     * @param productId identifier of the product
     * @param requested quantity requested
     * @param available quantity currently in stock
     */
    public InsufficientStockException(Integer productId, int requested, int available) {
        super("Insufficient stock for product id " + productId + ": requested " + requested + ", available " + available);
        this.productId = productId;
        this.requested = requested;
        this.available = available;
    }

    /**
     * Returns the product identifier.
     * Public to allow Controller and UI to inspect the affected product.
     *
     * @return the product id
     */
    public Integer getProductId() {
        return productId;
    }

    /**
     * Returns the quantity requested.
     * Public to reveal requested unit count that caused failure.
     *
     * @return the requested quantity
     */
    public int getRequested() {
        return requested;
    }

    /**
     * Returns the quantity available.
     * Public to reveal maximum physical inventory count remaining.
     *
     * @return the available quantity
     */
    public int getAvailable() {
        return available;
    }
}
