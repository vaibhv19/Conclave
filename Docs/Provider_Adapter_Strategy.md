# Provider Adapter Strategy: Multi-Model Context Unification

This document details the design rationales, vendor constraints, implementation structures, and extension guidelines for Conclave's **Provider Adapter Layer**. 

---

## 1. Architectural Context & Design Decision

### 1.1 The Problem
No two Large Language Model (LLM) providers accept conversation history in the same format. For example:
*   **Google Gemini** expects a strict alternating list of `user`/`model` roles and does not support native `system` role objects in its standard content array.
*   **Anthropic Claude** requires the system instruction to be passed as a root-level property (`system`), with a separate alternating list of `user`/`assistant` messages.
*   **OpenAI GPT** expects a flat list of `system`/`user`/`assistant` messages.

If the application sends a raw conversation transcript to these endpoints, the APIs will fail validation, block requests, or lose context.

### 1.2 The Decision
Conclave implements a custom **Adapter Pattern** at the Java level. Rather than directly binding the database representation to any single provider, all messages are persisted in a single, provider-agnostic **Canonical Schema** (`CanonicalMessage`). Outgoing history is dynamically mapped to the target vendor's format at runtime, and incoming responses are normalized back.

```
                  ┌───────────────────────────────┐
                  │   PostgreSQL DB Store         │
                  │   (CanonicalMessage Schema)   │
                  └───────────────┬───────────────┘
                                  │
                  ┌───────────────▼───────────────┐
                  │     ProviderAdapter           │
                  │     (Core Interface)          │
                  └─┬─────────────┼─────────────┬─┘
                    │             │             │
        ┌───────────▼───┐ ┌───────▼───────┐ ┌───▼───────────┐
        │ GeminiAdapter │ │ OpenAiAdapter │ │ ClaudeAdapter │
        └───────────────┘ └───────────────┘ └───────────────┘
```

### 1.3 Alternatives Considered & Trade-offs
*   **Alternative 1: Direct Spring AI Client Abstractions:** Spring AI provides common wrappers. However, Spring AI's baseline `ChatClient` does not enforce provider-specific history rules (like Gemini's alternating check) at compile-time. Relying solely on Spring AI leads to runtime HTTP failures when history sequences drift.
*   **Alternative 2: Client-side UI Payload Formatting:** Letting the React client format history. This was rejected because it leaks API structures to the client, increases payload sizes, and prevents backend orchestration and auditing.
*   **The Verdict:** Custom `ProviderAdapter` implementations in Java isolate mapping logic. The business orchestrator only depends on the adapter interface, separating domain logic from vendor API changes.

---

## 2. Adapter Specification & Vendor Constraints

### 2.1 Gemini Adapter (Live Integration)
*   **API Schema Requirement:** Strictly alternating array of `user` and `model` messages.
*   **System Prompt Constraint:** Gemini does not accept a `system` role within its standard chat `contents` array.
*   **Mapping Rules:**
    *   `CanonicalMessage(USER)` &rarr; `user`
    *   `CanonicalMessage(AI)` &rarr; `model`
    *   `CanonicalMessage(SYSTEM)` &rarr; Concatenated and prefixed to the first `user` turn content (e.g. `[System Objective]\n\n[User message]`).
*   **Validation Check:** Enforces that the mapped list alternate roles, starting with `user`. Throws `TranslationException` if two consecutive user or model messages are detected.

### 2.2 OpenAI Adapter (Fake Integration)
*   **API Schema Requirement:** Flat list of message objects containing `role` and `content`.
*   **System Prompt Mapping:** Supported natively as a message with `role = "system"`.
*   **Mapping Rules:**
    *   `CanonicalMessage(USER)` &rarr; `user`
    *   `CanonicalMessage(AI)` &rarr; `assistant`
    *   `CanonicalMessage(SYSTEM)` &rarr; `system`
