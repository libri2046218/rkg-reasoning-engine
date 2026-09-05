package org.rkg.config;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/**
 * Credentials used to authenticate requests to a GraphDB endpoint.
 *
 * <p>Credentials are read only from the environment. Set either {@code RKG_USERNAME} and
 * {@code RKG_PASSWORD} for HTTP Basic authentication, or {@code RKG_TOKEN} for bearer-token
 * authentication. The two modes are mutually exclusive.
 *
 * @param username HTTP Basic username, or null
 * @param password HTTP Basic password, or null
 * @param token bearer token, or null
 */
public record GraphDbCredentials(String username, String password, String token) {

    /**
     * Resolves GraphDB credentials from the process environment.
     *
     * @return configured credentials, or an unauthenticated credential set when no variables are set
     * @throws IllegalArgumentException if credential variables describe an incomplete or ambiguous mode
     */
    public static GraphDbCredentials fromEnvironment() {
        String username = System.getenv("RKG_USERNAME");
        String password = System.getenv("RKG_PASSWORD");
        String token = System.getenv("RKG_TOKEN");
        boolean hasUsername = username != null && !username.isBlank();
        boolean hasPassword = password != null && !password.isBlank();
        boolean hasToken = token != null && !token.isBlank();
        if (hasUsername != hasPassword || hasToken && (hasUsername || hasPassword)) {
            throw new IllegalArgumentException(
                    "Set either both RKG_USERNAME and RKG_PASSWORD, or RKG_TOKEN, but not both authentication modes.");
        }
        return new GraphDbCredentials(hasUsername ? username : null, hasPassword ? password : null,
                hasToken ? token : null);
    }

    /**
     * Returns the HTTP Authorization header value, without exposing it through diagnostics.
     *
     * @return authorization header value when credentials are configured
     */
    public Optional<String> authorizationHeader() {
        if (token != null) {
            return Optional.of("Bearer " + token);
        }
        if (username != null) {
            String value = username + ":" + password;
            return Optional.of("Basic " + Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8)));
        }
        return Optional.empty();
    }
}
