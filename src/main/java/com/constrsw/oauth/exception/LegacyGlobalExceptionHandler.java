package com.constrsw.oauth.exception;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import br.pucrs.constrsw.oauth.dto.ErrorResponse;
import br.pucrs.constrsw.oauth.dto.ErrorStackEntry;

/**
 * Traducao centralizada de excecoes dos controllers do package
 * com.constrsw.oauth em respostas HTTP + body ErrorResponse no formato exigido
 * pelo enunciado do trabalho: error_code / error_description / error_source /
 * error_stack.
 *
 * Escopado a basePackages="com.constrsw.oauth" para nao conflitar com o
 * GlobalExceptionHandler do package br.pucrs.constrsw.oauth (merge), que segue
 * cobrindo as excecoes globais (Security, /login, etc.) com o mesmo formato de
 * body.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.constrsw.oauth")
public class LegacyGlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(LegacyGlobalExceptionHandler.class);
    private static final String SOURCE = "OAuthAPI";

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex) {
        log.warn("ApiException: status={} code={} message={}",
                ex.getStatus().value(), ex.getErrorCode(), ex.getMessage());
        return build(ex.getStatus(), ex.getMessage(), ex);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<ErrorStackEntry> stack = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorStackEntry(fe.getField(), fe.getDefaultMessage()))
                .toList();
        String description = stack.stream()
                .map(e -> e.getType() + ": " + e.getMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Invalid request body");
        return build(HttpStatus.BAD_REQUEST, description, stack);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            HttpMediaTypeNotSupportedException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex) {
        return build(HttpStatus.BAD_REQUEST,
                ex.getMessage() != null ? ex.getMessage() : "Bad request",
                ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAny(Exception ex) {
        log.error("Unhandled exception on com.constrsw.oauth controller", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage() != null ? ex.getMessage() : "Internal server error",
                ex);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String description, Throwable cause) {
        List<ErrorStackEntry> stack = List.of(
                new ErrorStackEntry(cause.getClass().getSimpleName(),
                        cause.getMessage() != null ? cause.getMessage() : description));
        return build(status, description, stack);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String description, List<ErrorStackEntry> stack) {
        ErrorResponse body = new ErrorResponse(
                String.valueOf(status.value()),
                description,
                SOURCE,
                stack);
        return ResponseEntity.status(status).body(body);
    }
}
