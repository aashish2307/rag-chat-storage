package com.example.ragchat.model.dto;

import java.time.Instant;
import java.util.UUID;

public record SessionResponse(
        UUID id,
        UUID sessionId,
        String title,
        boolean favorite,
        Instant createdAt,
        Instant updatedAt
) {
    /** Convenience constructor: sessionId is set from id. */
    public SessionResponse(UUID id, String title, boolean favorite, Instant createdAt, Instant updatedAt) {
        this(id, id, title, favorite, createdAt, updatedAt);
    }
}
