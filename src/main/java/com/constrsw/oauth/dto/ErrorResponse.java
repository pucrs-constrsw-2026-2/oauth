package com.constrsw.oauth.dto;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Representacao padrao de erros retornados pela API. Sempre serializada em
 * kebab-case (jackson.property-naming-strategy = KEBAB_CASE), o que produz
 * "error-code", "timestamp" etc.
 */
public class ErrorResponse {

    private String errorCode;
    private String message;
    private String path;
    private OffsetDateTime timestamp;

    public ErrorResponse() {}

    public ErrorResponse(String errorCode, String message, String path) {
        this.errorCode = errorCode;
        this.message = message;
        this.path = path;
        this.timestamp = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public OffsetDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(OffsetDateTime timestamp) { this.timestamp = timestamp; }
}
