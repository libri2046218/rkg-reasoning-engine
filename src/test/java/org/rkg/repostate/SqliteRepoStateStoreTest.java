package org.rkg.repostate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link SqliteRepoStateStore} against a temporary SQLite file (§2 of
 * testing-strategy.md), verifying the composite {@code (endpointUrl, repoName)} key and the
 * documented staleness data-flow transitions (§3.2 of the software design document).
 */
@org.junit.jupiter.api.Tag("unit")
class SqliteRepoStateStoreTest {

    @Test
    void freshRepositoryStartsUnchasedAndUnvalidated(@TempDir Path tempDir) {
        RepoStateStore store = new SqliteRepoStateStore(tempDir.resolve("state.db"));
        store.createRepoState("http://localhost:7200", "repo-a");

        RepoState state = store.get("http://localhost:7200", "repo-a").orElseThrow();
        assertFalse(state.chased());
        assertEquals(null, state.definite());
    }

    @Test
    void compositeKeyDistinguishesSameRepoNameAcrossEndpoints(@TempDir Path tempDir) {
        RepoStateStore store = new SqliteRepoStateStore(tempDir.resolve("state.db"));
        store.createRepoState("http://endpoint-a:7200", "shared-name");
        store.createRepoState("http://endpoint-b:7200", "shared-name");
        store.markChased("http://endpoint-a:7200", "shared-name", Instant.now());

        assertTrue(store.get("http://endpoint-a:7200", "shared-name").orElseThrow().chased());
        assertFalse(store.get("http://endpoint-b:7200", "shared-name").orElseThrow().chased());
    }

    @Test
    void markStaleResetsChasedFlagAfterChase(@TempDir Path tempDir) {
        RepoStateStore store = new SqliteRepoStateStore(tempDir.resolve("state.db"));
        String endpoint = "http://localhost:7200";
        store.createRepoState(endpoint, "repo-a");
        store.markChased(endpoint, "repo-a", Instant.now());
        assertTrue(store.get(endpoint, "repo-a").orElseThrow().chased());

        store.markStale(endpoint, "repo-a");
        assertFalse(store.get(endpoint, "repo-a").orElseThrow().chased());
    }

    @Test
    void recordValidationPersistsIndefiniteElements(@TempDir Path tempDir) {
        RepoStateStore store = new SqliteRepoStateStore(tempDir.resolve("state.db"));
        String endpoint = "http://localhost:7200";
        store.createRepoState(endpoint, "repo-a");

        store.recordValidation(endpoint, "repo-a", false, List.of("http://ex.org#A", "http://ex.org#B"));

        RepoState state = store.get(endpoint, "repo-a").orElseThrow();
        assertEquals(Boolean.FALSE, state.definite());
        assertEquals(List.of("http://ex.org#A", "http://ex.org#B"), state.indefiniteElements());
    }

    @Test
    void deleteRepoStateRemovesRow(@TempDir Path tempDir) {
        RepoStateStore store = new SqliteRepoStateStore(tempDir.resolve("state.db"));
        String endpoint = "http://localhost:7200";
        store.createRepoState(endpoint, "repo-a");
        store.deleteRepoState(endpoint, "repo-a");

        assertTrue(store.get(endpoint, "repo-a").isEmpty());
    }

    @Test
    void listAllReturnsEveryRow(@TempDir Path tempDir) {
        RepoStateStore store = new SqliteRepoStateStore(tempDir.resolve("state.db"));
        store.createRepoState("http://localhost:7200", "repo-a");
        store.createRepoState("http://localhost:7200", "repo-b");

        assertEquals(2, store.listAll().size());
    }
}
