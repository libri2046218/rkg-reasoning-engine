package org.rkg.validation;

import java.util.ArrayList;
import java.util.List;
import org.rkg.connector.GraphDBConnector;
import org.rkg.connector.QueryResult;
import org.rkg.repostate.RepoStateStore;

/**
 * {@link DefinitenessValidator} implementation running the Proposition 5 populated/bottom check
 * (§5.2) via bounded SPARQL ASK queries issued through {@link GraphDBConnector}, with
 * {@code infer=true} so that the rule 1-21 closure GraphDB already materializes is visible, and no
 * named graphs (witnesses do not exist yet — validation always runs before the chase's witness
 * phase).
 */
public final class Rdf4jDefinitenessValidator implements DefinitenessValidator {

    private final GraphDBConnector connector;
    private final RepoStateStore repoStateStore;
    private final String endpointUrl;

    /**
     * Creates a validator that runs the Proposition 5 populated/bottom check via bounded SPARQL
     * ASK queries. Queries are issued with {@code infer=true} to ensure the rule 1-21 closure
     * is visible; named graphs are not queried (witnesses do not exist yet — validation always
     * runs before the chase's witness phase).
     *
     * @param connector used to execute candidate selection and populated/bottom queries
     * @param repoStateStore used to record validation results after check completes
     * @param endpointUrl the GraphDB endpoint URL (for state recording)
     */
    public Rdf4jDefinitenessValidator(GraphDBConnector connector, RepoStateStore repoStateStore, String endpointUrl) {
        this.connector = connector;
        this.repoStateStore = repoStateStore;
        this.endpointUrl = endpointUrl;
    }

    @Override
    public ValidationReport validate(String repoName) {
        List<String> indefiniteClasses = new ArrayList<>();
        List<String> indefiniteProperties = new ArrayList<>();

        for (String classIri : candidateElements(repoName, DefinitenessQueries.candidateClasses())) {
            boolean populated = ask(repoName, DefinitenessQueries.classPopulated(classIri));
            boolean bottom = !populated && ask(repoName, DefinitenessQueries.classIsBottom(classIri));
            if (!populated && !bottom) {
                indefiniteClasses.add(classIri);
            }
        }

        for (String propertyIri : candidateElements(repoName, DefinitenessQueries.candidateProperties())) {
            boolean populated = ask(repoName, DefinitenessQueries.propertyPopulated(propertyIri));
            boolean subPropertyOfEvery = !populated
                    && ask(repoName, DefinitenessQueries.propertyIsSubPropertyOfEvery(propertyIri));
            boolean universalDomainAndRange = !populated && !subPropertyOfEvery
                    && ask(repoName, DefinitenessQueries.propertyHasUniversalDomain(propertyIri))
                    && ask(repoName, DefinitenessQueries.propertyHasUniversalRange(propertyIri));
            if (!populated && !subPropertyOfEvery && !universalDomainAndRange) {
                indefiniteProperties.add(propertyIri);
            }
        }

        ValidationReport report = (indefiniteClasses.isEmpty() && indefiniteProperties.isEmpty())
                ? ValidationReport.definite()
                : ValidationReport.indefinite(indefiniteClasses, indefiniteProperties);

        List<String> allIndefinite = new ArrayList<>(indefiniteClasses);
        allIndefinite.addAll(indefiniteProperties);
        repoStateStore.recordValidation(endpointUrl, repoName, report.isDefinite(), allIndefinite);
        return report;
    }

    private List<String> candidateElements(String repoName, String selectQuery) {
        QueryResult result = connector.query(repoName, selectQuery, true, List.of());
        List<String> elements = new ArrayList<>();
        for (var row : result.rows()) {
            String value = row.get("a");
            if (value != null) {
                elements.add(value);
            }
        }
        return elements;
    }

    private boolean ask(String repoName, String askQuery) {
        QueryResult result = connector.query(repoName, askQuery, true, List.of());
        return Boolean.TRUE.equals(result.askResult());
    }
}
