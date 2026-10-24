package vgu.sales.database;

import java.math.BigDecimal;
import java.util.List;

/**
 * DAO interface for Product records, extending the generic Dao.
 */
public interface ProductDao extends Dao<ProductRecord, Integer> {
    /**
     * Search products with optional filters.
     * @param keyword search keyword (name, description), null = no filter
     * @param categoryId category ID filter, null = no filter
     * @param minPrice minimum price filter, null = no filter
     * @param maxPrice maximum price filter, null = no filter
     * @param inStockOnly if true only include products with stock > 0, null = no filter
     * @return a List of matching ProductRecords
     */
    List<ProductRecord> search(String keyword, Integer categoryId, BigDecimal minPrice, BigDecimal maxPrice, Boolean inStockOnly);

    /**
     * Update the stock quantity of a product.
     * @param productId the product ID
     * @param newStock the new stock quantity
     * @return true if a row was changed, false otherwise
     */
    boolean updateStock(Integer productId, Integer newStock);

    /**
     * Find products with stock at or below a threshold.
     * @param threshold the stock threshold
     * @return a List of LowStockRecords
     */
    List<LowStockRecord> findLowStock(int threshold);
}