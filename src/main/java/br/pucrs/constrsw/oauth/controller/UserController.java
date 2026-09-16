package br.pucrs.constrsw.oauth.controller;

import br.pucrs.constrsw.oauth.dto.CreateUserRequest;
import br.pucrs.constrsw.oauth.dto.UpdatePasswordRequest;
import br.pucrs.constrsw.oauth.dto.UpdateUserRequest;
import br.pucrs.constrsw.oauth.dto.UserResponse;
import br.pucrs.constrsw.oauth.service.KeycloakService;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

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
     * GET /users (filtro ?enabled=): Consumir API do Keycloak passando parâmetros de busca.
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getUsers(@RequestParam(value = "enabled", required = false) Boolean enabled) {
        log.info("Requisição para listagem de usuários com filtro enabled='{}'", enabled);
        List<UserResponse> users = keycloakService.getUsers(enabled);
        meterRegistry.counter("oauth.users.listed.total").increment();
        return ResponseEntity.ok(users);
    }

    /**
     * GET /users/{id}: Busca simples de usuário por identificador.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable("id") String id) {
        log.info("Requisição para busca de usuário id='{}'", id);
        UserResponse user = keycloakService.getUserById(id);
        meterRegistry.counter("oauth.users.retrieved.total").increment();
        return ResponseEntity.ok(user);
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
     * PUT /users/{id}: Atualizar dados de cadastro de um usuário (first-name, last-name, email, enabled).
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable("id") String id,
            @Valid @RequestBody UpdateUserRequest request) {
        log.info("Requisição para atualização de cadastro do usuário id='{}'", id);
        UserResponse updated = keycloakService.updateUser(id, request);
        meterRegistry.counter("oauth.users.updated.total").increment();
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /users/{id}: Deleção lógica de usuário (mudar enabled para false).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") String id) {
        log.info("Requisição para deleção lógica do usuário id='{}'", id);
        keycloakService.logicalDeleteUser(id);
        meterRegistry.counter("oauth.users.deleted.total").increment();
        return ResponseEntity.noContent().build();
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

    /**
     * POST /users/{id}/roles/{roleId}: Atribuir uma Role a um usuário (API de role-mapping do Keycloak).
     */
    @PostMapping("/{id}/roles/{roleId}")
    public ResponseEntity<Void> assignRoleToUser(
            @PathVariable("id") String id,
            @PathVariable("roleId") String roleId) {
        log.info("Requisição para atribuir role id='{}' ao usuário id='{}'", roleId, id);
        keycloakService.assignRoleToUser(id, roleId);
        meterRegistry.counter("oauth.users.roles.assigned.total").increment();
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /users/{id}/roles/{roleId}: Remover uma Role de um usuário.
     */
    @DeleteMapping("/{id}/roles/{roleId}")
    public ResponseEntity<Void> removeRoleFromUser(
            @PathVariable("id") String id,
            @PathVariable("roleId") String roleId) {
        log.info("Requisição para remover role id='{}' do usuário id='{}'", roleId, id);
        keycloakService.removeRoleFromUser(id, roleId);
        meterRegistry.counter("oauth.users.roles.removed.total").increment();
        return ResponseEntity.noContent().build();
    }
}
