# RKG Metamodeling Middleware for Ontotext GraphDB

This Java command-line middleware creates GraphDB repositories configured with the
project's RKG ruleset, validates whether a repository is a **definite RKG**, materializes
Skolem witnesses for the supported chase path, and runs SPARQL queries through either an
RKG-aware or raw route.

The implementation targets the definite-RKG path described in
[the software design document](docs/software-design-document.md). It is a research
prototype, not a production service.

## Requirements

- A JDK capable of running the included Gradle wrapper.
- A reachable GraphDB server. The supplied Docker Compose files use GraphDB `11.5.0`.
- Docker Compose only when starting GraphDB locally.

The application defaults to `http://localhost:7200`. Set `RKG_ENDPOINT` or pass the
global `--endpoint` option to use another server:

```sh
export RKG_ENDPOINT=http://graphdb.example:7200
```

Optional GraphDB authentication is supplied only through environment variables:

```sh
# HTTP Basic authentication
export RKG_USERNAME=alice
export RKG_PASSWORD='…'

# Or bearer-token authentication (do not combine this with the variables above)
export RKG_TOKEN='…'
```

## Start GraphDB locally

From the `docker/` directory, the test-oriented Compose file exposes GraphDB on port
7200 and mounts the ruleset and license expected by repository creation:

```sh
cd docker
docker compose -f docker-compose.test.yml up -d
```

Wait for GraphDB to be ready before creating a repository. Stop it with:

```sh
docker compose -f docker-compose.test.yml down
```

`docker-compose.yml` publishes GraphDB on the default local port, `7200`. For a remote or
otherwise custom deployment, the configured ruleset path must exist on the **GraphDB server**.
`RKG_GRAPHDB_RULESET_PATH` changes that server-side path; it does not upload a rules file.
The override changes only the file location: the server file must be the repository's exact
`rules/chase-rules.pie`, not a user-substituted ruleset. The default path is
`/opt/graphdb/rules/chase-rules.pie`.

## Build and run

Use the wrapper; no system Gradle installation is required:

```sh
./gradlew test
./gradlew run --args='--help'
```

Run every command through the application task:

```sh
./gradlew run --args='repo create people'
```

Repository names must start with a letter, be at most 64 characters, and otherwise contain
only letters, digits, `.`, `_`, or `-`.

## Typical definite-RKG workflow

```sh
# 1. Create a repository configured with the project ruleset.
./gradlew run --args='repo create people'

# 2. Import RDF. Supported filename mappings are Turtle (.ttl/.turtle),
#    RDF/XML (.rdf/.xml), and N-Quads (.nq/.nquads).
./gradlew run --args='data import --repo people --file data.ttl'

# 3. Inspect the Phase-1 definiteness result.
./gradlew run --args='validate --repo people'

# 4. Validate again and run the chase; --verbose and --explain are aliases.
./gradlew run --args='chase --repo people --explain'

# 5. Run an RKG-aware query, either inline or from a file (exactly one is required).
./gradlew run --args='query --repo people --file query.sparql'
./gradlew run --args='query --repo people "SELECT ?s WHERE { ?s ?p ?o }"'
```

The default query route requires the local repository state to say that the repository
has been chased. An import or `data clear` marks it stale; re-run `chase` before the next
RKG-aware query. `chase` performs validation itself and refuses an indefinite graph.

For a non-reasoning diagnostic query that bypasses the staleness check, use:

```sh
./gradlew run --args='query --raw --repo people --file query.sparql'
```

By default, the CLI renders `SELECT` results as tab-separated text, `ASK` as `true` or
`false`, and graph-query results as RDF4J statements. Use one of `--json` or `--csv` for
machine-readable output written to standard output; the options are mutually exclusive:

```sh
./gradlew run --args='query --repo people --json "ASK { ?s ?p ?o }"'
./gradlew run --args='query --repo people --csv --file query.sparql'
```

These flags do not create output files. Redirect standard output to save a result:

```sh
./gradlew run --args='query --repo people --csv --file query.sparql' > results.csv
./gradlew run --args='query --repo people --json "ASK { ?s ?p ?o }"' > results.json
```

## Commands currently exposed by the CLI

