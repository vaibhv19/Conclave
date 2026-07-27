# JWT Authentication Strategy

This document details the stateless JWT authentication strategy and configuration designed for **Conclave**.

---

## 🔐 1. Authentication Architecture (Stateless)

Conclave uses stateless JWT-based session management to protect restricted API endpoints, ensuring maximum scale and horizontal compatibility.

```text
  +------------------+          1. POST /api/auth/login          +------------------+
  |                  +------------------------------------------>|                  |
  |                  |          (credentials payload)            |                  |
  |                  |                                           |  AuthController  |
  |                  |          2. HTTP 200 OK                   |                  |
  |                  |<------------------------------------------+  (BCrypt Check)  |
  |                  |          (returns Bearer token + User)    |                  |
  |  Vite Frontend   |                                           +------------------+
  |  (React Store)   |
  |                  |          3. GET /api/rooms                +------------------+
  |                  +------------------------------------------>|  JwtAuthFilter   |
  |                  |          (Authorization: Bearer <token>)  |                  |
  |                  |                                           |  (Context setup) |
  |                  |          4. HTTP 200 OK                   |        |         |
  |                  |<------------------------------------------+  Restricted Route|
  +------------------+                                           +------------------+
```

---

## ⚡ 2. Token Lifecycle & Structure

### Signature
- Algorithm: **HMAC SHA-256 (HS256)**
- Key requirements: Minimum 256 bits (32 bytes). In development, a fallback key is embedded, but in production this is configured via:
  `conclave.jwt.secret` (environment variable injection).

### Payload Details
- **Subject (`sub`):** The user's email address (unique identifier).
- **Issued At (`iat`):** Timestamp indicating when the token was created.
- **Expiration (`exp`):** Timestamp indicating when the token expires (default is 24 hours, configurable via `conclave.jwt.expiration`).

---

## 🛡️ 3. Spring Security Filter Chain & Configuration

### Filter Chain Ordering
The custom `JwtAuthenticationFilter` intercepts requests and is registered **before** Spring Security's `UsernamePasswordAuthenticationFilter`.

```text
[HTTP Request]
       |
       ▼
[CorsFilter] (Permits origins, e.g. http://localhost:5173)
       |
       ▼
[JwtAuthenticationFilter]
       |---> If starts with "Bearer " and valid: sets SecurityContextHolder Authentication
       |---> Else: skips context setup (remains unauthenticated)
       ▼
[UsernamePasswordAuthenticationFilter] (Default basic auth checks)
       |
       ▼
[AuthorizationFilter] (Asserts route rule access)
       |---> Public path (/api/auth/**): OK
       |---> Restricted path (/api/**): Check Authentication in context
       ▼
[Controller Endpoint]
```

### Path Protection Rules
- **Public Routes:** `/api/auth/**` (Registration and Login) are accessible by anyone.
- **Secured Routes:** `/api/**` paths require a valid Bearer token. Attempts to access these paths unauthenticated will result in a `403 Forbidden` response.
- **CORS Config:** Configured globally to permit incoming requests from `http://localhost:5173` with credentials allowed, enabling integration with the local Vite dashboard.
