package freshlink.database.model;

import java.time.LocalDateTime;

/**
 * A discount code. discountType is "PERCENT" (value 1-100) or "FIXED" (value in VND).
 * maxDiscount and usageLimit are null when there is no limit.
 */
public record Voucher(int voucherId, String code, String description, String discountType,
                      long discountValue, long minOrderValue, Long maxDiscount,
                      LocalDateTime startDate, LocalDateTime endDate,
                      Integer usageLimit, int usedCount, boolean active) {
}
