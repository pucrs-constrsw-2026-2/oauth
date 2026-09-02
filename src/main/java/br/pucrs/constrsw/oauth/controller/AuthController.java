package br.pucrs.constrsw.oauth.controller;

import br.pucrs.constrsw.oauth.dto.ValidateRequest;
import br.pucrs.constrsw.oauth.dto.ValidateResponse;
import br.pucrs.constrsw.oauth.service.KeycloakService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@CrossOrigin(origins = "*")
@Tag(name = "Authorization", description = "Endpoints para validação de access token e permissões de acesso a recursos")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final KeycloakService keycloakService;

    public AuthController(KeycloakService keycloakService) {
        this.keycloakService = keycloakService;
    }

    @Operation(summary = "Valida access token e permissão a recurso via POST",
               description = "Valida o access token JWT junto ao Keycloak e verifica se o role do usuário permite acesso ao recurso informado no corpo da requisição.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Acesso permitido ao recurso"),
            @ApiResponse(responseCode = "403", description = "Acesso proibido ou token inválido/expirado")
    })
    @PostMapping(value = {"/validate", "/authorize"})
    public ResponseEntity<ValidateResponse> validateAccessPost(
            @Parameter(description = "Header com o access token Bearer", required = false)
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) ValidateRequest request) {

        String resource = (request != null) ? request.getResource() : null;
        return executeValidation(authHeader, resource);
    }

    @Operation(summary = "Valida access token e permissão a recurso via GET",
               description = "Valida o access token JWT junto ao Keycloak e verifica se o role do usuário permite acesso ao recurso informado via query parameter.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Acesso permitido ao recurso"),
            @ApiResponse(responseCode = "403", description = "Acesso proibido ou token inválido/expirado")
    })
    @GetMapping(value = {"/validate", "/authorize"})
    public ResponseEntity<ValidateResponse> validateAccessGet(
            @Parameter(description = "Header com o access token Bearer", required = false)
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Parameter(description = "Nome do recurso (ex: lessons, rooms, courses)", required = false)
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
