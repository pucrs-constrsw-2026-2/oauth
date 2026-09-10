package br.pucrs.constrsw.oauth.domain.model;

/**
 * Value object: dados necessarios para criar um novo usuario.
 */
public final class NewUser {

    private final String username;
    private final String password;
    private final String firstName;
    private final String lastName;

    public NewUser(String username, String password, String firstName, String lastName) {
        this.username = username;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }
}
