package org.rkg.connector;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.rio.RDFFormat;

/**
 * The sole point of contact with GraphDB (§4.1 of the software design document). Used directly
 * by Tier A CLI commands (repository administration, raw import/export/backup/restore, and
 * {@code rkg query --raw}) and internally by Tier B components ({@code DefinitenessValidator},
 * {@code ChaseOrchestrator}, {@code QueryAnsweringEngine}).
 *
 * <p>All methods throw a single normalized {@link GraphDBOperationException}. {@code update},
 * {@code importData}, and mutating calls trigger {@code RepoStateStore.markStale(repoName)} as a
 * side effect of the facade implementation, not of the caller.
 */
public interface GraphDBConnector {

    /**
     * Creates a repository configured with the fixed RKG ruleset located on the GraphDB server.
     *
     * @param name repository name
     */
    void createRepository(String name);

    /**
     * Deletes a repository (data and metadata).
     *
     * @param name repository name
     */
    void deleteRepository(String name);

    /**
     * Lists all repositories on the target GraphDB endpoint.
     *
     * @return repository names
     */
    List<String> listRepositories();

    /**
     * Lists namespace prefix mappings configured in a repository.
     *
     * @param repoName repository name
     * @return prefix-to-namespace mappings
     */
    Map<String, String> namespaces(String repoName);

    /**
     * Stores a namespace prefix mapping.
     *
     * @param repoName repository name
     * @param prefix namespace prefix
     * @param namespace namespace IRI
     */
    void setNamespace(String repoName, String prefix, String namespace);

    /**
     * Removes one namespace prefix mapping.
     *
     * @param repoName repository name
     * @param prefix namespace prefix
     */
    void removeNamespace(String repoName, String prefix);

    /**
     * Removes every namespace prefix mapping.
     *
     * @param repoName repository name
     */
    void clearNamespaces(String repoName);

    /**
     * Imports RDF data into a repository, optionally to a named graph.
     *
     * @param repoName target repository name
     * @param rdfData RDF input stream
     * @param format RDF format (TURTLE, RDFXML, NQUADS, etc.)
     * @param targetGraph named graph IRI (null maps imports to {@code urn:rkg:base-data})
     */
    void importData(String repoName, InputStream rdfData, RDFFormat format, String targetGraph);

    /**
     * Exports a repository's data to an RDF file, optionally from a named graph.
     *
     * @param repoName source repository name
     * @param out RDF output stream
     * @param format RDF format (TURTLE, RDFXML, NQUADS, etc.)
     * @param sourceGraph named graph IRI (null exports all contexts)
     */
    void exportData(String repoName, OutputStream out, RDFFormat format, String sourceGraph);

    /**
     * Executes a SPARQL query against the repository.
     *
     * @param repoName    repository name
     * @param sparqlQuery SPARQL query text
     * @param infer       whether to include reasoner-inferred statements; {@code false} together
     *                    with an empty {@code namedGraphs} list implements {@code rkg query --raw}.
     * @param namedGraphs extra graph IRIs exposed for explicit {@code GRAPH} patterns; RKG-aware
     *                    queries additionally place every repository context in their default
     *                    dataset so imported named-graph data participates in the chase path.
     * @return query result (SELECT, ASK, or GRAPH)
     */
    QueryResult query(String repoName, String sparqlQuery, boolean infer, List<String> namedGraphs);

    /**
     * Executes a SPARQL query with RDF4J bindings supplied out of band. Bindings preserve RDF
     * term identity, including blank nodes, instead of interpolating terms into query text.
     *
     * @param repoName repository name
     * @param sparqlQuery SPARQL query text
     * @param infer whether to include reasoner-inferred statements
     * @param namedGraphs additional named graphs to union into the query's dataset
     * @param bindings variable bindings keyed without the {@code ?} sigil
     * @return query result (SELECT, ASK, or GRAPH)
     */
    default QueryResult query(String repoName, String sparqlQuery, boolean infer, List<String> namedGraphs,
                              Map<String, Value> bindings) {
        if (!bindings.isEmpty()) {
            throw new UnsupportedOperationException("RDF4J query bindings are not supported");
        }
        return query(repoName, sparqlQuery, infer, namedGraphs);
    }

    /**
     * Executes a SPARQL update (INSERT/DELETE); marks the repository stale as a side effect.
     *
     * @param repoName      repository name
     * @param sparqlUpdate SPARQL update text
     */
    void update(String repoName, String sparqlUpdate);

    /**
     * Executes a SPARQL update with RDF4J bindings supplied out of band; marks the repository
     * stale. Bindings preserve RDF term identity, including blank nodes, instead of
     * interpolating terms into update text.
     *
     * @param repoName repository name
     * @param sparqlUpdate SPARQL update text
     * @param bindings variable bindings keyed without the {@code ?} sigil
     */
    default void update(String repoName, String sparqlUpdate, Map<String, Value> bindings) {
        if (!bindings.isEmpty()) {
            throw new UnsupportedOperationException("RDF4J update bindings are not supported");
        }
        update(repoName, sparqlUpdate);
    }

    /**
     * Exports a repository's data as an N-Quads backup file.
     *
     * @param repoName repository name
     * @param out      output stream to write backup data
     */
    void backup(String repoName, OutputStream out);

    /**
     * Restores a repository from a backup file.
     *
     * @param repoName repository name
     * @param in       input stream to read backup data
     */
    void restore(String repoName, InputStream in);
}
