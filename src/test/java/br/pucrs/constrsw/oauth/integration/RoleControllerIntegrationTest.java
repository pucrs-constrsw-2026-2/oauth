package br.pucrs.constrsw.oauth.integration;

import br.pucrs.constrsw.oauth.dto.CreateRoleRequest;
import br.pucrs.constrsw.oauth.dto.PatchRoleRequest;
import br.pucrs.constrsw.oauth.dto.RoleResponse;
import br.pucrs.constrsw.oauth.dto.UpdateRoleRequest;
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
class RoleControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KeycloakService keycloakService;

    @Test
    @DisplayName("Integração: POST /roles deve criar cargo e retornar 201 com Location")
    void testCreateRole() throws Exception {
        RoleResponse response = new RoleResponse("role-123", "auditor", "Desc", false, false, "realm", true);
        when(keycloakService.createRole(any(CreateRoleRequest.class))).thenReturn(response);

        mockMvc.perform(post("/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"auditor\",\"description\":\"Desc\",\"enabled\":true}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/roles/role-123"))
                .andExpect(jsonPath("$.id").value("role-123"))
                .andExpect(jsonPath("$.name").value("auditor"))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    @DisplayName("Integração: POST /roles com nome vazio deve retornar 400 VALIDATION_ERROR")
    void testCreateRoleInvalid() throws Exception {
        mockMvc.perform(post("/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"description\":\"Desc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("400"));
    }

    @Test
    @DisplayName("Integração: POST /roles duplicado deve retornar 409 ROLE_ALREADY_EXISTS")
    void testCreateRoleDuplicate() throws Exception {
        when(keycloakService.createRole(any(CreateRoleRequest.class)))
                .thenThrow(new KeycloakException("ROLE_ALREADY_EXISTS", "Cargo já cadastrado", HttpStatus.CONFLICT));

        mockMvc.perform(post("/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"duplicado\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error_code").value("409"));
    }

    @Test
    @DisplayName("Integração: GET /roles deve listar cargos")
    void testGetRoles() throws Exception {
        RoleResponse r = new RoleResponse("role-1", "admin", "Admin", false, false, "realm", true);
        when(keycloakService.getRoles()).thenReturn(List.of(r));

        mockMvc.perform(get("/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("admin"));
    }

    @Test
    @DisplayName("Integração: GET /roles/{id} existente deve retornar 200 OK")
    void testGetRoleById() throws Exception {
        RoleResponse r = new RoleResponse("role-1", "admin", "Admin", false, false, "realm", true);
        when(keycloakService.getRole("role-1")).thenReturn(r);

        mockMvc.perform(get("/roles/role-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("role-1"))
                .andExpect(jsonPath("$.name").value("admin"));
    }

    @Test
    @DisplayName("Integração: GET /roles/{id} inexistente deve retornar 404 ROLE_NOT_FOUND")
    void testGetRoleByIdNotFound() throws Exception {
        when(keycloakService.getRole("inexistente"))
                .thenThrow(new KeycloakException("ROLE_NOT_FOUND", "Cargo não encontrado", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/roles/inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error_code").value("404"));
    }

    @Test
    @DisplayName("Integração: PUT /roles/{id} deve retornar 200 OK")
    void testUpdateRole() throws Exception {
        RoleResponse updated = new RoleResponse("role-1", "admin_novo", "Nova desc", false, false, "realm", true);
        when(keycloakService.updateRole(eq("role-1"), any(UpdateRoleRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/roles/role-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"admin_novo\",\"description\":\"Nova desc\",\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("admin_novo"));
    }

    @Test
    @DisplayName("Integração: PATCH /roles/{id} deve retornar 200 OK")
    void testPatchRole() throws Exception {
        RoleResponse patched = new RoleResponse("role-1", "admin", "Desc patch", false, false, "realm", false);
        when(keycloakService.patchRole(eq("role-1"), any(PatchRoleRequest.class))).thenReturn(patched);

        mockMvc.perform(patch("/roles/role-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Desc patch\",\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Desc patch"))
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    @DisplayName("Integração: DELETE /roles/{id} deve retornar 204 No Content")
    void testDeleteRole() throws Exception {
        doNothing().when(keycloakService).logicalDeleteRole("role-1");

        mockMvc.perform(delete("/roles/role-1"))
                .andExpect(status().isNoContent());
    }
}
