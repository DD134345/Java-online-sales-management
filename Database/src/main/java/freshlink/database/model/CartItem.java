package freshlink.database.model;

/** One product in a customer's cart, with its current price. */
public record CartItem(int userId, int productId, String productName, String unit,
                       long price, int quantity, int stock) {

    public long getLineTotal() {
        return price * quantity;
    }
}
