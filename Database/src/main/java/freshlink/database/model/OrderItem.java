package freshlink.database.model;

/** One product in an order. Name and price are saved at checkout time. */
public record OrderItem(int orderItemId, int orderId, int productId, String productName,
                        long unitPrice, int quantity) {

    public long getLineTotal() {
        return unitPrice * quantity;
    }
}
