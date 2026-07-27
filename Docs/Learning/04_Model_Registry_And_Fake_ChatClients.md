# Model Registry & Simulated ChatClients Architecture

This document outlines the architectural design and design defense for using simulated "Fake" clients instead of network mocking (e.g. WireMock), simulated latency, token estimation rules, and instructions for transitioning from simulated to live API endpoints.

---

## 1. Design Defense: Fake Clients vs. Network Mocking

### 1.1 The Challenge of Network Mocking in Early Development
While tools like WireMock are excellent for integration testing of external HTTP calls, they introduce several drawbacks when simulating complex AI behavior during initial phases:
1. **Network Overhead:** Network mocks require starting HTTP servers (even local ones) which slows down local development and unit tests.
2. **Brittle Schemas:** Hardcoded HTTP responses easily break when external API schemas evolve.
3. **Complexity of Streaming:** Simulating Server-Sent Events (SSE) or chunked streams via WireMock requires complex HTTP protocol mocking that distracts from core domain orchestration.
4. **API Key Dependency:** Autoconfigured client starters often require active connections or credential validation during startup, resulting in early crashes.

### 1.2 The Solution: Custom Fakes
By implementing Spring AI's standard `ChatModel` interface directly (e.g. `FakeOpenAiChatClient` and `FakeClaudeChatClient`), we achieve:
- **Zero Network Dependency:** Booting the application requires no external endpoint connectivity or WireMock setup.
- **Type Safety:** Compilation checks ensure our simulated clients always match Spring AI's expected interfaces.
- **In-Memory Simulations:** Non-blocking Reactor stream chunking runs purely in memory, resulting in sub-millisecond execution times in unit tests.
- **Graceful API Key Fallback:** If Vertex AI Gemini keys are missing, the application context automatically falls back to an in-memory Gemini model, preventing startup crash.

---

## 2. Simulation Specifications

### 2.1 Latency Simulation
- **Non-Streaming Calls:** Executing `call(Prompt)` blocks the executing thread for exactly **1.5 seconds** (`Thread.sleep(1500)`) to simulate the typical network round-trip of a medium-sized LLM response.
- **Streaming Chunks:** Executing `stream(Prompt)` emits tokens (words) one-by-word separated by a **50ms** reactive delay (`delayElements(Duration.ofMillis(50))`) to simulate the token-by-token rendering on the frontend.

### 2.2 Token Estimation Heuristic
To provide realistic performance metrics without complex byte counting, fakes calculate simulated usage using a standard length heuristic:
$$\text{Tokens} = \frac{\text{Length of content (characters)}}{4}$$

- **Input Tokens:** Calculated based on the prompt's unified contents character length.
- **Output (Generation) Tokens:** Calculated based on the generated mock Markdown character length.
- **Metadata Injection:** Standard Spring AI `Usage` metadata is populated and injected into the final chunk of streaming responses, or in the root of non-streaming responses.

---

## 3. Transitioning from Fakes to Live Clients

The system is designed with **Dependency Inversion** so that transitioning from "Fake" to "Live" requires zero modifications to business logic.

### 3.1 Swapping Configs
To activate live OpenAI and Claude clients:
1. Update `backend/pom.xml` to include Spring AI starters for OpenAI and Anthropic:
   ```xml
   <dependency>
       <groupId>org.springframework.ai</groupId>
       <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
   </dependency>
   <dependency>
       <groupId>org.springframework.ai</groupId>
       <artifactId>spring-ai-anthropic-spring-boot-starter</artifactId>
   </dependency>
   ```
2. Update `SpringAiConfig.java` to inject the autoconfigured `OpenAiChatModel` and `AnthropicChatModel` beans rather than instantiating the fakes:
   ```java
   @Bean
   @Qualifier("openAiChatClient")
   public ChatClient openAiChatClient(OpenAiChatModel openAiChatModel) {
       return ChatClient.create(openAiChatModel);
   }

   @Bean
   @Qualifier("claudeChatClient")
   public ChatClient claudeChatClient(AnthropicChatModel anthropicChatModel) {
       return ChatClient.create(anthropicChatModel);
   }
   ```
3. Supply active API keys in `.env.local` or environment variables:
   - `SPRING_AI_OPENAI_API_KEY`
   - `SPRING_AI_ANTHROPIC_API_KEY`

Because the `ModelRegistry` continues to resolve qualified `ChatClient` beans by qualifier, the orchestration engine remains completely unaffected by the transition to live APIs.
