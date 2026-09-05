package org.rkg.cli;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.rkg.config.RkgContext;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/** {@code rkg update} — Tier A raw SPARQL update passthrough. */
@Command(name = "update", mixinStandardHelpOptions = true,
        description = "Execute a raw SPARQL update and mark the repository stale.")
public final class UpdateCommand implements Callable<Integer> {

    @ParentCommand
    private RkgCli parent;

    /** Creates the update command. */
    public UpdateCommand() {
    }

    @Option(names = "--repo", required = true, description = "Repository name.")
    private String repo;

    @Option(names = "--file", description = "Path to a file containing the SPARQL update.")
    private Path file;

    @Parameters(index = "0", arity = "0..1", description = "Inline SPARQL update text.")
    private String inlineUpdate;

    @Override
    public Integer call() {
        String sparqlUpdate = QueryInputResolver.resolve(inlineUpdate, file, "SPARQL update");
        RkgContext context = parent.context();
        context.connector().update(repo, sparqlUpdate);
        System.out.println("Updated repository '" + repo + "'.");
        return ExitCodes.OK;
    }
}
