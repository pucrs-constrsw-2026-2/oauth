package br.pucrs.constrsw.oauth.controller;

import br.pucrs.constrsw.oauth.dto.CreateUserRequest;
import br.pucrs.constrsw.oauth.dto.UpdatePasswordRequest;
import br.pucrs.constrsw.oauth.dto.UserResponse;
import br.pucrs.constrsw.oauth.service.KeycloakService;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "*")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final KeycloakService keycloakService;
    private final MeterRegistry meterRegistry;

    public UserController(KeycloakService keycloakService, MeterRegistry meterRegistry) {
        this.keycloakService = keycloakService;
        this.meterRegistry = meterRegistry;
    }

    /**
     * POST /users: Rota de criação de usuário no Keycloak.
     * Retorna 201 Created com o header Location e os dados do usuário criado.
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("Requisição para criação de usuário: username='{}', email='{}'", request.username(), request.email());
        UserResponse response = keycloakService.createUser(request);
        meterRegistry.counter("oauth.users.created.total").increment();

        URI location = URI.create("/users/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    /**
     * PATCH /users/{id}: Atualizar senha do usuário no Keycloak.
     * Retorna 204 No Content após sucesso.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<Void> updatePassword(
            @PathVariable("id") String id,
            @Valid @RequestBody UpdatePasswordRequest request) {
        log.info("Requisição para atualização de senha do usuário id='{}'", id);
        keycloakService.updatePassword(id, request);
        meterRegistry.counter("oauth.users.password_updated.total").increment();
        return ResponseEntity.noContent().build();
    }
}
