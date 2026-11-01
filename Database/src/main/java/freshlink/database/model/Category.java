package freshlink.database.model;

// parentId is null for a main category like "Thịt, cá, trứng, hải sản"
public record Category(int categoryId, Integer parentId, String name) {
}
