package com.constrsw.oauth.service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.constrsw.oauth.config.LegacyKeycloakProperties;
import com.constrsw.oauth.dto.LoginResponse;
import com.constrsw.oauth.dto.UserCreateRequest;
import com.constrsw.oauth.dto.UserResponse;
import com.constrsw.oauth.dto.UserUpdateRequest;
import com.constrsw.oauth.exception.BadRequestException;
import com.constrsw.oauth.exception.ConflictException;
import com.constrsw.oauth.exception.ForbiddenException;
import com.constrsw.oauth.exception.KeycloakUnavailableException;
import com.constrsw.oauth.exception.NotFoundException;
import com.constrsw.oauth.exception.UnauthorizedException;

/**
 * Camada de integracao com a REST API do Keycloak.
 * - login()          POST /realms/{realm}/protocol/openid-connect/token
 * - createUser()     POST /admin/realms/{realm}/users
 * - listUsers()      GET  /admin/realms/{realm}/users
 * - getUser()        GET  /admin/realms/{realm}/users/{id}
 * - updateUser()     PUT  /admin/realms/{realm}/users/{id}
 * - updatePassword() PUT  /admin/realms/{realm}/users/{id}/reset-password
 * - disableUser()    PUT  /admin/realms/{realm}/users/{id}  (enabled=false)
 *
 * Os status HTTP retornados pelo Keycloak sao mapeados nas excecoes especificas
 * (BadRequest/Unauthorized/Forbidden/NotFound/Conflict) para casar com o
 * contrato do enunciado.
 */
