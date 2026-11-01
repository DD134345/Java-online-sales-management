package freshlink.database.model;

/** One ingredient of a recipe. productId is null if the shop does not sell it. */
public record RecipeIngredient(int ingredientId, int recipeId, String name, String amount, Integer productId) {
}
