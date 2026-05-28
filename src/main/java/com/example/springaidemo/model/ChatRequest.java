package com.example.springaidemo.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Chat request payload")
public record ChatRequest(

        @Schema(description = "The user's message", example = "Explain Spring AI in 2 sentences")
        String message,

        @Schema(description = "Optional conversation ID for multi-turn chat", example = "session-abc-123")
        String conversationId
) {
    public ChatRequest {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        // Default conversationId so memory works even without explicit session
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = "default";
        }
    }
}
