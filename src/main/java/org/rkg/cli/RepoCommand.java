package org.rkg.cli;

import java.util.concurrent.Callable;
import org.rkg.config.RkgContext;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Parameters;

/**
 * {@code rkg repo create/list/delete} — Tier A repository lifecycle commands (§6.1). Thin
 * passthrough to {@code GraphDBConnector}; {@code create} additionally installs the single
 * bundled ruleset atomically (no ruleset option is exposed, per §7.2).
 */
@Command(name = "repo",
        description = "Repository lifecycle (create/list/delete).",
        subcommands = {
                RepoCommand.Create.class,
                RepoCommand.List.class,
                RepoCommand.Delete.class
        }
)
public final class RepoCommand implements Runnable {

    @ParentCommand
    private RkgCli parent;

    /** Default constructor. */
    public RepoCommand() {
    }

    RkgContext context() {
        return parent.context();
    }

    @Override
    public void run() {
        picocli.CommandLine.usage(this, System.out);
    }

    @Command(name = "create",
            description = "Create a repository and install the bundled RKG ruleset."
    )
    static final class Create implements Callable<Integer> {
        @ParentCommand
        private RepoCommand parent;
        @Parameters(index = "0", description = "Repository name.")
        private String name;

        @Override
        public Integer call() {
            parent.context().connector().createRepository(name);
            System.out.println("Created repository '" + name + "' with the bundled RKG ruleset installed.");
            return ExitCodes.OK;
        }
    }

    @Command(name = "list", description = "List repositories on the configured GraphDB endpoint.")
    static final class List implements Callable<Integer> {
        @ParentCommand
        private RepoCommand parent;

        @Override
        public Integer call() {
            RkgContext context = parent.context();
            for (String repoName : context.connector().listRepositories()) {
                var state = context.repoStateStore().get(context.endpointUrl(), repoName);
                String chased = state.map(s -> s.chased() ? "chased" : "stale").orElse("unknown");
                String definite = state.flatMap(s -> s.definiteOptional())
                        .map(d -> d ? "definite" : "indefinite")
                        .orElse("not validated");
                System.out.printf("%-30s %-8s %s%n", repoName, chased, definite);
            }
            return ExitCodes.OK;
        }
    }

    @Command(name = "delete", description = "Delete a repository.")
    static final class Delete implements Callable<Integer> {
        @ParentCommand
        private RepoCommand parent;
        @Parameters(index = "0", description = "Repository name.")
        private String name;

        @Override
        public Integer call() {
            parent.context().connector().deleteRepository(name);
            System.out.println("Deleted repository '" + name + "'.");
            return ExitCodes.OK;
        }
    }
}
