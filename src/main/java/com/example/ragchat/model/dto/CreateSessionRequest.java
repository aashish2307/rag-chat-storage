package com.example.ragchat.model.dto;

import jakarta.validation.constraints.Size;

public record CreateSessionRequest(
        @Size(max = 500, message = "Title must be at most 500 characters")
        String title
) {
    public String getTitleOrDefault() {
        return title != null && !title.isBlank() ? title.trim() : "New Chat";
    }
}
