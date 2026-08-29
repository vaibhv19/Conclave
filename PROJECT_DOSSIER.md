# Conclave

## 1. Project Overview
Conclave is an offline-capable, multi-agent AI consensus and orchestration platform built with a Java 21 / Spring Boot backend, a React 19 / Zustand frontend, and PostgreSQL. It allows multiple heterogeneous Local Large Language Models (LLMs)—specifically Llama 3, Mistral, and Gemma running locally via Ollama—to collaborate sequentially in a unified, moderated discussion workspace. 

Rather than locking conversation history to a vendor-specific format, Conclave persists turns in a provider-agnostic Canonical Schema (`CanonicalMessage`). It dynamically translates conversational history into each model's native chat template at runtime, streams token deltas over WebSocket (STOMP), serializes execution state via database-level pessimistic write locking (`SELECT FOR UPDATE`), and bounds GPU VRAM / context-window growth through an automated history compression service (the "Context Janitor").

---

## 2. Why I Built It
Power users and engineers working with multiple specialized models frequently "tab-hop" between isolated model interfaces (e.g., using Llama 3 for structured code logic, Mistral for creative prose, Gemma for concise reasoning). This manual workflow introduces cognitive friction, context fragmentation, and transcript duplication.

I built Conclave as a systems engineering project to explore:
1. How to design a unified, multi-model runtime that normalizes divergent prompt token templates without coupling the database to any single vendor.
2. How to manage sequential multi-agent execution pipelines reliably, including thread-safe pause, resume, and manual user intervention.
3. How to decouple high-frequency real-time WebSocket token streaming from React render cycles on the client.
4. How to run multi-agent collaboration 100% locally on consumer hardware without recurring API fees or privacy exposure.

---

## 3. Problem / Question
*   **Context Fragmentation & Schema Divergence:** Different open-weight LLMs expect drastically different special formatting tokens (`<|start_header_id|>` for Llama 3, `[INST]` for Mistral, `<start_of_turn>` for Gemma). How can an orchestration layer maintain a single coherent conversational history while dynamically compiling prompt buffers tailored to each model's template?
*   **Pipeline Race Conditions:** In an asynchronous multi-agent pipeline where one model's completion triggers the next, how can human operators reliably pause execution or inject corrections without data races or orphaned agent tasks?
*   **Client Render Thrashing:** When local models stream token deltas rapidly over WebSockets, how can the frontend update active text buffers at 60 FPS without triggering re-renders across parent layouts and sidebars?
*   **Local Hardware & VRAM Saturation:** Consumer GPUs have strict memory limits. Unbounded multi-turn conversations cause context windows to balloon and inference speeds to degrade. How can long multi-agent threads remain coherent within bounded memory limits?

---

## 4. What It Actually Does
1.  **Authentication & Workspace Management:** Users register and authenticate using stateless HMAC-SHA256 JWTs. They create consensus rooms with specific project objectives and configure an ordered execution sequence of AI roles (e.g., `@Lead-Writer` on Llama 3, `@Code-Critic` on Mistral) with custom UI theme accents.
2.  **Mention-Driven Sequential Orchestration:** When an operator posts a message mentioning a role (e.g., `@Lead-Writer please draft an API specification`), the backend parses the mention, resolves the model adapter, loads prior history, and kicks off asynchronous streaming execution.
3.  **Real-Time STOMP Streaming:** Responses are streamed back word-by-word over `/topic/room/{roomId}` alongside typing indicators (`TURN_STARTED`), token usage telemetry (`TURN_COMPLETED`), and system intervention alerts (`SYSTEM_INTERVENTION`).
4.  **Automatic Pipeline Progression:** When a role completes its turn, the orchestrator updates token metrics, advances the room's pipeline pointer (`currentPipelineIndex`), and automatically schedules the next assigned role in the pipeline sequence.
5.  **Pessimistic State Control (Pause / Resume / Intervene):** Operators can pause the pipeline at any point or send an intervention message. The backend uses pessimistic write locks (`findWithLockById`) to halt automated progression, allowing operators to steer the direction before resuming.
6.  **Context Janitor Compaction:** When conversational history exceeds a 10-message threshold, the background janitor service invokes Llama 3 to compress the transcript into a structured `WorkflowState` (`currentDraft` and `reviewComments`), purges intermediate history rows from PostgreSQL, and pushes the compressed state to the frontend sidebar.

