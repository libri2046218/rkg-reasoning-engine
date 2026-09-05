package org.rkg.connector;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Thin wrapper around GraphDB's repository-management REST API (distinct from the standard
 * RDF4J/SPARQL protocol used for query/update, which is handled separately by
 * {@link Rdf4jGraphDBConnector} via RDF4J's own HTTP client). Responsible for repository
 * lifecycle (create/list/delete) and for installing the single bundled ruleset at repository
 * creation time, per §4.1/§7.2 of the software design document.
 */
final class GraphDBRestClient {

    /** Resource name (classpath, under {@code src/main/resources}) of the bundled ruleset. */
    private static final String BUNDLED_RULESET_RESOURCE = "/rules/chase-rules.pie";
    /**
     * Identifier GraphDB stores the uploaded ruleset under; referenced by every repository config.
     * This is the fixed path inside the GraphDB Docker container where all rulesets are mounted.
     * Not configurable; part of the stable deployment contract with GraphDB 11.5+.
     */
    private static final String RULESET_ID = "/opt/graphdb/rules/chase-rules.pie";

    private final String endpointUrl;
    private final HttpClient httpClient;

    /**
     * Creates a REST client, normalizing the endpoint URL. Strips trailing "/" from the input
     * to ensure consistent endpoint formation in subsequent REST calls; this normalization is
     * idempotent and required for URI construction.
     *
     * @param endpointUrl GraphDB REST endpoint (e.g., "http://localhost:7200" or "http://localhost:7200/")
     */
    GraphDBRestClient(String endpointUrl) {
        this.endpointUrl = endpointUrl.endsWith("/") ? endpointUrl.substring(0, endpointUrl.length() - 1) : endpointUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    String endpointUrl() {
        return endpointUrl;
    }

    void createRepository(String repoName) {

        String configTurtle = repositoryConfigTurtle(repoName);
        Multipart multipart = new Multipart();
        multipart.addFormField("config", configTurtle, "text/turtle", "config.ttl");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpointUrl + "/rest/repositories"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", multipart.contentType())
                .POST(HttpRequest.BodyPublishers.ofByteArray(multipart.build()))
                .build();

        HttpResponse<String> response = send(request);
        if (response.statusCode() / 100 != 2) {
            throw errorFor(response, "createRepository(" + repoName + ")");
        }
    }

    void deleteRepository(String repoName) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpointUrl + "/rest/repositories/" + repoName))
                .timeout(Duration.ofSeconds(30))
                .DELETE()
                .build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() == 404) {
            throw new GraphDBOperationException(GraphDBOperationException.ErrorCategory.REPO_NOT_FOUND,
                    "Repository '" + repoName + "' does not exist", 404);
        }
        if (response.statusCode() / 100 != 2) {
            throw errorFor(response, "deleteRepository(" + repoName + ")");
        }
    }

    List<String> listRepositories() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpointUrl + "/rest/repositories"))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() / 100 != 2) {
            throw errorFor(response, "listRepositories()");
        }
        return parseRepositoryIds(response.body());
    }

    /**
     * Reads the bundled {@code chase-rules.pie} ruleset from the classpath resource
     * ({@link #BUNDLED_RULESET_RESOURCE}). This resource must exist as a UTF-8 encoded Datalog
     * rule file. Throws {@code IllegalStateException} if the resource is missing or cannot be read.
     *
     * @return ruleset content as a string
     */
    private String readBundledRuleset() {
        try (InputStream in = GraphDBRestClient.class.getResourceAsStream(BUNDLED_RULESET_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Bundled ruleset resource not found on classpath: "
                        + BUNDLED_RULESET_RESOURCE);
            }
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read bundled ruleset resource", e);
        }
    }

    /**
     * Generates a Turtle-formatted repository configuration for GraphDB, bound to the bundled
     * ruleset ({@link #RULESET_ID}). Key settings: disable-sameAs (RDF semantics, not OWL),
     * no inconsistency checks (Datalog only, no disjointness), context index enabled (efficient
     * named-graph queries), in-memory literal properties (for index performance). Configuration
     * is sent to GraphDB's /rest/repositories endpoint during repository creation.
     *
     * @param repoName repository ID (used in the config as both repositoryID and label)
     * @return Turtle-formatted RDF configuration
     */
    private String repositoryConfigTurtle(String repoName) {
        return """
                @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
                @prefix rep: <http://www.openrdf.org/config/repository#> .
                @prefix sr: <http://www.openrdf.org/config/repository/sail#> .
                @prefix sail: <http://www.openrdf.org/config/sail#> .
                @prefix graphdb: <http://www.ontotext.com/config/graphdb#> .

                [] a rep:Repository ;
                    rep:repositoryID "%s" ;
                    rdfs:label "RKG middleware repository (%s)" ;
                    rep:repositoryImpl [
                        rep:repositoryType "graphdb:SailRepository" ;
                        sr:sailImpl [
                            sail:sailType "graphdb:Sail" ;
                            graphdb:ruleset "%s" ;
                            graphdb:disable-sameAs "true" ;
                            graphdb:check-for-inconsistencies "false" ;
                            graphdb:enable-context-index "true" ;
                            graphdb:enablePredicateList "true" ;
                            graphdb:in-memory-literal-properties "true" ;
                            graphdb:enable-literal-index "true";
                        ]
                    ] .
                """.formatted(repoName, repoName, RULESET_ID);
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new GraphDBOperationException(GraphDBOperationException.ErrorCategory.CONNECTION,
                    "Could not reach GraphDB endpoint at " + endpointUrl + ": " + e.getMessage(), null, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GraphDBOperationException(GraphDBOperationException.ErrorCategory.CONNECTION,
                    "Request to GraphDB endpoint was interrupted", null, e);
        }
    }

    private GraphDBOperationException errorFor(HttpResponse<String> response, String operation) {
        int status = response.statusCode();
        GraphDBOperationException.ErrorCategory category = switch (status) {
            case 404 -> GraphDBOperationException.ErrorCategory.REPO_NOT_FOUND;
            case 400, 406 -> GraphDBOperationException.ErrorCategory.MALFORMED_QUERY;
            default -> status / 100 == 5
                    ? GraphDBOperationException.ErrorCategory.SERVER_ERROR
                    : GraphDBOperationException.ErrorCategory.SERVER_ERROR;
        };
        return new GraphDBOperationException(category,
                operation + " failed: HTTP " + status + " - " + response.body(), status);
    }

    private static List<String> parseRepositoryIds(String jsonBody) {
        // GraphDB returns a JSON array of repository descriptor objects; we only need the "id" field
        // and deliberately avoid pulling in a full JSON library dependency for this single use.
        List<String> ids = new ArrayList<>();
        Matcher matcher = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"").matcher(jsonBody);
        while (matcher.find()) {
            ids.add(matcher.group(1));
        }
        return ids;
    }

    /** Minimal multipart/form-data body builder, sufficient for GraphDB's REST endpoints. */
    private static final class Multipart {
        private final String boundary = "----rkg-middleware-" + System.nanoTime();
        private final List<byte[]> parts = new ArrayList<>();

        void addFormField(String name, String value, String contentType, String filename) {
            StringBuilder header = new StringBuilder();
            header.append("--").append(boundary).append("\r\n");
            header.append("Content-Disposition: form-data; name=\"").append(name).append("\"");
            if (filename != null) {
                header.append("; filename=\"").append(filename).append("\"");
            }
            header.append("\r\n");
            header.append("Content-Type: ").append(contentType).append("\r\n\r\n");
            parts.add(header.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            parts.add(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            parts.add("\r\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        byte[] build() {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            for (byte[] part : parts) {
                out.writeBytes(part);
            }
            out.writeBytes(("--" + boundary + "--\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return out.toByteArray();
        }

        String contentType() {
            return "multipart/form-data; boundary=" + boundary;
        }
    }
}
