package freshlink.database.model;

/** A product in the shop. Prices are in VND. salePrice is null when not on sale. */
public record Product(int productId, int categoryId, String name, String unit,
                      long price, Long salePrice, int stock, String origin, String description,
                      String imageUrl, boolean voucherEligible, boolean active) {

    /** The price the customer pays. */
    public long getFinalPrice() {
        return salePrice != null ? salePrice : price;
    }

    /** Discount in percent, e.g. 12 means "-12%". */
    public int getDiscountPercent() {
        if (salePrice == null) {
            return 0;
        }
        return (int) Math.round((price - salePrice) * 100.0 / price);
    }
}
