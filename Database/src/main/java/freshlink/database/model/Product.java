package freshlink.database.model;

// Prices are in VND. salePrice is null if the product is not on sale.
public record Product(int productId, int categoryId, String name, String unit,
                      long price, Long salePrice, int stock, String origin, String description,
                      String imageUrl, boolean voucherEligible, boolean active) {

    public long getFinalPrice() {
        return salePrice != null ? salePrice : price;
    }

    // 12 means the badge shows "-12%"
    public int getDiscountPercent() {
        if (salePrice == null) {
            return 0;
        }
        return (int) Math.round((price - salePrice) * 100.0 / price);
    }
}
