# Conclave Frontend Client Specification

The Conclave client is a single-page React 19 application built using Vite, Tailwind CSS, Zustand global stores, and `@stomp/stompjs` for real-time WebSocket communication.

---

## 🎯 Frontend Architecture & View Routing
Conclave uses a high-performance **State-Driven View Routing** model inside `App.jsx` instead of a traditional URL-based router. This structure minimizes routing overhead and guarantees that user flows are strictly aligned with credentials and session states.

```
                  +----------------------------------+
                  |           Start Mount            |
                  +-----------------┬----------------+
                                    │ Calls init()
                  +-----------------▼----------------+
                  |      Does Token Exist in Store?  |
                  +-------┬------------------┬-------+
                          │ No               │ Yes
        +-----------------▼----+      +------▼---------------------+
        | Login/Register View  |      |   Is Active Room Selected? |
        +----------------------+      +-------┬--------------┬-----+
                                              │ No           │ Yes
                                       +------▼-----+   +----▼------+
                                       | Setup View |   | Room View |
                                       +------------+   +-----------+
```

1.  **Authentication Guard:** If `authStore.token` is null, the application restricts navigation to either `LoginView` or `RegisterView`.
2.  **Room Configuration Guard:** Once logged in, if `roomStore.activeRoom` is null, the app routes the user to `SetupView` to configure model roles or select an existing meeting room.
3.  **Workspace Interface:** If a valid token and active room are present in state, the client mounts the main `RoomView` workspace.

---

## 📂 Folder Structure

```
frontend/
├── package.json                            # Package dependencies and workspace script registry
├── vite.config.js                          # Vite compiler and development server options
├── tailwind.config.js                      # Tactical HSL theme extensions and utility rules
├── index.html                              # Root HTML entry point
├── e2e/                                    # Playwright automated browser integration tests
│   ├── auth.spec.js                        # Registration and Login E2E validation flows
│   └── chat.spec.js                        # Room setup, mentions, streaming, and pause/resume E2E
└── src/                                    # React source codebase
    ├── main.jsx                            # Standard DOM mounter
    ├── App.jsx                             # View routing controller
    ├── App.css / index.css                 # Custom font definitions, Webkit resets, autofill overrides
    ├── assets/                             # Logo SVGs and UI screenshots
    ├── components/                         # Granular visual panels
    │   ├── AlertBanner.jsx                 # WebSocket status and pause control decks
    │   ├── ChatBar.jsx                     # Mention-enabled textarea inputs
    │   ├── MessageBubble.jsx               # Markdown rendering response cards
    │   ├── Sidebar.jsx                     # Room selection list and compressed state widgets
    │   └── TurnIndicator.jsx               # Active typing indicator
    ├── services/                           # HTTP & WebSockets transport clients
    │   ├── api.js                          # REST backend wrapper
    │   └── websocket.js                    # STOMP gateway subscriptions and connection callbacks
    ├── store/                              # Zustand global states
    │   ├── authStore.js                    # Authorization tokens and localStorage cache
    │   ├── roomStore.js                    # Active rooms selection and role configurations
    │   └── chatStore.js                    # Canonical message logs and streaming state
    └── tests/                              # Component and Store unit tests (Vitest)
```

---

## 📦 State Management & Decoupling

Conclave leverages **Zustand** to decouple incoming high-frequency WebSocket streams from React's component re-render loops. 

### Slices
*   **`authStore.js`:** Manages JWT tokens, handles registration/login API requests, and synchronizes tokens with `localStorage`.
*   **`roomStore.js`:** Coordinates available rooms, loads room configuration options, and stores the user's active room context.
*   **`chatStore.js`:** Maintains the central conversation array, appends streaming text deltas to the correct message index, tracks active typing states, and stores the current `WorkflowState` (compacted summary and code draft).

### Performance Optimization
Instead of registering WebSocket callbacks that invoke local React state setters (which would trigger parent re-renders for every single character chunk), the socket client (`websocket.js`) dispatches chunks directly to `chatStore.js`. React components subscribe to specific Zustand selectors (e.g., `useChatStore(state => state.messages)`). Only the components displaying text are re-rendered, maintaining 60fps performance during high-speed local streaming.

---

## 🌐 WebSocket & STOMP Streaming

Conclave interfaces with the backend message broker using `@stomp/stompjs` over a native WebSocket connection.
*   **Authentication Handshake:** The token from `authStore` is injected into the `Authorization` header during the STOMP `CONNECT` frame.
*   **Subscription Multiplexing:** Upon room selection, the socket client subscribes to `/topic/rooms/{roomId}`.
*   **Chunk Rehydration:** When a `CONTENT_CHUNK` event is received, `chatStore.appendStreamChunk()` aggregates the text in the message log. When `TURN_COMPLETED` is received, the full message is finalized, and usage metrics are updated.

---

## 🎨 UI Design Tokens & Styling Philosophy

The client features a dark, tactical, low-contrast terminal aesthetic tailored for developers. Colors are defined in `tailwind.config.js` and applied using HSL custom properties.

### Surface Elevation Scales
*   **Level 0 (Canvas Base):** Deep Slate Charcoal `#08080A` — Main layout background.
*   **Level 1 (Side Panels):** Dark Slate Surface `#121214` — Sidebar, headers, and modal surfaces.
*   **Level 2 (Elevated Inputs):** Raised Slate `#18181C` — Message bubbles, buttons, and text fields.
*   **Level 3 (Interactive Active):** Active Surface `#222227` — Hover states and selected room rows.

### Font System
*   **Standard Interface:** `Inter` — High legibility sans-serif for controls, headers, and sidebar items.
*   **Monospace Telemetry:** `JetBrains Mono` — Applied to prompt tokens count, execution times, and code blocks.

### Autofill Resets
Browser credential managers (like Chrome or Edge autofill) inject yellow/white background styles into standard inputs. `index.css` includes Webkit overrides to ensure that elevated input styles remain dark and consistent with the console design.

---

## 🛠️ CLI Operations & Script Registry

Navigate to the `frontend/` directory to run these commands:

### Install Node Modules
```bash
npm install
```

### Run Local Development Server
```bash
npm run dev
```
Hosts HMR (Hot Module Replacement) pages locally at `http://localhost:5173`.

### Compile Production Bundle
```bash
npm run build
```
Optimizes and compiles assets into the `dist/` directory.

### Run Unit Tests (Vitest)
```bash
npm run test
```
Runs store mutations and UI layout unit tests.

### Run Playwright E2E Tests
```bash
npm run test:e2e
```
Spins up a headless Chromium instance to validate registration, room creation, sequential model streaming, and pipeline locking in real-time.

---

## 🧩 Extension Guide (Adding UI Elements)

### Adding a New Theme Color for a Model Adapter
1. Open [frontend/src/components/MessageBubble.jsx](file:///d:/Coding/Projects----For%20Resume/Conclave/frontend/src/components/MessageBubble.jsx).
2. Locate the model configuration lookup object (`MODEL_METADATA`).
3. Add a new model mapping entry containing the hex color codes, text labels, and avatar configurations:
   ```javascript
   phi3: {
       borderColor: 'border-purple-900/50',
       bgColor: 'bg-purple-950/20',
       badgeColor: 'bg-purple-500/20 text-purple-300',
       label: 'Phi-3 (Local)'
   }
   ```
4. Verify rendering using component tests (`npm run test`).
