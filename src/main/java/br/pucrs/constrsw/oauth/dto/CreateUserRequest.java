package br.pucrs.constrsw.oauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateUserRequest(
        @NotBlank(message = "Username é obrigatório")
        String username,

        @NotBlank(message = "Email é obrigatório")
        @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Formato de email inválido")
        String email,

        @JsonProperty("first_name")
        String firstName,

        @JsonProperty("last_name")
        String lastName,

        Boolean enabled,

        String password
) {
    public CreateUserRequest {
        if (enabled == null) {
            enabled = true;
        }
    }
}
