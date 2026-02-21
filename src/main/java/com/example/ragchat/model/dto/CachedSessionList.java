package com.example.ragchat.model.dto;

import java.util.List;

/**
 * Serializable shape for session list cache (content + pagination info).
 */
public record CachedSessionList(
        List<SessionResponse> content,
        long totalElements,
        int totalPages,
        int size,
        int number
) {
}
