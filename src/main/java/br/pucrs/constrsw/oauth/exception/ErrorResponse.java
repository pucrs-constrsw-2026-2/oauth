package br.pucrs.constrsw.oauth.exception;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @JsonProperty("error_code")
    private String errorCode;

    @JsonProperty("error_description")
    private String errorDescription;

    @JsonProperty("error_source")
    private String errorSource = "OAuthAPI";

    @JsonProperty("error_stack")
    private List<ErrorItem> errorStack = new ArrayList<>();

    public ErrorResponse() {
        this.errorSource = "OAuthAPI";
    }

    public ErrorResponse(String errorCode, String errorDescription) {
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
        this.errorSource = "OAuthAPI";
        this.errorStack = new ArrayList<>();
        this.errorStack.add(new ErrorItem(errorCode, errorDescription, "OAuthAPI"));
    }

    public ErrorResponse(String errorCode, String errorDescription, String errorSource, List<ErrorItem> errorStack) {
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
        this.errorSource = errorSource != null ? errorSource : "OAuthAPI";
        this.errorStack = errorStack != null ? errorStack : new ArrayList<>();
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

    public List<ErrorItem> getErrorStack() {
        return errorStack;
    }

    public void setErrorStack(List<ErrorItem> errorStack) {
        this.errorStack = errorStack;
    }

    @JsonIgnore
    public String getMessage() {
        return errorDescription;
    }

    public void setMessage(String message) {
        this.errorDescription = message;
    }

    @JsonIgnore
    public Integer getStatus() {
        try {
            return Integer.parseInt(errorCode);
        } catch (Exception e) {
            return null;
        }
    }
}
