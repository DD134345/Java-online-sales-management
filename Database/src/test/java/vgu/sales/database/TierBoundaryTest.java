package vgu.sales.database;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Validates that the Database tier does not import forbidden packages.
 * Forbidden: vgu.sales.controller, vgu.sales.ui, javax.swing.
 */
class TierBoundaryTest {

    @Test
    void noForbiddenImports() throws IOException {
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
                    if (line.contains("vgu.sales.controller")
                            || line.contains("vgu.sales.ui")
                            || line.contains("javax.swing")) {
                        fail("Forbidden import in " + file + " at line " + (i + 1) + ": " + line);
                    }
                }
            }
        }
    }
}