package br.pucrs.constrsw.oauth.exception;

import br.pucrs.constrsw.oauth.dto.ErrorResponse;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * Central place that turns every exception thrown anywhere in the API into the
 * error envelope required by the assignment: error_code / error_description /
 * error_source / error_stack. Every future controller (users, roles, ...) can
 * either throw {@link OAuthApiException} directly or let Spring's own
 * validation exceptions bubble up here.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String DEFAULT_SOURCE = "OAuthAPI";

    @ExceptionHandler(OAuthApiException.class)
    public ResponseEntity<ErrorResponse> handleOAuthApiException(OAuthApiException ex) {
        log.warn("OAuthApiException: code={} description={}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus())
                .body(new ErrorResponse(ex.getErrorCode(), ex.getMessage(), ex.getErrorSource(), ex.getErrorStack()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class})
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.toList());
        return build(HttpStatus.BAD_REQUEST, "Erro na estrutura do request body.", details);
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingServletRequestPartException.class})
    public ResponseEntity<ErrorResponse> handleMissingParam(Exception ex) {
        return build(HttpStatus.BAD_REQUEST, "Parametro obrigatorio ausente: " + ex.getMessage(), List.of(ex.toString()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "Corpo da requisicao ausente ou malformado.", List.of(ex.getMostSpecificCause().toString()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of(ex.toString()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado na OAuth API.", List.of(ex.toString()));
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String description, List<String> stack) {
        ErrorResponse body = new ErrorResponse(String.valueOf(status.value()), description, DEFAULT_SOURCE, stack);
        return ResponseEntity.status(status).body(body);
    }
}
