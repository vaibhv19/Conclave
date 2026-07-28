# Testing Strategy: Conclave

This document defines the testing methodology, validation tiers, and concurrency simulation strategies used to verify the reliability of the **Conclave** platform.

---

## 1. The Challenge of AI & Real-Time Testing

Testing a multi-provider AI orchestration platform presents unique challenges:
*   **API Non-Determinism:** Real LLM APIs yield different responses for identical inputs, making assertions unstable.
*   **Financial Cost:** Running full end-to-end integration tests using real API keys (e.g. Gemini, OpenAI) during CI/CD runs incurs continuous cost.
*   **Real-time Synchronization:** Verifying that WebSocket STOMP chunks are broadcast in the correct order requires testing async event loops.
*   **Concurrency Race Conditions:** Verifying that pessimistic database locks block concurrent status changes requires simulating simultaneous thread execution.

To solve this, Conclave implements a **three-tiered testing strategy** combining static unit testing, embedded database integration testing, and mock-driven end-to-end simulation.

---

## 2. Three-Tiered Testing Strategy

```
  ┌────────────────────────────────────────────────────────┐
  │              Unit Testing Tier (JUnit 5)               │
  │  * Target: ProviderAdapter implementations             │
  │  * Methods: Translates static JSON strings to objects  │
  │  * Mocking: Mockito (no network calls, 100% stable)    │
  └───────────────────────────┬────────────────────────────┘
                              │
  ┌───────────────────────────▼────────────────────────────┐
  │        Integration Testing Tier (@SpringBootTest)      │
  │  * Target: ModelRegistry, Transactions & JPA           │
  │  * Database: Embedded H2 / PostgreSQL container        │
  │  * concurrency: Latch-controlled multi-threaded test   │
  └───────────────────────────┬────────────────────────────┘
                              │
  ┌───────────────────────────▼────────────────────────────┐
  │         Mock-Driven End-to-End simulation              │
  │  * Target: WebSockets, STOMP broadcasts, UI updates   │
  │  * Execution: UI + FakeChatClient beans                │
  │  * Cost: $0 real API spend (runs offline)              │
  └────────────────────────────────────────────────────────┘
```

---

## 3. Tier 1: Unit Testing (Adapter Mapping Validation)

Unit tests focus on proving the mathematical correctness of the adapter translation code. They run 100% offline, verify formatting logic, and check boundary conditions:
*   **Input Mocking:** Static JSON templates of raw OpenAI and Gemini response payloads are loaded from resource directories.
*   **Assertions:**
    *   Verifies that a `List<CanonicalMessage>` transforms into a valid target-specific JSON request payload (e.g. testing that Claude's system prompt is extracted into the root parameter).
    *   Verifies that the target response converts back into a `CanonicalMessage`.
*   **Enforcing Validation Constraints:** Tests that `GeminiAdapter` throws a `TranslationException` if sequential user or model messages are passed, confirming boundary validations work before network calls are made.

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
    void testResolveChatClient() {
        ChatClient client = modelRegistry.getClient("FAKE_CLAUDE");
        assertNotNull(client);
        assertTrue(client instanceof FakeClaudeChatClient);
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

## 5. Tier 3: Mock-Driven End-to-End Simulation

End-to-End tests verify the real-time sync between the backend services, the WebSocket broker, and the React UI:
*   **No-Cost Pipelines:** By configuring the active profile to mock-driven stubs, testers can run complete multi-model workflow runs (e.g., Writer &rarr; Critic &rarr; Reviewer) locally.
*   **WebSocket Verification:** Tests verify that STOMP subscribers receive `TURN_STARTED`, `CONTENT_CHUNK`, and `TURN_COMPLETED` packets in sequence, and that the local Zustand store appends text chunks without gaps.
*   **Compaction Auditing:** Verifies that once history exceeds 10 messages, the Context Janitor is triggered, the draft is updated, and middle messages are purged from the database.

---

## 6. Interview Talking Points (Architectural Defense)

*   **Offline Adapter Validation:** "We separate API integration testing from mapping correctness. Rather than making live calls or proxying endpoints via WireMock, we load static vendor JSON responses as resources and test the adapters in isolation. This allows us to guarantee schema translation correctness offline, without incurring API costs or dealing with network instability."
*   **Simulating Concurrency Locks:** "We test database concurrency by executing parallel database queries synchronized via a `CountDownLatch`. This forces two transactions to hit the database at the same instant, proving that the pessimistic lock (`SELECT FOR UPDATE`) locks the row and forces concurrent requests to execute sequentially rather than causing race conditions."
