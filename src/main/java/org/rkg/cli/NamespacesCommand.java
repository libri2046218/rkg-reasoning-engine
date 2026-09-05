package org.rkg.cli;

import java.util.Map;
import java.util.concurrent.Callable;
import org.rkg.config.RkgContext;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/** {@code rkg namespaces} — Tier A management of repository namespace mappings. */
@Command(name = "namespaces", mixinStandardHelpOptions = true, description = "Manage repository namespace mappings.",
        subcommands = {NamespacesCommand.List.class, NamespacesCommand.Set.class, NamespacesCommand.Remove.class,
                NamespacesCommand.Clear.class})
public final class NamespacesCommand implements Runnable {

    @ParentCommand
    private RkgCli parent;

    /** Creates the namespace-management command. */
    public NamespacesCommand() {
    }

    @Override
    public void run() {
        picocli.CommandLine.usage(this, System.out);
    }

    @Command(name = "list", description = "List namespace mappings.")
    static final class List implements Callable<Integer> {
        @ParentCommand private NamespacesCommand parent;
        @Option(names = "--repo", required = true) private String repo;

        @Override public Integer call() {
            for (Map.Entry<String, String> entry : parent.parent.context().connector().namespaces(repo).entrySet()) {
                System.out.printf("%s\t%s%n", entry.getKey(), entry.getValue());
            }
            return ExitCodes.OK;
        }
    }

    @Command(name = "set", description = "Set a namespace prefix mapping.")
    static final class Set implements Callable<Integer> {
        @ParentCommand private NamespacesCommand parent;
        @Option(names = "--repo", required = true) private String repo;
        @Parameters(index = "0") private String prefix;
        @Parameters(index = "1") private String namespace;

        @Override public Integer call() {
            parent.parent.context().connector().setNamespace(repo, prefix, namespace);
            return ExitCodes.OK;
        }
    }

    @Command(name = "remove", description = "Remove a namespace prefix mapping.")
    static final class Remove implements Callable<Integer> {
        @ParentCommand private NamespacesCommand parent;
        @Option(names = "--repo", required = true) private String repo;
        @Parameters(index = "0") private String prefix;

        @Override public Integer call() {
            parent.parent.context().connector().removeNamespace(repo, prefix);
            return ExitCodes.OK;
        }
    }

    @Command(name = "clear", description = "Remove all namespace prefix mappings.")
    static final class Clear implements Callable<Integer> {
        @ParentCommand private NamespacesCommand parent;
        @Option(names = "--repo", required = true) private String repo;

        @Override public Integer call() {
            parent.parent.context().connector().clearNamespaces(repo);
            return ExitCodes.OK;
        }
    }
}
