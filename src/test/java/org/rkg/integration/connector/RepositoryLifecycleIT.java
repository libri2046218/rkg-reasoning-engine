package org.rkg.integration.connector;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.rkg.integration.GraphDbITSupport;

@Tag("integration")
class RepositoryLifecycleIT extends GraphDbITSupport {

    @Test
    void createsAndDeletesRepositoryWithLocalStateInLockstep() {
        String repository = createRepository("lifecycle");

        assertTrue(connector.listRepositories().contains(repository));
        assertTrue(repoStateStore.get(endpointUrl, repository).isPresent());

        deleteRepository(repository);

        assertFalse(connector.listRepositories().contains(repository));
        assertTrue(repoStateStore.get(endpointUrl, repository).isEmpty());
    }
}
