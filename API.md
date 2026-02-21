# API Design — RAG Chat Storage

**Base path:** `/api/v1`  
**Versioning:** Path-based; future versions can use `/api/v2`.

---

## Headers

| Header     | Required | Description |
|-----------|----------|-------------|
| `X-User-Id` | Yes      | Identifies the user. All sessions and messages are scoped by this value. (Phase 3: may be derived from API key.) |

---

## Sessions

### Create session

**`POST /api/v1/sessions`**

**Request body:**

```json
{
  "title": "My Chat"
}
```

| Field  | Type   | Required | Description |
|--------|--------|----------|-------------|
| `title` | string | No       | Session title. Default: `"New Chat"`. Max 500 characters. |

**Response:** `201 Created`

```json
{
  "id": "uuid",
  "title": "My Chat",
  "favorite": false,
  "createdAt": "2025-02-21T10:00:00Z",
  "updatedAt": "2025-02-21T10:00:00Z"
}
```

---

### List sessions

**`GET /api/v1/sessions`**

**Query parameters:**

| Parameter  | Type    | Required | Description |
|------------|---------|----------|-------------|
| `favorite` | boolean | No       | If `true`, only favorite sessions. If `false`, only non-favorite. Omit for all. |
| `page`     | int     | No       | Zero-based page index. Default: `0`. |
| `size`     | int     | No       | Page size. Default: `20`. |

**Response:** `200 OK`

```json
{
  "content": [
    {
      "id": "uuid",
      "title": "My Chat",
      "favorite": false,
      "createdAt": "2025-02-21T10:00:00Z",
      "updatedAt": "2025-02-21T10:00:00Z"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0,
  "first": true,
  "last": true
}
```

---

### Get session

**`GET /api/v1/sessions/{id}`**

**Path:** `id` — session UUID.

**Response:** `200 OK` — same shape as a single session in the list.  
**Error:** `404 Not Found` if the session does not exist or does not belong to the user.

---

### Update session (partial)

**`PATCH /api/v1/sessions/{id}`**

**Request body:** All fields optional.

```json
{
  "title": "Renamed Chat",
  "isFavorite": true
}
```

| Field       | Type    | Required | Description |
|-------------|---------|----------|-------------|
| `title`     | string  | No       | New title. Max 500 characters. |
| `isFavorite` | boolean | No       | Mark or unmark as favorite. |

**Response:** `200 OK` — full session object.  
**Error:** `404 Not Found` if the session does not exist or does not belong to the user.

---

### Delete session

**`DELETE /api/v1/sessions/{id}`**

Deletes the session and all its messages (cascade).

**Response:** `204 No Content`  
**Error:** `404 Not Found` if the session does not exist or does not belong to the user.

---

## Messages

### Add message

**`POST /api/v1/sessions/{sessionId}/messages`**

**Path:** `sessionId` — session UUID.

**Request body:**

```json
{
  "sender": "user",
  "content": "Hello",
  "context": {
    "sources": ["doc1", "doc2"],
    "metadata": {}
  }
}
```

| Field    | Type   | Required | Description |
|----------|--------|----------|-------------|
| `sender`  | string | Yes      | `"user"` or `"assistant"`. |
| `content` | string | Yes      | Message text. Cannot be blank. |
| `context` | object | No       | RAG context. Suggested shape: `{ "sources": string[], "metadata": object }`. |

**Response:** `201 Created`

```json
{
  "id": "uuid",
  "sender": "user",
  "content": "Hello",
  "context": { "sources": ["doc1"], "metadata": {} },
  "createdAt": "2025-02-21T10:00:00Z"
}
```

**Error:** `404 Not Found` if the session does not exist or does not belong to the user.

---

### List messages (message history)

**`GET /api/v1/sessions/{sessionId}/messages`**

**Path:** `sessionId` — session UUID.

**Query parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `page`    | int  | No       | Zero-based page index. Default: `0`. |
| `size`    | int  | No       | Page size. Default: `20`. |

**Response:** `200 OK`

```json
{
  "content": [
    {
      "id": "uuid",
      "sender": "user",
      "content": "Hello",
      "context": null,
      "createdAt": "2025-02-21T10:00:00Z"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0,
  "first": true,
  "last": true
}
```

**Error:** `404 Not Found` if the session does not exist or does not belong to the user.

---

## Health (Spring Actuator)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/actuator/health` | Liveness. |
| GET | `/actuator/health/readiness` | Readiness (DB, Redis). |

No authentication required. Only these actuator endpoints are exposed in production.

---

## Error response format

All API errors use this shape:

```json
{
  "timestamp": "2025-02-21T10:00:00.000Z",
  "status": 404,
  "error": "Not Found",
  "message": "Session not found: <uuid>",
  "path": "/api/v1/sessions/<id>"
}
```

| Status | Typical cause |
|--------|----------------|
| 400 | Validation failed (e.g. missing/invalid body or query). |
| 404 | Session or message not found or not owned by the user. |
| 500 | Unexpected server error. |

---

## Pagination response shape

List endpoints return a `PageResponse<T>`:

| Field          | Type     | Description |
|----------------|----------|-------------|
| `content`       | array    | Items for the current page. |
| `totalElements` | number   | Total number of items. |
| `totalPages`    | number   | Total number of pages. |
| `size`          | number   | Requested page size. |
| `number`        | number   | Current page index (0-based). |
| `first`         | boolean  | `true` if this is the first page. |
| `last`          | boolean  | `true` if this is the last page. |
