package com.example.ragchat.model.entity;

import java.util.List;
import java.util.Map;

/**
 * RAG retrieved context shape: sources and optional metadata.
 */
public record RagContext(
        List<String> sources,
        Map<String, Object> metadata
) {
}
