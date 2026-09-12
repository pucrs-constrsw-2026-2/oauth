package com.seugrupo.oauth.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserResponse(
        String id,
        String username,
        @JsonProperty("first-name") @JsonAlias("firstName") String firstName,
        @JsonProperty("last-name") @JsonAlias("lastName") String lastName,
        boolean enabled
) {
}
