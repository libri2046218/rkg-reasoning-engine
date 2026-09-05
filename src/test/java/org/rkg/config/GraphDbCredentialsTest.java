package org.rkg.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class GraphDbCredentialsTest {

    @Test
    void producesBasicAuthorizationHeader() {
        GraphDbCredentials credentials = new GraphDbCredentials("alice", "secret", null);

        assertEquals("Basic YWxpY2U6c2VjcmV0", credentials.authorizationHeader().orElseThrow());
    }

    @Test
    void producesBearerAuthorizationHeader() {
        GraphDbCredentials credentials = new GraphDbCredentials(null, null, "token-value");

        assertEquals("Bearer token-value", credentials.authorizationHeader().orElseThrow());
    }

    @Test
    void exposesNoHeaderWithoutCredentials() {
        assertTrue(new GraphDbCredentials(null, null, null).authorizationHeader().isEmpty());
    }
}
