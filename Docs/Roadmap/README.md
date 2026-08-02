# Conclave Implementation Roadmap & Development Guide

This directory contains the **Conclave Implementation Roadmap**. It details the phase-by-phase engineering strategy used to construct the platform from scratch. 

For the complete architectural layout and system hierarchies, refer to the [Conclave Roadmap Master Index](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Roadmap/Roadmap_Index.md).

---

## 🎯 Purpose & Philosophy
The roadmap is designed to ensure **reproducible, atomic development steps**. By breaking down a complex systems-engineering project into 12 distinct phases, we ensure that:
1.  The codebase remains compile-safe and tested at every checkpoint.
2.  Each milestone delivers a demo-ready vertical slice of functionality.
3.  Architectural specifications (API contracts, security filters) are fully aligned before implementation begins.

---

## ⚙️ Development & Git Workflows

To maintain repository hygiene, all developers must adhere to the following git policies:

### Branching Strategy
*   `main`: Protected branch. Represents the stable production-ready state. No direct commits allowed.
*   `feature/phase-{XX}-{feature-name}`: Dedicated feature branches for roadmap tasks. 
    *   *Example:* `feature/phase-02-jwt-auth` or `feature/phase-08-pipeline-locking`.

### Commit Message Conventions
Commit messages must follow the structure:
`[Phase-XX] <verb> <description of changes>`
*   *Correct Example:* `[Phase-04] Add Llama3 adapter and model test suite`
*   *Incorrect Example:* `fixed some bugs`

---

## 🏛️ Definition of Done (DoD)
A roadmap phase is only marked as **Completed** when it satisfies the following validation checklist:
1.  **Code Compilation:** Code compiles with zero compiler warnings or errors on local JVM and Vite setups.
2.  **Test Suites:**
    *   Backend: `mvn test` completes with 100% pass rates.
    *   Frontend: `npm run test` executes without errors.
3.  **Linting & Style:**
    *   Oxlint/ESLint completes with zero warnings: `npm run lint`.
4.  **Security Checks:** No credentials or private keys are committed in code; all configurations are successfully extracted to environment templates.
5.  **Documentation:** The corresponding chapter in the [Engineering Handbook](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Learning/README.md) is updated to reflect the new implementation details.

---

## 🏁 Phase-by-Phase Roadmap Index

Use the table below to explore each phase's implementation details, goals, and direct links:

| Phase | Title & Spec Document | Primary Objective | Output Deliverables |
| :--- | :--- | :--- | :--- |
| **Phase 01** | [Project Setup](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Roadmap/Phase_01_Project_Setup.md) | Standardize monorepo structure. | Configured root directories, backend Pom.xml, frontend Vite configurations, and PostgreSQL 16 docker-compose definitions. |
| **Phase 02** | [Authentication & Domain](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Roadmap/Phase_02_Authentication_And_Domain.md) | Enforce schema boundaries & REST JWT security. | Postgres Database tables, JPA Entities (`User`, `Role`), and stateless Spring Security auth filters. |
| **Phase 03** | [Room Management API](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Roadmap/Phase_03_Room_Management.md) | Enable workspace orchestration config. | REST endpoints for room initialization, member registrations, and model role configuration mappings. |
| **Phase 04** | [Model Adapter Layer](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Roadmap/Phase_04_Model_Adapter_Layer.md) | Implement prompt tag translations. | `ModelAdapter` interfaces, Llama 3, Gemma, and Mistral tag templates, and serialization test suites. |
| **Phase 05** | [LLM Clients & Registry](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Roadmap/Phase_05_LLM_Clients_And_Registry.md) | Establish local Ollama model connection. | Spring AI Ollama configuration parameters, custom ChatModel wrappers, and dynamic bean registration mappings. |
| **Phase 06** | [Orchestration & WorkflowState](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Roadmap/Phase_06_Orchestration_And_WorkflowState.md) | Coordinate multi-model run pipelines. | `MessageOrchestrator` execution loop, Llama 3 Janitor context compactor prompts, and database-level message purges. |
| **Phase 07** | [WebSocket Real-time Layer](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Roadmap/Phase_07_WebSocket_Realtime.md) | Stream LLM responses word-by-word. | Spring WebSocket config, STOMP broker setup, frame serializations, and user principal handshakes. |
| **Phase 08** | [Pipeline Control System](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Roadmap/Phase_08_Pipeline_Control.md) | Implement locking & pausing control. | Pessimistic DB write lock (`SELECT FOR UPDATE`), pause state machines, and human intervention message injection hooks. |
| **Phase 09** | [Frontend Foundation](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Roadmap/Phase_09_Frontend_Base.md) | Build core client structures. | React 19 bootstrap, state-driven view routers, and Zustand global stores (`authStore`, `roomStore`, `chatStore`). |
| **Phase 10** | [Frontend UI Components](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Roadmap/Phase_10_Frontend_UI_Components.md) | Render real-time console layouts. | CSS layout grids, color-coded chat panels, @-mention input selector menus, and pause warning overlays. |
| **Phase 11** | [Dev Experience & Setup](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Roadmap/Phase_11_Developer_Experience_And_Setup.md) | Guarantee environment repeatability. | System validation checklists, local startup diagnostics scripts, and port conflict checks. |
| **Phase 12** | [Documentation & E2E Audit](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Roadmap/Phase_12_Documentation_And_Repository_Audit.md) | Execute end-to-end repository validations. | Playwright browser automated tests, link audits, and documentation navigation validations. |

---

## 🏆 Project Milestones & Deliverables

Every phase is structured to rolls up into **7 core delivery milestones**:
1.  **Milestone 1: Authenticated Base (Phases 1-2):** PostgreSQL up, REST user register and JWT logins operational.
2.  **Milestone 2: Room Blueprint (Phase 3):** Room configurations and color-coded model roles validated and persisted.
3.  **Milestone 3: Adapter Verification (Phases 4-5):** Llama/Mistral/Gemma bidirectional formatting and translation unit-tests pass.
4.  **Milestone 4: Orchestrated WS Engine (Phases 6-8):** Sequential pipeline runs execute via local Ollama and stream chunks via WebSocket.
5.  **Milestone 5: Connected Frontend Core (Phase 9):** Zustand authorization and room state stores synced via STOMP connections.
6.  **Milestone 6: Tactical Interface UI (Phases 10-11):** Low-contrast dashboard renders streaming model response cards and pause actions.
7.  **Milestone 7: Released Repository (Phase 12):** Playwright automated E2E tests run successfully, engineering audits pass.
