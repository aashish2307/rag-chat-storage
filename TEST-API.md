# Step-by-step API testing guide

Use these in order. Replace `SESSION_ID` with the `id` you get from Step 1 (or Step 2).

**Base URL:** `http://localhost:8080`  
**Headers for all requests:** `X-API-Key: dev-api-key` and `X-User-Id: user1`

---

## Step 1: Create a session

**Endpoint:** `POST /api/v1/sessions`

**What it does:** Creates a new chat session. Copy the `id` from the response for later steps.

```bash
curl -X POST http://localhost:8080/api/v1/sessions \
  -H "X-API-Key: dev-api-key" \
  -H "X-User-Id: user1" \
  -H "Content-Type: application/json" \
  -d '{"title": "My first chat"}'
```

**Expected:** Status `201`, JSON with `id`, `title`, `favorite`, `createdAt`, `updatedAt`.  
**Copy:** the `id` value (e.g. `a1b2c3d4-e5f6-7890-abcd-ef1234567890`) → use it as `SESSION_ID` below.

---

## Step 2: List all sessions

**Endpoint:** `GET /api/v1/sessions`

**What it does:** Returns all sessions for the user (paginated). Pagination is optional (defaults: `page=0`, `size=20`).

```bash
curl "http://localhost:8080/api/v1/sessions?page=0&size=20" \
  -H "X-API-Key: dev-api-key" \
  -H "X-User-Id: user1"
```

**Optional query params:** `favorite` (boolean), `page` (0-based: first page is **0**, default 0), `size` (default 20), `sort` (e.g. `createdAt,desc` or `title,asc`—use a single string, not an array). You can omit `page`, `size`, and `sort`; the server uses defaults.

**Expected:** Status `200`, JSON with `content` (array of sessions), `totalElements`, `totalPages`, etc.

**If `content` is empty but `totalElements` > 0:** You are on a page index that has no items. Use **`page=0`** for the first page. For example, with 3 items and `size=10`, only `page=0` has data; `page=1` returns empty `content`.

---

## Step 3: Get one session by ID

**Endpoint:** `GET /api/v1/sessions/{id}`

**What it does:** Returns a single session. Use the `id` from Step 1.

```bash
curl "http://localhost:8080/api/v1/sessions/SESSION_ID" \
  -H "X-API-Key: dev-api-key" \
  -H "X-User-Id: user1"
```

Replace `SESSION_ID` with the actual UUID (e.g. `a1b2c3d4-e5f6-7890-abcd-ef1234567890`).

**Expected:** Status `200`, one session object.  
**If wrong ID or other user:** Status `404`.

---

## Step 4: Add a message to the session

**Endpoint:** `POST /api/v1/sessions/{sessionId}/messages`

**What it does:** Adds a message to the session. Use the same `SESSION_ID`.

```bash
curl -X POST "http://localhost:8080/api/v1/sessions/SESSION_ID/messages" \
  -H "X-API-Key: dev-api-key" \
  -H "X-User-Id: user1" \
  -H "Content-Type: application/json" \
  -d '{"sender": "user", "content": "What is RAG?", "context": null}'
```

**Expected:** Status `201`, JSON with `id`, `sender`, `content`, `context`, `createdAt`.

**Another message (assistant reply):**

```bash
curl -X POST "http://localhost:8080/api/v1/sessions/SESSION_ID/messages" \
  -H "X-API-Key: dev-api-key" \
  -H "X-User-Id: user1" \
  -H "Content-Type: application/json" \
  -d '{"sender": "assistant", "content": "RAG is Retrieval-Augmented Generation.", "context": {"sources": ["doc1"], "metadata": {}}}'
```

---

## Step 5: List messages (message history)

**Endpoint:** `GET /api/v1/sessions/{sessionId}/messages`

**What it does:** Returns messages for the session (paginated).

```bash
curl "http://localhost:8080/api/v1/sessions/SESSION_ID/messages?page=0&size=20" \
  -H "X-API-Key: dev-api-key" \
  -H "X-User-Id: user1"
```

**Expected:** Status `200`, JSON with `content` (array of messages), `totalElements`, `totalPages`, etc.

---

## Step 6: Update session (rename or mark favorite)

**Endpoint:** `PATCH /api/v1/sessions/{id}`

**What it does:** Partially updates the session (title and/or favorite). Use the same `SESSION_ID`.

**Rename only:**
```bash
curl -X PATCH "http://localhost:8080/api/v1/sessions/SESSION_ID" \
  -H "X-API-Key: dev-api-key" \
  -H "X-User-Id: user1" \
  -H "Content-Type: application/json" \
  -d '{"title": "Renamed chat"}'
```

**Mark as favorite:**
```bash
curl -X PATCH "http://localhost:8080/api/v1/sessions/SESSION_ID" \
  -H "X-API-Key: dev-api-key" \
  -H "X-User-Id: user1" \
  -H "Content-Type: application/json" \
  -d '{"isFavorite": true}'
```

**Both:**
```bash
curl -X PATCH "http://localhost:8080/api/v1/sessions/SESSION_ID" \
  -H "X-API-Key: dev-api-key" \
  -H "X-User-Id: user1" \
  -H "Content-Type: application/json" \
  -d '{"title": "My favorite chat", "isFavorite": true}'
```

**Expected:** Status `200`, full session object with updated fields.

---

## Step 7: List only favorite sessions

**Endpoint:** `GET /api/v1/sessions?favorite=true`

**What it does:** Returns only sessions marked as favorite (after Step 6).

```bash
curl "http://localhost:8080/api/v1/sessions?favorite=true" \
  -H "X-API-Key: dev-api-key" \
  -H "X-User-Id: user1"
```

**Expected:** Status `200`, `content` contains only favorite sessions.

---

## Step 8: Delete a session

**Endpoint:** `DELETE /api/v1/sessions/{id}`

**What it does:** Deletes the session and all its messages. Use the same `SESSION_ID`.

```bash
curl -X DELETE "http://localhost:8080/api/v1/sessions/SESSION_ID" \
  -H "X-API-Key: dev-api-key" \
  -H "X-User-Id: user1"
```

**Expected:** Status `204` (no body).  
**Then:** Step 2 or Step 3 with that ID should return `404`.

---

## Quick reference: endpoints in order

| Step | Method | Endpoint | Purpose |
|------|--------|----------|---------|
| 1 | POST | `/api/v1/sessions` | Create session → get `id` |
| 2 | GET | `/api/v1/sessions` | List sessions |
| 3 | GET | `/api/v1/sessions/{id}` | Get one session |
| 4 | POST | `/api/v1/sessions/{id}/messages` | Add message(s) |
| 5 | GET | `/api/v1/sessions/{id}/messages` | List messages |
| 6 | PATCH | `/api/v1/sessions/{id}` | Rename / favorite |
| 7 | GET | `/api/v1/sessions?favorite=true` | List favorites |
| 8 | DELETE | `/api/v1/sessions/{id}` | Delete session |

**Health (no auth):** `GET http://localhost:8080/actuator/health`

**Wrong API key:** Any `/api/v1/*` request without valid `X-API-Key` → `401 Unauthorized`.
