# Testing Strategy — RKG Metamodeling Middleware

**Status:** implementation-aligned draft for version `0.1.0`
**Related:** [README](../README.md) and
[software design document](software-design-document.md).

## Test tasks and tags

The Gradle build defines three tagged test tasks:

| Command | Selected tag | Intended scope |
| --- | --- | --- |
| `./gradlew test` | `unit` | Fast tests that do not require a live GraphDB instance. |
| `./gradlew integrationTest` | `integration` | GraphDB-bound component tests. |
| `./gradlew e2eTest` | `e2e` | CLI workflow tests. |
| `./gradlew check` | `unit` and `integration` | `check` depends on `integrationTest`. |

Integration and E2E support is implemented with Testcontainers and GraphDB `11.5.0`; Docker
must be available. The test support creates disposable repositories and removes them after
tests. The supplied test fixtures are deliberately small:

- `src/test/resources/fixtures/it/definite-example.ttl`
- `src/test/resources/fixtures/it/indefinite-example.ttl`
- `src/test/resources/fixtures/e2e/definite-example.ttl`
- `src/test/resources/fixtures/e2e/indefinite-example.ttl`
- `src/test/resources/fixtures/e2e/person-query.sparql`

These fixtures are correctness-test inputs, **not benchmark datasets**.

## Current coverage

The current test classes show coverage for:

- endpoint and GraphDB credential resolution;
- repository-name validation;
- connector result handling and repository lifecycle;
- definiteness-query construction;
- deterministic witness naming and generated chase-query strings;
- the chase's validation refusal, witness idempotence, and stale-witness clear path;
- RKG-aware querying after a chase and default-query staleness refusal;
- root-level parsing for update, backup, and restore commands;
- terminal, JSON, and CSV rendering, including JSON/CSV mutual exclusion;
- three CLI workflows: a definite graph, a post-mutation stale graph, and an indefinite graph.

The integration tests demonstrate the expected witness context behavior in their supported
environment: raw queries do not return witnesses and an inference-enabled graph query can
find a witness in `urn:rkg:witnesses`.

## Validation limits

Passing these tests does not by itself establish:

- soundness or completeness with respect to the cited formal semantics;
- behavior on arbitrary GraphDB versions or configurations;
- the exact ordinary-default-graph semantics of the RKG-aware `SimpleDataset`;
- correctness of property indefiniteness until the binding-name mismatch identified in the
  design document is reconciled;
- performance, throughput, concurrent behavior, recovery, or security properties.

The current build has no benchmark task and makes no CI workflow claim in this repository.
Run the appropriate task locally or in the consuming CI environment.

## Benchmarking and datasets: future work

The dedicated baseline/comparative RDFS benchmark proposed for the project is **future
work**. There is no `datasets/`, `benchmark-data/`, `benchmark/` module, `rkg bench`
command, benchmark Gradle task, external harness, or recorded benchmark result in the
current repository.

Before publishing a benchmark, add versioned datasets, a reproducible driver, GraphDB
configuration/version capture, warm-up/repetition rules, expected semantic answers, and
result-reporting criteria. Keep this work separate from the correctness gates above.
