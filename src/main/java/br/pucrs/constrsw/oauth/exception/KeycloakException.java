package br.pucrs.constrsw.oauth.exception;

import org.springframework.http.HttpStatus;

public class KeycloakException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus status;

    public KeycloakException(String errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public KeycloakException(String errorCode, String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
