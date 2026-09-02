# Testing Strategy — RKG Metamodeling Middleware

**Status:** Draft — Phase 1 (definite RKGs)
**Related:** [`software-design-document.md`](./software-design-document.md)

This document describes how the middleware is tested: unit tests, integration tests, and the performance/semantic evaluation ("benchmarking") of the system as a whole. It complements the software design document rather than duplicating it — component responsibilities and interfaces are defined there; this document only concerns how correctness and performance are verified.

---

## 1. Guiding principle

The middleware's core value proposition is a *formal correctness guarantee*: SPARQL query results over a chased, definite repository must be sound and complete with respect to RKG Metamodeling Semantics. Testing is therefore organized around three layers of increasing scope and decreasing internal visibility:

1. **Unit tests** — verify individual components in isolation, with GraphDB itself mocked/stubbed out. Fast, no external dependency, run on every build.
2. **Integration tests** — verify the middleware against a real, ephemeral GraphDB instance, exercising the full stack (CLI → connector → GraphDB) for a handful of representative scenarios. Slower, requires Docker, run in CI.
3. **Benchmarking** — an external, black-box evaluation of the packaged middleware as a whole, measuring semantic completeness and computational overhead against a dedicated dataset. Run on demand, not part of the regular build/CI gate, and deliberately **not** implemented as an in-process module of the middleware itself (see §4 for the rationale).

---

## 2. Unit tests

**Scope:** individual classes/components in `connector/`, `validation/`, `chase/`, `query/`, `repostate/`, `config/`, in isolation from any network dependency.

**Approach:**
- `GraphDBConnector` is tested against a mocked RDF4J `Repository`/HTTP layer (e.g. WireMock or a hand-written test double implementing the same client interface), asserting that requests are built correctly (correct SPARQL protocol parameters, correct `infer`/named-graph scoping per §4.1 and §5.1 of the design document) and that GraphDB error responses are normalized into the expected `GraphDBOperationException` categories.
- `DefinitenessValidator` is tested against a fixed, in-memory set of triples (no live GraphDB) representing known definite and known indefinite graphs, asserting the populated/bottom classification matches hand-verified expected results, including edge cases from the ECAI 2025 paper's own examples (e.g. the indefinite `classa` example from the paper's Proposition 1 proof).
- `ChaseOrchestrator`'s Skolem-naming logic (§3.3 of the design document) is unit tested directly: given a class/property IRI, assert the derived witness IRI is deterministic, injective (no collisions across distinct namespaces sharing a local name), and correctly decodable back to the source IRI via percent-decoding.
- `RepoStateStore` is tested against a temporary SQLite file, asserting the `(endpointUrl, repoName)` composite key behaves correctly, the staleness flag transitions match the documented data flow (§3.2), and repo create/delete keeps the local store in sync.
- **Test framework:** JUnit 5, already present in `build.gradle.kts`.

**What unit tests intentionally do not cover:** GraphDB's actual reasoning behavior, real SPARQL protocol wire format, or end-to-end chase correctness against a live triplestore — these require integration tests.

---

## 3. Integration tests

**Scope:** the same components as above, but wired together and run against a real, disposable GraphDB instance, so behavior that only manifests against the actual engine (reasoning materialization, SPARQL protocol quirks, named-graph query scoping) is verified.

