# Conclave

**Status: 💤 Parked / Not Started**

## The Problem

While building previous projects, I found myself using multiple AI tools for different 
jobs — GPT, Gemini, Claude, Grok, Stitch, AI Studio, Antigravity, NotebookLM — each 
better suited to a specific role (technical writer, reviewer, tester, PM). 

The friction: I had to manually copy context between tools every time. No shared memory, 
no handoff, just me pasting the same background into 8 different chat windows. 
This project is meant to fix that.

## The Idea

A "group chat" for AI models, where each model is assigned a role (Writer, Reviewer, 
Refiner, Tester, PM, etc.) and they collaborate on a task — drafting, reviewing, and 
refining plans or documents — without me manually shuttling context between them.

## Planned Stack

- **Backend:** Spring Boot + Spring AI
- **Frontend:** React
- **Realtime:** WebSockets (STOMP) for live agent turns, chat-style

## Architecture Sketch

- Each agent = a Spring AI `ChatClient` bean, wired to a different provider/model, 
  with its role baked into the system prompt
- Shared `WorkflowState` object (task, current draft, review comments, history) passed 
  between agents instead of full transcript — cheaper and easier to reason about
- Sequential pipeline engine to start (e.g. Writer → Reviewer → Refiner), not a generic 
  agent graph — prove the mechanic before generalizing
- WebSocket broadcast so frontend shows each agent's turn live, chat-style
- "Pause & intervene" feature — stop the pipeline mid-run and inject my own message 
  before it continues (this solves the original frustration directly)

## Open Decisions (resolve before starting)

- [ ] Which 2 providers to support first — pick based on existing API access 
      (OpenAI/Anthropic are simplest to wire up in Spring AI; Gemini via Vertex AI 
      needs GCP project setup, heavier lift)
- [ ] How context gets passed — full history vs. summarized state object (leaning 
      toward summarized state for cost/simplicity)
- [ ] Hardcoded roles vs. dynamic role-to-model assignment (start hardcoded, 
      generalize later via a `Map<String, ChatClient>` registry)
- [ ] Cost tracking — multiple paid APIs per task add up fast, need per-turn 
      token/cost logging early

## Suggested Build Order (when resumed)

1. One `ChatClient` bean, single prompt/response — confirm Spring AI + provider 
   API keys work end to end
2. Second `ChatClient` (different provider), hardcoded sequential pipeline, 
   console-only output
3. WebSocket broadcasting to a minimal frontend
4. React chat UI consuming the WebSocket stream, agent-tagged messages
5. Pause/intervene feature
6. Generalize roles/models via registry instead of hardcoded beans

## Why Parked

Too much surface area to take on alongside current priorities — multi-provider 
integration, WebSockets, and a non-trivial frontend all at once. Better to ship 
other projects first and come back to this with more project experience and a 
clearer head.
