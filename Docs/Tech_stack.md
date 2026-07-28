# Tech Stack Specification: Conclave

This document defines the architectural choices, technical specifications, and system integration rationales for the **Conclave** platform. The stack is designed to demonstrate high-concurrency systems integration, state synchronization, and the **Adapter Design Pattern** in multi-provider AI orchestration.

---

## 1. Core Technology Components

### 1.1 Backend Infrastructure (Spring Boot & Java 21)

| Technology | Version | Rationale | Alternatives Considered | Trade-offs |
| :--- | :--- | :--- | :--- | :--- |
| **Java (JDK)** | `21` | Harnesses **Virtual Threads** (Loom) to handle highly-concurrent blocking LLM API transactions without running out of carrier threads. | JDK 17 (OS-level platform threads only). | Virtual threads simplify concurrency; however, pin warning checks must be monitored if synchronized blocks are hit. |
| **Spring Boot** | `3.3.1` | Provides the baseline framework for dependency injection, auto-configuration, and transaction boundaries. | Node.js (Express), Python (FastAPI). | Java/Spring delivers compile-time safety and a standard dependency structure, but has higher startup times than Node/FastAPI. |
| **Spring AI** | `1.0.0-M1` | Standardizes LLM client client interfaces (`ChatClient`, `ChatModel`) across vendors. | LangChain4j, custom raw HTTP wrappers. | Spring AI provides native Spring integration, though its rapid release lifecycle can introduce breaking updates. |
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

## 3. Systems-Level Defense: Mocking Strategy

A key architectural highlight of Conclave is its use of Java-level mock classes (`FakeOpenAiChatClient`, `FakeClaudeChatClient`) rather than network-level proxies (like WireMock) or completely omitting other providers.

```
       [ HTTP MOCK SYSTEM (WireMock) ]
       Outgoing REST Call ---> Intercepted at Port ---> Returns JSON String
       * Issues: Only tests HTTP network client serialization; ignores bean-lifecycle integration.

       [ CONCLAVE BEAN-LAYER MOCK SYSTEM ]
       MessageOrchestrator ---> ChatClient Bean (FakeOpenAiChatClient) ---> Simulated Latency
                                     │
                                     └──> Validates Adapter.toProviderFormat serialization
                                     └──> Generates Spring AI ChatResponse
       * Benefits: Exercises the Adapter Pattern directly; supports easy @Profile hot-swapping.
```

### 3.1 Why Bean-Layer Mocking Wins in Engineering Reviews:
1.  **Testing the Adapter directly:** WireMock merely intercepts network sockets and returns hardcoded JSON. By implementing custom `ChatClient` classes, we force the backend to run the actual translation code (`ProviderAdapter.toProviderFormat`) that maps the canonical message history into OpenAI's and Claude's specific API formats. This verifies the **serialization structure** and validation rules in Java code.
2.  **State and Latency Simulation:** Real LLMs stream their responses over time. The fake clients simulate this latency by splitting mock responses and generating chunks periodically on virtual threads. This exercises the frontend STOMP chunk handlers under realistic network conditions.
3.  **Plug-and-Play Profiles:** Switching OpenAI or Claude to a live integration in v2 requires no changes to the business logic. We simply swap the Bean definition in Spring AI using profile annotations (`@Profile("prod")` vs `@Profile("dev")`), demonstrating a clean modular architecture.