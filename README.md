# RAG Chat Storage Microservice

Backend microservice to store chat histories for RAG-based chatbot systems.

## Prerequisites

- **Java 17+** (21 preferred)
- **Maven 3.8+**
- **PostgreSQL 15+**
- **Redis 7+**
- **Docker** (optional, for local setup)

## Quick Start

See **[RUN.md](RUN.md)** for detailed run options.

**Fastest (no Docker/PostgreSQL/Redis):**

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Uses H2 in-memory and no Redis. API key: `local-dev-key`. Then open http://localhost:8080/swagger-ui.html.

**With full stack (Docker):**

```bash
docker compose up -d --build
```

**With local PostgreSQL + Redis:**

```bash
cp .env.example .env   # edit as needed
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### 4. Health check

```bash
curl http://localhost:8080/actuator/health
```

### 5. API docs (Swagger UI)

When the app is running: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html).  
OpenAPI JSON: `http://localhost:8080/v3/api-docs`.  
All `/api/v1/**` endpoints require header `X-API-Key` (and `X-User-Id` for user scope).

## API

See **[API.md](API.md)** for the full API design: endpoints, request/response bodies, headers (`X-User-Id`), pagination, and error format.

## Configuration

See `.env.example` for all environment variables. Main settings:

| Variable | Description | Default |
|----------|-------------|---------|
| `PORT` | Server port | 8080 |
| `DATABASE_URL` | PostgreSQL JDBC URL | jdbc:postgresql://localhost:5432/rag_chat |
| `REDIS_HOST` | Redis host | localhost |
| `API_KEY` | API key for auth (Phase 3) | your-secret-api-key |

## Project Structure

```
src/main/java/com/example/ragchat/
├── config/          # Security, filters, config
├── controller/      # REST controllers (Phase 2)
├── service/         # Business logic (Phase 2)
├── repository/      # JPA repositories (Phase 2)
├── model/entity/    # JPA entities (Phase 2)
├── model/dto/       # Request/response DTOs (Phase 2)
├── security/        # API key auth (Phase 3)
└── exception/       # Global exception handler
```

## Redis usage

- **Rate limiting:** Fixed-window limiter in Redis (key: `ratelimit:user:{X-User-Id}` or `ratelimit:ip:{IP}`). Config: `app.rate-limit.max-requests`, `app.rate-limit.window-seconds`. Responds with `429 Too Many Requests` when exceeded. Health endpoints are not limited.
- **Session list cache:** Per-user session list is cached in Redis (key pattern `sessionList::{userId}::*`) with TTL from `app.cache.session-list-ttl-minutes` (default 10). Cache is invalidated on create/update/delete session for that user.

When Redis is unavailable (e.g. test profile), rate limiting and session cache are disabled.

## Tests

```bash
./mvnw test
```

Uses H2 in-memory DB and disabled Redis for tests.
