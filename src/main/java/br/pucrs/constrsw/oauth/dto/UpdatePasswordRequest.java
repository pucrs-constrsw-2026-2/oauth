package br.pucrs.constrsw.oauth.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdatePasswordRequest(
        @NotBlank(message = "A nova senha (password) é obrigatória")
        String password,

        Boolean temporary
) {
    public UpdatePasswordRequest {
        if (temporary == null) {
            temporary = false;
        }
    }
}
