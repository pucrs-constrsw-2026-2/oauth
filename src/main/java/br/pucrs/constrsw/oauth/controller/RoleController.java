package br.pucrs.constrsw.oauth.controller;

import br.pucrs.constrsw.oauth.dto.CreateRoleRequest;
import br.pucrs.constrsw.oauth.dto.PatchRoleRequest;
import br.pucrs.constrsw.oauth.dto.RoleResponse;
import br.pucrs.constrsw.oauth.dto.UpdateRoleRequest;
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
@RequestMapping("/roles")
@CrossOrigin(origins = "*")
public class RoleController {

    private static final Logger log = LoggerFactory.getLogger(RoleController.class);

    private final KeycloakService keycloakService;
    private final MeterRegistry meterRegistry;

    public RoleController(KeycloakService keycloakService, MeterRegistry meterRegistry) {
        this.keycloakService = keycloakService;
        this.meterRegistry = meterRegistry;
    }

    /**
     * POST /roles: Criar cargo.
     */
    @PostMapping
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request) {
        log.info("Requisição para criação de cargo: name='{}'", request.name());
        RoleResponse role = keycloakService.createRole(request);
        meterRegistry.counter("oauth.roles.created.total").increment();

        URI location = URI.create("/roles/" + role.id());
        return ResponseEntity.created(location).body(role);
    }

    /**
     * GET /roles: Listar cargos.
     */
    @GetMapping
    public ResponseEntity<List<RoleResponse>> getRoles() {
        log.info("Requisição para listagem de cargos");
        List<RoleResponse> roles = keycloakService.getRoles();
        meterRegistry.counter("oauth.roles.listed.total").increment();
        return ResponseEntity.ok(roles);
    }

    /**
     * GET /roles/{id}: Buscar cargo específico por ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RoleResponse> getRoleById(@PathVariable("id") String id) {
        log.info("Requisição para busca de cargo id='{}'", id);
        RoleResponse role = keycloakService.getRole(id);
        meterRegistry.counter("oauth.roles.retrieved.total").increment();
        return ResponseEntity.ok(role);
    }

    /**
     * PUT /roles/{id}: Atualizar cargo completo.
     */
    @PutMapping("/{id}")
    public ResponseEntity<RoleResponse> updateRole(
            @PathVariable("id") String id,
            @Valid @RequestBody UpdateRoleRequest request) {
        log.info("Requisição para atualização completa do cargo id='{}'", id);
        RoleResponse updated = keycloakService.updateRole(id, request);
        meterRegistry.counter("oauth.roles.updated.total").increment();
        return ResponseEntity.ok(updated);
    }

    /**
     * PATCH /roles/{id}: Atualizar cargo parcial.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<RoleResponse> patchRole(
            @PathVariable("id") String id,
            @RequestBody PatchRoleRequest request) {
        log.info("Requisição para atualização parcial do cargo id='{}'", id);
        RoleResponse patched = keycloakService.patchRole(id, request);
        meterRegistry.counter("oauth.roles.patched.total").increment();
        return ResponseEntity.ok(patched);
    }

    /**
     * DELETE /roles/{id}: Deleção lógica de role.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable("id") String id) {
        log.info("Requisição para deleção lógica do cargo id='{}'", id);
        keycloakService.logicalDeleteRole(id);
        meterRegistry.counter("oauth.roles.deleted.total").increment();
        return ResponseEntity.noContent().build();
    }
}
