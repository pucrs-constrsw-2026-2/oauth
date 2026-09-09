package com.seugrupo.oauth.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

import static com.seugrupo.oauth.exception.ErrorResponse.ErrorStackEntry;

/** Centraliza o contrato de erros obrigatório da API. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OAuthApiException.class)
    public ResponseEntity<ErrorResponse> handleOAuthApiException(OAuthApiException ex) {
        ErrorResponse body = new ErrorResponse(
                ex.getErrorCode(),
                ex.getMessage(),
                ex.getErrorSource(),
                List.of(new ErrorStackEntry(ex.toString()))
        );
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ErrorResponse> handleKeycloakError(WebClientResponseException ex) {
        OAuthApiException mapped = KeycloakErrorMapper.map(ex, "OAuthAPI");
        ErrorResponse body = new ErrorResponse(
                mapped.getErrorCode(),
                mapped.getMessage(),
                mapped.getErrorSource(),
                List.of(new ErrorStackEntry(mapped.toString()))
        );
        return ResponseEntity.status(mapped.getStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(MethodArgumentNotValidException ex) {
        return validationResponse(ex);
    }

    // @ModelAttribute usa BindException para falhas nos formulários de autenticação.
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindError(BindException ex) {
        return validationResponse(ex);
    }

    private ResponseEntity<ErrorResponse> validationResponse(BindException ex) {
        List<ErrorStackEntry> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ErrorStackEntry(
                        fieldError.getField() + ": " + fieldError.getDefaultMessage()))
                .toList();

        ErrorResponse body = new ErrorResponse(
                "OA-400",
                "Erro de validação nos dados enviados.",
                "OAuthAPI",
                errors
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ServletRequestBindingException.class)
    public ResponseEntity<ErrorResponse> handleMalformedForm(ServletRequestBindingException ex) {
        return malformedRequestResponse(ex);
    }

    @ExceptionHandler({
            HttpMediaTypeNotSupportedException.class,
            MultipartException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ErrorResponse> handleMalformedRequest(Exception ex) {
        return malformedRequestResponse(ex);
    }

    private ResponseEntity<ErrorResponse> malformedRequestResponse(Exception ex) {
        ErrorResponse body = new ErrorResponse(
                "OA-400",
                "Erro na estrutura dos dados enviados.",
                "OAuthAPI",
                List.of(new ErrorStackEntry(ex.getMessage()))
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        ErrorResponse body = new ErrorResponse(
                "OA-400",
                ex.getMessage(),
                "OAuthAPI",
                List.of(new ErrorStackEntry(ex.toString()))
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        ErrorResponse body = new ErrorResponse(
                "OA-500",
                "Erro interno não tratado.",
                "OAuthAPI",
                List.of(new ErrorStackEntry(ex.toString()))
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
