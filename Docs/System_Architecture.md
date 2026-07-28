# System Architecture Specification: Conclave

This document defines the high-level system architecture, layout component hierarchy, and client-server stream communication flows for **Conclave**.

---

## 1. System Overview Diagram

```
                               +-------------------------------------+
                               |         Vite React Frontend         |
                               | (Level 0 Canvas / Level 1 Sidebars)  |
                               +-----------┬─────────────────┬-------+
                                           |                 |
                             REST Requests |                 | WebSocket STOMP
                             (Auth/Rooms)  |                 | (Chunk Streams)
                                           ▼                 ▼
                               +-------------------------------------+
                               |        Spring Boot Backend          |
                               |          (Port 8080)                |
                               +-------------------┬-----------------+
                                                   |
                                                   | Resolves Adapter
                                                   ▼
                               +-------------------------------------+
                               |         Model Registry              |
                               +---┬───────────────┬───────────────┬-+
                                   |               |               |
                                Gemini          OpenAI          Claude
                               Adapter          Fake            Fake
                                   ▼               ▼               ▼
                              [Vertex AI]    [Simulated]     [Simulated]
```

---

## 2. Layout Component Hierarchy

The frontend is structured as a single-page console layout that fills 100% of the viewport height and width:

```
App.jsx
 └── (Switch based on session state)
      ├── LoginView.jsx / RegisterView.jsx (Minimal dark panel centered cards)
      ├── SetupView.jsx (Low-profile Initializer Wizard)
      └── RoomView.jsx (Main Strategy Workspace Layout)
           ├── AlertBanner.jsx (Warning stripes warning decks when PAUSED)
           ├── Header Console (Console title, PID, and STOMP connection orb)
           └── Splitter Panel Layout (100% Height Flex Split)
                ├── Sidebar.jsx (Left side: Objective, Consensus Draft, Audited Metrics)
                └── Main Chat Area (Center: Message feed and Input panel)
                     ├── MessageBubble.jsx (Individual message alignments & telemetry)
                     ├── TurnIndicator.jsx (Typing indicator pulsing orb)
                     └── ChatBar.jsx (Command entry textarea & role shortcuts)
```

---

## 3. Core Architectural Modules

### 3.1 Frontend State Management (Zustand)
Global state slices are decoupled from the component rendering trees:
- **`authStore.js`**: Handles session authentication tokens and coordinates credentials validation.
- **`roomStore.js`**: Manages the room configuration parameters and coordinates new workspace setup triggers.
- **`chatStore.js`**: Holds active messages history, streams in raw chunk updates, tracks token usages, and synchronizes consensus drafts.

### 3.2 WebSocket STOMP Pub/Sub Stream
Bidirectional communication routes real-time token outputs:
- **Client Subscription**: Subscribes to `/topic/room/{roomId}` to capture streaming turns.
- **Server Broadcasting**: Pushes `TURN_STARTED`, `CONTENT_CHUNK`, `TURN_COMPLETED`, and `SYSTEM_INTERVENTION` frames.
- **State Integration**: Sockets callbacks execute direct Zustand mutator calls (`useChatStore.getState()`), allowing word-by-word streaming updates without full-screen page cascades.

### 3.3 Backend Adapter & Registry Layer
A unified translation layer resolves model interfaces at runtime:
- **`ModelRegistry`**: Maps role engine selections to active Spring AI or Fake Chat clients.
- **`ProviderAdapter`**: Translates the canonical message log database schema into provider-specific request structures, resolving the distinct message schemas of Google, OpenAI, and Anthropic.
