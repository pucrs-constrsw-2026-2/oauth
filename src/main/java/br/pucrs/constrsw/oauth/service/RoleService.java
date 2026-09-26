package br.pucrs.constrsw.oauth.service;

import br.pucrs.constrsw.oauth.config.KeycloakProperties;
import br.pucrs.constrsw.oauth.dto.RoleDto;
import br.pucrs.constrsw.oauth.error.KeycloakServiceException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;
import org.springframework.core.ParameterizedTypeReference;

import java.util.List;

@Service
public class RoleService {

    private final RestClient restClient;
    private final KeycloakProperties properties;
    private final ObjectMapper objectMapper;

    public RoleService(RestClient keycloakRestClient, KeycloakProperties properties, ObjectMapper objectMapper) {
        this.restClient = keycloakRestClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public RoleDto createRole(String authorization, RoleDto role) {
        try {
            RoleDto created = restClient.post()
                    .uri("/admin/realms/{realm}/roles", properties.realm())
                    .headers(h -> h.set("Authorization", authorization))
                    .body(role)
                    .retrieve()
                    .body(RoleDto.class);
            return created;
        } catch (RestClientResponseException ex) {
            throw newKeycloakException(ex, "Failed to create role");
        } catch (RestClientException ex) {
            throw new KeycloakServiceException(HttpStatus.BAD_GATEWAY.value(), "Identity provider is unavailable", ex.getMessage());
        }
    }

    public List<RoleDto> getAllRoles(String authorization) {
        try {
            List<RoleDto> roles = restClient.get()
                    .uri("/admin/realms/{realm}/roles", properties.realm())
                    .headers(h -> h.set("Authorization", authorization))
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<RoleDto>>() {});
            return roles;
        } catch (RestClientResponseException ex) {
            throw newKeycloakException(ex, "Failed to retrieve roles");
        } catch (RestClientException ex) {
            throw new KeycloakServiceException(HttpStatus.BAD_GATEWAY.value(), "Identity provider is unavailable", ex.getMessage());
        }
    }

    public RoleDto getRoleById(String authorization, String id) {
        try {
            RoleDto role = restClient.get()
                    .uri("/admin/realms/{realm}/roles-by-id/{id}", properties.realm(), id)
                    .headers(h -> h.set("Authorization", authorization))
                    .retrieve()
                    .body(RoleDto.class);
            return role;
        } catch (RestClientResponseException ex) {
            throw newKeycloakException(ex, "Failed to retrieve role");
        } catch (RestClientException ex) {
            throw new KeycloakServiceException(HttpStatus.BAD_GATEWAY.value(), "Identity provider is unavailable", ex.getMessage());
        }
    }

    public RoleDto updateRole(String authorization, String id, RoleDto role) {
        try {
            RoleDto updated = restClient.put()
                    .uri("/admin/realms/{realm}/roles-by-id/{id}", properties.realm(), id)
                    .headers(h -> h.set("Authorization", authorization))
                    .body(role)
                    .retrieve()
                    .body(RoleDto.class);
            return updated;
        } catch (RestClientResponseException ex) {
            throw newKeycloakException(ex, "Failed to update role");
        } catch (RestClientException ex) {
            throw new KeycloakServiceException(HttpStatus.BAD_GATEWAY.value(), "Identity provider is unavailable", ex.getMessage());
        }
    }

    public RoleDto patchRole(String authorization, String id, RoleDto partial) {
        try {
            // Keycloak does not have a dedicated PATCH; use PUT with the merged representation.
            RoleDto existing = getRoleById(authorization, id);
            if (StringUtils.hasText(partial.getName())) existing.setName(partial.getName());
            if (partial.getDescription() != null) existing.setDescription(partial.getDescription());
            if (partial.getAttributes() != null) existing.setAttributes(partial.getAttributes());
            return updateRole(authorization, id, existing);
        } catch (RestClientResponseException ex) {
            throw newKeycloakException(ex, "Failed to patch role");
        }
    }

    public void deleteRole(String authorization, String id) {
        try {
            // Keycloak performs physical deletes. To emulate logical delete we retrieve the role, modify the name
            // and update it prefixing with "DELETED_". Alternatively could set an attribute marking deleted.
            RoleDto existing = getRoleById(authorization, id);
            if (existing == null) {
                throw new KeycloakServiceException(HttpStatus.NOT_FOUND.value(), "Role not found", null);
            }
            existing.setName("DELETED_" + existing.getName());
            // Update role by id
            restClient.put()
                    .uri("/admin/realms/{realm}/roles-by-id/{id}", properties.realm(), id)
                    .headers(h -> h.set("Authorization", authorization))
                    .body(existing)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw newKeycloakException(ex, "Failed to delete(role) logically");
        } catch (RestClientException ex) {
            throw new KeycloakServiceException(HttpStatus.BAD_GATEWAY.value(), "Identity provider is unavailable", ex.getMessage());
        }
    }

    private KeycloakServiceException newKeycloakException(RestClientResponseException ex, String message) {
        String body = ex.getResponseBodyAsString();
        int status = ex.getRawStatusCode();
        return new KeycloakServiceException(status, message + ": " + ex.getMessage(), body);
    }
}
