# Tech Stack Specification: Conclave

This document defines the architectural choices, technical specifications, and system integration rationales for the **Conclave** platform. The stack is designed to demonstrate high-concurrency systems integration, state synchronization, and the **Adapter Design Pattern** in local multi-model AI orchestration.

---

## 1. Core Technology Components

### 1.1 Backend Infrastructure (Spring Boot & Java 21)

| Technology | Version | Rationale | Alternatives Considered | Trade-offs |
| :--- | :--- | :--- | :--- | :--- |
| **Java (JDK)** | `21` | Harnesses **Virtual Threads** (Loom) to handle highly-concurrent blocking LLM API transactions without running out of carrier threads. | JDK 17 (OS-level platform threads only). | Virtual threads simplify concurrency; however, pin warning checks must be monitored if synchronized blocks are hit. |
| **Spring Boot** | `3.3.1` | Provides the baseline framework for dependency injection, auto-configuration, and transaction boundaries. | Node.js (Express), Python (FastAPI). | Java/Spring delivers compile-time safety and a standard dependency structure, but has higher startup times than Node/FastAPI. |
| **Spring AI (Ollama)** | `1.0.0-M1` | Standardizes local model client interfaces (`OllamaChatModel`, `ChatClient`) utilizing Spring AI's Ollama integrations. | LangChain4j, custom raw HTTP wrappers. | Spring AI provides native Spring integration, though its rapid release lifecycle can introduce breaking updates. |
| **Spring Security** | `6.x` | Coordinates JWT-based stateless authentication filters for security and room operations. | Session-based state, OAuth2 Resource Servers. | JWT-based auth avoids database lookups for session verification, but complicates token revocation. |
| **Spring Data JPA** | `3.x` | Manages relational object mapping (Hibernate) and transactional locking. | JDBC Template, MyBatis. | JPA accelerates CRUD development, but requires careful tuning to avoid the N+1 query problem. |

### 1.2 Messaging & Database Integration

| Technology | Version | Rationale | Alternatives Considered | Trade-offs |
| :--- | :--- | :--- | :--- | :--- |
| **PostgreSQL** | `16` | Ensures ACID compliance and supports pessimistic write locking (`SELECT FOR UPDATE`) for transaction integrity. | MongoDB, Redis (as primary DB). | PostgreSQL handles structured entity relationships cleanly. However, scaling relational joins requires proper index maintenance. |
| **WebSockets (STOMP)** | `Spring Message` | Implements bidirectional event-based framing. STOMP provides clean routing paths (`/topic/room/...`) and frame headers out of the box. | SSE (Server-Sent Events) + HTTP POST, Raw WebSockets. | STOMP simplifies client-side subscription and multiplexing, though it has slightly more protocol overhead than raw TCP sockets. |

### 1.3 Frontend Architecture (React 19)

| Technology | Version | Rationale | Alternatives Considered | Trade-offs |
| :--- | :--- | :--- | :--- | :--- |
| **React** | `19` | Enables efficient rendering updates during real-time chunk streams. | Vue 3, Next.js (SSR). | Client-side React is ideal for dynamic dashboard apps. However, it requires careful bundle optimization for production. |
| **Vite** | `latest` | High-speed local dev HMR, essential for building real-time socket interfaces. | Webpack. | Fast builds, but configuration options are different from standard Webpack loaders. |
| **Zustand** | `latest` | Lightweight state store outside the React component lifecycle. Perfect for updating message lists from WebSocket callbacks. | Redux Toolkit, React Context. | Zustand reduces boilerplate and prevents unnecessary parent-component re-renders, but lacks Redux's extensive devtool ecosystem. |
| **Tailwind CSS** | `latest` | Rapid, low-overhead styling for complex grid layouts and model badges. | Styled Components, CSS Modules. | CSS file remains small, but HTML class lists can become cluttered. |

---

## 2. Architectural Trade-offs & Rationales

