package com.example.ragchat.model.dto;

import com.example.ragchat.model.entity.MessageSender;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddMessageRequest(
        @NotNull(message = "Sender is required")
        MessageSender sender,

        @NotBlank(message = "Content is required")
        String content,

        JsonNode context
) {
}
