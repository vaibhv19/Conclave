# Phase 06 — Message Orchestration & WorkflowState Management

## 1. Module Planning: Orchestration & WorkflowState

### 1.1 Purpose
The purpose of this phase is to construct the core orchestrator and context compression engine. The `MessageOrchestrator` parses model mentions (e.g., `@Lead-Writer`), retrieves history, translates it via adapters, runs inferences, and updates the `WorkflowState`. It also runs the **Janitor Service** (using Llama 3) to compress history when it exceeds 10 messages, and tracks tokens via the `TokenUsageLogService`.

### 1.2 Package / Folder Structure
```
backend/src/main/java/com/conclave/
├── service/
│   ├── MessageOrchestrator.java       # Central coordinator for model turns
│   ├── WorkflowStateService.java       # Handles context compression / Janitor
│   └── TokenUsageLogService.java       # Captures real/heuristics token data
├── util/
│   └── MentionParser.java              # Helper to extract @-mentions
└── exception/
    └── OrchestrationException.java
```

### 1.3 Responsibilities & Dependencies
- **Coordinate Turns:** Core engine maps user mentions, resolves the associated model configuration, executes the `ProviderAdapter`, sends payloads to the resolved `ChatClient`, normalizes outputs, and persists results.
- **Context Compression (The "Janitor" Turn):** 
  - *Trigger:* Triggers when a room's `conversation_history` exceeds 10 messages.
  - *Operation:* Resolves Llama 3 as the internal summarizer client. Passes the current draft, comments, and full message logs to summarize the context, writing the updated draft and comments back to the `workflow_state` table.
  - *Purge:* Deletes middle messages in the database, retaining only the initial context foundation message and the most recent 2 messages (short-term memory).
- **Token Logging Interceptor:** Intercepts responses. Extracts metrics:
  - *Ollama:* Read token count metadata from the `ChatResponse` payload.
  - Logs results to the database (`token_usage_log`).

---

## 2. Module Components

### 2.1 Public Interfaces

#### `MessageOrchestrator`
```java
package com.conclave.service;

import com.conclave.domain.CanonicalMessage;
import java.util.UUID;

public interface MessageOrchestrator {
    /**
     * Orchestrates a single conversation turn. Resolves targets, invokes LLM clients,
     * updates WorkflowState, logs tokens, and saves history.
     */
    CanonicalMessage processUserTurn(UUID roomId, String userMessageContent);
}
```

#### `WorkflowStateService`
```java
package com.conclave.service;

import com.conclave.domain.WorkflowState;
import java.util.UUID;

public interface WorkflowStateService {
    /** Checks history length and triggers Janitor summary cleanup if length > 10 */
    void evaluateAndCompressHistory(UUID roomId);

    /** Resolves active WorkflowState DTO representation for context preparation */
    WorkflowState getWorkflowState(UUID roomId);
}
```

---

## 3. Atomic Implementation Tasks

### Task 6.1: Develop MentionParser Utility
- **Estimated Size:** S
- **Risk:** Low
- **Prerequisites:** Phase 03 Room Management
- **Definition of Done:**
  - Create `MentionParser.java` containing static method `Optional<String> extractMention(String content)` that extracts the first occurrence of an `@` tag (e.g. `@Lead-Writer`).
  - Unit tests verify parser correctly extracts mentions from various text positions.

### Task 6.2: Implement TokenUsageLogService
- **Estimated Size:** S
- **Risk:** Low
- **Prerequisites:** Phase 02 Domain Models
- **Definition of Done:**
  - Create `TokenUsageLogService.java` mapping `logUsage(UUID roomId, UUID messageId, String modelId, int promptTokens, int completionTokens, boolean isMocked)`.
  - Service persists usage reports to PostgreSQL database.
  - Unit tests verify DB insertions.

### Task 6.3: Implement MessageOrchestrator Logic
- **Estimated Size:** L
- **Risk:** High
- **Prerequisites:** Task 6.1, Task 6.2, Phase 04 & Phase 05
- **Definition of Done:**
  - Create `MessageOrchestratorImpl.java`.
  - Process flow:
    1. Parse mention from user input. Resolve role, mapping, and target modelId.
    2. Load conversation history and `WorkflowState`.
    3. Resolve the target model adapter and `ChatClient` bean.
    4. Translate context, invoke client, and parse response.
    5. Persist the generated response as a `CanonicalMessage` (roleName set to target role).
    6. Extract token usage metadata, invoke `TokenUsageLogService`.
  - Write integration test using a mock Ollama client verifying the execution round-trip.

### Task 6.4: Implement WorkflowState Service and Llama 3 Janitor Summarizer
- **Estimated Size:** L
- **Risk:** High
- **Prerequisites:** Task 6.3
- **Definition of Done:**
- Create `WorkflowStateServiceImpl.java`.
- Implement `evaluateAndCompressHistory(UUID roomId)`:
  - Checks count of messages in `conversation_history`.
  - If count > 10, calls Llama 3 with a specialized system summarizer template: `[Current Draft]`, `[Review Comments]`, `[History]`. Instructions: Output updated consolidated draft and unresolved reviews.
  - Updates `workflow_state` table columns `current_draft`, `review_comments`, and `last_updated_at`.
  - Deletes middle logs, preserving the room's first message and the last 2 messages.
- Unit tests mock Llama 3 response to verify DB state updates and correct log deletion boundaries.

---

## 4. Documentation & Verification

### Documentation to Update / Create
- Create `Docs/Learning/05_Context_Compression_And_Janitor_Service.md` describing:
  - The Janitor pattern, summarization prompts, and token conservation math.
  - Visual charts showing history state pre-cleanup vs post-cleanup.

### Testing Checkpoint
- Perform integration tests: Seed 11 database history entries. Call orchestrator. Verify that message count resets to 3 (initial context message + 2 recent messages), and the `workflow_state` is updated.

### Suggested Git Commit Boundaries
1. `util: create MentionParser utility and tests`
2. `service: create token usage capture logging system`
3. `service: implement MessageOrchestrator core execution engine`
4. `service: implement WorkflowState Janitor context summarization and database cleanups`

### Suggested GitHub Issues
- **Issue 6.1:** Build parser to extract target mentions from inputs. (Points: 1)
- **Issue 6.2:** Develop token usage logger for database metrics capture. (Points: 1)
- **Issue 6.3:** Implement core message orchestrator logic. (Points: 3)
- **Issue 6.4:** Implement Context Janitor service for history compression. (Points: 3)
