package org.rkg.connector;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

/**
 * Normalized result of a SPARQL query executed through {@link GraphDBConnector#query}.
 * Exactly one of {@link #rows()}, {@link #askResult()}, or {@link #statements()} is populated,
 * depending on the SPARQL query form (SELECT, ASK, or CONSTRUCT/DESCRIBE respectively).
 */
public final class QueryResult {

    /** SPARQL query result kind: SELECT (tuple), ASK (boolean), or GRAPH (RDF statements). */
    public enum Kind { 
        /** SELECT query result (tuple). */
        SELECT,
        /** ASK query result (boolean). */
        ASK,
        /** GRAPH/CONSTRUCT/DESCRIBE result (RDF statements). */
        GRAPH
    }

    private final Kind kind;
    private final List<String> variableNames;
    private final List<Map<String, String>> rows;
    private final List<Map<String, Value>> valueRows;
    private final Boolean askResult;
    private final List<Statement> statements;

    private QueryResult(Kind kind, List<String> variableNames, List<Map<String, String>> rows,
                         List<Map<String, Value>> valueRows, Boolean askResult, List<Statement> statements) {
        this.kind = kind;
        this.variableNames = variableNames;
        this.rows = rows;
        this.valueRows = valueRows;
        this.askResult = askResult;
        this.statements = statements;
    }

    /**
     * Creates a SELECT result.
     *
     * @param variableNames query variables
     * @param rows result rows (each row: variable name -> binding value)
     * @return SELECT QueryResult
     */
    public static QueryResult select(List<String> variableNames, List<Map<String, String>> rows) {
        return new QueryResult(Kind.SELECT, variableNames, rows, Collections.emptyList(), null,
                Collections.emptyList());
    }

    /**
     * Creates a SELECT result while retaining the RDF4J values returned by the repository.
     * The string {@link #rows()} view remains available for existing callers.
     *
     * @param variableNames query variables
     * @param valueRows result rows (each row: variable name -> RDF4J binding value)
     * @return SELECT QueryResult
     */
    public static QueryResult selectValues(List<String> variableNames, List<Map<String, Value>> valueRows) {
        List<Map<String, String>> rows = new java.util.ArrayList<>(valueRows.size());
        for (Map<String, Value> valueRow : valueRows) {
            Map<String, String> row = new LinkedHashMap<>();
            for (String variableName : variableNames) {
                Value value = valueRow.get(variableName);
                row.put(variableName, value == null ? null : value.stringValue());
            }
            rows.add(row);
        }
        return new QueryResult(Kind.SELECT, variableNames, rows, valueRows, null, Collections.emptyList());
    }

    /**
     * Creates an ASK result.
     *
     * @param result boolean result
     * @return ASK QueryResult
     */
    public static QueryResult ask(boolean result) {
        return new QueryResult(Kind.ASK, Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), result, Collections.emptyList());
    }

    /**
     * Creates a GRAPH result.
     *
     * @param statements RDF statements
     * @return GRAPH QueryResult
     */
    public static QueryResult graph(List<Statement> statements) {
        return new QueryResult(Kind.GRAPH, Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), null, statements);
    }

    /**
     * Returns the result kind (SELECT, ASK, or GRAPH).
     *
     * @return result kind
     */
    public Kind kind() {
        return kind;
    }

    /**
     * Returns the query variables (SELECT queries only; empty for ASK/GRAPH).
     *
     * @return variable names
     */
    public List<String> variableNames() {
        return variableNames;
    }

    /**
     * Returns the result rows (SELECT queries only; empty for ASK/GRAPH).
     *
     * @return result rows
     */
    public List<Map<String, String>> rows() {
        return rows;
    }

    /**
     * Returns SELECT rows with their original RDF4J binding values. This is empty for results
     * made with the legacy {@link #select(List, List)} factory, which has only string values.
     *
     * @return RDF4J binding rows, or an empty list when bindings were not retained
     */
    public List<Map<String, Value>> valueRows() {
        return valueRows;
    }

    /**
     * Returns the boolean result (ASK queries only; null for SELECT/GRAPH).
     *
     * @return boolean result
     */
    public Boolean askResult() {
        return askResult;
    }

    /**
     * Returns the RDF statements (GRAPH/CONSTRUCT/DESCRIBE queries only; empty for SELECT/ASK).
     *
     * @return RDF statements
     */
    public List<Statement> statements() {
        return statements;
    }

    /**
     * Returns the size of the result, with semantics depending on query kind:
     * <ul>
     *   <li>{@code SELECT}: returns number of result rows</li>
     *   <li>{@code ASK}: always returns 1 (result is a single boolean)</li>
     *   <li>{@code GRAPH}: returns number of RDF statements</li>
     * </ul>
     *
     * @return result size (rows, 1, or statements count)
     */
    public int size() {
        return switch (kind) {
            case SELECT -> rows.size();
            case ASK -> 1;
            case GRAPH -> statements.size();
        };
    }
}
