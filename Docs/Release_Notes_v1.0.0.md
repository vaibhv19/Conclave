# Release Notes: Conclave v1.0.0

**Release Version:** v1.0.0  
**Release Date:** July 28, 2026  
**Status:** Initial Production Baseline  

Conclave v1.0.0 is the first release of the **Multi-Provider AI Context Unification Platform**. This release establishes the core backend orchestration, WebSocket STOMP real-time streams, pessimistic state locking, history compression, stateless security, and high-density console views.

---

## 🚀 Key Features

### 1. Multi-Model Schema Translation (`ModelAdapter`)
*   Decoupled core database representation from specific model template structures.
*   Enforced boundary validations (e.g. Llama 3 special headers, Mistral bracket tags) at the Java layer.
*   Implemented local Ollama inference execution (Llama 3, Mistral, Gemma).

### 2. WebSocket STOMP Real-Time Streams (`WebSocketConfig`)
*   Configured STOMP pub/sub channels (`/topic/room/{roomId}`) for client sync.
*   Streamed reactive Flux response chunks word-by-word, reducing perceived latency.
*   Broadcast typing indicators (`TURN_STARTED`) and metrics logs (`TURN_COMPLETED`) dynamically.

### 3. Pessimistic Write Locking (`PipelineManager`)
*   Serialized workflow status updates using database write locks (`SELECT FOR UPDATE`).
*   Blocked race conditions during concurrent pause signals and turn completions.
*   Permitted users to halt active sequential queues and inject manual state corrections (`isIntervention`).

### 4. Context Janitor History Compaction (`WorkflowStateService`)
*   Monitored room history logs, triggering compression when message count exceeds 10.
*   Invoked Gemini Pro to summarize progress into a structured `WorkflowState` (draft & comments).
*   Purged middle database rows to reduce token usage and context footprint by up to 75%.

### 5. Stateless JWT Security (`SecurityConfig`)
*   Validated stateless Bearer tokens in incoming REST headers using HMAC-SHA256 signatures.
*   Interceptors authorized STOMP handshakes and checked destination subscriptions.
*   Enforced JPA-level room ownership checks (`owner_id` verification) to isolate tenant workspaces.

### 6. React 19 Console Client (`RoomView`)
*   Structured a split-panel grid console based on low-contrast surface elevations (Levels 0-3).
*   Synchronized client states via global Zustand stores outside the React render loop.
*   Implemented webkit CSS overrides to block browser autofill style overrides.

---

## 💻 Compatibility Matrix

| Dependency | Required Version | Verification Status |
| :--- | :--- | :--- |
| **Java JDK** | `21` | Verified on OpenJDK 21.0.2 (Virtual threads checked). |
| **Spring Boot** | `3.3.1` | Verified (Spring AI 1.0.0-M1 starter). |
| **PostgreSQL** | `16.x` | Verified on Postgres 16 docker image. |
| **NodeJS** | `20.x` or higher | Verified (Vite 5 compilation). |
| **React** | `19.x` | Verified (React DOM render validated). |
| **Browsers** | Chrome, Safari, Firefox | Verified (Playwright browser test suites passing). |

---

## 🛠️ Deployment Configuration

### 1. Required System Properties
Configure these environment variables in your server hosting space:
*   `JWT_SECRET`: HS256 HMAC Secret Key (minimum 256 bits).
*   `GCP_PROJECT_ID` & `GCP_LOCATION`: GCP parameters for live Vertex AI calls.
*   `SPRING_DATASOURCE_URL`: PostgreSQL JDBC url (`jdbc:postgresql://<host>:<port>/conclave_db`).
*   `SPRING_DATASOURCE_USERNAME` / `PASSWORD`: Database credentials.

### 2. Spring Profiles
*   `dev`: Activates local logging and database updates (`ddl-auto: update`).
*   `test`: Runs in-memory H2 database configurations.
*   `prod`: Enforces SSL connections and schema validations.

---

## ⚠️ Known Issues & Roadmap

*   **Gemini Rate Limits:** Using Vertex AI free tier API keys restricts consecutive stream executions under load. (Planned mitigation: v2.0.0 will swap OpenAI/Claude mocks to live endpoints, allowing load balancing across multiple API keys).
*   **Single-Threaded Pipeline Queue:** Pipeline loops execute sequentially. Parallel multi-model consensus checks are planned for v3.0.0.

---

## 🔄 Provider Strategy Pivot (August 2, 2026) - Transition to Local Ollama Models

Conclave has transitioned its core AI orchestration engine from a hybrid model (live cloud Gemini + mocked OpenAI/Claude) to **100% real local inference** powered by a local **Ollama** server deployment. This strategic change shifts the platform to a zero-cost, high-privacy, and high-fidelity collaboration workspace.

### Key Changes
*   **Removal of Cloud API Dependencies:** Removed all dependencies and configuration settings related to GCP Vertex AI and cloud API credentials.
*   **Elimination of Mock/Fake Clients:** Removed all simulated stub classes (`FakeOpenAiChatClient`, `FakeClaudeChatClient`). Every agent role execution now triggers a real inference request.
*   **Ollama Client Integration:** Configured Spring AI's Ollama integration to connect to a local Ollama daemon (`http://localhost:11434`), supporting local models (e.g., Llama 3, Mistral, Gemma).
*   **Chat Template Adaptation:** Rewrote the adapter layer to reconcile formatting, control tokens, and system-prompt placement across different local model chat templates (Llama 3 special tokens, Mistral INST format, Gemma turn structures).
*   **Resource and VRAM Management:** Updated the Context Janitor history compression triggers to account for the smaller context windows of local models, preventing VRAM Out-of-Memory failures on developer machines.
