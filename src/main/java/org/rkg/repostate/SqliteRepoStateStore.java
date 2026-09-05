package org.rkg.repostate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQLite-backed {@link RepoStateStore} (§3.2 and §7.2 of the software design document). Uses a
 * single {@code repo_state} table keyed by the composite primary key
 * {@code (endpoint_url, repo_name)}. The composite key supports multi-endpoint scenarios,
 * allowing the middleware to track state across different GraphDB instances simultaneously
 * (each endpoint has its own isolated repository namespace).
 */
public final class SqliteRepoStateStore implements RepoStateStore {

    private final String jdbcUrl;

    /**
     * Creates an SQLite-backed repository state store. Automatically creates the parent directory
     * of {@code databaseFile} if it does not exist; throws {@code IllegalStateException} on I/O
     * failure. The schema (repo_state table) is initialized on first connection.
     *
     * @param databaseFile path to the SQLite database file (e.g., ~/.config/rkg-middleware/state.db)
     * @throws IllegalStateException if directory creation fails or schema initialization fails
     */
    public SqliteRepoStateStore(Path databaseFile) {
        try {
            Files.createDirectories(databaseFile.toAbsolutePath().getParent());
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Could not create state directory for " + databaseFile, e);
        }
        this.jdbcUrl = "jdbc:sqlite:" + databaseFile.toAbsolutePath();
        initSchema();
    }

    /**
     * Default location honoring XDG conventions, per §3.2: {@code ~/.config/rkg-middleware/state.db}.
     *
     * @return path to the default database file
     */
    public static Path defaultDatabasePath() {
        String xdgConfigHome = System.getenv("XDG_CONFIG_HOME");
        Path configHome = (xdgConfigHome != null && !xdgConfigHome.isBlank())
                ? Path.of(xdgConfigHome)
                : Path.of(System.getProperty("user.home"), ".config");
        return configHome.resolve("rkg-middleware").resolve("state.db");
    }

    private void initSchema() {
        String ddl = """
                CREATE TABLE IF NOT EXISTS repo_state (
                    endpoint_url          TEXT NOT NULL,
                    repo_name             TEXT NOT NULL,
                    chased                INTEGER NOT NULL DEFAULT 0,
                    last_chase_timestamp  TEXT,
                    definite              INTEGER,
                    indefinite_elements   TEXT,
                    PRIMARY KEY (endpoint_url, repo_name)
                )
                """;
        try (Connection conn = connect(); var stmt = conn.createStatement()) {
            stmt.execute(ddl);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize repo_state schema", e);
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    @Override
    public void createRepoState(String endpointUrl, String repoName) {
        String sql = """
                INSERT INTO repo_state (endpoint_url, repo_name, chased, last_chase_timestamp, definite, indefinite_elements)
                VALUES (?, ?, 0, NULL, NULL, NULL)
                ON CONFLICT(endpoint_url, repo_name) DO UPDATE SET
                    chased = 0, last_chase_timestamp = NULL, definite = NULL, indefinite_elements = NULL
                """;
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, endpointUrl);
            ps.setString(2, repoName);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create repo_state row for " + repoName, e);
        }
    }

    @Override
    public void deleteRepoState(String endpointUrl, String repoName) {
        String sql = "DELETE FROM repo_state WHERE endpoint_url = ? AND repo_name = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, endpointUrl);
            ps.setString(2, repoName);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete repo_state row for " + repoName, e);
        }
    }

    @Override
    public Optional<RepoState> get(String endpointUrl, String repoName) {
        String sql = "SELECT * FROM repo_state WHERE endpoint_url = ? AND repo_name = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, endpointUrl);
            ps.setString(2, repoName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to read repo_state row for " + repoName, e);
        }
    }

    @Override
    public List<RepoState> listAll() {
        String sql = "SELECT * FROM repo_state ORDER BY endpoint_url, repo_name";
        List<RepoState> result = new ArrayList<>();
        try (Connection conn = connect(); var stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to list repo_state rows", e);
        }
        return result;
    }

    @Override
    public void markStale(String endpointUrl, String repoName) {
        String sql = """
                UPDATE repo_state SET chased = 0
                WHERE endpoint_url = ? AND repo_name = ?
                """;
        executeUpdate(sql, endpointUrl, repoName);
    }

    @Override
    public void markChased(String endpointUrl, String repoName, Instant completedAt) {
        String sql = """
                UPDATE repo_state SET chased = 1, last_chase_timestamp = ?
                WHERE endpoint_url = ? AND repo_name = ?
                """;
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, completedAt.toString());
            ps.setString(2, endpointUrl);
            ps.setString(3, repoName);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to mark repo '" + repoName + "' as chased", e);
        }
    }

    @Override
    public void recordValidation(String endpointUrl, String repoName, boolean definite, List<String> indefiniteElements) {
        String sql = """
                UPDATE repo_state SET definite = ?, indefinite_elements = ?
                WHERE endpoint_url = ? AND repo_name = ?
                """;
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, definite ? 1 : 0);
            ps.setString(2, indefiniteElements.isEmpty() ? null : String.join(",", indefiniteElements));
            ps.setString(3, endpointUrl);
            ps.setString(4, repoName);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to record validation result for '" + repoName + "'", e);
        }
    }

    private void executeUpdate(String sql, String endpointUrl, String repoName) {
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, endpointUrl);
            ps.setString(2, repoName);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update repo_state row for '" + repoName + "'", e);
        }
    }

    private RepoState mapRow(ResultSet rs) throws SQLException {
        String endpointUrl = rs.getString("endpoint_url");
        String repoName = rs.getString("repo_name");
        boolean chased = rs.getInt("chased") != 0;
        String lastChaseTimestampStr = rs.getString("last_chase_timestamp");
        Instant lastChaseTimestamp = lastChaseTimestampStr == null ? null : Instant.parse(lastChaseTimestampStr);
        Object definiteObj = rs.getObject("definite");
        Boolean definite = definiteObj == null ? null : rs.getInt("definite") != 0;
        String indefiniteElementsStr = rs.getString("indefinite_elements");
        List<String> indefiniteElements = (indefiniteElementsStr == null || indefiniteElementsStr.isBlank())
                ? List.of()
                : List.of(indefiniteElementsStr.split(","));
        return new RepoState(endpointUrl, repoName, chased, lastChaseTimestamp, definite, indefiniteElements);
    }
}
