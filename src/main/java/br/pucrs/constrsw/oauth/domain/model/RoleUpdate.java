package br.pucrs.constrsw.oauth.domain.model;

/**
 * Value object: campos opcionais para atualizacao (total via PUT ou parcial
 * via PATCH) de um role. Campos null nao devem ser propagados ao provider.
 */
public final class RoleUpdate {

    private final String name;
    private final String description;
    private final Boolean enabled;

    public RoleUpdate(String name, String description, Boolean enabled) {
        this.name = name;
        this.description = description;
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public boolean isEmpty() {
        return name == null && description == null && enabled == null;
    }
}
