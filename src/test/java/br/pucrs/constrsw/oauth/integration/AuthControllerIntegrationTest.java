package br.pucrs.constrsw.oauth.integration;

import br.pucrs.constrsw.oauth.dto.LoginRequest;
import br.pucrs.constrsw.oauth.dto.LoginResponse;
import br.pucrs.constrsw.oauth.exception.KeycloakException;
import br.pucrs.constrsw.oauth.service.KeycloakService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KeycloakService keycloakService;

    @Test
    @DisplayName("Integração: GET /health deve retornar 200 UP")
    void testHealth() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("UP"));
    }

    @Test
    @DisplayName("Integração: POST /login com credenciais válidas deve retornar 200 OK com tokens")
    void testLoginSuccess() throws Exception {
        LoginResponse response = new LoginResponse("access-token-jwt", 300L, 1800L, "refresh-token-jwt", "Bearer", "openid");
        when(keycloakService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"student@pucrs.br\",\"password\":\"a12345678\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("access-token-jwt"))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(300));
    }

    @Test
    @DisplayName("Integração: POST /login com credenciais inválidas deve retornar 401 INVALID_CREDENTIALS")
    void testLoginInvalidCredentials() throws Exception {
        when(keycloakService.login(any(LoginRequest.class)))
                .thenThrow(new KeycloakException("INVALID_CREDENTIALS", "Usuário ou senha inválidos", HttpStatus.UNAUTHORIZED));

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"student@pucrs.br\",\"password\":\"senha-errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Integração: POST /validate com token e permissão válida deve retornar 200 OK")
    void testValidatePostAllowed() throws Exception {
        when(keycloakService.isTokenValidWithKeycloak(anyString())).thenReturn(true);
        when(keycloakService.extractUsername(anyString())).thenReturn("prof_user");
        when(keycloakService.extractRoles(anyString())).thenReturn(List.of("professor"));
        when(keycloakService.hasAccessToResource(List.of("professor"), "lessons")).thenReturn(true);

        mockMvc.perform(post("/validate")
                        .header("Authorization", "Bearer mock-prof-jwt-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resource\":\"lessons\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true))
                .andExpect(jsonPath("$.username").value("prof_user"))
                .andExpect(jsonPath("$.resource").value("lessons"))
                .andExpect(jsonPath("$.roles[0]").value("professor"));
    }

    @Test
    @DisplayName("Integração: GET /validate com token e permissão válida deve retornar 200 OK")
    void testValidateGetAllowed() throws Exception {
        when(keycloakService.isTokenValidWithKeycloak(anyString())).thenReturn(true);
        when(keycloakService.extractUsername(anyString())).thenReturn("admin_user");
        when(keycloakService.extractRoles(anyString())).thenReturn(List.of("administrator"));
        when(keycloakService.hasAccessToResource(List.of("administrator"), "rooms")).thenReturn(true);

        mockMvc.perform(get("/validate")
                        .header("Authorization", "Bearer mock-admin-jwt-token")
                        .param("resource", "rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true))
                .andExpect(jsonPath("$.username").value("admin_user"))
                .andExpect(jsonPath("$.resource").value("rooms"));
    }

    @Test
    @DisplayName("Integração: POST /validate sem Authorization deve retornar 403 Forbidden")
    void testValidatePostMissingAuth() throws Exception {
        mockMvc.perform(post("/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resource\":\"lessons\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.allowed").value(false))
                .andExpect(jsonPath("$.message").value("Authorization header is missing"));
    }

    @Test
    @DisplayName("Integração: POST /validate com role sem acesso deve retornar 403 Forbidden")
    void testValidatePostForbiddenRole() throws Exception {
        when(keycloakService.isTokenValidWithKeycloak(anyString())).thenReturn(true);
        when(keycloakService.extractUsername(anyString())).thenReturn("student_user");
        when(keycloakService.extractRoles(anyString())).thenReturn(List.of("student"));
        when(keycloakService.hasAccessToResource(List.of("student"), "lessons")).thenReturn(false);

        mockMvc.perform(post("/validate")
                        .header("Authorization", "Bearer mock-student-jwt-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resource\":\"lessons\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.allowed").value(false))
                .andExpect(jsonPath("$.message").value("User roles do not grant access to the requested resource"));
    }

    @Test
    @DisplayName("Integração: Alias POST /authorize e GET /authorize devem funcionar igualmente")
    void testAuthorizeAliases() throws Exception {
        when(keycloakService.isTokenValidWithKeycloak(anyString())).thenReturn(true);
        when(keycloakService.extractUsername(anyString())).thenReturn("coord_user");
        when(keycloakService.extractRoles(anyString())).thenReturn(List.of("coordinator"));
        when(keycloakService.hasAccessToResource(List.of("coordinator"), "courses")).thenReturn(true);

        mockMvc.perform(post("/authorize")
                        .header("Authorization", "Bearer mock-coord-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resource\":\"courses\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true));

        mockMvc.perform(get("/authorize")
                        .header("Authorization", "Bearer mock-coord-token")
                        .param("resource", "courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true));
    }
}
