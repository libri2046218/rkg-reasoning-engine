package org.rkg.query;

import java.util.List;
import java.util.Optional;
import org.rkg.connector.GraphDBConnector;
import org.rkg.connector.QueryResult;
import org.rkg.repostate.RepoState;
import org.rkg.repostate.RepoStateStore;

/**
 * {@link QueryAnsweringEngine} implementation for Phase 1 (definite RKGs only), per §5.4 of the
 * software design document: if the repository is chased and definite, forwards to
 * {@code GraphDBConnector.query(repoName, sparqlQuery, infer=true, namedGraphs=["urn:rkg:witnesses"])}
 * — sound and complete by Proposition 2, since the materialized {@code Ch(G)} already encodes the
 * canonical-model interpretation. If {@code chased = false}, refuses with a staleness warning
 * rather than silently answering over a stale or witness-incomplete graph. See
 * docs/software-design-document.md §5.4 for full details.
 *
 * <p>General/indefinite RKGs (Phase 2) will instead construct completions {@code Gφ} over
 * indefinite elements and merge results per Theorem 1 — not implemented in Phase 1.
 */
public final class Rdf4jQueryAnsweringEngine implements QueryAnsweringEngine {

    private static final String WITNESS_GRAPH = "urn:rkg:witnesses";

    private final GraphDBConnector connector;
    private final RepoStateStore repoStateStore;
    private final String endpointUrl;

    /**
     * Creates a query answering engine for Phase 1 (definite RKGs only). Every invocation of
     * {@link #query} first checks staleness ({@code chased} flag) and definiteness; if either
     * fails, raises an exception before executing the query. Sound and complete by Proposition 2
     * for definite RKGs, since the materialized closure encodes the canonical model.
     *
     * @param connector used to forward queries to GraphDB with reasoning + witness graph union
     * @param repoStateStore used to check staleness and definiteness before answering
     * @param endpointUrl the GraphDB endpoint URL (for state lookups)
     */
    public Rdf4jQueryAnsweringEngine(GraphDBConnector connector, RepoStateStore repoStateStore, String endpointUrl) {
        this.connector = connector;
        this.repoStateStore = repoStateStore;
        this.endpointUrl = endpointUrl;
    }

    @Override
    public QueryResult query(String repoName, String sparqlQuery) {
        Optional<RepoState> state = repoStateStore.get(endpointUrl, repoName);
        boolean chased = state.map(RepoState::chased).orElse(false);
        if (!chased) {
            throw new StaleRepositoryException(repoName);
        }
        Boolean definite = state.flatMap(RepoState::definiteOptional).orElse(null);
        if (Boolean.FALSE.equals(definite)) {
            throw new UnsupportedOperationException(
                    "Repository '" + repoName + "' is an indefinite RKG; general query answering "
                            + "(completion algorithm, Theorem 1) is out of scope for Phase 1. "
                            + "See docs/software-design-document.md §1.4.");
        }
        return connector.query(repoName, sparqlQuery, true, List.of(WITNESS_GRAPH));
    }
}
