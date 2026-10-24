package vgu.sales.database;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for DbSetup.splitStatements (no database needed).
 */
class DbSetupTest {

    @Test
    void splitStatementsRemovesCommentsAndSplits() {
        String sql = "-- c\nCREATE TABLE a(x int);\nINSERT INTO a VALUES (1);\n";
        List<String> statements = DbSetup.splitStatements(sql);
        assertEquals(2, statements.size());
        assertEquals("CREATE TABLE a(x int)", statements.get(0));
        assertEquals("INSERT INTO a VALUES (1)", statements.get(1));
    }

    @Test
    void splitStatementsHandlesEmptyInput() {
        List<String> statements = DbSetup.splitStatements("");
        assertEquals(0, statements.size());

        statements = DbSetup.splitStatements("   ");
        assertEquals(0, statements.size());

        statements = DbSetup.splitStatements(null);
        assertEquals(0, statements.size());
    }

    @Test
    void splitStatementsIgnoresCommentOnlyLines() {
        String sql = "-- comment only\n-- another comment\n";
        List<String> statements = DbSetup.splitStatements(sql);
        assertEquals(0, statements.size());
    }
}