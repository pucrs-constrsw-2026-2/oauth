package com.seugrupo.oauth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;

public record PatchRoleRequest(
        @Pattern(regexp = ".*\\S.*", message = "name não pode ser branco") String name,
        String description
) {
    @AssertTrue(message = "informe name e/ou description")
    public boolean hasChange() {
        return (name != null && !name.isBlank()) || description != null;
    }
}
