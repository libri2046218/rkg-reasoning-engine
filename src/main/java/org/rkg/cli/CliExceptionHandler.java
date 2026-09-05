package org.rkg.cli;

import org.rkg.chase.IndefiniteGraphException;
import org.rkg.connector.GraphDBOperationException;
import org.rkg.query.StaleRepositoryException;
import picocli.CommandLine;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.ParseResult;

/**
 * Normalizes exceptions thrown by any command into the exit code scheme documented in §4.4:
 * {@code 1} = user error, {@code 2} = connectivity error, {@code 3} = server error. Tier B
 * "expected outcome" conditions (indefinite graph, staleness) are printed as plain warnings
 * without a stack trace, per §4.4's distinction between failures and expected scope-boundary
 * outcomes.
 */
final class CliExceptionHandler implements IExecutionExceptionHandler {

    @Override
    public int handleExecutionException(Exception ex, CommandLine commandLine, ParseResult parseResult) {
        if (ex instanceof GraphDBOperationException graphDbEx) {
            commandLine.getErr().println("Error: " + graphDbEx.getMessage());
            return ExitCodes.forGraphDBError(graphDbEx);
        }
        if (ex instanceof IndefiniteGraphException || ex instanceof StaleRepositoryException) {
            commandLine.getErr().println("Warning: " + ex.getMessage());
            return ExitCodes.USER_ERROR;
        }
        if (ex instanceof IllegalArgumentException || ex instanceof UnsupportedOperationException) {
            commandLine.getErr().println("Error: " + ex.getMessage());
            return ExitCodes.USER_ERROR;
        }
        commandLine.getErr().println("Unexpected error: " + ex);
        return ExitCodes.SERVER_ERROR;
    }
}
