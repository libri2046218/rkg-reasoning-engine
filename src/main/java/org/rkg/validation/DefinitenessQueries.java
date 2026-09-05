package org.rkg.validation;

/**
 * Builds the SPARQL ASK/SELECT queries implementing Proposition 5 (Delfino, Lenzerini & Poggi,
 * ECAI 2025): the polynomial-time populated/bottom characterization of definiteness. Kept as
 * pure string-building logic, independent of {@link org.rkg.connector.GraphDBConnector}, so it is
 * directly unit-testable (§2 of testing-strategy.md).
 *
 * <p>Proposition 5 states an element {@code a} is <b>indefinite</b> in G if at least one of:
 * <ul>
 *   <li>{@code a} is a class, not populated (no {@code b} with {@code b type a}), and not
 *       "bottom" (there exists a class {@code c} such that {@code a subClassOf c} does not hold);</li>
 *   <li>{@code a} is a property, not populated (no {@code b,c} with {@code b a c}), not a
 *       sub-property of every property, and either its domain or its range is not universal
 *       (not every class {@code d} satisfies {@code a domain d} / {@code a range d}).</li>
 * </ul>
 * Equivalently, {@code a} is <b>definite</b> iff it is populated, or it is "bottom" (for classes:
 * a sub-class of every class; for properties: a sub-property of every property, or having both a
 * universal domain and a universal range).
 */
final class DefinitenessQueries {

    static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    static final String RDF_PROPERTY = "http://www.w3.org/1999/02/22-rdf-syntax-ns#Property";
    static final String RDFS_CLASS = "http://www.w3.org/2000/01/rdf-schema#Class";
    static final String RDFS_SUBCLASS_OF = "http://www.w3.org/2000/01/rdf-schema#subClassOf";
    static final String RDFS_SUBPROPERTY_OF = "http://www.w3.org/2000/01/rdf-schema#subPropertyOf";
    static final String RDFS_DOMAIN = "http://www.w3.org/2000/01/rdf-schema#domain";
    static final String RDFS_RANGE = "http://www.w3.org/2000/01/rdf-schema#range";
    static final String WITNESS_NAMESPACE = "urn:rkg:witness:";

    private DefinitenessQueries() {
    }

    /** Every element {@code a} such that {@code ⟨a type Class⟩ ∈ Ch(G)}. */
    static String candidateClasses() {
        return """
                SELECT DISTINCT ?a WHERE {
                    ?a <%s> <%s> .
                    FILTER (!STRSTARTS(STR(?a), "%s"))
                }""".formatted(RDF_TYPE, RDFS_CLASS, WITNESS_NAMESPACE);
    }

    /** Every element {@code a} such that {@code ⟨a type Property⟩ ∈ Ch(G)}. */
    static String candidateProperties() {
        return """
                SELECT DISTINCT ?a WHERE {
                    ?a <%s> <%s> .
                    FILTER (!STRSTARTS(STR(?a), "%s"))
                }""".formatted(RDF_TYPE, RDF_PROPERTY, WITNESS_NAMESPACE);
    }

    /** {@code ∃b: ⟨b type a⟩ ∈ Ch(G)}. */
    static String classPopulated(String classIri) {
        return "ASK { ?b <%s> <%s> }".formatted(RDF_TYPE, classIri);
    }

    /** {@code ∀c: ⟨c type Class⟩ ∈ Ch(G) ⟹ ⟨a subClassOf c⟩ ∈ Ch(G)} — a is a "bottom" class. */
    static String classIsBottom(String classIri) {
        return """
                ASK {
                    FILTER NOT EXISTS {
                        ?c <%s> <%s> .
                        FILTER NOT EXISTS { <%s> <%s> ?c }
                    }
                }""".formatted(RDF_TYPE, RDFS_CLASS, classIri, RDFS_SUBCLASS_OF);
    }

    /** {@code ∃b,c: ⟨b a c⟩ ∈ Ch(G)}. */
    static String propertyPopulated(String propertyIri) {
        return "ASK { ?b <%s> ?c }".formatted(propertyIri);
    }

    /** {@code ∀p: ⟨p type Property⟩ ∈ Ch(G) ⟹ ⟨a subPropertyOf p⟩ ∈ Ch(G)}. */
    static String propertyIsSubPropertyOfEvery(String propertyIri) {
        return """
                ASK {
                    FILTER NOT EXISTS {
                        ?p <%s> <%s> .
                        FILTER NOT EXISTS { <%s> <%s> ?p }
                    }
                }""".formatted(RDF_TYPE, RDF_PROPERTY, propertyIri, RDFS_SUBPROPERTY_OF);
    }

    /** {@code ∀d: ⟨d type Class⟩ ∈ Ch(G) ⟹ ⟨a domain d⟩ ∈ Ch(G)} — a has a universal domain. */
    static String propertyHasUniversalDomain(String propertyIri) {
        return """
                ASK {
                    FILTER NOT EXISTS {
                        ?d <%s> <%s> .
                        FILTER NOT EXISTS { <%s> <%s> ?d }
                    }
                }""".formatted(RDF_TYPE, RDFS_CLASS, propertyIri, RDFS_DOMAIN);
    }

    /** {@code ∀d: ⟨d type Class⟩ ∈ Ch(G) ⟹ ⟨a range d⟩ ∈ Ch(G)} — a has a universal range. */
    static String propertyHasUniversalRange(String propertyIri) {
        return """
                ASK {
                    FILTER NOT EXISTS {
                        ?d <%s> <%s> .
                        FILTER NOT EXISTS { <%s> <%s> ?d }
                    }
                }""".formatted(RDF_TYPE, RDFS_CLASS, propertyIri, RDFS_RANGE);
    }
}
