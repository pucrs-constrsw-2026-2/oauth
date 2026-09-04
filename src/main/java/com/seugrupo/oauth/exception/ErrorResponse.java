package com.seugrupo.oauth.exception;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

/**
 * Formato de erro exigido pelo enunciado:
 * {
 *   "error_code": "OA-000",
 *   "error_description": "...",
 *   "error_source": "...",
 *   "error_stack": [...]
 * }
 *
 * @JsonNaming converte automaticamente camelCase -> snake_case na
 * serialização, então os campos aqui ficam em camelCase (convenção Java) e
 * saem em snake_case no JSON.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ErrorResponse(
        String errorCode,
        String errorDescription,
        String errorSource,
        List<ErrorStackEntry> errorStack
) {
    public record ErrorStackEntry(String message) {
    }
}
