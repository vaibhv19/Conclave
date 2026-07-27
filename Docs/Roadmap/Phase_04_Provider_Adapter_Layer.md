# Phase 04 — Provider Adapter Layer

## 1. Module Planning: Provider Adapter Layer

### 1.1 Purpose
The purpose of this phase is to construct the `ProviderAdapter` contract and its three implementations. This layer maps the internal `CanonicalMessage` history and `WorkflowState` object into the specific schema structures expected by Google Gemini, OpenAI, and Anthropic Claude, demonstrating standard Adapter Pattern design.

### 1.2 Package / Folder Structure
```
backend/src/main/java/com/conclave/
└── integration/
    └── adapter/
        ├── ProviderAdapter.java        # Core adapter interface
        ├── GeminiAdapter.java          # Gemini turn-based mapping
        ├── OpenAiAdapter.java          # OpenAI flat array mapping
        └── ClaudeAdapter.java          # Claude top-level system parameter mapping
```

### 1.3 Responsibilities & Dependencies
- **Strict Translation Contracts:** The orchestrator must not have any compile-time dependency on vendor-specific message objects. All adapters must accept canonical JPA models and return normalized format representations.
- **Provider API Structures:**
  - **Gemini (Vertex AI):** Mandates alternating `user` and `model` turns. Has no native `system` role in the basic contents payload. System context from the `WorkflowState` must be concatenated and injected as a prefix to the very first `user` turn.
  - **OpenAI:** Uses a flat list of messages with explicit roles (`system`, `user`, `assistant`).
  - **Claude (Anthropic):** Strict rule: System prompt must be passed as a root-level parameter (`system`), separate from the `messages` array which contains alternating `user` and `assistant` messages.

---

## 2. Module Components

### 2.1 Public Interface: `ProviderAdapter`

```java
package com.conclave.integration.adapter;

import com.conclave.domain.CanonicalMessage;
import com.conclave.domain.WorkflowState;
import java.util.List;

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

### 2.2 Translation Implementation Specifications
- **`GeminiAdapter`**: Packages text inside `parts: [{ text: "..." }]`. Maps `USER` -> `user`, `AI` -> `model`. If the history starts with `SYSTEM` prompt structures, prepends them to the first user text.
- **`OpenAiAdapter`**: Creates a list of objects containing `role` and `content`. Maps `SYSTEM` -> `system`, `USER` -> `user`, `AI` -> `assistant`.
- **`ClaudeAdapter`**: Filters out any `SYSTEM` message from the history array and concatenates them to form the root `system` prompt parameter. The `messages` array contains only alternating `user`/`assistant` message maps.

---

## 3. Atomic Implementation Tasks

### Task 4.1: Define ProviderAdapter Interface and Adapter Types
- **Estimated Size:** S
- **Risk:** Low
- **Prerequisites:** Phase 02 Domain Models
- **Definition of Done:**
  - `ProviderAdapter.java` created with clean JavaDocs defining its input/output parameters.
  - Integration dependencies for Spring AI/Vertex AI or mock JSON containers defined in classpath.

### Task 4.2: Implement and Test GeminiAdapter
- **Estimated Size:** M
- **Risk:** Medium
- **Prerequisites:** Task 4.1
- **Definition of Done:**
  - `GeminiAdapter.java` implements `ProviderAdapter`.
  - Converts canonical history into standard Google Vertex AI contents payload structures.
  - Unit tests verify that system messages are successfully prepended to the first user turn and alternating roles are validated.

### Task 4.3: Implement and Test OpenAiAdapter (Fake Mapping Validation)
- **Estimated Size:** M
- **Risk:** Low
- **Prerequisites:** Task 4.1
- **Definition of Done:**
  - `OpenAiAdapter.java` implements `ProviderAdapter`.
  - Generates flat array structures conforming to OpenAI ChatCompletion requests (even though execution is stubbed in v1).
  - Unit tests verify serialization of the history list into the flat JSON structure and parsing of standard OpenAI JSON responses.

### Task 4.4: Implement and Test ClaudeAdapter (Fake Mapping Validation)
- **Estimated Size:** M
- **Risk:** Medium
- **Prerequisites:** Task 4.1
- **Definition of Done:**
  - `ClaudeAdapter.java` implements `ProviderAdapter`.
  - Extracts system messages to populate a root-level parameter, keeping the messages array clean of system roles.
  - Unit tests verify correct system text extraction and verification of alternating user/assistant arrays.

---

## 4. Documentation & Verification

### Documentation to Update / Create
- Create `Docs/Learning/03_Provider_Adapter_Pattern.md` detailing:
  - Adapter Pattern structure and why it was chosen.
  - Detailed comparisons of message payloads across the three providers.
  - Extensibility rationale for adding new models (e.g. Cohere, Llama).

### Testing Checkpoint
- Comprehensive JUnit suite: `GeminiAdapterTest`, `OpenAiAdapterTest`, and `ClaudeAdapterTest` passing successfully.
- Assert that any translation logic error throws a dedicated `TranslationException` containing details of the schema violation.

### Suggested Git Commit Boundaries
1. `integration: create ProviderAdapter interface definition`
2. `integration: implement GeminiAdapter translation logic and unit tests`
3. `integration: implement OpenAiAdapter translation logic and unit tests`
4. `integration: implement ClaudeAdapter translation logic and unit tests`

### Suggested GitHub Issues
- **Issue 4.1:** Define Core ProviderAdapter interface contracts. (Points: 1)
- **Issue 4.2:** Develop Google Gemini message payload translator. (Points: 2)
- **Issue 4.3:** Develop OpenAI request/response translator and unit verification. (Points: 2)
- **Issue 4.4:** Develop Anthropic Claude layout structure mapping with system extractor. (Points: 2)
