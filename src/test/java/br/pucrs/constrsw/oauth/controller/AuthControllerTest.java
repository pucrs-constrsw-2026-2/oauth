package br.pucrs.constrsw.oauth.controller;

import br.pucrs.constrsw.oauth.dto.LoginRequest;
import br.pucrs.constrsw.oauth.dto.LoginResponse;
import br.pucrs.constrsw.oauth.dto.ValidateRequest;
import br.pucrs.constrsw.oauth.dto.ValidateResponse;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private KeycloakService keycloakService;

    private SimpleMeterRegistry meterRegistry;
    private AuthController authController;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        authController = new AuthController(keycloakService, meterRegistry);
    }

    @Test
    @DisplayName("Deveria retornar 200 UP no endpoint /health")
    void testHealthEndpoint() {
        ResponseEntity<String> response = authController.health();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("UP", response.getBody());
    }

    @Test
    @DisplayName("Deveria autenticar com sucesso e retornar 200 OK no POST /login")
    void testLoginSuccess() {
        LoginRequest request = new LoginRequest("aluno@pucrs.br", "senha123");
        LoginResponse mockResponse = new LoginResponse("mock-jwt-token", 300L, 1800L, "mock-refresh-token", "Bearer", "openid");

        when(keycloakService.login(any(LoginRequest.class))).thenReturn(mockResponse);

        ResponseEntity<LoginResponse> response = authController.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("mock-jwt-token", response.getBody().accessToken());
        assertEquals(1.0, meterRegistry.counter("oauth.logins.total", "status", "success").count());
    }

    @Test
    @DisplayName("Deveria repassar KeycloakException 401 no POST /login com credenciais inválidas")
    void testLoginInvalidCredentials() {
        LoginRequest request = new LoginRequest("aluno@pucrs.br", "senhaErrada");

        when(keycloakService.login(any(LoginRequest.class)))
                .thenThrow(new KeycloakException("INVALID_CREDENTIALS", "Usuário ou senha inválidos", HttpStatus.UNAUTHORIZED));

        KeycloakException ex = assertThrows(KeycloakException.class, () -> authController.login(request));
        assertEquals("INVALID_CREDENTIALS", ex.getErrorCode());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    @DisplayName("Deveria retornar 403 quando cabeçalho Authorization estiver ausente")
    void testMissingAuthorizationHeader() {
        ValidateRequest request = new ValidateRequest("lessons");
        ResponseEntity<ValidateResponse> response = authController.validateAccessPost(null, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isAllowed());
        assertEquals("Authorization header is missing", response.getBody().getMessage());

        assertEquals(1.0, meterRegistry.counter("oauth.validations.denied", "reason", "missing_header").count());
    }

    @Test
    @DisplayName("Deveria retornar 403 quando recurso não for especificado")
    void testMissingResource() {
        ResponseEntity<ValidateResponse> response = authController.validateAccessGet("Bearer mock-token", null);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isAllowed());
        assertEquals("Resource must be specified", response.getBody().getMessage());

        assertEquals(1.0, meterRegistry.counter("oauth.validations.denied", "reason", "missing_resource").count());
    }

    @Test
    @DisplayName("Deveria retornar 403 quando token for inválido no Keycloak")
    void testInvalidToken() {
        when(keycloakService.isTokenValidWithKeycloak("Bearer invalid-token")).thenReturn(false);

        ValidateRequest request = new ValidateRequest("lessons");
        ResponseEntity<ValidateResponse> response = authController.validateAccessPost("Bearer invalid-token", request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isAllowed());
        assertEquals("Invalid or expired access token", response.getBody().getMessage());

        assertEquals(1.0, meterRegistry.counter("oauth.validations.denied", "reason", "invalid_token").count());
    }

    @Test
    @DisplayName("Deveria retornar 403 quando usuário não possuir role para o recurso")
    void testUnauthorizedRole() {
        when(keycloakService.isTokenValidWithKeycloak(anyString())).thenReturn(true);
        when(keycloakService.extractUsername(anyString())).thenReturn("student_user");
        when(keycloakService.extractRoles(anyString())).thenReturn(List.of("student"));
        when(keycloakService.hasAccessToResource(List.of("student"), "lessons")).thenReturn(false);

        ValidateRequest request = new ValidateRequest("lessons");
        ResponseEntity<ValidateResponse> response = authController.validateAccessPost("Bearer student-token", request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isAllowed());
        assertEquals("User roles do not grant access to the requested resource", response.getBody().getMessage());

        assertEquals(1.0, meterRegistry.counter("oauth.validations.denied", "reason", "unauthorized_role").count());
        assertEquals(1.0, meterRegistry.counter("oauth.validations.total", "status", "forbidden", "resource", "lessons").count());
    }

    @Test
    @DisplayName("Deveria retornar 200 OK quando usuário possuir permissão para o recurso via POST e GET")
    void testAuthorizedAccess() {
        when(keycloakService.isTokenValidWithKeycloak(anyString())).thenReturn(true);
        when(keycloakService.extractUsername(anyString())).thenReturn("prof_user");
        when(keycloakService.extractRoles(anyString())).thenReturn(List.of("professor"));
        when(keycloakService.hasAccessToResource(List.of("professor"), "lessons")).thenReturn(true);

        // Teste POST
        ValidateRequest request = new ValidateRequest("lessons");
        ResponseEntity<ValidateResponse> postResponse = authController.validateAccessPost("Bearer prof-token", request);

        assertEquals(HttpStatus.OK, postResponse.getStatusCode());
        assertNotNull(postResponse.getBody());
        assertTrue(postResponse.getBody().isAllowed());
        assertEquals("prof_user", postResponse.getBody().getUsername());
        assertEquals("lessons", postResponse.getBody().getResource());

        // Teste GET
        ResponseEntity<ValidateResponse> getResponse = authController.validateAccessGet("Bearer prof-token", "lessons");

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertNotNull(getResponse.getBody());
        assertTrue(getResponse.getBody().isAllowed());

        assertEquals(2.0, meterRegistry.counter("oauth.validations.total", "status", "allowed", "resource", "lessons").count());
    }
}
