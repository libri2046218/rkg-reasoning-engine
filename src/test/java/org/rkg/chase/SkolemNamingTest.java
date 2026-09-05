package org.rkg.chase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the Skolem witness naming scheme (§3.3 of the software design document):
 * determinism, injectivity across distinct namespaces, and human decodability.
 */
@org.junit.jupiter.api.Tag("unit")
class SkolemNamingTest {

    @Test
    void classWitnessIsDeterministic() {
        String a = "http://example.org/ontology#Person";
        assertEquals(SkolemNaming.classWitness(a, false), SkolemNaming.classWitness(a, false));
    }

    @Test
    void sameLocalNameDifferentNamespaceProducesDistinctWitnesses() {
        String personA = "http://example.org/ns-a#Person";
        String personB = "http://example.org/ns-b#Person";
        assertNotEquals(SkolemNaming.classWitness(personA, false), SkolemNaming.classWitness(personB, false));
    }

    @Test
    void witnessIsDecodableBackToSourceIri() {
        String iri = "http://example.org/ontology#Person";
        String witness = SkolemNaming.classWitness(iri, false);
        String encodedSegment = witness.substring("urn:rkg:witness:class:".length());
        assertEquals(iri, SkolemNaming.percentDecode(encodedSegment));
    }

    @Test
    void classAndPropertyWitnessesUseDistinctNamespaceSegments() {
        String iri = "http://example.org/ontology#worksFor";
        String classWitness = SkolemNaming.classWitness(iri, false);
        String propSrc = SkolemNaming.propertySourceWitness(iri, false);
        String propTgt = SkolemNaming.propertyTargetWitness(iri, false);
        assertTrue(classWitness.contains(":class:"));
        assertTrue(propSrc.contains(":prop:src:"));
        assertTrue(propTgt.contains(":prop:tgt:"));
        assertNotEquals(propSrc, propTgt);
    }

    @Test
    void blankNodeUsesStableLabelInsteadOfPercentEncoding() {
        String witness = SkolemNaming.classWitness("b0", true);
        assertEquals("urn:rkg:witness:class:bn:b0", witness);
    }

    @Test
    void rdf4jBlankNodeUsesTheSameStableWitnessName() {
        String witness = SkolemNaming.classWitness(SimpleValueFactory.getInstance().createBNode("b0"));
        assertEquals("urn:rkg:witness:class:bn:b0", witness);
    }

    @Test
    void percentEncodeRoundTripsArbitraryIris() {
        String iri = "http://example.org/ontology#Foo Bar/Baz:Qux";
        String encoded = SkolemNaming.percentEncode(iri);
        assertEquals(iri, SkolemNaming.percentDecode(encoded));
        assertNotEquals(iri, encoded);
    }
}
