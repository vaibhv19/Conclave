# DESIGN.md — Conclave Visual Design System

This document defines the governing visual design system for **Conclave**. The frontend is engineered to support the user’s role as an **Orchestrator**, providing absolute clarity over multi-model turn-taking and the shared context state.

---

## 1. Design Philosophy: "The Orchestrator’s Podium"

Conclave is not a passive chat app; it is a collaborative workspace where the user is the moderator. The UI must solve the "Who said what?" and "Who is next?" problems through explicit visual hierarchy.

### Core Principles:
*   **Role Sovereignty:** Every model must be visually distinct based on its assigned **Role**, not just its Provider name. A "Lead Writer" (Gemini) should look different from a "Reviewer" (Fake Claude).
*   **Intent Clarity:** Before a message is sent, the UI must clearly highlight the *target* of the turn. The "@-mention" isn't just text; it’s a routing command.
*   **State Awareness:** The user must always see the "Shared Context" (WorkflowState) that the models are using to generate their responses.
*   **Active Suspension:** The "Pause & Intervene" state must feel like a deliberate, controlled halt (a "Safety"), not a system hang or error.

---

## 2. Visual Identity & Palette Options

Conclave requires a palette that can support at least 4-5 distinct "Role Colors" without becoming visually chaotic.

### Option A: "The Command Deck" (Industrial & Tactical)
*   **Concept:** Inspired by flight decks and terminal interfaces. High contrast, dark-mode focused, using utility-first accents.
*   **Palette:**
    *   **Base:** `#121214` (Deep Charcoal) / `#1C1C1F` (Elevated Zinc)
    *   **User Accent:** `#3B82F6` (Electric Blue)
    *   **Intervention State:** `#F59E0B` (Amber Warning)
    *   **Role Colors:** Crimson, Emerald, Violet, and Sky-Blue.
*   **Rationale:** Perfect for demonstrating technical orchestration; feels like a "power tool."

### Option B: "The Atelier" (Sophisticated & Creative)
*   **Concept:** Inspired by high-end print design and editorial workspaces. Soft light-mode default with deep "ink" accents.
*   **Palette:**
    *   **Base:** `#FDFCFB` (Off-White) / `#F4F1EE` (Parchment)
    *   **User Accent:** `#1A1A1A` (Ink Black)
    *   **Intervention State:** `#E11D48` (Rose Red)
    *   **Role Colors:** Sage Green, Muted Terracotta, Ochre, and Indigo.
*   **Rationale:** Emphasizes the "shared drafting" and "collaborative writing" use case.

### Option C: "The Prism" (Vibrant & Digital)
*   **Concept:** Uses semi-transparent layers and vibrant gradients to emphasize the "fluidity" of AI context.
*   **Palette:**
    *   **Base:** `#0F172A` (Slate Navy)
    *   **User Accent:** `#10B981` (Vibrant Teal)
    *   **Intervention State:** `#8B5CF6` (Bright Purple)
    *   **Role Colors:** High-saturation Cyan, Magenta, Lime, and Orange.
*   **Rationale:** Best for high-energy demos; makes the different model turns "pop" during the WebSocket broadcast.

---

## 3. Key Screens & Component Design

### 3.1 Role Mapping (Room Setup)
*   **Design:** A "Deck" of cards. Each card represents a **Role** (e.g., Lead-Writer). The user uses a dropdown on the card to select a **Model/Provider** (Gemini, Fake-OpenAI, etc.) to establish the room's Role Mapping.
*   **Visual Cue:** When a provider is selected, the card’s border glows with that role's unique color. This establishes the visual "identity" of that model for the rest of the session.

### 3.2 The Multi-Bubble Chat Room
*   **Design:** A single vertical thread, but with **Header Badges** on every message.
*   **Header Shape:** `[Role Icon] Lead-Writer (via Gemini)`. 
*   **Alignment:** User messages are right-aligned (Blue). All AI messages are left-aligned, but each AI "Role" has a distinct background tint and left-border color corresponding to the Setup Screen.

### 3.3 The @-Mention Controller
*   **Interaction:** Typing `@` in the chat bar triggers a "Role Picker" popover.
*   **Design:** The popover shows the Roles (e.g., "@Reviewer") with their current status (Ready / Thinking). Selecting one transforms the text into a "Pill" (Token) that is visually distinct from normal text.

### 3.4 The "Pause & Intervene" Canvas
*   **Problem:** How to show the pipeline is halted for human input.
*   **Solution:** When "Pause" is toggled:
    1.  The chat history gains a subtle **Yellow Overlay** or "Hazard" stripes on the side margins.
    2.  The "Send" button transforms into an **"Inject & Resume"** button.
    3.  A floating banner appears at the top: `[PAUSED] Pipeline halted at 'Lead-Writer'. Provide feedback to continue.`

---

## 4. Component Patterns

### 4.1 The WorkflowState Sidebar (Shared Context)
*   **Design:** A collapsible right-hand panel showing the "Canonical State."
*   **Content:** Sections for `Latest Draft`, `Open Issues`, and `Project Goal`.
*   **Animation:** When a model finishes a turn, the sidebar content "flashes" or pulses to show that the shared memory has been updated.

### 4.2 Loading States: "The Turn Indicator"
*   **Design:** Instead of a generic spinner, Conclave uses a **"Pulse Badge."**
*   **Logic:**
    *   **Real Gemini:** Shows a slow, steady pulse while the API processes.
    *   **Fake Providers:** Shows the same pulse, but for a fixed "Simulated Thinking" duration (e.g., 1.5s).
*   **Rationale:** To the user, there should be **zero visual difference** between live and fake providers. This proves the "Unification" thesis—the UI treats all participants as equal entities.

### 4.3 Model Metadata Badges
*   **Context:** For reviewers/recruiters.
*   **Design:** A small info-icon next to the model name. On hover, it shows:
    *   `latency: 1.2s`
    *   `modelId: FAKE_GPT`
    *   `isMocked: true`
*   **Goal:** To prove the backend is actually tracking metrics even for the mocked turns.