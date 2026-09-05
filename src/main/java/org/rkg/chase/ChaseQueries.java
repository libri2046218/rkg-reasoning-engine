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

    /** Every currently populated class {@code a}, with a marker for whether it is a blank node. */
    static String populatedClasses() {
        return """
                SELECT DISTINCT ?a (IF(isBlank(?a), "true", "false") AS ?isBlank) WHERE {
                    ?b <%s> ?a .
                    FILTER (!STRSTARTS(STR(?a), "%s"))
                }""".formatted(RDF_TYPE, WITNESS_NAMESPACE);
    }

    /** Every currently populated property {@code p}, with a marker for whether it is a blank node. */
    static String populatedProperties() {
        return """
                SELECT DISTINCT ?p (IF(isBlank(?p), "true", "false") AS ?isBlank) WHERE {
                    ?a ?p ?b .
                    FILTER (!STRSTARTS(STR(?p), "%s"))
                }""".formatted(WITNESS_NAMESPACE);
    }

    /** Whether a rule-22 witness already exists for class {@code a} in the witness graph. */
    static String classWitnessExists(String classIri, String witnessIri) {
        return "ASK { GRAPH <%s> { <%s> <%s> <%s> } }".formatted(WITNESS_GRAPH, witnessIri, RDF_TYPE, classIri);
    }

    /** Whether a rule-23 witness pair already exists for property {@code p} in the witness graph. */
    static String propertyWitnessExists(String propertyIri, String sourceWitnessIri, String targetWitnessIri) {
        return "ASK { GRAPH <%s> { <%s> <%s> <%s> } }"
                .formatted(WITNESS_GRAPH, sourceWitnessIri, propertyIri, targetWitnessIri);
    }

    /** Batched INSERT DATA installing every newly-minted witness triple in one round trip. */
    static String insertWitnessesUpdate(Iterable<String> witnessTriples) {
        StringBuilder body = new StringBuilder();
        for (String triple : witnessTriples) {
            body.append("        ").append(triple).append(" .\n");
        }
        return "INSERT DATA { GRAPH <%s> {\n%s    } }".formatted(WITNESS_GRAPH, body);
    }

    static String classWitnessTriple(String witnessIri, String classIri) {
        return "<%s> <%s> <%s>".formatted(witnessIri, RDF_TYPE, classIri);
    }

    static String propertyWitnessTriple(String sourceWitnessIri, String propertyIri, String targetWitnessIri) {
        return "<%s> <%s> <%s>".formatted(sourceWitnessIri, propertyIri, targetWitnessIri);
    }
}
