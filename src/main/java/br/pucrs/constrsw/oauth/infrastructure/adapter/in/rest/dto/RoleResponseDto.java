package br.pucrs.constrsw.oauth.infrastructure.adapter.in.rest.dto;

import br.pucrs.constrsw.oauth.domain.model.Role;

/** Response de /roles e /roles/{id}. */
public class RoleResponseDto {

    private String id;
    private String name;
    private String description;
    private Boolean enabled;

    public RoleResponseDto() {}

    public RoleResponseDto(String id, String name, String description, Boolean enabled) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.enabled = enabled;
    }

    public static RoleResponseDto fromDomain(Role r) {
        return new RoleResponseDto(r.getId(), r.getName(), r.getDescription(), r.isEnabled());
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
