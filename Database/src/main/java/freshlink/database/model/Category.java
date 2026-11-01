package freshlink.database.model;

/** A product category. parentId is null for a main category. */
public record Category(int categoryId, Integer parentId, String name) {
}
