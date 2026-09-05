package org.rkg.chase;

/**
 * Runs the fixed, non-iterative 3-phase chase pipeline (§5.3 of the software design document):
 * (1) rely on GraphDB's native rules 1-21 closure, (2) batch Skolem witness generation for rules
 * 22/23, (3) a final closure pass. Requires the repository to already be validated as a definite
 * RKG ({@link org.rkg.validation.ValidationReport#isDefinite()} {@code == true}) — Phase 1 scope.
 */
public interface ChaseOrchestrator {

    /**
     * Runs the 3-phase chase pipeline on a repository.
     *
     * @param repoName repository to chase
     * @return per-phase triple counts
     * @throws IndefiniteGraphException if the repository fails the Proposition 5 definiteness check
     *                                   (expected outcome within Phase 1 scope boundary, not an error)
     */
    ChaseResult runChase(String repoName);
}
