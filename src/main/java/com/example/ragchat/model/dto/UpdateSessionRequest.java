package com.example.ragchat.model.dto;

import jakarta.validation.constraints.Size;

public record UpdateSessionRequest(
        @Size(max = 500, message = "Title must be at most 500 characters")
        String title,

        Boolean isFavorite
) {
}
