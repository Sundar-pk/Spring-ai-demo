package com.example.springaidemo.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Standard API response wrapper")
public record ApiResponse<T>(

        @Schema(description = "Demo module name")
        String demo,

        @Schema(description = "Response payload")
        T result,

        @Schema(description = "Python equivalent concept for reference")
        String pythonEquivalent
) {
    public static <T> ApiResponse<T> of(String demo, T result, String pythonEquivalent) {
        return new ApiResponse<>(demo, result, pythonEquivalent);
    }
}