| Command | Description |
| --- | --- |
| `repo create <name>` | Create a repository and its local state row. |
| `repo list` | List GraphDB repositories with last-known local state. |
| `repo delete <name>` | Delete a repository and its local state row. |
| `data import --repo <name> --file <path> [--graph <iri>]` | Import RDF. |
| `data export --repo <name> --file <path> [--graph <iri>]` | Export RDF. |
| `data clear --repo <name>` | Execute `CLEAR ALL`. |
| `update --repo <name> (<sparql-update> \| --file <path>)` | Execute a raw SPARQL Update and mark the repository stale. |
| `backup --repo <name> --file <path>` | Write a complete N-Quads repository backup. |
| `restore --repo <name> --file <path>` | Restore an N-Quads backup and mark the repository stale. |
| `namespaces list --repo <name>` | List namespace prefix mappings. |
| `namespaces set --repo <name> <prefix> <namespace>` | Set a namespace prefix mapping. |
| `namespaces remove --repo <name> <prefix>` | Remove one namespace prefix mapping. |
| `namespaces clear --repo <name>` | Remove all namespace prefix mappings. |
| `validate --repo <name>` | Report definiteness. |
| `chase --repo <name> [--explain]` | Run the supported chase path. |
| `query [--json \| --csv] --repo <name> (<sparql> \| --file <path>)` | Query through the RKG-aware route. |
| `query --raw [--json \| --csv] --repo <name> (<sparql> \| --file <path>)` | Query the base-data route without inference. |

For example, the administrative commands can be run as follows:

```sh
./gradlew run --args='update --repo people --file update.rq'
./gradlew run --args='namespaces set --repo people ex https://example.org/'
./gradlew run --args='backup --repo people --file people.nq'
./gradlew run --args='restore --repo people --file people.nq'
```

## Graph and state behavior

- Imports without `--graph` are stored in the named graph `urn:rkg:base-data`; they are
  not written to GraphDB's unnamed/default graph.
- Phase-2 witness triples are written to `urn:rkg:witnesses`. When a stale repository is
  chased, that graph is cleared before current witnesses are recreated.
- `data export` without `--graph` uses RDF4J's unscoped export, so it may include all
  repository contexts, including generated witnesses. Specify `--graph` to select one
  graph.
- `backup` is an unscoped N-Quads export and may therefore include all repository
  contexts. `restore` imports that N-Quads input through the connector and marks the
  repository stale.
- SPARQL Update and namespace mutations (`namespaces set`, `remove`, and `clear`) mark
  the local repository state stale; chase again before the next RKG-aware query.
- Local middleware state is SQLite at
  `$XDG_CONFIG_HOME/rkg-middleware/state.db`, or
  `~/.config/rkg-middleware/state.db` when `XDG_CONFIG_HOME` is unset. It tracks only
  repositories created through this CLI and cannot detect writes made directly to GraphDB.

RKG-aware queries use GraphDB's union default context and expose all repository contexts for
explicit `GRAPH` patterns. Therefore, user data imported with `--graph` participates in the
RKG-aware query and chase path; raw queries remain scoped to `urn:rkg:base-data`.

### Ruleset equivalence

`rules/chase-rules.pie` intentionally omits rule IDs 9, 10, and 12 because GraphDB custom
ruleset compiler optimizations do not support them reliably. They are not missing behavior:
the current axioms and rule 11 provide the equivalent consequences through existing rules.

| Omitted rule | Equivalent derivation in the current ruleset |
| --- | --- |
| Rule 9: `a p b → a rdf:type rdfs:Resource` | Rule 11 derives `p rdf:type rdf:Property`; the `rdf:Property` domain axiom and rule 4 derive the subject's `rdfs:Resource` type. |
| Rule 10: `a p b → b rdf:type rdfs:Resource` | Rule 11 derives `p rdf:type rdf:Property`; the `rdf:Property` range axiom and rule 5 derive the object's `rdfs:Resource` type. |
| Rule 12: `a rdf:type rdfs:Class → a rdfs:subClassOf rdfs:Resource` | Rule 7 derives `a rdfs:subClassOf a`; the `rdfs:Class rdfs:subClassOf rdfs:Resource` axiom and rule 3 derive the stated subclass relation. |

## Testing

```sh
./gradlew test             # @Tag("unit")
./gradlew integrationTest  # @Tag("integration"); requires Docker/GraphDB
./gradlew e2eTest          # @Tag("e2e"); requires Docker/GraphDB
./gradlew check            # unit tests plus integrationTest
```

See [the testing strategy](docs/testing-strategy.md) for the current test inventory and
limits.

## Benchmarks and datasets

The benchmark framework and benchmark datasets proposed for this project have **not been
implemented or added to this repository**. They are explicit future work, not a runnable
command, Gradle task, or current evaluation claim. See
[the original project proposal](docs/ProjectProposalLIBRI.md) and the
[testing strategy](docs/testing-strategy.md#benchmarking-and-datasets-future-work).

## Documentation

- [Software design document](docs/software-design-document.md)
- [Testing strategy](docs/testing-strategy.md)
- [Project proposal and implementation status](docs/ProjectProposalLIBRI.md)
- [Release alignment and reconciliation notes](docs/release-notes-0.1.0.md)

## References

1. Enrico Franconi et al., *The logic of extensional RDFS*, ISWC 2013.
2. Roberto Maria Delfino, Maurizio Lenzerini, and Antonella Poggi, *RDFS Knowledge Graphs
   through the lens of Logic: Semantics and Query Answering*, ECAI 2025.
