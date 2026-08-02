# Phase 10 — Frontend UI Components & Design System

## 1. Module Planning: Frontend UI Components

### 1.1 Purpose
The purpose of this phase is to construct the frontend user interface components using Tailwind CSS. It implements **Option A: "The Command Deck"** (Industrial & Tactical) design theme, creating a multi-bubble chat room, @-mention picker, WorkflowState sidebar, and the warning decks for PAUSED intervention modes.

### 1.2 Package / Folder Structure
```
frontend/src/components/
├── MessageBubble.jsx                   # Render color-coded bubbles with metadata
├── ChatBar.jsx                         # Text inputs, @-mention popovers
├── Sidebar.jsx                         # Side deck displaying WorkflowState
├── TurnIndicator.jsx                   # Slow-pulsing typing indicator
└── AlertBanner.jsx                     # Warning deck overlays for PAUSED states
```

### 1.3 Responsibilities & Dependencies
- **Command Deck Visual Palette:**
  - Base: `#121214` (Deep Charcoal) / `#1C1C1F` (Elevated Zinc).
  - User Accent: `#3B82F6` (Electric Blue).
  - Intervention State: `#F59E0B` (Amber Warning).
  - AI Role Accents: Crimson, Emerald, Violet, Sky-Blue (based on mapping selections).
- **User Experience Enhancements:**
  - Collapsible WorkflowState Sidebar displays static objectives, latest draft drafts, and comments. Performs subtle CSS transitions when values update.
  - Input field triggers a select dropdown when user types `@`. Displays assigned room roles.
  - Displays a warning banner and yellow borders on the chat margins when status transitions to `PAUSED`.

---

## 2. Module Components

### 2.1 UI Component Specifications

#### `MessageBubble.jsx`
- Left-aligned for models, right-aligned for user.
- Left-aligned bubbles apply background tint and borders matching `uiColorHex` from configurations.
- Header displays: `[Role Icon] Lead-Writer (via LLAMA3)`.
- Metadata info-icon displays on hover: `latency`, `modelId`, `isMocked`.

#### `ChatBar.jsx`
- Dropdown popover list maps active roles when `@` is typed. Selection injects text token pill.
- When room status is `PAUSED`, "Send" button transforms to display "Inject & Resume" styled in Amber Warning (`#F59E0B`).

#### `AlertBanner.jsx`
- Displays at the top of the chat view when status is `PAUSED`.
- Displays: `[PAUSED] Pipeline halted at [Role]. Provide feedback to continue.` with warning yellow stripes.

---

## 3. Atomic Implementation Tasks

### Task 10.1: Build MessageBubble and TurnIndicator Components
- **Estimated Size:** M
- **Risk:** Low
- **Prerequisites:** Phase 09 Setup
- **Definition of Done:**
  - Create `MessageBubble.jsx` rendering role names, model names, message content (via standard React Markdown parser), and metadata hover cards.
  - Align bubbles: User (right, blue accent), Models (left, border color matched to mapping).
  - Create `TurnIndicator.jsx` showing slow pulsing animations.
  - Component tests verify correct alignment and color styles.

### Task 10.2: Implement ChatBar Input with @-Mention Popovers
- **Estimated Size:** L
- **Risk:** Medium
- **Prerequisites:** Task 10.1
- **Definition of Done:**
  - Create `ChatBar.jsx` text input handler.
  - Typing `@` triggers popover displaying room roles. Selecting one replaces text with a styling pill.
  - When status is `PAUSED`, click handles submitting isIntervention message to the backend and changes the button text to "Inject & Resume".
  - Component tests verify mention triggers display dropdown options.

### Task 10.3: Create Collapsible WorkflowState Sidebar
- **Estimated Size:** M
- **Risk:** Low
- **Prerequisites:** Task 10.1
- **Definition of Done:**
  - Create `Sidebar.jsx` as collapsible right panel.
  - Displays: Project Objective, Latest Draft, Review Comments.
  - Implement pulse transition triggers when properties update in the store.
  - Responsive design folds panel into hamburger overlay on mobile screens.

### Task 10.4: Integrate Pause Overlays and Warning Canvas
- **Estimated Size:** M
- **Risk:** Low
- **Prerequisites:** Task 10.2, Task 10.3
- **Definition of Done:**
  - Create `AlertBanner.jsx`.
  - When room status transitions to `PAUSED`, overlay displays header warnings, applies diagonal warning lines to page borders, and changes input container themes.
  - Integrates "Pause" and "Resume" REST client triggers.
  - End-to-end user path runs: click Pause -> overlay displays -> submit text -> click Inject -> overlay disappears.

---

## 4. Documentation & Verification

### Documentation to Update / Create
- Create `Docs/Learning/09_Tailwind_Customization_For_Tactical_UIs.md` detailing:
  - Custom Tailwind configurations for deep Charcoal themes.
  - CSS animation rules for indicator pulses and sidebar refreshes.

### Testing Checkpoint
- Perform visual regression and usability check. Confirm color contrast ratios on Dark Command Deck themes meet accessibility guidelines (WCAG AA).
- Verify mock model replies render markdown syntax correctly.

### Suggested Git Commit Boundaries
1. `frontend: develop MessageBubble rendering and metadata hover badges`
2. `frontend: build input ChatBar with role popovers`
3. `frontend: develop WorkflowState sidebar with update animations`
4. `frontend: integrate AlertBanner and PAUSED canvas overlays`

### Suggested GitHub Issues
- **Issue 10.1:** Build Message bubble component and indicators. (Points: 2)
- **Issue 10.2:** Build input chat panel with mention popovers. (Points: 3)
- **Issue 10.3:** Build context sidebar panel and animations. (Points: 2)
- **Issue 10.4:** Implement Pause/Resume banner layouts. (Points: 2)
