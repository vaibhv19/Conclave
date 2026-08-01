# Phase 05 — LLM Clients & Model Registry

## 1. Module Planning: LLM Clients & Model Registry

### 1.1 Purpose
The purpose of this phase is to configure the Spring AI Ollama integration. This includes setting up the connection to the local Ollama daemon (by default on port `11434`), creating configured client beans for our local models (`llama3`, `mistral`, `gemma`), and building the `ModelRegistry` service to dynamically resolve the target model bean at runtime based on active room mappings. Every client call runs real local inference.

### 1.2 Package / Folder Structure
```
backend/src/main/java/com/conclave/
├── config/
│   └── OllamaConfig.java            # Configures local Ollama connections and options
└── integration/
    └── registry/
        └── ModelRegistry.java          # Runtime model resolver interface
        └── ModelRegistryImpl.java      # Runtime model resolver implementation
```

### 1.3 Responsibilities & Dependencies
- **Spring AI Abstraction:** All client implementations must satisfy Spring AI's standard `ChatModel`/`ChatClient` interfaces. This enforces design consistency and allows model swaps without core code modifications.
- **Model Registry:** Maps standard `ModelId` identifiers (`llama3`, `mistral`, `gemma`) to the configured `OllamaChatModel` beans.
- **Real Token Usage Metrics:** Local models report actual prompt and completion tokens in their response metadata. The registry and orchestrator parse these values directly from Spring AI's response models, removing the need for character-count heuristic estimates.
- **Real Generation Latency:** We perform real local inference, so generation latency is naturally handled by virtual threads during streaming.

---

## 2. Predefined Model Configurations

All local models are accessed via the local Ollama server configured in `application.yml` (default endpoint: `http://localhost:11434`).
*   **`llama3`**: Connection via `OllamaChatModel` with model parameter set to `"llama3"`.
*   **`mistral`**: Connection via `OllamaChatModel` with model parameter set to `"mistral"`.
*   **`gemma`**: Connection via `OllamaChatModel` with model parameter set to `"gemma"`.

---

## 3. Dynamic Resolution Interface

```java
package com.conclave.integration.registry;

import org.springframework.ai.chat.model.ChatModel;

public interface ModelRegistry {
    /**
     * Retrieves the ChatModel bean associated with the given model ID.
     * Throws IllegalArgumentException if the modelId is unsupported.
     */
    ChatModel getClient(String modelId);
}
```

---

## 4. Atomic Implementation Tasks

### Task 5.1: Setup Spring AI Ollama configurations in application.yml
- **Estimated Size:** S
- **Risk:** Low
- **Prerequisites:** Phase 01 Setup
- **Definition of Done:**
  - Standard `ModelId` Enum with options: `llama3`, `mistral`, `gemma`.
  - Spring AI Ollama dependencies configured in `pom.xml`.
  - `OllamaConfig.java` bootstrap properties verify local host connection.

### Task 5.2: Configure OllamaChatModel Beans
- **Estimated Size:** M
- **Risk:** Medium
- **Prerequisites:** Task 5.1
- **Definition of Done:**
  - Create beans in `OllamaConfig.java` for the individual local models, wrapping `OllamaChatModel` with custom parameters (e.g. temperature, system prompts, context-window sizes).
  - Add unit tests verifying prompt generation and parsing of real token usage metadata.

### Task 5.3: Implement the ModelRegistry Lookup Service
- **Estimated Size:** S
- **Risk:** Low
- **Prerequisites:** Task 5.2
- **Definition of Done:**
  - Create `ModelRegistryImpl.java` implementing `ModelRegistry`.
  - Inject the Ollama ChatModel beans and index them in a private final `Map<String, ChatModel>`.
  - Implement `getClient(String modelId)` with error checks for unsupported names.
  - Unit tests verify registry resolves each modelId to its exact expected bean class.

---

## 5. Documentation & Verification

### Documentation to Update / Create
- Create `Docs/Learning/04_Model_Registry_And_Ollama_Clients.md` documenting:
  - Configuration instructions for running Ollama and downloading the required models locally.
  - Performance, hardware constraints, VRAM allocation, and template options for local multi-model workspaces.

### Testing Checkpoint
- Perform registry integration verification: verify that calling registry resolver retrieves active beans.
- Verify that booting the application with a running Ollama daemon successfully initializes all model beans.

### Suggested Git Commit Boundaries
1. `config: configure Spring AI Ollama properties`
2. `config: define OllamaChatModel beans in OllamaConfig`
3. `integration: implement ModelRegistry mapping lookups`

### Suggested GitHub Issues
- **Issue 5.1:** Setup spring AI Ollama property configuration. (Points: 1)
- **Issue 5.2:** Configure OllamaChatModel beans. (Points: 2)
- **Issue 5.3:** Create ModelRegistry bean router service. (Points: 1)
