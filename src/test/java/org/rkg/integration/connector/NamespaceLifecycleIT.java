package org.rkg.integration.connector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.rkg.integration.GraphDbITSupport;

@Tag("integration")
class NamespaceLifecycleIT extends GraphDbITSupport {

    @Test
    void managesNamespacesAndMarksTheRepositoryStale() {
        String repository = createRepository("namespaces");

        connector.setNamespace(repository, "ex", "https://example.org/");
        assertEquals("https://example.org/", connector.namespaces(repository).get("ex"));
        assertFalse(repoStateStore.get(endpointUrl, repository).orElseThrow().chased());

        connector.removeNamespace(repository, "ex");
        assertFalse(connector.namespaces(repository).containsKey("ex"));

        connector.setNamespace(repository, "ex", "https://example.org/");
        connector.clearNamespaces(repository);
        assertFalse(connector.namespaces(repository).containsKey("ex"));
    }
}
