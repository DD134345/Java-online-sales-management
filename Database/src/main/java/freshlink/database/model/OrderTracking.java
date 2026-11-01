package freshlink.database.model;

import java.time.LocalDateTime;

/** One step of an order's delivery timeline. */
public record OrderTracking(int trackingId, int orderId, String status, String note, LocalDateTime createdAt) {
}
