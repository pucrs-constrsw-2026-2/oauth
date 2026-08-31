package br.pucrs.constrsw.oauth.exception;

import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;

/**
 * Exception carrying everything the {@link GlobalExceptionHandler} needs to build the
 * standard error envelope (error_code / error_description / error_source / error_stack).
 */
public class OAuthApiException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String errorCode;
    private final String errorSource;
    private final List<String> errorStack;

    public OAuthApiException(HttpStatus httpStatus, String errorCode, String errorDescription, String errorSource) {
        this(httpStatus, errorCode, errorDescription, errorSource, null, new ArrayList<>());
    }

    public OAuthApiException(HttpStatus httpStatus, String errorCode, String errorDescription, String errorSource,
                              Throwable cause, List<String> upstreamStack) {
        super(errorDescription, cause);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.errorSource = errorSource;
        this.errorStack = upstreamStack != null ? new ArrayList<>(upstreamStack) : new ArrayList<>();
    }

    public static OAuthApiException badRequest(String description, String source) {
        return new OAuthApiException(HttpStatus.BAD_REQUEST, "400", description, source);
    }

    public static OAuthApiException unauthorized(String description, String source) {
        return new OAuthApiException(HttpStatus.UNAUTHORIZED, "401", description, source);
    }

    public static OAuthApiException forbidden(String description, String source) {
        return new OAuthApiException(HttpStatus.FORBIDDEN, "403", description, source);
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorSource() {
        return errorSource;
    }

    public List<String> getErrorStack() {
        return errorStack;
    }
}
