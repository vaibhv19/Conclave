# Learning 08: Zustand State Synchronization with WebSockets

## 1. Problem Statement
Handling real-time token streaming chunks and async pipeline events (like turn start, completed, system intervention) requires responsive client-side state updates. Using standard React `useState` hooks inside components results in complex prop drilling, re-render cascades, and state synchronization failures when sockets close or reconnect.

## 2. Decision Rationale
We chose **Zustand** as the global state store unified with `@stomp/stompjs`:
- Zustand provides a lightweight, external state store that does not suffer from React context performance bottlenecks.
- Enables direct mutations from WebSocket callback listeners (`websocket.js`) without requiring React rendering context.
- Maintains clean separation of concerns: UI views only subscribe to state variables, while business logic resides inside stores.

## 3. Alternatives Considered
- **Redux Toolkit:** Rejected due to excessive boilerplate (actions, reducers, payload creators) which is overkill for a lightweight collaborative chat.
- **Context API:** Rejected because Context API triggers re-renders on all subscribing elements, resulting in significant latency lag during word-by-word real-time chunk streaming.

## 4. Internal Working
1.  **Event Subscription:** When a room page mounts, `connectWebSocket(roomId)` initiates connection.
2.  **Callback Interception:** The socket listener receives a STOMP frame and parses the event type payload.
3.  **Zustand Dispatch:** Callbacks directly trigger store actions (e.g. `chatStore.handleContentChunk(body)`), updating store state instantly.
4.  **UI Render:** Subscribing UI elements re-render only the updated text component, preventing page-wide layouts redraw.

## 5. Conclave Implementation
- Global stores are split into [authStore.js](file:///d:/Coding/Projects----For%20Resume/Conclave/frontend/src/store/authStore.js), [roomStore.js](file:///d:/Coding/Projects----For%20Resume/Conclave/frontend/src/store/roomStore.js), and [chatStore.js](file:///d:/Coding/Projects----For%20Resume/Conclave/frontend/src/store/chatStore.js).
- Event mappings to store dispatchers are configured in [websocket.js](file:///d:/Coding/Projects----For%20Resume/Conclave/frontend/src/services/websocket.js).
- Visual UI bindings are rendered in [RoomView.jsx](file:///d:/Coding/Projects----For%20Resume/Conclave/frontend/src/views/RoomView.jsx).

## 6. Key Classes
- [chatStore.js](file:///d:/Coding/Projects----For%20Resume/Conclave/frontend/src/store/chatStore.js) - Manages message arrays and active generating states.
- [websocket.js](file:///d:/Coding/Projects----For%20Resume/Conclave/frontend/src/services/websocket.js) - Handles connection activations and topic subscriptions.
- [RoomView.jsx](file:///d:/Coding/Projects----For%20Resume/Conclave/frontend/src/views/RoomView.jsx) - Main view subscribing to store selectors.

## 7. Common Pitfalls
- **Circular Store Dependencies:** Accessing state stores inside helper utilities before initialization can trigger undefined references. Utilize `useChatStore.getState()` instead of direct hook calls in non-react files.
- **Memory Leak Subscriptions:** Subscribing to WebSockets on mount without calling `disconnect` on unmount results in duplicate socket listeners. Always call cleanup in React `useEffect` unmount blocks.

## 8. Debugging Tips
- Trace store updates by console logging state shifts inside Zustand selectors.
- Check incoming socket traffic inside browser Network tool tabs to confirm chunk events are dispatching.

## 9. Interview Questions
1.  *Why did you select Zustand over React Context API for Conclave's real-time streaming state synchronization?*
2.  *How do you decouple websocket event callback listeners from React's component lifecycles in this codebase?*
3.  *How do you prevent duplicate socket subscriptions when a user switches rapidly between rooms?*

## 10. References
- [Zustand Documentation](https://zustand-demo.pmnd.rs/)
- [Playwright WebSockets Mocking Guide](https://playwright.dev/docs/network#mock-websockets)
