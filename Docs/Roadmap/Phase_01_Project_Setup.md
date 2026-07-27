# Phase 01 — Project Setup & Infrastructure

## 1. Module Planning: Project Setup

### 1.1 Purpose
The purpose of this phase is to establish the monorepo workspace for Conclave, bootstrap the Spring Boot backend and React frontend, and provision the local development PostgreSQL 16 database.

### 1.2 Package / Folder Structure
```
Conclave/
├── docker-compose.yml                # Spins up PostgreSQL 16
├── .gitignore                         # Standard project-level git exclusions
├── backend/                           # Maven Spring Boot module root
│   ├── pom.xml                        # Parent/Module project descriptor
│   └── src/
│       └── main/
│           ├── java/com/conclave/     # Core package root
│           │   └── BackendApplication.java
│           └── resources/
│               ├── application-dev.yml
│               └── application.yml
└── frontend/                          # Vite React module root
    ├── package.json
    ├── vite.config.js
    ├── tailwind.config.js
    ├── postcss.config.js
    └── src/
        ├── index.css
        ├── main.jsx
        └── App.jsx
```

### 1.3 Responsibilities & Dependencies
- **Backend:** Bootstrapping Java 21 & Spring Boot 3.3.1 ecosystem. Dependencies include: Spring Web, Spring Security, Spring Data JPA, PostgreSQL Driver, Spring AI (Vertex AI), Lombok, WebSockets.
- **Frontend:** Bootstrapping React 19, Vite, Tailwind CSS, Zustand, and `@stomp/stompjs`.
- **Infrastructure:** Docker Compose to run a local PostgreSQL instance.

---

## 2. Module Components

- **DTOs / Models:** None.
- **Configuration:**
  - `application.yml` / `application-dev.yml` (JPA settings, DB URL, Spring AI Vertex configs, STOMP broker endpoints).
  - `.env.example` (API key template for Gemini Vertex).
  - `docker-compose.yml` (Postgres service definition).
- **Security:** Standard `.gitignore` configurations to prevent API key leaks.
- **Testing Requirements:** Spin up check; verify Spring Context loading and database connection.

---

## 3. Atomic Implementation Tasks

### Task 1.1: Initialize Monorepo and Git Configuration
- **Estimated Size:** S
- **Risk:** Low
- **Prerequisites:** None
- **Definition of Done:** 
  - Root directory contains `.gitignore` filtering `.env`, `.env.local`, IDE files (`.idea`, `.vscode`), Maven targets (`target/`), and Node modules (`node_modules/`).
  - Git repository initialized (`git init`).

### Task 1.2: Configure Docker Compose for local PostgreSQL 16
- **Estimated Size:** S
- **Risk:** Low
- **Prerequisites:** Task 1.1
- **Definition of Done:**
  - `docker-compose.yml` created in root defining a `postgres:16-alpine` service with port forwarding `5432:5432`, volume mounting for persistence, database name `conclave_db`, user `conclave_user`, and password `conclave_password`.
  - Service started successfully via `docker compose up -d` and accepts TCP connections.

### Task 1.3: Bootstrap Backend Spring Boot Project
- **Estimated Size:** M
- **Risk:** Low
- **Prerequisites:** Task 1.2
- **Definition of Done:**
  - Maven project created in `backend/` using Spring Boot `3.3.1` and Java `21`.
  - `pom.xml` configured with: Spring Web, Spring Data JPA, PostgreSQL Driver, Lombok, and Spring AI BOM (`1.0.0-M1`).
  - `BackendApplication.java` contains the main method annotated with `@SpringBootApplication`.
  - `application.yml` and `application-dev.yml` configured to connect to the docker PostgreSQL container.
  - Project builds successfully via `mvn clean install` and runs without error.

### Task 1.4: Bootstrap Frontend React Project
- **Estimated Size:** M
- **Risk:** Low
- **Prerequisites:** Task 1.1
- **Definition of Done:**
  - React SPA initialized in `frontend/` using Vite with React 19.
  - Tailwind CSS configured and integrated via `postcss.config.js` and `tailwind.config.js`.
  - `package.json` contains dependencies for `react`, `react-dom`, `zustand`, `@stomp/stompjs`.
  - Run `npm run dev` spins up Vite server locally on port 5173.

---

## 4. Documentation & Verification

### Documentation to Update / Create
- Create `README.md` at root describing:
  - Prerequisites (Docker, JDK 21, Node.js).
  - Quickstart steps (running docker-compose, compiling backend, running frontend).
- Create developer setup guide in `Docs/Learning/01_Developer_Environment_Setup.md` containing local troubleshooting tips.

### Testing Checkpoint
- Run backend verification test: Spring context loads successfully and Hibernate makes initial database handshake.
- Run frontend verification: Default Vite page renders with Tailwind classes working (color test).

### Suggested Git Commit Boundaries
1. `setup: initialize monorepo git configurations`
2. `setup: create docker-compose for postgresql 16 database`
3. `setup: bootstrap spring boot backend Maven environment`
4. `setup: bootstrap react frontend Vite environment with tailwind`

### Suggested GitHub Issues
- **Issue 1.1:** Setup project monorepo structure and Docker DB. (Points: 1)
- **Issue 1.2:** Initialize and configure Spring Boot Backend pom dependencies. (Points: 2)
- **Issue 1.3:** Initialize React Vite Frontend with Tailwind CSS and base libraries. (Points: 2)
