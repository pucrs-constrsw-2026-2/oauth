package br.pucrs.constrsw.oauth.service;

import br.pucrs.constrsw.oauth.client.KeycloakClient;
import br.pucrs.constrsw.oauth.error.InvalidLoginRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class LoginServiceTest {

    private final KeycloakClient keycloakClient = mock(KeycloakClient.class);
    private final LoginService loginService = new LoginService(keycloakClient);

    @Test
    void rejectsBlankUsername() {
        assertThatThrownBy(() -> loginService.login("  ", "secret"))
                .isInstanceOf(InvalidLoginRequestException.class)
                .hasMessage("Username is required");

        verifyNoInteractions(keycloakClient);
    }

    @Test
    void rejectsBlankPassword() {
        assertThatThrownBy(() -> loginService.login("user@example.com", "  "))
                .isInstanceOf(InvalidLoginRequestException.class)
                .hasMessage("Password is required");

        verifyNoInteractions(keycloakClient);
    }
}
