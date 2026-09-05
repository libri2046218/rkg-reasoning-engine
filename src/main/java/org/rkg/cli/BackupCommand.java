package org.rkg.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

/** {@code rkg backup} — Tier A repository backup export. */
@Command(name = "backup", mixinStandardHelpOptions = true, description = "Write a complete N-Quads repository backup.")
public final class BackupCommand implements Callable<Integer> {

    @ParentCommand
    private RkgCli parent;

    /** Creates the backup command. */
    public BackupCommand() {
    }

    @Option(names = "--repo", required = true, description = "Repository name.")
    private String repo;

    @Option(names = "--file", required = true, description = "Output N-Quads backup file.")
    private Path file;

    @Override
    public Integer call() {
        try (var out = Files.newOutputStream(file)) {
            parent.context().connector().backup(repo, out);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not write backup file '" + file + "': " + e.getMessage(), e);
        }
        System.out.println("Backed up repository '" + repo + "' to '" + file + "'.");
        return ExitCodes.OK;
    }
}
