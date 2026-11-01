package freshlink.database.model;

// name and price are copied when the order is placed, so later price changes don't affect old orders
public record OrderItem(int orderItemId, int orderId, int productId, String productName,
                        long unitPrice, int quantity) {

    public long getLineTotal() {
        return unitPrice * quantity;
    }
}
