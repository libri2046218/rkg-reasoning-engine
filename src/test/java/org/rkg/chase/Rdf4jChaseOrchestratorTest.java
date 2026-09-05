package org.rkg.chase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.rkg.connector.GraphDBConnector;
import org.rkg.connector.QueryResult;
import org.rkg.repostate.RepoStateStore;
import org.rkg.validation.DefinitenessValidator;
import org.rkg.validation.ValidationReport;

/**
 * Unit tests for {@link Rdf4jChaseOrchestrator} against mocked collaborators, verifying: the
 * definiteness precondition (§5.3), idempotent witness generation (no duplicate INSERT when a
 * witness already exists), and the fixed 3-phase pipeline's reporting.
 */
@org.junit.jupiter.api.Tag("unit")
class Rdf4jChaseOrchestratorTest {

    private static final String REPO = "test-repo";
    private static final String ENDPOINT = "http://localhost:7200";

    @Test
    void abortsWithoutMutatingWhenGraphIsIndefinite() {
        GraphDBConnector connector = mock(GraphDBConnector.class);
        DefinitenessValidator validator = mock(DefinitenessValidator.class);
        RepoStateStore repoStateStore = mock(RepoStateStore.class);
        when(validator.validate(REPO)).thenReturn(ValidationReport.indefinite(List.of("http://ex.org#Bad"), List.of()));

        var orchestrator = new Rdf4jChaseOrchestrator(connector, validator, repoStateStore, ENDPOINT);

        assertThrows(IndefiniteGraphException.class, () -> orchestrator.runChase(REPO));
        verify(connector, never()).update(anyString(), anyString());
        verify(repoStateStore, never()).markChased(anyString(), anyString(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void mintsWitnessesOnlyForElementsWithoutOne() {
        GraphDBConnector connector = mock(GraphDBConnector.class);
        DefinitenessValidator validator = mock(DefinitenessValidator.class);
        RepoStateStore repoStateStore = mock(RepoStateStore.class);
        when(validator.validate(REPO)).thenReturn(ValidationReport.definite());

        String classA = "http://ex.org/ontology#Person";
        String classB = "http://ex.org/ontology#Animal";

        when(connector.query(eq(REPO), eq(ChaseQueries.defaultGraphTripleCount()), eq(true), eq(List.of())))
                .thenReturn(countResult(10)).thenReturn(countResult(12));
        when(connector.query(eq(REPO), eq(ChaseQueries.populatedClasses()), eq(true), eq(List.of())))
                .thenReturn(classRows(classA, classB));
        when(connector.query(eq(REPO), eq(ChaseQueries.populatedProperties()), eq(true), eq(List.of())))
                .thenReturn(QueryResult.select(List.of("p"), List.of()));

        String witnessA = SkolemNaming.classWitness(classA, false);
        String witnessB = SkolemNaming.classWitness(classB, false);
        when(connector.query(eq(REPO), eq(ChaseQueries.classWitnessExists(classA, witnessA)), eq(true), eq(List.of())))
                .thenReturn(QueryResult.ask(true)); // classA already has a witness
        when(connector.query(eq(REPO), eq(ChaseQueries.classWitnessExists(classB, witnessB)), eq(true), eq(List.of())))
                .thenReturn(QueryResult.ask(false)); // classB needs one minted

        var orchestrator = new Rdf4jChaseOrchestrator(connector, validator, repoStateStore, ENDPOINT);
        ChaseResult result = orchestrator.runChase(REPO);

        assertEquals(10, result.phase1ClosureTripleCount());
        assertEquals(1, result.phase2WitnessTripleCount());
        assertEquals(12, result.phase3ClosureTripleCount());
        verify(connector, times(1)).update(eq(REPO), contains(witnessB));
        verify(repoStateStore).markChased(eq(ENDPOINT), eq(REPO), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void skipsUpdateEntirelyWhenNoNewWitnessesNeeded() {
        GraphDBConnector connector = mock(GraphDBConnector.class);
        DefinitenessValidator validator = mock(DefinitenessValidator.class);
        RepoStateStore repoStateStore = mock(RepoStateStore.class);
        when(validator.validate(REPO)).thenReturn(ValidationReport.definite());

        when(connector.query(eq(REPO), eq(ChaseQueries.defaultGraphTripleCount()), eq(true), eq(List.of())))
                .thenReturn(countResult(5));
        when(connector.query(eq(REPO), eq(ChaseQueries.populatedClasses()), eq(true), eq(List.of())))
                .thenReturn(QueryResult.select(List.of("a"), List.of()));
        when(connector.query(eq(REPO), eq(ChaseQueries.populatedProperties()), eq(true), eq(List.of())))
                .thenReturn(QueryResult.select(List.of("p"), List.of()));

        var orchestrator = new Rdf4jChaseOrchestrator(connector, validator, repoStateStore, ENDPOINT);
        ChaseResult result = orchestrator.runChase(REPO);

        assertEquals(0, result.phase2WitnessTripleCount());
        verify(connector, never()).update(anyString(), anyString());
    }

    private static QueryResult countResult(int count) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("c", String.valueOf(count));
        return QueryResult.select(List.of("c"), List.of(row));
    }

    private static QueryResult classRows(String... classIris) {
        List<Map<String, String>> rows = new java.util.ArrayList<>();
        for (String iri : classIris) {
            Map<String, String> row = new LinkedHashMap<>();
            row.put("a", iri);
            row.put("isBlank", "false");
            rows.add(row);
        }
        return QueryResult.select(List.of("a", "isBlank"), rows);
    }
}
