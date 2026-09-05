package org.rkg.integration.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.rkg.chase.ChaseResult;
import org.rkg.chase.IndefiniteGraphException;
import org.rkg.connector.QueryResult;
import org.rkg.integration.GraphDbITSupport;

@Tag("integration")
class ChaseSemanticsIT extends GraphDbITSupport {

    @Test
    void mintsIdempotentWitnessesOnlyInTheWitnessGraph() throws IOException {
        String repository = createRepository("chase");
        importFixture(repository, "/fixtures/it/definite-example.ttl");

        assertTrue(validator.validate(repository).isDefinite());

        ChaseResult firstRun = chaseOrchestrator.runChase(repository);
        assertTrue(firstRun.phase2WitnessTripleCount() > 0);
        assertEquals(0, chaseOrchestrator.runChase(repository).phase2WitnessTripleCount());

        QueryResult rawTypes = connector.query(repository,
                "PREFIX sesame: <http://www.openrdf.org/schema/sesame#>\n"
                        + "SELECT ?s WHERE { GRAPH sesame:nil { ?s a <http://example.org/ontology#Person> } }",
                false, List.of());
        assertFalse(rawTypes.rows().stream().anyMatch(row -> row.get("s").startsWith("urn:rkg:witness:")));

        QueryResult rkgTypes = connector.query(repository,
                "SELECT ?g ?s WHERE { GRAPH ?g { ?s a <http://example.org/ontology#Person> } }",
                true, List.of("urn:rkg:witnesses"));
        assertTrue(rkgTypes.rows().stream().anyMatch(row -> "urn:rkg:witnesses".equals(row.get("g"))));
    }

    @Test
    void refusesChaseForIndefiniteGraphs() throws IOException {
        String repository = createRepository("indefinite");
        importFixture(repository, "/fixtures/it/indefinite-example.ttl");

        assertThrows(IndefiniteGraphException.class, () -> chaseOrchestrator.runChase(repository));
        assertFalse(repoStateStore.get(endpointUrl, repository).orElseThrow().chased());
    }
}
