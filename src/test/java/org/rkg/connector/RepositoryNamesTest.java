package org.rkg.connector;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class RepositoryNamesTest {

    @Test
    void acceptsGraphDbSafeRepositoryNames() {
        assertDoesNotThrow(() -> RepositoryNames.requireValid("rkg_1.production-test"));
    }

    @Test
    void rejectsNamesThatCanCorruptAUrlOrTurtleConfig() {
        assertThrows(IllegalArgumentException.class, () -> RepositoryNames.requireValid("repo/other"));
        assertThrows(IllegalArgumentException.class, () -> RepositoryNames.requireValid("repo\" ; injected"));
        assertThrows(IllegalArgumentException.class, () -> RepositoryNames.requireValid("1-starts-with-digit"));
    }
}
