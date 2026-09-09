package com.constrsw.oauth.exception;

import org.springframework.http.HttpStatus;

public class KeycloakUnavailableException extends ApiException {
    public KeycloakUnavailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "KEYCLOAK_UNAVAILABLE", message);
    }
}
