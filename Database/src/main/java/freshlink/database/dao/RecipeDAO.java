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

// Queries for the recipes and recipe_ingredients tables
public class RecipeDAO {

    public List<Recipe> getAllRecipes() throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM recipes ORDER BY recipe_id")) {
            return readRecipes(ps.executeQuery());
        }
    }

    public Recipe getRecipeById(int recipeId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM recipes WHERE recipe_id = ?")) {
            ps.setInt(1, recipeId);
            List<Recipe> list = readRecipes(ps.executeQuery());
            return list.isEmpty() ? null : list.get(0);
        }
    }

    public List<Recipe> searchRecipes(String keyword) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM recipes WHERE LOWER(title) LIKE LOWER(?) ORDER BY title")) {
            ps.setString(1, "%" + keyword.trim() + "%");
            return readRecipes(ps.executeQuery());
        }
    }

    // recipes that use this product, for "Món ngon với sản phẩm này" on the product page
    public List<Recipe> getRecipesByProduct(int productId) throws SQLException {
        String sql = "SELECT * FROM recipes WHERE recipe_id IN "
                + "(SELECT recipe_id FROM recipe_ingredients WHERE product_id = ?) ORDER BY title";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            return readRecipes(ps.executeQuery());
        }
    }

    public List<RecipeIngredient> getIngredients(int recipeId) throws SQLException {
        List<RecipeIngredient> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM recipe_ingredients WHERE recipe_id = ? ORDER BY ingredient_id")) {
            ps.setInt(1, recipeId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new RecipeIngredient(rs.getInt("ingredient_id"), rs.getInt("recipe_id"),
                        rs.getString("name"), rs.getString("amount"), rs.getObject("product_id", Integer.class)));
            }
        }
        return list;
    }

    // saves the recipe and all its ingredients together, returns the new recipe id
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
                recipeId = rs.getInt(1);
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
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM recipes WHERE recipe_id = ?")) {
            ps.setInt(1, recipeId);
            return ps.executeUpdate() > 0;
        }
    }

    private List<Recipe> readRecipes(ResultSet rs) throws SQLException {
        List<Recipe> list = new ArrayList<>();
        while (rs.next()) {
            list.add(new Recipe(rs.getInt("recipe_id"), rs.getString("title"), rs.getString("description"),
                    rs.getString("instructions"), rs.getInt("cook_time"), rs.getInt("servings")));
        }
        return list;
    }
}
