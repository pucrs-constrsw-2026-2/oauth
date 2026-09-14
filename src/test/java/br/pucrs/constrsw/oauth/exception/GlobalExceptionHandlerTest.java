package br.pucrs.constrsw.oauth.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Deveria formatar KeycloakException com status, error_code e error_stack")
    void testHandleKeycloakException() {
        KeycloakException ex = new KeycloakException("USER_ALREADY_EXISTS", "Usuário já existe", HttpStatus.CONFLICT);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleKeycloakException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("USER_ALREADY_EXISTS", response.getBody().getErrorCode());
        assertEquals("Usuário já existe", response.getBody().getMessage());
        assertEquals(409, response.getBody().getStatus());
        assertNotNull(response.getBody().getErrorStack());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    @DisplayName("Deveria formatar HttpClientErrorException.Conflict como USER_ALREADY_EXISTS")
    void testHandleHttpClientErrorExceptionConflict() {
        HttpClientErrorException ex = HttpClientErrorException.create(HttpStatus.CONFLICT, "Conflict", null, null, null);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleHttpClientErrorException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("USER_ALREADY_EXISTS", response.getBody().getErrorCode());
    }

    @Test
    @DisplayName("Deveria formatar IllegalArgumentException como INVALID_ARGUMENT")
    void testHandleIllegalArgumentException() {
        IllegalArgumentException ex = new IllegalArgumentException("Parâmetro inválido");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgumentException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INVALID_ARGUMENT", response.getBody().getErrorCode());
        assertEquals("Parâmetro inválido", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Deveria formatar exceção genérica como INTERNAL_SERVER_ERROR")
    void testHandleGenericException() {
        RuntimeException ex = new RuntimeException("Erro inesperado no sistema");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().getErrorCode());
        assertEquals("Erro inesperado no sistema", response.getBody().getMessage());
    }
}
