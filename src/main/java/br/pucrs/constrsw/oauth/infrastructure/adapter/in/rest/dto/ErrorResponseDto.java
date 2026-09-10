package br.pucrs.constrsw.oauth.infrastructure.adapter.in.rest.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Envelope padrao de erro exigido pelo enunciado:
 * error_code / error_description / error_source / error_stack.
 */
public class ErrorResponseDto {

    @JsonProperty("error_code")
    private String errorCode;

    @JsonProperty("error_description")
    private String errorDescription;

    @JsonProperty("error_source")
    private String errorSource;

    @JsonProperty("error_stack")
    private List<ErrorStackEntryDto> errorStack;

    public ErrorResponseDto() {}

    public ErrorResponseDto(String errorCode, String errorDescription, String errorSource,
                            List<ErrorStackEntryDto> errorStack) {
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
        this.errorSource = errorSource;
        this.errorStack = errorStack;
    }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorDescription() { return errorDescription; }
    public void setErrorDescription(String errorDescription) { this.errorDescription = errorDescription; }
    public String getErrorSource() { return errorSource; }
    public void setErrorSource(String errorSource) { this.errorSource = errorSource; }
    public List<ErrorStackEntryDto> getErrorStack() { return errorStack; }
    public void setErrorStack(List<ErrorStackEntryDto> errorStack) { this.errorStack = errorStack; }
}
