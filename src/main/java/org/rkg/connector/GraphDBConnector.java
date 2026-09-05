package org.rkg.connector;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
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
     * Creates a repository and atomically installs the single bundled ruleset
     * ({@code rules/chase-rules.pie}, rules 1-21). No ruleset parameter is exposed: the ruleset is
     * fixed and not user-configurable (§7.2).
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
     * Imports RDF data into a repository, optionally to a named graph.
     *
     * @param repoName target repository name
     * @param rdfData RDF input stream
     * @param format RDF format (TURTLE, RDFXML, NQUADS, etc.)
     * @param targetGraph named graph IRI (null means default graph {@code urn:rkg:base-data})
     */
    void importData(String repoName, InputStream rdfData, RDFFormat format, String targetGraph);

    /**
     * Exports a repository's data to an RDF file, optionally from a named graph.
     *
     * @param repoName source repository name
     * @param out RDF output stream
     * @param format RDF format (TURTLE, RDFXML, NQUADS, etc.)
     * @param sourceGraph named graph IRI (null means default graph {@code urn:rkg:base-data})
     */
    void exportData(String repoName, OutputStream out, RDFFormat format, String sourceGraph);

    /**
     * Executes a SPARQL query against the repository.
     *
     * @param repoName    repository name
     * @param sparqlQuery SPARQL query text
     * @param infer       whether to include reasoner-inferred statements; {@code false} together
     *                    with an empty {@code namedGraphs} list implements {@code rkg query --raw}.
     * @param namedGraphs additional named graphs to union into the query's dataset; the
     *                    {@code QueryAnsweringEngine} calls this with {@code infer=true} and
     *                    {@code namedGraphs=["urn:rkg:witnesses"]}.
     * @return query result (SELECT, ASK, or GRAPH)
     */
    QueryResult query(String repoName, String sparqlQuery, boolean infer, List<String> namedGraphs);

    /**
     * Executes a SPARQL update (INSERT/DELETE); marks the repository stale as a side effect.
     *
     * @param repoName      repository name
     * @param sparqlUpdate SPARQL update text
     */
    void update(String repoName, String sparqlUpdate);

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
