package br.pucrs.constrsw.oauth.error;

import br.pucrs.constrsw.oauth.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_SOURCE = "OAuthAPI";

    @ExceptionHandler({InvalidLoginRequestException.class, MissingServletRequestParameterException.class})
    ResponseEntity<ApiErrorResponse> handleBadRequest(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidCredentials(InvalidCredentialsException exception) {
        return error(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException exception) {
        return error(HttpStatus.BAD_REQUEST, "Content-Type must be multipart/form-data");
    }

    @ExceptionHandler(KeycloakCommunicationException.class)
    ResponseEntity<ApiErrorResponse> handleKeycloakFailure(KeycloakCommunicationException exception) {
        return error(HttpStatus.BAD_GATEWAY, exception.getMessage());
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String description) {
        ApiErrorResponse body = new ApiErrorResponse(
                String.valueOf(status.value()),
                description,
                ERROR_SOURCE,
                List.of());
        return ResponseEntity.status(status).body(body);
    }
}
