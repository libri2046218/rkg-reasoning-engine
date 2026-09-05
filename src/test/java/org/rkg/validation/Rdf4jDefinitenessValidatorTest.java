package org.rkg.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.rkg.connector.GraphDBConnector;
import org.rkg.connector.QueryResult;
import org.rkg.repostate.RepoStateStore;

/**
 * Unit tests for {@link Rdf4jDefinitenessValidator} against a mocked {@link GraphDBConnector},
 * exercising each branch of Proposition 5's populated/bottom characterization (§5.2 of the
 * software design document) without any real GraphDB instance.
 */
@org.junit.jupiter.api.Tag("unit")
class Rdf4jDefinitenessValidatorTest {

    private static final String REPO = "test-repo";
    private static final String POPULATED_CLASS = "http://ex.org/ontology#Populated";
    private static final String BOTTOM_CLASS = "http://ex.org/ontology#Bottom";
    private static final String INDEFINITE_CLASS = "http://ex.org/ontology#Indefinite";
    private static final String POPULATED_PROP = "http://ex.org/ontology#populatedProp";
    private static final String SUBPROP_PROP = "http://ex.org/ontology#subPropOfAllProp";
    private static final String UNIVERSAL_DOMAIN_RANGE_PROP = "http://ex.org/ontology#universalProp";
    private static final String INDEFINITE_PROP = "http://ex.org/ontology#indefiniteProp";

    private QueryResult rowsOf(String var, List<String> values) {
        List<Map<String, String>> rows = new java.util.ArrayList<>();
        for (String v : values) {
            Map<String, String> row = new LinkedHashMap<>();
            row.put(var, v);
            rows.add(row);
        }
        return QueryResult.select(List.of(var), rows);
    }

    @Test
    void classifiesClassesPerPropositionFive() {
        GraphDBConnector connector = mock(GraphDBConnector.class);
        RepoStateStore repoStateStore = mock(RepoStateStore.class);

        when(connector.query(eq(REPO), eq(DefinitenessQueries.candidateClasses()), eq(true), eq(List.of())))
                .thenReturn(rowsOf("a", List.of(POPULATED_CLASS, BOTTOM_CLASS, INDEFINITE_CLASS)));
        when(connector.query(eq(REPO), eq(DefinitenessQueries.candidateProperties()), eq(true), eq(List.of())))
                .thenReturn(rowsOf("a", List.of()));

        when(connector.query(eq(REPO), eq(DefinitenessQueries.classPopulated(POPULATED_CLASS)), eq(true), eq(List.of())))
                .thenReturn(QueryResult.ask(true));
        when(connector.query(eq(REPO), eq(DefinitenessQueries.classPopulated(BOTTOM_CLASS)), eq(true), eq(List.of())))
                .thenReturn(QueryResult.ask(false));
        when(connector.query(eq(REPO), eq(DefinitenessQueries.classIsBottom(BOTTOM_CLASS)), eq(true), eq(List.of())))
                .thenReturn(QueryResult.ask(true));
        when(connector.query(eq(REPO), eq(DefinitenessQueries.classPopulated(INDEFINITE_CLASS)), eq(true), eq(List.of())))
                .thenReturn(QueryResult.ask(false));
        when(connector.query(eq(REPO), eq(DefinitenessQueries.classIsBottom(INDEFINITE_CLASS)), eq(true), eq(List.of())))
                .thenReturn(QueryResult.ask(false));

        ValidationReport report = new Rdf4jDefinitenessValidator(connector, repoStateStore, "http://localhost:7200")
                .validate(REPO);

        assertFalse(report.isDefinite());
        assertEquals(List.of(INDEFINITE_CLASS), report.indefiniteClasses());
    }

    @Test
    void classifiesPropertiesPerPropositionFive() {
        GraphDBConnector connector = mock(GraphDBConnector.class);
        RepoStateStore repoStateStore = mock(RepoStateStore.class);

        when(connector.query(eq(REPO), eq(DefinitenessQueries.candidateClasses()), eq(true), eq(List.of())))
                .thenReturn(rowsOf("a", List.of()));
        when(connector.query(eq(REPO), eq(DefinitenessQueries.candidateProperties()), eq(true), eq(List.of())))
                .thenReturn(rowsOf("a", List.of(POPULATED_PROP, SUBPROP_PROP, UNIVERSAL_DOMAIN_RANGE_PROP, INDEFINITE_PROP)));

        stubPropertyDefinite(connector, POPULATED_PROP, true, null, null, null);
        stubPropertyDefinite(connector, SUBPROP_PROP, false, true, null, null);
        stubPropertyDefinite(connector, UNIVERSAL_DOMAIN_RANGE_PROP, false, false, true, true);
        stubPropertyDefinite(connector, INDEFINITE_PROP, false, false, false, null);

        ValidationReport report = new Rdf4jDefinitenessValidator(connector, repoStateStore, "http://localhost:7200")
                .validate(REPO);

        assertFalse(report.isDefinite());
        assertEquals(List.of(INDEFINITE_PROP), report.indefiniteProperties());
        assertTrue(report.indefiniteClasses().isEmpty());
    }

    private void stubPropertyDefinite(GraphDBConnector connector, String prop, boolean populated,
                                       Boolean subPropOfAll, Boolean universalDomain, Boolean universalRange) {
        when(connector.query(eq(REPO), eq(DefinitenessQueries.propertyPopulated(prop)), eq(true), eq(List.of())))
                .thenReturn(QueryResult.ask(populated));
        if (!populated) {
            when(connector.query(eq(REPO), eq(DefinitenessQueries.propertyIsSubPropertyOfEvery(prop)), eq(true), eq(List.of())))
                    .thenReturn(QueryResult.ask(subPropOfAll));
            if (!Boolean.TRUE.equals(subPropOfAll)) {
                when(connector.query(eq(REPO), eq(DefinitenessQueries.propertyHasUniversalDomain(prop)), eq(true), eq(List.of())))
                        .thenReturn(QueryResult.ask(Boolean.TRUE.equals(universalDomain)));
                if (Boolean.TRUE.equals(universalDomain)) {
                    when(connector.query(eq(REPO), eq(DefinitenessQueries.propertyHasUniversalRange(prop)), eq(true), eq(List.of())))
                            .thenReturn(QueryResult.ask(Boolean.TRUE.equals(universalRange)));
                }
            }
        }
    }

    @Test
    void recordsValidationResultInRepoStateStore() {
        GraphDBConnector connector = mock(GraphDBConnector.class);
        RepoStateStore repoStateStore = mock(RepoStateStore.class);
        when(connector.query(eq(REPO), anyString(), eq(true), eq(List.of()))).thenReturn(rowsOf("a", List.of()));

        new Rdf4jDefinitenessValidator(connector, repoStateStore, "http://localhost:7200").validate(REPO);

        org.mockito.Mockito.verify(repoStateStore)
                .recordValidation(eq("http://localhost:7200"), eq(REPO), eq(true), eq(List.of()));
    }
}
