# Phase 09 — Frontend Foundation

## 1. Module Planning: Frontend Foundation

### 1.1 Purpose
The purpose of this phase is to construct the frontend application foundation. This includes establishing the REST client wrapper, configuring global state stores with Zustand, creating authentication and room configuration views, and implementing the `@stomp/stompjs` client mapping connection handshakes to active JWT tokens.

### 1.2 Package / Folder Structure
```
frontend/src/
├── services/
│   ├── api.js                          # Fetch/Axios client with JWT interceptor
│   └── websocket.js                    # STOMP socket client connector
├── store/
│   ├── authStore.js                    # Global JWT token state
│   ├── roomStore.js                    # Room creation & config lists
│   └── chatStore.js                    # Message array and active WS sync state
└── views/
    ├── LoginView.jsx                   # Register / Login screen
    ├── SetupView.jsx                   # Room objective / assignment screen
    └── RoomView.jsx                    # Chat room layout skeleton
```

### 1.3 Responsibilities & Dependencies
- **HTTP client interceptors:** Configure standard REST clients (`api.js`) to read tokens from `authStore` and inject `Authorization: Bearer <token>` headers on all request paths except `/auth`.
- **Zustand stores:**
  - `authStore`: Synchronizes logins and user data. Persists session token in `localStorage`.
  - `roomStore`: Manages dashboard views, lists available user rooms, and manages room creation triggers.
  - `chatStore`: Holds the active room conversation history array and is the single source of truth for WebSocket payload state updates.
- **WebSocket STOMP Manager:** Initializes connections to `ws://localhost:8080/ws-conclave`. Transmits user JWT inside connection headers, manages automatic connection reconnect loops, and handles active room subscription channels.

---

## 2. Module Components

### 2.1 STOMP Subscription Handler Logic
Within `chatStore.js` and `websocket.js`, handlers must map websocket payloads to UI state modifications:
1. **`TURN_STARTED`**: Appends a temporary message block in the messages list associated with the target model, rendering a "thinking indicator".
2. **`CONTENT_CHUNK`**: Appends the text increment delta directly to the active message's content property.
3. **`TURN_COMPLETED`**: Finalizes the message content, updates the token usage metrics panel, and re-populates the local `WorkflowState` object from the event payload.
4. **`SYSTEM_INTERVENTION`**: Forces active pipeline indicator to display `PAUSED` status.

---

## 3. Atomic Implementation Tasks

### Task 9.1: Build API Client and Auth Zustand Store
- **Estimated Size:** S
- **Risk:** Low
- **Prerequisites:** Phase 02 & Phase 03 Backend API
- **Definition of Done:**
  - Create `authStore.js` managing login states, error tracking, and localStorage caching.
  - Create `api.js` using Axios/Fetch.
  - Implement request interceptors appending Bearer tokens to all requests.
  - Create `LoginView.jsx` and `RegisterView.jsx` displaying input fields.
  - Validate logins register/submit data and transition views.

### Task 9.2: Create Room Store and Room Creation Views
- **Estimated Size:** M
- **Risk:** Low
- **Prerequisites:** Task 9.1
- **Definition of Done:**
  - Create `roomStore.js` managing list of active rooms.
  - Create `SetupView.jsx` using Tailwind CSS styling:
    - Display Room Name and Room Objective inputs.
    - Role Configuration deck: cards containing dropdown list to select Model (Llama 3, Mistral, Gemma) and Hex colors.
  - Submitting form invokes `POST /api/rooms` and navigates to the Room Chat view route upon receipt of a successful response.

### Task 9.3: Implement WebSocket STOMP Client and State Sync
- **Estimated Size:** L
- **Risk:** High
- **Prerequisites:** Task 9.2 & Phase 07 Backend WebSocket
- **Definition of Done:**
  - Create `websocket.js` configuring `@stomp/stompjs` client instances.
  - Connect to socket passing JWT in headers.
  - Subscribes to `/topic/room/{roomId}`.
  - Implement message callbacks resolving received events to Zustand `chatStore` actions:
    - `TURN_STARTED` appends message placeholder and sets loading flags.
    - `CONTENT_CHUNK` updates text content.
    - `TURN_COMPLETED` sets message completed, saves token usage metrics, updates local `WorkflowState`.
    - `SYSTEM_INTERVENTION` sets room state status to `PAUSED`.
  - Console logs confirm clean socket handshake, token transfer, and state syncing.

---

## 4. Documentation & Verification

### Documentation to Update / Create
- Create `Docs/Learning/08_Zustand_State_Synchronization_With_WebSockets.md` detailing:
  - Global state storage designs and WebSocket integration patterns.
  - Strategies for handling dropped frames or reconnect synchronization.

### Testing Checkpoint
- Mock socket broadcasts to verify state modifications in the Zustand store.
- Verify that logging out clears localStorage credentials and disconnects the active WebSocket client connection.

### Suggested Git Commit Boundaries
1. `frontend: create api client and auth state management store`
2. `frontend: develop login, register, and room setup wizard screens`
3. `frontend: implement stomp client and chat store socket event sync handlers`

### Suggested GitHub Issues
- **Issue 9.1:** Build REST clients and Auth Zustand store. (Points: 2)
- **Issue 9.2:** Develop room setup wizard view. (Points: 2)
- **Issue 9.3:** Build STOMP WebSocket connection manager. (Points: 3)
