package org.rkg.query;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.rkg.connector.GraphDBConnector;
import org.rkg.connector.QueryResult;
import org.rkg.repostate.RepoState;
import org.rkg.repostate.RepoStateStore;

/**
 * Unit tests for {@link Rdf4jQueryAnsweringEngine} (§5.4 of the software design document):
 * staleness refusal, indefinite-graph rejection, and the definite/chased happy path routing to
 * the witness-graph-inclusive, reasoning-enabled query.
 */
@org.junit.jupiter.api.Tag("unit")
class Rdf4jQueryAnsweringEngineTest {

    private static final String REPO = "test-repo";
    private static final String ENDPOINT = "http://localhost:7200";

    @Test
    void refusesQueryWhenRepositoryIsStale() {
        GraphDBConnector connector = mock(GraphDBConnector.class);
        RepoStateStore repoStateStore = mock(RepoStateStore.class);
        when(repoStateStore.get(ENDPOINT, REPO))
                .thenReturn(Optional.of(new RepoState(ENDPOINT, REPO, false, null, true, List.of())));

        var engine = new Rdf4jQueryAnsweringEngine(connector, repoStateStore, ENDPOINT);
        assertThrows(StaleRepositoryException.class, () -> engine.query(REPO, "SELECT * WHERE { ?s ?p ?o }"));
    }

    @Test
    void refusesQueryWhenRepositoryIsIndefinite() {
        GraphDBConnector connector = mock(GraphDBConnector.class);
        RepoStateStore repoStateStore = mock(RepoStateStore.class);
        when(repoStateStore.get(ENDPOINT, REPO))
                .thenReturn(Optional.of(new RepoState(ENDPOINT, REPO, true, Instant.now(), false, List.of("http://ex.org#Bad"))));

        var engine = new Rdf4jQueryAnsweringEngine(connector, repoStateStore, ENDPOINT);
        assertThrows(UnsupportedOperationException.class, () -> engine.query(REPO, "SELECT * WHERE { ?s ?p ?o }"));
    }

    @Test
    void routesToConnectorWithInferenceAndWitnessGraphWhenChasedAndDefinite() {
        GraphDBConnector connector = mock(GraphDBConnector.class);
        RepoStateStore repoStateStore = mock(RepoStateStore.class);
        when(repoStateStore.get(ENDPOINT, REPO))
                .thenReturn(Optional.of(new RepoState(ENDPOINT, REPO, true, Instant.now(), true, List.of())));
        when(connector.query(eq(REPO), eq("SELECT * WHERE { ?s ?p ?o }"), eq(true), eq(List.of("urn:rkg:witnesses"))))
                .thenReturn(QueryResult.select(List.of(), List.of()));

        var engine = new Rdf4jQueryAnsweringEngine(connector, repoStateStore, ENDPOINT);
        engine.query(REPO, "SELECT * WHERE { ?s ?p ?o }");

        verify(connector).query(REPO, "SELECT * WHERE { ?s ?p ?o }", true, List.of("urn:rkg:witnesses"));
    }
}
