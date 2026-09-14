package br.pucrs.constrsw.oauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record ApiErrorResponse(
        @JsonProperty("error_code") String errorCode,
        @JsonProperty("error_description") String errorDescription,
        @JsonProperty("error_source") String errorSource,
        @JsonProperty("error_stack") List<Map<String, Object>> errorStack) {
}
