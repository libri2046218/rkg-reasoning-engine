package org.rkg.integration.query;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.rkg.connector.QueryResult;
import org.rkg.integration.GraphDbITSupport;

@Tag("integration")
class QueryAnsweringIT extends GraphDbITSupport {

    @Test
    void answersRkgAwareQueriesAfterChase() throws IOException {
        String repository = createRepository("query");
        importFixture(repository, "/fixtures/it/definite-example.ttl");
        validator.validate(repository);
        chaseOrchestrator.runChase(repository);

        QueryResult result = queryAnsweringEngine.query(repository,
                "SELECT ?s WHERE { ?s a <http://example.org/ontology#Person> }");

        assertFalse(result.rows().isEmpty());
        assertTrue(result.rows().stream().anyMatch(row -> row.get("s").startsWith("urn:rkg:witness:")));
    }
}
