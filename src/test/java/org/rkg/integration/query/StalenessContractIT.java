package org.rkg.integration.query;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.rkg.integration.GraphDbITSupport;
import org.rkg.query.StaleRepositoryException;

@Tag("integration")
class StalenessContractIT extends GraphDbITSupport {

    @Test
    void refusesDefaultQueriesUntilTheRepositoryIsChasedAgain() throws IOException {
        String repository = createRepository("staleness");
        importFixture(repository, "/fixtures/it/definite-example.ttl");
        validator.validate(repository);

        String query = "SELECT ?s WHERE { ?s a <http://example.org/ontology#Person> }";
        assertThrows(StaleRepositoryException.class, () -> queryAnsweringEngine.query(repository, query));

        chaseOrchestrator.runChase(repository);
        assertDoesNotThrow(() -> queryAnsweringEngine.query(repository, query));

        connector.update(repository,
                "INSERT DATA { <http://example.org/ontology#bob> a <http://example.org/ontology#Student> }");
        assertThrows(StaleRepositoryException.class, () -> queryAnsweringEngine.query(repository, query));
    }
}
