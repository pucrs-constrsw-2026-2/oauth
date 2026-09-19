package br.pucrs.constrsw.oauth.infrastructure.adapter.in.rest.dto;

import br.pucrs.constrsw.oauth.domain.model.NewRole;
import jakarta.validation.constraints.NotBlank;

/** Request body do POST /roles. */
public class RoleCreateRequestDto {

    @NotBlank(message = "name is required")
    private String name;

    private String description;

    public RoleCreateRequestDto() {}

    public NewRole toDomain() {
        return new NewRole(name, description);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
