# Learning 07: Pause & Intervene Pipeline Locking

## 1. Problem Statement
In a sequential multi-agent execution pipeline (e.g., Writer -> Editor -> Critic), the backend auto-advances the turn when a model completes its generation. However, if a user spots a mistake and wants to pause execution or inject corrections (intervene) immediately, race conditions arise if the next model starts generating simultaneously. The system needs a thread-safe locking mechanism to stop the pipeline instantly.

## 2. Decision Rationale
We implemented a database-backed **Pessimistic Write Lock** strategy combined with a **Pipeline Control State Manager**:
- By querying `RoomRepository.findWithLockById(UUID roomId)` with a pessimistic write lock (`PESSIMISTIC_WRITE`), we block concurrent threads from advancing the room pipeline state.
- If a pause request is received, the lock ensures that the currently executing thread updates the status to `PAUSED` and halts next-step scheduler queues.

## 3. Alternatives Considered
- **In-Memory Thread Flags (ConcurrentHashMap):** Rejected because in-memory flags do not scale horizontally across cluster nodes and lose state consistency during server restarts.
- **Optimistic Locking (@Version):** Rejected because optimistic locks fail silently with an exception upon conflict, rather than blocking the execution flow to check status flags.

## 4. Internal Working
1.  **Lock Acquisition:** When an AI turn starts or completes, `PipelineManager` locks the room row in the DB.
2.  **State Audit:** The orchestrator checks if status has transitioned to `PAUSED` or if the user injected an intervention.
3.  **Halt or Advance:** If status is `ACTIVE` and not at the end of the sequence, the pipeline schedules the next role Async. If `PAUSED`, it stops and releases the lock.

## 5. Conclave Implementation
- Pessimistic write lock queries are defined in [RoomRepository.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/repository/RoomRepository.java).
- Room pipeline controls (pause, resume) are managed by [PipelineManagerImpl.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/service/PipelineManagerImpl.java).
- Orchestration loop execution is governed by [MessageOrchestratorImpl.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/service/MessageOrchestratorImpl.java).

## 6. Key Classes
- [RoomRepository.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/repository/RoomRepository.java) - Declares Pessimistic DB Lock queries.
- [PipelineManagerImpl.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/service/PipelineManagerImpl.java) - Holds pause, resume, ownership validations.
- [MessageOrchestratorImpl.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/service/MessageOrchestratorImpl.java) - Auto-advances sequential pipelines.

## 7. Common Pitfalls
- **Deadlock Risks:** If multiple threads try to acquire locks on multiple rooms in different order, deadlocks can happen. Ensure database locks are only acquired per single room ID.
- **Long-Running Database Transactions:** Keeping a transaction open during LLM API call network I/O stalls database resources. The lock must only be held briefly while updating status or indexing indices.

## 8. Debugging Tips
- Trace lock acquisitions by checking Hibernate SQL console statements (`select ... for update`).
- Monitor thread states inside `PipelineManagerImpl` logs during Pause / Resume controls validation.

## 9. Interview Questions
1.  *Why did you select Pessimistic DB locking over Optimistic locking for Conclave's pipeline control system?*
2.  *How do you prevent database transaction pool exhaustion when a model is taking a long time to call external LLM APIs?*
3.  *What happens to the sequential sequence when a user submits an intervention message while the status is PAUSED?*

## 10. References
- [Java Platform Locking Guide](https://docs.oracle.com/javase/tutorial/essential/concurrency/locksync.html)
- [Baeldung: Hibernate Pessimistic Locking](https://www.baeldung.com/jpa-pessimistic-locking)
