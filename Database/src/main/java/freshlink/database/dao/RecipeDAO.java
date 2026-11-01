package freshlink.database.dao;

import freshlink.database.DBConnection;
import freshlink.database.model.Recipe;
import freshlink.database.model.RecipeIngredient;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** SQL for the recipes and recipe_ingredients tables. */
public class RecipeDAO {

    public List<Recipe> getAllRecipes() throws SQLException {
        return getRecipes("SELECT * FROM recipes ORDER BY recipe_id", null);
    }

    public Recipe getRecipeById(int recipeId) throws SQLException {
        List<Recipe> list = getRecipes("SELECT * FROM recipes WHERE recipe_id = ?", recipeId);
        return list.isEmpty() ? null : list.get(0);
    }

    /** Search by title, not case-sensitive. */
    public List<Recipe> searchRecipes(String keyword) throws SQLException {
        return getRecipes("SELECT * FROM recipes WHERE LOWER(title) LIKE LOWER(?) ORDER BY title",
                "%" + keyword.trim() + "%");
    }

    /** Recipes that use a product, to show "Món ngon với sản phẩm này" on the product page. */
    public List<Recipe> getRecipesByProduct(int productId) throws SQLException {
        return getRecipes("SELECT * FROM recipes WHERE recipe_id IN "
                + "(SELECT recipe_id FROM recipe_ingredients WHERE product_id = ?) ORDER BY title", productId);
    }

    public List<RecipeIngredient> getIngredients(int recipeId) throws SQLException {
        String sql = "SELECT * FROM recipe_ingredients WHERE recipe_id = ? ORDER BY ingredient_id";
        List<RecipeIngredient> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, recipeId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new RecipeIngredient(rs.getInt("ingredient_id"), rs.getInt("recipe_id"),
                        rs.getString("name"), rs.getString("amount"), rs.getObject("product_id", Integer.class)));
            }
        }
        return list;
    }

    /** Saves a recipe and its ingredients together. Returns the new recipe_id. */
    public int addRecipe(Recipe recipe, List<RecipeIngredient> ingredients) throws SQLException {
        Connection conn = DBConnection.getConnection();
        try {
            conn.setAutoCommit(false);
            int recipeId;
            String sql = "INSERT INTO recipes (title, description, instructions, cook_time, servings) "
                    + "VALUES (?, ?, ?, ?, ?) RETURNING recipe_id";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, recipe.title());
                ps.setString(2, recipe.description());
                ps.setString(3, recipe.instructions());
                ps.setInt(4, recipe.cookTime());
                ps.setInt(5, recipe.servings());
                ResultSet rs = ps.executeQuery();
                rs.next();
                recipeId = rs.getInt("recipe_id");
            }
            for (RecipeIngredient ing : ingredients) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO recipe_ingredients (recipe_id, name, amount, product_id) VALUES (?, ?, ?, ?)")) {
                    ps.setInt(1, recipeId);
                    ps.setString(2, ing.name());
                    ps.setString(3, ing.amount());
                    ps.setObject(4, ing.productId());
                    ps.executeUpdate();
                }
            }
            conn.commit();
            return recipeId;
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.close();
        }
    }

    public boolean deleteRecipe(int recipeId) throws SQLException {
        String sql = "DELETE FROM recipes WHERE recipe_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, recipeId);
            return ps.executeUpdate() > 0;
        }
    }

    /** Runs a recipe query with zero or one parameter. */
    private List<Recipe> getRecipes(String sql, Object param) throws SQLException {
        List<Recipe> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (param != null) {
                ps.setObject(1, param);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Recipe(rs.getInt("recipe_id"), rs.getString("title"), rs.getString("description"),
                        rs.getString("instructions"), rs.getInt("cook_time"), rs.getInt("servings")));
            }
        }
        return list;
    }
}
