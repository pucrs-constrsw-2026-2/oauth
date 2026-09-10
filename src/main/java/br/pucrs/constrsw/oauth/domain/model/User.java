package br.pucrs.constrsw.oauth.domain.model;

/**
 * Entidade de dominio: representa um usuario da API, agnostica a Keycloak,
 * HTTP e frameworks. O username coincide com o e-mail (regra do enunciado).
 */
public final class User {

    private final String id;
    private final String username;
    private final String firstName;
    private final String lastName;
    private final boolean enabled;

    public User(String id, String username, String firstName, String lastName, boolean enabled) {
        this.id = id;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.enabled = enabled;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
