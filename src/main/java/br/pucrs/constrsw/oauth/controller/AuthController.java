package br.pucrs.constrsw.oauth.controller;

import br.pucrs.constrsw.oauth.dto.ErrorResponse;
import br.pucrs.constrsw.oauth.dto.LoginResponse;
import br.pucrs.constrsw.oauth.exception.OAuthApiException;
import br.pucrs.constrsw.oauth.service.KeycloakAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Auth", description = "Autenticacao de usuarios via Keycloak")
public class AuthController {

    private final KeycloakAuthService keycloakAuthService;

    public AuthController(KeycloakAuthService keycloakAuthService) {
        this.keycloakAuthService = keycloakAuthService;
    }

    @Operation(summary = "Autentica um usuario e retorna o access token emitido pelo Keycloak.")
    @ApiResponse(responseCode = "201", description = "Created",
            content = @Content(schema = @Schema(implementation = LoginResponse.class)))
    @ApiResponse(responseCode = "400", description = "Bad Request",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(path = "/login", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LoginResponse> login(@RequestParam("username") String username,
                                                @RequestParam("password") String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw OAuthApiException.badRequest("Os campos 'username' e 'password' sao obrigatorios.", "OAuthAPI");
        }

        LoginResponse token = keycloakAuthService.login(username, password);
        return ResponseEntity.status(HttpStatus.CREATED).body(token);
    }
}
