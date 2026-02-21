package com.example.ragchat.model.dto;

import com.example.ragchat.model.entity.MessageSender;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        MessageSender sender,
        String content,
        JsonNode context,
        Instant createdAt
) {
}
