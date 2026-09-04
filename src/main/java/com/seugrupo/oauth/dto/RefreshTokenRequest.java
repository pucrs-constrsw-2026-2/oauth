package com.seugrupo.oauth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "refresh_token é obrigatório") String refresh_token
) {
    public String refreshToken() {
        return refresh_token;
    }
}