*   **State Inclusion:** Renders the `WorkflowState` (objective, draft, comments) as the very first `system` instruction, ensuring the model is aligned before processing the history.

### 2.3 Claude Adapter (Fake Integration)
*   **API Schema Requirement:** Root-level parameters for model configuration, with a clean alternating list of `user`/`assistant` messages.
*   **System Prompt Constraint:** System instructions must be passed as a top-level property (`system`), not inside the message array.
*   **Mapping Rules:**
    *   Extracts all `SYSTEM` messages and `WorkflowState` details.
    *   Concatenates them into a single string passed in the root `system` field of the request.
    *   Filters the `messages` list to contain only `user` and `assistant` messages, validating that they alternate.

---

## 3. Failure Modes & Recovery Strategies

| Failure Mode | Trigger / Cause | System Impact | Recovery Strategy |
| :--- | :--- | :--- | :--- |
| **Alternating Role Violation** (`GeminiAdapter`) | User sends two consecutive messages without an intervening model response, or a model fails to reply. | Throws a validation `TranslationException`. | The orchestrator catches the exception, halts the turn, rolls back the transaction, and broadcasts a `SYSTEM_INTERVENTION` error event via WebSocket. |
| **Janitor JSON Parsing Error** (`WorkflowStateServiceImpl`) | Gemini summarization outputs invalid JSON or includes markdown markdown indicators. | Fails to parse the updated draft/comments. | **Fallback:** Extracts raw text output, assigns it to `currentDraft`, logs the parsing exception to `reviewComments` for manual user resolution, and continues. |
| **Missing Model Mapping** (`ModelRegistry`) | Mapped role requests an unregistered Model ID. | Throws `OrchestrationException` and halts turn. | UI alerts the user, blocks execution, and returns `400 Bad Request`. |

---

## 4. Developer's Extension Guide: Adding a Live Adapter

To add a new provider (e.g., a live integration for **Cohere** or **Mistral**) in a future release, follow these steps:

### Step 1: Create the Adapter Class
Create a new class implementing `ProviderAdapter` in `com.conclave.integration.adapter`:
```java
public class CohereAdapter implements ProviderAdapter {
    @Override
    public Object toProviderFormat(List<CanonicalMessage> history, WorkflowState state) {
        // Translate canonical history and workflow state to Cohere API structure
        return new CohereRequest(...);
    }

    @Override
    public CanonicalMessage fromProviderFormat(Object response) {
        // Translate Cohere response payload back to CanonicalMessage
        return CanonicalMessage.builder()...build();
    }
}
```

### Step 2: Register the Bean
Add the new adapter configuration in `SpringAiConfig` or register it conditionally in `MessageOrchestratorImpl`:
```java
if (ModelId.COHERE.name().equals(modelId)) {
    adapter = new CohereAdapter();
}
```

### Step 3: Implement Unit Tests
Create unit tests in `src/test/java/com/conclave/integration/adapter/CohereAdapterTest.java` verifying that:
1.  Canonical messages are correctly mapped.
2.  System context is injected into the provider request.
3.  Alternating roles are enforced.

---

## 5. Interview Talking Points (Architectural Defense)

When defending the Provider Adapter Strategy in technical reviews:
*   **Dependency Inversion Principle (DIP):** "The core orchestrator is completely decoupled from provider APIs. It depends on the `ProviderAdapter` interface, not the concrete implementations, allowing us to swap OpenAI from a simulated mock bean to a live endpoint via configuration profiles without changing a single line of business logic."
*   **Single Responsibility Principle (SRP):** "Each adapter class has one job: translation. It contains no state and no network configuration. If Anthropic modifies its API structure, we only need to update the `ClaudeAdapter` class."
*   **Robust Boundary Validation:** "We perform validation *before* making network calls. For example, `GeminiAdapter` checks for alternating roles in Java code rather than sending a malformed request over the network. This saves API costs and provides instant feedback to the user."