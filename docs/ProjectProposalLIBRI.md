# Rule-based Implementation of RKG Metamodeling Semantics in GraphDB for definite RDFS Knowledge Graphs

**DATA MANAGEMENT 2025/2026: PROJECT PROPOSAL**

**Author:** Pietro Francesco Libri - 2046218 - [libri.2046218@studenti.uniroma1.it](mailto:libri.2046218@studenti.uniroma1.it)

---

## Project Objectives

Standard RDFS intensional semantics fails to capture true set-theoretic extensions and treats existential query variables via rigid mapping regimes rather than classical First-Order Logic (FOL)[^1]. Under standard SPARQL entailment, an existential variable must bind to the exact same domain object across all models. Conversely, a pure logic paradigm requires evaluating whether a valid homomorphism exists within each model individually, potentially accepting distinct bindings across different models. To resolve this, Delfino et al.[^2] introduced the *RKG Metamodeling Semantics*, providing a well-founded logic framework for RDFS Knowledge Graphs (RKGs) that interprets SPARQL conjunctions and existential quantifications according to classical FOL while fully preserving RDFS metamodeling capabilities.

The primary objective of this project is to formalize and implement a **custom reasoning profile** for the Ontotext **GraphDB**[^3] engine to support SPARQL query answering under RKG Metamodeling Semantics for *definite RKGs* as defined in Delfino et al.[^2]

Operationally, the system will first programmatically verify whether an incoming graph satisfies the semantic criteria of a definite RKG. The core development will then focus on engineering the reasoning rules for definite RKGs.

## Benchmark Design and Evaluation Framework

Rather than adopting existing standard benchmarks, a core output of this project will consist of the actual design, construction, and formal evaluation of a **dedicated RDFS benchmarking dataset**, engineered specifically to validate the new custom rule set under multi-level modeling conditions.

The resulting knowledge graph will be explicitly tailored to induce complex metamodeling behaviors and multi-level instantiation patterns. Structurally, the dataset will be partitioned into two controlled profiles: a **baseline suite** adhering strictly to the constraints of a definite RKG to verify the soundness, completeness, and correctness of the developed rule set within GraphDB, and a **comparative suite** containing controlled elements of indefiniteness to rigorously test the boundaries, error-handling capabilities, and limits of our programmatic pre-validation rules.

---

[^1]: Franconi, 2011
[^2]: Delfino et al., 2025
[^3]: https://www.ontotext.com/products/graphdb/