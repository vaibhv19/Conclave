# Conclave — Architecture Defense & Portfolio Review Matrix

This document provides a strategic defense matrix detailing the rationale behind major architectural decisions, answering common technical interview questions, and mapping key patterns directly to the codebase implementation.

---

## 🛡️ Architecture Defense Matrix

### 1. Adapter Design Pattern
*   **Core Question:** *Why not write standard if-else blocks inside the main Chat controller to route prompts to different models?*
*   **Strategic Defense & Rationale:** Direct conditional checks inside controllers violate the Single Responsibility and Open-Closed principles. Every time a vendor (Google, OpenAI, Anthropic) updates their payload schema or a new provider is added, the chat controller would require modification and testing. The Adapter pattern establishes a strict interface boundary (`ChatAdapter`), decoupling core orchestration from third-party schema details.
*   **Key Code Reference:** [ChatAdapter.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/integration/adapter/ChatAdapter.java) | [GeminiAdapter.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/integration/adapter/GeminiAdapter.java)
*   **Learning Guide Reference:** [03_Provider_Adapter_Pattern.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Learning/03_Provider_Adapter_Pattern.md)

### 2. Canonical Message Model
*   **Core Question:** *Why persist history in a unified shape instead of caching vendor-specific JSON payloads?*
*   **Strategic Defense & Rationale:** Caching vendor-specific raw payloads introduces vendor lock-in and prevents seamless context transitions. To execute multi-agent workflows where a GPT draft is reviewed by Gemini and audited by Claude, the backend requires a single, provider-agnostic representation. `CanonicalMessage` standardizes metadata, sender roles (`USER`, `AI`, `SYSTEM`), content text, and model identifier tags globally.
*   **Key Code Reference:** [CanonicalMessage.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/domain/CanonicalMessage.java)
*   **Learning Guide Reference:** [03_Provider_Adapter_Pattern.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Learning/03_Provider_Adapter_Pattern.md)

### 3. Model Registry & Simulated Fakes
*   **Core Question:** *Why build simulated ChatClient fakes inside the backend rather than using standard mock servers (e.g. WireMock)?*
*   **Strategic Defense & Rationale:** While mock servers check HTTP traffic bounds, they do not exercise internal mapper flows, data bindings, or token heuristic processing. Simulated fakes (`FakeOpenAiChatClient`) are fully integrated Spring beans that mimic realistic word-by-word streaming latency, calculate token usages, and test concurrency limits under multi-model environments locally. This allows full workspace validation without requiring API credentials or generating third-party billing costs.
*   **Key Code Reference:** [ModelRegistry.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/integration/registry/ModelRegistry.java) | [FakeOpenAiChatClient.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/integration/client/FakeOpenAiChatClient.java)
*   **Learning Guide Reference:** [04_Model_Registry_And_Fake_ChatClients.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Learning/04_Model_Registry_And_Fake_ChatClients.md)

### 4. WorkflowState & Context Janitor
*   **Core Question:** *Why summarize consensus drafts in a separate entity instead of forwarding full message histories?*
*   **Strategic Defense & Rationale:** Sending endless chat lists to LLM APIs quickly saturates model context windows and drives up API invocation costs. The `WorkflowState` entity consolidates active draft content and criticism comments. When history exceeds a configured limit, the Janitor Service purges middle messages in the database while retaining system parameters and the active consolidated draft, reducing token footprints by up to 75%.
*   **Key Code Reference:** [WorkflowState.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/domain/WorkflowState.java) | [WorkflowStateServiceImpl.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/service/WorkflowStateServiceImpl.java)
*   **Learning Guide Reference:** [05_Context_Compression_And_Janitor_Service.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Learning/05_Context_Compression_And_Janitor_Service.md)

### 5. Pessimistic DB Locking
*   **Core Question:** *Why use Pessimistic DB Write Locks on Pipeline status updates?*
*   **Strategic Defense & Rationale:** During multi-agent sequential pipeline executions, the system advances automatically. If a user triggers a "Pause" command while a background thread is finishing a model turn, race conditions can occur, leading to duplicate turns. By acquiring a pessimistic write lock (`select ... for update`), we serialize state updates, ensuring that pause signals halt next-step scheduler queues instantly and safely.
*   **Key Code Reference:** [RoomRepository.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/repository/RoomRepository.java) | [PipelineManagerImpl.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/service/PipelineManagerImpl.java)
*   **Learning Guide Reference:** [07_Pause_And_Intervene_Pipeline_Locking.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Learning/07_Pause_And_Intervene_Pipeline_Locking.md)
