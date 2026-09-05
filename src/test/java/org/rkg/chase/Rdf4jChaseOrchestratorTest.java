package org.rkg.chase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.rkg.connector.GraphDBConnector;
import org.rkg.connector.QueryResult;
import org.rkg.repostate.RepoStateStore;
import org.rkg.repostate.RepoState;
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

        String witnessB = SkolemNaming.classWitness(classB, false);
        when(connector.query(eq(REPO), eq(ChaseQueries.classWitnessExists()), eq(true), eq(List.of()), anyMap()))
                .thenAnswer(invocation -> QueryResult.ask(classA.equals(
                        invocation.<Map<String, Value>>getArgument(4).get("classTerm").stringValue())));

        var orchestrator = new Rdf4jChaseOrchestrator(connector, validator, repoStateStore, ENDPOINT);
        ChaseResult result = orchestrator.runChase(REPO);

        assertEquals(10, result.phase1ClosureTripleCount());
        assertEquals(1, result.phase2WitnessTripleCount());
        assertEquals(12, result.phase3ClosureTripleCount());
        ArgumentCaptor<Map<String, Value>> bindings = valueMapCaptor();
        verify(connector, times(1)).update(eq(REPO), eq(ChaseQueries.insertWitnessesUpdate(1)), bindings.capture());
        assertEquals(witnessB, bindings.getValue().get("subject0").stringValue());
        verify(repoStateStore).markChased(eq(ENDPOINT), eq(REPO), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void preservesBlankNodeBindingsForWitnessChecksAndInsertion() {
        GraphDBConnector connector = mock(GraphDBConnector.class);
        DefinitenessValidator validator = mock(DefinitenessValidator.class);
        RepoStateStore repoStateStore = mock(RepoStateStore.class);
        BNode classTerm = SimpleValueFactory.getInstance().createBNode("class-term");
        BNode propertyTerm = SimpleValueFactory.getInstance().createBNode("property-term");

        when(validator.validate(REPO)).thenReturn(ValidationReport.definite());
        when(repoStateStore.get(ENDPOINT, REPO)).thenReturn(java.util.Optional.of(
                new RepoState(ENDPOINT, REPO, true, java.time.Instant.now(), true, List.of())));
        when(connector.query(eq(REPO), eq(ChaseQueries.defaultGraphTripleCount()), eq(true), eq(List.of())))
                .thenReturn(countResult(3));
        when(connector.query(eq(REPO), eq(ChaseQueries.populatedClasses()), eq(true), eq(List.of())))
                .thenReturn(valueRows("a", classTerm));
        when(connector.query(eq(REPO), eq(ChaseQueries.populatedProperties()), eq(true), eq(List.of())))
                .thenReturn(valueRows("p", propertyTerm));
        when(connector.query(eq(REPO), eq(ChaseQueries.classWitnessExists()), eq(true), eq(List.of()), anyMap()))
                .thenReturn(QueryResult.ask(false));
        when(connector.query(eq(REPO), eq(ChaseQueries.propertyWitnessExists()), eq(true), eq(List.of()), anyMap()))
                .thenReturn(QueryResult.ask(false));

        ChaseResult result = new Rdf4jChaseOrchestrator(connector, validator, repoStateStore, ENDPOINT).runChase(REPO);

        assertEquals(2, result.phase2WitnessTripleCount());
        ArgumentCaptor<Map<String, Value>> classBindings = valueMapCaptor();
        verify(connector).query(eq(REPO), eq(ChaseQueries.classWitnessExists()), eq(true), eq(List.of()),
                classBindings.capture());
        assertTrue(classTerm == classBindings.getValue().get("classTerm"));

        ArgumentCaptor<Map<String, Value>> propertyBindings = valueMapCaptor();
        verify(connector).query(eq(REPO), eq(ChaseQueries.propertyWitnessExists()), eq(true), eq(List.of()),
                propertyBindings.capture());
        assertTrue(propertyTerm == propertyBindings.getValue().get("propertyTerm"));

        ArgumentCaptor<Map<String, Value>> updateBindings = valueMapCaptor();
        verify(connector).update(eq(REPO), eq(ChaseQueries.insertWitnessesUpdate(2)), updateBindings.capture());
        assertTrue(classTerm == updateBindings.getValue().get("object0"));
        assertTrue(propertyTerm == updateBindings.getValue().get("predicate1"));
    }

    @Test
    void skipsUpdateEntirelyWhenNoNewWitnessesNeeded() {
        GraphDBConnector connector = mock(GraphDBConnector.class);
        DefinitenessValidator validator = mock(DefinitenessValidator.class);
        RepoStateStore repoStateStore = mock(RepoStateStore.class);
        when(validator.validate(REPO)).thenReturn(ValidationReport.definite());
        when(repoStateStore.get(ENDPOINT, REPO)).thenReturn(java.util.Optional.of(
                new RepoState(ENDPOINT, REPO, true, java.time.Instant.now(), true, List.of())));

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

    @Test
    void clearsStaleWitnessesBeforeRegeneratingThem() {
        GraphDBConnector connector = mock(GraphDBConnector.class);
        DefinitenessValidator validator = mock(DefinitenessValidator.class);
        RepoStateStore repoStateStore = mock(RepoStateStore.class);
        when(validator.validate(REPO)).thenReturn(ValidationReport.definite());
        when(repoStateStore.get(ENDPOINT, REPO)).thenReturn(java.util.Optional.of(
                new RepoState(ENDPOINT, REPO, false, null, true, List.of())));
        when(connector.query(eq(REPO), eq(ChaseQueries.defaultGraphTripleCount()), eq(true), eq(List.of())))
                .thenReturn(countResult(1));
        when(connector.query(eq(REPO), eq(ChaseQueries.populatedClasses()), eq(true), eq(List.of())))
                .thenReturn(QueryResult.select(List.of("a"), List.of()));
        when(connector.query(eq(REPO), eq(ChaseQueries.populatedProperties()), eq(true), eq(List.of())))
                .thenReturn(QueryResult.select(List.of("p"), List.of()));

        new Rdf4jChaseOrchestrator(connector, validator, repoStateStore, ENDPOINT).runChase(REPO);

        verify(connector).update(REPO, "CLEAR GRAPH <urn:rkg:witnesses>");
    }

    private static QueryResult countResult(int count) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("c", String.valueOf(count));
        return QueryResult.select(List.of("c"), List.of(row));
    }

    private static QueryResult classRows(String... classIris) {
        List<Map<String, Value>> rows = new java.util.ArrayList<>();
        for (String iri : classIris) {
            Map<String, Value> row = new LinkedHashMap<>();
            row.put("a", SimpleValueFactory.getInstance().createIRI(iri));
            rows.add(row);
        }
        return QueryResult.selectValues(List.of("a"), rows);
    }

    private static QueryResult valueRows(String variable, Value value) {
        return QueryResult.selectValues(List.of(variable), List.of(Map.of(variable, value)));
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<Map<String, Value>> valueMapCaptor() {
        return (ArgumentCaptor<Map<String, Value>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(Map.class);
    }
}
