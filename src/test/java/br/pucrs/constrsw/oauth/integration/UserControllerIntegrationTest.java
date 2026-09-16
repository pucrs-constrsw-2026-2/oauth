package br.pucrs.constrsw.oauth.integration;

import br.pucrs.constrsw.oauth.dto.CreateUserRequest;
import br.pucrs.constrsw.oauth.dto.UpdatePasswordRequest;
import br.pucrs.constrsw.oauth.dto.UpdateUserRequest;
import br.pucrs.constrsw.oauth.dto.UserResponse;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KeycloakService keycloakService;

    @Test
    @DisplayName("Integração: GET /users deve retornar lista de usuários")
    void testGetUsers() throws Exception {
        UserResponse user = new UserResponse("u-1", "joao", "joao@pucrs.br", "Joao", "Silva", true);
        when(keycloakService.getUsers(true)).thenReturn(List.of(user));

        mockMvc.perform(get("/users?enabled=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("u-1"))
                .andExpect(jsonPath("$[0].username").value("joao"));
    }

    @Test
    @DisplayName("Integração: GET /users/{id} deve retornar usuário quando existir")
    void testGetUserById() throws Exception {
        UserResponse user = new UserResponse("u-1", "joao", "joao@pucrs.br", "Joao", "Silva", true);
        when(keycloakService.getUserById("u-1")).thenReturn(user);

        mockMvc.perform(get("/users/u-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("u-1"))
                .andExpect(jsonPath("$.email").value("joao@pucrs.br"));
    }

    @Test
    @DisplayName("Integração: GET /users/{id} quando usuário não existir deve retornar 404")
    void testGetUserByIdNotFound() throws Exception {
        when(keycloakService.getUserById("inexistente"))
                .thenThrow(new KeycloakException("USER_NOT_FOUND", "Usuário não encontrado", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/users/inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error_code").value("USER_NOT_FOUND"));
    }

    @Test
    @DisplayName("Integração: POST /users deve criar usuário e retornar 201 com Location")
    void testCreateUserSuccess() throws Exception {
        UserResponse response = new UserResponse(
                "user-uuid-123",
                "aluno.teste@pucrs.br",
                "aluno.teste@pucrs.br",
                "Aluno",
                "Teste",
                true
        );

        when(keycloakService.createUser(any(CreateUserRequest.class))).thenReturn(response);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"aluno.teste@pucrs.br\",\"email\":\"aluno.teste@pucrs.br\",\"first_name\":\"Aluno\",\"last_name\":\"Teste\",\"password\":\"senha123\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/users/user-uuid-123"))
                .andExpect(jsonPath("$.id").value("user-uuid-123"))
                .andExpect(jsonPath("$.username").value("aluno.teste@pucrs.br"))
                .andExpect(jsonPath("$.email").value("aluno.teste@pucrs.br"));
    }

    @Test
    @DisplayName("Integração: POST /users com email inválido deve retornar 400 VALIDATION_ERROR")
    void testCreateUserInvalidEmail() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"usuario\",\"email\":\"email-invalido\",\"first_name\":\"Nome\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Integração: POST /users quando usuário já existe deve retornar 409 USER_ALREADY_EXISTS")
    void testCreateUserConflict() throws Exception {
        when(keycloakService.createUser(any(CreateUserRequest.class)))
                .thenThrow(new KeycloakException("USER_ALREADY_EXISTS", "Usuário já cadastrado", HttpStatus.CONFLICT));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"existente@pucrs.br\",\"email\":\"existente@pucrs.br\",\"first_name\":\"Nome\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error_code").value("USER_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("Integração: PATCH /users/{id} com senha válida deve retornar 204 No Content")
    void testUpdatePasswordSuccess() throws Exception {
        doNothing().when(keycloakService).updatePassword(eq("user-uuid-123"), any(UpdatePasswordRequest.class));

        mockMvc.perform(patch("/users/user-uuid-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"novaSenhaSegura123\",\"temporary\":false}"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Integração: PATCH /users/{id} quando usuário não existe deve retornar 404 USER_NOT_FOUND")
    void testUpdatePasswordNotFound() throws Exception {
        doThrow(new KeycloakException("USER_NOT_FOUND", "Usuário não encontrado", HttpStatus.NOT_FOUND))
                .when(keycloakService).updatePassword(eq("nao-existe-id"), any(UpdatePasswordRequest.class));

        mockMvc.perform(patch("/users/nao-existe-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"novaSenhaSegura123\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error_code").value("USER_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Integração: POST /users/{id}/roles/{roleId} deve retornar 204 No Content")
    void testAssignRoleToUser() throws Exception {
        doNothing().when(keycloakService).assignRoleToUser("user-1", "role-1");

        mockMvc.perform(post("/users/user-1/roles/role-1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Integração: POST /users/{id}/roles/{roleId} quando role não existir deve retornar 404")
    void testAssignRoleNotFound() throws Exception {
        doThrow(new KeycloakException("ROLE_NOT_FOUND", "Cargo não encontrado", HttpStatus.NOT_FOUND))
                .when(keycloakService).assignRoleToUser("user-1", "role-invalida");

        mockMvc.perform(post("/users/user-1/roles/role-invalida"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error_code").value("ROLE_NOT_FOUND"));
    }

    @Test
    @DisplayName("Integração: DELETE /users/{id}/roles/{roleId} deve retornar 204 No Content")
    void testRemoveRoleFromUser() throws Exception {
        doNothing().when(keycloakService).removeRoleFromUser("user-1", "role-1");

        mockMvc.perform(delete("/users/user-1/roles/role-1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Integração: PUT /users/{id} deve atualizar dados cadastrais e retornar 200 OK")
    void testUpdateUser() throws Exception {
        UserResponse mockResponse = new UserResponse("user-1", "user1", "novo@pucrs.br", "NomeAtualizado", "Sobrenome", true);
        when(keycloakService.updateUser(eq("user-1"), any(UpdateUserRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(put("/users/user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"novo@pucrs.br\",\"first_name\":\"NomeAtualizado\",\"last_name\":\"Sobrenome\",\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("novo@pucrs.br"))
                .andExpect(jsonPath("$.first_name").value("NomeAtualizado"));
    }

    @Test
    @DisplayName("Integração: DELETE /users/{id} deve realizar deleção lógica e retornar 204 No Content")
    void testDeleteUser() throws Exception {
        doNothing().when(keycloakService).logicalDeleteUser("user-1");

        mockMvc.perform(delete("/users/user-1"))
                .andExpect(status().isNoContent());
    }
}
