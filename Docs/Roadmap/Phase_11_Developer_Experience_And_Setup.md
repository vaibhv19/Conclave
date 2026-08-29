# Phase 11 — Developer Experience & Environment Setup

## 1. Planning: Developer Experience & Environment Setup

### 1.1 Purpose
The purpose of this phase is to ensure that a new engineer can clone the repository and establish a fully verified local development environment on a fresh machine without relying on undocumented tribal knowledge. This phase focuses on dependency validation, configuration alignment, startup order testing, and workflow reproducibility.

### 1.2 Responsibilities & Scope
- **Environment Verification:** Confirm that all software dependencies (Java JDK 21, Maven, Node.js, PostgreSQL, Ollama) match required versions and boot correctly.
- **Configuration Review:** Validate development profile properties, map missing or obsolete environment variable placeholders, and review secret caching strategies.
- **Startup Diagnostics:** Trace execution lifecycles and verify port conflicts, database schema creations, and local model inferences.
- **Development Tooling:** Review manual setup steps, compile troubleshooting guidelines, and identify scripts to automate the local environment bootstrap.

---

## 2. Phase Components

### 2.1 Documentation Artifacts to Produce / Maintain
- [Development_Setup_Audit.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Development_Setup_Audit.md): Comprehensive system audit of required versions, database variables, and discrepancies.
- [Developer_Onboarding_Checklist.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Developer_Onboarding_Checklist.md): Step-by-step first-run checklist from clone to room execution.
- [Startup_Workflow.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Startup_Workflow.md): Sequence mapping database mounts, backend initialization, and frontend hot-reloading.
- [Environment_Configuration_Guide.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Environment_Configuration_Guide.md): Variables and profile settings mapping.

---

## 3. Atomic Implementation Tasks

### Task 11.1: Developer Environment Setup Audit & Validation
- **Estimated Size:** M
- **Risk:** Low
- **Prerequisites:** Phase 10 Completed
- **Definition of Done:**
  - Verify JDK 21 virtual threads concurrency compiles cleanly on a fresh build context.
  - Audit Node.js version requirements (ensure v18.x or higher) and document npm install behaviors.
  - Launch Postgres 16 container via Docker Compose and check port binding configurations.
  - Verify Ollama daemon presence on port 11434 and ensure `llama3`, `mistral`, and `gemma` models pull successfully.
  - Generate the initial [Development_Setup_Audit.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Development_Setup_Audit.md) report.

### Task 11.2: Environment Configuration Consistency Check
- **Estimated Size:** M
- **Risk:** Low
- **Prerequisites:** Task 11.1
- **Definition of Done:**
  - Analyze differences between the backend README env table and actual `@Value` mappings inside the Spring application.
  - Verify database credentials and Ollama URL parameters inside [application-dev.yml](file:///d:/Coding/Projects----For%20Resume/Conclave/backend/src/main/resources/application-dev.yml).
  - Draft the [Environment_Configuration_Guide.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Environment_Configuration_Guide.md) explaining relaxed binding and fallback keys.

### Task 11.3: Startup Flow & Port Drift Verification
- **Estimated Size:** S
- **Risk:** Low
- **Prerequisites:** Task 11.2
- **Definition of Done:**
  - Map and document the exact chronological boot order (PostgreSQL $\rightarrow$ Ollama $\rightarrow$ Spring Boot $\rightarrow$ React Vite) inside [Startup_Workflow.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Startup_Workflow.md).
  - Test port collision scenarios (e.g. starting Vite when port 5173 is occupied, causing port drift to 5174).
  - Verify that CORS errors occur due to port drift and document resolutions in the workflow guide.

### Task 11.4: First-Run Checklist & Build Validation
- **Estimated Size:** M
- **Risk:** Low
- **Prerequisites:** Task 11.3
- **Definition of Done:**
  - Create [Developer_Onboarding_Checklist.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Developer_Onboarding_Checklist.md) detailing commands for cloning, docker compose up, model pulls, backend runs, and frontend launches.
  - Verify that Maven test suites (`mvn test`) run successfully on the dev profile.
  - Confirm that frontend unit tests (`npm run test`) pass.

### Task 11.5: Setup Troubleshooting & Automation Review
- **Estimated Size:** S
- **Risk:** Low
- **Prerequisites:** Task 11.4
- **Definition of Done:**
  - Identify all steps that cannot currently be automated (downloading Ollama, pulling models, installing Playwright).
  - Document failure modes (e.g. database credentials mismatch, Ollama offline, missing models) and troubleshooting steps.
  - Create a list of future automation opportunities (bootstrap scripts, Maven dotenv plugins, Docker container health checks).

---

## 4. Documentation & Verification

### Documentation to Update / Create
- Generate [Development_Setup_Audit.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Development_Setup_Audit.md)
- Generate [Developer_Onboarding_Checklist.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Developer_Onboarding_Checklist.md)
- Generate [Startup_Workflow.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Startup_Workflow.md)
- Generate [Environment_Configuration_Guide.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Environment_Configuration_Guide.md)

### Testing Checkpoint
- Verify that a clean environment with no pre-existing configurations can compile the backend via Maven and boot the Vite server on the default port.
- Verify that Postgres tables are automatically generated by Hibernate on backend start.

### Suggested Git Commit Boundaries
- `docs: perform required software and service audits for local setup`
- `docs: audit environment variables and configuration consistency`
- `docs: document startup order, port collisions, and first-run checklists`
- `docs: review setup troubleshooting and compile automation opportunities`

### Suggested GitHub Issues
- **Issue 11.1:** Perform environment audit and configuration validation. (Points: 2)
- **Issue 11.2:** Map startup sequences and document port drift. (Points: 1)
- **Issue 11.3:** Compile first-run checklists and onboarding guides. (Points: 2)
- **Issue 11.4:** Generate troubleshooting and automation recommendations. (Points: 1)

### Suggested GitHub Milestones
- **Milestone 5:** Developer Experience & Environment Verification (Phase 11)
