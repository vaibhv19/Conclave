# WebSocket STOMP Architecture: Conclave

This document defines the real-time event-driven messaging layer for **Conclave**, specifying protocols, payload structures, connection lifecycles, and performance optimizations.

---

## 1. Problem & Design Decision

### 1.1 The Challenge
Orchestrating collaborative AI workspaces requires real-time feedback. Users need to see model turns as they are generated (word-by-word streaming), observe status changes (e.g. pipeline paused), and track token usage metrics dynamically. 
*   **HTTP Polling Limitations:** Short polling introduces massive latency and database load, while long polling keeps HTTP threads occupied.
*   **Server-Sent Events (SSE) Limitations:** SSE is unidirectional (server to client) and does not support client command routing or subscription management natively.

### 1.2 The Decision
Conclave implements a bidirectional **WebSocket connection** configured with the **STOMP (Simple Text Oriented Messaging Protocol)** protocol. STOMP overlays raw WebSockets with a structured messaging schema (using frames like `CONNECT`, `SUBSCRIBE`, `SEND`, and `DISCONNECT` along with headers), simplifying message routing and channel subscription management.

```
┌────────────────────────────────┐                 ┌──────────────────────────────┐
│   React Client (stompjs)       │                 │   Spring Boot WebSocket      │
└───────────────┬────────────────┘                 └──────────────┬───────────────┘
                │                                                 │
                │ 1. WebSocket Handshake Upgrade                  │
                ├────────────────────────────────────────────────>│
                │ 2. STOMP CONNECT (passcode: JWT)                │
                ├────────────────────────────────────────────────>│
                │                                                 │ [JwtChannelInterceptor]
                │                                                 │ * Verifies signature
                │                                                 │ * Authorizes principal
                │ 3. STOMP SUBSCRIBE (/topic/room/{roomId})       │
                ├────────────────────────────────────────────────>│
                │                                                 │ [Subscription guard]
                │                                                 │ * Checks Room ownership
                │                                                 │
                │ <─────── 4. Event Streams Broadcast ────────────┤
                │          (TURN_STARTED, CONTENT_CHUNK...)       │
```

---

## 2. Protocol Configuration & Routing

The communication tier is configured in `WebSocketConfig` using standard Spring message mappings:

*   **Connection Upgrade Path:** `/ws-conclave`
*   **Simple Message Broker prefix:** `/topic` (clients subscribe here)
*   **Destination Prefix:** `/app` (for client-initiated messages)

### 2.1 STOMP Event Types

#### `TURN_STARTED`
Signals to all clients that a specific model has begun processing a prompt.
*   **Payload Example:**
    ```json
    {
      "type": "TURN_STARTED",
      "roleName": "Critic",
      "modelId": "FAKE_CLAUDE",
      "isMocked": true
    }
    ```
*   **UI Impact:** React sets the active typing status for the Critic, triggering a pulsing typing indicator bubble in the chat view.

#### `CONTENT_CHUNK`
Pushes incoming text fragments to the client in real-time.
*   **Payload Example:**
    ```json
    {
      "type": "CONTENT_CHUNK",
      "delta": " The database schema ",
      "messageId": "ac82b3d2-3112-4c2c-882e-131154be1212"
    }
    ```
*   **UI Impact:** React appends the text delta to the matching message ID in the Zustand store, creating a fluid word-by-word streaming effect.

#### `TURN_COMPLETED`
Fires when the model completes its stream, returning the full content and token logs.
*   **Payload Example:**
    ```json
    {
      "type": "TURN_COMPLETED",
      "messageId": "ac82b3d2-3112-4c2c-882e-131154be1212",
      "content": "The completed drafted text block...",
      "usage": {
        "promptTokens": 140,
        "completionTokens": 320
      }
    }
    ```
*   **UI Impact:** React updates the target message's final content, refreshes the sidebar draft summary and telemetry metrics, and frees the turn block.

#### `SYSTEM_INTERVENTION`
Sent when a pipeline pause/resume is triggered or an error is caught.
*   **Payload Example:**
    ```json
    {
      "type": "SYSTEM_INTERVENTION",
      "messageId": "bd382c1e-3fdf-441c-b29e-445a16df3323",
      "content": "[SYSTEM HALT]: Pipeline paused by owner."
    }
    ```
*   **UI Impact:** React updates the room status banner to `PAUSED` and mounts the warning deck.

---

## 3. Performance & Scaling Considerations

### 3.1 Rendering Efficiency: Zustand Context Injection
*   **The Problem:** Standard React state updates during rapid streams (e.g. 30 words per second) trigger parent-component re-renders, causing input lag and UI freezing.
*   **The Solution:** The STOMP event callbacks update the local Zustand store slices (`chatStore.js`) directly outside the React render tree. Only the specific `MessageBubble` or `TelemetryMetric` subscribing to the store updates its DOM node, keeping layout rendering highly efficient.

### 3.2 Scaling the Message Broker: In-Memory vs. STOMP Relay
*   **Current Architecture:** Conclave uses Spring’s default in-memory **Simple Broker** to route messages. This is simple and self-contained, but does not scale horizontally across multiple backend instances.
*   **Production Extensibility:** For horizontal scale, the broker can be swapped for an external message broker (e.g., **RabbitMQ** or **ActiveMQ** with STOMP plugin enabled) by configuring `enableStompBrokerRelay` in `WebSocketConfig`:
    ```java
    config.enableStompBrokerRelay("/topic")
          .setRelayHost("rabbitmq-server")
          .setRelayPort(61613)
          .setClientLogin("guest")
          .setClientPasscode("guest");
    ```
    This allows messages to be broadcast across all scaled nodes in a cluster automatically.

---

## 4. Connection Failure Modes & Recovery Strategies

### 4.1 Client-Side Auto-Reconnection
*   **Failure:** User experiences a temporary network drop, causing the WebSocket connection to disconnect.
*   **Recovery:** The client (`@stomp/stompjs`) is configured with a `reconnectDelay` of **5000ms**. If disconnected, the library automatically schedules handshake retries in the background.

### 4.2 State Reconciliation Fallback (GET Polling Sync)
*   **Failure:** A client disconnects during active stream broadcasts, missing critical `TURN_COMPLETED` or `SYSTEM_INTERVENTION` frames, resulting in a drifted UI state.
*   **Recovery:** When the client re-establishes a WebSocket connection, or periodically every **15 seconds** (polling backup), React fetches the complete room state from the database via `GET /api/rooms/{id}`. This reconciles the message array and workflow draft, resolving any missed STOMP packets.
