package br.pucrs.constrsw.oauth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RoleResponse(
        String id,
        String name,
        String description,
        Boolean composite,
        Boolean clientRole,
        String containerId,
        Boolean enabled
) {}
