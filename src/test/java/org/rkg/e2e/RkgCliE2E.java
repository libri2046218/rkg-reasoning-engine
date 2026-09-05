package org.rkg.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.rkg.integration.GraphDbITSupport;

@Tag("e2e")
class RkgCliE2E extends GraphDbITSupport {

    @Test
    void importsValidatesChasesAndQueriesADefiniteFixture() throws Exception {
        String repository = newRepository("definite");
        try {
            assertCommand(0, "Created repository '" + repository + "'", "repo", "create", repository);
            assertCommand(0, "Imported", "data", "import", "--repo", repository,
                    "--file", fixturePath("definite-example.ttl").toString());
            assertCommand(0, "is a definite RKG", "validate", "--repo", repository);
            assertCommand(0, "Chase complete", "chase", "--repo", repository);
            assertCommand(0, "urn:rkg:witness:", "query", "--repo", repository,
                    "--file", fixturePath("person-query.sparql").toString());
            assertRepositoryIsChased(repository);
        } finally {
            deleteViaCli(repository);
        }
    }

    @Test
    void refusesDefaultQueriesAfterAMutation() throws Exception {
        String repository = newRepository("stale");
        try {
            assertCommand(0, "Created repository", "repo", "create", repository);
            assertCommand(0, "Imported", "data", "import", "--repo", repository,
                    "--file", fixturePath("definite-example.ttl").toString());
            assertCommand(0, "Chase complete", "chase", "--repo", repository);
            assertCommand(0, "Imported", "data", "import", "--repo", repository,
                    "--file", fixturePath("definite-example.ttl").toString());
            assertCommand(1, "has not been chased since its last mutation", "query", "--repo", repository,
                    "--file", fixturePath("person-query.sparql").toString());
        } finally {
            deleteViaCli(repository);
        }
    }

    @Test
    void reportsIndefinitenessAndRefusesChase() throws Exception {
        String repository = newRepository("indefinite");
        try {
            assertCommand(0, "Created repository", "repo", "create", repository);
            assertCommand(0, "Imported", "data", "import", "--repo", repository,
                    "--file", fixturePath("indefinite-example.ttl").toString());
            assertCommand(0, "is NOT a definite RKG", "validate", "--repo", repository);
            assertCommand(1, "is not a definite RKG; chase aborted", "chase", "--repo", repository);
        } finally {
            deleteViaCli(repository);
        }
    }

    private String newRepository(String scenario) {
        String repository = "rkg-e2e-" + scenario + "-" + UUID.randomUUID();
        trackRepository(repository);
        return repository;
    }

    private void deleteViaCli(String repository) throws IOException, InterruptedException {
        CommandResult result = run("repo", "delete", repository);
        if (result.exitCode == 0) {
            untrackRepository(repository);
        }
    }

    private void assertCommand(int expectedExitCode, String expectedOutput, String... arguments)
            throws IOException, InterruptedException {
        CommandResult result = run(arguments);
        assertEquals(expectedExitCode, result.exitCode, result.output);
        assertTrue(result.output.contains(expectedOutput), result.output);
    }

    private CommandResult run(String... arguments) throws IOException, InterruptedException {
        Path projectDirectory = Path.of(System.getProperty("rkg.project.directory"));
        List<String> command = new ArrayList<>();
        command.add(projectDirectory.resolve("gradlew").toString());
        command.add("--no-daemon");
        command.add("--console=plain");
        command.add("run");
        command.add("--args=" + String.join(" ", List.of(arguments)));
        ProcessBuilder processBuilder = new ProcessBuilder(command)
                .directory(projectDirectory.toFile())
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.PIPE);
        processBuilder.environment().put("RKG_ENDPOINT", endpointUrl);
        Process process = processBuilder.start();
        String output = new String(process.getInputStream().readAllBytes());
        return new CommandResult(process.waitFor(), output);
    }

    private void assertRepositoryIsChased(String repository) throws IOException, InterruptedException {
        CommandResult result = run("repo", "list");
        assertEquals(0, result.exitCode, result.output);
        assertTrue(result.output.contains(repository), result.output);
        assertTrue(result.output.contains("chased"), result.output);
    }

    private Path fixturePath(String fileName) {
        return Path.of(System.getProperty("rkg.project.directory"), "src", "test", "resources", "fixtures", "e2e",
                fileName);
    }

    private record CommandResult(int exitCode, String output) {
    }
}
