package freshlink.database.model;

import java.time.LocalDateTime;

// one step in the delivery timeline of an order
public record OrderTracking(int trackingId, int orderId, String status, String note, LocalDateTime createdAt) {
}
