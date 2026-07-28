# Learning 02: JWT Authentication Strategy

## 1. Problem Statement
In a multi-user collaborative workspace like Conclave, endpoints (like room creation and pipeline controls) must be protected. Standard session-based (stateful) authentication hinders horizontal scale, introduces server-side session tracking bloat, and complicates asynchronous WebSocket channel handshakes.

## 2. Decision Rationale
We chose a stateless **JWT (JSON Web Token)** authentication strategy:
- Enables stateless request verification at the filter layer without database queries on every HTTP request.
- Permits passing auth tokens in WebSocket STOMP handshake connect headers, securing socket connections seamlessly.
- Fits modern microservices-ready design criteria.

## 3. Alternatives Considered
- **Stateful HTTP Sessions:** Rejected due to replication requirements across distributed backends and incompatibility with stateless WebSocket interceptors.
- **API Key Authentication:** Rejected because it lacks expiration timelines, user context profiles, and standard claim verification mechanisms.

## 4. Internal Working
1.  **Generation:** AuthController validates credentials via BCrypt, issues token containing claims (`sub` = email, `iat`, `exp`), signed with a HMAC-SHA256 secret.
2.  **Filter Interception:** `JwtAuthenticationFilter` checks `Authorization: Bearer <token>` header, decodes signature, parses user context, and loads it into the `SecurityContextHolder`.
3.  **WebSocket Handshake Check:** Custom STOMP interceptor decodes incoming connection headers before completing connection protocol setups.

## 5. Conclave Implementation
- BCrypt hashing and stateless filter configurations are defined in [SecurityConfig.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/config/SecurityConfig.java).
- Token decoding is processed in [JwtTokenProvider.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/security/JwtTokenProvider.java) and verified by [JwtAuthenticationFilter.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/security/JwtAuthenticationFilter.java).
- Authenticated endpoints are mapped inside [AuthController.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/controller/AuthController.java).

## 6. Key Classes
- [JwtTokenProvider.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/security/JwtTokenProvider.java) - Decodes/generates signed JWTs.
- [SecurityConfig.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/config/SecurityConfig.java) - Security filter chain setup.
- [AuthController.java](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/java/com/conclave/controller/AuthController.java) - Auth endpoints.

## 7. Common Pitfalls
- **Expired Token Crash:** Tokens expire after 24 hours. The frontend store must handle token expiration by removing user credentials and redirecting to the login screen.
- **Weak Secret Key:** HS256 requires secret project keys of at least 256 bits (32 characters). Short strings will throw a startup error.

## 8. Debugging Tips
- Trace incoming header filters inside `JwtAuthenticationFilter.doFilterInternal` by logging claims.
- Inspect JSON signatures via external debugger platforms (e.g. `jwt.io`).

## 9. Interview Questions
1.  *What claims do you place in the JWT token payload, and why did you choose them?*
2.  *Where does the JwtAuthenticationFilter sit in the standard Spring Security Filter Chain?*
3.  *How do you handle JWT token validation inside WebSocket channels in Spring Boot?*

## 10. References
- [Spring Security Reference Manual](https://spring.io/projects/spring-security)
- [RFC 7519: JSON Web Token Spec](https://tools.ietf.org/html/rfc7519)
