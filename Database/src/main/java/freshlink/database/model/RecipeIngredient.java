package freshlink.database.model;

// productId is null when the shop doesn't sell this ingredient (e.g. coconut water)
public record RecipeIngredient(int ingredientId, int recipeId, String name, String amount, Integer productId) {
}
