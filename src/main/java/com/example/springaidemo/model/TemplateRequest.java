package com.example.springaidemo.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request for prompt template demo")
public record TemplateRequest(

        @Schema(description = "Topic to explain", example = "vector databases")
        String topic,

        @Schema(description = "Target audience skill level", example = "senior Java developer")
        String audience,

        @Schema(description = "Desired response format", example = "bullet points")
        String format
) {}
