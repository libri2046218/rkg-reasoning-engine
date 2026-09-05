package org.rkg.config;

import java.nio.file.Path;
import org.rkg.chase.ChaseOrchestrator;
import org.rkg.chase.Rdf4jChaseOrchestrator;
import org.rkg.connector.GraphDBConnector;
import org.rkg.connector.Rdf4jGraphDBConnector;
import org.rkg.query.QueryAnsweringEngine;
import org.rkg.query.Rdf4jQueryAnsweringEngine;
import org.rkg.repostate.RepoStateStore;
import org.rkg.repostate.SqliteRepoStateStore;
import org.rkg.validation.DefinitenessValidator;
import org.rkg.validation.Rdf4jDefinitenessValidator;

/**
 * Resolves the active GraphDB endpoint/credentials (Session/Config manager, §2.3, §4.5) and wires
 * up the concrete component graph used by the CLI layer. Endpoint resolution order: {@code --endpoint}
 * CLI option, then the {@code RKG_ENDPOINT} environment variable, then a
 * {@code http://localhost:7200} default suited to the local Docker Compose GraphDB instance
 * (docker/docker-compose.yml).
 */
public final class RkgContext {

    private final String endpointUrl;
    private final RepoStateStore repoStateStore;
    private final GraphDBConnector connector;
    private final DefinitenessValidator validator;
    private final ChaseOrchestrator chaseOrchestrator;
    private final QueryAnsweringEngine queryAnsweringEngine;

    /**
     * Creates an RKG context, resolving the endpoint and wiring all core components.
     *
     * @param endpointOverride explicit endpoint URL (--endpoint flag); null/blank means check env var or use default
     */
    public RkgContext(String endpointOverride) {
        this.endpointUrl = resolveEndpoint(endpointOverride);
        this.repoStateStore = new SqliteRepoStateStore(SqliteRepoStateStore.defaultDatabasePath());
        this.connector = new Rdf4jGraphDBConnector(endpointUrl, repoStateStore);
        this.validator = new Rdf4jDefinitenessValidator(connector, repoStateStore, endpointUrl);
        this.chaseOrchestrator = new Rdf4jChaseOrchestrator(connector, validator, repoStateStore, endpointUrl);
        this.queryAnsweringEngine = new Rdf4jQueryAnsweringEngine(connector, repoStateStore, endpointUrl);
    }

    private static String resolveEndpoint(String override) {
        if (override != null && !override.isBlank()) {
            return override;
        }
        String env = System.getenv("RKG_ENDPOINT");
        if (env != null && !env.isBlank()) {
            return env;
        }
        return "http://localhost:7200";
    }

    /**
     * Returns the resolved GraphDB endpoint URL.
     *
     * @return the endpoint URL
     */
    public String endpointUrl() {
        return endpointUrl;
    }

    /**
     * Returns the repository state store.
     *
     * @return the repository state store
     */
    public RepoStateStore repoStateStore() {
        return repoStateStore;
    }

    /**
     * Returns the GraphDB connector.
     *
     * @return the GraphDB connector
     */
    public GraphDBConnector connector() {
        return connector;
    }

    /**
     * Returns the definiteness validator.
     *
     * @return the definiteness validator
     */
    public DefinitenessValidator validator() {
        return validator;
    }

    /**
     * Returns the chase orchestrator.
     *
     * @return the chase orchestrator
     */
    public ChaseOrchestrator chaseOrchestrator() {
        return chaseOrchestrator;
    }

    /**
     * Returns the query answering engine.
     *
     * @return the query answering engine
     */
    public QueryAnsweringEngine queryAnsweringEngine() {
        return queryAnsweringEngine;
    }
}
