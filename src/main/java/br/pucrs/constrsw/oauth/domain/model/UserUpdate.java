package br.pucrs.constrsw.oauth.domain.model;

/**
 * Value object: campos opcionais para atualizacao parcial de um usuario
 * (PUT /users/{id}). Campos null nao devem ser propagados ao provider.
 */
public final class UserUpdate {

    private final String username;
    private final String firstName;
    private final String lastName;
    private final Boolean enabled;

    public UserUpdate(String username, String firstName, String lastName, Boolean enabled) {
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.enabled = enabled;
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

    public Boolean getEnabled() {
        return enabled;
    }

    public boolean isEmpty() {
        return username == null && firstName == null && lastName == null && enabled == null;
    }
}
