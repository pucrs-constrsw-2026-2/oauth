package br.pucrs.constrsw.oauth.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(KeycloakException.class)
    public ResponseEntity<ErrorResponse> handleKeycloakException(KeycloakException ex) {
        log.error("KeycloakException [{}]: {}", ex.getErrorCode(), ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                ex.getErrorCode(),
                ex.getMessage(),
                getStackTraceAsString(ex),
                ex.getStatus().value()
        );
        return ResponseEntity.status(ex.getStatus()).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String validationErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.warn("Erro de validação de argumentos: {}", validationErrors);
        ErrorResponse errorResponse = new ErrorResponse(
                "VALIDATION_ERROR",
                validationErrors,
                getStackTraceAsString(ex),
                HttpStatus.BAD_REQUEST.value()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<ErrorResponse> handleHttpClientErrorException(HttpClientErrorException ex) {
        log.error("HttpClientErrorException: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
        String code = "KEYCLOAK_CLIENT_ERROR";
        if (ex.getStatusCode() == HttpStatus.CONFLICT) {
            code = "USER_ALREADY_EXISTS";
        } else if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED || ex.getStatusCode() == HttpStatus.BAD_REQUEST) {
            code = "INVALID_CREDENTIALS";
        } else if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
            code = "RESOURCE_NOT_FOUND";
        }

        ErrorResponse errorResponse = new ErrorResponse(
                code,
                ex.getStatusText() != null ? ex.getStatusText() : ex.getMessage(),
                getStackTraceAsString(ex),
                ex.getStatusCode().value()
        );
        return ResponseEntity.status(ex.getStatusCode()).body(errorResponse);
    }

    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<ErrorResponse> handleHttpServerErrorException(HttpServerErrorException ex) {
        log.error("HttpServerErrorException do Keycloak: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                "KEYCLOAK_SERVER_ERROR",
                "Erro interno no provedor de autenticação Keycloak",
                getStackTraceAsString(ex),
                ex.getStatusCode().value()
        );
        return ResponseEntity.status(ex.getStatusCode()).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("IllegalArgumentException: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                "INVALID_ARGUMENT",
                ex.getMessage(),
                getStackTraceAsString(ex),
                HttpStatus.BAD_REQUEST.value()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Erro interno não tratado: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = new ErrorResponse(
                "INTERNAL_SERVER_ERROR",
                ex.getMessage() != null ? ex.getMessage() : "Ocorreu um erro interno inesperado",
                getStackTraceAsString(ex),
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    private String getStackTraceAsString(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
}
