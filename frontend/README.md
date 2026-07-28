# Conclave Frontend Client Specification

The Conclave client is a single-page React 19 application built using Vite, Tailwind CSS, Zustand global stores, and `@stomp/stompjs` for real-time WebSocket communication.

---

## 1. Architectural Highlights

*   **Zustand Store Decoupling:** Global state is split into isolated slices (`authStore`, `roomStore`, `chatStore`). WebSocket event streams update the store directly, bypassing the React component lifecycle to prevent parent render thrashing.
*   **STOMP subscription multiplexing:** A single WebSocket connection handles chat text deltas (`CONTENT_CHUNK`), pipeline interruptions (`SYSTEM_INTERVENTION`), and typing animations (`TURN_STARTED`) on a per-room topic channel.
*   **Autofill CSS Safeties:** Overrides standard browser pseudo-elements in `index.css` to prevent yellow/white backgrounds from breaking the dark console styling.
*   **Accessibly-Dense Grids:** Standard forms collapse illustration panes on mobile breakpoints using Flex layouts, keeping views dense and readable.

---

## 2. CLI Workflows & Script Registry

Run these npm commands from the `frontend/` directory:

### Install Node Modules
```bash
npm install
```

### Launch Vite Local Server
```bash
npm run dev
```
Hosts HMR (Hot Module Replacement) pages locally at `http://localhost:5173`.

### Compile Production Bundle
```bash
npm run build
```
Compiles, optimizes, and bundles frontend code inside the `dist/` directory.

### Run Unit Tests (Vitest)
```bash
npm run test
```
Runs component and store unit tests (e.g. testing markdown rendering, collapsing panels, and auth mutations).

### Run Playwright E2E Tests
```bash
npm run test:e2e
```
Spins up a headless browser to test complete registration, login, room configuration, and message workflows. Tests mock API routes using Playwright's `page.route` to run fast and database-independently.

---

## 3. Global Zustand Stores

*   **`authStore.js`:** Manages registration, login, JWT token caching inside `localStorage`, and handles sign-out cleanup.
*   **`roomStore.js`:** Loads room configuration catalogs, maps role assignments, and tracks active selected workspace IDs.
*   **`chatStore.js`:** Maintains message logs, appends incoming text deltas from sockets, updates WorkflowState summaries, and aggregates token usage logs.

---

## 4. Visual Design Tokens

The workspace follows a low-contrast console style with the following surface levels:
*   **Level 0 (Canvas Base):** Deep Slate Charcoal `#08080A` (main view background).
*   **Level 1 (Side Panels):** Dark Slate Surface `#121214` (headers, sidebars, forms).
*   **Level 2 (Elevated Inputs):** Raised Slate `#18181C` (textareas, buttons, code blocks).
*   **Level 3 (Interactive Active):** Active Surface `#222227` (hover states, active selection rows).
*   **Borders:** Subtle `#1F1F24`, focus `#2E2E36`.
*   **Fonts:** `Inter` for standard UI, `JetBrains Mono` for telemetry and token counts.

---

## 5. Engineering References

Study the following handbook chapters to understand the client internals:
*   **[Zustand Stores & Sockets Sync](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Learning/08_Zustand_State_Synchronization_With_WebSockets.md):** Dynamic store mutators and selective React hooks.
*   **[Tailwind CSS & Autofill Resets](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Learning/09_Tailwind_Customization_For_Tactical_UIs.md):** UI color scales, elevations, and webkit autofill styles.
*   **[Layout Component trees](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/UI_Design.md):** Detailed component relationship maps.
