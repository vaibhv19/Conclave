# Conclave — Architecture Defense & Portfolio Review Matrix

This document provides a strategic defense matrix detailing the rationale behind major architectural decisions, answering common technical interview questions, and mapping key patterns directly to the codebase implementation.

---

## 🛡️ Architecture Defense Matrix

### 1. Adapter Design Pattern
*   **Core Question:** *Why not write standard if-else blocks inside the main Chat controller to route prompts to different models?*
*   **Strategic Defense & Rationale:** Direct conditional checks inside controllers violate the Single Responsibility and Open-Closed principles. Different local models (Llama 3, Mistral, Gemma) require very specific chat templates (special tokens, wrapping tags) to perform optimally. The Adapter pattern establishes a strict interface boundary (`ModelAdapter`), isolating template assembly logic from core orchestration and controller code.
*   **Key Code Reference:** [ModelAdapter.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/integration/adapter/ModelAdapter.java)
*   **Learning Guide Reference:** [03_Model_Adapter_Pattern.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Learning/03_Model_Adapter_Pattern.md) (Updated to Model Adapter Pattern)

### 2. Canonical Message Model
*   **Core Question:** *Why persist history in a unified shape instead of caching model-specific JSON payloads?*
*   **Strategic Defense & Rationale:** Caching model-specific raw prompts introduces model lock-in and prevents seamless context transitions. To execute multi-agent workflows where a Llama 3 draft is reviewed by Mistral and audited by Gemma, the backend requires a single, model-agnostic representation. `CanonicalMessage` standardizes metadata, sender roles (`USER`, `AI`, `SYSTEM`), content text, and model identifier tags globally.
*   **Key Code Reference:** [CanonicalMessage.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/domain/CanonicalMessage.java)
*   **Learning Guide Reference:** [03_Model_Adapter_Pattern.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Learning/03_Model_Adapter_Pattern.md) (Updated to Model Adapter Pattern)

### 3. Model Registry & Ollama Clients
*   **Core Question:** *Why use a dynamic registry for local Ollama models rather than cloud provider clients or fake mocks?*
*   **Strategic Defense & Rationale:** Swapping cloud APIs or mock classes for a unified local Ollama server registry ensures high-fidelity inference, zero development or testing cloud costs, and complete data privacy. The registry maps roles to specific Ollama model keys, executing real local model turns. By avoiding simulated clients and external dependencies, we can verify prompt assembly and context limits in a production-equivalent environment locally.
*   **Key Code Reference:** [ModelRegistry.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/integration/registry/ModelRegistry.java)
*   **Learning Guide Reference:** [04_Model_Registry_And_Ollama_Clients.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Learning/04_Model_Registry_And_Ollama_Clients.md)

### 4. WorkflowState & Context Janitor
*   **Core Question:** *Why summarize consensus drafts in a separate entity instead of forwarding full message histories?*
*   **Strategic Defense & Rationale:** Sending endless chat lists to local LLMs quickly saturates their context windows (which are typically smaller, e.g., 4k tokens) and increases GPU VRAM load and processing latency. The `WorkflowState` entity consolidates active draft content and criticism comments. When history exceeds a configured limit, the Janitor Service invokes a local model to summarize progress and purges middle messages in the database while retaining system parameters and the active consolidated draft, reducing token footprints by up to 75% and preventing VRAM exhaustion.
*   **Key Code Reference:** [WorkflowState.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/domain/WorkflowState.java) | [WorkflowStateServiceImpl.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/service/WorkflowStateServiceImpl.java)
*   **Learning Guide Reference:** [05_Context_Compression_And_Janitor_Service.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Learning/05_Context_Compression_And_Janitor_Service.md)

### 5. Pessimistic DB Locking
*   **Core Question:** *Why use Pessimistic DB Write Locks on Pipeline status updates?*
*   **Strategic Defense & Rationale:** During multi-agent sequential pipeline executions, the system advances automatically. If a user triggers a "Pause" command while a background thread is finishing a model turn, race conditions can occur, leading to duplicate turns. By acquiring a pessimistic write lock (`select ... for update`), we serialize state updates, ensuring that pause signals halt next-step scheduler queues instantly and safely.
*   **Key Code Reference:** [RoomRepository.java](file:///d:/Coding/Projects----For%20Resume/Conclave/repository/RoomRepository.java) | [PipelineManagerImpl.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/service/PipelineManagerImpl.java)
*   **Learning Guide Reference:** [07_Pause_And_Intervene_Pipeline_Locking.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Learning/07_Pause_And_Intervene_Pipeline_Locking.md)
