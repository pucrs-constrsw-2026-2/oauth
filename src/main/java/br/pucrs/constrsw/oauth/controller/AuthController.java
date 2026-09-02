package br.pucrs.constrsw.oauth.controller;

import br.pucrs.constrsw.oauth.dto.ValidateRequest;
import br.pucrs.constrsw.oauth.dto.ValidateResponse;
import br.pucrs.constrsw.oauth.service.KeycloakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@CrossOrigin(origins = "*")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final KeycloakService keycloakService;

    public AuthController(KeycloakService keycloakService) {
        this.keycloakService = keycloakService;
    }

    /**
     * Endpoint para validar access token e autorização a recurso (POST).
     */
    @PostMapping(value = {"/validate", "/authorize"})
    public ResponseEntity<ValidateResponse> validateAccessPost(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) ValidateRequest request) {

        String resource = (request != null) ? request.getResource() : null;
        return executeValidation(authHeader, resource);
    }

    /**
     * Endpoint para validar access token e autorização a recurso (GET via query param).
     */
    @GetMapping(value = {"/validate", "/authorize"})
    public ResponseEntity<ValidateResponse> validateAccessGet(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "resource", required = false) String resource) {

        return executeValidation(authHeader, resource);
    }

    private ResponseEntity<ValidateResponse> executeValidation(String authHeader, String resource) {
        if (authHeader == null || authHeader.isBlank()) {
            log.warn("Tentativa de validação sem cabeçalho Authorization");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ValidateResponse.forbidden("Authorization header is missing", null, resource, List.of()));
        }

        if (resource == null || resource.isBlank()) {
            log.warn("Tentativa de validação sem informar o recurso");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ValidateResponse.forbidden("Resource must be specified", null, null, List.of()));
        }

        // 1. Valida o access token com o Keycloak
        boolean isTokenValid = keycloakService.isTokenValidWithKeycloak(authHeader);
        if (!isTokenValid) {
            log.warn("Access token inválido ou expirado");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ValidateResponse.forbidden("Invalid or expired access token", null, resource, List.of()));
        }

        // 2. Extrai dados do usuário e roles
        String username = keycloakService.extractUsername(authHeader);
        List<String> roles = keycloakService.extractRoles(authHeader);

        // 3. Verifica se algum dos roles dá acesso ao resource requisitado
        boolean hasAccess = keycloakService.hasAccessToResource(roles, resource);

        if (hasAccess) {
            log.info("Usuário '{}' autorizado com sucesso para o recurso '{}'", username, resource);
            return ResponseEntity.ok(ValidateResponse.allowed(username, resource, roles));
        } else {
            log.warn("Usuário '{}' não possui role para acessar o recurso '{}'. Roles: {}", username, resource, roles);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ValidateResponse.forbidden("User roles do not grant access to the requested resource", username, resource, roles));
        }
    }
}
