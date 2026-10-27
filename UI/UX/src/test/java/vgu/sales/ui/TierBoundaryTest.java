package vgu.sales.ui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Validates that the UI tier does not import forbidden packages.
 * Forbidden: vgu.sales.database, java.sql, javax.sql.
 * The UI talks only to vgu.sales.controller.
 * Package-private: only JUnit runs it.
 */
class TierBoundaryTest {

    /**
     * Checks all Java source files under src/main/java for forbidden imports.
     */
    @Test
    void uiTierImportsOnlyControllerAndJdk() throws IOException {
        Path srcMainJava = Paths.get("src/main/java");
        if (!Files.exists(srcMainJava)) {
            fail("src/main/java not found from working directory " + Paths.get("").toAbsolutePath());
        }

        List<Path> javaFiles = Files.walk(srcMainJava)
                .filter(p -> p.toString().endsWith(".java"))
                .toList();

        for (Path file : javaFiles) {
            List<String> lines = Files.readAllLines(file);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.startsWith("import ")) {
                    if (line.contains("vgu.sales.database")
                            || line.contains("java.sql")
                            || line.contains("javax.sql")) {
                        fail("Forbidden import in " + file + " at line " + (i + 1) + ": " + line);
                    }
                }
            }
        }
    }
}

