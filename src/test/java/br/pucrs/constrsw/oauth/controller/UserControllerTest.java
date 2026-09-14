package br.pucrs.constrsw.oauth.controller;

import br.pucrs.constrsw.oauth.dto.CreateUserRequest;
import br.pucrs.constrsw.oauth.dto.UpdatePasswordRequest;
import br.pucrs.constrsw.oauth.dto.UserResponse;
import br.pucrs.constrsw.oauth.exception.KeycloakException;
import br.pucrs.constrsw.oauth.service.KeycloakService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private KeycloakService keycloakService;

    private SimpleMeterRegistry meterRegistry;
    private UserController userController;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        userController = new UserController(keycloakService, meterRegistry);
    }

    @Test
    @DisplayName("Deveria criar usuário e retornar 201 Created com header Location")
    void testCreateUserSuccess() {
        CreateUserRequest request = new CreateUserRequest(
                "novo_aluno@pucrs.br",
                "novo_aluno@pucrs.br",
                "Novo",
                "Aluno",
                true,
                "senha123"
        );
        UserResponse mockResponse = new UserResponse(
                "uuid-12345",
                "novo_aluno@pucrs.br",
                "novo_aluno@pucrs.br",
                "Novo",
                "Aluno",
                true
        );

        when(keycloakService.createUser(any(CreateUserRequest.class))).thenReturn(mockResponse);

        ResponseEntity<UserResponse> response = userController.createUser(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("uuid-12345", response.getBody().id());
        assertEquals("novo_aluno@pucrs.br", response.getBody().username());
        assertEquals(URI.create("/users/uuid-12345"), response.getHeaders().getLocation());
        assertEquals(1.0, meterRegistry.counter("oauth.users.created.total").count());
    }

    @Test
    @DisplayName("Deveria lançar KeycloakException quando Keycloak retornar 409 Conflict")
    void testCreateUserConflict() {
        CreateUserRequest request = new CreateUserRequest(
                "existente@pucrs.br",
                "existente@pucrs.br",
                "Existe",
                "Usuario",
                true,
                "senha123"
        );

        when(keycloakService.createUser(any(CreateUserRequest.class)))
                .thenThrow(new KeycloakException("USER_ALREADY_EXISTS", "Usuário ou e-mail já cadastrado", HttpStatus.CONFLICT));

        KeycloakException ex = assertThrows(KeycloakException.class, () -> userController.createUser(request));
        assertEquals("USER_ALREADY_EXISTS", ex.getErrorCode());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    @DisplayName("Deveria atualizar senha e retornar 204 No Content")
    void testUpdatePasswordSuccess() {
        UpdatePasswordRequest request = new UpdatePasswordRequest("novaSenha123", false);

        doNothing().when(keycloakService).updatePassword(eq("uuid-12345"), any(UpdatePasswordRequest.class));

        ResponseEntity<Void> response = userController.updatePassword("uuid-12345", request);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertEquals(1.0, meterRegistry.counter("oauth.users.password_updated.total").count());
    }

    @Test
    @DisplayName("Deveria lançar KeycloakException 404 quando usuário não existir para atualização de senha")
    void testUpdatePasswordNotFound() {
        UpdatePasswordRequest request = new UpdatePasswordRequest("novaSenha123", false);

        doThrow(new KeycloakException("USER_NOT_FOUND", "Usuário não encontrado", HttpStatus.NOT_FOUND))
                .when(keycloakService).updatePassword(eq("invalido-id"), any(UpdatePasswordRequest.class));

        KeycloakException ex = assertThrows(KeycloakException.class, () -> userController.updatePassword("invalido-id", request));
        assertEquals("USER_NOT_FOUND", ex.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }
}
