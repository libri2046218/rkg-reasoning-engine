package org.rkg.integration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.rkg.chase.Rdf4jChaseOrchestrator;
import org.rkg.connector.GraphDBConnector;
import org.rkg.connector.Rdf4jGraphDBConnector;
import org.rkg.query.Rdf4jQueryAnsweringEngine;
import org.rkg.repostate.RepoStateStore;
import org.rkg.repostate.SqliteRepoStateStore;
import org.rkg.validation.Rdf4jDefinitenessValidator;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Shared GraphDB lifecycle and component wiring for tests that cross the service boundary.
 * Testcontainers exposes GraphDB on a dynamically mapped host port.
 */
@Testcontainers
public abstract class GraphDbITSupport {

    @Container
    private final ComposeContainer graphdb = new ComposeContainer(
            new File("docker/docker-compose.test.yml"))
            .withExposedService("graphdb", 7200, Wait.forHttp("/rest/repositories").forStatusCode(200));

    private final Set<String> repositories = new LinkedHashSet<>();

    protected String endpointUrl;
    protected RepoStateStore repoStateStore;
    protected GraphDBConnector connector;
    protected Rdf4jDefinitenessValidator validator;
    protected Rdf4jChaseOrchestrator chaseOrchestrator;
    protected Rdf4jQueryAnsweringEngine queryAnsweringEngine;

    @BeforeEach
    void wireGraphDbComponents(@TempDir Path tempDir) {
        endpointUrl = "http://" + graphdb.getServiceHost("graphdb", 7200)
                + ":" + graphdb.getServicePort("graphdb", 7200);
        repoStateStore = new SqliteRepoStateStore(tempDir.resolve("state.db"));
        connector = new Rdf4jGraphDBConnector(endpointUrl, repoStateStore);
        validator = new Rdf4jDefinitenessValidator(connector, repoStateStore, endpointUrl);
        chaseOrchestrator = new Rdf4jChaseOrchestrator(connector, validator, repoStateStore, endpointUrl);
        queryAnsweringEngine = new Rdf4jQueryAnsweringEngine(connector, repoStateStore, endpointUrl);
    }

    @AfterEach
    void deleteTestRepositories() {
        for (String repository : repositories) {
            connector.deleteRepository(repository);
        }
        repositories.clear();
    }

    protected String createRepository(String contract) {
        String repository = "rkg-" + contract + "-" + UUID.randomUUID();
        connector.createRepository(repository);
        repositories.add(repository);
        return repository;
    }

    protected void deleteRepository(String repository) {
        connector.deleteRepository(repository);
        repositories.remove(repository);
    }

    protected void importFixture(String repository, String fixtureResourcePath) throws IOException {
        try (InputStream input = GraphDbITSupport.class.getResourceAsStream(fixtureResourcePath)) {
            if (input == null) {
                throw new IllegalArgumentException("Fixture not found: " + fixtureResourcePath);
            }
            connector.importData(repository, input, RDFFormat.TURTLE, null);
        }
    }

    protected void trackRepository(String repository) {
        repositories.add(repository);
    }

    protected void untrackRepository(String repository) {
        repositories.remove(repository);
    }
}
