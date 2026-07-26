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
- **OpenAI adapter** and **Claude adapter** — fully implemented and unit-tested against
  mocked/stubbed responses, proving the translation logic without real API calls or cost;
  real wiring deferred to v2
- Dynamic role-to-model registry (`Map<String, ChatClient>`-style) — roles assigned to
  models at runtime, not hardcoded
- Summarized `WorkflowState` (task, current draft, review comments, history) passed
  between turns instead of full transcript — cheaper and easier to reason about
- Conversation persistence — full shared history stored and reusable across the session
- Per-turn token/cost logging — tracked even at $0 real spend, so the pattern is
  demonstrable and extensible to paid providers later
- Auth + user accounts

## Frontend Layer (React)

- Chat room UI — different colored bubbles per model, so it's visually clear who said what
- @-mention turn-taking — you direct which model responds next (you're the moderator, not
  an automated one)
- Shared context view — every model sees the same conversation history regardless of who's
  replying
- Pause & intervene control — stop the flow mid-turn-sequence and inject your own message
  before it continues

## Realtime Layer

- WebSockets (STOMP) — live agent turns broadcast to all connected clients, chat-style

## Signature Feature — Multi-Provider Context Unification

- The core engineering problem: no two LLM providers expect conversation history in the
  same shape
- Adapter pattern per provider, translating one canonical conversation into three
  different API formats on demand
- Differentiator preserved at zero real cost: Gemini's adapter is exercised live; OpenAI's
  and Claude's adapters are exercised through tested mocks, so all three translations are
  proven even though only one provider is actually called
- Explicitly scoped as integration/systems engineering, not AI engineering — no retrieval,
  no agentic reasoning in v1, just clean orchestration across providers
