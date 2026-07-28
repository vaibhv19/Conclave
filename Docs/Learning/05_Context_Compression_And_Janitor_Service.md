# Learning 05: Context Compression & Janitor Service

## 1. Problem Statement
Collaborative conversations involving multiple models generate massive quantities of history messages. Sending full history logs back and forth to LLM APIs results in context window saturation, exponential growth in API costs, and latency degradation. Developers need a way to purge intermediate details while preserving the consolidated output draft.

## 2. Decision Rationale
We implemented a **Context Compression** algorithm managed by a background **Janitor Service**:
- We extract the central consensus value to a single, persistent state entity: `WorkflowState` (storing the current unified draft and review comments).
- When the history length exceeds a configured threshold (e.g. 10 messages), the Janitor Service purges intermediate chat messages in the database, retaining only the latest state summary and the user's initial objectives.

## 3. Alternatives Considered
- **No Compression (Full History):** Rejected due to high token cost and risk of exceeding context window limits in multi-agent discussions.
- **Client-Side Slicing:** Rejected because if the client goes offline, the server loses the reference context. Context management must reside securely on the database layer.

## 4. Internal Working
1.  **State Consolidation:** After a model turn completes, the orchestrator updates `WorkflowState` with the latest summary.
2.  **Threshold Check:** The service checks `conversationHistoryRepository.countByRoomId(roomId)`.
3.  **Purging (Janitor):** If count exceeds threshold, the Janitor keeps the first 2 messages (system prompt/objective) and last 2 messages (the final model turns), deleting all middle logs and saving DB space.

## 5. Conclave Implementation
- Persistent states are mapped in [WorkflowState.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/domain/WorkflowState.java).
- Purge thresholds and cleanup execution are managed inside [WorkflowStateServiceImpl.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/service/WorkflowStateServiceImpl.java).
- Database deletion queries are defined in [ConversationHistoryRepository.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/repository/ConversationHistoryRepository.java).

## 6. Key Classes
- [WorkflowState.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/domain/WorkflowState.java) - Holds draft context and comments.
- [WorkflowStateServiceImpl.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/service/WorkflowStateServiceImpl.java) - Runs context compression loops.
- [ConversationHistoryRepository.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/repository/ConversationHistoryRepository.java) - Purges intermediate logs.

## 7. Common Pitfalls
- **Accidental Objective Deletion:** Failing to preserve the user's initial objective message when purging means the model loses the instructions, resulting in subsequent generations going off-topic.
- **Concurrent Modification Exception:** If multiple models complete their turns simultaneously and try to trigger the Janitor on the same room, database lock collisions can occur.

## 8. Debugging Tips
- Inspect logs for `Purging X middle messages from history` statements from `WorkflowStateServiceImpl`.
- Run database queries on `conversation_history` to verify that message counts drop immediately after exceeding thresholds.

## 9. Interview Questions
1.  *What is the purpose of the WorkflowState entity in Conclave, and how does it save token costs?*
2.  *Walk me through your database message purging logic. Which messages are preserved, and which are deleted?*
3.  *How do you prevent race conditions when the Janitor service runs during concurrent model turns?*

## 10. References
- [Spring Data JPA Repository Spec](https://spring.io/projects/spring-data-jpa)
- [Context Window Optimization in LLMs](https://arxiv.org/abs/2307.03172)
