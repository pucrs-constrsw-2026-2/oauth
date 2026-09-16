package br.pucrs.constrsw.oauth.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateRoleRequest(
        @NotBlank(message = "Nome do cargo é obrigatório")
        String name,

        String description,

        Boolean enabled
) {}
