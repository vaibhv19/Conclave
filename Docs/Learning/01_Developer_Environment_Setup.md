# Developer Environment Setup & Troubleshooting Guide

This guide describes local development setup, environment configurations, and troubleshooting steps for the Conclave workspace.

---

## 🛠️ Step-by-Step Local Environment Verification

### 1. Verification of PostgreSQL 16 Database
The database is containerized via Docker Compose.

**Checking container status:**
```bash
docker compose ps
```
The output should show:
```
NAME                IMAGE                STATUS         PORTS
conclave-postgres   postgres:16-alpine   Up             0.0.0.0:5432->5432/tcp
```

**Viewing database logs:**
```bash
docker compose logs postgres
```
Ensure you see the line: `database system is ready to accept connections`.

### 2. Verification of Spring Boot Backend
The backend requires JDK 21 to compile and run.

**Run a compilation check:**
```bash
cd backend
mvn clean install -DskipTests
```

**Run the automated context load and connection test:**
```bash
mvn test
```
This runs `BackendApplicationTests` which handshakes with the running PostgreSQL 16 database.

**Debugging Common Startup Issues:**
- **Error: `port 5432 is already allocated`:** A conflicting Postgres instance is running on your host machine. Run `docker stop <conflicting-container>` or stop the local postgres service.
- **Error: `GCP Project ID missing`:** Ensure that `application-dev.yml` contains dummy configuration values for `spring.ai.vertex.ai.gemini.project-id` or that your local `.env` has been set up from `.env.example`.

### 3. Verification of Vite React Frontend
The frontend requires Node.js v18+.

**Installing dependencies:**
```bash
cd frontend
npm install
```

**Building for production:**
```bash
npm run build
```

**Running the development server:**
```bash
npm run dev
```
Navigate to `http://localhost:5173/` in your browser.

**Debugging Common Frontend Issues:**
- **Error: `Tailwind styles not updating`:** Check that `tailwind.config.js` content array includes your template paths. Ensure `index.css` contains `@tailwind` directives.
- **Error: `port 5173 is already in use`:** Vite will automatically bind to the next available port (e.g. `5174`). You can specify the port in `vite.config.js` if strict port enforcement is needed.

---

## 🔒 Security Best Practices
- **API Keys:** Never commit `.env` or files containing raw API keys to Git. Ensure they match the exclusions defined in the root `.gitignore`.
- **Database Credentials:** The credentials used in development (`conclave_user` / `conclave_password`) are local-only credentials. In staging and production environments, inject credentials securely using environment variables or secret vaults.
