package br.pucrs.constrsw.oauth.service;

import br.pucrs.constrsw.oauth.config.KeycloakProperties;
import br.pucrs.constrsw.oauth.dto.RoleDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RoleMappingServiceTest {

    private MockRestServiceServer server;
    private RoleMappingService roleMappingService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://keycloak:8080");
        server = MockRestServiceServer.bindTo(builder).build();
        roleMappingService = new RoleMappingService(builder.build(),
                new KeycloakProperties("http://keycloak:8080", "constrsw", "oauth", "secret"));
    }

    @Test
    void assignsRoleToUser() {
        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/constrsw/roles-by-id/role-id"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer token"))
                .andRespond(withSuccess("{\"id\":\"role-id\",\"name\":\"admin\"}", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/constrsw/users/user-id/role-mappings/realm"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer token"))
                .andExpect(content().json("[{\"id\":\"role-id\",\"name\":\"admin\"}]"))
                .andRespond(withSuccess());

        roleMappingService.assignRoleToUser("Bearer token", "user-id", "role-id");

        server.verify();
    }

    @Test
    void removesRoleFromUser() {
        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/constrsw/roles-by-id/role-id"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":\"role-id\",\"name\":\"admin\"}", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/constrsw/users/user-id/role-mappings/realm"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess());

        roleMappingService.removeRoleFromUser("Bearer token", "user-id", "role-id");

        server.verify();
    }
}
