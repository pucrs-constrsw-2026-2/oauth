package com.constrsw.oauth.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.constrsw.oauth.dto.PasswordUpdateRequest;
import com.constrsw.oauth.dto.UserCreateRequest;
import com.constrsw.oauth.dto.UserResponse;
import com.constrsw.oauth.dto.UserUpdateRequest;
import com.constrsw.oauth.exception.BadRequestException;
import com.constrsw.oauth.service.KeycloakService;
import com.constrsw.oauth.util.EmailValidator;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Endpoints REST de USERS. Autorizacao vem do Bearer no header - o proprio
 * Keycloak valida quando repassamos essa credencial nas chamadas Admin API.
 */
@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "Gestao de usuarios (proxy do Keycloak Admin API)")
public class UserController {

    private final KeycloakService keycloak;

    public UserController(KeycloakService keycloak) {
        this.keycloak = keycloak;
    }

    // -----------------------------------------------------------------------
    // POST /users - criar usuario
    // -----------------------------------------------------------------------
    @Operation(summary = "Cria um novo usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Erro na estrutura ou e-mail invalido"),
            @ApiResponse(responseCode = "401", description = "Access token invalido"),
            @ApiResponse(responseCode = "403", description = "Sem permissao"),
            @ApiResponse(responseCode = "409", description = "Username ja existente")
    })
    @PostMapping
    public ResponseEntity<UserResponse> create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody UserCreateRequest req) {

        if (!EmailValidator.isValid(req.getUsername())) {
            throw new BadRequestException("Invalid e-mail (RFC 5322): " + req.getUsername());
        }
        UserResponse created = keycloak.createUser(authorization, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // -----------------------------------------------------------------------
    // GET /users - listar todos, com filtro opcional ?enabled=true|false
    // -----------------------------------------------------------------------
    @Operation(summary = "Lista todos os usuarios",
            description = "Filtro opcional por enabled (true/false).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Erro na estrutura da request"),
            @ApiResponse(responseCode = "401", description = "Access token invalido"),
            @ApiResponse(responseCode = "403", description = "Sem permissao")
    })
    @GetMapping
    public ResponseEntity<List<UserResponse>> list(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "enabled", required = false) Boolean enabled) {
        return ResponseEntity.ok(keycloak.listUsers(authorization, enabled));
    }

    // -----------------------------------------------------------------------
    // GET /users/{id} - recuperar por id
    // -----------------------------------------------------------------------
    @Operation(summary = "Recupera um usuario pelo id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Erro na estrutura da chamada"),
            @ApiResponse(responseCode = "401", description = "Access token invalido"),
            @ApiResponse(responseCode = "403", description = "Sem permissao"),
            @ApiResponse(responseCode = "404", description = "Objeto nao localizado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> get(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable("id") String id) {
        return ResponseEntity.ok(keycloak.getUser(authorization, id));
    }

    // -----------------------------------------------------------------------
    // PUT /users/{id} - atualizar atributos
    // -----------------------------------------------------------------------
    @Operation(summary = "Atualiza um usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Erro na estrutura da chamada"),
            @ApiResponse(responseCode = "401", description = "Access token invalido"),
            @ApiResponse(responseCode = "403", description = "Sem permissao"),
            @ApiResponse(responseCode = "404", description = "Objeto nao localizado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable("id") String id,
            @RequestBody UserUpdateRequest req) {

        if (req.getUsername() != null && !EmailValidator.isValid(req.getUsername())) {
            throw new BadRequestException("Invalid e-mail (RFC 5322): " + req.getUsername());
        }
        keycloak.updateUser(authorization, id, req);
        return ResponseEntity.ok().build();
    }

    // -----------------------------------------------------------------------
    // PATCH /users/{id} - atualizar senha
    // -----------------------------------------------------------------------
    @Operation(summary = "Atualiza a senha de um usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Erro na estrutura da chamada"),
            @ApiResponse(responseCode = "401", description = "Access token invalido"),
            @ApiResponse(responseCode = "403", description = "Sem permissao"),
            @ApiResponse(responseCode = "404", description = "Objeto nao localizado")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<Void> updatePassword(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable("id") String id,
            @Valid @RequestBody PasswordUpdateRequest req) {
        keycloak.updatePassword(authorization, id, req.getPassword());
        return ResponseEntity.ok().build();
    }

    // -----------------------------------------------------------------------
    // DELETE /users/{id} - exclusao logica (enabled=false)
    // -----------------------------------------------------------------------
    @Operation(summary = "Exclusao logica de um usuario",
            description = "Desabilita o usuario (enabled=false) via Keycloak.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "No content"),
            @ApiResponse(responseCode = "400", description = "Erro na estrutura da chamada"),
            @ApiResponse(responseCode = "401", description = "Access token invalido"),
            @ApiResponse(responseCode = "403", description = "Sem permissao"),
            @ApiResponse(responseCode = "404", description = "Objeto nao localizado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> disable(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable("id") String id) {
        keycloak.disableUser(authorization, id);
        return ResponseEntity.noContent().build();
    }
}
