package org.rkg.repostate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Per-repository local bookkeeping (§3.2 of the software design document): the staleness
 * ({@code chased}) flag, last chase timestamp, and last definiteness-validation result, keyed by
 * (GraphDB endpoint URL, repository name).
 *
 * @param endpointUrl GraphDB endpoint URL this repository belongs to
 * @param repoName repository name on that endpoint
 * @param chased whether the repository has been chased since last mutation
 * @param lastChaseTimestamp timestamp of the most recent successful chase (null if never chased)
 * @param definite tri-state: null (not validated), true (definite), false (indefinite)
 * @param indefiniteElements list of indefinite class/property IRIs (empty if definite or not validated)
 */
public record RepoState(
        String endpointUrl,
        String repoName,
        boolean chased,
        Instant lastChaseTimestamp,
        Boolean definite,
        List<String> indefiniteElements
) {
    /**
     * Creates a fresh (unchased, unvalidated) repository state.
     *
     * @param endpointUrl GraphDB endpoint URL
     * @param repoName repository name
     * @return fresh RepoState (chased=false, definite=null)
     */
    public static RepoState freshlyCreated(String endpointUrl, String repoName) {
        return new RepoState(endpointUrl, repoName, false, null, null, List.of());
    }

    /**
     * Returns the last chase timestamp, if any.
     *
     * @return optional timestamp (empty if never chased)
     */
    public Optional<Instant> lastChaseTimestampOptional() {
        return Optional.ofNullable(lastChaseTimestamp);
    }

    /**
     * The definiteness tri-state: null (not yet validated), true (definite RKG), false (indefinite).
     *
     * @return optional definiteness; empty if {@code validate} command has never run for this
     *         (endpoint, repoName) pair
     */
    public Optional<Boolean> definiteOptional() {
        return Optional.ofNullable(definite);
    }
}
