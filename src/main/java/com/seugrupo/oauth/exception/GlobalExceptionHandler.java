package com.seugrupo.oauth.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Ponto único de tratamento de erro da API inteira. Ninguém deveria
 * precisar montar um ErrorResponse manualmente num controller - joguem a
 * exceção (OAuthApiException, ou deixem o WebClientResponseException
 * propagar) que ela cai aqui.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Exceção de negócio explícita, já com status/código definidos
    @ExceptionHandler(OAuthApiException.class)
    public ResponseEntity<ErrorResponse> handleOAuthApiException(OAuthApiException ex) {
        ErrorResponse body = new ErrorResponse(
                ex.getErrorCode(),
                ex.getMessage(),
                ex.getErrorSource(),
                List.of(ex.toString())
        );
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    // Erro vindo direto do WebClient que ninguém tratou/mapeou explicitamente ainda
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ErrorResponse> handleKeycloakError(WebClientResponseException ex) {
        OAuthApiException mapped = KeycloakErrorMapper.map(ex, "OAuthAPI");
        ErrorResponse body = new ErrorResponse(
                mapped.getErrorCode(),
                mapped.getMessage(),
                mapped.getErrorSource(),
                List.of(ex.getResponseBodyAsString())
        );
        return ResponseEntity.status(mapped.getStatus()).body(body);
    }

    // Falha de validação do @Valid nos DTOs (ex: email inválido, campo obrigatório faltando)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.toList());

        ErrorResponse body = new ErrorResponse(
                "OA-400",
                "Erro de validação nos dados enviados.",
                "OAuthAPI",
                errors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // Falha de validação em query params / path variables
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        ErrorResponse body = new ErrorResponse(
                "OA-400",
                ex.getMessage(),
                "OAuthAPI",
                List.of(ex.toString())
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // Rede de segurança: qualquer coisa não prevista cai aqui como 500,
    // em vez de vazar stacktrace cru pro cliente
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        ErrorResponse body = new ErrorResponse(
                "OA-500",
                "Erro interno não tratado.",
                "OAuthAPI",
                List.of(ex.toString())
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
