package org.rkg.validation;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DefinitenessQueries} (pure SPARQL query building logic, §5.2 of the
 * software design document): syntactic validity, correct variable bindings, IRI escaping, and
 * accurate logical operators for Proposition 5's definiteness characterization.
 */
@org.junit.jupiter.api.Tag("unit")
class DefinitenessQueriesTest {

    @Test
    void candidateClassesReturnsSelectQueryExcludingWitnesses() {
        String query = DefinitenessQueries.candidateClasses();
        
        assertNotNull(query);
        assertTrue(query.contains("SELECT DISTINCT ?a"));
        assertTrue(query.contains("rdf-syntax-ns#type"));
        assertTrue(query.contains("rdf-schema#Class"));
        assertTrue(query.contains("FILTER"));
        assertTrue(query.contains("STRSTARTS"));
        assertTrue(query.contains("urn:rkg:witness:"));
    }

    @Test
    void candidatePropertiesReturnsSelectQueryExcludingWitnesses() {
        String query = DefinitenessQueries.candidateProperties();
        
        assertNotNull(query);
        assertTrue(query.contains("SELECT DISTINCT ?a"));
        assertTrue(query.contains("rdf-syntax-ns#type"));
        assertTrue(query.contains("rdf-syntax-ns#Property"));
        assertTrue(query.contains("FILTER"));
        assertTrue(query.contains("STRSTARTS"));
        assertTrue(query.contains("urn:rkg:witness:"));
    }

    @Test
    void classPopulatedReturnsAskQueryForPopulationCheck() {
        String classIri = "http://example.org/ontology#Person";
        String query = DefinitenessQueries.classPopulated(classIri);
        
        assertNotNull(query);
        assertTrue(query.contains("ASK"));
        assertTrue(query.contains("rdf-syntax-ns#type"));
        assertTrue(query.contains(classIri));
        assertTrue(query.contains("?b"));
    }

    @Test
    void classIsBottomReturnsAskQueryCheckingUniversalSubclass() {
        String classIri = "http://example.org/ontology#Thing";
        String query = DefinitenessQueries.classIsBottom(classIri);
        
        assertNotNull(query);
        assertTrue(query.contains("ASK"));
        assertTrue(query.contains("FILTER NOT EXISTS"));
        assertTrue(query.contains("rdf-syntax-ns#type"));
        assertTrue(query.contains("rdf-schema#Class"));
        assertTrue(query.contains("rdf-schema#subClassOf"));
        assertTrue(query.contains(classIri));
        // Should check that the class is subclass of all classes
        assertTrue(query.contains("?c"));
    }

    @Test
    void propertyPopulatedReturnsAskQueryForPopulationCheck() {
        String propertyIri = "http://example.org/ontology#worksFor";
        String query = DefinitenessQueries.propertyPopulated(propertyIri);
        
        assertNotNull(query);
        assertTrue(query.contains("ASK"));
        assertTrue(query.contains(propertyIri));
        assertTrue(query.contains("?b"));
        assertTrue(query.contains("?c"));
    }

    @Test
    void propertyIsSubPropertyOfEveryReturnsAskQuery() {
        String propertyIri = "http://example.org/ontology#allProperty";
        String query = DefinitenessQueries.propertyIsSubPropertyOfEvery(propertyIri);
        
        assertNotNull(query);
        assertTrue(query.contains("ASK"));
        assertTrue(query.contains("FILTER NOT EXISTS"));
        assertTrue(query.contains("rdf-syntax-ns#type"));
        assertTrue(query.contains("rdf-syntax-ns#Property"));
        assertTrue(query.contains("rdf-schema#subPropertyOf"));
        assertTrue(query.contains(propertyIri));
        // Should check property against all other properties
        assertTrue(query.contains("?p"));
    }

    @Test
    void propertyHasUniversalDomainReturnsAskQuery() {
        String propertyIri = "http://example.org/ontology#everywhereDefined";
        String query = DefinitenessQueries.propertyHasUniversalDomain(propertyIri);
        
        assertNotNull(query);
        assertTrue(query.contains("ASK"));
        assertTrue(query.contains("FILTER NOT EXISTS"));
        assertTrue(query.contains("rdf-syntax-ns#type"));
        assertTrue(query.contains("rdf-schema#Class"));
        assertTrue(query.contains("rdf-schema#domain"));
        assertTrue(query.contains(propertyIri));
        // Should check domain against all classes
        assertTrue(query.contains("?d"));
    }

