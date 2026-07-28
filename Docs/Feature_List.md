# Conclave — Feature List

**PROJECT NAME:** Conclave
**Stack:** Java / Spring Boot + Spring AI, React
**Repo shape:** Single monorepo (`backend/` + `frontend/`) — no separate AI-engine service
**Core Differentiator:** Multi-Provider Context Unification

## Core Concept

A shared "meeting room" where you and multiple LLMs (Gemini, OpenAI, Claude) discuss in
one conversation — no more manually copy-pasting context between tabs.

## Backend Layer (Spring Boot + Spring AI)

- Unified/canonical message schema — conversation stored once, in its own normalized format
- Per-provider adapter layer — translates the shared format into whatever shape each
  provider's API actually expects (Gemini, OpenAI, Claude each differ in message/role
  structure)
- **Gemini adapter** — real API integration (free tier), used for actual v1 conversations
- **OpenAI adapter** and **Claude adapter** — fully implemented and unit-tested as Fake Providers returning stubbed responses, proving the translation logic without real API calls or cost; real wiring deferred to v2
- Model Registry and Role Mapping — roles are assigned to model IDs at runtime (stored as Role Mappings), which are dynamically resolved to their corresponding `ChatClient` beans from the Model Registry
- Summarized `WorkflowState` (task, current draft, review comments, history) passed
  between turns instead of full transcript — cheaper and easier to reason about
- Conversation persistence — full shared history stored and reusable across the session
- Per-turn token/cost logging — tracked even at $0 real spend, so the pattern is
  demonstrable and extensible to paid providers later
- Auth + user accounts

## Frontend Layer (React)

- **Premium Console Layout**: Engineered as a flat, high-density split panel (Level 0 main background, Level 1 sidebar/header, Level 2 inputs/cards, Level 3 active hover states).
- **Geometric Consensus Branding**: Integrated custom 4-pointed Consensus Star logo that functions cleanly as a favicon and monochrome-compatible asset.
- **Form Autofill Overrides**: Overrides browser default credentials autofill to prevent yellow/white input box backgrounds.
- **@-mention turn-taking**: Moderated conversation control using popover mention menus.
- **Shared context view**: Monospace telemetry dashboards and side panels to track active objective states, consensus drafts, and token consumption metrics.
- **Pause & intervene control**: Diagonally striped warning decks with manual overrides to pause, inject corrections, and resume the active sequence.

## Realtime Layer

- WebSockets (STOMP) — live agent turns broadcast to all connected clients, chat-style

## Signature Feature — Multi-Provider Context Unification

- The core engineering problem: no two LLM providers expect conversation history in the
  same shape
- Adapter pattern per provider, translating one canonical conversation into three
  different API formats on demand
- Differentiator preserved at zero real cost: Gemini's adapter is exercised live; OpenAI's and Claude's adapters are exercised through Fake Providers, so all three translations are
  proven even though only one provider is actually called
- Explicitly scoped as integration/systems engineering, not AI engineering — no retrieval,
  no agentic reasoning in v1, just clean orchestration across providers
