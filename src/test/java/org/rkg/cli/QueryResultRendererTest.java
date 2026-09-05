package org.rkg.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.rkg.connector.QueryResult;

@Tag("unit")
class QueryResultRendererTest {

    @Test
    void preservesPlainSelectOutput() {
        QueryResult result = QueryResult.select(List.of("name", "age"),
                List.of(Map.of("name", "Ada", "age", "37")));

        assertEquals("name\tage%nAda\t37%n".formatted(),
                QueryResultRenderer.render(result, false, false));
    }

    @Test
    void rendersSelectResultAsEscapedJson() {
        QueryResult result = QueryResult.select(List.of("value"),
                List.of(Map.of("value", "one \"quoted\"\nline")));

        assertEquals("{\"type\":\"select\",\"variables\":[\"value\"],\"rows\":["
                        + "{\"value\":\"one \\\"quoted\\\"\\nline\"}]}%n".formatted(),
                QueryResultRenderer.render(result, true, false));
    }

    @Test
    void rendersSelectResultAsCsvWithEscaping() {
        QueryResult result = QueryResult.select(List.of("value", "missing"),
                List.of(Map.of("value", "one,\"quoted\"")));

        assertEquals("value,missing%n\"one,\"\"quoted\"\"\",%n".formatted(),
                QueryResultRenderer.render(result, false, true));
    }

    @Test
    void rendersAskAndGraphResultsInMachineReadableFormats() {
        QueryResult ask = QueryResult.ask(true);
        var values = SimpleValueFactory.getInstance();
        QueryResult graph = QueryResult.graph(List.of(values.createStatement(
                values.createIRI("urn:subject"), values.createIRI("urn:predicate"),
                values.createLiteral("object"))));

        assertEquals("{\"type\":\"ask\",\"boolean\":true}%n".formatted(),
                QueryResultRenderer.render(ask, true, false));
        String statement = graph.statements().getFirst().toString();
        assertEquals("statement%n\"%s\"%n".formatted(statement.replace("\"", "\"\"")),
                QueryResultRenderer.render(graph, false, true));
    }
}
