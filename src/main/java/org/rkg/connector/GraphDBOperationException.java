package org.rkg.connector;

/**
 * Normalized exception thrown by every {@link GraphDBConnector} method. Wraps the
 * underlying RDF4J/GraphDB failure into a single exception type carrying an
 * {@link ErrorCategory}, the original message, and an HTTP status code when applicable,
 * per §4.4 of the software design document.
 */
public class GraphDBOperationException extends RuntimeException {

    /** Error categories used by the CLI to select an exit code (§4.4). */
    public enum ErrorCategory {
        /** Could not reach the GraphDB endpoint at all (network/refused/timeout). */
        CONNECTION,
        /** The SPARQL query/update string was rejected by GraphDB as malformed. */
        MALFORMED_QUERY,
        /** The named repository does not exist on the target endpoint. */
        REPO_NOT_FOUND,
        /** GraphDB returned an unexpected server-side error (5xx or internal fault). */
        SERVER_ERROR
    }

    /** Error category of this exception. */
    private final ErrorCategory category;
    /** HTTP status code (if applicable); null if failure was at connection level. */
    private final Integer httpStatus;

    /**
     * Creates an exception with category, message, status, and cause.
     *
     * @param category error category
     * @param message  error message
     * @param httpStatus HTTP status code, or null if not from HTTP
     * @param cause    underlying throwable
     */
    public GraphDBOperationException(ErrorCategory category, String message, Integer httpStatus, Throwable cause) {
        super(message, cause);
        this.category = category;
        this.httpStatus = httpStatus;
    }

    /**
     * Creates an exception with category, message, and status (no cause).
     *
     * @param category error category
     * @param message  error message
     * @param httpStatus HTTP status code, or null if not from HTTP
     */
    public GraphDBOperationException(ErrorCategory category, String message, Integer httpStatus) {
        this(category, message, httpStatus, null);
    }

    /**
     * Returns the error category of this exception.
     *
     * @return error category
     */
    public ErrorCategory category() {
        return category;
    }

    /**
     * Returns the HTTP status code returned by GraphDB, if the failure occurred at the protocol level.
     *
     * @return HTTP status code, or null if not from HTTP
     */
    public Integer httpStatus() {
        return httpStatus;
    }
}
