package freshlink.database;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;

// Builds the database from schema.sql and fills it with seed.sql.
// Careful: this deletes everything that is already in the tables!
//   mvn compile exec:java                          -> freshlink
//   mvn compile exec:java -Dexec.args="--test"     -> freshlink_test
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

    private static String readFile(String name) throws Exception {
        try (InputStream in = DBSetup.class.getResourceAsStream(name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
