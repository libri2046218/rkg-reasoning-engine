package org.rkg.validation;

/**
 * Decides, in polynomial time, whether every class and property in a repository is <b>definite</b>
 * (populated or bottom), per Proposition 5 of Delfino, Lenzerini &amp; Poggi (ECAI 2025) &#8212; &#167;5.2 of
 * the software design document.
 */
public interface DefinitenessValidator {

    /**
     * Runs the populated/bottom check (Proposition 5) over the repository's current chase state.
     *
     * @param repoName repository to validate
     * @return validation report (definite/indefinite, list of indefinite elements)
     */
    ValidationReport validate(String repoName);
}
