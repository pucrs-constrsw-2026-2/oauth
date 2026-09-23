package br.pucrs.constrsw.oauth.infrastructure.adapter.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import br.pucrs.constrsw.oauth.OauthApplication;
import br.pucrs.constrsw.oauth.application.port.in.AttachRoleToUserUseCase;
import br.pucrs.constrsw.oauth.application.port.in.CreateRoleUseCase;
import br.pucrs.constrsw.oauth.application.port.in.CreateUserUseCase;
import br.pucrs.constrsw.oauth.application.port.in.DeleteRoleUseCase;
import br.pucrs.constrsw.oauth.application.port.in.DetachRoleFromUserUseCase;
import br.pucrs.constrsw.oauth.application.port.in.DisableUserUseCase;
import br.pucrs.constrsw.oauth.application.port.in.GetRoleUseCase;
import br.pucrs.constrsw.oauth.application.port.in.GetUserUseCase;
import br.pucrs.constrsw.oauth.application.port.in.ListRolesUseCase;
import br.pucrs.constrsw.oauth.application.port.in.ListUsersUseCase;
import br.pucrs.constrsw.oauth.application.port.in.LoginUseCase;
import br.pucrs.constrsw.oauth.application.port.in.UpdateRoleUseCase;
import br.pucrs.constrsw.oauth.application.port.in.UpdateUserUseCase;
import br.pucrs.constrsw.oauth.application.port.in.UpdatePasswordUseCase;
import br.pucrs.constrsw.oauth.domain.model.AuthTokens;
import br.pucrs.constrsw.oauth.domain.model.Role;
import br.pucrs.constrsw.oauth.domain.model.User;

