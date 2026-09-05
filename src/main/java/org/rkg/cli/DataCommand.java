package org.rkg.cli;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.rkg.config.RkgContext;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

/**
 * {@code rkg data import/export/clear} — Tier A bulk RDF load/dump commands (§6.1). Thin
 * passthrough to {@code GraphDBConnector}; {@code import} marks the repository stale as a side
 * effect of the connector facade (§3.2).
 */
@Command(name = "data", description = "Bulk RDF load/dump (import/export/clear).",
        subcommands = {DataCommand.Import.class, DataCommand.Export.class, DataCommand.Clear.class})
public final class DataCommand implements Runnable {

    @ParentCommand
    private RkgCli parent;

    /** Default constructor. */
    public DataCommand() {
    }

    RkgContext context() {
        return parent.context();
    }

    @Override
    public void run() {
        picocli.CommandLine.usage(this, System.out);
    }

    private static RDFFormat formatFor(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".ttl") || name.endsWith(".turtle")) {
            return RDFFormat.TURTLE;
        }
        if (name.endsWith(".rdf") || name.endsWith(".xml")) {
            return RDFFormat.RDFXML;
        }
        if (name.endsWith(".nq") || name.endsWith(".nquads")) {
            return RDFFormat.NQUADS;
        }
        return RDFFormat.TURTLE;
    }

    @Command(name = "import", description = "Import an RDF file (Turtle/RDF-XML/N-Quads) into a repository.")
    static final class Import implements Callable<Integer> {
        @ParentCommand
        private DataCommand parent;
        @Option(names = "--repo", required = true, description = "Target repository name.")
        private String repo;
        @Option(names = "--file", required = true, description = "Path to the RDF file to import.")
        private Path file;
        @Option(names = "--graph", description = "Target named graph IRI (default: urn:rkg:base-data).")
        private String graph;

        @Override
        public Integer call() throws Exception {
            RkgContext context = parent.context();
            try (var in = new FileInputStream(file.toFile())) {
                context.connector().importData(repo, in, formatFor(file), graph);
            }
            System.out.println("Imported '" + file + "' into repository '" + repo + "'.");
            return ExitCodes.OK;
        }
    }

    @Command(name = "export", description = "Export a repository's data to an RDF file.")
    static final class Export implements Callable<Integer> {
        @ParentCommand
        private DataCommand parent;
        @Option(names = "--repo", required = true, description = "Source repository name.")
        private String repo;
        @Option(names = "--file", required = true, description = "Output file path.")
        private Path file;
        @Option(names = "--graph", description = "Source named graph IRI (default: all graphs).")
        private String graph;

        @Override
        public Integer call() throws Exception {
            RkgContext context = parent.context();
            try (var out = new FileOutputStream(file.toFile())) {
                context.connector().exportData(repo, out, formatFor(file), graph);
            }
            System.out.println("Exported repository '" + repo + "' to '" + file + "'.");
            return ExitCodes.OK;
        }
    }

    @Command(name = "clear", description = "Remove all triples from a repository (does not delete the repository itself).")
    static final class Clear implements Callable<Integer> {
        @ParentCommand
        private DataCommand parent;
        @Option(names = "--repo", required = true, description = "Repository name.")
        private String repo;

        @Override
        public Integer call() {
            parent.context().connector().update(repo, "CLEAR ALL");
            System.out.println("Cleared all data from repository '" + repo + "'.");
            return ExitCodes.OK;
        }
    }
}
