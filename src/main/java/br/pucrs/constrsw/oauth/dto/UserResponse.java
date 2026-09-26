package br.pucrs.constrsw.oauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserResponse(
        String id,
        String username,
        @JsonProperty("first-name") String firstName,
        @JsonProperty("last-name") String lastName,
        boolean enabled) {
}
