# Product Requirements Document (PRD): Conclave

**Project Name:** Conclave — Multi-Provider Context Unification Platform  
**Status:** Planning / Architecture Phase  
**Document Version:** 1.0  

---

## 1. Problem Statement
The current AI landscape is fragmented across disparate silos (OpenAI, Anthropic, Google). Power users frequently "tab-hop" between these models to leverage specific strengths—e.g., using Claude for code architecture, GPT-4 for logic, and Gemini for large-context creative drafting. 

The friction in this workflow is **Context Fragmentation**: users must manually copy-paste background information, previous prompts, and model responses between tabs to maintain a coherent thread. This results in "context tax"—wasted time and lost nuance. 

**Conclave** solves this by providing a unified "meeting room" where multiple LLMs participate in a single conversation. The core thesis is **Context Unification**: engineering a backend capable of translating one canonical conversation history into the specific, varying API shapes required by different providers, ensuring all models share the same "memory" without manual intervention.

---

## 2. Target Persona & Use Case
*   **Target Persona:** Technical Recruiters and Engineering Managers.
*   **Core Use Case:** Demonstrating **Systems Engineering** and **Integration Patterns**. The project serves as a portfolio piece to show how a single Java/Spring Boot backend can orchestrate complex, multi-vendor API interactions using the Adapter Pattern. 
*   **User Action:** A user initiates a session, assigns roles (e.g., "Writer" to Gemini, "Reviewer" to a mocked Claude), and directs a collaborative task. The system handles the state translation so each model sees the updated "Workflow State" as the conversation progresses.

---

## 3. Functional Requirements (In-Scope)

### 3.1 Backend Layer (Spring Boot + Spring AI)
*   **Unified Message Schema:** A single, normalized database format to store conversation history, independent of provider-specific requirements.
*   **Provider Adapter Layer:** A robust translation tier that maps the canonical schema into the distinct message/role structures required by:
    *   **Google Gemini** (Live API integration).
    *   **OpenAI GPT** (Mocked adapter/stubbed responses for v1).
    *   **Anthropic Claude** (Mocked adapter/stubbed responses for v1).
*   **Workflow State Management:** Implementation of a `WorkflowState` object that summarizes the conversation to reduce token costs and noise when passing context between turns.
*   **Dynamic Model Registry:** A `Map`-based registry allowing the application to resolve and inject the correct `ChatClient` bean based on the assigned role/model.
*   **Real-time Orchestration:** Using WebSockets (STOMP) to broadcast model turns and status updates to the frontend.

### 3.2 Frontend Layer (React)
*   **Multi-Agent Chat UI:** A specialized interface using distinct visual identifiers (colors/icons) to represent different models in a single thread.
*   **Moderated Turn-Taking:** An "@-mention" system allowing the user to explicitly select which model responds next.
*   **The "Pause & Intervene" Mechanism:** UI controls to halt a sequential model pipeline (e.g., Writer → Reviewer) to inject manual corrections before the next model processes the state.
*   **Shared Context View:** A sidebar or toggle to view the current `WorkflowState` (the summarized context) being sent to the models.

---

## 4. Explicit Non-Goals
*   **No Retrieval-Augmented Generation (RAG):** The project focuses on orchestration and schema translation, not document retrieval or vector search.
*   **No Agentic Autonomy:** Models do not decide when to speak; they respond only to user-directed turns or hardcoded sequential pipelines.
*   **No Multi-Vendor Paid Costs:** In v1, real API calls are strictly limited to the Google Gemini free tier. All other providers must be fully implemented via the Adapter Pattern but return stubbed data.
*   **No Complex UI Animations:** Focus is on state consistency and message delivery, not high-fidelity chat aesthetics.

---

## 5. Success Criteria
*   **Schema Translation Integrity:** Verification (via unit tests) that a single `CanonicalMessage` correctly transforms into `User/Assistant` pairs for OpenAI and `user/model` pairs for Gemini.
*   **Turn-Taking Accuracy:** The system correctly routes an "@Gemini" request to the Google provider and an "@Claude" request to the Anthropic mock.
*   **State Persistence:** A shared "Workflow State" is successfully updated after each turn and persisted in PostgreSQL, allowing for session resumption.
*   **Latency Management:** WebSockets broadcast model "typing" states and responses with <200ms overhead (excluding AI generation time).

---

## 6. Key Risks & Open Questions
*   **Summarization Quality:** Does the `WorkflowState` summary lose critical technical details that a "Reviewer" model would need?
*   **State Collision:** How does the system handle a user "Intervening" at the exact moment a model response is being processed? (Needs a "Locking" or "Sequence" strategy).
*   **Adapter Maintenance:** Different providers frequently update their API shapes. The architecture must allow for easy updates to the Adapter classes without touching the core business logic.
*   **Context Window Limits:** Even with summarization, how does the system handle the transition if the conversation history grows beyond a specific provider's limit?