---

## 5. Architecture

```
                                  +-----------------------------+
                                  |     React 19 Client UI      |
                                  |  (Views, Components, CSS)   |
                                  +--------------┬--------------+
                                                 │
                                                 ▼
                                  +-----------------------------+
                                  |     Zustand Store Layer     |
                                  | (authStore, roomStore, chat)|
                                  +-------┬-------------┬-------+
                       HTTP / REST API    │             │   WebSocket / STOMP
                       (Port 8080)        │             │   (/ws-conclave)
                                          ▼             ▼
+----------------------------------------------------------------------------------------+
|                          Spring Boot Monolith (Java 21)                                |
|                                                                                        |
|  [Security Layer]                                                                      |
|    - JwtAuthenticationFilter (REST)                                                    |
|    - WebSocketAuthChannelInterceptor (STOMP CONNECT upgrade)                           |
|                                                                                        |
|  [Presentation Layer]                                                                  |
|    - AuthController (Register, Login)                                                  |
|    - RoomController (CRUD rooms, role assignments)                                     |
|    - ChatController (Post message, pause pipeline, resume pipeline)                    |
|                                                                                        |
|  [Application Services & Execution Layer]                                              |
|    - MessageOrchestratorImpl (Virtual Thread async execution, turn chaining)           |
|    - PipelineManagerImpl (Pessimistic write locking, status state transitions)         |
|    - WorkflowStateServiceImpl (Context Janitor history compaction)                     |
|    - TokenUsageLogService (Token telemetry accounting)                                 |
|    - RoomService (Workspace validation, ownership enforcement)                         |
|                                                                                        |
|  [Integration & SPI Layer]                                                             |
|    - ModelRegistryImpl (Dynamic ModelId -> ChatClient / ModelAdapter resolution)       |
|    - ModelAdapter (LlamaAdapter, MistralAdapter, GemmaAdapter)                         |
|    - OllamaChatModelWrapper (Spring AI Ollama adapter)                                 |
+------------------------------┬-----------------------------------------┬---------------+
                               │                                         │
                               ▼                                         ▼
            +------------------------------------+    +----------------------------------+
            |      PostgreSQL 16 Database        |    |       Ollama Local Daemon        |
            | (Users, Rooms, Roles, Messages,    |    |   (Llama 3, Mistral, Gemma 2)    |
            |  WorkflowState, TokenUsageLogs)    |    |   (Port 11434)                   |
            +------------------------------------+    +----------------------------------+
```

### Key Modules & Components
*   **`com.conclave.security`:** `JwtService`, `JwtAuthenticationFilter`, `WebSocketAuthChannelInterceptor`, `SecurityConfig` — Handles stateless HMAC-SHA256 authentication for REST endpoints and STOMP connection upgrades.
*   **`com.conclave.controller`:** `AuthController`, `RoomController`, `ChatController` — REST API entry points.
*   **`com.conclave.domain` & `com.conclave.dto`:** Entity definitions (`User`, `Room`, `RoleAssignment`, `CanonicalMessage`, `WorkflowState`, `TokenUsageLog`) and request/response transfer objects.
*   **`com.conclave.integration.adapter`:** `ModelAdapter`, `LlamaAdapter`, `MistralAdapter`, `GemmaAdapter` — Encapsulates prompt serialization, token wrapping, and response normalization.
*   **`com.conclave.integration.registry`:** `ModelRegistry`, `ModelRegistryImpl`, `OllamaChatModelWrapper` — Resolves configured local model instances dynamically.
*   **`com.conclave.service`:**
    *   `MessageOrchestratorImpl`: Schedules streaming turns on Java 21 Virtual Threads (`conclaveTaskExecutor`), streams chunks via `SimpMessagingTemplate`, logs token metrics, and chains sequential turns.
    *   `PipelineManagerImpl`: Implements thread-safe pause and resume state transitions with database row locks.
    *   `WorkflowStateServiceImpl`: Periodically condenses conversation history into a structured draft/review snapshot.
    *   `RoomService`: Manages workspace creation, model assignment rules, and user authorization.

