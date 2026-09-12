package com.seugrupo.oauth.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdatePasswordRequest(@NotBlank String password) {
}
