# Learning 01: Developer Environment Setup

## 1. Problem Statement
Setting up a modern multi-model orchestrator environment requires configuring a PostgreSQL 16 database, Spring Boot backend, React + Vite frontend, STOMP WebSockets, and multiple LLM API integrations (e.g. Gemini Vertex AI). Without a standardized local environment setup and troubleshooting playbook, developers encounter port conflicts, missing credentials, database connection failures, and dependency misalignment.

## 2. Decision Rationale
We chose a containerized PostgreSQL 16 service using Docker Compose combined with local Java 21 and Node.js executions. This hybrid approach:
- Eliminates database configuration variance across developer machines.
- Allows native hot-reloading for the Spring Boot backend (`spring-boot:run`) and React frontend (`vite`), enabling rapid code-edit-test loops.
- Provides standard logs and container health monitoring.

## 3. Alternatives Considered
- **Full Dockerization (Backend, DB, Frontend in Docker):** Rejected due to slow build compilation times and complex configurations for live hot-reloading.
- **Local PostgreSQL Installation:** Rejected because local Postgres versions vary, leading to database schema mismatch or port allocation conflicts.

## 4. Internal Working
The developer setup coordinates:
1.  **Docker Compose** orchestrating the database container.
2.  **Spring Boot Application** reading variables from active profiles (`dev` / `test`) to connect to JDBC endpoints.
3.  **Vite Hot-Module-Replacement (HMR)** hosting client pages at `http://localhost:5173`.
4.  **Playwright** driving browser context testing under virtual ports.

## 5. Conclave Implementation
- The database runs as a container named `conclave-postgres` via [docker-compose.yml](file:///d:/Coding/Projects----For%20Resume/Conclave/docker-compose.yml).
- The Spring Boot profile configuration is stored inside `backend/src/main/resources/application-dev.yml`.
- Frontend environment is bootstrapped using standard npm scripts.

## 6. Key Classes
- [docker-compose.yml](file:///d:/Coding/Projects----For%20Resume/Conclave/docker-compose.yml) - Defines container dependencies.
- [package.json](file:///d:/Coding/Projects----For%20Resume/Conclave/frontend/package.json) - Specifies scripts and dependencies.
- [application.yml](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/resources/application.yml) - Shared properties config.

## 7. Common Pitfalls
- **Port 5432 Already Allocated:** Caused by a running host-native Postgres instance. Stop the host instance using command line controls first.
- **Node Modules Compilation Failures:** Occurs when node version drops below `18.x`. Ensure `node -v` meets specifications.

## 8. Debugging Tips
- Verify database health: `docker compose ps` and `docker compose logs -f postgres`.
- Verify backend connection status: Check Spring Boot initialization logs for successful Hibernate validation.

## 9. Interview Questions
1.  *Why did you use Docker Compose for the database layer but run Spring Boot locally?*
2.  *How do you manage configuration profiles (dev vs. test) inside your Spring Boot application?*
3.  *What tools do you use to resolve port conflicts in a monorepo setup?*

## 10. References
- [Spring Boot Profile Guides](https://spring.io/guides)
- [Docker Compose Spec](https://docs.docker.com/compose/)