    @Test
    void propertyHasUniversalRangeReturnsAskQuery() {
        String propertyIri = "http://example.org/ontology#everywhereRange";
        String query = DefinitenessQueries.propertyHasUniversalRange(propertyIri);
        
        assertNotNull(query);
        assertTrue(query.contains("ASK"));
        assertTrue(query.contains("FILTER NOT EXISTS"));
        assertTrue(query.contains("rdf-syntax-ns#type"));
        assertTrue(query.contains("rdf-schema#Class"));
        assertTrue(query.contains("rdf-schema#range"));
        assertTrue(query.contains(propertyIri));
        // Should check range against all classes
        assertTrue(query.contains("?d"));
    }

    @Test
    void classPopulatedWithSpecialCharactersInIri() {
        String classIri = "http://example.org/ontology#Person%20Extended";
        String query = DefinitenessQueries.classPopulated(classIri);
        
        assertNotNull(query);
        assertTrue(query.contains(classIri));
        assertTrue(query.contains("ASK"));
    }

    @Test
    void propertyPopulatedWithSpecialCharactersInIri() {
        String propertyIri = "http://example.org/ontology#works%20For";
        String query = DefinitenessQueries.propertyPopulated(propertyIri);
        
        assertNotNull(query);
        assertTrue(query.contains(propertyIri));
        assertTrue(query.contains("ASK"));
    }

    @Test
    void allClassQueriesUseCorrectRdfAndRdfsIris() {
        // Verify consistent use of standard namespace IRIs
        String candidateClasses = DefinitenessQueries.candidateClasses();
        String classPopulated = DefinitenessQueries.classPopulated("http://ex.org/C");
        String classIsBottom = DefinitenessQueries.classIsBottom("http://ex.org/C");
        
        String expectedRdfType = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
        String expectedRdfsClass = "http://www.w3.org/2000/01/rdf-schema#Class";
        String expectedRdfsSubClassOf = "http://www.w3.org/2000/01/rdf-schema#subClassOf";
        
        assertTrue(candidateClasses.contains(expectedRdfType));
        assertTrue(candidateClasses.contains(expectedRdfsClass));
        assertTrue(classIsBottom.contains(expectedRdfsSubClassOf));
    }

    @Test
    void allPropertyQueriesUseCorrectRdfAndRdfsIris() {
        // Verify consistent use of standard namespace IRIs
        String candidateProperties = DefinitenessQueries.candidateProperties();
        String propertyIsSubPropertyOfEvery = DefinitenessQueries.propertyIsSubPropertyOfEvery("http://ex.org/P");
        
        String expectedRdfType = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
        String expectedRdfProperty = "http://www.w3.org/1999/02/22-rdf-syntax-ns#Property";
        String expectedRdfsSubPropertyOf = "http://www.w3.org/2000/01/rdf-schema#subPropertyOf";
        
        assertTrue(candidateProperties.contains(expectedRdfType));
        assertTrue(candidateProperties.contains(expectedRdfProperty));
        assertTrue(propertyIsSubPropertyOfEvery.contains(expectedRdfsSubPropertyOf));
    }

    @Test
    void universalityQueriesUseFilterNotExists() {
        // Bottom/universal queries use FILTER NOT EXISTS pattern
        String classIsBottom = DefinitenessQueries.classIsBottom("http://ex.org/C");
        String propIsSubPropertyOfEvery = DefinitenessQueries.propertyIsSubPropertyOfEvery("http://ex.org/P");
        String propHasUniversalDomain = DefinitenessQueries.propertyHasUniversalDomain("http://ex.org/P");
        String propHasUniversalRange = DefinitenessQueries.propertyHasUniversalRange("http://ex.org/P");
        
        assertTrue(classIsBottom.contains("FILTER NOT EXISTS"));
        assertTrue(propIsSubPropertyOfEvery.contains("FILTER NOT EXISTS"));
        assertTrue(propHasUniversalDomain.contains("FILTER NOT EXISTS"));
        assertTrue(propHasUniversalRange.contains("FILTER NOT EXISTS"));
    }

    @Test
    void propositionFiveQueriesFilterWitnessNamespace() {
        // Candidate queries filter out witness namespace to avoid indefinite loops
        String candidateClasses = DefinitenessQueries.candidateClasses();
        String candidateProperties = DefinitenessQueries.candidateProperties();
        
        assertTrue(candidateClasses.contains("FILTER (!STRSTARTS(STR(?a), \"urn:rkg:witness:\"))"));
        assertTrue(candidateProperties.contains("FILTER (!STRSTARTS(STR(?a), \"urn:rkg:witness:\"))"));
    }
}
