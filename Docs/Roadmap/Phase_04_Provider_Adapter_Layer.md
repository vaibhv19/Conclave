# Phase 04 — Provider Adapter Layer

## 1. Module Planning: Provider Adapter Layer

### 1.1 Purpose
The purpose of this phase is to construct the `ProviderAdapter` contract and its three implementations. This layer maps the internal `CanonicalMessage` history and `WorkflowState` object into the specific prompt templates (including special control tokens and formatting rules) expected by local models: Llama 3, Mistral, and Gemma. This maps the database logic from the underlying model template requirements, demonstrating standard Adapter Pattern design.

### 1.2 Package / Folder Structure
```
backend/src/main/java/com/conclave/
└── integration/
    └── adapter/
        ├── ProviderAdapter.java        # Core adapter interface
        ├── LlamaAdapter.java           # Llama 3 special token mapping
        ├── MistralAdapter.java         # Mistral instruction bracket wrapping
        └── GemmaAdapter.java           # Gemma turn control token mapping
```

### 1.3 Responsibilities & Dependencies
- **Strict Translation Contracts:** The orchestrator must not have any compile-time dependency on model-specific prompt formats. All adapters must accept canonical JPA models and return normalized format representations (such as compiled prompt strings or structured payload maps).
- **Model Chat Templates & Constraints:**
  - **Llama 3:** Requires special headers (`<|start_header_id|>`) and end-of-turn tokens (`<|eot_id|>`).
  - **Mistral:** Requires system and user instructions wrapped in `[INST]` and `[/INST]` tags.
  - **Gemma:** Requires `<start_of_turn>` and `<end_of_turn>` tags.
  - **VRAM and Context Limits:** Mappers must handle token constraints, trimming history or validating limits before sending requests to the local Ollama daemon.

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
     * into a model-specific request payload/prompt.
     */
    Object toProviderFormat(List<CanonicalMessage> history, WorkflowState state);

    /**
     * Translates a model-specific response payload back into a CanonicalMessage.
     */
    CanonicalMessage fromProviderFormat(Object response);
}
```

### 2.2 Translation Implementation Specifications
- **`LlamaAdapter`**: Formats output text utilizing Llama 3 headers and end-of-turn tokens. Maps `SYSTEM` -> system header, `USER` -> user header, `AI` -> assistant header.
- **`MistralAdapter`**: Formats output text wrapped in `[INST]` tags. Since Mistral has no native system tag, system instructions and state summaries are prepended inside the first `[INST]` block.
- **`GemmaAdapter`**: Formats output text wrapped in `<start_of_turn>` tags with `user`/`model` designations.

---

## 3. Atomic Implementation Tasks

### Task 4.1: Define ProviderAdapter Interface and Adapter Types
- **Estimated Size:** S
- **Risk:** Low
- **Prerequisites:** Phase 02 Domain Models
- **Definition of Done:**
  - `ProviderAdapter.java` created with clean JavaDocs defining its input/output parameters.
  - Integration dependencies for Spring AI Ollama configuration defined in classpath.

### Task 4.2: Implement and Test LlamaAdapter
- **Estimated Size:** M
- **Risk:** Medium
- **Prerequisites:** Task 4.1
- **Definition of Done:**
  - `LlamaAdapter.java` implements `ProviderAdapter`.
  - Converts canonical history and state into a Llama 3 chat template format.
  - Unit tests verify system prompts are correctly injected and Llama control tokens are correctly generated.

### Task 4.3: Implement and Test MistralAdapter
- **Estimated Size:** M
- **Risk:** Low
- **Prerequisites:** Task 4.1
- **Definition of Done:**
  - `MistralAdapter.java` implements `ProviderAdapter`.
  - Generates instruction formats conforming to Mistral `[INST]` and `[/INST]` tags.
  - Unit tests verify that system context is injected inside the first instruction block and that tags alternate correctly.

### Task 4.4: Implement and Test GemmaAdapter
- **Estimated Size:** M
- **Risk:** Medium
- **Prerequisites:** Task 4.1
- **Definition of Done:**
  - `GemmaAdapter.java` implements `ProviderAdapter`.
  - Wraps system and conversation messages inside Gemma's turn tokens.
  - Unit tests verify correct system text extraction and validation of alternating roles.

---

## 4. Documentation & Verification

### Documentation to Update / Create
- Create `Docs/Learning/03_Provider_Adapter_Pattern.md` detailing:
  - Adapter Pattern structure and why it was chosen.
  - Detailed comparisons of prompt templates across local models (Llama 3, Mistral, Gemma).
  - Extensibility rationale for adding new local models (e.g. Qwen, Phi-3).

### Testing Checkpoint
- Comprehensive JUnit suite: `LlamaAdapterTest`, `MistralAdapterTest`, and `GemmaAdapterTest` passing successfully.
- Assert that any translation logic error throws a dedicated `TranslationException` containing details of the schema violation.

### Suggested Git Commit Boundaries
1. `integration: create ProviderAdapter interface definition`
2. `integration: implement LlamaAdapter translation logic and unit tests`
3. `integration: implement MistralAdapter translation logic and unit tests`
4. `integration: implement GemmaAdapter translation logic and unit tests`

### Suggested GitHub Issues
- **Issue 4.1:** Define Core ProviderAdapter interface contracts. (Points: 1)
- **Issue 4.2:** Develop Llama 3 chat template translator and unit tests. (Points: 2)
- **Issue 4.3:** Develop Mistral instruction format translator and unit verification. (Points: 2)
- **Issue 4.4:** Develop Gemma control token format mapping and unit tests. (Points: 2)
