# Conclave Backend Service

The Conclave backend is a Spring Boot application built using Java 21, Spring Security, Spring AI, WebSockets (STOMP), and Hibernate JPA.

---

## ⚙️ Configuration & Environment Variables

The application resolves environment configurations via standard Spring properties and `.env` variable binding.

### Required Environment Properties:
- `JWT_SECRET`: HS256 HMAC Secret Key used to sign JWTs (minimum 256 bits / 32 characters).
- `JWT_EXPIRATION`: Token validity duration (default: `86400000` ms / 24 hours).
- `GCP_PROJECT_ID`: Your Google Cloud Project ID (required for Gemini Vertex AI integration).
- `GCP_LOCATION`: Vertex AI service deployment location (e.g. `us-central1`).
- `DB_URL`: JDBC Database URL (default: `jdbc:postgresql://localhost:5432/conclave_db`).
- `DB_USER` / `DB_PASSWORD`: PostgreSQL access credentials (default: `conclave_user` / `conclave_password`).

---

## 🛠️ Development & Maven Commands

Run these standard Maven wrapper commands from the `backend/` directory:

### Compile the project:
```bash
mvn clean compile
```

### Run automated unit and integration tests:
```bash
mvn test
```
This runs the entire test suite, including:
- Unit tests: `WorkflowStateServiceTest`, `MentionParserTest`.
- Controller integration tests: `AuthControllerIntegrationTest`, `PipelineSequentialIntegrationTest`.

### Run the application locally in developer mode:
```bash
mvn spring-boot:run
```

---

## 🗄️ Database & Hibernate Migrations

- **DDL Configuration:** The dev profile utilizes Hibernate automatic schema validation and update:
  `spring.jpa.hibernate.ddl-auto: update`
- **Dialect:** PostgreSQL 16 Dialect configurations:
  `spring.jpa.properties.hibernate.dialect: org.hibernate.dialect.PostgreSQLDialect`
- **Show SQL:** Hibernate queries are logged inside console terminals for quick analysis during development:
  `spring.jpa.show-sql: true`
