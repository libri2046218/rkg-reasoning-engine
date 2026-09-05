package org.rkg.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

/** {@code rkg restore} — Tier A N-Quads repository restore. */
@Command(name = "restore", mixinStandardHelpOptions = true,
        description = "Restore N-Quads backup data and mark the repository stale.")
public final class RestoreCommand implements Callable<Integer> {

    @ParentCommand
    private RkgCli parent;

    /** Creates the restore command. */
    public RestoreCommand() {
    }

    @Option(names = "--repo", required = true, description = "Repository name.")
    private String repo;

    @Option(names = "--file", required = true, description = "Input N-Quads backup file.")
    private Path file;

    @Override
    public Integer call() {
        try (var in = Files.newInputStream(file)) {
            parent.context().connector().restore(repo, in);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read backup file '" + file + "': " + e.getMessage(), e);
        }
        System.out.println("Restored backup '" + file + "' into repository '" + repo + "'.");
        return ExitCodes.OK;
    }
}
