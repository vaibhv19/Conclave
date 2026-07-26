# Provider Adapter Strategy: Multi-Model Context Unification

This document defines the core engineering differentiator of **Conclave**: the abstraction layer that enables a single, unified conversation to be understood by three distinct LLM providers (Google Gemini, OpenAI, and Anthropic Claude), each of which requires a unique API message structure.

---

## 1. The Canonical Conversation Schema
To prevent vendor lock-in and enable cross-model history, Conclave stores all interactions in a **Canonical Schema**. This format is independent of any provider’s specific requirements.

**`CanonicalMessage` Structure:**
```json
{
  "id": "UUID",
  "senderRole": "USER | AI | SYSTEM",
  "participantName": "String (e.g., 'Lead-Writer')",
  "content": "String (Markdown)",
  "timestamp": "ISO-8601",
  "metadata": {
    "modelId": "String",
    "tokenCount": "Integer"
  }
}
```

---

## 2. The Adapter Interface (`ProviderAdapter`)
Every provider implementation must satisfy the `ProviderAdapter` contract. This ensures the orchestrator can interact with any model without knowing its underlying API shape.

```java
public interface ProviderAdapter {
    /** Translates canonical history + WorkflowState into Provider-specific Request */
    ProviderRequest toProviderFormat(List<CanonicalMessage> history, WorkflowState state);

    /** Translates Provider-specific Response back into a CanonicalMessage */
    CanonicalMessage fromProviderFormat(ProviderResponse response);
}
```

---

## 3. Gemini Adapter (Live Implementation)
**Constraint:** Gemini (Vertex AI) uses a "Turn-based" array where roles are strictly toggled.

*   **Role Mapping:** 
    *   `USER` → `user`
    *   `AI` → `model`
    *   `SYSTEM` → Prefixed to the first `user` message (Gemini does not have a native `system` role in the standard `contents` array).
*   **Structure:** Content is wrapped in a `parts` array.
*   **Mapping Detail:**
    ```json
    { "role": "user", "parts": [{ "text": "..." }] }
    ```

---

## 4. OpenAI Adapter (Mocked implementation)
**Constraint:** OpenAI uses a "Flat-array" structure with a dedicated system role.

*   **Role Mapping:** 
    *   `USER` → `user`
    *   `AI` → `assistant`
    *   `SYSTEM` → `system`
*   **Structure:** Direct object-level content.
*   **Mock Verification:** Even though the API call is stubbed, the `toProviderFormat` logic is unit-tested to ensure it generates a valid OpenAI JSON payload. The mock response returns a serialized OpenAI `ChatCompletion` object, which is then passed through `fromProviderFormat` to test the full normalization round-trip.

---

## 5. Claude Adapter (Mocked implementation)
**Constraint:** Anthropic Claude requires the System prompt to be a **top-level parameter**, not a message within the array.

*   **Role Mapping:** 
    *   `USER` → `user`
    *   `AI` → `assistant`
*   **Structure:** 
    *   `messages`: Array of user/assistant turns.
    *   `system`: A separate string field at the root of the JSON (Crucial differentiator).
*   **Mapping Detail:** The adapter extracts all `SYSTEM` messages from history and concatenates them into the root `system` parameter, effectively "cleaning" the message array for Claude's strict validation.

---

## 6. WorkflowState: Context Compression
To minimize costs and avoid context-window saturation, Conclave does not pass the full conversation history to every model. Instead, it passes a summarized `WorkflowState`.

**The `WorkflowState` Object:**
1.  **Project Objective:** The static high-level goal.
2.  **Current Draft:** The latest version of the primary output (e.g., the code or article).
3.  **Review Comments:** A cumulative list of "pending fixes" identified by previous models.
4.  **Short-Term Memory:** Only the last **2 messages** for immediate conversational flow.

**Summarization Trigger:**
When the `CanonicalHistory` exceeds 10 messages, the backend triggers an internal "Janitor" turn (using Gemini) to update the **Current Draft** and **Review Comments** fields, then purges the middle of the history. The adapters then only package the `WorkflowState` + 2 recent messages for the next model.

---

## 7. Extension Path: Mock-to-Real Swap
The architecture is designed so that moving OpenAI or Claude from "Mock" to "Live" is a zero-code change for the business logic.

*   **The Switch:** Using Spring `@Profiles` or `@ConditionalOnProperty`, the application swaps the `FakeOpenAiChatClient` bean for the real `OpenAiChatClient` provided by Spring AI.
*   **The Contract:** Because the `OpenAiAdapter` already produces and consumes the correct OpenAI-shaped JSON, the logic remains identical.
*   **Interview Proof:** This demonstrates **Dependency Inversion**. The Orchestrator depends on the `ProviderAdapter` interface, not the implementation. Adding a real API key simply activates the network transport layer; the data translation logic is already verified in v1.

---

### Attachment: Conclave Feature List
*   **Unified message schema** (Normalized format).
*   **Per-provider adapter layer** (Translation logic).
*   **Conversation persistence** (Shared history).
*   **@-mention turn-taking** (Moderated flow).
*   **WorkflowState** (Summarized context passing).
*   **Multi-Provider Context Unification** (Core differentiator).