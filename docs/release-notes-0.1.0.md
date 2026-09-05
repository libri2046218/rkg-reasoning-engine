# Release Notes — `0.1.0`

## Implemented surface

- Repository lifecycle, RDF import/export/clear, raw SPARQL Update, N-Quads backup/restore,
  namespace management, validation, chase, and RKG-aware/raw query routes.
- Environment-only HTTP Basic (`RKG_USERNAME` and `RKG_PASSWORD`) or bearer-token
  (`RKG_TOKEN`) authentication.
- Local SQLite repository-state bookkeeping, stale-witness rebuilds, safe repository-ID
  validation, and RDF4J value bindings that preserve blank-node identity during the chase.
- Tabular, JSON, and CSV query output.

## Operational deployment boundary

GraphDB loads custom PIE rulesets from its own filesystem. Deploy the repository's
`rules/chase-rules.pie` to the configured GraphDB server path before creating a repository:
`RKG_GRAPHDB_RULESET_PATH`, or `/opt/graphdb/rules/chase-rules.pie` by default. The supplied
Compose configurations demonstrate the required mount. Repository creation references this
server-side path; it does not upload a rules file. The path override changes only the location,
and must reference the repository's exact ruleset.

## Intentional constraints

- Benchmark datasets and a benchmark framework are future work; this release contains neither.
- Rules 9, 10, and 12 are intentionally represented through the optimized axiom-plus-rule
  derivations documented in the README and design document, because GraphDB's custom-ruleset
  compiler does not reliably support their direct forms.
- The release supports definite RKGs. Completion-based query answering for indefinite RKGs is
  not implemented.

See the [README](../README.md), [software design document](software-design-document.md), and
[testing strategy](testing-strategy.md) for operation and verification details.