**Approach:**
- GraphDB is started as an ephemeral container for the test run, using the existing `docker/docker-compose.yml` (or a dedicated test-only compose override), torn down after the suite completes. No shared/persistent test instance, to keep tests hermetic and repeatable.
- **Repository lifecycle tests:** create/list/delete a repository through `GraphDBConnector`, asserting the bundled `.pie` ruleset (rules 1–21) is installed and active, and that the local `RepoStateStore` row is created/removed in lockstep (per the "Repository state" invariant in §3.2 of the design document).
- **Chase correctness tests:** import a small, hand-crafted definite RKG (Turtle fixture files, one directly modeled on the paper's own worked examples), run the chase, and assert:
  - the full expected `Ch(G)` triple set is present (both rules 1–21 closure and rule 22/23 witnesses),
  - witnesses land in `<urn:rkg:witnesses>` and nowhere else,
  - the chase is idempotent (running it twice adds zero new triples),
  - the pipeline completes in exactly 3 phases (no fixpoint loop), per the invariant argued in §5.3 of the design document.
- **Query-answering tests:** for the same fixtures, assert that `rkg query` (default, RKG-aware) returns results consistent with the paper's own worked entailments (e.g. queries that only hold under classical FOL existential semantics, not under standard SPARQL entailment), and that `rkg query --raw` returns only literal asserted triples, unaffected by the chase having run.
- **Staleness tests:** assert that a Tier A mutation (`rkg data import`/`update`) after a chase flips `chased` to `false`, and that `rkg query` (default) then refuses with the staleness warning until `rkg chase` is re-run.
- **Indefinite-graph tests:** import a graph known to be indefinite (e.g. containing the paper's own counterexample construction) and assert `rkg validate` correctly reports it as indefinite, and `rkg chase` refuses to proceed (Phase 1 scope boundary).
- **Test framework:** JUnit 5 with Testcontainers (or an equivalent Docker-orchestration test library) to manage the GraphDB container lifecycle per test class/suite.

---

## 4. Benchmarking (external, black-box)

### 4.1 Why black-box, not an in-process module

Benchmarking is **not** implemented as a module inside the middleware (no `benchmark/` package, no `rkg bench` command). This is a deliberate choice, not a deferred feature:

- **Fair comparison.** The benchmark's other arm — GraphDB's built-in `RDFS-Plus` profile — can only be measured from outside, since there is no way to instrument GraphDB's internal reasoner. Measuring the middleware in-process while measuring `RDFS-Plus` as a black box would introduce a structural bias in any timing comparison. Treating both consistently as black boxes is the only fair setup.
- **Fidelity.** In-process measurement (calling internal Java interfaces like `ChaseOrchestrator` directly) hides real-world overhead a user actually experiences: CLI startup, argument parsing, and the full round trip to GraphDB as actually issued by the packaged tool. Driving the real `rkg` executable measures what is actually shipped.
- **Separation of concerns.** The benchmarking dataset and evaluation harness are their own deliverable (per the project proposal), distinct from the middleware tool itself; bundling them together would mean end users installing the middleware also pull in benchmarking-only code and dependencies they don't need.

### 4.2 Approach (current phase)

For now, benchmarking is carried out as an **external process**, without introducing a separate subproject or standalone tool:

- The packaged `rkg` CLI is invoked as a subprocess (e.g. via a shell script or a small ad hoc test driver, not part of the Gradle build's main source set), against the baseline (strictly definite) and comparative (controlled indefiniteness) datasets described in the project proposal.
- Wall-clock timing is measured externally around each invocation (`rkg data import`, `rkg chase`, `rkg query`), rather than via any internal instrumentation hook.
- Semantic completeness is measured by comparing `rkg query` results against a hand-verified expected-answer set derived from the ECAI 2025 paper's own worked examples and the dataset's known entailments.
- Computational overhead is measured by comparing the same workload (import → reasoning → query) against a GraphDB repository configured with the standard `RDFS-Plus` reasoning profile instead of the middleware's chase pipeline, using the same external timing methodology on both.
- Results (timings, pass/fail per expected entailment) are recorded manually/via a lightweight script for now; formalizing this into a dedicated subproject is left as a future decision, not undertaken as part of the current design.

### 4.3 Dataset

The benchmarking dataset itself — the baseline and comparative RDFS knowledge graphs engineered to induce multi-level metamodeling behavior, per the project proposal — is a data artifact (Turtle fixtures), not code, and is checked into the repository independently of any test or benchmark tooling (e.g. under a `datasets/` or `benchmark-data/` directory), so it can be consumed by whatever tooling eventually runs against it, in-repo or external.

---

## 5. Summary

| Layer | Scope | Dependency | When run |
|---|---|---|---|
| Unit tests | Individual components, GraphDB mocked | None (in-memory/mocked only) | Every build |
| Integration tests | Full stack, real GraphDB | Docker (ephemeral GraphDB container) | CI, pre-merge |
| Benchmarking | Whole packaged middleware, black-box | Docker/GraphDB + external driver script | On demand, not part of the CI gate |
