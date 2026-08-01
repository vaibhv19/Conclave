# Conclave Backend Service Spec

The Conclave backend is a monolithic Spring Boot service built using Java 21, Spring Security, Spring AI, WebSockets (STOMP), and Hibernate JPA. 

---

## 1. Architectural Highlights

*   **Virtual Threads Concurrency:** Uses Java 21 virtual threads via a customized `AsyncTaskExecutor` config (`AsyncConfig.java`) to offload slow, blocking LLM requests from Tomcat's carrier threads.
*   **Dynamic registry Model Resolution:** Resolves target AI clients at runtime (`ModelRegistryImpl.java`) to standard Spring AI client beans.
*   **Provider Adapter Pattern:** Decouples core logic from provider structures using the `ProviderAdapter` interface to serialize outgoing payloads and parse incoming responses.
*   **Pessimistic DB Locks:** Restricts pipeline race conditions using SQL write locks (`SELECT FOR UPDATE`) on the `rooms` entity during pause/resume state transitions.

---

## 2. Configuration & Environment Variables

The service parses configuration properties at boot time from environment variables. Copy `.env.example` in the root folder to `backend/.env` (or configure host variables) before starting:

| Property Name | Expected Type | Description | Default Value |
| :--- | :--- | :--- | :--- |
| `JWT_SECRET` | `VARCHAR` | HMAC secret key used to sign JWTs (min 256 bits). | `ConclavePlatformSuperSecureSecretKey123!` |
| `JWT_EXPIRATION` | `INTEGER` | JWT token validity duration (ms). | `86400000` (24 Hours) |
| `GCP_PROJECT_ID` | `VARCHAR` | GCP project ID hosting Vertex AI. | *(Required for Gemini)* |
| `GCP_LOCATION` | `VARCHAR` | Vertex AI service deployment zone. | `us-central1` |
| `DB_URL` | `VARCHAR` | JDBC database URL. | `jdbc:postgresql://localhost:5432/conclave_db` |
| `DB_USER` | `VARCHAR` | PostgreSQL database user. | `conclave_user` |
| `DB_PASSWORD` | `VARCHAR` | PostgreSQL access password. | `conclave_password` |

---

## 3. CLI Operations & Maven Commands

Run these wrapper scripts from the `backend/` directory:

### Clean & Compile Backend
```bash
./mvnw clean compile
```

### Run Backend Tests
```bash
./mvnw test
```
The test suite includes:
*   **Unit Tests:** Validates adapter translations (`GemmaAdapterTest`, `LlamaAdapterTest`, `MistralAdapterTest`) and parser filters (`MentionParserTest`).
*   **Integration Tests:** Verifies REST auth and sequential execution transactions (`AuthControllerIntegrationTest`, `PipelineSequentialIntegrationTest`).

### Run Local Development Server
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## 4. Hibernate & Database Mapping

*   **DDL Updates:** The dev profile uses Hibernate auto-schema configuration:
    `spring.jpa.hibernate.ddl-auto: update`
*   **SQL Auditing:** SQL statements are printed inside console terminals for quick query analysis:
    `spring.jpa.show-sql: true`
*   **Cascading Deletes:** Room cascades are handled at database transaction boundaries (`ON DELETE CASCADE`), automatically clearing associated messages and usage logs when a room is dropped.

---

## 5. Engineering References

Study the following handbook chapters to understand the backend internals:
*   **[JWT Security Filter Internals](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Learning/02_JWT_Authentication_Strategy.md):** Detailed filter mappings and STOMP upgrade interceptors.
*   **[Model Adapter Mappings](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Learning/03_Provider_Adapter_Pattern.md):** Serializations and sequence checks.
*   **[Model Bean Registry Specs](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Learning/04_Model_Registry_And_Ollama_Clients.md):** Dynamic mappings and Ollama model clients.
*   **[History Compactor Janitor](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Learning/05_Context_Compression_And_Janitor_Service.md):** Summarization prompts and purges.
*   **[Pessimistic Locking Details](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Learning/07_Pause_And_Intervene_Pipeline_Locking.md):** Database locking states and race prevention.
