# Security Architecture: Conclave

This document defines the security policies, authentication flows, authorization checks, and data isolation strategies for the **Conclave** platform.

---

## 1. Problem & Threat Model

Orchestrating multi-model AI workspaces in a collaborative environment introduces key security challenges:
*   **Data Leakage & Cloud Exposure:** Sending sensitive conversation data, project objectives, or proprietary draft code to external cloud AI providers (e.g., OpenAI, Google Gemini, Anthropic) risks intellectual property exposure and violates compliance guidelines.
*   **Cross-Tenant Data Exposure:** Unauthorized users must not be able to read room objectives, chat histories, or draft summaries belonging to other users.
*   **Orchestration Hijacking:** Attackers must be prevented from injecting rogue prompts, hijacking active model queues, or triggering spam model executions.
*   **WebSocket Interception:** STOMP socket connections must be validated and isolated, preventing malicious users from subscribing to and listening to other rooms' event streams.

---

## 2. Authentication Architecture

Conclave uses a **Stateless JWT (JSON Web Token) Security Model**. The server does not maintain session records; instead, every REST and WebSocket connection is verified by extracting and validating a signed token payload.

```
       [ Client Application ]  ─────────── JWT in Header / STOMP payload ──────────┐
                  │                                                                │
                  ▼                                                                ▼
    ┌───────────────────────────┐                                    ┌───────────────────────────┐
    │  JwtAuthenticationFilter  │                                    │   JwtChannelInterceptor   │
    │  (REST / HTTPS Endpoint)  │                                    │   (WebSocket Upgrade)     │
    └─────────────┬─────────────┘                                    └─────────────┬─────────────┘
                  │                                                                │
                  ├─────────────────── Verification Logic ─────────────────────────┤
                  │                                                                │
                  ▼                                                                ▼
       * Extracts 'Bearer' text                                         * Extracts 'passcode' header
       * Verifies HS256 HMAC Signature                                  * Verifies signature & claims
       * Validates expiration claims                                    * Binds user to STOMP principal
                  │                                                                │
                  ▼                                                                ▼
      ┌──────────────────────────┐                                     ┌──────────────────────────┐
      │ SecurityContextHolder    │                                     │ WebSocket SimpUser       │
      │ (REST Authentication)    │                                     │ (Authorized Socket)      │
      └──────────────────────────┘                                     └──────────────────────────┘
```

