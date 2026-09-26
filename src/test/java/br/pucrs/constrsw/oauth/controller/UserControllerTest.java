package br.pucrs.constrsw.oauth.controller;

import br.pucrs.constrsw.oauth.dto.CreateUserRequest;
import br.pucrs.constrsw.oauth.dto.UpdatePasswordRequest;
import br.pucrs.constrsw.oauth.dto.UpdateUserRequest;
import br.pucrs.constrsw.oauth.dto.UserResponse;
import br.pucrs.constrsw.oauth.error.GlobalExceptionHandler;
import br.pucrs.constrsw.oauth.error.UserManagementNotImplementedException;
import br.pucrs.constrsw.oauth.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({UserController.class, GlobalExceptionHandler.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void returnsCreatedUserAndDelegatesCreation() throws Exception {
        CreateUserRequest request = new CreateUserRequest(
                "user@example.com", "secret", "First", "Last");
        UserResponse response = new UserResponse(
                "user-id", "user@example.com", "First", "Last", true);
        when(userService.create(request)).thenReturn(response);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "user@example.com",
                                  "password": "secret",
                                  "first-name": "First",
                                  "last-name": "Last"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("user-id"))
                .andExpect(jsonPath("$.username").value("user@example.com"))
                .andExpect(jsonPath("$['first-name']").value("First"))
                .andExpect(jsonPath("$['last-name']").value("Last"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.password").doesNotExist());

        verify(userService).create(request);
    }

    @Test
    void rejectsInvalidCreationRequest() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "not-an-email",
                                  "password": "",
                                  "first-name": "",
                                  "last-name": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("400"))
                .andExpect(jsonPath("$.error_source").value("OAuthAPI"));

        verifyNoInteractions(userService);
    }

    @Test
    void returnsUsersFromService() throws Exception {
        when(userService.findAll()).thenReturn(List.of(
                new UserResponse("user-id", "user@example.com", "First", "Last", true)));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("user-id"))
                .andExpect(jsonPath("$[0]['first-name']").value("First"));

        verify(userService).findAll();
    }

    @Test
    void returnsUserById() throws Exception {
        when(userService.findById("user-id")).thenReturn(
                new UserResponse("user-id", "user@example.com", "First", "Last", true));

        mockMvc.perform(get("/users/user-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-id"));

        verify(userService).findById("user-id");
    }

    @Test
    void updatesUserAndReturnsOk() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest("Updated", "User", false);

        mockMvc.perform(put("/users/user-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "first-name": "Updated",
                                  "last-name": "User",
                                  "enabled": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(userService).update("user-id", request);
    }

    @Test
    void updatesPasswordAndReturnsOk() throws Exception {
        UpdatePasswordRequest request = new UpdatePasswordRequest("new-secret");

        mockMvc.perform(patch("/users/user-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"new-secret\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(userService).updatePassword("user-id", request);
    }

    @Test
    void rejectsBlankPasswordUpdate() throws Exception {
        mockMvc.perform(patch("/users/user-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("400"));

        verifyNoInteractions(userService);
    }

    @Test
    void disablesUserAndReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/users/user-id"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(userService).disable("user-id");
    }

    @Test
    void reportsUnimplementedUserIntegrationExplicitly() throws Exception {
        when(userService.findAll()).thenThrow(new UserManagementNotImplementedException());

        mockMvc.perform(get("/users"))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.error_code").value("501"))
                .andExpect(jsonPath("$.error_description")
                        .value("User management integration is not implemented yet"));
    }
}
