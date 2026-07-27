# Phase 05 — LLM Clients & Model Registry

## 1. Module Planning: LLM Clients & Model Registry

### 1.1 Purpose
The purpose of this phase is to configure the Spring AI integrations. This includes setting up the live Google Vertex AI client bean, implementing two custom `FakeChatClient` beans simulating OpenAI GPT and Anthropic Claude, and creating the `ModelRegistry` service to dynamically resolve the correct model bean at runtime based on active room mappings.

### 1.2 Package / Folder Structure
```
backend/src/main/java/com/conclave/
├── config/
│   └── SpringAiConfig.java            # Configures Gemini Vertex AI & Fake Clients
└── integration/
    ├── registry/
    │   └── ModelRegistry.java          # Runtime bean resolver
    └── client/
        ├── FakeOpenAiChatClient.java   # Fake ChatClient simulating GPT
        └── FakeClaudeChatClient.java   # Fake ChatClient simulating Claude
```

### 1.3 Responsibilities & Dependencies
- **Spring AI Abstraction:** All client implementations must satisfy Spring AI's standard `ChatClient` interface. This enforces design consistency and allows fake-to-live profile swapping without core code modifications.
- **Model Registry:** Maps standard `ModelId` identifiers (`GEMINI_PRO`, `FAKE_OPENAI`, `FAKE_CLAUDE`) to the registered `ChatClient` beans.
- **Latency & Stutter Simulation:** To align with the unified frontend design:
  - Fakes must simulate network lag using `Thread.sleep` or reactor delays (1s to 3s).
  - Fakes must support simulated streaming by chunking their stubbed responses with brief delays (e.g., 50ms per word chunk).
- **Token Estimation Heuristic:** Fakes must calculate simulated usage: input tokens = query characters / 4; output tokens = response characters / 4.

---

## 2. Module Components

### 2.1 Predefined Model Configurations
- **`GEMINI_PRO`**: Real connection via `VertexAiChatClient`. Requires `SPRING_AI_VERTEX_AI_GEMINI_API_KEY` injected from `.env.local`.
- **`FAKE_OPENAI`**: Wired via `FakeOpenAiChatClient`. Returns mock Markdown text (structured as analytical reviews).
- **`FAKE_CLAUDE`**: Wired via `FakeClaudeChatClient`. Returns mock Markdown text (structured as code critique comments).

### 2.2 Dynamic Resolution Interface
```java
package com.conclave.integration.registry;

import org.springframework.ai.chat.client.ChatClient;
import java.util.Map;

public interface ModelRegistry {
    /**
     * Retrieves the ChatClient bean associated with the given model ID.
     * Throws IllegalArgumentException if the modelId is unsupported.
     */
    ChatClient getClient(String modelId);
}
```

---

## 3. Atomic Implementation Tasks

### Task 5.1: Define Supported Models and Setup Spring AI configurations
- **Estimated Size:** S
- **Risk:** Low
- **Prerequisites:** Phase 01 Setup
- **Definition of Done:**
  - Create standard `ModelId` Enum with options: `GEMINI_PRO`, `FAKE_OPENAI`, `FAKE_CLAUDE`.
  - Spring AI Vertex AI dependency verified in classpath.
  - `SpringAiConfig.java` bootstraps connection details. Verify environment key fallback to prevent crash when key is missing in test execution profiles.

### Task 5.2: Develop FakeOpenAiChatClient and FakeClaudeChatClient
- **Estimated Size:** M
- **Risk:** Medium
- **Prerequisites:** Task 5.1
- **Definition of Done:**
  - Create `FakeOpenAiChatClient.java` implementing standard Spring AI `ChatClient` method contracts.
  - Implement streaming methods (e.g. returning `Flux<ChatResponse>`) that split dummy review text into chunks separated by 50ms thread pauses to simulate real-time API output.
  - Implement non-streaming methods simulating a 1.5s total latency.
  - Create `FakeClaudeChatClient.java` using the same patterns but returning mock developer critique templates.
  - Add unit tests verifying mock responses and token usage calculations.

### Task 5.3: Implement the ModelRegistry Lookup Service
- **Estimated Size:** S
- **Risk:** Low
- **Prerequisites:** Task 5.2
- **Definition of Done:**
  - Create `ModelRegistryImpl.java` annotated with `@Service`.
  - Inject the three ChatClient beans (Gemini, FakeOpenAI, FakeClaude) and index them in a private final `Map<String, ChatClient>`.
  - Implement `getClient(String modelId)` with error checks for unsupported names.
  - Unit tests verify registry resolves each modelId to its exact expected bean class.

---

## 4. Documentation & Verification

### Documentation to Update / Create
- Create `Docs/Learning/04_Model_Registry_And_Fake_ChatClients.md` documenting:
  - The design defense for using fakes instead of network mocking (WireMock).
  - Simulated latency values, token calculation rules, and instructions on how to switch fakes to live API configurations in the future.

### Testing Checkpoint
- Perform registry integration verification: verify that calling registry resolver retrieves active beans.
- Verify that booting the application with no API Key successfully registers fake client beans and boots the backend without crashing.

### Suggested Git Commit Boundaries
1. `config: define ModelId structures and Spring AI Vertex properties`
2. `integration: implement FakeOpenAiChatClient with streaming simulation`
3. `integration: implement FakeClaudeChatClient with streaming simulation`
4. `integration: implement ModelRegistry mapping lookups`

### Suggested GitHub Issues
- **Issue 5.1:** Setup spring AI property profiles and model mappings. (Points: 1)
- **Issue 5.2:** Build Fake ChatClient wrappers for simulated OpenAI logic. (Points: 2)
- **Issue 5.3:** Build Fake ChatClient wrappers for simulated Claude logic. (Points: 2)
- **Issue 5.4:** Create ModelRegistry bean router service. (Points: 1)
