# Software Design Document — RKG Metamodeling Middleware

**Status:** implementation-aligned draft for version `0.1.0`
**Related:** [README](../README.md), [testing strategy](testing-strategy.md), and
[release alignment notes](release-notes-0.1.0.md).

## 1. Scope

The project is a Java/picocli command-line client for GraphDB. Its implemented path is:

1. create a GraphDB repository whose configuration refers to `chase-rules.pie`;
2. import RDF;
3. determine whether the repository is a definite RKG;
4. for a definite repository, derive deterministic witness triples and query using the
   RKG-aware route.

The formal motivation is RKG Metamodeling Semantics for RDFS Knowledge Graphs as described
by Delfino, Lenzerini, and Poggi. This document describes repository behavior that is
observable from the current source; it does not certify soundness, completeness, or a
GraphDB deployment's runtime reasoning configuration.

### Explicit non-goals

- General/indefinite-RKG completion-based query answering is not implemented.
- No benchmark framework, benchmark command, benchmark dataset, or benchmark result is
  present in the repository.
- The project is not a multi-tenant service, authorization system, or GraphDB replacement.

## 2. Components

| Component | Current responsibility |
| --- | --- |
| `org.rkg.cli` | Parses the CLI and prints plain-text results/errors. |
| `org.rkg.config.RkgContext` | Resolves the endpoint and wires the application components. |
| `org.rkg.connector` | Repository REST calls and RDF4J data/query operations. |
| `org.rkg.validation` | Runs the populated/bottom definiteness checks and stores the result. |
| `org.rkg.chase` | Performs the three observed chase stages and mints deterministic IRIs. |
| `org.rkg.query` | Refuses stale default queries and delegates eligible queries to the connector. |
| `org.rkg.repostate` | Stores local, per-endpoint repository state in SQLite. |

## 3. Configuration and deployment

Endpoint precedence is:

1. the inherited `--endpoint` CLI option;
2. `RKG_ENDPOINT`;
3. `http://localhost:7200`.

`GraphDbCredentials` reads either `RKG_USERNAME` and `RKG_PASSWORD` for HTTP Basic
authentication, or `RKG_TOKEN` for bearer authentication. Supplying an incomplete Basic
pair or combining the modes is rejected. Credentials are passed to the repository REST
client and RDF4J HTTP repository; the source does not add an authorization policy of its
own.

Repository creation references a server-side ruleset path. It is
`/opt/graphdb/rules/chase-rules.pie` by default and can be changed with
`RKG_GRAPHDB_RULESET_PATH`. This setting changes only the deployment location: the referenced
server file must be the repository's exact `rules/chase-rules.pie`, not a user-substituted
ruleset. The path must be valid inside the GraphDB server/container; the client does not upload
the file when it creates a repository.

The supplied Compose definitions mount `rules/chase-rules.pie` at the default path, expose
port 7200, and use GraphDB `11.5.0`. A production deployment must arrange GraphDB licensing,
ruleset availability, and server authentication itself.

## 4. Repository and graph model

### 4.1 Local state

`SqliteRepoStateStore` keeps a row keyed by `(endpoint URL, repository name)` at
`$XDG_CONFIG_HOME/rkg-middleware/state.db`, or
`~/.config/rkg-middleware/state.db`. A row contains:

| Field | Meaning |
| --- | --- |
| `chased` | Whether the most recent middleware chase completed after a tracked mutation. |
| `last_chase_timestamp` | Completion time recorded by the chase. |
| `definite` | Last validation result, or absent if never validated. |
| `indefinite_elements` | Comma-separated class/property identifiers from the last negative result. |

`repo create` creates/reset this row and `repo delete` removes it. `importData`, `update`,
`restore` (through import), namespace mutations, and consequently `data clear` mark it
stale. The state is not stored in GraphDB and does not observe writes performed through
GraphDB Workbench or another client. Such writes can make a local `chased` indication
inaccurate.

### 4.2 RDF contexts

The connector imports data without `--graph` into the named graph
`urn:rkg:base-data`. A specified `--graph` is used verbatim. Witness triples are inserted
into `urn:rkg:witnesses`; imports never use the unnamed GraphDB context by default.

`data export --graph <iri>` exports only that context. Without `--graph`, the connector
calls RDF4J's unscoped export, so callers must assume that every accessible context can be
included, including `urn:rkg:witnesses`.

The source configures raw queries with `urn:rkg:base-data` as their default graph and with
inference disabled. RKG-aware queries request inference and use GraphDB's union default context;
every repository context is also available to explicit `GRAPH` patterns. The integration tests
establish witness and user named-graph visibility for the supported GraphDB environment; they do
not establish a general graph-union contract.

## 5. Validation, chase, and querying

### 5.1 Definiteness validation

The validator selects RDFS class and property candidates while excluding terms under
`urn:rkg:witness:`. It asks whether each candidate is populated; for unpopulated candidates
it queries the relevant bottom/universal conditions and records a `ValidationReport`.

`validate --repo <name>` prints the report and exits successfully even when the graph is
indefinite. `chase --repo <name>` always validates first and aborts with a user-level
warning if the graph is indefinite.