---

## 6. Important Technical Decisions

### 1. Canonical Storage Schema vs. Vendor-Specific Storage
*   *Decision:* Store all conversational turns in a provider-agnostic `CanonicalMessage` schema in PostgreSQL, serializing into model-specific chat templates at inference runtime via `ModelAdapter` implementations.
*   *Trade-offs:* Requires an adapter layer and runtime template construction for every LLM family, but eliminates schema migrations or vendor lock-in when adding or replacing local models.

### 2. Java 21 Virtual Threads (`Executors.newVirtualThreadPerTaskExecutor()`)
*   *Decision:* Offload long-running LLM streaming inference calls to Java 21 Virtual Threads via an `AsyncTaskExecutor` bean (`conclaveTaskExecutor`), rather than dedicating fixed platform OS thread pools.
*   *Trade-offs:* Virtual threads allow blocking I/O (waiting for Ollama HTTP stream responses) without exhausting server thread pools or requiring fully reactive code across every database layer.

### 3. Database Pessimistic Write Locking for State Coordination
*   *Decision:* Use JPA pessimistic write locking (`@Lock(LockModeType.PESSIMISTIC_WRITE)` / `SELECT FOR UPDATE`) on the `Room` entity during pipeline state modifications and turn transitions.
*   *Trade-offs:* Introduces short-duration row-level locks on the database during status transitions, but prevents race conditions when a user triggers an intervention or pause signal while a streaming turn is completing.

### 4. Client-Side Decoupling via Zustand
*   *Decision:* Route incoming WebSocket STOMP packets directly into a centralized Zustand store (`chatStore.js`) outside React's top-level component tree.
*   *Trade-offs:* Subscribed components selectively listen only to their specific slice of state (e.g., only `MessageBubble` updates during chunk streaming), preventing whole-page re-renders during high-speed token delivery.

### 5. Local-First Inference via Ollama over Cloud APIs
*   *Decision:* Standardize 100% of inference on a local Ollama daemon (`http://localhost:11434`) using Spring AI's Ollama integration.
*   *Trade-offs:* Inference speed is constrained by local hardware GPU/VRAM capacity, but it provides zero operational cost, complete data privacy, and deterministic offline reproducibility.

---

## 7. Interesting Engineering Problems
*   **Dynamic Chat Template Compilation:** Each open-weight model family expects specific control tokens and message placement. `LlamaAdapter` translates canonical history into `<|start_header_id|>...<|eot_id|>` envelopes with system prompt prefixes, `MistralAdapter` wraps turns in `[INST]...[/INST]`, and `GemmaAdapter` constructs `<start_of_turn>user...<end_of_turn>` turns. This design isolates template quirks from the persistence layer.
*   **Dual-Layer Security for REST and WebSockets:** Standard HTTP headers are absent after the initial WebSocket TCP handshake. The backend implements `WebSocketAuthChannelInterceptor` to hook into STOMP `CONNECT` frames, extract the Bearer token from native STOMP headers, authenticate the `UserPrincipal`, and bind it to the WebSocket session context.
*   **Context Window Compaction ("Context Janitor"):** To avoid VRAM exhaustion on local GPUs during lengthy multi-agent discussions, `WorkflowStateServiceImpl` evaluates history length after every turn. When exceeding 10 messages, it prompts Llama 3 to compile existing context into a structured `WorkflowState` object (`currentDraft` and `reviewComments`), persists this state, and purges intermediate messages from the database.

---

## 8. Failure Modes / Things That Went Wrong
*   **Database Refusal on Context Bootstrapping:** `@SpringBootTest` test configurations attempt to connect to PostgreSQL on `localhost:5432` during context initialization. Running `mvn test` in environments where the PostgreSQL Docker container is stopped causes `PSQLException: Connection refused`. Standalone unit tests (`*AdapterTest`, `*ParserTest`, `*JwtServiceTest`) run independently, while full integration tests require `docker compose up -d postgres`.
*   **Vite Dynamic Import Optimization:** `websocket.js` was dynamically imported by `authStore.js` to avoid circular dependencies, while also being statically imported by `RoomView.jsx`. Vite generated an `[INEFFECTIVE_DYNAMIC_IMPORT]` build warning because the module is bundled into the main chunk regardless.
*   **Autofill Browser Style Injection:** Modern web browsers inject white/yellow background styles into HTML inputs when auto-completing credentials, breaking dark tactical console themes. Resolved by injecting `-webkit-autofill` CSS overrides with `transition: background-color 5000s ease-in-out 0s` in `index.css`.

