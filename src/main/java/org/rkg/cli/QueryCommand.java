package org.rkg.cli;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import org.rkg.config.RkgContext;
import org.rkg.connector.QueryResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * {@code rkg query} — the sole flag-dependent-tier command (§2.1, §6.1): by default Tier B
 * (RKG-aware, routed through {@code QueryAnsweringEngine}, requires {@code chased = true}); with
 * {@code --raw}, Tier A (literal passthrough straight to {@code GraphDBConnector} with
 * {@code infer=false}, default graph only, no staleness check).
 *
 * <p>Accepts the SPARQL text via either an inline argument or {@code --file} (§6.2); exactly one
 * of the two must be supplied. Both input methods are resolved here, at the CLI layer, before the
 * plain {@code String} is handed to the connector/query-answering-engine interfaces.
 */
@Command(name = "query", description = "Query a repository (default: RKG-aware; --raw: literal passthrough).")
public final class QueryCommand implements Callable<Integer> {

    @ParentCommand
    private RkgCli parent;

    @Option(names = "--repo", required = true, description = "Repository name.")
    private String repo;

    @Option(names = "--raw", description = "Bypass RKG semantics: infer=false, default graph only, no staleness check.")
    private boolean raw;

    @Option(names = "--json", description = "Render the result as JSON to standard output.")
    private boolean json;

    @Option(names = "--csv", description = "Render the result as CSV to standard output.")
    private boolean csv;

    @Option(names = "--file", description = "Path to a .sparql/.rq file containing the query text.")
    private Path file;

    @Parameters(index = "0", arity = "0..1", description = "Inline SPARQL query text.")
    private String inlineQuery;

    /** Default constructor. */
    public QueryCommand() {
    }

    @Override
    public Integer call() throws Exception {
        if (json && csv) {
            throw new IllegalArgumentException("Specify at most one of --json or --csv.");
        }
        String sparqlQuery = QueryInputResolver.resolve(inlineQuery, file);
        RkgContext context = parent.context();

        QueryResult result = raw
                ? context.connector().query(repo, sparqlQuery, false, List.of())
                : context.queryAnsweringEngine().query(repo, sparqlQuery);

        System.out.print(QueryResultRenderer.render(result, json, csv));
        return ExitCodes.OK;
    }
}