### 2.1 REST Token Verification Lifecycle (`JwtAuthenticationFilter`)
1.  **Request Interception:** The client attaches `Authorization: Bearer <token>` to the HTTP headers. The request is intercepted by `JwtAuthenticationFilter` (extending Spring's `OncePerRequestFilter`).
2.  **Token Extraction:** The filter parses the token from the header, extracting the username (email) and expiration claims.
3.  **Signature Validation:** The token is validated using the server's private secret key. It uses the `HS256` HMAC algorithm.
4.  **Security Context Injection:** If valid, the filter loads the user's details (`UserPrincipal`), creates a `UsernamePasswordAuthenticationToken` object, and injects it into Spring Security's `SecurityContextHolder`. The request continues to the controller.
5.  **Invalid Tokens:** If signature verification fails, token has expired, or the user is not found, the filter returns a `401 Unauthorized` response to the client.

### 2.2 WebSocket STOMP Handshake & Authorization
Standard WebSockets do not natively support custom HTTP authorization headers after the initial upgrade handshake. To secure the socket stream:
*   **STOMP Connection Headers:** The client (`@stomp/stompjs`) sends the JWT token in the `passcode` or `Authorization` header of the STOMP `CONNECT` frame.
*   **Channel Interceptor validation:** Spring's WebSocket configuration registers a custom `ChannelInterceptor` (bound to `configureClientInboundChannel`).
*   **Token Verification:** On intercepting a `CONNECT` frame, the interceptor parses the token, verifies its signature, and binds the resolved `UserPrincipal` as the principal user of the WebSocket session (`SimpUser`).
*   **Destination Subscription Guard:** When a client attempts to subscribe to `/topic/room/{roomId}`, the interceptor checks that the authenticated session principal has access to that specific room. Unauthorized subscription requests are rejected, preventing eavesdropping.

---

## 3. Authorization & Tenant Isolation Strategy

Authentication only verifies *who* the user is. **Authorization** ensures they can only access resources they own.

```java
// PipelineManagerImpl.java (Ownership check)
Room room = roomRepository.findWithLockById(roomId)
        .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

if (!room.getOwner().getId().equals(requester.getId())) {
    throw new UnauthorizedAccessException("Only the room owner can pause/resume the pipeline");
}
```

### 3.1 Cross-Tenant Isolation Rules:
*   **Room Ownership:** The `rooms` table links each workspace to a specific `owner_id` (foreign key to `users`).
*   **Service-Level Assertions:** Every write mutation (e.g. updating model assignments, sending messages, pausing pipelines, or resuming pipelines) checks room ownership at the database transaction boundary.
*   **History Isolation:** `GET /chat/{roomId}/history` fetches the room details first and verifies that the authenticated user matches the room's owner before returning any messages. If unauthorized, the system throws `UnauthorizedAccessException` yielding a `403 Forbidden` response.

---

## 4. Localized Execution & Zero Cloud Credentials

To ensure complete data privacy and security, Conclave implements a localized model execution strategy:
*   **Zero External API Credentials:** Because all models run on the local host via Ollama, there are no third-party API keys (like Google Vertex AI, OpenAI, or Claude keys) stored on the backend or transmitted over the network.
*   **Backend Proxying:** The React frontend never connects to Ollama directly. All connections are proxied and authenticated via the Spring Boot backend using JWTs, ensuring workspace isolation.
*   **Elimination of Key Compromise Threat:** Standard risk vectors like API key compromise, billing spikes, and credential exposure in code repositories are completely mitigated.

---

## 5. Security Failure Modes & Recovery Strategies

| Failure Mode | Trigger / Cause | System Impact | Recovery Strategy |
| :--- | :--- | :--- | :--- |
| **Token Expiration** | JWT token lifespan exceeds configured validity limit (e.g., 24 hours). | API requests fail with `401 Unauthorized`. | The React client catches the 401 error, purges the expired token from the local Zustand store, and redirects the user to `/login`. |
| **WebSocket Hijacking Attempt** | Authenticated user attempts to subscribe to `/topic/room/{unauthorizedRoomId}`. | Interceptor rejects subscription frame. | The server throws an access exception, logs the security violation, and terminates the WebSocket connection. |
| **Secret Compromise** | Secret key exposed or leaked. | Attackers can sign rogue JWTs. | System administrators must rotate the signing secret (`jwt.secret` configuration property) in the backend and restart services, invalidating all active sessions. |

---

## 6. Interview Talking Points (Architectural Defense)

*   **Stateless JWT vs Stateful Sessions:** "We chose stateless JWTs to decouple authentication state from the backend. This enables horizontal scaling without needing shared session clustering (e.g. Redis Session stores), making the backend lightweight."
*   **Secure WebSockets Handshake:** "We secure WebSockets by registering a custom STOMP channel interceptor. Rather than trusting any client that completes the HTTP upgrade handshake, we validate the JWT signature during the STOMP `CONNECT` frame and enforce destination-level ownership checks during `SUBSCRIBE` requests, preventing cross-tenant channel eavesdropping."
*   **Zero Cloud Footprint / Complete Data Privacy:** "We enforce a zero cloud footprint architecture. All AI inference is run locally via Ollama. The React frontend is completely unaware of LLM connection mechanics, only communicating with our Spring Boot server using secure JWT tokens. The server handles template formatting and interfaces with the local Ollama daemon, ensuring no data ever leaves the local environment."
