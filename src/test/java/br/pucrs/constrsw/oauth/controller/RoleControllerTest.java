package br.pucrs.constrsw.oauth.controller;

import br.pucrs.constrsw.oauth.dto.RoleDto;
import br.pucrs.constrsw.oauth.error.GlobalExceptionHandler;
import br.pucrs.constrsw.oauth.service.RoleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({RoleController.class, GlobalExceptionHandler.class})
class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoleService roleService;

    @Test
    void createsRoleWithBearerToken() throws Exception {
        RoleDto role = new RoleDto("role-id", "admin", "Administrator", null);
        when(roleService.createRole(eq("Bearer token"), any(RoleDto.class))).thenReturn(role);

        mockMvc.perform(post("/roles")
                        .header("Authorization", "Bearer token")
                        .contentType("application/json")
                        .content("""
                                {"id":"role-id","name":"admin","description":"Administrator"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("role-id"))
                .andExpect(jsonPath("$.name").value("admin"));

        verify(roleService).createRole(eq("Bearer token"), argThat(value ->
            "role-id".equals(value.getId()) && "admin".equals(value.getName())));
    }

    @Test
    void rejectsMissingAuthorization() throws Exception {
        mockMvc.perform(get("/roles"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code").value("401"));

        verifyNoInteractions(roleService);
    }

    @Test
    void listsRoles() throws Exception {
        when(roleService.getAllRoles("Bearer token")).thenReturn(List.of(
                new RoleDto("role-id", "admin", "Administrator", null)));

        mockMvc.perform(get("/roles").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("admin"));

        verify(roleService).getAllRoles("Bearer token");
    }

    @Test
    void getsRoleById() throws Exception {
        RoleDto role = new RoleDto("role-id", "admin", "Administrator", null);
        when(roleService.getRoleById("Bearer token", "role-id")).thenReturn(role);

        mockMvc.perform(get("/roles/role-id").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("role-id"));

        verify(roleService).getRoleById("Bearer token", "role-id");
    }

    @Test
    void updatesRole() throws Exception {
        RoleDto role = new RoleDto("role-id", "admin", "Updated", null);
        when(roleService.updateRole(eq("Bearer token"), eq("role-id"), any(RoleDto.class))).thenReturn(role);

        mockMvc.perform(put("/roles/role-id")
                        .header("Authorization", "Bearer token")
                        .contentType("application/json")
                        .content("""
                            {"id":"role-id","name":"admin","description":"Updated"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated"));

        verify(roleService).updateRole(eq("Bearer token"), eq("role-id"), argThat(value ->
            "role-id".equals(value.getId()) && "Updated".equals(value.getDescription())));
    }

    @Test
    void patchesRole() throws Exception {
        RoleDto partial = new RoleDto(null, "editor", null, null);
        when(roleService.patchRole(eq("Bearer token"), eq("role-id"), any(RoleDto.class))).thenReturn(partial);

        mockMvc.perform(patch("/roles/role-id")
                        .header("Authorization", "Bearer token")
                        .contentType("application/json")
                        .content("""
                            {"name":"editor"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("editor"));

        verify(roleService).patchRole(eq("Bearer token"), eq("role-id"), argThat(value ->
            "editor".equals(value.getName())));
    }

    @Test
    void deletesRole() throws Exception {
        mockMvc.perform(delete("/roles/role-id").header("Authorization", "Bearer token"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(roleService).deleteRole("Bearer token", "role-id");
    }
}