---

## 9. Verification / Testing

### Backend Test Matrix
*   **Unit Tests:**
    *   `GemmaAdapterTest`, `LlamaAdapterTest`, `MistralAdapterTest`: Validates prompt serialization, control token encapsulation, and response extraction.
    *   `JwtServiceTest`: Tests token generation, signature validation, expiration calculation, and claims extraction.
    *   `MentionParserTest`: Validates role mention extraction (`@RoleName`) and edge case handling.
*   **Integration Tests:**
    *   `AuthControllerIntegrationTest`: Tests user registration and login workflows against mock database endpoints.
    *   `RoomControllerIntegrationTest`: Verifies room creation, role configuration constraints, and ownership security checks.
    *   `ChatControllerIntegrationTest`: Validates asynchronous chat message acceptance (202 Accepted) and mention parsing.
    *   `PipelineSequentialIntegrationTest`: Tests end-to-end multi-agent sequential progression, pause/resume locking, and STOMP event dispatching.
    *   `TokenUsageLogServiceTest` & `WorkflowStateServiceTest`: Verifies telemetry logging and context compaction purging.

### Frontend Test Matrix
*   **Vitest & React Testing Library (12 passed tests):**
    *   `MessageBubble`: Asserts user vs. AI bubble alignments, markdown parsing (bold, inline code, code blocks), role colors, and telemetry popup hover behavior.
    *   `TurnIndicator`: Validates streaming pulse animations and role labels.
    *   `ChatBar`: Tests text input, '@' character mention popover dropdown selection, keyboard navigation (`ArrowUp`, `ArrowDown`, `Enter`), and PAUSED state button variations.
    *   `Sidebar`: Tests objective rendering, consensus draft updates, audit telemetry calculation, and collapse/expand toggling.
    *   `AlertBanner`: Tests system halt banners and force resume callback triggers.
*   **Static Analysis & Production Build:**
    *   `oxlint`: Clean static check across all JSX, store, and service files (0 errors).
    *   `vite build`: Compiles production bundle (`dist/`) cleanly in ~1.34s.

---

## 10. Deployment
*   **Infrastructure Requirements:**
    *   Java OpenJDK 21
    *   Node.js 20+ & npm
    *   PostgreSQL 16 database (configured via `docker-compose.yml` or native install)
    *   Ollama server running locally (`http://localhost:11434`) with models pulled:
        ```bash
        ollama pull llama3
        ollama pull mistral
        ollama pull gemma
        ```
*   **Environment Configuration:**
    *   Configured via root `.env.example` / system environment variables:
        *   `SPRING_DATASOURCE_URL`: `jdbc:postgresql://localhost:5432/conclave_db`
        *   `SPRING_DATASOURCE_USERNAME`: `conclave_user`
        *   `SPRING_DATASOURCE_PASSWORD`: `conclave_password`
        *   `SPRING_AI_OLLAMA_BASE_URL`: `http://localhost:11434`
*   **Running the Application:**
    *   *Start Database:* `docker compose up -d`
    *   *Start Backend:* `cd backend && ./mvnw spring-boot:run`
    *   *Start Frontend:* `cd frontend && npm install && npm run dev`

---

## 11. What I Learned
*   **Abstracting LLM Interfaces:** Designing software around LLMs requires treating prompt templates and control tokens as dynamic compilation targets rather than static strings.
*   **Concurrency in State Machines:** Using database-backed pessimistic write locks (`SELECT FOR UPDATE`) provides a simple, robust barrier against race conditions in asynchronous multi-agent pipelines compared to purely in-memory locks across clustered instances.
*   **High-Frequency UI State Synchronization:** Decoupling high-frequency data streams (like character-by-character token deltas) into external state containers (Zustand) is essential to preserve smooth 60 FPS rendering in modern React.

