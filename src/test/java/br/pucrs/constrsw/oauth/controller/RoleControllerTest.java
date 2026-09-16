package br.pucrs.constrsw.oauth.controller;

import br.pucrs.constrsw.oauth.dto.CreateRoleRequest;
import br.pucrs.constrsw.oauth.dto.PatchRoleRequest;
import br.pucrs.constrsw.oauth.dto.RoleResponse;
import br.pucrs.constrsw.oauth.dto.UpdateRoleRequest;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleControllerTest {

    @Mock
    private KeycloakService keycloakService;

    private SimpleMeterRegistry meterRegistry;
    private RoleController roleController;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        roleController = new RoleController(keycloakService, meterRegistry);
    }

    @Test
    @DisplayName("Deveria criar cargo e retornar 201 Created com header Location")
    void testCreateRoleSuccess() {
        CreateRoleRequest request = new CreateRoleRequest("auditor", "Auditor fiscal", true);
        RoleResponse mockResponse = new RoleResponse("role-uuid-1", "auditor", "Auditor fiscal", false, false, "realm-id", true);

        when(keycloakService.createRole(any(CreateRoleRequest.class))).thenReturn(mockResponse);

        ResponseEntity<RoleResponse> response = roleController.createRole(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("role-uuid-1", response.getBody().id());
        assertEquals("auditor", response.getBody().name());
        assertEquals(URI.create("/roles/role-uuid-1"), response.getHeaders().getLocation());
        assertEquals(1.0, meterRegistry.counter("oauth.roles.created.total").count());
    }

    @Test
    @DisplayName("Deveria listar cargos e retornar 200 OK")
    void testGetRolesSuccess() {
        RoleResponse r1 = new RoleResponse("r-1", "admin", "Admin", false, false, "realm-id", true);
        RoleResponse r2 = new RoleResponse("r-2", "user", "User", false, false, "realm-id", false);

        when(keycloakService.getRoles()).thenReturn(List.of(r1, r2));

        ResponseEntity<List<RoleResponse>> response = roleController.getRoles();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals(1.0, meterRegistry.counter("oauth.roles.listed.total").count());
    }

    @Test
    @DisplayName("Deveria buscar cargo por ID e retornar 200 OK")
    void testGetRoleByIdSuccess() {
        RoleResponse r1 = new RoleResponse("r-1", "admin", "Admin", false, false, "realm-id", true);
        when(keycloakService.getRole("r-1")).thenReturn(r1);

        ResponseEntity<RoleResponse> response = roleController.getRoleById("r-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("admin", response.getBody().name());
        assertEquals(1.0, meterRegistry.counter("oauth.roles.retrieved.total").count());
    }

    @Test
    @DisplayName("Deveria lançar 404 ao buscar cargo inexistente")
    void testGetRoleByIdNotFound() {
        when(keycloakService.getRole("r-inexistente"))
                .thenThrow(new KeycloakException("ROLE_NOT_FOUND", "Cargo não encontrado", HttpStatus.NOT_FOUND));

        KeycloakException ex = assertThrows(KeycloakException.class, () -> roleController.getRoleById("r-inexistente"));
        assertEquals("ROLE_NOT_FOUND", ex.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    @DisplayName("Deveria atualizar cargo completo via PUT e retornar 200 OK")
    void testUpdateRoleSuccess() {
        UpdateRoleRequest request = new UpdateRoleRequest("auditor_chefe", "Auditor Chefe", true);
        RoleResponse mockResponse = new RoleResponse("r-1", "auditor_chefe", "Auditor Chefe", false, false, "realm-id", true);

        when(keycloakService.updateRole(eq("r-1"), any(UpdateRoleRequest.class))).thenReturn(mockResponse);

        ResponseEntity<RoleResponse> response = roleController.updateRole("r-1", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("auditor_chefe", response.getBody().name());
        assertEquals(1.0, meterRegistry.counter("oauth.roles.updated.total").count());
    }

    @Test
    @DisplayName("Deveria atualizar cargo parcialmente via PATCH e retornar 200 OK")
    void testPatchRoleSuccess() {
        PatchRoleRequest request = new PatchRoleRequest(null, "Nova descrição parcial", null);
        RoleResponse mockResponse = new RoleResponse("r-1", "auditor", "Nova descrição parcial", false, false, "realm-id", true);

        when(keycloakService.patchRole(eq("r-1"), any(PatchRoleRequest.class))).thenReturn(mockResponse);

        ResponseEntity<RoleResponse> response = roleController.patchRole("r-1", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Nova descrição parcial", response.getBody().description());
        assertEquals(1.0, meterRegistry.counter("oauth.roles.patched.total").count());
    }

    @Test
    @DisplayName("Deveria realizar deleção lógica e retornar 204 No Content")
    void testLogicalDeleteRoleSuccess() {
        doNothing().when(keycloakService).logicalDeleteRole("r-1");

        ResponseEntity<Void> response = roleController.deleteRole("r-1");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertEquals(1.0, meterRegistry.counter("oauth.roles.deleted.total").count());
    }
}
