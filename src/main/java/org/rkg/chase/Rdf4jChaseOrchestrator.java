package org.rkg.chase;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
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

    private static final SimpleValueFactory VALUES = SimpleValueFactory.getInstance();
    private static final IRI RDF_TYPE = VALUES.createIRI(ChaseQueries.RDF_TYPE);

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

        // A repository made stale by a mutation may contain witnesses no longer supported by its
        // base graph. Rebuild the reserved graph before deriving the current witness set.
        boolean alreadyChased = repoStateStore.get(endpointUrl, repoName)
                .map(org.rkg.repostate.RepoState::chased)
                .orElse(false);
        if (!alreadyChased) {
            connector.update(repoName, "CLEAR GRAPH <urn:rkg:witnesses>");
        }

        // Phase 2: batch Skolem witness generation (rules 22/23).
        List<WitnessTriple> witnessTriples = new ArrayList<>();
        for (var row : select(repoName, ChaseQueries.populatedClasses())) {
            Value classTerm = row.get("a");
            String witness = SkolemNaming.classWitness(classTerm);
            IRI witnessIri = VALUES.createIRI(witness);
            boolean exists = ask(repoName, ChaseQueries.classWitnessExists(),
                    Map.of("witness", witnessIri, "classTerm", classTerm));
            if (!exists) {
                witnessTriples.add(new WitnessTriple(witnessIri, RDF_TYPE, classTerm));
            }
        }
        for (var row : select(repoName, ChaseQueries.populatedProperties())) {
            Value propertyTerm = row.get("p");
            IRI sourceWitness = VALUES.createIRI(SkolemNaming.propertySourceWitness(propertyTerm));
            IRI targetWitness = VALUES.createIRI(SkolemNaming.propertyTargetWitness(propertyTerm));
            boolean exists = ask(repoName, ChaseQueries.propertyWitnessExists(),
                    Map.of("source", sourceWitness, "propertyTerm", propertyTerm, "target", targetWitness));
            if (!exists) {
                witnessTriples.add(new WitnessTriple(sourceWitness, propertyTerm, targetWitness));
            }
        }

        int phase2Count = witnessTriples.size();
        if (!witnessTriples.isEmpty()) {
            connector.update(repoName, ChaseQueries.insertWitnessesUpdate(witnessTriples.size()),
                    witnessBindings(witnessTriples));
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

    private List<Map<String, Value>> select(String repoName, String query) {
        QueryResult result = connector.query(repoName, query, true, List.of());
        if (result.valueRows().size() != result.rows().size()) {
            throw new IllegalStateException("Chase SELECT results must retain RDF4J binding values");
        }
        return result.valueRows();
    }

    private boolean ask(String repoName, String query, Map<String, Value> bindings) {
        return Boolean.TRUE.equals(connector.query(repoName, query, true, List.of(), bindings).askResult());
    }

    private static Map<String, Value> witnessBindings(List<WitnessTriple> witnessTriples) {
        Map<String, Value> bindings = new LinkedHashMap<>();
        for (int index = 0; index < witnessTriples.size(); index++) {
            WitnessTriple triple = witnessTriples.get(index);
            bindings.put("subject" + index, triple.subject());
            bindings.put("predicate" + index, triple.predicate());
            bindings.put("object" + index, triple.object());
        }
        return bindings;
    }

    private record WitnessTriple(Value subject, Value predicate, Value object) {
    }
}
