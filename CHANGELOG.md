# Changelog

All notable changes to the Conclave platform will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.0] - 2026-08-02

### Added
- **Multi-Model Schema Translation (`ModelAdapter`):** Decoupled internal canonical conversation storage (`CanonicalMessage`) from model-specific prompt formats. Added adapters for Llama 3, Mistral, and Gemma.
- **WebSocket STOMP Real-Time Streams (`WebSocketConfig`):** Real-time pub/sub channels (`/topic/room/{roomId}`) with word-by-word streaming chunk delivery, dynamic typing indicators (`TURN_STARTED`), and telemetry summaries (`TURN_COMPLETED`).
- **Pessimistic Write Locking (`PipelineManager`):** Thread-safe workflow coordination using PostgreSQL database write locks (`SELECT FOR UPDATE`) to prevent race conditions during pause and turn advancement.
- **User Intervention Protocol:** Ability for operators to pause execution and inject manual steering/corrections (`isIntervention`).
- **Context Janitor History Compaction (`WorkflowStateService`):** Background history summarization compressing conversational history into updated draft and critique states when turns exceed thresholds.
- **Stateless JWT Security (`SecurityConfig`):** HMAC-SHA256 authenticated REST endpoints and WebSocket upgrade channel interceptors with room ownership verification.
- **React 19 Console Client (`RoomView`):** IDE-inspired split-view operator workspace with Zustand global state synchronization decoupled from React render cycles.

### Changed
- **Local Ollama Inference Transition:** Migrated core AI orchestration engine from external cloud APIs to 100% real local inference via a local Ollama daemon (`http://localhost:11434`), eliminating API keys and recurring cloud costs.
