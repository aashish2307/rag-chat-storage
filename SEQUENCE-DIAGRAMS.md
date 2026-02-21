# RAG Chat Storage — Sequence Diagrams

Mermaid sequence diagrams for the main API flows. They render in GitHub, GitLab, VS Code (with a Mermaid extension), and many Markdown viewers.

---

## 1. Request pipeline (filters → controller)

Every `/api/v1/*` request passes through filters, then the controller.

```mermaid
sequenceDiagram
    participant Client
    participant RequestIdFilter
    participant RateLimitFilter
    participant ApiKeyAuthFilter
    participant Controller
    participant GlobalExceptionHandler

    Client->>RequestIdFilter: HTTP request
    RequestIdFilter->>RequestIdFilter: Set requestId in MDC / X-Request-Id
    RequestIdFilter->>RateLimitFilter: chain.doFilter()
    RateLimitFilter->>RateLimitFilter: Check Redis rate limit (if Redis)
    alt Rate limit exceeded
        RateLimitFilter-->>Client: 429 Too Many Requests
    else OK
        RateLimitFilter->>ApiKeyAuthFilter: chain.doFilter()
        ApiKeyAuthFilter->>ApiKeyAuthFilter: Validate X-API-Key
        alt Invalid/missing API key
            ApiKeyAuthFilter-->>Client: 401 Unauthorized
        else Authenticated
            ApiKeyAuthFilter->>Controller: chain.doFilter() → dispatch
            Controller->>Controller: Call service
            alt Exception
                Controller->>GlobalExceptionHandler: handle exception
                GlobalExceptionHandler-->>Client: 4xx/5xx + ErrorResponse JSON
            else Success
                Controller-->>Client: 2xx + response body
            end
        end
    end
```

---

## 2. Create session (POST /api/v1/sessions)

```mermaid
sequenceDiagram
    participant Client
    participant SessionController
    participant SessionService
    participant SessionRepository
    participant SessionListCacheEvictor
    participant DB

    Client->>SessionController: POST /api/v1/sessions (X-User-Id, body)
    SessionController->>SessionService: create(userId, request)
    SessionService->>SessionService: new Session(), set title, userId, favorite=false
    SessionService->>SessionRepository: save(session)
    SessionRepository->>DB: INSERT sessions
    DB-->>SessionRepository: OK
    SessionRepository-->>SessionService: session
    SessionService->>SessionListCacheEvictor: evictForUser(userId)
    SessionListCacheEvictor->>SessionListCacheEvictor: Delete Redis keys sessions::userId::*
    SessionService->>SessionService: toResponse(session)
    SessionService-->>SessionController: SessionResponse
    SessionController-->>Client: 201 Created, SessionResponse
```

---

## 3. List sessions (GET /api/v1/sessions) — cache miss then hit

```mermaid
sequenceDiagram
    participant Client
    participant SessionController
    participant SessionService
    participant SessionListCache
    participant SessionRepository
    participant DB

    Client->>SessionController: GET /api/v1/sessions?page=0&size=20 (X-User-Id)
    SessionController->>SessionService: listByUser(userId, favorite, pageable)

    Note over SessionService,SessionListCache: Cache miss
    SessionService->>SessionListCache: get(cacheKey)
    SessionListCache-->>SessionService: null
    SessionService->>SessionRepository: findByUserId(userId, pageable)
    SessionRepository->>DB: SELECT sessions ... LIMIT/OFFSET
    DB-->>SessionRepository: rows
    SessionRepository-->>SessionService: Page<Session>
    SessionService->>SessionService: map to SessionResponse list
    SessionService->>SessionListCache: put(cacheKey, CachedSessionList)
    SessionService-->>SessionController: Page<SessionResponse>
    SessionController-->>Client: 200, PageResponse

    Client->>SessionController: GET /api/v1/sessions?page=0&size=20 (same user)
    SessionController->>SessionService: listByUser(userId, favorite, pageable)
    Note over SessionService,SessionListCache: Cache hit
    SessionService->>SessionListCache: get(cacheKey)
    SessionListCache-->>SessionService: CachedSessionList
    SessionService->>SessionService: new PageImpl(cached.content(), ...)
    SessionService-->>SessionController: Page<SessionResponse>
    SessionController-->>Client: 200, PageResponse
```

---

## 4. Get session by ID (GET /api/v1/sessions/{id})

```mermaid
sequenceDiagram
    participant Client
    participant SessionController
    participant SessionService
    participant SessionRepository
    participant DB
    participant GlobalExceptionHandler

    Client->>SessionController: GET /api/v1/sessions/{id} (X-User-Id)
    SessionController->>SessionService: getById(userId, id)
    SessionService->>SessionRepository: findByIdAndUserId(id, userId)
    SessionRepository->>DB: SELECT * FROM sessions WHERE id=? AND user_id=?
    alt Not found
        DB-->>SessionRepository: empty
        SessionRepository-->>SessionService: Optional.empty()
        SessionService->>SessionService: orElseThrow(ResourceNotFoundException)
        SessionService->>GlobalExceptionHandler: throw ResourceNotFoundException
        GlobalExceptionHandler-->>Client: 404, ErrorResponse
    else Found
        DB-->>SessionRepository: row
        SessionRepository-->>SessionService: Optional(session)
        SessionService->>SessionService: toResponse(session)
        SessionService-->>SessionController: SessionResponse
        SessionController-->>Client: 200, SessionResponse
    end
```

---

## 5. Update session (PATCH /api/v1/sessions/{id})

