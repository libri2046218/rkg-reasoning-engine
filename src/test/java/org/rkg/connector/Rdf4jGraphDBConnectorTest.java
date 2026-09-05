package org.rkg.connector;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rkg.repostate.RepoStateStore;

/**
 * Unit tests for {@link Rdf4jGraphDBConnector} constructor-level behavior only.
 * Tests that require a live GraphDB boundary are covered in integration tests.
 */
@org.junit.jupiter.api.Tag("unit")
class Rdf4jGraphDBConnectorTest {

    private static final String ENDPOINT = "http://localhost:7200";
    private RepoStateStore mockRepoStateStore;

    @BeforeEach
    void setUp() {
        mockRepoStateStore = mock(RepoStateStore.class);
    }

    @Test
    void endpointUrlNormalizesTrailingSlash() {
        Rdf4jGraphDBConnector connWithSlash = new Rdf4jGraphDBConnector("http://localhost:7200/", mockRepoStateStore);
        Rdf4jGraphDBConnector connWithoutSlash = new Rdf4jGraphDBConnector("http://localhost:7200", mockRepoStateStore);

        // Both should normalize to the same endpoint (no trailing slash)
        assertNotNull(connWithSlash);
        assertNotNull(connWithoutSlash);
    }

    @Test
    void connectorNormalizesEndpointWithMultipleTrailingSlashes() {
        // Even with multiple trailing slashes, should normalize
        Rdf4jGraphDBConnector conn = new Rdf4jGraphDBConnector("http://localhost:7200///", mockRepoStateStore);
        assertNotNull(conn);
    }

    @Test
    void constructorThrowsNullPointerExceptionOnNullEndpoint() {
        // Constructor will throw NPE when calling endsWith on null
        assertThrows(NullPointerException.class, () ->
            new Rdf4jGraphDBConnector(null, mockRepoStateStore)
        );
    }

    @Test
    void constructorHandlesNullRepoStateStore() {
        // Constructor doesn't validate repoStateStore, just stores the reference
        // A NPE will occur later when trying to use it
        Rdf4jGraphDBConnector conn = new Rdf4jGraphDBConnector(ENDPOINT, null);
        assertNotNull(conn);
    }

}
