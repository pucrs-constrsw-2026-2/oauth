package br.pucrs.constrsw.oauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRequest(
        @JsonProperty("first-name") @NotBlank String firstName,
        @JsonProperty("last-name") @NotBlank String lastName,
        @NotNull Boolean enabled) {
}
