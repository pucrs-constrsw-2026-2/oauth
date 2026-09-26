package br.pucrs.constrsw.oauth.service;

import br.pucrs.constrsw.oauth.config.KeycloakProperties;
import br.pucrs.constrsw.oauth.dto.RoleDto;
import br.pucrs.constrsw.oauth.error.KeycloakServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.http.HttpStatus.NOT_FOUND;

class RoleServiceTest {

    private MockRestServiceServer server;
    private RoleService roleService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://keycloak:8080");
        server = MockRestServiceServer.bindTo(builder).build();
        roleService = new RoleService(builder.build(),
                new KeycloakProperties("http://keycloak:8080", "constrsw", "oauth", "secret"),
                new ObjectMapper());
    }

    @Test
    void createsRole() {
        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/constrsw/roles"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"id\":\"role-id\",\"name\":\"admin\"}", MediaType.APPLICATION_JSON));

        RoleDto result = roleService.createRole("Bearer token", new RoleDto(null, "admin", null, null));

        assertThat(result.getId()).isEqualTo("role-id");
        assertThat(result.getName()).isEqualTo("admin");
        server.verify();
    }

    @Test
    void listsRoles() {
        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/constrsw/roles"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[{\"id\":\"role-id\",\"name\":\"admin\"}]", MediaType.APPLICATION_JSON));

        List<RoleDto> result = roleService.getAllRoles("Bearer token");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("admin");
        server.verify();
    }

    @Test
    void mapsKeycloakErrors() {
        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/constrsw/roles-by-id/missing"))
                .andRespond(withStatus(NOT_FOUND).body("{\"error\":\"not found\"}"));

        assertThatThrownBy(() -> roleService.getRoleById("Bearer token", "missing"))
                .isInstanceOf(KeycloakServiceException.class)
                .hasMessageContaining("Failed to retrieve role");
        server.verify();
    }
}
