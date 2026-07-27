# Phase 08 — Pipeline Control System (Pause & Intervene)

## 1. Module Planning: Pipeline Control System

### 1.1 Purpose
The purpose of this phase is to construct the pipeline orchestration controls. This implements the "Pause & Intervene" execution logic, allowing a user to halt sequential model pipelines (e.g., Lead-Writer -> Code-Critic), inject corrections as priority system context, and resume execution with updated context.

### 1.2 Package / Folder Structure
```
backend/src/main/java/com/conclave/
├── service/
│   └── PipelineManager.java            # Controls sequence flow and lock states
└── controller/
    └── ChatController.java             # Exposes /pipeline/pause and /pipeline/resume
```

### 1.3 Responsibilities & Dependencies
- **Pipeline State Machine:** Operates on the Room status field (`ACTIVE`, `PAUSED`). Uses the Room columns `pipeline_sequence` (JSON/text list of role names) and `current_pipeline_index` (Integer) to manage step execution.
- **Thread-Safe Locking:** Uses database Pessimistic Locking (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) on the `Room` record during status updates to prevent race conditions (e.g., user clicks Pause at the exact millisecond a model turn completes and attempts to start the next one).
- **Intervention Injection:** When a message is sent with `isIntervention: true`:
  - Appends it to history as a `SYSTEM`/`USER` message.
  - Triggers the WorkflowState summarizer to re-evaluate and rebuild the draft/comments.
  - Broadcasts the `SYSTEM_INTERVENTION` event containing updated summaries.
  - Halts progress until `POST /pipeline/resume` is explicitly invoked.

---

## 2. Module Components

### 2.1 Mapped REST Endpoints

#### `POST /api/chat/pipeline/pause`
- **Input:** `{ "roomId" }`
- **Output:** `{ "status": "PAUSED" }`
- **Operation:** Locks room, changes status to `PAUSED`.

#### `POST /api/chat/pipeline/resume`
- **Input:** `{ "roomId" }`
- **Output:** `{ "status": "ACTIVE" }`
- **Operation:** Locks room, sets status to `ACTIVE`, increments pipeline pointer, and triggers the next model async thread execution task.

#### `POST /api/chat/message` with `isIntervention: true`
- **Operation:** Injects manual feedback, updates context, broadcasts `SYSTEM_INTERVENTION` event.

---

## 3. Atomic Implementation Tasks

### Task 8.1: Implement PipelineManager State Controls
- **Estimated Size:** M
- **Risk:** High
- **Prerequisites:** Phase 06 & Phase 07 Setup
- **Definition of Done:**
  - Create `PipelineManager.java` managing room status transactions.
  - Implement pessimistic db locking on `RoomRepository.findWithLockById(UUID id)` to acquire record lock.
  - Implement state transition checks. Validates that status updates between `ACTIVE` and `PAUSED` perform correctly.
  - Service unit tests verify locks are obtained and race conditions are mitigated.

### Task 8.2: Implement Pipeline Control REST Endpoints
- **Estimated Size:** S
- **Risk:** Low
- **Prerequisites:** Task 8.1
- **Definition of Done:**
  - Map endpoints `/api/chat/pipeline/pause` and `/api/chat/pipeline/resume` inside `ChatController.java`.
  - Implement security checks: verify user request matches room owner context.
  - Handshake controllers verify status returns `PAUSED` or `ACTIVE` response payloads.

### Task 8.3: Develop Pipeline Sequential Execution and Intervention Logic
- **Estimated Size:** L
- **Risk:** High
- **Prerequisites:** Task 8.2
- **Definition of Done:**
  - Integrate pipeline sequencer with `MessageOrchestrator`:
    - After completing a turn, if status is `ACTIVE` and index < list size, increment `current_pipeline_index` and invoke the next model async.
    - If status is `PAUSED`, halt next execution block.
  - Implement manual intervention flow:
    - If `isIntervention: true` is received, insert message, run Janitor, and publish WebSocket `SYSTEM_INTERVENTION`.
  - Integration tests verify that pausing halts sequence, intervention modifies draft context, and resume triggers the next model with updated context.

---

## 4. Documentation & Verification

### Documentation to Update / Create
- Create `Docs/Learning/07_Pause_And_Intervene_Pipeline_Locking.md` detailing:
  - Database locking patterns (Pessimistic Write) used to prevent status collisions.
  - State chart diagram showing pipeline loop states.

### Testing Checkpoint
- Perform concurrent test suite execution: Simulate multi-model pipeline trigger and send a high-frequency pause request. Verify that the room status remains locked and the second model does not execute.

### Suggested Git Commit Boundaries
1. `service: implement thread-safe locking and state transitions in PipelineManager`
2. `controller: expose REST mappings for pipeline pause and resume actions`
3. `orchestration: implement sequential loops and manual context intervention injectors`

### Suggested GitHub Issues
- **Issue 8.1:** Develop Pipeline state transitions with database locks. (Points: 2)
- **Issue 8.2:** Expose REST controllers for Pause and Resume actions. (Points: 1)
- **Issue 8.3:** Integrate sequential execution threads with intervention logic. (Points: 3)
