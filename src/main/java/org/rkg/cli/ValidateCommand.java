package org.rkg.cli;

import java.util.concurrent.Callable;
import org.rkg.config.RkgContext;
import org.rkg.validation.ValidationReport;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

/**
 * {@code rkg validate} — Tier B definiteness check (§6.1): runs the Proposition 5 populated/bottom
 * check and prints a report. An indefinite result is an expected Phase 1 scope-boundary outcome,
 * not an error (§4.4).
 */
@Command(name = "validate", description = "Run the Proposition 5 definiteness check and print a report.")
public final class ValidateCommand implements Callable<Integer> {

    @ParentCommand
    private RkgCli parent;

    @Option(names = "--repo", required = true, description = "Repository name.")
    private String repo;

    /** Default constructor. */
    public ValidateCommand() {
    }

    @Override
    public Integer call() {
        RkgContext context = parent.context();
        ValidationReport report = context.validator().validate(repo);
        if (report.isDefinite()) {
            System.out.println("Repository '" + repo + "' is a definite RKG.");
        } else {
            System.out.println("Repository '" + repo + "' is NOT a definite RKG.");
            System.out.println("Indefinite classes:    " + report.indefiniteClasses());
            System.out.println("Indefinite properties: " + report.indefiniteProperties());
        }
        return ExitCodes.OK;
    }
}
