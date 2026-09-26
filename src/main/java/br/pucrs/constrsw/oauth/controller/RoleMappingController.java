package br.pucrs.constrsw.oauth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

@Tag(name = "Role Mapping", description = "Assign and remove roles for users")
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

    @Operation(summary = "Assign a role to a user", description = "Assigns the specified role to the user")
    @ApiResponse(responseCode = "201", description = "Role assigned")
    @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "User or role not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @PostMapping("/{roleId}")
    public ResponseEntity<Void> assignRole(
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable("id") String userId,
            @PathVariable("roleId") String roleId) {
        String auth = requireAuth(authorization);
        service.assignRoleToUser(auth, userId, roleId);
        return ResponseEntity.status(201).build();
    }

    @Operation(summary = "Remove a role from a user", description = "Removes a role previously assigned to a user")
    @ApiResponse(responseCode = "204", description = "Role removed")
    @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "User or role not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
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