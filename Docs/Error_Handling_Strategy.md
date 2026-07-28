# Error Handling Strategy: Conclave

This document defines the centralized exception model, REST error payloads, and asynchronous WebSocket error propagation patterns for the **Conclave** platform.

---

## 1. The Challenge of Async & Sync Exceptions

Handling exceptions in an AI orchestration platform is complex because errors can occur in two distinct execution environments:
1.  **Synchronous REST Requests:** Simple client-server HTTP transactions (e.g. logging in, creating rooms). Exceptions here must translate directly to standard HTTP status codes (4xx, 5xx) and JSON bodies.
2.  **Asynchronous Orchestration Loops:** Long-running LLM stream generations executing on background **Virtual Threads**. If a third-party API rate limit (429) or JSON parsing failure occurs here, the client HTTP connection is already closed. The exception must be caught and broadcast to all subscribers over WebSockets via STOMP events.

---

## 2. Centralized Exception Model

Conclave defines a structured hierarchy of custom exceptions inheriting from a runtime base `ConclaveException`:

```
                    ┌──────────────────────────────────┐
                    │        ConclaveException         │
                    │        (Runtime Base Class)      │
                    └────────────────┬─────────────────┘
                                     │
       ┌──────────────────┬──────────┴────────┬───────────────────┐
┌──────▼──────┐    ┌──────▼──────┐     ┌──────▼──────┐     ┌──────▼──────┐
│  Resource   │    │Unauthorized │     │Orchestration│     │ Translation │
│  NotFound   │    │   Access    │     │  Exception  │     │  Exception  │
│  Exception  │    │  Exception  │     │ (Pipeline/  │     │ (Adapter    │
│  (404)      │    │  (403)      │     │  Mentions)  │     │  Mapping)   │
└─────────────┘    └─────────────┘     └─────────────┘     └─────────────┘
```

### 2.1 Exceptions Registry

*   **`ConclaveException`:** The base runtime exception for the application.
*   **`ResourceNotFoundException` (404):** Thrown when a room, message, or user UUID is not found in the database.
*   **`UnauthorizedAccessException` (403):** Thrown when a user attempts to access or mutate a room owned by another account.
*   **`OrchestrationException` (400):** Thrown when workflow operations violate constraints (e.g., submitting messages to a paused room, sending messages with no role mention, or requesting unregistered model IDs).
*   **`TranslationException` (400):** Thrown by `ProviderAdapter` implementations if serialization constraints are violated (e.g., consecutive roles in `GeminiAdapter`).
*   **`EmailAlreadyExistsException` (409):** Thrown during registration if the user's email is already registered.
*   **`InvalidMappingException` (400):** Thrown if role assignments map invalid parameters.

---

## 3. Synchronous HTTP Error Handling (`@RestControllerAdvice`)

Synchronous controller exceptions are intercepted by `GlobalExceptionHandler`:

```
   Controller/Service throws Exception
                 │
                 ▼
   GlobalExceptionHandler Interception
                 │
                 ├──> Resolves Exception Type
                 └──> Builds Standard JSON ErrorResponse
                 │
                 ▼
   Client receives standardized JSON payload + HTTP status code
```

*   **REST Error Payload Shape:**
    ```json
    {
      "status": 400,
      "error": "Bad Request",
      "message": "No role assignment found in room matching mention: @Critic",
      "timestamp": "2026-07-28T10:48:00.123"
    }
    ```
*   **Method Handler Mapping:**
    *   `ResourceNotFoundException` &rarr; `@ResponseStatus(HttpStatus.NOT_FOUND)`
    *   `UnauthorizedAccessException` &rarr; `@ResponseStatus(HttpStatus.FORBIDDEN)`
    *   `EmailAlreadyExistsException` &rarr; `@ResponseStatus(HttpStatus.CONFLICT)`
    *   `OrchestrationException` / `TranslationException` &rarr; `@ResponseStatus(HttpStatus.BAD_REQUEST)`
    *   Generic `Exception` &rarr; `@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)`

---

## 4. Asynchronous WebSocket Error Handling

For async streaming tasks running on background Virtual Threads (within `MessageOrchestratorImpl.executeStreamingTurnAsync`), errors are handled within a try-catch block to prevent thread deaths from leaking:

```java
// MessageOrchestratorImpl.java
try {
    // 1. Acquire pessimistic lock
    // 2. Resolve adapter & call model stream
    // 3. Broadcast chunks
} catch (Exception e) {
    log.error("Exception occurred during asynchronous AI turn execution", e);
    
    // Broadcast fallback error message to STOMP channel
    messagingTemplate.convertAndSend(
        "/topic/room/" + roomId,
        new SystemInterventionEvent("Execution error: " + e.getMessage())
    );
}
```

### 4.1 Asynchronous Error Recovery Flow:
1.  **Error Interception:** If an exception occurs (e.g., Gemini API key missing, network timeout, rate limit exceeded), the `catch` block captures it.
2.  **Notification Broadcast:** The backend compiles a `SystemInterventionEvent` with the error text and sends it to the STOMP destination `/topic/room/{roomId}`.
3.  **UI Warning Display:** The React client receives the `SYSTEM_INTERVENTION` frame. The Zustand store appends the error notification, and the UI mounts the diagonally striped warning deck (Alert Banner), allowing the user to view the error and manually intervene.

---

## 5. Database Transaction Rollback Rules

To maintain data consistency under transactional exceptions:
*   **Automatic Rollback:** Core database writes (like saving user messages and updating status values) are protected by Spring's `@Transactional` annotation. If any runtime exception (`ConclaveException`) is thrown during the execution scope, Spring JPA initiates a transaction rollback.
*   **Preventing Orphaned Records:** If a model invocation fails *before* the first response chunk is written, the placeholder message is rolled back, preventing empty or half-completed entries from cluttering `conversation_history`.
*   **Lock Release:** Any acquired pessimistic write locks (`findWithLockById`) are automatically released when the transaction rolls back, preventing database deadlock scenarios.

---

## 6. Interview Talking Points (Architectural Defense)

*   **Unified Exceptions Strategy:** "We separate REST and WebSocket exceptions. While REST endpoint validation maps directly to standard HTTP status codes via a global controller advice, async worker thread errors are caught inside the run loops and translated into real-time STOMP event frames (`SYSTEM_INTERVENTION`), ensuring the frontend is instantly notified of backend pipeline failures."
*   **Async Thread Safety:** "Because async streaming processes run on separate Virtual Threads, they cannot rely on Tomcat's request-lifecycle transaction context. We handle exceptions locally inside each worker runnable to ensure that transactional rollbacks are triggered and pessimistic locks are released safely if an LLM call fails."