**Implementation reconciliation required:** `candidateProperties()` selects `?p`, while
`Rdf4jDefinitenessValidator.candidateElements()` reads the binding named `a`. As written,
that can omit property candidates. The resulting property-definiteness claim must not be
treated as verified until the parent code change reconciles those names and tests it against
GraphDB.

### 5.2 Observed chase stages

For a definite repository, `Rdf4jChaseOrchestrator`:

1. counts triples visible to its inference-enabled baseline query; it does not explicitly
   request a GraphDB closure/fixpoint operation;
2. reads populated classes and properties, creates deterministic `urn:rkg:witness:` IRIs,
   tests each candidate witness, and batch-inserts missing triples into
   `urn:rkg:witnesses`;
3. counts triples again and records the repository as chased.

If the state was stale, the witness context is cleared before stage 2. A second chase of an
unchanged repository avoids duplicate witness insertion by testing for existing witness
triples. `--explain`/`--verbose` prints the two counts and the number of stage-2 insertions.

The project rules file intentionally includes rules labelled 1–8, 11, and 13–21. Rule IDs
9, 10, and 12 are deliberately omitted because GraphDB custom-ruleset compiler
optimizations do not support them reliably; this is not missing behavior. The newer axioms
plus rule 11 give the equivalent derivations through existing rules:

| Omitted rule | Equivalent derivation |
| --- | --- |
| 9: `a p b → a rdf:type rdfs:Resource` | Rule 11 derives `p rdf:type rdf:Property`; the `rdf:Property` domain axiom plus rule 4 derives the subject type. |
| 10: `a p b → b rdf:type rdfs:Resource` | Rule 11 derives `p rdf:type rdf:Property`; the `rdf:Property` range axiom plus rule 5 derives the object type. |
| 12: `a rdf:type rdfs:Class → a rdfs:subClassOf rdfs:Resource` | Rule 7 supplies the self-subclass fact; the `rdfs:Class` subclass axiom plus rule 3 derives the result. |

The middleware supplies the witness behavior attributed to rules 22/23. Claims that
GraphDB has materialized a full closure, or that a separate final closure pass is forcibly
triggered, remain unsupported by the checked-in code alone. Whether inference after a
named-graph insert has the desired closure effect is a deployed-runtime question for parent
reconciliation.

### 5.3 Query routes

- **Default `query`:** `Rdf4jQueryAnsweringEngine` requires local `chased = true`. It
  rejects a stored negative definiteness result and delegates to the connector with
  inference enabled and the witness graph requested.
- **`query --raw`:** calls the connector with inference disabled and no witness graph,
  bypassing the local chased/definiteness checks. It targets `urn:rkg:base-data`, not an
  RDF store's unnamed/default graph.

Both routes accept exactly one inline SPARQL argument or `--file`; the CLI recognizes
`SELECT`, `ASK`, `CONSTRUCT`, and `DESCRIBE` through RDF4J query types. `--json` and
`--csv` select mutually exclusive machine-readable rendering written to standard output;
they do not create files. JSON emits a result kind plus query-form-specific fields; CSV
emits a header and rows (or a `statement` column for graph results). Callers can save either
format with shell redirection.

## 6. CLI contract

The implemented command forms are documented in the
[README](../README.md#commands-currently-exposed-by-the-cli). In addition to repository
and data commands, the root CLI exposes:

| Command | Behavior |
| --- | --- |
| `update --repo <name> (<update> \| --file <path>)` | Raw SPARQL Update; exactly one input form is required and the repository is marked stale. |
| `backup --repo <name> --file <path>` | Unscoped N-Quads export. |
| `restore --repo <name> --file <path>` | N-Quads import that marks the repository stale. |
| `namespaces list/set/remove/clear` | List or mutate repository namespace mappings; mutations mark the repository stale. |
| `query --json` / `query --csv` | Render query results as JSON or CSV to standard output; the two options cannot be combined and do not create files. |

`repo create` and `repo delete` accept a positional repository name; they do not accept
`--name`. Data and administration commands that operate on a repository require `--repo`.

Errors are normalized to:

| Exit status | Meaning |
| --- | --- |
| 0 | Command completed, including a negative `validate` report. |
| 1 | User error, stale/indefinite default-query condition, malformed query/data, or missing repository. |
| 2 | Connection error. |
| 3 | Other server or unexpected error. |

This mapping reflects the CLI exception handler, not a guarantee that every RDF4J/GraphDB
failure will be classified correctly.

## 7. Production limitations

Only the following limitations are established by source:

- The local SQLite state can drift after out-of-band GraphDB writes.
- Generated witness data can appear in an unscoped export.
- A stale chase clears the complete witness graph, so it must be reserved for middleware
  generated data.
- The GraphDB ruleset is addressed by a server filesystem path and deployment is external
  to the application.
- The code has no concurrency control around repository state or chase execution.
- Authentication is pass-through; transport security and authorization are GraphDB/deployment
  responsibilities.

Do not infer transaction isolation, durable recovery behavior, performance characteristics,
or formal semantic guarantees from this document. Those require runtime validation and,
where appropriate, formal review.

## 8. References

1. Enrico Franconi et al., *The logic of extensional RDFS*, ISWC 2013.
2. Roberto Maria Delfino, Maurizio Lenzerini, and Antonella Poggi, *RDFS Knowledge Graphs
   through the lens of Logic: Semantics and Query Answering*, ECAI 2025.
