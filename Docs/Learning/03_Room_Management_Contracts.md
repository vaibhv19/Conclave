# REST API Contracts & Validation: Room Management

This document details the API contracts, payload schemas, validation rules, and security controls for the **Room Management** module.

---

## 🔒 1. Access Control & Security Check
All room management endpoints are protected. The caller must supply a valid JWT token via the `Authorization: Bearer <token>` header.
- **Ownership Rule:** For `GET` and `PUT` operations, the backend validates that the user ID derived from the authenticated JWT token matches the `owner_id` stored in the `rooms` table. Any unauthorized access results in an **HTTP 403 Forbidden** response.

---

## 🛰️ 2. REST Endpoints

### 2.1 Create Room
Exposes transactional creation of a collaboration session blueprint.

- **URL:** `POST /api/rooms`
- **Headers:** `Content-Type: application_json`, `Authorization: Bearer <token>`
- **Request Body (Example):**
  ```json
  {
    "name": "Consensus Drafting Session",
    "objective": "Draft a unified engineering design document for the messaging component",
    "roleAssignments": [
      {
        "roleName": "Lead Writer",
        "modelId": "GEMINI_PRO",
        "uiColorHex": "#00FF00"
      },
      {
        "roleName": "Fact Checker",
        "modelId": "FAKE_CLAUDE",
        "uiColorHex": "#0000FF"
      }
    ]
  }
  ```
- **Validation Constraints:**
  - `name`: Must not be blank.
  - `objective`: Must not be blank.
  - `roleAssignments`: Must not be empty (minimum 1 role required).
  - `uiColorHex`: Must match the 6-digit hex format starting with `#` (`^#[0-9A-Fa-f]{6}$`).
  - `modelId`: Must be one of the supported models: `GEMINI_PRO`, `FAKE_OPENAI`, `FAKE_CLAUDE`.
  - `roleName`: Must be unique within the room payload.
- **Success Response (HTTP 201 Created):**
  ```json
  {
    "roomId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "name": "Consensus Drafting Session",
    "objective": "Draft a unified engineering design document for the messaging component",
    "status": "INITIALIZED",
    "roleAssignments": [
      {
        "roleName": "Lead Writer",
        "modelId": "GEMINI_PRO",
        "uiColorHex": "#00FF00"
      },
      {
        "roleName": "Fact Checker",
        "modelId": "FAKE_CLAUDE",
        "uiColorHex": "#0000FF"
      }
    ],
    "workflowState": {
      "id": "e42bc19a-58cc-4372-a567-0e02b2c3d480",
      "currentDraft": "",
      "reviewComments": "",
      "lastUpdatedAt": "2026-07-27T16:20:00"
    }
  }
  ```

---

### 2.2 Fetch Room Detail
Fetches the current configuration, role mappings, and workflow states for a room.

- **URL:** `GET /api/rooms/{id}`
- **Headers:** `Authorization: Bearer <token>`
- **Response (HTTP 200 OK):**
  - Returns the `RoomResponse` payload matching the creation output format.
- **Errors:**
  - **403 Forbidden:** If the caller is not the room owner.
  - **404 Not Found:** If the room ID does not exist in the database.

---

### 2.3 Update Mappings (Role Assignments)
Overwrites all model-to-role mappings for an existing room.

- **URL:** `PUT /api/rooms/{id}/role-assignments`
- **Headers:** `Content-Type: application_json`, `Authorization: Bearer <token>`
- **Request Body (Example):**
  ```json
  [
    {
      "roleName": "Lead Writer",
      "modelId": "GEMINI_PRO",
      "uiColorHex": "#FF5733"
    },
    {
      "roleName": "Architect Reviewer",
      "modelId": "FAKE_OPENAI",
      "uiColorHex": "#E0115F"
    }
  ]
  ```
- **Validation Constraints:**
  - Validates the new list of assignments using the same rules as room creation (hex colors, supported model list, uniqueness of role names).
- **Success Response (HTTP 200 OK):**
  - Returns the updated `RoomResponse` containing the new `roleAssignments` list.
- **Errors:**
  - **400 Bad Request:** If any validation constraint fails.
  - **403 Forbidden:** If the caller is not the room owner.
  - **404 Not Found:** If the room ID does not exist.
