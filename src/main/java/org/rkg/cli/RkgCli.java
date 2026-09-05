package org.rkg.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import org.rkg.config.RkgContext;

/**
 * Top-level {@code rkg} command dispatcher (§2.3, §6.1 of the software design document). Each
 * subcommand is tagged Tier A (thin GraphDB passthrough) or Tier B (RKG-specific logic) in its own
 * class-level Javadoc; {@code rkg query} is the sole flag-dependent-tier exception (§2.1).
 */
@Command(
        name = "rkg",
        mixinStandardHelpOptions = true,
        version = "rkg 0.1.0",
        description = "Middleware between the user and GraphDB, adding RKG Metamodeling Semantics.",
        subcommands = {
                RepoCommand.class,
                DataCommand.class,
                QueryCommand.class,
                ValidateCommand.class,
                ChaseCommand.class
        }
)
public final class RkgCli implements Runnable {

    @Option(names = "--endpoint", scope = CommandLine.ScopeType.INHERIT,
            description = "GraphDB endpoint URL (default: $RKG_ENDPOINT env var, or http://localhost:7200).")
    String endpoint;

    /** Default constructor. */
    public RkgCli() {
    }

    @Override
    public void run() {
        new CommandLine(this).usage(System.out);
    }

    RkgContext context() {
        return new RkgContext(endpoint);
    }

    /**
     * Entry point for the RKG middleware CLI.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        RkgCli root = new RkgCli();
        int exitCode = new CommandLine(root)
                .setExecutionExceptionHandler(new CliExceptionHandler())
                .execute(args);
        System.exit(exitCode);
    }
}