---

## 12. What Changed in My Thinking
*   *Before:* I assumed multi-agent systems should use cloud LLM APIs with JSON function calling for every turn.
*   *After:* I realized that for interactive consensus and local privacy, coordinating local open-weight models via an adapter layer and streaming WebSockets delivers zero operational cost, zero data leakage, and low latency without vendor dependence.
*   *Before:* I thought stateful pipelines could be managed entirely via in-memory thread variables.
*   *After:* I recognized that coordinating human-in-the-loop interventions (pause, edit, resume) requires persisting state transitions transactionally in the database with explicit row-level locking.

---

## 13. Distinctive / Interesting Details
*   **Canonical Schema & Runtime Template Serialization:** Messages are stored in a universal representation (`CanonicalMessage`) and translated on the fly into model-specific chat templates (`LlamaAdapter`, `MistralAdapter`, `GemmaAdapter`), keeping the database schema fully model-agnostic.
*   **Human-in-the-Loop Intervention Protocol:** Operators can pause the consensus pipeline at any point to inject corrections (`isIntervention = true`), forcing state synchronization and context re-compaction before execution resumes.
*   **Context Janitor Compaction:** When conversational length exceeds 10 messages, Llama 3 automatically summarizes the thread into an updated consensus draft and review comments, purging older messages from PostgreSQL to prevent GPU VRAM exhaustion.
*   **Stateless STOMP Authentication:** A custom channel interceptor validates HMAC-SHA256 JWT tokens during the initial STOMP `CONNECT` frame, binding authenticated user credentials directly to WebSocket session topics.

---

## 14. Skills Demonstrated

### Engineering Skills
*   Full-stack systems architecture and API design
*   Multi-LLM orchestration and prompt template normalization
*   Thread-safe state coordination and pessimistic database locking
*   Real-time WebSocket (STOMP) streaming architecture
*   Decoupled reactive frontend state management
*   Stateless authentication across HTTP and WebSocket protocols
*   Automated test engineering (Unit, Integration, Component, E2E)

### Technologies & Tools
*   **Backend:** Java 21 (Virtual Threads), Spring Boot 3.3.1, Spring AI (1.0.0-M1), Spring Security, Spring WebSocket (STOMP), Hibernate JPA, jjwt (0.12.5), Maven
*   **Frontend:** React 19, Vite 5, Zustand 5, Tailwind CSS 3, `@stomp/stompjs` 7, Vitest, React Testing Library, Playwright
*   **Database & Infrastructure:** PostgreSQL 16, Docker Compose, Ollama (Llama 3, Mistral, Gemma)

### Concepts
*   Canonical Data Modeling & Adapter Pattern
*   Pessimistic Write Locking (`SELECT FOR UPDATE`)
*   Project Loom Virtual Threads & Asynchronous Execution
*   Context Window Compression & Memory Bounding
*   STOMP Pub/Sub Broker Channels
*   Stateless JWT Authentication & Channel Interceptors

### Best Skills for LinkedIn
1. **Java 21 & Spring Boot**
2. **Spring AI & Local LLM Orchestration**
3. **WebSocket Architecture (STOMP)**
4. **PostgreSQL & Database Locking (JPA)**
5. **React 19 & Zustand State Management**
6. **Distributed Systems Concurrency**
7. **Full-Stack Application Development**

---

## 15. Public Content

### LinkedIn Project Description
When working across multiple Large Language Models, engineers often jump between isolated browser tabs, manually copying context, prompt instructions, and outputs back and forth. 

To explore a unified, local-first alternative, I built **Conclave** — an open-source multi-agent consensus workspace that coordinates heterogeneous local models (Llama 3, Mistral, Gemma via Ollama) inside a single moderated environment.

