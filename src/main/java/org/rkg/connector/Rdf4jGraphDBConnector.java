package org.rkg.connector;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.rkg.config.GraphDbCredentials;
import org.rkg.repostate.RepoStateStore;

/**
 * {@link GraphDBConnector} implementation backed by GraphDB's REST management API (repository
 * lifecycle, ruleset installation, via {@link GraphDBRestClient}) and RDF4J's standard SPARQL 1.1
 * Protocol / RDF4J HTTP client (query, update, import, export, backup, restore), per §4.1 and
 * §4.3 of the software design document.
 *
 * <p>Every mutating operation ({@link #update}, {@link #importData}) marks the repository stale
 * in the {@link RepoStateStore} as a side effect of this facade, per §3.2's documented data flow
 * — callers never need to remember to do this themselves.
 */
public final class Rdf4jGraphDBConnector implements GraphDBConnector {

    private static final String BASE_DATA_GRAPH = "urn:rkg:base-data";
    private static final String WITNESS_GRAPH = "urn:rkg:witnesses";

    private final String endpointUrl;
    private final GraphDBRestClient restClient;
    private final RepoStateStore repoStateStore;
    private final GraphDbCredentials credentials;

    /**
     * Creates a connector, normalizing the endpoint URL (stripping trailing slash) and creating a
     * REST client facade for repository lifecycle operations. All mutating operations
     * ({@link #update}, {@link #importData}) will trigger {@code repoStateStore.markStale()}
     * as a side effect.
     *
     * @param endpointUrl GraphDB endpoint (e.g., "http://localhost:7200"); normalized to remove trailing "/"
     * @param repoStateStore persistent store for staleness tracking and validation results
     */
    public Rdf4jGraphDBConnector(String endpointUrl, RepoStateStore repoStateStore) {
        this(endpointUrl, repoStateStore, new GraphDbCredentials(null, null, null));
    }

    /**
     * Creates a connector with optional HTTP Basic or bearer-token credentials.
     *
     * @param endpointUrl GraphDB endpoint
     * @param repoStateStore persistent store for staleness tracking and validation results
     * @param credentials GraphDB credentials, or an unauthenticated credential set
     */
    public Rdf4jGraphDBConnector(String endpointUrl, RepoStateStore repoStateStore, GraphDbCredentials credentials) {
        this.endpointUrl = endpointUrl.endsWith("/") ? endpointUrl.substring(0, endpointUrl.length() - 1) : endpointUrl;
        this.restClient = new GraphDBRestClient(this.endpointUrl, credentials);
        this.repoStateStore = repoStateStore;
        this.credentials = credentials;
    }

    @Override
    public void createRepository(String name) {
        RepositoryNames.requireValid(name);
        restClient.createRepository(name);
        repoStateStore.createRepoState(endpointUrl, name);
    }

    @Override
    public void deleteRepository(String name) {
        RepositoryNames.requireValid(name);
        restClient.deleteRepository(name);
        repoStateStore.deleteRepoState(endpointUrl, name);
    }

    @Override
    public List<String> listRepositories() {
        return restClient.listRepositories();
    }

    @Override
    public Map<String, String> namespaces(String repoName) {
        return withConnectionResult(repoName, conn -> {
            Map<String, String> namespaces = new LinkedHashMap<>();
            try (var result = conn.getNamespaces()) {
                while (result.hasNext()) {
                    var namespace = result.next();
                    namespaces.put(namespace.getPrefix(), namespace.getName());
                }
            }
            return namespaces;
        });
    }

    @Override
    public void setNamespace(String repoName, String prefix, String namespace) {
        withConnection(repoName, conn -> conn.setNamespace(prefix, namespace));
        repoStateStore.markStale(endpointUrl, repoName);
    }

    @Override
    public void removeNamespace(String repoName, String prefix) {
        withConnection(repoName, conn -> conn.removeNamespace(prefix));
        repoStateStore.markStale(endpointUrl, repoName);
    }

    @Override
    public void clearNamespaces(String repoName) {
        withConnection(repoName, RepositoryConnection::clearNamespaces);
        repoStateStore.markStale(endpointUrl, repoName);
    }

