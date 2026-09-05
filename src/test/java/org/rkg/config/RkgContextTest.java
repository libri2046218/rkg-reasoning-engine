package org.rkg.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.rkg.chase.ChaseOrchestrator;
import org.rkg.connector.GraphDBConnector;
import org.rkg.query.QueryAnsweringEngine;
import org.rkg.repostate.RepoStateStore;
import org.rkg.validation.DefinitenessValidator;

/**
 * Unit tests for {@link RkgContext} (§2.3, §4.5 of the software design document): endpoint
 * resolution (CLI override, environment variable, default) and component assembly/wiring.
 */
@org.junit.jupiter.api.Tag("unit")
class RkgContextTest {

    @Test
    void contextWithExplicitEndpointUsesThatEndpoint() {
        String endpoint = "http://custom.graphdb:7200";
        RkgContext context = new RkgContext(endpoint);
        
        assertEquals(endpoint, context.endpointUrl());
    }

    @Test
    void contextWithNullEndpointUsesDefault() {
        RkgContext context = new RkgContext(null);
        
        assertEquals("http://localhost:7200", context.endpointUrl());
    }

    @Test
    void contextWithBlankEndpointUsesDefault() {
        RkgContext context = new RkgContext("   ");
        
        assertEquals("http://localhost:7200", context.endpointUrl());
    }

    @Test
    void contextWithEmptyStringEndpointUsesDefault() {
        RkgContext context = new RkgContext("");
        
        assertEquals("http://localhost:7200", context.endpointUrl());
    }

    @Test
    void contextAssembleAllComponents() {
        RkgContext context = new RkgContext("http://localhost:7200");
        
        assertNotNull(context.endpointUrl());
        assertNotNull(context.repoStateStore());
        assertNotNull(context.connector());
        assertNotNull(context.validator());
        assertNotNull(context.chaseOrchestrator());
        assertNotNull(context.queryAnsweringEngine());
    }

    @Test
    void contextRepoStateStoreIsNotNull() {
        RkgContext context = new RkgContext("http://localhost:7200");
        
        RepoStateStore store = context.repoStateStore();
        assertNotNull(store);
    }

    @Test
    void contextConnectorIsNotNull() {
        RkgContext context = new RkgContext("http://localhost:7200");
        
        GraphDBConnector connector = context.connector();
        assertNotNull(connector);
    }

    @Test
    void contextValidatorIsNotNull() {
        RkgContext context = new RkgContext("http://localhost:7200");
        
        DefinitenessValidator validator = context.validator();
        assertNotNull(validator);
    }

    @Test
    void contextChaseOrchestratorIsNotNull() {
        RkgContext context = new RkgContext("http://localhost:7200");
        
        ChaseOrchestrator orchestrator = context.chaseOrchestrator();
        assertNotNull(orchestrator);
    }

    @Test
    void contextQueryAnsweringEngineIsNotNull() {
        RkgContext context = new RkgContext("http://localhost:7200");
        
        QueryAnsweringEngine engine = context.queryAnsweringEngine();
        assertNotNull(engine);
    }

    @Test
    void contextComponentsWiredWithSameEndpoint() {
        String endpoint = "http://example.graphdb:7200";
        RkgContext context = new RkgContext(endpoint);
        
        // All components should be initialized with the same endpoint
        assertEquals(endpoint, context.endpointUrl());
        assertNotNull(context.connector());
        assertNotNull(context.validator());
        assertNotNull(context.chaseOrchestrator());
    }

    @Test
    void contextWithCustomEndpointPersists() {
        String customEndpoint = "http://remote.server.com:7200";
        RkgContext context = new RkgContext(customEndpoint);
        
        // Endpoint should not change
        assertEquals(customEndpoint, context.endpointUrl());
    }

    @Test
    void multipleContextsWithDifferentEndpoints() {
        RkgContext context1 = new RkgContext("http://endpoint1:7200");
        RkgContext context2 = new RkgContext("http://endpoint2:7200");
        
        assertEquals("http://endpoint1:7200", context1.endpointUrl());
        assertEquals("http://endpoint2:7200", context2.endpointUrl());
    }

    @Test
    void contextEndpointCanContainCredentials() {
        String endpointWithCreds = "http://user:pass@localhost:7200";
        RkgContext context = new RkgContext(endpointWithCreds);
        
        assertEquals(endpointWithCreds, context.endpointUrl());
    }

    @Test
    void contextEndpointWithPortNumber() {
        String endpointWithPort = "http://localhost:9999";
        RkgContext context = new RkgContext(endpointWithPort);
        
        assertEquals(endpointWithPort, context.endpointUrl());
    }

    @Test
    void contextDefaultEndpointIncludesCorrectPort() {
        RkgContext context = new RkgContext(null);
        
        assertTrue(context.endpointUrl().contains("7200"),
                "Default endpoint should use GraphDB default port 7200");
    }

    @Test
    void contextCanBeCreatedMultipleTimes() {
        RkgContext ctx1 = new RkgContext("http://localhost:7200");
        RkgContext ctx2 = new RkgContext("http://localhost:7200");
        
        // Both should be valid and independent instances
        assertNotNull(ctx1.connector());
        assertNotNull(ctx2.connector());
        assertEquals(ctx1.endpointUrl(), ctx2.endpointUrl());
    }

    @Test
    void contextConnectorAccessorReturnsSameInstance() {
        RkgContext context = new RkgContext("http://localhost:7200");
        GraphDBConnector connector1 = context.connector();
        GraphDBConnector connector2 = context.connector();
        
        // Should return the same instance (not create new ones)
        assertTrue(connector1 == connector2);
    }
}
