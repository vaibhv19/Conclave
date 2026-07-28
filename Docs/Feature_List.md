# Feature List: Conclave Multi-Provider Context Unification Platform

This document specifies the core features of the Conclave platform. Every feature is mapped to its corresponding architectural layer, defining its inputs, outputs, state transitions, and failure modes.

---

## 1. Workspace & Room Management

This domain governs the lifecycle of collaborative multi-model session rooms.

| Feature Name | Description | Inputs | Outputs | State Transitions | Failure Modes & Mitigations |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Room Initialization** (`RoomController`) | Creates a new isolated project space with a specific objective and assigned roles. | `RoomCreateRequest` (name, objective, role assignments, pipeline sequence). | `RoomResponse` JSON including UUID, owner ID, and status. | Database inserts in `rooms` and `role_assignments`. Room status set to `INITIALIZED`. | **Failure:** Duplicate names or empty objective. <br>**Mitigation:** Field validation returns `400 Bad Request`. |
| **Pipeline State Control** (`PipelineManager`) | Controls the execution loop, allowing manual suspension and resumption. | Room UUID, requesting `User` principal, action (Pause/Resume). | Updated `Room` status. | Status transitions: <br>- `INITIALIZED`/`PAUSED` &rarr; `ACTIVE` <br>- `ACTIVE` &rarr; `PAUSED`. | **Failure:** Unauthorized pause request by non-owner. <br>**Mitigation:** Throws `UnauthorizedAccessException` and blocks transition. |
| **Pessimistic State Locking** (`RoomRepository`) | Locks the room state during pipeline updates to prevent concurrency anomalies. | Room UUID. | Locked `Room` entity (database-level lock). | Imposes a database lock (`SELECT FOR UPDATE`) until transaction commits. | **Failure:** Concurrent update request timeouts. <br>**Mitigation:** Acquires lock with timeout; throws `OrchestrationException` if lock cannot be acquired. |

---

## 2. Multi-Provider Orchestration & Adapters

This domain maps provider-agnostic conversation logs into individual vendor schemas.

| Feature Name | Description | Inputs | Outputs | State Transitions | Failure Modes & Mitigations |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Schema Translation** (`ProviderAdapter`) | Maps a flat history list (`CanonicalMessage`) and context summary (`WorkflowState`) into API-specific request models. | `List<CanonicalMessage>`, `WorkflowState`. | Vendor-specific payload (`GeminiRequest`, `OpenAiRequest`, `ClaudeRequest`). | None (Stateless translation). | **Failure:** Alternating role validation failure in Gemini. <br>**Mitigation:** Throws `TranslationException` and prevents API invocation. |
| **Model Registry** (`ModelRegistry`) | Dynamically resolves and fetches Spring AI `ChatClient` and `ChatModel` beans by Model ID. | `modelId` (String). | Resolved `@Bean` instance of `ChatClient`/`ChatModel`. | None. | **Failure:** Unregistered Model ID requested. <br>**Mitigation:** Throws `OrchestrationException` during startup or turn resolution. |
| **Fake Provider Simulation** (`FakeChatClient`) | Simulates responses and latency for OpenAI and Claude to enable cost-free demos. | `Prompt`. | `ChatResponse` containing simulated text and heuristic metadata. | Appends mock messages to `conversation_history`. | **Failure:** Mock service latency issues. <br>**Mitigation:** Runs on a dedicated virtual thread to isolate blocking delays. |

---

## 3. Context Janitor & State Consolidation

This domain manages token count efficiency by summarizing history and purging middle messages.

