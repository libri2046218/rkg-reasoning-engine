package org.rkg.validation;

import java.util.List;

/**
 * Result of a {@link DefinitenessValidator#validate} run, per §4.2/§5.2 of the software design
 * document: {@code ValidationReport { boolean isDefinite; List<IRI> indefiniteClasses; List<IRI> indefiniteProperties }}.
 *
 * @param isDefinite whether all classes and properties in the repository are definite
 * @param indefiniteClasses list of indefinite class IRIs (empty if definite)
 * @param indefiniteProperties list of indefinite property IRIs (empty if definite)
 */
public record ValidationReport(
        boolean isDefinite,
        List<String> indefiniteClasses,
        List<String> indefiniteProperties
) {
    /**
     * Creates a report indicating the repository is definite.
     *
     * @return definite ValidationReport
     */
    public static ValidationReport definite() {
        return new ValidationReport(true, List.of(), List.of());
    }

    /**
     * Creates a report indicating the repository is indefinite.
     *
     * @param indefiniteClasses list of indefinite class IRIs
     * @param indefiniteProperties list of indefinite property IRIs
     * @return indefinite ValidationReport
     */
    public static ValidationReport indefinite(List<String> indefiniteClasses, List<String> indefiniteProperties) {
        return new ValidationReport(false, indefiniteClasses, indefiniteProperties);
    }
}
