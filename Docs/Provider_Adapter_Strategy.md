# Provider Adapter Strategy: Local Model Context Unification

This document details the design rationales, local model constraints, implementation structures, and extension guidelines for Conclave's **Provider Adapter Layer** when integrated with local **Ollama**-served models.

---

## 1. Architectural Context & Design Decision

### 1.1 The Problem
No two Large Language Model (LLM) templates accept conversation history in the same format. When hosting models locally using **Ollama**, each model is fine-tuned to expect a specific chat template format to maintain performance and formatting:
*   **Llama 3** expects a specific format with special tokens:
    `<|start_header_id|>system<|end_header_id|>\n\n{system_prompt}<|eot_id|><|start_header_id|>user<|end_header_id|>\n\n{user_prompt}<|eot_id|><|start_header_id|>assistant<|end_header_id|>`
*   **Mistral** expects history wrapped in instruction tags:
    `<s>[INST] {system_prompt}\n\n{user_prompt} [/INST] {assistant_response}</s>[INST] {next_user_prompt} [/INST]`
*   **Gemma** uses control tokens:
    `<start_of_turn>user\n{system_prompt}\n\n{user_prompt}<end_of_turn>\n<start_of_turn>model\n`

Additionally, local environments are heavily constrained by **VRAM / RAM bounds** and have smaller default context windows (typically 2,048 to 8,192 tokens) compared to cloud-based APIs. Sending raw history or mismatched template tags causes model drift, garbage outputs, or VRAM Out-of-Memory (OOM) crashes.

### 1.2 The Decision
Conclave implements a custom **Adapter Pattern** at the Java level. Rather than directly binding the database representation to any single template format, all messages are persisted in a single, provider-agnostic **Canonical Schema** (`CanonicalMessage`). Outgoing history is dynamically mapped to the target local model's template format at runtime, and incoming response streams are normalized back. Every model roundtrip is a real inference call directed to the local Ollama API.

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
        │ LlamaAdapter  │ │MistralAdapter │ │ GemmaAdapter  │
        └───────────────┘ └───────────────┘ └───────────────┘
