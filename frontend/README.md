# Conclave Frontend Client

The Conclave client is a single-page React 19 application built using Vite, Tailwind CSS, Zustand global stores, and `@stomp/stompjs` for WebSocket streaming.

---

## 🛠️ CLI Operations & NPM Scripts

Run these scripts from the `frontend/` directory:

### Install node modules:
```bash
npm install
```

### Launch Vite development server:
```bash
npm run dev
```
Hosts HMR page contexts locally at `http://localhost:5173`.

### Build production bundle:
```bash
npm run build
```
Compiles and bundles the application inside `dist/`.

### Run component unit tests:
```bash
npm run test
```
Executes the Vitest test runner, testing markdown parser outputs, user/AI alignments, and collapsed Sidebar states.

### Run Playwright E2E integration tests:
```bash
npm run test:e2e
```
Launches the local Vite server and runs simulated user registration, login, room setup, and workspace chat interaction specs.

---

## 🗃️ Global State Stores (Zustand)

Global client states are managed via lightweight stores inside `src/store/`:
- `authStore.js`: Coordinates user registration, login status, and caches JWT credentials inside `localStorage` for session maintenance.
- `roomStore.js`: Manages list of rooms, active selected room, and initializes new room configurations.
- `chatStore.js`: Holds conversation message logs, streams incoming delta chunks, tracks audited token usage, and consolidates the active WorkflowState (draft & review comments).

---

## 🎭 Playwright E2E Configuration

- **Test Directory:** Tests are written inside `frontend/e2e/conclave.spec.js`.
- **API Mocking:** Utilizes Playwright `page.route` to mock registration, login, and room spec queries. This ensures that the test runner executes fast and has no dependencies on active database records.
- **Vite Autolaunch:** The configuration (`playwright.config.js`) is configured with a `webServer` block that automatically spins up the Vite server on port `5173` if it's not already running.
