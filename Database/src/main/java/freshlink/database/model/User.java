package freshlink.database.model;

import java.time.LocalDateTime;

/** A customer or admin account. role is "CUSTOMER" or "ADMIN". */
public record User(int userId, String username, String passwordHash, String passwordSalt,
                   String fullName, String email, String phone, String role,
                   boolean active, LocalDateTime createdAt) {
}
