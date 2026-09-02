package com.seugrupo.oauth.exception;

import org.springframework.http.HttpStatus;

/**
 * Exceção base da API. Lancem essa (ou uma subclasse, se precisarem) nos
 * controllers/services em vez de RuntimeException genérica - o
 * GlobalExceptionHandler sabe como transformar isso no formato de erro
 * padronizado.
 */
public class OAuthApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;
    private final String errorSource;

    public OAuthApiException(HttpStatus status, String errorCode, String errorSource, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.errorSource = errorSource;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorSource() {
        return errorSource;
    }
}
