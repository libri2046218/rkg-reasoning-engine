package org.rkg.cli;

import org.rkg.connector.GraphDBOperationException;

/** Exit code scheme shared by every command, per §4.4 of the software design document. */
final class ExitCodes {

    static final int OK = 0;
    static final int USER_ERROR = 1;
    static final int CONNECTIVITY_ERROR = 2;
    static final int SERVER_ERROR = 3;

    private ExitCodes() {
    }

    static int forGraphDBError(GraphDBOperationException e) {
        return switch (e.category()) {
            case MALFORMED_QUERY, REPO_NOT_FOUND -> USER_ERROR;
            case CONNECTION -> CONNECTIVITY_ERROR;
            case SERVER_ERROR -> SERVER_ERROR;
        };
    }
}
