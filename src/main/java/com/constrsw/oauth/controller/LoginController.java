package com.constrsw.oauth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.constrsw.oauth.dto.LoginResponse;
import com.constrsw.oauth.exception.BadRequestException;
import com.constrsw.oauth.service.KeycloakService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * POST /login - autenticacao de usuario via password grant do Keycloak.
 * Aceita form-data (multipart) e application/x-www-form-urlencoded, como pede
 * o enunciado ("form-data incluindo username, password").
 */
@RestController
@RequestMapping("/login")
@Tag(name = "Auth", description = "Autenticacao de usuarios")
@SecurityRequirements
public class LoginController {

    private final KeycloakService keycloak;

    public LoginController(KeycloakService keycloak) {
        this.keycloak = keycloak;
    }

    @Operation(summary = "Autenticacao de usuario",
            description = "Consome POST /realms/{realm}/protocol/openid-connect/token do Keycloak "
                    + "com grant_type=password e devolve os tokens gerados.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Erro na estrutura da chamada"),
            @ApiResponse(responseCode = "401", description = "Username e/ou password invalidos")
    })
    @PostMapping(consumes = {"multipart/form-data", "application/x-www-form-urlencoded"})
    public ResponseEntity<LoginResponse> login(
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "password", required = false) String password) {

        if (username == null || username.isBlank()) {
            throw new BadRequestException("username is required");
        }
        if (password == null || password.isBlank()) {
            throw new BadRequestException("password is required");
        }

        LoginResponse body = keycloak.login(username, password);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }
}
