# Tech Stack Specification: Conclave

This document defines the architectural choices and technical specifications for **Conclave**. The stack is designed to demonstrate high-level systems integration and the "Adapter Pattern" for multi-vendor AI orchestration using a Spring-centric backend.

---

## 1. Backend Orchestration (Spring Boot)

| Technology | Version | Rationale |
| :--- | :--- | :--- |
| **Java (JDK)** | `21` | Uses Virtual Threads to handle high-concurrency blocking I/O during long-running LLM API requests. |
| **Spring Boot** | `3.3.1` | Provides the foundational ecosystem for dependency injection and production-ready REST/Socket handling. |
| **Spring AI** | `1.0.0-M1` | Leverages the `ChatClient` abstraction to standardize interactions across different LLM providers. |
| **Spring Security** | `6.x` | Implements JWT-based stateless authentication for user accounts and session persistence. |
| **Spring Data JPA** | `3.x` | Manages relational persistence for Chat Rooms, `CanonicalMessage` history, and `WorkflowState`. |
| **Dynamic Registry** | `Map<String, ChatClient>` | A custom `@Service` registry that resolves the specific model implementation (Gemini, Mock-OpenAI) at runtime based on user-assigned roles. |

---

## 2. Real-time & Integration Layer

| Technology | Version | Rationale |
| :--- | :--- | :--- |
| **WebSockets (STOMP)** | `Spring Message` | Implements a Pub/Sub broadcast pattern to push model "typing" states and partial responses to the frontend. |
| **Gemini Integration** | `Google Vertex AI` | The primary live integration; uses Spring AI's Vertex AI starter with environment-based API key injection. |
| **Mocked Adapters** | `Stubbed ChatClient` | OpenAI and Claude are implemented as "Fake" beans that simulate the latency and message structure of the real APIs without incurring costs. |
| **Payload Schema** | `JSON / STOMP` | Broadcasts a `TurnEvent` object containing `roleIdentifier`, `modelName`, `contentDelta`, and the updated `WorkflowState` summary. |

---

## 3. Frontend Client (React)

| Technology | Version | Rationale |
| :--- | :--- | :--- |
| **Framework** | `React 19` | Utilizes the latest rendering engine for a highly responsive, real-time message stream. |
| **Build Tool** | `Vite` | Optimized for fast HMR (Hot Module Replacement), essential for fine-tuning real-time WebSocket UIs. |
| **State Management** | `Zustand` | A lightweight store used to manage the active "Meeting Room" state, @-mention logic, and sequential pipeline progress. |
| **WebSocket Client** | `@stomp/stompjs` | Provides a robust client-side implementation of the STOMP protocol with automatic reconnection logic. |
| **UI Components** | `Tailwind CSS` | Enables rapid styling of distinct color-coded bubbles per model to visually differentiate contributors. |

---

## 4. Monitoring & Data Strategy

### 4.1 Cost & Token Logging
Even with $0 real spend on mocked providers, Conclave implements a **Usage Interceptor**:
*   **Live (Gemini):** Extracts real token usage from the `ChatResponse` metadata.
*   **Mocks (OpenAI/Claude):** Calculates "Simulated Tokens" based on a character-count heuristic (e.g., chars/4).
*   **Storage:** Every turn logs a `UsageTransaction` in PostgreSQL, recording `modelId`, `promptTokens`, and `completionTokens` to demonstrate a production-ready cost-tracking pattern.

### 4.2 Local Infrastructure
*   **PostgreSQL 16:** Local container or instance for persistence of rooms and workflow history.
*   **Docker Compose:** Orchestrates the Backend, Frontend, and Database for a one-click local developer experience.
*   **Env Configuration:** A `.env.local` template for managing the Gemini API key and toggling between "Strict Mock" and "Live" modes.

---

## 5. Architectural Rationales: The "Interview" Defense

### Why use "Fake ChatClient" beans for OpenAI/Claude instead of WireMock or skipping them?
"In an interview setting, I would defend this choice as a focus on **Internal Interface Engineering** rather than **Network Proxying**. 

1. **Testing the Adapter:** By implementing a `FakeOpenAiChatClient` bean that satisfies the Spring AI `ChatClient` interface, I am forced to write the actual translation logic that converts my `CanonicalMessage` into the OpenAI `Message` format. If I used WireMock, I would only be testing the HTTP call. This approach proves I can engineer the **Provider Adapter Pattern** itself.
2. **Deterministic Simulation:** Real LLM APIs are non-deterministic and have latency. A fake bean allows me to simulate 'Thinking...' states and staggered WebSocket broadcasts reliably during a demo, ensuring the frontend handling of multi-model turns is bulletproof.
3. **Extensibility:** Switching OpenAI from 'Mock' to 'Live' in v2 becomes a simple configuration change (replacing a `@Bean` or changing a `@Profile`), proving the system is built with a 'Plug-and-Play' architecture from Day 1."