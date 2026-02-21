# How to Run RAG Chat Storage

Pick one of the options below.

---

## Option 1: Run locally (no Docker, no PostgreSQL/Redis)

Uses **H2 in-memory** database and **no Redis** (rate limiting and session cache disabled). Easiest for a quick run.

```bash
cd rag-chat-storage
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

- **API:** http://localhost:8080  
- **Health:** http://localhost:8080/actuator/health  
- **Swagger UI:** http://localhost:8080/swagger-ui.html  
- **API key:** `local-dev-key` (or set `API_KEY` env var)  
- **Headers:** `X-API-Key: local-dev-key`, `X-User-Id: any-user-id`

Example:

```bash
curl -s -X POST http://localhost:8080/api/v1/sessions \
  -H "X-API-Key: local-dev-key" \
  -H "X-User-Id: user1" \
  -H "Content-Type: application/json" \
  -d '{"title":"My Chat"}' | jq
```

---

## Option 2: Run with Docker Compose (recommended for full stack)

**One command (starts Docker Desktop if needed on Mac, then starts all services):**

```bash
cd rag-chat-storage
./start.sh
```

Or manually, with Docker already running:

```bash
cd rag-chat-storage
docker compose up -d --build
```

- **App:** http://localhost:8080  
- **Adminer (DB UI):** http://localhost:8081 — System: PostgreSQL, Server: postgres, User: raguser, Password: ragpass, Database: rag_chat  
- **API key:** `dev-api-key` (or set `API_KEY` before `docker compose up`)

Wait ~30s for the app to pass its healthcheck, then:

```bash
curl http://localhost:8080/actuator/health
```

---

## Option 3: Run with local PostgreSQL and Redis

1. Start **PostgreSQL** (port 5432) and **Redis** (port 6379) on your machine.  
2. Create database: `createdb rag_chat` (or via your PostgreSQL tool).  
3. Set env (or use defaults in `.env`):

   ```bash
   export DATABASE_URL=jdbc:postgresql://localhost:5432/rag_chat
   export DB_USERNAME=your_user
   export DB_PASSWORD=your_password
   export REDIS_HOST=localhost
   export API_KEY=your-secret-api-key
   ```

4. Run:

   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

Flyway will run migrations on first start.

---

**Step-by-step API testing:** See **[TEST-API.md](TEST-API.md)** for copy-paste curl commands in order (create session → add messages → list → update → delete).

---

## Quick checks

| Check              | Command |
|--------------------|---------|
| Health             | `curl http://localhost:8080/actuator/health` |
| Create session     | `curl -X POST http://localhost:8080/api/v1/sessions -H "X-API-Key: local-dev-key" -H "X-User-Id: u1" -H "Content-Type: application/json" -d '{}'` |
| List sessions      | `curl "http://localhost:8080/api/v1/sessions" -H "X-API-Key: local-dev-key" -H "X-User-Id: u1"` |
