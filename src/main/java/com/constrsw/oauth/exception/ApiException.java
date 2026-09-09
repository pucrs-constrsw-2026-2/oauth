package com.constrsw.oauth.exception;

import org.springframework.http.HttpStatus;

/**
 * Excecao raiz da API. Carrega o HTTP status que o handler global deve emitir
 * mais um error code opcional (para diagnostico no body).
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public ApiException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() { return status; }
    public String getErrorCode() { return errorCode; }
}
