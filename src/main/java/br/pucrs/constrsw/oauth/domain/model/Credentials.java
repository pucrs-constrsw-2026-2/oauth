package br.pucrs.constrsw.oauth.domain.model;

/**
 * Value object: credenciais para autenticacao.
 */
public final class Credentials {

    private final String username;
    private final String password;

    public Credentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