| Feature Name | Description | Inputs | Outputs | State Transitions | Failure Modes & Mitigations |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **History Compactor** (`WorkflowStateServiceImpl`) | Triggers automatically when messages in a room exceed 10. | Room UUID. | JSON object with `currentDraft` and `reviewComments`. | - Updates `workflow_state` table. <br>- Deletes middle messages in database (retains index 0 and last 2). | **Failure:** Gemini summarizer output fails JSON parsing. <br>**Mitigation:** Fallback logic sets full response to `currentDraft` and logs warning in `reviewComments`. |
| **Telemetry State Builder** (`WorkflowStateDTO`) | Packages task objective, latest draft, review comments, and short-term memory for frontend. | Room UUID. | Completed `WorkflowStateDTO` with active messages. | None (Read-only compilation). | **Failure:** Message repository returns empty list. <br>**Mitigation:** Returns default initial state with empty lists. |

---

## 4. Real-time Message Stream

This domain handles live message dispatching and chunk-by-chunk streaming over WebSockets.

| Feature Name | Description | Inputs | Outputs | State Transitions | Failure Modes & Mitigations |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **STOMP Broadcast** (`WebSocketConfig`) | Exposes pub/sub channels (`/topic/room/{roomId}`) for real-time telemetry. | Model tokens/chunks generated in backend. | STOMP event packets (`TURN_STARTED`, `CONTENT_CHUNK`, `TURN_COMPLETED`, `SYSTEM_INTERVENTION`). | Push updates client-side without page reload. | **Failure:** Client socket connection drop. <br>**Mitigation:** Client-side `@stomp/stompjs` auto-reconnects; frontend runs GET sync backup. |
| **Stream Consumer Loop** (`MessageOrchestratorImpl`) | Consumes `Flux<ChatResponse>` blocking-style on a Virtual Thread and pushes chunks to WebSocket. | Room UUID, role, prompt. | Continuous text chunks broadcast to UI. | Updates UI input state to disabled during generation. | **Failure:** Model API returns empty stream. <br>**Mitigation:** Catches exception, broadcasts `SYSTEM_INTERVENTION` error message to UI, and resumes. |

---

## 5. UI Layout & Credentials Safety

This domain governs client-side presentation, stores, and browser security.

| Feature Name | Description | Inputs | Outputs | State Transitions | Failure Modes & Mitigations |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Split-Panel Console Layout** (`RoomView`) | High-density grid panel (Levels 0-3 elevation) dividing sidebars, main chat, and objective sidebar. | Zustand store state slices. | CSS Grid rendering. | Updates visual layout states based on active active stores. | **Failure:** Layout overflows on small screens. <br>**Mitigation:** Flex layouts collapse illustration blocks on mobile breakpoints. |
| **Autofill Overrides** (`index.css`) | Prevents yellow/white default browser credential backgrounds in input forms. | Browser auto-fill signals. | CSS shadow inset rendering. | Keeps input elements on `#18181C` Level 2 backgrounds. | **Failure:** Non-webkit browsers ignore styles. <br>**Mitigation:** Standard input resets applied globally. |
| **@-Mention Turn Trigger** (`ChatBar`) | Moderates conversation flow using popover autocomplete menus. | User typing `@` character. | Autocomplete role selector list. | UI focus moves to selector; selected role appended to input. | **Failure:** No matching role found for mention. <br>**Mitigation:** Controller validation blocks message; throws UI validation alert. |

---

## 6. Authentication & Audit Logs

This domain manages user identity and token consumption audit logs.

| Feature Name | Description | Inputs | Outputs | State Transitions | Failure Modes & Mitigations |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **JWT Stateless Security** (`JwtAuthenticationFilter`) | Intercepts HTTP and WebSocket upgrade headers to validate tokens. | Authorization Bearer HTTP header. | Authentication Context injection. | None. | **Failure:** Expired or malformed JWT token. <br>**Mitigation:** Returns `401 Unauthorized` response to client. |
| **Token Usage Logger** (`TokenUsageLogService`) | Logs actual or simulated tokens for audit and cost analysis. | Room UUID, message ID, model ID, prompt tokens, completion tokens. | Database entry in `token_usage_log`. | None. | **Failure:** Token usage metadata missing in response. <br>**Mitigation:** Calculates estimated usage based on character length / 4. |
