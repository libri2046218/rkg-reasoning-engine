package org.rkg.query;

import org.rkg.connector.QueryResult;

/**
 * Default (RKG-aware) path for {@code rkg query} (§4.2, §5.4 of the software design document):
 * reasoning-enabled query over the default graph plus the reserved witness graph, for definite
 * RKGs. {@code rkg query --raw} bypasses this engine entirely and calls
 * {@code GraphDBConnector.query} directly with {@code infer=false} and no extra named graphs.
 */
public interface QueryAnsweringEngine {

    /**
     * Executes a SPARQL query with RKG semantics (reasoning-enabled, witness graph included).
     *
     * @param repoName repository to query
     * @param sparqlQuery SPARQL query text
     * @return query result (SELECT, ASK, or GRAPH)
     * @throws StaleRepositoryException if the repository has not been chased since last mutation
     *                                   (staleness check prevents unsound answers; use
     *                                   {@code --raw} flag to bypass this check)
     * @throws UnsupportedOperationException if the repository is an indefinite RKG (general query
     *                                        answering via Theorem 1 is Phase 2, not Phase 1)
     */
    QueryResult query(String repoName, String sparqlQuery);
}
