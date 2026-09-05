package org.rkg.chase;

/**
 * Builds the SPARQL queries/updates used by the chase pipeline's three phases (§5.3 of the
 * software design document). Kept as pure string-building logic, independent of
 * {@link org.rkg.connector.GraphDBConnector}, so it is directly unit-testable.
 */
final class ChaseQueries {

    static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    static final String WITNESS_GRAPH = "urn:rkg:witnesses";
    static final String WITNESS_NAMESPACE = "urn:rkg:witness:";

    private ChaseQueries() {
    }

    /** Total triple count in the default graph (Phase 1 baseline / Phase 3 result). */
    static String defaultGraphTripleCount() {
        return "SELECT (COUNT(*) AS ?c) WHERE { ?s ?p ?o }";
    }

    /** Every currently populated class {@code a}. */
    static String populatedClasses() {
        return """
                SELECT DISTINCT ?a WHERE {
                    ?b <%s> ?a .
                    FILTER (!STRSTARTS(STR(?a), "%s"))
                }""".formatted(RDF_TYPE, WITNESS_NAMESPACE);
    }

    /** Every currently populated property {@code p}. */
    static String populatedProperties() {
        return """
                SELECT DISTINCT ?p WHERE {
                    ?a ?p ?b .
                    FILTER (!STRSTARTS(STR(?p), "%s"))
                }""".formatted(WITNESS_NAMESPACE);
    }

    /**
     * Whether a rule-22 witness exists in the witness graph. Callers bind {@code witness} and
     * {@code classTerm} as RDF4J values; this is essential when {@code classTerm} is a blank node.
     */
    static String classWitnessExists() {
        return "ASK { GRAPH <%s> { ?witness <%s> ?classTerm } }".formatted(WITNESS_GRAPH, RDF_TYPE);
    }

    /**
     * Whether a rule-23 witness pair exists in the witness graph. Callers bind {@code source},
     * {@code propertyTerm}, and {@code target} as RDF4J values.
     */
    static String propertyWitnessExists() {
        return "ASK { GRAPH <%s> { ?source ?propertyTerm ?target } }".formatted(WITNESS_GRAPH);
    }

    /**
     * Batched INSERT installing witness triples in one round trip. The {@code subjectN},
     * {@code predicateN}, and {@code objectN} variables must be RDF4J-bound by the caller.
     */
    static String insertWitnessesUpdate(int tripleCount) {
        if (tripleCount < 1) {
            throw new IllegalArgumentException("At least one witness triple is required");
        }
        StringBuilder body = new StringBuilder();
        for (int index = 0; index < tripleCount; index++) {
            body.append("        ?subject").append(index)
                    .append(" ?predicate").append(index)
                    .append(" ?object").append(index).append(" .\n");
        }
        return "INSERT { GRAPH <%s> {\n%s    } } WHERE { }".formatted(WITNESS_GRAPH, body);
    }
}
