package org.rkg.cli;

import java.util.concurrent.Callable;
import org.rkg.chase.ChaseResult;
import org.rkg.config.RkgContext;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

/**
 * {@code rkg chase} — Tier B fixed 3-phase chase pipeline (§6.1, §5.3). Aborts with a clear
 * message (via {@code IndefiniteGraphException}) if the repository is not a definite RKG, rather
 * than throwing an opaque failure — this is an expected Phase 1 scope-boundary outcome (§4.4).
 */
@Command(name = "chase", description = "Run the 3-phase chase pipeline (closure -> witnesses -> closure).")
public final class ChaseCommand implements Callable<Integer> {

    @ParentCommand
    private RkgCli parent;

    @Option(names = "--repo", required = true, description = "Repository name.")
    private String repo;

    @Option(names = {"--verbose", "--explain"}, description = "Print per-phase triple counts and timings.")
    private boolean explain;

    /** Default constructor. */
    public ChaseCommand() {
    }

    @Override
    public Integer call() {
        RkgContext context = parent.context();
        long start = System.nanoTime();
        ChaseResult result = context.chaseOrchestrator().runChase(repo);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        System.out.println("Chase complete for repository '" + repo + "'.");
        if (explain) {
            System.out.println("  Phase 1 (native rules 1-21 closure) triple count: " + result.phase1ClosureTripleCount());
            System.out.println("  Phase 2 (Skolem witnesses inserted):              " + result.phase2WitnessTripleCount());
            System.out.println("  Phase 3 (final closure) triple count:             " + result.phase3ClosureTripleCount()
                    + " (+" + result.phase3NewTripleCount() + " new)");
            System.out.println("  Elapsed: " + elapsedMs + " ms");
        }
        return ExitCodes.OK;
    }
}
