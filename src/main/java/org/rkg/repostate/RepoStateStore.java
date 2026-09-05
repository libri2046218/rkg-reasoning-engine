package org.rkg.repostate;

import java.util.List;
import java.util.Optional;

/**
 * Local, GraphDB-independent store of per-repository middleware bookkeeping (§3.2): the
 * staleness flag, last chase timestamp, and last definiteness-validation result.
 *
 * <p>This store is intentionally never written into the GraphDB repository itself (no reserved
 * named graph) so that Tier A operations on real data (export, backup/restore, clear) cannot
 * disturb or be disturbed by the middleware's own state tracking.
 */
public interface RepoStateStore {

    /**
     * Creates a fresh (unchased, unvalidated) row for a newly created repository.
     *
     * @param endpointUrl GraphDB endpoint URL
     * @param repoName repository name
     */
    void createRepoState(String endpointUrl, String repoName);

    /**
     * Removes the row for a deleted repository.
     *
     * @param endpointUrl GraphDB endpoint URL
     * @param repoName repository name
     */
    void deleteRepoState(String endpointUrl, String repoName);

    /**
     * Retrieves the state for a repository.
     *
     * @param endpointUrl GraphDB endpoint URL
     * @param repoName repository name
     * @return repository state, or empty if not yet created
     */
    Optional<RepoState> get(String endpointUrl, String repoName);

    /**
     * Retrieves the state for all repositories across all endpoints.
     *
     * @return all repository states
     */
    List<RepoState> listAll();

    /**
     * Marks a repository as stale (chased = false) after a mutation.
     * Called by the connector after any Tier A mutating operation (update/import/clear).
     *
     * @param endpointUrl GraphDB endpoint URL
     * @param repoName repository name
     */
    void markStale(String endpointUrl, String repoName);

    /**
     * Marks a repository as successfully chased.
     * Called by the chase orchestrator only after all three pipeline phases succeed.
     *
     * @param endpointUrl GraphDB endpoint URL
     * @param repoName repository name
     * @param completedAt timestamp of chase completion
     */
    void markChased(String endpointUrl, String repoName, java.time.Instant completedAt);

    /**
     * Records the result of a definiteness validation run.
     * Called by the definiteness validator after each run.
     *
     * @param endpointUrl GraphDB endpoint URL
     * @param repoName repository name
     * @param definite whether the repository is definite (true/false) or null if validation failed
     * @param indefiniteElements list of indefinite class/property IRIs (empty if definite)
     */
    void recordValidation(String endpointUrl, String repoName, boolean definite, List<String> indefiniteElements);
}
