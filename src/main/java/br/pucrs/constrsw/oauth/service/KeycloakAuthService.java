package br.pucrs.constrsw.oauth.service;

import br.pucrs.constrsw.oauth.config.KeycloakProperties;
import br.pucrs.constrsw.oauth.dto.ErrorStackEntry;
import br.pucrs.constrsw.oauth.dto.LoginResponse;
import br.pucrs.constrsw.oauth.exception.OAuthApiException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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
 * Wraps Keycloak's own OpenID Connect token endpoint to implement POST /login,
 * translating Keycloak's response/errors into this API's contract.
 */
@Service
public class KeycloakAuthService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAuthService.class);
    private static final String SOURCE_KEYCLOAK = "Keycloak";

    private final RestTemplate restTemplate;
    private final KeycloakProperties keycloakProperties;

    public KeycloakAuthService(RestTemplate restTemplate, KeycloakProperties keycloakProperties) {
        this.restTemplate = restTemplate;
        this.keycloakProperties = keycloakProperties;
    }

    public LoginResponse login(String username, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", keycloakProperties.getClientId());
        form.add("client_secret", keycloakProperties.getClientSecret());
        form.add("grant_type", "password");
        form.add("username", username);
        form.add("password", password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    keycloakProperties.tokenEndpoint(),
                    org.springframework.http.HttpMethod.POST,
                    new HttpEntity<>(form, headers),
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});

            return toLoginResponse(response.getBody());
        } catch (HttpStatusCodeException ex) {
            throw translateKeycloakError(ex);
        } catch (ResourceAccessException ex) {
            log.error("Keycloak unreachable at {}", keycloakProperties.tokenEndpoint(), ex);
            throw new OAuthApiException(HttpStatus.SERVICE_UNAVAILABLE, "503",
                    "Nao foi possivel contatar o Keycloak.", SOURCE_KEYCLOAK, ex,
                    List.of(new ErrorStackEntry(ex.getClass().getSimpleName(), ex.getMessage())));
        }
    }

    private LoginResponse toLoginResponse(Map<String, Object> body) {
        if (body == null) {
            throw new OAuthApiException(HttpStatus.BAD_GATEWAY, "502",
                    "Resposta vazia do Keycloak.", SOURCE_KEYCLOAK);
        }
        return new LoginResponse(
                (String) body.get("token_type"),
                (String) body.get("access_token"),
                toLong(body.get("expires_in")),
                (String) body.get("refresh_token"),
                toLong(body.get("refresh_expires_in")));
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof Number ? ((Number) value).longValue() : Long.valueOf(value.toString());
    }

    private OAuthApiException translateKeycloakError(HttpStatusCodeException ex) {
        String keycloakBody = ex.getResponseBodyAsString();
        log.warn("Keycloak token endpoint returned {}: {}", ex.getStatusCode(), keycloakBody);

        // Keycloak answers invalid credentials with invalid_grant (400 or 401 depending on version);
        // the assignment maps that specific case to 401 regardless of what Keycloak used.
        if (keycloakBody != null && keycloakBody.contains("invalid_grant")) {
            return new OAuthApiException(HttpStatus.UNAUTHORIZED, "401",
                    "Username e/ou password invalidos.", SOURCE_KEYCLOAK, ex,
                    List.of(new ErrorStackEntry("KeycloakError", keycloakBody)));
        }

        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.BAD_GATEWAY;
        }
        return new OAuthApiException(status, String.valueOf(ex.getStatusCode().value()),
                "Erro ao autenticar no Keycloak: " + keycloakBody, SOURCE_KEYCLOAK, ex,
                List.of(new ErrorStackEntry("KeycloakError", keycloakBody)));
    }
}