@SpringBootTest(classes = OauthApplication.class)
@AutoConfigureMockMvc
class RestControllersIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private JwtDecoder jwtDecoder;

  @MockBean
  private LoginUseCase loginUseCase;
  @MockBean
  private CreateUserUseCase createUserUseCase;
  @MockBean
  private ListUsersUseCase listUsersUseCase;
  @MockBean
  private GetUserUseCase getUserUseCase;
  @MockBean
  private UpdateUserUseCase updateUserUseCase;
  @MockBean
  private UpdatePasswordUseCase updatePasswordUseCase;
  @MockBean
  private DisableUserUseCase disableUserUseCase;
  @MockBean
  private CreateRoleUseCase createRoleUseCase;
  @MockBean
  private ListRolesUseCase listRolesUseCase;
  @MockBean
  private GetRoleUseCase getRoleUseCase;
  @MockBean
  private UpdateRoleUseCase updateRoleUseCase;
  @MockBean
  private DeleteRoleUseCase deleteRoleUseCase;
  @MockBean
  private AttachRoleToUserUseCase attachRoleToUserUseCase;
  @MockBean
  private DetachRoleFromUserUseCase detachRoleFromUserUseCase;

  @Test
  void loginAcceptsMultipartCredentialsWithoutAuthentication() throws Exception {
    AuthTokens tokens = new AuthTokens("Bearer", "access-token", 300L, "refresh-token", 1800L);
    when(loginUseCase.execute(any())).thenReturn(tokens);

    mockMvc.perform(multipart("/login")
        .param("username", "ana@example.com")
        .param("password", "secret"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.access_token").value("access-token"))
        .andExpect(jsonPath("$.token_type").value("Bearer"));
  }

  @Test
  void userEndpointsTranslateRequestsAndResponses() throws Exception {
    User user = new User("u1", "ana@example.com", "Ana", "Silva", true);
    when(createUserUseCase.execute(nullable(String.class), any())).thenReturn(user);
    when(listUsersUseCase.execute(nullable(String.class), any())).thenReturn(List.of(user));
    when(getUserUseCase.execute(nullable(String.class), nullable(String.class))).thenReturn(user);

    mockMvc.perform(post("/users").with(jwt())
        .contentType(MediaType.APPLICATION_JSON)
        .content(
            "{\"username\":\"ana@example.com\",\"password\":\"secret\",\"first-name\":\"Ana\",\"last-name\":\"Silva\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value("u1"));
    mockMvc.perform(get("/users").param("enabled", "true").with(jwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].username").value("ana@example.com"));
    mockMvc.perform(get("/users/u1").with(jwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.enabled").value(true));

    verify(listUsersUseCase).execute(nullable(String.class), org.mockito.ArgumentMatchers.eq(true));
  }

  @Test
  void userMutationsReturnContractStatuses() throws Exception {
    mockMvc.perform(put("/users/u1").with(jwt())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"first-name\":\"Beatriz\",\"enabled\":true}"))
        .andExpect(status().isOk());
    mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/users/u1").with(jwt())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"password\":\"new-secret\"}"))
        .andExpect(status().isOk());
    mockMvc.perform(delete("/users/u1").with(jwt()))
        .andExpect(status().isNoContent());

    verify(updateUserUseCase).execute(nullable(String.class), org.mockito.ArgumentMatchers.eq("u1"), any());
    verify(updatePasswordUseCase).execute(nullable(String.class), org.mockito.ArgumentMatchers.eq("u1"),
        org.mockito.ArgumentMatchers.eq("new-secret"));
    verify(disableUserUseCase).execute(nullable(String.class), org.mockito.ArgumentMatchers.eq("u1"));
  }

  @Test
  void roleEndpointsSupportCrudAndAssignment() throws Exception {
    Role role = new Role("r1", "teacher", "Teaching role", true);
    when(createRoleUseCase.execute(nullable(String.class), any())).thenReturn(role);
    when(listRolesUseCase.execute(nullable(String.class), any())).thenReturn(List.of(role));
    when(getRoleUseCase.execute(nullable(String.class), nullable(String.class))).thenReturn(role);

    mockMvc.perform(post("/roles").with(jwt())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"name\":\"teacher\",\"description\":\"Teaching role\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value("r1"));
    mockMvc.perform(get("/roles").param("enabled", "true").with(jwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("teacher"));
    mockMvc.perform(get("/roles/r1").with(jwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("teacher"));
    mockMvc.perform(put("/roles/r1").with(jwt())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"description\":\"Updated\"}"))
        .andExpect(status().isOk());
    mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/roles/r1").with(jwt())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"enabled\":false}"))
        .andExpect(status().isOk());
    mockMvc.perform(post("/roles/r1/users/u1").with(jwt()))
        .andExpect(status().isNoContent());
    mockMvc.perform(delete("/roles/r1/users/u1").with(jwt()))
        .andExpect(status().isNoContent());
    mockMvc.perform(delete("/roles/r1").with(jwt()))
        .andExpect(status().isNoContent());

    verify(listRolesUseCase).execute(nullable(String.class), org.mockito.ArgumentMatchers.eq(true));
    verify(updateRoleUseCase, org.mockito.Mockito.times(2))
        .execute(nullable(String.class), org.mockito.ArgumentMatchers.eq("r1"), any());
    verify(attachRoleToUserUseCase).execute(nullable(String.class), org.mockito.ArgumentMatchers.eq("u1"),
        org.mockito.ArgumentMatchers.eq("r1"));
    verify(detachRoleFromUserUseCase).execute(nullable(String.class), org.mockito.ArgumentMatchers.eq("u1"),
        org.mockito.ArgumentMatchers.eq("r1"));
    verify(deleteRoleUseCase).execute(nullable(String.class), org.mockito.ArgumentMatchers.eq("r1"));
  }

  @Test
  void protectedEndpointsRejectAnonymousRequests() throws Exception {
    mockMvc.perform(get("/users")).andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error_code").value("401"));
  }

  @Test
  void prometheusEndpointIsExposedForAuthenticatedScrapers() throws Exception {
    mockMvc.perform(get("/actuator/prometheus").with(jwt()))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("http_server_requests")));
  }
}
