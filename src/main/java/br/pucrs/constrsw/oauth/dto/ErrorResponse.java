package br.pucrs.constrsw.oauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Standard error envelope shared by every route of the API, as defined by the
 * assignment's "Tratamento de Erros" section.
 */
public class ErrorResponse {

    @JsonProperty("error_code")
    private String errorCode;

    @JsonProperty("error_description")
    private String errorDescription;

    @JsonProperty("error_source")
    private String errorSource;

    @JsonProperty("error_stack")
    private List<String> errorStack;

    public ErrorResponse() {
    }

    public ErrorResponse(String errorCode, String errorDescription, String errorSource, List<String> errorStack) {
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
        this.errorSource = errorSource;
        this.errorStack = errorStack;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public String getErrorSource() {
        return errorSource;
    }

    public void setErrorSource(String errorSource) {
        this.errorSource = errorSource;
    }

    public List<String> getErrorStack() {
        return errorStack;
    }

    public void setErrorStack(List<String> errorStack) {
        this.errorStack = errorStack;
    }
}
