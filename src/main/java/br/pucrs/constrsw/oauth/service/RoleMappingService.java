package br.pucrs.constrsw.oauth.service;

import br.pucrs.constrsw.oauth.config.KeycloakProperties;
import br.pucrs.constrsw.oauth.dto.RoleDto;
import br.pucrs.constrsw.oauth.error.KeycloakServiceException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClient;


import java.util.List;

@Service
public class RoleMappingService {

    private final RestClient restClient;
    private final KeycloakProperties properties;
    private final ObjectMapper objectMapper;

    public RoleMappingService(RestClient keycloakRestClient, KeycloakProperties properties, ObjectMapper objectMapper) {
        this.restClient = keycloakRestClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public void assignRoleToUser(String authorization, String userId, RoleDto role) {
        try {
            // POST to role-mappings/realm with an array of role representations
            restClient.post()
                    .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", properties.realm(), userId)
                    .headers(h -> h.set("Authorization", authorization))
                    .body(List.of(role))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw new KeycloakServiceException(ex.getRawStatusCode(), "Failed to assign role to user: " + ex.getMessage(), ex.getResponseBodyAsString());
        } catch (RestClientException ex) {
            throw new KeycloakServiceException(HttpStatus.BAD_GATEWAY.value(), "Identity provider is unavailable", ex.getMessage());
        }
    }

    public void removeRoleFromUser(String authorization, String userId, String roleId) {
        try {
            // First retrieve role representation by id
            RoleDto role = restClient.get()
                    .uri("/admin/realms/{realm}/roles-by-id/{id}", properties.realm(), roleId)
                    .headers(h -> h.set("Authorization", authorization))
                    .retrieve()
                    .body(RoleDto.class);

            if (role == null) {
                throw new KeycloakServiceException(HttpStatus.NOT_FOUND.value(), "Role not found", null);
            }

            // DELETE mapping by sending array with role representation
            restClient.method(org.springframework.http.HttpMethod.DELETE)
                    .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", properties.realm(), userId)
                    .headers(h -> h.set("Authorization", authorization))
                    .body(List.of(role))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw new KeycloakServiceException(ex.getRawStatusCode(), "Failed to remove role from user: " + ex.getMessage(), ex.getResponseBodyAsString());
        } catch (RestClientException ex) {
            throw new KeycloakServiceException(HttpStatus.BAD_GATEWAY.value(), "Identity provider is unavailable", ex.getMessage());
        }
    }
}