Here are a few of the core engineering decisions behind the system:
1. **Canonical Schema Translation:** Rather than coupling the database to one vendor, all turns are saved in a universal schema. Dedicated model adapters dynamically compile conversational history into each model's native chat template tokens (`<|start_header_id|>`, `[INST]`, `<start_of_turn>`) at runtime.
2. **Thread-Safe Pipeline Locking:** Multi-agent runs execute sequentially on Java 21 Virtual Threads. To support human-in-the-loop interventions, the backend uses PostgreSQL pessimistic write locks (`SELECT FOR UPDATE`) to coordinate pause, resume, and prompt injection without race conditions.
3. **Decoupled WebSocket Streaming:** High-frequency STOMP token deltas stream directly into a centralized Zustand store on the React 19 frontend, preventing unnecessary component re-renders during high-speed local inference.
4. **Context Janitor Compaction:** When discussion history exceeds 10 turns, a background compaction service prompts Llama 3 to summarize the thread into an updated draft and review state, pruning older database rows to keep context sizes and GPU VRAM bounded.

The entire platform runs 100% locally with zero cloud API keys, providing complete privacy and zero inference costs.

### LinkedIn Featured Description
*(Not applicable — local offline development project without a hosted public production deployment).*

### Resume Bullet Points
*   Architected a multi-agent AI orchestration platform in Java 21 / Spring Boot, decoupling vendor chat templates from persistence via an adapter layer supporting local Llama 3, Mistral, and Gemma models.
*   Implemented thread-safe pipeline execution using Java 21 Virtual Threads and PostgreSQL pessimistic write locks (`SELECT FOR UPDATE`) to allow operator pause, resume, and mid-stream prompt intervention.
*   Built real-time STOMP WebSocket streaming to a React 19 / Zustand client, decoupling high-frequency token chunk dispatch from UI render cycles and integrating automated background context compression.

### GitHub Repo One-Liner
Multi-agent AI consensus platform orchestrating local Ollama models with Spring Boot and React.

---

## 16. Claims That Should NOT Be Made
*   *Do NOT claim massive production throughput or concurrent enterprise user scale* (Conclave is a local-first offline workspace designed for individual operators and engineering evaluation).
*   *Do NOT claim sub-millisecond cloud LLM inference speeds* (Inference latency is governed by local hardware, GPU VRAM, and the local Ollama server).
*   *Do NOT claim proprietary fine-tuned model weights* (The platform orchestrates standard open-weight models: Llama 3, Mistral, Gemma).
*   *Do NOT claim multi-region distributed clustering* (The backend is a Spring Boot monolith with a PostgreSQL database).
*   *Do NOT claim automated parallel multi-model consensus voting* (The pipeline currently executes models in an ordered sequential chain; parallel branches are a future roadmap concept).

---

## 17. Evidence / Source References
*   **Model Adapters & Token Templates:** `com.conclave.integration.adapter.LlamaAdapter`, `MistralAdapter`, `GemmaAdapter` in [backend/src/main/java/com/conclave/integration/adapter/](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/integration/adapter)
*   **Virtual Threads & Async Configuration:** `com.conclave.config.AsyncConfig` in [backend/src/main/java/com/conclave/config/AsyncConfig.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/config/AsyncConfig.java)
*   **Pessimistic Write Locking:** `findWithLockById` in `com.conclave.repository.RoomRepository` and `PipelineManagerImpl` in [backend/src/main/java/com/conclave/service/PipelineManagerImpl.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/service/PipelineManagerImpl.java)
*   **Context Janitor Compaction:** `com.conclave.service.WorkflowStateServiceImpl` in [backend/src/main/java/com/conclave/service/WorkflowStateServiceImpl.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/service/WorkflowStateServiceImpl.java)
*   **STOMP Handshake JWT Interceptor:** `com.conclave.security.WebSocketAuthChannelInterceptor` in [backend/src/main/java/com/conclave/security/WebSocketAuthChannelInterceptor.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/security/WebSocketAuthChannelInterceptor.java)
*   **Frontend Zustand Store & WebSocket Client:** `frontend/src/store/chatStore.js` and `frontend/src/services/websocket.js` in [frontend/src/](file:///d:/Coding/Projects----For%20Resume/Conclave/frontend/src)
*   **Verification Tests:** `backend/src/test/java/com/conclave/` (18 test classes) and `frontend/src/tests/components.test.jsx` (12 Vitest tests)
