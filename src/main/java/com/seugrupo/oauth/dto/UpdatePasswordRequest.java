package com.seugrupo.oauth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePasswordRequest(
        @NotBlank @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres.") String password
) {
}
