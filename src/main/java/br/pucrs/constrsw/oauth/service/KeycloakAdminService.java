package br.pucrs.constrsw.oauth.service;

import br.pucrs.constrsw.oauth.config.KeycloakProperties;
import br.pucrs.constrsw.oauth.dto.ErrorStackEntry;
import br.pucrs.constrsw.oauth.exception.OAuthApiException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * Obtains and caches a Keycloak master-realm admin access token so the Users and Roles
 * controllers (built by the rest of the group) can call Keycloak's Admin REST API
 * ({@code {{base-url}}/admin/realms/{{realm}}/...}) without each reimplementing token
 * management. The oauth client's own service account is only granted view/manage-clients
 * on realm-management (see keycloak/realm-export.json), not manage-users/manage-realm,
 * so the Admin API has to be called as the Keycloak bootstrap admin (KEYCLOAK_ADMIN /
 * KEYCLOAK_ADMIN_PASSWORD, already wired into the container by docker-compose.yml)
 * authenticating against the built-in admin-cli client of the master realm.
 *
 * Usage from a Users/Roles service: inject this bean, call {@link #adminAuthHeaders()}
 * for the Authorization header and {@link #adminApiBaseUrl()} for the base URL, then
 * append the specific path (e.g. "/users", "/roles/{id}").
 */
@Service
public class KeycloakAdminService {

    private static final String ADMIN_CLIENT_ID = "admin-cli";
    private static final String SOURCE_KEYCLOAK = "Keycloak";

    private final RestTemplate restTemplate;
    private final KeycloakProperties keycloakProperties;

    private volatile String cachedToken;
    private volatile Instant expiresAt = Instant.EPOCH;

    public KeycloakAdminService(RestTemplate restTemplate, KeycloakProperties keycloakProperties) {
        this.restTemplate = restTemplate;
        this.keycloakProperties = keycloakProperties;
    }

    public synchronized String getAdminAccessToken() {
        if (cachedToken != null && Instant.now().isBefore(expiresAt)) {
            return cachedToken;
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", ADMIN_CLIENT_ID);
        form.add("grant_type", "password");
        form.add("username", keycloakProperties.getAdminUsername());
        form.add("password", keycloakProperties.getAdminPassword());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    keycloakProperties.masterTokenEndpoint(),
                    HttpMethod.POST,
                    new HttpEntity<>(form, headers),
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});

            Map<String, Object> body = response.getBody();
            if (body == null || body.get("access_token") == null) {
                throw new OAuthApiException(HttpStatus.BAD_GATEWAY, "502",
                        "Resposta vazia do Keycloak ao obter token de administrador.", SOURCE_KEYCLOAK);
            }

            cachedToken = (String) body.get("access_token");
            long expiresIn = ((Number) body.getOrDefault("expires_in", 60)).longValue();
            expiresAt = Instant.now().plusSeconds(Math.max(5, expiresIn - 10));
            return cachedToken;
        } catch (HttpStatusCodeException ex) {
            throw new OAuthApiException(HttpStatus.BAD_GATEWAY, String.valueOf(ex.getStatusCode().value()),
                    "Nao foi possivel autenticar como administrador no Keycloak.", SOURCE_KEYCLOAK, ex,
                    List.of(new ErrorStackEntry("KeycloakError", ex.getResponseBodyAsString())));
        } catch (ResourceAccessException ex) {
            throw new OAuthApiException(HttpStatus.SERVICE_UNAVAILABLE, "503",
                    "Nao foi possivel contatar o Keycloak.", SOURCE_KEYCLOAK, ex,
                    List.of(new ErrorStackEntry(ex.getClass().getSimpleName(), ex.getMessage())));
        }
    }

    /** Ready-to-use headers (Authorization + Content-Type: application/json) for calling the Admin API. */
    public HttpHeaders adminAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAdminAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /** e.g. http://keycloak:8080/admin/realms/constrsw */
    public String adminApiBaseUrl() {
        return keycloakProperties.adminApiBaseUrl();
    }
}
