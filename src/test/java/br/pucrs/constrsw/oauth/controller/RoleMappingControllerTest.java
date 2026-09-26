package br.pucrs.constrsw.oauth.controller;

import br.pucrs.constrsw.oauth.error.GlobalExceptionHandler;
import br.pucrs.constrsw.oauth.service.RoleMappingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({RoleMappingController.class, GlobalExceptionHandler.class})
class RoleMappingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoleMappingService roleMappingService;

    @Test
    void assignsRoleWithBearerToken() throws Exception {
        mockMvc.perform(post("/users/user-id/roles/role-id")
                .header("Authorization", "Bearer token"))
                .andExpect(status().isCreated())
                .andExpect(content().string(""));

        verify(roleMappingService).assignRoleToUser("Bearer token", "user-id", "role-id");
    }

    @Test
    void rejectsMissingAuthorizationWhenAssigningRole() throws Exception {
        mockMvc.perform(post("/users/user-id/roles/role-id"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code").value("401"));

        verifyNoInteractions(roleMappingService);
    }

    @Test
    void removesRole() throws Exception {
        mockMvc.perform(delete("/users/user-id/roles/role-id")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(roleMappingService).removeRoleFromUser("Bearer token", "user-id", "role-id");
    }
}
