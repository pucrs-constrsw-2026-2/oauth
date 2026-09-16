package br.pucrs.constrsw.oauth.dto;

public record PatchRoleRequest(
        String name,

        String description,

        Boolean enabled
) {}
