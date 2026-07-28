# Learning 03: Provider Adapter Pattern

## 1. Problem Statement
Different LLM providers (e.g. Gemini, OpenAI, Claude) use highly heterogeneous API request and response formats. In a collaborative multi-model workspace, writing ad-hoc vendor-specific mapping logic inside chat controllers leads to tightly coupled code, high fragility, and significant refactoring effort whenever a vendor updates their API schema.

## 2. Decision Rationale
We implemented the **Adapter Design Pattern**. By defining a unified interface (`ChatAdapter`) and a unified internal model (`CanonicalMessage`), we decouple core system orchestration from individual LLM JSON schemas. Adding a new provider is as simple as creating a new class implementing the interface, without modifying chat routing logic.

## 3. Alternatives Considered
- **Direct Conditional Mapping (if-else):** Rejected because branching checks for every API request make controllers bloat, violate the Single Responsibility Principle, and fail under multi-model environments.
- **Spring AI Client Defaults:** Rejected because standard Spring AI dependencies do not support unified sequential pipeline controls and custom UI color theme configurations inside room profiles.

## 4. Internal Working
1.  **Request Adaptations:** A room's list of roles triggers message generation. The system extracts history logs as `CanonicalMessage` objects, and the adapter converts them to vendor-specific layouts (e.g., Gemini's Content lists, OpenAI's messages role lists).
2.  **Streaming & Response Translation:** The adapter converts vendor responses (whether actual API network packets or simulated mock content) back into the standard `CanonicalMessage` model format.

## 5. Conclave Implementation
- The unified adapter interface is defined in [ChatAdapter.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/integration/adapter/ChatAdapter.java).
- Standard vendor schemas are converted inside [GeminiAdapter.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/integration/adapter/GeminiAdapter.java).
- Decoupled database logs are structured in [CanonicalMessage.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/domain/CanonicalMessage.java).

## 6. Key Classes
- [ChatAdapter.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/integration/adapter/ChatAdapter.java) - Shared adapter interface contracts.
- [GeminiAdapter.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/integration/adapter/GeminiAdapter.java) - Google Vertex AI adapter.
- [CanonicalMessage.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/domain/CanonicalMessage.java) - DB entity for unified history logs.

## 7. Common Pitfalls
- **Loss of Role Metadata:** Different models label system prompts and assistant prompts differently. Failing to map these carefully can result in models failing to understand who said what in a multi-turn chat history.
- **Token Count Discrepancies:** Different providers use different tokenizers (tiktoken, SentencePiece). Token usage audits should rely on provider-supplied metadata fields rather than local heuristics.

## 8. Debugging Tips
- Trace mapping outputs by adding debug breakpoints inside `toProviderFormat` and `fromProviderFormat` methods.
- Check database logs inside `conversation_history` to verify that message fields conform to Canonical representation types.

## 9. Interview Questions
1.  *How does the Adapter pattern prevent vendor lock-in inside Conclave?*
2.  *What properties reside in your CanonicalMessage entity, and how do they map to Gemini's user/model roles?*
3.  *If you wanted to add a Cohere API adapter, what steps would be required?*

## 10. References
- [GoF Design Patterns: Adapter](https://en.wikipedia.org/wiki/Adapter_pattern)
- [Google Cloud Vertex AI Gemini API Reference](https://cloud.google.com/vertex-ai/docs/generative-ai/model-reference/gemini)
