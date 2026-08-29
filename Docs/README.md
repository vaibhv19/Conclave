# Conclave Engineering Documentation Portal

Welcome to the central **Conclave Documentation Portal**. This portal indexes all canonical architectural specifications, system diagrams, and technical references.

The goal of this directory is to ensure the repository remains self-documenting. Any engineer can navigate this portal to understand how subsystems are constructed and how to extend or operate them safely.

---

## 🎨 Documentation Philosophy
1.  **Architecture Before Code:** All core subsystems are backed by specifications outlining database schema designs, API contracts, WebSocket payloads, and security boundaries.
2.  **No Placeholders:** Documentation accurately reflects the active codebase.
3.  **Local Isolation Focus:** All architectural and security decisions are designed for local offline operation (using Ollama for local LLM inference) without dependencies on external cloud API keys.

---

## 📂 Canonical Documentation Structure

```
Docs/
├── README.md                  # This file (Documentation Index & Portal)
├── System_Architecture.md     # High-level architecture, flow paths, JPA classes
├── DB_Schema.md               # Relational schema, indices, pessimistic locks
├── API_Specification.md       # REST endpoint details & STOMP socket payloads
├── Security.md                # JWT Filters, token structures, socket interceptors
├── WebSocket_Architecture.md  # WebSocket topics, message envelope formats
├── Model_Adapter_Strategy.md  # LLM tag transformations & token templating
├── Error_Handling_Strategy.md # Global exception handler & recovery procedures
├── Testing_Strategy.md        # Verification matrix (Unit, Integration, E2E)
├── UI_Design.md               # Component hierarchy & HSL styling elevations
├── PRD.md                     # Product Requirements Document
├── Feature_List.md            # Detailed catalog of active features
├── App_Flow.md                # UI state-transition maps & user loops
└── Tech_stack.md              # Framework selections and library dependency rationales
```

---

## 🗺️ Reading Order Recommendations

Depending on your role and objectives:

*   **For Software Engineers & Maintainers:**
    1.  [System_Architecture.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/System_Architecture.md) (Architecture overview & topology)
    2.  [API_Specification.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/API_Specification.md) (REST & STOMP contracts)
    3.  [Model_Adapter_Strategy.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Model_Adapter_Strategy.md) (Model adapters & token formatting)
    4.  [Testing_Strategy.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Testing_Strategy.md) (Test suites & verification strategy)
*   **For System Architects:**
    1.  [System_Architecture.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/System_Architecture.md) (High-level topology & classes)
    2.  [DB_Schema.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/DB_Schema.md) (Pessimistic locking & entity maps)
    3.  [WebSocket_Architecture.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/WebSocket_Architecture.md) (Real-time message routing)
    4.  [Security.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Security.md) (JWT & handshake filters)

---

## 🗃️ Canonical Documents Index

### Architecture & Security
| Document | Link | Core Focus |
| :--- | :--- | :--- |
| **System Architecture** | [System_Architecture.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/System_Architecture.md) | High-level system structure, database design, and sequence diagrams. |
| **Database Schema** | [DB_Schema.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/DB_Schema.md) | JPA entity mappings, database indexes, and pessimistic write lock details. |
| **Security Architecture** | [Security.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Security.md) | JWT payload structure, WebSocket handshake security, and REST endpoint authorization. |
| **WebSocket Broker** | [WebSocket_Architecture.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/WebSocket_Architecture.md) | STOMP routing configurations, client reconnect rules, and socket frame types. |
| **Model Adapter Pattern** | [Model_Adapter_Strategy.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Model_Adapter_Strategy.md) | Token templates and format specs for Llama 3, Mistral, and Gemma models. |
| **UI Design Tokens** | [UI_Design.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/UI_Design.md) | Component hierarchy and color codes for Slate surface elevations Level 0-3. |

### Specifications & Product Requirements
| Document | Link | Core Focus |
| :--- | :--- | :--- |
| **PRD** | [PRD.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/PRD.md) | Vision, target audience, scope boundaries, and core feature list. |
| **API Specification** | [API_Specification.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/API_Specification.md) | REST API endpoints and WebSocket event payload schemas. |
| **Feature List** | [Feature_List.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Feature_List.md) | Functional descriptions of active pipeline and room components. |
| **Application Flow** | [App_Flow.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/App_Flow.md) | UI state transition graphs and user action loops. |
| **Tech Stack Rationale** | [Tech_stack.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Tech_stack.md) | Tabular list of dependencies and reasons for selecting JDK 21, Spring Boot, React, and Zustand. |
| **Error Strategy** | [Error_Handling_Strategy.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Error_Handling_Strategy.md) | System exceptions catalog and WebSocket connection recovery methods. |
| **Testing Strategy** | [Testing_Strategy.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Testing_Strategy.md) | Three-tier testing verification mapping (Backend Unit, Frontend Unit, Playwright E2E). |
