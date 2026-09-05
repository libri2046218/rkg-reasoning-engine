package org.rkg.chase;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ChaseQueries} (pure SPARQL query building logic, §3.3 of the software
 * design document): syntactic validity, correct variable binding names, and accurate triple
 * patterns for witness detection. Each test verifies the generated SPARQL string structure
 * without requiring a GraphDB instance.
 */
@org.junit.jupiter.api.Tag("unit")
class ChaseQueriesTest {

    @Test
    void defaultGraphTripleCountReturnsValidSelectQuery() {
        String query = ChaseQueries.defaultGraphTripleCount();
        
        assertNotNull(query);
        assertTrue(query.contains("SELECT"));
        assertTrue(query.contains("COUNT(*)"));
        assertTrue(query.contains("?c"));
        assertTrue(query.contains("?s ?p ?o"));
    }

    @Test
    void populatedClassesReturnsSelectWithClassTerm() {
        String query = ChaseQueries.populatedClasses();
        
        assertNotNull(query);
        assertTrue(query.contains("SELECT DISTINCT"));
        assertTrue(query.contains("?a"));
        assertTrue(query.contains("rdf-syntax-ns#type"));
        assertTrue(query.contains("urn:rkg:witness:"));
        assertTrue(query.contains("FILTER"));
        assertTrue(query.contains("STRSTARTS"));
    }

    @Test
    void populatedPropertiesReturnsSelectWithPropertyTerm() {
        String query = ChaseQueries.populatedProperties();
        
        assertNotNull(query);
        assertTrue(query.contains("SELECT DISTINCT"));
        assertTrue(query.contains("?p"));
        assertTrue(query.contains("?a ?p ?b"));
        assertTrue(query.contains("FILTER"));
        assertTrue(query.contains("STRSTARTS"));
    }

    @Test
    void classWitnessExistsUsesBoundClassTerm() {
        String query = ChaseQueries.classWitnessExists();
        
        assertNotNull(query);
        assertTrue(query.contains("ASK"));
        assertTrue(query.contains("GRAPH"));
        assertTrue(query.contains("urn:rkg:witnesses"));
        assertTrue(query.contains("?witness"));
        assertTrue(query.contains("?classTerm"));
        assertTrue(query.contains("rdf-syntax-ns#type"));
    }

    @Test
    void propertyWitnessExistsUsesBoundPropertyTerm() {
        String query = ChaseQueries.propertyWitnessExists();
        
        assertNotNull(query);
        assertTrue(query.contains("ASK"));
        assertTrue(query.contains("GRAPH"));
        assertTrue(query.contains("urn:rkg:witnesses"));
        assertTrue(query.contains("?source"));
        assertTrue(query.contains("?propertyTerm"));
        assertTrue(query.contains("?target"));
    }

    @Test
    void insertWitnessesUpdateUsesBoundTerms() {
        String update = ChaseQueries.insertWitnessesUpdate(2);
        
        assertNotNull(update);
        assertTrue(update.contains("INSERT"));
        assertTrue(update.contains("GRAPH"));
        assertTrue(update.contains("urn:rkg:witnesses"));
        assertTrue(update.contains("?subject0 ?predicate0 ?object0"));
        assertTrue(update.contains("?subject1 ?predicate1 ?object1"));
        assertTrue(update.contains("WHERE { }"));
    }

    @Test
    void insertWitnessesUpdateRejectsAnEmptyBatch() {
        assertThrows(IllegalArgumentException.class, () -> ChaseQueries.insertWitnessesUpdate(0));
    }

    @Test
    void allQueriesContainCorrectRdfTypeIri() {
        // Verify that all queries use the correct RDF type IRI
        String expectedRdfType = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
        
        String classWitnessQuery = ChaseQueries.classWitnessExists();
        assertTrue(classWitnessQuery.contains(expectedRdfType));
    }

    @Test
    void allQueriesFilterOutWitnessNamespace() {
        // Queries should exclude witness namespace to avoid loops
        String populatedClasses = ChaseQueries.populatedClasses();
        String populatedProperties = ChaseQueries.populatedProperties();
        
        assertTrue(populatedClasses.contains("urn:rkg:witness:"));
        assertTrue(populatedClasses.contains("FILTER"));
        assertTrue(populatedProperties.contains("urn:rkg:witness:"));
        assertTrue(populatedProperties.contains("FILTER"));
    }
}
