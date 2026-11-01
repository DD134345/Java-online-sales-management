package freshlink.database;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Creates all tables (schema.sql) and fills them with sample data (seed.sql).
 * Run: mvn compile exec:java              (main database)
 *      mvn compile exec:java -Dexec.args="--test"   (test database)
 * WARNING: this deletes all existing data.
 */
public class DBSetup {

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("--test")) {
            DBConnection.useTestDatabase();
        }
        run();
        System.out.println("FreshLink database is ready.");
    }

    public static void run() throws Exception {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(readFile("/schema.sql"));
            stmt.execute(readFile("/seed.sql"));
        }
    }

    private static String readFile(String path) throws Exception {
        try (InputStream in = DBSetup.class.getResourceAsStream(path)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