    @Override
    public void importData(String repoName, InputStream rdfData, RDFFormat format, String targetGraph) {
        withConnection(repoName, conn -> {
            try {
                if (targetGraph != null) {
                    conn.add(rdfData, format, conn.getValueFactory().createIRI(targetGraph));
                } else {
                    conn.add(rdfData, format, conn.getValueFactory().createIRI(BASE_DATA_GRAPH));
                }
            } catch (java.io.IOException e) {
                throw new GraphDBOperationException(GraphDBOperationException.ErrorCategory.SERVER_ERROR,
                        "Failed to read RDF data stream: " + e.getMessage(), null, e);
            } catch (RDFParseException e) {
                throw new GraphDBOperationException(GraphDBOperationException.ErrorCategory.MALFORMED_QUERY,
                        "Malformed RDF data: " + e.getMessage(), null, e);
            }
        });
        repoStateStore.markStale(endpointUrl, repoName);
    }

    @Override
    public void exportData(String repoName, OutputStream out, RDFFormat format, String sourceGraph) {
        withConnection(repoName, conn -> {
            var writer = org.eclipse.rdf4j.rio.Rio.createWriter(format, out);
            if (sourceGraph != null) {
                conn.export(writer, conn.getValueFactory().createIRI(sourceGraph));
            } else {
                conn.export(writer);
            }
        });
    }

    @Override
    public QueryResult query(String repoName, String sparqlQuery, boolean infer, List<String> namedGraphs) {
        return query(repoName, sparqlQuery, infer, namedGraphs, Map.of());
    }

    @Override
    public QueryResult query(String repoName, String sparqlQuery, boolean infer, List<String> namedGraphs,
                             Map<String, Value> bindings) {
        return withConnectionResult(repoName, conn -> {
            try {
                Query query = conn.prepareQuery(QueryLanguage.SPARQL, sparqlQuery);
                bindings.forEach(query::setBinding);
                boolean rkgQuery = infer || namedGraphs.contains(WITNESS_GRAPH);
                query.setIncludeInferred(rkgQuery);
                org.eclipse.rdf4j.query.impl.SimpleDataset dataset = new org.eclipse.rdf4j.query.impl.SimpleDataset();
                if (rkgQuery) {
                    addRkgDatasetGraphs(conn, dataset);
                    for (String namedGraph : namedGraphs) {
                        if (!WITNESS_GRAPH.equals(namedGraph)) {
                            dataset.addDefaultGraph(conn.getValueFactory().createIRI(namedGraph));
                            dataset.addNamedGraph(conn.getValueFactory().createIRI(namedGraph));
                        }
                    }
                } else {
                    dataset.addDefaultGraph(conn.getValueFactory().createIRI(BASE_DATA_GRAPH));
                }

                query.setDataset(dataset);

                if (query instanceof BooleanQuery booleanQuery) {
                    return QueryResult.ask(booleanQuery.evaluate());
                } else if (query instanceof TupleQuery tupleQuery) {
                    try (TupleQueryResult result = tupleQuery.evaluate()) {
                        List<String> variableNames = result.getBindingNames();
                        List<Map<String, Value>> rows = new ArrayList<>();
                        while (result.hasNext()) {
                            BindingSet bindingSet = result.next();
                            Map<String, Value> row = new LinkedHashMap<>();
                            for (String var : variableNames) {
                                row.put(var, bindingSet.getValue(var));
                            }
                            rows.add(row);
                        }
                        return QueryResult.selectValues(variableNames, rows);
                    }
                } else if (query instanceof GraphQuery graphQuery) {
                    List<Statement> statements = new ArrayList<>();
                    try (GraphQueryResult result = graphQuery.evaluate()) {
                        while (result.hasNext()) {
                            statements.add(result.next());
                        }
                    }
                    return QueryResult.graph(statements);
                }
                throw new GraphDBOperationException(GraphDBOperationException.ErrorCategory.MALFORMED_QUERY,
                        "Unsupported SPARQL query form", null);
            } catch (MalformedQueryException e) {
                throw new GraphDBOperationException(GraphDBOperationException.ErrorCategory.MALFORMED_QUERY,
                        "Malformed SPARQL query: " + e.getMessage(), null, e);
            }
        });
    }

