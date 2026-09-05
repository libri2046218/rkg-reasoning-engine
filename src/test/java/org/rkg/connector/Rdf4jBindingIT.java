package org.rkg.connector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.rkg.integration.GraphDbITSupport;

@Tag("integration")
class Rdf4jBindingIT extends GraphDbITSupport {

    @Test
    void retainsAndBindsBlankNodesWithoutSerializingTheirLabels() {
        String repository = createRepository("blank-binding");
        connector.importData(repository, new ByteArrayInputStream(("""
                @prefix ex: <urn:test:> .
                ex:source ex:term [] .
                """).getBytes(StandardCharsets.UTF_8)), RDFFormat.TURTLE, null);

        QueryResult selected = connector.query(repository,
                "SELECT ?term WHERE { <urn:test:source> <urn:test:term> ?term }", false, List.of());
        Value blankNode = selected.valueRows().get(0).get("term");
        assertTrue(blankNode instanceof BNode);

        String insert = """
                INSERT { GRAPH <urn:test:bound> {
                    <urn:test:witness> <urn:test:has-term> ?term
                } } WHERE { }
                """;
        connector.update(repository, insert, Map.of("term", blankNode));

        QueryResult inserted = connector.query(repository,
                "SELECT ?term WHERE { GRAPH <urn:test:bound> { <urn:test:witness> <urn:test:has-term> ?term } }",
                true, List.of("urn:test:bound"));
        assertEquals(blankNode, inserted.valueRows().get(0).get("term"));

        QueryResult boundAsk = connector.query(repository,
                "ASK { GRAPH <urn:test:bound> { <urn:test:witness> <urn:test:has-term> ?term } }",
                true, List.of("urn:test:bound"), Map.of("term", blankNode));
        assertTrue(boundAsk.askResult());
    }
}