### 2.1 Backend Concurrency Model: Virtual Threads vs. Spring WebFlux (Reactive)
When orchestrating multiple API integrations (which are notoriously slow and blocking), high concurrency is critical. We considered two paths:
*   **Spring WebFlux:** Excellent for scaling I/O using a small number of threads, but it requires a complete paradigm shift to reactive programming (`Mono`/`Flux`), making debugging, stack traces, and database transactions (R2DBC) complex.
*   **Java 21 Virtual Threads:** Allows writing standard, synchronous blocking code (which is easier to read and maintain) while the JVM handles mounting and unmounting virtual threads from carrier threads during blocking calls. 
*   **The Verdict:** Conclave uses **Virtual Threads** in combination with Spring's async task executors. This retains the standard JPA transaction stack (`@Transactional`) while achieving the high I/O concurrency required for concurrent LLM streaming calls.

### 2.2 Relational Database vs. NoSQL
*   **NoSQL (e.g., MongoDB):** Sounds appealing for document-like conversation histories and loose message payloads.
*   **Relational (PostgreSQL):** Selected because Conclave relies on strict transactional relationships (such as checking if a `User` owns a `Room` before altering its status) and requires **pessimistic locking** to prevent race conditions during pipeline pause/resume operations.
*   **The Verdict:** PostgreSQL provides structured safety. Message content is stored in text columns, while the database-level lock ensures that state transitions are safe under heavy loads.

### 2.3 Messaging: WebSockets STOMP vs. Server-Sent Events (SSE)
*   **SSE:** Excellent for unidirectional streaming (model to user) and runs over HTTP.
*   **WebSockets STOMP:** Selected because it supports bidirectional messaging, permitting the client to send commands and pause requests on the same connection. STOMP's protocol layer (with standard frames like `SUBSCRIBE`, `SEND`, and custom headers) makes mapping events like `TURN_STARTED` and `CONTENT_CHUNK` straightforward.

---

## 3. Systems-Level Strategy: Local Ollama Inference

A key architectural highlight of Conclave is its reliance on a locally-run Ollama service for running model inference. Outgoing calls are dispatched locally to model instances running on the developer's hardware.

```
       [ CLOUD API OR MOCK SYSTEM ]
       Outgoing REST Call ---> Cloud Gateway ---> Returns JSON String (Expensive / High Latency / Privacy Leak)

       [ CONCLAVE LOCAL OLLAMA SYSTEM ]
       MessageOrchestrator ---> ChatModel Bean (OllamaChatModel) ---> Real Local GPU/CPU Inference
                                      │
                                      └──> Validates Adapter.toProviderFormat serialization (chat templates)
                                      └──> Submits request to http://localhost:11434
                                      └──> Streams real model output chunks in real-time
       * Benefits: Exercises actual model reasoning locally; 100% offline & secure; $0 cost.
```

### 3.1 Why Local Ollama Inference Wins in Engineering Reviews:
1.  **Testing Chat Templates directly:** By mapping to local models, the backend runs the actual prompt-assembly code (`ProviderAdapter.toProviderFormat`) that injects the canonical message history and state into model-specific chat templates (e.g., Llama 3 tags vs. Gemma tokens). This verifies **prompt serialization and token structure** in Java code before execution.
2.  **Real Latency and Performance Auditing:** Instead of fake delay loops, the system handles real model inference. Response chunks stream back based on local hardware capabilities, providing a high-fidelity representation of streaming speed, CPU/GPU loads, and memory bottlenecks.
3.  **Local Data Isolation:** All conversations remain entirely within the local host (or private server). No data is sent to external AI providers, satisfying security rules for private development and eliminating any potential cloud billing costs.
4.  **Flexible Model Selection:** Swapping models is done by configuration. By modifying the `modelId` (e.g., swapping `llama3` for `mistral`), the backend resolves the correct chat model and connects to Ollama, demonstrating a clean modular design.