@Service
public class KeycloakService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakService.class);

    private final LegacyKeycloakProperties props;
    private final RestTemplate rest;

    public KeycloakService(LegacyKeycloakProperties props, RestTemplate keycloakRestTemplate) {
        this.props = props;
        this.rest = keycloakRestTemplate;
    }

    // -----------------------------------------------------------------------
    // Autenticacao
    // -----------------------------------------------------------------------

    /**
     * Executa o password grant no Keycloak e devolve o payload de tokens.
     */
    public LoginResponse login(String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", props.getClientId());
        form.add("client_secret", props.getClientSecret());
        form.add("username", username);
        form.add("password", password);

        ResponseEntity<Map> response = exchange(
                props.tokenEndpoint(), HttpMethod.POST,
                new HttpEntity<>(form, headers), Map.class);

        HttpStatusCode status = response.getStatusCode();
        if (status.is2xxSuccessful()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new KeycloakUnavailableException("Empty token response from Keycloak");
            }
            LoginResponse out = new LoginResponse();
            out.setTokenType((String) body.get("token_type"));
            out.setAccessToken((String) body.get("access_token"));
            out.setExpiresIn(asLong(body.get("expires_in")));
            out.setRefreshToken((String) body.get("refresh_token"));
            out.setRefreshExpiresIn(asLong(body.get("refresh_expires_in")));
            return out;
        }

        // Keycloak 26 devolve HTTP 400 + error="invalid_grant" para credenciais
        // invalidas (nao ha mais 401). Convertemos essa condicao especifica em
        // 401 Unauthorized conforme exigido pelo enunciado; outros 400 seguem
        // como Bad Request. Alguns runtimes ainda podem responder 401 direto,
        // que tambem tratamos como Unauthorized.
        Object body = response.getBody();
        String errorCode = extractField(body, "error");
        if (status.value() == HttpStatus.UNAUTHORIZED.value()
                || "invalid_grant".equalsIgnoreCase(errorCode)) {
            throw new UnauthorizedException("Invalid username or password");
        }
        if (status.value() == HttpStatus.BAD_REQUEST.value()) {
            throw new BadRequestException(errorDescription(body, "Bad login request"));
        }
        throw new KeycloakUnavailableException("Unexpected Keycloak response: " + status);
    }

    private String extractField(Object body, String key) {
        if (body instanceof Map<?, ?> map) {
            Object v = map.get(key);
            return v == null ? null : v.toString();
        }
        return null;
    }

    // -----------------------------------------------------------------------
    // Usuarios
    // -----------------------------------------------------------------------

    /**
     * Cria um usuario no Keycloak e devolve o id extraido do header Location.
     */
    public UserResponse createUser(String bearer, UserCreateRequest req) {
        Map<String, Object> body = new HashMap<>();
        body.put("username", req.getUsername());
        body.put("email", req.getUsername()); // username = e-mail (spec)
        body.put("firstName", req.getFirstName());
        body.put("lastName", req.getLastName());
        body.put("enabled", true);
        body.put("emailVerified", true);

        Map<String, Object> credential = new HashMap<>();
        credential.put("type", "password");
        credential.put("value", req.getPassword());
        credential.put("temporary", false);
        body.put("credentials", Collections.singletonList(credential));

        ResponseEntity<Map> response = exchange(
                props.usersEndpoint(), HttpMethod.POST,
                new HttpEntity<>(body, authHeaders(bearer, MediaType.APPLICATION_JSON)),
                Map.class);

        HttpStatusCode status = response.getStatusCode();
        if (status.value() == HttpStatus.CREATED.value()) {
            String id = extractIdFromLocation(response.getHeaders().getLocation());
            if (id == null) {
                throw new KeycloakUnavailableException("Keycloak did not return a Location header");
            }
            return new UserResponse(id, req.getUsername(), req.getFirstName(),
                    req.getLastName(), Boolean.TRUE);
        }
        mapErrorStatus(status, response.getBody(), "Failed to create user");
        return null; // unreachable
    }

    /**
     * Lista usuarios, opcionalmente filtrando por enabled.
     */
    @SuppressWarnings("unchecked")
    public List<UserResponse> listUsers(String bearer, Boolean enabled) {
        UriComponentsBuilder ub = UriComponentsBuilder.fromUriString(props.usersEndpoint())
                .queryParam("briefRepresentation", true)
                // Keycloak nao tem limite default; pedimos uma janela larga.
                .queryParam("first", 0)
                .queryParam("max", 1000);
        if (enabled != null) {
            ub.queryParam("enabled", enabled);
        }

        ResponseEntity<List> response = exchange(
                ub.build(true).toUriString(), HttpMethod.GET,
                new HttpEntity<>(authHeaders(bearer, null)), List.class);

        HttpStatusCode status = response.getStatusCode();
        if (status.is2xxSuccessful()) {
            List<Map<String, Object>> raw = (List<Map<String, Object>>) response.getBody();
            List<UserResponse> out = new ArrayList<>();
            if (raw != null) {
                for (Map<String, Object> u : raw) {
                    out.add(toUserResponse(u));
                }
            }
            // Fallback defensivo: se a instalacao do Keycloak ignorar o filtro
            // "enabled" (algumas versoes fazem isso quando briefRepresentation
            // esta ligado), aplicamos client-side.
            if (enabled != null) {
                out.removeIf(u -> u.getEnabled() == null || !u.getEnabled().equals(enabled));
            }
            return out;
        }
        mapErrorStatus(status, response.getBody(), "Failed to list users");
        return Collections.emptyList(); // unreachable
    }

    public UserResponse getUser(String bearer, String id) {
        ResponseEntity<Map> response = exchange(
                props.userEndpoint(id), HttpMethod.GET,
                new HttpEntity<>(authHeaders(bearer, null)), Map.class);

        HttpStatusCode status = response.getStatusCode();
        if (status.is2xxSuccessful()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> u = response.getBody();
            if (u == null) throw new NotFoundException("User not found: " + id);
            return toUserResponse(u);
        }
        mapErrorStatus(status, response.getBody(), "Failed to get user");
        return null; // unreachable
    }

    /**
     * Atualiza atributos de um usuario (PUT no Keycloak; envia so os campos
     * presentes na request).
     */
    public void updateUser(String bearer, String id, UserUpdateRequest req) {
        Map<String, Object> body = new HashMap<>();
        if (req.getUsername() != null) {
            body.put("username", req.getUsername());
            body.put("email", req.getUsername());
        }
        if (req.getFirstName() != null) body.put("firstName", req.getFirstName());
        if (req.getLastName() != null) body.put("lastName", req.getLastName());
        if (req.getEnabled() != null) body.put("enabled", req.getEnabled());

        if (body.isEmpty()) {
            throw new BadRequestException("No fields to update");
        }

        ResponseEntity<Map> response = exchange(
                props.userEndpoint(id), HttpMethod.PUT,
                new HttpEntity<>(body, authHeaders(bearer, MediaType.APPLICATION_JSON)),
                Map.class);

        HttpStatusCode status = response.getStatusCode();
        // Keycloak retorna 204 (No Content) em PUT com sucesso.
        if (status.is2xxSuccessful()) return;
        mapErrorStatus(status, response.getBody(), "Failed to update user");
    }

    /**
     * Atualiza somente a senha (endpoint dedicado /reset-password do Keycloak).
     */
    public void updatePassword(String bearer, String id, String newPassword) {
        Map<String, Object> body = new HashMap<>();
        body.put("type", "password");
        body.put("value", newPassword);
        body.put("temporary", false);

        ResponseEntity<Map> response = exchange(
                props.resetPasswordEndpoint(id), HttpMethod.PUT,
                new HttpEntity<>(body, authHeaders(bearer, MediaType.APPLICATION_JSON)),
                Map.class);

        HttpStatusCode status = response.getStatusCode();
        if (status.is2xxSuccessful()) return;
        mapErrorStatus(status, response.getBody(), "Failed to update password");
    }

    /**
     * Desabilita (exclusao logica) o usuario: PUT enabled=false. Se o usuario
     * nao existir, o Keycloak devolve 404 e mapeamos para NotFoundException.
     */
    public void disableUser(String bearer, String id) {
        // Antes de fazer PUT, confirmamos existencia para poder retornar 404
        // adequadamente mesmo quando o PUT abaixo, por algum motivo, devolveria
        // outro status.
        UserResponse existing = getUser(bearer, id);

        Map<String, Object> body = new HashMap<>();
        body.put("enabled", Boolean.FALSE);

        ResponseEntity<Map> response = exchange(
                props.userEndpoint(existing.getId()), HttpMethod.PUT,
                new HttpEntity<>(body, authHeaders(bearer, MediaType.APPLICATION_JSON)),
                Map.class);

        HttpStatusCode status = response.getStatusCode();
        if (status.is2xxSuccessful()) return;
        mapErrorStatus(status, response.getBody(), "Failed to disable user");
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private HttpHeaders authHeaders(String bearer, MediaType contentType) {
        if (bearer == null || bearer.isBlank()) {
            throw new UnauthorizedException("Missing bearer token");
        }
        HttpHeaders h = new HttpHeaders();
        h.set(HttpHeaders.AUTHORIZATION, bearer.startsWith("Bearer ") ? bearer : "Bearer " + bearer);
        if (contentType != null) h.setContentType(contentType);
        h.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return h;
    }

    /**
     * O Keycloak devolve o id do novo usuario no header Location, no formato
     * .../admin/realms/{realm}/users/{id}.
     */
    private String extractIdFromLocation(URI location) {
        if (location == null) return null;
        String path = location.getPath();
        int idx = path.lastIndexOf('/');
        return (idx >= 0 && idx < path.length() - 1) ? path.substring(idx + 1) : null;
    }

    private UserResponse toUserResponse(Map<String, Object> u) {
        return new UserResponse(
                (String) u.get("id"),
                (String) u.getOrDefault("username", u.get("email")),
                (String) u.get("firstName"),
                (String) u.get("lastName"),
                (Boolean) u.getOrDefault("enabled", Boolean.TRUE));
    }

    private Long asLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(o.toString()); }
        catch (NumberFormatException e) { return null; }
    }

    /**
     * Extrai a mensagem de erro do body do Keycloak. Como as respostas de erro
     * do endpoint de token vem como Map e algumas outras rotas Admin podem
     * devolver List/Map, aceitamos Object e testamos o formato.
     */
    private String errorDescription(Object body, String fallback) {
        if (body instanceof Map<?, ?> map) {
            Object d = map.get("error_description");
            if (d != null) return d.toString();
            Object m = map.get("errorMessage");
            if (m != null) return m.toString();
            Object e = map.get("error");
            if (e != null) return e.toString();
        }
        return fallback;
    }

    private void mapErrorStatus(HttpStatusCode status, Object body, String defaultMessage) {
        String description = errorDescription(body, defaultMessage);
        int code = status.value();
        if (code == 400) throw new BadRequestException(description);
        if (code == 401) throw new UnauthorizedException("Invalid or expired access token");
        if (code == 403) throw new ForbiddenException("Access token does not grant permission for this operation");
        if (code == 404) throw new NotFoundException("Resource not found");
        if (code == 409) throw new ConflictException("Username already exists");
        throw new KeycloakUnavailableException("Unexpected Keycloak response " + code + ": " + description);
    }

    private <T> ResponseEntity<T> exchange(String url, HttpMethod method,
                                           HttpEntity<?> entity, Class<T> respType) {
        try {
            return rest.exchange(url, method, entity, respType);
        } catch (ResourceAccessException ex) {
            log.error("Keycloak indisponivel em {}: {}", url, ex.getMessage());
            throw new KeycloakUnavailableException("Cannot reach Keycloak at " + url);
        }
    }

    // Deixado como utilitario para depuracao/queries futuras.
    @SuppressWarnings("unused")
    private String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
