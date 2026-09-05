package org.rkg.chase;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.rkg.connector.GraphDBConnector;
import org.rkg.connector.QueryResult;
import org.rkg.repostate.RepoStateStore;
import org.rkg.validation.DefinitenessValidator;
import org.rkg.validation.ValidationReport;

/**
 * {@link ChaseOrchestrator} implementation running the fixed, non-iterative 3-phase pipeline
 * (§5.3): (1) GraphDB's native rules 1-21 closure (already materialized, nothing to trigger
 * explicitly), (2) batch Skolem witness generation for rules 22/23, (3) a final closure pass
 * (GraphDB re-materializes rules 1-21 over the newly-inserted witnesses as part of the same
 * commit). No fixpoint loop is required: see §5.3 for the correctness argument.
 */
public final class Rdf4jChaseOrchestrator implements ChaseOrchestrator {

    private final GraphDBConnector connector;
    private final DefinitenessValidator validator;
    private final RepoStateStore repoStateStore;
    private final String endpointUrl;

    /**
     * Creates a chase orchestrator. {@link #runChase(String)} will first validate the repository
     * as a definite RKG; if validation fails, throws {@code IndefiniteGraphException} rather than
     * attempting the chase. Precondition: repository must already be created.
     *
     * @param connector used to execute queries, updates, and measure triple counts
     * @param validator runs Proposition 5 definiteness check before chase begins
     * @param repoStateStore tracks staleness and chase completion timestamp
     * @param endpointUrl the GraphDB endpoint this orchestra manages repositories on
     */
    public Rdf4jChaseOrchestrator(GraphDBConnector connector, DefinitenessValidator validator,
                                   RepoStateStore repoStateStore, String endpointUrl) {
        this.connector = connector;
        this.validator = validator;
        this.repoStateStore = repoStateStore;
        this.endpointUrl = endpointUrl;
    }

    @Override
    public ChaseResult runChase(String repoName) {
        ValidationReport validation = validator.validate(repoName);
        if (!validation.isDefinite()) {
            throw new IndefiniteGraphException(repoName, validation);
        }

        // Phase 1: rules 1-21 closure is already materialized natively by GraphDB on every commit
        // (the bundled ruleset is installed at repository-creation time); we only need its size as
        // the baseline for reporting.
        int phase1Count = countDefaultGraphTriples(repoName);

        // Phase 2: batch Skolem witness generation (rules 22/23).
        List<String> witnessTriples = new ArrayList<>();
        for (var row : select(repoName, ChaseQueries.populatedClasses())) {
            String classIri = row.get("a");
            boolean isBlank = "true".equals(row.get("isBlank"));
            String witness = SkolemNaming.classWitness(classIri, isBlank);
            boolean exists = ask(repoName, ChaseQueries.classWitnessExists(classIri, witness));
            if (!exists) {
                witnessTriples.add(ChaseQueries.classWitnessTriple(witness, classIri));
            }
        }
        for (var row : select(repoName, ChaseQueries.populatedProperties())) {
            String propertyIri = row.get("p");
            boolean isBlank = "true".equals(row.get("isBlank"));
            String sourceWitness = SkolemNaming.propertySourceWitness(propertyIri, isBlank);
            String targetWitness = SkolemNaming.propertyTargetWitness(propertyIri, isBlank);
            boolean exists = ask(repoName, ChaseQueries.propertyWitnessExists(propertyIri, sourceWitness, targetWitness));
            if (!exists) {
                witnessTriples.add(ChaseQueries.propertyWitnessTriple(sourceWitness, propertyIri, targetWitness));
            }
        }

        int phase2Count = witnessTriples.size();
        if (!witnessTriples.isEmpty()) {
            connector.update(repoName, ChaseQueries.insertWitnessesUpdate(witnessTriples));
        }

        // Phase 3: final closure pass. GraphDB materializes rules 1-21 over the newly inserted
        // witnesses as part of the same commit that performed the Phase 2 INSERT DATA above, so
        // by the time control returns here the closure is already up to date; we only measure it.
        int phase3Count = countDefaultGraphTriples(repoName);

        repoStateStore.markChased(endpointUrl, repoName, Instant.now());
        return new ChaseResult(phase1Count, phase2Count, phase3Count);
    }

    private int countDefaultGraphTriples(String repoName) {
        QueryResult result = connector.query(repoName, ChaseQueries.defaultGraphTripleCount(), true, List.of());
        String count = result.rows().get(0).get("c");
        return Integer.parseInt(count);
    }

    private List<java.util.Map<String, String>> select(String repoName, String query) {
        return connector.query(repoName, query, true, List.of()).rows();
    }

    private boolean ask(String repoName, String query) {
        return Boolean.TRUE.equals(connector.query(repoName, query, true, List.of()).askResult());
    }
}
