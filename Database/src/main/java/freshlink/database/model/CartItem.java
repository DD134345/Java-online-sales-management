package freshlink.database.model;

// price is the current price (already the sale price if there is one)
public record CartItem(int userId, int productId, String productName, String unit,
                       long price, int quantity, int stock) {

    public long getLineTotal() {
        return price * quantity;
    }
}
