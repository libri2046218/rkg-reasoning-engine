package org.rkg.cli;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

@Tag("unit")
class RkgCliCommandSurfaceTest {

    @Test
    void exposesTierAAdministrationCommandsAtRoot() {
        StringWriter output = new StringWriter();
        CommandLine commandLine = new CommandLine(new RkgCli());
        commandLine.setOut(new PrintWriter(output));

        int exitCode = commandLine.execute("--help");

        assertTrue(exitCode == ExitCodes.OK);
        assertTrue(output.toString().contains("update"));
        assertTrue(output.toString().contains("backup"));
        assertTrue(output.toString().contains("restore"));
    }

    @Test
    void parsesNewCommandInputsWithoutExecutingThem() {
        CommandLine commandLine = new CommandLine(new RkgCli());

        assertDoesNotThrow(() -> commandLine.parseArgs(
                "update", "--repo", "people", "INSERT DATA { <urn:s> <urn:p> <urn:o> }"));
        assertDoesNotThrow(() -> commandLine.parseArgs("backup", "--repo", "people", "--file", "backup.nq"));
        assertDoesNotThrow(() -> commandLine.parseArgs("restore", "--repo", "people", "--file", "backup.nq"));
    }

    @Test
    void queryMachineReadableFormatsAreMutuallyExclusive() {
        CommandLine commandLine = new CommandLine(new RkgCli());

        assertDoesNotThrow(() -> commandLine.parseArgs(
                "query", "--repo", "people", "--json", "SELECT * WHERE { ?s ?p ?o }"));
        assertDoesNotThrow(() -> commandLine.parseArgs(
                "query", "--repo", "people", "--csv", "SELECT * WHERE { ?s ?p ?o }"));
    }

    @Test
    void rejectsCombiningQueryMachineReadableFormatsBeforeContactingGraphDb() {
        CommandLine commandLine = new CommandLine(new RkgCli())
                .setExecutionExceptionHandler(new CliExceptionHandler());

        assertEquals(ExitCodes.USER_ERROR, commandLine.execute(
                "query", "--repo", "people", "--json", "--csv", "SELECT * WHERE { ?s ?p ?o }"));
    }
}
