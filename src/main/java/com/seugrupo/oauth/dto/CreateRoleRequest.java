package com.seugrupo.oauth.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateRoleRequest(@NotBlank String name, String description) {
}
