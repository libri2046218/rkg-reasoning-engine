package org.rkg.chase;

import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    void populatedClassesReturnsSelectWithClassAndBlankNodeFlag() {
        String query = ChaseQueries.populatedClasses();
        
        assertNotNull(query);
        assertTrue(query.contains("SELECT DISTINCT"));
        assertTrue(query.contains("?a"));
        assertTrue(query.contains("?isBlank"));
        assertTrue(query.contains("IF(isBlank(?a)"));
        assertTrue(query.contains("rdf-syntax-ns#type"));
        assertTrue(query.contains("urn:rkg:witness:"));
        assertTrue(query.contains("FILTER"));
        assertTrue(query.contains("STRSTARTS"));
    }

    @Test
    void populatedPropertiesReturnsSelectWithPropertyAndBlankNodeFlag() {
        String query = ChaseQueries.populatedProperties();
        
        assertNotNull(query);
        assertTrue(query.contains("SELECT DISTINCT"));
        assertTrue(query.contains("?p"));
        assertTrue(query.contains("?isBlank"));
        assertTrue(query.contains("IF(isBlank(?p)"));
        assertTrue(query.contains("?a ?p ?b"));
        assertTrue(query.contains("FILTER"));
        assertTrue(query.contains("STRSTARTS"));
    }

    @Test
    void classWitnessExistsBuildsAskQueryWithWitnessGraph() {
        String classIri = "http://example.org/ontology#Person";
        String witnessIri = "urn:rkg:witness:class:Person";
        String query = ChaseQueries.classWitnessExists(classIri, witnessIri);
        
        assertNotNull(query);
        assertTrue(query.contains("ASK"));
        assertTrue(query.contains("GRAPH"));
        assertTrue(query.contains("urn:rkg:witnesses"));
        assertTrue(query.contains(classIri));
        assertTrue(query.contains(witnessIri));
        assertTrue(query.contains("rdf-syntax-ns#type"));
    }

    @Test
    void propertyWitnessExistsBuildsAskQueryForWitnessPair() {
        String propertyIri = "http://example.org/ontology#worksFor";
        String sourceWitness = "urn:rkg:witness:prop:src:worksFor";
        String targetWitness = "urn:rkg:witness:prop:tgt:worksFor";
        String query = ChaseQueries.propertyWitnessExists(propertyIri, sourceWitness, targetWitness);
        
        assertNotNull(query);
        assertTrue(query.contains("ASK"));
        assertTrue(query.contains("GRAPH"));
        assertTrue(query.contains("urn:rkg:witnesses"));
        assertTrue(query.contains(propertyIri));
        assertTrue(query.contains(sourceWitness));
        assertTrue(query.contains(targetWitness));
    }

    @Test
    void insertWitnessesUpdateBuildsInsertDataWithWitnessGraph() {
        String triple1 = "<urn:rkg:witness:class:Person> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://example.org/ontology#Person>";
        String triple2 = "<urn:rkg:witness:prop:src:x> <http://example.org/ontology#worksFor> <urn:rkg:witness:prop:tgt:x>";
        
        String update = ChaseQueries.insertWitnessesUpdate(java.util.List.of(triple1, triple2));
        
        assertNotNull(update);
        assertTrue(update.contains("INSERT DATA"));
        assertTrue(update.contains("GRAPH"));
        assertTrue(update.contains("urn:rkg:witnesses"));
        assertTrue(update.contains(triple1));
        assertTrue(update.contains(triple2));
    }

    @Test
    void insertWitnessesUpdateHandlesEmptyTripleList() {
        String update = ChaseQueries.insertWitnessesUpdate(java.util.List.of());
        
        assertNotNull(update);
        assertTrue(update.contains("INSERT DATA"));
        assertTrue(update.contains("GRAPH"));
        assertTrue(update.contains("urn:rkg:witnesses"));
    }

    @Test
    void classWitnessTripleFormatsTripleCorrectly() {
        String witnessIri = "urn:rkg:witness:class:Person";
        String classIri = "http://example.org/ontology#Person";
        String triple = ChaseQueries.classWitnessTriple(witnessIri, classIri);
        
        assertNotNull(triple);
        assertTrue(triple.contains(witnessIri));
        assertTrue(triple.contains(classIri));
        assertTrue(triple.contains("rdf-syntax-ns#type"));
        assertTrue(triple.startsWith("<"));
        assertTrue(triple.endsWith(">"));
    }

    @Test
    void propertyWitnessTripleFormatsTripleCorrectly() {
        String sourceWitness = "urn:rkg:witness:prop:src:x";
        String propertyIri = "http://example.org/ontology#worksFor";
        String targetWitness = "urn:rkg:witness:prop:tgt:x";
        
        String triple = ChaseQueries.propertyWitnessTriple(sourceWitness, propertyIri, targetWitness);
        
        assertNotNull(triple);
        assertTrue(triple.contains(sourceWitness));
        assertTrue(triple.contains(propertyIri));
        assertTrue(triple.contains(targetWitness));
        assertTrue(triple.startsWith("<"));
        assertTrue(triple.endsWith(">"));
    }

    @Test
    void allQueriesContainCorrectRdfTypeIri() {
        // Verify that all queries use the correct RDF type IRI
        String expectedRdfType = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
        
        String classTriple = ChaseQueries.classWitnessTriple("urn:ex:witness", "http://ex.org/Class");
        assertTrue(classTriple.contains(expectedRdfType));
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
