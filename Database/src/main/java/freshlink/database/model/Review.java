package freshlink.database.model;

import java.time.LocalDateTime;

/** A product review with 1-5 stars. reviewerName is only filled when reading. */
public record Review(int reviewId, int productId, int userId, String reviewerName,
                     int rating, String comment, LocalDateTime createdAt) {
}
