package org.rkg.query;

/**
 * Thrown when {@code rkg query} (default, RKG-aware) is invoked on a repository whose
 * {@code chased} flag is {@code false} — per §4.4, this is a warning-level condition directing
 * the user to re-run {@code rkg chase}, not answered silently over a stale graph.
 * {@code rkg query --raw} never triggers this check.
 */
public final class StaleRepositoryException extends RuntimeException {

    /**
     * Creates an exception for a repository that must be chased before query answering.
     *
     * @param repoName repository name that is stale
     */
    public StaleRepositoryException(String repoName) {
        super("Repository '" + repoName + "' has not been chased since its last mutation "
                + "(chased = false). Run 'rkg chase --repo " + repoName + "' before 'rkg query', "
                + "or use 'rkg query --raw' to bypass RKG semantics entirely.");
    }
}
