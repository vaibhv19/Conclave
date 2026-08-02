# Conclave Engineering Documentation Portal

Welcome to the central **Conclave Documentation Portal**. This portal indexes all specifications, architectural decisions, learning tracks, and roadmap phases. 

The goal of this directory is to ensure the repository remains self-documenting. Any developer should be able to navigate this portal to understand how subsystems are built and how to extend them safely.

---

## 🎨 Documentation Philosophy
1.  **Architecture Before Code:** No feature is built without a prior specification outlining database schema changes, API request/response structures, and sequence diagrams.
2.  **No Placeholders:** Documentation must accurately match the active codebase. Outdated specs must be updated or marked as deprecated.
3.  **Local Isolation Focus:** All architectural and security decisions are designed for local offline operation (specifically using Ollama for inference) without dependencies on external API keys.

---

## 📂 Documentation Folder Structure

```
Docs/
├── README.md                               # This file (Documentation Index & Portal)
├── PRD.md                                  # Product Requirements Document
├── System_Architecture.md                  # High-level architecture, flow paths, JPA classes
├── DB_Schema.md                            # Relational schema, indices, pessimistic locks
├── API_Specification.md                    # REST endpoint details & STOMP socket payloads
├── Security.md                             # JWT Filters, token structures, socket interceptors
├── WebSocket_Architecture.md               # WebSocket topics, message envelope formats
├── Error_Handling_Strategy.md              # Global exception handler & recovery procedures
├── Testing_Strategy.md                     # Verification matrix (Unit, Integration, E2E)
├── UI_Design.md                            # Component hierarchy & HSL styling elevations
├── Model_Adapter_Strategy.md               # LLM tag transformations & token templating
├── Portfolio_And_Interview_Readiness_Defense.md # Portfolio highlights & architectural FAQs
├── Release_Notes_v1.0.0.md                 # Version 1.0.0 release specifications
├── App_Flow.md                             # UI state-transition maps & user loops
├── Feature_List.md                         # Detailed catalog of active features
├── Tech_stack.md                           # Framework selections and library dependency rationales
├── Learning/                               # Engineering Handbook Chapters
│   ├── README.md                           # Onboarding handbook curriculum index
│   └── 01_Developer_Environment_Setup.md ... 09_Tailwind_Customization_For_Tactical_UIs.md
└── Roadmap/                                # Project Implementation Roadmap
    ├── README.md                           # Roadmap index and development workflows
    └── Phase_01_Project_Setup.md ... Phase_12_Documentation_And_Repository_Audit.md
```

---

## 🗺️ Reading Order Recommendations

Depending on your role and objectives, we recommend reading the documentation in the following order:

*   **For New Developers Onboarding:**
    1.  [Docs/Learning/README.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Learning/README.md) (Engineering Handbook Index)
    2.  [Docs/Learning/01_Developer_Environment_Setup.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Learning/01_Developer_Environment_Setup.md) (Environment Setup Guide)
    3.  [Docs/System_Architecture.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/System_Architecture.md) (Architecture overview)
    4.  [Docs/Roadmap/README.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Roadmap/README.md) (Phase-by-phase index)
*   **For System Architects:**
    1.  [Docs/System_Architecture.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/System_Architecture.md) (High-level topology & classes)
    2.  [Docs/DB_Schema.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/DB_Schema.md) (Pessimistic locking & entity maps)
    3.  [Docs/WebSocket_Architecture.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/WebSocket_Architecture.md) (Real-time message routing)
    4.  [Docs/Security.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Security.md) (JWT & handshake filters)
*   **For Interview & Defense Preparation:**
    1.  [Docs/Portfolio_And_Interview_Readiness_Defense.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Portfolio_And_Interview_Readiness_Defense.md) (Systems design FAQs)

---

## 🗃️ Document Categories Index

Use the table below to navigate directly to individual files in this directory:

### Architecture Documents
| Document | Link | Core Focus |
| :--- | :--- | :--- |
| **System Architecture** | [System_Architecture.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/System_Architecture.md) | High-level system structure, database design, and sequence diagrams. |
| **Database Schema** | [DB_Schema.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/DB_Schema.md) | JPA entity mappings, database indexes, and pessimistic write lock details. |
| **Security Architecture** | [Security.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Security.md) | JWT payload structure, WebSocket handshake security, and REST endpoint authorization. |
| **WebSocket Broker** | [WebSocket_Architecture.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/WebSocket_Architecture.md) | STOMP routing configurations, client reconnect rules, and socket frame types. |
| **Model Adapter Pattern** | [Model_Adapter_Strategy.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Model_Adapter_Strategy.md) | Token templates and format specs for Llama 3, Mistral, and Gemma models. |
| **UI Design Tokens** | [UI_Design.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/UI_Design.md) | Component hierarchy and color codes for Slate surface elevations Level 0-3. |

### Planning Documents
| Document | Link | Core Focus |
| :--- | :--- | :--- |
| **PRD** | [PRD.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/PRD.md) | Vision, target audience, scope boundaries, and core feature list. |
| **Feature List** | [Feature_List.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Feature_List.md) | Functional descriptions of active pipeline and room components. |
| **Application Flow** | [App_Flow.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/App_Flow.md) | UI state transition graphs and user action loops. |
| **Tech Stack Rationale** | [Tech_stack.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Tech_stack.md) | Tabular list of dependencies and reasons for selecting JDK 21, Spring Boot, React, and Zustand. |
| **Project Roadmap** | [Roadmap/README.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Roadmap/README.md) | Index of the 12 implementation phases and milestone checklists. |

### Engineering & Learning Documents
| Document | Link | Core Focus |
| :--- | :--- | :--- |
| **Error Strategy** | [Error_Handling_Strategy.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Error_Handling_Strategy.md) | System exceptions catalog and WebSocket connection recovery methods. |
| **Testing Strategy** | [Testing_Strategy.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Testing_Strategy.md) | Three-tier testing verification mapping (Backend Unit, Frontend Unit, Playwright E2E). |
| **Engineering Handbook** | [Learning/README.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Learning/README.md) | Entry point to the 9-chapter onboarding curriculum. |
| **Interview Defense** | [Portfolio_And_Interview_Readiness_Defense.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Portfolio_And_Interview_Readiness_Defense.md) | Highlight of systems-engineering accomplishments for portfolio reviews. |
| **Release Notes** | [Release_Notes_v1.0.0.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Release_Notes_v1.0.0.md) | Release history, dependencies log, and known issues for v1.0.0. |
