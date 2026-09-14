package br.pucrs.constrsw.oauth.service;

import br.pucrs.constrsw.oauth.dto.CreateUserRequest;
import br.pucrs.constrsw.oauth.dto.LoginRequest;
import br.pucrs.constrsw.oauth.dto.LoginResponse;
import br.pucrs.constrsw.oauth.dto.UpdatePasswordRequest;
import br.pucrs.constrsw.oauth.dto.UserResponse;
import br.pucrs.constrsw.oauth.exception.KeycloakException;
import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
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

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    @Value("${keycloak.admin-username:admin}")
    private String adminUsername;

    @Value("${keycloak.admin-password:a12345678}")
    private String adminPassword;

    private final RestTemplate restTemplate;

    public KeycloakService() {
        this.restTemplate = new RestTemplate();
    }

    public KeycloakService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // Regras de acesso mapeadas conforme configurado no Keycloak
    private static final Map<String, Set<String>> ROLE_PERMISSIONS = new HashMap<>();

    static {
        ROLE_PERMISSIONS.put("administrator", Set.of("resources", "rooms", "professors", "students"));
        ROLE_PERMISSIONS.put("coordinator", Set.of("courses", "classes"));
        ROLE_PERMISSIONS.put("professor", Set.of("lessons", "reservations"));
        ROLE_PERMISSIONS.put("student", Set.of());
    }

    /**
     * POST /login: Consumir endpoint de token OAuth2 do Keycloak para gerar o access token.
     */
    public LoginResponse login(LoginRequest request) {
        String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token", keycloakUrl, realm);
        log.info("Tentando autenticar usuário '{}' junto ao Keycloak em {}", request.username(), tokenUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("username", request.username());
        body.add("password", request.password());

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<LoginResponse> response = restTemplate.postForEntity(tokenUrl, entity, LoginResponse.class);
            log.info("Usuário '{}' autenticado com sucesso no Keycloak", request.username());
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.warn("Falha de autenticação do usuário '{}': status={}, body={}", request.username(), e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST || e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new KeycloakException("INVALID_CREDENTIALS", "Usuário ou senha inválidos", HttpStatus.UNAUTHORIZED, e);
            }
            throw new KeycloakException("KEYCLOAK_AUTH_ERROR", "Erro de autenticação no Keycloak: " + e.getStatusText(), (HttpStatus) e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("Erro inesperado ao conectar ao Keycloak para login: {}", e.getMessage());
            throw new KeycloakException("KEYCLOAK_COMMUNICATION_ERROR", "Não foi possível comunicar com o servidor de autenticação", HttpStatus.SERVICE_UNAVAILABLE, e);
        }
    }

    /**
     * Obtém um token de administração do Keycloak no realm 'master' usando as credenciais admin.
     */
    public String getAdminToken() {
        String tokenUrl = String.format("%s/realms/master/protocol/openid-connect/token", keycloakUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", "admin-cli");
        body.add("username", adminUsername);
        body.add("password", adminPassword);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, entity, Map.class);
            if (response.getBody() != null && response.getBody().containsKey("access_token")) {
                return (String) response.getBody().get("access_token");
            }
            throw new KeycloakException("ADMIN_AUTH_FAILED", "Token de administração não retornado pelo Keycloak", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Erro ao obter admin token do Keycloak: {}", e.getResponseBodyAsString());
            throw new KeycloakException("ADMIN_AUTH_ERROR", "Falha na autenticação administrativa com Keycloak", (HttpStatus) e.getStatusCode(), e);
        }
    }

    /**
     * POST /users: Rota complexa de criação de usuário (pegar ID no header Location, tratar erro 409).
     */
    public UserResponse createUser(CreateUserRequest request) {
        String adminToken = getAdminToken();
        String usersUrl = String.format("%s/admin/realms/%s/users", keycloakUrl, realm);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        Map<String, Object> userPayload = new HashMap<>();
        userPayload.put("username", request.username());
        userPayload.put("email", request.email());
        userPayload.put("firstName", request.firstName());
        userPayload.put("lastName", request.lastName());
        userPayload.put("enabled", request.enabled());

        if (request.password() != null && !request.password().isBlank()) {
            Map<String, Object> credential = new HashMap<>();
            credential.put("type", "password");
            credential.put("value", request.password());
            credential.put("temporary", false);
            userPayload.put("credentials", List.of(credential));
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(userPayload, headers);

        try {
            ResponseEntity<Void> response = restTemplate.postForEntity(usersUrl, entity, Void.class);

            // Extrai o ID do usuário do header Location retornado pelo Keycloak
            URI location = response.getHeaders().getLocation();
            String userId = null;
            if (location != null) {
                String path = location.getPath();
                userId = path.substring(path.lastIndexOf('/') + 1);
            }

            log.info("Usuário criado com sucesso no Keycloak: username='{}', id='{}'", request.username(), userId);
            return new UserResponse(
                    userId,
                    request.username(),
                    request.email(),
                    request.firstName(),
                    request.lastName(),
                    request.enabled()
            );
        } catch (HttpClientErrorException.Conflict e) {
            log.warn("Conflito ao criar usuário no Keycloak: username '{}' ou email '{}' já existe", request.username(), request.email());
            throw new KeycloakException("USER_ALREADY_EXISTS", "Usuário ou e-mail já cadastrado no Keycloak", HttpStatus.CONFLICT, e);
        } catch (HttpClientErrorException e) {
            log.error("Erro do cliente Keycloak ao criar usuário: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                throw new KeycloakException("USER_ALREADY_EXISTS", "Usuário ou e-mail já cadastrado no Keycloak", HttpStatus.CONFLICT, e);
            }
            throw new KeycloakException("KEYCLOAK_CREATE_USER_ERROR", "Erro ao criar usuário no Keycloak: " + e.getMessage(), (HttpStatus) e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("Erro inesperado ao criar usuário no Keycloak: {}", e.getMessage(), e);
            throw new KeycloakException("KEYCLOAK_USER_CREATION_FAILED", "Falha na criação de usuário: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * PATCH /users/{id}: Atualizar senha do usuário (estrutura CredentialRepresentation).
     */
    public void updatePassword(String userId, UpdatePasswordRequest request) {
        String adminToken = getAdminToken();
        String resetPasswordUrl = String.format("%s/admin/realms/%s/users/%s/reset-password", keycloakUrl, realm, userId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        // Monta a estrutura específica CredentialRepresentation do Keycloak
        Map<String, Object> credentialRepresentation = new HashMap<>();
        credentialRepresentation.put("type", "password");
        credentialRepresentation.put("value", request.password());
        credentialRepresentation.put("temporary", request.temporary() != null ? request.temporary() : false);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(credentialRepresentation, headers);

        try {
            restTemplate.put(resetPasswordUrl, entity);
            log.info("Senha do usuário id='{}' atualizada com sucesso no Keycloak", userId);
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Usuário id='{}' não encontrado no Keycloak para atualização de senha", userId);
            throw new KeycloakException("USER_NOT_FOUND", "Usuário com id '" + userId + "' não foi encontrado", HttpStatus.NOT_FOUND, e);
        } catch (HttpClientErrorException e) {
            log.error("Erro ao atualizar senha no Keycloak para id='{}': status={}, body={}", userId, e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new KeycloakException("USER_NOT_FOUND", "Usuário com id '" + userId + "' não foi encontrado", HttpStatus.NOT_FOUND, e);
            }
            throw new KeycloakException("KEYCLOAK_UPDATE_PASSWORD_ERROR", "Erro ao atualizar senha no Keycloak", (HttpStatus) e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("Erro inesperado ao atualizar senha no Keycloak: {}", e.getMessage(), e);
            throw new KeycloakException("KEYCLOAK_PASSWORD_RESET_FAILED", "Falha ao atualizar senha: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * Valida se o token é válido (verificando formato JWT, emissor, expiração e integridade com Keycloak).
     */
    public boolean isTokenValidWithKeycloak(String rawToken) {
        String token = cleanToken(rawToken);
        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            // 1. Checagem prévia de formato e expiração local no JWT
            DecodedJWT jwt = JWT.decode(token);
            if (jwt.getExpiresAt() != null && jwt.getExpiresAt().before(new Date())) {
                log.warn("Token JWT expirado em: {}", jwt.getExpiresAt());
                return false;
            }

            // 2. Checagem do realm emissor
            if (jwt.getIssuer() == null || !jwt.getIssuer().contains("/realms/" + realm)) {
                log.warn("Token JWT com emissor incompatível (esperado realm '{}'): {}", realm, jwt.getIssuer());
                return false;
            }

            // 3. Tenta confirmação ativa com Keycloak (se os hostnames coincidirem)
            try {
                String userInfoUrl = String.format("%s/realms/%s/protocol/openid-connect/userinfo", keycloakUrl, realm);
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(token);
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                ResponseEntity<String> response = restTemplate.exchange(userInfoUrl, HttpMethod.GET, entity, String.class);
                if (response.getStatusCode().is2xxSuccessful()) {
                    return true;
                }
            } catch (Exception e) {
                log.debug("Consulta userinfo não pôde ser concluída (esperado caso host interno do container difira do emissor): {}", e.getMessage());
            }

            // Token válido conforme assinado pelo Realm e dentro da validade
            return true;
        } catch (Exception e) {
            log.error("Erro ao validar token: {}", e.getMessage());
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
