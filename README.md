# Conclave

Conclave is a multi-provider AI consensus workspace where multiple AI models (Google Gemini, OpenAI, Anthropic Claude) collaborate, debate, and refine plans in a live, real-time environment. It uses the Provider Adapter Pattern to standardize multi-vendor APIs, a custom Model Registry resolved at runtime, and WebSockets (STOMP) for dynamic turn broadcasting.

---

## 🛠️ Prerequisites

To run this project locally, ensure you have the following installed:
- **Java (JDK) 21** or higher
- **Node.js** (v18.x or higher) and **npm**
- **Docker** and **Docker Compose**
- **Maven** (optional, you can use the wrapper if provided, or direct `mvn` command)

---

## 🚀 Quickstart Guide

Follow these steps to get the local workspace running.

### 1. Database Setup (Infrastructure)
Spin up the local PostgreSQL 16 database using Docker Compose:
```bash
docker compose up -d
```
This provisions a database on port `5432` with:
- Database: `conclave_db`
- Username: `conclave_user`
- Password: `conclave_password`

### 2. Backend Orchestration (Spring Boot)
1. Navigate to the `backend/` directory.
2. Inject your API keys using the `.env` template:
   - Copy `.env.example` in the root to `.env` and fill out your GCP Project ID and Location.
3. Build and run the Spring Boot application:
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```
   The backend will bootstrap and start a server listening on port `8080`.

### 3. Frontend Client (React + Vite)
1. Navigate to the `frontend/` directory.
2. Install the node packages:
   ```bash
   npm install
   ```
3. Launch the Vite local dev server:
   ```bash
   npm run dev
   ```
   Open your browser and navigate to `http://localhost:5173/` to see the Conclave dashboard.

---

## 📂 Project Architecture

```
Conclave/
├── docker-compose.yml                # Spins up PostgreSQL 16
├── .gitignore                         # Monorepo git exclusions
├── .env.example                       # API key template for Gemini Vertex
├── backend/                           # Spring Boot Maven backend
│   ├── pom.xml                        # Maven project descriptor
│   └── src/
└── frontend/                          # React + Vite frontend
    ├── package.json                   # Client configurations
    ├── tailwind.config.js             # Tailwind CSS styling config
    └── src/                           # Frontend React components
```

For detailed guides, troubleshooting tips, and deeper architecture reviews, see:
- [01_Developer_Environment_Setup.md](file:///d:/Coding/Projects----For%20Resume/Conclave/Docs/Learning/01_Developer_Environment_Setup.md)
