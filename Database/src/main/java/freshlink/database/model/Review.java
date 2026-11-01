package freshlink.database.model;

import java.time.LocalDateTime;

// rating is 1-5 stars. reviewerName is only filled in when reading reviews.
public record Review(int reviewId, int productId, int userId, String reviewerName,
                     int rating, String comment, LocalDateTime createdAt) {
}
