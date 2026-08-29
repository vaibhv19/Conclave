# Phase 03 — Room Management API

## 1. Module Planning: Room Management

### 1.1 Purpose
The purpose of this phase is to construct the Room Management module. It handles session blueprint setup, room initialization (status: `INITIALIZED`), dynamic mapping of model roles (e.g., Writer -> Gemini), validation of UI color schemas, and verification of room owner security context.

### 1.2 Package / Folder Structure
```
backend/src/main/java/com/conclave/
├── dto/
│   ├── RoomCreateRequest.java
│   ├── RoleAssignmentDTO.java
│   ├── RoomResponse.java
│   └── WorkflowStateDTO.java
├── service/
│   └── RoomService.java               # Business logic for room transactions
├── controller/
│   └── RoomController.java             # REST controller (/api/rooms)
└── exception/
    ├── ResourceNotFoundException.java
    ├── UnauthorizedAccessException.java
    └── InvalidMappingException.java
```

### 1.3 Responsibilities & Dependencies
- **Room Lifecycle:** Manages room status transitions (`INITIALIZED` -> `ACTIVE` -> `PAUSED` -> `ARCHIVED`). Upon room creation, it must automatically create a linked `WorkflowState` instance.
- **Validation Engine:** Enforces validation rules:
  - `modelId` must exist in the supported model list (`LLAMA3`, `MISTRAL`, `GEMMA`).
  - `uiColorHex` must be a valid 6-character hex code starting with `#` (`^#[0-9A-Fa-f]{6}$`).
  - Uniqueness of role names within the scope of the room (`room_id`, `role_name`).
- **Security Dependency:** Pulls the logged-in user details from the security context to set `owner_id`. Restricts role assignment updates and room retrieval to the room owner.

---

## 2. Module Components

### 2.1 REST Endpoints & Payloads

#### `POST /api/rooms` (Create Room)
- **Input:** `RoomCreateRequest` (name, objective, roleAssignments list).
- **Validation:** Minimum 1 role assignment required. Objective cannot be empty.
- **Output:** `RoomResponse` (roomId, name, objective, status: `INITIALIZED`, roleAssignments, workflowState).

#### `GET /api/rooms/{id}` (Fetch Room Detail)
- **Input:** Path Variable `id` (UUID).
- **Output:** `RoomResponse`.

#### `PUT /api/rooms/{id}/role-assignments` (Update Mappings)
- **Input:** `List<RoleAssignmentDTO>`.
- **Output:** `RoomResponse`.

### 2.2 Exception Handling & Security
- Check ownership during `GET` and `PUT` calls. If `owner_id` does not match the active JWT subject, return a 403 Forbidden payload.
- Return structured API errors for validation failures (e.g., duplicate roles or invalid color codes).

---

## 3. Atomic Implementation Tasks

### Task 3.1: Create Room Management DTOs
- **Estimated Size:** S
- **Risk:** Low
- **Prerequisites:** Phase 02 Domain Models
- **Definition of Done:**
  - Create DTO classes: `RoomCreateRequest.java`, `RoleAssignmentDTO.java`, `RoomResponse.java`, and `WorkflowStateDTO.java`.
  - Annotate fields with validation constraints (e.g., `@Pattern(regexp = "^#[0-9A-Fa-f]{6}$")` for colors, `@NotBlank` for names).
  - Classes compiled.

### Task 3.2: Implement RoomService Business Logic
- **Estimated Size:** M
- **Risk:** Low
- **Prerequisites:** Task 3.1
- **Definition of Done:**
  - Create `RoomService.java` with `@Transactional` support.
  - Implement `createRoom(RoomCreateRequest request, User owner)`:
    - Verifies role assignment colors.
    - Saves `Room` entity (status `INITIALIZED`).
    - Saves corresponding list of `RoleAssignment` entities.
    - Instantiates and saves an empty `WorkflowState` record (with `currentDraft` and `reviewComments` initialized to empty).
  - Implement `getRoomById(UUID roomId, User currentUser)` and check ownership.
  - Implement `updateRoleAssignments(UUID roomId, List<RoleAssignmentDTO> newAssignments, User currentUser)`:
    - Fetches the room, verifies ownership.
    - Purges existing assignments and saves the new list after validating unique role names and correct model assignments.
  - Write service unit tests verifying room configurations persist correctly.

### Task 3.3: Configure Room REST Controller and Endpoint Validation
- **Estimated Size:** M
- **Risk:** Low
- **Prerequisites:** Task 3.2
- **Definition of Done:**
  - Create `RoomController.java` mapped to `/api/rooms`.
  - Annotate with `@RestController`. Secure methods with `@AuthenticationPrincipal` to inject current user details.
  - Add POST `/` endpoint using `@Valid` to trigger automatic request checks.
  - Add GET `/{id}` endpoint returning the room details.
  - Add PUT `/{id}/role-assignments` endpoint.
  - Implement global controller advice to map `InvalidMappingException` or `UnauthorizedAccessException` to HTTP 400 and 403 standard error payloads.
  - Integration tests verify that validation failures trigger a 400 Bad Request.

---

## 4. Documentation & Verification

### Documentation to Update / Create
- Update REST API contract documentation to verify room creation payload structures.
- Document validation policies (hex codes, maximum character limits) for rooms and assignments.

### Testing Checkpoint
- Mock MVC endpoint tests verifying that posting an invalid color code (e.g. `red` or `#123`) fails validation.
- Integration test checking that User B attempting to view User A's room via GET `/api/rooms/{id}` fails with a 403 Forbidden response.

### Suggested Git Commit Boundaries
1. `dto: create room management request and response payloads`
2. `service: implement RoomService room creation and mapping update logic`
3. `controller: expose RoomController endpoints with validation and exception handling`

### Suggested GitHub Issues
- **Issue 3.1:** Create Room management DTO wrappers. (Points: 1)
- **Issue 3.2:** Build RoomService transactional logic for room setups. (Points: 2)
- **Issue 3.3:** Expose REST controller mappings for Room operations. (Points: 2)
