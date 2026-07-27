# API Specification: Conclave

This document defines the public REST API and WebSocket contract for the **Conclave** orchestration platform. It specifies how the React frontend interacts with the Spring Boot backend to manage rooms, trigger multi-provider turns, and handle real-time state synchronization.

---

## 1. Global Conventions
- **Base URL:** `http://localhost:8080/api`
- **Auth Scheme:** HTTP Bearer Token (JWT)
- **Content Type:** `application/json`
- **Error Format:**
  ```json
  {
    "status": 400,
    "error": "Bad Request",
    "message": "Model 'Claude' is currently in Mock mode.",
    "timestamp": "2024-07-26T10:11:00Z"
  }
  ```

---

## 2. Authentication (`/auth`)
| Path | Method | Description | Request Body | Response Body |
| :--- | :--- | :--- | :--- | :--- |
| `/login` | `POST` | Authenticates user | `{ "email", "password" }` | `{ "token", "user" }` |
| `/register` | `POST` | Creates new account | `{ "email", "password", "name" }` | `{ "token", "user" }` |

---

## 3. Room Management (`/rooms`)
| Path | Method | Description | Request | Response |
| :--- | :--- | :--- | :--- | :--- |
| `/` | `POST` | Creates a new meeting room | `RoomCreateRequest` | `RoomResponse` |
| `/{id}` | `GET` | Fetches current room state | Path Variable `id` | `RoomResponse` |
| `/{id}/role-assignments` | `PUT` | Updates role-to-model mapping | `List<RoleAssignmentDTO>` | `RoomResponse` |

**`RoomCreateRequest`**:
```json
{
  "name": "Project Apollo",
  "objective": "Drafting technical architecture",
  "roleAssignments": [
    {
      "roleName": "Lead-Writer",
      "modelId": "GEMINI_PRO",
      "uiColorHex": "#E11D48"
    },
    {
      "roleName": "Code-Critic",
      "modelId": "FAKE_CLAUDE",
      "uiColorHex": "#10B981"
    }
  ]
}
```

**`RoleAssignmentDTO`**:
```json
{
  "roleName": "String",
  "modelId": "String",
  "uiColorHex": "String"
}
```

**`RoomResponse`**:
```json
{
  "roomId": "UUID",
  "name": "Project Apollo",
  "objective": "Drafting technical architecture",
  "status": "INITIALIZED | ACTIVE | PAUSED | ARCHIVED",
  "roleAssignments": [
    {
      "roleName": "Lead-Writer",
      "modelId": "GEMINI_PRO",
      "uiColorHex": "#E11D48"
    },
    {
      "roleName": "Code-Critic",
      "modelId": "FAKE_CLAUDE",
      "uiColorHex": "#10B981"
    }
  ],
  "workflowState": {
    "currentDraft": "String",
    "reviewComments": "String",
    "lastUpdatedAt": "ISO-8601"
  }
}
```

---

## 4. Chat & Orchestration (`/chat`)
| Path | Method | Description | Request | Response |
| :--- | :--- | :--- | :--- | :--- |
| `/message` | `POST` | User input (includes @-mentions) | `ChatMessageRequest` | `void (Async via WS)` |
| `/{roomId}/history` | `GET` | Fetches canonical history | Query: `limit` | `List<MessageResponse>` |
| `/pipeline/pause` | `POST` | Halts active model sequence | `{ "roomId" }` | `{ "status": "PAUSED" }` |
| `/pipeline/resume`| `POST` | Restarts sequence from last state| `{ "roomId" }` | `{ "status": "ACTIVE" }` |

#### Key DTO Shapes:
**`ChatMessageRequest`**:
```json
{
  "roomId": "UUID",
  "content": "@Lead-Writer please draft the database schema.",
  "isIntervention": false
}
```

**`MessageResponse`** (The Canonical Schema):
```json
{
  "messageId": "UUID",
  "senderType": "USER | AI | SYSTEM",
  "roleName": "Lead-Writer",
  "modelId": "GEMINI_PRO",
  "content": "...",
  "timestamp": "ISO-8601",
  "isMocked": false 
}
```

---

## 5. WebSocket Contract (STOMP)

Conclave uses WebSockets for real-time broadcast of model turns. This is critical for observing the orchestration as it happens.

*   **Connection Endpoint:** `ws://localhost:8080/ws-conclave`
*   **Topic Subscription:** `/topic/room/{roomId}`

### Broadcast Events
The backend pushes a payload to the topic whenever the room state changes.

**1. Turn Started Event** (Fires when a model begins "thinking"):
```json
{
  "type": "TURN_STARTED",
  "roleName": "Code-Critic",
  "modelId": "FAKE_CLAUDE",
  "isMocked": true
}
```

**2. Message Delta Event** (Fires for streaming content):
*Note: Real Gemini calls stream chunks; Fake OpenAI/Claude calls simulate streaming with 50ms delays.*
```json
{
  "type": "CONTENT_CHUNK",
  "delta": "The database ",
  "messageId": "UUID"
}
```

**3. Turn Completed Event** (Fires when `WorkflowState` is updated):
```json
{
  "type": "TURN_COMPLETED",
  "messageId": "UUID",
  "summary": "Updated WorkflowState: DB schema drafted.",
  "usage": {
    "promptTokens": 140,
    "completionTokens": 320
  }
}
```

**4. System Intervention Event** (Fires when a user intervenes in the pipeline):
```json
{
  "type": "SYSTEM_INTERVENTION",
  "messageId": "UUID",
  "content": "User intervention text..."
}
```

---

## 6. Implementation Notes: Real vs. Fake

| Feature | Live Provider: **Gemini** | Fake Provider: **OpenAI / Claude** |
| :--- | :--- | :--- |
| **Inference Type** | **Real API Call** (Google Vertex/AI) | **Fake ChatClient** (Internal Stub) |
| **Latency** | 2s - 8s (Variable) | 1s - 3s (Simulated) |
| **Streaming** | Real server-sent events | Local iterative broadcast |
| **Token Tracking** | Metadata from API response | Character-based heuristic |
| **Auth** | API Key required in `.env` | No external auth required |

### Intervention Logic
When `POST /chat/message` is called with `isIntervention: true`, the system:
1.  Appends the message to the `conversation_history`.
2.  Forces a re-summarization of the `WorkflowState`.
3.  Broadcasts a `SYSTEM_INTERVENTION` event via WebSocket to signal to all clients that the pipeline context has been manually altered.