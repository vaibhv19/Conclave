# Learning 04: Model Registry & Fake ChatClients

## 1. Problem Statement
Running end-to-end integration tests and building UI layouts with actual LLM API endpoints introduces high billing costs, latency spikes, and network dependencies. If model API limits are reached, the local test suites fail. Furthermore, developers need a way to run and test model executions locally without requiring active API keys.

## 2. Decision Rationale
We implemented a **Model Registry** pattern combined with **Fake ChatClients** (mock adapters):
- A centralized Model Registry tracks available model engines and dynamically resolves the appropriate `ChatClient` bean at runtime based on the configured room model.
- Fake clients simulate streaming responses chunk-by-chunk over WebSockets, reproducing latency characteristics and token counts of actual LLM responses locally without performing external network requests.

## 3. Alternatives Considered
- **WireMock Mock Server:** Rejected because mock HTTP servers do not execute standard internal java mapper mappings, making it harder to test Java-side data translations and sequential state transitions.
- **Direct Spring Profiling (@Profile):** Rejected because it locks the backend to one provider globally. We need different model types running simultaneously in a single chat room.

## 4. Internal Working
1.  **Registry Mapping:** When the orchestrator processes a role turn, it queries `ModelRegistry.getAdapter(modelId)`.
2.  **Dynamic Resolution:** The registry holds a key-value mapping of model identifiers (e.g. `GEMINI_PRO`, `FAKE_OPENAI`) to their corresponding bean implementations.
3.  **Chunk Generation:** Fake clients generate a responsive sentence, split it into chunks, and use Virtual Threads / scheduler queues to simulate realistic word-by-word typing latency.

## 5. Conclave Implementation
- Available models and mapping keys are tracked inside [ModelRegistry.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/integration/registry/ModelRegistry.java).
- Standard Gemini endpoints are integrated in [GeminiChatClient.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/integration/client/GeminiChatClient.java).
- Simulated fakes are implemented inside [FakeOpenAiChatClient.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/integration/client/FakeOpenAiChatClient.java) and [FakeClaudeChatClient.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/integration/client/FakeClaudeChatClient.java).

## 6. Key Classes
- [ModelRegistry.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/integration/registry/ModelRegistry.java) - Orchestrates model mapping resolutions.
- [FakeOpenAiChatClient.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/integration/client/FakeOpenAiChatClient.java) - Simulates GPT-4o streaming.
- [FakeClaudeChatClient.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/integration/client/FakeClaudeChatClient.java) - Simulates Claude Sonnet streaming.

## 7. Common Pitfalls
- **Unregistered Model Identifier:** If a room setup includes a model ID not mapped inside the `ModelRegistry`, the pipeline throws an `OrchestrationException` at runtime.
- **Mock Latency Thread Blocks:** Using `Thread.sleep` inside mock clients block physical platform threads unless run inside virtual threads or asynchronous executors.

## 8. Debugging Tips
- Trace model resolution by placing a logger point inside `ModelRegistry.getAdapter`.
- Toggle profile switches inside application properties to verify fake client mappings.

## 9. Interview Questions
1.  *How does the ModelRegistry resolve the appropriate client bean at runtime in Conclave?*
2.  *How do your Fake ChatClients simulate streaming latency chunk-by-chunk without blocking the platform thread pool?*
3.  *What exception is thrown when a user triggers a turn using an unsupported model ID, and how is it handled?*

## 10. References
- [Spring Dependency Injection Registry Pattern](https://spring.io)
- [Reactive Streams Spec: Publisher & Subscriber](https://www.reactive-streams.org/)
