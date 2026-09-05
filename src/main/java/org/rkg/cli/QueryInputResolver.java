package org.rkg.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resolves the SPARQL query text for {@code rkg query} from either an inline argument or a
 * {@code --file} attachment (§6.2 of the software design document). Exactly one of the two must
 * be supplied. Kept as a pure, static utility so it is directly unit-testable without invoking
 * picocli.
 */
final class QueryInputResolver {

    private QueryInputResolver() {
    }

    static String resolve(String inlineQuery, Path file) {
        return resolve(inlineQuery, file, "SPARQL query");
    }

    static String resolve(String inlineQuery, Path file, String inputDescription) {
        boolean hasInline = inlineQuery != null && !inlineQuery.isBlank();
        boolean hasFile = file != null;
        if (hasInline == hasFile) {
            throw new IllegalArgumentException(
                    "Supply exactly one inline " + inputDescription + " argument or --file <path.sparql>, "
                            + "not both/neither.");
        }
        if (hasFile) {
            try {
                return Files.readString(file);
            } catch (IOException e) {
                throw new IllegalArgumentException("Could not read query file '" + file + "': " + e.getMessage(), e);
            }
        }
        return inlineQuery;
    }
}
