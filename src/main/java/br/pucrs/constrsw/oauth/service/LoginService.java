package br.pucrs.constrsw.oauth.service;

import br.pucrs.constrsw.oauth.client.KeycloakClient;
import br.pucrs.constrsw.oauth.dto.LoginResponse;
import br.pucrs.constrsw.oauth.error.InvalidLoginRequestException;
import org.springframework.stereotype.Service;

@Service
public class LoginService {

    private final KeycloakClient keycloakClient;

    public LoginService(KeycloakClient keycloakClient) {
        this.keycloakClient = keycloakClient;
    }

    public LoginResponse login(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new InvalidLoginRequestException("Username is required");
        }
        if (password == null || password.isBlank()) {
            throw new InvalidLoginRequestException("Password is required");
        }
        return keycloakClient.authenticate(username, password);
    }
}
