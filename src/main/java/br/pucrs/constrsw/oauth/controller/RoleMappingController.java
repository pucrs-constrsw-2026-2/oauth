package br.pucrs.constrsw.oauth.controller;

import br.pucrs.constrsw.oauth.dto.RoleDto;
import br.pucrs.constrsw.oauth.dto.ApiErrorResponse;
import br.pucrs.constrsw.oauth.error.UnauthorizedException;
import br.pucrs.constrsw.oauth.service.RoleMappingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Role Mapping", description = "Atribuição e remoção de roles para usuários")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/users/{id}/roles")
public class RoleMappingController {

    private final RoleMappingService service;

    public RoleMappingController(RoleMappingService service) {
        this.service = service;
    }

    private String requireAuth(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing or invalid Authorization header");
        }
        return authorization;
    }

    @Operation(summary = "Atribuir um role a um usuário", description = "Mapeia um role específico para o usuário informado pelo ID")
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Not found - Usuário ou role não localizados", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @PostMapping
    public ResponseEntity<Void> assignRole(
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable("id") String userId,
            @RequestBody RoleDto role) {
        String auth = requireAuth(authorization);
        service.assignRoleToUser(auth, userId, role);
        return ResponseEntity.status(201).build();
    }

    @Operation(summary = "Remover atribuição de um role", description = "Remove um role previamente atribuído a um usuário")
    @ApiResponse(responseCode = "204", description = "No content")
    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Not found - Usuário ou role não localizados", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @DeleteMapping("/{roleId}")
    public ResponseEntity<Void> removeRole(
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable("id") String userId,
            @PathVariable("roleId") String roleId) {
        String auth = requireAuth(authorization);
        service.removeRoleFromUser(auth, userId, roleId);
        return ResponseEntity.noContent().build();
    }
}