```mermaid
sequenceDiagram
    participant Client
    participant SessionController
    participant SessionService
    participant SessionRepository
    participant SessionListCacheEvictor
    participant DB

    Client->>SessionController: PATCH /api/v1/sessions/{id} (body: title, isFavorite)
    SessionController->>SessionService: update(userId, id, request)
    SessionService->>SessionRepository: findByIdAndUserId(id, userId)
    SessionRepository->>DB: SELECT
    DB-->>SessionRepository: session (or empty)
    alt Not found
        SessionRepository-->>SessionService: Optional.empty()
        SessionService->>SessionService: orElseThrow(ResourceNotFoundException)
    else Found
        SessionRepository-->>SessionService: Optional(session)
        SessionService->>SessionService: set title / favorite if present
        SessionService->>SessionRepository: save(session)
        SessionRepository->>DB: UPDATE sessions
        DB-->>SessionRepository: OK
        SessionService->>SessionListCacheEvictor: evictForUser(userId)
        SessionService->>SessionService: toResponse(session)
        SessionService-->>SessionController: SessionResponse
        SessionController-->>Client: 200, SessionResponse
    end
```

---

## 6. Delete session (DELETE /api/v1/sessions/{id})

```mermaid
sequenceDiagram
    participant Client
    participant SessionController
    participant SessionService
    participant SessionRepository
    participant SessionListCacheEvictor
    participant DB

    Client->>SessionController: DELETE /api/v1/sessions/{id}
    SessionController->>SessionService: delete(userId, id)
    SessionService->>SessionRepository: existsByIdAndUserId(id, userId)
    SessionRepository->>DB: SELECT EXISTS ...
    alt Not found
        DB-->>SessionRepository: false
        SessionRepository-->>SessionService: false
        SessionService->>SessionService: throw ResourceNotFoundException
    else Exists
        DB-->>SessionRepository: true
        SessionRepository-->>SessionService: true
        SessionService->>SessionRepository: deleteById(id)
        SessionRepository->>DB: DELETE sessions (CASCADE messages)
        SessionService->>SessionListCacheEvictor: evictForUser(userId)
        SessionService-->>SessionController: void
        SessionController-->>Client: 204 No Content
    end
```

---

## 7. Add message (POST /api/v1/sessions/{sessionId}/messages)

```mermaid
sequenceDiagram
    participant Client
    participant MessageController
    participant MessageService
    participant SessionRepository
    participant MessageRepository
    participant DB

    Client->>MessageController: POST .../sessions/{sessionId}/messages (body: sender, content, context)
    MessageController->>MessageService: add(userId, sessionId, request)
    MessageService->>SessionRepository: findByIdAndUserId(sessionId, userId)
    SessionRepository->>DB: SELECT session
    alt Session not found
        DB-->>SessionRepository: empty
        MessageService->>MessageService: orElseThrow(ResourceNotFoundException)
    else Found
        DB-->>SessionRepository: session
        SessionRepository-->>MessageService: Optional(session)
        MessageService->>MessageService: new Message(), set session, sender, content, context
        MessageService->>MessageRepository: save(message)
        MessageRepository->>DB: INSERT messages
        DB-->>MessageRepository: message
        MessageRepository-->>MessageService: message
        MessageService->>MessageService: toResponse(message)
        MessageService-->>MessageController: MessageResponse
        MessageController-->>Client: 201 Created, MessageResponse
    end
```

---

## 8. List messages (GET /api/v1/sessions/{sessionId}/messages)

```mermaid
sequenceDiagram
    participant Client
    participant MessageController
    participant MessageService
    participant SessionRepository
    participant MessageRepository
    participant DB

    Client->>MessageController: GET .../sessions/{sessionId}/messages?page=0&size=20
    MessageController->>MessageService: getBySession(userId, sessionId, pageable)
    MessageService->>SessionRepository: existsByIdAndUserId(sessionId, userId)
    SessionRepository->>DB: SELECT EXISTS sessions
    alt Session not found
        DB-->>SessionRepository: false
        MessageService->>MessageService: throw ResourceNotFoundException
    else Exists
        DB-->>SessionRepository: true
        MessageService->>MessageRepository: findBySessionIdAndSessionUserId(sessionId, userId, pageable)
        MessageRepository->>DB: SELECT messages ... LIMIT/OFFSET
        DB-->>MessageRepository: Page<Message>
        MessageRepository-->>MessageService: Page<Message>
        MessageService->>MessageService: map to MessageResponse
        MessageService-->>MessageController: Page<MessageResponse>
        MessageController-->>Client: 200, PageResponse
    end
```

---

## 9. Validation error (e.g. invalid request body)

```mermaid
sequenceDiagram
    participant Client
    participant SessionController
    participant GlobalExceptionHandler

    Client->>SessionController: POST /api/v1/sessions (invalid body / @Valid fails)
    SessionController->>SessionController: create(...)
    Note over SessionController: MethodArgumentNotValidException thrown before method body
    SessionController->>GlobalExceptionHandler: handleValidation(ex)
    GlobalExceptionHandler->>GlobalExceptionHandler: Collect field errors, build ErrorResponse
    GlobalExceptionHandler-->>Client: 400 Bad Request, ErrorResponse JSON
```

---

## How to view

- **GitHub / GitLab:** Open this file in the repo; Mermaid blocks render as diagrams.
- **VS Code:** Install "Markdown Preview Mermaid Support" or "Mermaid" extension, then preview the `.md` file.
- **Online:** Paste a diagram block into [mermaid.live](https://mermaid.live) to edit or export as PNG/SVG.
