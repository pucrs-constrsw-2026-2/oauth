package br.pucrs.constrsw.oauth.domain.model;

/**
 * Entidade de dominio: representa um role (realm role) do Keycloak,
 * agnostica a HTTP e frameworks.
 */
public final class Role {

    private final String id;
    private final String name;
    private final String description;
    private final boolean enabled;

    public Role(String id, String name, String description, boolean enabled) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.enabled = enabled;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
