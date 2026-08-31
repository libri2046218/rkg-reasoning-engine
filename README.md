# RKG Metamodeling Middleware for Ontotext GraphDB

This project implements a hybrid reasoning middleware that enables **RKG Metamodeling Semantics** over **Ontotext GraphDB** for **definite RDFS Knowledge Graphs (RKGs)**, as formalized by Delfino, Lenzerini, and Poggi (ECAI 2025) and grounded in the logic of extensional RDFS (Franconi et al., 2013).

---

## 📌 Project Overview & Goals

Standard RDFS intensional semantics fails to capture true set-theoretic extensions and evaluates SPARQL existential query variables through rigid mapping regimes rather than classical First-Order Logic (FOL). 

The **RKG Metamodeling Semantics** resolves this by interpreting SPARQL conjunctions and existential quantifications according to classical FOL while fully preserving RDFS metamodeling capabilities (allowing entities to simultaneously play the role of individuals, classes, and properties).

Because commercial Semantic Web engines like Ontotext GraphDB are strictly Datalog-based (cannot dynamically create blank nodes/existential witnesses in rule heads), we implement a **hybrid middleware architecture**

---

## 🚀 Execution & Getting Started


---

## 📂 Project Structure

---

## 🛠️ Architecture & Technical Workflow


---

## 📊 Benchmarking & Performance Evaluation

This project includes a dedicated benchmarking framework designed to evaluate the **RKG Metamodeling Semantics** implementation against standard GraphDB reasoning profiles (e.g., `RDFS-Plus`). The evaluation covers two main axes: **Semantic Completeness** and **Computational Overhead**.

### 1. Evaluation Methodology

### 2. Metrics & Comparison Dimensions

### 3. Running the Benchmark Suite

---

## 📍 Project Status & Roadmap

- [x] **Phase 1 (v0.1.0) - Definite RKG Metamodeling Semantics** *(Current)*
  - [x] 
  - [x] 
  - [x] 

- [ ] **Phase 2 (v1.0.0) - Full RKG Metamodeling Semantics** *(In Progress)*
  - [ ] 
  - [ ] 
  - [ ] 

---

## 📚 References
* **[1]** Enrico Franconi et al. *"The logic of extensional RDFS"*. ISWC 2013.
* **[2]** Roberto Maria Delfino, Maurizio Lenzerini, and Antonella Poggi. *"RDFS Knowledge Graphs through the lens of Logic: Semantics and Query Answering"*. ECAI 2025.