```

### 1.3 Alternatives Considered & Trade-offs
*   **Alternative 1: Raw Ollama API Default Formatting:** Ollama provides automatic formatting if you send messages via its `/api/chat` endpoint. However, this relies on Ollama's internal modelfiles, which can be inconsistent or strip custom system instructions when history size grows. A Java-level adapter gives us exact character-level control over prompt assembly.
*   **Alternative 2: Client-side UI Template Generation:** Formatting the final prompt on the React client. This was rejected because it exposes formatting details to the UI, increases network payload sizes, and prevents backend orchestration (such as injecting system-level logs or evaluating history sizes for compression).
*   **The Verdict:** Custom `ProviderAdapter` implementations in Java isolate the template mapping logic. The business orchestrator only depends on the adapter interface, allowing us to support new local models by simply adding their template rules.

---

## 2. Adapter Specification & Vendor Constraints

### 2.1 Llama Adapter
*   **Target Model:** `llama3` / `llama3.1` (or equivalent).
*   **Chat Template Requirement:** Llama 3 special header tags and end-of-turn tokens (`<|start_header_id|>`, `<|end_header_id|>`, `<|eot_id|>`).
*   **System Prompt Mapping:** Rendered inside `<|start_header_id|>system<|end_header_id|>`.
*   **Mapping Rules:**
    *   `CanonicalMessage(USER)` &rarr; Wrap content in `user` header.
    *   `CanonicalMessage(AI)` &rarr; Wrap content in `assistant` header.
    *   `CanonicalMessage(SYSTEM)` &rarr; Concat with active `WorkflowState` details (objective, draft, comments) and wrap in `system` header.
*   **Constraints:** <!-- TODO: Revisit during implementation --> Context limits must be strictly checked before assembly to prevent token overflow.

### 2.2 Mistral Adapter
*   **Target Model:** `mistral` (or equivalent).
*   **Chat Template Requirement:** `[INST]` and `[/INST]` tags.
*   **System Prompt Mapping:** Since Mistral's basic chat template does not have a native separate system role tag, system instructions are prepended to the first `[INST]` instruction block.
*   **Mapping Rules:**
    *   Prepend the system prompt and `WorkflowState` summary to the first user message.
    *   `CanonicalMessage(USER)` &rarr; Enclose message in `[INST] ... [/INST]`.
    *   `CanonicalMessage(AI)` &rarr; Output as plain text between instruction blocks.

### 2.3 Gemma Adapter
*   **Target Model:** `gemma` / `gemma2` (or equivalent).
*   **Chat Template Requirement:** `<start_of_turn>` and `<end_of_turn>` tags with `user` and `model` role designations.
*   **Mapping Rules:**
    *   `CanonicalMessage(SYSTEM)` &rarr; Concatenated with `WorkflowState` and placed inside `<start_of_turn>user` (or as a separate system turn if supported by the specific Gemma variation).
    *   `CanonicalMessage(USER)` &rarr; `<start_of_turn>user\n{content}<end_of_turn>`
    *   `CanonicalMessage(AI)` &rarr; `<start_of_turn>model\n{content}<end_of_turn>`

---

## 3. Failure Modes & Recovery Strategies

| Failure Mode | Trigger / Cause | System Impact | Recovery Strategy |
| :--- | :--- | :--- | :--- |
| **Ollama Server Offline** | Local Ollama service is not running or port `11434` is blocked. | API throws `ConnectException`. Execution fails. | The orchestrator catches the connection error, halts the turn execution, rolls back the transaction, and broadcasts a `SYSTEM_INTERVENTION` event suggesting the user verify the Ollama service status. |
| **Model Not Registered/Loaded** | The assigned `modelId` (e.g. `mistral`) is not downloaded in Ollama. | Ollama returns `404 Not Found`. | Backend catches the error, pauses the pipeline, and alerts the user to run `ollama pull <model>` on their server. |
| **VRAM Out-of-Memory (OOM)** | Running multiple models concurrently exceeds GPU capacity. | Inference times out or Ollama process crashes. | The request times out. The orchestrator catches the timeout exception, releases pessimistic locks, sets room status to `PAUSED`, and broadcasts an optimization suggestion to the user interface. |
| **Context Window Saturation** | Message history exceeds the local model's token limit. | Output quality degrades or model starts ignoring older context. | The Context Janitor service detects that history exceeds the limit (10 messages) *before* the turn runs, triggers compression, updates `WorkflowState`, and purges middle messages. |

---

## 4. Developer's Extension Guide: Adding a Local Model Adapter

To add a new local model (e.g., a template for **Qwen** or **Phi-3**) in a future release, follow these steps:

### Step 1: Create the Adapter Class
Create a new class implementing `ProviderAdapter` in `com.conclave.integration.adapter`:
```java
public class QwenAdapter implements ProviderAdapter {
    @Override
    public Object toProviderFormat(List<CanonicalMessage> history, WorkflowState state) {
        // Format history and workflow state to Qwen's specific ChatML template format
        return new QwenRequest(...);
    }

    @Override
    public CanonicalMessage fromProviderFormat(Object response) {
        // Translate response payload back to CanonicalMessage
        return CanonicalMessage.builder()...build();
    }
}
```

### Step 2: Register the Bean
Add the new adapter configuration in `OllamaConfig` or register it conditionally in `MessageOrchestratorImpl`:
```java
if ("qwen".equals(modelId)) {
    adapter = new QwenAdapter();
}
```

### Step 3: Implement Unit Tests
Create unit tests in `src/test/java/com/conclave/integration/adapter/QwenAdapterTest.java` verifying that:
1.  Canonical messages are correctly mapped into Qwen ChatML control tokens.
2.  System context is injected into the prompt.
3.  Context limits are respected.

---

## 5. Interview Talking Points (Architectural Defense)

When defending the Provider Adapter Strategy in technical reviews:
*   **Template Unification:** "Different local open-source models require very specific chat templates (e.g., Llama 3 tokens vs. Mistral's instruction tags) to perform optimally. The `ProviderAdapter` abstraction layer shifts this template reconciliation to dedicated Java classes, ensuring our database representation remains 100% provider-agnostic."
*   **Hardware and Resource Isolation:** "By executing inference locally via Ollama, we remove external network calls and cloud billing entirely. The adapter layer ensures we can manage context windows locally, preventing VRAM OOM crashes by running validations and history compression *before* submitting prompts to Ollama."
*   **Single Responsibility Principle (SRP):** "Each adapter class has one job: template mapping. It contains no connection details or network configurations. If we swap a local `llama3` model for a new version with a different template, we only update the `LlamaAdapter` class."