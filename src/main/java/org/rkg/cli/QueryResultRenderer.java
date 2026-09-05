package org.rkg.cli;

import java.util.List;
import java.util.Map;
import org.rkg.connector.QueryResult;

/** Formats query results for standard output as terminal text, JSON, or CSV. */
final class QueryResultRenderer {

    private QueryResultRenderer() {
    }

    static String render(QueryResult result, boolean json, boolean csv) {
        if (json) {
            return renderJson(result);
        }
        if (csv) {
            return renderCsv(result);
        }
        return renderPlain(result);
    }

    private static String renderPlain(QueryResult result) {
        return switch (result.kind()) {
            case ASK -> result.askResult() + System.lineSeparator();
            case SELECT -> {
                StringBuilder output = new StringBuilder(String.join("\t", result.variableNames()))
                        .append(System.lineSeparator());
                for (Map<String, String> row : result.rows()) {
                    output.append(result.variableNames().stream()
                            .map(variable -> String.valueOf(row.get(variable)))
                            .reduce((left, right) -> left + "\t" + right)
                            .orElse(""))
                            .append(System.lineSeparator());
                }
                yield output.toString();
            }
            case GRAPH -> {
                StringBuilder output = new StringBuilder();
                result.statements().forEach(statement -> output.append(statement).append(System.lineSeparator()));
                yield output.toString();
            }
        };
    }

    private static String renderJson(QueryResult result) {
        return switch (result.kind()) {
            case ASK -> "{\"type\":\"ask\",\"boolean\":" + result.askResult() + "}" + System.lineSeparator();
            case SELECT -> {
                StringBuilder output = new StringBuilder("{\"type\":\"select\",\"variables\":");
                appendJsonStrings(output, result.variableNames());
                output.append(",\"rows\":[");
                for (int rowIndex = 0; rowIndex < result.rows().size(); rowIndex++) {
                    if (rowIndex > 0) {
                        output.append(',');
                    }
                    output.append('{');
                    Map<String, String> row = result.rows().get(rowIndex);
                    for (int variableIndex = 0; variableIndex < result.variableNames().size(); variableIndex++) {
                        if (variableIndex > 0) {
                            output.append(',');
                        }
                        String variable = result.variableNames().get(variableIndex);
                        appendJsonString(output, variable);
                        output.append(':');
                        String value = row.get(variable);
                        if (value == null) {
                            output.append("null");
                        } else {
                            appendJsonString(output, value);
                        }
                    }
                    output.append('}');
                }
                yield output.append("]}").append(System.lineSeparator()).toString();
            }
            case GRAPH -> {
                StringBuilder output = new StringBuilder("{\"type\":\"graph\",\"statements\":");
                appendJsonStrings(output, result.statements().stream().map(Object::toString).toList());
                yield output.append('}').append(System.lineSeparator()).toString();
            }
        };
    }

    private static String renderCsv(QueryResult result) {
        return switch (result.kind()) {
            case ASK -> "boolean" + System.lineSeparator() + result.askResult() + System.lineSeparator();
            case SELECT -> {
                StringBuilder output = new StringBuilder();
                appendCsvRow(output, result.variableNames());
                for (Map<String, String> row : result.rows()) {
                    appendCsvRow(output, result.variableNames().stream().map(row::get).toList());
                }
                yield output.toString();
            }
            case GRAPH -> {
                StringBuilder output = new StringBuilder();
                appendCsvRow(output, List.of("statement"));
                for (Object statement : result.statements()) {
                    appendCsvRow(output, List.of(statement.toString()));
                }
                yield output.toString();
            }
        };
    }

    private static void appendJsonStrings(StringBuilder output, List<String> values) {
        output.append('[');
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                output.append(',');
            }
            appendJsonString(output, values.get(index));
        }
        output.append(']');
    }

    private static void appendJsonString(StringBuilder output, String value) {
        output.append('"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> output.append("\\\"");
                case '\\' -> output.append("\\\\");
                case '\b' -> output.append("\\b");
                case '\f' -> output.append("\\f");
                case '\n' -> output.append("\\n");
                case '\r' -> output.append("\\r");
                case '\t' -> output.append("\\t");
                default -> {
                    if (character < 0x20) {
                        output.append("\\u%04x".formatted((int) character));
                    } else {
                        output.append(character);
                    }
                }
            }
        }
        output.append('"');
    }

    private static void appendCsvRow(StringBuilder output, List<String> values) {
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                output.append(',');
            }
            String value = values.get(index);
            if (value != null) {
                boolean quoted = value.indexOf(',') >= 0 || value.indexOf('"') >= 0
                        || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0;
                if (quoted) {
                    output.append('"');
                }
                output.append(value.replace("\"", "\"\""));
                if (quoted) {
                    output.append('"');
                }
            }
        }
        output.append(System.lineSeparator());
    }
}
