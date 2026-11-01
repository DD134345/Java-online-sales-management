package freshlink.database.model;

// cookTime is in minutes
public record Recipe(int recipeId, String title, String description, String instructions,
                     int cookTime, int servings) {
}
