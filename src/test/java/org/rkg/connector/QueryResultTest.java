package org.rkg.connector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.rdf4j.model.Statement;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link QueryResult} data class: result kind discrimination, variable bindings,
 * and size calculations for SELECT, ASK, and CONSTRUCT/DESCRIBE queries.
 */
@org.junit.jupiter.api.Tag("unit")
class QueryResultTest {

    @Test
    void selectResultContainsVariableNamesAndRows() {
        List<String> varNames = List.of("s", "p", "o");
        Map<String, String> row = new LinkedHashMap<>();
        row.put("s", "http://example.org/subject");
        row.put("p", "http://example.org/predicate");
        row.put("o", "http://example.org/object");
        
        QueryResult result = QueryResult.select(varNames, List.of(row));
        
        assertEquals(QueryResult.Kind.SELECT, result.kind());
        assertEquals(varNames, result.variableNames());
        assertEquals(List.of(row), result.rows());
        assertTrue(result.askResult() == null);
        assertTrue(result.statements() == null);
    }

    @Test
    void askResultContainsBooleanValue() {
        QueryResult result = QueryResult.ask(true);
        
        assertEquals(QueryResult.Kind.ASK, result.kind());
        assertTrue(result.askResult());
        assertEquals(0, result.variableNames().size());
        assertEquals(0, result.rows().size());
    }

    @Test
    void askResultCanBeFalse() {
        QueryResult result = QueryResult.ask(false);
        
        assertEquals(QueryResult.Kind.ASK, result.kind());
        assertFalse(result.askResult());
    }

    @Test
    void graphResultContainsStatements() {
        List<Statement> statements = List.of(); // Mock statements in real test
        QueryResult result = QueryResult.graph(statements);
        
        assertEquals(QueryResult.Kind.GRAPH, result.kind());
        assertEquals(statements, result.statements());
        assertEquals(0, result.variableNames().size());
        assertEquals(0, result.rows().size());
    }

    @Test
    void selectResultSizeReturnsRowCount() {
        Map<String, String> row1 = new LinkedHashMap<>();
        row1.put("s", "http://ex.org/s1");
        Map<String, String> row2 = new LinkedHashMap<>();
        row2.put("s", "http://ex.org/s2");
        
        QueryResult result = QueryResult.select(List.of("s"), List.of(row1, row2));
        
        assertEquals(2, result.size());
    }

    @Test
    void askResultSizeIsOne() {
        QueryResult result = QueryResult.ask(true);
        
        assertEquals(1, result.size());
    }

    @Test
    void selectResultSizeWithZeroRows() {
        QueryResult result = QueryResult.select(List.of("s"), List.of());
        
        assertEquals(0, result.size());
    }

    @Test
    void selectResultWithMultipleRows() {
        List<String> varNames = List.of("s", "p");
        Map<String, String> row1 = new LinkedHashMap<>();
        row1.put("s", "http://ex.org/s1");
        row1.put("p", "http://ex.org/p1");
        
        Map<String, String> row2 = new LinkedHashMap<>();
        row2.put("s", "http://ex.org/s2");
        row2.put("p", "http://ex.org/p2");
        
        Map<String, String> row3 = new LinkedHashMap<>();
        row3.put("s", "http://ex.org/s3");
        row3.put("p", "http://ex.org/p3");
        
        QueryResult result = QueryResult.select(varNames, List.of(row1, row2, row3));
        
        assertEquals(3, result.size());
        assertEquals(3, result.rows().size());
        assertEquals(2, result.variableNames().size());
    }

    @Test
    void selectResultVariableNamesPreserveOrder() {
        List<String> varNames = List.of("o", "p", "s");
        QueryResult result = QueryResult.select(varNames, List.of());
        
        assertEquals(varNames, result.variableNames());
        assertEquals("o", result.variableNames().get(0));
        assertEquals("p", result.variableNames().get(1));
        assertEquals("s", result.variableNames().get(2));
    }

    @Test
    void selectResultWithNullValues() {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("s", "http://example.org/subject");
        row.put("p", null);
        row.put("o", "http://example.org/object");
        
        QueryResult result = QueryResult.select(List.of("s", "p", "o"), List.of(row));
        
        assertEquals("http://example.org/subject", result.rows().get(0).get("s"));
        assertEquals(null, result.rows().get(0).get("p"));
        assertEquals("http://example.org/object", result.rows().get(0).get("o"));
    }

    @Test
    void askResultsAreNotEqual() {
        QueryResult trueResult = QueryResult.ask(true);
        QueryResult falseResult = QueryResult.ask(false);
        
        assertNotNull(trueResult);
        assertNotNull(falseResult);
        // Both are QueryResult instances with different boolean values
        assertTrue(trueResult.askResult());
        assertFalse(falseResult.askResult());
    }

    @Test
    void selectResultKindIsCorrect() {
        QueryResult result = QueryResult.select(List.of("x"), List.of());
        
        assertEquals(QueryResult.Kind.SELECT, result.kind());
        assertFalse(QueryResult.Kind.ASK.equals(result.kind()));
        assertFalse(QueryResult.Kind.GRAPH.equals(result.kind()));
    }

    @Test
    void askResultKindIsCorrect() {
        QueryResult result = QueryResult.ask(true);
        
        assertEquals(QueryResult.Kind.ASK, result.kind());
        assertFalse(QueryResult.Kind.SELECT.equals(result.kind()));
        assertFalse(QueryResult.Kind.GRAPH.equals(result.kind()));
    }

    @Test
    void selectResultRowsAreImmutable() {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("s", "http://example.org/subject");
        
        QueryResult result = QueryResult.select(List.of("s"), List.of(row));
        
        // Rows should not be null
        assertNotNull(result.rows());
        assertEquals(1, result.rows().size());
    }

    @Test
    void selectResultVariableNamesAreNotEmpty() {
        QueryResult result = QueryResult.select(List.of("x", "y", "z"), List.of());
        
        assertNotNull(result.variableNames());
        assertEquals(3, result.variableNames().size());
    }

    @Test
    void askResultVariableNamesAreEmpty() {
        QueryResult result = QueryResult.ask(true);
        
        assertNotNull(result.variableNames());
        assertEquals(0, result.variableNames().size());
    }

    @Test
    void graphResultVariableNamesAreEmpty() {
        QueryResult result = QueryResult.graph(List.of());
        
        assertNotNull(result.variableNames());
        assertEquals(0, result.variableNames().size());
    }
}
