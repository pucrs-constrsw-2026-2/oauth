package br.pucrs.constrsw.oauth.infrastructure.adapter.in.rest.dto;

import br.pucrs.constrsw.oauth.domain.model.RoleUpdate;

/**
 * Request body do PUT /roles/{id} (atualizacao total) e do
 * PATCH /roles/{id} (atualizacao parcial). Todos os campos sao opcionais;
 * o que vier null nao e alterado no provider.
 */
public class RoleUpdateRequestDto {

    private String name;
    private String description;
    private Boolean enabled;

    public RoleUpdateRequestDto() {}

    public RoleUpdate toDomain() {
        return new RoleUpdate(name, description, enabled);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
