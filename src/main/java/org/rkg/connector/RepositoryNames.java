package org.rkg.connector;

import java.util.Objects;
import java.util.regex.Pattern;

/** Validates GraphDB repository identifiers before embedding them in HTTP paths or RDF config. */
final class RepositoryNames {

    private static final Pattern VALID_IDENTIFIER = Pattern.compile("[A-Za-z][A-Za-z0-9._-]{0,63}");

    private RepositoryNames() {
    }

    static String requireValid(String repositoryName) {
        Objects.requireNonNull(repositoryName, "repositoryName");
        if (!VALID_IDENTIFIER.matcher(repositoryName).matches()) {
            throw new IllegalArgumentException(
                    "Repository name must start with a letter and contain only letters, digits, '.', '_', or '-' (max 64 characters).");
        }
        return repositoryName;
    }
}
