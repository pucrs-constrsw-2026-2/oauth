package com.seugrupo.oauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateUserRequest(
        @NotBlank
        @Pattern(regexp = "([-!#-'*+/-9=?A-Z^-~]+(\\.[-!#-'*+/-9=?A-Z^-~]+)*|\"([\\x21\\x23-\\x5B\\x5D-\\x7E \\t]|(\\\\[\\t -~]))+\")@([-!#-'*+/-9=?A-Z^-~]+(\\.[-!#-'*+/-9=?A-Z^-~]+)*|\\[[\\t -Z^-~]*\\])")
        String username,
        @NotBlank String password,
        @JsonProperty("first-name") @NotBlank String firstName,
        @JsonProperty("last-name") @NotBlank String lastName
) {
}
