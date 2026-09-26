package br.pucrs.constrsw.oauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
        @NotBlank @Email String username,
        @NotBlank String password,
        @JsonProperty("first-name") @NotBlank String firstName,
        @JsonProperty("last-name") @NotBlank String lastName) {
}
