package org.rkg.chase;

import org.rkg.validation.ValidationReport;

/**
 * Thrown when {@code rkg chase} is invoked on a repository that fails the Proposition 5
 * definiteness check. Not a bug: an indefinite graph is an expected outcome within Phase 1's
 * scope boundary (§4.4) — the CLI catches this and prints the offending elements rather than a
 * stack trace.
 */
public final class IndefiniteGraphException extends RuntimeException {

    /** Validation report indicating which classes and properties are indefinite. */
    private final ValidationReport validationReport;

    /**
     * Creates an exception for a repository that failed the definiteness check.
     *
     * @param repoName repository name
     * @param validationReport validation report (isDefinite = false)
     */
    public IndefiniteGraphException(String repoName, ValidationReport validationReport) {
        super("Repository '" + repoName + "' is not a definite RKG; chase aborted. Indefinite classes: "
                + validationReport.indefiniteClasses() + ", indefinite properties: "
                + validationReport.indefiniteProperties());
        this.validationReport = validationReport;
    }

    /**
     * Returns the validation report.
     *
     * @return validation report
     */
    public ValidationReport validationReport() {
        return validationReport;
    }
}
