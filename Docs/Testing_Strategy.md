# Testing Strategy: Conclave

This document defines the testing methodology, validation tiers, and concurrency simulation strategies used to verify the reliability of the **Conclave** platform.

---

## 1. The Challenge of AI & Real-Time Testing

Testing a multi-provider AI orchestration platform presents unique challenges:
*   **API Non-Determinism:** Local LLM outputs yield different responses for identical inputs, making exact-string assertions unstable.
*   **Inference Performance & Latency:** Running local model inference during automated test cycles can consume significant CPU/GPU resources and slow down execution.
*   **Real-time Synchronization:** Verifying that WebSocket STOMP chunks are broadcast in the correct order requires testing async event loops.
*   **Concurrency Race Conditions:** Verifying that pessimistic database locks block concurrent status changes requires simulating simultaneous thread execution.

To solve this, Conclave implements a **three-tiered testing strategy** combining static unit testing, embedded database integration testing, and local Ollama-based integration simulation.

---

## 2. Three-Tiered Testing Strategy

```
  ┌────────────────────────────────────────────────────────┐
  │              Unit Testing Tier (JUnit 5)               │
  │  * Target: ProviderAdapter implementations             │
  │  * Methods: Translates CanonicalMessage log to prompts │
  │  * Mocking: Mockito (no network calls, 100% stable)    │
  └───────────────────────────┬────────────────────────────┘
                              │
  ┌───────────────────────────▼────────────────────────────┐
  │        Integration Testing Tier (@SpringBootTest)      │
  │  * Target: ModelRegistry, Transactions & JPA           │
  │  * Database: Embedded H2 / PostgreSQL container        │
  │  * Concurrency: Latch-controlled multi-threaded test   │
  └───────────────────────────┬────────────────────────────┘
                              │
  ┌───────────────────────────▼────────────────────────────┐
  │      Local Integration & End-to-End Testing            │
  │  * Target: WebSockets, STOMP broadcasts, UI updates   │
  │  * Execution: UI + local Ollama test configurations   │
  │  * Cost: $0 real API spend (runs offline locally)      │
  └────────────────────────────────────────────────────────┘
```

---

## 3. Tier 1: Unit Testing (Adapter Mapping Validation)

Unit tests focus on proving the correctness of the adapter template generation. They run 100% offline, verify formatting logic, and check boundary conditions:
*   **Prompt Formatting Verification:** Asserts that a `List<CanonicalMessage>` transforms into a valid target-specific chat template prompt (e.g. checking that Llama 3 adapters output correct control tags like `<|start_header_id|>system<|end_header_id|>`).
*   **Enforcing Validation Constraints:** Tests that the adapters fail gracefully if history formatting constraints (like token length checks) are violated, confirming boundary validations work before network calls are made.

---

## 4. Tier 2: Integration Testing (Registry & Concurrency Locks)

Integration tests verify that components integrate with the Spring application context and the database layer using `@SpringBootTest` and active testing profiles:

### 4.1 Registry Bean Verification
Tests that the `ModelRegistry` dynamically resolves client beans based on assignments:
```java
@SpringBootTest
@ActiveProfiles("test")
class ModelRegistryTest {
    @Autowired
    private ModelRegistry modelRegistry;

    @Test
    void testResolveChatModel() {
        ChatModel model = modelRegistry.getClient("llama3");
        assertNotNull(model);
        assertTrue(model instanceof OllamaChatModel);
    }
}
```

### 4.2 Concurrency & Pessimistic Lock Testing
To verify that `PESSIMISTIC_WRITE` locks block concurrent state modifications safely during pipeline advances:
*   **The Setup:** A test spawns two concurrent threads using Java's `ExecutorService` and synchronizes their start times using a `CountDownLatch`.
*   **Thread 1 (Orchestration Turn):** Calls `MessageOrchestrator.executeStreamingTurnAsync`, acquiring a pessimistic write lock on Room A.
*   **Thread 2 (User Pause Command):** Simultaneously calls `PipelineManager.pausePipeline` to alter Room A.
*   **The Assertion:** The test verifies that Thread 2 is blocked (waits) until Thread 1 commits its transaction and releases the lock, confirming that status transitions occur sequentially.

---

## 5. Tier 3: Local Ollama End-to-End Testing

End-to-End tests verify the real-time sync between the backend services, the WebSocket broker, and the React UI:
*   **Local Test Configurations:** The testing suite is configured to point to a local Ollama server running lightweight models (e.g., Qwen 1.5B or Gemma 2B) for rapid local inference. This allows running full collaborative pipelines (Writer &rarr; Critic &rarr; Reviewer) locally.
*   **WebSocket Verification:** Tests verify that STOMP subscribers receive `TURN_STARTED`, `CONTENT_CHUNK`, and `TURN_COMPLETED` packets in sequence, and that the local Zustand store appends text chunks without gaps.
*   **Compaction Auditing:** Verifies that once history exceeds 10 messages, the Context Janitor is triggered, the draft is updated via the local summarizer model, and middle messages are purged from the database.

---

## 6. Interview Talking Points (Architectural Defense)

*   **Offline Adapter Validation:** "We separate prompt template formatting verification from runtime inference. Rather than making live calls or proxying endpoints, we test the template mappers in isolation, asserting that canonical histories translate exactly to Llama/Mistral/Gemma special token structures. This ensures formatting accuracy before we hit the Ollama API."
*   **Test Suite Efficiency with Lightweight Models:** "To prevent GPU overhead during local testing, our test profiles route inference calls to highly optimized, lightweight local models (e.g., Gemma 2B or Qwen 1.5B) run via Ollama. This demonstrates a production-ready testing setup where 100% real inference is verified with zero cloud billing and minimal local latency."
*   **Simulating Concurrency Locks:** "We test database concurrency by executing parallel database queries synchronized via a `CountDownLatch`. This forces two transactions to hit the database at the same instant, proving that the pessimistic lock (`SELECT FOR UPDATE`) locks the row and forces concurrent requests to execute sequentially rather than causing race conditions."
