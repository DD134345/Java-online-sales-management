package freshlink.database.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

// total = subtotal - discount + shippingFee
// deliverySlot:  "08:00-10:00", "10:00-12:00", "14:00-16:00", "16:00-18:00", "18:00-20:00"
// paymentMethod: COD, BANK_CARD, MOMO, ZALOPAY
// paymentStatus: UNPAID, PAID, REFUNDED
// status:        PENDING, CONFIRMED, PACKING, SHIPPING, DELIVERED, CANCELLED
public record Order(int orderId, int userId, String receiverName, String receiverPhone,
                    String deliveryAddress, LocalDate deliveryDate, String deliverySlot,
                    Integer voucherId, long subtotal, long discount, long shippingFee, long total,
                    String paymentMethod, String paymentStatus, String status, String note,
                    LocalDateTime createdAt) {
}
