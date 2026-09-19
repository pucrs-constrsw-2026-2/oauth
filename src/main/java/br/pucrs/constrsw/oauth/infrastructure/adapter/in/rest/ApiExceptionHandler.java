package br.pucrs.constrsw.oauth.infrastructure.adapter.in.rest;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import br.pucrs.constrsw.oauth.domain.exception.AccessDeniedException;
import br.pucrs.constrsw.oauth.domain.exception.AuthorizationRequiredException;
import br.pucrs.constrsw.oauth.domain.exception.IdentityProviderUnavailableException;
import br.pucrs.constrsw.oauth.domain.exception.InvalidCredentialsException;
import br.pucrs.constrsw.oauth.domain.exception.InvalidEmailException;
import br.pucrs.constrsw.oauth.domain.exception.InvalidInputException;
import br.pucrs.constrsw.oauth.domain.exception.RoleAlreadyExistsException;
import br.pucrs.constrsw.oauth.domain.exception.RoleNotFoundException;
import br.pucrs.constrsw.oauth.domain.exception.UserAlreadyExistsException;
import br.pucrs.constrsw.oauth.domain.exception.UserNotFoundException;
import br.pucrs.constrsw.oauth.infrastructure.adapter.in.rest.dto.ErrorResponseDto;
import br.pucrs.constrsw.oauth.infrastructure.adapter.in.rest.dto.ErrorStackEntryDto;

/**
 * Tradutor central de excecoes em respostas HTTP. Domain exceptions viram
 * status code + envelope padronizado do enunciado (error_code /
 * error_description / error_source / error_stack).
 *
 * O source padrao e "OAuthAPI". Quando a excecao veio do provedor externo,
 * o adapter (KeycloakAuthGateway/KeycloakUserGateway) inclui isso na origem
 * lancando uma excecao de dominio com mensagem descritiva.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final String SOURCE = "OAuthAPI";

    // -------------------------- Domain exceptions --------------------------

    @ExceptionHandler(InvalidEmailException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidEmail(InvalidEmailException ex) {
        return build(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidInput(InvalidInputException ex) {
        return build(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidCredentials(InvalidCredentialsException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex);
    }

    @ExceptionHandler(AuthorizationRequiredException.class)
    public ResponseEntity<ErrorResponseDto> handleAuthorization(AuthorizationRequiredException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, ex);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNotFound(UserNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDto> handleConflict(UserAlreadyExistsException ex) {
        return build(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleRoleNotFound(RoleNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(RoleAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDto> handleRoleConflict(RoleAlreadyExistsException ex) {
        return build(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(IdentityProviderUnavailableException.class)
    public ResponseEntity<ErrorResponseDto> handleUpstream(IdentityProviderUnavailableException ex) {
        log.error("Identity provider unavailable", ex);
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex);
    }

    // -------------------------- Framework exceptions --------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleBeanValidation(MethodArgumentNotValidException ex) {
        List<ErrorStackEntryDto> stack = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorStackEntryDto(fe.getField(), fe.getDefaultMessage()))
                .toList();
        String description = stack.stream()
                .map(e -> e.getType() + ": " + e.getMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Invalid request body");
        return buildWithStack(HttpStatus.BAD_REQUEST, description, stack);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            HttpMediaTypeNotSupportedException.class,
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class,
            MethodArgumentTypeMismatchException.class,
            HttpRequestMethodNotSupportedException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ErrorResponseDto> handleBadRequest(Exception ex) {
        return build(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleAny(Exception ex) {
        log.error("Unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ex);
    }

    // -------------------------- Helpers --------------------------

    private ResponseEntity<ErrorResponseDto> build(HttpStatus status, Throwable cause) {
        String description = cause.getMessage() != null
                ? cause.getMessage()
                : status.getReasonPhrase();
        List<ErrorStackEntryDto> stack = List.of(
                new ErrorStackEntryDto(cause.getClass().getSimpleName(), description));
        return buildWithStack(status, description, stack);
    }

    private ResponseEntity<ErrorResponseDto> buildWithStack(HttpStatus status, String description,
                                                            List<ErrorStackEntryDto> stack) {
        ErrorResponseDto body = new ErrorResponseDto(
                String.valueOf(status.value()), description, SOURCE, stack);
        return ResponseEntity.status(status).body(body);
    }
}