    private void addRkgDatasetGraphs(RepositoryConnection connection,
                                     org.eclipse.rdf4j.query.impl.SimpleDataset dataset) {
        // GraphDB's RDF4J endpoint exposes its union view through the null default context.
        // Contexts are added as named graphs for explicit GRAPH-pattern visibility.
        dataset.addDefaultGraph(null);
        try (var contexts = connection.getContextIDs()) {
            while (contexts.hasNext()) {
                Resource context = contexts.next();
                if (context instanceof org.eclipse.rdf4j.model.IRI iri) {
                    dataset.addNamedGraph(iri);
                }
            }
        }
    }

    @Override
    public void update(String repoName, String sparqlUpdate) {
        update(repoName, sparqlUpdate, Map.of());
    }

    @Override
    public void update(String repoName, String sparqlUpdate, Map<String, Value> bindings) {
        withConnection(repoName, conn -> {
            try {
                Update update = conn.prepareUpdate(QueryLanguage.SPARQL, sparqlUpdate);
                bindings.forEach(update::setBinding);
                update.execute();
            } catch (MalformedQueryException e) {
                throw new GraphDBOperationException(GraphDBOperationException.ErrorCategory.MALFORMED_QUERY,
                        "Malformed SPARQL update: " + e.getMessage(), null, e);
            }
        });
        repoStateStore.markStale(endpointUrl, repoName);
    }

    @Override
    public void backup(String repoName, OutputStream out) {
        exportData(repoName, out, RDFFormat.NQUADS, null);
    }

    @Override
    public void restore(String repoName, InputStream in) {
        importData(repoName, in, RDFFormat.NQUADS, null);
    }

    private HTTPRepository open(String repoName) {
        RepositoryNames.requireValid(repoName);
        HTTPRepository repository = new HTTPRepository(endpointUrl, repoName);
        if (credentials.username() != null) {
            repository.setUsernameAndPassword(credentials.username(), credentials.password());
        }
        credentials.authorizationHeader().filter(header -> credentials.token() != null)
                .ifPresent(header -> repository.setAdditionalHttpHeaders(Map.of("Authorization", header)));
        try {
            repository.init();
        } catch (RepositoryException e) {
            throw translateConnectionError(repoName, e);
        }
        return repository;
    }

    private void withConnection(String repoName, java.util.function.Consumer<RepositoryConnection> action) {
        HTTPRepository repository = open(repoName);
        try (RepositoryConnection conn = repository.getConnection()) {
            action.accept(conn);
        } catch (RepositoryException e) {
            throw translateConnectionError(repoName, e);
        } finally {
            repository.shutDown();
        }
    }

    private <T> T withConnectionResult(String repoName, java.util.function.Function<RepositoryConnection, T> action) {
        HTTPRepository repository = open(repoName);
        try (RepositoryConnection conn = repository.getConnection()) {
            return action.apply(conn);
        } catch (RepositoryException e) {
            throw translateConnectionError(repoName, e);
        } finally {
            repository.shutDown();
        }
    }

    private GraphDBOperationException translateConnectionError(String repoName, RepositoryException e) {
        String message = e.getMessage() == null ? "" : e.getMessage();
        if (message.contains("404") || message.toLowerCase().contains("not found")) {
            return new GraphDBOperationException(GraphDBOperationException.ErrorCategory.REPO_NOT_FOUND,
                    "Repository '" + repoName + "' not found on " + endpointUrl, 404, e);
        }
        if (message.contains("Connection refused") || e.getCause() instanceof java.net.ConnectException) {
            return new GraphDBOperationException(GraphDBOperationException.ErrorCategory.CONNECTION,
                    "Could not connect to GraphDB at " + endpointUrl + ": " + message, null, e);
        }
        return new GraphDBOperationException(GraphDBOperationException.ErrorCategory.SERVER_ERROR,
                "GraphDB operation on '" + repoName + "' failed: " + message, null, e);
    }
}
