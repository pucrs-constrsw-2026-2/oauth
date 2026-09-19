package br.pucrs.constrsw.oauth.domain.model;

/**
 * Value object: dados necessarios para criar um novo role.
 */
public final class NewRole {

    private final String name;
    private final String description;

    public NewRole(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
