package org.rkg.chase;

/**
 * Per-phase triple counts for one {@code rkg chase} run, surfaced via {@code --explain} (§4.2,
 * §6.4 of the software design document).
 *
 * @param phase1ClosureTripleCount total triples in the default graph after GraphDB's native
 *                                 rules 1-21 closure, before any witness is minted
 * @param phase2WitnessTripleCount number of new Skolem witness triples inserted into
 *                                 {@code urn:rkg:witnesses} in the batch INSERT DATA
 * @param phase3ClosureTripleCount total triples in the default graph after GraphDB re-runs rules
 *                                 1-21 over the newly inserted witnesses (Phase 3)
 */
public record ChaseResult(
        int phase1ClosureTripleCount,
        int phase2WitnessTripleCount,
        int phase3ClosureTripleCount
) {
    /**
     * Net new triples derived in Phase 3 as a consequence of the Phase 2 witnesses.
     *
     * @return {@code phase3ClosureTripleCount - phase1ClosureTripleCount}
     */
    public int phase3NewTripleCount() {
        return phase3ClosureTripleCount - phase1ClosureTripleCount;
    }
}
