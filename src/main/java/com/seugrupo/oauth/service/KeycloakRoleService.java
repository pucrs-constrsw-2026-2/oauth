package com.seugrupo.oauth.service;

import com.seugrupo.oauth.config.KeycloakProperties;
import com.seugrupo.oauth.dto.CreateRoleRequest;
import com.seugrupo.oauth.dto.PatchRoleRequest;
import com.seugrupo.oauth.dto.RoleResponse;
import com.seugrupo.oauth.dto.UpdateRoleRequest;
import com.seugrupo.oauth.exception.KeycloakErrorMapper;
import com.seugrupo.oauth.exception.OAuthApiException;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

@Service
public class KeycloakRoleService {

    private static final String ERROR_SOURCE = "OAuthAPI.Roles";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final WebClient keycloakWebClient;
    private final KeycloakProperties properties;

    public KeycloakRoleService(WebClient keycloakWebClient, KeycloakProperties properties) {
        this.keycloakWebClient = keycloakWebClient;
        this.properties = properties;
    }

    public List<RoleResponse> list(String authorization) {
        return execute(() -> keycloakWebClient.get()
                .uri(properties.getAdminRolesUrl())
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .bodyToFlux(RoleResponse.class)
                .collectList()
                .timeout(REQUEST_TIMEOUT)
                .block());
    }

    public RoleResponse getById(String authorization, String id) {
        return list(authorization).stream()
                .filter(role -> Objects.equals(id, role.id()))
                .findFirst()
                .orElseThrow(() -> new OAuthApiException(
                        HttpStatus.NOT_FOUND,
                        "OA-404",
                        ERROR_SOURCE,
                        "Role não localizada no Keycloak."
                ));
    }

    public RoleResponse create(String authorization, CreateRoleRequest request) {
        post(authorization, roleBody(request.name(), request.description()));
        return getByName(authorization, request.name());
    }

    public void update(String authorization, String id, UpdateRoleRequest request) {
        RoleResponse current = getById(authorization, id);
        putByCurrentName(authorization, current.name(), request.name(), request.description());
    }

    public void patch(String authorization, String id, PatchRoleRequest request) {
        RoleResponse current = getById(authorization, id);
        String nextName = request.name() == null ? current.name() : request.name();
        String nextDescription = request.description() == null
                ? current.description() : request.description();
        putByCurrentName(authorization, current.name(), nextName, nextDescription);
    }

    /**
     * Realm roles do not have an enabled flag in Keycloak, so deletion is physical.
     */
    public void delete(String authorization, String id) {
        RoleResponse current = getById(authorization, id);
        execute(() -> {
            keycloakWebClient.delete()
                    .uri(roleUri(current.name()))
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(REQUEST_TIMEOUT)
                    .block();
            return null;
        });
    }

    public void assignToUser(String authorization, String userId, String roleId) {
        sendMapping(HttpMethod.POST, authorization, userId, getById(authorization, roleId));
    }

    public void unassignFromUser(String authorization, String userId, String roleId) {
        sendMapping(HttpMethod.DELETE, authorization, userId, getById(authorization, roleId));
    }

    private void sendMapping(
            HttpMethod method,
            String authorization,
            String userId,
            RoleResponse role
    ) {
        execute(() -> {
            keycloakWebClient.method(method)
                    .uri(mappingUri(userId))
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(List.of(role))
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(REQUEST_TIMEOUT)
                    .block();
            return null;
        });
    }

    private RoleResponse getByName(String authorization, String name) {
        return execute(() -> keycloakWebClient.get()
                .uri(roleUri(name))
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .bodyToMono(RoleResponse.class)
                .switchIfEmpty(Mono.error(upstreamFailure()))
                .timeout(REQUEST_TIMEOUT)
                .block());
    }

    private void post(String authorization, Map<String, Object> body) {
        execute(() -> {
            keycloakWebClient.post()
                    .uri(properties.getAdminRolesUrl())
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(REQUEST_TIMEOUT)
                    .block();
            return null;
        });
    }

    private void putByCurrentName(
            String authorization,
            String currentName,
            String nextName,
            String description
    ) {
        execute(() -> {
            keycloakWebClient.put()
                    .uri(roleUri(currentName))
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(roleBody(nextName, description))
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(REQUEST_TIMEOUT)
                    .block();
            return null;
        });
    }

    private Map<String, Object> roleBody(String name, String description) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        if (description != null) {
            body.put("description", description);
        }
        return body;
    }

    private URI roleUri(String name) {
        return UriComponentsBuilder.fromUriString(properties.getAdminRolesUrl())
                .pathSegment(name)
                .build()
                .encode()
                .toUri();
    }

    private URI mappingUri(String userId) {
        return UriComponentsBuilder.fromUriString(properties.getAdminUsersUrl())
                .pathSegment(userId, "role-mappings", "realm")
                .build()
                .encode()
                .toUri();
    }

    private <T> T execute(Supplier<T> request) {
        try {
            return request.get();
        } catch (WebClientResponseException ex) {
            throw KeycloakErrorMapper.map(ex, ERROR_SOURCE);
        } catch (WebClientRequestException | DecodingException ex) {
            throw upstreamFailure();
        } catch (RuntimeException ex) {
            if (Exceptions.unwrap(ex) instanceof TimeoutException) {
                throw upstreamFailure();
            }
            throw ex;
        }
    }

    private OAuthApiException upstreamFailure() {
        return new OAuthApiException(
                HttpStatus.BAD_GATEWAY,
                "OA-502",
                ERROR_SOURCE,
                "Falha ao comunicar com o Keycloak."
        );
    }
}
