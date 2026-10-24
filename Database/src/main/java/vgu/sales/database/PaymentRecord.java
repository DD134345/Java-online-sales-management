package vgu.sales.database;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Immutable carrier for one row of table payment; id is null before insert.
 * @param paymentId PK or null before insert
 * @param orderId FK of paid order
 * @param method CASH or CARD only
 * @param amount payment >= 0
 * @param paidAt paid datetime
 * @param cashReceived cash (CASH) or null
 * @param changeAmount change (CASH) or null
 * @param bankReference bank ref (CARD) or null
 * @param cardholderName cardholder (CARD) or null
 */
public record PaymentRecord(
    Integer paymentId,
    Integer orderId,
    String method,
    BigDecimal amount,
    LocalDateTime paidAt,
    BigDecimal cashReceived,
    BigDecimal changeAmount,
    String bankReference,
    String cardholderName
) {
    /** Only CASH or CARD. Amount not null, >= 0. */
    public PaymentRecord {
        if (!"CASH".equals(method) && !"CARD".equals(method)) throw new IllegalArgumentException("method must be CASH or CARD");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("amount must be >=0");
    }
}
