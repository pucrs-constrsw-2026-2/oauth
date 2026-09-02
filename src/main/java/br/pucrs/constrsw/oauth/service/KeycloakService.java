package br.pucrs.constrsw.oauth.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class KeycloakService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakService.class);

    @Value("${keycloak.url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    private final RestTemplate restTemplate = new RestTemplate();

    // Regras de acesso mapeadas conforme configurado no Keycloak
    private static final Map<String, Set<String>> ROLE_PERMISSIONS = new HashMap<>();

    static {
        ROLE_PERMISSIONS.put("administrator", Set.of("resources", "rooms", "professors", "students"));
        ROLE_PERMISSIONS.put("coordinator", Set.of("courses", "classes"));
        ROLE_PERMISSIONS.put("professor", Set.of("lessons", "reservations"));
        ROLE_PERMISSIONS.put("student", Set.of());
    }

    /**
     * Valida se o token é válido junto ao Keycloak chamando o endpoint de userinfo.
     */
    public boolean isTokenValidWithKeycloak(String rawToken) {
        String token = cleanToken(rawToken);
        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            // Checagem prévia de expiração local no JWT
            DecodedJWT jwt = JWT.decode(token);
            if (jwt.getExpiresAt() != null && jwt.getExpiresAt().before(new Date())) {
                log.warn("Token JWT expirado em: {}", jwt.getExpiresAt());
                return false;
            }

            // Confirmação ativa junto ao Keycloak
            String userInfoUrl = String.format("%s/realms/%s/protocol/openid-connect/userinfo", keycloakUrl, realm);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(userInfoUrl, HttpMethod.GET, entity, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            log.warn("Token rejeitado pelo Keycloak: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Erro ao validar token com Keycloak: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extrai todas as roles (client roles e realm roles) do access token.
     */
    public List<String> extractRoles(String rawToken) {
        String token = cleanToken(rawToken);
        Set<String> roles = new HashSet<>();

        try {
            DecodedJWT jwt = JWT.decode(token);

            // 1. Roles do client (resource_access.{clientId}.roles)
            Claim resourceAccessClaim = jwt.getClaim("resource_access");
            if (!resourceAccessClaim.isNull()) {
                Map<String, Object> resourceAccessMap = resourceAccessClaim.asMap();
                if (resourceAccessMap != null && resourceAccessMap.containsKey(clientId)) {
                    Object clientData = resourceAccessMap.get(clientId);
                    if (clientData instanceof Map<?, ?> clientMap) {
                        Object rolesObj = clientMap.get("roles");
                        if (rolesObj instanceof List<?> list) {
                            for (Object r : list) {
                                roles.add(String.valueOf(r).toLowerCase());
                            }
                        }
                    }
                }
            }

            // 2. Roles do Realm (realm_access.roles)
            Claim realmAccessClaim = jwt.getClaim("realm_access");
            if (!realmAccessClaim.isNull()) {
                Map<String, Object> realmAccessMap = realmAccessClaim.asMap();
                if (realmAccessMap != null && realmAccessMap.containsKey("roles")) {
                    Object rolesObj = realmAccessMap.get("roles");
                    if (rolesObj instanceof List<?> list) {
                        for (Object r : list) {
                            roles.add(String.valueOf(r).toLowerCase());
                        }
                    }
                }
            }
        } catch (JWTDecodeException e) {
            log.error("Falha ao decodificar JWT para extração de roles: {}", e.getMessage());
        }

        return new ArrayList<>(roles);
    }

    /**
     * Extrai o nome de usuário (preferred_username ou email ou sub) do token.
     */
    public String extractUsername(String rawToken) {
        String token = cleanToken(rawToken);
        try {
            DecodedJWT jwt = JWT.decode(token);
            String preferredUsername = jwt.getClaim("preferred_username").asString();
            if (preferredUsername != null && !preferredUsername.isBlank()) {
                return preferredUsername;
            }
            String email = jwt.getClaim("email").asString();
            if (email != null && !email.isBlank()) {
                return email;
            }
            return jwt.getSubject();
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Verifica se alguma das roles do usuário confere acesso ao recurso requisitado.
     */
    public boolean hasAccessToResource(List<String> userRoles, String resource) {
        if (resource == null || resource.isBlank() || userRoles == null || userRoles.isEmpty()) {
            return false;
        }

        String normalizedResource = normalizeResource(resource);

        for (String role : userRoles) {
            Set<String> allowedResources = ROLE_PERMISSIONS.get(role.toLowerCase());
            if (allowedResources != null && allowedResources.contains(normalizedResource)) {
                log.info("Acesso permitido: role '{}' tem permissão para recurso '{}'", role, normalizedResource);
                return true;
            }
        }

        log.warn("Acesso negado: nenhuma das roles {} tem permissão para o recurso '{}'", userRoles, normalizedResource);
        return false;
    }

    public String normalizeResource(String resource) {
        if (resource == null) return "";
        return resource.trim().replace("/", "").toLowerCase();
    }

    private String cleanToken(String rawToken) {
        if (rawToken == null) return null;
        if (rawToken.startsWith("Bearer ") || rawToken.startsWith("bearer ")) {
            return rawToken.substring(7).trim();
        }
        return rawToken.trim();
    }
}
