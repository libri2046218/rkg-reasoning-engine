package org.rkg.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for the {@code rkg query} input resolution logic (§6.2 of the software design
 * document): exactly one of inline argument or {@code --file} must be supplied.
 */
@org.junit.jupiter.api.Tag("unit")
class QueryInputResolverTest {

    @Test
    void resolvesInlineQuery() {
        assertEquals("SELECT * WHERE { ?s ?p ?o }",
                QueryInputResolver.resolve("SELECT * WHERE { ?s ?p ?o }", null));
    }

    @Test
    void resolvesFileQuery(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("query.sparql");
        Files.writeString(file, "SELECT * WHERE { ?s a ?type }");

        assertEquals("SELECT * WHERE { ?s a ?type }", QueryInputResolver.resolve(null, file));
    }

    @Test
    void rejectsNeitherInputSupplied() {
        assertThrows(IllegalArgumentException.class, () -> QueryInputResolver.resolve(null, null));
    }

    @Test
    void rejectsBothInputsSuppliedTogether(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("query.sparql");
        Files.writeString(file, "SELECT * WHERE { ?s ?p ?o }");

        assertThrows(IllegalArgumentException.class,
                () -> QueryInputResolver.resolve("SELECT * WHERE { ?s ?p ?o }", file));
    }
}
