# Provider Adapter Pattern & Multi-Model Translation Strategy

This document details the design, rationale, and implementation specifications for the **Provider Adapter Layer** of Conclave. This layer decouples internal domain representations of conversations from the specific API schemas of LLM vendors (Google Gemini, OpenAI, and Anthropic Claude).

---

## 1. Context & Architectural Rationale

### 1.1 The Challenge
LLM providers do not agree on a single API structure for chat history:
- **Google Gemini** requires a turn-based list of `contents` strictly alternating roles between `user` and `model`. It lacks a native system role in the standard turn list.
- **OpenAI** expects a flat list of message objects mapping roles (`system`, `user`, `assistant`) sequentially.
- **Anthropic Claude** requires the system prompt to be a top-level property (`system`) completely detached from the messages array, which itself must contain alternating `user` and `assistant` turns.

Direct integration of these schemas into the core orchestration/business logic would result in tight coupling, high code churn, and violation of the Single Responsibility Principle (SRP).

### 1.2 The Solution: The Adapter Pattern
By applying the **Adapter Pattern**, Conclave establishes a single, unified contract: `ProviderAdapter`. The orchestrator interacts exclusively with this contract, remaining agnostic to vendor schemas.

```
       +-------------------+
       |    Orchestrator   |
       +---------+---------+
                 |
                 v
       +-------------------+
       |  ProviderAdapter  |
       +---------+---------+
                 |
        +--------+--------+
        |                 |
        v                 v
+---------------+ +---------------+
| GeminiAdapter | | OpenAiAdapter | (and ClaudeAdapter)
+---------------+ +---------------+
```

---

## 2. Interface Definition

The contract is defined in `com.conclave.integration.adapter.ProviderAdapter`:

```java
public interface ProviderAdapter {
    /**
     * Translates the canonical conversation history combined with the WorkflowState summary 
     * into a vendor-specific request payload.
     */
    Object toProviderFormat(List<CanonicalMessage> history, WorkflowState state);

    /**
     * Translates a vendor-specific response payload back into a CanonicalMessage.
     */
    CanonicalMessage fromProviderFormat(Object response);
}
```

- **Inputs:** A sequential list of `CanonicalMessage`s (database-backed domain entities) and the current compressed `WorkflowState` summary.
- **Outputs:** An agnostic `Object` representing the request payload, and a normalized `CanonicalMessage` parsed from the vendor's response.
- **Error Handling:** Any translation or schema validation error throws a `TranslationException`.

---

## 3. Comparison of Message Payloads & Handling

### 3.1 Google Gemini (Vertex AI)
- **Role Map:** `USER` -> `user`, `AI` -> `model`.
- **System Prompt:** Gemini does not support `system` roles inside the standard `contents` array. The adapter extracts all start-of-conversation `SYSTEM` messages and combines them with the `WorkflowState` details (objective, current draft, review comments). The combined block is injected as a prefix to the content of the very first `user` turn.
- **Alternating Turn Check:** The adapter enforces that the turns strictly alternate starting with `user`.

#### Request Schema
```json
{
  "contents": [
    {
      "role": "user",
      "parts": [
        {
          "text": "[System Context + Workflow State]\n\nUser message goes here..."
        }
      ]
    },
    {
      "role": "model",
      "parts": [
        {
          "text": "Model response..."
        }
      ]
    }
  ]
}
```

---

### 3.2 OpenAI
- **Role Map:** `SYSTEM` -> `system`, `USER` -> `user`, `AI` -> `assistant`.
- **System Prompt:** Injected as a flat `system` message at the beginning of the messages list, alongside any system messages from history.
- **Alternating Turn Check:** Not strictly mandated by OpenAI API, but sequentially mapped.

#### Request Schema
```json
{
  "model": "gpt-4",
  "messages": [
    {
      "role": "system",
      "content": "[Workflow State Context]"
    },
    {
      "role": "system",
      "content": "System message 1"
    },
    {
      "role": "user",
      "content": "User message..."
    }
  ]
}
```

---

### 3.3 Anthropic Claude
- **Role Map:** `USER` -> `user`, `AI` -> `assistant`.
- **System Prompt:** Extracted completely from the message array. All `SYSTEM` messages + `WorkflowState` context are concatenated and placed into the root-level `"system"` parameter.
- **Alternating Turn Check:** The adapter strictly validates that the remaining `messages` list starts with `user` and alternates with `assistant`.

#### Request Schema
```json
{
  "model": "claude-3-opus-20240229",
  "system": "[System Context + Workflow State]",
  "messages": [
    {
      "role": "user",
      "content": "User message..."
    },
    {
      "role": "assistant",
      "content": "Claude response..."
    }
  ]
}
```

---

## 4. Extensibility & Future Models

Adding new models (e.g. Cohere, Llama, Mistral) requires zero changes to the orchestrator:
1. Create a new class implementing `ProviderAdapter` (e.g. `LlamaAdapter`).
2. Implement `toProviderFormat` and `fromProviderFormat` conforming to the new model's schema requirements.
3. Hook the new adapter into the orchestrator registry (Phase 05).

This demonstrates **Dependency Inversion** and the **Open-Closed Principle (OCP)**.
