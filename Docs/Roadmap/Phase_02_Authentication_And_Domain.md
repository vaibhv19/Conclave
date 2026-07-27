# Phase 02 — Authentication & Domain Models

## 1. Module Planning: Authentication & Domain Models

### 1.1 Purpose
The purpose of this phase is to construct the foundational database schemas, map them to Java classes via JPA Entities, and implement stateless, JWT-based user authentication. Establishing the database models early prevents cyclic refactoring in subsequent phases.

### 1.2 Package / Folder Structure
```
backend/src/main/java/com/conclave/
├── domain/
│   ├── User.java                       # maps to users
│   ├── Room.java                       # maps to rooms
│   ├── RoleAssignment.java             # maps to role_assignments
│   ├── CanonicalMessage.java           # maps to conversation_history
│   ├── WorkflowState.java              # maps to workflow_state
│   ├── TokenUsageLog.java              # maps to token_usage_log
│   └── enums/
│       ├── RoomStatus.java             # INITIALIZED, ACTIVE, PAUSED, ARCHIVED
│       └── SenderType.java             # USER, AI, SYSTEM
├── repository/
│   ├── UserRepository.java
│   ├── RoomRepository.java
│   ├── RoleAssignmentRepository.java
│   ├── CanonicalMessageRepository.java
│   ├── WorkflowStateRepository.java
│   └── TokenUsageLogRepository.java
├── security/
│   ├── SecurityConfig.java             # Spring Security config
│   ├── JwtService.java                 # Generates/Validates JWTs
│   ├── JwtAuthenticationFilter.java    # Intercepts Bearer tokens
│   └── UserPrincipal.java              # UserDetails implementation
├── dto/
│   ├── UserRegisterRequest.java
│   ├── UserLoginRequest.java
│   ├── AuthResponse.java
│   └── UserResponse.java
└── controller/
    └── AuthController.java             # /api/auth/register and /api/auth/login
```

### 1.3 Responsibilities & Dependencies
- **Domain Mapping:** Define table constraints, indexes, unique constraints (`room_id`, `role_name`), and cascading deletion configurations (`ON DELETE CASCADE`) using Spring Data JPA annotations.
- **Security:** Standard JWT implementation using `io.jsonwebtoken` (jjwt-api, jjwt-impl, jjwt-jackson dependencies must be added to pom). Configure Spring Security to permit all requests on `/api/auth/**`, requiring Bearer token validation for all other `/api/**` paths.

---

## 2. Module Components

### 2.1 DTOs
- `UserRegisterRequest`: `{ email, password, name }` (email must be unique, password length >= 8).
- `UserLoginRequest`: `{ email, password }`.
- `AuthResponse`: `{ token, user: UserResponse }`.
- `UserResponse`: `{ id, email, name }`.

### 2.2 Validation & Logging
- Validate inputs using standard Jakarta validation annotations (`@Email`, `@NotBlank`, `@Size`).
- Exception handler intercepting `MethodArgumentNotValidException` or custom authentication errors to return the standard Conclave error format.
- Secure logging: Log user registration and logins. Never log raw passwords or JWT signatures.

---

## 3. Atomic Implementation Tasks

### Task 2.1: Create User Entity and UserRepository
- **Estimated Size:** S
- **Risk:** Low
- **Prerequisites:** Phase 01 Setup
- **Definition of Done:**
  - `User.java` class mapped to `users` table with columns: `id` (UUID), `email` (VARCHAR 255, unique, non-null), `passwordHash` (VARCHAR 255, non-null), `name` (VARCHAR 100, non-null).
  - `UserRepository.java` created extending `JpaRepository<User, UUID>` containing method `Optional<User> findByEmail(String email)`.
  - Compile and run basic Spring JPA context verification.

### Task 2.2: Implement Security Configurations and JWT Services
- **Estimated Size:** M
- **Risk:** Medium
- **Prerequisites:** Task 2.1
- **Definition of Done:**
  - Maven dependencies for JWT added (`io.jsonwebtoken:jjwt-api`, etc.).
  - `JwtService.java` created containing: `generateToken(UserDetails userDetails)`, `extractEmail(String token)`, and `isTokenValid(String token, UserDetails userDetails)`.
  - Token signing key configured via properties with secure fallback for development.
  - `UserPrincipal.java` created implementing Spring Security `UserDetails`.
  - Unit tests verify JWT token generation, extraction, expiration, and validation.

### Task 2.3: Configure Spring Security Filter Chain and Auth Controller
- **Estimated Size:** M
- **Risk:** Medium
- **Prerequisites:** Task 2.2
- **Definition of Done:**
  - `JwtAuthenticationFilter.java` implements request interception, extracts Bearer token, validates it against `JwtService` and sets the SecurityContext.
  - `SecurityConfig.java` defines the `@Bean SecurityFilterChain` enabling stateless sessions (`SessionCreationPolicy.STATELESS`), password encoding via `@Bean BCryptPasswordEncoder`, and CORS policy allowing requests from local Vite dashboard.
  - `AuthController.java` exposes `POST /api/auth/register` (hashes password with BCrypt, stores User, returns JWT) and `POST /api/auth/login` (checks password, returns JWT).
  - Integrations validated by checking authenticated versus public routes via integration test.

### Task 2.4: Create Core Project Domain Entities and Repositories
- **Estimated Size:** M
- **Risk:** Low
- **Prerequisites:** Task 2.1
- **Definition of Done:**
  - Create entities: `Room` (maps to `rooms`, containing status enum), `RoleAssignment` (maps to `role_assignments`, unique on `(room_id, role_name)`), `CanonicalMessage` (maps to `conversation_history`), `WorkflowState` (maps to `workflow_state`), and `TokenUsageLog` (maps to `token_usage_log`).
  - Configure foreign keys, `@ManyToOne` bindings, and `CascadeType.ALL` or `CascadeType.REMOVE` bindings to ensure cascade delete paths operate as specified in the DB Schema.
  - Create repositories for each entity extending `JpaRepository` with appropriate query methods (e.g., `findByRoomIdOrderByCreatedAtAsc` for messages).

---

## 4. Documentation & Verification

### Documentation to Update / Create
- Create `Docs/Learning/02_JWT_Authentication_Strategy.md` describing stateless login flow, token payload details, and Spring Security Filter Chain ordering.
- Document database schema mappings in project development wiki.

### Testing Checkpoint
- Perform integration tests checking REST authentication. Assert `/api/auth/register` rejects duplicate emails with a 409 status code.
- Validate database tables creation against local Postgres via PgAdmin/psql commands to verify JPA schema generation match.

### Suggested Git Commit Boundaries
1. `domain: create user entity and user repository`
2. `security: configure jwt services and helper classes`
3. `security: configure filter chain security rules and auth controllers`
4. `domain: build remaining relational entities and repository layer`

### Suggested GitHub Issues
- **Issue 2.1:** Create User entity, JPA configuration, and repositories. (Points: 1)
- **Issue 2.2:** Build JWT generator, validator services, and custom filter configurations. (Points: 2)
- **Issue 2.3:** Implement REST Authentication controller endpoints. (Points: 2)
- **Issue 2.4:** Build other database models (Room, Role, Message, State) and relationships. (Points: 3)
